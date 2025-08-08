package com.example.networkofone.authentication

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.networkofone.MainActivityScheduler
import com.example.networkofone.activities.MainActivityReferee
import com.example.networkofone.databinding.DialogForgotPasswordBinding
import com.example.networkofone.databinding.FragmentLoginBinding
import com.example.networkofone.databinding.LayoutProgressDialogBinding
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.viewModels.LoginViewModel
import com.example.networkofone.mvvm.viewModels.LoginViewModel.AuthResult
import com.example.networkofone.utils.ActivityNavigatorUtil
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.SharedPrefManager
import com.example.networkofone.utils.ToastType
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomsheet.BottomSheetDialog


class Login : Fragment() {

    private lateinit var dialogBinding: DialogForgotPasswordBinding
    private lateinit var loadingDialog: Dialog
    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var bottomSheetDialog: BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        animateParentEnter()

        binding.apply {

            btnLogin.setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                if (validateInput(email, password)) {
                    hideKeyboard()
                    viewModel.login(email, password)
                }
            }/*ivGoogle.setOnClickListener {
                signInWithGoogle()
            }*/
            btnForgotPass.setOnClickListener {
                forgotPasswordDialog()
            }

            etPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                    // Not needed
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tLay2.error = null
                }

                override fun afterTextChanged(s: Editable?) {
                    // Not needed
                }
            })
            etEmail.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                    // Not needed
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tLay1.error = null
                }

                override fun afterTextChanged(s: Editable?) {
                    // Not needed
                }
            })
        }



        observeViewModel()
        return binding.root
    }

    private fun forgotPasswordDialog() {
        dialogBinding = DialogForgotPasswordBinding.inflate(LayoutInflater.from(requireContext()))
        bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(dialogBinding.root)

        dialogBinding.apply {
            ivBack.setOnClickListener { bottomSheetDialog.dismiss() }
            btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }
            btnConfirm.setOnClickListener {
                tLay.error = null
                val email = etEmailForgotPass.text.toString().trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tLay.error = "Invalid email address!"
                } else {
                    hideKeyboard(etEmailForgotPass) // Pass the dialog's EditText
                    viewModel.resetPassword(email)
                }

            }
        }
        bottomSheetDialog.show()
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Success -> {
                    if (binding.checkboxRemember.isChecked) keepUserLogged(1)
                    viewModel.showLoadingOnButton(true)
                    viewModel.getUser()
                }

                is AuthResult.EmailNotVerified -> {
                    showToast("Please verify your email before logging in.", ToastType.ERROR)
                }

                is AuthResult.Failure -> {
                    showToast("Login Failed: ${result.message}", ToastType.ERROR)
                }

            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.apply {
                if (isLoading) {
                    progressBar.visibility = View.VISIBLE
                    btnLogin.isEnabled = false
                    btnLogin.text = null
//                    ivGoogle.isEnabled = false
                } else {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
//                    ivGoogle.isEnabled = true
                }
            }
        }
        viewModel.resetStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Success -> {
                    showToast(
                        "Password reset email sent. Check your email inbox", ToastType.SUCCESS
                    )
                    bottomSheetDialog.dismiss()
                }

                is AuthResult.Failure -> {
                    showToast(result.message, ToastType.ERROR)
                }

                AuthResult.EmailNotVerified -> {
                    showToast("Email not verified", ToastType.ERROR)
                }
            }
        }

        viewModel.loadingDialog.observe(viewLifecycleOwner) { isLoading ->
            dialogBinding.apply {
                if (isLoading) {
                    progressBar.visibility = View.VISIBLE
                    btnConfirm.isEnabled = false
                    btnConfirm.text = null
                } else {
                    progressBar.visibility = View.GONE
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirm"
                }
            }
        }

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            viewModel.showLoadingOnButton(false)
            if (user != null) {
                SharedPrefManager(requireContext()).saveUser(user)
                navigateToNextScreen(user.userType)
            } else {
                NewToastUtil.showError(requireContext(), "Something went wrong!")
            }

        }

    }

    private fun validateInput(email: String, password: String): Boolean {
        // Email format check
        binding.apply {
            tLay1.error = null
            tLay2.error = null
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tLay1.error = "Invalid email format."
            return false
        }

        // Prevent SQL Injection: Ensure email only contains valid characters
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$".toRegex()
        if (!emailRegex.matches(email)) {
            binding.tLay1.error = "Invalid characters in email."
            return false
        }

        // Password security checks
        if (password.isEmpty()) {
            binding.tLay2.error = "Password cannot be empty."
            return false
        }

        return true
    }


    private fun showToast(message: String, type: ToastType) {
        NewToastUtil.show(requireContext(), message, type)
    }

    private fun hideKeyboard(focusedView: View? = null) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = focusedView ?: activity?.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun navigateToNextScreen(userType: UserType) {
        val targetActivity = when (userType) {
            UserType.ADMIN -> MainActivityScheduler::class.java
            UserType.SCHOOL -> MainActivityScheduler::class.java
            UserType.REFEREE -> MainActivityReferee::class.java
        }
        ActivityNavigatorUtil.startActivity(requireActivity(), targetActivity, clearStack = true)
        requireActivity().finish()
    }


    private fun signInWithGoogle() {
        val providers = listOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val signInIntent =
            AuthUI.getInstance().createSignInIntentBuilder().setIsSmartLockEnabled(false)
                .setAvailableProviders(providers).build()

        val (loadingDialog, _) = DialogUtil.createTransparentDialogWithBinding(
            context = requireContext(),
            bindingInflater = LayoutProgressDialogBinding::inflate,
            cancelable = false
        )
        this.loadingDialog = loadingDialog
        this.loadingDialog.show()
        launcher.launch(signInIntent)

    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            keepUserLogged(1)
            loadingDialog.dismiss()
            navigateToNextScreen(UserType.SCHOOL)
        } else {
            loadingDialog.dismiss()
            showToast("Request cancelled!", ToastType.ERROR)
        }
    }


    private fun keepUserLogged(value: Int) {
        requireContext().getSharedPreferences("Logged", Context.MODE_PRIVATE).edit {
            putInt("isLogged", value)
        }
    }


    companion object {
        private const val TAG: String = "Login Fragment"
    }


    private fun animateParentEnter() {
        binding.apply {
            tLay1.apply {
                alpha = 0f
                translationY = 20f
                animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }
            tLay2.apply {
                alpha = 0f
                translationY = 20f
                animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }
            checkboxRemember.apply {
                alpha = 0f
                translationY = 20f
                animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }
            btnForgotPass.apply {
                alpha = 0f
                translationY = 20f
                animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }
            btnLogin.apply {
                alpha = 0f
                translationY = 20f
                animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }/*t2.apply {
                alpha = 0f
                translationY = 20f
                animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }
            ivGoogle.apply {
                alpha = 0f
                translationY = 20f
                animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }*/
        }
    }

}
