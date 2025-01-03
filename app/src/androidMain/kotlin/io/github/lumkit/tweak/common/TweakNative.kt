package io.github.lumkit.tweak.common

object TweakNative {
    init {
        System.loadLibrary("tweak-native")
    }

    external fun getKernelPropLong(path: String): Long
}