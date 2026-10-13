package com.example.data

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

class RbacRepository(
    private val rbacDao: RbacDao,
    private val fileCacheDao: FileCacheDao,
    private val userSessionDao: UserSessionDao
) {

    // --- RBAC User & Role Flows ---

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

    // --- File Cache Repository Methods for High-Performance Engine Access ---

    val allCachedFiles: Flow<List<CachedFileEntity>> = fileCacheDao.getAllCachedFiles()

    fun getCachedFileByIdFlow(fileId: String): Flow<CachedFileEntity?> = fileCacheDao.getCachedFileByIdFlow(fileId)

    suspend fun getCachedFileById(fileId: String): CachedFileEntity? = fileCacheDao.getCachedFileById(fileId)

    suspend fun getFileByChecksum(checksum: String): CachedFileEntity? = fileCacheDao.getFileByChecksum(checksum)

    suspend fun insertCachedFile(file: CachedFileEntity) = fileCacheDao.insertCachedFile(file)

    suspend fun insertCachedRecords(records: List<CachedRecordEntity>) = fileCacheDao.insertCachedRecords(records)

    suspend fun deleteCachedFile(fileId: String) = fileCacheDao.deleteCachedFile(fileId)

    suspend fun clearAllCachedFiles() = fileCacheDao.clearAllCachedFiles()

    fun getRecordsForFileFlow(fileId: String): Flow<List<CachedRecordEntity>> = fileCacheDao.getRecordsForFileFlow(fileId)

    suspend fun getRecordsPaged(fileId: String, limit: Int, offset: Int): List<CachedRecordEntity> =
        fileCacheDao.getRecordsPaged(fileId, limit, offset)

    fun getAnomaliesForFileFlow(fileId: String): Flow<List<CachedRecordEntity>> = fileCacheDao.getAnomaliesForFileFlow(fileId)

    suspend fun getDepartmentBreakdown(fileId: String): List<DeptCountResult> = fileCacheDao.getDepartmentBreakdown(fileId)

    suspend fun getPerformanceBreakdown(fileId: String): List<PerfCountResult> = fileCacheDao.getPerformanceBreakdown(fileId)

    suspend fun updateLastAccessed(fileId: String) = fileCacheDao.updateLastAccessed(fileId)

    // --- User Session Repository Methods ---

    val activeSession: Flow<UserSessionEntity?> = userSessionDao.getActiveSessionFlow()

    val allSessions: Flow<List<UserSessionEntity>> = userSessionDao.getAllSessionsFlow()

    suspend fun getActiveSession(): UserSessionEntity? = userSessionDao.getActiveSession()

    fun getSessionsForUser(email: String): Flow<List<UserSessionEntity>> = userSessionDao.getSessionsForUser(email)

    suspend fun insertSession(session: UserSessionEntity) = userSessionDao.insertSession(session)

    suspend fun touchSession(sessionId: String) = userSessionDao.touchSession(sessionId)

    suspend fun deactivateSession(sessionId: String) = userSessionDao.deactivateSession(sessionId)

    suspend fun deactivateAllSessionsForUser(email: String) = userSessionDao.deactivateAllSessionsForUser(email)

    suspend fun updateSessionPortal(sessionId: String, portal: String) = userSessionDao.updateActivePortal(sessionId, portal)

    suspend fun cleanupOldSessions(cutoffTime: Long) = userSessionDao.cleanupOldSessions(cutoffTime)

    suspend fun clearAllSessions() = userSessionDao.clearAllSessions()

    /**
     * Seeds initial roles, permissions, role-permission mappings, users,
     * sample cached files and user sessions if database is unpopulated.
     */
    suspend fun seedInitialDataIfEmpty() {
        val existingUsers = rbacDao.getAllUsers().firstOrNull()
        if (!existingUsers.isNullOrEmpty()) {
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
                timestamp = System.currentTimeMillis() - 14400000,
                activityType = "FILE_PROCESSING",
                title = "Parsed quarterly billing ledger (timesheet_q2_raw.csv)",
                status = "SUCCESS",
                recordsProcessed = 10000,
                durationMs = 124L,
                operatorEmail = "david.miller@korvix.com"
            ),
            SystemActivityEntity(
                timestamp = System.currentTimeMillis() - 10800000,
                activityType = "SECURITY_ALERT",
                title = "Executed global digital signature compliance scan",
                status = "SUCCESS",
                recordsProcessed = 12480,
                durationMs = 342L,
                operatorEmail = "david.miller@korvix.com"
            ),
            SystemActivityEntity(
                timestamp = System.currentTimeMillis() - 7200000,
                activityType = "FILE_PROCESSING",
                title = "Ingested employee roster update (payroll_records.csv)",
                status = "SUCCESS",
                recordsProcessed = 2500,
                durationMs = 45L,
                operatorEmail = "sarah.hr@korvix.com"
            ),
            SystemActivityEntity(
                timestamp = System.currentTimeMillis() - 3600000,
                activityType = "COMPLIANCE_SYNC",
                title = "Rotated HSM cryptography log keys",
                status = "SUCCESS",
                recordsProcessed = 120,
                durationMs = 18L,
                operatorEmail = "david.miller@korvix.com"
            )
        )
        initialActivities.forEach { rbacDao.insertSystemActivity(it) }

        // 7. Seed Initial Room Cached Files for High-Performance File Parsing Engine
        val seededFileId = "file_seed_001"
        val sampleMockData = CSVProcessor.generateMockCSVBytes()
        val sampleChecksum = CSVProcessor.calculateChecksum(sampleMockData)
        val initialCachedFile = CachedFileEntity(
            fileId = seededFileId,
            fileName = "timesheet_q2_raw.csv",
            fileType = "text/csv",
            fileSizeBytes = sampleMockData.size.toLong(),
            uploadedAt = System.currentTimeMillis() - 14400000L,
            uploadedBy = "david.miller@korvix.com",
            recordCount = 10000,
            checksum = sampleChecksum,
            rawContent = "department,date,performance\nEngineering,16 May 2024,Good\nSales,15 May 2024,Very Good\n...",
            cacheStatus = "CACHED",
            lastAccessedAt = System.currentTimeMillis() - 600000L,
            parsingDurationMs = 118L,
            summaryJson = null
        )
        fileCacheDao.insertCachedFile(initialCachedFile)

        // Seed 10 sample indexed records for immediate query testing
        val sampleRecords = listOf(
            CachedRecordEntity(fileId = seededFileId, rowIndex = 1, department = "Engineering", recordDate = "16 May 2024", performanceRating = "Good"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 2, department = "Sales", recordDate = "15 May 2024", performanceRating = "Very Good"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 3, department = "HR", recordDate = "14 May 2024", performanceRating = "Excellent"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 4, department = "Finance", recordDate = "13 May 2024", performanceRating = "Good"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 5, department = "Marketing", recordDate = "12 May 2024", performanceRating = "Needs Improvement"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 6, department = "Legal", recordDate = "11 May 2024", performanceRating = "Unsatisfactory", hasAnomaly = true, anomalyDetails = "Elevated dissatisfaction threshold"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 7, department = "", recordDate = "16 May 2024", performanceRating = "Excellent", hasAnomaly = true, anomalyDetails = "Missing department"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 8, department = "Engineering", recordDate = "", performanceRating = "Good", hasAnomaly = true, anomalyDetails = "Missing date"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 9, department = "Sales", recordDate = "15 May 2024", performanceRating = "", hasAnomaly = true, anomalyDetails = "Missing rating"),
            CachedRecordEntity(fileId = seededFileId, rowIndex = 10, department = "Finance", recordDate = "10 May 2024", performanceRating = "Very Good")
        )
        fileCacheDao.insertCachedRecords(sampleRecords)

        // 8. Seed Initial User Session in Room
        val initialSession = UserSessionEntity(
            sessionId = "sess_" + UUID.randomUUID().toString().take(8),
            userEmail = "david.miller@korvix.com",
            userName = "David Miller",
            userRole = "ADMIN",
            department = "All",
            sessionToken = "kvx_tk_" + UUID.randomUUID().toString().replace("-", ""),
            createdAt = System.currentTimeMillis() - 7200000L,
            lastActiveAt = System.currentTimeMillis() - 120000L,
            expiresAt = System.currentTimeMillis() + 79200000L,
            isActive = true,
            deviceInfo = "KORVIX Workstation Terminal (Android/Compose)",
            ipAddress = "10.142.0.4",
            activePortal = "ADMIN"
        )
        userSessionDao.insertSession(initialSession)
    }
}
