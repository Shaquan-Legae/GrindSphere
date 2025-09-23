package com.example.grindsphere.hustler


import android.R.attr.rating
import java.io.Serializable

// Add this to a new file or existing data class file

data class Review(
    val id: String = "",
    val serviceId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)




