package com.hamonie.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.text.HtmlCompat
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.appthemehelper.util.VersionUtils
import com.hamonie.activities.base.AbsMusicServiceActivity
import com.hamonie.databinding.ActivityPermissionBinding
import com.hamonie.extensions.accentBackgroundColor
import com.hamonie.extensions.show
import com.hamonie.util.RingtoneManager

class PermissionActivity : AbsMusicServiceActivity() {
    private lateinit var binding: ActivityPermissionBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusbarColorAuto()
        setTaskDescriptionColorAuto()
        setupTitle()

        binding.storagePermission.setButtonClick {
            requestPermissions()
        }
        if (VersionUtils.hasMarshmallow()) binding.audioPermission.show()
        binding.audioPermission.setButtonClick {
            if (RingtoneManager.requiresDialog(this@PermissionActivity)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivity(intent)
            }
        }
        binding.finish.accentBackgroundColor()
        binding.finish.setOnClickListener {
            if (hasPermissions()) {
                startActivity(
                    Intent(this, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                )
                finish()
            }
        }
    }

    private fun setupTitle() {
        val color = ThemeStore.accentColor(this)
        val hexColor = String.format("#%06X", 0xFFFFFF and color)
        val appName = HtmlCompat.fromHtml(
            "Hello there! <br>Welcome to <b>hamo<span  style='color:$hexColor';>nie</span style='color:$hexColor'> music<span> player</span></b>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        binding.appNameText.text = appName
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        if (hasStoragePermission()) {
            binding.storagePermission.checkImage.visibility = View.VISIBLE
            binding.storagePermission.checkImage.imageTintList =
                ColorStateList.valueOf(ThemeStore.accentColor(this))
        }
        if (hasAudioPermission()) {
            binding.audioPermission.checkImage.visibility = View.VISIBLE
            binding.audioPermission.checkImage.imageTintList =
                ColorStateList.valueOf(ThemeStore.accentColor(this))
        }

        super.onResume()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasStoragePermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasAudioPermission(): Boolean {
        return Settings.System.canWrite(this)
    }
}
