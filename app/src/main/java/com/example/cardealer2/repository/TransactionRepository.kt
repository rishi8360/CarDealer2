package com.example.cardealer2.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.cardealer2.data.PersonTransaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

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
    
    // StateFlow for transactions
    private val _transactions = MutableStateFlow<List<PersonTransaction>>(emptyList())
    val transactions: StateFlow<List<PersonTransaction>> = _transactions.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var listenerRegistration: ListenerRegistration? = null
    
    // Coroutine scope for repository operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        startListening()
    }
    
    /**
     * Start listening to Transactions collection changes
     */
    private fun startListening() {
        listenerRegistration?.remove()
        
        _isLoading.value = true
        _error.value = null
        
        listenerRegistration = transactionCollection.addSnapshotListener { snapshot, error ->
            _isLoading.value = false
            
            if (error != null) {
                val errorMsg = error.message ?: "Error loading transactions"
                _error.value = errorMsg
                Log.e(TAG, "❌ Error in transaction listener: $errorMsg", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                try {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        try {
                            docToPersonTransaction(doc)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing transaction document ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    _transactions.value = transactions
                    _error.value = null
                    Log.d(TAG, "✅ Transaction listener updated: ${transactions.size} transactions")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing transaction snapshot: ${e.message}", e)
                    _error.value = "Error processing transactions: ${e.message}"
                }
            }
        }
        
        Log.d(TAG, "✅ Transaction listener started successfully")
    }
    
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
                date = doc.getTimestampOrLong("date"),
                orderNumber = (doc.get("orderNumber") as? Number)?.toInt(),
                description = doc.get("description") as? String ?: "",
                status = doc.get("status") as? String ?: "COMPLETED",
                createdAt = doc.getTimestampOrLong("createdAt")
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
     * Create a new transaction record
     * This should be called from within existing transactions (Purchase/Sale) to ensure atomicity
     */
    suspend fun createTransaction(transaction: PersonTransaction): Result<String> {
        return try {
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
                "date" to Timestamp(Date(transaction.date)),
                "description" to transaction.description,
                "status" to transaction.status,
                "createdAt" to Timestamp(Date(transaction.createdAt))
            )
            
            // Add references if they exist
            transaction.personRef?.let { transactionData["personRef"] = it }
            transaction.relatedRef?.let { transactionData["relatedRef"] = it }
            transaction.orderNumber?.let { transactionData["orderNumber"] = it }
            
            transactionDocRef.set(transactionData).await()
            
            Log.d(TAG, "✅ Transaction created successfully with ID: $transactionId")
            Result.success(transactionId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating transaction: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create transaction record within a Firestore transaction
     * Use this when you need to create transaction as part of an atomic operation
     */
    fun createTransactionInTransaction(
        transaction: PersonTransaction,
        firestoreTransaction: com.google.firebase.firestore.Transaction
    ): String {
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
            "date" to Timestamp(Date(transaction.date)),
            "description" to transaction.description,
            "status" to transaction.status,
            "createdAt" to Timestamp(Date(transaction.createdAt))
        )
        
        // Add references if they exist
        transaction.personRef?.let { transactionData["personRef"] = it }
        transaction.relatedRef?.let { transactionData["relatedRef"] = it }
        transaction.orderNumber?.let { transactionData["orderNumber"] = it }
        
        firestoreTransaction.set(transactionDocRef, transactionData)
        
        Log.d(TAG, "✅ Transaction created in transaction with ID: $transactionId")
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
     * Get all transactions - now returns StateFlow value
     * Use transactions StateFlow directly in ViewModels instead
     */
    @Deprecated("Use transactions StateFlow directly instead")
    suspend fun getAllTransactions(): Result<List<PersonTransaction>> {
        return Result.success(_transactions.value)
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
}

