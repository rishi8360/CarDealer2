package com.humbleSolutions.cardealer2.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humbleSolutions.cardealer2.data.Company
import com.humbleSolutions.cardealer2.repository.CompanyRepository
import com.humbleSolutions.cardealer2.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val repository = CompanyRepository
    private val passwordRepository = PasswordRepository
    
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
    
    // Password management
    val password: StateFlow<String> = passwordRepository.password
    val isPasswordLoading: StateFlow<Boolean> = passwordRepository.isLoading
    val passwordError: StateFlow<String?> = passwordRepository.error
    
    private val _isPasswordSaving = MutableStateFlow(false)
    val isPasswordSaving: StateFlow<Boolean> = _isPasswordSaving.asStateFlow()
    
    private val _passwordSaveMessage = MutableStateFlow<String?>(null)
    val passwordSaveMessage: StateFlow<String?> = _passwordSaveMessage.asStateFlow()
    
    fun fetchPassword(context: Context) {
        viewModelScope.launch {
            passwordRepository.fetchPassword(context)
        }
    }
    
    fun verifyCurrentPassword(context: Context, currentPassword: String): Boolean {
        return try {
            kotlinx.coroutines.runBlocking {
                passwordRepository.verifyPassword(context, currentPassword)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun verifyCurrentPasswordAsync(context: Context, currentPassword: String): Boolean {
        return passwordRepository.verifyPassword(context, currentPassword)
    }
    
    fun updatePassword(context: Context, newPassword: String, confirmPassword: String, currentPassword: String? = null) {
        viewModelScope.launch {
            if (newPassword.isBlank()) {
                _passwordSaveMessage.value = "Password cannot be empty"
                return@launch
            }
            
            if (newPassword != confirmPassword) {
                _passwordSaveMessage.value = "Passwords do not match"
                return@launch
            }
            
            // If password exists and current password is provided, verify it
            val storedPassword = password.value
            if (storedPassword.isNotEmpty()) {
                if (currentPassword == null || currentPassword.isBlank()) {
                    _passwordSaveMessage.value = "Current password is required"
                    return@launch
                }
                
                val isValid = passwordRepository.verifyPassword(context, currentPassword)
                if (!isValid) {
                    _passwordSaveMessage.value = "Current password is incorrect"
                    return@launch
                }
            }
            
            _isPasswordSaving.value = true
            _passwordSaveMessage.value = null
            
            val result = passwordRepository.updatePassword(context, newPassword)
            _isPasswordSaving.value = false
            
            if (result.isSuccess) {
                _passwordSaveMessage.value = "Password updated successfully"
            } else {
                _passwordSaveMessage.value = "Failed to update password: ${result.exceptionOrNull()?.message}"
            }
        }
    }
    
    fun clearPasswordMessage() {
        _passwordSaveMessage.value = null
    }
}

