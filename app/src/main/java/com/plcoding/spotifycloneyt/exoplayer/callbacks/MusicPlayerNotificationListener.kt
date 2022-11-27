package com.plcoding.spotifycloneyt.exoplayer.callbacks

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.plcoding.spotifycloneyt.exoplayer.MusicService
import com.plcoding.spotifycloneyt.other.Constants.NOTIFICATION_ID

/** We use this class to start and stop foreground music service
 * A service being in foreground and having a notification is the same thing, cant have one without
 * the other. That is why `startForeground` takes a notification and stopForeground takes
 * `removeNotification` Boolean*/

class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {
    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        musicService.apply {
            stopForeground(true)
            isInForeground = false
            stopSelf()
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        musicService.apply {
            // not foreground, start foreground service
            if (ongoing && !isInForeground) {
                ContextCompat.startForegroundService(this, Intent(applicationContext, this::class.java))
                startForeground(NOTIFICATION_ID, notification)
                isInForeground = true
            }
        }
    }
}