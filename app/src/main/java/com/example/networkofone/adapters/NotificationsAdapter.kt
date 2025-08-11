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
import com.example.networkofone.databinding.ItemNotificationsBinding
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.utils.NotificationIconFormatter
import com.example.networkofone.R
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val context: Context,
    private var notifications: List<Notification>
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    private val selectedPositions = hashSetOf<Int>() // Track selected positions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationsBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = notifications[position]
        with(holder.binding) {
            tvTitle.text = item.title
            tvMessage.text = Html.fromHtml("<b>${item.userName}</b> -> ${item.message}",Html.FROM_HTML_MODE_LEGACY)
            tvTime.text = getFormattedTime(item.createdAt)
            /*if (item.gameName.isEmpty()) layGameName.gone()
            if (item.refereeName.isEmpty()) layRefereeName.gone()
            tvGameName.text = item.gameName
            tvRefereeName.text = item.refereeName*/
            NotificationIconFormatter.styleNotificationIcon(context, icon, item.type!!)

            parent.setBackgroundColor(
                if (selectedPositions.contains(position)) {
                    ContextCompat.getColor(context, R.color.brandTrans)
                } else {
                    Color.TRANSPARENT
                }
            )

            parent.setOnLongClickListener {
                toggleSelection(position, holder)
                true
            }

            parent.setOnClickListener {
                toggleSelection(position, holder)
            }
        }
    }

    private fun toggleSelection(position: Int, holder: NotificationViewHolder) {
        with(holder.binding) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position)
                parent.setBackgroundColor(Color.TRANSPARENT)
            } else {
                selectedPositions.add(position)
                parent.setBackgroundColor(ContextCompat.getColor(context, R.color.brandTrans))
            }
        }
    }

    private fun getFormattedTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp)
    }

    override fun getItemCount(): Int = notifications.size

    fun View.visible(){
        this.visibility = VISIBLE
    }
    fun View.gone(){
        this.visibility = GONE
    }
    fun View.inVisible(){
        this.visibility = INVISIBLE
    }

    class NotificationViewHolder(val binding: ItemNotificationsBinding) :
        RecyclerView.ViewHolder(binding.root)
}