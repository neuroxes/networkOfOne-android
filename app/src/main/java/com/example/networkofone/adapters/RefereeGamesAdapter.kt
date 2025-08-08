package com.example.networkofone.adapters

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.networkofone.R
import com.example.networkofone.databinding.ItemRefereeGameBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.utils.NumberFormatterUtil

class RefereeGamesAdapter(
    private val onGameClick: (GameData) -> Unit,
    private val onAcceptClick: (GameData) -> Unit,
    private val onCheckInClick: (GameData) -> Unit,
    private val onRequestPayout: (GameData) -> Unit,
    private val onLocationClicked: (Double, Double) -> Unit,
) : ListAdapter<GameData, RefereeGamesAdapter.GameViewHolder>(GameDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemRefereeGameBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GameViewHolder(
        private val binding: ItemRefereeGameBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatTextViewDrawableApis", "SetTextI18n")
        fun bind(game: GameData) {
            with(binding) {

                val context = itemView.context
                val typedValue = TypedValue()
                context.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary,
                    typedValue,
                    true
                )
                val colorPrimary = typedValue.data

                gameName.text = game.title
                gameLocation.text = game.location
                gameTime.text = "${game.date} ${game.time}"
                gamePrice.text = "$${NumberFormatterUtil.format(game.feeAmount.toDouble())}"

                // Set status bar color and note based on game status
                when (game.status) {
                    GameStatus.PENDING -> {
                        btnAccept.apply {
                            text = "Accept"
                            backgroundTintList =
                                ContextCompat.getColorStateList(itemView.context, colorPrimary)
                        }
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_pending)
                        )
                    }

                    GameStatus.ACCEPTED -> {
                        btnAccept.apply {
                            text = "Check in"
                            backgroundTintList =
                                ContextCompat.getColorStateList(itemView.context, R.color.brand)
                        }
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_processing)
                        )
                    }

                    GameStatus.COMPLETED -> {
                        btnAccept.visibility = GONE
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_confirmed)
                        )
                    }

                    GameStatus.REJECTED -> {
                        btnAccept.visibility = GONE
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_cancelled)
                        )
                    }

                    GameStatus.CHECKED_IN -> {
                        btnAccept.apply {
                            text = "Request Payout"
                            backgroundTintList = ContextCompat.getColorStateList(
                                itemView.context,
                                R.color.status_delivered
                            )
                        }
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_processing)
                        )
                    }
                }

                // Set click listeners
                root.setOnClickListener { onGameClick(game) }
                btnAccept.setOnClickListener {
                    when (btnAccept.text) {
                        "Accept" -> onAcceptClick(game)
                        "Check in" -> onCheckInClick(game)
                        "Request Payout" -> onRequestPayout(game)
                    }
                }
                btnLocation.setOnClickListener { onLocationClicked(game.latitude, game.longitude) }
            }
        }
    }

    private class GameDiffCallback : DiffUtil.ItemCallback<GameData>() {
        override fun areItemsTheSame(oldItem: GameData, newItem: GameData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GameData, newItem: GameData): Boolean {
            return oldItem == newItem
        }
    }
}