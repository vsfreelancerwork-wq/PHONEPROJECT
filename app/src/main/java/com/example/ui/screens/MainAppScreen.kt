package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.InactivityState
import com.example.ui.MainViewModel
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
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
    val departments by viewModel.departments.collectAsState(initial = emptyList())

    // Inactivity warning states
    val inactivityState by viewModel.inactivityState.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()

    // Screen State
    var currentScreenIndex by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = Tasks, 2 = Issues, 3 = Handovers, 4 = More Menu

    // Secondary screen select
    var secondaryScreen by remember { mutableStateOf<String?>(null) } // checklists, employees, departments, tracking, logs, reports, trash, links, settings

    val isManager = user.role == "mainadmin" || user.role == "admin"

    // Real-time Badge Calculations
    val pendingTasksCount = tasks.count { it.status == "pending" }
    val myPendingTodayCount = tasks.count { 
        (user.name in it.assignedTo || user.email in it.assigneeEmails) && 
        it.status == "pending" && 
        it.schedDate == Utils.toDay() 
    }
    val unresolvedIssuesCount = issues.count { it.status == "open" }
    val escalationCount = issues.count { it.status == "open" && it.priority.lowercase() == "high" }
    val totalChecklistsCount = tasks.size
    val totalEmployeesCount = employees.size
    val totalDepartmentsCount = departments.size
    val activeHandoversCount = handovers.count { it.status == "accepted" }
    val activeDelegationCount = delegations.count { it.status in listOf("pending", "accepted", "extended") }

    // Reset inactivity timer on any click
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = true, onClick = { viewModel.resetInactivityTimer() })
    ) {
        Scaffold(
            topBar = {
                val headerTitle = if (secondaryScreen != null) {
                    secondaryScreen!!.replace("_", " ").uppercase()
                } else {
                    when (currentScreenIndex) {
                        0 -> "DASHBOARD"
                        1 -> "TASKS REGISTER"
                        2 -> "INCIDENTS LOG"
                        3 -> "COOPERATION"
                        else -> "PORTAL MENU"
                    }
                }
                AppHeader(
                    title = headerTitle,
                    userName = user.name,
                    userRole = user.role,
                    onLogoutClick = { viewModel.logout() }
                )
            },
            bottomBar = {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealPrimary,
                    selectedTextColor = TealPrimary,
                    unselectedIconColor = Color.Gray.copy(alpha = 0.8f),
                    unselectedTextColor = Color.Gray.copy(alpha = 0.8f),
                    indicatorColor = TealPrimary.copy(alpha = 0.1f)
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // 1. Dashboard
                    NavigationBarItem(
                        selected = currentScreenIndex == 0 && secondaryScreen == null,
                        onClick = {
                            currentScreenIndex = 0
                            secondaryScreen = null
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    // 2. Tasks (Pending badge)
                    NavigationBarItem(
                        selected = currentScreenIndex == 1 && secondaryScreen == null,
                        onClick = {
                            currentScreenIndex = 1
                            secondaryScreen = null
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (myPendingTodayCount > 0) {
                                        Badge(containerColor = Color.Red) {
                                            Text("$myPendingTodayCount", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = "My Tasks")
                            }
                        },
                        label = { Text("My Tasks", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tasks")
                    )

                    // 3. Issues (Unresolved badge)
                    NavigationBarItem(
                        selected = currentScreenIndex == 2 && secondaryScreen == null,
                        onClick = {
                            currentScreenIndex = 2
                            secondaryScreen = null
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unresolvedIssuesCount > 0) {
                                        Badge(containerColor = Color.Red) {
                                            Text("$unresolvedIssuesCount", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Issues")
                            }
                        },
                        label = { Text("Issues", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_issues")
                    )

                    // 4. Handovers/Delegations
                    NavigationBarItem(
                        selected = currentScreenIndex == 3 && secondaryScreen == null,
                        onClick = {
                            currentScreenIndex = 3
                            secondaryScreen = null
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    val activeCollabs = activeHandoversCount + activeDelegationCount
                                    if (activeCollabs > 0) {
                                        Badge(containerColor = TealPrimary) {
                                            Text("$activeCollabs", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Collaborate")
                            }
                        },
                        label = { Text("Duty Sync", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_handovers")
                    )

                    // 5. More Menu
                    NavigationBarItem(
                        selected = currentScreenIndex == 4 || secondaryScreen != null,
                        onClick = {
                            currentScreenIndex = 4
                            secondaryScreen = null
                        },
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                        label = { Text("Menu", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_menu")
                    )
                }
            }
        ) { paddingValues ->
            val innerModifier = Modifier.padding(paddingValues)

            if (secondaryScreen != null) {
                // Secondary Screen routers
                when (secondaryScreen) {
                    "checklists" -> ChecklistsScreen(viewModel, innerModifier)
                    "employees" -> EmployeesScreen(viewModel, innerModifier)
                    "departments" -> DepartmentsScreen(viewModel, innerModifier)
                    "tracking" -> LiveTrackingScreen(viewModel, innerModifier)
                    "logs" -> ActivityLogScreen(viewModel, innerModifier)
                    "reports" -> MISReportingScreen(viewModel, innerModifier)
                    "trash" -> TrashScreen(viewModel, innerModifier)
                    "links" -> LinkBoxScreen(viewModel, innerModifier)
                    "settings" -> SettingsScreen(viewModel, innerModifier)
                    "escalation" -> EscalationsScreen(viewModel, innerModifier)
                }
            } else {
                // Primary Bottom Bar router
                when (currentScreenIndex) {
                    0 -> DashboardScreen(viewModel, innerModifier)
                    1 -> TasksScreen(viewModel, innerModifier)
                    2 -> IssuesScreen(viewModel, innerModifier)
                    3 -> HandoversDelegationsScreen(viewModel, innerModifier)
                    4 -> PortalMenuGrid(
                        isManager = isManager,
                        userPerms = user.perms,
                        pendingTasks = pendingTasksCount,
                        unresolvedIssues = unresolvedIssuesCount,
                        highOpenIssues = escalationCount,
                        totalChecklists = totalChecklistsCount,
                        totalEmployees = totalEmployeesCount,
                        totalDepartments = totalDepartmentsCount,
                        activeHandovers = activeHandoversCount,
                        activeDelegations = activeDelegationCount,
                        onSelect = { secondaryScreen = it },
                        modifier = innerModifier
                    )
                }
            }
        }

        // Inactivity警告 Modal Dialog
        if (inactivityState == InactivityState.Warning) {
            AlertDialog(
                onDismissRequest = { /* Force response */ },
                title = { Text("Security Warning", color = Color.Red, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "You have been inactive. For your security, you will be logged out in $secondsRemaining seconds due to HIPAA/compliance rules.",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetInactivityTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Stay Logged In", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun PortalMenuGrid(
    isManager: Boolean,
    userPerms: List<String>,
    pendingTasks: Int,
    unresolvedIssues: Int,
    highOpenIssues: Int,
    totalChecklists: Int,
    totalEmployees: Int,
    totalDepartments: Int,
    activeHandovers: Int,
    activeDelegations: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemsList = remember(isManager, userPerms) {
        val list = mutableListOf<Triple<String, String, @Composable () -> Unit>>()

        // Settings and Links - Available to everyone
        list.add(Triple("links", "Personal Bookmark Box", { BadgeWithLabel("🔖", 0) }))
        list.add(Triple("settings", "App Configuration", { BadgeWithLabel("⚙️", 0) }))

        // Dynamic Perm check
        if (userPerms.contains("checklist_view") || isManager) {
            list.add(Triple("checklists", "Dept Checklists", { BadgeWithLabel("📋", totalChecklists, containerColor = Color.Red) }))
        }
        if (userPerms.contains("tracking_view") || isManager) {
            list.add(Triple("tracking", "Live Status Sync", { BadgeWithLabel("📡", 0) }))
        }
        if (userPerms.contains("escalation_view") || isManager) {
            list.add(Triple("escalation", "Urgent Escalations", { BadgeWithLabel("🚨", highOpenIssues, containerColor = Color.Red) }))
        }
        if (userPerms.contains("employees_view") || isManager) {
            list.add(Triple("employees", "Personnel Files", { BadgeWithLabel("👥", totalEmployees) }))
        }
        if (userPerms.contains("departments_view") || isManager) {
            list.add(Triple("departments", "Ward Management", { BadgeWithLabel("🏥", totalDepartments) }))
        }
        if (userPerms.contains("mis_view") || isManager) {
            list.add(Triple("reports", "MIS Stats Analytics", { BadgeWithLabel("📈", 0) }))
        }
        if (isManager) {
            list.add(Triple("logs", "Security Log Viewer", { BadgeWithLabel("📜", 0) }))
        }
        if (userPerms.contains("trash_view") || isManager) {
            list.add(Triple("trash", "Recycle Bin", { BadgeWithLabel("🗑️", 0) }))
        }

        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text("Portal Applications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.padding(bottom = 12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(itemsList.size) { idx ->
                val (id, label, iconBadge) = itemsList[idx]
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clickable { onSelect(id) }
                        .testTag("menu_item_$id")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        iconBadge()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeWithLabel(
    icon: String,
    badgeCount: Int,
    containerColor: Color = TealPrimary
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = containerColor) {
                    Text("$badgeCount", color = Color.White, fontSize = 9.sp)
                }
            }
        ) {
            Text(icon, fontSize = 24.sp)
        }
    } else {
        Text(icon, fontSize = 24.sp)
    }
}
