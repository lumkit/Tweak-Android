package io.github.lumkit.tweak.data

import kotlinx.serialization.Serializable

@Serializable
enum class ClientType {
    Windows, Android
}

fun Int.asClientType() : ClientType = ClientType.entries[this]