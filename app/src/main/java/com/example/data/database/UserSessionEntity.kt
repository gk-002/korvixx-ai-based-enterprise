package com.example.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Entity representing active and historical user sessions stored in Room database.
 * Preserves authentication context, role assignments, active portal state, and token lifecycle.
 */
@Entity(
    tableName = "user_sessions",
    indices = [
        Index(value = ["userEmail"]),
        Index(value = ["sessionToken"], unique = true),
        Index(value = ["isActive"])
    ]
)
data class UserSessionEntity(
    @PrimaryKey val sessionId: String,
    val userEmail: String,
    val userName: String,
    val userRole: String, // "ADMIN", "HR", "SECRETARY", "EMPLOYEE"
    val department: String,
    val sessionToken: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L, // 24 hours expiry
    val isActive: Boolean = true,
    val deviceInfo: String = "KORVIX Workstation Client (Android/Compose)",
    val ipAddress: String = "10.142.0.4",
    val activePortal: String = "ADMIN",
    val sessionStateJson: String? = null
)

@Dao
interface UserSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSessionEntity)

    @Query("SELECT * FROM user_sessions WHERE isActive = 1 ORDER BY lastActiveAt DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_sessions WHERE isActive = 1 ORDER BY lastActiveAt DESC LIMIT 1")
    suspend fun getActiveSession(): UserSessionEntity?

    @Query("SELECT * FROM user_sessions WHERE userEmail = :email ORDER BY lastActiveAt DESC")
    fun getSessionsForUser(email: String): Flow<List<UserSessionEntity>>

    @Query("SELECT * FROM user_sessions ORDER BY createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<UserSessionEntity>>

    @Query("UPDATE user_sessions SET lastActiveAt = :lastActiveAt WHERE sessionId = :sessionId")
    suspend fun touchSession(sessionId: String, lastActiveAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_sessions SET isActive = 0 WHERE sessionId = :sessionId")
    suspend fun deactivateSession(sessionId: String)

    @Query("UPDATE user_sessions SET isActive = 0 WHERE userEmail = :email")
    suspend fun deactivateAllSessionsForUser(email: String)

    @Query("UPDATE user_sessions SET activePortal = :portal, lastActiveAt = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateActivePortal(sessionId: String, portal: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_sessions WHERE expiresAt < :cutoffTime OR isActive = 0")
    suspend fun cleanupOldSessions(cutoffTime: Long)

    @Query("DELETE FROM user_sessions")
    suspend fun clearAllSessions()
}
