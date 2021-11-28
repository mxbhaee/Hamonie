package com.hamonie.fragments.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.transition.Fade
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hamonie.appthemehelper.util.ToolbarContentTintHelper
import com.hamonie.appthemehelper.util.VersionUtils
import com.hamonie.R
import com.hamonie.activities.MainActivity
import com.hamonie.activities.tageditor.TagWriter
import com.hamonie.databinding.FragmentLyricsBinding
import com.hamonie.databinding.FragmentNormalLyricsBinding
import com.hamonie.databinding.FragmentSyncedLyricsBinding
import com.hamonie.extensions.accentColor
import com.hamonie.extensions.surfaceColor
import com.hamonie.extensions.textColorSecondary
import com.hamonie.fragments.base.AbsMusicServiceFragment
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.helper.MusicProgressViewUpdateHelper
import com.hamonie.lyrics.LrcView
import com.hamonie.model.AudioTagInfo
import com.hamonie.model.Song
import com.hamonie.util.LyricUtil
import com.hamonie.util.RetroUtil
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialContainerTransform
import kotlinx.coroutines.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.*

class LyricsFragment : AbsMusicServiceFragment(R.layout.fragment_lyrics) {

    private var _binding: FragmentLyricsBinding? = null
    private val binding get() = _binding!!
    private lateinit var song: Song

    val mainActivity: MainActivity
        get() = activity as MainActivity

    private lateinit var lyricsSectionsAdapter: LyricsSectionsAdapter

    private val googleSearchLrcUrl: String
        get() {
            var baseUrl = "http://www.google.com/search?"
            var query = song.title + "+" + song.artistName
            query = "q=" + query.replace(" ", "+") + " lyrics"
            baseUrl += query
            return baseUrl
        }
    private val syairSearchLrcUrl: String
        get() {
            var baseUrl = "https://www.syair.info/search?"
            var query = song.title + "+" + song.artistName
            query = "q=" + query.replace(" ", "+")
            baseUrl += query
            return baseUrl
        }

    private fun buildContainerTransform(): MaterialContainerTransform {
        val transform = MaterialContainerTransform()
        transform.setAllContainerColors(
            MaterialColors.getColor(requireView().findViewById(R.id.container), R.attr.colorSurface)
        )
        transform.addTarget(R.id.container)
        transform.duration = 300
        return transform
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterTransition = Fade()
        exitTransition = Fade()
        lyricsSectionsAdapter = LyricsSectionsAdapter(requireActivity())
        _binding = FragmentLyricsBinding.bind(view)
        ViewCompat.setTransitionName(binding.container, "lyrics")

        setupWakelock()

        binding.tabLyrics.setBackgroundColor(surfaceColor())
        binding.container.setBackgroundColor(surfaceColor())
        setupViews()
        setupToolbar()
        updateTitleSong()
        if (VersionUtils.hasR()) {
            binding.editButton.isVisible = false
        }
    }

    private fun setupViews() {
        binding.lyricsPager.adapter = lyricsSectionsAdapter
        TabLayoutMediator(binding.tabLyrics, binding.lyricsPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Synced Lyrics"
                1 -> "Normal Lyrics"
                else -> ""
            }
        }.attach()
//        lyricsPager.isUserInputEnabled = false

        binding.tabLyrics.setSelectedTabIndicatorColor(accentColor())
        binding.tabLyrics.setTabTextColors(textColorSecondary(), accentColor())
        binding.editButton.accentColor()
        binding.editButton.setOnClickListener {
            when (binding.lyricsPager.currentItem) {
                0 -> {
                    editSyncedLyrics()
                }
                1 -> {
                    editNormalLyrics()
                }
            }
        }
    }

    private fun setupToolbar() {
        mainActivity.setSupportActionBar(binding.toolbar)
        binding.toolbar.setBackgroundColor(surfaceColor())
        ToolbarContentTintHelper.colorBackButton(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateTitleSong()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateTitleSong()
    }

    private fun updateTitleSong() {
        song = MusicPlayerRemote.currentSong
        binding.toolbar.title = song.title
        binding.toolbar.subtitle = song.artistName
    }

    private fun setupWakelock() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        }
        if (item.itemId == R.id.action_search) {
            RetroUtil.openUrl(
                requireActivity(), when (binding.lyricsPager.currentItem) {
                    0 -> syairSearchLrcUrl
                    1 -> googleSearchLrcUrl
                    else -> googleSearchLrcUrl
                }
            )
        }
        return super.onOptionsItemSelected(item)
    }


    @SuppressLint("CheckResult")
    private fun editNormalLyrics() {
        var content = ""
        val file = File(MusicPlayerRemote.currentSong.data)
        try {
            content = AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(res = R.string.edit_normal_lyrics)
            input(
                hintRes = R.string.paste_lyrics_here,
                prefill = content,
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            ) { _, input ->
                val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
                fieldKeyValueMap[FieldKey.LYRICS] = input.toString()
                GlobalScope.launch {
                    TagWriter.writeTagsToFiles(
                        requireContext(), AudioTagInfo(
                            listOf(song.data), fieldKeyValueMap, null
                        )
                    )
                }
            }
            positiveButton(res = R.string.save) {
                (lyricsSectionsAdapter.fragments[1].first as NormalLyrics).loadNormalLyrics()
            }
            negativeButton(res = android.R.string.cancel)
        }
    }


    @SuppressLint("CheckResult")
    private fun editSyncedLyrics() {
        var lrcFile: File? = null
        if (LyricUtil.isLrcOriginalFileExist(song.data)) {
            lrcFile = LyricUtil.getLocalLyricOriginalFile(song.data)
        } else if (LyricUtil.isLrcFileExist(song.title, song.artistName)) {
            lrcFile = LyricUtil.getLocalLyricFile(song.title, song.artistName)
        }
        val content: String = LyricUtil.getStringFromLrc(lrcFile)

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(res = R.string.edit_synced_lyrics)
            input(
                hintRes = R.string.paste_timeframe_lyrics_here,
                prefill = content,
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            ) { _, input ->
                LyricUtil.writeLrc(song, input.toString())
            }
            positiveButton(res = R.string.save) {
                (lyricsSectionsAdapter.fragments[0].first as SyncedLyrics).loadLRCLyrics()
            }
            negativeButton(res = android.R.string.cancel)
        }
    }

    class LyricsSectionsAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        val fragments = listOf(
            Pair(SyncedLyrics(), R.string.synced_lyrics),
            Pair(NormalLyrics(), R.string.normal_lyrics)
        )


        override fun getItemCount(): Int {
            return fragments.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position].first
        }
    }

    class NormalLyrics : AbsMusicServiceFragment(R.layout.fragment_normal_lyrics) {

        private var _binding: FragmentNormalLyricsBinding? = null
        private val binding get() = _binding!!

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            _binding = FragmentNormalLyricsBinding.bind(view)
            loadNormalLyrics()
            super.onViewCreated(view, savedInstanceState)
        }

        fun loadNormalLyrics() {
            var lyrics: String? = null
            val file = File(MusicPlayerRemote.currentSong.data)
            try {
                lyrics = AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (lyrics.isNullOrEmpty()) {
                binding.noLyricsFound.visibility = View.VISIBLE
            } else {
                binding.noLyricsFound.visibility = View.GONE
            }
            binding.normalLyrics.text = lyrics
        }

        override fun onPlayingMetaChanged() {
            super.onPlayingMetaChanged()
            loadNormalLyrics()
        }

        override fun onServiceConnected() {
            super.onServiceConnected()
            loadNormalLyrics()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }

    class SyncedLyrics : AbsMusicServiceFragment(R.layout.fragment_synced_lyrics),
        MusicProgressViewUpdateHelper.Callback {

        private var _binding: FragmentSyncedLyricsBinding? = null
        private val binding get() = _binding!!
        private lateinit var updateHelper: MusicProgressViewUpdateHelper

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            updateHelper = MusicProgressViewUpdateHelper(this, 500, 1000)
            _binding = FragmentSyncedLyricsBinding.bind(view)
            setupLyricsView()
            loadLRCLyrics()
            super.onViewCreated(view, savedInstanceState)
        }

        fun loadLRCLyrics() {
            binding.lyricsView.setLabel("Empty")
            val song = MusicPlayerRemote.currentSong
            if (LyricUtil.isLrcOriginalFileExist(song.data)) {
                binding.lyricsView.loadLrc(LyricUtil.getLocalLyricOriginalFile(song.data))
            } else if (LyricUtil.isLrcFileExist(song.title, song.artistName)) {
                binding.lyricsView.loadLrc(LyricUtil.getLocalLyricFile(song.title, song.artistName))
            }
        }

        private fun setupLyricsView() {
            binding.lyricsView.apply {
                setCurrentColor(accentColor())
                setTimeTextColor(accentColor())
                setTimelineColor(accentColor())
                setTimelineTextColor(accentColor())
                setDraggable(true, LrcView.OnPlayClickListener {
                    MusicPlayerRemote.seekTo(it.toInt())
                    return@OnPlayClickListener true
                })
            }
        }

        override fun onUpdateProgressViews(progress: Int, total: Int) {
            binding.lyricsView.updateTime(progress.toLong())
        }

        override fun onPlayingMetaChanged() {
            super.onPlayingMetaChanged()
            loadLRCLyrics()
        }

        override fun onServiceConnected() {
            super.onServiceConnected()
            loadLRCLyrics()
        }

        override fun onResume() {
            super.onResume()
            updateHelper.start()
        }

        override fun onPause() {
            super.onPause()
            updateHelper.stop()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (MusicPlayerRemote.playingQueue.isNotEmpty())
            (requireActivity() as MainActivity).expandPanel()
    }
}
