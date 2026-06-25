package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY schedDate DESC, time DESC")
    fun getTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getTasksOnce(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()


    // --- Employees ---
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getEmployeesFlow(): Flow<List<Employee>>

    @Query("SELECT * FROM employees")
    suspend fun getEmployeesOnce(): List<Employee>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeById(id: String): Employee?

    @Query("SELECT * FROM employees WHERE email = :email LIMIT 1")
    suspend fun getEmployeeByEmail(email: String): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<Employee>)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployeeById(id: String)


    // --- Issues ---
    @Query("SELECT * FROM issues ORDER BY createdAt DESC")
    fun getIssuesFlow(): Flow<List<Issue>>

    @Query("SELECT * FROM issues")
    suspend fun getIssuesOnce(): List<Issue>

    @Query("SELECT * FROM issues WHERE id = :id")
    suspend fun getIssueById(id: String): Issue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: Issue)

    @Query("DELETE FROM issues WHERE id = :id")
    suspend fun deleteIssueById(id: String)


    // --- Global Activity Log ---
    @Query("SELECT * FROM actlog ORDER BY at DESC")
    fun getActivityLogsFlow(): Flow<List<GlobalActivityLog>>

    @Query("SELECT * FROM actlog")
    suspend fun getActivityLogsOnce(): List<GlobalActivityLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: GlobalActivityLog)

    @Query("DELETE FROM actlog")
    suspend fun clearAllActivityLogs()


    // --- Handovers ---
    @Query("SELECT * FROM handovers ORDER BY createdAt DESC")
    fun getHandoversFlow(): Flow<List<Handover>>

    @Query("SELECT * FROM handovers")
    suspend fun getHandoversOnce(): List<Handover>

    @Query("SELECT * FROM handovers WHERE id = :id")
    suspend fun getHandoverById(id: String): Handover?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHandover(handover: Handover)

    @Query("DELETE FROM handovers WHERE id = :id")
    suspend fun deleteHandoverById(id: String)


    // --- Delegations ---
    @Query("SELECT * FROM delegations ORDER BY createdAt DESC")
    fun getDelegationsFlow(): Flow<List<Delegation>>

    @Query("SELECT * FROM delegations")
    suspend fun getDelegationsOnce(): List<Delegation>

    @Query("SELECT * FROM delegations WHERE id = :id")
    suspend fun getDelegationById(id: String): Delegation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelegation(delegation: Delegation)

    @Query("DELETE FROM delegations WHERE id = :id")
    suspend fun deleteDelegationById(id: String)


    // --- Departments ---
    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getDepartmentsFlow(): Flow<List<Department>>

    @Query("SELECT * FROM departments")
    suspend fun getDepartmentsOnce(): List<Department>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartment(department: Department)

    @Query("DELETE FROM departments WHERE id = :id")
    suspend fun deleteDepartmentById(id: String)


    // --- Personal Links ---
    @Query("SELECT * FROM links WHERE userId = :userId")
    fun getLinksByUserIdFlow(userId: String): Flow<List<PersonalLink>>

    @Query("SELECT * FROM links")
    suspend fun getLinksOnce(): List<PersonalLink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: PersonalLink)

    @Query("DELETE FROM links WHERE id = :id")
    suspend fun deleteLinkById(id: String)


    // --- Trash ---
    @Query("SELECT * FROM trash ORDER BY deletedAt DESC")
    fun getTrashItemsFlow(): Flow<List<TrashItem>>

    @Query("SELECT * FROM trash")
    suspend fun getTrashOnce(): List<TrashItem>

    @Query("SELECT * FROM trash WHERE id = :id")
    suspend fun getTrashItemById(id: String): TrashItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItem)

    @Query("DELETE FROM trash WHERE id = :id")
    suspend fun deleteTrashItemById(id: String)

    @Query("DELETE FROM trash")
    suspend fun clearTrash()
}
