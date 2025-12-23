package com.example.cardealer2.repository

import android.util.Log
import com.example.cardealer2.data.EmiDetails
import com.example.cardealer2.data.VehicleSale
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

object SaleRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestoreSettings = settings
    }
    
    private const val TAG = "SaleRepository"
    private val saleCollection = db.collection("VehicleSales")
    private val capitalCollection = db.collection("Capital")
    
    // StateFlow for sales
    private val _sales = MutableStateFlow<List<VehicleSale>>(emptyList())
    val sales: StateFlow<List<VehicleSale>> = _sales.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var listenerRegistration: ListenerRegistration? = null
    
    init {
        startListening()
    }
    
    /**
     * Start listening to VehicleSales collection changes
     */
    private fun startListening() {
        listenerRegistration?.remove()
        
        _isLoading.value = true
        _error.value = null
        
        listenerRegistration = saleCollection.addSnapshotListener { snapshot, error ->
            _isLoading.value = false
            
            if (error != null) {
                val errorMsg = error.message ?: "Error loading sales"
                _error.value = errorMsg
                Log.e(TAG, "❌ Error in sale listener: $errorMsg", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                try {
                    val sales = snapshot.documents.mapNotNull { doc ->
                        try {
                            docToVehicleSale(doc)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing sale document ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    _sales.value = sales
                    _error.value = null
                    Log.d(TAG, "✅ Sale listener updated: ${sales.size} sales")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing sale snapshot: ${e.message}", e)
                    _error.value = "Error processing sales: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Convert Firestore document to VehicleSale
     */
    private fun docToVehicleSale(doc: com.google.firebase.firestore.DocumentSnapshot): VehicleSale? {
        return try {
            val emiDetailsMap = doc.get("emiDetails") as? Map<String, Any>
            val emiDetails = emiDetailsMap?.let { map ->
                EmiDetails(
                    interestRate = (map["interestRate"] as? Number)?.toDouble() ?: 0.0,
                    frequency = map["frequency"] as? String ?: "MONTHLY",
                    installmentsCount = (map["installmentsCount"] as? Number)?.toInt() ?: 0,
                    installmentAmount = (map["installmentAmount"] as? Number)?.toDouble() ?: 0.0,
                    nextDueDate = map.getTimestampOrLong("nextDueDate"),
                    remainingInstallments = (map["remainingInstallments"] as? Number)?.toInt() ?: 0,
                    lastPaidDate = map.getTimestampOrLongOrNull("lastPaidDate"),
                    paidInstallments = (map["paidInstallments"] as? Number)?.toInt() ?: 0
                )
            }
            
            VehicleSale(
                saleId = doc.id,
                customerRef = doc.get("customerRef") as? DocumentReference,
                vehicleRef = doc.get("vehicleRef") as? DocumentReference,
                purchaseType = doc.get("purchaseType") as? String ?: "",
                purchaseDate = doc.getTimestampOrLong("purchaseDate"),
                totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                downPayment = (doc.get("downPayment") as? Number)?.toDouble() ?: 0.0,
                emiDetails = emiDetails,
                status = doc.get("status") as? String ?: "Active",
                createdAt = doc.getTimestampOrLong("createdAt")
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error converting document to VehicleSale: ${e.message}", e)
            null
        }
    }
    
    /**
     * Add a new vehicle sale
     */
    suspend fun addSale(
        customerRef: DocumentReference?,
        vehicleRef: DocumentReference?,
        purchaseType: String,
        totalAmount: Double,
        downPayment: Double = 0.0,
        emiDetails: EmiDetails? = null
    ): Result<String> {
        return try {
            val saleDocRef = saleCollection.document()
            val saleId = saleDocRef.id
            val nowTimestamp = Timestamp.now()

            val saleData = hashMapOf<String, Any>(
                "saleId" to saleId,
                "purchaseType" to purchaseType,
                "purchaseDate" to nowTimestamp,
                "totalAmount" to totalAmount,
                "status" to "Active",
                "createdAt" to nowTimestamp
            )
            
            customerRef?.let { saleData["customerRef"] = it }
            vehicleRef?.let { saleData["vehicleRef"] = it }
            
            if (purchaseType == "DOWN_PAYMENT" || purchaseType == "FULL_PAYMENT") {
                saleData["downPayment"] = downPayment
            }
            
            emiDetails?.let { emi ->
                val emiMap = hashMapOf<String, Any>(
                    "interestRate" to emi.interestRate,
                    "frequency" to emi.frequency,
                    "installmentsCount" to emi.installmentsCount,
                    "installmentAmount" to emi.installmentAmount,
                    "nextDueDate" to Timestamp(Date(emi.nextDueDate)),
                    "remainingInstallments" to emi.remainingInstallments,
                    "paidInstallments" to emi.paidInstallments
                )
                emi.lastPaidDate?.let { emiMap["lastPaidDate"] = Timestamp(Date(it)) }
                saleData["emiDetails"] = emiMap
            }
            
            // If down payment, add to capital (Bank or Cash)
            if ((purchaseType == "DOWN_PAYMENT" || purchaseType == "FULL_PAYMENT") && downPayment > 0) {
                // Add to Bank capital (you can modify this to allow selection)
                addToCapital("Bank", downPayment, saleDocRef)
            }
            
            saleDocRef.set(saleData).await()
            
            // Update the sold status of the vehicle
            vehicleRef?.let { ref ->
                VehicleRepository.getProductByReference(ref).fold(
                    onSuccess = { product ->
                        VehicleRepository.updateVehicle(
                            originalChassisNumber = product.chassisNumber,
                            updatedProduct = product.copy(sold = true)
                        )
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error updating product sold status: ${e.message}", e)
                    }
                )
            }
            
            Log.d(TAG, "✅ Sale created successfully with ID: $saleId")
            Result.success(saleId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding sale: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add amount to capital (Bank or Cash)
     */
    private suspend fun addToCapital(
        type: String,
        amount: Double,
        saleRef: DocumentReference
    ) {
        try {
            val capitalDocRef = capitalCollection.document(type)
            
            val now = Timestamp.now()
            val transactionData = hashMapOf<String, Any>(
                "transactionDate" to now,
                "createdAt" to now,
                "amount" to amount,
                "reference" to saleRef
            )
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(capitalDocRef)
                val existingTransactions = snapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                val currentBalance = (snapshot.get("balance") as? Long)?.toDouble() 
                    ?: (snapshot.get("balance") as? Double) ?: 0.0
                
                // Add amount to balance (sale increases capital)
                val newBalance = currentBalance + amount
                
                val updatedTransactions = existingTransactions.toMutableList()
                updatedTransactions.add(transactionData)
                
                transaction.set(
                    capitalDocRef,
                    mapOf(
                        "transactions" to updatedTransactions,
                        "balance" to newBalance
                    ),
                    SetOptions.merge()
                )
            }.await()
            
            Log.d(TAG, "✅ Added $amount to $type capital")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding to capital: ${e.message}", e)
        }
    }
    
    /**
     * Get sales by customer
     */
    suspend fun getSalesByCustomer(customerRef: DocumentReference): Result<List<VehicleSale>> {
        return try {
            val querySnapshot = saleCollection
                .whereEqualTo("customerRef", customerRef)
                .get()
                .await()
            
            val sales = querySnapshot.documents.mapNotNull { doc ->
                docToVehicleSale(doc)
            }
            
            Result.success(sales)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting sales by customer: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get sales with due payments today
     */
    suspend fun getSalesDueToday(): Result<List<VehicleSale>> {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val tomorrow = today + (24 * 60 * 60 * 1000)
            
            // Get all active sales with EMI
            val querySnapshot = saleCollection
                .whereEqualTo("status", "Active")
                .whereEqualTo("purchaseType", "EMI")
                .get()
                .await()
            
            val sales = querySnapshot.documents.mapNotNull { doc ->
                docToVehicleSale(doc)
            }.filter { sale ->
                sale.emiDetails?.let { emi ->
                    emi.nextDueDate >= today && emi.nextDueDate < tomorrow
                } ?: false
            }
            
            Result.success(sales)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting sales due today: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all active sales
     */
    suspend fun getAllActiveSales(): Result<List<VehicleSale>> {
        return try {
            val querySnapshot = saleCollection
                .whereEqualTo("status", "Active")
                .get()
                .await()
            
            val sales = querySnapshot.documents.mapNotNull { doc ->
                docToVehicleSale(doc)
            }
            
            Result.success(sales)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting active sales: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Record an EMI payment
     */
    suspend fun recordEmiPayment(
        saleId: String,
        cashAmount: Double,
        bankAmount: Double,
        paymentDate: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            val paymentAmount = cashAmount + bankAmount
            if (paymentAmount <= 0.0) {
                return Result.failure(Exception("Payment amount must be greater than zero"))
            }

            val saleDocRef = saleCollection.document(saleId)
            val saleDoc = saleDocRef.get().await()
            
            if (!saleDoc.exists()) {
                return Result.failure(Exception("Sale not found"))
            }
            
            val sale = docToVehicleSale(saleDoc) ?: return Result.failure(Exception("Invalid sale data"))
            val emiDetails = sale.emiDetails ?: return Result.failure(Exception("No EMI details found"))
            
            val newPaidInstallments = emiDetails.paidInstallments + 1
            val newRemainingInstallments = emiDetails.remainingInstallments - 1
            
            // Calculate next due date based on frequency
            val nextDueDate = calculateNextDueDate(
                emiDetails.nextDueDate,
                emiDetails.frequency
            )
            
            // Update status if all installments are paid
            val newStatus = if (newRemainingInstallments <= 0) "Completed" else "Active"
            
            saleDocRef.update(
                mapOf(
                    "emiDetails.paidInstallments" to newPaidInstallments,
                    "emiDetails.remainingInstallments" to newRemainingInstallments,
                    "emiDetails.lastPaidDate" to Timestamp(Date(paymentDate)),
                    "emiDetails.nextDueDate" to Timestamp(Date(nextDueDate)),
                    "status" to newStatus
                )
            ).await()
            
            // Add payment to capital
            if (cashAmount > 0.0) {
                addToCapital("Cash", cashAmount, saleDocRef)
            }
            if (bankAmount > 0.0) {
                addToCapital("Bank", bankAmount, saleDocRef)
            }
            
            Log.d(TAG, "✅ EMI payment recorded for sale: $saleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recording EMI payment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculate next due date based on frequency
     */
    private fun calculateNextDueDate(currentDueDate: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        
        when (frequency) {
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "QUARTERLY" -> calendar.add(Calendar.MONTH, 3)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getTimestampOrLong(field: String): Long {
        val timestamp = getTimestamp(field)
        if (timestamp != null) return timestamp.toDate().time
        val number = get(field) as? Number
        return number?.toLong() ?: System.currentTimeMillis()
    }

    private fun Map<String, Any>.getTimestampOrLong(key: String): Long {
        val value = this[key]
        return when (value) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            else -> System.currentTimeMillis()
        }
    }

    private fun Map<String, Any>.getTimestampOrLongOrNull(key: String): Long? {
        val value = this[key]
        return when (value) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            else -> null
        }
    }
}

