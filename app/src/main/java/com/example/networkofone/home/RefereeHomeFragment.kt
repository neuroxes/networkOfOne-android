package com.example.networkofone.home

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import com.example.networkofone.R
import com.example.networkofone.activities.AuthenticationActivity
import com.example.networkofone.activities.GameDetailActivity
import com.example.networkofone.activities.NotificationActivity
import com.example.networkofone.adapters.RefereeGamesAdapter
import com.example.networkofone.databinding.DialogLogoutBinding
import com.example.networkofone.databinding.FragmentHomeRefereeBinding
import com.example.networkofone.databinding.LayoutProvidePaymentDetailBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.NotificationTypeLocal
import com.example.networkofone.mvvm.models.PaymentMethod
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import com.example.networkofone.mvvm.repo.NotificationRepository
import com.example.networkofone.mvvm.viewModels.GameUiState
import com.example.networkofone.mvvm.viewModels.HomeViewModelReferee
import com.example.networkofone.mvvm.viewModels.HomeViewModelRefereeFactory
import com.example.networkofone.utils.ActivityNavigatorUtil
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.GameSorter
import com.example.networkofone.utils.LoadingDialog
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.NotificationUtil
import com.example.networkofone.utils.NumberFormatterUtil
import com.example.networkofone.utils.SharedPrefManager
import com.firebase.ui.auth.AuthUI
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.incity.incity_stores.AppFragmentLoader
import java.util.Calendar
import kotlin.random.Random

class RefereeHomeFragment(
    private val context: AppCompatActivity,
    private val verifyLocationForCheckIn: (Double, Double) -> Unit,
) : AppFragmentLoader(R.layout.fragment_root_nested_scroll_view) {
    private lateinit var binding: FragmentHomeRefereeBinding
    private lateinit var base: NestedScrollView
    private lateinit var viewModel: HomeViewModelReferee
    private var userModel: UserModel? = null
    private lateinit var gamesAdapter: RefereeGamesAdapter

    private lateinit var loader: LoadingDialog
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

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
        loader = LoadingDialog(context)
        settingUpBinding()
        checkAndRequestNotificationPermission()
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
        setupUnreadCountListener()
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
            game.refereeName = userModel?.name
            game.acceptedByRefereeId = userModel?.id
            viewModel.updateGame(game, GameStatus.ACCEPTED)
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

    @SuppressLint("SetTextI18n")
    private fun initiatePayoutRequest(game: GameData) {
        val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
            context, LayoutProvidePaymentDetailBinding::inflate
        )
        dialog.show()
        dialogBinding.apply {
            tvAmount.text = "$${NumberFormatterUtil.format(game.feeAmount)}"
            ivBack.setOnClickListener { dialog.dismiss() }
            btnCancel.setOnClickListener { dialog.dismiss() }
            paymentMethodRadioGroup.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.rBtn1 -> {
                        lay2.visibility = VISIBLE
                        setPaymentMethod(
                            "XRPL Address",
                            "Enter your XRPL wallet address",
                            R.drawable.round_currency_bitcoin_24
                        )
                    }

                    R.id.rBtn2 -> {
                        lay2.visibility = GONE
                        setPaymentMethod(
                            "Bank Account Details",
                            "Enter your bank account number",
                            R.drawable.bank
                        )
                    }

                    R.id.rBtn3 -> {
                        lay2.visibility = GONE
                        setPaymentMethod(
                            "Card Details", "Enter your credit card number", R.drawable.cvv_card
                        )
                    }

                    R.id.rBtn4 -> {
                        lay2.visibility = GONE
                        setPaymentMethod(
                            "Payment Details", "Enter your payment details", R.drawable.sack_dollar
                        )
                    }
                }
            }
            add1.setOnClickListener { etAccountDetail.setText(add1.text) }
            add2.setOnClickListener { etAccountDetail.setText(add2.text) }
            add3.setOnClickListener { etAccountDetail.setText(add3.text) }
            add4.setOnClickListener { etAccountDetail.setText(add4.text) }


            btnSave.setOnClickListener {
                if (isDataValid()) {
                    val paymentDetail = PaymentRequestData(
                        gameId = game.id,
                        gameName = game.title,
                        refereeId = userModel?.id ?: "Null",
                        id = "",
                        refereeName = userModel?.name ?: "Null",
                        schedularName = game.schedularName,
                        schedularId = game.createdBySchoolId,
                        amount = game.feeAmount,
                        paymentMethod = when (paymentMethodRadioGroup.checkedRadioButtonId) {
                            R.id.rBtn1 -> PaymentMethod.XRPL
                            R.id.rBtn2 -> PaymentMethod.BANK_TRANSFER
                            R.id.rBtn3 -> PaymentMethod.PAYPAL
                            R.id.rBtn4 -> PaymentMethod.VENMO
                            else -> PaymentMethod.NONE
                        },
                    )
                    loader.startLoadingAnimation()
                    viewModel.createPaymentRequest(paymentDetail)
                    dialog.dismiss()
                }
            }
        }
    }

    fun LayoutProvidePaymentDetailBinding.isDataValid(): Boolean {
        val accountDetail = etAccountDetail.getText().toString().trim()
        if (accountDetail.isEmpty()) {
            etLayPrice.error = "Required"
            return false
        }
        etLayPrice.error = null
        return true
    }

    fun LayoutProvidePaymentDetailBinding.setPaymentMethod(
        title: String,
        hint: String,
        iconRes: Int,
    ) {
        t4.text = title
        etAccountDetail.hint = hint
        etAccountDetail.setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(
                context, iconRes
            ), null, null, null
        )
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
            gamesAdapter.submitList(GameSorter.sortGamesByDateTime(games))


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
                NewToastUtil.showSuccess(context, "Action Completed")
            } else {
                NewToastUtil.showError(context, "Something went wrong")
            }
        }

        viewModel.paymentRequestResult.observe(context) { result ->
            loader.endLoadingAnimation()
            if (result.isSuccess) {
                viewModel.observeGames()
                NewToastUtil.showSuccess(context, "Payment request submitted successfully!")
            } else {
                NewToastUtil.showError(
                    context,
                    "Failed to submit payment request: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    private fun navigateToGoogleMaps(latitude: Double, longitude: Double) {
        val gmmIntentUri = "geo:$latitude,$longitude?q=$latitude,$longitude(Location)".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)
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


    private fun showDeleteConfirmationDialog(game: GameData) {
        AlertDialog.Builder(context).setTitle("Delete Game")
            .setMessage("Are you sure you want to delete this game?")
            .setPositiveButton("Delete") { _, _ ->

            }.setNegativeButton("Cancel", null).show()
    }

    fun onCheckInAttempt(isWithinRange: Float) {
        if (isWithinRange < 100) {
            viewModel.checkInGame?.let {
                viewModel.updateGame(it, GameStatus.CHECKED_IN)
            }
        } else {
            NewToastUtil.showError(
                context,
                "You are not in the check-in range of the game location. Distance $isWithinRange"
            )
        }
    }


    private fun setupUnreadCountListener() {
        notificationRepository.getUnreadNotificationCountRealtime(userModel!!.userType) { notificationsList ->
            context.runOnUiThread {
                updateNotificationBadge(notificationsList.size)
                notificationsList.forEach { it ->
                    NotificationUtil.showSystemNotification(
                        context = context,
                        notificationId = Random.nextInt(),
                        title = it.title,
                        message = it.message,
                        largeIconResId = when (it.type) {
                            NotificationTypeLocal.ACCEPTED -> R.drawable.check_circle
                            NotificationTypeLocal.PENDING -> R.drawable.pending
                            NotificationTypeLocal.PAYMENT_REQUESTED -> R.drawable.sack_dollar_notification
                            NotificationTypeLocal.REJECTED -> R.drawable.cross_white
                            NotificationTypeLocal.CHECKED_IN -> R.drawable.terms_check
                            NotificationTypeLocal.COMPLETED -> R.drawable.check_circle
                            null -> null
                        }
                    )
                }
            }
        }
    }

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationUtil.areNotificationsEnabled(context)) {
                if (NotificationUtil.shouldShowRequestPermissionRationale(context)) {
                    // Show explanation why you need the permission
                    showPermissionRationaleDialog()
                } else {
                    // Directly request the permission
                    NotificationUtil.requestNotificationPermission(
                        context, NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(context).setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to alert you about important updates.")
            .setPositiveButton("OK") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    NotificationUtil.requestNotificationPermission(
                        context, NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }.setNegativeButton("Cancel", null).show()
    }

    // Handle permission result
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can now show notifications
                } else {
                    // Permission denied
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

    companion object {
        private const val TAG = "Home Frag"
    }
}