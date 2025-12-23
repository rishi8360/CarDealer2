package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Brand
import com.example.cardealer2.data.Product
import com.example.cardealer2.data.VehicleSummary
import com.example.cardealer2.repository.VehicleRepository
import com.example.cardealer2.utils.CatalogHtmlTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

data class CatalogSelectionState(
    val brands: List<Brand> = emptyList(),
    val selectedBrandIds: Set<String> = emptySet(),
    val selectedVehicles: Map<String, Set<String>> = emptyMap(), // brandId -> Set of productIds
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGeneratingCatalog: Boolean = false,
    val catalogGenerationError: String? = null
)

class CatalogSelectionViewModel : ViewModel() {
    private val _state = MutableStateFlow(CatalogSelectionState())
    val state: StateFlow<CatalogSelectionState> = _state
    val totalSelectedCount = state.map { it.selectedVehicles.values.sumOf { vehicles -> vehicles.size } }
    private val repository = VehicleRepository

    fun loadBrands(brands: List<Brand>) {
        _state.update { it.copy(brands = brands, error = null) }
    }

    fun toggleBrandSelection(brandId: String) {
        _state.update { state ->
            val brand = state.brands.find { it.brandId == brandId } ?: return@update state
            val isCurrentlySelected = state.selectedBrandIds.contains(brandId)

            if (isCurrentlySelected) {
                // Deselect brand and all its vehicles
                val newSelectedBrands = state.selectedBrandIds - brandId
                val newSelectedVehicles = state.selectedVehicles.toMutableMap().apply {
                    remove(brandId)
                }
                state.copy(
                    selectedBrandIds = newSelectedBrands,
                    selectedVehicles = newSelectedVehicles
                )
            } else {
                // Select brand and all its vehicles
                val allVehicleIds = brand.vehicle.map { it.productId }.toSet()
                val newSelectedBrands = state.selectedBrandIds + brandId
                val newSelectedVehicles = state.selectedVehicles.toMutableMap().apply {
                    put(brandId, allVehicleIds)
                }
                state.copy(
                    selectedBrandIds = newSelectedBrands,
                    selectedVehicles = newSelectedVehicles
                )
            }
        }
    }

    fun toggleVehicleSelection(brandId: String, productId: String) {
        _state.update { state ->
            val brand = state.brands.find { it.brandId == brandId } ?: return@update state
            val currentVehicleSelection = state.selectedVehicles[brandId] ?: emptySet()

            val isVehicleSelected = currentVehicleSelection.contains(productId)
            val newVehicleSelection = if (isVehicleSelected) {
                currentVehicleSelection - productId
            } else {
                currentVehicleSelection + productId
            }

            // Update the selected vehicles map
            val newSelectedVehicles = state.selectedVehicles.toMutableMap().apply {
                if (newVehicleSelection.isEmpty()) {
                    remove(brandId)
                } else {
                    put(brandId, newVehicleSelection)
                }
            }

            // Update brand selection: brand is selected if it has ANY vehicles selected
            val newSelectedBrands = if (newVehicleSelection.isNotEmpty()) {
                state.selectedBrandIds + brandId
            } else {
                state.selectedBrandIds - brandId
            }

            state.copy(
                selectedBrandIds = newSelectedBrands,
                selectedVehicles = newSelectedVehicles
            )
        }
    }

    fun selectAllBrands() {
        _state.update { state ->
            val allBrandIds = state.brands.map { it.brandId }.toSet()
            val allVehiclesMap = state.brands.associate { brand ->
                brand.brandId to brand.vehicle.map { it.productId }.toSet()
            }
            state.copy(
                selectedBrandIds = allBrandIds,
                selectedVehicles = allVehiclesMap
            )
        }
    }

    fun clearAllSelections() {
        _state.update { state ->
            state.copy(
                selectedBrandIds = emptySet(),
                selectedVehicles = emptyMap()
            )
        }
    }

    fun getSelectedBrands(): List<Brand> {
        return _state.value.brands.filter { it.brandId in _state.value.selectedBrandIds }
    }

    fun getSelectedVehiclesForBrand(brandId: String): List<VehicleSummary> {
        val brand = _state.value.brands.find { it.brandId == brandId } ?: return emptyList()
        val selectedProductIds = _state.value.selectedVehicles[brandId] ?: return emptyList()
        return brand.vehicle.filter { it.productId in selectedProductIds }
    }

    fun getTotalSelectedCount(): Int {
        return _state.value.selectedVehicles.values.sumOf { it.size }
    }

    fun getSelectedBrandsCount(): Int {
        return _state.value.selectedBrandIds.size
    }

    // Helper function to check if all vehicles of a brand are selected
    fun areAllVehiclesSelectedForBrand(brandId: String): Boolean {
        val brand = _state.value.brands.find { it.brandId == brandId } ?: return false
        val allVehicleIds = brand.vehicle.map { it.productId }.toSet()
        val selectedVehicleIds = _state.value.selectedVehicles[brandId] ?: emptySet()
        return allVehicleIds.isNotEmpty() && selectedVehicleIds.containsAll(allVehicleIds)
    }

    suspend fun generateCatalogHtml(): Result<String> {
        _state.update { it.copy(isGeneratingCatalog = true, catalogGenerationError = null) }
        
        return try {
            // Use repository brands StateFlow - automatically updated via Firebase listener
            val allBrands = repository.brands.value
            
            val selectedBrandIds = _state.value.selectedBrandIds
            if (selectedBrandIds.isEmpty()) {
                _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "Please select at least one brand") }
                return Result.failure(Exception("Please select at least one brand"))
            }

            val selectedBrands = allBrands.filter { it.brandId in selectedBrandIds }
            if (selectedBrands.isEmpty()) {
                _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "Selected brands not found") }
                return Result.failure(Exception("Selected brands not found"))
            }

            // Build selected vehicles map using fresh brand data
            val selectedVehiclesMap = selectedBrands.associate { brand ->
                val selectedProductIds = _state.value.selectedVehicles[brand.brandId] ?: emptySet()
                brand.brandId to brand.vehicle.filter { it.productId in selectedProductIds }
            }

            // Check if any vehicles are selected
            val totalVehicles = selectedVehiclesMap.values.sumOf { it.size }
            if (totalVehicles == 0) {
                _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "Please select at least one vehicle") }
                return Result.failure(Exception("Please select at least one vehicle"))
            }

            // Fetch full Product data organized by Brand -> Model -> Products
            val brandModelProductsList = mutableListOf<CatalogHtmlTemplate.BrandModelProducts>()

            // Process each brand sequentially to ensure proper error handling
            for (brand in selectedBrands) {
                val selectedVehicleSummaries = selectedVehiclesMap[brand.brandId] ?: emptyList()

                if (selectedVehicleSummaries.isEmpty()) continue

                // Get selected model names (productId = model name) from vehicle summaries
                val selectedModelNames = selectedVehicleSummaries.map { it.productId }.distinct().toList()

                if (selectedModelNames.isEmpty()) {
                    android.util.Log.w("CatalogGeneration", "No model names found for brand ${brand.brandId}")
                    continue
                }

                // Fetch ALL full Product objects for selected models
                // Use getProductsByBrandIdAndModels for efficiency when multiple models selected
                val allProductsResult = if (selectedModelNames.size == 1) {
                    // Single model - use getProductByBrandIdProductId
                    repository.getProductByBrandIdProductId(brand.brandId, selectedModelNames.first())
                } else {
                    // Multiple models - use getProductsByBrandIdAndModels (more efficient)
                    repository.getProductsByBrandIdAndModels(brand.brandId, selectedModelNames)
                }

                val allProducts = allProductsResult.getOrNull() ?: emptyList()

                if (allProducts.isNotEmpty()) {
                    // Verify we have full Product data (not VehicleSummary)
                    android.util.Log.d("CatalogGeneration", "Fetched ${allProducts.size} full Product objects for brand ${brand.brandId}")
                    // Log sample product to verify data completeness
                    if (allProducts.isNotEmpty()) {
                        val sampleProduct = allProducts.first()
                        android.util.Log.d("CatalogGeneration", "Sample product: model=${sampleProduct.productId}, chassis=${sampleProduct.chassisNumber}, price=${sampleProduct.price}, images=${sampleProduct.images.size}, kms=${sampleProduct.kms}")
                    }

                    // Group products by model (productId = model name)
                    val productsByModel = allProducts.groupBy { it.productId }
                    
                    // Verify all selected models have products
                    val modelsWithProducts = productsByModel.keys
                    val missingModels = selectedModelNames.filter { it !in modelsWithProducts }
                    if (missingModels.isNotEmpty()) {
                        android.util.Log.w("CatalogGeneration", "No products found for models: $missingModels in brand ${brand.brandId}")
                    }

                    brandModelProductsList.add(
                        CatalogHtmlTemplate.BrandModelProducts(
                            brandId = brand.brandId,
                            brandLogo = brand.logo,
                            models = productsByModel
                        )
                    )
                } else {
                    android.util.Log.w("CatalogGeneration", "No full Product data found for brand ${brand.brandId} with models: $selectedModelNames")
                    allProductsResult.exceptionOrNull()?.let { exception ->
                        android.util.Log.e("CatalogGeneration", "Error fetching products: ${exception.message}", exception)
                    }
                }
            }

            if (brandModelProductsList.isEmpty()) {
                _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "No product data found for selected items") }
                return Result.failure(Exception("No product data found for selected items"))
            }

            // Create catalog in Firestore with product references
            createCatalogInFirestore(selectedBrands, selectedVehiclesMap)

            val htmlContent = CatalogHtmlTemplate.generateCatalogHtml(
                brandModelProducts = brandModelProductsList
            )

            // Don't set isGeneratingCatalog to false here - keep it true for PDF generation
            Result.success(htmlContent)
        } catch (e: Exception) {
            _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "Error: ${e.message}") }
            Result.failure(e)
        }
    }
    
    // Create catalog document in Firestore with product IDs
    private suspend fun createCatalogInFirestore(
        selectedBrands: List<Brand>,
        selectedVehiclesMap: Map<String, List<VehicleSummary>>
    ) {
        try {
            val allProductIds = mutableListOf<String>()
            
            // Get product IDs for all selected brands and models
            for (brand in selectedBrands) {
                val selectedVehicleSummaries = selectedVehiclesMap[brand.brandId] ?: continue
                val selectedModelNames = selectedVehicleSummaries.map { it.productId }.distinct()
                
                if (selectedModelNames.isEmpty()) continue
                
                // Get product IDs using the new helper method
                val productIdsResult = repository.getProductIdsByBrandAndModels(
                    brand.brandId,
                    selectedModelNames
                )
                
                productIdsResult.getOrNull()?.let { ids ->
                    allProductIds.addAll(ids)
                }
            }
            
            // Create catalog document in Firestore
            if (allProductIds.isNotEmpty()) {
                val catalogResult = repository.createCatalog(allProductIds)
                catalogResult.fold(
                    onSuccess = { catalogId ->
                        android.util.Log.d("CatalogGeneration", "Catalog created in Firestore with ID: $catalogId")
                    },
                    onFailure = { exception ->
                        android.util.Log.e("CatalogGeneration", "Error creating catalog in Firestore: ${exception.message}", exception)
                    }
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("CatalogGeneration", "Error creating catalog: ${e.message}", e)
            // Don't fail the whole catalog generation if Firestore save fails
        }
    }

    fun setCatalogGenerationComplete() {
        _state.update { it.copy(isGeneratingCatalog = false) }
    }

    fun setCatalogGenerationError(error: String) {
        _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = error) }
    }

    fun clearCatalogGenerationError() {
        _state.update { it.copy(catalogGenerationError = null) }
    }
}



