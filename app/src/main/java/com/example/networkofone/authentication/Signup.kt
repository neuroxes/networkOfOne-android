package com.example.networkofone.authentication

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.networkofone.R
import com.example.networkofone.databinding.FragmentSignupBinding
import com.example.networkofone.databinding.LayoutUserRoleBinding
import com.example.networkofone.mvvm.viewModels.AuthenticationResponses
import com.example.networkofone.mvvm.viewModels.SignupViewModel
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.ToastType


class Signup : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private lateinit var viewModel: SignupViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SignupViewModel::class.java]

        setupObservers()
        binding.btnSignup.setOnClickListener { handleSignup() }
        return binding.root
    }

    private fun handleSignup() {
        binding.apply {
            hideKeyboard()
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString()
            if (validateInput(name, email, phone, password, confirmPassword)) {
                val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
                    requireContext(), LayoutUserRoleBinding::inflate
                )
                dialog.show()
                dialogBinding.apply {
                    layClient.setOnClickListener {
                        layClient.setBackgroundResource(R.drawable.outlined_10dp_round_simple_selected)
                        img1.strokeWidth = 1f
                        img2.strokeWidth = 0f
                        layProf.setBackgroundResource(R.drawable.outlined_10dp_round_simple)
                        btnContinue.setEnabled(true)
                        viewModel.isProf = false
                    }
                    layProf.setOnClickListener {
                        layProf.setBackgroundResource(R.drawable.outlined_10dp_round_simple_selected)
                        layClient.setBackgroundResource(R.drawable.outlined_10dp_round_simple)
                        img1.strokeWidth = 0f
                        img2.strokeWidth = 1f
                        btnContinue.setEnabled(true)
                        viewModel.isProf = true
                    }
                    btnContinue.setOnClickListener {
                        dialog.dismiss()
                        createAccount(name, email, phone, password)
                    }
                }
            }
        }
    }

    private fun createAccount(
        name: String, email: String, phone: String, password: String
    ) {
        binding.apply {
            btnSignup.isEnabled = false
            btnSignup.text = null
            progressBar.visibility = View.VISIBLE
            viewModel.createUser(name, email, phone, password)
        }
    }

    private fun validateInput(
        name: String, email: String, phone: String, password: String, confirmPassword: String
    ): Boolean {
        binding.apply {

            clearErrors()

            val nameRegex = "^[A-Za-z\\s]{2,50}$".toRegex()
            if (!nameRegex.matches(name.trim())) {
                tLay0.error = "Enter a valid name (only letters & spaces, min 2 chars)"
                return false
            }

            // Email validation using regex (additional check)
            val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$".toRegex()
            if (!Patterns.EMAIL_ADDRESS.matcher(email)
                    .matches() || !emailRegex.matches(email.trim())
            ) {
                tLay1.error = "Invalid email format"
                return false
            }

            // Phone number validation (optional, but if present, must be valid)
            val phoneRegex = "^\\+?[1-9]\\d{1,14}$".toRegex() // E.164 format regex
            if (phone.isNotEmpty() && !phoneRegex.matches(phone)) {
                phoneLay.error = "Invalid phone number format"
                return false
            }


            // Password security check (Min 8 chars, 1 letter, 1 number, 1 special char)
            val passwordRegex =
                "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])[A-Za-z\\d\\S]{6,}\$".toRegex()
            if (!passwordRegex.matches(password)) {
                tLay2.error =
                    "Password must have at least 6 chars, 1 letter, 1 number & 1 special character"
                return false
            }

            // Check if passwords match
            if (password != confirmPassword) {
                tLay3.error = "Passwords do not match"
                return false
            }
        }
        clearErrors()
        return true
    }

    private fun clearErrors() {
        binding.apply {
            tLay0.error = null
            tLay1.error = null
            phoneLay.error = null
            tLay2.error = null
            tLay3.error = null
        }
    }


    private fun setupObservers() {
        viewModel.signupState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is AuthenticationResponses.Loading -> {
                    binding.btnSignup.text = null
                    binding.btnSignup.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }

                is AuthenticationResponses.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignup.text = "Signup"
                    binding.btnSignup.isEnabled = true
                    showToast(resource.data, ToastType.SUCCESS)
                    clearFields()
                }

                is AuthenticationResponses.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignup.text = "Signup"
                    binding.btnSignup.isEnabled = true
                    showToast(resource.message, ToastType.ERROR)

                }
            }
        }
    }

    private fun clearFields() {
        binding.apply {
            etEmail.text = null
            etName.text = null
            etPassword.text = null
            etConfirmPassword.text = null
            etConfirmPassword.clearFocus()
            etName.clearFocus()
            etEmail.clearFocus()
            etPassword.clearFocus()
        }
    }

    private fun showToast(message: String, type: ToastType) {
        NewToastUtil.show(requireContext(), message, type)
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity?.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
