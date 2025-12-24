package com.example.cardealer2.repository

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.example.cardealer2.data.Customer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object CustomerRepository {
    @SuppressLint("StaticFieldLeak")
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Configure Firestore settings for better reliability on real devices
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Unlimited cache
            .build()
        firestoreSettings = settings
    }
    private val customerCollection = db.collection("Customer")

    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    private const val TAG = "CustomerRepository"

    // 🔹 StateFlow exposed to ViewModels - automatically updates when Firestore changes
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    // 🔹 Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 🔹 Keep reference to listener so we can remove it if needed
    private var listenerRegistration: ListenerRegistration? = null

    // 🔹 Initialize listener when repository is first accessed
    init {
        startListening()
    }

    /**
     * Start listening to Firestore collection changes
     * This replaces manual cache and manual reload calls
     */
    private fun startListening() {
        // Remove existing listener if any
        listenerRegistration?.remove()

        _isLoading.value = true
        _error.value = null

        listenerRegistration = customerCollection.addSnapshotListener { snapshot, error ->
            _isLoading.value = false

            if (error != null) {
                val errorMsg = error.message ?: "Error loading customers"
                _error.value = errorMsg
                Log.e(TAG, "❌ Error in customer listener: $errorMsg", error)
                Log.e(TAG, "Error code: ${error.code}, Error details: ${error.cause?.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    Log.d(TAG, "📥 Received snapshot with ${snapshot.documents.size} documents")

                    val customers = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Customer::class.java)?.copy(customerId = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing customer document ${doc.id}: ${e.message}", e)
                            null // Skip this document and continue with others
                        }
                    }
                    _customers.value = customers
                    _error.value = null
                    Log.d(TAG, "✅ Customer listener updated: ${customers.size} customers")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing customer snapshot: ${e.message}", e)
                    _error.value = "Error processing customers: ${e.message}"
                }
            } else {
                Log.w(TAG, "⚠️ Snapshot is null in customer listener")
            }
        }
        
        Log.d(TAG, "✅ Customer listener started successfully")
    }

    /**
     * Stop listening (useful for cleanup, though usually not needed since repository is singleton)
     */
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }


    suspend fun addCustomer(customer: Customer): Result<Unit> {
        return try {
            val newCustomerRef = customerCollection.document()
            val customerId = newCustomerRef.id

            // ✅ Upload customer photos
            val uploadedPhotoUrls = uploadImagesToStorage(
                images = customer.photoUrl,
                folderPath = "customers/$customerId/photos"
            )

            // ✅ Upload ID proof PDFs (ensure correct content type and extension)
            val uploadedIdProofUrls = uploadPdfsToStorage(
                pdfs = customer.idProofImageUrls ?: emptyList(),
                folderPath = "customers/$customerId/id_proofs"
            )

            // ✅ Create Firestore data map
            val customerData = hashMapOf(
                "customerId" to customerId,
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "photoUrl" to uploadedPhotoUrls,         // ✅ Firebase URLs
                "idProofType" to customer.idProofType,
                "idProofNumber" to customer.idProofNumber,
                "idProofImageUrls" to uploadedIdProofUrls, // ✅ Firebase URLs
                "amount" to customer.amount,
                "createdAt" to customer.createdAt
            )

            // ✅ Store in Firestore
            db.runTransaction { transaction ->
                transaction.set(newCustomerRef, customerData)
            }.await()

            // ✅ No need to update cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Customer added successfully with ID: $customerId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding customer: ${e.message}", e)
            Result.failure(e)
        }
    }


    private suspend fun uploadImagesToStorage(
        images: List<String>,
        folderPath: String
    ): List<String> {
        val downloadUrls = mutableListOf<String>()

        for ((index, uriString) in images.withIndex()) {
            try {
                // If the string is already a hosted URL, keep it as-is
                if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    downloadUrls.add(uriString)
                    continue
                }

                val uri = Uri.parse(uriString)
                // Support both content:// and file:// URIs (camera captures often return file://)
                if (uri.scheme.isNullOrEmpty() || (uri.scheme != "content" && uri.scheme != "file")) {
                    continue
                }

                val fileName = "image_${System.currentTimeMillis()}_$index.jpg"
                val imageRef = storageRef.child("$folderPath/$fileName")

                // ✅ Upload the file (works for both content:// and file://)
                imageRef.putFile(uri).await()

                // ✅ Get and store the download URL
                val downloadUrl = imageRef.downloadUrl.await().toString()
                downloadUrls.add(downloadUrl)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to upload $uriString: ${e.message}", e)
            }
        }

        return downloadUrls
    }

    private suspend fun uploadPdfsToStorage(
        pdfs: List<String>,
        folderPath: String
    ): List<String> {
        val downloadUrls = mutableListOf<String>()

        for ((index, uriString) in pdfs.withIndex()) {
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

                val fileName = "document_${'$'}{System.currentTimeMillis()}_${'$'}index.pdf"
                val fileRef = storageRef.child("${'$'}folderPath/${'$'}fileName")

                // Ensure correct content type for PDFs
                val metadata = StorageMetadata.Builder()
                    .setContentType("application/pdf")
                    .build()

                fileRef.putFile(uri, metadata).await()
                val downloadUrl = fileRef.downloadUrl.await().toString()
                downloadUrls.add(downloadUrl)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to upload PDF $uriString: ${e.message}", e)
            }
        }

        return downloadUrls
    }

    /**
     * Get all customers - now just returns the current StateFlow value
     * Use customers StateFlow directly in ViewModels instead of calling this
     * @deprecated Use customers StateFlow directly instead
     */
    @Deprecated("Use customers StateFlow directly instead")
    suspend fun getAllCustomers(): Result<List<Customer>> {
        return Result.success(_customers.value)
    }

    /**
     * Get all customer names - derived from StateFlow
     */
    suspend fun getAllCustomersNames(): Result<List<String>> {
        val names = _customers.value.map { it.name }
        return Result.success(names)
    }

    /**
     * Get customer by ID - first checks StateFlow, then fetches from Firestore if not found
     */
    suspend fun getCustomerById(customerId: String): Result<Customer> {
        // Check current StateFlow first
        val customer = _customers.value.find { it.customerId == customerId }
        if (customer != null) {
            Log.d(TAG, "📦 Found customer in StateFlow: ${customer.name}")
            return Result.success(customer)
        }

        // If not in StateFlow, fetch from Firestore (might be a new document)
        return try {
            val document = customerCollection.document(customerId).get().await()
            val fetchedCustomer = document.toObject(Customer::class.java)?.copy(customerId = document.id)

            if (fetchedCustomer != null) {
                Log.d(TAG, "✅ Customer found in Firestore: ${fetchedCustomer.name}")
                // Note: Listener will automatically update StateFlow when document is added
                Result.success(fetchedCustomer)
            } else {
                Result.failure(Exception("Customer not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching customer: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Unit> {
        return try {
            // ✅ Upload new customer photos (existing Firebase URLs are preserved, new local URIs are uploaded)
            val uploadedPhotoUrls = uploadImagesToStorage(
                images = customer.photoUrl,
                folderPath = "customers/${customer.customerId}/photos"
            )

            // ✅ Upload new ID proof PDFs (existing Firebase URLs are preserved, new local URIs are uploaded)
            val uploadedIdProofUrls = uploadPdfsToStorage(
                pdfs = customer.idProofImageUrls ?: emptyList(),
                folderPath = "customers/${customer.customerId}/id_proofs"
            )

            val customerData = hashMapOf(
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "photoUrl" to uploadedPhotoUrls,  // ✅ Use uploaded URLs (Firebase URLs for existing, new URLs for new uploads)
                "idProofType" to customer.idProofType,
                "idProofNumber" to customer.idProofNumber,
                "idProofImageUrls" to uploadedIdProofUrls, // ✅ Use uploaded URLs
                "amount" to customer.amount
            )

            customerCollection.document(customer.customerId)
                .update(customerData as Map<String, Any>)
                .await()

            // ✅ No need to update cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Customer updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating customer: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            customerCollection.document(customerId).delete().await()

            // ✅ No need to update cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Customer deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting customer: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get customer reference by customer ID
     */
    suspend fun getCustomerReference(customerId: String): Result<com.google.firebase.firestore.DocumentReference> {
        return try {
            val docRef = customerCollection.document(customerId)
            // Verify document exists
            val doc = docRef.get().await()
            if (doc.exists()) {
                Result.success(docRef)
            } else {
                Result.failure(Exception("Customer with ID $customerId not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting customer reference: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Clear error state (useful for UI error handling)
     */
    fun clearError() {
        _error.value = null
    }
}