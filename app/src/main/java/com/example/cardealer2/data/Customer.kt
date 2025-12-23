package com.example.cardealer2.data
data class Customer(
    val customerId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val photoUrl: List<String> = emptyList(),
    val idProofType: String = "",
    val idProofNumber: String = "",
    val idProofImageUrls: List<String> = emptyList(), // Changed to List<String>
    val amount: Int = 0,
    val createdAt: Long = 0L
)
