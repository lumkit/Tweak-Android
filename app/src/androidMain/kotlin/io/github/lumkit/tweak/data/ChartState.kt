package io.github.lumkit.tweak.data

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.util.UUID

@Immutable
@Serializable
class ChartState(
    @FloatRange(from = 0.0, to = 1.0) val progress: Float,
    val uuid: String = UUID.randomUUID().toString(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChartState) return false
        if (uuid != other.uuid) return false
        return true
    }

    override fun hashCode(): Int {
        var result = progress.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    override fun toString(): String {
        return "ChartState(progress=$progress)"
    }
}