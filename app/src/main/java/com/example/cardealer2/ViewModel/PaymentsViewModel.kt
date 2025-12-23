package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Product
import com.example.cardealer2.data.VehicleSale
import com.example.cardealer2.repository.CustomerRepository
import com.example.cardealer2.repository.SaleRepository
import com.example.cardealer2.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for managing payments and tracking due dates
 */
class PaymentsViewModel : ViewModel() {
    private val saleRepository = SaleRepository
    private val customerRepository = CustomerRepository
    private val vehicleRepository = VehicleRepository
    
    // All sales
    private val _allSales = MutableStateFlow<List<VehicleSale>>(emptyList())
    val allSales: StateFlow<List<VehicleSale>> = _allSales.asStateFlow()
    
    // Sales due today
    private val _salesDueToday = MutableStateFlow<List<VehicleSale>>(emptyList())
    val salesDueToday: StateFlow<List<VehicleSale>> = _salesDueToday.asStateFlow()
    
    // Completed sales
    private val _completedSales = MutableStateFlow<List<VehicleSale>>(emptyList())
    val completedSales: StateFlow<List<VehicleSale>> = _completedSales.asStateFlow()
    
    // Expanded sales with customer and vehicle details
    data class SaleWithDetails(
        val sale: VehicleSale,
        val customer: CustomerDetails?,
        val vehicle: Product?
    )
    
    private val _salesWithDetails = MutableStateFlow<List<SaleWithDetails>>(emptyList())
    val salesWithDetails: StateFlow<List<SaleWithDetails>> = _salesWithDetails.asStateFlow()
    
    private val _salesDueTodayWithDetails = MutableStateFlow<List<SaleWithDetails>>(emptyList())
    val salesDueTodayWithDetails: StateFlow<List<SaleWithDetails>> = _salesDueTodayWithDetails.asStateFlow()
    
    private val _completedSalesWithDetails = MutableStateFlow<List<SaleWithDetails>>(emptyList())
    val completedSalesWithDetails: StateFlow<List<SaleWithDetails>> = _completedSalesWithDetails.asStateFlow()
    
    private val _emiSchedule = MutableStateFlow<List<SaleWithDetails>>(emptyList())
    val emiSchedule: StateFlow<List<SaleWithDetails>> = _emiSchedule.asStateFlow()
    
    private val _overdueEmi = MutableStateFlow<List<SaleWithDetails>>(emptyList())
    val overdueEmi: StateFlow<List<SaleWithDetails>> = _overdueEmi.asStateFlow()
    
    private val _upcomingEmi = MutableStateFlow<List<SaleWithDetails>>(emptyList())
    val upcomingEmi: StateFlow<List<SaleWithDetails>> = _upcomingEmi.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    data class CustomerDetails(
        val customerName: String,
        val phoneNumber: String,
        val address: String,
        val idProofType: String,
        val idProofNumber: String,
        val idProofImageUrls: List<String>
    )
    
    init {
        loadSales()
    }
    
    /**
     * Load all sales and populate details
     */
    fun loadSales() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Collect from repository StateFlow
                saleRepository.sales.collect { sales ->
                    _allSales.value = sales
                    
                    // Filter by status
                    _salesDueToday.value = sales.filter { sale ->
                        sale.status == "Active" && 
                        sale.purchaseType == "EMI" &&
                        isDueToday(sale.emiDetails?.nextDueDate ?: 0L)
                    }
                    
                    _completedSales.value = sales.filter { it.status == "Completed" }
                    
                    // Load details for all sales
                    loadSalesDetails(sales)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load sales"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load customer and vehicle details for sales
     */
    private suspend fun loadSalesDetails(sales: List<VehicleSale>) {
        val salesWithDetailsList = sales.map { sale ->
            val customer = sale.customerRef?.let { ref ->
                val result = vehicleRepository.getCustomerByReference(ref)
                result.getOrNull()?.let { customer ->
                    CustomerDetails(
                        customerName = customer.name,
                        phoneNumber = customer.phone,
                        address = customer.address,
                        idProofType = customer.idProofType,
                        idProofNumber = customer.idProofNumber,
                        idProofImageUrls = customer.idProofImageUrls
                    )
                }
            }
            
            val vehicle = sale.vehicleRef?.let { ref ->
                val result = vehicleRepository.getProductByReference(ref)
                result.getOrNull()
            }
            
            SaleWithDetails(sale, customer, vehicle)
        }
        
        _salesWithDetails.value = salesWithDetailsList
        _salesDueTodayWithDetails.value = salesWithDetailsList.filter { saleDetail ->
            saleDetail.sale.status == "Active" &&
            saleDetail.sale.purchaseType == "EMI" &&
            isDueToday(saleDetail.sale.emiDetails?.nextDueDate ?: 0L)
        }
        _completedSalesWithDetails.value = salesWithDetailsList.filter { 
            it.sale.status == "Completed" 
        }
        
        val emiOnly = salesWithDetailsList.filter { detail ->
            detail.sale.status == "Active" &&
            detail.sale.purchaseType == "EMI" &&
            (detail.sale.emiDetails?.nextDueDate ?: 0L) > 0L
        }.sortedBy { it.sale.emiDetails?.nextDueDate ?: Long.MAX_VALUE }
        
        _emiSchedule.value = emiOnly
        _overdueEmi.value = emiOnly.filter { detail ->
            isOverdue(detail.sale.emiDetails?.nextDueDate ?: 0L)
        }
        _upcomingEmi.value = emiOnly.filterNot { detail ->
            isOverdue(detail.sale.emiDetails?.nextDueDate ?: 0L)
        }
        
        _isLoading.value = false
    }
    
    /**
     * Check if a date is today
     */
    private fun isDueToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val tomorrow = today + (24 * 60 * 60 * 1000)
        
        return timestamp >= today && timestamp < tomorrow
    }
    
    private fun isOverdue(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return timestamp < todayStart
    }
    
    /**
     * Record an EMI payment
     */
    fun recordEmiPayment(saleId: String, cashAmount: Double, bankAmount: Double) {
        if (cashAmount <= 0.0 && bankAmount <= 0.0) {
            _error.value = "Enter payment amount"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = saleRepository.recordEmiPayment(saleId, cashAmount, bankAmount)
            
            result.fold(
                onSuccess = {
                    _isLoading.value = false
                    // Sales will automatically update via StateFlow listener
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to record payment"
                    _isLoading.value = false
                }
            )
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}

