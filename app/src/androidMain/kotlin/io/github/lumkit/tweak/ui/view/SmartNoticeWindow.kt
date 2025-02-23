package io.github.lumkit.tweak.ui.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.WindowManager
import androidx.cardview.widget.CardView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class SmartNoticeWindow(
    context: Context,
    private val windowManager: WindowManager,
    private val windowLayoutParams: WindowManager.LayoutParams,
) : CardView(context) {

    companion object {
        var duration = 700L
    }

    private val density = Density(this.context)

    init {
        alpha = 0f
        with(density) {
            radius = 16.dp.toPx()
            setCardBackgroundColor("#FF000000".toColorInt())
            cardElevation = 0f

            windowLayoutParams.y = 10.dp.roundToPx()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun show() = with(density) {
        val animatorSet = AnimatorSet()

        val width = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "width",
            windowLayoutParams.width.toFloat().let { if (it < 0) 0f else it },
            100.dp.toPx()
        ).apply {
            addUpdateListener {
                windowLayoutParams.width = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {

                }
            }
        }
        val height = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "height",
            windowLayoutParams.height.toFloat().let { if (it < 0) 0f else it },
            32.dp.toPx()
        ).apply {
            addUpdateListener {
                windowLayoutParams.height = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {

                }
            }
        }

        val alpha = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "alpha",
            this@SmartNoticeWindow.alpha,
            1f,
        )

        animatorSet.playTogether(width, height, alpha)
        animatorSet.duration = duration

        animatorSet.start()
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun hide() {
        val animatorSet = AnimatorSet()

        val width = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "width",
            windowLayoutParams.width.toFloat().let { if (it < 0) 0f else it },
            0f
        ).apply {
            addUpdateListener {
                windowLayoutParams.width = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {

                }
            }
        }
        val height = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "height",
            windowLayoutParams.height.toFloat().let { if (it < 0) 0f else it }, 0f
        ).apply {
            addUpdateListener {
                windowLayoutParams.height = (it.animatedValue as Float).roundToInt()
                try {
                    windowManager.updateViewLayout(this@SmartNoticeWindow, windowLayoutParams)
                } catch (_: Exception) {

                }
            }
        }

        val alpha = ObjectAnimator.ofFloat(
            this@SmartNoticeWindow,
            "alpha",
            this.alpha,
            0f,
        )

        animatorSet.playTogether(width, height, alpha)
        animatorSet.duration = duration

        animatorSet.start()
    }


}