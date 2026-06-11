package com.example.nexcart.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
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

    private val viewModel: SellerViewModel by activityViewModels()
    private lateinit var productAdapter: ProductAdapter
    private lateinit var recommendedAdapter: ProductAdapter

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
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        val onProductClick: (Product) -> Unit = { product ->
            val action = SellerFragmentDirections.actionSellerFragmentToProductDetailFragment(product)
            findNavController().navigate(action)
        }

        // Seller's own products — 2-column grid
        productAdapter = ProductAdapter(onProductClick)
        binding.rvSellerProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
            isNestedScrollingEnabled = false
        }

        // Recommended — horizontal scrolling list
        recommendedAdapter = ProductAdapter(onProductClick, isHorizontal = true)
        binding.rvRecommended.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendedAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_sellerFragment_to_addProductFragment)
        }
    }

    private fun observeViewModel() {
        // Seller's products
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sellerProducts.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.pbLoading.isVisible = true
                            binding.tvEmpty.isVisible = false
                        }
                        is UiState.Success<*> -> {
                            binding.pbLoading.isVisible = false
                            @Suppress("UNCHECKED_CAST")
                            val products = state.data as? List<Product>
                            products?.let {
                                productAdapter.submitList(it)
                                binding.tvEmpty.isVisible = it.isEmpty()
                                binding.rvSellerProducts.isVisible = it.isNotEmpty()
                            }
                        }
                        is UiState.Error -> {
                            binding.pbLoading.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is UiState.Idle -> binding.pbLoading.isVisible = false
                    }
                }
            }
        }

        // Recommended products from FakeStore API
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recommendedProducts.collect { state ->
                    when (state) {
                        is UiState.Success<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val products = state.data as? List<Product>
                            products?.let {
                                recommendedAdapter.submitList(it)
                                binding.tvRecommendedHeader.isVisible = it.isNotEmpty()
                                binding.rvRecommended.isVisible = it.isNotEmpty()
                            }
                        }
                        is UiState.Error -> {
                            binding.tvRecommendedHeader.isVisible = false
                            binding.rvRecommended.isVisible = false
                        }
                        else -> { /* Loading / Idle — keep hidden until data arrives */ }
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
