package com.example.networkofone.activities

import android.content.ContentValues
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.networkofone.R
import com.example.networkofone.databinding.ActivityAdminMainBinding
import com.example.networkofone.databinding.DialogLogoutBinding
import com.example.networkofone.databinding.DialogLogoutBinding.inflate
import com.example.networkofone.mvvm.models.DashboardUiState
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.models.asCurrency
import com.example.networkofone.mvvm.repo.DashboardRepository
import com.example.networkofone.mvvm.viewModels.DashboardViewModel
import com.example.networkofone.mvvm.viewModels.DashboardViewModelFactory
import com.example.networkofone.utils.ActivityNavigatorUtil
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.SharedPrefManager
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import java.util.Calendar

class AdminMainActivity : ComponentActivity() {
    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var base: NestedScrollView
    private var userModel: UserModel? = null
    private val databaseUrl =
        "https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(
            DashboardRepository(
                database = Firebase.database(databaseUrl),
                auth = FirebaseAuth.getInstance(),
                databaseUrl = databaseUrl
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_main_root)
        findViewById<TextView>(R.id.tv_greeting).text = greetingMsg()
        userModel = SharedPrefManager(this).getUser()
        findViewById<ImageView>(R.id.ivLogout).setOnClickListener { setupLogoutDialog() }
        Handler(Looper.getMainLooper()).postDelayed({ initiateLayout() }, 1500)
    }

    private fun initiateLayout() {
        try {
            base = findViewById(R.id.base)
            base.removeAllViews()
            binding = ActivityAdminMainBinding.inflate(layoutInflater,base)

            binding.root.alpha = 0f
            binding.root.translationY = 20f
            binding.root.animate().translationY(0f).alpha(1f).setDuration(500)
                .setInterpolator(FastOutSlowInInterpolator()).start()

            findViewById<TextView>(R.id.tv_user_name).text = userModel?.name ?: "Dear Admin"
            collectUi()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun collectUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        try {
            binding.apply {
                // Live System Metrics
                valueGames.text = state.totalGames.toString()
                valuePayouts.text = state.totalPayoutsCount.toString()

                // Games Analytics
                valuePending.text = state.gamesPending.toString()
                valueAccepted.text = state.gamesAccepted.toString()
                valueCompleted.text = state.gamesCompleted.toString()
                valueCancelled.text = state.gamesCancelled.toString()

                // Payout Analytics
                valueTotalValue.text = state.payoutTotalValue.asCurrency()
                valuePendingPayout.text = state.payoutPendingCount.toString()
                valueCompletedPayout.text = state.payoutCompletedCount.toString()
                valueAvgAmount.text = state.payoutAverageAmount.asCurrency()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
    private fun setupLogoutDialog() {
        val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
            this, DialogLogoutBinding::inflate
        )
        dialog.show()
        dialogBinding.apply {
            btnCancel.setOnClickListener { dialog.dismiss() }
            btnLogout.setOnClickListener {
                clearAllUserSettings()
                val userType = checkSignInMethod()
                Log.e(ContentValues.TAG, "setupLogoutDialog: Logout User type : $userType")
                if (userType == 0) FirebaseAuth.getInstance().signOut()
                else if (userType == 1) AuthUI.getInstance().signOut(this@AdminMainActivity)

                ActivityNavigatorUtil.startActivity(
                    this@AdminMainActivity, AuthenticationActivity::class.java, findViewById(R.id.animator), true
                )
                dialog.dismiss()
                finish()
            }
        }
    }

    private fun clearAllUserSettings() {
        getSharedPreferences("Logged", MODE_PRIVATE).edit { putInt("isLogged", 0) }/*val localDb = LocalDatabaseManager(context,"courseDB")
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
}
