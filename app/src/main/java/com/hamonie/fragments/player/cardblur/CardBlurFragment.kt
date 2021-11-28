package com.hamonie.fragments.player.cardblur

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import com.hamonie.appthemehelper.util.ToolbarContentTintHelper
import com.hamonie.NEW_BLUR_AMOUNT
import com.hamonie.R
import com.hamonie.databinding.FragmentCardBlurPlayerBinding
import com.hamonie.extensions.drawAboveSystemBars
import com.hamonie.fragments.base.AbsPlayerFragment
import com.hamonie.fragments.player.PlayerAlbumCoverFragment
import com.hamonie.fragments.player.normal.PlayerFragment
import com.hamonie.glide.BlurTransformation
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.glide.RetroMusicColoredTarget
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.model.Song
import com.hamonie.util.PreferenceUtil.blurAmount
import com.hamonie.util.color.MediaNotificationProcessor

class CardBlurFragment : AbsPlayerFragment(R.layout.fragment_card_blur_player),
    SharedPreferences.OnSharedPreferenceChangeListener {
    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    private var lastColor: Int = 0
    override val paletteColor: Int
        get() = lastColor
    private lateinit var playbackControlsFragment: CardBlurPlaybackControlsFragment

    private var _binding: FragmentCardBlurPlayerBinding? = null
    private val binding get() = _binding!!

    override fun onShow() {
        playbackControlsFragment.show()
    }

    override fun onHide() {
        playbackControlsFragment.hide()
        onBackPressed()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun toolbarIconColor(): Int {
        return Color.WHITE
    }

    override fun onColorChanged(color: MediaNotificationProcessor) {
        playbackControlsFragment.setColor(color)
        lastColor = color.backgroundColor
        libraryViewModel.updateColor(color.backgroundColor)
        ToolbarContentTintHelper.colorizeToolbar(binding.playerToolbar, Color.WHITE, activity)

        binding.playerToolbar.setTitleTextColor(Color.WHITE)
        binding.playerToolbar.setSubtitleTextColor(Color.WHITE)
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCardBlurPlayerBinding.bind(view)
        setUpSubFragments()
        setUpPlayerToolbar()
        binding.cardContainer?.drawAboveSystemBars()
    }

    private fun setUpSubFragments() {
        playbackControlsFragment =
            childFragmentManager.findFragmentById(R.id.playbackControlsFragment) as CardBlurPlaybackControlsFragment
        (childFragmentManager.findFragmentById(R.id.playerAlbumCoverFragment) as PlayerAlbumCoverFragment?)?.setCallbacks(
            this
        )
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressed() }
            setTitleTextColor(Color.WHITE)
            setSubtitleTextColor(Color.WHITE)
            ToolbarContentTintHelper.colorizeToolbar(binding.playerToolbar, Color.WHITE, activity)
        }.setOnMenuItemClickListener(this)
    }

    override fun onServiceConnected() {
        updateIsFavorite()
        updateBlur()
        updateSong()
    }

    override fun onPlayingMetaChanged() {
        updateIsFavorite()
        updateBlur()
        updateSong()
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.playerToolbar.apply {
            title = song.title
            subtitle = song.artistName
        }
    }

    private fun updateBlur() {
        binding.colorBackground.clearColorFilter()
        GlideApp.with(requireActivity()).asBitmapPalette()
            .songCoverOptions(MusicPlayerRemote.currentSong)
            .load(RetroGlideExtension.getSongModel(MusicPlayerRemote.currentSong))
            .dontAnimate()
            .transform(
                BlurTransformation.Builder(requireContext()).blurRadius(blurAmount.toFloat())
                    .build()
            )
            .into(object : RetroMusicColoredTarget(binding.colorBackground) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    if (colors.backgroundColor == defaultFooterColor) {
                        binding.colorBackground.setColorFilter(colors.backgroundColor)
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
        _binding = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == NEW_BLUR_AMOUNT) {
            updateBlur()
        }
    }

    companion object {
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }
}
