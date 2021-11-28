package com.hamonie.views.insets

import android.content.Context
import android.util.AttributeSet
import android.view.WindowInsets
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import com.hamonie.extensions.drawAboveSystemBarsWithPadding
import com.hamonie.extensions.recordInitialPaddingForView
import com.hamonie.extensions.requestApplyInsetsWhenAttached
import com.hamonie.util.RetroUtil
import com.afollestad.materialdialogs.utils.MDUtil.updatePadding

class InsetsConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        if (!RetroUtil.isLandscape())
            drawAboveSystemBarsWithPadding()
    }
}