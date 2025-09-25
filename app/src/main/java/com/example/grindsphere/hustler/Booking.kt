package com.example.grindsphere.hustler

data class BookingRequest(
    val id: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val hustlerUid: String = "",
    val status: String = "pending", // pending, accepted, declined
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""

)
