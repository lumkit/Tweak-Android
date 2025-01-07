package io.github.lumkit.tweak.ui.component

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lumkit.tweak.data.ChartState
import io.github.lumkit.tweak.ui.local.json
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.encodeToString
import java.util.UUID
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue

private val chartSaver = object : Saver<LinkedBlockingQueue<Float>, String> {
    override fun restore(value: String): LinkedBlockingQueue<Float> {
        return json.decodeFromString(value)
    }

    override fun SaverScope.save(value: LinkedBlockingQueue<Float>): String {
        return json.encodeToString(value)
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
    var loadHistory by rememberSaveable { mutableStateOf(LinkedBlockingDeque<Float>()) }
    var chartCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        val newMap = LinkedBlockingDeque(loadHistory)
        if (newMap.size <= chartCount) {
            for (i in 0 until chartCount - newMap.size) {
                newMap.addFirst(0f)
            }
        }
        newMap.put((state.progress * 100f).bounds(0f, 100f))
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
            val top = ((100f - ratio) * height / 100f).coerceIn(0f, height - 2.dp.toPx())
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