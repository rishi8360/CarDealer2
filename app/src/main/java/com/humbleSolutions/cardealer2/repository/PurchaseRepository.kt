package com.humbleSolutions.cardealer2.repository

import android.util.Log
import com.humbleSolutions.cardealer2.data.Purchase
import com.humbleSolutions.cardealer2.data.CapitalTransaction
import com.humbleSolutions.cardealer2.data.PersonTransaction
import com.humbleSolutions.cardealer2.data.TransactionType
import com.humbleSolutions.cardealer2.data.PersonType
import com.humbleSolutions.cardealer2.data.PaymentMethod
import com.humbleSolutions.cardealer2.data.Product
import com.humbleSolutions.cardealer2.data.TransactionResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PurchaseRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Configure Firestore settings for better reliability on real devices
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Unlimited cache
            .build()
        firestoreSettings = settings
    }
    
    private const val TAG = "PurchaseRepository"
    
    private val purchaseCollection = db.collection("Purchase")
    private val capitalCollection = db.collection("Capital")
    private val maxOrderNoCollection = db.collection("MaxOrderNo")
    private val maxTransactionNoCollection = db.collection("MaxTransactionNo")
    
    // 🔹 StateFlow exposed to ViewModels - automatically updates when Firestore changes
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    // 🔹 MaxOrderNo StateFlow - listens to the first document in MaxOrderNo collection
    private val _maxOrderNo = MutableStateFlow<Int>(0)
    val maxOrderNo: StateFlow<Int> = _maxOrderNo.asStateFlow()

    // 🔹 MaxTransactionNo StateFlow - listens to the first document in MaxTransactionNo collection
    private val _maxTransactionNo = MutableStateFlow<Int>(0)
    val maxTransactionNo: StateFlow<Int> = _maxTransactionNo.asStateFlow()

    // 🔹 Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 🔹 Keep references to listeners
    private var purchaseListenerRegistration: ListenerRegistration? = null
    private var maxOrderNoListenerRegistration: ListenerRegistration? = null
    private var maxOrderNoDocRef: DocumentReference? = null
    private var maxTransactionNoListenerRegistration: ListenerRegistration? = null
    private var maxTransactionNoDocRef: DocumentReference? = null

    // 🔹 Coroutine scope for repository operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 🔹 Initialize listeners when repository is first accessed
    init {
        startListening()
        repositoryScope.launch {
            initializeMaxOrderNoListener()
            initializeMaxTransactionNoListener()
        }
    }

    /**
     * Start listening to Purchase collection changes
     */
    private fun startListening() {
        purchaseListenerRegistration?.remove()

        _isLoading.value = true
        _error.value = null

        purchaseListenerRegistration = purchaseCollection.addSnapshotListener { snapshot, error ->
            _isLoading.value = false

            if (error != null) {
                val errorMsg = error.message ?: "Error loading purchases"
                _error.value = errorMsg
                Log.e(TAG, "❌ Error in purchase listener: $errorMsg", error)
                Log.e(TAG, "Error code: ${error.code}, Error details: ${error.cause?.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    Log.d(TAG, "📥 Received snapshot with ${snapshot.documents.size} documents")

                    val purchases = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Purchase::class.java)?.copy(purchaseId = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing purchase document ${doc.id}: ${e.message}", e)
                            null // Skip this document and continue with others
                        }
                    }
                    _purchases.value = purchases
                    _error.value = null
                    Log.d(TAG, "✅ Purchase listener updated: ${purchases.size} purchases")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing purchase snapshot: ${e.message}", e)
                    _error.value = "Error processing purchases: ${e.message}"
                }
            } else {
                Log.w(TAG, "⚠️ Snapshot is null in purchase listener")
            }
        }
    }

    /**
     * Initialize and listen to MaxOrderNo document
     */
    private suspend fun initializeMaxOrderNoListener() {
        maxOrderNoListenerRegistration?.remove()

        try {
            // Check if any document exists
            val existingDocs = maxOrderNoCollection.limit(1).get().await()
            
            if (existingDocs.isEmpty) {
                // Create initial document
                val docRef = maxOrderNoCollection.document()
                docRef.set(mapOf("maxOrderNo" to 0)).await()
                maxOrderNoDocRef = docRef
            } else {
                maxOrderNoDocRef = existingDocs.documents.first().reference
            }

            // Start listening to the document
            maxOrderNoDocRef?.let { docRef ->
                maxOrderNoListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val errorMsg = error.message ?: "Error loading MaxOrderNo"
                        Log.e(TAG, "❌ Error in MaxOrderNo listener: $errorMsg", error)
                        _error.value = errorMsg
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val maxOrderNo = getIntFromFirestore(snapshot.get("maxOrderNo"))
                            _maxOrderNo.value = maxOrderNo
                            _error.value = null
                            Log.d(TAG, "✅ MaxOrderNo listener updated: $maxOrderNo")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing MaxOrderNo: ${e.message}", e)
                            _error.value = "Error parsing MaxOrderNo: ${e.message}"
                        }
                    } else {
                        Log.w(TAG, "⚠️ MaxOrderNo snapshot is null or doesn't exist")
                    }
                }
                Log.d(TAG, "✅ MaxOrderNo listener started successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing MaxOrderNo listener: ${e.message}", e)
        }
    }

    /**
     * Initialize and listen to MaxTransactionNo document
     */
    private suspend fun initializeMaxTransactionNoListener() {
        maxTransactionNoListenerRegistration?.remove()

        try {
            // Check if any document exists
            val existingDocs = maxTransactionNoCollection.limit(1).get().await()
            
            if (existingDocs.isEmpty) {
                // Create initial document
                val docRef = maxTransactionNoCollection.document()
                docRef.set(mapOf("maxTransactionNo" to 0)).await()
                maxTransactionNoDocRef = docRef
            } else {
                maxTransactionNoDocRef = existingDocs.documents.first().reference
            }

            // Start listening to the document
            maxTransactionNoDocRef?.let { docRef ->
                maxTransactionNoListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val errorMsg = error.message ?: "Error loading MaxTransactionNo"
                        Log.e(TAG, "❌ Error in MaxTransactionNo listener: $errorMsg", error)
                        _error.value = errorMsg
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val maxTransactionNo = getIntFromFirestore(snapshot.get("maxTransactionNo"))
                            _maxTransactionNo.value = maxTransactionNo
                            _error.value = null
                            Log.d(TAG, "✅ MaxTransactionNo listener updated: $maxTransactionNo")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing MaxTransactionNo: ${e.message}", e)
                            _error.value = "Error parsing MaxTransactionNo: ${e.message}"
                        }
                    } else {
                        Log.w(TAG, "⚠️ MaxTransactionNo snapshot is null or doesn't exist")
                    }
                }
                Log.d(TAG, "✅ MaxTransactionNo listener started successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing MaxTransactionNo listener: ${e.message}", e)
        }
    }

    /**
     * Stop listening (useful for cleanup)
     */
    fun stopListening() {
        purchaseListenerRegistration?.remove()
        maxOrderNoListenerRegistration?.remove()
        maxTransactionNoListenerRegistration?.remove()
        purchaseListenerRegistration = null
        maxOrderNoListenerRegistration = null
        maxTransactionNoListenerRegistration = null
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
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
     * Initialize MaxOrderNo collection if it doesn't exist
     * Creates a document with auto-generated ID containing MaxOrderNo = 0
     * @deprecated Listener handles initialization automatically
     */
    @Deprecated("Listener handles initialization automatically")
    suspend fun initializeMaxOrderNo(): Result<Unit> {
        return try {
            // Check if any document exists in MaxOrderNo collection
            val existingDocs = maxOrderNoCollection.limit(1).get().await()
            
            if (existingDocs.isEmpty) {
                // Create initial document with MaxOrderNo = 0
                val docRef = maxOrderNoCollection.document()
                val maxOrderNoData = hashMapOf(
                    "maxOrderNo" to 0
                )
                docRef.set(maxOrderNoData).await()
                Log.d(TAG, "✅ Initialized MaxOrderNo collection with value 0")
            } else {
                val doc = existingDocs.documents.first()
                val maxOrderNo = getIntFromFirestore(doc.get("maxOrderNo"))
                Log.d(TAG, "✅ MaxOrderNo already exists with value: $maxOrderNo")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing MaxOrderNo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the current MaxOrderNo - now returns StateFlow value
     * Use maxOrderNo StateFlow directly in ViewModels instead
     * @deprecated Use maxOrderNo StateFlow directly instead
     */
    @Deprecated("Use maxOrderNo StateFlow directly instead")
    suspend fun getMaxOrderNo(): Result<Int> {
        return Result.success(_maxOrderNo.value)
    }
    
    /**
     * Increment and get the next order number
     */
    suspend fun getNextOrderNumber(): Result<Int> {
        return try {
            // Use stored docRef if available, otherwise get it
            val docRef = if (maxOrderNoDocRef != null) {
                maxOrderNoDocRef!!
            } else {
                // Fallback: get the document
                val docs = maxOrderNoCollection.limit(1).get().await()
                if (docs.isEmpty) {
                    // Initialize if not exists
                    val newDocRef = maxOrderNoCollection.document()
                    newDocRef.set(mapOf("maxOrderNo" to 0)).await()
                    maxOrderNoDocRef = newDocRef
                    newDocRef
                } else {
                    maxOrderNoDocRef = docs.documents.first().reference
                    docs.documents.first().reference
                }
            }
            
            // Use transaction to atomically increment
            val result = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentMax = getIntFromFirestore(snapshot.get("maxOrderNo"))
                val newMax = currentMax + 1
                transaction.update(docRef, "maxOrderNo", newMax)
                newMax
            }.await()
            
            // ✅ No need to update cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Next order number: $result")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting next order number: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add a new purchase
     */
    suspend fun addPurchase(
        grandTotal: Double,
        gstAmount: Double,
        paymentMethods: Map<String, String>,  // Map with keys: "bank", "cash", "credit"
        vehicle: Map<String, String>,
        middleMan: String,
        ownerRef: DocumentReference? = null,
        brokerRef: DocumentReference? = null
    ): Result<String> {
        return try {
            // Get next order number
            val orderNumberResult = getNextOrderNumber()
            if (orderNumberResult.isFailure) {
                return Result.failure(orderNumberResult.exceptionOrNull() ?: Exception("Failed to get order number"))
            }
            val orderNumber = orderNumberResult.getOrThrow()
            
            // Create purchase document with auto-generated ID
            val purchaseDocRef = purchaseCollection.document()
            val purchaseId = purchaseDocRef.id
            
            val purchaseData = hashMapOf<String, Any>(
                "purchaseId" to purchaseId,
                "grandTotal" to grandTotal,
                "gstAmount" to gstAmount,
                "orderNumber" to orderNumber,
                "paymentMethods" to paymentMethods,
                "vehicle" to vehicle,
                "middleMan" to middleMan,
                "createdAt" to System.currentTimeMillis()
            )
            
            // Add ownerRef if provided
            ownerRef?.let {
                purchaseData["ownerRef"] = it
            }
            
            // Add brokerRef if provided
            brokerRef?.let {
                purchaseData["brokerRef"] = it
            }
            
            purchaseDocRef.set(purchaseData).await()
            
            // ✅ No need to invalidate cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Purchase created successfully with ID: $purchaseId, Order Number: $orderNumber")
            Result.success(purchaseId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding purchase: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get purchase by ID
     */
    suspend fun getPurchaseById(purchaseId: String): Result<Purchase> {
        return try {
            val document = purchaseCollection.document(purchaseId).get().await()
            if (document.exists()) {
                val purchase = document.toObject(Purchase::class.java)
                    ?: return Result.failure(Exception("Failed to parse purchase data"))
                Result.success(purchase.copy(purchaseId = purchaseId))
            } else {
                Result.failure(Exception("Purchase not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting purchase: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get purchase by DocumentReference
     */
    suspend fun getPurchaseByReference(purchaseRef: com.google.firebase.firestore.DocumentReference): Result<Purchase> {
        return try {
            val document = purchaseRef.get().await()
            if (document.exists()) {
                val purchase = document.toObject(Purchase::class.java)
                    ?: return Result.failure(Exception("Failed to parse purchase data"))
                Result.success(purchase.copy(purchaseId = document.id))
            } else {
                Result.failure(Exception("Purchase not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting purchase by reference: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all purchases - now returns StateFlow value
     * Use purchases StateFlow directly in ViewModels instead
     * @deprecated Use purchases StateFlow directly instead
     */
    @Deprecated("Use purchases StateFlow directly instead")
    suspend fun getAllPurchases(): Result<List<Purchase>> {
        return Result.success(_purchases.value)
    }
    
    /**
     * Add capital transaction to Cash, Bank, or Credit document
     * The Capital collection has three documents: "Cash", "Bank", "Credit"
     * Each document contains an array of transactions and a balance field
     */
    suspend fun addCapitalTransaction(
        type: String,  // "Cash", "Bank", or "Credit"
        purchaseReference: DocumentReference?,
        orderNumber: Int,
        amount: Double
    ): Result<String> {
        return try {
            // Validate type
            if (type !in listOf("Cash", "Bank", "Credit")) {
                return Result.failure(Exception("Invalid capital type. Must be Cash, Bank, or Credit"))
            }
            
            // Get or create the capital document (Cash, Bank, or Credit)
            val capitalDocRef = capitalCollection.document(type)
            
            val transactionData = hashMapOf<String, Any>(
                "transactionDate" to System.currentTimeMillis(),
                "createdAt" to System.currentTimeMillis(),
                "orderNumber" to orderNumber,
                "amount" to amount
            )
            
            // Add purchase reference if provided
            purchaseReference?.let {
                transactionData["reference"] = it
            }
            
            // Use transaction to atomically add to array and subtract from balance
            db.runTransaction { transaction ->
                val snapshot = transaction.get(capitalDocRef)
                val existingTransactions = snapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                val currentBalance = (snapshot.get("balance") as? Long)?.toDouble() ?: (snapshot.get("balance") as? Double) ?: 0.0
                
                // Subtract amount from balance (purchase reduces capital)
                val newBalance = currentBalance - amount
                
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
            
            Log.d(TAG, "✅ Capital transaction added to $type, amount: $amount subtracted from balance")
            Result.success("success")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding capital transaction: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current balance for a specific capital type (Cash, Bank, or Credit)
     */
    suspend fun getCapitalBalance(type: String): Result<Double> {
        return try {
            if (type !in listOf("Cash", "Bank", "Credit")) {
                return Result.failure(Exception("Invalid capital type. Must be Cash, Bank, or Credit"))
            }
            
            val capitalDocRef = capitalCollection.document(type)
            val document = capitalDocRef.get().await()
            
            if (!document.exists()) {
                return Result.success(0.0)
            }
            
            val balance = (document.get("balance") as? Long)?.toDouble() 
                ?: (document.get("balance") as? Double) ?: 0.0
            
            Log.d(TAG, "✅ Loaded balance for $type: $balance")
            Result.success(balance)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting capital balance: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Adjust capital balance manually (add or subtract)
     * Positive amount adds to balance, negative amount subtracts
     */
    suspend fun adjustCapitalBalance(
        type: String,
        amount: Double,
        description: String = "Manual Adjustment",
        reason: String? = null
    ): Result<Unit> {
        return try {
            if (type !in listOf("Cash", "Bank", "Credit")) {
                return Result.failure(Exception("Invalid capital type. Must be Cash, Bank, or Credit"))
            }
            
            val capitalDocRef = capitalCollection.document(type)
            
            val transactionData = hashMapOf<String, Any>(
                "transactionDate" to System.currentTimeMillis(),
                "createdAt" to System.currentTimeMillis(),
                "orderNumber" to 0,
                "amount" to kotlin.math.abs(amount),
                "type" to if (amount >= 0) "MANUAL_ADD" else "MANUAL_SUBTRACT",
                "description" to description
            )
            
            reason?.let {
                transactionData["reason"] = it
            }
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(capitalDocRef)
                val existingTransactions = snapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                val currentBalance = (snapshot.get("balance") as? Long)?.toDouble() 
                    ?: (snapshot.get("balance") as? Double) ?: 0.0
                
                // Add or subtract amount from balance
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
            
            Log.d(TAG, "✅ Adjusted $type balance by $amount. New balance: ${getCapitalBalance(type).getOrNull() ?: 0.0}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adjusting capital balance: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Set capital balance directly (edits the balance to a specific value)
     * Records the difference as a transaction for audit purposes
     */
    suspend fun setCapitalBalance(
        type: String,
        newBalance: Double,
        description: String = "Manual Balance Edit",
        reason: String? = null
    ): Result<Unit> {
        return try {
            if (type !in listOf("Cash", "Bank", "Credit")) {
                return Result.failure(Exception("Invalid capital type. Must be Cash, Bank, or Credit"))
            }
            
            val capitalDocRef = capitalCollection.document(type)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(capitalDocRef)
                val existingTransactions = snapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                val currentBalance = (snapshot.get("balance") as? Long)?.toDouble() 
                    ?: (snapshot.get("balance") as? Double) ?: 0.0
                
                // Calculate the difference for audit trail
                val difference = newBalance - currentBalance
                
                val transactionData = hashMapOf<String, Any>(
                    "transactionDate" to System.currentTimeMillis(),
                    "createdAt" to System.currentTimeMillis(),
                    "orderNumber" to 0,
                    "amount" to kotlin.math.abs(difference),
                    "type" to "MANUAL_EDIT",
                    "description" to description,
                    "previousBalance" to currentBalance,
                    "newBalance" to newBalance
                )
                
                reason?.let {
                    transactionData["reason"] = it
                }
                
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
            
            Log.d(TAG, "✅ Set $type balance to $newBalance")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting capital balance: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Set initial capital balance (creates document if doesn't exist)
     */
    suspend fun setInitialCapitalBalance(
        type: String,
        amount: Double,
        description: String = "Initial Balance"
    ): Result<Unit> {
        return try {
            if (type !in listOf("Cash", "Bank", "Credit")) {
                return Result.failure(Exception("Invalid capital type. Must be Cash, Bank, or Credit"))
            }
            
            val capitalDocRef = capitalCollection.document(type)
            
            val transactionData = hashMapOf<String, Any>(
                "transactionDate" to System.currentTimeMillis(),
                "createdAt" to System.currentTimeMillis(),
                "orderNumber" to 0,
                "amount" to amount,
                "type" to "INITIAL_BALANCE",
                "description" to description
            )
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(capitalDocRef)
                val existingTransactions = snapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                
                // If document exists and has balance, don't overwrite - use adjust instead
                val currentBalance = (snapshot.get("balance") as? Long)?.toDouble() 
                    ?: (snapshot.get("balance") as? Double) ?: 0.0
                
                val updatedTransactions = existingTransactions.toMutableList()
                updatedTransactions.add(transactionData)
                
                transaction.set(
                    capitalDocRef,
                    mapOf(
                        "transactions" to updatedTransactions,
                        "balance" to amount
                    ),
                    SetOptions.merge()
                )
            }.await()
            
            Log.d(TAG, "✅ Set initial balance for $type: $amount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting initial capital balance: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all capital transactions for a specific type (Cash, Bank, or Credit)
     */
    suspend fun getCapitalTransactions(type: String): Result<List<CapitalTransaction>> {
        return try {
            if (type !in listOf("Cash", "Bank", "Credit")) {
                return Result.failure(Exception("Invalid capital type. Must be Cash, Bank, or Credit"))
            }
            
            val capitalDocRef = capitalCollection.document(type)
            val document = capitalDocRef.get().await()
            
            if (!document.exists()) {
                return Result.success(emptyList())
            }
            
            val transactionsList = document.get("transactions") as? List<Map<String, Any>> ?: emptyList()
            
            val transactions = transactionsList.mapNotNull { transactionMap ->
                try {
                    val purchaseRef = transactionMap["reference"] as? DocumentReference
                    CapitalTransaction(
                        transactionDate = (transactionMap["transactionDate"] as? Long) ?: System.currentTimeMillis(),
                        createdAt = (transactionMap["createdAt"] as? Long) ?: System.currentTimeMillis(),
                        purchaseReference = purchaseRef,
                        orderNumber = getIntFromFirestore(transactionMap["orderNumber"])
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Log.d(TAG, "✅ Loaded ${transactions.size} capital transactions for $type")
            Result.success(transactions)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading capital transactions: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add purchase with automatic capital transaction creation
     * Creates purchase and corresponding capital transactions for each payment method
     */
    suspend fun addPurchaseWithCapitalTransactions(
        grandTotal: Double,
        gstAmount: Double,
        paymentMethods: Map<String, String>,  // Map with keys: "bank", "cash", "credit"
        vehicle: Map<String, String>,
        middleMan: String,
        ownerRef: DocumentReference? = null,
        brokerRef: DocumentReference? = null
    ): Result<String> {
        return try {
            // Add purchase
            val purchaseResult = addPurchase(
                grandTotal = grandTotal,
                gstAmount = gstAmount,
                paymentMethods = paymentMethods,
                vehicle = vehicle,
                middleMan = middleMan,
                ownerRef = ownerRef,
                brokerRef = brokerRef
            )
            
            if (purchaseResult.isFailure) {
                return purchaseResult
            }
            
            val purchaseId = purchaseResult.getOrThrow()
            val purchaseDocRef = purchaseCollection.document(purchaseId)
            // Get order number from the purchase
            val purchaseDoc = purchaseCollection.document(purchaseId).get().await()
            val orderNumber = getIntFromFirestore(purchaseDoc.get("orderNumber"))
            
            // Add capital transactions for each payment method that has an amount
            paymentMethods.forEach { (method, amountStr) ->
                if (amountStr.isNotBlank()) {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        val capitalType = when (method.lowercase()) {
                            "cash" -> "Cash"
                            "bank" -> "Bank"
                            "credit" -> "Credit"
                            else -> null
                        }
                        
                        capitalType?.let {
                            addCapitalTransaction(
                                type = it,
                                purchaseReference = purchaseDocRef,
                                orderNumber = orderNumber,
                                amount = amount
                            )
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ Purchase and capital transactions created successfully")
            Result.success(purchaseId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding purchase with capital transactions: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Helper function to determine payment method from payment methods map
     */
    private fun determinePaymentMethod(paymentMethods: Map<String, String>): String {
        val cashAmount = paymentMethods["cash"]?.toDoubleOrNull() ?: 0.0
        val bankAmount = paymentMethods["bank"]?.toDoubleOrNull() ?: 0.0
        val creditAmount = paymentMethods["credit"]?.toDoubleOrNull() ?: 0.0
        
        val nonZeroCount = listOf(cashAmount, bankAmount, creditAmount).count { it > 0 }
        
        return when {
            nonZeroCount > 1 -> PaymentMethod.MIXED
            cashAmount > 0 -> PaymentMethod.CASH
            bankAmount > 0 -> PaymentMethod.BANK
            creditAmount > 0 -> PaymentMethod.CREDIT
            else -> PaymentMethod.CASH
        }
    }
    
    /**
     * Helper function to extract payment amounts from payment methods map
     */
    private fun extractPaymentAmounts(paymentMethods: Map<String, String>): Triple<Double, Double, Double> {
        val cashAmount = paymentMethods["cash"]?.toDoubleOrNull() ?: 0.0
        val bankAmount = paymentMethods["bank"]?.toDoubleOrNull() ?: 0.0
        val creditAmount = paymentMethods["credit"]?.toDoubleOrNull() ?: 0.0
        return Triple(cashAmount, bankAmount, creditAmount)
    }
    
    /**
     * Atomically add purchase with capital transactions AND vehicle in a single transaction
     * This ensures data consistency - either everything succeeds or everything fails
     * 
     * @param grandTotal Total purchase amount
     * @param gstAmount GST amount
     * @param paymentMethods Map with keys: "bank", "cash", "credit" and their amounts as strings
     * @param vehicle Vehicle data map
     * @param middleMan Middle man/broker name
     * @param brandId Brand ID
     * @param product Product data (with uploaded image URLs already set)
     * @param imageUrls Already uploaded image URLs
     * @param nocUrls Already uploaded NOC PDF URLs
     * @param rcUrls Already uploaded RC PDF URLs
     * @param insuranceUrls Already uploaded Insurance PDF URLs
     * @param vehicleOtherDocUrls Already uploaded Other Vehicle Document PDF URLs
     * @param brandDocRef Brand document reference
     * @param brandNameRef BrandNames document reference (optional)
     * @param brokerOrMiddleManRef Broker/Middle Man document reference (optional)
     * @param ownerRef Owner document reference (optional)
     * @param purchaseType Purchase type: "Direct", "Broker", or "Middle Man" (optional, will be determined from refs if not provided)
     * @param brokerFee Broker fee amount (optional, for broker fee transactions)
     * @param brokerFeePaymentMethods Broker fee payment methods map (optional)
     * @return Result with purchase ID on success
     */
    suspend fun addPurchaseWithVehicleAtomic(
        grandTotal: Double,
        gstAmount: Double,
        paymentMethods: Map<String, String>,
        vehicle: Map<String, String>,
        middleMan: String,
        brandId: String,
        product: Product,
        imageUrls: List<String>,
        nocUrls: List<String>,
        rcUrls: List<String>,
        insuranceUrls: List<String>,
        vehicleOtherDocUrls: List<String>,
        brandDocRef: DocumentReference,
        brandNameRef: DocumentReference?,
        brokerOrMiddleManRef: DocumentReference?,
        ownerRef: DocumentReference?,
        purchaseType: String? = null,
        brokerFee: Double = 0.0,
        brokerFeePaymentMethods: Map<String, String>? = null,
        note: String = "",
        date: String = ""
    ): Result<TransactionResult> {
        val purchaseDate = date.ifBlank {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date())
        }
        return try {
            // Get maxOrderNo document reference
            val maxOrderNoDocRefLocal = if (maxOrderNoDocRef != null) {
                maxOrderNoDocRef!!
            } else {
                val docs = maxOrderNoCollection.limit(1).get().await()
                if (docs.isEmpty) {
                    val newDocRef = maxOrderNoCollection.document()
                    newDocRef.set(mapOf("maxOrderNo" to 0)).await()
                    maxOrderNoDocRef = newDocRef
                    newDocRef
                } else {
                    maxOrderNoDocRef = docs.documents.first().reference
                    docs.documents.first().reference
                }
            }
            
            // Get maxTransactionNo document reference
            val maxTransactionNoDocs = maxTransactionNoCollection.limit(1).get().await()
            val maxTransactionNoDocRefLocal = if (maxTransactionNoDocs.isEmpty) {
                val newDocRef = maxTransactionNoCollection.document()
                newDocRef.set(mapOf("maxTransactionNo" to 0)).await()
                newDocRef
            } else {
                maxTransactionNoDocs.documents.first().reference
            }
            
            // Get capital document references
            val cashDocRef = capitalCollection.document("Cash")
            val bankDocRef = capitalCollection.document("Bank")
            val creditDocRef = capitalCollection.document("Credit")
            
            // Get product and chassis collections
            val productCollection = db.collection("Product")
            val chassisCollection = db.collection("ChassisNumber")
            
            // Determine which capital documents need to be read/updated based on payment methods
            // Use Set to automatically handle uniqueness
            val capitalDocsToUpdate = mutableSetOf<DocumentReference>()
            paymentMethods.forEach { (method, amountStr) ->
                if (amountStr.isNotBlank()) {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        when (method.lowercase()) {
                            "cash" -> capitalDocsToUpdate.add(cashDocRef)
                            "bank" -> capitalDocsToUpdate.add(bankDocRef)
                            "credit" -> capitalDocsToUpdate.add(creditDocRef)
                        }
                    }
                }
            }
            
            // Calculate payment details outside transaction block for fallback use
            val (cashAmount, bankAmount, creditAmount) = extractPaymentAmounts(paymentMethods)
            val paymentMethod = determinePaymentMethod(paymentMethods)
            
            // Run everything in a single transaction
            val (purchaseId, personTransaction, orderNumber) = db.runTransaction { transaction ->
                // ========== PHASE 1: READ ALL DOCUMENTS FIRST ==========
                // Firestore requires all reads to complete before any writes
                // IMPORTANT: Any DocumentReference that will be written must have its document read first
                
                // 1. Read maxOrderNo document
                val maxOrderNoSnapshot = transaction.get(maxOrderNoDocRefLocal)
                val currentMax = getIntFromFirestore(maxOrderNoSnapshot.get("maxOrderNo"))
                val orderNumber = currentMax + 1
                
                // 1b. Read maxTransactionNo document
                val maxTransactionNoSnapshot = transaction.get(maxTransactionNoDocRefLocal)
                var currentTransactionNo = getIntFromFirestore(maxTransactionNoSnapshot.get("maxTransactionNo"))
                
                // Track the purchase transaction number (will be set when owner transaction is created, or middle man, or broker fee)
                var purchaseTransactionNumber: Int? = null
                
                // 2. Read all capital documents that will be updated
                val capitalSnapshots = mutableMapOf<DocumentReference, com.google.firebase.firestore.DocumentSnapshot>()
                capitalDocsToUpdate.forEach { docRef ->
                    capitalSnapshots[docRef] = transaction.get(docRef)
                }
                
                // 3. Read brand document
                val brandSnapshot = transaction.get(brandDocRef)
                
                // 4. Read ownerRef document if provided (required for writing DocumentReference in transaction)
                ownerRef?.let {
                    try {
                        transaction.get(it)
                        Log.d(TAG, "✅ Read ownerRef document in transaction: ${it.path}")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Could not read ownerRef document: ${it.path}, error: ${e.message}")
                    }
                }
                
                // 5. Read brokerOrMiddleManRef document if provided (required for writing DocumentReference in transaction)
                brokerOrMiddleManRef?.let {
                    try {
                        transaction.get(it)
                        Log.d(TAG, "✅ Read brokerOrMiddleManRef document in transaction: ${it.path}")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Could not read brokerOrMiddleManRef document: ${it.path}, error: ${e.message}")
                    }
                }
                
                // ========== PHASE 2: PERFORM ALL WRITES ==========
                // Now that all reads are complete, we can perform writes
                
                // 1. Update maxOrderNo
                transaction.update(maxOrderNoDocRefLocal, "maxOrderNo", orderNumber)
                
                // 2. Create purchase document (new document, no read needed)
                val purchaseDocRef = purchaseCollection.document()
                val purchaseIdValue = purchaseDocRef.id
                
                // Build purchase data map with all fields, including references
                val purchaseData = mutableMapOf<String, Any>(
                    "purchaseId" to purchaseIdValue,
                    "grandTotal" to grandTotal,
                    "gstAmount" to gstAmount,
                    "orderNumber" to orderNumber,
                    "paymentMethods" to paymentMethods,
                    "vehicle" to vehicle,
                    "middleMan" to middleMan,
                    "createdAt" to System.currentTimeMillis()
                )
                
                // Add ownerRef if provided
                ownerRef?.let {
                    purchaseData["ownerRef"] = it
                    Log.d(TAG, "✅ Adding ownerRef to purchase: ${it.path}")
                } ?: Log.d(TAG, "⚠️ ownerRef is null, not adding to purchase")
                
                // Add brokerRef if provided (using brokerOrMiddleManRef)
                brokerOrMiddleManRef?.let {
                    purchaseData["brokerRef"] = it
                    Log.d(TAG, "✅ Adding brokerRef to purchase: ${it.path}")
                } ?: Log.d(TAG, "⚠️ brokerOrMiddleManRef is null, not adding to purchase")
                
                // Note: transactionNumber will be added after transaction creation logic
                // We'll update purchaseData before transaction.set()
                
                // 3. Update capital documents (using snapshots read in phase 1)
                paymentMethods.forEach { (method, amountStr) ->
                    if (amountStr.isNotBlank()) {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val capitalType = when (method.lowercase()) {
                                "cash" -> "Cash"
                                "bank" -> "Bank"
                                "credit" -> "Credit"
                                else -> null
                            }
                            
                            if (capitalType != null) {
                                val capitalDocRef = when (capitalType) {
                                    "Cash" -> cashDocRef
                                    "Bank" -> bankDocRef
                                    "Credit" -> creditDocRef
                                    else -> null
                                }
                                
                                capitalDocRef?.let { docRef ->
                                    val capitalSnapshot = capitalSnapshots[docRef]
                                    if (capitalSnapshot != null) {
                                        val existingTransactions = capitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                                        val currentBalance = (capitalSnapshot.get("balance") as? Long)?.toDouble() 
                                            ?: (capitalSnapshot.get("balance") as? Double) ?: 0.0
                                        
                                        // Subtract amount from balance (purchase reduces capital)
                                        val newBalance = currentBalance - amount
                                        
                                        val transactionData = hashMapOf<String, Any>(
                                            "transactionDate" to System.currentTimeMillis(),
                                            "createdAt" to System.currentTimeMillis(),
                                            "orderNumber" to orderNumber,
                                            "amount" to amount,
                                            "reference" to purchaseDocRef
                                        )
                                        
                                        val updatedTransactions = existingTransactions.toMutableList()
                                        updatedTransactions.add(transactionData)
                                        
                                        transaction.set(
                                            docRef,
                                            mapOf(
                                                "transactions" to updatedTransactions,
                                                "balance" to newBalance
                                            ),
                                            SetOptions.merge()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 4. Update brand's vehicle array (using snapshot read in phase 1)
                val currentVehicles = brandSnapshot.get("vehicle") as? List<Map<String, Any>> ?: emptyList()
                
                val existingVehicleIndex = currentVehicles.indexOfFirst { it["productId"] == product.productId }
                val updatedVehicles = currentVehicles.toMutableList()
                
                if (existingVehicleIndex != -1) {
                    val existingVehicle = updatedVehicles[existingVehicleIndex].toMutableMap()
                    val currentQuantity = (existingVehicle["quantity"] as? Long)?.toInt() ?: 1
                    existingVehicle["quantity"] = currentQuantity + 1
                    updatedVehicles[existingVehicleIndex] = existingVehicle
                } else {
                    val newVehicle = hashMapOf(
                        "imageUrl" to (imageUrls.firstOrNull() ?: "https://example.com/default_image.png"),
                        "productId" to product.productId,
                        "type" to product.type,
                        "quantity" to 1
                    )
                    updatedVehicles.add(newVehicle)
                }
                transaction.update(brandDocRef, "vehicle", updatedVehicles)
                
                // 5. Create chassis document (new document, no read needed)
                val chassisRef = chassisCollection.document()
                transaction.set(
                    chassisRef,
                    mapOf(
                        "chassisNumber" to product.chassisNumber,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                
                // 6. Create product document (new document, no read needed)
                val newProductRef = productCollection.document()
                val productDetails = hashMapOf<String, Any>(
                    "brandId" to brandId,
                    "productId" to product.productId,
                    "chassisNumber" to product.chassisNumber,
                    "chassisReference" to chassisRef,
                    "colour" to product.colour,
                    "condition" to product.condition,
                    "images" to imageUrls,
                    "kms" to product.kms,
                    "lastService" to product.lastService,
                    "previousOwners" to product.previousOwners,
                    "price" to product.price,
                    "sellingPrice" to product.sellingPrice,
                    "type" to product.type,
                    "year" to product.year,
                    "noc" to nocUrls,
                    "rc" to rcUrls,
                    "insurance" to insuranceUrls,
                    "vehicleOtherDoc" to vehicleOtherDocUrls,
                    "brokerOrMiddleMan" to product.brokerOrMiddleMan,
                    "owner" to product.owner
                )
                
                // Add brandRef if available
                brandNameRef?.let {
                    productDetails["brandRef"] = it
                }
                
                // Add references if they exist
                brokerOrMiddleManRef?.let {
                    productDetails["brokerOrMiddleManRef"] = it
                }
                
                ownerRef?.let {
                    productDetails["ownerRef"] = it
                }
                
                transaction.set(newProductRef, productDetails)
                
                // 7. Create person transaction records (within the same transaction)
                val transactionCollection = db.collection("Transactions")
                val nowTimestamp = com.google.firebase.Timestamp.now()
                // Note: cashAmount, bankAmount, creditAmount, paymentMethod are already calculated outside
                
                // Determine purchase type from brokerOrMiddleManRef if not provided
                val determinedPurchaseType = purchaseType ?: brokerOrMiddleManRef?.let { ref ->
                    when {
                        ref.path.contains("/Broker/") -> "Broker"
                        ref.path.contains("/Customer/") -> "Middle Man"
                        else -> null
                    }
                }
                
                // Track the primary transaction (Owner > Middle Man > Broker Fee)
                var createdPersonTransaction: PersonTransaction? = null
                
                // Create BROKER_FEE transaction (if broker involved and broker fee exists)
                if (determinedPurchaseType == "Broker" && brokerFee > 0 && brokerOrMiddleManRef != null) {
                    currentTransactionNo++
                    purchaseTransactionNumber = currentTransactionNo  // Set purchase transaction number
                    val brokerFeePaymentMethod = brokerFeePaymentMethods?.let { determinePaymentMethod(it) } ?: paymentMethod
                    val (brokerFeeCash, brokerFeeBank, brokerFeeCredit) = brokerFeePaymentMethods?.let { extractPaymentAmounts(it) } 
                        ?: Triple(0.0, 0.0, 0.0)
                    
                    val brokerFeeTransactionRef = transactionCollection.document()
                    val brokerFeeTransactionData = hashMapOf<String, Any>(
                        "transactionId" to brokerFeeTransactionRef.id,
                        "type" to TransactionType.BROKER_FEE,
                        "personType" to PersonType.BROKER,
                        "personRef" to brokerOrMiddleManRef,
                        "personName" to middleMan,
                        "relatedRef" to purchaseDocRef,
                        "amount" to brokerFee,
                        "paymentMethod" to brokerFeePaymentMethod,
                        "cashAmount" to brokerFeeCash,
                        "bankAmount" to brokerFeeBank,
                        "creditAmount" to brokerFeeCredit,
                        "date" to purchaseDate,
                        "orderNumber" to orderNumber,
                        "transactionNumber" to currentTransactionNo,
                        "description" to "Broker fee - Order #$orderNumber",
                        "status" to "COMPLETED",
                        "createdAt" to nowTimestamp,
                        "note" to note
                    )
                    transaction.set(brokerFeeTransactionRef, brokerFeeTransactionData)
                    
                    // Store as primary if no owner/middle man (lowest priority)
                    if (createdPersonTransaction == null) {
                        createdPersonTransaction = PersonTransaction(
                            transactionId = brokerFeeTransactionRef.id,
                            type = TransactionType.BROKER_FEE,
                            personType = PersonType.BROKER,
                            personRef = brokerOrMiddleManRef,
                            personName = middleMan,
                            relatedRef = purchaseDocRef,
                            amount = brokerFee,
                            paymentMethod = brokerFeePaymentMethod,
                            cashAmount = brokerFeeCash,
                            bankAmount = brokerFeeBank,
                            creditAmount = brokerFeeCredit,
                            date = purchaseDate,
                            orderNumber = orderNumber,
                            transactionNumber = currentTransactionNo,
                            description = "Broker fee - Order #$orderNumber",
                            status = "COMPLETED",
                            createdAt = nowTimestamp.toDate().time,
                            note = note
                        )
                    }
                }
                
                // Create PURCHASE transaction for Middle Man (if middle man involved)
                if (determinedPurchaseType == "Middle Man" && brokerOrMiddleManRef != null) {
                    currentTransactionNo++
                    purchaseTransactionNumber = currentTransactionNo  // Set purchase transaction number if not already set
                    val middleManTransactionRef = transactionCollection.document()
                    val middleManTransactionData = hashMapOf<String, Any>(
                        "transactionId" to middleManTransactionRef.id,
                        "type" to TransactionType.PURCHASE,
                        "personType" to PersonType.MIDDLE_MAN,
                        "personRef" to brokerOrMiddleManRef,
                        "personName" to middleMan,
                        "relatedRef" to purchaseDocRef,
                        "amount" to grandTotal,
                        "paymentMethod" to paymentMethod,
                        "cashAmount" to cashAmount,
                        "bankAmount" to bankAmount,
                        "creditAmount" to creditAmount,
                        "date" to purchaseDate,
                        "orderNumber" to orderNumber,
                        "transactionNumber" to currentTransactionNo,
                        "description" to "Vehicle purchase - Order #$orderNumber",
                        "status" to "COMPLETED",
                        "createdAt" to nowTimestamp,
                        "note" to note
                    )
                    transaction.set(middleManTransactionRef, middleManTransactionData)
                    
                    // Store as primary if no owner (medium priority)
                    if (createdPersonTransaction == null) {
                        createdPersonTransaction = PersonTransaction(
                            transactionId = middleManTransactionRef.id,
                            type = TransactionType.PURCHASE,
                            personType = PersonType.MIDDLE_MAN,
                            personRef = brokerOrMiddleManRef,
                            personName = middleMan,
                            relatedRef = purchaseDocRef,
                            amount = grandTotal,
                            paymentMethod = paymentMethod,
                            cashAmount = cashAmount,
                            bankAmount = bankAmount,
                            creditAmount = creditAmount,
                            date = purchaseDate,
                            orderNumber = orderNumber,
                            transactionNumber = currentTransactionNo,
                            description = "Vehicle purchase - Order #$orderNumber",
                            status = "COMPLETED",
                            createdAt = nowTimestamp.toDate().time,
                            note = note
                        )
                    }
                }
                
                // Create PURCHASE transaction for Owner (if owner specified)
                ownerRef?.let { ref ->
                    currentTransactionNo++
                    purchaseTransactionNumber = currentTransactionNo  // Owner transaction takes priority
                    val ownerTransactionRef = transactionCollection.document()
                    val ownerTransactionData = hashMapOf<String, Any>(
                        "transactionId" to ownerTransactionRef.id,
                        "type" to TransactionType.PURCHASE,
                        "personType" to PersonType.CUSTOMER,
                        "personRef" to ref,
                        "personName" to product.owner,
                        "relatedRef" to purchaseDocRef,
                        "amount" to grandTotal,
                        "paymentMethod" to paymentMethod,
                        "cashAmount" to cashAmount,
                        "bankAmount" to bankAmount,
                        "creditAmount" to creditAmount,
                        "date" to purchaseDate,
                        "orderNumber" to orderNumber,
                        "transactionNumber" to currentTransactionNo,
                        "description" to "Vehicle purchase - Order #$orderNumber",
                        "status" to "COMPLETED",
                        "createdAt" to nowTimestamp,
                        "note" to note
                    )
                    transaction.set(ownerTransactionRef, ownerTransactionData)
                    
                    // Owner transaction has highest priority - always use this
                    createdPersonTransaction = PersonTransaction(
                        transactionId = ownerTransactionRef.id,
                        type = TransactionType.PURCHASE,
                        personType = PersonType.CUSTOMER,
                        personRef = ref,
                        personName = product.owner,
                        relatedRef = purchaseDocRef,
                        amount = grandTotal,
                        paymentMethod = paymentMethod,
                        cashAmount = cashAmount,
                        bankAmount = bankAmount,
                        creditAmount = creditAmount,
                        date = purchaseDate,
                        orderNumber = orderNumber,
                        transactionNumber = currentTransactionNo,
                        description = "Vehicle purchase - Order #$orderNumber",
                        status = "COMPLETED",
                        createdAt = nowTimestamp.toDate().time,
                        note = note
                    )
                }
                
                // Add transactionNumber to purchase data (use owner transaction if exists, otherwise middle man, otherwise broker fee)
                purchaseTransactionNumber?.let {
                    purchaseData["transactionNumber"] = it
                } ?: run {
                    // If no transaction was created (shouldn't happen, but handle gracefully)
                    purchaseData["transactionNumber"] = currentTransactionNo
                }
                
                Log.d(TAG, "📝 Purchase data keys before transaction.set: ${purchaseData.keys}")
                transaction.set(purchaseDocRef, purchaseData)
                
                // Update maxTransactionNo with the final value
                transaction.update(maxTransactionNoDocRefLocal, "maxTransactionNo", currentTransactionNo)
                
                // Return purchase ID, transaction, and orderNumber (or null if no transaction was created)
                Triple(purchaseIdValue, createdPersonTransaction, orderNumber)
            }.await()
            
            Log.d(TAG, "✅ Purchase, capital transactions, vehicle, and person transactions created atomically with purchaseId: $purchaseId")
            
            // If no transaction was created (shouldn't happen, but handle gracefully)
            val transaction = personTransaction ?: PersonTransaction(
                transactionId = "",
                type = TransactionType.PURCHASE,
                personType = PersonType.CUSTOMER,
                personRef = ownerRef ?: brokerOrMiddleManRef,
                personName = product.owner.ifBlank { middleMan },
                relatedRef = null,
                amount = grandTotal,
                paymentMethod = paymentMethod,
                cashAmount = cashAmount,
                bankAmount = bankAmount,
                creditAmount = creditAmount,
                date = purchaseDate,
                orderNumber = orderNumber,
                transactionNumber = null,
                description = "Vehicle purchase - Order #$orderNumber",
                status = "COMPLETED",
                createdAt = System.currentTimeMillis(),
                note = note
            )
            
            Result.success(TransactionResult(purchaseId, transaction))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in atomic purchase with vehicle: ${e.message}", e)
            Result.failure(e)
        }
    }
    
}

