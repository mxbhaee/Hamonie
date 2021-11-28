package com.hamonie.fragments.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.App
import com.hamonie.R
import com.hamonie.databinding.FragmentMainSettingsBinding
import com.hamonie.extensions.addBottomInsets
import com.hamonie.extensions.hide
import com.hamonie.extensions.show
import com.hamonie.util.NavigationUtil
import com.hamonie.util.RetroUtil

class MainSettingsFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentMainSettingsBinding? = null
    private val binding get() = _binding!!


    override fun onClick(view: View) {
        when (view.id) {
            R.id.generalSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_themeSettingsFragment)
            R.id.audioSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_audioSettings)
            R.id.personalizeSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_personalizeSettingsFragment)
            R.id.imageSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_imageSettingFragment)
            R.id.notificationSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_notificationSettingsFragment)
            R.id.otherSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_otherSettingsFragment)
            R.id.aboutSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_aboutActivity)
            R.id.nowPlayingSettings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment)
            R.id.backup_restore_settings -> findNavController().navigate(R.id.action_mainSettingsFragment_to_backupFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.generalSettings.setOnClickListener(this)
        binding.audioSettings.setOnClickListener(this)
        binding.nowPlayingSettings.setOnClickListener(this)
        binding.personalizeSettings.setOnClickListener(this)
        binding.imageSettings.setOnClickListener(this)
        binding.notificationSettings.setOnClickListener(this)
        binding.otherSettings.setOnClickListener(this)
        binding.aboutSettings.setOnClickListener(this)
        binding.backupRestoreSettings.setOnClickListener(this)

        binding.buyProContainer.apply {
            if (App.isProVersion()) hide() else show()
            setOnClickListener {
                NavigationUtil.goToProVersion(requireContext())
            }
        }
        binding.buyPremium.setOnClickListener {
            NavigationUtil.goToProVersion(requireContext())
        }
        ThemeStore.accentColor(requireContext()).let {
            binding.buyPremium.setTextColor(it)
            binding.diamondIcon.imageTintList = ColorStateList.valueOf(it)
        }
        if (!RetroUtil.isLandscape()) {
            binding.container.updatePadding(bottom = RetroUtil.getNavigationBarHeight())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
