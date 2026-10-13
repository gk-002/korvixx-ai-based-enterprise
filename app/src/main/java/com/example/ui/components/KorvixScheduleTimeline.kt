package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class ScheduleEvent(
    val time: String,
    val title: String,
    val subtitle: String,
    val color: Color,
    val bgLight: Color
)

@Composable
fun KorvixScheduleTimeline(
    modifier: Modifier = Modifier
) {
    val events = listOf(
        ScheduleEvent("09:00 AM", "Morning Standup", "Room 402 - Tech Team", KorvixPrimary, KorvixPrimaryLight),
        ScheduleEvent("11:00 AM", "Board Executive Sync", "Main Conference Hall", KorvixBlue, KorvixBlueLight),
        ScheduleEvent("02:00 PM", "Candidate Interview (HR)", "Online - Google Meet", KorvixOrange, KorvixOrangeLight),
        ScheduleEvent("04:30 PM", "Q3 Document Approval", "Office of the Secretary", KorvixGreen, KorvixGreenLight)
    )

    Column(
        modifier = modifier
            .background(KorvixGlassSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Today's Schedule",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = KorvixTextDark,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val hours = listOf(
                "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
                "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
            )

            hours.forEach { hour ->
                val matchingEvent = events.find { it.time == hour }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Time Label
                    Text(
                        text = hour,
                        fontSize = 11.sp,
                        color = KorvixTextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .width(70.dp)
                            .padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Timeline line and Node / Bubble
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (matchingEvent != null) 65.dp else 30.dp)
                    ) {
                        // Vertical guideline
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .background(KorvixBorder)
                                .align(Alignment.CenterStart)
                        )

                        if (matchingEvent != null) {
                            // Overlay appointment bubble
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp)
                                    .background(matchingEvent.bgLight, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left accent bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(matchingEvent.color, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = matchingEvent.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KorvixTextDark
                                    )
                                    Text(
                                        text = matchingEvent.subtitle,
                                        fontSize = 10.sp,
                                        color = KorvixTextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            // Blank guideline node indicator
                            Box(
                                modifier = Modifier
                                    .padding(start = 0.dp)
                                    .size(6.dp)
                                    .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                    .align(Alignment.TopStart)
                            )
                        }
                    }
                }
            }
        }
    }
}
