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

package com.hamonie.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.appthemehelper.util.ATHUtil
import com.hamonie.appthemehelper.util.ColorUtil
import com.hamonie.appthemehelper.util.NavigationViewUtil
import com.hamonie.R
import com.hamonie.util.PreferenceUtil
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationBarTinted @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    init {
        labelVisibilityMode = PreferenceUtil.tabTitleMode

        if (!PreferenceUtil.materialYou) {
            val iconColor = ATHUtil.resolveColor(context, android.R.attr.colorControlNormal)
            val accentColor = ThemeStore.accentColor(context)
            NavigationViewUtil.setItemIconColors(
                this,
                ColorUtil.withAlpha(iconColor, 0.5f),
                accentColor
            )
            NavigationViewUtil.setItemTextColors(
                this,
                ColorUtil.withAlpha(iconColor, 0.5f),
                accentColor
            )
            itemRippleColor = ColorStateList.valueOf(accentColor.addAlpha(0.08F))
            background = ColorDrawable(ATHUtil.resolveColor(context, R.attr.bottomSheetTint))
            itemActiveIndicatorColor = ColorStateList.valueOf(accentColor.addAlpha(0.12F))
        }
    }
}

fun Int.addAlpha(alpha: Float): Int {
    return ColorUtil.withAlpha(this, alpha)
}
