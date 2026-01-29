package com.humbleSolutions.cardealer2.data

import com.google.firebase.firestore.DocumentReference

data class Purchase(
    val purchaseId: String = "",  // Auto-generated document ID
    val grandTotal: Double = 0.0,
    val gstAmount: Double = 0.0,
    val orderNumber: Int = 0,
    val transactionNumber: Int = 0,
    val paymentMethods: Map<String, String> = emptyMap(),  // Keys: "bank", "cash", "credit"
    val vehicle: Map<String, String> = emptyMap(),  // Vehicle details as map
    val middleMan: String = "",
    val ownerRef: DocumentReference? = null,  // Reference to owner document
    val brokerRef: DocumentReference? = null,  // Reference to broker/middle man document
    val createdAt: Long = System.currentTimeMillis()
)


