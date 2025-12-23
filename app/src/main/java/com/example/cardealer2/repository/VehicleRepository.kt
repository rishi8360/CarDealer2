package com.example.cardealer2.repository

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.cardealer2.data.Brand
import com.example.cardealer2.data.Product
import com.example.cardealer2.data.VehicleSummary
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Broker
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object VehicleRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Configure Firestore settings for better reliability on real devices
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Unlimited cache
            .build()
        firestoreSettings = settings
    }
    
    private const val TAG = "VehicleRepository"

    private val brandCollection = db.collection("Brand")
    private val productCollection = db.collection("Product")
    private val brandNamesCollection = db.collection("BrandNames")
    val storage = Firebase.storage
    val storageRef = storage.reference

    // 🔹 StateFlow exposed to ViewModels - automatically updates when Firestore changes
    private val _brands = MutableStateFlow<List<Brand>>(emptyList())
    val brands: StateFlow<List<Brand>> = _brands.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    // 🔹 Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 🔹 Keep references to listeners
    private var brandListenerRegistration: ListenerRegistration? = null
    
    // 🔹 Active product listeners - key is query identifier (brandId, brandId+productId, etc.)
    private val activeProductListeners = mutableMapOf<String, ListenerRegistration>()
    private val listenerProducts = mutableMapOf<String, List<Product>>() // Store products per listener
    
    // 🔹 Coroutine scope for repository operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 🔹 Initialize listeners when repository is first accessed
    init {
        startBrandListening()
    }

    /**
     * Start listening to Brand collection changes
     */
    private fun startBrandListening() {
        brandListenerRegistration?.remove()

        _isLoading.value = true
        _error.value = null

        // Listen to brands collection
        brandListenerRegistration = brandCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                val errorMsg = error.message ?: "Error loading brands"
                _error.value = errorMsg
                Log.e(TAG, "❌ Error in brand listener: $errorMsg", error)
                Log.e(TAG, "Error code: ${error.code}, Error details: ${error.cause?.message}")
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    Log.d(TAG, "📥 Received snapshot with ${snapshot.documents.size} documents")

                    val brands = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Brand::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing brand document ${doc.id}: ${e.message}", e)
                            null // Skip this document and continue with others
                        }
                    }
                    _brands.value = brands
                    _error.value = null
                    _isLoading.value = false
                    Log.d(TAG, "✅ Brand listener updated: ${brands.size} brands")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing brand snapshot: ${e.message}", e)
                    _error.value = "Error processing brands: ${e.message}"
                    _isLoading.value = false
                }
            } else {
                Log.w(TAG, "⚠️ Snapshot is null in brand listener")
                _isLoading.value = false
            }
        }
        
        Log.d(TAG, "✅ Brand listener started successfully")
    }

    /**
     * Merge all products from active listeners into the _products StateFlow
     * Uses chassisNumber as unique identifier to deduplicate products from multiple listeners
     */
    private fun updateProductsFromListeners() {
        // Use chassisNumber as unique key to avoid duplicates
        val productMap = mutableMapOf<String, Product>()
        listenerProducts.values.forEach { products ->
            products.forEach { product ->
                if (product.chassisNumber.isNotBlank()) {
                    // If product already exists, prefer the newer one (or keep existing logic)
                    productMap[product.chassisNumber] = product
                }
            }
        }
        _products.value = productMap.values.toList()
        _isLoading.value = false
        Log.d(TAG, "✅ Products updated from listeners: ${_products.value.size} total products")
    }

    /**
     * Start listening to products by brandId
     */
    fun startListeningToProductsByBrandId(brandId: String) {
        val queryKey = "brand_$brandId"
        
        // If already listening, don't add duplicate
        if (activeProductListeners.containsKey(queryKey)) {
            Log.w(TAG, "⚠️ Already listening to products for brandId: $brandId")
            return
        }

        _isLoading.value = true
        
        repositoryScope.launch {
            try {
                val brandRef = resolveBrandRefByName(brandId)
                val query = if (brandRef != null) {
                    productCollection.whereEqualTo("brandRef", brandRef)
                } else {
                    productCollection.whereEqualTo("brandId", brandId)
                }

                val registration = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val errorMsg = error.message ?: "Error loading products for brand $brandId"
                        _error.value = errorMsg
                        Log.e(TAG, "❌ Error in product listener (brandId=$brandId): $errorMsg", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            Log.d(TAG, "📥 Received product snapshot for brandId=$brandId with ${snapshot.documents.size} documents")
                            val products = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Product::class.java)
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error parsing product document ${doc.id}: ${e.message}", e)
                                    null // Skip this document and continue with others
                                }
                            }
                            listenerProducts[queryKey] = products
                            updateProductsFromListeners()
                            _error.value = null
                            Log.d(TAG, "✅ Product listener updated (brandId=$brandId): ${products.size} products")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing product snapshot (brandId=$brandId): ${e.message}", e)
                            _error.value = "Error processing products: ${e.message}"
                        }
                    } else {
                        Log.w(TAG, "⚠️ Product snapshot is null for brandId=$brandId")
                    }
                }

                activeProductListeners[queryKey] = registration
                Log.d(TAG, "✅ Started listening to products for brandId: $brandId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting product listener for brandId $brandId: ${e.message}", e)
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * Start listening to products by brandId and productId
     */
    fun startListeningToProductsByBrandIdAndProductId(brandId: String, productId: String) {
        val queryKey = "brand_${brandId}_product_$productId"
        
        // If already listening, don't add duplicate
        if (activeProductListeners.containsKey(queryKey)) {
            Log.w(TAG, "⚠️ Already listening to products for brandId: $brandId, productId: $productId")
            return
        }

        _isLoading.value = true
        
        repositoryScope.launch {
            try {
                val brandRef = resolveBrandRefByName(brandId)
                val query = if (brandRef != null) {
                    productCollection
                        .whereEqualTo("brandRef", brandRef)
                        .whereEqualTo("productId", productId)
                } else {
                    productCollection
                        .whereEqualTo("brandId", brandId)
                        .whereEqualTo("productId", productId)
                }

                val registration = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val errorMsg = error.message ?: "Error loading products for brand $brandId, product $productId"
                        _error.value = errorMsg
                        Log.e(TAG, "❌ Error in product listener (brandId=$brandId, productId=$productId): $errorMsg", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            Log.d(TAG, "📥 Received product snapshot for brandId=$brandId, productId=$productId with ${snapshot.documents.size} documents")
                            val products = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Product::class.java)
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error parsing product document ${doc.id}: ${e.message}", e)
                                    null // Skip this document and continue with others
                                }
                            }
                            listenerProducts[queryKey] = products
                            updateProductsFromListeners()
                            _error.value = null
                            Log.d(TAG, "✅ Product listener updated (brandId=$brandId, productId=$productId): ${products.size} products")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing product snapshot (brandId=$brandId, productId=$productId): ${e.message}", e)
                            _error.value = "Error processing products: ${e.message}"
                        }
                    } else {
                        Log.w(TAG, "⚠️ Product snapshot is null for brandId=$brandId, productId=$productId")
                    }
                }

                activeProductListeners[queryKey] = registration
                Log.d(TAG, "✅ Started listening to products for brandId: $brandId, productId: $productId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting product listener for brandId $brandId, productId $productId: ${e.message}", e)
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * Start listening to products by brandId and multiple productIds (models)
     */
    fun startListeningToProductsByBrandIdAndModels(brandId: String, productIds: List<String>) {
        if (productIds.isEmpty()) return

        // Firestore whereIn has a limit of 10 items, so create separate listeners if needed
        val chunks = if (productIds.size <= 10) {
            listOf(productIds)
        } else {
            productIds.chunked(10)
        }

        chunks.forEachIndexed { index, chunk ->
            val queryKey = "brand_${brandId}_models_${index}_${chunk.joinToString("_")}"
            
            // If already listening, don't add duplicate
            if (activeProductListeners.containsKey(queryKey)) {
                println("⚠️ Already listening to products for brandId: $brandId, models: $chunk")
                return@forEachIndexed
            }

            _isLoading.value = true
            
            repositoryScope.launch {
                try {
                    val brandRef = resolveBrandRefByName(brandId)
                    val query = if (brandRef != null) {
                        productCollection
                            .whereEqualTo("brandRef", brandRef)
                            .whereIn("productId", chunk)
                    } else {
                        productCollection
                            .whereEqualTo("brandId", brandId)
                            .whereIn("productId", chunk)
                    }

                    val registration = query.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _error.value = error.message ?: "Error loading products for brand $brandId, models"
                            println("❌ Error in product listener (brandId=$brandId, models=$chunk): ${error.message}")
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            try {
                                val products = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        doc.toObject(Product::class.java)
                                    } catch (e: Exception) {
                                        println("❌ Error parsing product document ${doc.id}: ${e.message}")
                                        null // Skip this document and continue with others
                                    }
                                }
                                listenerProducts[queryKey] = products
                                updateProductsFromListeners()
                                _error.value = null
                                println("✅ Product listener updated (brandId=$brandId, models=$chunk): ${products.size} products")
                            } catch (e: Exception) {
                                println("❌ Error processing product snapshot (brandId=$brandId, models=$chunk): ${e.message}")
                                _error.value = "Error processing products: ${e.message}"
                            }
                        }
                    }

                    activeProductListeners[queryKey] = registration
                    println("✅ Started listening to products for brandId: $brandId, models: $chunk")
                } catch (e: Exception) {
                    println("❌ Error starting product listener for brandId $brandId, models $chunk: ${e.message}")
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Stop listening to products by brandId
     */
    fun stopListeningToProductsByBrandId(brandId: String) {
        val queryKey = "brand_$brandId"
        activeProductListeners[queryKey]?.remove()
        activeProductListeners.remove(queryKey)
        listenerProducts.remove(queryKey)
        updateProductsFromListeners()
        println("✅ Stopped listening to products for brandId: $brandId")
    }

    /**
     * Stop listening to products by brandId and productId
     */
    fun stopListeningToProductsByBrandIdAndProductId(brandId: String, productId: String) {
        val queryKey = "brand_${brandId}_product_$productId"
        activeProductListeners[queryKey]?.remove()
        activeProductListeners.remove(queryKey)
        listenerProducts.remove(queryKey)
        updateProductsFromListeners()
        println("✅ Stopped listening to products for brandId: $brandId, productId: $productId")
    }

    /**
     * Stop listening to products by brandId and models
     */
    fun stopListeningToProductsByBrandIdAndModels(brandId: String, productIds: List<String>) {
        val chunks = if (productIds.size <= 10) {
            listOf(productIds)
        } else {
            productIds.chunked(10)
        }
        
        chunks.forEachIndexed { index, chunk ->
            val queryKey = "brand_${brandId}_models_${index}_${chunk.joinToString("_")}"
            activeProductListeners[queryKey]?.remove()
            activeProductListeners.remove(queryKey)
            listenerProducts.remove(queryKey)
        }
        updateProductsFromListeners()
        println("✅ Stopped listening to products for brandId: $brandId, models")
    }

    /**
     * Stop all product listeners
     */
    fun stopAllProductListeners() {
        activeProductListeners.values.forEach { it.remove() }
        activeProductListeners.clear()
        listenerProducts.clear()
        _products.value = emptyList()
        println("✅ Stopped all product listeners")
    }

    /**
     * Stop listening (useful for cleanup)
     */
    fun stopListening() {
        brandListenerRegistration?.remove()
        stopAllProductListeners()
        brandListenerRegistration = null
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    suspend fun uploadImagesToStorage(
        brandId: String,
        productId: String,
        imageUris: List<Uri>
    ): Result<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()
            val storage = Firebase.storage
            val storageRef = storage.reference

            for (uri in imageUris) {
                val imageRef = storageRef.child("vehicle_images/$brandId/$productId/${System.currentTimeMillis()}.jpg")
                imageRef.putFile(uri).await()                       // Upload image
                val url = imageRef.downloadUrl.await().toString()   // Get download URL
                downloadUrls.add(url)
            }

            Result.success(downloadUrls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upload images from string list (handles both Firebase URLs and local URIs)
    suspend fun uploadImagesToStorageFromStrings(
        imageStrings: List<String>,
        brandId: String,
        productId: String
    ): Result<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()
            val storage = Firebase.storage
            val storageRef = storage.reference

            for ((index, uriString) in imageStrings.withIndex()) {
                try {
                    // If already a hosted URL, keep as-is
                    if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                        downloadUrls.add(uriString)
                        continue
                    }

                    val uri = Uri.parse(uriString)
                    if (uri.scheme.isNullOrEmpty() || (uri.scheme != "content" && uri.scheme != "file")) {
                        continue
                    }

                    val fileName = "image_${System.currentTimeMillis()}_$index.jpg"
                    val imageRef = storageRef.child("vehicle_images/$brandId/$productId/$fileName")
                    imageRef.putFile(uri).await()
                    val downloadUrl = imageRef.downloadUrl.await().toString()
                    downloadUrls.add(downloadUrl)
                } catch (e: Exception) {
                    println("❌ Failed to upload image $uriString: ${e.message}")
                }
            }

            Result.success(downloadUrls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadPdfsToStorage(
        pdfUris: List<String>,
        brandId: String,
        productId: String,
        documentType: String
    ): Result<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()
            val storage = Firebase.storage
            val storageRef = storage.reference

            for ((index, uriString) in pdfUris.withIndex()) {
                try {
                    // If already a hosted URL, keep as-is
                    if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                        downloadUrls.add(uriString)
                        continue
                    }

                    val uri = Uri.parse(uriString)
                    if (uri.scheme.isNullOrEmpty() || (uri.scheme != "content" && uri.scheme != "file")) {
                        continue
                    }

                    val fileName = "${documentType}_${System.currentTimeMillis()}_$index.pdf"
                    val fileRef = storageRef.child("vehicle_documents/$brandId/$productId/$fileName")

                    // Ensure correct content type for PDFs
                    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType("application/pdf")
                        .build()

                    fileRef.putFile(uri, metadata).await()
                    val downloadUrl = fileRef.downloadUrl.await().toString()
                    downloadUrls.add(downloadUrl)
                } catch (e: Exception) {
                    println("❌ Failed to upload PDF $uriString: ${e.message}")
                }
            }

            Result.success(downloadUrls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkChassisNumberExists(chassisNumber: String): Boolean {
        return try {
            // Use the same collection name used when adding a vehicle ("chassisNumber")
            val result = db.collection("ChassisNumber")
                .whereEqualTo("chassisNumber", chassisNumber)
                .limit(1)
                .get()
                .await()

            !result.isEmpty
        } catch (e: Exception) {
            throw e
        }
    }
    /**
     * Get all brands - now returns StateFlow value
     * Use brands StateFlow directly in ViewModels instead
     * @deprecated Use brands StateFlow directly instead
     */
    @Deprecated("Use brands StateFlow directly instead")
    suspend fun getBrands(): Result<List<Brand>> {
        return Result.success(_brands.value)
    }
    suspend fun getColours(): Result<List<String>> {
        return try {
            val document = db.collection("Colour")
                .document("unfVwvmNspz7mhoyGz9z") // 👈 Use the actual document ID
                .get()
                .await()

            val colours = document.get("colour") as? List<String> ?: emptyList()
            Result.success(colours)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addColour(newColour: String): Result<Unit> {
        return try {
            val colourDocRef = db.collection("Colour")
                .document("unfVwvmNspz7mhoyGz9z")

            // Add the new colour to the existing array
            colourDocRef.update("colour", FieldValue.arrayUnion(newColour)).await()

            println("✅ Successfully added colour: $newColour to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error adding colour: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun getModelsByBrandId(brandId: String): Result<List<String>> {
        return try {
            // Prefer reference-based lookup; fallback to legacy string field
            val brandRef = resolveBrandRefByName(brandId)

            val querySnapshot = if (brandRef != null) {
                brandCollection
                    .whereEqualTo("brandRef", brandRef)
                    .get()
                    .await()
            } else {
                brandCollection
                    .whereEqualTo("brandId", brandId)
                    .get()
                    .await()
            }

            // Get the first brand document
            val brandDoc = querySnapshot.documents.firstOrNull()
                ?: return Result.failure(Exception("Brand not found"))

            // Extract the models array directly (assuming field name is "modelNames")
            val models = brandDoc.get("modelNames") as? List<String> ?: emptyList()

            println("✅ Loaded ${models.size} models for brand $brandId: $models")
            Result.success(models)
        } catch (e: Exception) {
            println("❌ Error loading models: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get brand by ID - first checks StateFlow, then fetches from Firestore if not found
     */
    suspend fun getBrandById(brandId: String): Result<Brand> {
        // Check current StateFlow first
        val brand = _brands.value.find { it.brandId == brandId }
        if (brand != null) {
            println("📦 Found brand in StateFlow: ${brand.brandId}")
            return Result.success(brand)
        }

        // If not in StateFlow, fetch from Firestore (might be a new document)
        return try {
            val brandRef = resolveBrandRefByName(brandId)

            val querySnapshot = if (brandRef != null) {
                brandCollection
                    .whereEqualTo("brandRef", brandRef)
                    .get()
                    .await()
            } else {
                brandCollection
                    .whereEqualTo("brandId", brandId)
                    .get()
                    .await()
            }

            val fetchedBrand = querySnapshot.documents.firstOrNull()?.toObject(Brand::class.java)
            if (fetchedBrand != null) {
                println("✅ Brand found in Firestore: ${fetchedBrand.brandId}")
                // Note: Listener will automatically update StateFlow when document is added
                Result.success(fetchedBrand)
            } else {
                Result.failure(Exception("Brand not found"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching brand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all products - now returns StateFlow value
     * Use products StateFlow directly in ViewModels instead
     * @deprecated Use products StateFlow directly instead
     */
    @Deprecated("Use products StateFlow directly instead")
    suspend fun getAllProducts(): Result<List<Product>> {
        return Result.success(_products.value)
    }

    // 🔹 Product queries - check StateFlow first, start listener, then query Firestore if needed
    suspend fun getProductByBrandIdProductId(
        brandId: String,
        productId: String
    ): Result<List<Product>> {
        // Start active listener for this query
        startListeningToProductsByBrandIdAndProductId(brandId, productId)
        
        // Try to get from StateFlow first
        val filtered = _products.value.filter { 
            it.brandId == brandId && it.productId == productId 
        }
        if (filtered.isNotEmpty()) {
            println("📦 Found products in StateFlow: brandId=$brandId, productId=$productId")
            return Result.success(filtered)
        }

        // If not in StateFlow yet (listener might be initializing), query Firestore once
        return try {
            val brandRef = resolveBrandRefByName(brandId)
            val querySnapshot = if (brandRef != null) {
                productCollection
                    .whereEqualTo("brandRef", brandRef)
                    .whereEqualTo("productId", productId)
                    .get()
                    .await()
            } else {
                productCollection
                    .whereEqualTo("brandId", brandId)
                    .whereEqualTo("productId", productId)
                    .get()
                    .await()
            }

            val products = querySnapshot.documents.mapNotNull { it.toObject(Product::class.java) }
            
            // ✅ Listener will automatically update StateFlow, but we return immediate results

            if (products.isNotEmpty()) {
                println("✅ Found ${products.size} products (query result, listener active)")
                Result.success(products)
            } else {
                println("❌ No products found")
                Result.failure(Exception("No products found"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching products: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Get all products for a specific brand - start listener, check StateFlow first
    suspend fun getProductsByBrandId(brandId: String): Result<List<Product>> {
        // Start active listener for this query
        startListeningToProductsByBrandId(brandId)
        
        // Try to get from StateFlow first
        val filtered = _products.value.filter { 
            it.brandId == brandId
        }
        if (filtered.isNotEmpty()) {
            println("📦 Found products in StateFlow: brandId=$brandId")
            return Result.success(filtered)
        }

        // If not in StateFlow yet (listener might be initializing), query Firestore once
        return try {
            val brandRef = resolveBrandRefByName(brandId)

            val querySnapshot = if (brandRef != null) {
                productCollection
                    .whereEqualTo("brandRef", brandRef)
                    .get()
                    .await()
            } else {
                productCollection
                    .whereEqualTo("brandId", brandId)
                    .get()
                    .await()
            }

            val products = querySnapshot.documents.mapNotNull { it.toObject(Product::class.java) }
            
            // ✅ Listener will automatically update StateFlow, but we return immediate results
            
            println("✅ Loaded ${products.size} products for brand $brandId (query result, listener active)")
            Result.success(products)
        } catch (e: Exception) {
            println("❌ Error loading products by brand: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Get all products for specific models of a brand - start listener, check StateFlow first
    suspend fun getProductsByBrandIdAndModels(
        brandId: String,
        modelNames: List<String>
    ): Result<List<Product>> {
        // Start active listener for this query
        startListeningToProductsByBrandIdAndModels(brandId, modelNames)
        
        // Try to get from StateFlow first
        val filtered = _products.value.filter { 
            it.brandId == brandId && it.productId in modelNames
        }
        if (filtered.isNotEmpty()) {
            println("📦 Found products in StateFlow: brandId=$brandId, models=$modelNames")
            return Result.success(filtered)
        }

        // If not in StateFlow yet (listener might be initializing), query Firestore once
        return try {
            // Firestore whereIn has a limit of 10 items, so we need to batch if needed
            val allProducts = mutableListOf<Product>()
            val brandRef = resolveBrandRefByName(brandId)
            
            if (modelNames.size <= 10) {
                // Single query if <= 10 models
                val querySnapshot = if (brandRef != null) {
                    productCollection
                        .whereEqualTo("brandRef", brandRef)
                        .whereIn("productId", modelNames)
                        .get()
                        .await()
                } else {
                    productCollection
                        .whereEqualTo("brandId", brandId)
                        .whereIn("productId", modelNames)
                        .get()
                        .await()
                }
                
                allProducts.addAll(querySnapshot.documents.mapNotNull { it.toObject(Product::class.java) })
            } else {
                // Batch queries for > 10 models
                modelNames.chunked(10).forEach { chunk ->
                    val querySnapshot = if (brandRef != null) {
                        productCollection
                            .whereEqualTo("brandRef", brandRef)
                            .whereIn("productId", chunk)
                            .get()
                            .await()
                    } else {
                        productCollection
                            .whereEqualTo("brandId", brandId)
                            .whereIn("productId", chunk)
                            .get()
                            .await()
                    }
                    
                    allProducts.addAll(querySnapshot.documents.mapNotNull { it.toObject(Product::class.java) })
                }
            }
            
            // ✅ Listener will automatically update StateFlow, but we return immediate results
            
            println("✅ Loaded ${allProducts.size} products for brand $brandId and models (query result, listener active)")
            Result.success(allProducts)
        } catch (e: Exception) {
            println("❌ Error loading products by brand and models: ${e.message}")
            Result.failure(e)
        }
    }
    suspend fun addNewBrand(brand: Brand): Result<Unit> {
        return try {
            // Ensure BrandNames entry exists (unique by name)
            val brandName = brand.brandId
            val brandNameQuery = brandNamesCollection
                .whereEqualTo("name", brandName)
                .limit(1)
                .get()
                .await()

            val brandNameDocRef = if (brandNameQuery.documents.isNotEmpty()) {
                brandNameQuery.documents.first().reference
            } else {
                // Create BrandNames/{autoId} with { name }
                val doc = brandNamesCollection.document()
                doc.set(mapOf("name" to brandName)).await()
                doc
            }

            // Check if a Brand already exists pointing at this BrandNames ref
            val existingBrandQuery = brandCollection
                .whereEqualTo("brandRef", brandNameDocRef)
                .limit(1)
                .get()
                .await()

            if (existingBrandQuery.documents.isNotEmpty()) {
                return Result.failure(Exception("Brand with name ${brandName} already exists"))
            }

            // Create a new document in Brand
            val newBrandRef = brandCollection.document()

            // Run transaction to add brand safely
            db.runTransaction { transaction ->
                val brandData = hashMapOf(
                    // Keep legacy string for backward-compat reads
                    "brandId" to brandName,
                    // New reference field
                    "brandRef" to brandNameDocRef,
                    "logo" to brand.logo,
                    "modelNames" to brand.modelNames, // Can be empty list
                    "vehicle" to brand.vehicle // Optional vehicle summary
                )
                transaction.set(newBrandRef, brandData)
            }.await()

            // ✅ No need to update cache - listener will automatically update StateFlow

            println("✅ Successfully added new brand ${brand.brandId} with ID ${newBrandRef.id} and BrandNames ref ${brandNameDocRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error adding new brand: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addModelToBrand(brandId: String, modelName: String): Result<Unit> {
        return try {
            // Get the brand document reference
            val brandRef = resolveBrandRefByName(brandId)
            val brandDocRef = if (brandRef != null) {
                brandCollection
                    .whereEqualTo("brandRef", brandRef)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()?.reference
            } else {
                brandCollection
                    .whereEqualTo("brandId", brandId)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()?.reference
            } ?: return Result.failure(Exception("Brand not found"))

            // Add the model name to the brand's models array
            brandDocRef.update("modelNames", FieldValue.arrayUnion(modelName)).await()

            // ✅ No need to update cache - listener will automatically update StateFlow

            println("✅ Successfully added model $modelName to brand $brandId in Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error adding model to brand: ${e.message}")
            Result.failure(e)
        }
    }
    suspend fun getProductFeatureByChassis(chassisNumber: String): Result<Product?> {
        // Check StateFlow first
        val product = _products.value.find { it.chassisNumber == chassisNumber }
        if (product != null) {
            println("📦 Found product in StateFlow: $chassisNumber")
            return Result.success(product)
        }

        // If not in StateFlow, query Firestore
        return try {
            val querySnapshot = productCollection
                .whereEqualTo("chassisNumber", chassisNumber)
                .limit(1)
                .get()
                .await()

            val fetchedProduct = querySnapshot.documents.firstOrNull()?.toObject(Product::class.java)
            
            if (fetchedProduct != null) {
                // ✅ No need to update cache - listener will automatically update StateFlow
                println("✅ Product found by chassis number: $chassisNumber")
                Result.success(fetchedProduct)
            } else {
                println("❌ Chassis number not found: $chassisNumber")
                Result.failure(Exception("Chassis number $chassisNumber not found"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching product by chassis: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateVehicle(
        originalChassisNumber: String,
        updatedProduct: Product
    ): Result<Unit> {
        return try {
            // Find the existing product document by original chassis number
            val querySnapshot = productCollection
                .whereEqualTo("chassisNumber", originalChassisNumber)
                .limit(1)
                .get()
                .await()

            val docRef = querySnapshot.documents.firstOrNull()?.reference
                ?: return Result.failure(Exception("Vehicle with chassis $originalChassisNumber not found"))

            // ✅ Upload new images (existing Firebase URLs are preserved, new local URIs are uploaded)
            val uploadedImageUrls = uploadImagesToStorageFromStrings(
                imageStrings = updatedProduct.images,
                brandId = updatedProduct.brandId,
                productId = updatedProduct.productId
            )

            // ✅ Upload new PDFs (existing Firebase URLs are preserved, new local URIs are uploaded)
            val uploadedNocUrls = uploadPdfsToStorage(
                pdfUris = updatedProduct.noc,
                brandId = updatedProduct.brandId,
                productId = updatedProduct.productId,
                documentType = "noc"
            )
            val uploadedRcUrls = uploadPdfsToStorage(
                pdfUris = updatedProduct.rc,
                brandId = updatedProduct.brandId,
                productId = updatedProduct.productId,
                documentType = "rc"
            )
            val uploadedInsuranceUrls = uploadPdfsToStorage(
                pdfUris = updatedProduct.insurance,
                brandId = updatedProduct.brandId,
                productId = updatedProduct.productId,
                documentType = "insurance"
            )

            // Get uploaded URLs (or use empty list if upload failed)
            val finalImageUrls = if (uploadedImageUrls.isSuccess) uploadedImageUrls.getOrThrow() else updatedProduct.images
            val finalNocUrls = if (uploadedNocUrls.isSuccess) uploadedNocUrls.getOrThrow() else updatedProduct.noc
            val finalRcUrls = if (uploadedRcUrls.isSuccess) uploadedRcUrls.getOrThrow() else updatedProduct.rc
            val finalInsuranceUrls = if (uploadedInsuranceUrls.isSuccess) uploadedInsuranceUrls.getOrThrow() else updatedProduct.insurance

            // Resolve brand reference for the updated product's brand name (kept in updatedProduct.brandId)
            val updatedBrandRef = resolveBrandRefByName(updatedProduct.brandId)

            // Update the document with new fields
            val updateMap = mutableMapOf<String, Any>(
                // legacy string retained
                "brandId" to updatedProduct.brandId,
                // new reference when available
                "productId" to updatedProduct.productId,
                "chassisNumber" to updatedProduct.chassisNumber,
                "colour" to updatedProduct.colour,
                "condition" to updatedProduct.condition,
                "images" to finalImageUrls,  // ✅ Use uploaded URLs (Firebase URLs for existing, new URLs for new uploads)
                "kms" to updatedProduct.kms,
                "lastService" to updatedProduct.lastService,
                "previousOwners" to updatedProduct.previousOwners,
                "price" to updatedProduct.price,
                "type" to updatedProduct.type,
                "year" to updatedProduct.year,
                // New fields
                "noc" to finalNocUrls,  // ✅ Use uploaded URLs
                "rc" to finalRcUrls,  // ✅ Use uploaded URLs
                "insurance" to finalInsuranceUrls,  // ✅ Use uploaded URLs
                "brokerOrMiddleMan" to updatedProduct.brokerOrMiddleMan,
                "owner" to updatedProduct.owner,
                "sold" to updatedProduct.sold
            )
            
            // Add brandRef if available
            updatedBrandRef?.let {
                updateMap["brandRef"] = it
            }
            
            // Add reference fields if they exist
            updatedProduct.brokerOrMiddleManRef?.let {
                updateMap["brokerOrMiddleManRef"] = it
            }
            updatedProduct.ownerRef?.let {
                updateMap["ownerRef"] = it
            }

            docRef.update(updateMap).await()

            // ✅ No need to update cache - listener will automatically update StateFlow

            println("✅ Vehicle updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error updating vehicle: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteVehicleByChassis(chassisNumber: String): Result<Unit> {
        return try {
            // 1) Find the product by chassis number
            val productQuery = productCollection
                .whereEqualTo("chassisNumber", chassisNumber)
                .limit(1)
                .get()
                .await()

            val productDoc = productQuery.documents.firstOrNull()
                ?: return Result.failure(Exception("Vehicle with chassis $chassisNumber not found"))

            val product = productDoc.toObject(Product::class.java)
                ?: return Result.failure(Exception("Invalid product data for chassis $chassisNumber"))

            val productDocRef = productDoc.reference

            // 2) Find the brand document, prefer reference
            val productBrandRef = try {
                productDoc.get("brandRef") as? DocumentReference
            } catch (e: Exception) { null }

            val brandQuery = if (productBrandRef != null) {
                brandCollection
                    .whereEqualTo("brandRef", productBrandRef)
                    .limit(1)
                    .get()
                    .await()
            } else {
                brandCollection
                    .whereEqualTo("brandId", product.brandId)
                    .limit(1)
                    .get()
                    .await()
            }

            val brandDocRef = brandQuery.documents.firstOrNull()?.reference
                ?: return Result.failure(Exception("Brand ${product.brandId} not found for delete"))

            // 3) Transaction: decrement or remove vehicle summary, delete product
            db.runTransaction { transaction ->
                val brandSnapshot = transaction.get(brandDocRef)
                val currentVehicles = brandSnapshot.get("vehicle") as? List<Map<String, Any>> ?: emptyList()

                val existingVehicleIndex = currentVehicles.indexOfFirst { it["productId"] == product.productId }

                if (existingVehicleIndex != -1) {
                    val updatedVehicles = currentVehicles.toMutableList()
                    val existingVehicle = updatedVehicles[existingVehicleIndex].toMutableMap()
                    val currentQuantity = (existingVehicle["quantity"] as? Long)?.toInt() ?: 0

                    if (currentQuantity > 1) {
                        existingVehicle["quantity"] = currentQuantity - 1
                        updatedVehicles[existingVehicleIndex] = existingVehicle
                    } else {
                        updatedVehicles.removeAt(existingVehicleIndex)
                    }

                    transaction.update(brandDocRef, "vehicle", updatedVehicles)
                }

                // Always delete the product document
                transaction.delete(productDocRef)
            }.await()

            // ✅ No need to update cache - listener will automatically update StateFlow

            println("✅ Vehicle deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error deleting vehicle: ${e.message}")
            Result.failure(e)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    suspend fun addVehicleToBrand(
        brandId: String,
        product: Product,
        imageUris: List<Uri>,
        nocPdfs: List<String> = emptyList(),
        rcPdfs: List<String> = emptyList(),
        insurancePdfs: List<String> = emptyList(),
        brokerOrMiddleManId: String? = null,
        ownerId: String? = null,
        purchaseType: String? = null
    ): Result<Unit> {
        return try {
            // 1️⃣ Upload images to Storage first
            val uploadResult = uploadImagesToStorage(brandId, product.productId, imageUris)
            if (uploadResult.isFailure) return Result.failure(uploadResult.exceptionOrNull()!!)
            val imageUrls = uploadResult.getOrThrow()
            
            // 1b️⃣ Upload PDFs to Storage
            val nocUrlsResult = uploadPdfsToStorage(nocPdfs, brandId, product.productId, "noc")
            val rcUrlsResult = uploadPdfsToStorage(rcPdfs, brandId, product.productId, "rc")
            val insuranceUrlsResult = uploadPdfsToStorage(insurancePdfs, brandId, product.productId, "insurance")
            
            val nocUrls = if (nocUrlsResult.isSuccess) nocUrlsResult.getOrThrow() else emptyList()
            val rcUrls = if (rcUrlsResult.isSuccess) rcUrlsResult.getOrThrow() else emptyList()
            val insuranceUrls = if (insuranceUrlsResult.isSuccess) insuranceUrlsResult.getOrThrow() else emptyList()

            // Resolve BrandNames reference by brand name
            val brandNameRef = resolveBrandRefByName(brandId)
            val brandDocRef = if (brandNameRef != null) {
                val q = brandCollection.whereEqualTo("brandRef", brandNameRef).get().await()
                q.documents.firstOrNull()?.reference
            } else {
                val q = brandCollection.whereEqualTo("brandId", brandId).get().await()
                q.documents.firstOrNull()?.reference
            } ?: return Result.failure(Exception("Brand not found"))

            // Resolve brokerOrMiddleMan reference
            val brokerOrMiddleManRef: DocumentReference? = when {
                !brokerOrMiddleManId.isNullOrBlank() && purchaseType == "Broker" -> {
                    // Broker reference
                    val ref = db.collection("Broker").document(brokerOrMiddleManId)
                    println("✅ Creating Broker reference: ${ref.path} for ID: $brokerOrMiddleManId")
                    ref
                }
                !brokerOrMiddleManId.isNullOrBlank() && purchaseType == "Middle Man" -> {
                    // Customer reference (middle man is a customer)
                    val ref = db.collection("Customer").document(brokerOrMiddleManId)
                    println("✅ Creating Customer reference (Middle Man): ${ref.path} for ID: $brokerOrMiddleManId")
                    ref
                }
                else -> {
                    println("⚠️ brokerOrMiddleManRef is null. purchaseType: $purchaseType, brokerOrMiddleManId: $brokerOrMiddleManId")
                    null
                }
            }

            // Resolve owner reference (always a customer)
            val ownerRef: DocumentReference? = if (!ownerId.isNullOrBlank()) {
                val ref = db.collection("Customer").document(ownerId)
                println("✅ Creating Owner reference: ${ref.path} for ID: $ownerId")
                ref
            } else {
                println("⚠️ ownerRef is null. ownerId: $ownerId")
                null
            }

            // 2️⃣ Run transaction
            db.runTransaction { transaction ->
                val brandSnapshot = transaction.get(brandDocRef)
                val currentVehicles =
                    brandSnapshot.get("vehicle") as? List<Map<String, Any>> ?: emptyList()

                val existingVehicleIndex = currentVehicles.indexOfFirst { it["productId"] == product.productId }

                val updatedVehicles = currentVehicles.toMutableList()
                if (existingVehicleIndex != -1) {
                    val existingVehicle = updatedVehicles[existingVehicleIndex].toMutableMap()
                    val currentQuantity = (existingVehicle["quantity"] as? Long)?.toInt() ?: 1
                    existingVehicle["quantity"] = currentQuantity + 1
                    updatedVehicles[existingVehicleIndex] = existingVehicle
                } else {
                val newVehicle = hashMapOf(
                        "imageUrl" to (imageUrls.firstOrNull()?: "https://example.com/default_image.png"),
                        "productId" to product.productId,
                        "type" to product.type,
                        "quantity" to 1
                    )
                    updatedVehicles.add(newVehicle)
                }

                transaction.update(brandDocRef, "vehicle", updatedVehicles)

                // 👇 Add chassis reference
                // 1️⃣ Create a new chassis document with an auto-generated ID
                val chassisRef = db.collection("ChassisNumber").document()

                // 2️⃣ Add any data you want to that chassis document

                transaction.set(
                    chassisRef,
                    mapOf(
                        "chassisNumber" to product.chassisNumber,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                val newProductRef = productCollection.document()
                val productDetails = hashMapOf<String, Any>(
                    // legacy string for backward-compat
                    "brandId" to brandId,
                    "productId" to product.productId,
                    "chassisNumber" to product.chassisNumber,
                    "chassisReference" to chassisRef, // 👈 NEW FIELD
                    "colour" to product.colour,
                    "condition" to product.condition,
                    "images" to imageUrls,
                    "kms" to product.kms,
                    "lastService" to product.lastService,
                    "previousOwners" to product.previousOwners,
                    "price" to product.price,
                    "type" to product.type,
                    "year" to product.year,
                    // New fields
                    "noc" to nocUrls,
                    "rc" to rcUrls,
                    "insurance" to insuranceUrls,
                    "brokerOrMiddleMan" to product.brokerOrMiddleMan,
                    "owner" to product.owner,
                    "sold" to false
                )
                
                // Add brandRef if available
                brandNameRef?.let {
                    productDetails["brandRef"] = it
                }
                
                // Add references if they exist
                brokerOrMiddleManRef?.let {
                    productDetails["brokerOrMiddleManRef"] = it
                    println("✅ Added brokerOrMiddleManRef to productDetails: ${it.path}")
                } ?: run {
                    println("❌ brokerOrMiddleManRef is null, not adding to productDetails")
                }
                
                ownerRef?.let {
                    productDetails["ownerRef"] = it
                    println("✅ Added ownerRef to productDetails: ${it.path}")
                } ?: run {
                    println("❌ ownerRef is null, not adding to productDetails")
                }
                
                transaction.set(newProductRef, productDetails)
            }.await()


            // ✅ No need to update cache - listener will automatically update StateFlow

            println("✅ Vehicle added successfully to brand $brandId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error adding vehicle to brand: ${e.message}")
            Result.failure(e)
        }
    }


    private fun HashMap<String, String?>.toVehicleSummary(): VehicleSummary {
        return VehicleSummary(
            productId = this["productId"] as? String ?: "",
            type = this["type"] as? String ?: "",
            imageUrl = this["imageUrl"] as? String ?: ""
        )
    }

    // Resolve BrandNames/{autoId} DocumentReference by brand name; null if not found
    private suspend fun resolveBrandRefByName(brandName: String): DocumentReference? {
        return try {
            val query = brandNamesCollection
                .whereEqualTo("name", brandName)
                .limit(1)
                .get()
                .await()
            query.documents.firstOrNull()?.reference
        } catch (_: Exception) {
            null
        }
    }

    // Add a method to create a catalog document in Firestore
    suspend fun createCatalog(
        productIds: List<String>
    ): Result<String> {
        return try {
            val catalogCollection = db.collection("Catalog")
            val catalogDocRef = catalogCollection.document()
            val catalogId = catalogDocRef.id

            val catalogData = hashMapOf(
                "products" to productIds,
                "createdAt" to System.currentTimeMillis(),
                "productCount" to productIds.size
            )

            catalogDocRef.set(catalogData).await()
            
            println("✅ Catalog created successfully with ID: $catalogId")
            Result.success(catalogId)
        } catch (e: Exception) {
            println("❌ Error creating catalog: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Helper method to get product document IDs by brand and model names
    suspend fun getProductIdsByBrandAndModels(
        brandId: String,
        modelNames: List<String>
    ): Result<List<String>> {
        return try {
            val productIds = mutableListOf<String>()
            val brandRef = resolveBrandRefByName(brandId)
            
            if (modelNames.size <= 10) {
                // Single query if <= 10 models
                val querySnapshot = if (brandRef != null) {
                    productCollection
                        .whereEqualTo("brandRef", brandRef)
                        .whereIn("productId", modelNames)
                        .get()
                        .await()
                } else {
                    productCollection
                        .whereEqualTo("brandId", brandId)
                        .whereIn("productId", modelNames)
                        .get()
                        .await()
                }
                
                productIds.addAll(querySnapshot.documents.map { it.id })
            } else {
                // Batch queries for > 10 models
                modelNames.chunked(10).forEach { chunk ->
                    val querySnapshot = if (brandRef != null) {
                        productCollection
                            .whereEqualTo("brandRef", brandRef)
                            .whereIn("productId", chunk)
                            .get()
                            .await()
                    } else {
                        productCollection
                            .whereEqualTo("brandId", brandId)
                            .whereIn("productId", chunk)
                            .get()
                            .await()
                    }
                    
                    productIds.addAll(querySnapshot.documents.map { it.id })
                }
            }
            
            Result.success(productIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetch customer by DocumentReference
    suspend fun getCustomerByReference(customerRef: DocumentReference): Result<Customer> {
        return try {
            val document = customerRef.get().await()
            val customer = document.toObject(Customer::class.java)
            
            if (customer != null) {
                // Set customerId from document ID
                val customerWithId = customer.copy(customerId = document.id)
                Result.success(customerWithId)
            } else {
                Result.failure(Exception("Customer not found"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching customer by reference: ${e.message}")
            Result.failure(e)
        }
    }

    // Fetch broker by DocumentReference
    suspend fun getBrokerByReference(brokerRef: DocumentReference): Result<Broker> {
        return try {
            val document = brokerRef.get().await()
            val broker = document.toObject(Broker::class.java)
            
            if (broker != null) {
                // Set brokerId from document ID
                val brokerWithId = broker.copy(brokerId = document.id)
                Result.success(brokerWithId)
            } else {
                Result.failure(Exception("Broker not found"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching broker by reference: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Resolve brand document reference and BrandNames reference for a given brand ID
     * @return Pair of (brandDocRef, brandNameRef) or null if brand not found
     */
    suspend fun resolveBrandReferences(brandId: String): Result<Pair<DocumentReference, DocumentReference?>> {
        return try {
            val brandNameRef = resolveBrandRefByName(brandId)
            val brandDocRef = if (brandNameRef != null) {
                val q = brandCollection.whereEqualTo("brandRef", brandNameRef).get().await()
                q.documents.firstOrNull()?.reference
            } else {
                val q = brandCollection.whereEqualTo("brandId", brandId).get().await()
                q.documents.firstOrNull()?.reference
            } ?: return Result.failure(Exception("Brand not found"))
            
            Result.success(Pair(brandDocRef, brandNameRef))
        } catch (e: Exception) {
            println("❌ Error resolving brand references: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get vehicle reference by chassis number
     */
    suspend fun getVehicleReferenceByChassis(chassisNumber: String): Result<DocumentReference> {
        return try {
            val querySnapshot = productCollection
                .whereEqualTo("chassisNumber", chassisNumber)
                .limit(1)
                .get()
                .await()
            
            val docRef = querySnapshot.documents.firstOrNull()?.reference
                ?: return Result.failure(Exception("Vehicle with chassis $chassisNumber not found"))
            
            Result.success(docRef)
        } catch (e: Exception) {
            println("❌ Error getting vehicle reference: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get product by chassis number (alias for getProductFeatureByChassis)
     */
    suspend fun getProductByChassisNumber(chassisNumber: String): Result<Product> {
        return try {
            val result = getProductFeatureByChassis(chassisNumber)
            result.fold(
                onSuccess = { product ->
                    if (product != null) {
                        Result.success(product)
                    } else {
                        Result.failure(Exception("Product not found"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get product by DocumentReference
     */
    suspend fun getProductByReference(productRef: DocumentReference): Result<Product> {
        return try {
            val document = productRef.get().await()
            val product = document.toObject(Product::class.java)
            
            if (product != null) {
                Result.success(product)
            } else {
                Result.failure(Exception("Product not found"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching product by reference: ${e.message}")
            Result.failure(e)
        }
    }

}

// 🔹 Helper extension to convert map to VehicleSummary object
// Helper to convert map to VehicleSummary


