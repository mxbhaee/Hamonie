package com.hamonie.fragments.home

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.View
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hamonie.appthemehelper.common.ATHToolbarActivity
import com.hamonie.appthemehelper.util.ToolbarContentTintHelper
import com.hamonie.*
import com.hamonie.adapter.HomeAdapter
import com.hamonie.databinding.FragmentBannerHomeBinding
import com.hamonie.databinding.FragmentHomeBinding
import com.hamonie.dialogs.CreatePlaylistDialog
import com.hamonie.dialogs.ImportPlaylistDialog
import com.hamonie.extensions.accentColor
import com.hamonie.extensions.drawNextToNavbar
import com.hamonie.fragments.base.AbsMainActivityFragment
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.util.PreferenceUtil
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis

class HomeFragment :
    AbsMainActivityFragment(if (PreferenceUtil.isHomeBanner) R.layout.fragment_banner_home else R.layout.fragment_home) {

    private var _binding: HomeBindingAdapter? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = getBinding(PreferenceUtil.isHomeBanner, view)
        mainActivity.setSupportActionBar(binding.toolbar)
        mainActivity.supportActionBar?.title = null
        setupListeners()
        binding.titleWelcome.text = String.format("%s", PreferenceUtil.userName)

        enterTransition = MaterialFadeThrough().apply {
            addTarget(binding.contentContainer)
        }

        val homeAdapter = HomeAdapter(mainActivity)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = homeAdapter
        }
        libraryViewModel.getHome().observe(viewLifecycleOwner, {
            homeAdapter.swapData(it)
        })

        loadProfile()
        setupTitle()
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())
        binding.toolbar.drawNextToNavbar()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            remove()
            mainActivity.finish()
        }
    }

    private fun setupListeners() {
        binding.bannerImage?.setOnClickListener {
            findNavController().navigate(
                R.id.user_info_fragment, null, null, FragmentNavigatorExtras(
                    binding.userImage to "user_image"
                )
            )
            reenterTransition = null
        }

        binding.lastAdded.setOnClickListener {
            findNavController().navigate(
                R.id.detailListFragment,
                bundleOf(EXTRA_PLAYLIST_TYPE to LAST_ADDED_PLAYLIST)
            )
            setSharedAxisYTransitions()
        }

        binding.topPlayed.setOnClickListener {
            findNavController().navigate(
                R.id.detailListFragment,
                bundleOf(EXTRA_PLAYLIST_TYPE to TOP_PLAYED_PLAYLIST)
            )
            setSharedAxisYTransitions()
        }

        binding.actionShuffle.setOnClickListener {
            libraryViewModel.shuffleSongs()
        }

        binding.history.setOnClickListener {
            findNavController().navigate(
                R.id.detailListFragment,
                bundleOf(EXTRA_PLAYLIST_TYPE to HISTORY_PLAYLIST)
            )
            setSharedAxisYTransitions()
        }

        binding.userImage.setOnClickListener {
            findNavController().navigate(
                R.id.user_info_fragment, null, null, FragmentNavigatorExtras(
                    binding.userImage to "user_image"
                )
            )
        }
    }

    private fun getBinding(homeBanner: Boolean, view: View): HomeBindingAdapter {
        return if (homeBanner) {
            val homeBannerBinding = FragmentBannerHomeBinding.bind(view)
            HomeBindingAdapter(null, homeBannerBinding)
        } else {
            val homeBinding = FragmentHomeBinding.bind(view)
            HomeBindingAdapter(homeBinding, null)
        }
    }

    private fun setupTitle() {
        binding.toolbar.setNavigationOnClickListener {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).addTarget(binding.root)
            reenterTransition =
                MaterialSharedAxis(MaterialSharedAxis.Z, false)
            findNavController().navigate(R.id.searchFragment, null, navOptions)
        }
        val hexColor = String.format("#%06X", 0xFFFFFF and accentColor())
        val appName = HtmlCompat.fromHtml(
            "Hamo<span  style='color:$hexColor';>nie</span>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        binding.appNameText.text = appName
    }

    private fun loadProfile() {
        binding.bannerImage?.let {
            GlideApp.with(requireContext())
                .asBitmap()
                .profileBannerOptions(RetroGlideExtension.getBannerModel())
                .load(RetroGlideExtension.getBannerModel())
                .into(it)
        }
        GlideApp.with(requireActivity()).asBitmap()
            .userProfileOptions(RetroGlideExtension.getUserModel())
            .load(RetroGlideExtension.getUserModel())
            .into(binding.userImage)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.findItem(R.id.action_settings).setShowAsAction(SHOW_AS_ACTION_IF_ROOM)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(binding.toolbar)
        )
        //Setting up cast button
        CastButtonFactory.setUpMediaRouteButton(requireContext(), menu, R.id.action_cast)
    }

    fun scrollToTop() {
        binding.container.scrollTo(0, 0)
        binding.appBarLayout.setExpanded(true)
    }

    fun setSharedAxisXTransitions() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            addTarget(binding.root)
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    private fun setSharedAxisYTransitions() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
            addTarget(binding.root)
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    companion object {

        const val TAG: String = "BannerHomeFragment"

        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> findNavController().navigate(
                R.id.settingsActivity,
                null,
                navOptions
            )
            R.id.action_import_playlist -> ImportPlaylistDialog().show(
                childFragmentManager,
                "ImportPlaylist"
            )
            R.id.action_add_to_playlist -> CreatePlaylistDialog.create(emptyList()).show(
                childFragmentManager,
                "ShowCreatePlaylistDialog"
            )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), binding.toolbar)
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.fetchHomeSections()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
