package com.hamonie.extensions

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.hamonie.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun DialogFragment.materialDialog(title: Int): MaterialAlertDialogBuilder {
    return MaterialAlertDialogBuilder(
        requireContext(),
        R.style.MaterialAlertDialogTheme
    ).setTitle(title)
}

fun AlertDialog.colorButtons(): AlertDialog {
    setOnShowListener {
        getButton(AlertDialog.BUTTON_POSITIVE).accentTextColor()
        getButton(AlertDialog.BUTTON_NEGATIVE).accentTextColor()
        getButton(AlertDialog.BUTTON_NEUTRAL).accentTextColor()
    }
    return this
}
