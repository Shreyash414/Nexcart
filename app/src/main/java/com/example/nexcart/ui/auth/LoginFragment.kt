package com.example.nexcart.ui.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nexcart.R
import com.example.nexcart.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    // ─────────────────────────────────────────────
    // ViewBinding & ViewModel
    // ─────────────────────────────────────────────

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    // ─────────────────────────────────────────────
    // Google Sign-In
    // ─────────────────────────────────────────────

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    showToast("Google sign-in failed: No token received.")
                }
            } catch (e: ApiException) {
                showToast("Google sign-in failed: ${e.localizedMessage}")
            }
        }
    }

    // ─────────────────────────────────────────────
    // Password Toggle State
    // ─────────────────────────────────────────────

    private var isPasswordVisible = false

    // ─────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignIn()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─────────────────────────────────────────────
    // Google Sign-In Setup
    // ─────────────────────────────────────────────

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    // ─────────────────────────────────────────────
    // Click Listeners
    // ─────────────────────────────────────────────

    private fun setupClickListeners() {

        // Sign In button
        binding.btnSignIn.setOnClickListener { 
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.signInWithEmail(email, password)
        }

        // Google Sign In
        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            viewModel.sendPasswordResetEmail(email)
        }

        // Navigate to Sign Up
        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Password visibility toggle
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility()
        }
    }

    // ─────────────────────────────────────────────
    // Observe ViewModel
    // ─────────────────────────────────────────────

    private fun observeViewModel() {

        // Login state → show/hide loader
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> showLoading(true)
                        is AuthUiState.Success -> showLoading(false)
                        is AuthUiState.Error   -> {
                            showLoading(false)
                            showToast(state.message)
                            viewModel.resetLoginState()
                        }
                        is AuthUiState.Idle    -> showLoading(false)
                    }
                }
            }
        }

        // One-time events → navigation / toasts
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authEvent.collect { event ->
                    when (event) {
                        is AuthEvent.NavigateToMain -> {
                            if (event.isSeller) {
                                findNavController().navigate(R.id.action_loginFragment_to_sellerFragment)
                            } else {
                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                            }
                        }
                        is AuthEvent.NavigateToLogin -> Unit // already here
                        is AuthEvent.ShowToast       -> showToast(event.message)
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Show password
            binding.etPassword.inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
        } else {
            // Hide password
            binding.etPassword.inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        }
        // Move cursor to end
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnSignIn.isEnabled  = !isLoading
        binding.btnGoogle.isEnabled  = !isLoading
        binding.pbLoading.isVisible = isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}