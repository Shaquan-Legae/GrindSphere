package com.example.grindsphere.models

data class Service(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val images: List<String> = listOf(),
    val banner: String = "",
    val profilePicUrl: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val categories: List<String> = listOf(),
    val bookings: Long = 0L,
    val views: Long = 0L,
    val category: String = "Other",
    val price: Double = 0.0,
    val rating: Double = 0.0
)