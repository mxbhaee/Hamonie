package com.hamonie.dialogs

import android.app.Dialog
import android.media.MediaScannerConnection
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.hamonie.appthemehelper.util.VersionUtils
import com.hamonie.App
import com.hamonie.EXTRA_PLAYLIST
import com.hamonie.R
import com.hamonie.db.PlaylistWithSongs
import com.hamonie.extensions.colorButtons
import com.hamonie.extensions.createNewFile
import com.hamonie.extensions.extraNotNull
import com.hamonie.extensions.materialDialog
import com.hamonie.helper.M3UWriter
import com.hamonie.util.PlaylistsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavePlaylistDialog : DialogFragment() {
    companion object {
        fun create(playlistWithSongs: PlaylistWithSongs): SavePlaylistDialog {
            return SavePlaylistDialog().apply {
                arguments = bundleOf(
                    EXTRA_PLAYLIST to playlistWithSongs
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playlistWithSongs = extraNotNull<PlaylistWithSongs>(EXTRA_PLAYLIST).value

        if (VersionUtils.hasR()) {
            createNewFile(
                "audio/mpegurl",
                playlistWithSongs.playlistEntity.playlistName
            ) { outputStream, data ->
                try {
                    if (outputStream != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            M3UWriter.writeIO(
                                outputStream,
                                playlistWithSongs
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    String.format(
                                        requireContext().getString(R.string.saved_playlist_to),
                                        data?.lastPathSegment
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                                dismiss()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Something went wrong : " + e.message,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val file = PlaylistsUtil.savePlaylistWithSongs(playlistWithSongs)
                MediaScannerConnection.scanFile(
                    requireActivity(),
                    arrayOf<String>(file.path),
                    null
                ) { _, _ ->
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        String.format(App.getContext().getString(R.string.saved_playlist_to), file),
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return materialDialog(R.string.save_playlist_title)
            .setView(R.layout.loading)
            .create().colorButtons()
    }
}
