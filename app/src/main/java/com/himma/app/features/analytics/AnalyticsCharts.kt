package com.himma.app.features.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.himma.app.R
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.MuscleVolumeDistribution
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class ChartDataPoint(
    val timestamp: Long,
    val displayValue: Double,
)

@Composable
fun TrendLineChart(
    points: List<ChartDataPoint>,
    unitSuffix: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
) {
    if (points.isEmpty()) return

    val values = points.map { it.displayValue }
    val rawMin = values.min()
    val rawMax = values.max()
    val paddingRange = if (rawMax == rawMin) (rawMax * 0.1).coerceAtLeast(2.0) else (rawMax - rawMin) * 0.18
    val chartMin = (rawMin - paddingRange).coerceAtLeast(0.0)
    val chartMax = rawMax + paddingRange
    val valueSpan = (chartMax - chartMin).coerceAtLeast(1.0)

    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val startDate = Instant.ofEpochMilli(points.first().timestamp).atZone(ZoneId.systemDefault()).format(dateFormatter)
    val endDate = Instant.ofEpochMilli(points.last().timestamp).atZone(ZoneId.systemDefault()).format(dateFormatter)

    val minText = "${((rawMin * 10.0).roundToInt() / 10.0)} $unitSuffix"
    val maxText = "${((rawMax * 10.0).roundToInt() / 10.0)} $unitSuffix"
    val latestText = "${((values.last() * 10.0).roundToInt() / 10.0)} $unitSuffix"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Max: $maxText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = latestText,
                style = MaterialTheme.typography.titleMedium,
                color = lineColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Min: $minText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Charts always flow chronologically Left -> Right even in RTL locales
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(vertical = 8.dp),
            ) {
                val width = size.width
                val height = size.height
                val horizontalPad = 14.dp.toPx()
                val usableWidth = (width - horizontalPad * 2).coerceAtLeast(1f)

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                    val y = height * fraction
                    drawLine(
                        color = gridColor,
                        start = Offset(horizontalPad, y),
                        end = Offset(width - horizontalPad, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                }

                val offsets = points.mapIndexed { idx, pt ->
                    val x = if (points.size == 1) {
                        width / 2f
                    } else {
                        horizontalPad + (idx.toFloat() / (points.size - 1)) * usableWidth
                    }
                    val normalizedY = ((pt.displayValue - chartMin) / valueSpan).toFloat()
                    val y = height - (normalizedY * height)
                    Offset(x, y)
                }

                if (offsets.size > 1) {
                    val linePath = Path().apply {
                        moveTo(offsets.first().x, offsets.first().y)
                        for (i in 1 until offsets.size) {
                            val prev = offsets[i - 1]
                            val curr = offsets[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(offsets.last().x, height)
                        lineTo(offsets.first().x, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.28f),
                                lineColor.copy(alpha = 0.01f),
                            ),
                        ),
                    )

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                offsets.forEachIndexed { idx, center ->
                    val isLast = idx == offsets.lastIndex
                    drawCircle(
                        color = lineColor,
                        radius = if (isLast) 5.5.dp.toPx() else 3.5.dp.toPx(),
                        center = center,
                    )
                    if (isLast) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.28f),
                            radius = 10.dp.toPx(),
                            center = center,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = startDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (points.size > 1) {
                Text(
                    text = endDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MuscleVolumeBarList(
    distributions: List<MuscleVolumeDistribution>,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        distributions.forEach { dist ->
            val labelRes = when (dist.muscleGroup) {
                MuscleGroup.CHEST -> R.string.muscle_chest
                MuscleGroup.BACK -> R.string.muscle_back
                MuscleGroup.LEGS -> R.string.muscle_legs
                MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
                MuscleGroup.ARMS -> R.string.muscle_arms
                MuscleGroup.CORE -> R.string.muscle_core
            }
            val label = stringResource(id = labelRes)
            val progress = (dist.percentageOfSets / 100.0).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${stringResource(id = R.string.analytics_sets_count, dist.completedSets)} (${dist.percentageOfSets.roundToInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
