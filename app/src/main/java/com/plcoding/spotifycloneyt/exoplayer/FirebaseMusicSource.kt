package com.plcoding.spotifycloneyt.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.plcoding.spotifycloneyt.data.remote.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
){
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()
    private var state: State = State.STATE_CREATED
        set(value) {
            if (value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == State.STATE_INITIALIZED)
                    }
                }
            } else field = value
        }
    var songsMediaMetadata = emptyList<MediaMetadataCompat>()

    // fetch songsMediaMetadata from musicDatabase
    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        state = State.STATE_INITIALIZING
        val allSongs = musicDatabase.getSongs()
        songsMediaMetadata = allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                .putString(METADATA_KEY_ARTIST, song.artist)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.artist)
                .build()
        }
        state = State.STATE_INITIALIZED
    }

    // return a ConcatenatingMediaSource from songsMediaMetadata for ExoPlayer
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource{
        val concatenatingMediaSource =  ConcatenatingMediaSource()
        songsMediaMetadata.forEach {
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(it.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    // return a List<MediaItem> for MediaBrowserService
    fun asMediaItems() = songsMediaMetadata.map {
        // set song desc
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(it.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(it.description.title)
            .setSubtitle(it.description.subtitle)
            .setMediaId(it.description.mediaId)
            .setIconUri(it.description.iconUri)
            .build()
        // create media item
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }.toMutableList()

    fun whenReady(action: (Boolean) -> Unit): Boolean {
        return if (state != State.STATE_CREATED || state != State.STATE_INITIALIZING) {
            onReadyListeners += action
            false
        } else {
            action(state == State.STATE_INITIALIZED)
            true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}