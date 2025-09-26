package com.example.grindsphere.models

import com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val serviceId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0L
)