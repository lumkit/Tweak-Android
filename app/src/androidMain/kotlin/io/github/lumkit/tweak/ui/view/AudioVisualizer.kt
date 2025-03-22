package io.github.lumkit.tweak.ui.view

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import io.github.lumkit.tweak.TweakApplication
import kotlin.math.min
import kotlin.random.Random

class AudioVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 假设 TweakApplication.density 是屏幕密度
    private val density = TweakApplication.density
    private val chartWidth = 4.dp        // 每个 bar 的宽度
    private val chartRadius = 2.dp       // bar 的圆角半径
    private val spacing = 1.dp           // bar 之间的间隔

    // 定义绘制 bar 的画笔（可自定义颜色）
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFFFFFFF".toColorInt()
    }

    // 当前 bar 的数量
    private var numBars: Int = 0

    // 存储每个 bar 的随机幅度（取值范围 [0, 1]）
    private var barAmplitudesCurrent: FloatArray = FloatArray(0)
    private var animatorSet: AnimatorSet? = null

    // 定时更新随机数据源
    private val updateRunnable = object : Runnable {
        override fun run() {
            animatorSet?.cancel()
            animatorSet = AnimatorSet()
            val animators = mutableListOf<ValueAnimator>()
            for ((index, progress) in barAmplitudesCurrent.withIndex()) {
                animators.add(
                    ValueAnimator.ofFloat(
                        progress,
                        Random.nextFloat()
                    ).apply {
                        addUpdateListener {
                            barAmplitudesCurrent[index] = it.animatedValue as Float
                            invalidate()
                        }
                    }
                )
            }
            animatorSet?.apply {
                interpolator = LinearInterpolator()
                duration = 150
                playTogether(animators.toList())
                start()
            }
            postDelayed(this, 150)
        }
    }

    fun start() {
        animatorSet?.cancel()
        removeCallbacks(updateRunnable)
        post(updateRunnable)
    }

    fun stop() {
        animatorSet?.cancel()
        removeCallbacks(updateRunnable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        with(density) {
            // 根据组件宽度计算 bar 数量，每个 bar 占用 (chartWidth + spacing)
            val totalBarUnit = chartWidth.roundToPx() + spacing.roundToPx()
            numBars = if (totalBarUnit > 0) w / totalBarUnit else 0
            // 初始化随机数据源数组
            barAmplitudesCurrent = FloatArray(numBars) { .1f }
            super.onSizeChanged(w, h, oldw, oldh)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        with(density) {
            val viewWidth = width
            val viewHeight = height
            if (viewWidth <= 0 || viewHeight <= 0 || numBars <= 0) return

            // 计算所有 bar 占用的总宽度
            val totalWidth = numBars * chartWidth.toPx() + (numBars - 1) * spacing.toPx()
            // 居中水平偏移量
            val horizontalOffset = (viewWidth - totalWidth) / 2f
            // 垂直中心点
            val centerY = viewHeight / 2f

            // 定义 bar 高度的范围：最低 10% 高度，最高 80% 高度
            val minBarHeight = viewHeight * 0.1f
            val maxBarHeight = viewHeight * 0.8f

            // 遍历绘制每个 bar
            for (i in 0 until numBars) {
                val amplitudeFactor = barAmplitudesCurrent[i]  // 随机值 [0,1]
                // 将随机幅度映射到 [minBarHeight, maxBarHeight]
                val barHeight = minBarHeight + amplitudeFactor * (maxBarHeight - minBarHeight)
                val left = horizontalOffset + i * (chartWidth.toPx() + spacing.toPx())
                val right = left + chartWidth.toPx()
                // 使 bar 在垂直方向居中显示
                val top = centerY - barHeight / 2f
                val bottom = centerY + barHeight / 2f
                // 绘制圆角矩形
                canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    chartRadius.toPx(),
                    chartRadius.toPx(),
                    barPaint.apply {
                        color = Color(0xFFFFFFFF).copy(
                            alpha = min(1f, amplitudeFactor + .5f),
                        ).toArgb()
                    }
                )
            }
        }
    }
}
