package com.crystall.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.widget.RemoteViews
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.crystall.music.R
import com.crystall.music.player.MusicPlayerManager
import com.crystall.music.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 1:1 YouTube Music Style Home Screen App Widget
 */
class CrystallMusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.crystall.music.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.crystall.music.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.crystall.music.widget.ACTION_PREV"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CrystallMusicWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (widgetIds.isEmpty()) return

            val playerManager = MusicPlayerManager.getInstance(context)
            val currentTrack = playerManager.currentTrack.value
            val isPlaying = playerManager.isPlaying.value

            widgetScope.launch {
                var coverBitmap: Bitmap? = null
                if (currentTrack != null && currentTrack.coverUrl.isNotBlank()) {
                    try {
                        val imageLoader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(currentTrack.coverUrl)
                            .size(160, 160)
                            .allowHardware(false)
                            .build()
                        val result = imageLoader.execute(request)
                        if (result is SuccessResult) {
                            coverBitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        }
                    } catch (_: Exception) {}
                }

                for (id in widgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_music_player)

                    // Open App on widget click
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                    // Transport Actions
                    val prevIntent = Intent(context, CrystallMusicWidgetProvider::class.java).apply {
                        action = ACTION_PREV
                    }
                    val prevPendingIntent = PendingIntent.getBroadcast(
                        context,
                        1,
                        prevIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

                    val playPauseIntent = Intent(context, CrystallMusicWidgetProvider::class.java).apply {
                        action = ACTION_PLAY_PAUSE
                    }
                    val playPausePendingIntent = PendingIntent.getBroadcast(
                        context,
                        2,
                        playPauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

                    val nextIntent = Intent(context, CrystallMusicWidgetProvider::class.java).apply {
                        action = ACTION_NEXT
                    }
                    val nextPendingIntent = PendingIntent.getBroadcast(
                        context,
                        3,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

                    // Content
                    if (currentTrack != null) {
                        views.setTextViewText(R.id.widget_title, currentTrack.title)
                        views.setTextViewText(R.id.widget_artist, currentTrack.artist)
                    } else {
                        views.setTextViewText(R.id.widget_title, "Crystall Music")
                        views.setTextViewText(R.id.widget_artist, "Включите музыку")
                    }

                    // Play/Pause icon
                    val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                    views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

                    // Artwork
                    if (coverBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_cover, coverBitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_cover, R.drawable.ic_crystall_logo)
                    }

                    appWidgetManager.updateAppWidget(id, views)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val playerManager = MusicPlayerManager.getInstance(context)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                playerManager.togglePlayPause()
                updateAllWidgets(context)
            }
            ACTION_NEXT -> {
                playerManager.skipNext()
                updateAllWidgets(context)
            }
            ACTION_PREV -> {
                playerManager.skipPrevious()
                updateAllWidgets(context)
            }
        }
    }
}
