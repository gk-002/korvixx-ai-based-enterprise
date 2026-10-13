package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
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

object CSVProcessor {

    /**
     * Generates a mock CSV with 10,000 records of enterprise data.
     * Intentionally injects missing data patterns and critical anomalies for AI detection.
     */
    fun generateMockCSVStream(): InputStream {
        val departments = listOf("Engineering", "Sales", "HR", "Finance", "Marketing", "Legal")
        val performanceLevels = listOf("Excellent", "Very Good", "Good", "Needs Improvement", "Unsatisfactory")
        val baseDate = 1715817600000L // 16 May 2024
        val msInDay = 86400000L

        val stringBuilder = java.lang.StringBuilder()
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

        return stringBuilder.toString().byteInputStream()
    }

    /**
     * Parse and aggregate 10,000+ records in the background without blocking the UI thread.
     * Identifies critical anomalies and missing data patterns.
     */
    suspend fun parseAndAggregate(inputStream: InputStream, onProgress: (Float) -> Unit = {}): AggregatedData = withContext(Dispatchers.Default) {
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

        // Map to track performance rating counts per department (for anomaly detection)
        val deptPerfCounts = mutableMapOf<String, MutableMap<String, Int>>()

        // Read all lines
        while (line != null) {
            line = reader.readLine() ?: break
            totalRows++

            val columns = line.split(",")
            if (columns.size < 3) {
                corruptRowsCount++
                continue
            }

            val dept = columns[0].trim()
            val date = columns[1].trim()
            val perf = columns[2].trim()

            var hasMissingData = false

            if (dept.isEmpty()) {
                missingDeptCount++
                hasMissingData = true
            }
            if (date.isEmpty()) {
                missingDateCount++
                hasMissingData = true
            }
            if (perf.isEmpty()) {
                missingPerfCount++
                hasMissingData = true
            }

            if (!hasMissingData) {
                // Aggregators for valid/partially valid rows
                deptCounts[dept] = (deptCounts[dept] ?: 0) + 1
                if (performanceCounts.containsKey(perf)) {
                    performanceCounts[perf] = performanceCounts[perf]!! + 1
                } else {
                    performanceCounts[perf] = (performanceCounts[perf] ?: 0) + 1
                }
                dateCounts[date] = (dateCounts[date] ?: 0) + 1

                // Track performance levels per department
                val perfMap = deptPerfCounts.getOrPut(dept) { mutableMapOf() }
                perfMap[perf] = (perfMap[perf] ?: 0) + 1
            }

            // Simulate slight streaming progress chunking so the UI feels alive
            if (totalRows % 2000 == 0) {
                val progress = totalRows.toFloat() / 10000f
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }
                delay(80) // Visual progress pacing
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
                    val percentageStr = String.format("%.1f%%", ratio * 100f)
                    anomalies.add("🔥 Critical Performance Anomaly: '$dept' department exhibits an unusual 'Unsatisfactory' rating concentration of $percentageStr.")
                }
            }
        }

        // Package results
        val deptList = deptCounts.map { (name, count) ->
            DeptCount(
                name = name,
                count = count,
                percentage = String.format("%.1f%%", (count.toFloat() / totalRows) * 100f)
            )
        }.sortedByDescending { it.count }

        val perfList = performanceCounts.map { (rating, count) ->
            PerformanceCount(
                rating = rating,
                count = count,
                percentage = String.format("%.1f%%", (count.toFloat() / totalRows) * 100f)
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

        AggregatedData(
            totalRowsParsed = totalRows,
            deptDistribution = deptList,
            performanceMetrics = perfList,
            dateTrend = dateList,
            anomalies = anomalies
        )
    }
}
