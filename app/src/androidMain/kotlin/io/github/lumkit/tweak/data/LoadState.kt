package io.github.lumkit.tweak.data

import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock

/**
 * 加载状态
 */
sealed class LoadState(
    open val message: String? = null,
    open val key: Long,
) {
    data class Loading(
        override val message: String? = null,
        override val key: Long = Clock.System.now().toEpochMilliseconds(),
    ): LoadState(message, key)

    data class Success(
        override val message: String? = null,
        override val key: Long = Clock.System.now().toEpochMilliseconds(),
    ): LoadState(message, key)

    data class Fail(
        override val message: String? = null,
        override val key: Long = Clock.System.now().toEpochMilliseconds(),
    ): LoadState(message, key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoadState) return false

        if (key != other.key) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}

/**
 * 快捷监听加载状态的变化
 */
suspend fun State<ConcurrentMap<String, LoadState>>.watch(id: String, block: suspend (LoadState?) -> Unit) {
    snapshotFlow { value[id] }
        .distinctUntilChanged()
        .collect {
            block(it)
        }
}