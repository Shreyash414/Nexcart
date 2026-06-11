package com.example.nexcart.ui.detail

import android.content.Intent
import android.net.Uri
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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nexcart.R
import com.example.nexcart.databinding.FragmentProductDetailBinding
import com.example.nexcart.databinding.ItemProductImageBinding
import com.example.nexcart.domain.model.Product
import com.example.nexcart.domain.model.User
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailViewModel by viewModels()
    private val args: ProductDetailFragmentArgs by navArgs()

    // ── Image Pager Adapter ───────────────────────────────────────────────
    private inner class ImagePagerAdapter(private val urls: List<String>) :
        RecyclerView.Adapter<ImagePagerAdapter.PagerVH>() {

        inner class PagerVH(val b: ItemProductImageBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = urls.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH =
            PagerVH(ItemProductImageBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(holder: PagerVH, position: Int) {
            Glide.with(requireContext())
                .load(urls[position])
                .placeholder(R.drawable.ic_add_a_photo)
                .into(holder.b.ivPage)
        }
    }
    // ─────────────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        binding.tvTitle.text = product.title
        binding.tvPrice.text = "$${product.price}"
        binding.tvDescription.text = product.description
        binding.tvCategory.text = product.category.uppercase()
        binding.ratingBar.rating = product.rating.rate.toFloat()
        binding.tvRatingCount.text = "(${product.rating.count} reviews)"

        // Build the image list — prefer product.images, fall back to [product.image]
        val imageUrls = product.images.ifEmpty {
            if (product.image.isNotBlank()) listOf(product.image) else emptyList()
        }
        val pagerAdapter = ImagePagerAdapter(imageUrls)
        binding.vpImages.adapter = pagerAdapter

        // Wire dots indicator — only show if more than 1 image
        TabLayoutMediator(binding.tabDots, binding.vpImages) { _, _ -> }.attach()
        binding.tabDots.visibility = if (imageUrls.size > 1) View.VISIBLE else View.GONE

        viewModel.loadSellerInfo(product.sellerId)
    }

    private fun setupClickListeners(product: Product) {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite(product) }
    }

    private fun observeViewModel(productId: String) {
        viewModel.checkFavoriteStatus(productId)

        // Favorite icon — proper heart icons
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite.collect { isFav ->
                    binding.btnFavorite.setImageResource(
                        if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                    )
                }
            }
        }

        // Full seller info
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seller.collect { user -> bindSellerInfo(user) }
            }
        }
    }

    private fun bindSellerInfo(user: User?) {
        binding.tvSellerName.text = user?.name?.takeIf { it.isNotBlank() }
            ?: user?.email?.takeIf { it.isNotBlank() }
            ?: "NexCart Seller"
        binding.tvSellerEmail.text = user?.email ?: ""

        val photoUrl = user?.photoUrl
        if (!photoUrl.isNullOrBlank() && photoUrl != "null") {
            Glide.with(requireContext()).load(photoUrl).placeholder(R.drawable.ic_person)
                .circleCrop().into(binding.ivSellerPhoto)
        } else {
            binding.ivSellerPhoto.setImageResource(R.drawable.ic_person)
        }

        val email = user?.email
        binding.btnContact.setOnClickListener {
            if (!email.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                    putExtra(Intent.EXTRA_SUBJECT, "Enquiry about product")
                }
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
