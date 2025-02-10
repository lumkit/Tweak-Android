package io.github.lumkit.tweak.common.util

import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.Const

object AuthorizationUtils {

    private val shard = TweakApplication.shared

    fun load(): String? = shard.getString(Const.APP_AUTHORIZATION, null)
    fun save(token: String?) {
        val edit = shard.edit()
        if (token == null) {
            edit.remove(Const.APP_AUTHORIZATION).apply()
        } else {
            edit.putString(Const.APP_AUTHORIZATION, token).apply()
        }
    }
}