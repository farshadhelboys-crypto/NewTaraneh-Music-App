package com.newtaraneh.music.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.newtaraneh.music.data.BASE_URL
import com.newtaraneh.music.data.SongDto

object PlayerHolder {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    var queue: List<SongDto> = emptyList()
        private set
    var current: SongDto? = null
        private set

    fun connect(context: Context, onReady: () -> Unit = {}) {
        if (controller != null) {
            onReady(); return
        }
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = runCatching { future.get() }.getOrNull()
            onReady()
        }, MoreExecutors.directExecutor())
    }

    fun player(): Player? = controller

    fun playQueue(songs: List<SongDto>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        queue = songs
        val c = controller ?: return
        val items = songs.map { song ->
            val meta = MediaMetadata.Builder()
                .setTitle(song.title ?: "آهنگ")
                .setArtist(song.artist ?: "New Taraneh")
                .setArtworkUri(song.cover_url?.let { android.net.Uri.parse(it) })
                .build()
            MediaItem.Builder()
                .setUri("${BASE_URL}songs/${song.id}/stream")
                .setMediaId(song.id.toString())
                .setMediaMetadata(meta)
                .build()
        }
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0)
        current = songs.getOrNull(startIndex)
        c.prepare()
        c.play()
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(ms: Long) = controller?.seekTo(ms)
    fun seekBy(delta: Long) {
        val c = controller ?: return
        c.seekTo((c.currentPosition + delta).coerceAtLeast(0))
    }

    fun setRepeat(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        current = null
    }

    fun release(context: Context) {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }
}
