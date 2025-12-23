package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Brand
import com.example.cardealer2.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for selected brand
 * Uses repository StateFlow for brands - automatically updates when Firestore changes
 */
class BrandSelectedViewModel(
    private val repository: VehicleRepository = VehicleRepository
) : ViewModel() {

    private val _brand = MutableStateFlow<Brand?>(null)
    val brand: StateFlow<Brand?> = _brand

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Load brand by ID - first checks provided list, then repository StateFlow, then queries if needed
     */
    fun loadBrandById(brandId: String, brands: List<Brand>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // First try to find in provided list
                var selectedBrand = brands.find { it.brandId == brandId }

                if (selectedBrand != null) {
                    _brand.value = selectedBrand
                } else {
                    // If not in provided list, check repository StateFlow
                    selectedBrand = repository.brands.value.find { it.brandId == brandId }
                    if (selectedBrand != null) {
                        _brand.value = selectedBrand
                    } else {
                        // Finally, query repository (which checks StateFlow first)
                        val result = repository.getBrandById(brandId)
                        result.onSuccess { brand ->
                            _brand.value = brand
                        }.onFailure { error ->
                            _error.value = error.message ?: "Brand not found with ID: $brandId"
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
