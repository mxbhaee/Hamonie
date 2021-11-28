package com.hamonie.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.preference.TwoStatePreference
import com.hamonie.appthemehelper.common.prefs.supportv7.ATEListPreference
import com.hamonie.*

class PersonalizeSettingsFragment : AbsSettingsFragment() {

    override fun invalidateSettings() {
        val toggleFullScreen: TwoStatePreference? = findPreference(TOGGLE_FULL_SCREEN)
        toggleFullScreen?.setOnPreferenceChangeListener { _, _ ->
            restartActivity()
            true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_ui)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeArtistStyle: ATEListPreference? = findPreference(HOME_ARTIST_GRID_STYLE)
        homeArtistStyle?.setOnPreferenceChangeListener { preference, newValue ->
            setSummary(preference, newValue)
            true
        }
        val homeAlbumStyle: ATEListPreference? = findPreference(HOME_ALBUM_GRID_STYLE)
        homeAlbumStyle?.setOnPreferenceChangeListener { preference, newValue ->
            setSummary(preference, newValue)
            true
        }
        val tabTextMode: ATEListPreference? = findPreference(TAB_TEXT_MODE)
        tabTextMode?.setOnPreferenceChangeListener { prefs, newValue ->
            setSummary(prefs, newValue)
            true
        }
    }
}
