package com.example.networkofone.adapters

import android.view.LayoutInflater
import android.view.View.VISIBLE
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.networkofone.R
import com.example.networkofone.databinding.ItemPayoutsBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus


class PayoutsAdapter(
    private val onAcceptClick: (GameData) -> Unit, private val onRejectClick: (GameData) -> Unit
) : ListAdapter<GameData, PayoutsAdapter.PayoutViewHolder>(PayoutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayoutViewHolder {
        val binding = ItemPayoutsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PayoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PayoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PayoutViewHolder(private val binding: ItemPayoutsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(gameData: GameData) {
            binding.apply {
                tvGameName.text = gameData.title
                tvGameLocation.text = gameData.feeAmount

                // Handle accept button click
                ivCancel.setOnClickListener {
                    onAcceptClick(gameData)
                }

                // Handle reject button click
                ivReject.setOnClickListener {
                    onRejectClick(gameData)
                }

                // Update UI based on status
                when (gameData.status) {
                    GameStatus.PENDING -> {
                        ivCancel.visibility = VISIBLE
                        ivReject.visibility = VISIBLE
                    }

                    GameStatus.ACCEPTED -> {
                        ivCancel.visibility = View.GONE
                        ivReject.visibility = View.GONE
                        tvOrderStatus.apply {
                            visibility = VISIBLE
                            text = "Accepted"
                            setTextColor(itemView.context.getColor(R.color.status_delivered))
                            backgroundTintList = itemView.context.getColorStateList(R.color.status_delivered_bg)
                        }
                    }

                    GameStatus.REJECTED -> {
                        ivCancel.visibility = View.GONE
                        ivReject.visibility = View.GONE
                        tvOrderStatus.apply {
                            visibility = VISIBLE
                            text = "Rejected"
                            setTextColor(itemView.context.getColor(R.color.colorError))
                            backgroundTintList = itemView.context.getColorStateList(R.color.status_cancelled_bg)
                        }
                    }

                    GameStatus.COMPLETED -> {
                        ivCancel.visibility = View.GONE
                        ivReject.visibility = View.GONE
                    }

                    GameStatus.CHECKED_IN -> {
                        ivCancel.visibility = View.GONE
                        ivReject.visibility = View.GONE
                    }
                }
            }
        }
    }

    class PayoutDiffCallback : DiffUtil.ItemCallback<GameData>() {
        override fun areItemsTheSame(oldItem: GameData, newItem: GameData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GameData, newItem: GameData): Boolean {
            return oldItem == newItem
        }
    }
}
