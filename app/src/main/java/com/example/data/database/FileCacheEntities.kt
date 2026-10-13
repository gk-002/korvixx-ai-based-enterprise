package com.example.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Entity representing cached files stored locally in SQLite for high-performance access
 * by the KORVIX file parsing engine.
 */
@Entity(
    tableName = "cached_files",
    indices = [
        Index(value = ["checksum"]),
        Index(value = ["uploadedBy"]),
        Index(value = ["lastAccessedAt"])
    ]
)
data class CachedFileEntity(
    @PrimaryKey val fileId: String,
    val fileName: String,
    val fileType: String = "text/csv",
    val fileSizeBytes: Long,
    val uploadedAt: Long,
    val uploadedBy: String,
    val recordCount: Int,
    val checksum: String,
    val rawContent: String? = null,
    val cacheStatus: String = "CACHED", // "CACHED", "PROCESSED", "EVICTED"
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val parsingDurationMs: Long = 0L,
    val summaryJson: String? = null
)

/**
 * Entity representing individual parsed records stored in Room with database indices.
 * Provides sub-millisecond querying, filtering, and aggregation for 10,000+ record datasets.
 */
@Entity(
    tableName = "cached_records",
    foreignKeys = [
        ForeignKey(
            entity = CachedFileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fileId", "rowIndex"]),
        Index(value = ["fileId", "department"]),
        Index(value = ["fileId", "hasAnomaly"]),
        Index(value = ["fileId", "recordDate"])
    ]
)
data class CachedRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: String,
    val rowIndex: Int,
    val department: String,
    val recordDate: String,
    val performanceRating: String,
    val hasAnomaly: Boolean = false,
    val anomalyDetails: String? = null
)

data class DeptCountResult(
    val department: String,
    val count: Int
)

data class PerfCountResult(
    val performanceRating: String,
    val count: Int
)

@Dao
interface FileCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedFile(file: CachedFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedFiles(files: List<CachedFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRecords(records: List<CachedRecordEntity>)

    @Query("SELECT * FROM cached_files ORDER BY lastAccessedAt DESC")
    fun getAllCachedFiles(): Flow<List<CachedFileEntity>>

    @Query("SELECT * FROM cached_files WHERE fileId = :fileId LIMIT 1")
    suspend fun getCachedFileById(fileId: String): CachedFileEntity?

    @Query("SELECT * FROM cached_files WHERE fileId = :fileId LIMIT 1")
    fun getCachedFileByIdFlow(fileId: String): Flow<CachedFileEntity?>

    @Query("SELECT * FROM cached_files WHERE checksum = :checksum LIMIT 1")
    suspend fun getFileByChecksum(checksum: String): CachedFileEntity?

    @Query("UPDATE cached_files SET lastAccessedAt = :timestamp WHERE fileId = :fileId")
    suspend fun updateLastAccessed(fileId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_files WHERE fileId = :fileId")
    suspend fun deleteCachedFile(fileId: String)

    @Query("DELETE FROM cached_files")
    suspend fun clearAllCachedFiles()

    @Query("DELETE FROM cached_records WHERE fileId = :fileId")
    suspend fun deleteRecordsForFile(fileId: String)

    @Query("SELECT * FROM cached_records WHERE fileId = :fileId ORDER BY rowIndex ASC")
    fun getRecordsForFileFlow(fileId: String): Flow<List<CachedRecordEntity>>

    @Query("SELECT * FROM cached_records WHERE fileId = :fileId ORDER BY rowIndex ASC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaged(fileId: String, limit: Int, offset: Int): List<CachedRecordEntity>

    @Query("SELECT COUNT(*) FROM cached_records WHERE fileId = :fileId")
    suspend fun getRecordCount(fileId: String): Int

    @Query("SELECT * FROM cached_records WHERE fileId = :fileId AND hasAnomaly = 1 ORDER BY rowIndex ASC")
    fun getAnomaliesForFileFlow(fileId: String): Flow<List<CachedRecordEntity>>

    @Query("SELECT department, COUNT(*) as count FROM cached_records WHERE fileId = :fileId AND department != '' GROUP BY department ORDER BY count DESC")
    suspend fun getDepartmentBreakdown(fileId: String): List<DeptCountResult>

    @Query("SELECT performanceRating, COUNT(*) as count FROM cached_records WHERE fileId = :fileId AND performanceRating != '' GROUP BY performanceRating ORDER BY count DESC")
    suspend fun getPerformanceBreakdown(fileId: String): List<PerfCountResult>
}
