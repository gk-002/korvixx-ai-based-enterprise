package com.example.data

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class RbacRepository(private val rbacDao: RbacDao) {

    val allUsers: Flow<List<UserEntity>> = rbacDao.getAllUsers()

    // Manually combine Flows to create UserWithRole safely
    fun getUserWithRole(email: String): Flow<UserWithRole?> {
        return combine(rbacDao.getUserByEmail(email), rbacDao.getAllRoles()) { user, roles ->
            if (user == null) null else {
                val role = roles.firstOrNull { it.roleId == user.roleId } ?: RoleEntity(user.roleId, "Unknown Role")
                UserWithRole(user, role)
            }
        }
    }

    // Manually and robustly combine Flows for all roles with their assigned permissions
    val allRolesWithPermissions: Flow<List<RoleWithPermissions>> = combine(
        rbacDao.getAllRoles(),
        rbacDao.getAllRolePermissionCrossRefs(),
        rbacDao.getAllPermissions()
    ) { roles, crossRefs, permissions ->
        val permissionMap = permissions.associateBy { it.permissionId }
        val crossRefsByRole = crossRefs.groupBy { it.roleId }
        roles.map { role ->
            val permIds = crossRefsByRole[role.roleId]?.map { it.permissionId } ?: emptyList()
            val rolePerms = permIds.mapNotNull { permissionMap[it] }
            RoleWithPermissions(role, rolePerms)
        }
    }

    fun getRoleWithPermissions(roleId: String): Flow<RoleWithPermissions?> {
        return allRolesWithPermissions.map { list ->
            list.firstOrNull { it.role.roleId == roleId }
        }
    }

    fun getUserPermissions(email: String): Flow<List<String>> {
        return rbacDao.getUserPermissionsFlow(email)
    }

    suspend fun hasPermission(email: String, permissionId: String): Boolean {
        return rbacDao.hasPermission(email, permissionId)
    }

    val allAuditLogs: Flow<List<AuditLogEntity>> = rbacDao.getAllAuditLogs()

    suspend fun insertAuditLog(log: AuditLogEntity) = rbacDao.insertAuditLog(log)

    val allSystemActivities: Flow<List<SystemActivityEntity>> = rbacDao.getAllSystemActivities()

    suspend fun insertSystemActivity(activity: SystemActivityEntity) = rbacDao.insertSystemActivity(activity)

    suspend fun insertUser(user: UserEntity) = rbacDao.insertUser(user)
    suspend fun insertRole(role: RoleEntity) = rbacDao.insertRole(role)
    suspend fun insertPermission(permission: PermissionEntity) = rbacDao.insertPermission(permission)
    suspend fun insertRolePermissionCrossRef(crossRef: RolePermissionCrossRef) = rbacDao.insertRolePermissionCrossRef(crossRef)

    /**
     * Seeds initial roles, permissions, role-permission mappings, and users if the database is unpopulated.
     */
    suspend fun seedInitialDataIfEmpty() {
        val existingUsers = rbacDao.getAllUsers().firstOrNull()
        if (!existingUsers.isNullOrEmpty()) {
            // Already seeded
            return
        }

        // 1. Seed Roles
        val roles = listOf(
            RoleEntity("ADMIN", "Administrator with unrestricted root level authorization"),
            RoleEntity("HR", "Human Resources Personnel with employee & records management access"),
            RoleEntity("SECRETARY", "Office Secretary with meeting & agenda synchronization access"),
            RoleEntity("EMPLOYEE", "Regular Employee with access to personal workspace only")
        )
        roles.forEach { rbacDao.insertRole(it) }

        // 2. Seed Permissions
        val permissions = listOf(
            PermissionEntity("VIEW_SYSTEM_METRICS", "Unrestricted system telemetry view"),
            PermissionEntity("MANAGE_SYSTEM", "Unrestricted system configuration control"),
            PermissionEntity("VIEW_HR_PORTAL", "Access to HR database logs and analytics"),
            PermissionEntity("VIEW_SALARY_METRICS", "Access to salary/compensation summaries"),
            PermissionEntity("VIEW_LEAVE_LOGS", "Access to employee leave/appraisal reports"),
            PermissionEntity("MANAGE_SCHEDULE", "Access to schedule board room agendas"),
            PermissionEntity("VIEW_EMPLOYEE_PORTAL", "Access to personal dashboard workspace")
        )
        permissions.forEach { rbacDao.insertPermission(it) }

        // 3. Seed Role-Permission Associations
        val rolePermissionMappings = listOf(
            // ADMIN gets everything
            RolePermissionCrossRef("ADMIN", "VIEW_SYSTEM_METRICS"),
            RolePermissionCrossRef("ADMIN", "MANAGE_SYSTEM"),
            RolePermissionCrossRef("ADMIN", "VIEW_HR_PORTAL"),
            RolePermissionCrossRef("ADMIN", "VIEW_SALARY_METRICS"),
            RolePermissionCrossRef("ADMIN", "VIEW_LEAVE_LOGS"),
            RolePermissionCrossRef("ADMIN", "MANAGE_SCHEDULE"),
            RolePermissionCrossRef("ADMIN", "VIEW_EMPLOYEE_PORTAL"),

            // HR gets HR portal & logs & employee portal
            RolePermissionCrossRef("HR", "VIEW_HR_PORTAL"),
            RolePermissionCrossRef("HR", "VIEW_SALARY_METRICS"),
            RolePermissionCrossRef("HR", "VIEW_LEAVE_LOGS"),
            RolePermissionCrossRef("HR", "VIEW_EMPLOYEE_PORTAL"),

            // SECRETARY gets Schedule & employee portal
            RolePermissionCrossRef("SECRETARY", "MANAGE_SCHEDULE"),
            RolePermissionCrossRef("SECRETARY", "VIEW_EMPLOYEE_PORTAL"),

            // EMPLOYEE gets Employee portal
            RolePermissionCrossRef("EMPLOYEE", "VIEW_EMPLOYEE_PORTAL")
        )
        rolePermissionMappings.forEach { rbacDao.insertRolePermissionCrossRef(it) }

        // 4. Seed Standard Corporate Users
        val users = listOf(
            UserEntity("david.miller@korvix.com", "David Miller", "ADMIN", "All"),
            UserEntity("sarah.hr@korvix.com", "Sarah Jenkins", "HR", "Human Resources"),
            UserEntity("emily.sec@korvix.com", "Emily Davis", "SECRETARY", "Administration"),
            UserEntity("ankit.sharma@korvix.com", "Ankit Sharma", "EMPLOYEE", "Engineering")
        )
        users.forEach { rbacDao.insertUser(it) }

        // 5. Seed Compliance Audit Logs
        val initialLogs = listOf(
            AuditLogEntity(
                timestamp = System.currentTimeMillis() - 7200000,
                userEmail = "david.miller@korvix.com",
                userRole = "ADMIN",
                actionType = "LOGIN",
                details = "Root node authorization confirmed for David Miller",
                status = "SUCCESS"
            ),
            AuditLogEntity(
                timestamp = System.currentTimeMillis() - 5400000,
                userEmail = "sarah.hr@korvix.com",
                userRole = "HR",
                actionType = "LOGIN",
                details = "User Sarah Jenkins successfully logged into HR Portal",
                status = "SUCCESS"
            ),
            AuditLogEntity(
                timestamp = System.currentTimeMillis() - 3600000,
                userEmail = "ankit.sharma@korvix.com",
                userRole = "EMPLOYEE",
                actionType = "AI_QUERY",
                details = "Successfully processed user AI query: \"What are my available hours?\"",
                status = "SUCCESS"
            ),
            AuditLogEntity(
                timestamp = System.currentTimeMillis() - 1800000,
                userEmail = "ankit.sharma@korvix.com",
                userRole = "EMPLOYEE",
                actionType = "AI_QUERY",
                details = "Blocked unauthorized query from Employee: \"Who are HR users?\"",
                status = "DENIED"
            )
        )
        initialLogs.forEach { rbacDao.insertAuditLog(it) }

        // 6. Seed System Activities and File Jobs
        val initialActivities = listOf(
            SystemActivityEntity(
                timestamp = System.currentTimeMillis() - 14400000, // 4 hours ago
                activityType = "FILE_PROCESSING",
                title = "Parsed quarterly billing ledger (timesheet_q2_raw.csv)",
                status = "SUCCESS",
                recordsProcessed = 10000,
                durationMs = 124L,
                operatorEmail = "david.miller@korvix.com"
            ),
            SystemActivityEntity(
                timestamp = System.currentTimeMillis() - 10800000, // 3 hours ago
                activityType = "SECURITY_ALERT",
                title = "Executed global digital signature compliance scan",
                status = "SUCCESS",
                recordsProcessed = 12480,
                durationMs = 342L,
                operatorEmail = "david.miller@korvix.com"
            ),
            SystemActivityEntity(
                timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                activityType = "FILE_PROCESSING",
                title = "Ingested employee roster update (payroll_records.csv)",
                status = "SUCCESS",
                recordsProcessed = 2500,
                durationMs = 45L,
                operatorEmail = "sarah.hr@korvix.com"
            ),
            SystemActivityEntity(
                timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                activityType = "COMPLIANCE_SYNC",
                title = "Rotated HSM cryptography log keys",
                status = "SUCCESS",
                recordsProcessed = 120,
                durationMs = 18L,
                operatorEmail = "david.miller@korvix.com"
            )
        )
        initialActivities.forEach { rbacDao.insertSystemActivity(it) }
    }
}
