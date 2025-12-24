package com.example.cardealer2.data

import com.google.firebase.firestore.DocumentReference

data class CapitalTransaction(
    val transactionDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val purchaseReference: DocumentReference? = null,
    val orderNumber: Int = 0
)










