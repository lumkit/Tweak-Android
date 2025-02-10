package io.github.lumkit.tweak.util

import android.content.Context

object Aes {
    init {
        System.loadLibrary("libtweak")
    }
    external fun encrypt(context: Context, plainText: String): String?
    external fun decrypt(context: Context, cipherText: String): String?
}