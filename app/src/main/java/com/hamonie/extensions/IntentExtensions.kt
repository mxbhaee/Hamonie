package com.hamonie.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import java.io.File
import java.io.OutputStream

fun Fragment.createNewFile(
    mimeType: String,
    fileName: String,
    write: (outputStream: OutputStream?, data: Uri?) -> Unit
) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = mimeType
    intent.putExtra(Intent.EXTRA_TITLE, fileName)
    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val outputStream: OutputStream? =
                    context?.contentResolver?.openOutputStream(result.data?.data!!)
                write(outputStream, result.data?.data)
            }

        }
    startForResult.launch(intent)
}