package com.incity.incity_stores

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewbinding.ViewBinding

class AppFragment @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : FrameLayout (context, attrs, defStyleAttr, defStyleRes) {
    @LayoutRes private var layoutLoaded: Int = 0
    var onAppFragmentLoader: AppFragmentLoader? = null
    private fun load () {
        // animate show
        alpha = 0f; isVisible = true; translationY = 50f
        animate().alpha(1f).translationY(0f).setDuration(500).setInterpolator(FastOutSlowInInterpolator()).start()

        if (onAppFragmentLoader == null) return
        if (layoutLoaded == onAppFragmentLoader?.layout) return
        removeAllViews()
        isVisible = false
        onAppFragmentLoader?.apply {
            LayoutInflater.from(context).inflate(layout, this@AppFragment, true).post {
                create(this@AppFragment)
                layoutLoaded = layout
                load()
            }
        }
    }
    fun visible (show: Boolean) = if (show) load() else isVisible = false
}
// this one is simpler form without providing any extra methods .. oky? yes
//abstract class AppFragmentLoader2 (@LayoutRes val layout: Int) { abstract fun onCreate (v: View) }
//this is the container code just like as in fragment

abstract class AppFragmentLoader(@LayoutRes val layout: Int) {

    var view: View? = null
        private set

    private var isInitialized = false

    val v: View
        get() = view!!

    fun create(v: View) {
        if (view == null) {
            view = v
            if (!isInitialized) {
                isInitialized = true
                onCreate()
            }
        }
    }

    abstract fun onCreate()

    fun <T : View> find(@IdRes id: Int) = v.findViewById<T>(id)

    fun findView(@IdRes id: Int) = v.findViewById<View>(id)
}
