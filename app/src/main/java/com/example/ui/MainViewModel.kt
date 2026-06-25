package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed interface InactivityState {
    object Active : InactivityState
    object Warning : InactivityState // 1 minute left (4 mins elapsed)
    object LoggedOut : InactivityState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.appDao())

    // --- Core Flows ---
    val currentUser = repository.currentUser
    val tasks = repository.tasksFlow
    val employees = repository.employeesFlow
    val issues = repository.issuesFlow
    val handovers = repository.handoversFlow
    val delegations = repository.delegationsFlow
    val departments = repository.departmentsFlow
    val trashItems = repository.trashItemsFlow
    val activityLogs = repository.activityLogsFlow

    // Personal links flow based on logged in user
    private val _personalLinks = MutableStateFlow<List<PersonalLink>>(emptyList())
    val personalLinks: StateFlow<List<PersonalLink>> = _personalLinks.asStateFlow()

    // --- Settings & UI States ---
    private val _isDarkMode = MutableStateFlow(true) // defaults to true as requested
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // --- Inactivity Timer ---
    private val _inactivityState = MutableStateFlow<InactivityState>(InactivityState.Active)
    val inactivityState: StateFlow<InactivityState> = _inactivityState.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(60)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private var inactivityJob: Job? = null
    private var lastActivityTime = System.currentTimeMillis()

    // --- Active Auto Cycle Check Flag ---
    private var isCyclingInProgress = false

    init {
        // Seed initial data asynchronously on start
        viewModelScope.launch {
            repository.seedInitialDataIfNecessary()
            startInactivityTimer()
            observeTasksAndAutoCycle()
        }

        // Listen for current user to reload links
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    repository.getLinksFlowForUser(user.email).collect { linksList ->
                        _personalLinks.value = linksList
                    }
                } else {
                    _personalLinks.value = emptyList()
                }
            }
        }
    }

    // --- Auto-Cycle Observer ---
    private fun observeTasksAndAutoCycle() {
        viewModelScope.launch {
            tasks.collect { taskList ->
                if (taskList.isNotEmpty() && !isCyclingInProgress) {
                    isCyclingInProgress = true
                    try {
                        val newChildren = Utils.autoCycleTasks(taskList)
                        if (newChildren.isNotEmpty()) {
                            for (child in newChildren) {
                                repository.insertTask(child, "System (Auto-Cycle)")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isCyclingInProgress = false
                    }
                }
            }
        }
    }

    // --- Session & Inactivity Tracker ---
    fun resetInactivityTimer() {
        lastActivityTime = System.currentTimeMillis()
        if (_inactivityState.value != InactivityState.LoggedOut) {
            _inactivityState.value = InactivityState.Active
        }
    }

    private fun startInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val user = currentUser.value
                if (user == null) {
                    _inactivityState.value = InactivityState.Active
                    continue
                }

                val now = System.currentTimeMillis()
                val elapsedMs = now - lastActivityTime

                val warningThreshold = 4 * 60 * 1000L // 4 minutes
                val logoutThreshold = 5 * 60 * 1000L  // 5 minutes

                if (elapsedMs >= logoutThreshold) {
                    _inactivityState.value = InactivityState.LoggedOut
                    logout()
                } else if (elapsedMs >= warningThreshold) {
                    _inactivityState.value = InactivityState.Warning
                    val secondsLeft = ((logoutThreshold - elapsedMs) / 1000).toInt()
                    _secondsRemaining.value = secondsLeft.coerceIn(0, 60)
                } else {
                    _inactivityState.value = InactivityState.Active
                }
            }
        }
    }

    // --- Auth Actions ---
    fun login(username: String, password: String): Boolean {
        // 1. Check Hardcoded MainAdmin
        if (username == "VIBHAV" && password == "Vibhav@0206") {
            val allPerms = listOf(
                "tasks_view", "tasks_add", "tasks_edit", "tasks_delete", "tasks_assign",
                "issues_view", "issues_add", "issues_resolve",
                "employees_view", "employees_edit",
                "handover_view", "handover_edit",
                "departments_view", "departments_edit",
                "tracking_view", "checklist_view", "escalation_view",
                "mis_view", "trash_view",
                "delegation_view", "delegation_add",
                "all_task_details"
            )
            repository.login(
                LoggedInUser(
                    id = "mainadmin",
                    email = "vibhav@hospital.com",
                    name = "VIBHAV SHARMA",
                    role = "mainadmin",
                    perms = allPerms,
                    dept = "ADMINISTRATION"
                )
            )
            resetInactivityTimer()
            viewModelScope.launch {
                repository.logActivity("VIBHAV", "Login", "Main Administrator logged in.")
            }
            return true
        }

        // 2. Check Employee DB
        var success = false
        viewModelScope.launch {
            val employee = repository.getEmployeeByEmail(username) ?: repository.getEmployeesOnce().find { it.name.lowercase() == username.lowercase() }
            if (employee != null && employee.password == password) {
                // Determine Role: admin if they have any of the management permissions, otherwise staff
                val isManager = employee.perms.any { 
                    it in listOf("employees_edit", "departments_edit", "trash_view", "mis_view", "tasks_delete")
                }
                val role = if (isManager) "admin" else "staff"

                repository.login(
                    LoggedInUser(
                        id = employee.id,
                        email = employee.email,
                        name = employee.name,
                        role = role,
                        perms = employee.perms,
                        dept = employee.dept
                    )
                )
                success = true
                resetInactivityTimer()
                repository.logActivity(employee.name, "Login", "Employee logged in with role $role.")
            }
        }
        
        // Wait a small moment because database query is suspend
        Thread.sleep(150) // safe for local dev
        return currentUser.value != null
    }

    fun logout() {
        val user = currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logActivity(user.name, "Logout", "Logged out of system.")
            }
        }
        repository.logout()
        _inactivityState.value = InactivityState.Active
    }

    fun changePassword(newPass: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val emp = repository.getEmployeeByEmail(user.email)
            if (emp != null) {
                val updated = emp.copy(password = newPass)
                repository.insertEmployee(updated, user.name)
                repository.logActivity(user.name, "Change Password", "Changed password successfully.")
            } else if (user.role == "mainadmin") {
                repository.logActivity("VIBHAV", "Change Password", "Attempted to change hardcoded admin password (ignored).")
            }
        }
    }

    // --- Task Actions ---
    fun addTask(
        name: String,
        dept: String,
        freq: String,
        assignedTo: List<String>,
        priority: String,
        notes: String,
        time: String,
        schedDate: String
    ) {
        val creator = currentUser.value?.name ?: "System"
        val emails = mutableListOf<String>()
        viewModelScope.launch {
            val allEmps = repository.getEmployeesOnce()
            for (nameSelected in assignedTo) {
                val e = allEmps.find { it.name == nameSelected }
                if (e != null) emails.add(e.email)
            }

            val task = Task(
                id = Utils.uid(),
                name = name.uppercase(Locale.US),
                dept = dept,
                freq = freq,
                assignedTo = assignedTo,
                assigneeEmails = emails,
                schedDate = schedDate,
                time = time,
                priority = priority,
                notes = notes,
                status = "pending",
                created = Utils.toDay(),
                createdBy = creator,
                activityLog = listOf(
                    ActivityLogEntry(by = creator, action = "Created", details = "Task created from scratch", at = Utils.fDateTime())
                )
            )
            repository.insertTask(task, creator)
        }
    }

    fun editTask(task: Task) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            val updatedLog = task.activityLog.toMutableList().apply {
                add(ActivityLogEntry(by = user, action = "Edited", details = "Task properties modified", at = Utils.fDateTime()))
            }
            repository.insertTask(task.copy(activityLog = updatedLog), user)
        }
    }

    fun deleteTask(task: Task) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            repository.softDeleteTask(task, user)
        }
    }

    fun markTaskDone(task: Task, remark: String, delayReason: String) {
        val user = currentUser.value?.name ?: "System"
        val todayStr = Utils.toDay()
        val overdue = todayStr > task.schedDate
        
        viewModelScope.launch {
            val updatedHistory = task.completionHistory.toMutableList().apply {
                add("${Utils.fDateTime()} by $user")
            }
            
            val updatedLog = task.activityLog.toMutableList().apply {
                add(ActivityLogEntry(
                    by = user, 
                    action = "Completed", 
                    details = "Marked completed. Remark: $remark. Delay Reason: $delayReason", 
                    at = Utils.fDateTime()
                ))
            }

            val doneTask = task.copy(
                status = "done",
                doneBy = user,
                doneTime = Utils.fDateTime(),
                doneRemark = remark,
                delayReason = delayReason,
                isDelayed = overdue || delayReason.isNotEmpty(),
                lastDone = todayStr,
                completionHistory = updatedHistory,
                activityLog = updatedLog
            )

            // If virtual task / cycle pending task
            if (task.parentTaskId.isNotEmpty()) {
                // Update parent task's lastDone = today
                val parent = repository.getTasksOnce().find { it.id == task.parentTaskId }
                if (parent != null) {
                    repository.deleteOrCreateTask(parent.copy(lastDone = todayStr), user)
                }
            }

            repository.insertTask(doneTask, user)

            // Back-date fix: if a daily task is completed late, spawn another child for today!
            if (task.freq.lowercase() == "daily" && task.schedDate < todayStr) {
                val alreadyHasToday = repository.getTasksOnce().any { 
                    it.freq.lowercase() == "daily" && 
                    it.name == task.name && 
                    it.schedDate == todayStr && 
                    it.status == "pending" 
                }
                if (!alreadyHasToday) {
                    val child = Task(
                        id = Utils.uid(),
                        name = task.name,
                        dept = task.dept,
                        freq = task.freq,
                        assignedTo = task.assignedTo,
                        assigneeEmails = task.assigneeEmails,
                        schedDate = todayStr,
                        time = task.time,
                        priority = task.priority,
                        notes = task.notes,
                        status = "pending",
                        created = todayStr,
                        createdBy = task.createdBy,
                        activityLog = listOf(
                            ActivityLogEntry(by = "System", action = "Late Back-Date Fix", details = "Spawned child daily task for today", at = Utils.fDateTime())
                        ),
                        parentTaskId = task.parentTaskId.ifEmpty { task.id }
                    )
                    repository.insertTask(child, "System")
                }
            }
        }
    }

    // --- Handover Actions ---
    fun addHandover(toName: String, dept: String, dateStart: String, dateEnd: String, reason: String, taskIds: List<String>) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val handover = Handover(
                id = Utils.uid(),
                fromName = user.name,
                toName = toName,
                dept = dept,
                dateStart = dateStart,
                dateEnd = dateEnd,
                reason = reason,
                taskIds = taskIds,
                status = "pending",
                createdAt = Utils.toDay()
            )
            repository.insertHandover(handover, user.name)
        }
    }

    fun updateHandoverStatus(handoverId: String, status: String, remark: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val ho = database.appDao().getHandoverById(handoverId) ?: return@launch
            val updated = ho.copy(status = status, remark = remark)
            repository.insertHandover(updated, user.name)
        }
    }

    // --- Delegation Actions ---
    fun addDelegation(task: String, doerName: String, dept: String, dueDate: String, remarks: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val delegation = Delegation(
                id = Utils.uid(),
                task = task.uppercase(Locale.US),
                doerName = doerName,
                dept = dept,
                dueDate = dueDate,
                remarks = remarks,
                status = "pending",
                createdBy = user.name,
                createdAt = Utils.toDay()
            )
            repository.insertDelegation(delegation, user.name)

            // Also inject a virtual task representing the delegation so it appears in tasks lists
            val allEmps = repository.getEmployeesOnce()
            val emp = allEmps.find { it.name == doerName }
            val taskRepresentation = Task(
                id = delegation.id, // match delegation id so edits sync
                name = "[DELEGATION] ${task.uppercase(Locale.US)}",
                dept = dept,
                freq = "delegation",
                assignedTo = listOf(doerName),
                assigneeEmails = if (emp != null) listOf(emp.email) else emptyList(),
                schedDate = dueDate,
                time = "17:00",
                priority = "medium",
                notes = remarks,
                status = "pending",
                created = Utils.toDay(),
                createdBy = user.name
            )
            repository.insertTask(taskRepresentation, user.name)
        }
    }

    fun updateDelegationStatus(id: String, status: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val del = database.appDao().getDelegationById(id) ?: return@launch
            val updated = del.copy(status = status)
            repository.insertDelegation(updated, user.name)

            // Sync task representation
            val task = database.appDao().getTaskById(id)
            if (task != null) {
                if (status == "done") {
                    repository.insertTask(task.copy(status = "done", doneBy = user.name, doneTime = Utils.fDateTime()), user.name)
                } else {
                    repository.insertTask(task.copy(status = "pending"), user.name)
                }
            }
        }
    }

    fun requestDelegationExtension(id: String, reason: String, newDate: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val del = database.appDao().getDelegationById(id) ?: return@launch
            
            // Allow up to 3 extension requests
            if (del.extensionRequests.size >= 3) {
                return@launch
            }

            val request = ExtensionRequest(
                requestedAt = Utils.fDateTime(),
                reason = reason,
                newDate = newDate,
                status = "pending"
            )
            val updatedRequests = del.extensionRequests.toMutableList().apply { add(request) }
            val updated = del.copy(
                status = "extension-requested",
                extensionRequests = updatedRequests
            )
            repository.insertDelegation(updated, user.name)
        }
    }

    fun approveDelegationExtension(id: String, requestIndex: Int, approve: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val del = database.appDao().getDelegationById(id) ?: return@launch
            val requests = del.extensionRequests.toMutableList()
            if (requestIndex < 0 || requestIndex >= requests.size) return@launch

            val req = requests[requestIndex]
            val updatedReq = req.copy(status = if (approve) "approved" else "rejected")
            requests[requestIndex] = updatedReq

            val newStatus = if (approve) "extended" else "pending"
            val newDueDate = if (approve) req.newDate else del.dueDate

            val updated = del.copy(
                status = newStatus,
                dueDate = newDueDate,
                extensionRequests = requests
            )
            repository.insertDelegation(updated, user.name)

            // Update associated task representation
            val task = database.appDao().getTaskById(id)
            if (task != null) {
                repository.insertTask(task.copy(schedDate = newDueDate), user.name)
            }
        }
    }

    // --- Issue Actions ---
    fun addIssue(title: String, dept: String, priority: String, details: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val issue = Issue(
                id = Utils.uid(),
                title = title,
                dept = dept,
                priority = priority,
                reporter = user.name,
                status = "open",
                details = details,
                createdAt = Utils.toDay()
            )
            repository.insertIssue(issue, user.name)
        }
    }

    fun resolveIssue(id: String, resolution: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val issue = database.appDao().getIssueById(id) ?: return@launch
            val resolved = issue.copy(
                status = "resolved",
                resolution = resolution,
                resolvedAt = Utils.fDateTime(),
                resolvedBy = user.name
            )
            repository.insertIssue(resolved, user.name)
        }
    }

    fun deleteIssue(issue: Issue) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            repository.softDeleteIssue(issue, user)
        }
    }

    // --- Employee Management ---
    fun addEmployee(name: String, dept: String, role: String, contact: String, email: String, password: String, perms: List<String>) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            val emp = Employee(
                id = Utils.uid(),
                name = name.uppercase(Locale.US),
                dept = dept,
                role = role,
                contact = contact,
                email = email,
                password = password,
                perms = perms
            )
            repository.insertEmployee(emp, user)
        }
    }

    fun editEmployee(emp: Employee) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            repository.insertEmployee(emp, user)
        }
    }

    fun deleteEmployee(emp: Employee) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            repository.softDeleteEmployee(emp, user)
        }
    }

    // --- Department Actions ---
    fun addDepartment(name: String) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            val dept = Department(Utils.uid(), name.uppercase(Locale.US))
            repository.insertDepartment(dept, user)
        }
    }

    fun deleteDepartment(dept: Department) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            repository.deleteDepartment(dept.id, dept.name, user)
        }
    }

    // --- Trash Actions ---
    fun restoreTrashItem(item: TrashItem) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            when (item.type) {
                "task" -> {
                    val task = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                        .adapter(Task::class.java)
                        .fromJson(item.jsonData)
                    if (task != null) {
                        repository.restoreTask(item.id, task, user)
                    }
                }
                "employee" -> {
                    val emp = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                        .adapter(Employee::class.java)
                        .fromJson(item.jsonData)
                    if (emp != null) {
                        repository.restoreEmployee(item.id, emp, user)
                    }
                }
                "issue" -> {
                    val issue = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                        .adapter(Issue::class.java)
                        .fromJson(item.jsonData)
                    if (issue != null) {
                        repository.restoreIssue(item.id, issue, user)
                    }
                }
            }
        }
    }

    fun deleteTrashPermanently(item: TrashItem) {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            repository.permanentlyDeleteTrash(item.id, item.name, item.type, user)
        }
    }

    fun emptyTrash() {
        val user = currentUser.value?.name ?: "System"
        viewModelScope.launch {
            repository.clearTrash()
            repository.logActivity(user, "Empty Trash", "Permanently emptied all soft-deleted items.")
        }
    }

    // --- Link Actions ---
    fun addLink(title: String, url: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val link = PersonalLink(
                id = Utils.uid(),
                userId = user.email,
                title = title,
                url = url
            )
            repository.insertLink(link)
        }
    }

    fun deleteLink(id: String) {
        viewModelScope.launch {
            repository.deleteLink(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearActivityLogs()
        }
    }
}
