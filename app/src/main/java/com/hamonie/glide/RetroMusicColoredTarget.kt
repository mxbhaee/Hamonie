package com.hamonie.glide

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.hamonie.appthemehelper.util.ATHUtil
import com.hamonie.App
import com.hamonie.R
import com.hamonie.glide.palette.BitmapPaletteTarget
import com.hamonie.glide.palette.BitmapPaletteWrapper
import com.hamonie.util.color.MediaNotificationProcessor
import com.bumptech.glide.request.transition.Transition

abstract class RetroMusicColoredTarget(view: ImageView) : BitmapPaletteTarget(view) {

    protected val defaultFooterColor: Int
        get() = ATHUtil.resolveColor(getView().context, R.attr.colorControlNormal)

    abstract fun onColorReady(colors: MediaNotificationProcessor)

    override fun onLoadFailed(errorDrawable: Drawable?) {
        super.onLoadFailed(errorDrawable)
        val colors = MediaNotificationProcessor(App.getContext(), errorDrawable)
        onColorReady(colors)
    }

    override fun onResourceReady(
        resource: BitmapPaletteWrapper,
        transition: Transition<in BitmapPaletteWrapper>?
    ) {
        super.onResourceReady(resource, transition)
        MediaNotificationProcessor(App.getContext()).getPaletteAsync({
            onColorReady(it)
        }, resource.bitmap)
    }
}
