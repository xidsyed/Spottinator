package com.plcoding.spotifycloneyt.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.spotifycloneyt.exoplayer.MusicService
import com.plcoding.spotifycloneyt.exoplayer.MusicServiceConnection
import com.plcoding.spotifycloneyt.exoplayer.currentPlaybackPosition
import com.plcoding.spotifycloneyt.other.Constants.UPDATE_PLAYER_POSITION_INTERVAL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SongViewModel @ViewModelInject constructor(
    musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val playbackState = musicServiceConnection.playbackState

    private val _currSongDuration = MutableLiveData<Long>()
    val currSongDuration: LiveData<Long> = _currSongDuration

    private val _currPlayerPosition = MutableLiveData<Long>()
    val currPlayerPosition: LiveData<Long> = _currPlayerPosition

    init {
        updateCurrentPlayerPosition()
    }

    private fun updateCurrentPlayerPosition() {
        viewModelScope.launch {
            while (true) {
                val pos = playbackState.value?.currentPlaybackPosition?.minus(1800000L)
                if (currPlayerPosition.value != pos) {
                    _currPlayerPosition.postValue(pos)
                    _currSongDuration.postValue(MusicService.currSongDuration.minus(1800000L))
                }
                delay(UPDATE_PLAYER_POSITION_INTERVAL)
            }
        }
    }

}