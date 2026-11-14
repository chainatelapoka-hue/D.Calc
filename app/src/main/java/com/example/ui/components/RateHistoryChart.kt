package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexoraAccentBlue
import com.example.ui.theme.NexoraMutedText
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RateHistoryChart(
    points: List<Pair<Long, Double>>,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = NexoraAccentBlue
    val mutedColor = NexoraMutedText
    val dateFormat = remember { SimpleDateFormat("dd MMM yy", Locale.US) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "LAST 1 YEAR (WEEKLY)",
            color = mutedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer8()

        when {
            loading && points.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), color = lineColor)
            }

            points.size < 2 -> Box(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ইতিহাস লোড করা যায়নি (ইন্টারনেট চেক করুন)",
                    color = mutedColor,
                    fontSize = 12.sp
                )
            }

            else -> {
                val values = points.map { it.second }
                val minV = values.minOrNull() ?: 0.0
                val maxV = values.maxOrNull() ?: 0.0
                val range = (maxV - minV).takeIf { it > 0 } ?: 1.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text("High: ${"%.4f".format(Locale.US, maxV)}", color = lineColor, fontSize = 11.sp)
                    Text("Low: ${"%.4f".format(Locale.US, minV)}", color = mutedColor, fontSize = 11.sp)
                }
                Spacer8()

                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / (points.size - 1)
                    val path = Path()
                    points.forEachIndexed { i, (_, v) ->
                        val x = i * stepX
                        val y = h - ((v - minV) / range * h).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path = path, color = lineColor, style = Stroke(width = 3f))
                }

                Spacer8()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(dateFormat.format(points.first().first), color = mutedColor, fontSize = 10.sp)
                    Text(dateFormat.format(points.last().first), color = mutedColor, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun Spacer8() = androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
