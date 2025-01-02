package io.github.lumkit.tweak.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos

@Composable
@Preview
private fun PreviewIndicator() {
    Column {
        CircleIndicator(
            circleDiameter = 95.dp,
            backgroundIndicatorStrokeWidth = 4.dp,
            progress = 1f
        )
        HorizontalIndicator(
            progress = .5f,
            modifier = Modifier.size(95.dp, 8.dp)
        )
    }
}

val midColor: Color = Color(0xFFFC8A1B).copy(alpha = .75f)
val highColor: Color = Color(0xFFF9592F).copy(alpha = .75f)

/**
 * 计算弦高（Sagitta）
 *
 * @param radius 圆的半径
 * @param sweepAngleDegrees 扫过角度（度）
 * @return 弦高距离
 */
fun calculateSagitta(radius: Float, sweepAngleDegrees: Float): Float {
    // 将角度转换为弧度
    val sweepAngleRadians = Math.toRadians(sweepAngleDegrees.toDouble())
    // 使用弧度计算弦高
    return (radius * (1 - cos(sweepAngleRadians / 2))).toFloat()
}

@Composable
fun CircleIndicator(
    circleDiameter: Dp,
    progress: Float,
    angle: Float = 240f,
    backgroundIndicatorColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
    backgroundIndicatorStrokeWidth: Dp = 8.dp,
    foregroundColor: Color = MaterialTheme.colorScheme.primary,
    text: @Composable (ColumnScope) -> Unit = {}
) {

    val density = LocalDensity.current

    val targetIndicatorValue by animateFloatAsState(
        targetValue = if (progress <= .005f) .5f else progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val indicatorColor by animateColorAsState(
        targetValue = when (progress) {
            in .5f .. .80f -> midColor
            in .80f .. 1f -> highColor
            else -> foregroundColor
        }
    )

    val currentAngle by remember {
        derivedStateOf { angle }
    }

    val sagitta by remember {
        derivedStateOf {
            with(density) {
                calculateSagitta(
                    (circleDiameter / 2f).toPx(),
                    angle
                ).toDp()
            }
        }
    }

    val startAngle by remember {
        derivedStateOf {
            val gapAngleDegrees = 360f - currentAngle
            90f + (gapAngleDegrees / 2f)
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(circleDiameter)
            .height(sagitta + backgroundIndicatorStrokeWidth / 2f)
            .drawBehind {
                val think = backgroundIndicatorStrokeWidth.toPx()
                val padding = think / 2f
                drawBackgroundIndicator(
                    padding,
                    startAngle,
                    currentAngle,
                    backgroundIndicatorColor,
                    think,
                    sagitta,
                )
                drawForegroundIndicator(
                    padding,
                    startAngle,
                    currentAngle,
                    targetIndicatorValue,
                    indicatorColor,
                    think,
                    sagitta
                )
            }
    ) {
        Spacer(
            modifier = Modifier.height((circleDiameter - sagitta) / 2)
        )
        text(this)
    }
}

@Composable
fun HorizontalIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    cornerRadius: Dp = 4.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
) {

    val progressWidthAnimation by animateFloatAsState(
        targetValue = if (progress <= .005f) .5f else progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val indicatorColor by animateColorAsState(
        targetValue = when (progress) {
            in .5f .. .80f -> midColor
            in .80f .. 1f -> highColor
            else -> color
        }
    )

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        modifier = modifier
            .height(cornerRadius * 2)
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithContent {
                drawContent()
                val progressWidth = drawContext.size.width * progressWidthAnimation
                drawRoundRect(
                    color = indicatorColor,
                    size = drawContext.size.copy(width = progressWidth),
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            },
        content = {}
    )
}

private fun DrawScope.drawBackgroundIndicator(
    padding: Float,
    startAngle: Float,
    currentAngle: Float,
    backgroundIndicatorColor: Color,
    think: Float,
    sagitta: Dp
) {
    val rect = Rect(
        left = padding,
        top = padding,
        right = size.width - padding,
        bottom = size.height - padding + (size.width - sagitta.toPx()) - think / 2f
    )

    val path = Path().apply {
        arcTo(
            rect = rect,
            startAngleDegrees = startAngle,
            sweepAngleDegrees = currentAngle,
            forceMoveTo = false
        )
    }

    drawPath(
        path = path,
        color = backgroundIndicatorColor,
        style = Stroke(
            width = think,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}


private fun DrawScope.drawForegroundIndicator(
    padding: Float,
    startAngle: Float,
    currentAngle: Float,
    progress: Float,
    foregroundColor: Color,
    think: Float,
    sagitta: Dp,
) {
    val rect = Rect(
        left = padding,
        top = padding,
        right = size.width - padding,
        bottom = size.height - padding + (size.width - sagitta.toPx()) - think / 2f
    )

    val path = Path().apply {
        arcTo(
            rect = rect,
            startAngleDegrees = startAngle,
            sweepAngleDegrees = currentAngle * progress,
            forceMoveTo = false
        )
    }

    drawPath(
        path = path,
        color = foregroundColor,
        style = Stroke(
            width = think,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}