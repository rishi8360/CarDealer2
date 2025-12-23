package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Brand
import com.example.cardealer2.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel that observes VehicleRepository's StateFlow for brands
 * No need for manual loading or cache invalidation - repository handles it via Firebase listeners
 */
class HomeScreenViewModel : ViewModel(){
    private val repository: VehicleRepository = VehicleRepository

    // 🔹 Directly expose repository's StateFlow - automatically updates when Firestore changes
    val brands: StateFlow<List<Brand>> = repository.brands
    
    // 🔹 Filtered brands - derived from brands StateFlow
    private val _filteredBrands = MutableStateFlow<List<Brand>>(emptyList())
    val filteredBrands: StateFlow<List<Brand>> = _filteredBrands

    // 🔹 Expose repository's loading and error states
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    init {
        // 🔹 Update filtered brands whenever brands change
        viewModelScope.launch {
            repository.brands.collect { brandsList ->
                _filteredBrands.value = brandsList
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        repository.clearError()
    }

    /**
     * Filter brands based on input query
     */
    fun filterBrands(input: String) {
        val query = input.trim()
        val currentBrands = brands.value
        _filteredBrands.value = if (query.isEmpty()) {
            currentBrands
        } else {
            currentBrands.filter { brand ->
                brand.brandId.contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * @deprecated No longer needed - brands are automatically loaded via Firebase listener in repository
     * Brands StateFlow updates automatically when Firestore changes
     */
    @Deprecated("Brands are automatically loaded via Firebase listener in repository")
    fun loadBrands(forceRefresh: Boolean = false) {
        // No-op: Repository listener handles this automatically
    }

    /**
     * @deprecated No longer needed - brands are automatically loaded via Firebase listener in repository
     */
    @Deprecated("Brands are automatically loaded via Firebase listener in repository")
    suspend fun loadBrandsSuspend(forceRefresh: Boolean = false) {
        // No-op: Repository listener handles this automatically
    }
}