package com.example.nexcart.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.nexcart.R
import com.example.nexcart.databinding.FragmentProductDetailBinding
import com.example.nexcart.domain.model.Product
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailViewModel by viewModels()
    private val args: ProductDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val product = args.product
        setupUI(product)
        setupClickListeners(product)
        observeViewModel(product.id)
    }

    private fun setupUI(product: Product) {
        binding.apply {
            tvTitle.text = product.title
            tvPrice.text = "$${product.price}"
            tvDescription.text = product.description
            tvCategory.text = product.category.uppercase()
            ratingBar.rating = product.rating.rate.toFloat()
            tvRatingCount.text = "(${product.rating.count} reviews)"

            Glide.with(requireContext())
                .load(product.image)
                .into(ivProduct)
            
            viewModel.loadSellerInfo(product.sellerId)
        }
    }

    private fun setupClickListeners(product: Product) {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavorite(product)
        }
    }

    private fun observeViewModel(productId: String) {
        viewModel.checkFavoriteStatus(productId)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite.collect { isFavorite ->
                    val icon = if (isFavorite) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                    binding.btnFavorite.setImageResource(icon)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sellerName.collect { name ->
                    binding.tvSellerName.text = name
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
