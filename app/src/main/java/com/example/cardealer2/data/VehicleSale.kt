package com.example.cardealer2.data

import com.google.firebase.firestore.DocumentReference

/**
 * Represents a vehicle sale transaction
 */
data class VehicleSale(
    val saleId: String = "",  // Auto-generated document ID
    val customerRef: DocumentReference? = null,  // Reference to customer document
    val vehicleRef: DocumentReference? = null,  // Reference to vehicle document
    val purchaseType: String = "",  // "EMI", "FULL_PAYMENT" or "DOWN_PAYMENT"
    val purchaseDate: Long = System.currentTimeMillis(),  // Timestamp as Long
    val totalAmount: Double = 0.0,  // Vehicle price
    val downPayment: Double = 0.0,  // If applicable
    val emiDetails: EmiDetails? = null,  // EMI information if purchaseType is "EMI"
    val status: String = "Active",  // "Active", "Completed", "Pending"
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * EMI payment details
 */
data class EmiDetails(
    val interestRate: Double = 0.0,  // Annual interest rate percentage
    val frequency: String = "MONTHLY",  // "MONTHLY", "QUARTERLY", "YEARLY"
    val installmentsCount: Int = 0,  // Total number of installments
    val installmentAmount: Double = 0.0,  // Amount per installment
    val nextDueDate: Long = 0L,  // Next payment due date (timestamp)
    val remainingInstallments: Int = 0,  // Remaining installments
    val lastPaidDate: Long? = null,  // Last payment date (optional)
    val paidInstallments: Int = 0  // Number of installments paid
)




