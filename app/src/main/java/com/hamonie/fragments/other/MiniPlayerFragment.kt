package com.hamonie.fragments.other

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.hamonie.R
import com.hamonie.databinding.FragmentMiniPlayerBinding
import com.hamonie.extensions.accentColor
import com.hamonie.extensions.show
import com.hamonie.extensions.textColorPrimary
import com.hamonie.extensions.textColorSecondary
import com.hamonie.fragments.base.AbsMusicServiceFragment
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.helper.MusicProgressViewUpdateHelper
import com.hamonie.helper.PlayPauseButtonOnClickHandler
import com.hamonie.util.PreferenceUtil
import com.hamonie.util.RetroUtil
import kotlin.math.abs

open class MiniPlayerFragment : AbsMusicServiceFragment(R.layout.fragment_mini_player),
    MusicProgressViewUpdateHelper.Callback, View.OnClickListener {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.actionNext -> MusicPlayerRemote.playNextSong()
            R.id.actionPrevious -> MusicPlayerRemote.back()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)
        view.setOnTouchListener(FlingPlayBackController(requireContext()))
        setUpMiniPlayer()

        if (RetroUtil.isTablet()) {
            binding.actionNext.show()
            binding.actionPrevious.show()
        } else {
            binding.actionNext.visibility =
                if (PreferenceUtil.isExtraControls) View.VISIBLE else View.GONE
            binding.actionPrevious.visibility =
                if (PreferenceUtil.isExtraControls) View.VISIBLE else View.GONE
        }
        binding.actionNext.setOnClickListener(this)
        binding.actionPrevious.setOnClickListener(this)
    }

    private fun setUpMiniPlayer() {
        setUpPlayPauseButton()
        binding.progressBar.accentColor()
    }

    private fun setUpPlayPauseButton() {
        binding.miniPlayerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    private fun updateSongTitle() {

        val song = MusicPlayerRemote.currentSong

        val builder = SpannableStringBuilder()

        val title = SpannableString(song.title)
        title.setSpan(ForegroundColorSpan(textColorPrimary()), 0, title.length, 0)

        val text = SpannableString(song.artistName)
        text.setSpan(ForegroundColorSpan(textColorSecondary()), 0, text.length, 0)

        builder.append(title).append(" • ").append(text)

        binding.miniPlayerTitle.isSelected = true
        binding.miniPlayerTitle.text = builder

//        binding.title.isSelected = true
//        binding.title.text = song.title
//        binding.text.isSelected = true
//        binding.text.text = song.artistName
    }

    private fun updateSongCover() {
//        val song = MusicPlayerRemote.currentSong
//        GlideApp.with(requireContext())
//            .asBitmap()
//            .songCoverOptions(song)
//            .transition(RetroGlideExtension.getDefaultTransition())
//            .load(RetroGlideExtension.getSongModel(song))
//            .into(binding.image)
    }

    override fun onServiceConnected() {
        updateSongTitle()
        updateSongCover()
        updatePlayPauseDrawableState()
    }

    override fun onPlayingMetaChanged() {
        updateSongTitle()
        updateSongCover()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.progressBar.max = total
        val animator = ObjectAnimator.ofInt(binding.progressBar, "progress", progress)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    protected fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    class FlingPlayBackController(context: Context) : View.OnTouchListener {

        private var flingPlayBackController: GestureDetector

        init {
            flingPlayBackController = GestureDetector(context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        if (abs(velocityX) > abs(velocityY)) {
                            if (velocityX < 0) {
                                MusicPlayerRemote.playNextSong()
                                return true
                            } else if (velocityX > 0) {
                                MusicPlayerRemote.playPreviousSong()
                                return true
                            }
                        }
                        return false
                    }
                })
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return flingPlayBackController.onTouchEvent(event)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
