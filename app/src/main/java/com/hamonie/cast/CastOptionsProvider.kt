package com.hamonie.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions
import java.util.*


class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val buttonActions: MutableList<String> = ArrayList()
        buttonActions.add(MediaIntentReceiver.ACTION_SKIP_PREV)
        buttonActions.add(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK)
        buttonActions.add(MediaIntentReceiver.ACTION_SKIP_NEXT)
        buttonActions.add(MediaIntentReceiver.ACTION_STOP_CASTING)
        val compatButtonActionsIndices = intArrayOf(1, 3)
        val notificationOptions = NotificationOptions.Builder()
            .setActions(buttonActions, compatButtonActionsIndices)
            .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context?): List<SessionProvider>? {
        return null
    }
}