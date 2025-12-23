package com.example.cardealer2.ViewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Customer
import com.example.cardealer2.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerFormUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val shouldNavigateBack: Boolean = false,
    val nameError: String? = null,
    val phoneError: String? = null,
    val addressError: String? = null,
    val photoError: String? = null,
    val idProofTypeError: String? = null,
    val idProofNumberError: String? = null,
    val idProofImageError: String? = null,
    val amountError: String? = null
)

class CustomerFormViewModel(
    private val repository: CustomerRepository = CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerFormUiState())
    val uiState: StateFlow<CustomerFormUiState> = _uiState

    private val _customer = MutableStateFlow<Customer?>(null)
    val customer: StateFlow<Customer?> = _customer.asStateFlow()

    /**
     * Load customer by ID - first checks repository StateFlow, then queries if needed
     * Repository listener automatically updates StateFlow when customer changes in Firestore
     */
    fun loadCustomer(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            try {
                // First check repository StateFlow
                val customerFromStateFlow = repository.customers.value.find { it.customerId == customerId }
                if (customerFromStateFlow != null) {
                    _customer.value = customerFromStateFlow
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    // If not in StateFlow, query repository (which checks StateFlow first)
                    val result = repository.getCustomerById(customerId)
                    result.onSuccess { c ->
                        _customer.value = c
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Failed to load customer")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Failed to load customer")
            }
        }
    }

    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        photoUris: List<Uri>,
        idProofType: String,
        idProofNumber: String,
        idProofImageUrls: List<String>,
        amount: Int
    ) {
        clearMessages()
        if (!validateInputs(name, phone, address, photoUris, idProofType, idProofNumber, idProofImageUrls, amount)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val customer = Customer(
                    customerId = "",
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    photoUrl = photoUris.map { it.toString() },
                    idProofType = idProofType,
                    idProofNumber = idProofNumber.trim(),
                    idProofImageUrls = idProofImageUrls,
                    amount = amount,
                    createdAt = System.currentTimeMillis()
                )
                val result = repository.addCustomer(customer)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Customer added successfully!", shouldNavigateBack = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Failed to add customer")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun updateCustomer(
        customerId: String,
        name: String,
        phone: String,
        address: String,
        photoUrls: List<String>,
        idProofType: String,
        idProofNumber: String,
        idProofImageUrls: List<String>,
        amount: Int
    ) {
        clearMessages()
        // Validate with empty photo list since edits may keep existing URLs
        if (!validateInputs(name, phone, address, emptyList(), idProofType, idProofNumber, idProofImageUrls, amount)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val customer = Customer(
                    customerId = customerId,
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    photoUrl = photoUrls,
                    idProofType = idProofType,
                    idProofNumber = idProofNumber.trim(),
                    idProofImageUrls = idProofImageUrls,
                    amount = amount,
                    createdAt = _customer.value?.createdAt ?: System.currentTimeMillis()
                )
                val result = repository.updateCustomer(customer)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Customer updated successfully!", shouldNavigateBack = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Failed to update customer")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, successMessage = null, errorMessage = null)
            try {
                val result = repository.deleteCustomer(customerId)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Customer deleted successfully", shouldNavigateBack = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete customer")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Unexpected error while deleting")
            }
        }
    }

    private fun validateInputs(
        name: String,
        phone: String,
        address: String,
        photoUris: List<Uri>,
        idProofType: String,
        idProofNumber: String,
        idProofImageUrls: List<String>,
        amount: Int
    ): Boolean {
        var isValid = true
        val errors = mutableMapOf<String, String>()

        if (name.isBlank()) { errors["name"] = "Customer name is required"; isValid = false }
        else if (name.trim().length < 2) { errors["name"] = "Name must be at least 2 characters"; isValid = false }

        if (phone.isBlank()) { errors["phone"] = "Phone number is required"; isValid = false }
        else if (!phone.trim().matches(Regex("^[0-9]{10}$"))) { errors["phone"] = "Phone number must be 10 digits"; isValid = false }

        if (address.isBlank()) { errors["address"] = "Address is required"; isValid = false }
        else if (address.trim().length < 10) { errors["address"] = "Address must be at least 10 characters"; isValid = false }

        if (idProofType.isBlank()) { errors["idProofType"] = "ID proof type is required"; isValid = false }

        if (idProofNumber.isBlank()) { errors["idProofNumber"] = "ID proof number is required"; isValid = false }
        else if (idProofNumber.trim().length < 5) { errors["idProofNumber"] = "ID proof number must be at least 5 characters"; isValid = false }

        if (idProofImageUrls.isEmpty()) { errors["idProofImage"] = "Please select at least one ID proof document"; isValid = false }

        _uiState.value = _uiState.value.copy(
            nameError = errors["name"],
            phoneError = errors["phone"],
            addressError = errors["address"],
            photoError = errors["photo"],
            idProofTypeError = errors["idProofType"],
            idProofNumberError = errors["idProofNumber"],
            idProofImageError = errors["idProofImage"],
            amountError = errors["amount"]
        )
        return isValid
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}


