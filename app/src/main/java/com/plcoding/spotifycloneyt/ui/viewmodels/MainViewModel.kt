package com.plcoding.spotifycloneyt.ui.viewmodels

import android.annotation.SuppressLint
import android.media.MediaMetadata.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.plcoding.spotifycloneyt.data.entities.Song
import com.plcoding.spotifycloneyt.exoplayer.MusicServiceConnection
import com.plcoding.spotifycloneyt.exoplayer.isPlayEnabled
import com.plcoding.spotifycloneyt.exoplayer.isPlaying
import com.plcoding.spotifycloneyt.exoplayer.isPrepared
import com.plcoding.spotifycloneyt.other.Constants.MEDIA_ROOT_ID
import com.plcoding.spotifycloneyt.other.Resource


class MainViewModel @ViewModelInject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val currPlayingSong = musicServiceConnection.currPlayingSong
    val playbackState = musicServiceConnection.playbackState

    init {
        _mediaItems.postValue(Resource.loading(null))

        // we get the songs here to _mediaItems
        musicServiceConnection.subscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    Log.d(
                        "SONGLOG",
                        "MainViewModel.musicServiceConnection.subscribe callback onChildrenLoaded: ${children.size} children loaded"
                    )
                    val items = children.map {
                        Song(
                            it.mediaId!!,
                            it.description.title.toString(),
                            it.description.subtitle.toString(),
                            it.description.mediaUri.toString(),
                            it.description.iconUri.toString(),
                        )
                    }
                    _mediaItems.postValue(Resource.success(items))
                }
            })
    }

    fun skipToNextSong() = musicServiceConnection.transportControls.skipToNext()
    fun skipToPreviousSong() = musicServiceConnection.transportControls.skipToPrevious()
    fun seekTo(pos: Long) = musicServiceConnection.transportControls.seekTo(pos)

    @SuppressLint("LogNotTimber")
    fun playOrToggleSong(mediaItem: Song, toggle: Boolean = false) {
        val isPrepared = playbackState.value?.isPrepared ?: false
        Log.d("SONGLOG", "playOrToggleSong: $isPrepared")
        // if player is prepared and you want to play/toggle the song that is already playing/paused
        if (isPrepared && mediaItem.media_id ==
            currPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)
        ) {
            Log.d("SONGLOG", "playing same song ${mediaItem.media_id}")
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if (toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        } else { // play a new song
            Log.d(
                "SONGLOG",
                "playing new song ${mediaItem.media_id} not the curr playing song ${
                    currPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)
                }"
            )
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.media_id, null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
    }


}