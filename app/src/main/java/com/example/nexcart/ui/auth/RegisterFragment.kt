package com.example.nexcart.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nexcart.R
import com.example.nexcart.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val isSeller = binding.cbIsSeller.isChecked
            viewModel.signUp(username, email, password, isSeller)
        }

        binding.tvSignIn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            binding.btnSignUp.isEnabled = false
                        }
                        is AuthUiState.Success -> {
                            binding.btnSignUp.isEnabled = true
                        }
                        is AuthUiState.Error -> {
                            binding.btnSignUp.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetRegisterState()
                        }
                        is AuthUiState.Idle -> {
                            binding.btnSignUp.isEnabled = true
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authEvent.collect { event ->
                    when (event) {
                        is AuthEvent.NavigateToMain -> {
                            if (event.isSeller) {
                                findNavController().navigate(R.id.action_registerFragment_to_sellerFragment)
                            } else {
                                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                            }
                        }
                        is AuthEvent.ShowToast -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.etPassword.inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
        } else {
            binding.etPassword.inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        }
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}