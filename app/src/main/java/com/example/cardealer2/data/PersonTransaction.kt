package com.example.cardealer2.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

/**
 * Represents a transaction involving a person (Customer, Broker, or Middle Man)
 */
data class PersonTransaction(
    val transactionId: String = "",              // Auto-generated document ID
    val type: String = "",                       // "PURCHASE", "SALE", "EMI_PAYMENT", "BROKER_FEE"
    val personType: String = "",                 // "CUSTOMER", "BROKER", "MIDDLE_MAN"
    val personRef: DocumentReference? = null,    // Reference to Customer or Broker document
    val personName: String = "",                 // Cached name for quick display
    val relatedRef: DocumentReference? = null,   // Reference to Purchase, Sale, or Product
    val amount: Double = 0.0,                    // Transaction amount
    val paymentMethod: String = "",              // "CASH", "BANK", "CREDIT", "MIXED"
    val cashAmount: Double = 0.0,                // Cash portion
    val bankAmount: Double = 0.0,                // Bank portion
    val creditAmount: Double = 0.0,              // Credit portion
    val date: Long = System.currentTimeMillis(), // Transaction date (timestamp as Long)
    val orderNumber: Int? = null,                // Order number (for purchases)
    val description: String = "",                // Human-readable description
    val status: String = "COMPLETED",            // "COMPLETED", "PENDING", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis() // Record creation timestamp
)

// Transaction type constants
object TransactionType {
    const val PURCHASE = "PURCHASE"
    const val SALE = "SALE"
    const val EMI_PAYMENT = "EMI_PAYMENT"
    const val BROKER_FEE = "BROKER_FEE"
}

// Person type constants
object PersonType {
    const val CUSTOMER = "CUSTOMER"
    const val BROKER = "BROKER"
    const val MIDDLE_MAN = "MIDDLE_MAN"
}

// Payment method constants
object PaymentMethod {
    const val CASH = "CASH"
    const val BANK = "BANK"
    const val CREDIT = "CREDIT"
    const val MIXED = "MIXED"
}

// Transaction status constants
object TransactionStatus {
    const val COMPLETED = "COMPLETED"
    const val PENDING = "PENDING"
    const val CANCELLED = "CANCELLED"
}


