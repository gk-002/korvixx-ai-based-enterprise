package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class AttendanceStatus {
    PRESENT, ABSENT, LEAVE, NONE
}

@Composable
fun KorvixAttendanceCalendar(
    modifier: Modifier = Modifier
) {
    // Standard Month Attendance Grid (e.g., May 2025)
    // Keys represent day numbers 1 to 31. Initial states:
    val initialAttendance = remember {
        mutableStateMapOf<Int, AttendanceStatus>().apply {
            // Populate attendance status mapping for May 2025
            for (day in 1..31) {
                this[day] = when {
                    day in listOf(3, 4, 10, 11, 17, 18, 24, 25, 31) -> AttendanceStatus.NONE // Weekends
                    day in listOf(5, 12, 19) -> AttendanceStatus.LEAVE
                    day == 15 -> AttendanceStatus.ABSENT
                    day <= 23 -> AttendanceStatus.PRESENT
                    else -> AttendanceStatus.NONE // Future dates
                }
            }
        }
    }

    var selectedDay by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .background(KorvixGlassSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Month Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "May 2025",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = KorvixTextDark
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = KorvixTextMuted)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = KorvixTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekdays Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            weekDays.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = KorvixTextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Month Grid Layout (May 2025 starts on a Thursday - 3 lead days empty)
        val totalCells = 35 // 5 weeks * 7 days
        val leadEmptyCells = 3 // Thursday is index 3 (0-based: Mon=0, Tue=1, Wed=2, Thu=3)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadEmptyCells + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..31) {
                                val status = initialAttendance[dayNumber] ?: AttendanceStatus.NONE
                                val isSelected = selectedDay == dayNumber

                                // Background coloring based on attendance status
                                val (bg, textCol) = when (status) {
                                    AttendanceStatus.PRESENT -> KorvixGreenLight to KorvixGreen
                                    AttendanceStatus.ABSENT -> KorvixRedLight to KorvixRed
                                    AttendanceStatus.LEAVE -> KorvixOrangeLight to KorvixOrange
                                    else -> Color.Transparent to KorvixTextDark
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) KorvixPrimary.copy(alpha = 0.15f) else bg,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedDay = dayNumber
                                            // Interactive toggling feature for live demonstration
                                            initialAttendance[dayNumber] = when (status) {
                                                AttendanceStatus.NONE -> AttendanceStatus.PRESENT
                                                AttendanceStatus.PRESENT -> AttendanceStatus.LEAVE
                                                AttendanceStatus.LEAVE -> AttendanceStatus.ABSENT
                                                AttendanceStatus.ABSENT -> AttendanceStatus.NONE
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || status != AttendanceStatus.NONE) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) KorvixPrimary else textCol
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend / Indicator Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = KorvixGreen, label = "Present")
            LegendItem(color = KorvixRed, label = "Absent")
            LegendItem(color = KorvixOrange, label = "Leave")
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = KorvixTextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}
