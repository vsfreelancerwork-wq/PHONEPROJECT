package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

data class LoggedInUser(
    val id: String,
    val email: String,
    val name: String,
    val role: String, // mainadmin | admin | staff
    val perms: List<String> = emptyList(),
    val dept: String = ""
)

class AppRepository(private val dao: AppDao) {

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<LoggedInUser?>(null)
    val currentUser: StateFlow<LoggedInUser?> = _currentUser.asStateFlow()

    fun login(user: LoggedInUser) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }

    // --- Data Flows ---
    val tasksFlow: Flow<List<Task>> = dao.getTasksFlow()
    val employeesFlow: Flow<List<Employee>> = dao.getEmployeesFlow()
    val issuesFlow: Flow<List<Issue>> = dao.getIssuesFlow()
    val handoversFlow: Flow<List<Handover>> = dao.getHandoversFlow()
    val delegationsFlow: Flow<List<Delegation>> = dao.getDelegationsFlow()
    val departmentsFlow: Flow<List<Department>> = dao.getDepartmentsFlow()
    val trashItemsFlow: Flow<List<TrashItem>> = dao.getTrashItemsFlow()
    val activityLogsFlow: Flow<List<GlobalActivityLog>> = dao.getActivityLogsFlow()

    fun getLinksFlowForUser(userId: String): Flow<List<PersonalLink>> {
        return dao.getLinksByUserIdFlow(userId)
    }

    // --- Direct Access Helpers (Once) ---
    suspend fun getTasksOnce(): List<Task> = dao.getTasksOnce()
    suspend fun getEmployeesOnce(): List<Employee> = dao.getEmployeesOnce()
    suspend fun getEmployeeByEmail(email: String): Employee? = dao.getEmployeeByEmail(email)
    suspend fun getDepartmentsOnce(): List<Department> = dao.getDepartmentsOnce()

    // --- Operations ---
    suspend fun insertTask(task: Task, user: String) {
        dao.insertTask(task)
        logActivity(user, "Task Added/Updated", "Task '${task.name}' added or updated.")
    }

    suspend fun deleteOrCreateTask(task: Task, user: String) {
        dao.insertTask(task)
    }

    suspend fun softDeleteTask(task: Task, user: String) {
        val json = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
            .adapter(Task::class.java)
            .toJson(task)

        val trash = TrashItem(
            id = Utils.uid(),
            originalId = task.id,
            type = "task",
            name = task.name,
            jsonData = json,
            deletedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())
        )
        dao.insertTrashItem(trash)
        dao.deleteTaskById(task.id)
        logActivity(user, "Task Deleted (Soft)", "Task '${task.name}' moved to trash.")
    }

    suspend fun restoreTask(trashId: String, task: Task, user: String) {
        dao.insertTask(task)
        dao.deleteTrashItemById(trashId)
        logActivity(user, "Task Restored", "Task '${task.name}' restored from trash.")
    }

    suspend fun permanentlyDeleteTrash(id: String, name: String, type: String, user: String) {
        dao.deleteTrashItemById(id)
        logActivity(user, "Permanent Delete", "Permanently deleted $type '$name' from trash.")
    }

    // --- Employees ---
    suspend fun insertEmployee(employee: Employee, user: String) {
        dao.insertEmployee(employee)
        logActivity(user, "Employee Saved", "Employee ${employee.name} (${employee.role}) added/updated.")
    }

    suspend fun softDeleteEmployee(employee: Employee, user: String) {
        val json = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
            .adapter(Employee::class.java)
            .toJson(employee)

        val trash = TrashItem(
            id = Utils.uid(),
            originalId = employee.id,
            type = "employee",
            name = employee.name,
            jsonData = json,
            deletedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())
        )
        dao.insertTrashItem(trash)
        dao.deleteEmployeeById(employee.id)
        logActivity(user, "Employee Deleted (Soft)", "Employee '${employee.name}' moved to trash.")
    }

    suspend fun restoreEmployee(trashId: String, employee: Employee, user: String) {
        dao.insertEmployee(employee)
        dao.deleteTrashItemById(trashId)
        logActivity(user, "Employee Restored", "Employee '${employee.name}' restored from trash.")
    }

    // --- Issues ---
    suspend fun insertIssue(issue: Issue, user: String) {
        dao.insertIssue(issue)
        logActivity(user, "Issue Saved", "Issue '${issue.title}' created/updated.")
    }

    suspend fun softDeleteIssue(issue: Issue, user: String) {
        val json = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
            .adapter(Issue::class.java)
            .toJson(issue)

        val trash = TrashItem(
            id = Utils.uid(),
            originalId = issue.id,
            type = "issue",
            name = issue.title,
            jsonData = json,
            deletedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())
        )
        dao.insertTrashItem(trash)
        dao.deleteIssueById(issue.id)
        logActivity(user, "Issue Deleted (Soft)", "Issue '${issue.title}' moved to trash.")
    }

    suspend fun restoreIssue(trashId: String, issue: Issue, user: String) {
        dao.insertIssue(issue)
        dao.deleteTrashItemById(trashId)
        logActivity(user, "Issue Restored", "Issue '${issue.title}' restored from trash.")
    }

    // --- Handovers ---
    suspend fun insertHandover(handover: Handover, user: String) {
        dao.insertHandover(handover)
        logActivity(user, "Handover Created/Updated", "Handover from ${handover.fromName} to ${handover.toName} in ${handover.dept} is ${handover.status}.")
    }

    // --- Delegations ---
    suspend fun insertDelegation(delegation: Delegation, user: String) {
        dao.insertDelegation(delegation)
        logActivity(user, "Delegation Saved", "Delegation task '${delegation.task}' is ${delegation.status}.")
    }

    // --- Departments ---
    suspend fun insertDepartment(dept: Department, user: String) {
        dao.insertDepartment(dept)
        logActivity(user, "Department Added", "Department '${dept.name}' registered.")
    }

    suspend fun deleteDepartment(id: String, name: String, user: String) {
        dao.deleteDepartmentById(id)
        logActivity(user, "Department Deleted", "Department '$name' removed.")
    }

    // --- Links ---
    suspend fun insertLink(link: PersonalLink) {
        dao.insertLink(link)
    }

    suspend fun deleteLink(id: String) {
        dao.deleteLinkById(id)
    }

    // --- Global Logs ---
    suspend fun logActivity(by: String, action: String, details: String) {
        val log = GlobalActivityLog(
            id = Utils.uid(),
            by = by,
            action = action,
            details = details,
            at = Utils.fDateTime()
        )
        dao.insertActivityLog(log)
    }

    suspend fun clearActivityLogs() {
        dao.clearAllActivityLogs()
    }

    suspend fun clearTrash() {
        dao.clearTrash()
    }

    // --- Initial Seeding ---
    suspend fun seedInitialDataIfNecessary() {
        val existingDepts = dao.getDepartmentsOnce()
        if (existingDepts.isEmpty()) {
            // Seed Departments
            val depts = listOf(
                Department(Utils.uid(), "CARDIOLOGY"),
                Department(Utils.uid(), "EMERGENCY"),
                Department(Utils.uid(), "ICU"),
                Department(Utils.uid(), "NURSING"),
                Department(Utils.uid(), "RADIOLOGY"),
                Department(Utils.uid(), "OUTPATIENT")
            )
            for (d in depts) {
                dao.insertDepartment(d)
            }

            // Seed Admins and Employees
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

            val initialEmployees = listOf(
                Employee(
                    id = Utils.uid(),
                    name = "VIBHAV SHARMA",
                    dept = "ICU",
                    role = "Senior Administrator",
                    contact = "9876543210",
                    email = "vibhav@hospital.com",
                    password = "password123",
                    perms = allPerms
                ),
                Employee(
                    id = Utils.uid(),
                    name = "AMIT PATEL",
                    dept = "CARDIOLOGY",
                    role = "Department Head",
                    contact = "9876543211",
                    email = "amit@hospital.com",
                    password = "password123",
                    perms = listOf("tasks_view", "tasks_add", "tasks_edit", "issues_view", "issues_add", "handover_view", "handover_edit", "all_task_details")
                ),
                Employee(
                    id = Utils.uid(),
                    name = "PRIYA SINGH",
                    dept = "NURSING",
                    role = "Nursing Staff",
                    contact = "9876543212",
                    email = "priya@hospital.com",
                    password = "password123",
                    perms = listOf("tasks_view", "issues_add")
                )
            )

            dao.insertEmployees(initialEmployees)

            // Seed Some Tasks
            val today = Utils.toDay()
            val sampleTasks = listOf(
                Task(
                    id = Utils.uid(),
                    name = "EQUIPMENT CALIBRATION",
                    dept = "ICU",
                    freq = "daily",
                    assignedTo = listOf("VIBHAV SHARMA"),
                    assigneeEmails = listOf("vibhav@hospital.com"),
                    schedDate = today,
                    time = "09:00",
                    priority = "high",
                    notes = "Calibrate all patient monitors and emergency ventilators.",
                    status = "pending",
                    created = today,
                    createdBy = "VIBHAV SHARMA"
                ),
                Task(
                    id = Utils.uid(),
                    name = "CARDIAC MONITOR CHECK",
                    dept = "CARDIOLOGY",
                    freq = "daily",
                    assignedTo = listOf("AMIT PATEL"),
                    assigneeEmails = listOf("amit@hospital.com"),
                    schedDate = today,
                    time = "10:30",
                    priority = "high",
                    notes = "Perform morning rounds inspection of cardiac monitors.",
                    status = "pending",
                    created = today,
                    createdBy = "AMIT PATEL"
                ),
                Task(
                    id = Utils.uid(),
                    name = "MEDICINE STOCK COUNT",
                    dept = "NURSING",
                    freq = "daily",
                    assignedTo = listOf("PRIYA SINGH"),
                    assigneeEmails = listOf("priya@hospital.com"),
                    schedDate = today,
                    time = "14:00",
                    priority = "medium",
                    notes = "Count and inventory life-saving drug levels.",
                    status = "pending",
                    created = today,
                    createdBy = "AMIT PATEL"
                )
            )
            dao.insertTasks(sampleTasks)

            // Seed Some Issues
            dao.insertIssue(
                Issue(
                    id = Utils.uid(),
                    title = "Defibrillator Power Cord Damaged",
                    dept = "CARDIOLOGY",
                    priority = "high",
                    reporter = "AMIT PATEL",
                    status = "open",
                    details = "Defibrillator cord in Room 4 is torn. Urgent replacement needed.",
                    createdAt = today
                )
            )

            logActivity("System", "Initial Seeding", "Pre-populated database with standard departments, roles, and sample tasks.")
        }
    }
}
