package com.example.data

import com.example.data.database.CachedFileEntity
import com.example.data.database.CachedRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import kotlin.random.Random

data class AggregatedData(
    val totalRowsParsed: Int,
    val deptDistribution: List<DeptCount>,
    val performanceMetrics: List<PerformanceCount>,
    val dateTrend: List<DateTrendCount>,
    val anomalies: List<String> = emptyList()
)

data class DeptCount(val name: String, val count: Int, val percentage: String)
data class PerformanceCount(val rating: String, val count: Int, val percentage: String)
data class DateTrendCount(val date: String, val count: Int)

data class ParsedFileResult(
    val aggregatedData: AggregatedData,
    val cachedFile: CachedFileEntity,
    val records: List<CachedRecordEntity>,
    val isCacheHit: Boolean = false
)

object CSVProcessor {

    /**
     * Calculates SHA-256 checksum for byte payload to ensure data integrity and instant cache lookup.
     */
    fun calculateChecksum(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a mock CSV with 10,000 records of enterprise data as raw ByteArray.
     * Intentionally injects missing data patterns and critical anomalies for AI detection.
     */
    fun generateMockCSVBytes(): ByteArray {
        val departments = listOf("Engineering", "Sales", "HR", "Finance", "Marketing", "Legal")
        val performanceLevels = listOf("Excellent", "Very Good", "Good", "Needs Improvement", "Unsatisfactory")
        val baseDate = 1715817600000L // 16 May 2024
        val msInDay = 86400000L

        val stringBuilder = java.lang.StringBuilder(350000)
        stringBuilder.append("department,date,performance\n")

        for (i in 1..10000) {
            // 1. Inject missing department anomaly at index 500 & 4500
            if (i == 500 || i == 4500) {
                stringBuilder.append(",16 May 2024,Excellent\n")
                continue
            }
            // 2. Inject missing date anomaly at index 100
            if (i == 100) {
                stringBuilder.append("Engineering,,Good\n")
                continue
            }
            // 3. Inject missing performance rating anomaly at index 1500
            if (i == 1500) {
                stringBuilder.append("Sales,15 May 2024,\n")
                continue
            }
            // 4. Inject corrupted line anomaly (less than 3 columns) at index 3000
            if (i == 3000) {
                stringBuilder.append("Marketing\n")
                continue
            }

            val dept = departments[Random.nextInt(departments.size)]
            // Distribute over 15 days
            val dateOffset = Random.nextInt(15) * msInDay
            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                .format(java.util.Date(baseDate - dateOffset))

            // 5. Inject a critical "Unsatisfactory" spike in Legal department
            val perf = if (dept == "Legal" && Random.nextFloat() < 0.42f) {
                "Unsatisfactory"
            } else {
                performanceLevels[Random.nextInt(performanceLevels.size)]
            }
            
            stringBuilder.append("$dept,$dateStr,$perf\n")
        }

        return stringBuilder.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Generates a mock CSV with 10,000 records of enterprise data.
     */
    fun generateMockCSVStream(): InputStream {
        return generateMockCSVBytes().inputStream()
    }

    /**
     * Serializes AggregatedData into lightweight JSON for Room database caching.
     */
    fun serializeAggregatedData(data: AggregatedData): String {
        val json = JSONObject()
        json.put("totalRowsParsed", data.totalRowsParsed)
        
        val deptArray = JSONArray()
        data.deptDistribution.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("count", it.count)
            obj.put("percentage", it.percentage)
            deptArray.put(obj)
        }
        json.put("deptDistribution", deptArray)

        val perfArray = JSONArray()
        data.performanceMetrics.forEach {
            val obj = JSONObject()
            obj.put("rating", it.rating)
            obj.put("count", it.count)
            obj.put("percentage", it.percentage)
            perfArray.put(obj)
        }
        json.put("performanceMetrics", perfArray)

        val dateArray = JSONArray()
        data.dateTrend.forEach {
            val obj = JSONObject()
            obj.put("date", it.date)
            obj.put("count", it.count)
            dateArray.put(obj)
        }
        json.put("dateTrend", dateArray)

        val anomalyArray = JSONArray()
        data.anomalies.forEach { anomalyArray.put(it) }
        json.put("anomalies", anomalyArray)

        return json.toString()
    }

    /**
     * Deserializes AggregatedData from Room database cached summary.
     */
    fun deserializeAggregatedData(jsonStr: String): AggregatedData {
        val json = JSONObject(jsonStr)
        val total = json.getInt("totalRowsParsed")
        
        val deptList = mutableListOf<DeptCount>()
        val deptArray = json.optJSONArray("deptDistribution") ?: JSONArray()
        for (i in 0 until deptArray.length()) {
            val obj = deptArray.getJSONObject(i)
            deptList.add(DeptCount(obj.getString("name"), obj.getInt("count"), obj.getString("percentage")))
        }

        val perfList = mutableListOf<PerformanceCount>()
        val perfArray = json.optJSONArray("performanceMetrics") ?: JSONArray()
        for (i in 0 until perfArray.length()) {
            val obj = perfArray.getJSONObject(i)
            perfList.add(PerformanceCount(obj.getString("rating"), obj.getInt("count"), obj.getString("percentage")))
        }

        val dateList = mutableListOf<DateTrendCount>()
        val dateArray = json.optJSONArray("dateTrend") ?: JSONArray()
        for (i in 0 until dateArray.length()) {
            val obj = dateArray.getJSONObject(i)
            dateList.add(DateTrendCount(obj.getString("date"), obj.getInt("count")))
        }

        val anomalyList = mutableListOf<String>()
        val anomalyArray = json.optJSONArray("anomalies") ?: JSONArray()
        for (i in 0 until anomalyArray.length()) {
            anomalyList.add(anomalyArray.getString(i))
        }

        return AggregatedData(total, deptList, perfList, dateList, anomalyList)
    }

    /**
     * Parse and aggregate 10,000+ records in the background without blocking the UI thread.
     * Identifies critical anomalies and missing data patterns.
     */
    suspend fun parseAndAggregate(inputStream: InputStream, onProgress: (Float) -> Unit = {}): AggregatedData = withContext(Dispatchers.Default) {
        val result = parseInternal(inputStream, fileId = "temp", onProgress)
        result.first
    }

    /**
     * High-performance parsing and record extraction for Room SQLite caching.
     * Returns both the aggregated analytical structure and the individual parsed records
     * ready for batched insertion into Room.
     */
    suspend fun parseAndExtractRecords(
        bytes: ByteArray,
        fileName: String,
        fileId: String,
        uploadedBy: String,
        onProgress: (Float) -> Unit = {}
    ): ParsedFileResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val checksum = calculateChecksum(bytes)
        val (aggregated, records) = parseInternal(bytes.inputStream(), fileId = fileId, onProgress = onProgress)
        val duration = System.currentTimeMillis() - startTime

        val rawPreview = if (bytes.size > 2048) {
            String(bytes.take(2048).toByteArray(), Charsets.UTF_8) + "\n...[truncated preview]"
        } else {
            String(bytes, Charsets.UTF_8)
        }

        val cachedFile = CachedFileEntity(
            fileId = fileId,
            fileName = fileName,
            fileType = "text/csv",
            fileSizeBytes = bytes.size.toLong(),
            uploadedAt = System.currentTimeMillis(),
            uploadedBy = uploadedBy,
            recordCount = aggregated.totalRowsParsed,
            checksum = checksum,
            rawContent = rawPreview,
            cacheStatus = "CACHED",
            lastAccessedAt = System.currentTimeMillis(),
            parsingDurationMs = duration,
            summaryJson = serializeAggregatedData(aggregated)
        )

        ParsedFileResult(
            aggregatedData = aggregated,
            cachedFile = cachedFile,
            records = records,
            isCacheHit = false
        )
    }

    private suspend fun parseInternal(
        inputStream: InputStream,
        fileId: String,
        onProgress: (Float) -> Unit
    ): Pair<AggregatedData, List<CachedRecordEntity>> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine() // Header

        var totalRows = 0
        var missingDeptCount = 0
        var missingDateCount = 0
        var missingPerfCount = 0
        var corruptRowsCount = 0

        val deptCounts = mutableMapOf<String, Int>()
        val performanceCounts = mutableMapOf(
            "Excellent" to 0,
            "Very Good" to 0,
            "Good" to 0,
            "Needs Improvement" to 0,
            "Unsatisfactory" to 0
        )
        val dateCounts = mutableMapOf<String, Int>()
        val deptPerfCounts = mutableMapOf<String, MutableMap<String, Int>>()

        val cachedRecords = ArrayList<CachedRecordEntity>(10050)

        // Read all lines
        while (line != null) {
            line = reader.readLine() ?: break
            totalRows++

            val columns = line.split(",")
            if (columns.size < 3) {
                corruptRowsCount++
                cachedRecords.add(
                    CachedRecordEntity(
                        fileId = fileId,
                        rowIndex = totalRows,
                        department = "",
                        recordDate = "",
                        performanceRating = "",
                        hasAnomaly = true,
                        anomalyDetails = "Corrupted line: insufficient columns"
                    )
                )
                continue
            }

            val dept = columns[0].trim()
            val date = columns[1].trim()
            val perf = columns[2].trim()

            var hasMissingData = false
            val anomalyReasons = mutableListOf<String>()

            if (dept.isEmpty()) {
                missingDeptCount++
                hasMissingData = true
                anomalyReasons.add("Missing department")
            }
            if (date.isEmpty()) {
                missingDateCount++
                hasMissingData = true
                anomalyReasons.add("Missing date")
            }
            if (perf.isEmpty()) {
                missingPerfCount++
                hasMissingData = true
                anomalyReasons.add("Missing rating")
            }

            if (!hasMissingData) {
                deptCounts[dept] = (deptCounts[dept] ?: 0) + 1
                performanceCounts[perf] = (performanceCounts[perf] ?: 0) + 1
                dateCounts[date] = (dateCounts[date] ?: 0) + 1

                val perfMap = deptPerfCounts.getOrPut(dept) { mutableMapOf() }
                perfMap[perf] = (perfMap[perf] ?: 0) + 1
            }

            cachedRecords.add(
                CachedRecordEntity(
                    fileId = fileId,
                    rowIndex = totalRows,
                    department = dept,
                    recordDate = date,
                    performanceRating = perf,
                    hasAnomaly = hasMissingData,
                    anomalyDetails = if (anomalyReasons.isNotEmpty()) anomalyReasons.joinToString("; ") else null
                )
            )

            // Simulate slight streaming progress chunking so the UI feels responsive
            if (totalRows % 2000 == 0) {
                val progress = totalRows.toFloat() / 10000f
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }
                delay(60) // Visual progress pacing
            }
        }

        reader.close()

        // Compile identified anomalies
        val anomalies = mutableListOf<String>()
        if (missingDeptCount > 0) {
            anomalies.add("⚠️ Missing Department identifier found in $missingDeptCount records.")
        }
        if (missingDateCount > 0) {
            anomalies.add("⚠️ Missing Timestamp/Date found in $missingDateCount records.")
        }
        if (missingPerfCount > 0) {
            anomalies.add("⚠️ Missing Performance Rating found in $missingPerfCount records.")
        }
        if (corruptRowsCount > 0) {
            anomalies.add("🛑 Incomplete columns (Corrupted CSV structure) in $corruptRowsCount records.")
        }

        // Check for department-specific Unsatisfactory performance concentration spikes (>35%)
        deptPerfCounts.forEach { (dept, perfMap) ->
            val totalInDept = perfMap.values.sum()
            val unsatisfactoryCount = perfMap["Unsatisfactory"] ?: 0
            if (totalInDept > 50) {
                val ratio = unsatisfactoryCount.toFloat() / totalInDept.toFloat()
                if (ratio > 0.35f) {
                    val percentageStr = String.format(java.util.Locale.US, "%.1f%%", ratio * 100f)
                    anomalies.add("🔥 Critical Performance Anomaly: '$dept' department exhibits an unusual 'Unsatisfactory' rating concentration of $percentageStr.")
                }
            }
        }

        val deptList = deptCounts.map { (name, count) ->
            DeptCount(
                name = name,
                count = count,
                percentage = String.format(java.util.Locale.US, "%.1f%%", (count.toFloat() / totalRows) * 100f)
            )
        }.sortedByDescending { it.count }

        val perfList = performanceCounts.map { (rating, count) ->
            PerformanceCount(
                rating = rating,
                count = count,
                percentage = String.format(java.util.Locale.US, "%.1f%%", (count.toFloat() / totalRows) * 100f)
            )
        }

        val dateList = dateCounts.map { (date, count) ->
            DateTrendCount(date = date, count = count)
        }.sortedBy { 
            try {
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).parse(it.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        val aggregated = AggregatedData(
            totalRowsParsed = totalRows,
            deptDistribution = deptList,
            performanceMetrics = perfList,
            dateTrend = dateList,
            anomalies = anomalies
        )

        return Pair(aggregated, cachedRecords)
    }
}
