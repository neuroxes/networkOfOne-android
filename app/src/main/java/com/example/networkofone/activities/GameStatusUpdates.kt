package com.example.networkofone.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.networkofone.adapters.GameUpdatesAdapter
import com.example.networkofone.databinding.ActivityGameStatusUpdatesBinding
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.repo.GameUpdatesRepository
import com.example.networkofone.mvvm.viewModels.GameUpdatesViewModel
import com.example.networkofone.mvvm.viewModels.GameUpdatesViewModelFactory
import com.example.networkofone.utils.NewToastUtil
import kotlinx.coroutines.launch

class GameStatusUpdates : AppCompatActivity() {

    private lateinit var binding: ActivityGameStatusUpdatesBinding
    private lateinit var repository: GameUpdatesRepository
    private lateinit var viewModelFactory: GameUpdatesViewModelFactory
    private val viewModel: GameUpdatesViewModel by viewModels { viewModelFactory }
    private lateinit var updatesAdapter: GameUpdatesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameStatusUpdatesBinding.inflate(layoutInflater)
        setContentView(binding.root) // Your activity layout

        // Initialize repository and ViewModel factory
        repository = GameUpdatesRepository()
        viewModelFactory = GameUpdatesViewModelFactory(repository)

        setupViews()
        setupRecyclerView()
        observeViewModel()

        // Get gameId from intent
        val gameId = intent.getStringExtra("gameId") ?: ""
        if (gameId.isNotEmpty()) {
            viewModel.gameId = gameId
            viewModel.loadNotifications()
        } else {
            showError("Game ID not found")
        }
    }

    private fun setupViews() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {


        // Initialize your adapter here
        updatesAdapter = GameUpdatesAdapter(this, emptyList())
        binding.rcvNotiDate.adapter = updatesAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            // Observe notifications
            viewModel.notifications.collect { notifications ->
                updateUI(notifications)
                // Update your adapter here
                updatesAdapter.updateNotifications(notifications)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                showLoading(isLoading)
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    showError(it)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateUI(notifications: List<Notification>) {
        binding.apply {
            if (notifications.isEmpty()) {
                // Show empty state
                rcvNotiDate.visibility = View.GONE
                progressIndicator.visibility = View.GONE
                layResult.visibility = View.VISIBLE
            } else {
                // Show notifications
                rcvNotiDate.visibility = View.VISIBLE
                progressIndicator.visibility = View.GONE
                layResult.visibility = View.GONE
            }

        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                progressIndicator.visibility = View.VISIBLE
                rcvNotiDate.visibility = View.GONE
                layResult.visibility = View.GONE
            } else {
                progressIndicator.visibility = View.GONE
            }

        }
    }

    private fun showError(message: String) {
        NewToastUtil.showError(this, message)
    }
}