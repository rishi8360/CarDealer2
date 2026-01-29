package com.humbleSolutions.cardealer2.ViewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humbleSolutions.cardealer2.data.Brand
import com.humbleSolutions.cardealer2.data.Product
import com.humbleSolutions.cardealer2.repository.VehicleRepository
import com.humbleSolutions.cardealer2.utility.ChassisValidationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VehicleFormUiState(
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

class VehicleFormViewModel(
    private val repository: VehicleRepository = VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleFormUiState())
    val uiState: StateFlow<VehicleFormUiState> = _uiState

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _modelsList = MutableStateFlow<List<String>>(emptyList())
    val modelsList = _modelsList.asStateFlow()

    private val _colourList = MutableStateFlow<List<String>>(emptyList())
    val colourList = _colourList.asStateFlow()

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

    fun loadProduct(chassisNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = repository.getProductFeatureByChassis(chassisNumber)
            result.onSuccess { vehicleProduct ->
                _product.value = vehicleProduct
                _uiState.value = _uiState.value.copy(isLoading = false)
                vehicleProduct?.let { p ->
                    originalChassisForValidation = p.chassisNumber
                    _chassisNumber.value = p.chassisNumber
                    _chassisValidationState.value = ChassisValidationState.Idle
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load vehicle"
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

    fun loadColours() {
        viewModelScope.launch {
            val result = repository.getColours()
            result.onSuccess { colours ->
                _colourList.value = colours
            }.onFailure {
                _colourList.value = emptyList()
            }
        }
    }

    fun addNewColour(newColour: String) {

        // Optimistic UI update (instant UI response)
        _colourList.value = _colourList.value + newColour

        viewModelScope.launch {
            try {
                val result = repository.addColour(newColour)

                if (result.isSuccess) {
                    // Success → keep optimistic update
                    println("✅ Added new colour: $newColour")
                } else {
                    // Backend failed → rollback UI
                    _colourList.value = _colourList.value - newColour
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.exceptionOrNull()?.message
                    )
                    println("❌ Failed to add colour: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                // Exception → rollback UI
                _colourList.value = _colourList.value - newColour
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
                println("❌ Error adding colour: ${e.message}")
            }
        }
    }


    fun addNewModel(brandId: String, newModel: String) {
        viewModelScope.launch {
            // Add the new model to the list immediately (optimistic update)
            _modelsList.value = _modelsList.value + newModel
            try {
                val result = repository.addModelToBrand(brandId, newModel)
                if (result.isSuccess) {
                    // ✅ Success - listener will automatically update brands StateFlow
                    // The observer in init will automatically update _modelsList
                    println("✅ Added new model: $newModel to brand: $brandId")
                } else {
                    // If backend save fails, remove from list
                    _modelsList.value = _modelsList.value - newModel
                    _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
                    println("❌ Failed to add model: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                // If error occurs, remove from list
                _modelsList.value = _modelsList.value - newModel
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
                println("❌ Error adding model: ${e.message}")
            }
        }
    }

    fun addNewBrand(newBrand: Brand) {
        viewModelScope.launch {
            try {
                val result = repository.addNewBrand(newBrand)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(successMessage = "Brand ${newBrand.brandId} added successfully")
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
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
        if (originalChassisForValidation != null && current.equals(originalChassisForValidation, ignoreCase = true)) {
            _chassisValidationState.value = ChassisValidationState.Available
            return
        }
        viewModelScope.launch {
            _chassisValidationState.value = ChassisValidationState.Checking
            try {
                val exists = repository.checkChassisNumberExists(current)
                _chassisValidationState.value = if (exists) ChassisValidationState.AlreadyExists else ChassisValidationState.Available
            } catch (e: Exception) {
                _chassisValidationState.value = ChassisValidationState.Error(e.message ?: "Failed to check chassis number")
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
        sellingPrice: String,
        year: String,
        type: String,
        nocPdfs: List<String> = emptyList(),
        rcPdfs: List<String> = emptyList(),
        insurancePdfs: List<String> = emptyList(),
        brokerOrMiddleMan: String = "",
        owner: String = ""
    ) {
        clearMessages()
        if (!validateInputs(modelName, colour, chassisNumber, condition, kms, lastService, previousOwners, price, year)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val chassisNumNormalized = chassisNumber.trim()
                val exists = repository.checkChassisNumberExists(chassisNumNormalized)
                if (exists) {
                    _uiState.value = _uiState.value.copy(isLoading = false, chassisNumberError = "Chassis number already exists")
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
                    sellingPrice = sellingPrice.toIntOrNull() ?: 0,
                    year = year.toIntOrNull() ?: 0,
                    type = type.lowercase(), // Normalize to lowercase before saving
                    noc = emptyList(), // Will be uploaded
                    rc = emptyList(), // Will be uploaded
                    insurance = emptyList(), // Will be uploaded
                    brokerOrMiddleMan = brokerOrMiddleMan,
                    owner = owner
                )
                val result = repository.addVehicleToBrand(
                    brandId = brandId,
                    product = product,
                    imageUris = images,
                    nocPdfs = nocPdfs,
                    rcPdfs = rcPdfs,
                    insurancePdfs = insurancePdfs
                )
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Vehicle added successfully!", shouldNavigateBack = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Failed to add vehicle")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "An unexpected error occurred")
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
        images: List<Uri>,
        kms: String,
        lastService: String,
        previousOwners: String,
        price: String,
        sellingPrice: String,
        year: String,
        type: String,
        nocPdfs: List<String> = emptyList(),
        rcPdfs: List<String> = emptyList(),
        insurancePdfs: List<String> = emptyList(),
        vehicleOtherDocPdfs: List<String> = emptyList(),
        brokerOrMiddleMan: String = "",
        owner: String = ""
    ) {
        clearMessages()
        if (!validateInputs(modelName, colour, chassisNumber, condition, kms, lastService, previousOwners, price, year)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Convert image URIs to strings
                // Repository's uploadImagesToStorageFromStrings will:
                // - Keep Firebase URLs (http/https) as-is
                // - Upload local URIs (content://, file://) to Firebase Storage
                val imageStrings = images.map { it.toString() }

                val updatedProduct = Product(
                    brandId = brandId,
                    productId = modelName,
                    colour = colour,
                    chassisNumber = chassisNumber,
                    condition = condition,
                    images = imageStrings,
                    kms = kms.toIntOrNull() ?: 0,
                    lastService = lastService,
                    previousOwners = previousOwners.toIntOrNull() ?: 0,
                    price = price.toIntOrNull() ?: 0,
                    sellingPrice = sellingPrice.toIntOrNull() ?: 0,
                    year = year.toIntOrNull() ?: 0,
                    type = type.lowercase(), // Normalize to lowercase before saving
                    noc = nocPdfs,
                    rc = rcPdfs,
                    insurance = insurancePdfs,
                    vehicleOtherDoc = vehicleOtherDocPdfs,
                    brokerOrMiddleMan = brokerOrMiddleMan,
                    owner = owner
                )
                // Repository's updateVehicle will handle uploading new images/PDFs:
                // - uploadImagesToStorageFromStrings: uploads local image URIs, preserves Firebase URLs
                // - uploadPdfsToStorage: uploads local PDF URIs, preserves Firebase URLs
                val result = repository.updateVehicle(originalChassisNumber, updatedProduct)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Vehicle updated successfully!", shouldNavigateBack = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Failed to update vehicle")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun deleteVehicle(chassisNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, successMessage = null, errorMessage = null)
            try {
                val result = repository.deleteVehicleByChassis(chassisNumber)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Vehicle deleted successfully", shouldNavigateBack = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete vehicle")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Unexpected error while deleting")
            }
        }
    }

    private fun validateInputs(
        modelName: String,
        colour: String,
        chassisNumber: String,
        condition: String,
        kms: String,
        lastService: String,
        previousOwners: String,
        price: String,
        year: String
    ): Boolean {
        var isValid = true
        val errors = mutableMapOf<String, String>()

        if (modelName.isBlank()) { errors["modelName"] = "Model name is required"; isValid = false }
        if (colour.isBlank()) { errors["colour"] = "Colour is required"; isValid = false }
        if (chassisNumber.isBlank()) { errors["chassisNumber"] = "Chassis number is required"; isValid = false }
        if (condition.isBlank()) { errors["condition"] = "Condition is required"; isValid = false }

        if (kms.isNotBlank()) {
            if (kms.toIntOrNull() == null || kms.toInt() < 0) { errors["kms"] = "KMs must be a valid positive number"; isValid = false }
        } else { errors["kms"] = "KMs is required"; isValid = false }

        if (price.isNotBlank()) {
            if (price.toIntOrNull() == null || price.toInt() < 0) { errors["price"] = "Price must be a valid positive number"; isValid = false }
        } else { errors["price"] = "Price is required"; isValid = false }

        if (year.isNotBlank()) {
            val yearInt = year.toIntOrNull()
            if (yearInt == null || yearInt < 1900 || yearInt > 2030) { errors["year"] = "Year must be between 1900 and 2030"; isValid = false }
        } else { errors["year"] = "Year is required"; isValid = false }

        if (previousOwners.isNotBlank()) {
            if (previousOwners.toIntOrNull() == null || previousOwners.toInt() < 0) { errors["previousOwners"] = "Previous owners must be a valid positive number"; isValid = false }
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
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}


