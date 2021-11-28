package com.hamonie.appwidgets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.hamonie.App
import com.hamonie.R
import com.hamonie.activities.MainActivity
import com.hamonie.appwidgets.base.BaseAppWidget
import com.hamonie.service.MusicService
import com.hamonie.service.MusicService.*
import com.hamonie.util.PreferenceUtil
import com.hamonie.util.RetroUtil

class AppWidgetText : BaseAppWidget() {
    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(context.packageName, R.layout.app_widget_text)

        appWidgetView.setImageViewBitmap(
            R.id.button_next, createBitmap(
                RetroUtil.getTintedVectorDrawable(
                    context, R.drawable.ic_skip_next, ContextCompat.getColor(
                        context, R.color.md_white_1000
                    )
                )!!, 1f
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev, createBitmap(
                RetroUtil.getTintedVectorDrawable(
                    context, R.drawable.ic_skip_previous, ContextCompat.getColor(
                        context, R.color.md_white_1000
                    )
                )!!, 1f
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause, createBitmap(
                RetroUtil.getTintedVectorDrawable(
                    context, R.drawable.ic_play_arrow_white_32dp, ContextCompat.getColor(
                        context, R.color.md_white_1000
                    )
                )!!, 1f
            )
        )

        appWidgetView.setTextColor(
            R.id.title, ContextCompat.getColor(context, R.color.md_white_1000)
        )
        appWidgetView.setTextColor(
            R.id.text, ContextCompat.getColor(context, R.color.md_white_1000)
        )

        linkButtons(context, appWidgetView)
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    /**
     * Link up various button actions using [PendingIntent].
     */
    private fun linkButtons(context: Context, views: RemoteViews) {
        val action = Intent(context, MainActivity::class.java)
            .putExtra(
                MainActivity.EXPAND_PANEL,
                PreferenceUtil.isExpandPanel
            )

        val serviceName = ComponentName(context, MusicService::class.java)

        // Home
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.image, pendingIntent)
        views.setOnClickPendingIntent(R.id.media_titles, pendingIntent)

        // Previous track
        pendingIntent = buildPendingIntent(context, ACTION_REWIND, serviceName)
        views.setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, ACTION_TOGGLE_PAUSE, serviceName)
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, ACTION_SKIP, serviceName)
        views.setOnClickPendingIntent(R.id.button_next, pendingIntent)
    }

    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(service.packageName, R.layout.app_widget_text)

        val isPlaying = service.isPlaying
        val song = service.currentSong

        // Set the titles and artwork
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE)
            appWidgetView.setTextViewText(R.id.title, song.title)
            appWidgetView.setTextViewText(R.id.text, song.artistName)
        }
        // Link actions buttons to intents
        linkButtons(service, appWidgetView)

        // Set correct drawable for pause state
        val playPauseRes = if (isPlaying) R.drawable.ic_pause
        else R.drawable.ic_play_arrow_white_32dp
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause, createBitmap(
                RetroUtil.getTintedVectorDrawable(
                    App.getContext(), playPauseRes, ContextCompat.getColor(
                        App.getContext(), R.color.md_white_1000
                    )
                )!!, 1f
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_next, createBitmap(
                RetroUtil.getTintedVectorDrawable(
                    App.getContext(),
                    R.drawable.ic_skip_next,
                    ContextCompat.getColor(
                        App.getContext(), R.color.md_white_1000
                    )
                )!!, 1f
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev, createBitmap(
                RetroUtil.getTintedVectorDrawable(
                    App.getContext(),
                    R.drawable.ic_skip_previous,
                    ContextCompat.getColor(
                        App.getContext(), R.color.md_white_1000
                    )
                )!!, 1f
            )
        )

        pushUpdate(service.applicationContext, appWidgetIds, appWidgetView)
    }

    companion object {

        const val NAME: String = "app_widget_text"

        private var mInstance: AppWidgetText? = null

        val instance: AppWidgetText
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetText()
                }
                return mInstance!!
            }
    }
}
