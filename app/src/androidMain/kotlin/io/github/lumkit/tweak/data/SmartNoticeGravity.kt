package io.github.lumkit.tweak.data

import android.view.Gravity
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import kotlinx.serialization.Serializable

@Serializable
enum class SmartNoticeGravity(val gravity: Int) {
    Start(Gravity.START), Center(Gravity.CENTER), End(Gravity.END);

    fun asString(): String =
        when (this) {
            Start -> TweakApplication.application.getString(R.string.text_cutout_gravity_start)
            Center -> TweakApplication.application.getString(R.string.text_cutout_gravity_center)
            End -> TweakApplication.application.getString(R.string.text_cutout_gravity_end)
        }
}