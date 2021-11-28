package com.hamonie.extensions

import androidx.core.view.WindowInsetsCompat

fun WindowInsetsCompat?.safeGetBottomInsets(): Int {
    return this?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
}
