package io.github.lumkit.tweak.common.util

import android.annotation.SuppressLint
import java.util.concurrent.TimeUnit

/**
 * 格式化已开机时间为可读的字符串
 * @return 格式化后的字符串，例如 "2小时15分钟30秒"
 */
@SuppressLint("DefaultLocale")
fun Long.formatUptime(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}