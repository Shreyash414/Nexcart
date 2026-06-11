package com.example.nexcart.ui.home

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
import com.example.nexcart.databinding.FragmentHomeBinding
import com.example.nexcart.domain.model.Product
import com.example.nexcart.ui.homeScreens.HomeViewModel
import com.example.nexcart.ui.homeScreens.ProductAdapter
import com.example.nexcart.utils.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var productAdapter: ProductAdapter
    private lateinit var recommendedAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
        viewModel.refresh()
    }

    private fun setupRecyclerViews() {
        val onProductClick: (Product) -> Unit = { product ->
            val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product)
            findNavController().navigate(action)
        }

        // Main Products Adapter
        productAdapter = ProductAdapter(onProductClick)
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
        }

        // Recommended Products Adapter
        recommendedAdapter = ProductAdapter(onProductClick, isHorizontal = true)
        binding.rvRecommended.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendedAdapter
        }
    }

    private fun observeViewModel() {
        // Observe All Products
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { state ->
                    when (state) {
                        is UiState.Idle -> {
                            binding.progressBar.isVisible = false
                        }
                        is UiState.Loading -> {
                            binding.progressBar.isVisible = true
                        }
                        is UiState.Success<*> -> {
                            binding.progressBar.isVisible = false
                            @Suppress("UNCHECKED_CAST")
                            val data = state.data as? List<Product>
                            data?.let { productAdapter.submitList(it) }
                        }
                        is UiState.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Observe Recommended Products
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recommendedProducts.collect { state ->
                    when (state) {
                        is UiState.Idle -> {
                            binding.tvRecommendedHeader.isVisible = false
                            binding.rvRecommended.isVisible = false
                        }
                        is UiState.Loading -> {
                            // Optionally show a separate shimmer/loader
                        }
                        is UiState.Success<*> -> {
                            binding.tvRecommendedHeader.isVisible = true
                            binding.rvRecommended.isVisible = true
                            @Suppress("UNCHECKED_CAST")
                            val data = state.data as? List<Product>
                            data?.let { recommendedAdapter.submitList(it) }
                        }
                        is UiState.Error -> {
                            binding.tvRecommendedHeader.isVisible = false
                            binding.rvRecommended.isVisible = false
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
