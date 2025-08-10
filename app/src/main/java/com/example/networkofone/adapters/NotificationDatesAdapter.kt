package com.example.networkofone.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.networkofone.databinding.ItemDateNotificationBinding
import com.example.networkofone.mvvm.models.NotificationDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationDatesAdapter(
    private val context: Context,
    private var notificationDates: List<NotificationDate>,
) : RecyclerView.Adapter<NotificationDatesAdapter.NotificationDateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationDateViewHolder {
        val binding =
            ItemDateNotificationBinding.inflate(LayoutInflater.from(context), parent, false)
        return NotificationDateViewHolder(binding)
    }

    inner class NotificationDateViewHolder(private val binding: ItemDateNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationDate, context: Context) {
            binding.tvDate.text = getFormattedDate(notification.date)
            binding.rcvNotifications.adapter =
                NotificationsAdapter(context, notification.notifications)
        }

        private fun getFormattedDate(timestamp: String): String {
            try {
                // First parse the input string (assuming format "yyyy-MM-dd")
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(timestamp) ?: return timestamp

                // Then format it to the desired output
                val outputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                val formattedDate = outputFormat.format(date)

                return when {
                    isToday(date) -> "Today"
                    isYesterday(date) -> "Yesterday"
                    else -> formattedDate
                }
            } catch (e: Exception) {
                return timestamp // return original if parsing fails
            }
        }

        private fun isToday(date: Date): Boolean {
            val today = Calendar.getInstance()
            val inputDate = Calendar.getInstance().apply { time = date }
            return today.get(Calendar.YEAR) == inputDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == inputDate.get(
                Calendar.DAY_OF_YEAR
            )
        }

        private fun isYesterday(date: Date): Boolean {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
            val inputDate = Calendar.getInstance().apply { time = date }
            return yesterday.get(Calendar.YEAR) == inputDate.get(Calendar.YEAR) && yesterday.get(
                Calendar.DAY_OF_YEAR
            ) == inputDate.get(Calendar.DAY_OF_YEAR)
        }
    }

    override fun onBindViewHolder(holder: NotificationDateViewHolder, position: Int) {
        holder.bind(notificationDates[position], context)
    }

    override fun getItemCount(): Int = notificationDates.size

    fun updateData(newNotificationDates: List<NotificationDate>) {
        notificationDates = newNotificationDates
        notifyDataSetChanged()
    }


}