package com.example.nexcart.domain.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val email: String? = null,
    val name: String? = null,
    val photoUrl: String? = null,
    @get:PropertyName("seller")
    @set:PropertyName("seller")
    var seller: Boolean = false
)