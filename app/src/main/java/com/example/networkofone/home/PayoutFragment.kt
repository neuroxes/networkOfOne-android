package com.example.networkofone.home

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.networkofone.R
import com.example.networkofone.activities.PayoutDetailActivity
import com.example.networkofone.adapters.PayoutsAdapter
import com.example.networkofone.databinding.LayoutPayoutFragmentBinding
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.PaymentStatus
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.models.asCurrency
import com.example.networkofone.mvvm.viewModels.PayoutsUiState
import com.example.networkofone.mvvm.viewModels.PayoutsViewModel
import com.example.networkofone.utils.LoadingDialog
import com.example.networkofone.utils.SharedPrefManager
import com.google.gson.Gson
import com.incity.incity_stores.AppFragmentLoader

class PayoutFragment(
    private val context: AppCompatActivity,
) : AppFragmentLoader(R.layout.fragment_root_nested_scroll_view) {
    private lateinit var binding: LayoutPayoutFragmentBinding
    private lateinit var base: NestedScrollView
    private var userModel: UserModel? = null
    private lateinit var viewModel: PayoutsViewModel
    private lateinit var payoutsAdapter: PayoutsAdapter
    private lateinit var loader: LoadingDialog

    override fun onCreate() {
        try {
            base = find(R.id.base)
            Handler(Looper.getMainLooper()).postDelayed({ initiateLayout() }, 1000)
        } catch (e: Exception) {
            Log.e(TAG, "initiateData: ${e.message}")
        }
    }

    fun refreshData() {
        // With real-time listeners, this method is less critical but still available
        viewModel.refreshData()
    }

    private fun initiateLayout() {
        loader = LoadingDialog(context)
        userModel = SharedPrefManager(context).getUser()
        settingUpBinding()
    }

    private fun settingUpBinding() {
        base.removeAllViews()
        binding = LayoutPayoutFragmentBinding.inflate(context.layoutInflater, base)
        binding.root.alpha = 0f
        binding.root.translationY = 20f
        binding.root.animate().translationY(0f).alpha(1f).setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator()).start()

        binding.ivInfo.setOnClickListener {
            AlertDialog.Builder(context).setTitle("Payouts Information").setMessage(
                "This section displays pending payout requests in real-time. You can:\n" +
                        "- View details of each payout request.\n" +
                        "- Search for specific payouts.\n" +
                        "- Approve or reject pending payouts.\n" +
                        "- Click on a payout to see more details about the associated game.\n" +
                        "- Data updates automatically when changes occur."
            ).setPositiveButton("OK", null).show()
        }

        setupViewModel()
        setupRecyclerView()
        setupSearchView()
        observeUiState()
        setupWithImprovedExtension()

        // Start real-time listening
        startRealTimeUpdates()
    }

    private fun startRealTimeUpdates() {
        // Load payouts with real-time listener
        viewModel.loadPayouts(userModel?.userType)
    }

    private fun setupWithImprovedExtension() {
        base.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            base.stickyHeader(
                v = binding.laySerNm,
                elevationWhenSticky = 12f,
                listener = { isSticky ->
                    Log.d("Sticky", "Search view sticky: $isSticky")
                }
            )
        })
    }

    fun NestedScrollView.stickyHeader(
        v: View,
        applyTranslationZ: Boolean = true,
        baseTranslationZ: Float = 0f,
        autoSelect: Boolean = false,
        elevationWhenSticky: Float = 8f,
        listener: (sticked: Boolean) -> Unit = {},
    ) {
        if (v.top == 0 && v.height == 0) return

        val sticked = scrollY > v.top
        val newY = if (sticked) scrollY.toFloat() - v.top else 0f

        if (newY == v.translationY) return

        v.apply {
            translationY = newY
            if (applyTranslationZ) {
                translationZ = baseTranslationZ + if (sticked) elevationWhenSticky else 0f
            }
            if (autoSelect) {
                isSelected = sticked
            }
        }

        listener(sticked)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(context)[PayoutsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        payoutsAdapter = PayoutsAdapter(
            onAcceptClick = { payout ->
                showConfirmationDialog(payout)
            },
            onRejectClick = { payout ->
                showRejectConfirmationDialog(payout)
            },
            onClick = { payout ->
                val payoutJson = Gson().toJson(payout)
                val intent = Intent(context, PayoutDetailActivity::class.java)
                    .putExtra("payoutData", payoutJson)
                context.startActivity(intent)
            }
        )

        binding.rcvTemplate.apply {
            adapter = payoutsAdapter
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            itemDecoration.setDrawable(context.getDrawable(R.drawable.long_line_bg_color)!!)
            addItemDecoration(itemDecoration)
        }
    }

    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchPayouts(s.toString().trim())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeUiState() {
        viewModel.uiState.observe(context) { state ->
            when (state) {
                is PayoutsUiState.Loading -> {
                    Log.e(TAG, "observeUiState: payouts -> Loading")
                    binding.progressBar.visibility = View.VISIBLE
                    binding.cardData.visibility = View.GONE
                    binding.cardPayout.visibility = View.GONE
                    binding.layResult.visibility = View.GONE
                }

                is PayoutsUiState.Empty -> {
                    Log.e(TAG, "observeUiState: payouts -> Empty")
                    binding.progressBar.visibility = View.GONE
                    binding.cardData.visibility = View.GONE
                    binding.cardPayout.visibility = View.VISIBLE
                    binding.layResult.visibility = View.VISIBLE

                    // Update message based on search state
                    val searchQuery = binding.etSearch.text.toString().trim()
                    binding.tvMsg.text = if (searchQuery.isNotEmpty()) {
                        "No payouts found matching '$searchQuery'"
                    } else {
                        "No payouts available"
                    }
                }

                is PayoutsUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.cardData.visibility = View.VISIBLE
                    binding.layResult.visibility = View.GONE
                    binding.cardPayout.visibility = View.VISIBLE

                    Log.d(TAG, "observeUiState: payouts -> Success with ${state.payouts.size} items")

                    // Update title with count
                    val searchQuery = binding.etSearch.text.toString().trim()
                    binding.t1.text = if (searchQuery.isNotEmpty()) {
                        "Search Results (${state.payouts.size})"
                    } else {
                        "Payouts (${state.payouts.size})"
                    }

                    setupStatisticsData(state.payouts)

                    // Submit sorted list to adapter
                    val sortedPayouts = state.payouts.sortedByDescending { it.requestedAt }
                    payoutsAdapter.submitList(sortedPayouts)
                }

                is PayoutsUiState.Error -> {
                    Log.e(TAG, "observeUiState: payouts -> Error: ${state.message}")
                    binding.progressBar.visibility = View.GONE
                    binding.cardData.visibility = View.GONE
                    binding.cardPayout.visibility = View.VISIBLE
                    binding.layResult.visibility = View.VISIBLE
                    binding.tvMsg.text = "Error: ${state.message}"
                }
            }
        }
    }

    private fun setupStatisticsData(payouts: List<PaymentRequestData>) {
        if (payouts.isEmpty()) {
            // Reset statistics to zero
            binding.apply {
                valueTotalValue.text = 0.0.asCurrency()
                valuePendingPayout.text = "0"
                valueCompletedPayout.text = "0"
                valueCancelled.text = "0"
                valueCompleted.text = "0"
                valueAvgAmount.text = 0.0.asCurrency()
            }
            return
        }

        var totalAmount = 0.0
        var totalPending = 0
        var totalAccepted = 0
        var totalRejected = 0
        var totalPaid = 0

        for (payout in payouts) {
            totalAmount += payout.amount.toDouble()
            when (payout.status) {
                PaymentStatus.PENDING -> totalPending++
                PaymentStatus.APPROVED -> totalAccepted++
                PaymentStatus.REJECTED -> totalRejected++
                PaymentStatus.PAID -> totalPaid++
            }
        }

        binding.apply {
            valueTotalValue.text = totalAmount.asCurrency()
            valuePendingPayout.text = totalPending.toString()
            valueCompletedPayout.text = totalAccepted.toString()
            valueCancelled.text = totalRejected.toString()
            valueCompleted.text = totalPaid.toString()
            valueAvgAmount.text = (totalAmount / payouts.size).asCurrency()
        }
    }

    private fun showRejectConfirmationDialog(payout: PaymentRequestData) {
        AlertDialog.Builder(context)
            .setTitle("Reject Payout")
            .setMessage("Are you sure you want to reject this payout?")
            .setPositiveButton("Reject") { _, _ ->
                loader.startLoadingAnimation()
                viewModel.rejectPayout(payout).observe(context) { success ->
                    loader.endLoadingAnimation()
                    if (!success) {
                        // Show error message if rejection failed
                        AlertDialog.Builder(context)
                            .setTitle("Error")
                            .setMessage("Failed to reject payout. Please try again.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    // No need to manually refresh - real-time listener will update automatically
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showConfirmationDialog(payout: PaymentRequestData) {
        AlertDialog.Builder(context)
            .setTitle("Approve Payout")
            .setMessage("Are you sure you want to approve this payout?")
            .setPositiveButton("Approve") { _, _ ->
                loader.startLoadingAnimation()
                viewModel.acceptPayout(payout).observe(context) { success ->
                    loader.endLoadingAnimation()
                    if (!success) {
                        // Show error message if approval failed
                        AlertDialog.Builder(context)
                            .setTitle("Error")
                            .setMessage("Failed to approve payout. Please try again.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    // No need to manually refresh - real-time listener will update automatically
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Call this method when the fragment is being destroyed to clean up listeners
    fun onDestroy() {
        // ViewModel will handle cleanup in onCleared()
    }

    companion object {
        private const val TAG = "PayoutFragScheduler"
    }
}