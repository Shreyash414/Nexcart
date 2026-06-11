package com.example.nexcart.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val category: String = "",
    /** Primary/thumbnail image URL — kept for adapter & FakeStore API compatibility. */
    val image: String = "",
    /** All product images (min 3 for user-uploaded products). Falls back to [image] when empty. */
    val images: List<String> = emptyList(),
    val rating: Rating = Rating(),
    val sellerId: String = ""
) : Parcelable

@Parcelize
data class Rating(
    val rate: Double = 0.0,
    val count: Int = 0
) : Parcelable
