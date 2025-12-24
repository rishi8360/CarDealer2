package com.example.cardealer2.ViewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Brand
import com.example.cardealer2.data.Product
import com.example.cardealer2.repository.VehicleRepository
import com.example.cardealer2.utility.ChassisValidationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddBrandVehicleUiState(
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

class AddBrandVehicleViewModel(
    private val repository: VehicleRepository = VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBrandVehicleUiState())
    val uiState: StateFlow<AddBrandVehicleUiState> = _uiState

    private val _modelsList = MutableStateFlow<List<String>>(emptyList())
    val modelsList = _modelsList.asStateFlow()
    private val _colourList = MutableStateFlow<List<String>>(emptyList())
    val colourList = _colourList.asStateFlow()

    private val _chassisNumber = MutableStateFlow("")
    val chassisNumber: StateFlow<String> = _chassisNumber.asStateFlow()

    private val _chassisValidationState = MutableStateFlow<ChassisValidationState>(ChassisValidationState.Idle)
    val chassisValidationState: StateFlow<ChassisValidationState> = _chassisValidationState.asStateFlow()

    private var currentBrandId: String? = null

    init {
        // 🔹 Observe brands StateFlow to automatically update models list when models change
        viewModelScope.launch {
            repository.brands.collect { brands ->
                currentBrandId?.let { brandId ->
                    val brand = brands.find { it.brandId == brandId }
                    brand?.let {
                        _modelsList.value = it.modelNames
                        println("✅ Models list automatically updated from StateFlow: ${it.modelNames}")
                    }
                }
            }
        }
    }

    fun updateChassisNumber(value: String) {
        _chassisNumber.value = value.uppercase().trim()
        // Reset validation state when user types
        if (_chassisValidationState.value !is ChassisValidationState.Idle) {
            _chassisValidationState.value = ChassisValidationState.Idle
        }
    }

    fun checkChassisNumber() {
        val chassisNum = _chassisNumber.value.trim()

        if (chassisNum.isBlank()) {
            _chassisValidationState.value = ChassisValidationState.Error("Please enter a chassis number")
            return
        }

        viewModelScope.launch {
            _chassisValidationState.value = ChassisValidationState.Checking

            try {
                val exists = repository.checkChassisNumberExists(chassisNum)
                _chassisValidationState.value = if (exists) {
                    ChassisValidationState.AlreadyExists
                } else {
                    ChassisValidationState.Available
                }
            } catch (e: Exception) {
                _chassisValidationState.value = ChassisValidationState.Error(
                    e.message ?: "Failed to check chassis number"
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
                println("✅ Models loaded from StateFlow: ${brand.modelNames}")
            } else {
                // If not in StateFlow or no models, query repository
                val result = repository.getModelsByBrandId(brandId)
                result.onSuccess { models ->
                    _modelsList.value = models
                    println("✅ Models loaded successfully: $models")
                }.onFailure { error ->
                    _modelsList.value = emptyList()
                    println("❌ Failed to load models: ${error.message}")
                }
            }
        }
    }
    fun loadColours() {
        viewModelScope.launch {
            val result = repository.getColours()
            result.onSuccess { colours ->
                _colourList.value = colours
                println("✅ Colours loaded successfully: $colours")
            }.onFailure { error ->
                _colourList.value = emptyList()
                println("❌ Failed to load colours: ${error.message}")
            }
        }
    }

    suspend fun addNewColour(newColour: String): Boolean {
        // Add the new colour to the list immediately (optimistic update)
        _colourList.value = _colourList.value + newColour

        // Save to backend
        return try {
            val result = repository.addColour(newColour)
            if (result.isSuccess) {
                println("✅ Added new colour: $newColour")
                true
            } else {
                // If backend save fails, remove from list
                _colourList.value = _colourList.value - newColour
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add colour: ${result.exceptionOrNull()?.message}"
                )
                println("❌ Failed to add colour: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            // If error occurs, remove from list
            _colourList.value = _colourList.value - newColour
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error adding colour: ${e.message}"
            )
            println("❌ Error adding colour: ${e.message}")
            false
        }
    }


    fun addNewModel(brandId: String, newModel: String) {
        viewModelScope.launch {
            // Add the new model to the list immediately (optimistic update)
            _modelsList.value = _modelsList.value + newModel

            // Save to backend
            try {
                val result = repository.addModelToBrand(brandId, newModel)
                if (result.isSuccess) {
                    // ✅ Success - listener will automatically update brands StateFlow
                    // The observer in init will automatically update _modelsList
                    println("✅ Added new model: $newModel to brand: $brandId")
                } else {
                    // If backend save fails, remove from list
                    _modelsList.value = _modelsList.value - newModel
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to add model: ${result.exceptionOrNull()?.message}"
                    )
                    println("❌ Failed to add model: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                // If error occurs, remove from list
                _modelsList.value = _modelsList.value - newModel
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error adding model: ${e.message}"
                )
                println("❌ Error adding model: ${e.message}")
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
                    println("✅ Successfully added new brand: ${newBrand.brandId}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to add brand: ${result.exceptionOrNull()?.message}"
                    )
                    println("❌ Failed to add brand: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error adding brand: ${e.message}"
                )
                println("❌ Error adding brand: ${e.message}")
            }
        }
    }



    fun addVehicle(
        brandId: String,
        modelName: String,
        colour: String,
        chassisNumber: String,
        condition: String,
        images: List<Uri>,
        kms: String,
        lastService: String,
        previousOwners: String,
        price: String,
        year: String,
        type: String
    ) {
        // Clear previous messages
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

        // Validate inputs
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
                // Ensure chassis number is unique before proceeding
                val chassisNumNormalized = chassisNumber.trim()
                val exists = repository.checkChassisNumberExists(chassisNumNormalized)
                if (exists) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        chassisNumberError = "Chassis number already exists"
                    )
                    return@launch
                }

                val product = Product(
                    brandId = brandId,
                    productId = modelName,
                    colour = colour,
                    chassisNumber = chassisNumber,
                    condition = condition,
                    images = emptyList(),
                    kms = kms.toIntOrNull() ?: 0,
                    lastService = lastService,
                    previousOwners = previousOwners.toIntOrNull() ?: 0,
                    price = price.toIntOrNull() ?: 0,
                    year = year.toIntOrNull() ?: 0,
                    type = type
                )

                val result = repository.addVehicleToBrand(
                    brandId = brandId,
                    product = product,
                    imageUris = images
                )

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Vehicle added successfully!",
                        shouldNavigateBack = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to add vehicle"
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
        modelName: String,
        colour: String,
        chassisNumber: String,
        condition: String,
        image: List<Uri>,
        kms: String,
        lastService: String,
        previousOwners: String,
        price: String,
        year: String
    ): Boolean {
        var isValid = true
        val errors = mutableMapOf<String, String>()

        // Required field validations
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

        // Numeric field validations
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

        // Optional numeric field validations
        if (previousOwners.isNotBlank()) {
            if (previousOwners.toIntOrNull() == null || previousOwners.toInt() < 0) {
                errors["previousOwners"] = "Previous owners must be a valid positive number"
                isValid = false
            }
        }

        // Update UI state with errors
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