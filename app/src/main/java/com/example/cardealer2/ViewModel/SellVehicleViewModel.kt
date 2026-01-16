package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.EmiDetails
import com.example.cardealer2.data.Product
import com.example.cardealer2.repository.CustomerRepository
import com.example.cardealer2.repository.SaleRepository
import com.example.cardealer2.repository.VehicleRepository
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
    
    // Down payment (for EMI)
    private val _downPayment = MutableStateFlow("")
    val downPayment: StateFlow<String> = _downPayment.asStateFlow()
    
    // Down payment split (for EMI)
    private val _cashDownPayment = MutableStateFlow("")
    val cashDownPayment: StateFlow<String> = _cashDownPayment.asStateFlow()
    
    private val _bankDownPayment = MutableStateFlow("")
    val bankDownPayment: StateFlow<String> = _bankDownPayment.asStateFlow()
    
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

    // Document handover flags
    private val _nocHandedOver = MutableStateFlow(false)
    val nocHandedOver: StateFlow<Boolean> = _nocHandedOver.asStateFlow()

    private val _rcHandedOver = MutableStateFlow(false)
    val rcHandedOver: StateFlow<Boolean> = _rcHandedOver.asStateFlow()

    private val _insuranceHandedOver = MutableStateFlow(false)
    val insuranceHandedOver: StateFlow<Boolean> = _insuranceHandedOver.asStateFlow()

    private val _otherDocsHandedOver = MutableStateFlow(false)
    val otherDocsHandedOver: StateFlow<Boolean> = _otherDocsHandedOver.asStateFlow()
    
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
                    // Use sellingPrice if available (non-zero), otherwise fallback to price
                    _salePrice.value = if (product.sellingPrice > 0) product.sellingPrice.toString() else product.price.toString()
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
     * Set down payment
     */
    fun setDownPayment(value: String) {
        _downPayment.value = value
        if (_paymentType.value == "EMI") {
            calculateEmi()
        }
    }
    
    /**
     * Set cash down payment
     */
    fun setCashDownPayment(value: String) {
        _cashDownPayment.value = value
    }
    
    /**
     * Set bank down payment
     */
    fun setBankDownPayment(value: String) {
        _bankDownPayment.value = value
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
    fun setSelectedCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun setNocHandedOver(value: Boolean) {
        _nocHandedOver.value = value
    }

    fun setRcHandedOver(value: Boolean) {
        _rcHandedOver.value = value
    }

    fun setInsuranceHandedOver(value: Boolean) {
        _insuranceHandedOver.value = value
    }

    fun setOtherDocsHandedOver(value: Boolean) {
        _otherDocsHandedOver.value = value
    }
    
    /**
     * Calculate EMI amount using simple interest
     * Formula: Total Interest = Principal × Interest Rate × Number of Periods
     * Total Amount = Principal + Total Interest
     * EMI = Total Amount ÷ Number of Payments
     * Number of Payments = Duration (months) ÷ Frequency multiplier
     *   - MONTHLY: multiplier = 1 (payments = months)
     *   - QUARTERLY: multiplier = 3 (payments = months / 3)
     *   - SEMI_ANNUALLY: multiplier = 6 (payments = months / 6)
     *   - YEARLY: multiplier = 12 (payments = months / 12)
     */
    private fun calculateEmi() {
        val salePriceAmount = getSalePriceAmount()
        val downPaymentAmount = getDownPaymentAmount()
        
        if (salePriceAmount <= 0) {
            _calculatedEmi.value = 0.0
            return
        }
        
        // Principal = Sale Price - Down Payment
        val principal = salePriceAmount - downPaymentAmount
        
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
        
        // Allow 0% interest (no-interest EMI) but disallow negative values or non-positive duration
        if (rate < 0 || months <= 0) {
            _calculatedEmi.value = 0.0
            return
        }
        
        // Calculate number of periods based on frequency (round down)
        val periods = calculatePeriods(months, _frequency.value)
        
        if (periods <= 0) {
            _calculatedEmi.value = 0.0
            return
        }
        
        // Convert rate percentage to decimal
        val rateDecimal = rate / 100.0
        
        // Simple Interest: Total Interest = Principal × Rate × Periods
        val totalInterest = principal * rateDecimal * periods
        
        // Total Amount = Principal + Total Interest
        val totalAmount = principal + totalInterest
        
        // Calculate number of payments based on frequency (same as periods)
        val numberOfPayments = periods
        
        // EMI = Total Amount ÷ Number of Payments
        val emi = totalAmount / numberOfPayments
        
        _calculatedEmi.value = emi
    }
    
    /**
     * Calculate number of periods based on frequency (round down)
     */
    private fun calculatePeriods(months: Int, frequency: String): Int {
        return when (frequency) {
            "MONTHLY" -> months
            "QUARTERLY" -> months / 3
            "SEMI_ANNUALLY" -> months / 6
            "YEARLY" -> months / 12
            else -> months
        }
    }
    
    private fun getSalePriceAmount(): Double =
        _salePrice.value.toDoubleOrNull() ?: 0.0
    
    private fun getDownPaymentAmount(): Double =
        _downPayment.value.toDoubleOrNull() ?: 0.0
    
    /**
     * Calculate next due date based on frequency
     */
    private fun calculateNextDueDate(frequency: String): Long {
        val calendar = Calendar.getInstance()
        when (frequency) {
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "QUARTERLY" -> calendar.add(Calendar.MONTH, 3)
            "SEMI_ANNUALLY" -> calendar.add(Calendar.MONTH, 6)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Complete the sale
     */
    fun completeSale(note: String = "", date: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _success.value = false
            
            val vehicle = _selectedVehicle.value
            val customer = _selectedCustomer.value
            val paymentType = _paymentType.value
            val salePriceValue = getSalePriceAmount()
            val nocHandedOver = _nocHandedOver.value
            val rcHandedOver = _rcHandedOver.value
            val insuranceHandedOver = _insuranceHandedOver.value
            val otherDocsHandedOver = _otherDocsHandedOver.value
            
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
                        downPayment = salePriceValue,
                        note = note,
                        date = date,
                        nocHandedOver = nocHandedOver,
                        rcHandedOver = rcHandedOver,
                        insuranceHandedOver = insuranceHandedOver,
                        otherDocsHandedOver = otherDocsHandedOver
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
                    val downPaymentValue = getDownPaymentAmount()
                    val cashDownPaymentValue = _cashDownPayment.value.toDoubleOrNull() ?: 0.0
                    val bankDownPaymentValue = _bankDownPayment.value.toDoubleOrNull() ?: 0.0
                    
                    // Allow 0% interest (no-interest EMI) but disallow negative values or non-positive duration
                    if (rate < 0 || months <= 0) {
                        _error.value = "Please enter valid EMI details (rate cannot be negative and months must be > 0)"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    // Validate down payment is not greater than sale price
                    if (downPaymentValue < 0 || downPaymentValue >= salePriceValue) {
                        _error.value = "Down payment must be between 0 and sale price"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    // Validate cash and bank down payment amounts
                    if (downPaymentValue > 0) {
                        if (cashDownPaymentValue < 0 || bankDownPaymentValue < 0) {
                            _error.value = "Cash and Bank amounts cannot be negative"
                            _isLoading.value = false
                            return@launch
                        }
                        val totalSplit = cashDownPaymentValue + bankDownPaymentValue
                        if (kotlin.math.abs(totalSplit - downPaymentValue) > 0.01) {
                            _error.value = "Cash + Bank amount must equal Down Payment amount"
                            _isLoading.value = false
                            return@launch
                        }
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
                        downPayment = downPaymentValue,
                        cashDownPayment = cashDownPaymentValue,
                        bankDownPayment = bankDownPaymentValue,
                        emiDetails = emiDetails,
                        note = note,
                        date = date,
                        nocHandedOver = nocHandedOver,
                        rcHandedOver = rcHandedOver,
                        insuranceHandedOver = insuranceHandedOver,
                        otherDocsHandedOver = otherDocsHandedOver
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
        _downPayment.value = ""
        _cashDownPayment.value = ""
        _bankDownPayment.value = ""
        _error.value = null
        _success.value = false
        _nocHandedOver.value = false
        _rcHandedOver.value = false
        _insuranceHandedOver.value = false
        _otherDocsHandedOver.value = false
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}




