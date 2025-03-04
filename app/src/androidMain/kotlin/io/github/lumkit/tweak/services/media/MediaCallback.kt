package io.github.lumkit.tweak.services.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lumkit.tweak.services.SmartNoticeService

class MediaCallback(
    val mediaController: MediaController,
    private val context: SmartNoticeService,
) : MediaController.Callback() {

    private var metadata: MediaMetadata? = null
    var mediaStruct by mutableStateOf(
        MediaStruct()
    )

    init {
        if (mediaController.metadata != null && mediaController.playbackState != null) {
            metadata = mediaController.metadata
            mediaController.playbackState?.let {
                mediaStruct = mediaStruct.copy(
                    playbackState = it
                )
            }
            updateMediaStruct()
        }
        context.topMediaCallback.value = this
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        super.onMetadataChanged(metadata)
        this.metadata = metadata
        updateMediaStruct()
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        super.onPlaybackStateChanged(state)
        if (state == null) {
            return
        }
        when (state.state) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_PAUSED -> {
                context.topMediaCallback.value = this
            }

            else -> Unit
        }
        mediaStruct = mediaStruct.copy(
            playbackState = state
        )
        updateMediaStruct()
    }

    private fun updateMediaStruct() {
        mediaStruct = mediaStruct.copy(
            packageName = mediaController.packageName,
            title = (metadata?.getText(MediaMetadata.METADATA_KEY_TITLE) ?: "").toString(),
            artist = (metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST) ?: "").toString(),
            cover = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
            duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        )
        // TODO 获取歌词
    }

    override fun onSessionDestroyed() {
        super.onSessionDestroyed()
        context.callbackMap.value.forEach { (k, v) ->
            if (mediaController.packageName == k) {
                v.mediaController.unregisterCallback(v)
                val map = mutableMapOf<String, MediaCallback>()
                map.putAll(context.callbackMap.value)
                map.remove(k)
                context.callbackMap.value = map
            }
        }
        if (context.topMediaCallback.value?.mediaController?.packageName == mediaController.packageName) {
            context.topMediaCallback.value = null
        }
    }
}