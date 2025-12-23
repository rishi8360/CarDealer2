package com.example.cardealer2.ViewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Customer
import com.example.cardealer2.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AddCustomerUiState(
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

class AddCustomerViewModel(
    private val repository: CustomerRepository = CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCustomerUiState())
    val uiState: StateFlow<AddCustomerUiState> = _uiState

    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        photoUrl: List<Uri>,
        idProofType: String,
        idProofNumber: String,
        idProofImageUrls: List<String>, // Changed to List<String>
        amount: Int
    ) {
        // Clear previous errors
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null,
            nameError = null,
            phoneError = null,
            addressError = null,
            photoError = null,
            idProofTypeError = null,
            idProofNumberError = null,
            idProofImageError = null,
            amountError = null
        )

        // Validate inputs
        if (!validateInputs(
                name, phone, address, photoUrl, idProofType,
                idProofNumber, idProofImageUrls, amount
            )
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val customer = Customer(
                    customerId = "",
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    photoUrl = photoUrl.map{it.toString()},
                    idProofType = idProofType,
                    idProofNumber = idProofNumber.trim(),
                    idProofImageUrls = idProofImageUrls, // Now a list
                    amount = amount,
                    createdAt = System.currentTimeMillis()
                )

                val result = repository.addCustomer(customer)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Customer added successfully!",
                        shouldNavigateBack = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to add customer"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    private fun validateInputs(
        name: String,
        phone: String,
        address: String,
        photoUrl: List<Uri>,
        idProofType: String,
        idProofNumber: String,
        idProofImageUrls: List<String>, // Changed to List<String>
        amount: Int
    ): Boolean {
        var isValid = true
        val errors = mutableMapOf<String, String>()

        // Name validation
        if (name.isBlank()) {
            errors["name"] = "Customer name is required"
            isValid = false
        } else if (name.trim().length < 2) {
            errors["name"] = "Name must be at least 2 characters"
            isValid = false
        }

        // Phone validation
        if (phone.isBlank()) {
            errors["phone"] = "Phone number is required"
            isValid = false
        } else if (!phone.trim().matches(Regex("^[0-9]{10}$"))) {
            errors["phone"] = "Phone number must be 10 digits"
            isValid = false
        }

        // Address validation
        if (address.isBlank()) {
            errors["address"] = "Address is required"
            isValid = false
        } else if (address.trim().length < 10) {
            errors["address"] = "Address must be at least 10 characters"
            isValid = false
        }

        // Photo URL validation (optional)


        // ID Proof Type validation
        if (idProofType.isBlank()) {
            errors["idProofType"] = "ID proof type is required"
            isValid = false
        }

        // ID Proof Number validation
        if (idProofNumber.isBlank()) {
            errors["idProofNumber"] = "ID proof number is required"
            isValid = false
        } else if (idProofNumber.trim().length < 5) {
            errors["idProofNumber"] = "ID proof number must be at least 5 characters"
            isValid = false
        }

        // ID Proof Image URLs validation (optional but validate if provided)
        if (idProofImageUrls.isEmpty()) {
            errors["idProofImage"] = "Please select at least one ID proof document"
            isValid = false
        }

        // Amount validation


        // Update UI state with errors
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

    private fun isValidUrl(url: String): Boolean {
        return try {
            val urlPattern = android.util.Patterns.WEB_URL
            urlPattern.matcher(url).matches()
        } catch (e: Exception) {
            false
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
}