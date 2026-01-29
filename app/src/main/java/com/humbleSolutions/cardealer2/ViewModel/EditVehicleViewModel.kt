package com.humbleSolutions.cardealer2.ViewModel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humbleSolutions.cardealer2.data.Product
import com.humbleSolutions.cardealer2.data.Brand
import com.humbleSolutions.cardealer2.repository.VehicleRepository
import com.humbleSolutions.cardealer2.utility.ChassisValidationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditVehicleUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val shouldNavigateBack: Boolean = false,
    val modelNameError: String? = null,
    val colourError: String? = null,
    val chassisNumberError: String? = null,
    val conditionError: String? = null,
    val imageError: String? = null,
    val kmsError: String? = null,
    val lastServiceError: String? = null,
    val previousOwnersError: String? = null,
    val priceError: String? = null,
    val yearError: String? = null
)

class EditVehicleViewModel(
    private val repository: VehicleRepository = VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditVehicleUiState())
    val uiState: StateFlow<EditVehicleUiState> = _uiState

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _modelsList = MutableStateFlow<List<String>>(emptyList())
    val modelsList = _modelsList.asStateFlow()

    private val _chassisNumber = MutableStateFlow("")
    val chassisNumber: StateFlow<String> = _chassisNumber.asStateFlow()

    private val _chassisValidationState = MutableStateFlow<ChassisValidationState>(ChassisValidationState.Idle)
    val chassisValidationState: StateFlow<ChassisValidationState> = _chassisValidationState.asStateFlow()

    private var originalChassisForValidation: String? = null
    private var currentBrandId: String? = null

    init {
        // 🔹 Observe brands StateFlow to automatically update models list when models change
        viewModelScope.launch {
            repository.brands.collect { brands ->
                currentBrandId?.let { brandId ->
                    val brand = brands.find { it.brandId == brandId }
                    brand?.let {
                        _modelsList.value = it.modelNames
                    }
                }
            }
        }
    }

    fun loadVehicle(brandId: String, chassisNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = repository.getProductFeatureByChassis(chassisNumber)
            result.onSuccess { vehicleProduct ->
                _product.value = vehicleProduct
                _uiState.value = _uiState.value.copy(isLoading = false)

                vehicleProduct?.let { product ->
                    originalChassisForValidation = product.chassisNumber
                    _chassisNumber.value = product.chassisNumber
                    _chassisValidationState.value = ChassisValidationState.Idle
                }

                // Load models for this brand (sets currentBrandId and loads models)
                loadModels(brandId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load vehicle"
                )
            }
        }
    }

    fun updateChassisNumber(value: String) {
        _chassisNumber.value = value.uppercase().trim()
        if (_chassisValidationState.value !is ChassisValidationState.Idle) {
            _chassisValidationState.value = ChassisValidationState.Idle
        }
    }

    fun checkChassisNumber() {
        val current = _chassisNumber.value.trim()
        if (current.isBlank()) {
            _chassisValidationState.value = ChassisValidationState.Error("Please enter a chassis number")
            return
        }

        // If unchanged from original, consider it available for update
        if (originalChassisForValidation != null && current.equals(originalChassisForValidation, ignoreCase = true)) {
            _chassisValidationState.value = ChassisValidationState.Available
            return
        }

        viewModelScope.launch {
            _chassisValidationState.value = ChassisValidationState.Checking
            try {
                val exists = repository.checkChassisNumberExists(current)
                _chassisValidationState.value = if (exists) {
                    ChassisValidationState.AlreadyExists
                } else {
                    ChassisValidationState.Available
                }
            } catch (e: Exception) {
                _chassisValidationState.value = ChassisValidationState.Error(e.message ?: "Failed to check chassis number")
            }
        }
    }

    fun deleteVehicle(chassisNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, successMessage = null, errorMessage = null)
            try {
                val result = repository.deleteVehicleByChassis(chassisNumber)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Vehicle deleted successfully",
                        shouldNavigateBack = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete vehicle"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unexpected error while deleting"
                )
            }
        }
    }
    /**
     * Load models for a brand - first checks repository StateFlow, then queries if needed
     * Repository listener automatically updates brands StateFlow when models change
     */
    fun loadModels(brandId: String) {
        currentBrandId = brandId
        viewModelScope.launch {
            // First check repository StateFlow
            val brand = repository.brands.value.find { it.brandId == brandId }
            if (brand != null && brand.modelNames.isNotEmpty()) {
                _modelsList.value = brand.modelNames
            } else {
                // If not in StateFlow or no models, query repository
                val result = repository.getModelsByBrandId(brandId)
                result.onSuccess { models ->
                    _modelsList.value = models
                }.onFailure {
                    _modelsList.value = emptyList()
                }
            }
        }
    }

    fun addNewModel(brandId: String, newModel: String) {
        viewModelScope.launch {
            // Add the new model to the list immediately (optimistic update)
            _modelsList.value = _modelsList.value + newModel

            try {
                val result = repository.addModelToBrand(brandId, newModel)
                if (result.isFailure) {
                    // If backend save fails, remove from list
                    _modelsList.value = _modelsList.value - newModel
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to add model: ${result.exceptionOrNull()?.message}"
                    )
                } else {
                    // ✅ Success - listener will automatically update brands StateFlow
                    // The observer in init will automatically update _modelsList
                }
            } catch (e: Exception) {
                // If error occurs, remove from list
                _modelsList.value = _modelsList.value - newModel
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error adding model: ${e.message}"
                )
            }
        }
    }

    fun updateVehicle(
        originalChassisNumber: String,
        brandId: String,
        modelName: String,
        colour: String,
        chassisNumber: String,
        condition: String,
        images: List<String>,
        kms: String,
        lastService: String,
        previousOwners: String,
        price: String,
        year: String,
        type: String
    ) {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null,
            modelNameError = null,
            colourError = null,
            chassisNumberError = null,
            conditionError = null,
            imageError = null,
            kmsError = null,
            lastServiceError = null,
            previousOwnersError = null,
            priceError = null,
            yearError = null
        )

        if (!validateInputs(
                modelName, colour, chassisNumber, condition, images,
                kms, lastService, previousOwners, price, year
            )
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val updatedProduct = Product(
                    brandId = brandId,
                    productId = modelName,
                    colour = colour,
                    chassisNumber = chassisNumber,
                    condition = condition,
                    images = images,
                    kms = kms.toIntOrNull() ?: 0,
                    lastService = lastService,
                    previousOwners = previousOwners.toIntOrNull() ?: 0,
                    price = price.toIntOrNull() ?: 0,
                    year = year.toIntOrNull() ?: 0,
                    type = type.lowercase() // Normalize to lowercase before saving
                )

                val result = repository.updateVehicle(originalChassisNumber, updatedProduct)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Vehicle updated successfully!",
                        shouldNavigateBack = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to update vehicle"
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

    fun addNewBrand(newBrand: Brand) {
        viewModelScope.launch {
            try {
                val result = repository.addNewBrand(newBrand)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Brand ${newBrand.brandId} added successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to add brand: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error adding brand: ${e.message}"
                )
            }
        }
    }

    private fun validateInputs(
        modelName: String,
        colour: String,
        chassisNumber: String,
        condition: String,
        images: List<String>,
        kms: String,
        lastService: String,
        previousOwners: String,
        price: String,
        year: String
    ): Boolean {
        var isValid = true
        val errors = mutableMapOf<String, String>()

        if (modelName.isBlank()) {
            errors["modelName"] = "Model name is required"
            isValid = false
        }

        if (colour.isBlank()) {
            errors["colour"] = "Colour is required"
            isValid = false
        }

        if (chassisNumber.isBlank()) {
            errors["chassisNumber"] = "Chassis number is required"
            isValid = false
        }

        if (condition.isBlank()) {
            errors["condition"] = "Condition is required"
            isValid = false
        }

        if (kms.isNotBlank()) {
            if (kms.toIntOrNull() == null || kms.toInt() < 0) {
                errors["kms"] = "KMs must be a valid positive number"
                isValid = false
            }
        } else {
            errors["kms"] = "KMs is required"
            isValid = false
        }

        if (price.isNotBlank()) {
            if (price.toIntOrNull() == null || price.toInt() < 0) {
                errors["price"] = "Price must be a valid positive number"
                isValid = false
            }
        } else {
            errors["price"] = "Price is required"
            isValid = false
        }

        if (year.isNotBlank()) {
            val yearInt = year.toIntOrNull()
            if (yearInt == null || yearInt < 1900 || yearInt > 2030) {
                errors["year"] = "Year must be between 1900 and 2030"
                isValid = false
            }
        } else {
            errors["year"] = "Year is required"
            isValid = false
        }

        if (previousOwners.isNotBlank()) {
            if (previousOwners.toIntOrNull() == null || previousOwners.toInt() < 0) {
                errors["previousOwners"] = "Previous owners must be a valid positive number"
                isValid = false
            }
        }

        _uiState.value = _uiState.value.copy(
            modelNameError = errors["modelName"],
            colourError = errors["colour"],
            chassisNumberError = errors["chassisNumber"],
            conditionError = errors["condition"],
            imageError = errors["image"],
            kmsError = errors["kms"],
            lastServiceError = errors["lastService"],
            previousOwnersError = errors["previousOwners"],
            priceError = errors["price"],
            yearError = errors["year"]
        )

        return isValid
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
}