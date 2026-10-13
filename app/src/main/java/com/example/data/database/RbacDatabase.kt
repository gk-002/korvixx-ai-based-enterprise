package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Room Entities for KORVIX RBAC system ---

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey val roleId: String, // e.g., "ADMIN", "HR", "SECRETARY", "EMPLOYEE"
    val description: String
)

@Entity(tableName = "permissions")
data class PermissionEntity(
    @PrimaryKey val permissionId: String, // e.g., "VIEW_SYSTEM_METRICS", "VIEW_HR_PORTAL"
    val description: String
)

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["roleId"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val roleId: String, // foreign key to RoleEntity.roleId
    val department: String
)

@Entity(
    tableName = "role_permission_cross_ref",
    primaryKeys = ["roleId", "permissionId"],
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["roleId"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PermissionEntity::class,
            parentColumns = ["permissionId"],
            childColumns = ["permissionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RolePermissionCrossRef(
    val roleId: String,
    val permissionId: String
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val userEmail: String,
    val userRole: String,
    val actionType: String, // e.g. "LOGIN", "LOGOUT", "SWITCH_PORTAL", "FILE_UPLOAD", "AI_QUERY", "EXPORT_TABLE"
    val details: String,
    val status: String // e.g. "SUCCESS", "DENIED"
)

@Entity(tableName = "system_activities")
data class SystemActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val activityType: String, // e.g., "FILE_PROCESSING", "SECURITY_ALERT", "COMPLIANCE_SYNC", "EXPORT_JOB"
    val title: String,
    val status: String, // "SUCCESS", "FAILED", "RUNNING"
    val recordsProcessed: Int = 0,
    val durationMs: Long = 0L,
    val operatorEmail: String
)

// --- Plain Kotlin Data Classes (Mapped in Repository layer) ---

data class UserWithRole(
    val user: UserEntity,
    val role: RoleEntity
)

data class RoleWithPermissions(
    val role: RoleEntity,
    val permissions: List<PermissionEntity>
)

// --- Data Access Object ---

@Dao
interface RbacDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: RoleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: PermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRolePermissionCrossRef(crossRef: RolePermissionCrossRef)

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM roles")
    fun getAllRoles(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM permissions")
    fun getAllPermissions(): Flow<List<PermissionEntity>>

    @Query("SELECT * FROM role_permission_cross_ref")
    fun getAllRolePermissionCrossRefs(): Flow<List<RolePermissionCrossRef>>

    @Query("""
        SELECT rp.permissionId FROM role_permission_cross_ref rp
        INNER JOIN users u ON u.roleId = rp.roleId
        WHERE u.email = :email
    """)
    fun getUserPermissionsFlow(email: String): Flow<List<String>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM users u
            INNER JOIN role_permission_cross_ref rp ON u.roleId = rp.roleId
            WHERE u.email = :email AND rp.permissionId = :permissionId
        )
    """)
    suspend fun hasPermission(email: String, permissionId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemActivity(activity: SystemActivityEntity)

    @Query("SELECT * FROM system_activities ORDER BY timestamp DESC")
    fun getAllSystemActivities(): Flow<List<SystemActivityEntity>>
}

// --- App Database Holder ---

@Database(
    entities = [
        UserEntity::class,
        RoleEntity::class,
        PermissionEntity::class,
        RolePermissionCrossRef::class,
        AuditLogEntity::class,
        SystemActivityEntity::class,
        CachedFileEntity::class,
        CachedRecordEntity::class,
        UserSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class RbacDatabase : RoomDatabase() {
    abstract fun rbacDao(): RbacDao
    abstract fun fileCacheDao(): FileCacheDao
    abstract fun userSessionDao(): UserSessionDao

    companion object {
        @Volatile
        private var INSTANCE: RbacDatabase? = null

        fun getDatabase(context: android.content.Context): RbacDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    RbacDatabase::class.java,
                    "korvix_rbac_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
