package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KorvixPrimary
import com.example.ui.theme.KorvixSecondary
import com.example.ui.theme.KorvixTextDark
import com.example.ui.theme.KorvixTextMuted

// Slices representing chart segment
data class DonutSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun KorvixDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String = "Total",
    centerValue: String = "100%",
    selectedSliceLabel: String? = null,
    onSliceClick: ((String?) -> Unit)? = null
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val animateFloat = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
                .clickable(enabled = selectedSliceLabel != null) {
                    if (onSliceClick != null) onSliceClick(null)
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                var startAngle = -90f

                slices.forEach { slice ->
                    val sweepAngle = (slice.value / total) * 360f * animateFloat.value
                    val isSelected = selectedSliceLabel == slice.label
                    val currentStrokeWidth = if (selectedSliceLabel != null && isSelected) strokeWidth * 1.35f else strokeWidth
                    val opacity = if (selectedSliceLabel == null || isSelected) 1f else 0.35f

                    drawArc(
                        color = slice.color.copy(alpha = opacity),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (selectedSliceLabel != null) "Filtered By" else centerLabel,
                    fontSize = 10.sp,
                    color = KorvixTextMuted,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = if (selectedSliceLabel != null) {
                        if (selectedSliceLabel.length > 10) selectedSliceLabel.take(8) + ".." else selectedSliceLabel
                    } else centerValue,
                    fontSize = if (selectedSliceLabel != null) 13.sp else 16.sp,
                    color = KorvixPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (selectedSliceLabel != null) {
                    Text(
                        text = "Reset ✕",
                        fontSize = 9.sp,
                        color = KorvixTextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Legend
        Column(
            modifier = Modifier.padding(start = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            slices.forEach { slice ->
                val percentage = if (total > 0) (slice.value / total * 100).toInt() else 0
                val isSelected = selectedSliceLabel == slice.label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (onSliceClick != null) {
                                if (isSelected) {
                                    onSliceClick(null)
                                } else {
                                    onSliceClick(slice.label)
                                }
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = slice.color.copy(alpha = if (selectedSliceLabel == null || isSelected) 1f else 0.35f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${slice.label} ($percentage%)",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) KorvixPrimary else KorvixTextDark,
                        modifier = Modifier.alpha(if (selectedSliceLabel == null || isSelected) 1f else 0.5f)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = KorvixPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KorvixLineChart(
    dataPoints: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    fillColor: Color = KorvixSecondary.copy(alpha = 0.15f),
    lineColor: Color = KorvixPrimary
) {
    val animateFloat = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            val maxVal = (dataPoints.maxOrNull() ?: 100f).coerceAtLeast(1f)
            val paddingLeft = 30.dp.toPx()
            val paddingBottom = 20.dp.toPx()
            val chartWidth = size.width - paddingLeft
            val chartHeight = size.height - paddingBottom

            val points = dataPoints.mapIndexed { index, value ->
                val x = paddingLeft + (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * chartWidth
                val normalizedY = (value / maxVal) * animateFloat.value
                val y = chartHeight - (normalizedY * chartHeight)
                Offset(x, y)
            }

            // Draw grid lines
            for (i in 0..3) {
                val yGrid = chartHeight - (i.toFloat() / 3f) * chartHeight
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(paddingLeft, yGrid),
                    end = Offset(size.width, yGrid),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw line area gradient (if there are points)
            if (points.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(points.first().x, chartHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, chartHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(fillColor, Color.Transparent),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // Draw main line path
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots
                points.forEach { point ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 2.5.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        // Draw horizontal labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = KorvixTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun KorvixHorizontalBarChart(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = KorvixPrimary
) {
    val maxVal = (items.maxOfOrNull { it.second } ?: 100).coerceAtLeast(1)
    val animateFloat = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (label, value) ->
            val percentage = (value.toFloat() / maxVal.toFloat()) * animateFloat.value

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KorvixTextDark
                    )
                    Text(
                        text = "$value",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.LightGray.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = percentage.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(barColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
