package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AggregatedData
import com.example.data.CSVProcessor
import com.example.data.DeptCount
import com.example.data.PerformanceCount
import com.example.data.DateTrendCount
import com.example.data.database.CachedFileEntity
import com.example.data.database.CachedRecordEntity
import com.example.data.database.RbacDatabase
import com.example.data.database.UserSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: RbacDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RbacDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KORVIX", appName)
    }

    @Test
    fun `test file cache DAO inserting and instant cache hit retrieval`() = runBlocking {
        val fileCacheDao = database.fileCacheDao()
        val fileId = "test_file_001"
        val checksum = "a1b2c3d4e5f67890abcdef"
        val sampleJson = """{"totalRowsParsed":10000,"deptDistribution":[],"performanceMetrics":[],"dateTrend":[],"anomalies":[]}"""

        val fileEntity = CachedFileEntity(
            fileId = fileId,
            fileName = "timesheet_test.csv",
            fileType = "text/csv",
            fileSizeBytes = 102400L,
            uploadedAt = System.currentTimeMillis(),
            uploadedBy = "admin@korvix.com",
            recordCount = 10000,
            checksum = checksum,
            rawContent = "department,date,performance\nEngineering,16 May 2024,Good",
            cacheStatus = "CACHED",
            lastAccessedAt = System.currentTimeMillis(),
            parsingDurationMs = 74L,
            summaryJson = sampleJson
        )

        // 1. Insert into Room
        fileCacheDao.insertCachedFile(fileEntity)

        // 2. High-performance cache lookup by checksum
        val cachedHit = fileCacheDao.getFileByChecksum(checksum)
        assertNotNull("Expected cache hit for matching checksum", cachedHit)
        assertEquals(fileId, cachedHit?.fileId)
        assertEquals("timesheet_test.csv", cachedHit?.fileName)
        assertEquals(10000, cachedHit?.recordCount)

        // 3. Insert parsed records
        val records = listOf(
            CachedRecordEntity(fileId = fileId, rowIndex = 1, department = "Engineering", recordDate = "16 May 2024", performanceRating = "Good"),
            CachedRecordEntity(fileId = fileId, rowIndex = 2, department = "Engineering", recordDate = "16 May 2024", performanceRating = "Very Good"),
            CachedRecordEntity(fileId = fileId, rowIndex = 3, department = "Legal", recordDate = "15 May 2024", performanceRating = "Unsatisfactory", hasAnomaly = true, anomalyDetails = "Elevated dissatisfaction")
        )
        fileCacheDao.insertCachedRecords(records)

        // 4. Query record count
        val count = fileCacheDao.getRecordCount(fileId)
        assertEquals(3, count)

        // 5. Query department breakdown from indexed records
        val deptBreakdown = fileCacheDao.getDepartmentBreakdown(fileId)
        assertEquals(2, deptBreakdown.size)
        val engCount = deptBreakdown.firstOrNull { it.department == "Engineering" }?.count
        assertEquals(2, engCount)

        // 6. Query anomalies flow
        val anomalies = fileCacheDao.getAnomaliesForFileFlow(fileId).first()
        assertEquals(1, anomalies.size)
        assertEquals("Legal", anomalies[0].department)

        // 7. Cleanup
        fileCacheDao.deleteCachedFile(fileId)
        val afterDelete = fileCacheDao.getCachedFileById(fileId)
        assertNull(afterDelete)
    }

    @Test
    fun `test user session DAO lifecycle in Room database`() = runBlocking {
        val userSessionDao = database.userSessionDao()
        val sessionId = "sess_test_123"
        val token = "kvx_tk_test_xyz789"
        val email = "sarah.hr@korvix.com"

        val session = UserSessionEntity(
            sessionId = sessionId,
            userEmail = email,
            userName = "Sarah Jenkins",
            userRole = "HR",
            department = "Human Resources",
            sessionToken = token,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L,
            isActive = true,
            activePortal = "HR"
        )

        // 1. Insert session
        userSessionDao.insertSession(session)

        // 2. Query active session
        val active = userSessionDao.getActiveSession()
        assertNotNull("Expected active session", active)
        assertEquals(sessionId, active?.sessionId)
        assertEquals("HR", active?.userRole)

        // 3. Update active portal
        userSessionDao.updateActivePortal(sessionId, "EMPLOYEE")
        val updated = userSessionDao.getActiveSession()
        assertEquals("EMPLOYEE", updated?.activePortal)

        // 4. Deactivate session
        userSessionDao.deactivateSession(sessionId)
        val afterDeactivation = userSessionDao.getActiveSession()
        assertNull("Deactivated session should not be returned by getActiveSession", afterDeactivation)
    }

    @Test
    fun `test CSVProcessor JSON serialization and checksum`() {
        val testData = "department,date,performance\nEngineering,16 May 2024,Good\n".toByteArray(Charsets.UTF_8)
        val checksum = CSVProcessor.calculateChecksum(testData)
        assertNotNull(checksum)
        assertTrue(checksum.length == 64) // SHA-256 hex string length

        val aggregatedData = AggregatedData(
            totalRowsParsed = 10000,
            deptDistribution = listOf(DeptCount("Engineering", 3200, "32.0%")),
            performanceMetrics = listOf(PerformanceCount("Good", 5000, "50.0%")),
            dateTrend = listOf(DateTrendCount("16 May 2024", 800)),
            anomalies = listOf("Missing department anomaly detected")
        )

        val jsonString = CSVProcessor.serializeAggregatedData(aggregatedData)
        assertNotNull(jsonString)

        val deserialized = CSVProcessor.deserializeAggregatedData(jsonString)
        assertEquals(10000, deserialized.totalRowsParsed)
        assertEquals(1, deserialized.deptDistribution.size)
        assertEquals("Engineering", deserialized.deptDistribution[0].name)
        assertEquals("Missing department anomaly detected", deserialized.anomalies[0])
    }
}
