package com.hamonie.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.hamonie.appthemehelper.common.prefs.supportv7.ATEListPreference
import com.hamonie.LANGUAGE_NAME
import com.hamonie.LAST_ADDED_CUTOFF
import com.hamonie.R
import com.hamonie.fragments.LibraryViewModel
import com.hamonie.fragments.ReloadType.HomeSections
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

/**
 * @author Hemanth S (h4h13).
 */

class OtherSettingsFragment : AbsSettingsFragment() {
    private val libraryViewModel by sharedViewModel<LibraryViewModel>()

    override fun invalidateSettings() {
        val languagePreference: ATEListPreference? = findPreference(LANGUAGE_NAME)
        languagePreference?.setOnPreferenceChangeListener { _, _ ->
            println("Invalidated")
            restartActivity()
            return@setOnPreferenceChangeListener true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_advanced)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val preference: Preference? = findPreference(LAST_ADDED_CUTOFF)
        preference?.setOnPreferenceChangeListener { lastAdded, newValue ->
            setSummary(lastAdded, newValue)
            libraryViewModel.forceReload(HomeSections)
            true
        }
        val languagePreference: Preference? = findPreference(LANGUAGE_NAME)
        languagePreference?.setOnPreferenceChangeListener { prefs, newValue ->
            setSummary(prefs, newValue)
            val code = newValue.toString()
            val manager = SplitInstallManagerFactory.create(requireContext())
            if (code != "auto") {
                // Try to download language resources
                val request =
                    SplitInstallRequest.newBuilder().addLanguage(Locale.forLanguageTag(code))
                        .build()
                manager.startInstall(request)
                    // Recreate the activity on download complete
                    .addOnCompleteListener {
                        restartActivity()
                    }
            } else {
                requireActivity().recreate()
            }
            true
        }
    }
}
