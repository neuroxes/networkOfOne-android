package com.example.networkofone.home

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.networkofone.R
import com.example.networkofone.adapters.GamesAdapter
import com.example.networkofone.databinding.FragmentHomeBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import com.example.networkofone.mvvm.viewModels.GameUiState
import com.example.networkofone.mvvm.viewModels.HomeViewModel
import com.example.networkofone.mvvm.viewModels.HomeViewModelFactory
import com.google.android.material.tabs.TabLayout
import com.incity.incity_stores.AppFragmentLoader

class HomeFragment(private val context: AppCompatActivity) :
    AppFragmentLoader(R.layout.fragment_store_info_root) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var base: NestedScrollView
    private lateinit var viewModel: HomeViewModel
    private lateinit var gamesAdapter: GamesAdapter

    override fun onCreate() {
        try {
            Handler(Looper.getMainLooper()).postDelayed({ initiateLayout() }, 1000)
        } catch (e: Exception) {
            //noinspection RedundantSuppression
            Log.e(TAG, "initiateData: ${e.message}")
        }
    }

    private fun initiateLayout() {
        settingUpBinding()
    }

    private fun settingUpBinding() {
        base = find(R.id.base)
        base.removeAllViews()
        binding = FragmentHomeBinding.inflate(context.layoutInflater, base)
        binding.root.alpha = 0f
        binding.root.translationY = 20f
        binding.root.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()

        setupViewModel()
        setupRecyclerView()
        setupTabs()
        setupUI()
        observeViewModel()
        onClicks()
    }

    private fun onClicks() {

    }

    private fun setupUI() {


    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider.create(context, HomeViewModelFactory(GameRepositoryImpl()))[HomeViewModel::class.java]
    }

    private fun setupRecyclerView() {
        gamesAdapter = GamesAdapter(
            onGameClick = { game ->
                // Handle game item click
                // Navigate to game details or edit screen
            },
            onMoreOptionsClick = { game ->
                showGameOptionsDialog(game)
            }
        )

        binding.rcvGames.apply {
            adapter = gamesAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    viewModel.onTabSelected(position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(context) { state ->
            when (state) {
                is GameUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rcvGames.visibility = View.GONE
                    binding.layResult.visibility = View.GONE
                }
                is GameUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rcvGames.visibility = View.VISIBLE
                    binding.layResult.visibility = View.GONE
                }
                is GameUiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rcvGames.visibility = View.GONE
                    binding.layResult.visibility = View.VISIBLE

                    // Update empty state UI
                    binding.tvTitle.text = "No Games Found"
                    binding.tvMsg.text = "You haven't added any games to your store. Tap the '+' button to add your first game."
                }
                is GameUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rcvGames.visibility = View.GONE
                    binding.layResult.visibility = View.VISIBLE

                    // Update error state UI
                    binding.tvTitle.text = "Something went wrong"
                    binding.tvMsg.text = state.message
                }
            }
        }

        viewModel.filteredGames.observe(context) { games ->
            gamesAdapter.submitList(games)

            // Update empty state for filtered results
            if (games.isEmpty() && viewModel.uiState.value is GameUiState.Success) {
                binding.rcvGames.visibility = View.GONE
                binding.layResult.visibility = View.VISIBLE
                binding.tvTitle.text = "No Games Found"
                binding.tvMsg.text = "No games match the selected filter."
            } else if (games.isNotEmpty()) {
                binding.rcvGames.visibility = View.VISIBLE
                binding.layResult.visibility = View.GONE
            }
        }
    }

    private fun showGameOptionsDialog(game: GameData) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(context)
            .setTitle("Game Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Edit game
                        // Navigate to edit screen or show edit dialog
                        editGame(game)
                    }
                    1 -> {
                        // Delete game
                        showDeleteConfirmationDialog(game)
                    }
                }
            }
            .show()
    }

    private fun editGame(game: GameData) {
        // Example: Update game status or other properties
        val updatedGame = game.copy(
            status = when (game.status) {
                GameStatus.PENDING -> GameStatus.ACCEPTED
                GameStatus.ACCEPTED -> GameStatus.COMPLETED
                GameStatus.COMPLETED -> GameStatus.PENDING
                GameStatus.REJECTED -> GameStatus.PENDING
            }
        )
        viewModel.editGame(updatedGame)
    }

    private fun showDeleteConfirmationDialog(game: GameData) {
        AlertDialog.Builder(context)
            .setTitle("Delete Game")
            .setMessage("Are you sure you want to delete this game?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteGame(game.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    companion object{
        private const val TAG = "Home Frag"
    }
}