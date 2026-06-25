package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.SwapHoriz
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

@Composable
fun HandoversDelegationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userState by viewModel.currentUser.collectAsState()
    val user = userState ?: return

    val handovers by viewModel.handovers.collectAsState(initial = emptyList())
    val delegations by viewModel.delegations.collectAsState(initial = emptyList())
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())

    val context = LocalContext.current

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Handovers, 1 = Delegations

    // Dialog/Form triggers
    var showCreateHandoverDialog by remember { mutableStateOf(false) }
    var showCreateDelegationDialog by remember { mutableStateOf(false) }
    var showAcceptRejectDialog by remember { mutableStateOf<Handover?>(null) }
    var showExtensionRequestDialog by remember { mutableStateOf<Delegation?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab row
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TealPrimary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Handovers Register", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("handovers_tab")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Delegation Tracker", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("delegations_tab")
            )
        }

        if (activeSubTab == 0) {
            // HANDOVER REGISTER
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shift Handovers",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        if (user.perms.contains("handover_edit") || user.role == "mainadmin") {
                            Button(
                                onClick = { showCreateHandoverDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("create_handover_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Handover", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (handovers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No Handovers recorded.", fontSize = 14.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(handovers, key = { it.id }) { ho ->
                                HandoverItemRow(
                                    ho = ho,
                                    currentUser = user,
                                    onAcceptReject = { showAcceptRejectDialog = ho }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // DELEGATION TRACKER
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Task Delegations",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        if (user.perms.contains("delegation_add") || user.role == "mainadmin") {
                            Button(
                                onClick = { showCreateDelegationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("create_delegation_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delegate Task", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (delegations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No Delegations active.", fontSize = 14.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(delegations, key = { it.id }) { del ->
                                DelegationItemRow(
                                    del = del,
                                    currentUser = user,
                                    onDone = { viewModel.updateDelegationStatus(del.id, "done") },
                                    onAccept = { viewModel.updateDelegationStatus(del.id, "accepted") },
                                    onRequestExtension = { showExtensionRequestDialog = del },
                                    onApproveExtension = { index, approve ->
                                        viewModel.approveDelegationExtension(del.id, index, approve)
                                        Toast.makeText(context, if (approve) "Extension Approved!" else "Extension Rejected!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogues
    if (showCreateHandoverDialog) {
        CreateHandoverDialog(
            viewModel = viewModel,
            employees = employees,
            tasks = tasks,
            onDismiss = { showCreateHandoverDialog = false }
        )
    }

    if (showCreateDelegationDialog) {
        CreateDelegationDialog(
            viewModel = viewModel,
            employees = employees,
            onDismiss = { showCreateDelegationDialog = false }
        )
    }

    if (showAcceptRejectDialog != null) {
        AcceptRejectHandoverDialog(
            ho = showAcceptRejectDialog!!,
            viewModel = viewModel,
            onDismiss = { showAcceptRejectDialog = null }
        )
    }

    if (showExtensionRequestDialog != null) {
        RequestExtensionDialog(
            del = showExtensionRequestDialog!!,
            viewModel = viewModel,
            onDismiss = { showExtensionRequestDialog = null }
        )
    }
}

@Composable
fun HandoverItemRow(
    ho: Handover,
    currentUser: LoggedInUser,
    onAcceptReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Handover", tint = TealPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPT: ${ho.dept}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NavyAccent
                    )
                }

                // Status Badge
                val sColor = when (ho.status) {
                    "accepted" -> SuccessGreen
                    "rejected" -> ErrorRed
                    "cancelled" -> Color.Gray
                    else -> WarningOrange
                }
                Text(
                    text = ho.status.uppercase(),
                    color = sColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(sColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "From: ${ho.fromName} ➔ To: ${ho.toName}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "In-Dates: ${Utils.fDate(ho.dateStart)} to ${Utils.fDate(ho.dateEnd)}",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (ho.reason.isNotEmpty()) {
                Text(
                    text = "Reason: ${ho.reason}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (ho.remark.isNotEmpty()) {
                Text(
                    text = "Recipient Remark: ${ho.remark}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TealPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Handovers Contains: ${ho.taskIds.size} task(s)",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Flow accept/reject buttons
            if (ho.status == "pending" && ho.toName == currentUser.name) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAcceptReject,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("action_handover_button")
                ) {
                    Text("Process Handover (Accept / Reject)", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DelegationItemRow(
    del: Delegation,
    currentUser: LoggedInUser,
    onDone: () -> Unit,
    onAccept: () -> Unit,
    onRequestExtension: () -> Unit,
    onApproveExtension: (index: Int, approve: Boolean) -> Unit
) {
    val isCreator = del.createdBy == currentUser.name || currentUser.role == "mainadmin"
    val isDoer = del.doerName == currentUser.name

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assignment, contentDescription = "Delegation", tint = TealPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DELEGATION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NavyAccent,
                        modifier = Modifier
                            .background(NavyAccent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val badgeColor = when (del.status) {
                    "done" -> SuccessGreen
                    "rejected" -> ErrorRed
                    "extended", "accepted" -> TealPrimary
                    "extension-requested" -> WarningOrange
                    else -> Color.Gray
                }
                Text(
                    text = del.status.uppercase(),
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = del.task,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Doer: ${del.doerName} • Dept: ${del.dept}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Text(
                text = "Due Date: ${Utils.fDate(del.dueDate)} • Creator: ${del.createdBy}",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (del.remarks.isNotEmpty()) {
                Text(
                    text = "Remarks: ${del.remarks}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Extension Requests Log
            if (del.extensionRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Extension Log (Max 3):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                del.extensionRequests.forEachIndexed { index, req ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Text("Requested On: ${req.requestedAt} • New Date: ${Utils.fDate(req.newDate)}", fontSize = 10.sp)
                        Text("Reason: ${req.reason}", fontSize = 10.sp, color = Color.Gray)
                        Text("Status: ${req.status.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (req.status == "approved") SuccessGreen else if (req.status == "rejected") ErrorRed else WarningOrange)
                        
                        // Approval buttons (if I am creator and request is pending)
                        if (req.status == "pending" && isCreator && !isDoer) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApproveExtension(index, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Approve", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { onApproveExtension(index, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Reject", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Doer Interaction flows
            if (isDoer && del.status != "done") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (del.status == "pending") {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("Accept", fontSize = 11.sp, color = Color.White)
                        }
                    } else if (del.status == "accepted" || del.status == "extended" || del.status == "extension-requested") {
                        Button(
                            onClick = onDone,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1.2f).height(32.dp)
                        ) {
                            Text("Complete Task", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Request Extension (If less than 3)
                    if (del.extensionRequests.size < 3 && del.status != "pending") {
                        OutlinedButton(
                            onClick = onRequestExtension,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                            modifier = Modifier.weight(1.5f).height(32.dp)
                        ) {
                            Text("Request Extension", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateHandoverDialog(
    viewModel: MainViewModel,
    employees: List<Employee>,
    tasks: List<Task>,
    onDismiss: () -> Unit
) {
    var toEmployeeName by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("CARDIOLOGY") }
    var dateStart by remember { mutableStateOf(Utils.toDay()) }
    var dateEnd by remember { mutableStateOf(Utils.toDay()) }
    var reason by remember { mutableStateOf("") }
    val selectedTaskIds = remember { mutableStateListOf<String>() }

    val departments by viewModel.departments.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Initiate Duty Handover", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxHeight(0.6f)) {
                item {
                    FilterDropdown(
                        label = "Recipient Employee",
                        selected = if (toEmployeeName.isEmpty()) "Select Recipient" else toEmployeeName,
                        options = employees.map { it.name },
                        onSelected = { toEmployeeName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    FilterDropdown(
                        label = "Department",
                        selected = dept,
                        options = departments.map { it.name },
                        onSelected = { dept = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = dateStart,
                        onValueChange = { dateStart = it },
                        label = { Text("Start Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = dateEnd,
                        onValueChange = { dateEnd = it },
                        label = { Text("End Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Handover Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select Tasks to Handover:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealPrimary)
                }

                val pendingDeptTasks = tasks.filter { it.dept == dept && it.status == "pending" }
                if (pendingDeptTasks.isEmpty()) {
                    item {
                        Text("No pending tasks in this department.", fontSize = 11.sp, color = Color.Gray)
                    }
                } else {
                    items(pendingDeptTasks) { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedTaskIds.contains(t.id)) selectedTaskIds.remove(t.id)
                                    else selectedTaskIds.add(t.id)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedTaskIds.contains(t.id),
                                onCheckedChange = {
                                    if (it == true) selectedTaskIds.add(t.id)
                                    else selectedTaskIds.remove(t.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t.name, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (toEmployeeName.isNotEmpty() && selectedTaskIds.isNotEmpty()) {
                        viewModel.addHandover(toEmployeeName, dept, dateStart, dateEnd, reason, selectedTaskIds.toList())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Send Handover", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateDelegationDialog(
    viewModel: MainViewModel,
    employees: List<Employee>,
    onDismiss: () -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var doerName by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("CARDIOLOGY") }
    var dueDate by remember { mutableStateOf(Utils.toDay()) }
    var remarks by remember { mutableStateOf("") }

    val departments by viewModel.departments.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delegate Assignment", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_delegation_task_name")
                )

                FilterDropdown(
                    label = "Doer Employee",
                    selected = if (doerName.isEmpty()) "Select Assignee" else doerName,
                    options = employees.map { it.name },
                    onSelected = { doerName = it },
                    modifier = Modifier.fillMaxWidth()
                )

                FilterDropdown(
                    label = "Department",
                    selected = dept,
                    options = departments.map { it.name },
                    onSelected = { dept = it },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Guidelines") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isNotEmpty() && doerName.isNotEmpty()) {
                        viewModel.addDelegation(taskName, doerName, dept, dueDate, remarks)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Delegate", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AcceptRejectHandoverDialog(
    ho: Handover,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var remark by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Process Shift Handover", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confirm if you accept duty responsibilities for ${ho.taskIds.size} tasks handed over by ${ho.fromName}.", fontSize = 13.sp)
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Acknowledge Remark") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.updateHandoverStatus(ho.id, "accepted", remark)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Accept", color = Color.White)
                }
                Button(
                    onClick = {
                        viewModel.updateHandoverStatus(ho.id, "rejected", remark)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Reject", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RequestExtensionDialog(
    del: Delegation,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf(Utils.toDay()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Task Extension", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You can request up to 3 extensions. Task creator will approve or reject.", fontSize = 12.sp)
                OutlinedTextField(
                    value = newDate,
                    onValueChange = { newDate = it },
                    label = { Text("Proposed New Due Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Extension Justification") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isNotEmpty()) {
                        viewModel.requestDelegationExtension(del.id, reason, newDate)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Submit Request", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
