package com.hamonie.activities.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.R
import com.google.android.material.snackbar.Snackbar

abstract class AbsBaseActivity : AbsThemeActivity() {
    private var hadPermissions: Boolean = false
    private lateinit var permissions: Array<String>
    private var permissionDeniedMessage: String? = null

    open fun getPermissionsToRequest(): Array<String> {
        return arrayOf()
    }

    protected fun setPermissionDeniedMessage(message: String) {
        permissionDeniedMessage = message
    }

    fun getPermissionDeniedMessage(): String {
        return if (permissionDeniedMessage == null) getString(R.string.permissions_denied) else permissionDeniedMessage!!
    }

    private val snackBarContainer: View
        get() = window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        permissions = getPermissionsToRequest()
        hadPermissions = hasPermissions()
        permissionDeniedMessage = null
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!hasPermissions()) {
            // requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermissions = hasPermissions()
        if (hasPermissions != hadPermissions) {
            hadPermissions = hasPermissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                onHasPermissionsChanged(hasPermissions)
            }
        }
    }

    protected open fun onHasPermissionsChanged(hasPermissions: Boolean) {
        // implemented by sub classes
        println(hasPermissions)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            showOverflowMenu()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showOverflowMenu() {
    }

    protected open fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST)
        }
    }

    protected fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@AbsBaseActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    ) {
                        // User has deny from permission dialog
                        Snackbar.make(
                            snackBarContainer,
                            permissionDeniedMessage!!,
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction(com.hamonie.R.string.action_grant) { requestPermissions() }
                            .setActionTextColor(ThemeStore.accentColor(this)).show()
                    } else {
                        // User has deny permission and checked never show permission dialog so you can redirect to Application settings page
                        Snackbar.make(
                            snackBarContainer,
                            permissionDeniedMessage!!,
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction(com.hamonie.R.string.action_settings) {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                this@AbsBaseActivity.packageName,
                                null
                            )
                            intent.data = uri
                            startActivity(intent)
                        }.setActionTextColor(ThemeStore.accentColor(this)).show()
                    }
                    return
                }
            }
            hadPermissions = true
            onHasPermissionsChanged(true)
        }
    }

    companion object {
        const val PERMISSION_REQUEST = 100
    }
    // this  lets keyboard close when clicked in backgroud
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
