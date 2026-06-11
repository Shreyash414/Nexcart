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
    val image: String = "",
    val rating: Rating = Rating(),
    val sellerId: String = ""
) : Parcelable

@Parcelize
data class Rating(
    val rate: Double = 0.0,
    val count: Int = 0
) : Parcelable
