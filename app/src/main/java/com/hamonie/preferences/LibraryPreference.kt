/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.hamonie.preferences

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.widget.Toast
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hamonie.appthemehelper.common.prefs.supportv7.ATEDialogPreference
import com.hamonie.R
import com.hamonie.adapter.CategoryInfoAdapter
import com.hamonie.extensions.colorButtons
import com.hamonie.extensions.colorControlNormal
import com.hamonie.extensions.materialDialog
import com.hamonie.model.CategoryInfo
import com.hamonie.util.PreferenceUtil


class LibraryPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ATEDialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            context.colorControlNormal(),
            SRC_IN
        )
    }
}

class LibraryPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater
            .inflate(R.layout.preference_dialog_library_categories, null)

        val categoryAdapter = CategoryInfoAdapter()
        categoryAdapter.categoryInfos = PreferenceUtil.libraryCategory
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = categoryAdapter
        categoryAdapter.attachToRecyclerView(recyclerView)


        return materialDialog(R.string.library_categories)
            .setNeutralButton(
                R.string.reset_action
            ) { _, _ ->
                updateCategories(PreferenceUtil.defaultCategories)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton( R.string.done) { _, _ -> updateCategories(categoryAdapter.categoryInfos) }
            .setView(view)
            .create()
            .colorButtons()
    }

    private fun updateCategories(categories: List<CategoryInfo>) {
        if (getSelected(categories) == 0) return
        if (getSelected(categories) > 5) {
            Toast.makeText(context, "Not more than 5 items", Toast.LENGTH_SHORT).show()
            return
        }
        PreferenceUtil.libraryCategory = categories
    }

    private fun getSelected(categories: List<CategoryInfo>): Int {
        var selected = 0
        for (categoryInfo in categories) {
            if (categoryInfo.visible)
                selected++
        }
        return selected
    }

    companion object {
        fun newInstance(): LibraryPreferenceDialog {
            return LibraryPreferenceDialog()
        }
    }
}