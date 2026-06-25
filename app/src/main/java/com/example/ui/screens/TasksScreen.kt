package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userState by viewModel.currentUser.collectAsState()
    val user = userState ?: return

    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val departments by viewModel.departments.collectAsState(initial = emptyList())

    val context = LocalContext.current

    // Navigation and Tab States
    var selectedTab by remember { mutableStateOf(0) } // 0 = My Tasks, 1 = All Task Details
    val hasAllDetailsPerm = user.perms.contains("all_task_details")

    // Filters
    var searchKeyword by remember { mutableStateOf("") }
    var deptFilter by remember { mutableStateOf("All") }
    var statusFilter by remember { mutableStateOf("All") }
    var freqFilter by remember { mutableStateOf("All") }
    var delayFilter by remember { mutableStateOf("All") } // All, Delayed, On-Time

    // Dialog States
    var showMarkDoneDialog by remember { mutableStateOf<Task?>(null) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf<Task?>(null) }

    // Handovers (for badges/locking)
    val handovers by viewModel.handovers.collectAsState(initial = emptyList())
    val activeHandoversToMe = handovers.filter { 
        it.status == "accepted" && 
        it.toName == user.name && 
        Utils.toDay() >= it.dateStart && 
        Utils.toDay() <= it.dateEnd 
    }
    val activeHandoversFromMe = handovers.filter { 
        it.status == "accepted" && 
        it.fromName == user.name && 
        Utils.toDay() >= it.dateStart && 
        Utils.toDay() <= it.dateEnd 
    }

    // Filter Tasks list
    val processedTasks = remember(allTasks, selectedTab, searchKeyword, deptFilter, statusFilter, freqFilter, delayFilter, activeHandoversToMe) {
        var baseList = if (selectedTab == 0) {
            // My Tasks (Assigned to me, OR tasks handed over to me!)
            val handedOverTaskIds = activeHandoversToMe.flatMap { it.taskIds }.toSet()
            allTasks.filter { task ->
                val isDirectlyAssigned = user.name in task.assignedTo || user.email in task.assigneeEmails
                val isHandedOver = task.id in handedOverTaskIds
                isDirectlyAssigned || isHandedOver
            }
        } else {
            // All Details (Full System)
            allTasks
        }

        // Apply search
        if (searchKeyword.isNotEmpty()) {
            baseList = baseList.filter { 
                it.name.contains(searchKeyword, ignoreCase = true) || 
                it.dept.contains(searchKeyword, ignoreCase = true) ||
                it.createdBy.contains(searchKeyword, ignoreCase = true)
            }
        }

        // Apply filters
        if (deptFilter != "All") {
            baseList = baseList.filter { it.dept.lowercase() == deptFilter.lowercase() }
        }
        if (statusFilter != "All") {
            baseList = baseList.filter { it.status.lowercase() == statusFilter.lowercase() }
        }
        if (freqFilter != "All") {
            baseList = baseList.filter { it.freq.lowercase() == freqFilter.lowercase() }
        }
        if (delayFilter != "All") {
            baseList = baseList.filter {
                val isOverdue = Utils.toDay() > it.schedDate && it.status == "pending"
                if (delayFilter == "Delayed") isOverdue || it.isDelayed else !(isOverdue || it.isDelayed)
            }
        }

        baseList
    }

    Scaffold(
        floatingActionButton = {
            if (user.perms.contains("tasks_add") || user.role == "mainadmin") {
                FloatingActionButton(
                    onClick = { showCreateTaskDialog = true },
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_task_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Task")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selector
            if (hasAllDetailsPerm) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = TealPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Tasks", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("my_tasks_tab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("All Task Details", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("all_tasks_tab")
                    )
                }
            }

            // Filters panel
            Card(
                shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search and Export
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchKeyword,
                            onValueChange = { searchKeyword = it },
                            placeholder = { Text("Search tasks, dept, creator...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("task_search_bar")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val exportData = processedTasks.map {
                                    mapOf(
                                        "ID" to it.id,
                                        "Task Name" to it.name,
                                        "Department" to it.dept,
                                        "Frequency" to it.freq,
                                        "Scheduled Date" to it.schedDate,
                                        "Time" to it.time,
                                        "Priority" to it.priority,
                                        "Status" to it.status,
                                        "Assigned To" to it.assignedTo.joinToString("; "),
                                        "Created By" to it.createdBy,
                                        "Delay Reason" to it.doneRemark
                                    )
                                }
                                Utils.exportToExcel(context, exportData, "Tasks_Export")
                            },
                            modifier = Modifier.testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export Excel", tint = TealPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Drops filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Dept Filter
                        FilterDropdown(
                            label = "Dept",
                            selected = deptFilter,
                            options = listOf("All") + departments.map { it.name },
                            onSelected = { deptFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                        // Status Filter
                        FilterDropdown(
                            label = "Status",
                            selected = statusFilter,
                            options = listOf("All", "Pending", "Done"),
                            onSelected = { statusFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                        // Freq Filter
                        FilterDropdown(
                            label = "Freq",
                            selected = freqFilter,
                            options = listOf("All", "Daily", "15-day", "Monthly", "Quarterly", "Half-Yearly", "Yearly", "Delegation"),
                            onSelected = { freqFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                        // Delay Filter
                        FilterDropdown(
                            label = "Overdue",
                            selected = delayFilter,
                            options = listOf("All", "Delayed", "On-Time"),
                            onSelected = { delayFilter = it },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }

            // Tasks List
            if (processedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Assignment, contentDescription = "No Tasks", tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No tasks found matching filters.", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
                ) {
                    items(processedTasks, key = { it.id }) { task ->
                        // Check if handed over from me or to me
                        val handedOverToMe = activeHandoversToMe.any { task.id in it.taskIds }
                        val handedOverFromMe = activeHandoversFromMe.any { task.id in it.taskIds }
                        val handoverToName = activeHandoversFromMe.find { task.id in it.taskIds }?.toName ?: ""
                        val handoverFromName = activeHandoversToMe.find { task.id in it.taskIds }?.fromName ?: ""

                        TaskRow(
                            task = task,
                            canEditDelete = selectedTab == 1 && user.role == "mainadmin", // only main admin can edit/delete in all details tab
                            handedOverToMe = handedOverToMe,
                            handedOverFromMe = handedOverFromMe,
                            handoverToName = handoverToName,
                            handoverFromName = handoverFromName,
                            onMarkDone = { showMarkDoneDialog = task },
                            onEdit = { showEditTaskDialog = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs / Modals
    if (showMarkDoneDialog != null) {
        MarkDoneModal(
            task = showMarkDoneDialog!!,
            onDismiss = { showMarkDoneDialog = null },
            onConfirm = { remark, delayReason ->
                viewModel.markTaskDone(showMarkDoneDialog!!, remark, delayReason)
                showMarkDoneDialog = null
                Toast.makeText(context, "Task completed successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            viewModel = viewModel,
            onDismiss = { showCreateTaskDialog = false }
        )
    }

    if (showEditTaskDialog != null) {
        EditTaskDialog(
            task = showEditTaskDialog!!,
            viewModel = viewModel,
            onDismiss = { showEditTaskDialog = null }
        )
    }
}

@Composable
fun TaskRow(
    task: Task,
    canEditDelete: Boolean,
    handedOverToMe: Boolean,
    handedOverFromMe: Boolean,
    handoverToName: String,
    handoverFromName: String,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = Utils.toDay() > task.schedDate && task.status == "pending"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Handover Ribbons
            if (handedOverFromMe) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarningOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔄 Handover Diya — $handoverToName (Locked)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else if (handedOverToMe) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔄 HANDOVER from $handoverFromName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Priority and Freq badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Priority Badge
                    val pColor = when (task.priority.lowercase()) {
                        "high" -> ErrorRed
                        "medium" -> WarningOrange
                        else -> SuccessGreen
                    }
                    Text(
                        text = "● ${task.priority.uppercase()}",
                        color = pColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(pColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Freq Badge
                    Text(
                        text = task.freq.uppercase(),
                        color = NavyAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(NavyAccent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Status Badge
                val (stText, stColor) = when {
                    task.status == "done" && task.isDelayed -> "⏰ DELAYED" to ErrorRed
                    task.status == "done" -> "✅ ON TIME" to SuccessGreen
                    overdue -> "🚨 OVERDUE" to ErrorRed
                    else -> "⏳ PENDING" to WarningOrange
                }
                Text(
                    text = stText,
                    color = stColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(stColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task Name and notes
            Text(
                text = task.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (task.notes.isNotEmpty()) {
                Text(
                    text = task.notes,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider(color = Color.LightGray.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(8.dp))

            // Meta Info Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dept: ${task.dept} • Time: ${task.time}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Assigned To: ${task.assignedTo.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Sched Date: ${Utils.fDate(task.schedDate)} • By: ${task.createdBy}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // Completion Details / Mark Done
                if (task.status == "done") {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Done By: ${task.doneBy}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = task.doneTime,
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                } else if (!handedOverFromMe) {
                    Button(
                        onClick = onMarkDone,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("mark_done_button")
                    ) {
                        Text("Mark Done", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Edit / Delete actions for main admin
            if (canEditDelete) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TealPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TealPrimary.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clickable { expanded = true }
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = selected,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    maxLines = 1
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop", tint = TealPrimary, modifier = Modifier.size(16.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 11.sp) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkDoneModal(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (remark: String, delayReason: String) -> Unit
) {
    var remark by remember { mutableStateOf("") }
    var delayReason by remember { mutableStateOf("") }

    val todayStr = Utils.toDay()
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    val currentTimeStr = sdf.format(Date())

    // Delay detection: today's date > schedDate OR (today matches schedDate and current time > scheduled task time)
    val isDateDelayed = todayStr > task.schedDate
    val isTimeDelayed = todayStr == task.schedDate && Utils.parseTimeToMinutes(currentTimeStr) > Utils.parseTimeToMinutes(task.time)
    val isDelayed = isDateDelayed || isTimeDelayed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Complete Task",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TealPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = task.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Scheduled: ${Utils.fDate(task.schedDate)} @ ${task.time}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Completion Time: ${Utils.fDateTime()}",
                    fontSize = 11.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )

                if (isDelayed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "⏰ DELAYED COMPILATION DETECTED",
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Task is past its scheduled deadline. A delay justification is required.",
                                fontSize = 10.sp,
                                color = ErrorRed
                            )
                        }
                    }

                    OutlinedTextField(
                        value = delayReason,
                        onValueChange = { delayReason = it },
                        label = { Text("Delay Reason (Mandatory)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ErrorRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delay_reason_input")
                    )
                }

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Done Remark (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remark_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isDelayed && delayReason.trim().isEmpty()) {
                        return@Button
                    }
                    onConfirm(remark, delayReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDelayed) ErrorRed else TealPrimary),
                modifier = Modifier.testTag("confirm_done_button")
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_button")) {
                Text("Cancel")
            }
        }
    )
}

// Dialog placeholders for creation and editing to keep file size controlled
@Composable
fun CreateTaskDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("CARDIOLOGY") }
    var freq by remember { mutableStateOf("daily") }
    var priority by remember { mutableStateOf("medium") }
    var notes by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("09:00") }
    var schedDate by remember { mutableStateOf(Utils.toDay()) }
    var assignedEmployeeName by remember { mutableStateOf("") }

    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val departments by viewModel.departments.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign New Task", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_name")
                )

                // Department
                FilterDropdown(
                    label = "Department",
                    selected = dept,
                    options = departments.map { it.name },
                    onSelected = { dept = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency
                FilterDropdown(
                    label = "Frequency",
                    selected = freq,
                    options = listOf("daily", "15-day", "monthly", "quarterly", "half-yearly", "yearly", "delegation"),
                    onSelected = { freq = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Assignee
                FilterDropdown(
                    label = "Assign To",
                    selected = if (assignedEmployeeName.isEmpty()) "Select Employee" else assignedEmployeeName,
                    options = employees.map { it.name },
                    onSelected = { assignedEmployeeName = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:MM)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = schedDate,
                        onValueChange = { schedDate = it },
                        label = { Text("Sched Date") },
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Priority
                    FilterDropdown(
                        label = "Priority",
                        selected = priority,
                        options = listOf("high", "medium", "low"),
                        onSelected = { priority = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Task Description / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && assignedEmployeeName.isNotEmpty()) {
                        viewModel.addTask(name, dept, freq, listOf(assignedEmployeeName), priority, notes, time, schedDate)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Assign", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: Task,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var notes by remember { mutableStateOf(task.notes) }
    var priority by remember { mutableStateOf(task.priority) }
    var time by remember { mutableStateOf(task.time) }
    var schedDate by remember { mutableStateOf(task.schedDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task Parameters", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = schedDate,
                        onValueChange = { schedDate = it },
                        label = { Text("Sched Date") },
                        modifier = Modifier.weight(1.2f)
                    )
                }
                FilterDropdown(
                    label = "Priority",
                    selected = priority,
                    options = listOf("high", "medium", "low"),
                    onSelected = { priority = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        viewModel.editTask(task.copy(name = name, notes = notes, priority = priority, time = time, schedDate = schedDate))
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
