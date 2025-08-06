package com.example.networkofone.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import kotlin.collections.forEach
import kotlin.jvm.javaClass
import kotlin.let
import kotlin.text.isNullOrEmpty

class ActivityNavigatorUtil {


    companion object {
        fun startActivity(
            from: Activity,
            to: Class<out Activity>,
            sharedView: View? = null,
            clearStack: Boolean = false,
            bundle: Bundle? = null,
            useHeroAnimation: Boolean = false // New boolean parameter
        ) {
            val intent = Intent(from, to)
            if (clearStack) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            if (bundle != null) {
                bundle.keySet().forEach { key ->
                    when (key) {
                        "img" -> bundle.getString(key)?.let { intent.putExtra(key, it) }
                        "userName" -> bundle.getString(key)?.let { intent.putExtra(key, it) }
                        "userEmail" -> bundle.getString(key)?.let { intent.putExtra(key, it) }
                    }
                }
                Log.e("TAG", "startActivity: Bundle -> $bundle")
            }

            sharedView?.let { view ->
                if (useHeroAnimation) {
                    val transitionName = ViewCompat.getTransitionName(view)
                    if (!transitionName.isNullOrEmpty()) {
                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            from,
                            view,
                            transitionName
                        )
                        from.startActivity(intent, options.toBundle())
                        return
                    }
                } else {
                    // ScaleUp animation (default)
                    val options = ActivityOptionsCompat.makeScaleUpAnimation(
                        view,  // The view to scale from
                        0,     // X start position
                        0,     // Y start position
                        view.width,   // Initial width
                        view.height   // Initial height
                    )
                    from.startActivity(intent, options.toBundle())
                    return
                }
            }

            Log.e("ActivityNavigator", "Starting activity without shared element transition")
            from.startActivity(intent)
        }

    }

    fun logBundleContents(bundle: Bundle) {
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            Log.d(
                "BundleInspector",
                "Key: $key, Value: $value, Type: ${value?.javaClass?.simpleName}"
            )
        }
    }
}