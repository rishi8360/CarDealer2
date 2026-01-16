package com.example.cardealer2.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.cardealer2.data.EmiDetails
import com.example.cardealer2.data.VehicleSale
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.TransactionType
import com.example.cardealer2.data.PersonType
import com.example.cardealer2.data.PaymentMethod
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
import java.text.SimpleDateFormat
import java.util.Locale

object SaleRepository {
    @SuppressLint("StaticFieldLeak")
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestoreSettings = settings
    }
    
    private const val TAG = "SaleRepository"
    private val saleCollection = db.collection("VehicleSales")
    private val emiDetailsCollection = db.collection("EmiDetails")
    private val capitalCollection = db.collection("Capital")
    private val maxTransactionNoCollection = db.collection("MaxTransactionNo")
    private val brandCollection = db.collection("Brand")
    
    // StateFlow for sales
    private val _sales = MutableStateFlow<List<VehicleSale>>(emptyList())
    val sales: StateFlow<List<VehicleSale>> = _sales.asStateFlow()
    
    // StateFlow to notify when EmiDetails changes (triggers refresh)
    private val _emiDetailsUpdateTrigger = MutableStateFlow<Long>(0L)
    val emiDetailsUpdateTrigger: StateFlow<Long> = _emiDetailsUpdateTrigger.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var saleListenerRegistration: ListenerRegistration? = null
    private var emiDetailsListenerRegistration: ListenerRegistration? = null
    
    init {
        startListening()
    }
    
    /**
     * Start listening to VehicleSales and EmiDetails collection changes
     */
    private fun startListening() {
        // Remove existing listeners
        saleListenerRegistration?.remove()
        emiDetailsListenerRegistration?.remove()
        
        _isLoading.value = true
        _error.value = null
        
        // Listen to VehicleSales collection
        saleListenerRegistration = saleCollection.addSnapshotListener { snapshot, error ->
            _isLoading.value = false
            
            if (error != null) {
                val errorMsg = error.message ?: "Error loading sales"
                _error.value = errorMsg
                Log.e(TAG, "❌ Error in sale listener: $errorMsg", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                try {
                    // Note: docToVehicleSale is now suspend, but snapshot listener is not suspend
                    // We'll convert synchronously for the listener (emiDetailsRef will be set but not loaded)
                    val sales = snapshot.documents.mapNotNull { doc ->
                        try {
                            val emi = (doc.get("emi") as? Boolean) ?: false
                            val status = (doc.get("status") as? Boolean) ?: false
                            val emiDetailsRef = doc.get("emiDetailsRef") as? DocumentReference
                            val nocHandedOver = (doc.get("nocHandedOver") as? Boolean) ?: false
                            val rcHandedOver = (doc.get("rcHandedOver") as? Boolean) ?: false
                            val insuranceHandedOver = (doc.get("insuranceHandedOver") as? Boolean) ?: false
                            val otherDocsHandedOver = (doc.get("otherDocsHandedOver") as? Boolean) ?: false

                            VehicleSale(
                                saleId = doc.id,
                                customerRef = doc.get("customerRef") as? DocumentReference,
                                vehicleRef = doc.get("vehicleRef") as? DocumentReference,
                                purchaseDate = doc.getTimestampOrLong("purchaseDate"),
                                totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                                emi = emi,
                                status = status,
                                emiDetailsRef = emiDetailsRef,
                                nocHandedOver = nocHandedOver,
                                rcHandedOver = rcHandedOver,
                                insuranceHandedOver = insuranceHandedOver,
                                otherDocsHandedOver = otherDocsHandedOver
                            )
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
        
        // Listen to EmiDetails collection changes
        emiDetailsListenerRegistration = emiDetailsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                val errorMsg = error.message ?: "Error loading EmiDetails"
                Log.e(TAG, "❌ Error in EmiDetails listener: $errorMsg", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                // Check if there are actual document changes
                val hasChanges = snapshot.documentChanges.isNotEmpty()
                
                // Trigger update if there are changes
                // Firestore listeners typically only fire on committed changes
                if (hasChanges) {
                    // Trigger update by emitting current timestamp
                    // This will cause ViewModel to reload EmiDetails for all sales
                    _emiDetailsUpdateTrigger.value = System.currentTimeMillis()
                    Log.d(TAG, "✅ EmiDetails listener updated: ${snapshot.documentChanges.size} EmiDetails documents changed")
                }
            }
        }
    }
    
    /**
     * Convert Firestore document to VehicleSale
     */
    private suspend fun docToVehicleSale(doc: com.google.firebase.firestore.DocumentSnapshot): VehicleSale? {
        return try {
            val emi = (doc.get("emi") as? Boolean) ?: false
            val status = (doc.get("status") as? Boolean) ?: false
            val emiDetailsRef = doc.get("emiDetailsRef") as? DocumentReference
            val nocHandedOver = (doc.get("nocHandedOver") as? Boolean) ?: false
            val rcHandedOver = (doc.get("rcHandedOver") as? Boolean) ?: false
            val insuranceHandedOver = (doc.get("insuranceHandedOver") as? Boolean) ?: false
            val otherDocsHandedOver = (doc.get("otherDocsHandedOver") as? Boolean) ?: false
            
            VehicleSale(
                saleId = doc.id,
                customerRef = doc.get("customerRef") as? DocumentReference,
                vehicleRef = doc.get("vehicleRef") as? DocumentReference,
                purchaseDate = doc.getTimestampOrLong("purchaseDate"),
                totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                emi = emi,
                status = status,
                emiDetailsRef = emiDetailsRef,
                nocHandedOver = nocHandedOver,
                rcHandedOver = rcHandedOver,
                insuranceHandedOver = insuranceHandedOver,
                otherDocsHandedOver = otherDocsHandedOver
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error converting document to VehicleSale: ${e.message}", e)
            null
        }
    }
    
    /**
     * Convert Firestore document to EmiDetails
     */
    private fun docToEmiDetails(doc: com.google.firebase.firestore.DocumentSnapshot): EmiDetails? {
        return try {
            val installmentsCount = (doc.get("installmentsCount") as? Number)?.toInt() ?: 0
            val installmentAmount = (doc.get("installmentAmount") as? Number)?.toDouble() ?: 0.0
            val priceWithInterest = (doc.get("priceWithInterest") as? Number)?.toDouble() 
                ?: (installmentsCount * installmentAmount)
            
            EmiDetails(
                vehicleSaleRef = doc.get("vehicleSaleRef") as? DocumentReference,
                interestRate = (doc.get("interestRate") as? Number)?.toDouble() ?: 0.0,
                frequency = doc.get("frequency") as? String ?: "MONTHLY",
                installmentsCount = installmentsCount,
                installmentAmount = installmentAmount,
                nextDueDate = doc.getTimestampOrLong("nextDueDate"),
                remainingInstallments = (doc.get("remainingInstallments") as? Number)?.toInt() ?: 0,
                lastPaidDate = doc.getTimestampOrLongOrNull("lastPaidDate"),
                paidInstallments = (doc.get("paidInstallments") as? Number)?.toInt() ?: 0,
                priceWithInterest = priceWithInterest,
                customerRef = doc.get("customerRef") as? DocumentReference,
                vehicleRef = doc.get("vehicleRef") as? DocumentReference,
                pendingExtraBalance = (doc.get("pendingExtraBalance") as? Number)?.toDouble() ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error converting document to EmiDetails: ${e.message}", e)
            null
        }
    }
    
    /**
     * Add a new vehicle sale atomically
     * Creates sale, updates capital, marks vehicle as sold, and creates EmiDetails in separate collection
     * This ensures data consistency - either everything succeeds or everything fails
     */
    suspend fun addSale(
        customerRef: DocumentReference?,
        vehicleRef: DocumentReference?,
        purchaseType: String,
        totalAmount: Double,
        downPayment: Double = 0.0,
        cashDownPayment: Double = 0.0,
        bankDownPayment: Double = 0.0,
        emiDetails: EmiDetails? = null,
        note: String = "",
        date: String = "",
        nocHandedOver: Boolean = false,
        rcHandedOver: Boolean = false,
        insuranceHandedOver: Boolean = false,
        otherDocsHandedOver: Boolean = false
    ): Result<String> {
        val saleDate = if (date.isBlank()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date())
        } else {
            date
        }
        return try {
            // Get capital document references
            val cashDocRef = capitalCollection.document("Cash")
            val bankDocRef = capitalCollection.document("Bank")
            
            // Get maxTransactionNo document reference
            val maxTransactionNoDocs = maxTransactionNoCollection.limit(1).get().await()
            val maxTransactionNoDocRefLocal = if (maxTransactionNoDocs.isEmpty) {
                val newDocRef = maxTransactionNoCollection.document()
                newDocRef.set(mapOf("maxTransactionNo" to 0)).await()
                newDocRef
            } else {
                maxTransactionNoDocs.documents.first().reference
            }
            
            // Query Brand collection using brandId from product (BEFORE transaction)
            val brandDocRef = vehicleRef?.let { ref ->
                try {
                    val productDoc = ref.get().await() // Read product first
                    if (productDoc.exists()) {
                        val brandId = productDoc.get("brandId") as? String
                        if (brandId != null) {
                            Log.d(TAG, "🔍 Looking for Brand with brandId: $brandId")
                            val brandQuery = brandCollection
                                .whereEqualTo("brandId", brandId)
                                .limit(1)
                                .get()
                                .await()
                            val brandRef = brandQuery.documents.firstOrNull()?.reference
                            if (brandRef != null) {
                                Log.d(TAG, "✅ Found Brand document: ${brandRef.id}")
                            } else {
                                Log.e(TAG, "❌ Brand document not found for brandId: $brandId")
                            }
                            brandRef
                        } else {
                            Log.e(TAG, "❌ Product document has no brandId field")
                            null
                        }
                    } else {
                        Log.e(TAG, "❌ Product document does not exist")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error getting brand doc ref: ${e.message}", e)
                    null
                }
            }
            
            // Determine if we need to update capital
            val needsCapitalUpdate = (purchaseType == "DOWN_PAYMENT" || purchaseType == "FULL_PAYMENT") && downPayment > 0
            val needsEmiCapitalUpdate = purchaseType == "EMI" && downPayment > 0
            val needsCashUpdate = needsEmiCapitalUpdate && cashDownPayment > 0
            val needsBankUpdate = (needsCapitalUpdate || (needsEmiCapitalUpdate && bankDownPayment > 0))
            val isEmi = purchaseType == "EMI"
            
            // Run everything in a single transaction
            val saleId = db.runTransaction { transaction ->
                // ========== PHASE 1: READ ALL DOCUMENTS FIRST ==========
                // Firestore requires all reads to complete before any writes
                
                // 1. Read maxTransactionNo document
                val maxTransactionNoSnapshot = transaction.get(maxTransactionNoDocRefLocal)
                var currentTransactionNo = getIntFromFirestore(maxTransactionNoSnapshot.get("maxTransactionNo"))
                
                // 2. Read capital documents (if down payment exists)
                val cashCapitalSnapshot = if (needsCashUpdate) {
                    transaction.get(cashDocRef)
                } else {
                    null
                }
                
                val bankCapitalSnapshot = if (needsBankUpdate) {
                    transaction.get(bankDocRef)
                } else {
                    null
                }
                
                // For backward compatibility with FULL_PAYMENT/DOWN_PAYMENT
                val capitalSnapshot = if (needsCapitalUpdate) {
                    bankCapitalSnapshot
                } else {
                    null
                }
                
                // 3. Read product document (if vehicle reference exists)
                val productSnapshot = vehicleRef?.let { ref ->
                    transaction.get(ref)
                }
                
                // 4. Read brand document (using brandDocRef found before transaction)
                val brandSnapshot = brandDocRef?.let { ref ->
                    transaction.get(ref)
                }
                
                // 5. Read customer document (if customer reference exists) - for transaction record
                val customerSnapshot = customerRef?.let { ref ->
                    transaction.get(ref)
                }
                
                // ========== PHASE 2: PERFORM ALL WRITES ==========
                // Now that all reads are complete, we can perform writes
                
                // 1. Create sale document (new document, no read needed)
                val saleDocRef = saleCollection.document()
                val saleIdValue = saleDocRef.id
                val nowTimestamp = Timestamp.now()
                
                val saleData = hashMapOf<String, Any>(
                    "saleId" to saleIdValue,
                    "purchaseDate" to nowTimestamp,
                    "totalAmount" to totalAmount,
                    "emi" to isEmi,
                    "status" to false,  // false for pending
                    "nocHandedOver" to nocHandedOver,
                    "rcHandedOver" to rcHandedOver,
                    "insuranceHandedOver" to insuranceHandedOver,
                    "otherDocsHandedOver" to otherDocsHandedOver
                )
                
                customerRef?.let { saleData["customerRef"] = it }
                vehicleRef?.let { saleData["vehicleRef"] = it }
                
                // 2. Create EmiDetails document in separate collection if EMI
                var emiDetailsRef: DocumentReference? = null
                if (isEmi && emiDetails != null) {
                    val emiDetailsDocRef = emiDetailsCollection.document()
                    val priceWithInterest = emiDetails.installmentsCount * emiDetails.installmentAmount
                    
                    val emiData = hashMapOf<String, Any>(
                        "vehicleSaleRef" to saleDocRef,
                        "interestRate" to emiDetails.interestRate,
                        "frequency" to emiDetails.frequency,
                        "installmentsCount" to emiDetails.installmentsCount,
                        "installmentAmount" to emiDetails.installmentAmount,
                        "nextDueDate" to Timestamp(Date(emiDetails.nextDueDate)),
                        "remainingInstallments" to emiDetails.remainingInstallments,
                        "paidInstallments" to emiDetails.paidInstallments,
                        "priceWithInterest" to priceWithInterest,
                        "pendingExtraBalance" to 0.0
                    )
                    
                    customerRef?.let { emiData["customerRef"] = it }
                    vehicleRef?.let { emiData["vehicleRef"] = it }
                    emiDetails.lastPaidDate?.let { emiData["lastPaidDate"] = Timestamp(Date(it)) }
                    
                    transaction.set(emiDetailsDocRef, emiData)
                    emiDetailsRef = emiDetailsDocRef
                    saleData["emiDetailsRef"] = emiDetailsDocRef
                }
                
                transaction.set(saleDocRef, saleData)
                
                // 3. Update capital document (if down payment exists, using snapshot read in phase 1)
                if (needsCapitalUpdate && capitalSnapshot != null) {
                    val existingTransactions = capitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                    val currentBalance = (capitalSnapshot.get("balance") as? Long)?.toDouble() 
                        ?: (capitalSnapshot.get("balance") as? Double) ?: 0.0
                    
                    // Add amount to balance (sale increases capital)
                    val newBalance = currentBalance + downPayment
                    
                    val transactionData = hashMapOf<String, Any>(
                        "transactionDate" to nowTimestamp,
                        "createdAt" to nowTimestamp,
                        "amount" to downPayment,
                        "reference" to saleDocRef
                    )
                    
                    val updatedTransactions = existingTransactions.toMutableList()
                    updatedTransactions.add(transactionData)
                    
                    transaction.set(
                        bankDocRef,
                        mapOf(
                            "transactions" to updatedTransactions,
                            "balance" to newBalance
                        ),
                        SetOptions.merge()
                    )
                }
                
                // 3.5. Update Cash capital for EMI down payment
                if (needsCashUpdate && cashCapitalSnapshot != null) {
                    val existingTransactions = cashCapitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                    val currentBalance = (cashCapitalSnapshot.get("balance") as? Long)?.toDouble() 
                        ?: (cashCapitalSnapshot.get("balance") as? Double) ?: 0.0
                    
                    // Add cash down payment to balance
                    val newBalance = currentBalance + cashDownPayment
                    
                    val transactionData = hashMapOf<String, Any>(
                        "transactionDate" to nowTimestamp,
                        "createdAt" to nowTimestamp,
                        "amount" to cashDownPayment,
                        "reference" to saleDocRef
                    )
                    
                    val updatedTransactions = existingTransactions.toMutableList()
                    updatedTransactions.add(transactionData)
                    
                    transaction.set(
                        cashDocRef,
                        mapOf(
                            "transactions" to updatedTransactions,
                            "balance" to newBalance
                        ),
                        SetOptions.merge()
                    )
                }
                
                // 3.6. Update Bank capital for EMI down payment
                if (needsEmiCapitalUpdate && bankDownPayment > 0 && bankCapitalSnapshot != null) {
                    val existingTransactions = bankCapitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                    val currentBalance = (bankCapitalSnapshot.get("balance") as? Long)?.toDouble() 
                        ?: (bankCapitalSnapshot.get("balance") as? Double) ?: 0.0
                    
                    // Add bank down payment to balance
                    val newBalance = currentBalance + bankDownPayment
                    
                    val transactionData = hashMapOf<String, Any>(
                        "transactionDate" to nowTimestamp,
                        "createdAt" to nowTimestamp,
                        "amount" to bankDownPayment,
                        "reference" to saleDocRef
                    )
                    
                    val updatedTransactions = existingTransactions.toMutableList()
                    updatedTransactions.add(transactionData)
                    
                    transaction.set(
                        bankDocRef,
                        mapOf(
                            "transactions" to updatedTransactions,
                            "balance" to newBalance
                        ),
                        SetOptions.merge()
                    )
                }
                
                // 4. Update product document sold status (using snapshot read in phase 1)
                productSnapshot?.let { snapshot ->
                    if (snapshot.exists()) {
                        transaction.update(snapshot.reference, "sold", true)
                    }
                }
                
                // 4.5. Decrement Brand vehicle quantity (using snapshots read in phase 1)
                productSnapshot?.let { productSnap ->
                    brandSnapshot?.let { brandSnap ->
                        if (productSnap.exists() && brandSnap.exists()) {
                            // Get productId (model name) and type from product document
                            val productId = productSnap.get("productId") as? String
                            val productType = productSnap.get("type") as? String
                            
                            Log.d(TAG, "🔍 Debug - Product ID: $productId, Type: $productType")
                            
                            if (productId != null && productType != null) {
                                val currentVehicles = brandSnap.get("vehicle") as? List<Map<String, Any>> ?: emptyList()
                                Log.d(TAG, "🔍 Debug - Found ${currentVehicles.size} vehicles in brand")
                                
                                // Log all vehicles for debugging
                                currentVehicles.forEachIndexed { index, vehicle ->
                                    Log.d(TAG, "   Vehicle $index: productId=${vehicle["productId"]}, type=${vehicle["type"]}, quantity=${vehicle["quantity"]}")
                                }
                                
                                val vehicleIndex = currentVehicles.indexOfFirst { vehicle ->
                                    val vProductId = vehicle["productId"] as? String
                                    val vType = vehicle["type"] as? String
                                    val matches = vProductId == productId && vType == productType
                                    if (matches) {
                                        Log.d(TAG, "🔍 Debug - Match found! productId: $vProductId, type: $vType")
                                    }
                                    matches
                                }
                                
                                if (vehicleIndex != -1) {
                                    val updatedVehicles = currentVehicles.toMutableList()
                                    val vehicle = updatedVehicles[vehicleIndex].toMutableMap()
                                    val currentQuantity = (vehicle["quantity"] as? Long)?.toInt() ?: 0
                                    val newQuantity = maxOf(0, currentQuantity - 1)
                                    
                                    Log.d(TAG, "🔍 Debug - Current quantity: $currentQuantity, New quantity: $newQuantity")
                                    
                                    vehicle["quantity"] = newQuantity
                                    updatedVehicles[vehicleIndex] = vehicle
                                    transaction.update(brandSnap.reference, "vehicle", updatedVehicles)
                                    Log.d(TAG, "✅ Decremented vehicle quantity for productId: $productId, type: $productType, new quantity: $newQuantity")
                                } else {
                                    Log.e(TAG, "❌ No matching vehicle found in brand. Looking for productId: $productId, type: $productType")
                                    Log.e(TAG, "   Available vehicles:")
                                    currentVehicles.forEachIndexed { index, vehicle ->
                                        Log.e(TAG, "     Vehicle $index: productId=${vehicle["productId"]}, type=${vehicle["type"]}")
                                    }
                                }
                            } else {
                                Log.e(TAG, "❌ Missing productId or type - productId: $productId, type: $productType")
                                // Log all fields in product for debugging
                                Log.d(TAG, "   Product document fields:")
                                productSnap.data?.forEach { (key, value) ->
                                    Log.d(TAG, "     $key: $value")
                                }
                            }
                        } else {
                            Log.e(TAG, "❌ Product or Brand snapshot does not exist - productExists: ${productSnap.exists()}, brandExists: ${brandSnap?.exists()}")
                        }
                    } ?: Log.e(TAG, "❌ Brand snapshot is null - brandDocRef might not be set")
                } ?: Log.e(TAG, "❌ Product snapshot is null")
                
                // 5. Create person transaction record (within the same transaction)
                customerRef?.let { ref ->
                    val transactionCollection = db.collection("Transactions")
                    // Get customer name from snapshot read in phase 1
                    val customerName = customerSnapshot?.get("name") as? String ?: ""
                    
                    // For FULL_PAYMENT and DOWN_PAYMENT
                    val transactionAmount = when (purchaseType) {
                        "FULL_PAYMENT" -> totalAmount
                        "DOWN_PAYMENT" -> downPayment
                        else -> 0.0 // EMI - handled separately below
                    }
                    
                    // Only create transaction if there's an amount (not EMI-only)
                    if (transactionAmount > 0 && purchaseType != "EMI") {
                        currentTransactionNo++
                        val saleTransactionRef = transactionCollection.document()
                        
                        val saleTransactionData = hashMapOf<String, Any>(
                            "transactionId" to saleTransactionRef.id,
                            "type" to TransactionType.SALE,
                            "personType" to PersonType.CUSTOMER,
                            "personRef" to ref,
                            "personName" to customerName,
                            "relatedRef" to saleDocRef,
                            "amount" to transactionAmount,
                            "paymentMethod" to PaymentMethod.BANK, // Down payment goes to Bank
                            "cashAmount" to 0.0,
                            "bankAmount" to transactionAmount,
                            "creditAmount" to 0.0,
                            "date" to saleDate,
                            "orderNumber" to 0,
                            "transactionNumber" to currentTransactionNo,
                            "description" to when (purchaseType) {
                                "FULL_PAYMENT" -> "Vehicle sale - Full payment"
                                "DOWN_PAYMENT" -> "Vehicle sale - Down payment"
                                else -> "Vehicle sale"
                            },
                            "status" to "COMPLETED",
                            "createdAt" to nowTimestamp,
                            "note" to note
                        )
                        transaction.set(saleTransactionRef, saleTransactionData)
                    }
                    
                    // For EMI: Create separate transactions for cash and/or bank down payment
                    if (isEmi && downPayment > 0) {
                        // Create Cash transaction if cash down payment exists
                        if (cashDownPayment > 0) {
                            currentTransactionNo++
                            val cashTransactionRef = transactionCollection.document()
                            
                            val cashTransactionData = hashMapOf<String, Any>(
                                "transactionId" to cashTransactionRef.id,
                                "type" to TransactionType.SALE,
                                "personType" to PersonType.CUSTOMER,
                                "personRef" to ref,
                                "personName" to customerName,
                                "relatedRef" to saleDocRef,
                                "amount" to cashDownPayment,
                                "paymentMethod" to PaymentMethod.CASH,
                                "cashAmount" to cashDownPayment,
                                "bankAmount" to 0.0,
                                "creditAmount" to 0.0,
                                "date" to saleDate,
                                "orderNumber" to 0,
                                "transactionNumber" to currentTransactionNo,
                                "description" to "Vehicle sale - EMI down payment (Cash)",
                                "status" to "COMPLETED",
                                "createdAt" to nowTimestamp,
                                "note" to note
                            )
                            transaction.set(cashTransactionRef, cashTransactionData)
                        }
                        
                        // Create Bank transaction if bank down payment exists
                        if (bankDownPayment > 0) {
                            currentTransactionNo++
                            val bankTransactionRef = transactionCollection.document()
                            
                            val bankTransactionData = hashMapOf<String, Any>(
                                "transactionId" to bankTransactionRef.id,
                                "type" to TransactionType.SALE,
                                "personType" to PersonType.CUSTOMER,
                                "personRef" to ref,
                                "personName" to customerName,
                                "relatedRef" to saleDocRef,
                                "amount" to bankDownPayment,
                                "paymentMethod" to PaymentMethod.BANK,
                                "cashAmount" to 0.0,
                                "bankAmount" to bankDownPayment,
                                "creditAmount" to 0.0,
                                "date" to saleDate,
                                "orderNumber" to 0,
                                "transactionNumber" to currentTransactionNo,
                                "description" to "Vehicle sale - EMI down payment (Bank)",
                                "status" to "COMPLETED",
                                "createdAt" to nowTimestamp,
                                "note" to note
                            )
                            transaction.set(bankTransactionRef, bankTransactionData)
                        }
                    }
                }
                
                // Update maxTransactionNo with the final value
                transaction.update(maxTransactionNoDocRefLocal, "maxTransactionNo", currentTransactionNo)
                
                // Return sale ID
                saleIdValue
            }.await()
            
            Log.d(TAG, "✅ Sale created atomically with ID: $saleId")
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
                val emi = (doc.get("emi") as? Boolean) ?: false
                val status = (doc.get("status") as? Boolean) ?: false
                val emiDetailsRef = doc.get("emiDetailsRef") as? DocumentReference
                val nocHandedOver = (doc.get("nocHandedOver") as? Boolean) ?: false
                val rcHandedOver = (doc.get("rcHandedOver") as? Boolean) ?: false
                val insuranceHandedOver = (doc.get("insuranceHandedOver") as? Boolean) ?: false
                val otherDocsHandedOver = (doc.get("otherDocsHandedOver") as? Boolean) ?: false

                VehicleSale(
                    saleId = doc.id,
                    customerRef = doc.get("customerRef") as? DocumentReference,
                    vehicleRef = doc.get("vehicleRef") as? DocumentReference,
                    purchaseDate = doc.getTimestampOrLong("purchaseDate"),
                    totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                    emi = emi,
                    status = status,
                    emiDetailsRef = emiDetailsRef,
                    nocHandedOver = nocHandedOver,
                    rcHandedOver = rcHandedOver,
                    insuranceHandedOver = insuranceHandedOver,
                    otherDocsHandedOver = otherDocsHandedOver
                )
            }
            
            Result.success(sales)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting sales by customer: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get EmiDetails by reference
     */
    suspend fun getEmiDetailsByRef(emiDetailsRef: DocumentReference): Result<EmiDetails?> {
        return try {
            val doc = emiDetailsRef.get().await()
            if (doc.exists()) {
                Result.success(docToEmiDetails(doc))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting EmiDetails: ${e.message}", e)
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
            
            // Get all pending sales with EMI
            val querySnapshot = saleCollection
                .whereEqualTo("status", false)
                .whereEqualTo("emi", true)
                .get()
                .await()
            
            val sales = querySnapshot.documents.mapNotNull { doc ->
                val emi = (doc.get("emi") as? Boolean) ?: false
                val status = (doc.get("status") as? Boolean) ?: false
                val emiDetailsRef = doc.get("emiDetailsRef") as? DocumentReference
                val nocHandedOver = (doc.get("nocHandedOver") as? Boolean) ?: false
                val rcHandedOver = (doc.get("rcHandedOver") as? Boolean) ?: false
                val insuranceHandedOver = (doc.get("insuranceHandedOver") as? Boolean) ?: false
                val otherDocsHandedOver = (doc.get("otherDocsHandedOver") as? Boolean) ?: false

                VehicleSale(
                    saleId = doc.id,
                    customerRef = doc.get("customerRef") as? DocumentReference,
                    vehicleRef = doc.get("vehicleRef") as? DocumentReference,
                    purchaseDate = doc.getTimestampOrLong("purchaseDate"),
                    totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                    emi = emi,
                    status = status,
                    emiDetailsRef = emiDetailsRef,
                    nocHandedOver = nocHandedOver,
                    rcHandedOver = rcHandedOver,
                    insuranceHandedOver = insuranceHandedOver,
                    otherDocsHandedOver = otherDocsHandedOver
                )
            }.filter { sale ->
                sale.emiDetailsRef?.let { ref ->
                    val emiResult = getEmiDetailsByRef(ref).getOrNull()
                    emiResult?.let { emi ->
                    emi.nextDueDate >= today && emi.nextDueDate < tomorrow
                    } ?: false
                } ?: false
            }
            
            Result.success(sales)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting sales due today: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all active sales (pending sales)
     */
    suspend fun getAllActiveSales(): Result<List<VehicleSale>> {
        return try {
            val querySnapshot = saleCollection
                .whereEqualTo("status", false)
                .get()
                .await()
            
            val sales = querySnapshot.documents.mapNotNull { doc ->
                val emi = (doc.get("emi") as? Boolean) ?: false
                val status = (doc.get("status") as? Boolean) ?: false
                val emiDetailsRef = doc.get("emiDetailsRef") as? DocumentReference
                val nocHandedOver = (doc.get("nocHandedOver") as? Boolean) ?: false
                val rcHandedOver = (doc.get("rcHandedOver") as? Boolean) ?: false
                val insuranceHandedOver = (doc.get("insuranceHandedOver") as? Boolean) ?: false
                val otherDocsHandedOver = (doc.get("otherDocsHandedOver") as? Boolean) ?: false

                VehicleSale(
                    saleId = doc.id,
                    customerRef = doc.get("customerRef") as? DocumentReference,
                    vehicleRef = doc.get("vehicleRef") as? DocumentReference,
                    purchaseDate = doc.getTimestampOrLong("purchaseDate"),
                    totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                    emi = emi,
                    status = status,
                    emiDetailsRef = emiDetailsRef,
                    nocHandedOver = nocHandedOver,
                    rcHandedOver = rcHandedOver,
                    insuranceHandedOver = insuranceHandedOver,
                    otherDocsHandedOver = otherDocsHandedOver
                )
            }
            
            Result.success(sales)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting active sales: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Record an EMI payment
     * Updates EmiDetails, VehicleSale status, Customer amount, and capital in a single transaction
     * @param sale VehicleSale object (for easy access to sale data)
     * @param emiDetails EmiDetails object (for easy access to EMI data)
     * @param cashAmount Cash payment amount
     * @param bankAmount Bank payment amount
     * @param paymentDate Payment date timestamp (defaults to current time)
     */
    suspend fun recordEmiPayment(
        sale: VehicleSale,
        emiDetails: EmiDetails,
        cashAmount: Double,
        bankAmount: Double,
        paymentDate: Long = System.currentTimeMillis(),
        note: String = "",
        date: String = ""
    ): Result<Unit> {
        val paymentDateString = if (date.isBlank()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date(paymentDate))
        } else {
            date
        }
        return try {
            // Step 1: Calculate total payment
            val totalPayment = cashAmount + bankAmount
            if (totalPayment <= 0.0) {
                return Result.failure(Exception("Payment amount must be greater than zero"))
            }

            // Validate references
            if (sale.saleId.isBlank()) {
                return Result.failure(Exception("Invalid sale: saleId is blank"))
            }
            val emiDetailsRef = sale.emiDetailsRef
                ?: return Result.failure(Exception("Invalid sale: emiDetailsRef is null"))
            val customerRef = sale.customerRef
                ?: return Result.failure(Exception("Invalid sale: customerRef is null"))

            val saleDocRef = saleCollection.document(sale.saleId)
            
            // Get maxTransactionNo document reference
            val maxTransactionNoDocs = maxTransactionNoCollection.limit(1).get().await()
            val maxTransactionNoDocRefLocal = if (maxTransactionNoDocs.isEmpty) {
                val newDocRef = maxTransactionNoCollection.document()
                newDocRef.set(mapOf("maxTransactionNo" to 0)).await()
                newDocRef
            } else {
                maxTransactionNoDocs.documents.first().reference
            }
            
            // Run everything in a single transaction
            db.runTransaction { transaction ->
                // ========== PHASE 1: READ ALL DOCUMENTS FIRST ==========
                
                // 1. Read maxTransactionNo document
                val maxTransactionNoSnapshot = transaction.get(maxTransactionNoDocRefLocal)
                var currentTransactionNo = getIntFromFirestore(maxTransactionNoSnapshot.get("maxTransactionNo"))
                
                // 2. Read EmiDetails document
                val emiDetailsSnapshot = transaction.get(emiDetailsRef)
                if (!emiDetailsSnapshot.exists()) {
                    throw Exception("EMI details not found")
                }
                
                // 3. Read Customer document
                val customerSnapshot = transaction.get(customerRef)
                if (!customerSnapshot.exists()) {
                    throw Exception("Customer not found")
                }
                
                // 3. Read capital documents (if needed)
                val cashCapitalRef = if (cashAmount > 0.0) {
                    capitalCollection.document("Cash")
                } else null
                val bankCapitalRef = if (bankAmount > 0.0) {
                    capitalCollection.document("Bank")
                } else null
                
                val cashCapitalSnapshot = cashCapitalRef?.let { transaction.get(it) }
                val bankCapitalSnapshot = bankCapitalRef?.let { transaction.get(it) }
                
                // ========== PHASE 2: PERFORM ALL WRITES ==========
                
                // Get current values from EmiDetails (read from database snapshot, not parameter)
                val currentPaidInstallments = (emiDetailsSnapshot.get("paidInstallments") as? Number)?.toInt() ?: 0
                val currentRemainingInstallments = (emiDetailsSnapshot.get("remainingInstallments") as? Number)?.toInt() ?: 0
                val currentPendingExtraBalance = (emiDetailsSnapshot.get("pendingExtraBalance") as? Number)?.toDouble() ?: 0.0
                val currentNextDueDate = emiDetailsSnapshot.getTimestampOrLong("nextDueDate")
                val currentLastPaidDate = emiDetailsSnapshot.getTimestampOrLongOrNull("lastPaidDate")
                val frequency = emiDetailsSnapshot.get("frequency") as? String ?: "MONTHLY"
                val installmentAmount = (emiDetailsSnapshot.get("installmentAmount") as? Number)?.toDouble() ?: 0.0
                val installmentsCount = (emiDetailsSnapshot.get("installmentsCount") as? Number)?.toInt() ?: 0
                
                // Get current customer amount
                val currentCustomerAmount = (customerSnapshot.get("amount") as? Number)?.toInt() ?: 0
                
                // Step 2: Calculate total available amount (pendingExtraBalance + current payment)
                val totalAvailable = currentPendingExtraBalance + totalPayment
                var newPendingExtraBalance = 0.0
                var newPaidInstallments = currentPaidInstallments
                var newRemainingInstallments = currentRemainingInstallments
                var newNextDueDate = currentNextDueDate
                var newLastPaidDate: Long? = currentLastPaidDate  // Initialize with current value from database
                var customerAmountChange = 0.0
                
                if (totalAvailable < installmentAmount) {
                    // Total available (pending balance + payment) is less than required
                    val remainingAmount = installmentAmount - totalAvailable
                    
                    // All available amount goes to pendingExtraBalance (it's still not enough)
                    newPendingExtraBalance = totalAvailable
                    
                    // Add remaining amount to customer.amount (customer owes more)
                    customerAmountChange = remainingAmount
                    
                    // Do NOT increment paidInstallments or update dates
                    // Keep lastPaidDate unchanged (don't update it in the map)
                } else {
                    // Total available (pending balance + payment) is equal to or more than required
                    val excessAmount = totalAvailable - installmentAmount
                    
                    // Subtract excess from customer.amount (customer owes less)
                    customerAmountChange = -excessAmount
                    
                    // Step 4: Increment paidInstallments and decrement remainingInstallments
                    newPaidInstallments += 1
                    newRemainingInstallments -= 1
                    
                    // Step 3: Shift nextDueDate and lastPaidDate (payment was successful)
                    newLastPaidDate = paymentDate
                    if (newRemainingInstallments > 0) {
                        // Calculate next due date based on frequency
                        newNextDueDate = calculateNextDueDate(currentNextDueDate, frequency)
                    }
                    
                    // If there's excess, add it to pendingExtraBalance for future installments
                    newPendingExtraBalance = excessAmount
                }
                
                // Update EmiDetails document
                val emiUpdateMap = hashMapOf<String, Any>(
                    "paidInstallments" to newPaidInstallments,
                    "remainingInstallments" to newRemainingInstallments,
                    "pendingExtraBalance" to newPendingExtraBalance,
                    "nextDueDate" to Timestamp(Date(newNextDueDate))
                )
                
                // Update lastPaidDate only if payment was sufficient (installment was completed)
                // When payment is insufficient, newLastPaidDate remains as currentLastPaidDate,
                // but we don't want to update it in the database (keep existing value)
                if (totalAvailable >= installmentAmount && newLastPaidDate != null) {
                    emiUpdateMap["lastPaidDate"] = Timestamp(Date(newLastPaidDate))
                }
                
                transaction.update(emiDetailsRef, emiUpdateMap)
                
                // Step 5: Update VehicleSale status if remainingInstallments is 0
                if (newRemainingInstallments == 0) {
                    transaction.update(saleDocRef, "status", true)
                }
                
                // Update Customer amount
                val newCustomerAmount = currentCustomerAmount + customerAmountChange.toInt()
                transaction.update(customerRef, "amount", newCustomerAmount)
                
                // Update capital documents
                if (cashAmount > 0.0 && cashCapitalSnapshot != null) {
                    val existingTransactions = cashCapitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                    val currentBalance = (cashCapitalSnapshot.get("balance") as? Long)?.toDouble() 
                        ?: (cashCapitalSnapshot.get("balance") as? Double) ?: 0.0
                    val newBalance = currentBalance + cashAmount
                    
                    val transactionData = hashMapOf<String, Any>(
                        "transactionDate" to Timestamp(Date(paymentDate)),
                        "createdAt" to Timestamp.now(),
                        "amount" to cashAmount,
                        "reference" to saleDocRef
                    )
                    
                    val updatedTransactions = existingTransactions.toMutableList()
                    updatedTransactions.add(transactionData)
                    
                    transaction.set(
                        cashCapitalRef!!,
                        mapOf(
                            "transactions" to updatedTransactions,
                            "balance" to newBalance
                        ),
                        SetOptions.merge()
                    )
                }
                
                if (bankAmount > 0.0 && bankCapitalSnapshot != null) {
                    val existingTransactions = bankCapitalSnapshot.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                    val currentBalance = (bankCapitalSnapshot.get("balance") as? Long)?.toDouble() 
                        ?: (bankCapitalSnapshot.get("balance") as? Double) ?: 0.0
                    val newBalance = currentBalance + bankAmount
                    
                    val transactionData = hashMapOf<String, Any>(
                        "transactionDate" to Timestamp(Date(paymentDate)),
                        "createdAt" to Timestamp.now(),
                        "amount" to bankAmount,
                        "reference" to saleDocRef
                    )
                    
                    val updatedTransactions = existingTransactions.toMutableList()
                    updatedTransactions.add(transactionData)
                    
                    transaction.set(
                        bankCapitalRef!!,
                        mapOf(
                            "transactions" to updatedTransactions,
                            "balance" to newBalance
                        ),
                        SetOptions.merge()
                    )
                }
                
                // Create EMI payment transaction record
                currentTransactionNo++
                val transactionCollection = db.collection("Transactions")
                val emiTransactionRef = transactionCollection.document()
                val customerName = customerSnapshot.get("name") as? String ?: ""
                
                val paymentMethod = when {
                    cashAmount > 0.0 && bankAmount > 0.0 -> PaymentMethod.MIXED
                    cashAmount > 0.0 -> PaymentMethod.CASH
                    bankAmount > 0.0 -> PaymentMethod.BANK
                    else -> PaymentMethod.CASH
                }
                
                val emiTransactionData = hashMapOf<String, Any>(
                    "transactionId" to emiTransactionRef.id,
                    "type" to TransactionType.EMI_PAYMENT,
                    "personType" to PersonType.CUSTOMER,
                    "personRef" to customerRef,
                    "personName" to customerName,
                    "relatedRef" to saleDocRef,
                    "amount" to totalPayment,
                    "paymentMethod" to paymentMethod,
                    "cashAmount" to cashAmount,
                    "bankAmount" to bankAmount,
                    "creditAmount" to 0.0,
                    "date" to paymentDateString,
                    "orderNumber" to 0,
                    "transactionNumber" to currentTransactionNo,
                    "description" to "EMI Payment - Installment ${newPaidInstallments}/${installmentsCount}",
                    "status" to "COMPLETED",
                    "createdAt" to Timestamp.now(),
                    "note" to note
                )
                transaction.set(emiTransactionRef, emiTransactionData)
                
                // Update maxTransactionNo with the final value
                transaction.update(maxTransactionNoDocRefLocal, "maxTransactionNo", currentTransactionNo)
            }.await()
            
            Log.d(TAG, "✅ EMI payment recorded for sale: ${sale.saleId}")
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
            "SEMI_ANNUALLY" -> calendar.add(Calendar.MONTH, 6)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        saleListenerRegistration?.remove()
        saleListenerRegistration = null
        emiDetailsListenerRegistration?.remove()
        emiDetailsListenerRegistration = null
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
    
    private fun com.google.firebase.firestore.DocumentSnapshot.getTimestampOrLongOrNull(field: String): Long? {
        val timestamp = getTimestamp(field)
        if (timestamp != null) return timestamp.toDate().time
        val number = get(field) as? Number
        return number?.toLong()
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

