package com.hamonie.glide

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.hamonie.appthemehelper.util.ATHUtil
import com.hamonie.R
import com.hamonie.glide.palette.BitmapPaletteTarget
import com.hamonie.glide.palette.BitmapPaletteWrapper
import com.hamonie.util.ColorUtil
import com.bumptech.glide.request.transition.Transition

abstract class SingleColorTarget(view: ImageView) : BitmapPaletteTarget(view) {

    private val defaultFooterColor: Int
        get() = ATHUtil.resolveColor(view.context, R.attr.colorControlNormal)

    abstract fun onColorReady(color: Int)

    override fun onLoadFailed(errorDrawable: Drawable?) {
        super.onLoadFailed(errorDrawable)
        onColorReady(defaultFooterColor)
    }

    override fun onResourceReady(
        resource: BitmapPaletteWrapper,
        transition: Transition<in BitmapPaletteWrapper>?
    ) {
        super.onResourceReady(resource, transition)
        onColorReady(
            ColorUtil.getColor(
                resource.palette,
                ATHUtil.resolveColor(view.context, R.attr.colorPrimary)
            )
        )
    }
}
