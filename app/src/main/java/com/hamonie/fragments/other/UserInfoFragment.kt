package com.hamonie.fragments.other

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hamonie.appthemehelper.util.ColorUtil
import com.hamonie.appthemehelper.util.MaterialValueHelper
import com.hamonie.Constants.USER_BANNER
import com.hamonie.Constants.USER_PROFILE
import com.hamonie.R
import com.hamonie.databinding.FragmentUserInfoBinding
import com.hamonie.extensions.accentColor
import com.hamonie.extensions.applyToolbar
import com.hamonie.fragments.LibraryViewModel
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.util.ImageUtil
import com.hamonie.util.PreferenceUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class UserInfoFragment : Fragment() {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!
    private val libraryViewModel: LibraryViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            duration = 300L
            scrimColor = Color.TRANSPARENT
        }
        _binding = FragmentUserInfoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyToolbar(binding.toolbar)

        binding.nameContainer.accentColor()
        binding.name.setText(PreferenceUtil.userName)

        binding.userImage.setOnClickListener {
            pickNewPhoto()
        }

        binding.bannerImage.setOnClickListener {
            selectBannerImage()
        }

        binding.next.setOnClickListener {
            val nameString = binding.name.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(nameString)) {
                Toast.makeText(
                    requireContext(),
                    "Umm you're name can't be empty!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            PreferenceUtil.userName = nameString
            findNavController().navigateUp()
        }

        val textColor =
            MaterialValueHelper.getPrimaryTextColor(
                requireContext(),
                ColorUtil.isColorLight(accentColor())
            )
        binding.next.backgroundTintList = ColorStateList.valueOf(accentColor())
        binding.next.iconTint = ColorStateList.valueOf(textColor)
        binding.next.setTextColor(textColor)
        loadProfile()
        postponeEnterTransition()
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }
        libraryViewModel.getFabMargin().observe(viewLifecycleOwner, {
            binding.next.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it
            }
        })
    }

    private fun loadProfile() {
        binding.bannerImage.let {
            GlideApp.with(this)
                .asBitmap()
                .load(RetroGlideExtension.getBannerModel())
                .profileBannerOptions(RetroGlideExtension.getBannerModel())
                .into(it)
        }
        GlideApp.with(this).asBitmap()
            .load(RetroGlideExtension.getUserModel())
            .userProfileOptions(RetroGlideExtension.getUserModel())
            .into(binding.userImage)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectBannerImage() {
        ImagePicker.with(this)
            .compress(1440)
            .provider(ImageProvider.GALLERY)
            .crop(16f, 9f)
            .start(PICK_BANNER_REQUEST)
    }

    private fun pickNewPhoto() {
        ImagePicker.with(this)
            .provider(ImageProvider.GALLERY)
            .cropSquare()
            .compress(1440)
            .start(PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            val fileUri = data?.data
            fileUri?.let { setAndSaveUserImage(it) }
        } else if (resultCode == Activity.RESULT_OK && requestCode == PICK_BANNER_REQUEST) {
            val fileUri = data?.data
            fileUri?.let { setAndSaveBannerImage(it) }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(requireContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAndSaveBannerImage(fileUri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(fileUri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .listener(object : RequestListener<Bitmap> {
                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.let { saveImage(it, USER_BANNER) }
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(binding.bannerImage)
    }

    private fun saveImage(bitmap: Bitmap, fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val appDir = requireContext().filesDir
            val file = File(appDir, fileName)
            var successful = false
            try {
                val os = BufferedOutputStream(FileOutputStream(file))
                successful = ImageUtil.resizeBitmap(bitmap, 2048)
                    .compress(Bitmap.CompressFormat.WEBP, 100, os)
                withContext(Dispatchers.IO) { os.close() }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (successful) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setAndSaveUserImage(fileUri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(fileUri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .listener(object : RequestListener<Bitmap> {
                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.let { saveImage(it, USER_PROFILE) }
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(binding.userImage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 9002
        private const val PICK_BANNER_REQUEST = 9004
    }
}
