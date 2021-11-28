package com.hamonie.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import com.hamonie.R
import com.hamonie.activities.*
import com.hamonie.activities.bugreport.BugReportActivity
import com.hamonie.helper.MusicPlayerRemote.audioSessionId
import com.google.android.material.bottomsheet.BottomSheetBehavior

object NavigationUtil {
    fun bugReport(activity: Activity) {
        ActivityCompat.startActivity(
            activity,
            Intent(activity, BugReportActivity::class.java),
            null
        )
    }

    fun goToOpenSource(activity: Activity) {
        ActivityCompat.startActivity(activity, Intent(activity, LicenseActivity::class.java), null)
    }

    fun goToLyrics(activity: Activity) {
        if (activity !is MainActivity) return
        activity.apply {
            //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
            setBottomNavVisibility(false)
            if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
                collapsePanel()
            }

            findNavController(R.id.fragment_container).navigate(
                R.id.lyrics_fragment
            )
        }
    }

    fun goToProVersion(context: Context) {
        ActivityCompat.startActivity(context, Intent(context, PurchaseActivity::class.java), null)
    }

    fun goToSupportDevelopment(activity: Activity) {
        ActivityCompat.startActivity(
            activity, Intent(activity, SupportDevelopmentActivity::class.java), null
        )
    }

    fun gotoDriveMode(activity: Activity) {
        ActivityCompat.startActivity(
            activity,
            Intent(activity, DriveModeActivity::class.java),
            null
        )
    }

    fun gotoWhatNews(activity: Activity) {
        ActivityCompat.startActivity(activity, Intent(activity, WhatsNewActivity::class.java), null)
    }

    fun openEqualizer(activity: Activity) {
        stockEqualizer(activity)
    }

    private fun stockEqualizer(activity: Activity) {
        val sessionId = audioSessionId
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(
                activity, activity.resources.getString(R.string.no_audio_ID), Toast.LENGTH_LONG
            )
                .show()
        } else {
            try {
                val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                activity.startActivityForResult(effects, 0)
            } catch (notFound: ActivityNotFoundException) {
                Toast.makeText(
                    activity,
                    activity.resources.getString(R.string.no_equalizer),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }
}