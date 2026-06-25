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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun IssuesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val issues by viewModel.issues.collectAsState(initial = emptyList())
    val departments by viewModel.departments.collectAsState(initial = emptyList())
    val userState by viewModel.currentUser.collectAsState()
    val user = userState ?: return

    var showAddDialog by remember { mutableStateOf(false) }
    var showResolveDialog by remember { mutableStateOf<Issue?>(null) }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Operation Issues & Incidents", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
            if (user.perms.contains("issues_add") || user.role == "mainadmin") {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Report")
                    Text("Report Problem", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (issues.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No operational issues reported. Everything runs smoothly!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(issues) { issue ->
                    IssueItemRow(
                        issue = issue,
                        currentUser = user,
                        onResolve = { showResolveDialog = issue },
                        onDelete = { viewModel.deleteIssue(issue) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddIssueDialog(
            departments = departments,
            onDismiss = { showAddDialog = false },
            onSave = { title, dept, priority, details ->
                viewModel.addIssue(title, dept, priority, details)
                showAddDialog = false
                Toast.makeText(context, "Problem reported!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showResolveDialog != null) {
        ResolveIssueDialog(
            issue = showResolveDialog!!,
            onDismiss = { showResolveDialog = null },
            onResolve = { resolution ->
                viewModel.resolveIssue(showResolveDialog!!.id, resolution)
                showResolveDialog = null
                Toast.makeText(context, "Issue resolved successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EscalationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val issues by viewModel.issues.collectAsState(initial = emptyList())
    val userState by viewModel.currentUser.collectAsState()
    val user = userState ?: return

    var showResolveDialog by remember { mutableStateOf<Issue?>(null) }
    val context = LocalContext.current

    val highPriorityOpenIssues = remember(issues) {
        issues.filter { it.priority.lowercase() == "high" && it.status == "open" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Escalation", tint = ErrorRed, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CRITICAL ESCALATIONS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
        }
        Text("Displaying urgent high-priority unresolved incidents.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))

        Spacer(modifier = Modifier.height(12.dp))

        if (highPriorityOpenIssues.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Safe", tint = SuccessGreen, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Zero active escalations. Excellent!", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(highPriorityOpenIssues) { issue ->
                    IssueItemRow(
                        issue = issue,
                        currentUser = user,
                        onResolve = { showResolveDialog = issue },
                        onDelete = { viewModel.deleteIssue(issue) }
                    )
                }
            }
        }
    }

    if (showResolveDialog != null) {
        ResolveIssueDialog(
            issue = showResolveDialog!!,
            onDismiss = { showResolveDialog = null },
            onResolve = { resolution ->
                viewModel.resolveIssue(showResolveDialog!!.id, resolution)
                showResolveDialog = null
                Toast.makeText(context, "Escalation Resolved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun IssueItemRow(
    issue: Issue,
    currentUser: LoggedInUser,
    onResolve: () -> Unit,
    onDelete: () -> Unit
) {
    val isResolved = issue.status == "resolved"

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
                    val pColor = if (issue.priority.lowercase() == "high") ErrorRed else if (issue.priority.lowercase() == "medium") WarningOrange else SuccessGreen
                    Text(
                        text = "${issue.priority.uppercase()} PRIORITY",
                        color = pColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(pColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DEPT: ${issue.dept}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                // Status Badge
                Text(
                    text = issue.status.uppercase(),
                    color = if (isResolved) SuccessGreen else ErrorRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background((if (isResolved) SuccessGreen else ErrorRed).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(issue.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (issue.details.isNotEmpty()) {
                Text(issue.details, fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reported By: ${issue.reporter}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    if (isResolved) {
                        Text("Resolved By: ${issue.resolvedBy} • ${issue.resolvedAt}", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        Text("Resolution: ${issue.resolution}", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                if (!isResolved && (currentUser.perms.contains("issues_resolve") || currentUser.role == "mainadmin")) {
                    Button(
                        onClick = onResolve,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Resolve", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            if (currentUser.role == "mainadmin") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.activityLogs.collectAsState(initial = emptyList())
    var keyword by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredLogs = remember(logs, keyword) {
        if (keyword.isEmpty()) logs
        else logs.filter { it.by.contains(keyword, ignoreCase = true) || it.action.contains(keyword, ignoreCase = true) || it.details.contains(keyword, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Text("Global Operations Activity Log", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("Filter logs...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = {
                val csv = filteredLogs.map { mapOf("By" to it.by, "Action" to it.action, "Details" to it.details, "At" to it.at) }
                Utils.exportToExcel(context, csv, "ActivityLog")
            }) {
                Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = TealPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = { viewModel.clearAllLogs() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = ErrorRed)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No activities match filters.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredLogs) { log ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.by.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                                Text(log.at, fontSize = 9.sp, color = Color.Gray)
                            }
                            Text("${log.action}: ${log.details}", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrashScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.trashItems.collectAsState(initial = emptyList())
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Trash Bin (Soft Deleted)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
            Button(
                onClick = { viewModel.emptyTrash() },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Empty")
                Text("Empty Trash", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Trash bin is empty.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { tr ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tr.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Type: ${tr.type.uppercase()} • Deleted: ${tr.deletedAt}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = { viewModel.restoreTrashItem(tr) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Restore", tint = SuccessGreen)
                                }
                                IconButton(onClick = { viewModel.deleteTrashPermanently(tr) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MISReportingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val employees by viewModel.employees.collectAsState(initial = emptyList())

    val total = tasks.size
    val done = tasks.count { it.status == "done" }
    val delayed = tasks.count { it.isDelayed }

    val completionRate = if (total > 0) (done * 100) / total else 100
    val delayRate = if (done > 0) (delayed * 100) / done else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("MIS Operational Performance Report", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
            Text("Aggregated compliance statistics and employee tables.", fontSize = 11.sp, color = Color.Gray)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard("Completion Rate", "$completionRate%", "📊", TealPrimary, Modifier.weight(1f))
                StatCard("Task Delay Rate", "$delayRate%", "⏰", ErrorRed, Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Employee Performance Board", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyAccent)
                    Spacer(modifier = Modifier.height(8.dp))

                    employees.forEach { emp ->
                        val empTasks = tasks.filter { emp.name in it.assignedTo }
                        val empDone = empTasks.count { it.status == "done" }
                        val empLate = empTasks.count { it.isDelayed }
                        val score = if (empDone > 0) (((empDone - empLate).toFloat() / empDone.toFloat()) * 100).toInt().coerceAtLeast(0) else 100

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Dept: ${emp.dept} • Done: $empDone • Late: $empLate", fontSize = 9.sp, color = Color.Gray)
                            }
                            Text(
                                text = "$score Score",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (score >= 80) SuccessGreen else if (score >= 50) WarningOrange else ErrorRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkBoxScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val links by viewModel.personalLinks.collectAsState()
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Text("My Personal Links Box", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
        Text("Save web references, guidelines, and manuals.", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(10.dp))

        // Save reference form
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Bookmark", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    )
                }
                Button(
                    onClick = {
                        if (title.isNotEmpty() && url.isNotEmpty()) {
                            viewModel.addLink(title, url)
                            title = ""
                            url = ""
                            Toast.makeText(context, "Link bookmarked!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End).height(32.dp)
                ) {
                    Text("Bookmark", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (links.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No references saved yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(links) { link ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(link.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(link.url, fontSize = 10.sp, color = TealPrimary, maxLines = 1)
                            }
                            IconButton(onClick = { viewModel.deleteLink(link.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline Sub-dialogs
@Composable
fun AddIssueDialog(
    departments: List<Department>,
    onDismiss: () -> Unit,
    onSave: (title: String, dept: String, priority: String, details: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("CARDIOLOGY") }
    var priority by remember { mutableStateOf("medium") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Problem/Incident", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Incident Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                FilterDropdown(
                    label = "Department",
                    selected = dept,
                    options = departments.map { it.name },
                    onSelected = { dept = it },
                    modifier = Modifier.fillMaxWidth()
                )
                FilterDropdown(
                    label = "Priority",
                    selected = priority,
                    options = listOf("high", "medium", "low"),
                    onSelected = { priority = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Provide details...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        onSave(title, dept, priority, details)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Report", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ResolveIssueDialog(
    issue: Issue,
    onDismiss: () -> Unit,
    onResolve: (resolution: String) -> Unit
) {
    var resolution by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resolve Incident Incident", fontWeight = FontWeight.Bold, color = SuccessGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Incident: ${issue.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Reporter: ${issue.reporter}", fontSize = 11.sp, color = Color.Gray)
                OutlinedTextField(
                    value = resolution,
                    onValueChange = { resolution = it },
                    label = { Text("Provide resolution summary...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (resolution.isNotEmpty()) {
                        onResolve(resolution)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Text("Confirm Resolution", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
