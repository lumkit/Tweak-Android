package io.github.lumkit.tweak.util

object Aes {
    init {
        System.loadLibrary("liblibtweak")
    }
    external fun encrypt(plainText: String): String?
    external fun decrypt(cipherText: String): String?
}