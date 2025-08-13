package com.example.networkofone.adapters

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.networkofone.R
import com.example.networkofone.databinding.ItemUpdatesBinding
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.models.NotificationTypeLocal
import com.example.networkofone.utils.NotificationIconFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class GameUpdatesAdapter(
    private val context: Context,
    private var notifications: List<Notification>,
) : RecyclerView.Adapter<GameUpdatesAdapter.UpdatesViewHolder>() {

    //private val selectedPositions = hashSetOf<Int>() // Track selected positions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdatesViewHolder {
        val binding = ItemUpdatesBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return UpdatesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpdatesViewHolder, position: Int) {
        val item = notifications[position]
        with(holder.binding) {
            // Map to the correct IDs from item_updates.xml
            tvTitle.text = item.title
            tvMessage.text = Html.fromHtml("${item.message}", Html.FROM_HTML_MODE_LEGACY)
            tvTime.text = getFormattedTime(item.createdAt)

            // Set the notification type as heading (you can customize this based on your needs)
            /*tvHeading.text = when (item.type) {
                NotificationTypeLocal.ACCEPTED -> "Payment Acce"
                NotificationTypeLocal.PENDING -> "Status Changed"
                NotificationTypeLocal.PAYMENT_REQUESTED -> "Payment Requested"
                NotificationTypeLocal.REJECTED -> "Referee Removed"
                NotificationTypeLocal.CHECKED_IN -> "Checked In"
                NotificationTypeLocal.COMPLETED -> "Game Finished"
                null -> "Notification"
            }*/

            // Style the notification icon
            NotificationIconFormatter.styleNotificationIcon(context, icon, item.type!!)

            // Handle selection background
            /*parent.setBackgroundColor(
                if (selectedPositions.contains(position)) {
                    ContextCompat.getColor(context, R.color.brandTrans)
                } else {
                    Color.TRANSPARENT
                }
            )*/

            // Long click listener for selection
            /*parent.setOnLongClickListener {
                toggleSelection(position, holder)
                true
            }*/

            // Click listener for selection
            /*parent.setOnClickListener {
                toggleSelection(position, holder)
            }*/
        }
    }

    /*private fun toggleSelection(position: Int, holder: UpdatesViewHolder) {
        with(holder.binding) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position)
                parent.setBackgroundColor(Color.TRANSPARENT)
            } else {
                selectedPositions.add(position)
                parent.setBackgroundColor(ContextCompat.getColor(context, R.color.brandTrans))
            }
        }
    }*/

    private fun getFormattedTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp)
    }

    override fun getItemCount(): Int = notifications.size

    // Method to update notifications list
    fun updateNotifications(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    // Method to get selected notifications
    /*fun getSelectedNotifications(): List<Notification> {
        return selectedPositions.map { notifications[it] }
    }

    // Method to clear selections
    fun clearSelections() {
        selectedPositions.clear()
        notifyDataSetChanged()
    }*/

    // Method to check if any items are selected
    //fun hasSelections(): Boolean = selectedPositions.isNotEmpty()

    fun View.visible() {
        this.visibility = VISIBLE
    }

    fun View.gone() {
        this.visibility = GONE
    }

    fun View.inVisible() {
        this.visibility = INVISIBLE
    }

    class UpdatesViewHolder(val binding: ItemUpdatesBinding) : RecyclerView.ViewHolder(binding.root)
}
