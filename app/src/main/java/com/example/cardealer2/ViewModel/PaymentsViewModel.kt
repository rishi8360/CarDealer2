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
import com.example.cardealer2.repository.PurchaseRepository
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
        val vehicle: Product?,
        val emiDetails: EmiDetails? = null  // EmiDetails fetched from separate collection
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
    
    // Capital balances
    private val _cashBalance = MutableStateFlow<Double?>(null)
    val cashBalance: StateFlow<Double?> = _cashBalance.asStateFlow()
    
    private val _bankBalance = MutableStateFlow<Double?>(null)
    val bankBalance: StateFlow<Double?> = _bankBalance.asStateFlow()
    
    private val _creditBalance = MutableStateFlow<Double?>(null)
    val creditBalance: StateFlow<Double?> = _creditBalance.asStateFlow()
    
    private val _isLoadingBalances = MutableStateFlow(false)
    val isLoadingBalances: StateFlow<Boolean> = _isLoadingBalances.asStateFlow()

    data class CustomerDetails(
        val customerName: String,
        val phoneNumber: String,
        val address: String,
        val idProofType: String,
        val idProofNumber: String,
        val idProofImageUrls: List<String>
    )
    
    init {
        // No longer auto-loading on init - manual loading only
        // Load capital balances on init
        loadCapitalBalances()
    }
    
    /**
     * Load capital balances for Cash, Bank, and Credit
     */
    fun loadCapitalBalances() {
        viewModelScope.launch {
            _isLoadingBalances.value = true
            try {
                val cashResult = PurchaseRepository.getCapitalBalance("Cash")
                _cashBalance.value = cashResult.getOrNull() ?: 0.0
                
                val bankResult = PurchaseRepository.getCapitalBalance("Bank")
                _bankBalance.value = bankResult.getOrNull() ?: 0.0
                
                val creditResult = PurchaseRepository.getCapitalBalance("Credit")
                _creditBalance.value = creditResult.getOrNull() ?: 0.0
            } catch (e: Exception) {
                // Handle error silently or set to 0
                _cashBalance.value = 0.0
                _bankBalance.value = 0.0
                _creditBalance.value = 0.0
            } finally {
                _isLoadingBalances.value = false
            }
        }
    }
    
    /**
     * Manually load EMI schedule with date range filtering
     * @param fromDate Start of date range (timestamp in milliseconds, start of day)
     * @param toDate End of date range (timestamp in milliseconds, end of day)
     */
    fun loadEmiScheduleWithDateRange(fromDate: Long, toDate: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Manually fetch active sales (pending EMI sales)
                val result = saleRepository.getAllActiveSales()
                
                result.fold(
                    onSuccess = { sales ->
                        // Filter only EMI sales
                        val emiSales = sales.filter { it.emi && it.emiDetailsRef != null }
                        _allSales.value = emiSales
                        
                        // Load details for EMI sales (including EmiDetails)
                        loadSalesDetailsWithDateRange(emiSales, fromDate, toDate)
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to load EMI schedule"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load EMI schedule"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load customer and vehicle details for sales, including EmiDetails from separate collection
     * Filters by date range: shows EMIs within range OR overdue (before fromDate)
     */
    private suspend fun loadSalesDetailsWithDateRange(sales: List<VehicleSale>, fromDate: Long, toDate: Long) {
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
            
            // Fetch EmiDetails from separate collection if emiDetailsRef exists
            val emiDetails = sale.emiDetailsRef?.let { ref ->
                saleRepository.getEmiDetailsByRef(ref).getOrNull()
            }
            
            SaleWithDetails(sale, customer, vehicle, emiDetails)
        }
        
        _salesWithDetails.value = salesWithDetailsList
        
        // Filter EMI-only sales (status = false/pending, emi = true)
        val emiOnly = salesWithDetailsList.filter { detail ->
            !detail.sale.status &&  // pending
            detail.sale.emi &&  // is EMI
            (detail.emiDetails?.nextDueDate ?: 0L) > 0L
        }
        
        // Apply date range filter: include EMIs within range OR overdue (before fromDate)
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Calculate end of toDate (end of day)
        val toDateEnd = Calendar.getInstance().apply {
            timeInMillis = toDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        val filteredEmi = emiOnly.filter { detail ->
            val dueDate = detail.emiDetails?.nextDueDate ?: 0L
            if (dueDate <= 0L) return@filter false
            
            // Include if:
            // 1. Due date is within the selected range (fromDate to toDateEnd), OR
            // 2. Due date is overdue (before fromDate and before today)
            (dueDate >= fromDate && dueDate <= toDateEnd) || 
            (dueDate < fromDate && dueDate < todayStart)
        }.sortedBy { it.emiDetails?.nextDueDate ?: Long.MAX_VALUE }
        
        _emiSchedule.value = filteredEmi
        _overdueEmi.value = filteredEmi.filter { detail ->
            isOverdue(detail.emiDetails?.nextDueDate ?: 0L)
        }
        _upcomingEmi.value = filteredEmi.filterNot { detail ->
            isOverdue(detail.emiDetails?.nextDueDate ?: 0L)
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
    fun recordEmiPayment(sale: VehicleSale, emiDetails: EmiDetails, cashAmount: Double, bankAmount: Double, note: String = "", date: String = "") {
        if (cashAmount <= 0.0 && bankAmount <= 0.0) {
            _error.value = "Enter payment amount"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = saleRepository.recordEmiPayment(sale, emiDetails, cashAmount, bankAmount, note = note, date = date)
            
            result.fold(
                onSuccess = {
                    _isLoading.value = false
                    // Sales will automatically update via StateFlow listener
                    // Refresh capital balances after recording payment
                    loadCapitalBalances()
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

