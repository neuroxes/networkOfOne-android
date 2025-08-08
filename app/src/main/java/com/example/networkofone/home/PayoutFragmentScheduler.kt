package com.example.networkofone.home


import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.networkofone.R
import com.example.networkofone.adapters.PayoutsAdapter
import com.example.networkofone.databinding.LayoutPayoutFragmentBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.viewModels.PayoutsUiState
import com.example.networkofone.mvvm.viewModels.PayoutsViewModel
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.SharedPrefManager
import com.incity.incity_stores.AppFragmentLoader

class PayoutFragmentScheduler(
    private val context: AppCompatActivity
) : AppFragmentLoader(R.layout.fragment_root_constraint_layout) {
    private lateinit var binding: LayoutPayoutFragmentBinding
    private lateinit var base: ConstraintLayout
    private var userModel: UserModel? = null

    private lateinit var viewModel: PayoutsViewModel
    private lateinit var payoutsAdapter: PayoutsAdapter

    override fun onCreate() {
        try {
            Handler(Looper.getMainLooper()).postDelayed({ initiateLayout() }, 1000)
        } catch (e: Exception) {
            //noinspection RedundantSuppression
            Log.e(TAG, "initiateData: ${e.message}")
        }
    }

    fun refreshData() {
        viewModel.loadPayouts()
    }

    private fun initiateLayout() {
        settingUpBinding()
        userModel = SharedPrefManager(context).getUser()
    }

    private fun settingUpBinding() {
        base = find(R.id.root)
        base.removeAllViews()
        binding = LayoutPayoutFragmentBinding.inflate(context.layoutInflater, base)
        binding.root.alpha = 0f
        binding.root.translationY = 20f
        binding.root.animate().translationY(0f).alpha(1f).setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator()).start()

        setupViewModel()
        setupRecyclerView()
        setupSearchView()
        observeUiState()
    }

    private fun onClicks() {

    }

    private fun setupUI() {
        binding.apply {

        }
    }


    private fun setupViewModel() {
        viewModel = ViewModelProvider(context)[PayoutsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        payoutsAdapter = PayoutsAdapter(onAcceptClick = { gameData ->
            viewModel.acceptPayout(gameData.id)
        }, onRejectClick = { gameData ->
            showRejectConfirmationDialog(gameData)
        })

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
                    binding.progressBar.visibility = View.VISIBLE
                    binding.cardData.visibility = View.GONE
                    binding.layResult.visibility = View.GONE
                }

                is PayoutsUiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.cardData.visibility = View.GONE
                    binding.layResult.visibility = View.VISIBLE
                }

                is PayoutsUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.cardData.visibility = View.VISIBLE
                    binding.layResult.visibility = View.GONE
                    payoutsAdapter.submitList(state.payouts)
                }

                is PayoutsUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.cardData.visibility = View.GONE
                    binding.layResult.visibility = View.VISIBLE
                    // You might want to show error message in tvMsg
                    binding.tvMsg.text = state.message
                }
            }
        }
    }


    private fun showRejectConfirmationDialog(game: GameData) {
        AlertDialog.Builder(context).setTitle("Reject Payout")
            .setMessage("Are you sure you want to reject this payout?")
            .setPositiveButton("Reject") { _, _ ->
                if (viewModel.rejectPayout(game.id).value == true) {
                    NewToastUtil.showSuccess(context, "Payout rejected.")
                } else {
                    NewToastUtil.showError(context, "Something went wrong")
                }
            }.setNegativeButton("Cancel", null).show()
    }

    companion object {
        private const val TAG = "Payout Frag"
    }
}