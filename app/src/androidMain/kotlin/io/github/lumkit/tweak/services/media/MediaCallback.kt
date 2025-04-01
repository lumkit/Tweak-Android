package io.github.lumkit.tweak.services.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.github.lumkit.tweak.model.LiveData
import io.github.lumkit.tweak.ui.screen.notice.model.MusicPlugin

class MediaCallback(
    val mediaController: MediaController,
    private val plugin: MusicPlugin,
) : MediaController.Callback() {

    private var metadata: MediaMetadata? = null
    var struct by mutableStateOf(
        MediaStruct()
    )

    init {
        metadata = mediaController.metadata
        updateMediaStruct()
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        super.onMetadataChanged(metadata)
        this.metadata = metadata
        updateMediaStruct()
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        super.onPlaybackStateChanged(state)
        updateMediaStruct()
        when (state?.state) {
            PlaybackState.STATE_PLAYING -> {
                plugin.topMediaCallback.value = this
            }

            PlaybackState.STATE_BUFFERING -> {

            }

            PlaybackState.STATE_CONNECTING -> {

            }

            PlaybackState.STATE_ERROR -> {

            }

            PlaybackState.STATE_FAST_FORWARDING -> {

            }

            PlaybackState.STATE_NONE -> {

            }

            PlaybackState.STATE_PAUSED -> {

            }

            PlaybackState.STATE_REWINDING -> {

            }

            PlaybackState.STATE_SKIPPING_TO_NEXT -> {

            }

            PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> {

            }

            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> {

            }

            PlaybackState.STATE_STOPPED -> {

            }
        }
    }

    private fun updateMediaStruct() {
        struct = struct.copy(
            packageName = mediaController.packageName,
            title = (metadata?.getText(MediaMetadata.METADATA_KEY_TITLE) ?: "").toString(),
            artist = (metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST) ?: "").toString(),
            cover = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
            duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
        )

        mediaController.playbackState?.let {
            struct = struct.copy(
                playbackState = it
            )
        }
    }

    override fun onSessionDestroyed() {
        super.onSessionDestroyed()
        plugin.factory.minimize()
    }
}