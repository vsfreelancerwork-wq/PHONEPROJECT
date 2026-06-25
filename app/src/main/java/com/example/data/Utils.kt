package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Utils {

    fun uid(): String {
        return UUID.randomUUID().toString()
    }

    fun toDay(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun fDate(str: String): String {
        if (str.isEmpty()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("dd MMM yy", Locale.US)
            val date = inputFormat.parse(str)
            if (date != null) outputFormat.format(date) else str
        } catch (e: Exception) {
            str
        }
    }

    fun fDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
        return sdf.format(Date())
    }

    fun parseTimeToMinutes(str: String): Int {
        if (str.isEmpty() || !str.contains(":")) return 0
        return try {
            val parts = str.split(":")
            val hours = parts[0].trim().toInt()
            val minutes = parts[1].trim().toInt()
            hours * 60 + minutes
        } catch (e: Exception) {
            0
        }
    }

    fun wasCompletedLate(task: Task): Boolean {
        if (task.isDelayed) return true
        if (task.status != "done") return false
        
        // Late if doneTime was recorded after the schedDate
        // Compare schedDate with current date when marked done
        // Or check if current time > scheduled time on the scheduled day
        return task.isDelayed
    }

    fun isTaskDueToday(task: Task): Boolean {
        val todayStr = toDay()
        if (task.schedDate == todayStr) return true

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        val sched = Calendar.getInstance()

        try {
            val schedDateObj = sdf.parse(task.schedDate) ?: return false
            sched.time = schedDateObj

            val todayDay = today.get(Calendar.DAY_OF_MONTH)
            val todayMonth = today.get(Calendar.MONTH) // 0-11
            val todayYear = today.get(Calendar.YEAR)

            val schedDay = sched.get(Calendar.DAY_OF_MONTH)
            val schedMonth = sched.get(Calendar.MONTH)
            val schedYear = sched.get(Calendar.YEAR)

            return when (task.freq.lowercase(Locale.US)) {
                "daily" -> true
                "delegation" -> true
                "15-day" -> {
                    val diffInMillis = Math.abs(today.timeInMillis - sched.timeInMillis)
                    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                    diffInDays % 15 == 0L
                }
                "monthly" -> {
                    todayDay == schedDay
                }
                "quarterly" -> {
                    val monthDiff = (todayYear - schedYear) * 12 + (todayMonth - schedMonth)
                    todayDay == schedDay && monthDiff % 3 == 0
                }
                "half-yearly" -> {
                    val monthDiff = (todayYear - schedYear) * 12 + (todayMonth - schedMonth)
                    todayDay == schedDay && monthDiff % 6 == 0
                }
                "yearly" -> {
                    todayDay == schedDay && todayMonth == schedMonth
                }
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * For each done task where lastDone !== today AND isTaskDueToday(task) AND no existing pending child task exists 
     * -> create a new pending child task with parentTaskId = original.id, schedDate = today, reset all completion fields.
     */
    fun autoCycleTasks(allTasks: List<Task>): List<Task> {
        val todayStr = toDay()
        val newChildTasks = mutableListOf<Task>()

        // Group tasks to easily check if a child already exists for today
        val parentToChildrenMap = allTasks.groupBy { it.parentTaskId }
        val pendingTasksTodayMap = allTasks
            .filter { it.status == "pending" && it.schedDate == todayStr }
            .groupBy { it.parentTaskId.ifEmpty { it.id } }

        // Find parent tasks (tasks with empty parentTaskId) that are completed
        val parentTasks = allTasks.filter { it.parentTaskId.isEmpty() }

        for (parent in parentTasks) {
            // Must be "done" (or has been completed at least once in history)
            // AND lastDone is not today
            // AND is due today
            val isDone = parent.status == "done" || parent.lastDone.isNotEmpty()
            val notDoneTodayYet = parent.lastDone != todayStr

            if (isDone && notDoneTodayYet && isTaskDueToday(parent)) {
                // Check if we already have a pending child task for today
                val alreadyHasPendingToday = pendingTasksTodayMap.containsKey(parent.id)
                if (!alreadyHasPendingToday) {
                    // Create a new pending child task
                    val child = Task(
                        id = uid(),
                        name = parent.name,
                        dept = parent.dept,
                        freq = parent.freq,
                        assignedTo = parent.assignedTo,
                        assigneeEmails = parent.assigneeEmails,
                        schedDate = todayStr,
                        time = parent.time,
                        priority = parent.priority,
                        notes = parent.notes,
                        status = "pending",
                        doneBy = "",
                        doneTime = "",
                        doneRemark = "",
                        delayReason = "",
                        isDelayed = false,
                        lastDone = "",
                        completionHistory = emptyList(),
                        extensions = emptyList(),
                        created = todayStr,
                        createdBy = parent.createdBy,
                        activityLog = listOf(
                            ActivityLogEntry(
                                by = "System",
                                action = "Auto-Cycle",
                                details = "Created automated cycle child task for today",
                                at = fDateTime()
                            )
                        ),
                        parentTaskId = parent.id
                    )
                    newChildTasks.add(child)
                }
            }
        }

        return newChildTasks
    }

    /**
     * CSV Exporter representing the Excel export.
     * Writes CSV string and triggers Android Share sheet.
     */
    fun exportToExcel(context: Context, dataList: List<Map<String, String>>, filename: String) {
        if (dataList.isEmpty()) {
            Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val headers = dataList.first().keys.toList()
            val csvBuilder = java.lang.StringBuilder()
            
            // Append header
            csvBuilder.append(headers.joinToString(","))
            csvBuilder.append("\n")

            // Append rows
            for (row in dataList) {
                val line = headers.map { key ->
                    val value = row[key] ?: ""
                    // Escape quotes and commas
                    val escapedValue = value.replace("\"", "\"\"")
                    if (escapedValue.contains(",") || escapedValue.contains("\n") || escapedValue.contains("\"")) {
                        "\"$escapedValue\""
                    } else {
                        escapedValue
                    }
                }.joinToString(",")
                csvBuilder.append(line)
                csvBuilder.append("\n")
            }

            val file = File(context.cacheDir, "$filename.csv")
            val writer = FileWriter(file)
            writer.write(csvBuilder.toString())
            writer.flush()
            writer.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Export $filename")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Export File"))
            Toast.makeText(context, "Exported successfully as CSV!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
