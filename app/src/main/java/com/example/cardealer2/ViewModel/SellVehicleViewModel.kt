package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.EmiDetails
import com.example.cardealer2.data.Product
import com.example.cardealer2.data.VehicleSale
import com.example.cardealer2.repository.CustomerRepository
import com.example.cardealer2.repository.SaleRepository
import com.example.cardealer2.repository.VehicleRepository
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class SellVehicleViewModel : ViewModel() {
    private val saleRepository = SaleRepository
    private val customerRepository = CustomerRepository
    private val vehicleRepository = VehicleRepository
    
    // Selected vehicle
    private val _selectedVehicle = MutableStateFlow<Product?>(null)
    val selectedVehicle: StateFlow<Product?> = _selectedVehicle.asStateFlow()
    
    // Selected customer
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    // Payment type
    private val _paymentType = MutableStateFlow<String?>(null) // "EMI" or "FULL_PAYMENT"
    val paymentType: StateFlow<String?> = _paymentType.asStateFlow()
    
    // Sale price (editable)
    private val _salePrice = MutableStateFlow("")
    val salePrice: StateFlow<String> = _salePrice.asStateFlow()
    
    // EMI details
    private val _interestRate = MutableStateFlow("")
    val interestRate: StateFlow<String> = _interestRate.asStateFlow()
    
    private val _frequency = MutableStateFlow("MONTHLY")
    val frequency: StateFlow<String> = _frequency.asStateFlow()
    
    private val _durationMonths = MutableStateFlow("")
    val durationMonths: StateFlow<String> = _durationMonths.asStateFlow()
    
    private val _calculatedEmi = MutableStateFlow(0.0)
    val calculatedEmi: StateFlow<Double> = _calculatedEmi.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()
    
    // Customers list for selection
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()
    
    init {
        loadCustomers()
    }
    
    /**
     * Load vehicle by chassis number
     */
    fun loadVehicle(chassisNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = vehicleRepository.getProductByChassisNumber(chassisNumber)
            result.fold(
                onSuccess = { product ->
                    _selectedVehicle.value = product
                    _salePrice.value = product.price.toString()
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load vehicle"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Load all customers
     */
    private fun loadCustomers() {
        viewModelScope.launch {
            customerRepository.customers.collect { customerList ->
                _customers.value = customerList
            }
        }
    }
    
    /**
     * Set payment type
     */
    fun setPaymentType(type: String) {
        _paymentType.value = type
        if (type == "EMI") {
            calculateEmi()
        }
    }
    
    /**
     * Set sale price
     */
    fun setSalePrice(value: String) {
        _salePrice.value = value
        if (_paymentType.value == "EMI") {
            calculateEmi()
        }
    }
    
    /**
     * Set interest rate
     */
    fun setInterestRate(rate: String) {
        _interestRate.value = rate
        calculateEmi()
    }
    
    /**
     * Set frequency
     */
    fun setFrequency(freq: String) {
        _frequency.value = freq
        calculateEmi()
    }
    
    /**
     * Set duration in months
     */
    fun setDurationMonths(months: String) {
        _durationMonths.value = months
        calculateEmi()
    }
    
    /**
     * Set selected customer
     */
    fun setSelectedCustomer(customer: Customer) {
        _selectedCustomer.value = customer
    }
    
    /**
     * Calculate EMI amount
     */
    private fun calculateEmi() {
        val principal = getSalePriceAmount()
        if (principal <= 0) {
            _calculatedEmi.value = 0.0
            return
        }
        
        if (_selectedVehicle.value == null) {
            _calculatedEmi.value = 0.0
            return
        }
        
        val rateStr = _interestRate.value
        val monthsStr = _durationMonths.value
        
        if (rateStr.isBlank() || monthsStr.isBlank()) {
            _calculatedEmi.value = 0.0
            return
        }
        
        val rate = rateStr.toDoubleOrNull() ?: 0.0
        val months = monthsStr.toIntOrNull() ?: 0
        
        if (rate <= 0 || months <= 0) {
            _calculatedEmi.value = 0.0
            return
        }
        
        // Convert annual rate to monthly rate
        val monthlyRate = (rate / 100.0) / 12.0
        
        // EMI formula: P * r * (1 + r)^n / ((1 + r)^n - 1)
        val emi = if (monthlyRate > 0) {
            principal * monthlyRate * Math.pow(1 + monthlyRate, months.toDouble()) /
                    (Math.pow(1 + monthlyRate, months.toDouble()) - 1)
        } else {
            principal / months
        }
        
        _calculatedEmi.value = emi
    }
    
    private fun getSalePriceAmount(): Double =
        _salePrice.value.toDoubleOrNull() ?: 0.0
    
    /**
     * Calculate next due date based on frequency
     */
    private fun calculateNextDueDate(frequency: String): Long {
        val calendar = Calendar.getInstance()
        when (frequency) {
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "QUARTERLY" -> calendar.add(Calendar.MONTH, 3)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Complete the sale
     */
    fun completeSale() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _success.value = false
            
            val vehicle = _selectedVehicle.value
            val customer = _selectedCustomer.value
            val paymentType = _paymentType.value
            val salePriceValue = getSalePriceAmount()
            
            if (vehicle == null) {
                _error.value = "Please select a vehicle"
                _isLoading.value = false
                return@launch
            }
            
            if (customer == null) {
                _error.value = "Please select a customer"
                _isLoading.value = false
                return@launch
            }
            
            if (paymentType == null) {
                _error.value = "Please select a payment type"
                _isLoading.value = false
                return@launch
            }
            
            if (salePriceValue <= 0) {
                _error.value = "Please enter a valid sale price"
                _isLoading.value = false
                return@launch
            }
            
            // Get vehicle reference
            val vehicleRefResult = vehicleRepository.getVehicleReferenceByChassis(vehicle.chassisNumber)
            if (vehicleRefResult.isFailure) {
                _error.value = "Failed to get vehicle reference"
                _isLoading.value = false
                return@launch
            }
            val vehicleRef = vehicleRefResult.getOrNull()
            
            // Get customer reference
            val customerRefResult = customerRepository.getCustomerReference(customer.customerId)
            if (customerRefResult.isFailure) {
                _error.value = "Failed to get customer reference"
                _isLoading.value = false
                return@launch
            }
            val customerRef = customerRefResult.getOrNull()
            
            when (paymentType) {
                "FULL_PAYMENT" -> {
                    val result = saleRepository.addSale(
                        customerRef = customerRef,
                        vehicleRef = vehicleRef,
                        purchaseType = "FULL_PAYMENT",
                        totalAmount = salePriceValue,
                        downPayment = salePriceValue
                    )
                    
                    result.fold(
                        onSuccess = {
                            _success.value = true
                            _isLoading.value = false
                        },
                        onFailure = { exception ->
                            _error.value = exception.message ?: "Failed to complete sale"
                            _isLoading.value = false
                        }
                    )
                }
                
                "EMI" -> {
                    val rate = _interestRate.value.toDoubleOrNull() ?: 0.0
                    val months = _durationMonths.value.toIntOrNull() ?: 0
                    
                    if (rate <= 0 || months <= 0) {
                        _error.value = "Please enter valid EMI details"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    val emiAmount = _calculatedEmi.value
                    if (emiAmount <= 0) {
                        _error.value = "Please calculate EMI first"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    val emiDetails = EmiDetails(
                        interestRate = rate,
                        frequency = _frequency.value,
                        installmentsCount = months,
                        installmentAmount = emiAmount,
                        nextDueDate = calculateNextDueDate(_frequency.value),
                        remainingInstallments = months,
                        paidInstallments = 0
                    )
                    
                    val result = saleRepository.addSale(
                        customerRef = customerRef,
                        vehicleRef = vehicleRef,
                        purchaseType = "EMI",
                        totalAmount = salePriceValue,
                        downPayment = 0.0,
                        emiDetails = emiDetails
                    )
                    
                    result.fold(
                        onSuccess = {
                            _success.value = true
                            _isLoading.value = false
                        },
                        onFailure = { exception ->
                            _error.value = exception.message ?: "Failed to complete sale"
                            _isLoading.value = false
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Reset form
     */
    fun reset() {
        _selectedVehicle.value = null
        _selectedCustomer.value = null
        _paymentType.value = null
        _interestRate.value = ""
        _frequency.value = "MONTHLY"
        _durationMonths.value = ""
        _calculatedEmi.value = 0.0
        _salePrice.value = ""
        _error.value = null
        _success.value = false
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}




