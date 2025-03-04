package io.github.lumkit.tweak.services.media

import android.graphics.Bitmap
import android.media.session.PlaybackState

data class MediaStruct(
    var packageName: String = "",
    var artist: String = "",
    var title: String = "",
    var cover: Bitmap? = null,
    var playbackState: PlaybackState = PlaybackState.Builder()
        .setState(PlaybackState.STATE_NONE, 0, 0f).build(),
    var duration: Long = 0L,
) {
    fun isPlaying(): Boolean {
        return playbackState.state == PlaybackState.STATE_PLAYING
    }
}