package com.example.nexcart.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nexcart.R
import com.example.nexcart.databinding.FragmentSellerDashboardBinding
import com.example.nexcart.domain.model.Product
import com.example.nexcart.ui.homeScreens.ProductAdapter
import com.example.nexcart.utils.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SellerFragment : Fragment() {

    private var _binding: FragmentSellerDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SellerViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            val action = SellerFragmentDirections.actionSellerFragmentToProductDetailFragment(product)
            findNavController().navigate(action)
        }
        binding.rvSellerProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_sellerFragment_to_addProductFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sellerProducts.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.pbLoading.isVisible = true
                        }
                        is UiState.Success<*> -> {
                            binding.pbLoading.isVisible = false
                            @Suppress("UNCHECKED_CAST")
                            val products = state.data as? List<Product>
                            products?.let {
                                productAdapter.submitList(it)
                                binding.tvEmpty.isVisible = it.isEmpty()
                            }
                        }
                        is UiState.Error -> {
                            binding.pbLoading.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is UiState.Idle -> {
                            binding.pbLoading.isVisible = false
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSellerProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
