package com.example.cardealer2.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Company
import com.example.cardealer2.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val repository = CompanyRepository
    
    // Expose company data from repository
    val company: StateFlow<Company> = repository.company
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error
    
    // UI state for editing
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()
    
    /**
     * Fetch company data (called from splash screen)
     * @param context Required for local cache access
     */
    fun fetchCompanyData(context: Context) {
        viewModelScope.launch {
            repository.fetchCompanyData(context)
        }
    }
    
    /**
     * Update company data
     * @param context Required for local cache access
     */
    fun updateCompanyData(context: Context, company: Company) {
        if (company.name.isBlank()) {
            _saveMessage.value = "Company name cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _isSaving.value = true
            _saveMessage.value = null
            
            val result = repository.updateCompanyData(context, company)
            
            _isSaving.value = false
            
            if (result.isSuccess) {
                _saveMessage.value = "Company information updated successfully"
            } else {
                _saveMessage.value = result.exceptionOrNull()?.message ?: "Failed to update company information"
            }
        }
    }
    
    /**
     * Clear save message
     */
    fun clearMessage() {
        _saveMessage.value = null
    }
}

