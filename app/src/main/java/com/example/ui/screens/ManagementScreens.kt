package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
fun ChecklistsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val departments by viewModel.departments.collectAsState(initial = emptyList())

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.status == "done" }
    val progressPercent = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 100

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Overall Checklist Progress", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 1f,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$progressPercent% Completed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$completedTasks / $totalTasks Tasks", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("Departmental Checklists", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
        }

        val deptGroups = tasks.groupBy { it.dept }
        val displayedDepts = departments.map { it.name }

        items(displayedDepts) { deptName ->
            val deptTasks = deptGroups[deptName] ?: emptyList()
            val dTotal = deptTasks.size
            val dDone = deptTasks.count { it.status == "done" }
            val dPending = dTotal - dDone
            val dProgress = if (dTotal > 0) (dDone.toFloat() / dTotal.toFloat()) else 1f

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
                        Text(deptName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        // Red pending tasks alert badge if pending tasks exist
                        if (dPending > 0) {
                            Text(
                                text = "$dPending PENDING",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .background(ErrorRed, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        } else {
                            Text(
                                text = "COMPLETE",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .background(SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = dProgress,
                        color = TealPrimary,
                        trackColor = Color.LightGray.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${(dProgress * 100).toInt()}% Done", fontSize = 11.sp, color = Color.Gray)
                        Text("$dDone done out of $dTotal total", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val departments by viewModel.departments.collectAsState(initial = emptyList())
    val userState by viewModel.currentUser.collectAsState()
    val user = userState ?: return

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Employee?>(null) }

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
            Text("Employees Directory", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
            if (user.perms.contains("employees_edit") || user.role == "mainadmin") {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_employee_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Text("Add Employee", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (employees.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No employees registered.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(employees) { emp ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = TealPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            Text(emp.role, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                            Text("Dept: ${emp.dept}", fontSize = 10.sp, color = TealPrimary, fontWeight = FontWeight.Medium)
                            Text("Ph: ${emp.contact}", fontSize = 9.sp, color = Color.Gray)
                            Text(emp.email, fontSize = 9.sp, color = Color.Gray, maxLines = 1)

                            Spacer(modifier = Modifier.height(8.dp))

                            if (user.perms.contains("employees_edit") || user.role == "mainadmin") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { showEditDialog = emp }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(onClick = { viewModel.deleteEmployee(emp) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        EmployeeFormDialog(
            departments = departments,
            onDismiss = { showAddDialog = false },
            onSave = { name, dept, role, contact, email, pass, perms ->
                viewModel.addEmployee(name, dept, role, contact, email, pass, perms)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog != null) {
        EmployeeFormDialog(
            employee = showEditDialog,
            departments = departments,
            onDismiss = { showEditDialog = null },
            onSave = { name, dept, role, contact, email, pass, perms ->
                viewModel.editEmployee(showEditDialog!!.copy(name = name, dept = dept, role = role, contact = contact, email = email, password = pass, perms = perms))
                showEditDialog = null
            }
        )
    }
}

@Composable
fun DepartmentsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val departments by viewModel.departments.collectAsState(initial = emptyList())
    val userState by viewModel.currentUser.collectAsState()
    val user = userState ?: return

    var showAddDialog by remember { mutableStateOf(false) }
    var newDeptName by remember { mutableStateOf("") }

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
            Text("Hospital Departments", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
            if (user.perms.contains("departments_edit") || user.role == "mainadmin") {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Text("Add Dept", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (departments.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No departments configured.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(departments) { dept ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dept.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyAccent)
                            if (user.perms.contains("departments_edit") || user.role == "mainadmin") {
                                IconButton(onClick = { viewModel.deleteDepartment(dept) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Department", fontWeight = FontWeight.Bold, color = TealPrimary) },
            text = {
                OutlinedTextField(
                    value = newDeptName,
                    onValueChange = { newDeptName = it },
                    label = { Text("Department Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDeptName.isNotEmpty()) {
                            viewModel.addDepartment(newDeptName)
                            newDeptName = ""
                            showAddDialog = false
                            Toast.makeText(context, "Department added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LiveTrackingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val employees by viewModel.employees.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Real-Time Employee Task Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
            Text("View current active duty statuses and completed counts.", fontSize = 11.sp, color = Color.Gray)
        }

        items(employees) { emp ->
            val empTasks = tasks.filter { emp.name in it.assignedTo }
            val total = empTasks.size
            val done = empTasks.count { it.status == "done" }
            val pending = total - done
            val delayed = empTasks.count { it.status == "pending" && Utils.toDay() > it.schedDate }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = emp.name.take(2).uppercase(),
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${emp.role} • ${emp.dept}", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Done: $done", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            Text("Pending: $pending", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                            if (delayed > 0) {
                                Text("Overdue: $delayed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (pending == 0 && total > 0) SuccessGreen.copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (total > 0) "${(done * 100) / total}% Done" else "Idle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pending == 0 && total > 0) SuccessGreen else TealPrimary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeFormDialog(
    employee: Employee? = null,
    departments: List<Department>,
    onDismiss: () -> Unit,
    onSave: (name: String, dept: String, role: String, contact: String, email: String, pass: String, perms: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var dept by remember { mutableStateOf(employee?.dept ?: "CARDIOLOGY") }
    var role by remember { mutableStateOf(employee?.role ?: "Staff") }
    var contact by remember { mutableStateOf(employee?.contact ?: "") }
    var email by remember { mutableStateOf(employee?.email ?: "") }
    var pass by remember { mutableStateOf(employee?.password ?: "password123") }

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

    val selectedPerms = remember { mutableStateListOf<String>().apply {
        if (employee != null) addAll(employee.perms)
        else addAll(listOf("tasks_view", "issues_add")) // default
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employee == null) "Register New Employee" else "Modify Employee File", fontWeight = FontWeight.Bold, color = TealPrimary) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxHeight(0.65f)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name (UPPERCASE)") },
                        modifier = Modifier.fillMaxWidth().testTag("employee_name_input")
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
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Designation / Role") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Phone Contact") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Official Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Portal Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select Permissions (Grants Access):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                }

                items(allPerms) { perm ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedPerms.contains(perm)) selectedPerms.remove(perm)
                                else selectedPerms.add(perm)
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = selectedPerms.contains(perm),
                            onCheckedChange = {
                                if (it == true) selectedPerms.add(perm)
                                else selectedPerms.remove(perm)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(perm.replace("_", " ").uppercase(), fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && email.isNotEmpty()) {
                        onSave(name, dept, role, contact, email, pass, selectedPerms.toList())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Save Record", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
