package com.hamonie.appthemehelper.common.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.hamonie.appthemehelper.ATH
import com.hamonie.appthemehelper.ThemeStore

/**
 * @author Aidan Follestad (afollestad)
 */
class ATESwitch : SwitchCompat {

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        ATH.setTint(this, ThemeStore.accentColor(context))
    }

    override fun isShown(): Boolean {
        return parent != null && isVisible
    }
}