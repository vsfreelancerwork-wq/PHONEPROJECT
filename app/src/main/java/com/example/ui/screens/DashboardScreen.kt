package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userState by viewModel.currentUser.collectAsState()
    val user = userState ?: return

    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val issues by viewModel.issues.collectAsState(initial = emptyList())
    val handovers by viewModel.handovers.collectAsState(initial = emptyList())
    val delegations by viewModel.delegations.collectAsState(initial = emptyList())
    val logs by viewModel.activityLogs.collectAsState(initial = emptyList())

    val isManager = user.role == "mainadmin" || user.role == "admin"

    if (isManager) {
        ManagerDashboard(
            tasks = tasks,
            employees = employees,
            issues = issues,
            handovers = handovers,
            delegations = delegations,
            logs = logs,
            modifier = modifier
        )
    } else {
        StaffDashboard(
            user = user,
            tasks = tasks,
            delegations = delegations,
            modifier = modifier
        )
    }
}

@Composable
fun ManagerDashboard(
    tasks: List<Task>,
    employees: List<Employee>,
    issues: List<Issue>,
    handovers: List<Handover>,
    delegations: List<Delegation>,
    logs: List<GlobalActivityLog>,
    modifier: Modifier = Modifier
) {
    val pendingTasks = tasks.count { it.status == "pending" }
    val doneToday = tasks.count { it.status == "done" && it.lastDone == Utils.toDay() }
    val delayedTasks = tasks.count { it.status == "pending" && Utils.toDay() > it.schedDate }
    val openIssues = issues.count { it.status == "open" }
    val activeHandovers = handovers.count { it.status == "accepted" }
    val totalDelegations = delegations.count { it.status == "pending" || it.status == "extended" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        item {
            Text(
                text = "Operational Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Stats Card Grid
        item {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("Total Tasks", "${tasks.size}", "📋", TealPrimary, Modifier.weight(1f))
                    StatCard("Pending", "$pendingTasks", "⏳", WarningOrange, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("Done Today", "$doneToday", "✅", SuccessGreen, Modifier.weight(1f))
                    StatCard("Delayed Tasks", "$delayedTasks", "🚨", ErrorRed, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("Employees", "${employees.size}", "👥", NavyAccent, Modifier.weight(1f))
                    StatCard("Open Issues", "$openIssues", "⚠️", ErrorRed, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("Handovers", "$activeHandovers", "🔄", TealPrimary, Modifier.weight(1f))
                    StatCard("Delegations", "$totalDelegations", "📤", NavyAccent, Modifier.weight(1f))
                }
            }
        }

        // Charts
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Performance Analytics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            val doneCount = tasks.count { it.status == "done" }.toFloat()
            val pendingCount = tasks.count { it.status == "pending" }.toFloat()
            
            val delayedHistoryCount = tasks.count { it.isDelayed }.toFloat()
            val onTimeHistoryCount = (tasks.count { it.status == "done" } - delayedHistoryCount).coerceAtLeast(0f)

            Row(modifier = Modifier.fillMaxWidth()) {
                DonutChart(
                    title = "Task Completion Status",
                    slices = listOf(doneCount to SuccessGreen, pendingCount to WarningOrange),
                    labels = listOf("Done", "Pending"),
                    modifier = Modifier.weight(1f)
                )
                DonutChart(
                    title = "Historical Delay Rate",
                    slices = listOf(delayedHistoryCount to ErrorRed, onTimeHistoryCount to SuccessGreen),
                    labels = listOf("Delayed", "On-Time"),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bar Chart
        item {
            Spacer(modifier = Modifier.height(12.dp))
            val deptData = tasks.groupBy { it.dept }
                .map { (dept, list) -> dept to list.count { it.status == "done" } }
                .take(6)
            SimpleBarChart(
                title = "Department-Wise Completed Tasks",
                data = deptData
            )
        }

        // Recent Activity Feed
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recent Operations Log",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        val recentLogs = logs.take(5)
        if (recentLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No activities recorded yet.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentLogs) { log ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Log", tint = TealPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.by.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TealPrimary
                                )
                                Text(
                                    text = log.at,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "${log.action}: ${log.details}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffDashboard(
    user: LoggedInUser,
    tasks: List<Task>,
    delegations: List<Delegation>,
    modifier: Modifier = Modifier
) {
    val myTasks = tasks.filter { user.name in it.assignedTo || user.email in it.assigneeEmails }
    val myPending = myTasks.count { it.status == "pending" }
    val doneToday = myTasks.count { it.status == "done" && it.lastDone == Utils.toDay() }
    val delayedTasks = myTasks.count { it.status == "pending" && Utils.toDay() > it.schedDate }
    val myDelegations = delegations.filter { it.doerName == user.name && it.status in listOf("pending", "extended", "extension-requested") }
    val totalCompleted = myTasks.count { it.status == "done" }

    // Calculate performance score
    val totalDoneAllTime = myTasks.count { it.status == "done" }
    val totalLateAllTime = myTasks.count { it.isDelayed }
    val performanceScore = if (totalDoneAllTime > 0) {
        ((totalDoneAllTime - totalLateAllTime).toFloat() / totalDoneAllTime.toFloat() * 100).coerceAtLeast(0f).toInt()
    } else {
        100
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        item {
            Text(
                text = "Welcome back, ${user.name}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "${user.role.uppercase()} • ${user.dept}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Stats Cards
        item {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("My Pending", "$myPending", "⏳", WarningOrange, Modifier.weight(1f))
                    StatCard("Done Today", "$doneToday", "✅", SuccessGreen, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("Delayed Tasks", "$delayedTasks", "🚨", ErrorRed, Modifier.weight(1f))
                    StatCard("My Delegations", "${myDelegations.size}", "📤", NavyAccent, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("Total Completed", "$totalCompleted", "🏆", SuccessGreen, Modifier.fillMaxWidth(0.5f))
                }
            }
        }

        // Donut charts
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DonutChart(
                    title = "Performance Score",
                    slices = listOf(performanceScore.toFloat() to SuccessGreen, (100 - performanceScore).toFloat() to ErrorRed),
                    labels = listOf("Quality", "Late"),
                    modifier = Modifier.weight(1f)
                )
                DonutChart(
                    title = "Today's Task Status",
                    slices = listOf(doneToday.toFloat() to SuccessGreen, myPending.toFloat() to WarningOrange),
                    labels = listOf("Done", "Pending"),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // My Pending Tasks List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Today's Critical Tasks",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        val pendingToday = myTasks.filter { it.status == "pending" && it.schedDate == Utils.toDay() }
        if (pendingToday.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Great work! No pending tasks due today.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(pendingToday) { task ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (task.priority.lowercase() == "high") ErrorRed.copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = "Task",
                                tint = if (task.priority.lowercase() == "high") ErrorRed else TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Due at: ${task.time} • Priority: ${task.priority.uppercase()}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Active Delegations
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Delegations Track",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (myDelegations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No active delegations assigned to you.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(myDelegations) { del ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NavyAccent.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Delegation", tint = NavyAccent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = del.task,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Due: ${Utils.fDate(del.dueDate)} • Status: ${del.status.uppercase()}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
