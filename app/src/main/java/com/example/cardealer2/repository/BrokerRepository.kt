package com.example.cardealer2.repository

import android.util.Log
import com.example.cardealer2.data.Broker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object BrokerRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Configure Firestore settings for better reliability on real devices
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Unlimited cache
            .build()
        firestoreSettings = settings
    }
    private val brokerCollection = db.collection("Broker")
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    private const val TAG = "BrokerRepository"

    // 🔹 StateFlow exposed to ViewModels - automatically updates when Firestore changes
    private val _brokers = MutableStateFlow<List<Broker>>(emptyList())
    val brokers: StateFlow<List<Broker>> = _brokers.asStateFlow()

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

        listenerRegistration = brokerCollection.addSnapshotListener { snapshot, error ->
            _isLoading.value = false

            if (error != null) {
                val errorMsg = error.message ?: "Error loading brokers"
                _error.value = errorMsg
                Log.e(TAG, "❌ Error in broker listener: $errorMsg", error)
                Log.e(TAG, "Error code: ${error.code}, Error details: ${error.cause?.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    Log.d(TAG, "📥 Received snapshot with ${snapshot.documents.size} documents")

                    val brokers = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Broker::class.java)?.copy(brokerId = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing broker document ${doc.id}: ${e.message}", e)
                            null // Skip this document and continue with others
                        }
                    }
                    _brokers.value = brokers
                    _error.value = null
                    Log.d(TAG, "✅ Broker listener updated: ${brokers.size} brokers")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing broker snapshot: ${e.message}", e)
                    _error.value = "Error processing brokers: ${e.message}"
                }
            } else {
                Log.w(TAG, "⚠️ Snapshot is null in broker listener")
            }
        }
        
        Log.d(TAG, "✅ Broker listener started successfully")
    }

    /**
     * Stop listening (useful for cleanup, though usually not needed since repository is singleton)
     */
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Clear error state (useful for UI error handling)
     */
    fun clearError() {
        _error.value = null
    }

    suspend fun addBroker(broker: Broker): Result<Unit> {
        return try {
            val newBrokerRef = brokerCollection.document()
            val brokerId = newBrokerRef.id

            // Upload ID proof PDFs
            val uploadedIdProofUrls = uploadPdfsToStorage(
                pdfs = broker.idProof,
                folderPath = "brokers/$brokerId/id_proofs"
            )

            // Upload broker bill PDFs
            val uploadedBrokerBillUrls = uploadPdfsToStorage(
                pdfs = broker.brokerBill,
                folderPath = "brokers/$brokerId/broker_bills"
            )

            // Create Firestore data map
            val brokerData = hashMapOf(
                "brokerId" to brokerId,
                "name" to broker.name,
                "phoneNumber" to broker.phoneNumber,
                "idProof" to uploadedIdProofUrls,
                "address" to broker.address,
                "brokerBill" to uploadedBrokerBillUrls,
                "amount" to broker.amount,
                "createdAt" to broker.createdAt
            )

            // Store in Firestore
            db.runTransaction { transaction ->
                transaction.set(newBrokerRef, brokerData)
            }.await()

            // ✅ No need to update cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Broker added successfully with ID: $brokerId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding broker: ${e.message}", e)
            Result.failure(e)
        }
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

                val uri = android.net.Uri.parse(uriString)
                if (uri.scheme.isNullOrEmpty() || (uri.scheme != "content" && uri.scheme != "file")) {
                    continue
                }

                val fileName = "document_${System.currentTimeMillis()}_$index.pdf"
                val fileRef = storageRef.child("$folderPath/$fileName")

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
     * Get all brokers - now just returns the current StateFlow value
     * Use brokers StateFlow directly in ViewModels instead of calling this
     * @deprecated Use brokers StateFlow directly instead
     */
    @Deprecated("Use brokers StateFlow directly instead")
    suspend fun getAllBrokers(): Result<List<Broker>> {
        return Result.success(_brokers.value)
    }

    /**
     * Get all broker names - derived from StateFlow
     */
    suspend fun getAllBrokersNames(): Result<List<String>> {
        val names = _brokers.value.map { it.name }
        return Result.success(names)
    }

    /**
     * Get broker by ID - first checks StateFlow, then fetches from Firestore if not found
     */
    suspend fun getBrokerById(brokerId: String): Result<Broker> {
        // Check current StateFlow first
        val broker = _brokers.value.find { it.brokerId == brokerId }
        if (broker != null) {
            Log.d(TAG, "📦 Found broker in StateFlow: ${broker.name}")
            return Result.success(broker)
        }

        // If not in StateFlow, fetch from Firestore (might be a new document)
        return try {
            val document = brokerCollection.document(brokerId).get().await()
            val fetchedBroker = document.toObject(Broker::class.java)?.copy(brokerId = document.id)

            if (fetchedBroker != null) {
                Log.d(TAG, "✅ Broker found in Firestore: ${fetchedBroker.name}")
                // Note: Listener will automatically update StateFlow when document is added
                Result.success(fetchedBroker)
            } else {
                Result.failure(Exception("Broker not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching broker: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateBroker(broker: Broker): Result<Unit> {
        return try {
            // Upload new ID proof PDFs (existing Firebase URLs are preserved, new local URIs are uploaded)
            val uploadedIdProofUrls = uploadPdfsToStorage(
                pdfs = broker.idProof,
                folderPath = "brokers/${broker.brokerId}/id_proofs"
            )

            // Upload new broker bill PDFs (existing Firebase URLs are preserved, new local URIs are uploaded)
            val uploadedBrokerBillUrls = uploadPdfsToStorage(
                pdfs = broker.brokerBill,
                folderPath = "brokers/${broker.brokerId}/broker_bills"
            )

            val brokerData = hashMapOf(
                "name" to broker.name,
                "phoneNumber" to broker.phoneNumber,
                "address" to broker.address,
                "idProof" to uploadedIdProofUrls,
                "brokerBill" to uploadedBrokerBillUrls,
                "amount" to broker.amount
            )

            brokerCollection.document(broker.brokerId)
                .update(brokerData as Map<String, Any>)
                .await()

            // ✅ No need to update cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Broker updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating broker: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteBroker(brokerId: String): Result<Unit> {
        return try {
            brokerCollection.document(brokerId).delete().await()

            // ✅ No need to update cache - listener will automatically update StateFlow
            Log.d(TAG, "✅ Broker deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting broker: ${e.message}", e)
            Result.failure(e)
        }
    }
}

