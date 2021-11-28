package com.hamonie.extensions

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.*
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.appthemehelper.util.TintHelper
import com.hamonie.util.PreferenceUtil
import com.hamonie.util.RetroUtil
import com.afollestad.materialdialogs.utils.MDUtil.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.inflate(@LayoutRes layout: Int): T {
    return LayoutInflater.from(context).inflate(layout, this, false) as T
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.hidden() {
    visibility = View.INVISIBLE
}

fun View.showOrHide(show: Boolean) = if (show) show() else hide()

fun EditText.appHandleColor(): EditText {
    if (PreferenceUtil.materialYou) return this
    TintHelper.colorHandles(this, ThemeStore.accentColor(context))
    return this
}

fun View.translateYAnimate(value: Float): Animator {
    return ObjectAnimator.ofFloat(this, "translationY", value)
        .apply {
            duration = 300
            doOnStart {
                if (value == 0f) {
                    show()
                }
            }
            doOnEnd {
                if (value != 0f) {
                    hide()
                }
            }
            start()
        }
}

fun BottomSheetBehavior<*>.peekHeightAnimate(value: Int) {
    ObjectAnimator.ofInt(this, "peekHeight", value)
        .apply {
            duration = 300
            start()
        }
}

fun View.focusAndShowKeyboard() {
    /**
     * This is to be called when the window already has focus.
     */
    fun View.showTheKeyboardNow() {
        if (isFocused) {
            post {
                // We still post the call, just in case we are being notified of the windows focus
                // but InputMethodManager didn't get properly setup yet.
                val imm =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    requestFocus()
    if (hasWindowFocus()) {
        // No need to wait for the window to get focus.
        showTheKeyboardNow()
    } else {
        // We need to wait until the window gets focus.
        viewTreeObserver.addOnWindowFocusChangeListener(
            object : ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    // This notification will arrive just before the InputMethodManager gets set up.
                    if (hasFocus) {
                        this@focusAndShowKeyboard.showTheKeyboardNow()
                        // It’s very important to remove this listener once we are done.
                        viewTreeObserver.removeOnWindowFocusChangeListener(this)
                    }
                }
            })
    }
}

fun ShapeableImageView.setCircleShape(boolean: Boolean) {
    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        val radius = width / 2f
        shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(radius)
    }
}


/**
 * This will draw our view above the navigation bar instead of behind it by adding margins.
 */
fun View.drawAboveSystemBars(onlyPortrait: Boolean = true) {
    if (onlyPortrait && RetroUtil.isLandscape()) return
    // Create a snapshot of the view's margin state
    val initialMargin = recordInitialMarginForView(this)
    ViewCompat.setOnApplyWindowInsetsListener(
        (this)
    ) { _: View, windowInsets: WindowInsetsCompat ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        // Apply the insets as a margin to the view.
        updateLayoutParams<MarginLayoutParams> {
            leftMargin = initialMargin.left + insets.left
            bottomMargin = initialMargin.bottom + insets.bottom
            rightMargin = initialMargin.right + insets.right
        }
        windowInsets
    }
}

/**
 * This will draw our view above the navigation bar instead of behind it by adding padding.
 */
fun View.drawAboveSystemBarsWithPadding(consume: Boolean = false) {
    val initialPadding = recordInitialPaddingForView(this)

    ViewCompat.setOnApplyWindowInsetsListener(
        (this)
    ) { v: View, windowInsets: WindowInsetsCompat ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            left = initialPadding.left + insets.left,
            bottom = initialPadding.bottom + insets.bottom,
            right = initialPadding.right + insets.right
        )
        if (consume) WindowInsetsCompat.CONSUMED else windowInsets
    }
    requestApplyInsetsWhenAttached()
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun View.drawNextToNavbar() {
    val initialPadding = recordInitialPaddingForView(this)

    ViewCompat.setOnApplyWindowInsetsListener(
        (this)
    ) { v: View, windowInsets: WindowInsetsCompat ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            left = initialPadding.left + insets.left,
            right = initialPadding.right + insets.right
        )
        windowInsets
    }
    requestApplyInsetsWhenAttached()
}

fun View.addBottomInsets() {
    // Create a snapshot of the view's margin state
    val initialMargin = recordInitialMarginForView(this)
    ViewCompat.setOnApplyWindowInsetsListener(
        (this)
    ) { _: View, windowInsets: WindowInsetsCompat ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        // Apply the insets as a margin to the view.
        updateLayoutParams<MarginLayoutParams> {
            bottomMargin = initialMargin.bottom + insets.bottom
        }
        windowInsets
    }
}

data class InitialMargin(
    val left: Int, val top: Int,
    val right: Int, val bottom: Int
)

fun recordInitialMarginForView(view: View) = InitialMargin(
    view.marginLeft, view.marginTop, view.marginRight, view.marginBottom
)


data class InitialPadding(
    val left: Int, val top: Int,
    val right: Int, val bottom: Int
)

fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)
