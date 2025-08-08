package com.example.networkofone.home

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import com.example.networkofone.R
import com.example.networkofone.activities.GameDetailActivity
import com.example.networkofone.adapters.RefereeGamesAdapter
import com.example.networkofone.databinding.FragmentHomeRefereeBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import com.example.networkofone.mvvm.viewModels.GameUiState
import com.example.networkofone.mvvm.viewModels.HomeViewModelReferee
import com.example.networkofone.mvvm.viewModels.HomeViewModelRefereeFactory
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.SharedPrefManager
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.incity.incity_stores.AppFragmentLoader
import java.util.Calendar

class HomeFragmentReferee(
    private val context: AppCompatActivity,
    private val verifyLocationForCheckIn: (Double, Double) -> Unit,
) : AppFragmentLoader(R.layout.fragment_store_info_root) {
    private lateinit var binding: FragmentHomeRefereeBinding
    private lateinit var base: NestedScrollView
    private lateinit var viewModel: HomeViewModelReferee
    private var userModel: UserModel? = null
    private lateinit var gamesAdapter: RefereeGamesAdapter

    override fun onCreate() {
        try {
            Handler(Looper.getMainLooper()).postDelayed({ initiateLayout() }, 1000)
        } catch (e: Exception) {
            //noinspection RedundantSuppression
            Log.e(TAG, "initiateData: ${e.message}")
        }
    }

    fun refreshData() {
        viewModel.observeGames()
    }

    private fun initiateLayout() {
        userModel = SharedPrefManager(context).getUser()
        settingUpBinding()
    }

    private fun settingUpBinding() {
        base = find(R.id.base)
        base.removeAllViews()
        binding = FragmentHomeRefereeBinding.inflate(context.layoutInflater, base)
        binding.root.alpha = 0f
        binding.root.translationY = 20f
        binding.root.animate().translationY(0f).alpha(1f).setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator()).start()

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
        binding.apply {
            tvUserName.text = userModel?.name ?: "Referee Dashboard"
            tvGreeting.text = greetingMsg()
        }
    }

    private fun greetingMsg(): String {
        val c = Calendar.getInstance()
        return when (c.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            in 16..20 -> "Good Evening"
            in 21..23 -> "Good Night"
            else -> {
                "Hello"
            }
        }

    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider.create(
            context, HomeViewModelRefereeFactory(GameRepositoryImpl())
        )[HomeViewModelReferee::class.java]
    }

    private fun setupRecyclerView() {
        gamesAdapter = RefereeGamesAdapter(onGameClick = { game ->
            val intent = Intent(context, GameDetailActivity::class.java)
            val gameJson = Gson().toJson(game)
            intent.putExtra("game_data", gameJson)
            context.startActivity(intent)
        }, onAcceptClick = { game ->
            viewModel.updateGame(game.id, GameStatus.ACCEPTED)
        }, onCheckInClick = { game ->
            viewModel.checkInGame = game
            verifyLocationForCheckIn(game.latitude, game.longitude)
        }, onRequestPayout = { game ->
            initiatePayoutRequest(game)
        }, onLocationClicked = { lat, long ->
            navigateToGoogleMaps(lat, long)
        })

        binding.rcvGames.apply {
            adapter = gamesAdapter
        }
    }

    private fun initiatePayoutRequest(game: GameData) {
        TODO("Not yet implemented")
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
                    binding.tvMsg.text =
                        "You haven't added any games to your store. Tap the '+' button to add your first game."
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

        viewModel.updateResult.observe(context) { result ->
            if (result.isSuccess) {
                NewToastUtil.showSuccess(context, "Game accepted!")
            } else {
                NewToastUtil.showError(context, "Something went wrong")
            }
        }

    }

    private fun navigateToGoogleMaps(latitude: Double, longitude: Double) {
        val gmmIntentUri = "geo:$latitude,$longitude?q=$latitude,$longitude(Location)".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)
    }

    private fun showGameOptionsDialog(game: GameData) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(context).setTitle("Game Options").setItems(options) { _, which ->
            when (which) {
                0 -> {
                    // Edit game
                    // Navigate to edit screen or show edit dialog

                    //editGame(game)
                }

                1 -> {
                    // Delete game
                    showDeleteConfirmationDialog(game)
                }
            }
        }.show()
    }

    private fun editGame(game: GameData) {
        // Example: Update game status or other properties
        val updatedGame = game.copy(
            status = when (game.status) {
                GameStatus.PENDING -> GameStatus.ACCEPTED
                GameStatus.ACCEPTED -> GameStatus.COMPLETED
                GameStatus.COMPLETED -> GameStatus.CHECKED_IN
                GameStatus.REJECTED -> GameStatus.PENDING
                GameStatus.CHECKED_IN -> GameStatus.CHECKED_IN
            }
        )
        //viewModel.updateGame(updatedGame)
    }

    private fun showDeleteConfirmationDialog(game: GameData) {
        AlertDialog.Builder(context).setTitle("Delete Game")
            .setMessage("Are you sure you want to delete this game?")
            .setPositiveButton("Delete") { _, _ ->

            }.setNegativeButton("Cancel", null).show()
    }

    fun onCheckInAttempt(isWithinRange: Boolean) {
        if (isWithinRange) {
            viewModel.checkInGame?.let {
                viewModel.updateGame(it.id, GameStatus.CHECKED_IN)
            }
        } else {
            NewToastUtil.showError(
                context, "You are not in the check-in range of the game location."
            )
        }
    }

    companion object {
        private const val TAG = "Home Frag"
    }
}