package com.plcoding.spotifycloneyt.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlaybackPreparer
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlayerEventListener
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlayerNotificationListener
import com.plcoding.spotifycloneyt.other.Constants.MEDIA_ROOT_ID
import com.plcoding.spotifycloneyt.other.Constants.NETWORK_ERROR
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


const val SERVICE_LOG_TAG = "SpottinatorMusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var musicNotificationManager: MusicNotificationManager
    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    private var currPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInitialised = false

    var isInForeground = false

    companion object {
        var currSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // launch coroutine and send fetch request
        serviceScope.launch { firebaseMusicSource.fetchMediaData() }

        // create a PendingIntent for mediaSession
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        // create media session and set session token
        mediaSession = MediaSessionCompat(this, SERVICE_LOG_TAG)
        mediaSession.setSessionActivity(activityIntent)
        mediaSession.isActive = true
        sessionToken = mediaSession.sessionToken

        // setup notification manager
        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) { currSongDuration = exoPlayer.duration }

        // Create MediaSessionConnector's Playback Preparer
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource) {
            currPlayingSong = it
            preparePlayer(firebaseMusicSource.songsMediaMetadata, it, true)
        }

        // Create Media Session Connector
        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(musicPlaybackPreparer)
            setQueueNavigator(MusicQueueNavigator())
            setPlayer(exoPlayer)
        }

        // add player event listener to exoplayer
        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)

        // show Notification
        musicNotificationManager.showNotification(exoPlayer)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext.cancelChildren()
        exoPlayer.release()
        exoPlayer.removeListener(musicPlayerEventListener)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ) = BrowserRoot(MEDIA_ROOT_ID, null)

    // returns MutableList<MediaItem> to Controllers asking the service for the children of a parent media item
    // and prepares player with the songs list and the first song media item
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { isMusicSourceInitialised ->
                    if (isMusicSourceInitialised) {
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        Log.d("SONGLOG", "MusicService.onLoadChildren : ${firebaseMusicSource.songsMediaMetadata.size} songs  sent")
                        if (!isPlayerInitialised && firebaseMusicSource.songsMediaMetadata.isNotEmpty()) {
                            preparePlayer(
                                firebaseMusicSource.songsMediaMetadata,
                                firebaseMusicSource.songsMediaMetadata[0],
                                false
                            )
                            Log.d("SONGLOG", "onLoadChildren: player prepared")
                            isPlayerInitialised = true
                        }
                    } else {
                        mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }
                if (!resultsSent) result.detach()
            }
        }
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        currItem: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val currSongIndex = if (currPlayingSong == null) 0 else songs.indexOf(currItem)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            firebaseMusicSource.songsMediaMetadata[windowIndex].description
    }
}