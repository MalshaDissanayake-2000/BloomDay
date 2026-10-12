package com.example.bloomday.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bloomday.MainActivity
import com.example.bloomday.R

class HydrationReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "bloomday_hydration_channel"
        private const val CHANNEL_NAME = "Hydration reminders"
        private const val CHANNEL_DESC = "Reminders to drink water"
        const val NOTIF_ID = 2001

        fun createChannelIfNeeded(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)
                val existing = nm.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    ch.description = CHANNEL_DESC
                    nm.createNotificationChannel(ch)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        createChannelIfNeeded(context)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingActivity = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar) // add icon resource or change to existing
            .setContentTitle("Hydration Reminder")
            .setContentText("Time to drink some water 💧")
            .setContentIntent(pendingActivity)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notif)
    }
}
