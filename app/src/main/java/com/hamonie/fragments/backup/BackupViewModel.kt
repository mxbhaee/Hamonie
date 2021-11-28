package com.hamonie.fragments.backup

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hamonie.helper.BackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.system.exitProcess


class BackupViewModel : ViewModel() {
    private val backupsMutableLiveData = MutableLiveData<List<File>>()
    val backupsLiveData: LiveData<List<File>> = backupsMutableLiveData

    fun loadBackups() {
        File(BackupHelper.backupRootPath).listFiles { _, name ->
            return@listFiles name.endsWith(BackupHelper.BACKUP_EXTENSION)
        }?.toList()?.let {
            backupsMutableLiveData.value = it
        }
    }

    suspend fun restoreBackup(activity: Activity, inputStream: InputStream?) {
        BackupHelper.restoreBackup(activity, inputStream)
        withContext(Dispatchers.Main) {
            val intent = Intent(
                activity,
                activity::class.java
            )
            activity.startActivity(intent)
            exitProcess(0)
        }
    }
}