package com.example.networkofone.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.example.networkofone.R
import kotlin.apply

object NewToastUtil {

    private var toast: Toast? = null

    fun show(context: Context, message: String, type: ToastType = ToastType.SUCCESS) {
        // Cancel previous toast if exists and clear reference
        toast?.cancel()
        toast = null

        // Use application context to avoid memory leaks
        val appContext = context.applicationContext

        val inflater = LayoutInflater.from(appContext)
        val view = inflater.inflate(R.layout.toast_layout, null)

        // Initialize views
        val rootLayout = view.findViewById<ConstraintLayout>(R.id.rootLayout)
        val toastHeading = view.findViewById<TextView>(R.id.t1)
        val toastText = view.findViewById<TextView>(R.id.toast_text)
        val toastIcon = view.findViewById<ImageView>(R.id.start_icon)

        // Set message text
        toastText.text = message

        // Configure based on type
        when (type) {
            ToastType.SUCCESS -> {
                toastHeading.text = "Success:"
                ViewCompat.setBackgroundTintList(
                    rootLayout,
                    ContextCompat.getColorStateList(appContext, R.color.green)
                )
                toastIcon.setImageResource(R.drawable.check_circle)
            }
            ToastType.ERROR -> {
                toastHeading.text = "Error:"
                ViewCompat.setBackgroundTintList(
                    rootLayout,
                    ContextCompat.getColorStateList(appContext, R.color.colorError)
                )
                toastIcon.setImageResource(R.drawable.warning)
            }
            ToastType.INFO -> {
                toastHeading.text = "Info:"
                ViewCompat.setBackgroundTintList(
                    rootLayout,
                    ContextCompat.getColorStateList(appContext, R.color.colorInfo)
                )
                toastIcon.setImageResource(R.drawable.round_info_outline_24_20dp)
            }
            ToastType.WARNING -> {
                toastHeading.text = "Warning:"
                ViewCompat.setBackgroundTintList(
                    rootLayout,
                    ContextCompat.getColorStateList(appContext, R.color.yellow)
                )
                toastIcon.setImageResource(R.drawable.warning)
            }
        }

        // Create and show new toast with application context
        toast = Toast(appContext).apply {
            duration = Toast.LENGTH_LONG
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 20)
            this.view = view
            show()
        }
    }

    // Optional: Method to clear toast reference when no longer needed
    fun clear() {
        toast?.cancel()
        toast = null
    }

    // Convenience methods for each type
    fun showSuccess(context: Context, message: String) = show(context, message, ToastType.SUCCESS)
    fun showError(context: Context, message: String) = show(context, message, ToastType.ERROR)
    fun showInfo(context: Context, message: String) = show(context, message, ToastType.INFO)
    fun showWarning(context: Context, message: String) = show(context, message, ToastType.WARNING)
}

enum class ToastType {
    SUCCESS, ERROR, INFO, WARNING
}