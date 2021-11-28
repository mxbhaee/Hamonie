package com.hamonie.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.hamonie.appthemehelper.util.VersionUtils
import com.hamonie.EXTRA_SONG
import com.hamonie.R
import com.hamonie.activities.saf.SAFGuideActivity
import com.hamonie.extensions.extraNotNull
import com.hamonie.fragments.LibraryViewModel
import com.hamonie.fragments.ReloadType
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.model.Song
import com.hamonie.util.MusicUtil
import com.hamonie.util.SAFUtil
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

class DeleteSongsDialog : DialogFragment() {
    lateinit var libraryViewModel: LibraryViewModel

    companion object {
        fun create(song: Song): DeleteSongsDialog {
            val list = ArrayList<Song>()
            list.add(song)
            return create(list)
        }

        fun create(songs: List<Song>): DeleteSongsDialog {
            return DeleteSongsDialog().apply {
                arguments = bundleOf(
                    EXTRA_SONG to ArrayList(songs)
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        libraryViewModel = activity?.getViewModel() as LibraryViewModel
        val songs = extraNotNull<List<Song>>(EXTRA_SONG).value
        val pair = if (songs.size > 1) {
            Pair(
                R.string.delete_songs_title,
                HtmlCompat.fromHtml(
                    String.format(getString(R.string.delete_x_songs), songs.size),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            )
        } else {
            Pair(
                R.string.delete_song_title,
                HtmlCompat.fromHtml(
                    String.format(getString(R.string.delete_song_x), songs[0].title),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            )
        }

        return MaterialDialog(requireContext())
            .title(pair.first)
            .message(text = pair.second)
            .noAutoDismiss()
            .cornerRadius(16F)
            .negativeButton(android.R.string.cancel) {
                dismiss()
            }
            .positiveButton(R.string.action_delete) {
                if ((songs.size == 1) && MusicPlayerRemote.isPlaying(songs[0])) {
                    MusicPlayerRemote.playNextSong()
                }
                if (VersionUtils.hasR()) {
                    dismiss()
                    MusicUtil.deleteTracksR(requireActivity(), songs)
                    reloadTabs()
                } else if (!SAFUtil.isSAFRequiredForSongs(songs)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        dismiss()
                        MusicUtil.deleteTracks(requireContext(), songs)
                        reloadTabs()
                    }
                } else {
                    if (SAFUtil.isSDCardAccessGranted(requireActivity())) {
                        deleteSongs(songs)
                    } else {
                        startActivityForResult(
                            Intent(requireActivity(), SAFGuideActivity::class.java),
                            SAFGuideActivity.REQUEST_CODE_SAF_GUIDE
                        )
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SAFGuideActivity.REQUEST_CODE_SAF_GUIDE -> {
                SAFUtil.openTreePicker(this)
            }
            SAFUtil.REQUEST_SAF_PICK_TREE,
            SAFUtil.REQUEST_SAF_PICK_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    SAFUtil.saveTreeUri(requireActivity(), data)
                    val songs = extraNotNull<List<Song>>(EXTRA_SONG).value
                    deleteSongs(songs)
                }
            }
        }
    }

    fun deleteSongs(songs: List<Song>) {
        CoroutineScope(Dispatchers.IO).launch {
            dismiss()
            MusicUtil.deleteTracks(requireActivity(), songs, null, null)
            reloadTabs()
        }
    }

    fun reloadTabs() {
        libraryViewModel.forceReload(ReloadType.Songs)
        libraryViewModel.forceReload(ReloadType.HomeSections)
        libraryViewModel.forceReload(ReloadType.Artists)
        libraryViewModel.forceReload(ReloadType.Albums)
    }
}
