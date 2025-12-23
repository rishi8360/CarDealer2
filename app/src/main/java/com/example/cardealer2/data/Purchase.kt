package com.example.cardealer2.data

import com.google.firebase.firestore.DocumentReference

data class Purchase(
    val purchaseId: String = "",  // Auto-generated document ID
    val grandTotal: Double = 0.0,
    val gstAmount: Double = 0.0,
    val orderNumber: Int = 0,
    val paymentMethods: Map<String, String> = emptyMap(),  // Keys: "bank", "cash", "credit"
    val vehicle: Map<String, String> = emptyMap(),  // Vehicle details as map
    val middleMan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


