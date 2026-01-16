package com.example.cardealer2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

object AppPreferences {
    private val PUNJABI_ENABLED_KEY = booleanPreferencesKey("punjabi_enabled")
    
    // Company data keys
    private val COMPANY_NAME_KEY = stringPreferencesKey("company_name")
    private val COMPANY_PHONE_KEY = stringPreferencesKey("company_phone")
    private val COMPANY_NAME_OF_OWNER_KEY = stringPreferencesKey("company_name_of_owner")
    private val COMPANY_PHONE_NUMBER_KEY = stringPreferencesKey("company_phone_number")
    private val COMPANY_EMAIL_KEY = stringPreferencesKey("company_email")
    private val COMPANY_GSTIN_KEY = stringPreferencesKey("company_gstin")
    
    fun isPunjabiEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PUNJABI_ENABLED_KEY] ?: false
        }
    }
    
    suspend fun setPunjabiEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PUNJABI_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Save company data to local cache
     */
    suspend fun saveCompanyData(context: Context, company: Company) {
        context.dataStore.edit { preferences ->
            preferences[COMPANY_NAME_KEY] = company.name
            preferences[COMPANY_PHONE_KEY] = company.phone
            preferences[COMPANY_NAME_OF_OWNER_KEY] = company.nameOfOwner
            preferences[COMPANY_PHONE_NUMBER_KEY] = company.phoneNumber
            preferences[COMPANY_EMAIL_KEY] = company.email
            preferences[COMPANY_GSTIN_KEY] = company.gstin
        }
    }
    
    /**
     * Load company data from local cache (Flow)
     */
    fun getCompanyData(context: Context): Flow<Company> {
        return context.dataStore.data.map { preferences ->
            Company(
                name = preferences[COMPANY_NAME_KEY] ?: "",
                phone = preferences[COMPANY_PHONE_KEY] ?: "",
                nameOfOwner = preferences[COMPANY_NAME_OF_OWNER_KEY] ?: "",
                phoneNumber = preferences[COMPANY_PHONE_NUMBER_KEY] ?: "",
                email = preferences[COMPANY_EMAIL_KEY] ?: "",
                gstin = preferences[COMPANY_GSTIN_KEY] ?: ""
            )
        }
    }
    
    /**
     * Get company data synchronously (for immediate access)
     */
    suspend fun getCompanyDataSync(context: Context): Company {
        val preferences = context.dataStore.data.first()
        return Company(
            name = preferences[COMPANY_NAME_KEY] ?: "",
            phone = preferences[COMPANY_PHONE_KEY] ?: "",
            nameOfOwner = preferences[COMPANY_NAME_OF_OWNER_KEY] ?: "",
            phoneNumber = preferences[COMPANY_PHONE_NUMBER_KEY] ?: "",
            email = preferences[COMPANY_EMAIL_KEY] ?: "",
            gstin = preferences[COMPANY_GSTIN_KEY] ?: ""
        )
    }
}

