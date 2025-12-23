package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Product
import com.example.cardealer2.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for brand vehicles
 * Uses repository StateFlow for products - automatically updates when Firestore changes
 */
class BrandVehicleViewModel(
    private val repository: VehicleRepository = VehicleRepository
) : ViewModel() {

    // Track current brandId and productId being viewed
    private val _currentBrandId = MutableStateFlow<String?>(null)
    private val _currentProductId = MutableStateFlow<String?>(null)

    // Observe repository products StateFlow and filter by brandId/productId
    // This ensures UI updates automatically when products change in Firestore
    private val _product = MutableStateFlow<List<Product>>(emptyList())
    val product: StateFlow<List<Product>> = _product

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Observe repository products StateFlow and automatically update local products
        // when the repository StateFlow changes (e.g., when products are updated)
        viewModelScope.launch {
            combine(
                repository.products,
                _currentBrandId,
                _currentProductId
            ) { products, brandId, productId ->
                if (brandId != null && productId != null) {
                    products.filter {
                        it.brandId == brandId && it.productId == productId
                    }
                } else {
                    emptyList()
                }
            }.collect { filteredProducts ->
                if (_currentBrandId.value != null && _currentProductId.value != null) {
                    _product.value = filteredProducts
                    _error.value = null
                }
            }
        }
    }

    /**
     * Load products by brand and model - starts listener and sets current filters
     * The init block will automatically update _product when repository StateFlow changes
     */
    fun loadProductByModelNameBrandName(productId: String, brandId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentBrandId.value = brandId
            _currentProductId.value = productId
            
            try {
                // Start listener for this brand and product
                repository.startListeningToProductsByBrandIdAndProductId(brandId, productId)
                
                // First check repository StateFlow
                val filteredProducts = repository.products.value.filter {
                    it.brandId == brandId && it.productId == productId
                }
                
                if (filteredProducts.isNotEmpty()) {
                    _product.value = filteredProducts
                    _isLoading.value = false
                } else {
                    // If not in StateFlow yet, query repository (which will start listener)
                    val result = repository.getProductByBrandIdProductId(brandId = brandId, productId = productId)
                    if (result.isSuccess) {
                        _product.value = result.getOrNull() ?: emptyList()
                    } else {
                        _error.value = result.exceptionOrNull()?.message
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }
}
