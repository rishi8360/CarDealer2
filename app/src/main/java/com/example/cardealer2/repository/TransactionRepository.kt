package com.example.cardealer2.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.PersonType
import com.example.cardealer2.data.TransactionStatus
import com.example.cardealer2.data.TransactionType
import com.example.cardealer2.data.MaxTransactionNo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

object TransactionRepository {
    @SuppressLint("StaticFieldLeak")
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestoreSettings = settings
    }
    
    private const val TAG = "TransactionRepository"
    private val transactionCollection = db.collection("Transactions")
    private val capitalCollection = db.collection("Capital")
    private val maxTransactionNoCollection = db.collection("MaxTransactionNo")
    private val productCollection = db.collection("Product")
    private val chassisCollection = db.collection("ChassisNumber")
    private val brandCollection = db.collection("Brand")
    private val purchaseCollection = db.collection("Purchase")
    
    // MaxTransactionNo document reference (initialized on first use)
    private var maxTransactionNoDocRef: DocumentReference? = null
    
    /**
     * Convert Firestore document to PersonTransaction
     */
    private fun docToPersonTransaction(doc: com.google.firebase.firestore.DocumentSnapshot): PersonTransaction? {
        return try {
            PersonTransaction(
                transactionId = doc.id,
                type = doc.get("type") as? String ?: "",
                personType = doc.get("personType") as? String ?: "",
                personRef = doc.get("personRef") as? DocumentReference,
                personName = doc.get("personName") as? String ?: "",
                relatedRef = doc.get("relatedRef") as? DocumentReference,
                amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
                paymentMethod = doc.get("paymentMethod") as? String ?: "",
                cashAmount = (doc.get("cashAmount") as? Number)?.toDouble() ?: 0.0,
                bankAmount = (doc.get("bankAmount") as? Number)?.toDouble() ?: 0.0,
                creditAmount = (doc.get("creditAmount") as? Number)?.toDouble() ?: 0.0,
                date = doc.getDateString("date"),
                orderNumber = (doc.get("orderNumber") as? Number)?.toInt(),
                transactionNumber = (doc.get("transactionNumber") as? Number)?.toInt(),
                description = doc.get("description") as? String ?: "",
                status = doc.get("status") as? String ?: "COMPLETED",
                createdAt = doc.getTimestampOrLong("createdAt"),
                note = doc.get("note") as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error converting document to PersonTransaction: ${e.message}", e)
            null
        }
    }
    
    /**
     * Helper function to safely extract Long from Firestore Timestamp or Long
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.getTimestampOrLong(field: String): Long {
        val timestamp = getTimestamp(field)
        if (timestamp != null) return timestamp.toDate().time
        val number = get(field) as? Number
        return number?.toLong() ?: System.currentTimeMillis()
    }
    
    /**
     * Helper function to convert timestamp to date string (yyyy-MM-dd)
     */
    private fun timestampToDateString(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Helper function to get date string from Firestore document
     * Handles both String (new format) and Timestamp/Long (old format for backward compatibility)
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.getDateString(field: String): String {
        // Try String first (new format)
        val dateString = get(field) as? String
        if (!dateString.isNullOrBlank()) return dateString
        
        // Fallback to Timestamp/Long (old format)
        val timestamp = getTimestamp(field)
        if (timestamp != null) return timestampToDateString(timestamp.toDate().time)
        
        val number = get(field) as? Number
        if (number != null) return timestampToDateString(number.toLong())
        
        // Default to today's date
        return timestampToDateString(System.currentTimeMillis())
    }
    
    /**
     * Get current date as string (yyyy-MM-dd)
     */
    private fun getCurrentDateString(): String {
        return timestampToDateString(System.currentTimeMillis())
    }
    
    /**
     * Initialize MaxTransactionNo document reference (called on first use)
     */
    private suspend fun initializeMaxTransactionNoDocRef() {
        if (maxTransactionNoDocRef != null) return
        
        try {
            // Check if any document exists
            val existingDocs = maxTransactionNoCollection.limit(1).get().await()
            
            if (existingDocs.isEmpty) {
                // Create initial document
                val docRef = maxTransactionNoCollection.document()
                docRef.set(mapOf("maxTransactionNo" to 0)).await()
                maxTransactionNoDocRef = docRef
                Log.d(TAG, "✅ MaxTransactionNo document created and initialized")
            } else {
                maxTransactionNoDocRef = existingDocs.documents.first().reference
                Log.d(TAG, "✅ MaxTransactionNo document reference initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing MaxTransactionNo document reference: ${e.message}", e)
        }
    }
    
    /**
     * Helper function to safely extract Int from Firestore document
     * Handles both Long and Int types from Firestore
     */
    private fun getIntFromFirestore(value: Any?): Int {
        return when (value) {
            is Long -> value.toInt()
            is Int -> value
            is Number -> value.toInt()
            else -> 0
        }
    }
    
    /**
     * Get the current MaxTransactionNo
     */
    suspend fun getMaxTransactionNo(): Result<Int> {
        return try {
            initializeMaxTransactionNoDocRef()
            val docRef = maxTransactionNoDocRef
                ?: return Result.failure(Exception("MaxTransactionNo document reference not initialized"))
            
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val maxTransactionNo = getIntFromFirestore(snapshot.get("maxTransactionNo"))
                Result.success(maxTransactionNo)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting MaxTransactionNo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Increment and get the next transaction number
     */
    suspend fun getNextTransactionNumber(): Result<Int> {
        return try {
            // Initialize document reference if needed
            initializeMaxTransactionNoDocRef()
            val docRef = maxTransactionNoDocRef
                ?: return Result.failure(Exception("MaxTransactionNo document reference not initialized"))
            
            // Use transaction to atomically increment
            val result = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentMax = getIntFromFirestore(snapshot.get("maxTransactionNo"))
                val newMax = currentMax + 1
                transaction.update(docRef, "maxTransactionNo", newMax)
                newMax
            }.await()
            
            Log.d(TAG, "✅ Next transaction number: $result")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting next transaction number: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a new transaction record
     * This should be called from within existing transactions (Purchase/Sale) to ensure atomicity
     */
    suspend fun createTransaction(transaction: PersonTransaction): Result<String> {
        return try {
            // Get next transaction number if not already provided
            val transactionNumber = transaction.transactionNumber ?: run {
                val numberResult = getNextTransactionNumber()
                if (numberResult.isFailure) {
                    return Result.failure(numberResult.exceptionOrNull() ?: Exception("Failed to get transaction number"))
                }
                numberResult.getOrNull() ?: return Result.failure(Exception("Failed to get transaction number"))
            }
            
            val transactionDocRef = transactionCollection.document()
            val transactionId = transactionDocRef.id
            
            val transactionData = hashMapOf<String, Any>(
                "transactionId" to transactionId,
                "type" to transaction.type,
                "personType" to transaction.personType,
                "personName" to transaction.personName,
                "amount" to transaction.amount,
                "paymentMethod" to transaction.paymentMethod,
                "cashAmount" to transaction.cashAmount,
                "bankAmount" to transaction.bankAmount,
                "creditAmount" to transaction.creditAmount,
                "date" to transaction.date,
                "transactionNumber" to transactionNumber,
                "description" to transaction.description,
                "status" to transaction.status,
                "createdAt" to Timestamp(Date(transaction.createdAt)),
                "note" to transaction.note
            )
            
            // Add references if they exist
            transaction.personRef?.let { transactionData["personRef"] = it }
            transaction.relatedRef?.let { transactionData["relatedRef"] = it }
            transaction.orderNumber?.let { transactionData["orderNumber"] = it }
            
            transactionDocRef.set(transactionData).await()
            
            Log.d(TAG, "✅ Transaction created successfully with ID: $transactionId, Transaction Number: $transactionNumber")
            Result.success(transactionId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating transaction: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create transaction record within a Firestore transaction
     * Use this when you need to create transaction as part of an atomic operation
     * Note: If transactionNumber is not provided, it will be obtained from MaxTransactionNo within the transaction
     * IMPORTANT: maxTransactionNoDocRef must be initialized before calling this method
     */
    fun createTransactionInTransaction(
        transaction: PersonTransaction,
        firestoreTransaction: Transaction
    ): String {
        val transactionDocRef = transactionCollection.document()
        val transactionId = transactionDocRef.id
        
        // Get transaction number - use provided one or get from MaxTransactionNo within the transaction
        val transactionNumber = transaction.transactionNumber ?: run {
            // Get MaxTransactionNo document reference - must be initialized
            val docRef = maxTransactionNoDocRef
                ?: throw IllegalStateException("MaxTransactionNo document reference not initialized")
            
            // Read and increment within the transaction
            val snapshot = firestoreTransaction.get(docRef)
            val currentMax = getIntFromFirestore(snapshot.get("maxTransactionNo"))
            val newMax = currentMax + 1
            firestoreTransaction.update(docRef, "maxTransactionNo", newMax)
            newMax
        }
        
        val transactionData = hashMapOf<String, Any>(
            "transactionId" to transactionId,
            "type" to transaction.type,
            "personType" to transaction.personType,
            "personName" to transaction.personName,
            "amount" to transaction.amount,
            "paymentMethod" to transaction.paymentMethod,
            "cashAmount" to transaction.cashAmount,
            "bankAmount" to transaction.bankAmount,
            "creditAmount" to transaction.creditAmount,
            "date" to transaction.date,
            "transactionNumber" to transactionNumber,
            "description" to transaction.description,
            "status" to transaction.status,
            "createdAt" to Timestamp(Date(transaction.createdAt)),
            "note" to transaction.note
        )
        
        // Add references if they exist
        transaction.personRef?.let { transactionData["personRef"] = it }
        transaction.relatedRef?.let { transactionData["relatedRef"] = it }
        transaction.orderNumber?.let { transactionData["orderNumber"] = it }
        
        firestoreTransaction.set(transactionDocRef, transactionData)
        
        Log.d(TAG, "✅ Transaction created in transaction with ID: $transactionId, Transaction Number: $transactionNumber")
        return transactionId
    }
    
    /**
     * Get all transactions for a specific person (Customer or Broker)
     * Note: We sort in memory instead of using orderBy to avoid requiring a composite index
     */
    suspend fun getTransactionsByPerson(personRef: DocumentReference): Result<List<PersonTransaction>> {
        return try {
            val querySnapshot = transactionCollection
                .whereEqualTo("personRef", personRef)
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { doc ->
                docToPersonTransaction(doc)
            }.sortedByDescending { it.date } // Sort by date descending in memory
            
            Result.success(transactions)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting transactions by person: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get transactions by type
     * Note: We sort in memory instead of using orderBy to avoid requiring a composite index
     */
    suspend fun getTransactionsByType(type: String): Result<List<PersonTransaction>> {
        return try {
            val querySnapshot = transactionCollection
                .whereEqualTo("type", type)
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { doc ->
                docToPersonTransaction(doc)
            }.sortedByDescending { it.date } // Sort by date descending in memory
            
            Result.success(transactions)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting transactions by type: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get transactions by date range
     */
    suspend fun getTransactionsByDateRange(
        startDate: Timestamp,
        endDate: Timestamp
    ): Result<List<PersonTransaction>> {
        return try {
            // Note: We can use orderBy here because we're ordering by the same field we're filtering on
            // But to avoid index issues, we'll sort in memory instead
            val querySnapshot = transactionCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { doc ->
                docToPersonTransaction(doc)
            }.sortedByDescending { it.date } // Sort by date descending in memory
            
            Result.success(transactions)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting transactions by date range: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get top N transactions sorted by createdAt (latest first)
     * This fetches once without using listeners
     */
    suspend fun getTopTransactions(limit: Int = 10): Result<List<PersonTransaction>> {
        return try {
            val querySnapshot = transactionCollection
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { doc ->
                try {
                    docToPersonTransaction(doc)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing transaction document ${doc.id}: ${e.message}", e)
                    null
                }
            }
                .sortedByDescending { it.createdAt } // Sort by createdAt descending (latest first)
                .take(limit) // Take only top N
            
            Log.d(TAG, "✅ Fetched ${transactions.size} transactions (top $limit)")
            Result.success(transactions)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting top transactions: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get top N transfer transactions sorted by createdAt (latest first)
     * This fetches once without using listeners
     */
    suspend fun getTopTransferTransactions(limit: Int = 10): Result<List<PersonTransaction>> {
        return try {
            val querySnapshot = transactionCollection
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { doc ->
                docToPersonTransaction(doc)
            }
                .filter { transaction ->
                    transaction.description.startsWith("Transfer from")
                }
                .sortedByDescending { it.createdAt } // Sort by createdAt descending (latest first)
                .take(limit) // Take only top N
            
            Log.d(TAG, "✅ Fetched ${transactions.size} transfer transactions (top $limit)")
            Result.success(transactions)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting top transfer transactions: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    
    /**
     * Transfer amount from customer to capital (Bank or Cash)
     * This function performs all operations atomically within a Firestore transaction
     */
    suspend fun transferFromCustomerToCapital(
        customerId: String,
        amount: Double,
        capitalType: String,
        description: String = "Transfer from customer",
        note: String = "",
        date: String = ""
    ): Result<Unit> {
        val transactionDate = if (date.isBlank()) getCurrentDateString() else date
        return try {
            // Validate capital type
            if (capitalType !in listOf("Bank", "Cash")) {
                return Result.failure(Exception("Invalid capital type. Must be Bank or Cash"))
            }
            
            // Validate amount
            if (amount <= 0) {
                return Result.failure(Exception("Amount must be greater than 0"))
            }
            
            // Get customer reference
            val customerRefResult = CustomerRepository.getCustomerReference(customerId)
            val customerRef = customerRefResult.getOrNull() 
                ?: return Result.failure(Exception("Customer not found"))
            
            // Get customer details for transaction record
            val customerResult = CustomerRepository.getCustomerById(customerId)
            val customer = customerResult.getOrNull() 
                ?: return Result.failure(Exception("Customer not found"))
            
            // Initialize MaxTransactionNo document reference before transaction
            initializeMaxTransactionNoDocRef()
            val maxTransactionNoDocRefLocal = maxTransactionNoDocRef
                ?: return Result.failure(Exception("MaxTransactionNo document reference not initialized"))
            
            // Run everything in a single Firestore transaction
            db.runTransaction { transaction ->
                // ========== PHASE 1: ALL READS FIRST ==========
                // Firestore requires all reads to complete before any writes
                
                // Read MaxTransactionNo document (required for createTransactionInTransaction)
                val maxTransactionNoSnapshot = transaction.get(maxTransactionNoDocRefLocal) // Read it in the transaction
                val currentTransactionNo = getIntFromFirestore(maxTransactionNoSnapshot.get("maxTransactionNo"))
                val newTransactionNo = currentTransactionNo + 1
                
                // Read customer document
                val customerSnapshot = transaction.get(customerRef)
                val currentCustomerAmount = (customerSnapshot.get("amount") as? Number)?.toInt() ?: 0
                val newCustomerAmount = currentCustomerAmount - amount.toInt()
                
                // Read capital document
                val capitalDocRef = capitalCollection.document(capitalType)
                val capitalSnapshot = transaction.get(capitalDocRef)
                
                val existingTransactions = capitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                val currentBalance = (capitalSnapshot.get("balance") as? Long)?.toDouble() 
                    ?: (capitalSnapshot.get("balance") as? Double) ?: 0.0
                
                // ========== PHASE 2: ALL WRITES AFTER READS ==========
                
                // Write 1: Update customer amount
                transaction.update(customerRef, "amount", newCustomerAmount)
                
                // Write 2: Update capital balance
                val newBalance = currentBalance + amount
                val transactionData = hashMapOf<String, Any>(
                    "transactionDate" to Timestamp.now(),
                    "createdAt" to Timestamp.now(),
                    "orderNumber" to 0,
                    "amount" to amount,
                    "description" to description,
                    "type" to "CUSTOMER_TRANSFER"
                )
                
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
                
                // Write 3: Update MaxTransactionNo
                transaction.update(maxTransactionNoDocRefLocal, "maxTransactionNo", newTransactionNo)
                
                // Write 4: Create PersonTransaction record
                val paymentMethod = if (capitalType == "Bank") "BANK" else "CASH"
                val cashAmount = if (capitalType == "Cash") amount else 0.0
                val bankAmount = if (capitalType == "Bank") amount else 0.0
                
                val personTransaction = PersonTransaction(
                    type = TransactionType.SALE, // Treating as money received from customer
                    personType = PersonType.CUSTOMER,
                    personRef = customerRef,
                    personName = customer.name,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    cashAmount = cashAmount,
                    bankAmount = bankAmount,
                    creditAmount = 0.0,
                    date = transactionDate,
                    transactionNumber = newTransactionNo, // Pass the transaction number
                    description = description,
                    status = TransactionStatus.COMPLETED,
                    createdAt = System.currentTimeMillis(),
                    note = note
                )
                
                createTransactionInTransaction(personTransaction, transaction)
            }.await()
            
            Log.d(TAG, "✅ Transfer from customer $customerId to $capitalType completed: $amount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error transferring from customer to capital: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Transfer between any sources (Customer, Broker, Cash, Bank)
     * Takes full Customer/Broker objects to avoid re-reading from Firestore
     * No balance validation - allows negative balances
     */
    suspend fun transferBetweenSources(
        fromType: String,  // "Customer", "Broker", "Cash", "Bank"
        fromCustomer: com.example.cardealer2.data.Customer?,  // Full Customer object if fromType == "Customer"
        fromBroker: com.example.cardealer2.data.Broker?,      // Full Broker object if fromType == "Broker"
        toType: String,    // "Customer", "Broker", "Cash", "Bank"
        toCustomer: com.example.cardealer2.data.Customer?,    // Full Customer object if toType == "Customer"
        toBroker: com.example.cardealer2.data.Broker?,        // Full Broker object if toType == "Broker"
        amount: Double,
        description: String = "Transfer",
        note: String = "",
        date: String = ""
    ): Result<Unit> {
        val transactionDate = if (date.isBlank()) getCurrentDateString() else date
        
        return try {
            // Validation
            if (amount <= 0) {
                return Result.failure(Exception("Amount must be greater than 0"))
            }
            
            // Validate FROM and TO types
            val validTypes = listOf("Customer", "Broker", "Cash", "Bank")
            if (fromType !in validTypes || toType !in validTypes) {
                return Result.failure(Exception("Invalid transfer type"))
            }
            
            if (fromType == toType) {
                return Result.failure(Exception("Cannot transfer to same source"))
            }
            
            // Validate required objects
            if (fromType == "Customer" && fromCustomer == null) {
                return Result.failure(Exception("Customer object required for FROM"))
            }
            if (fromType == "Broker" && fromBroker == null) {
                return Result.failure(Exception("Broker object required for FROM"))
            }
            if (toType == "Customer" && toCustomer == null) {
                return Result.failure(Exception("Customer object required for TO"))
            }
            if (toType == "Broker" && toBroker == null) {
                return Result.failure(Exception("Broker object required for TO"))
                }
            
            // Get document references
            var fromRef: DocumentReference? = null
            var toRef: DocumentReference? = null
            var fromName = ""
            var toName = ""
            
            // Handle FROM source
            when (fromType) {
                "Customer" -> {
                    fromRef = db.collection("Customer").document(fromCustomer!!.customerId)
                    fromName = fromCustomer.name
                }
                "Broker" -> {
                    fromRef = db.collection("Broker").document(fromBroker!!.brokerId)
                    fromName = fromBroker.name
                }
                "Cash", "Bank" -> {
                    fromRef = null // Capital doesn't need reference
                    fromName = fromType
                }
            }
            
            // Handle TO destination
            when (toType) {
                "Customer" -> {
                    toRef = db.collection("Customer").document(toCustomer!!.customerId)
                    toName = toCustomer.name
                }
                "Broker" -> {
                    toRef = db.collection("Broker").document(toBroker!!.brokerId)
                    toName = toBroker.name
                }
                "Cash", "Bank" -> {
                    toRef = null // Capital doesn't need reference
                    toName = toType
                }
            }
            
            // Initialize MaxTransactionNo
            initializeMaxTransactionNoDocRef()
            val maxTransactionNoDocRefLocal = maxTransactionNoDocRef
                ?: return Result.failure(Exception("MaxTransactionNo document reference not initialized"))
            
            // Run everything in a single Firestore transaction
            db.runTransaction { transaction ->
                // ========== PHASE 1: ALL READS FIRST ==========
                
                // Read MaxTransactionNo
                val maxTransactionNoSnapshot = transaction.get(maxTransactionNoDocRefLocal)
                val currentTransactionNo = getIntFromFirestore(maxTransactionNoSnapshot.get("maxTransactionNo"))
                val newTransactionNo = currentTransactionNo + 1
                
                // Read FROM source balance and transactions
                var fromCurrentBalance = 0.0
                var fromExistingTransactions: List<Map<String, Any>> = emptyList()
                when (fromType) {
                    "Customer" -> {
                        val fromSnapshot = transaction.get(fromRef!!)
                        fromCurrentBalance = (fromSnapshot.get("amount") as? Number)?.toDouble() ?: 0.0
                    }
                    "Broker" -> {
                        val fromSnapshot = transaction.get(fromRef!!)
                        fromCurrentBalance = (fromSnapshot.get("amount") as? Number)?.toDouble() ?: 0.0
                            }
                    "Cash", "Bank" -> {
                        val capitalDocRef = capitalCollection.document(fromType)
                        val capitalSnapshot = transaction.get(capitalDocRef)
                        fromCurrentBalance = (capitalSnapshot.get("balance") as? Long)?.toDouble() 
                            ?: (capitalSnapshot.get("balance") as? Double) ?: 0.0
                        fromExistingTransactions = capitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                    }
                }
                
                // Read TO destination balance and transactions
                var toCurrentBalance = 0.0
                var toExistingTransactions: List<Map<String, Any>> = emptyList()
                when (toType) {
                    "Customer" -> {
                        val toSnapshot = transaction.get(toRef!!)
                        toCurrentBalance = (toSnapshot.get("amount") as? Number)?.toDouble() ?: 0.0
                    }
                    "Broker" -> {
                        val toSnapshot = transaction.get(toRef!!)
                        toCurrentBalance = (toSnapshot.get("amount") as? Number)?.toDouble() ?: 0.0
                    }
                    "Cash", "Bank" -> {
                        val capitalDocRef = capitalCollection.document(toType)
                        val capitalSnapshot = transaction.get(capitalDocRef)
                        toCurrentBalance = (capitalSnapshot.get("balance") as? Long)?.toDouble() 
                            ?: (capitalSnapshot.get("balance") as? Double) ?: 0.0
                        toExistingTransactions = capitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                    }
                }
                
                // ========== PHASE 2: ALL WRITES AFTER READS ==========
                
                // Write 1: Update FROM source balance (decrease)
                when (fromType) {
                    "Customer" -> {
                        val newAmount = fromCurrentBalance - amount
                        transaction.update(fromRef!!, "amount", newAmount.toInt())
                    }
                    "Broker" -> {
                        val newAmount = fromCurrentBalance - amount
                        transaction.update(fromRef!!, "amount", newAmount.toInt())
                    }
                    "Cash", "Bank" -> {
                        val capitalDocRef = capitalCollection.document(fromType)
                        val newBalance = fromCurrentBalance - amount
                        
                        val transactionData = hashMapOf<String, Any>(
                            "transactionDate" to Timestamp.now(),
                            "createdAt" to Timestamp.now(),
                            "orderNumber" to 0,
                            "amount" to -amount, // Negative for outflow
                            "description" to description,
                            "type" to "TRANSFER_OUT",
                            "to" to toName
                        )
                        
                        val updatedTransactions = fromExistingTransactions.toMutableList()
                        updatedTransactions.add(transactionData)
                        
                        transaction.set(
                            capitalDocRef,
                            mapOf(
                                "transactions" to updatedTransactions,
                                "balance" to newBalance
                            ),
                            SetOptions.merge()
                        )
                    }
                }
                
                // Write 2: Update TO destination balance (increase)
                when (toType) {
                    "Customer" -> {
                        val newAmount = toCurrentBalance + amount
                        transaction.update(toRef!!, "amount", newAmount.toInt())
                    }
                    "Broker" -> {
                        val newAmount = toCurrentBalance + amount
                        transaction.update(toRef!!, "amount", newAmount.toInt())
                    }
                    "Cash", "Bank" -> {
                        val capitalDocRef = capitalCollection.document(toType)
                        val newBalance = toCurrentBalance + amount
                        
                        val transactionData = hashMapOf<String, Any>(
                            "transactionDate" to Timestamp.now(),
                            "createdAt" to Timestamp.now(),
                            "orderNumber" to 0,
                            "amount" to amount,
                            "description" to description,
                            "type" to "TRANSFER_IN",
                            "from" to fromName
                        )
                        
                        val updatedTransactions = toExistingTransactions.toMutableList()
                        updatedTransactions.add(transactionData)
                        
                        transaction.set(
                            capitalDocRef,
                            mapOf(
                                "transactions" to updatedTransactions,
                                "balance" to newBalance
                            ),
                            SetOptions.merge()
                        )
                    }
                }
                
                // Write 3: Update MaxTransactionNo
                transaction.update(maxTransactionNoDocRefLocal, "maxTransactionNo", newTransactionNo)
                
                // Write 4: Create PersonTransaction record
                val paymentMethod = when {
                    (fromType == "Cash" || toType == "Cash") && (fromType == "Bank" || toType == "Bank") -> "MIXED"
                    fromType == "Bank" || toType == "Bank" -> "BANK"
                    fromType == "Cash" || toType == "Cash" -> "CASH"
                    else -> "CASH" // Default
                }
                
                val cashAmount = if (fromType == "Cash" || toType == "Cash") amount else 0.0
                val bankAmount = if (fromType == "Bank" || toType == "Bank") amount else 0.0
                
                // Determine transaction type and person type
                val transactionType = when {
                    fromType == "Customer" || fromType == "Broker" -> TransactionType.SALE // Money received
                    else -> TransactionType.PURCHASE // Money paid
                }
                
                val personType = when (fromType) {
                    "Customer" -> PersonType.CUSTOMER
                    "Broker" -> PersonType.BROKER
                    else -> when (toType) {
                        "Customer" -> PersonType.CUSTOMER
                        "Broker" -> PersonType.BROKER
                        else -> PersonType.CUSTOMER // Default
                    }
                }
                
                val personRef = when {
                    fromType == "Customer" -> fromRef
                    fromType == "Broker" -> fromRef
                    toType == "Customer" -> toRef
                    toType == "Broker" -> toRef
                    else -> null
                }
                
                val personName = when {
                    fromType == "Customer" || fromType == "Broker" -> fromName
                    toType == "Customer" || toType == "Broker" -> toName
                    else -> "$fromName to $toName"
                }
                
                val personTransaction = PersonTransaction(
                    type = transactionType,
                    personType = personType,
                    personRef = personRef,
                    personName = personName,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    cashAmount = cashAmount,
                    bankAmount = bankAmount,
                    creditAmount = 0.0,
                    date = transactionDate,
                    transactionNumber = newTransactionNo,
                    description = description,
                    status = TransactionStatus.COMPLETED,
                    createdAt = System.currentTimeMillis(),
                    note = note
                )
                
                createTransactionInTransaction(personTransaction, transaction)
            }.await()
            
            Log.d(TAG, "✅ Transfer from $fromType ($fromName) to $toType ($toName) completed: $amount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error transferring: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a single transaction by ID
     */
    suspend fun getTransactionById(transactionId: String): Result<PersonTransaction?> {
        return try {
            val doc = transactionCollection.document(transactionId).get().await()
            if (doc.exists()) {
                val transaction = docToPersonTransaction(doc)
                Result.success(transaction)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting transaction by ID: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a transaction and revert all related changes
     * This function handles cleanup for all transaction types:
     * - PURCHASE: Reverts person balance, capital balances, removes from Capital transactions array,
     *   deletes Product, Chassis, updates Brand inventory, deletes Purchase document
     * - SALE: Reverts person balance, capital balances, removes from Capital transactions array,
     *   sets product.sold = false, updates Brand inventory
     * - EMI_PAYMENT: Reverts EmiDetails, VehicleSale status, person balance, capital balances
     * - BROKER_FEE: Reverts broker balance, capital balances, removes from Capital transactions array
     * - TRANSFER: Reverts both FROM and TO balances, removes from Capital transactions arrays
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🗑️  STARTING TRANSACTION DELETION")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📋 Transaction ID: $transactionId")
        
        return try {
            // 1. Read the transaction document first
            Log.d(TAG, "📖 Step 1: Reading transaction document...")
            val transactionDoc = transactionCollection.document(transactionId).get().await()
            if (!transactionDoc.exists()) {
                Log.e(TAG, "❌ Transaction document does not exist: $transactionId")
                return Result.failure(Exception("Transaction not found"))
            }
            Log.d(TAG, "✅ Transaction document found and read successfully")
            
            val transaction = docToPersonTransaction(transactionDoc)
                ?: run {
                    Log.e(TAG, "❌ Failed to parse transaction document")
                    return Result.failure(Exception("Failed to parse transaction"))
                }
            
            Log.d(TAG, "📊 Transaction Details:")
            Log.d(TAG, "   • Type: ${transaction.type}")
            Log.d(TAG, "   • Person: ${transaction.personName} (${transaction.personType})")
            Log.d(TAG, "   • Amount: ${transaction.amount}")
            Log.d(TAG, "   • Payment Method: ${transaction.paymentMethod}")
            Log.d(TAG, "   • Cash Amount: ${transaction.cashAmount}")
            Log.d(TAG, "   • Bank Amount: ${transaction.bankAmount}")
            Log.d(TAG, "   • Credit Amount: ${transaction.creditAmount}")
            Log.d(TAG, "   • Date: ${transaction.date}")
            Log.d(TAG, "   • Order Number: ${transaction.orderNumber}")
            Log.d(TAG, "   • Transaction Number: ${transaction.transactionNumber}")
            Log.d(TAG, "   • Description: ${transaction.description}")
            Log.d(TAG, "   • Related Ref: ${transaction.relatedRef?.path ?: "None"}")
            
            // Store product info for brand update (needed after transaction)
            var productBrandId: String? = null
            var productId: String? = null
            var productType: String? = null
            var productDocId: String? = null
            
            // 2. Run everything in a Firestore transaction for atomicity
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔄 Step 2: Starting Firestore Transaction")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📚 PHASE 1: Reading all required documents...")
            
            db.runTransaction { firestoreTransaction ->
                // ========== PHASE 1: ALL READS FIRST ==========
                
                // Read person document (Customer/Broker)
                Log.d(TAG, "   👤 Reading person document...")
                val personSnapshot = transaction.personRef?.let { ref ->
                    Log.d(TAG, "      Person Ref Path: ${ref.path}")
                    firestoreTransaction.get(ref)
                }
                personSnapshot?.let {
                    Log.d(TAG, "      ✅ Person document found: ${it.id}")
                    if (it.exists()) {
                        val currentAmount = (it.get("amount") as? Number)?.toDouble() ?: 0.0
                        Log.d(TAG, "      Current person balance: $currentAmount")
                    }
                } ?: Log.d(TAG, "      ⚠️  No person reference found")
                
                // Read capital documents based on payment method
                Log.d(TAG, "   💰 Reading capital documents...")
                val capitalRefs = mutableListOf<DocumentReference>()
                val capitalSnapshots = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                
                if (transaction.cashAmount > 0) {
                    val cashRef = capitalCollection.document("Cash")
                    capitalRefs.add(cashRef)
                    capitalSnapshots.add(firestoreTransaction.get(cashRef))
                    Log.d(TAG, "      ✅ Reading Cash capital document (Amount: ${transaction.cashAmount})")
                }
                if (transaction.bankAmount > 0) {
                    val bankRef = capitalCollection.document("Bank")
                    capitalRefs.add(bankRef)
                    capitalSnapshots.add(firestoreTransaction.get(bankRef))
                    Log.d(TAG, "      ✅ Reading Bank capital document (Amount: ${transaction.bankAmount})")
                }
                if (transaction.creditAmount > 0) {
                    val creditRef = capitalCollection.document("Credit")
                    capitalRefs.add(creditRef)
                    capitalSnapshots.add(firestoreTransaction.get(creditRef))
                    Log.d(TAG, "      ✅ Reading Credit capital document (Amount: ${transaction.creditAmount})")
                }
                
                // For EMI_PAYMENT transactions, read EmiDetails and VehicleSale
                var emiDetailsSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
                var saleSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
                
                if (transaction.type == TransactionType.EMI_PAYMENT && transaction.relatedRef != null) {
                    Log.d(TAG, "   💳 Reading EMI_PAYMENT related documents...")
                    Log.d(TAG, "      Related Ref Path: ${transaction.relatedRef!!.path}")
                    saleSnapshot = firestoreTransaction.get(transaction.relatedRef!!)
                    if (saleSnapshot.exists()) {
                        Log.d(TAG, "      ✅ VehicleSale document found: ${saleSnapshot.id}")
                        val emiDetailsRef = saleSnapshot.get("emiDetailsRef") as? DocumentReference
                        emiDetailsRef?.let {
                            Log.d(TAG, "      Reading EmiDetails document: ${it.path}")
                            emiDetailsSnapshot = firestoreTransaction.get(it)
                            if (emiDetailsSnapshot?.exists() == true) {
                                Log.d(TAG, "      ✅ EmiDetails document found: ${emiDetailsSnapshot!!.id}")
                            } else {
                                Log.w(TAG, "      ⚠️  EmiDetails document not found")
                            }
                        } ?: Log.w(TAG, "      ⚠️  No EmiDetails reference in VehicleSale")
                    } else {
                        Log.w(TAG, "      ⚠️  VehicleSale document not found")
                    }
                }
                
                // For SALE transactions, read VehicleSale, Product, and EmiDetails (if EMI sale)
                var productSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
                var saleEmiDetailsSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
                
                if (transaction.type == TransactionType.SALE && transaction.relatedRef != null) {
                    Log.d(TAG, "   🛒 Reading SALE related documents...")
                    Log.d(TAG, "      Related Ref Path: ${transaction.relatedRef!!.path}")
                    saleSnapshot = firestoreTransaction.get(transaction.relatedRef!!)
                    if (saleSnapshot.exists()) {
                        Log.d(TAG, "      ✅ VehicleSale document found: ${saleSnapshot.id}")
                        
                        // Read EmiDetails document if this is an EMI sale
                        val emiDetailsRef = saleSnapshot.get("emiDetailsRef") as? DocumentReference
                        emiDetailsRef?.let {
                            Log.d(TAG, "      Reading EmiDetails document: ${it.path}")
                            saleEmiDetailsSnapshot = firestoreTransaction.get(it)
                            if (saleEmiDetailsSnapshot?.exists() == true) {
                                Log.d(TAG, "      ✅ EmiDetails document found: ${saleEmiDetailsSnapshot!!.id}")
                            } else {
                                Log.w(TAG, "      ⚠️  EmiDetails document not found")
                            }
                        } ?: Log.d(TAG, "      ℹ️  No EmiDetails reference (not an EMI sale)")
                        
                        val vehicleRef = saleSnapshot.get("vehicleRef") as? DocumentReference
                        vehicleRef?.let {
                            Log.d(TAG, "      Reading Product document: ${it.path}")
                            productSnapshot = firestoreTransaction.get(it)
                            if (productSnapshot?.exists() == true) {
                                productBrandId = productSnapshot!!.get("brandId") as? String
                                productId = productSnapshot!!.get("productId") as? String
                                productType = productSnapshot!!.get("type") as? String
                                productDocId = productSnapshot!!.reference.id
                                Log.d(TAG, "      ✅ Product document found: ${productSnapshot!!.id}")
                                Log.d(TAG, "         Brand ID: $productBrandId")
                                Log.d(TAG, "         Product ID: $productId")
                                Log.d(TAG, "         Product Type: $productType")
                            } else {
                                Log.w(TAG, "      ⚠️  Product document not found")
                            }
                        } ?: Log.w(TAG, "      ⚠️  No vehicleRef in VehicleSale")
                    } else {
                        Log.w(TAG, "      ⚠️  VehicleSale document not found")
                    }
                }
                
                // For PURCHASE transactions, read Purchase document
                var purchaseSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
                if ((transaction.type == TransactionType.PURCHASE || transaction.type == TransactionType.BROKER_FEE) 
                    && transaction.relatedRef != null) {
                    Log.d(TAG, "   📦 Reading PURCHASE/BROKER_FEE related documents...")
                    Log.d(TAG, "      Related Ref Path: ${transaction.relatedRef!!.path}")
                    purchaseSnapshot = firestoreTransaction.get(transaction.relatedRef!!)
                    if (purchaseSnapshot.exists()) {
                        Log.d(TAG, "      ✅ Purchase document found: ${purchaseSnapshot.id}")
                    } else {
                        Log.w(TAG, "      ⚠️  Purchase document not found")
                    }
                }
                
                Log.d(TAG, "✅ PHASE 1 Complete: All documents read")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✍️  PHASE 2: Writing updates and deletions...")
                // ========== PHASE 2: ALL WRITES AFTER READS ==========
                
                // 1. Reverse person balance
                Log.d(TAG, "   🔄 Step 2.1: Reversing person balance...")
                personSnapshot?.let { snapshot ->
                    if (snapshot.exists()) {
                        val currentAmount = (snapshot.get("amount") as? Number)?.toDouble() ?: 0.0
                        Log.d(TAG, "      Current person balance: $currentAmount")
                        // Reverse the transaction effect based on type
                        val reversedAmount = when (transaction.type) {
                            TransactionType.SALE -> {
                                // Check if this is a down payment transaction for an EMI sale
                                // Down payment transactions for EMI sales have description "Vehicle sale - EMI down payment (Cash)" or "(Bank)"
                                // These transactions should NOT reverse customer balance because the down payment was already handled separately
                                val descriptionLower = transaction.description.lowercase()
                                val isEmiDownPayment = descriptionLower.contains("emi down payment")
                                
                                Log.d(TAG, "      Checking if down payment for EMI:")
                                Log.d(TAG, "         Description: ${transaction.description}")
                                Log.d(TAG, "         Lowercase description: $descriptionLower")
                                Log.d(TAG, "         Contains 'emi down payment': $isEmiDownPayment")
                                
                                if (isEmiDownPayment) {
                                    // Don't reverse customer balance for down payment transactions
                                    // The down payment was already handled in separate transactions
                                    // Deleting this transaction is just removing the wrong transaction record
                                    Log.d(TAG, "      Transaction Type: SALE - Down payment for EMI (SKIPPING balance reversal)")
                                    Log.d(TAG, "      ℹ️  Down payment balance already handled separately, not reversing customer balance")
                                    currentAmount // Keep current balance unchanged
                                } else {
                                    val newAmount = currentAmount - transaction.amount
                                    Log.d(TAG, "      Transaction Type: SALE (decreased balance by ${transaction.amount})")
                                    Log.d(TAG, "      Calculation: $currentAmount - ${transaction.amount} = $newAmount")
                                    newAmount
                                }
                            }
                            TransactionType.PURCHASE -> {
                                val newAmount = currentAmount + transaction.amount
                                Log.d(TAG, "      Transaction Type: PURCHASE (increased balance by ${transaction.amount})")
                                Log.d(TAG, "      Calculation: $currentAmount + ${transaction.amount} = $newAmount")
                                newAmount
                            }
                            TransactionType.EMI_PAYMENT -> {
                                val newAmount = currentAmount - transaction.amount
                                Log.d(TAG, "      Transaction Type: EMI_PAYMENT (increased debt by ${transaction.amount})")
                                Log.d(TAG, "      Calculation: $currentAmount - ${transaction.amount} = $newAmount")
                                newAmount
                            }
                            TransactionType.BROKER_FEE -> {
                                val newAmount = currentAmount + transaction.amount
                                Log.d(TAG, "      Transaction Type: BROKER_FEE (increased balance by ${transaction.amount})")
                                Log.d(TAG, "      Calculation: $currentAmount + ${transaction.amount} = $newAmount")
                                newAmount
                            }
                            else -> {
                                Log.d(TAG, "      Transaction Type: ${transaction.type} (no balance change)")
                                currentAmount
                            }
                        }
                        Log.d(TAG, "      ✅ Updating person balance to: ${reversedAmount.toInt()}")
                        firestoreTransaction.update(snapshot.reference, "amount", reversedAmount.toInt())
                    } else {
                        Log.w(TAG, "      ⚠️  Person document exists but is empty")
                    }
                } ?: Log.d(TAG, "      ⚠️  No person snapshot to update")
                
                // 2. Reverse EmiDetails if this is an EMI payment
                if (transaction.type == TransactionType.EMI_PAYMENT && emiDetailsSnapshot != null && emiDetailsSnapshot!!.exists()) {
                    Log.d(TAG, "   🔄 Step 2.2: Reversing EMI Details...")
                    val currentPaidInstallments = (emiDetailsSnapshot!!.get("paidInstallments") as? Number)?.toInt() ?: 0
                    val currentRemainingInstallments = (emiDetailsSnapshot!!.get("remainingInstallments") as? Number)?.toInt() ?: 0
                    val currentPendingExtraBalance = (emiDetailsSnapshot!!.get("pendingExtraBalance") as? Number)?.toDouble() ?: 0.0
                    val installmentAmount = (emiDetailsSnapshot!!.get("installmentAmount") as? Number)?.toDouble() ?: 0.0
                    
                    Log.d(TAG, "      Current EMI State:")
                    Log.d(TAG, "         Paid Installments: $currentPaidInstallments")
                    Log.d(TAG, "         Remaining Installments: $currentRemainingInstallments")
                    Log.d(TAG, "         Pending Extra Balance: $currentPendingExtraBalance")
                    Log.d(TAG, "         Installment Amount: $installmentAmount")
                    Log.d(TAG, "         Transaction Amount: ${transaction.amount}")
                    
                    // Since we now always count payments as installments (regardless of amount),
                    // we should always reverse the installment count when deleting
                    val description = transaction.description
                    Log.d(TAG, "      Description: $description")
                    
                    val emiUpdateMap = hashMapOf<String, Any>()
                    
                    // Always reverse installment count (since all payments now count as installments)
                    if (currentPaidInstallments > 0) {
                        // Reverse: decrement paidInstallments, increment remainingInstallments
                        val newPaidInstallments = currentPaidInstallments - 1
                        val newRemainingInstallments = currentRemainingInstallments + 1
                        emiUpdateMap["paidInstallments"] = newPaidInstallments
                        emiUpdateMap["remainingInstallments"] = newRemainingInstallments
                        
                        Log.d(TAG, "      ✅ Reversing installment count:")
                        Log.d(TAG, "         Paid Installments: $currentPaidInstallments -> $newPaidInstallments")
                        Log.d(TAG, "         Remaining Installments: $currentRemainingInstallments -> $newRemainingInstallments")
                    }
                    
                    // Reverse pendingExtraBalance based on payment amount
                    val totalPayment = transaction.amount
                    val totalAvailable = currentPendingExtraBalance + totalPayment
                    
                    if (totalAvailable < installmentAmount) {
                        // Payment was less than installment amount, reverse by subtracting total payment
                        val newPendingExtraBalance = maxOf(0.0, currentPendingExtraBalance - totalPayment)
                        emiUpdateMap["pendingExtraBalance"] = newPendingExtraBalance
                        Log.d(TAG, "      ✅ Reversing payment (less than installment):")
                        Log.d(TAG, "         Pending Extra Balance: $currentPendingExtraBalance -> $newPendingExtraBalance")
                    } else {
                        // Payment was equal to or greater than installment amount
                        val excessAmount = totalAvailable - installmentAmount
                        val newPendingExtraBalance = maxOf(0.0, currentPendingExtraBalance - excessAmount)
                        emiUpdateMap["pendingExtraBalance"] = newPendingExtraBalance
                        Log.d(TAG, "      ✅ Reversing payment (equal or greater than installment):")
                        Log.d(TAG, "         Excess Amount: $excessAmount")
                        Log.d(TAG, "         Pending Extra Balance: $currentPendingExtraBalance -> $newPendingExtraBalance")
                    }
                    
                    Log.d(TAG, "      ✅ Updating EmiDetails document with: $emiUpdateMap")
                    firestoreTransaction.update(emiDetailsSnapshot!!.reference, emiUpdateMap)
                    
                    // If sale was marked as completed, revert to pending
                    if (saleSnapshot != null && saleSnapshot!!.exists()) {
                        val currentStatus = saleSnapshot!!.get("status") as? Boolean ?: false
                        Log.d(TAG, "      Checking VehicleSale status: $currentStatus")
                        if (currentStatus && currentRemainingInstallments == 0) {
                            // Sale was completed, but now we're reversing an installment
                            Log.d(TAG, "      ✅ Reverting VehicleSale status from completed to pending")
                            firestoreTransaction.update(saleSnapshot!!.reference, "status", false)
                        } else {
                            Log.d(TAG, "      ⚠️  VehicleSale status unchanged (status: $currentStatus, remaining: $currentRemainingInstallments)")
                        }
                    }
                } else {
                    if (transaction.type == TransactionType.EMI_PAYMENT) {
                        Log.d(TAG, "   ⚠️  Step 2.2: Skipping EMI Details reversal (document not found or null)")
                    }
                }
                
                // 3. Reverse SALE transaction effects (product.sold = false, brand inventory)
                if (transaction.type == TransactionType.SALE && productSnapshot != null && productSnapshot!!.exists()) {
                    Log.d(TAG, "   🔄 Step 2.3: Reversing SALE transaction effects...")
                    val currentSoldStatus = productSnapshot!!.get("sold") as? Boolean ?: false
                    Log.d(TAG, "      Current product.sold status: $currentSoldStatus")
                    // Set product.sold = false
                    firestoreTransaction.update(productSnapshot!!.reference, "sold", false)
                    Log.d(TAG, "      ✅ Updated product.sold to: false")
                    Log.d(TAG, "      📝 Brand inventory update will be done after transaction (outside Firestore transaction)")
                } else {
                    if (transaction.type == TransactionType.SALE) {
                        Log.d(TAG, "   ⚠️  Step 2.3: Skipping SALE reversal (product snapshot not found)")
                    }
                }
                
                // 4. Reverse capital balances and remove transaction entries
                Log.d(TAG, "   🔄 Step 2.4: Reversing capital balances...")
                
                // For transfers, determine which account is FROM and which is TO
                val isTransfer = transaction.description.contains("Transfer", ignoreCase = true) 
                    || (transaction.cashAmount > 0 && transaction.bankAmount > 0)
                
                // Determine transfer direction from description if it's a transfer
                var transferFromType: String? = null
                var transferToType: String? = null
                if (isTransfer) {
                    // Try to extract FROM and TO from description
                    // Format: "Transfer from X to Y" or "Transfer from Customer X to Cash"
                    val desc = transaction.description
                    if (desc.contains("from", ignoreCase = true) && desc.contains("to", ignoreCase = true)) {
                        val fromMatch = Regex("from\\s+([^\\s]+)", RegexOption.IGNORE_CASE).find(desc)
                        val toMatch = Regex("to\\s+([^\\s]+)", RegexOption.IGNORE_CASE).find(desc)
                        fromMatch?.let { transferFromType = it.groupValues[1] }
                        toMatch?.let { transferToType = it.groupValues[1] }
                    }
                }
                
                capitalSnapshots.forEachIndexed { index, snapshot ->
                    val capitalRef = capitalRefs[index]
                    val capitalType = capitalRef.id // "Cash", "Bank", or "Credit"
                    val amountToReverse = when (capitalType) {
                        "Cash" -> transaction.cashAmount
                        "Bank" -> transaction.bankAmount
                        "Credit" -> transaction.creditAmount
                        else -> 0.0
                    }
                    
                    Log.d(TAG, "      Processing $capitalType capital:")
                    Log.d(TAG, "         Amount to reverse: $amountToReverse")
                    
                    if (amountToReverse > 0) {
                        val existingTransactions = snapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                        val currentBalance = (snapshot.get("balance") as? Long)?.toDouble() 
                            ?: (snapshot.get("balance") as? Double) ?: 0.0
                        
                        Log.d(TAG, "         Current balance: $currentBalance")
                        Log.d(TAG, "         Current transactions count: ${existingTransactions.size}")
                        
                        // Find matching transaction entry to get its amount sign
                        // Match by transactionNumber OR reference OR orderNumber OR description/amount for transfers
                        val matchingTrans = existingTransactions.find { trans ->
                            val matchTransactionNumber = (trans["transactionNumber"] as? Number)?.toInt() == transaction.transactionNumber
                            val matchReference = (trans["reference"] as? DocumentReference)?.path == transaction.relatedRef?.path
                            val matchOrderNumber = (trans["orderNumber"] as? Number)?.toInt() == transaction.orderNumber
                            // For transfers, also try matching by description and amount (absolute value)
                            val matchDescription = isTransfer && (trans["description"] as? String)?.contains("Transfer", ignoreCase = true) == true
                            val matchAmount = isTransfer && kotlin.math.abs((trans["amount"] as? Number)?.toDouble() ?: 0.0) == amountToReverse
                            val matchType = isTransfer && (trans["type"] as? String)?.let { it == "TRANSFER_OUT" || it == "TRANSFER_IN" } == true
                            matchTransactionNumber || matchReference || matchOrderNumber || (matchDescription && matchAmount) || (matchType && matchAmount)
                        }
                        
                        // Get the stored amount from capital transaction (can be negative for outflows)
                        val storedAmount = matchingTrans?.let { trans ->
                            (trans["amount"] as? Number)?.toDouble() ?: amountToReverse
                        } ?: run {
                            // If not found and it's a transfer, infer from capital type and transfer direction
                            if (isTransfer) {
                                // For transfers between capital accounts:
                                // - FROM account has negative stored amount (outflow) or type "TRANSFER_OUT"
                                // - TO account has positive stored amount (inflow) or type "TRANSFER_IN"
                                // Check if this capital type is the FROM or TO account
                                val isFromAccount = when {
                                    transferFromType != null -> capitalType.equals(transferFromType, ignoreCase = true)
                                    // If both cashAmount and bankAmount are set, check for TRANSFER_OUT type in transactions
                                    else -> {
                                        // Try to find any transaction with TRANSFER_OUT type for this capital
                                        existingTransactions.any { it["type"] == "TRANSFER_OUT" }
                                    }
                                }
                                if (isFromAccount) {
                                    -amountToReverse // Negative for FROM account
                                } else {
                                    amountToReverse // Positive for TO account
                                }
                            } else {
                                amountToReverse // Default to positive if not a transfer
                            }
                        }
                        
                        // Remove transaction entry matching this transaction
                        val initialCount = existingTransactions.size
                        val updatedTransactions = existingTransactions.filterNot { trans ->
                            val matchTransactionNumber = (trans["transactionNumber"] as? Number)?.toInt() == transaction.transactionNumber
                            val matchReference = (trans["reference"] as? DocumentReference)?.path == transaction.relatedRef?.path
                            val matchOrderNumber = (trans["orderNumber"] as? Number)?.toInt() == transaction.orderNumber
                            // For transfers, also match by description and amount
                            val matchDescription = isTransfer && (trans["description"] as? String)?.contains("Transfer", ignoreCase = true) == true
                            val matchAmount = isTransfer && kotlin.math.abs((trans["amount"] as? Number)?.toDouble() ?: 0.0) == amountToReverse
                            val matchType = isTransfer && (trans["type"] as? String)?.let { it == "TRANSFER_OUT" || it == "TRANSFER_IN" } == true
                            matchTransactionNumber || matchReference || matchOrderNumber || (matchDescription && matchAmount) || (matchType && matchAmount)
                        }
                        val removedCount = initialCount - updatedTransactions.size
                        Log.d(TAG, "         Removed $removedCount transaction entry/entries from transactions array")
                        Log.d(TAG, "         Updated transactions count: ${updatedTransactions.size}")
                        Log.d(TAG, "         Stored amount in capital transaction: $storedAmount")
                        
                        // Reverse balance change based on transaction type
                        // For transfers, use the sign of stored amount: negative = outflow (reverse by adding), positive = inflow (reverse by subtracting)
                        val newBalance = when {
                            isTransfer -> {
                                // Use sign of stored amount: negative = outflow, positive = inflow
                                val newBal = if (storedAmount < 0) {
                                    // Negative amount = outflow was recorded, reverse by adding (subtracting negative = adding)
                                    currentBalance + amountToReverse
                                } else {
                                    // Positive amount = inflow was recorded, reverse by subtracting
                                    currentBalance - amountToReverse
                                }
                                Log.d(TAG, "         Type: TRANSFER (stored amount: $storedAmount)")
                                Log.d(TAG, "         ${if (storedAmount < 0) "Outflow" else "Inflow"} - reversing by ${if (storedAmount < 0) "adding" else "subtracting"}")
                                Log.d(TAG, "         Calculation: $currentBalance ${if (storedAmount < 0) "+" else "-"} $amountToReverse = $newBal")
                                newBal
                            }
                            transaction.type == TransactionType.SALE -> {
                                val newBal = currentBalance - amountToReverse
                                Log.d(TAG, "         Type: SALE (subtracting from capital)")
                                Log.d(TAG, "         Calculation: $currentBalance - $amountToReverse = $newBal")
                                newBal
                            }
                            transaction.type == TransactionType.PURCHASE -> {
                                val newBal = currentBalance + amountToReverse
                                Log.d(TAG, "         Type: PURCHASE (adding to capital)")
                                Log.d(TAG, "         Calculation: $currentBalance + $amountToReverse = $newBal")
                                newBal
                            }
                            transaction.type == TransactionType.EMI_PAYMENT -> {
                                val newBal = currentBalance - amountToReverse
                                Log.d(TAG, "         Type: EMI_PAYMENT (subtracting from capital)")
                                Log.d(TAG, "         Calculation: $currentBalance - $amountToReverse = $newBal")
                                newBal
                            }
                            transaction.type == TransactionType.BROKER_FEE -> {
                                val newBal = currentBalance + amountToReverse
                                Log.d(TAG, "         Type: BROKER_FEE (adding to capital)")
                                Log.d(TAG, "         Calculation: $currentBalance + $amountToReverse = $newBal")
                                newBal
                            }
                            else -> {
                                Log.d(TAG, "         Type: ${transaction.type} (no balance change)")
                                currentBalance
                            }
                        }
                        
                        Log.d(TAG, "         ✅ New balance: $newBalance")
                        Log.d(TAG, "         ✅ Updating $capitalType capital document")
                        firestoreTransaction.set(
                            capitalRef,
                            mapOf(
                                "transactions" to updatedTransactions,
                                "balance" to newBalance
                            ),
                            SetOptions.merge()
                        )
                    } else {
                        Log.d(TAG, "         ⚠️  Amount to reverse is 0, skipping")
                    }
                }
                
                // 5. Update Purchase document's transactionNumber if needed
                if (purchaseSnapshot != null && purchaseSnapshot!!.exists()) {
                    Log.d(TAG, "   🔄 Step 2.5: Updating Purchase document...")
                    val purchaseTransactionNumber = (purchaseSnapshot!!.get("transactionNumber") as? Number)?.toInt()
                    Log.d(TAG, "      Purchase transaction number: $purchaseTransactionNumber")
                    Log.d(TAG, "      Transaction transaction number: ${transaction.transactionNumber}")
                    if (purchaseTransactionNumber == transaction.transactionNumber) {
                        // Clear the transactionNumber field since we're deleting the referenced transaction
                        Log.d(TAG, "      ✅ Clearing transactionNumber in Purchase document (setting to 0)")
                        firestoreTransaction.update(purchaseSnapshot!!.reference, "transactionNumber", 0)
                    } else {
                        Log.d(TAG, "      ⚠️  Transaction numbers don't match, skipping update")
                    }
                } else {
                    if (transaction.type == TransactionType.PURCHASE || transaction.type == TransactionType.BROKER_FEE) {
                        Log.d(TAG, "   ⚠️  Step 2.5: Skipping Purchase document update (snapshot not found)")
                    }
                }
                
                // 6. Delete VehicleSale and EmiDetails documents for SALE transactions
                if (transaction.type == TransactionType.SALE && saleSnapshot != null && saleSnapshot!!.exists()) {
                    Log.d(TAG, "   🗑️  Step 2.6: Deleting VehicleSale and related documents...")
                    
                    // Delete EmiDetails document if it exists (read in Phase 1)
                    saleEmiDetailsSnapshot?.let { snapshot ->
                        if (snapshot.exists()) {
                            Log.d(TAG, "      🗑️  Deleting EmiDetails document: ${snapshot.reference.path}")
                            firestoreTransaction.delete(snapshot.reference)
                            Log.d(TAG, "      ✅ EmiDetails document marked for deletion")
                        } else {
                            Log.d(TAG, "      ⚠️  EmiDetails document does not exist")
                        }
                    } ?: Log.d(TAG, "      ℹ️  No EmiDetails document (not an EMI sale)")
                    
                    // Delete VehicleSale document
                    Log.d(TAG, "      🗑️  Deleting VehicleSale document: ${saleSnapshot!!.reference.path}")
                    firestoreTransaction.delete(saleSnapshot!!.reference)
                    Log.d(TAG, "      ✅ VehicleSale document marked for deletion")
                } else {
                    if (transaction.type == TransactionType.SALE) {
                        Log.d(TAG, "   ⚠️  Step 2.6: Skipping VehicleSale deletion (snapshot not found)")
                    }
                }
                
                // 7. Delete the transaction document
                Log.d(TAG, "   🗑️  Step 2.7: Deleting transaction document...")
                firestoreTransaction.delete(transactionCollection.document(transactionId))
                Log.d(TAG, "      ✅ Transaction document marked for deletion")
                Log.d(TAG, "✅ PHASE 2 Complete: All updates and deletions prepared")
            }.await()
            Log.d(TAG, "✅ Firestore Transaction completed successfully")
            
            // Handle brand inventory update for SALE transactions (outside transaction due to query limitation)
            if (transaction.type == TransactionType.SALE && productBrandId != null && productId != null && productType != null) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔄 Step 3: Updating Brand inventory for SALE transaction...")
                Log.d(TAG, "   Brand ID: $productBrandId")
                Log.d(TAG, "   Product ID: $productId")
                Log.d(TAG, "   Product Type: $productType")
                try {
                    val brandId = productBrandId
                    
                    if (brandId != null) {
                        Log.d(TAG, "   📖 Querying Brand document...")
                        val brandQuery = brandCollection.whereEqualTo("brandId", brandId).limit(1).get().await()
                        val brandDoc = brandQuery.documents.firstOrNull()
                        
                        brandDoc?.let { doc ->
                            Log.d(TAG, "   ✅ Brand document found: ${doc.id}")
                            val currentVehicles = doc.get("vehicle") as? List<Map<String, Any>> ?: emptyList()
                            Log.d(TAG, "   Current vehicles count: ${currentVehicles.size}")
                            
                            val vehicleIndex = currentVehicles.indexOfFirst { 
                                it["productId"] == productId && it["type"] == productType 
                            }
                            
                            if (vehicleIndex != -1) {
                                Log.d(TAG, "   ✅ Vehicle found at index: $vehicleIndex")
                                val updatedVehicles = currentVehicles.toMutableList()
                                val vehicle = updatedVehicles[vehicleIndex].toMutableMap()
                                val currentQuantity = (vehicle["quantity"] as? Long)?.toInt() ?: 0
                                val newQuantity = currentQuantity + 1
                                
                                Log.d(TAG, "   Current quantity: $currentQuantity")
                                Log.d(TAG, "   New quantity: $newQuantity")
                                vehicle["quantity"] = newQuantity
                                updatedVehicles[vehicleIndex] = vehicle
                                
                                Log.d(TAG, "   ✅ Updating Brand inventory...")
                                brandCollection.document(doc.id)
                                    .update("vehicle", updatedVehicles)
                                    .await()
                                Log.d(TAG, "   ✅ Brand inventory updated successfully")
                            } else {
                                Log.w(TAG, "   ⚠️  Vehicle not found in Brand inventory (productId: $productId, type: $productType)")
                            }
                        } ?: Log.w(TAG, "   ⚠️  Brand document not found for brandId: $brandId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Error updating brand inventory: ${e.message}", e)
                    // Don't fail the entire deletion if brand update fails
                }
            } else {
                if (transaction.type == TransactionType.SALE) {
                    Log.d(TAG, "   ⚠️  Step 3: Skipping Brand inventory update (missing product info)")
                }
            }
            
            // Handle PURCHASE transaction cleanup (outside transaction due to query limitation)
            if (transaction.type == TransactionType.PURCHASE && transaction.relatedRef != null) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔄 Step 4: Cleaning up PURCHASE transaction data...")
                Log.d(TAG, "   Purchase Reference: ${transaction.relatedRef!!.path}")
                try {
                    // 1. Read Purchase document to get chassisNumber from vehicle map
                    Log.d(TAG, "   📖 Step 4.1: Reading Purchase document...")
                    val purchaseDoc = transaction.relatedRef!!.get().await()
                    if (purchaseDoc.exists()) {
                        Log.d(TAG, "   ✅ Purchase document found: ${purchaseDoc.id}")
                        val vehicleMap = purchaseDoc.get("vehicle") as? Map<String, String> ?: emptyMap()
                        val chassisNumber = vehicleMap["chassisNumber"] as? String
                        Log.d(TAG, "   Vehicle map keys: ${vehicleMap.keys}")
                        Log.d(TAG, "   Chassis Number: $chassisNumber")
                        
                        if (chassisNumber != null) {
                            // 2. Find Product by chassisNumber
                            Log.d(TAG, "   📖 Step 4.2: Finding Product by chassis number...")
                            val productQuery = productCollection
                                .whereEqualTo("chassisNumber", chassisNumber)
                                .limit(1)
                                .get()
                                .await()
                            
                            val productDoc = productQuery.documents.firstOrNull()
                            
                            if (productDoc != null) {
                                Log.d(TAG, "   ✅ Product document found: ${productDoc.id}")
                                val productId = productDoc.get("productId") as? String
                                val productType = productDoc.get("type") as? String
                                val brandId = productDoc.get("brandId") as? String
                                val chassisRef = productDoc.get("chassisReference") as? DocumentReference
                                
                                Log.d(TAG, "   Product Details:")
                                Log.d(TAG, "      Product ID: $productId")
                                Log.d(TAG, "      Product Type: $productType")
                                Log.d(TAG, "      Brand ID: $brandId")
                                Log.d(TAG, "      Chassis Ref: ${chassisRef?.path ?: "None"}")
                                
                                // 3. Delete Product document
                                Log.d(TAG, "   🗑️  Step 4.3: Deleting Product document...")
                                productDoc.reference.delete().await()
                                Log.d(TAG, "      ✅ Deleted Product document for chassis: $chassisNumber")
                                
                                // 4. Delete Chassis document
                                if (chassisRef != null) {
                                    Log.d(TAG, "   🗑️  Step 4.4: Deleting Chassis document...")
                                    Log.d(TAG, "      Chassis Ref Path: ${chassisRef.path}")
                                    chassisRef.delete().await()
                                    Log.d(TAG, "      ✅ Deleted Chassis document")
                                } else {
                                    Log.w(TAG, "   ⚠️  Step 4.4: No Chassis reference found in Product")
                                }
                                
                                // 5. Update Brand inventory (decrement quantity)
                                if (brandId != null && productId != null && productType != null) {
                                    Log.d(TAG, "   🔄 Step 4.5: Updating Brand inventory...")
                                    Log.d(TAG, "      Brand ID: $brandId")
                                    val brandQuery = brandCollection
                                        .whereEqualTo("brandId", brandId)
                                        .limit(1)
                                        .get()
                                        .await()
                                    
                                    val brandDoc = brandQuery.documents.firstOrNull()
                                    brandDoc?.let { doc ->
                                        Log.d(TAG, "      ✅ Brand document found: ${doc.id}")
                                        val currentVehicles = doc.get("vehicle") as? List<Map<String, Any>> ?: emptyList()
                                        Log.d(TAG, "      Current vehicles count: ${currentVehicles.size}")
                                        
                                        val vehicleIndex = currentVehicles.indexOfFirst { 
                                            it["productId"] == productId && it["type"] == productType 
                                        }
                                        
                                        if (vehicleIndex != -1) {
                                            Log.d(TAG, "      ✅ Vehicle found at index: $vehicleIndex")
                                            val updatedVehicles = currentVehicles.toMutableList()
                                            val vehicle = updatedVehicles[vehicleIndex].toMutableMap()
                                            val currentQuantity = (vehicle["quantity"] as? Long)?.toInt() ?: 0
                                            Log.d(TAG, "      Current quantity: $currentQuantity")
                                            
                                            if (currentQuantity > 1) {
                                                // Decrement quantity
                                                val newQuantity = currentQuantity - 1
                                                vehicle["quantity"] = newQuantity
                                                updatedVehicles[vehicleIndex] = vehicle
                                                Log.d(TAG, "      ✅ Decrementing quantity: $currentQuantity -> $newQuantity")
                                            } else {
                                                // Remove entry if quantity becomes 0
                                                updatedVehicles.removeAt(vehicleIndex)
                                                Log.d(TAG, "      ✅ Removing vehicle entry (quantity would become 0)")
                                            }
                                            
                                            Log.d(TAG, "      ✅ Updating Brand inventory...")
                                            brandCollection.document(doc.id)
                                                .update("vehicle", updatedVehicles)
                                                .await()
                                            Log.d(TAG, "      ✅ Brand inventory updated successfully")
                                        } else {
                                            Log.w(TAG, "      ⚠️  Vehicle not found in Brand inventory (productId: $productId, type: $productType)")
                                        }
                                    } ?: Log.w(TAG, "      ⚠️  Brand document not found for brandId: $brandId")
                                } else {
                                    Log.w(TAG, "   ⚠️  Step 4.5: Missing product info (brandId: $brandId, productId: $productId, productType: $productType)")
                                }
                            } else {
                                Log.w(TAG, "   ⚠️  Step 4.2: Product not found for chassis: $chassisNumber")
                            }
                        } else {
                            Log.w(TAG, "   ⚠️  Step 4.1: Chassis number not found in Purchase vehicle map")
                        }
                        
                        // 6. Delete Purchase document
                        Log.d(TAG, "   🗑️  Step 4.6: Deleting Purchase document...")
                        purchaseDoc.reference.delete().await()
                        Log.d(TAG, "      ✅ Deleted Purchase document: ${purchaseDoc.id}")
                    } else {
                        Log.w(TAG, "   ⚠️  Step 4.1: Purchase document not found: ${transaction.relatedRef!!.path}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Error cleaning up purchase-related data: ${e.message}", e)
                    Log.e(TAG, "   Stack trace:", e)
                    // Don't fail the entire deletion if cleanup fails
                }
            } else {
                if (transaction.type == TransactionType.PURCHASE) {
                    Log.d(TAG, "   ⚠️  Step 4: Skipping PURCHASE cleanup (no related reference)")
                }
            }
            
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "✅ TRANSACTION DELETION COMPLETED SUCCESSFULLY")
            Log.d(TAG, "   Transaction ID: $transactionId")
            Log.d(TAG, "   Transaction Type: ${transaction.type}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌ TRANSACTION DELETION FAILED")
            Log.e(TAG, "   Transaction ID: $transactionId")
            Log.e(TAG, "   Error: ${e.message}", e)
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
    }
    
}

