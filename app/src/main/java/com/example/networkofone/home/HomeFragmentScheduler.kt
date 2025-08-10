package com.example.networkofone.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import com.example.networkofone.R
import com.example.networkofone.activities.AuthenticationActivity
import com.example.networkofone.activities.NotificationActivity
import com.example.networkofone.adapters.GamesAdapter
import com.example.networkofone.databinding.DialogLogoutBinding
import com.example.networkofone.databinding.FragmentHomeBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import com.example.networkofone.mvvm.repo.NotificationRepository
import com.example.networkofone.mvvm.viewModels.GameUiState
import com.example.networkofone.mvvm.viewModels.HomeViewModel
import com.example.networkofone.mvvm.viewModels.HomeViewModelFactory
import com.example.networkofone.utils.ActivityNavigatorUtil
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.SharedPrefManager
import com.firebase.ui.auth.AuthUI
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.incity.incity_stores.AppFragmentLoader
import java.util.Calendar

class HomeFragmentScheduler(
    private val context: AppCompatActivity, private val onGameEditing: (GameData) -> Unit,
) : AppFragmentLoader(R.layout.fragment_root_nested_scroll_view) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var base: NestedScrollView
    private lateinit var viewModel: HomeViewModel
    private var userModel: UserModel? = null
    private lateinit var gamesAdapter: GamesAdapter
    private val notificationRepository = NotificationRepository()


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
        Log.e(TAG, "initiateLayout: $userModel")
        settingUpBinding()
    }

    private fun settingUpBinding() {
        base = find(R.id.base)
        base.removeAllViews()
        binding = FragmentHomeBinding.inflate(context.layoutInflater, base)
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
        setupUnreadCountListener()
    }

    private fun onClicks() {
        binding.apply {
            ivLogout.setOnClickListener { setupLogoutDialog() }
            notification.setOnClickListener {
                context.startActivity(
                    Intent(
                        context, NotificationActivity::class.java
                    ).putExtra("userType", userModel?.userType)
                )
            }
        }
    }

    private fun setupLogoutDialog() {
        val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
            context, DialogLogoutBinding::inflate
        )
        dialog.show()
        dialogBinding.apply {
            btnCancel.setOnClickListener { dialog.dismiss() }
            btnLogout.setOnClickListener {
                clearAllUserSettings()
                val userType = checkSignInMethod()
                Log.e(ContentValues.TAG, "setupLogoutDialog: Logout User type : $userType")
                if (userType == 0) FirebaseAuth.getInstance().signOut()
                else if (userType == 1) AuthUI.getInstance().signOut(context)

                ActivityNavigatorUtil.startActivity(
                    context, AuthenticationActivity::class.java, findView(R.id.animator), true
                )
                dialog.dismiss()
                context.finish()
            }
        }
    }

    private fun clearAllUserSettings() {
        context.getSharedPreferences("Logged", MODE_PRIVATE).edit { putInt("isLogged", 0) }/*val localDb = LocalDatabaseManager(context,"courseDB")
        Log.e(TAG, "course Data Removed : " + localDb.delete())*/
    }


    private fun checkSignInMethod(): Int {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            for (profile in user.providerData) {
                when (profile.providerId) {
                    "password" -> return 0
                    "google.com" -> return 1
                    "phone" -> return 2
                }
            }
        }
        return -1
    }


    private fun setupUI() {
        binding.apply {
            tvUserName.text = userModel?.name ?: "User"
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
            context, HomeViewModelFactory(GameRepositoryImpl())
        )[HomeViewModel::class.java]
    }

    private fun setupRecyclerView() {
        gamesAdapter = GamesAdapter(onGameClick = { game ->
            // Handle game item click
            // Navigate to game details or edit screen
        }, onMoreOptionsClick = { game ->
            showGameOptionsDialog(game)
        })

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
    }

    private fun showGameOptionsDialog(game: GameData) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(context).setTitle("Game Options").setItems(options) { _, which ->
            when (which) {
                0 -> {
                    // Edit game
                    // Navigate to edit screen or show edit dialog
                    onGameEditing(game)
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
                GameStatus.PAYMENT_REQUESTED -> GameStatus.ACCEPTED
            }
        )
        viewModel.editGame(updatedGame)
    }

    private fun showDeleteConfirmationDialog(game: GameData) {
        AlertDialog.Builder(context).setTitle("Delete Game")
            .setMessage("Are you sure you want to delete this game?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteGame(game.id)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun setupUnreadCountListener() {
        notificationRepository.getUnreadNotificationCountRealtime(userModel!!.userType) { listNotifications ->
            context.runOnUiThread {
                updateNotificationBadge(listNotifications.size)
                listNotifications.forEach { it ->
                    showSystemNotification(
                        channelId = "default_channel",
                        channelName = "General Notifications",
                        notificationId = 1,
                        title = it.title,
                        message = it.message,
                        smallIconResId = R.drawable.bell,
                        largeIconResId = R.drawable.logo_transparent
                    )
                }
            }
        }
    }

    private fun updateNotificationBadge(count: Int) {
        // Update your notification badge/indicator
        val badge = binding.noOfNotifications
        if (count > 0) {
            badge.visibility = View.VISIBLE
            badge.text = if (count > 99) "99+" else count.toString()
        } else {
            badge.visibility = View.GONE
        }
    }


    fun showSystemNotification(
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String,
        message: String,
        smallIconResId: Int,
        largeIconResId: Int? = null,
        autoCancel: Boolean = true,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8.0+)
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "App Notifications"
        }
        notificationManager.createNotificationChannel(channel)

        // Create intent for MainActivity
        val intent = Intent(context, NotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Create pending intent
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(smallIconResId)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel) // Dismiss when tapped
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Set large icon if provided
        largeIconResId?.let {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, it))
        }

        // Show notification
        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        private const val TAG = "Home Frag"
    }
}