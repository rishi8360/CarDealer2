package com.example.cardealer2.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.cardealer2.data.Company
import com.example.cardealer2.data.AppPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object CompanyRepository {
    @SuppressLint("StaticFieldLeak")
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestoreSettings = settings
    }
    
    private const val TAG = "CompanyRepository"
    private const val COMPANY_COLLECTION = "Company"
    private const val COMPANY_DOC_ID = "companyInfo"
    
    private val companyCollection = db.collection(COMPANY_COLLECTION)
    
    // StateFlow for company data
    private val _company = MutableStateFlow<Company>(Company())
    val company: StateFlow<Company> = _company.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load company data from local cache first (instant), then fetch from Firestore
     * @param context Required for accessing local cache
     */
    suspend fun fetchCompanyData(context: Context): Result<Company> {
        return try {
            // Step 1: Load from local cache first (instant, works offline)
            val cachedCompany = AppPreferences.getCompanyDataSync(context)
            if (cachedCompany.name.isNotEmpty()) {
                _company.value = cachedCompany
                Log.d(TAG, "✅ Loaded company data from cache: ${cachedCompany.name}")
            }
            
            // Step 2: Try to fetch from Firestore (will use cache if offline)
            _isLoading.value = true
            _error.value = null
            
            val companyDocRef = companyCollection.document(COMPANY_DOC_ID)
            
            // Try network first, fallback to cache if offline
            val snapshot = try {
                companyDocRef.get(Source.SERVER).await()
            } catch (e: Exception) {
                // If network fails, try cache
                Log.d(TAG, "⚠️ Network unavailable, trying cache: ${e.message}")
                companyDocRef.get(Source.CACHE).await()
            }
            
            _isLoading.value = false
            
            if (snapshot.exists()) {
                val company = snapshot.toObject(Company::class.java) ?: Company()
                
                // Update local cache
                AppPreferences.saveCompanyData(context, company)
                
                // Update StateFlow
                _company.value = company
                
                Log.d(TAG, "✅ Company data fetched from Firestore: ${company.name}")
                Result.success(company)
            } else {
                // Document doesn't exist in Firestore
                if (cachedCompany.name.isNotEmpty()) {
                    // Use cached data if available
                    Log.d(TAG, "⚠️ Company document doesn't exist in Firestore, using cached data")
                    Result.success(cachedCompany)
                } else {
                    // No cache, use default
                    val defaultCompany = Company(name = "Car Dealer")
                    _company.value = defaultCompany
                    Log.d(TAG, "⚠️ No company data found, using default")
                    Result.success(defaultCompany)
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            
            // If Firestore fails, try to use cached data
            val cachedCompany = AppPreferences.getCompanyDataSync(context)
            if (cachedCompany.name.isNotEmpty()) {
                _company.value = cachedCompany
                Log.d(TAG, "✅ Using cached company data due to error: ${e.message}")
                Result.success(cachedCompany)
            } else {
                val errorMsg = "Failed to fetch company data: ${e.message}"
                _error.value = errorMsg
                Log.e(TAG, "❌ $errorMsg", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update company data in Firestore and local cache
     */
    suspend fun updateCompanyData(context: Context, company: Company): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null
            
            if (company.name.isBlank()) {
                return Result.failure(Exception("Company name cannot be empty"))
            }
            
            val companyDocRef = companyCollection.document(COMPANY_DOC_ID)
            companyDocRef.set(company, com.google.firebase.firestore.SetOptions.merge()).await()
            
            // Update local cache
            AppPreferences.saveCompanyData(context, company)
            
            // Update StateFlow
            _company.value = company
            
            _isLoading.value = false
            Log.d(TAG, "✅ Company data updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            _isLoading.value = false
            
            // Even if Firestore update fails, save to cache
            try {
                AppPreferences.saveCompanyData(context, company)
                _company.value = company
                Log.d(TAG, "⚠️ Firestore update failed, but saved to cache: ${e.message}")
            } catch (cacheError: Exception) {
                Log.e(TAG, "❌ Failed to save to cache: ${cacheError.message}")
            }
            
            val errorMsg = "Failed to update company data: ${e.message}"
            _error.value = errorMsg
            Log.e(TAG, "❌ $errorMsg", e)
            Result.failure(e)
        }
    }
}

