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

package com.hamonie.transform

import android.view.View
import androidx.viewpager.widget.ViewPager

class VerticalFlipTransformation : ViewPager.PageTransformer {
    override fun transformPage(page: View, position: Float) {

        page.translationX = -position * page.width
        page.cameraDistance = 100000f

        if (position < 0.5 && position > -0.5) {
            page.visibility = View.VISIBLE
        } else {
            page.visibility = View.INVISIBLE
        }


        if (position < -1) {     // [-Infinity,-1)
            // This page is way off-screen to the left.
            page.alpha = 0f

        } else if (position <= 0) {    // [-1,0]
            page.alpha = 1f
            page.rotationY = 180 * (1 - Math.abs(position) + 1)

        } else if (position <= 1) {    // (0,1]
            page.alpha = 1f
            page.rotationY = -180 * (1 - Math.abs(position) + 1)

        } else {    // (1,+Infinity]
            // This page is way off-screen to the right.
            page.alpha = 0f

        }


    }
}