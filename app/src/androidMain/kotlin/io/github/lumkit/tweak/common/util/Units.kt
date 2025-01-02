package io.github.lumkit.tweak.common.util

import android.annotation.SuppressLint
import kotlin.math.ln
import kotlin.math.pow

private val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")

@SuppressLint("DefaultLocale")
fun Long.autoUnit(showUnit: Boolean = true, decimalDigits: Int = 2): String {
    if (this < 1024) {
        val unit = if (showUnit) {
            " B"
        } else {
            ""
        }
        return "$this$unit"
    }
    val exp = (ln(this.toDouble()) / ln(1024.0)).toInt()
    val value = this / 1024.0.pow(exp.toDouble())
    return String.format("%.${decimalDigits}f%s", value, if (showUnit) " ${units[exp]}" else "")
}