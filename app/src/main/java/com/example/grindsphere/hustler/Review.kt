package com.example.grindsphere.hustler

data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0,
    val serviceId: String = ""
)

data class ServiceWithReviews(
    val service: HustlerServiceCard,
    val reviews: List<Review> = emptyList(),
    val averageRating: Double = 0.0
)
