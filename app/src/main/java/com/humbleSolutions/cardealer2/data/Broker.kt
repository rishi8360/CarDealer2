package com.humbleSolutions.cardealer2.data

data class Broker(
    val brokerId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val idProof: List<String> = emptyList(),
    val address: String = "",
    val brokerBill: List<String> = emptyList(),
    val amount: Int = 0, // Broker fee/amount
    val createdAt: Long = 0L
)





