package com.plcoding.spotifycloneyt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.RequestManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var glide: RequestManager
    private var id = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // create channel and set importance
        createNotificationChannel()


        val btn: MaterialButton = findViewById(R.id.btn)
        btn.setOnClickListener {
            displayNotification(id++, "Reminder", "You Forgot to do the cool thing today!")
        }
    }

    private fun displayNotification(id: Int, title: String, content: String) {
        //create PendingIntent for MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // set notification content
        var builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.drawable.exo_notification_small_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // set the tap action intent
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)    // removes notification when user taps

        // show notification
        with(NotificationManagerCompat.from(this)) {
            notify(id, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create Notification Channel on API 26+ Android 8 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminders"
            val descriptionText = "Reminders about the latest events and other notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH

            // create channel
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // register channel
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}