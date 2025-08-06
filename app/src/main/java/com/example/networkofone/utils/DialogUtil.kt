package com.example.networkofone.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.widget.TooltipCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.viewbinding.ViewBinding
import com.example.networkofone.R
import com.google.android.material.bottomsheet.BottomSheetDialog

object DialogUtil {

    /**
     * Creates a transparent dialog using ViewBinding.
     * @param context The context
     * @param bindingInflater A lambda that inflates your specific ViewBinding
     * @param cancelable Whether the dialog is cancelable
     * @return Pair of Dialog and its ViewBinding instance
     */
    fun <T : ViewBinding> createTransparentDialogWithBinding(
        context: Context,
        bindingInflater: (LayoutInflater) -> T,
        cancelable: Boolean = true
    ): Pair<Dialog, T> {
        val inflater = LayoutInflater.from(context)
        val binding = bindingInflater(inflater)

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(cancelable)

        // Transparent background
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // Calculate 20dp in pixels
        val horizontalMarginPx = (20 * context.resources.displayMetrics.density).toInt()

        // Full width optional
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams

        (binding.root.parent as? ViewGroup)?.setPadding(
            horizontalMarginPx,
            0,
            horizontalMarginPx,
            0
        )
        return Pair(dialog, binding)
    }

    fun <T : ViewBinding> createBottomDialogWithBinding(
        context: Context,
        bindingInflater: (LayoutInflater) -> T,
        cancelable: Boolean = true
    ): Pair<BottomSheetDialog, T> {
        val inflater = LayoutInflater.from(context)
        val binding = bindingInflater(inflater)

        val dialog = BottomSheetDialog(context)
        dialog.setContentView(binding.root)
        dialog.setCancelable(cancelable)

        return Pair(dialog, binding)
    }


    fun createTransparentDialog(
        context: Context,
        contentView: Int = R.layout.layout_progress_dialog,
        cancelable: Boolean = true,
        fullWidth: Boolean = true
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(contentView)
        dialog.setCancelable(cancelable)

        // Transparent background
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        if (fullWidth) {
            // Calculate 20dp in pixels for margins
            val horizontalMarginPx = (20 * context.resources.displayMetrics.density).toInt()

            // Set full width with margins
            val layoutParams = WindowManager.LayoutParams().apply {
                copyFrom(dialog.window?.attributes)
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            dialog.window?.attributes = layoutParams

            // Apply margins to the parent view
            /*(contentView.rootView.parent as? ViewGroup)?.setPadding(
                horizontalMarginPx,
                0,
                horizontalMarginPx,
                0
            )*/
        }

        return dialog
    }

    fun <T : ViewBinding> createCenteredTransparentDialogWithBinding(
        context: Context,
        anchorView: View,
        bindingInflater: (LayoutInflater) -> T,
        cancelable: Boolean = true
    ): Pair<Dialog, T> {
        val inflater = LayoutInflater.from(context)
        val binding = bindingInflater(inflater)

        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            setCancelable(cancelable)

            // Transparent background
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            // Calculate 20dp in pixels
            val horizontalMarginPx = (20 * context.resources.displayMetrics.density).toInt()

            // Full width optional
            val layoutParams = WindowManager.LayoutParams().apply {
                copyFrom(window?.attributes)
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            window?.attributes = layoutParams

            (binding.root.parent as? ViewGroup)?.setPadding(
                horizontalMarginPx,
                0,
                horizontalMarginPx,
                0
            )

            // Wait for window to be attached and measured
            binding.root.post {
                // Calculate center position relative to anchor view
                val location = IntArray(2)
                anchorView.getLocationOnScreen(location)
                val viewCenterX = location[0] + anchorView.width / 2
                val viewCenterY = location[1] + anchorView.height / 2

                // Get dialog dimensions
                val dialogWidth = binding.root.measuredWidth
                val dialogHeight = binding.root.measuredHeight

                // Adjust window position
                window?.let { window ->
                    val params = window.attributes
                    params.gravity = Gravity.TOP or Gravity.START
                    params.x = viewCenterX - dialogWidth / 2
                    params.y = viewCenterY - dialogHeight / 2
                    window.attributes = params
                }
            }
        }

        return Pair(dialog, binding)
    }

    fun showTooltip(view: View, text: String) {
        TooltipCompat.setTooltipText(view, text)
        view.performLongClick()
    }

}

