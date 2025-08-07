package com.example.networkofone.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.networkofone.R
import com.example.networkofone.databinding.ItemGameBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus

class GamesAdapter(
    private val onGameClick: (GameData) -> Unit,
    private val onMoreOptionsClick: (GameData) -> Unit
) : ListAdapter<GameData, GamesAdapter.GameViewHolder>(GameDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GameViewHolder(
        private val binding: ItemGameBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatTextViewDrawableApis")
        fun bind(game: GameData) {
            with(binding) {
                gameName.text = game.title
                gameLocation.text = game.location
                gameTime.text = "${game.date} ${game.time}"
                gamePrice.text = "$${game.feeAmount}"

                // Set status bar color and note based on game status
                when (game.status) {
                    GameStatus.PENDING -> {
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_pending)
                        )
                        gameStatusNote.apply {
                            text = "Game acceptance request is pending"
                            setBackgroundTintList(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(context, R.color.status_pending_bg)
                                )
                            )
                            setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.round_access_alarm_24, 0, 0, 0
                            )
                            compoundDrawableTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.status_pending)
                            )
                        }
                    }

                    GameStatus.ACCEPTED -> {
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_processing)
                        )
                        gameStatusNote.apply {
                            text = "Game is active and in progress"
                            setBackgroundTintList(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(context, R.color.status_processing_bg)
                                )
                            )
                            compoundDrawableTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.status_processing)
                            )
                        }
                    }

                    GameStatus.COMPLETED -> {
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_confirmed)
                        )
                        gameStatusNote.apply {
                            text = "Game completed successfully"
                            setBackgroundTintList(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(context, R.color.status_confirmed_bg)
                                )
                            )
                            setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.check_circle, 0, 0, 0
                            )
                            compoundDrawableTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.status_confirmed)
                            )
                        }
                    }

                    GameStatus.REJECTED -> {
                        statusBar.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.status_cancelled)
                        )
                        gameStatusNote.apply {
                            text = "Game request was rejected"
                            setBackgroundTintList(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(context, R.color.status_cancelled_bg)
                                )
                            )
                            setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.warning, 0, 0, 0
                            )
                            compoundDrawableTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.status_cancelled)
                            )
                        }
                    }
                }

                // Set click listeners
                root.setOnClickListener { onGameClick(game) }
                moreOptions.setOnClickListener { onMoreOptionsClick(game) }
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