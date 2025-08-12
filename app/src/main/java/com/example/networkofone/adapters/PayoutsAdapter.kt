package com.example.networkofone.adapters

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.networkofone.R
import com.example.networkofone.databinding.ItemPayoutsBinding
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.PaymentStatus
import com.example.networkofone.utils.NumberFormatterUtil
import com.google.firebase.auth.FirebaseAuth


class PayoutsAdapter(
    private val onAcceptClick: (PaymentRequestData) -> Unit,
    private val onRejectClick: (PaymentRequestData) -> Unit,
    private val onClick: (PaymentRequestData) -> Unit,
) : ListAdapter<PaymentRequestData, PayoutsAdapter.PayoutViewHolder>(PayoutDiffCallback()) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid

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

        fun bind(paymentRequestData: PaymentRequestData) {
            binding.apply {
                if (paymentRequestData.refereeId == userId) {
                    ivCancel.visibility = GONE
                    ivReject.visibility = GONE
                }
                tvGameName.text = paymentRequestData.gameName
                tvPrice.text = Html.fromHtml(
                    "$${NumberFormatterUtil.format(paymentRequestData.amount)} \u25cf ${paymentRequestData.paymentMethod}",
                    Html.FROM_HTML_MODE_LEGACY
                )
                tvRequestedAt.text = "Created at: ${paymentRequestData.paidAt}"

                // Handle accept button click
                ivCancel.setOnClickListener {
                    onAcceptClick(paymentRequestData)
                }

                // Handle reject button click
                ivReject.setOnClickListener {
                    onRejectClick(paymentRequestData)
                }

                binding.root.setOnClickListener { onClick(paymentRequestData) }

                // Update UI based on status
                when (paymentRequestData.status) {

                    PaymentStatus.PENDING -> {

                        if (paymentRequestData.refereeId == userId) {
                            ivCancel.visibility = GONE
                            ivReject.visibility = GONE
                            tvOrderStatus.apply {
                                visibility = VISIBLE
                                text = "Requested"
                                setTextColor(itemView.context.getColor(R.color.status_pending))
                                backgroundTintList =
                                    itemView.context.getColorStateList(R.color.status_pending_bg)
                            }
                        } else {
                            ivCancel.visibility = VISIBLE
                            ivReject.visibility = VISIBLE
                            tvOrderStatus.visibility = GONE
                        }
                    }

                    PaymentStatus.APPROVED -> {
                        ivCancel.visibility = View.GONE
                        ivReject.visibility = View.GONE
                        tvOrderStatus.apply {
                            visibility = VISIBLE
                            text = "Accepted"
                            setTextColor(itemView.context.getColor(R.color.status_delivered))
                            backgroundTintList =
                                itemView.context.getColorStateList(R.color.status_delivered_bg)
                        }
                    }

                    PaymentStatus.REJECTED -> {
                        ivCancel.visibility = View.GONE
                        ivReject.visibility = View.GONE
                        tvOrderStatus.apply {
                            visibility = VISIBLE
                            text = "Rejected"
                            setTextColor(itemView.context.getColor(R.color.colorError))
                            backgroundTintList =
                                itemView.context.getColorStateList(R.color.status_cancelled_bg)
                        }
                    }

                    PaymentStatus.PAID -> {
                        ivCancel.visibility = View.GONE
                        ivReject.visibility = View.GONE
                        tvOrderStatus.apply {
                            visibility = VISIBLE
                            text = "Paid"
                            setTextColor(itemView.context.getColor(R.color.status_delivered))
                            backgroundTintList =
                                itemView.context.getColorStateList(R.color.status_delivered_bg)
                        }
                    }
                }
            }
        }
    }

    class PayoutDiffCallback : DiffUtil.ItemCallback<PaymentRequestData>() {
        override fun areItemsTheSame(
            oldItem: PaymentRequestData,
            newItem: PaymentRequestData,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: PaymentRequestData,
            newItem: PaymentRequestData,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
