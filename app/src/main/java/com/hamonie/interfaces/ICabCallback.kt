package com.hamonie.interfaces

import android.view.Menu
import android.view.MenuItem
import com.afollestad.materialcab.attached.AttachedCab

interface ICabCallback {
    fun onCabCreated(cab: AttachedCab, menu: Menu): Boolean

    fun onCabItemClicked(item: MenuItem): Boolean

    fun onCabFinished(cab: AttachedCab): Boolean
}
