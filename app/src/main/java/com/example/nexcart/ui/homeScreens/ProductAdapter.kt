package com.example.nexcart.ui.homeScreens

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nexcart.databinding.ItemProductBinding
import com.example.nexcart.domain.model.Product

/**
 * @param isHorizontal When true, each card is given a fixed 160dp width so it
 *                     displays correctly in a horizontal RecyclerView instead of
 *                     filling the full viewport.
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val isHorizontal: Boolean = false
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // When used in a horizontal list the root is match_parent which fills the
        // entire viewport width showing only one card. Override to a fixed 160dp.
        if (isHorizontal) {
            val widthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 160f, parent.context.resources.displayMetrics
            ).toInt()
            binding.root.layoutParams = binding.root.layoutParams.apply {
                width = widthPx
            }
        }

        return ProductViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onProductClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.apply {
                tvProductTitle.text = product.title
                tvProductPrice.text = "$${product.price}"
                Glide.with(ivProductImage.context)
                    .load(product.image)
                    .into(ivProductImage)
                root.setOnClickListener {
                    onProductClick(product)
                }
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
    }
}
