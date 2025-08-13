package com.example.networkofone.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.networkofone.R
import com.example.networkofone.mvvm.models.NotificationTypeLocal

object NotificationIconFormatter {

    fun styleNotificationIcon(context: Context, imageView: ImageView, type: NotificationTypeLocal) {
        val (bgColorRes, tintColorRes, iconRes) = when (type) {
            /*NotificationTypeLocal.CHECKED_IN -> Triple(
                R.color.order_update_bg_light,
                R.color.order_update_tint_dark,
                R.drawable.cart_arrow_down
            )

            NotificationTypeLocal.PENDING -> Triple(
                R.color.promotion_bg_light, R.color.promotion_tint_dark, R.drawable.chart_histogram
            )

            NotificationTypeLocal.ACCEPTED -> Triple(
                R.color.system_bg_light, R.color.system_tint_dark, R.drawable.bell
            )

            NotificationTypeLocal.PAYMENT_REQUESTED -> Triple(
                R.color.store_update_bg_light, R.color.store_update_tint_dark, R.drawable.refresh
            )

            NotificationTypeLocal.REJECTED -> Triple(
                R.color.delivery_update_bg_light,
                R.color.delivery_update_tint_dark,
                R.drawable.shipping_fast
            )*/

            NotificationTypeLocal.PENDING -> Triple(
                R.color.new_booking_bg_light,
                R.color.new_booking_tint_dark,
                R.drawable.reservation_smartphone
            )
            /*
                        NotificationTypeLocal.CANCELLATION -> Triple(
                            R.color.cancellation_bg_light, R.color.cancellation_tint_dark, R.drawable.cart_minus
                        )*/

            NotificationTypeLocal.ACCEPTED -> Triple(
                R.color.reschedule_bg_light,
                R.color.reschedule_tint_dark,
                R.drawable.calendar_update
            )

            NotificationTypeLocal.PAYMENT_REQUESTED -> Triple(
                R.color.payment_bg_light, R.color.payment_tint_dark, R.drawable.sack_dollar
            )

            NotificationTypeLocal.COMPLETED -> Triple(
                R.color.payment_bg_light, R.color.payment_tint_dark, R.drawable.check_circle
            )

            NotificationTypeLocal.REJECTED -> Triple(
                R.color.review_bg_light, R.color.review_tint_dark, R.drawable.comment_dots
            )

            NotificationTypeLocal.CHECKED_IN -> Triple(
                R.color.reminder_bg_light, R.color.reminder_tint_dark, R.drawable.bell
            )
        }

        // Create circular background
        val background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, bgColorRes))
        }

        // Apply styling
        with(imageView) {
            this.background = background
            setColorFilter(ContextCompat.getColor(context, tintColorRes))
            setImageResource(iconRes)
        }
    }
}