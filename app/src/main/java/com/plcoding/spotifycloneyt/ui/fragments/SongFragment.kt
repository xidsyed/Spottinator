package com.plcoding.spotifycloneyt.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import com.plcoding.spotifycloneyt.R
import com.plcoding.spotifycloneyt.data.entities.Song
import com.plcoding.spotifycloneyt.exoplayer.isPlaying
import com.plcoding.spotifycloneyt.exoplayer.toSong
import com.plcoding.spotifycloneyt.other.Status
import com.plcoding.spotifycloneyt.ui.viewmodels.MainViewModel
import com.plcoding.spotifycloneyt.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_song.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment(R.layout.fragment_song) {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()

    private var playbackState: PlaybackStateCompat? = null

    private var currPlayingSong: Song? = null

    private var shouldUpdateSeekbar: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        subscribeToObservers()

        ivPlayPauseDetail.setOnClickListener {
            currPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }

        ivSkipPrevious.setOnClickListener {
            mainViewModel.skipToPreviousSong()
        }

        ivSkip.setOnClickListener {
            mainViewModel.skipToNextSong()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) setCurrPlayerTimeToTextView(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                shouldUpdateSeekbar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong().plus(1800000L))
                    shouldUpdateSeekbar = true
                }
            }
        })

    }

    private fun updateTitleAndSongImage(song: Song) {
        val title = "${song.title} - ${song.artist}"
        tvSongName.text = title
        glide.load(song.image_url).into(ivSongImage)
    }

    private fun subscribeToObservers() {
        // update to first song in list if navigated to songFragment with no playing songs
        mainViewModel.songList.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            if (currPlayingSong == null) {  // just launched fragment no song playing
                                if(songs.isNotEmpty()) {
                                    currPlayingSong = songs[0]
                                    updateTitleAndSongImage(songs[0])
                                }
                            } else {

                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.currPlayingSong.observe(viewLifecycleOwner) { mediaMetadata ->
            if (mediaMetadata == null) return@observe
            currPlayingSong = mediaMetadata.toSong()
            updateTitleAndSongImage(currPlayingSong!!)
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            playbackState = it
            ivPlayPauseDetail.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
            seekBar.progress = it?.position?.toInt() ?: 0
        }

        songViewModel.currPlayerPosition.observe(viewLifecycleOwner) {
            if (shouldUpdateSeekbar) {
                seekBar.progress = it.toInt()
                setCurrPlayerTimeToTextView(it)
            }
        }

        songViewModel.currSongDuration.observe(viewLifecycleOwner) { ms ->
            seekBar.max = ms.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            tvSongDuration.text = dateFormat.format(ms)
        }
    }

    private fun setCurrPlayerTimeToTextView(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        tvCurTime.text = dateFormat.format(ms)
    }
}