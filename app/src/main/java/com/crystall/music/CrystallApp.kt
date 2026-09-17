package com.crystall.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.crystall.music.downloader.MusicDownloadManager
import com.crystall.music.player.MusicPlayerManager

class CrystallApp : Application() {

    companion object {
        const val PLAYBACK_CHANNEL_ID = "playback_channel"
        const val DOWNLOAD_CHANNEL_ID = "download_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Pre-initialize singletons
        MusicPlayerManager.getInstance(this)
        MusicDownloadManager.getInstance(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val playbackChannel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Воспроизведение музыки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления плеера Crystall Music"
                setShowBadge(false)
            }

            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Загрузки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Прогресс загрузки треков"
            }

            notificationManager.createNotificationChannel(playbackChannel)
            notificationManager.createNotificationChannel(downloadChannel)
        }
    }
}
