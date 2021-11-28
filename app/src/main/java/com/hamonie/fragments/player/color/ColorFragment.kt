package com.hamonie.fragments.player.color

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import com.hamonie.appthemehelper.util.ATHUtil
import com.hamonie.appthemehelper.util.ToolbarContentTintHelper
import com.hamonie.R
import com.hamonie.databinding.FragmentColorPlayerBinding
import com.hamonie.extensions.drawAboveSystemBars
import com.hamonie.fragments.base.AbsPlayerFragment
import com.hamonie.fragments.player.PlayerAlbumCoverFragment
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.model.Song
import com.hamonie.util.color.MediaNotificationProcessor

class ColorFragment : AbsPlayerFragment(R.layout.fragment_color_player) {

    private var lastColor: Int = 0
    private var navigationColor: Int = 0
    private lateinit var playbackControlsFragment: ColorPlaybackControlsFragment
    private var valueAnimator: ValueAnimator? = null
    private var _binding: FragmentColorPlayerBinding? = null
    private val binding get() = _binding!!


    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    override val paletteColor: Int
        get() = navigationColor

    override fun onColorChanged(color: MediaNotificationProcessor) {
        libraryViewModel.updateColor(color.backgroundColor)
        lastColor = color.secondaryTextColor
        playbackControlsFragment.setColor(color)
        navigationColor = color.backgroundColor

        binding.colorGradientBackground.setBackgroundColor(color.backgroundColor)
        val animator =
            playbackControlsFragment.createRevealAnimator(binding.colorGradientBackground)
        animator.doOnEnd {
            _binding?.root?.setBackgroundColor(color.backgroundColor)
        }
        animator.start()
        ToolbarContentTintHelper.colorizeToolbar(
            binding.playerToolbar,
            color.secondaryTextColor,
            requireActivity()
        )
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

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
        return lastColor
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (valueAnimator != null) {
            valueAnimator!!.cancel()
            valueAnimator = null
        }
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentColorPlayerBinding.bind(view)
        setUpSubFragments()
        setUpPlayerToolbar()
        val playerAlbumCoverFragment =
            childFragmentManager.findFragmentById(R.id.playerAlbumCoverFragment) as PlayerAlbumCoverFragment
        playerAlbumCoverFragment.setCallbacks(this)
        playerToolbar().drawAboveSystemBars()
    }

    private fun setUpSubFragments() {
        playbackControlsFragment =
            childFragmentManager.findFragmentById(R.id.playbackControlsFragment) as ColorPlaybackControlsFragment
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressed() }
            setOnMenuItemClickListener(this@ColorFragment)
            ToolbarContentTintHelper.colorizeToolbar(
                this,
                ATHUtil.resolveColor(requireContext(), R.attr.colorControlNormal),
                requireActivity()
            )
        }
    }

    companion object {
        fun newInstance(): ColorFragment {
            return ColorFragment()
        }
    }
}
