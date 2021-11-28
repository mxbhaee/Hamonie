package com.hamonie.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.appthemehelper.util.VersionUtils
import com.hamonie.R
import com.hamonie.activities.base.AbsThemeActivity
import com.hamonie.appshortcuts.DynamicShortcutManager
import com.hamonie.databinding.ActivitySettingsBinding
import com.hamonie.extensions.applyToolbar
import com.hamonie.extensions.extra
import com.hamonie.extensions.findNavController
import com.hamonie.extensions.surfaceColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.ColorCallback

class SettingsActivity : AbsThemeActivity(), ColorCallback, OnThemeChangedListener {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setDrawUnderStatusBar()
        val mSavedInstanceState = extra<Bundle>(TAG).value ?: savedInstanceState
        super.onCreate(mSavedInstanceState)
        setLightStatusbarAuto(surfaceColor())
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
    }

    private fun setupToolbar() {
        applyToolbar(binding.toolbar)
        val navController: NavController = findNavController(R.id.contentFrame)
        navController.addOnDestinationChangedListener { _, _, _ ->
            binding.collapsingToolbarLayout.title =
                navController.currentDestination?.let { getStringFromDestination(it) }
        }
    }

    private fun getStringFromDestination(currentDestination: NavDestination): String {
        val idRes = when (currentDestination.id) {
            R.id.mainSettingsFragment -> R.string.action_settings
            R.id.audioSettings -> R.string.pref_header_audio
            R.id.imageSettingFragment -> R.string.pref_header_images
            R.id.notificationSettingsFragment -> R.string.notification
            R.id.nowPlayingSettingsFragment -> R.string.now_playing
            R.id.otherSettingsFragment -> R.string.others
            R.id.personalizeSettingsFragment -> R.string.personalize
            R.id.themeSettingsFragment -> R.string.general_settings_title
            R.id.aboutActivity -> R.string.action_about
            R.id.backup_fragment -> R.string.backup_restore_title
            else -> R.id.action_settings
        }
        return getString(idRes)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.contentFrame).navigateUp() || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun invoke(dialog: MaterialDialog, color: Int) {
        ThemeStore.editTheme(this).accentColor(color).commit()
        if (VersionUtils.hasNougatMR())
            DynamicShortcutManager(this).updateDynamicShortcuts()

        restart()
    }

    override fun onThemeValuesChanged() {
        restart()
    }

    private fun restart() {
        val savedInstanceState = Bundle().apply {
            onSaveInstanceState(this)
        }
        finish()
        val intent = Intent(this, this::class.java).putExtra(TAG, savedInstanceState)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        val TAG: String = SettingsActivity::class.java.simpleName
    }
}

interface OnThemeChangedListener {
    fun onThemeValuesChanged()
}
