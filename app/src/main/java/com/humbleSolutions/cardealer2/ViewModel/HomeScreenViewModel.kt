package com.humbleSolutions.cardealer2.ViewModel

import androidx.compose.material3.DrawerValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humbleSolutions.cardealer2.data.Brand
import com.humbleSolutions.cardealer2.repository.VehicleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // 🔹 Drawer state management
    private val _drawerState = MutableStateFlow(DrawerValue.Closed)
    val drawerState: StateFlow<DrawerValue> = _drawerState
    
    private val _canOpenDrawer = MutableStateFlow(true)
    val canOpenDrawer: StateFlow<Boolean> = _canOpenDrawer

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
     * Open drawer
     */
    fun openDrawer() {
        if (_canOpenDrawer.value) {
            _drawerState.value = DrawerValue.Open
        }
    }

    /**
     * Close drawer
     */
    fun closeDrawer() {
        _drawerState.value = DrawerValue.Closed
    }

    /**
     * Reset drawer state when returning to home screen
     */
    fun resetDrawerState() {
        _canOpenDrawer.value = false
        _drawerState.value = DrawerValue.Closed
        viewModelScope.launch {
            delay(150)
            _canOpenDrawer.value = true
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