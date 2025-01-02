package io.github.lumkit.tweak.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

@Stable
class ChartState(
    val progress: Float,
    val uuid: String = UUID.randomUUID().toString(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChartState) return false
        if (uuid != other.uuid) return false
        return true
    }

    override fun hashCode(): Int {
        var result = progress.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    override fun toString(): String {
        return "ChartState(progress=$progress)"
    }
}

internal fun Float.bounds(min: Float, max: Float) = if (this >= max) max else if (this <= min) min else this

@SuppressLint("MutableCollectionMutableState")
@Composable
fun LintStackChart(
    modifier: Modifier = Modifier,
    state: ChartState,
    chartWith: Dp = 10.dp,
    defaultColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = .125f),
    midColor: Color = Color(0xFFFC8A1B),
    highColor: Color = Color(0xFFF9592F),
) {
    var loadHistory by remember { mutableStateOf(LinkedBlockingQueue<Float>()) }
    var chartCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        val newMap = LinkedBlockingQueue(loadHistory)
        if (newMap.size <= 0) {
            for (i in 0 until chartCount - 1) {
                newMap.add(0f)
            }
        }
        newMap.put((state.progress).bounds(0f, 100f))
        if (newMap.size > chartCount) {
            for (i in chartCount until newMap.size) {
                newMap.poll()
            }
        }
        loadHistory = newMap
    }

    Canvas(
        modifier = modifier
    ) {
        val width = this.drawContext.size.width
        val height = this.drawContext.size.height
        val chartWithPx = chartWith.toPx()
        chartCount = (width / chartWithPx).toInt()

        val space = (width - chartCount * chartWithPx) / 2f

        var index = 0
        loadHistory.forEach { ratio ->
            var chartColor = if (ratio > 85f) {
                highColor
            } else if (ratio > 65f) {
                midColor
            } else {
                defaultColor
            }
            chartColor = if (ratio > 50f) {
                chartColor.copy(alpha = 1f)
            } else {
                val fl = 0.5f + (ratio / 100.0f)
                chartColor.copy(alpha = if (fl <= 0f) 0f else if (fl >= 1f) 1f else fl)
            }
            val top = if (ratio <= 2f) {
                height - 10f
            } else if (ratio >= 98f) {
                0f
            } else {
                (100f - ratio) * height / 100f
            }
            drawRoundRect(
                color = chartColor,
                topLeft = Offset(x = chartWithPx * index + space, y = top),
                size = Size(chartWithPx * .9f, height - top),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
            index++
        }
    }
}