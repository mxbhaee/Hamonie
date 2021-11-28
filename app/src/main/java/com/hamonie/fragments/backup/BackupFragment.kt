package com.hamonie.fragments.backup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hamonie.R
import com.hamonie.adapter.backup.BackupAdapter
import com.hamonie.databinding.FragmentBackupBinding
import com.hamonie.helper.BackupHelper
import com.hamonie.helper.sanitize
import com.hamonie.util.BackupUtil
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class BackupFragment : Fragment(R.layout.fragment_backup), BackupAdapter.BackupClickedListener {

    private val backupViewModel by viewModels<BackupViewModel>()
    private var backupAdapter: BackupAdapter? = null

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBackupBinding.bind(view)
        initAdapter()
        setupRecyclerview()
        backupViewModel.backupsLiveData.observe(this) {
            if (it.isNotEmpty())
                backupAdapter?.swapDataset(it)
            else
                backupAdapter?.swapDataset(listOf())
        }
        backupViewModel.loadBackups()
        val openFilePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            lifecycleScope.launch(Dispatchers.IO) {
                backupViewModel.restoreBackup(requireActivity(), requireContext().contentResolver.openInputStream(it))
            }
        }
        binding.createBackup.setOnClickListener {
            showCreateBackupDialog()
        }
        binding.restoreBackup.setOnClickListener {
            openFilePicker.launch(arrayOf("application/octet-stream"))
        }
    }

    private fun initAdapter() {
        backupAdapter = BackupAdapter(requireActivity(), ArrayList(), this)
        backupAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

    private fun checkIsEmpty() {
        val isEmpty = backupAdapter!!.itemCount == 0
        binding.backupTitle.isVisible = !isEmpty
        binding.backupRecyclerview.isVisible = !isEmpty
    }

    fun setupRecyclerview() {
        binding.backupRecyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = backupAdapter
        }
    }

    @SuppressLint("CheckResult")
    private fun showCreateBackupDialog() {
        MaterialDialog(requireContext()).show {
            cornerRadius(res = R.dimen.m3_card_corner_radius)
            title(res = R.string.action_rename)
            input(prefill = System.currentTimeMillis().toString()) { _, text ->
                // Text submitted with the action button
                lifecycleScope.launch {
                    BackupHelper.createBackup(requireContext(), text.sanitize())
                    backupViewModel.loadBackups()
                }
            }
            positiveButton(android.R.string.ok)
            negativeButton(R.string.action_cancel)
            setTitle(R.string.title_new_backup)
        }
    }

    override fun onBackupClicked(file: File) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.restore)
            .setMessage(R.string.restore_message)
            .setPositiveButton(R.string.restore) { _, _ ->
                lifecycleScope.launch {
                    backupViewModel.restoreBackup(requireActivity(), file.inputStream())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    @SuppressLint("CheckResult")
    override fun onBackupMenuClicked(file: File, menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_delete -> {
                try {
                    file.delete()
                } catch (exception: SecurityException) {
                    Toast.makeText(
                        activity,
                        "Could not delete backup",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                backupViewModel.loadBackups()
                return true
            }
            R.id.action_share -> {
                activity?.startActivity(
                    Intent.createChooser(
                        BackupUtil.createShareFileIntent(file, requireContext()),
                        null
                    )
                )
                return true
            }
            R.id.action_rename -> {
                MaterialDialog(requireContext()).show {
                    title(res = R.string.action_rename)
                    input(prefill = file.nameWithoutExtension) { _, text ->
                        // Text submitted with the action button
                        val renamedFile =
                            File(file.parent + File.separator + text + BackupHelper.APPEND_EXTENSION)
                        if (!renamedFile.exists()) {
                            file.renameTo(renamedFile)
                            backupViewModel.loadBackups()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "File already exists",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    positiveButton(android.R.string.ok)
                    negativeButton(R.string.action_cancel)
                    setTitle(R.string.action_rename)
                }
                return true
            }
        }
        return false
    }
}