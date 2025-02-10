package io.github.lumkit.tweak.common.util

import android.annotation.SuppressLint
import kotlinx.datetime.LocalDateTime

@SuppressLint("DefaultLocale")
fun LocalDateTime.format(): String {
    return buildString {
        append(
            String.format(
                "%04d-%02d-%02d %02d:%02d:%02d",
                year,
                monthNumber,
                dayOfMonth,
                hour,
                minute,
                second
            )
        )
    }
}