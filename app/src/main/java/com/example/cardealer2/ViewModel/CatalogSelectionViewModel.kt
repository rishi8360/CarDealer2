package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Brand
import com.example.cardealer2.data.CatalogProduct
import com.example.cardealer2.data.Product
import com.example.cardealer2.data.VehicleSummary
import com.example.cardealer2.repository.VehicleRepository
import com.example.cardealer2.utils.CatalogHtmlTemplate
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProductWithPrice(
    val product: Product,
    val productRef: DocumentReference,
    var sellingPrice: Int
)

data class FilterState(
    val selectedColor: String? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val chassisNumber: String? = null,
    val condition: String? = null,
    val minKms: Int? = null,
    val maxKms: Int? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null
) {
    fun hasActiveFilters(): Boolean {
        return selectedColor != null || minPrice != null || maxPrice != null || 
               chassisNumber != null || condition != null ||
               minKms != null || maxKms != null || minYear != null || maxYear != null
    }
    
    fun getActiveFilterCount(): Int {
        var count = 0
        if (selectedColor != null) count++
        if (minPrice != null) count++
        if (maxPrice != null) count++
        if (!chassisNumber.isNullOrBlank()) count++
        if (condition != null) count++
        if (minKms != null) count++
        if (maxKms != null) count++
        if (minYear != null) count++
        if (maxYear != null) count++
        return count
    }
}

data class CatalogSelectionState(
    val brands: List<Brand> = emptyList(),
    val selectedBrandIds: Set<String> = emptySet(),
    val selectedVehicles: Map<String, Set<String>> = emptyMap(), // brandId -> Set of productIds (models)
    val selectedIndividualVehicles: Set<String> = emptySet(), // Set of chassis numbers for selected individual vehicles
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGeneratingCatalog: Boolean = false,
    val catalogGenerationError: String? = null,
    val productsForPriceEditing: List<ProductWithPrice> = emptyList(),
    val individualVehiclesForSelection: List<Product> = emptyList(), // All individual vehicles for selection step
    val isLoadingProducts: Boolean = false,
    val isLoadingIndividualVehicles: Boolean = false,
    val recipientName: String? = null,
    val catalogId: String? = null,
    val filterState: FilterState = FilterState()
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

    /**
     * Select all vehicles (models) for a specific brand
     * Used in Vehicle Selection screen
     */
    fun selectAllVehiclesForBrand(brandId: String) {
        _state.update { state ->
            val brand = state.brands.find { it.brandId == brandId } ?: return@update state
            val allVehicleIds = brand.vehicle.map { it.productId }.toSet()
            val newSelectedVehicles = state.selectedVehicles.toMutableMap().apply {
                put(brandId, allVehicleIds)
            }
            val newSelectedBrands = if (allVehicleIds.isNotEmpty()) {
                state.selectedBrandIds + brandId
            } else {
                state.selectedBrandIds
            }
            state.copy(
                selectedBrandIds = newSelectedBrands,
                selectedVehicles = newSelectedVehicles
            )
        }
    }

    /**
     * Select all filtered individual vehicles
     * Used in Individual Vehicle Selection screen - only selects vehicles that match current filters
     */
    fun selectAllFilteredIndividualVehicles() {
        val currentState = _state.value
        val allVehicles = currentState.individualVehiclesForSelection
        val filters = currentState.filterState
        
        // Apply filters manually (same logic as getFilteredIndividualVehicles)
        val filteredVehicles = if (!filters.hasActiveFilters()) {
            allVehicles
        } else {
            allVehicles.filter { product ->
                // Color filter
                if (filters.selectedColor != null && !filters.selectedColor.isBlank()) {
                    if (!product.colour.equals(filters.selectedColor, ignoreCase = true)) {
                        return@filter false
                    }
                }
                
                // Price range filters (using sellingPrice)
                if (filters.minPrice != null && product.sellingPrice < filters.minPrice) {
                    return@filter false
                }
                if (filters.maxPrice != null && product.sellingPrice > filters.maxPrice) {
                    return@filter false
                }
                
                // Chassis number filter
                if (!filters.chassisNumber.isNullOrBlank()) {
                    if (!product.chassisNumber.contains(filters.chassisNumber, ignoreCase = true)) {
                        return@filter false
                    }
                }
                
                // Condition filter
                if (filters.condition != null && !filters.condition.isBlank()) {
                    if (!product.condition.equals(filters.condition, ignoreCase = true)) {
                        return@filter false
                    }
                }
                
                // KM range filters
                if (filters.minKms != null && product.kms < filters.minKms) {
                    return@filter false
                }
                if (filters.maxKms != null && product.kms > filters.maxKms) {
                    return@filter false
                }
                
                // Year range filters
                if (filters.minYear != null && product.year < filters.minYear) {
                    return@filter false
                }
                if (filters.maxYear != null && product.year > filters.maxYear) {
                    return@filter false
                }
                
                true
            }
        }
        
        val filteredChassisNumbers = filteredVehicles.map { it.chassisNumber }.toSet()
        _state.update { it.copy(selectedIndividualVehicles = filteredChassisNumbers) }
    }

    /**
     * Reset all product prices to their original selling prices
     * Used in Price Editing screen
     */
    fun resetAllPricesToOriginal() {
        _state.update { state ->
            val updatedProducts = state.productsForPriceEditing.map { productWithPrice ->
                productWithPrice.copy(sellingPrice = productWithPrice.product.sellingPrice)
            }
            state.copy(productsForPriceEditing = updatedProducts)
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

    /**
     * Clear all vehicle selections for a specific brand
     * Used in Vehicle Selection screen
     */
    fun clearVehiclesForBrand(brandId: String) {
        _state.update { state ->
            val newSelectedVehicles = state.selectedVehicles.toMutableMap().apply {
                remove(brandId)
            }
            val newSelectedBrands = state.selectedBrandIds - brandId
            state.copy(
                selectedBrandIds = newSelectedBrands,
                selectedVehicles = newSelectedVehicles
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

    /**
     * Get unsold vehicles for a brand by checking actual Product data
     * If products aren't loaded yet, returns all vehicles (fallback)
     */
    fun getUnsoldVehiclesForBrand(brandId: String): List<VehicleSummary> {
        return getFilteredVehiclesForBrand(brandId, FilterState())
    }
    
    /**
     * Get filtered and unsold vehicles for a brand by checking actual Product data
     * Applies filters from filterState if provided
     */
    fun getFilteredVehiclesForBrand(brandId: String, filterState: FilterState? = null): List<VehicleSummary> {
        val brand = _state.value.brands.find { it.brandId == brandId } ?: return emptyList()
        val allProducts = repository.products.value
        val filters = filterState ?: _state.value.filterState
        
        // If no products loaded yet, return all vehicles (fallback)
        if (allProducts.isEmpty()) {
            return brand.vehicle
        }
        
        // Get all products for this brand
        val brandProducts = allProducts.filter { it.brandId == brandId && !it.sold }
        
        // Apply filters
        val filteredProducts = brandProducts.filter { product ->
            // Color filter
            if (filters.selectedColor != null && !filters.selectedColor.isBlank()) {
                if (!product.colour.equals(filters.selectedColor, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // Price range filters
            if (filters.minPrice != null && product.sellingPrice < filters.minPrice) {
                return@filter false
            }
            if (filters.maxPrice != null && product.sellingPrice > filters.maxPrice) {
                return@filter false
            }
            
            // Chassis number filter
            if (!filters.chassisNumber.isNullOrBlank()) {
                if (!product.chassisNumber.contains(filters.chassisNumber, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // Condition filter
            if (filters.condition != null && !filters.condition.isBlank()) {
                if (!product.condition.equals(filters.condition, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            true
        }
        
        val filteredProductIds = filteredProducts.map { it.productId }.toSet()
        
        // If we have products but none match, return empty list
        val brandProductIds = allProducts.filter { it.brandId == brandId }.map { it.productId }.toSet()
        if (brandProductIds.isNotEmpty() && filteredProductIds.isEmpty()) {
            return emptyList()
        }
        
        return brand.vehicle.filter { vehicle ->
            vehicle.productId in filteredProductIds
        }
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

    fun setRecipientName(name: String) {
        _state.update { it.copy(recipientName = name) }
    }
    
    fun setFilterState(filterState: FilterState) {
        _state.update { it.copy(filterState = filterState) }
    }
    
    fun clearFilters() {
        _state.update { it.copy(filterState = FilterState()) }
    }
    
    /**
     * Load all individual vehicles (Product objects) for selected models
     * Used in IndividualVehicleSelection step
     */
    suspend fun loadIndividualVehiclesForSelection() {
        _state.update { it.copy(isLoadingIndividualVehicles = true, error = null) }
        
        try {
            val allBrands = repository.brands.value
            val selectedBrandIds = _state.value.selectedBrandIds
            val selectedVehicles = _state.value.selectedVehicles
            
            if (selectedBrandIds.isEmpty() || selectedVehicles.isEmpty()) {
                _state.update { 
                    it.copy(
                        isLoadingIndividualVehicles = false,
                        individualVehiclesForSelection = emptyList(),
                        error = "No models selected"
                    ) 
                }
                return
            }

            val allIndividualVehicles = mutableListOf<Product>()

            // Process each selected brand
            for (brandId in selectedBrandIds) {
                val brand = allBrands.find { it.brandId == brandId } ?: continue
                val selectedModelNames = selectedVehicles[brandId] ?: continue

                if (selectedModelNames.isEmpty()) continue

                // Fetch products for this brand and models
                val productsResult = if (selectedModelNames.size == 1) {
                    repository.getProductByBrandIdProductId(brandId, selectedModelNames.first())
                } else {
                    repository.getProductsByBrandIdAndModels(brandId, selectedModelNames.toList())
                }

                val products = productsResult.getOrNull() ?: emptyList()

                // Filter only unsold products
                for (product in products) {
                    if (!product.sold) {
                        allIndividualVehicles.add(product)
                    }
                }
            }

            _state.update { 
                it.copy(
                    isLoadingIndividualVehicles = false,
                    individualVehiclesForSelection = allIndividualVehicles,
                    error = null
                ) 
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    isLoadingIndividualVehicles = false,
                    error = "Error loading individual vehicles: ${e.message}"
                ) 
            }
        }
    }
    
    /**
     * Get filtered individual vehicles based on FilterState
     * Used in IndividualVehicleSelection step
     */
    fun getFilteredIndividualVehicles(): List<Product> {
        val allVehicles = _state.value.individualVehiclesForSelection
        val filters = _state.value.filterState
        
        if (!filters.hasActiveFilters()) {
            return allVehicles
        }
        
        return allVehicles.filter { product ->
            // Color filter
            if (filters.selectedColor != null && !filters.selectedColor.isBlank()) {
                if (!product.colour.equals(filters.selectedColor, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // Price range filters (using sellingPrice)
            if (filters.minPrice != null && product.sellingPrice < filters.minPrice) {
                return@filter false
            }
            if (filters.maxPrice != null && product.sellingPrice > filters.maxPrice) {
                return@filter false
            }
            
            // Chassis number filter
            if (!filters.chassisNumber.isNullOrBlank()) {
                if (!product.chassisNumber.contains(filters.chassisNumber, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // Condition filter
            if (filters.condition != null && !filters.condition.isBlank()) {
                if (!product.condition.equals(filters.condition, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // KM range filters
            if (filters.minKms != null && product.kms < filters.minKms) {
                return@filter false
            }
            if (filters.maxKms != null && product.kms > filters.maxKms) {
                return@filter false
            }
            
            // Year range filters
            if (filters.minYear != null && product.year < filters.minYear) {
                return@filter false
            }
            if (filters.maxYear != null && product.year > filters.maxYear) {
                return@filter false
            }
            
            true
        }
    }
    
    /**
     * Toggle selection of an individual vehicle by chassis number
     */
    fun toggleIndividualVehicleSelection(chassisNumber: String) {
        _state.update { state ->
            val currentSelection = state.selectedIndividualVehicles
            val newSelection = if (currentSelection.contains(chassisNumber)) {
                currentSelection - chassisNumber
            } else {
                currentSelection + chassisNumber
            }
            state.copy(selectedIndividualVehicles = newSelection)
        }
    }
    
    /**
     * Clear all individual vehicle selections
     */
    fun clearIndividualVehicleSelections() {
        _state.update { it.copy(selectedIndividualVehicles = emptySet()) }
    }
    
    fun removeVehicleFromSelection(brandId: String, productId: String) {
        _state.update { state ->
            val currentVehicleSelection = state.selectedVehicles[brandId] ?: emptySet()
            val newVehicleSelection = currentVehicleSelection - productId
            
            val newSelectedVehicles = state.selectedVehicles.toMutableMap().apply {
                if (newVehicleSelection.isEmpty()) {
                    remove(brandId)
                } else {
                    put(brandId, newVehicleSelection)
                }
            }
            
            // Update brand selection: brand is selected if it has ANY vehicles selected
            val newSelectedBrands = if (newVehicleSelection.isNotEmpty()) {
                state.selectedBrandIds
            } else {
                state.selectedBrandIds - brandId
            }
            
            state.copy(
                selectedBrandIds = newSelectedBrands,
                selectedVehicles = newSelectedVehicles
            )
        }
    }

    /**
     * Load products for price editing screen
     * Only loads selected individual vehicles (by chassis number)
     * Uses individualVehiclesForSelection list and gets references for selected ones
     */
    suspend fun loadProductsForPriceEditing() {
        _state.update { it.copy(isLoadingProducts = true, error = null) }
        
        try {
            val selectedChassisNumbers = _state.value.selectedIndividualVehicles
            val allIndividualVehicles = _state.value.individualVehiclesForSelection
            
            if (selectedChassisNumbers.isEmpty()) {
                _state.update { 
                    it.copy(
                        isLoadingProducts = false,
                        productsForPriceEditing = emptyList(),
                        error = "No individual vehicles selected"
                    ) 
                }
                return
            }

            val productsWithPrices = mutableListOf<ProductWithPrice>()

            // Filter individualVehiclesForSelection by selected chassis numbers
            val selectedProducts = allIndividualVehicles.filter { 
                it.chassisNumber in selectedChassisNumbers 
            }

            // Get product references for selected products
            for (product in selectedProducts) {
                val productRefResult = repository.getProductReferenceByChassis(product.chassisNumber)
                productRefResult.getOrNull()?.let { productRef ->
                    productsWithPrices.add(
                        ProductWithPrice(
                            product = product,
                            productRef = productRef,
                            sellingPrice = product.sellingPrice
                        )
                    )
                }
            }

            _state.update { 
                it.copy(
                    isLoadingProducts = false,
                    productsForPriceEditing = productsWithPrices,
                    error = null
                ) 
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    isLoadingProducts = false,
                    error = "Error loading products: ${e.message}"
                ) 
            }
        }
    }

    /**
     * Get filtered products for price editing based on FilterState
     */
    fun getFilteredProductsForPriceEditing(): List<ProductWithPrice> {
        val allProducts = _state.value.productsForPriceEditing
        val filters = _state.value.filterState
        
        if (!filters.hasActiveFilters()) {
            return allProducts
        }
        
        return allProducts.filter { productWithPrice ->
            val product = productWithPrice.product
            
            // Color filter
            if (filters.selectedColor != null && !filters.selectedColor.isBlank()) {
                if (!product.colour.equals(filters.selectedColor, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // Price range filters (using sellingPrice)
            if (filters.minPrice != null && productWithPrice.sellingPrice < filters.minPrice) {
                return@filter false
            }
            if (filters.maxPrice != null && productWithPrice.sellingPrice > filters.maxPrice) {
                return@filter false
            }
            
            // Chassis number filter
            if (!filters.chassisNumber.isNullOrBlank()) {
                if (!product.chassisNumber.contains(filters.chassisNumber, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // Condition filter
            if (filters.condition != null && !filters.condition.isBlank()) {
                if (!product.condition.equals(filters.condition, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // KM range filters
            if (filters.minKms != null && product.kms < filters.minKms) {
                return@filter false
            }
            if (filters.maxKms != null && product.kms > filters.maxKms) {
                return@filter false
            }
            
            // Year range filters
            if (filters.minYear != null && product.year < filters.minYear) {
                return@filter false
            }
            if (filters.maxYear != null && product.year > filters.maxYear) {
                return@filter false
            }
            
            true
        }
    }
    
    /**
     * Update selling price for a specific product
     */
    fun updateProductSellingPrice(productRef: DocumentReference, newPrice: Int) {
        _state.update { state ->
            val updatedProducts = state.productsForPriceEditing.map { productWithPrice ->
                if (productWithPrice.productRef == productRef) {
                    productWithPrice.copy(sellingPrice = newPrice)
                } else {
                    productWithPrice
                }
            }
            state.copy(productsForPriceEditing = updatedProducts)
        }
    }

    /**
     * Generate catalog HTML using modified selling prices
     */
    suspend fun generateCatalogHtmlWithPrices(): Result<String> {
        _state.update { it.copy(isGeneratingCatalog = true, catalogGenerationError = null) }
        
        return try {
            val productsWithPrices = _state.value.productsForPriceEditing
            
            if (productsWithPrices.isEmpty()) {
                _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "No products selected") }
                return Result.failure(Exception("No products selected"))
            }

            // Group products by brand and model
            val brandModelProductsList = mutableListOf<CatalogHtmlTemplate.BrandModelProducts>()
            
            // Get brand info from repository
            val allBrands = repository.brands.value
            
            // Group products by brand
            val productsByBrand = productsWithPrices.groupBy { it.product.brandId }
            
            for ((brandId, products) in productsByBrand) {
                val brand = allBrands.find { it.brandId == brandId } ?: continue
                
                // Update selling prices in products
                val productsWithUpdatedPrices = products.map { productWithPrice ->
                    productWithPrice.product.copy(sellingPrice = productWithPrice.sellingPrice)
                }
                
                // Group by model (productId)
                val productsByModel = productsWithUpdatedPrices.groupBy { it.productId }
                
                brandModelProductsList.add(
                    CatalogHtmlTemplate.BrandModelProducts(
                        brandId = brandId,
                        brandLogo = brand.logo,
                        models = productsByModel
                    )
                )
            }

            if (brandModelProductsList.isEmpty()) {
                _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "No product data found") }
                return Result.failure(Exception("No product data found"))
            }

            // Create catalog in Firestore with product references, selling prices, and recipient name
            val recipientName = _state.value.recipientName ?: ""
            createCatalogInFirestoreWithPrices(productsWithPrices, recipientName)

            val htmlContent = CatalogHtmlTemplate.generateCatalogHtml(
                brandModelProducts = brandModelProductsList
            )

            Result.success(htmlContent)
        } catch (e: Exception) {
            _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "Error: ${e.message}") }
            Result.failure(e)
        }
    }

    /**
     * Create catalog document in Firestore with CatalogProduct list (productRef + sellingPrice) and recipient name
     */
    private suspend fun createCatalogInFirestoreWithPrices(
        productsWithPrices: List<ProductWithPrice>,
        recipientName: String
    ) {
        try {
            val catalogProducts = productsWithPrices.map { productWithPrice ->
                CatalogProduct(
                    productRef = productWithPrice.productRef,
                    sellingPrice = productWithPrice.sellingPrice
                )
            }
            
            if (catalogProducts.isNotEmpty()) {
                val catalogResult = repository.createCatalogWithProducts(catalogProducts, recipientName)
                catalogResult.fold(
                    onSuccess = { catalogId ->
                        android.util.Log.d("CatalogGeneration", "Catalog created in Firestore with ID: $catalogId")
                        _state.update { it.copy(catalogId = catalogId) }
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

    /**
     * Create catalog in Firestore and return catalog ID
     * This is used for sharing catalog link directly without generating HTML/PDF
     */
    suspend fun createCatalogAndGetId(): Result<String> {
        _state.update { it.copy(isGeneratingCatalog = true, catalogGenerationError = null) }
        
        return try {
            val productsWithPrices = _state.value.productsForPriceEditing
            
            if (productsWithPrices.isEmpty()) {
                _state.update { it.copy(isGeneratingCatalog = false, catalogGenerationError = "No products selected") }
                return Result.failure(Exception("No products selected"))
            }

            val recipientName = _state.value.recipientName ?: ""
            val catalogProducts = productsWithPrices.map { productWithPrice ->
                CatalogProduct(
                    productRef = productWithPrice.productRef,
                    sellingPrice = productWithPrice.sellingPrice
                )
            }
            
            val catalogResult = repository.createCatalogWithProducts(catalogProducts, recipientName)
            catalogResult.fold(
                onSuccess = { catalogId ->
                    android.util.Log.d("CatalogGeneration", "Catalog created in Firestore with ID: $catalogId")
                    _state.update { 
                        it.copy(
                            catalogId = catalogId,
                            isGeneratingCatalog = false
                        ) 
                    }
                    Result.success(catalogId)
                },
                onFailure = { exception ->
                    android.util.Log.e("CatalogGeneration", "Error creating catalog in Firestore: ${exception.message}", exception)
                    _state.update { 
                        it.copy(
                            isGeneratingCatalog = false,
                            catalogGenerationError = "Error creating catalog: ${exception.message}"
                        ) 
                    }
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("CatalogGeneration", "Error creating catalog: ${e.message}", e)
            _state.update { 
                it.copy(
                    isGeneratingCatalog = false,
                    catalogGenerationError = "Error: ${e.message}"
                ) 
            }
            Result.failure(e)
        }
    }
}



