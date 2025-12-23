package com.example.cardealer2.repository

import android.util.Log
import com.example.cardealer2.data.Purchase
import com.example.cardealer2.data.CapitalTransaction
import com.example.cardealer2.data.MaxOrderNo
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
    
    // 🔹 StateFlow exposed to ViewModels - automatically updates when Firestore changes
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    // 🔹 MaxOrderNo StateFlow - listens to the first document in MaxOrderNo collection
    private val _maxOrderNo = MutableStateFlow<Int>(0)
    val maxOrderNo: StateFlow<Int> = _maxOrderNo.asStateFlow()

    // 🔹 Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 🔹 Keep references to listeners
    private var purchaseListenerRegistration: ListenerRegistration? = null
    private var maxOrderNoListenerRegistration: ListenerRegistration? = null
    private var maxOrderNoDocRef: DocumentReference? = null

    // 🔹 Coroutine scope for repository operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 🔹 Initialize listeners when repository is first accessed
    init {
        startListening()
        repositoryScope.launch {
            initializeMaxOrderNoListener()
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
     * Stop listening (useful for cleanup)
     */
    fun stopListening() {
        purchaseListenerRegistration?.remove()
        maxOrderNoListenerRegistration?.remove()
        purchaseListenerRegistration = null
        maxOrderNoListenerRegistration = null
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
        middleMan: String
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
        middleMan: String
    ): Result<String> {
        return try {
            // Add purchase
            val purchaseResult = addPurchase(
                grandTotal = grandTotal,
                gstAmount = gstAmount,
                paymentMethods = paymentMethods,
                vehicle = vehicle,
                middleMan = middleMan
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
     * @param brandDocRef Brand document reference
     * @param brandNameRef BrandNames document reference (optional)
     * @param brokerOrMiddleManRef Broker/Middle Man document reference (optional)
     * @param ownerRef Owner document reference (optional)
     * @return Result with purchase ID on success
     */
    suspend fun addPurchaseWithVehicleAtomic(
        grandTotal: Double,
        gstAmount: Double,
        paymentMethods: Map<String, String>,
        vehicle: Map<String, String>,
        middleMan: String,
        brandId: String,
        product: com.example.cardealer2.data.Product,
        imageUrls: List<String>,
        nocUrls: List<String>,
        rcUrls: List<String>,
        insuranceUrls: List<String>,
        brandDocRef: com.google.firebase.firestore.DocumentReference,
        brandNameRef: com.google.firebase.firestore.DocumentReference?,
        brokerOrMiddleManRef: com.google.firebase.firestore.DocumentReference?,
        ownerRef: com.google.firebase.firestore.DocumentReference?
    ): Result<String> {
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
            
            // Run everything in a single transaction
            val purchaseId = db.runTransaction { transaction ->
                // ========== PHASE 1: READ ALL DOCUMENTS FIRST ==========
                // Firestore requires all reads to complete before any writes
                
                // 1. Read maxOrderNo document
                val maxOrderNoSnapshot = transaction.get(maxOrderNoDocRefLocal)
                val currentMax = getIntFromFirestore(maxOrderNoSnapshot.get("maxOrderNo"))
                val orderNumber = currentMax + 1
                
                // 2. Read all capital documents that will be updated
                val capitalSnapshots = mutableMapOf<DocumentReference, com.google.firebase.firestore.DocumentSnapshot>()
                capitalDocsToUpdate.forEach { docRef ->
                    capitalSnapshots[docRef] = transaction.get(docRef)
                }
                
                // 3. Read brand document
                val brandSnapshot = transaction.get(brandDocRef)
                
                // ========== PHASE 2: PERFORM ALL WRITES ==========
                // Now that all reads are complete, we can perform writes
                
                // 1. Update maxOrderNo
                transaction.update(maxOrderNoDocRefLocal, "maxOrderNo", orderNumber)
                
                // 2. Create purchase document (new document, no read needed)
                val purchaseDocRef = purchaseCollection.document()
                val purchaseIdValue = purchaseDocRef.id
                val purchaseData = hashMapOf<String, Any>(
                    "purchaseId" to purchaseIdValue,
                    "grandTotal" to grandTotal,
                    "gstAmount" to gstAmount,
                    "orderNumber" to orderNumber,
                    "paymentMethods" to paymentMethods,
                    "vehicle" to vehicle,
                    "middleMan" to middleMan,
                    "createdAt" to System.currentTimeMillis()
                )
                transaction.set(purchaseDocRef, purchaseData)
                
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
                    "type" to product.type,
                    "year" to product.year,
                    "noc" to nocUrls,
                    "rc" to rcUrls,
                    "insurance" to insuranceUrls,
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
                
                // Return purchase ID
                purchaseIdValue
            }.await()
            
            Log.d(TAG, "✅ Purchase, capital transactions, and vehicle created atomically with purchaseId: $purchaseId")
            Result.success(purchaseId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in atomic purchase with vehicle: ${e.message}", e)
            Result.failure(e)
        }
    }
    
}

