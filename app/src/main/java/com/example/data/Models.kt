package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class ActivityLogEntry(
    val by: String = "",
    val action: String = "",
    val details: String = "",
    val at: String = ""
)

data class ExtensionRequest(
    val requestedAt: String = "",
    val reason: String = "",
    val newDate: String = "",
    val status: String = "pending" // pending, approved, rejected
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val name: String,
    val dept: String,
    val freq: String, // daily | 15-day | monthly | quarterly | half-yearly | yearly | delegation
    val assignedTo: List<String>,
    val assigneeEmails: List<String>,
    val schedDate: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val priority: String, // high | medium | low
    val notes: String = "",
    val status: String, // pending | done
    val doneBy: String = "",
    val doneTime: String = "",
    val doneRemark: String = "",
    val delayReason: String = "",
    val isDelayed: Boolean = false,
    val lastDone: String = "",
    val completionHistory: List<String> = emptyList(),
    val extensions: List<String> = emptyList(),
    val created: String, // YYYY-MM-DD
    val createdBy: String,
    val activityLog: List<ActivityLogEntry> = emptyList(),
    val parentTaskId: String = ""
)

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey val id: String,
    val name: String, // UPPERCASE NAME
    val dept: String,
    val role: String,
    val contact: String,
    val email: String,
    val password: String, // plain text
    val perms: List<String>
)

@Entity(tableName = "issues")
data class Issue(
    @PrimaryKey val id: String,
    val title: String,
    val dept: String,
    val priority: String, // high | medium | low
    val reporter: String,
    val status: String, // open | resolved
    val details: String = "",
    val resolution: String = "",
    val resolvedAt: String = "",
    val resolvedBy: String = "",
    val createdAt: String = ""
)

@Entity(tableName = "actlog")
data class GlobalActivityLog(
    @PrimaryKey val id: String,
    val by: String,
    val action: String,
    val details: String,
    val at: String
)

@Entity(tableName = "handovers")
data class Handover(
    @PrimaryKey val id: String,
    val fromName: String,
    val toName: String,
    val dept: String,
    val dateStart: String, // YYYY-MM-DD
    val dateEnd: String, // YYYY-MM-DD
    val reason: String,
    val taskIds: List<String>,
    val status: String, // pending | accepted | rejected | cancelled
    val createdAt: String,
    val remark: String = ""
)

@Entity(tableName = "delegations")
data class Delegation(
    @PrimaryKey val id: String,
    val task: String, // TASK NAME
    val doerName: String,
    val dept: String,
    val dueDate: String, // YYYY-MM-DD
    val remarks: String,
    val status: String, // pending | accepted | done | extension-requested | extended | rejected
    val createdBy: String,
    val createdAt: String,
    val extensionRequests: List<ExtensionRequest> = emptyList()
)

@Entity(tableName = "departments")
data class Department(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "links")
data class PersonalLink(
    @PrimaryKey val id: String,
    val userId: String, // employee email or "VIBHAV"
    val title: String,
    val url: String
)

@Entity(tableName = "trash")
data class TrashItem(
    @PrimaryKey val id: String,
    val originalId: String,
    val type: String, // task | issue | employee
    val name: String, // descriptive title
    val jsonData: String, // serialized json object
    val deletedAt: String // YYYY-MM-DD HH:MM:SS
)
