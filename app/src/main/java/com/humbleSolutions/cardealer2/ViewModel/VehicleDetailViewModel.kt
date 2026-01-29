package com.humbleSolutions.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humbleSolutions.cardealer2.data.Product
import com.humbleSolutions.cardealer2.data.Customer
import com.humbleSolutions.cardealer2.data.Broker
import com.humbleSolutions.cardealer2.repository.VehicleRepository
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for vehicle details
 * Uses repository StateFlow for products - automatically updates when Firestore changes
 */
class VehicleDetailViewModel(
    private val repository: VehicleRepository = VehicleRepository
) : ViewModel() {

    // Track current chassis number being viewed
    private val _currentChassisNumber = MutableStateFlow<String?>(null)
    
    // Observe repository products StateFlow and filter by chassisNumber
    // This ensures UI updates automatically when product changes in Firestore
    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        // Observe repository products StateFlow and automatically update local product
        // when the repository StateFlow changes (e.g., when product is updated)
        viewModelScope.launch {
            combine(
                repository.products,
                _currentChassisNumber
            ) { products, chassisNumber ->
                if (chassisNumber != null) {
                    products.find { it.chassisNumber.equals(chassisNumber, ignoreCase = true) }
                } else {
                    null
                }
            }.collect { foundProduct ->
                if (foundProduct != null && _currentChassisNumber.value != null) {
                    _product.value = foundProduct
                    _error.value = null
                }
            }
        }
    }

    private val _customer = MutableStateFlow<Customer?>(null)
    val customer: StateFlow<Customer?> = _customer

    private val _broker = MutableStateFlow<Broker?>(null)
    val broker: StateFlow<Broker?> = _broker

    private val _isLoadingCustomer = MutableStateFlow(false)
    val isLoadingCustomer: StateFlow<Boolean> = _isLoadingCustomer

    private val _isLoadingBroker = MutableStateFlow(false)
    val isLoadingBroker: StateFlow<Boolean> = _isLoadingBroker

    private val _customerError = MutableStateFlow<String?>(null)
    val customerError: StateFlow<String?> = _customerError

    private val _brokerError = MutableStateFlow<String?>(null)
    val brokerError: StateFlow<String?> = _brokerError

    /**
     * Load product by chassis number - sets current chassis number and loads product
     * The init block will automatically update _product when repository StateFlow changes
     */
    fun loadProductFeatureByChassis(products: List<Product>, chassisNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val trimmedChassis = chassisNumber.trim()
            _currentChassisNumber.value = trimmedChassis
            
            try {
                // First try to find in provided list
                val foundProduct = products.find {
                    it.chassisNumber.equals(trimmedChassis, ignoreCase = true)
                }

                if (foundProduct != null) {
                    _product.value = foundProduct
                    _isLoading.value = false
                } else {
                    // If not in provided list, check repository StateFlow
                    val productFromStateFlow = repository.products.value.find {
                        it.chassisNumber.equals(trimmedChassis, ignoreCase = true)
                    }
                    if (productFromStateFlow != null) {
                        _product.value = productFromStateFlow
                        _isLoading.value = false
                    } else {
                        // Finally, query repository (which will check StateFlow first)
                        // Also try to start a listener for this product's brandId and productId if available
                        val result = repository.getProductFeatureByChassis(trimmedChassis)
                        result.onSuccess { product ->
                            _product.value = product
                            // Start listener for this product's brand and model
                            if (product != null && product.brandId.isNotEmpty() && product.productId.isNotEmpty()) {
                                repository.startListeningToProductsByBrandIdAndProductId(
                                    product.brandId,
                                    product.productId
                                )
                            }
                            _isLoading.value = false
                        }.onFailure { error ->
                            _error.value = error.message ?: "No product found with chassis number: $trimmedChassis"
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error finding product"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Reload product from repository - sets current chassis number
     * The init block will automatically update _product when repository StateFlow changes
     */
    fun reloadProductFromRepository(chassisNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val trimmedChassis = chassisNumber.trim()
            _currentChassisNumber.value = trimmedChassis
            
            try {
                // Repository will check StateFlow first, then query Firestore if needed
                val result = repository.getProductFeatureByChassis(trimmedChassis)
                result.onSuccess { product ->
                    _product.value = product
                    // Start listener for this product's brand and model
                    if (product != null && product.brandId.isNotEmpty() && product.productId.isNotEmpty()) {
                        repository.startListeningToProductsByBrandIdAndProductId(
                            product.brandId,
                            product.productId
                        )
                    }
                    _error.value = null
                    _isLoading.value = false
                }.onFailure { error ->
                    _error.value = error.message ?: "Failed to reload product"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error reloading product"
                _isLoading.value = false
            }
        }
    }

    fun loadCustomerByReference(customerRef: DocumentReference?) {
        if (customerRef == null) {
            _customerError.value = "Customer reference is null"
            return
        }
        
        viewModelScope.launch {
            _isLoadingCustomer.value = true
            _customerError.value = null
            try {
                val result = repository.getCustomerByReference(customerRef)
                result.onSuccess { customer ->
                    _customer.value = customer
                    _customerError.value = null
                }.onFailure { error ->
                    _customerError.value = error.message ?: "Failed to load customer"
                    _customer.value = null
                }
            } catch (e: Exception) {
                _customerError.value = e.message ?: "Error loading customer"
                _customer.value = null
            } finally {
                _isLoadingCustomer.value = false
            }
        }
    }

    fun loadBrokerByReference(brokerRef: DocumentReference?) {
        if (brokerRef == null) {
            _brokerError.value = "Broker reference is null"
            return
        }
        
        viewModelScope.launch {
            _isLoadingBroker.value = true
            _brokerError.value = null
            try {
                val result = repository.getBrokerByReference(brokerRef)
                result.onSuccess { broker ->
                    _broker.value = broker
                    _brokerError.value = null
                }.onFailure { error ->
                    _brokerError.value = error.message ?: "Failed to load broker"
                    _broker.value = null
                }
            } catch (e: Exception) {
                _brokerError.value = e.message ?: "Error loading broker"
                _broker.value = null
            } finally {
                _isLoadingBroker.value = false
            }
        }
    }

    fun clearCustomerData() {
        _customer.value = null
        _customerError.value = null
    }

    fun clearBrokerData() {
        _broker.value = null
        _brokerError.value = null
    }

}










