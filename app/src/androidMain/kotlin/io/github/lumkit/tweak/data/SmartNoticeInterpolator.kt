package io.github.lumkit.tweak.data

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import kotlinx.serialization.Serializable

@Serializable
enum class SmartNoticeInterpolator {
    Linear, AccelerateDecelerate, Overshoot;

    fun asString(): String = when (this) {
        Linear -> TweakApplication.application.getString(
            R.string.text_smart_notice_animation_interpolator_linear
        )

        AccelerateDecelerate -> TweakApplication.application.getString(
            R.string.text_smart_notice_animation_interpolator_accelerate_decelerate
        )

        Overshoot -> TweakApplication.application.getString(
            R.string.text_smart_notice_animation_interpolator_overshoot
        )
    }
}

fun SmartNoticeInterpolator.asInterpolator(): Interpolator = when (this) {
    SmartNoticeInterpolator.Linear -> LinearInterpolator()
    SmartNoticeInterpolator.AccelerateDecelerate -> AccelerateDecelerateInterpolator()
    SmartNoticeInterpolator.Overshoot -> OvershootInterpolator(1.3f)
}