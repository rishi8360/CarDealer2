package com.humbleSolutions.cardealer2.data

import com.google.firebase.firestore.DocumentReference

/**
 * Represents a vehicle sale transaction
 */
data class VehicleSale(
    val saleId: String = "",  // Auto-generated document ID
    val customerRef: DocumentReference? = null,  // Reference to customer document
    val vehicleRef: DocumentReference? = null,  // Reference to vehicle document
    val purchaseDate: Long = System.currentTimeMillis(),  // Timestamp as Long
    val totalAmount: Double = 0.0,  // Vehicle price
    val emi: Boolean = false,  // true if EMI, false if full payment
    val status: Boolean = false,  // true for completed, false for pending
    val emiDetailsRef: DocumentReference? = null,  // Reference to EmiDetails document in separate collection
    // Document handover flags
    val nocHandedOver: Boolean = false,
    val rcHandedOver: Boolean = false,
    val insuranceHandedOver: Boolean = false,
    val otherDocsHandedOver: Boolean = false
)

/**
 * EMI payment details - stored in separate EmiDetails collection
 */
data class EmiDetails(
    val vehicleSaleRef: DocumentReference? = null,  // Reference to VehicleSale document (set when creating in Firestore)
    val interestRate: Double = 0.0,  // Annual interest rate percentage
    val frequency: String = "MONTHLY",  // "MONTHLY", "QUARTERLY", "YEARLY"
    val installmentsCount: Int = 0,  // Total number of installments
    val installmentAmount: Double = 0.0,  // Amount per installment
    val nextDueDate: Long = 0L,  // Next payment due date (timestamp)
    val remainingInstallments: Int = 0,  // Remaining installments
    val lastPaidDate: Long? = null,  // Last payment date (optional)
    val paidInstallments: Int = 0,  // Number of installments paid
    val priceWithInterest: Double = 0.0,  // Total price with interest (installmentsCount * installmentAmount)
    val customerRef: DocumentReference? = null,  // Reference to customer document
    val vehicleRef: DocumentReference? = null,  // Reference to vehicle document
    val pendingExtraBalance: Double = 0.0  // Balance when customer pays less than installmentAmount
)
