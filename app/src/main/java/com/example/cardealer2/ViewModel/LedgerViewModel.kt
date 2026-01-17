package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.TransactionType
import com.example.cardealer2.data.VehicleSale
import com.example.cardealer2.repository.CustomerRepository
import com.example.cardealer2.repository.PurchaseRepository
import com.example.cardealer2.repository.SaleRepository
import com.example.cardealer2.repository.TransactionRepository
import com.example.cardealer2.repository.VehicleRepository
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Broker
import com.example.cardealer2.data.EmiDetails
import com.example.cardealer2.data.Purchase
import com.example.cardealer2.data.Company
import com.example.cardealer2.data.AppPreferences
import com.example.cardealer2.utils.TransactionBillGenerator
import com.google.firebase.firestore.DocumentReference
import android.content.Context
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Data classes for grouped ledger information
 */
data class MoneyReceivedGroup(
    val personName: String,
    val personType: String,
    val totalAmount: Double,
    val transactions: List<PersonTransaction>
)

data class MoneyPaidGroup(
    val personName: String,
    val personType: String,
    val totalAmount: Double,
    val transactions: List<PersonTransaction>
)

data class EmiOutstanding(
    val customerName: String,
    val saleId: String,
    val totalAmount: Double,
    val downPayment: Double,
    val remainingInstallments: Int,
    val installmentAmount: Double,
    val outstandingAmount: Double,
    val nextDueDate: Long,
    val sale: VehicleSale
)

data class CreditOwed(
    val personName: String,
    val personType: String,
    val totalCreditAmount: Double,
    val transactions: List<PersonTransaction>
)

class LedgerViewModel : ViewModel() {
    
    // Capital balances
    private val _cashBalance = MutableStateFlow<Double?>(null)
    val cashBalance: StateFlow<Double?> = _cashBalance.asStateFlow()
    
    private val _bankBalance = MutableStateFlow<Double?>(null)
    val bankBalance: StateFlow<Double?> = _bankBalance.asStateFlow()
    
    private val _creditBalance = MutableStateFlow<Double?>(null)
    val creditBalance: StateFlow<Double?> = _creditBalance.asStateFlow()
    
    private val _isLoadingBalances = MutableStateFlow(false)
    val isLoadingBalances: StateFlow<Boolean> = _isLoadingBalances.asStateFlow()
    
    // Transactions (fetched incrementally, not via listener)
    private val _transactions = MutableStateFlow<List<PersonTransaction>>(emptyList())
    val transactions: StateFlow<List<PersonTransaction>> = _transactions.asStateFlow()
    
    // Track how many transactions have been loaded and the last requested limit
    private var loadedTransactionCount = 0
    private var lastRequestedLimit = 0
    private val loadMoreBatchSize = 10
    
    // Has more transactions StateFlow (updates when transactions change)
    private val _hasMoreTransactions = MutableStateFlow(false)
    val hasMoreTransactions: StateFlow<Boolean> = _hasMoreTransactions.asStateFlow()
    
    val sales: StateFlow<List<VehicleSale>> = SaleRepository.sales
    
    /**
     * Load top 10 transactions initially (fetches once, no listener)
     */
    fun loadTopTransactions() {
        viewModelScope.launch {
            try {
                val limit = loadMoreBatchSize
                val result = TransactionRepository.getTopTransactions(limit = limit)
                result.fold(
                    onSuccess = { transactions ->
                        _transactions.value = transactions
                        loadedTransactionCount = transactions.size
                        lastRequestedLimit = limit
                        // Update hasMoreTransactions: if we got exactly what we requested, there might be more
                        _hasMoreTransactions.value = transactions.size == limit && transactions.size > 0
                    },
                    onFailure = { exception ->
                        // Handle error silently or log it
                        _transactions.value = emptyList()
                        loadedTransactionCount = 0
                        lastRequestedLimit = 0
                        _hasMoreTransactions.value = false
                    }
                )
            } catch (e: Exception) {
                _transactions.value = emptyList()
                loadedTransactionCount = 0
                lastRequestedLimit = 0
            }
        }
    }
    
    /**
     * Load more transactions (pagination)
     * Fetches top N+10 transactions and updates the list
     */
    fun loadMoreTransactions() {
        viewModelScope.launch {
            try {
                val nextLimit = lastRequestedLimit + loadMoreBatchSize
                val result = TransactionRepository.getTopTransactions(limit = nextLimit)
                result.fold(
                    onSuccess = { transactions ->
                        _transactions.value = transactions
                        loadedTransactionCount = transactions.size
                        lastRequestedLimit = nextLimit
                        // Update hasMoreTransactions: if we got exactly what we requested, there might be more
                        _hasMoreTransactions.value = transactions.size == nextLimit && transactions.size > 0
                    },
                    onFailure = { exception ->
                        // Handle error silently - don't clear existing transactions
                    }
                )
            } catch (e: Exception) {
                // Handle error silently - don't clear existing transactions
            }
        }
    }
    
    
    // Section B: Who gave me money (grouped by customer)
    val moneyReceivedGroups: StateFlow<List<MoneyReceivedGroup>> = transactions
        .map { transactionList ->
            val moneyInTransactions = transactionList.filter { transaction ->
                transaction.type == TransactionType.SALE || transaction.type == TransactionType.EMI_PAYMENT
            }
            
            moneyInTransactions
                .groupBy { "${it.personName}_${it.personType}" }
                .map { (_, transList) ->
                    val firstTrans = transList.first()
                    MoneyReceivedGroup(
                        personName = firstTrans.personName,
                        personType = firstTrans.personType,
                        totalAmount = transList.sumOf { it.amount },
                        transactions = transList.sortedByDescending { it.date }
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Section C: Whom did I give money to (grouped by person)
    val moneyPaidGroups: StateFlow<List<MoneyPaidGroup>> = transactions
        .map { transactionList ->
            val moneyOutTransactions = transactionList.filter { transaction ->
                transaction.type == TransactionType.PURCHASE || transaction.type == TransactionType.BROKER_FEE
            }
            
            moneyOutTransactions
                .groupBy { "${it.personName}_${it.personType}" }
                .map { (_, transList) ->
                    val firstTrans = transList.first()
                    MoneyPaidGroup(
                        personName = firstTrans.personName,
                        personType = firstTrans.personType,
                        totalAmount = transList.sumOf { it.amount },
                        transactions = transList.sortedByDescending { it.date }
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Section D: Who owes me money (EMI outstanding)
    val emiOutstanding: StateFlow<List<EmiOutstanding>> = combine(sales, transactions) { salesList, transactionList ->
            salesList
                .filter { sale ->
                    !sale.status && sale.emi && sale.emiDetailsRef != null  // pending (false), is EMI, has EmiDetailsRef
                }
                .mapNotNull { sale ->
                    // Fetch EmiDetails from separate collection
                    val emiDetailsResult = sale.emiDetailsRef?.let { ref ->
                        SaleRepository.getEmiDetailsByRef(ref).getOrNull()
                    } ?: return@mapNotNull null
                    
                    val outstandingAmount = (emiDetailsResult.remainingInstallments * emiDetailsResult.installmentAmount) + emiDetailsResult.pendingExtraBalance
                    
                    // Get customer name from transactions linked to this sale
                    val customerName = transactionList
                        .firstOrNull { transaction ->
                            transaction.type == TransactionType.SALE &&
                            transaction.relatedRef?.id == sale.saleId
                        }?.personName ?: "Customer"
                    
                    EmiOutstanding(
                        customerName = customerName,
                        saleId = sale.saleId,
                        totalAmount = sale.totalAmount,
                        downPayment = 0.0,  // downPayment field removed, always 0 for new structure
                        remainingInstallments = emiDetailsResult.remainingInstallments,
                        installmentAmount = emiDetailsResult.installmentAmount,
                        outstandingAmount = outstandingAmount,
                        nextDueDate = emiDetailsResult.nextDueDate,
                        sale = sale
                    )
                }
                .sortedBy { it.nextDueDate }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Section E: Who do I owe money to (credit purchases)
    val creditOwedGroups: StateFlow<List<CreditOwed>> = transactions
        .map { transactionList ->
            val creditTransactions = transactionList.filter { transaction ->
                (transaction.creditAmount > 0 || transaction.paymentMethod == "CREDIT" || 
                 transaction.paymentMethod == "MIXED") && 
                transaction.type == TransactionType.PURCHASE
            }
            
            creditTransactions
                .groupBy { "${it.personName}_${it.personType}" }
                .map { (_, transList) ->
                    val firstTrans = transList.first()
                    CreditOwed(
                        personName = firstTrans.personName,
                        personType = firstTrans.personType,
                        totalCreditAmount = transList.sumOf { it.creditAmount },
                        transactions = transList.sortedByDescending { it.date }
                    )
                }
                .filter { it.totalCreditAmount > 0 }
                .sortedByDescending { it.totalCreditAmount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Transfer transactions (fetched once, not via listener)
    private val _transferTransactions = MutableStateFlow<List<PersonTransaction>>(emptyList())
    val transferTransactions: StateFlow<List<PersonTransaction>> = _transferTransactions.asStateFlow()
    
    /**
     * Load top N transfer transactions sorted by createdAt (latest first)
     */
    fun loadTopTransferTransactions(limit: Int = 10) {
        viewModelScope.launch {
            try {
                val result = TransactionRepository.getTopTransferTransactions(limit)
                result.fold(
                    onSuccess = { transactions ->
                        _transferTransactions.value = transactions
                    },
                    onFailure = { exception ->
                        // Handle error silently or log it
                        _transferTransactions.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                _transferTransactions.value = emptyList()
            }
        }
    }
    
    init {
        loadCapitalBalances()
        loadTopTransactions() // Load top 10 transactions when ViewModel is created
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
     * Adjust capital balance
     */
    fun adjustCapitalBalance(
        type: String,
        amount: Double,
        description: String,
        reason: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = PurchaseRepository.adjustCapitalBalance(type, amount, description, reason)
                result.fold(
                    onSuccess = {
                        loadCapitalBalances() // Reload balances
                        onSuccess()
                    },
                    onFailure = { exception ->
                        onError(exception.message ?: "Failed to adjust balance")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Failed to adjust balance")
            }
        }
    }
    
    /**
     * Set initial capital balance
     */
    fun setInitialCapitalBalance(
        type: String,
        amount: Double,
        description: String = "Initial Balance",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = PurchaseRepository.setInitialCapitalBalance(type, amount, description)
                result.fold(
                    onSuccess = {
                        loadCapitalBalances() // Reload balances
                        onSuccess()
                    },
                    onFailure = { exception ->
                        onError(exception.message ?: "Failed to set initial balance")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Failed to set initial balance")
            }
        }
    }
    
    /**
     * Transfer amount from customer to capital (Bank or Cash)
     */
    fun transferFromCustomer(
        customerId: String,
        amount: Double,
        capitalType: String,
        description: String = "Transfer from customer",
        note: String = "",
        date: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = TransactionRepository.transferFromCustomerToCapital(
                    customerId = customerId,
                    amount = amount,
                    capitalType = capitalType,
                    description = description,
                    note = note,
                    date = date
                )
                result.fold(
                    onSuccess = {
                        loadCapitalBalances() // Reload balances
                        loadTopTransactions() // Reload top transactions
                        loadTopTransferTransactions(limit = 10) // Reload transfer transactions
                        onSuccess()
                    },
                    onFailure = { exception ->
                        onError(exception.message ?: "Failed to transfer from customer")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Failed to transfer from customer")
            }
        }
    }
    
    /**
     * Transfer between any sources (Customer, Broker, Cash, Bank)
     */
    fun transferBetweenSources(
        fromType: String,
        fromCustomer: Customer?,
        fromBroker: Broker?,
        toType: String,
        toCustomer: Customer?,
        toBroker: Broker?,
        amount: Double,
        description: String = "Transfer",
        note: String = "",
        date: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = TransactionRepository.transferBetweenSources(
                    fromType = fromType,
                    fromCustomer = fromCustomer,
                    fromBroker = fromBroker,
                    toType = toType,
                    toCustomer = toCustomer,
                    toBroker = toBroker,
                    amount = amount,
                    description = description,
                    note = note,
                    date = date
                )
                result.fold(
                    onSuccess = {
                        loadCapitalBalances() // Reload balances
                        loadTopTransactions() // Reload top transactions
                        loadTopTransferTransactions(limit = 10) // Reload transfer transactions
                        onSuccess()
                    },
                    onFailure = { exception ->
                        onError(exception.message ?: "Transfer failed")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Transfer failed")
            }
        }
    }
    
    /**
     * Get a single transaction by ID
     */
    suspend fun getTransactionById(transactionId: String): Result<PersonTransaction?> {
        return try {
            TransactionRepository.getTransactionById(transactionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a transaction and reload relevant data
     */
    fun deleteTransaction(
        transactionId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = TransactionRepository.deleteTransaction(transactionId)
                result.fold(
                    onSuccess = {
                        // Reload all relevant data after deletion
                        loadCapitalBalances()
                        loadTopTransactions()
                        loadTopTransferTransactions(limit = 10)
                        onSuccess()
                    },
                    onFailure = { exception ->
                        onError(exception.message ?: "Failed to delete transaction")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete transaction")
            }
        }
    }
    
    /**
     * Fetch all related data for generating a transaction bill
     */
    suspend fun fetchBillRelatedData(context: Context, transaction: PersonTransaction): Result<TransactionBillGenerator.BillRelatedData> {
        return try {
            var personDetails: TransactionBillGenerator.PersonBillDetails? = null
            var purchaseDetails: Purchase? = null
            var saleDetails: VehicleSale? = null
            var emiDetails: EmiDetails? = null
            var productDetails: TransactionBillGenerator.ProductBillDetails? = null
            
            // Fetch person details
            transaction.personRef?.let { personRef ->
                when (transaction.personType) {
                    "CUSTOMER", "MIDDLE_MAN" -> {
                        VehicleRepository.getCustomerByReference(personRef).getOrNull()?.let { customer ->
                            personDetails = TransactionBillGenerator.PersonBillDetails(
                                name = customer.name,
                                phone = customer.phone,
                                address = customer.address
                            )
                        }
                    }
                    "BROKER" -> {
                        VehicleRepository.getBrokerByReference(personRef).getOrNull()?.let { broker ->
                            personDetails = TransactionBillGenerator.PersonBillDetails(
                                name = broker.name,
                                phone = broker.phoneNumber,
                                address = broker.address
                            )
                        }
                    }
                }
            }
            
            // Fetch related document based on transaction type
            transaction.relatedRef?.let { relatedRef ->
                when (transaction.type) {
                    TransactionType.PURCHASE, TransactionType.BROKER_FEE -> {
                        // Fetch Purchase document
                        PurchaseRepository.getPurchaseById(relatedRef.id).getOrNull()?.let { purchase ->
                            purchaseDetails = purchase
                            
                            // Extract product details from purchase vehicle map
                            purchase.vehicle["chassisNumber"]?.let { chassisNumber ->
                                productDetails = TransactionBillGenerator.ProductBillDetails(
                                    chassisNumber = chassisNumber,
                                    type = purchase.vehicle["type"],
                                    year = purchase.vehicle["year"]?.toIntOrNull(),
                                    colour = purchase.vehicle["colour"]
                                )
                            }
                        }
                    }
                    TransactionType.SALE, TransactionType.EMI_PAYMENT -> {
                        // Fetch VehicleSale document
                        try {
                            val saleDoc = relatedRef.get().await()
                            if (saleDoc.exists()) {
                                val emi = (saleDoc.get("emi") as? Boolean) ?: false
                                val status = (saleDoc.get("status") as? Boolean) ?: false
                                
                                // Convert timestamp properly
                                val purchaseDateLong = try {
                                    val timestamp = saleDoc.get("purchaseDate")
                                    when (timestamp) {
                                        is com.google.firebase.Timestamp -> timestamp.toDate().time
                                        is Long -> timestamp
                                        is Number -> timestamp.toLong()
                                        else -> System.currentTimeMillis()
                                    }
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                                
                                val sale = VehicleSale(
                                    saleId = saleDoc.id,
                                    customerRef = saleDoc.get("customerRef") as? DocumentReference,
                                    vehicleRef = saleDoc.get("vehicleRef") as? DocumentReference,
                                    purchaseDate = purchaseDateLong,
                                    totalAmount = (saleDoc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                                    emi = emi,
                                    status = status,
                                    emiDetailsRef = saleDoc.get("emiDetailsRef") as? DocumentReference
                                )
                                saleDetails = sale
                                
                                // Fetch product details if vehicleRef exists
                                sale.vehicleRef?.let { vehicleRef ->
                                    VehicleRepository.getProductByReference(vehicleRef).getOrNull()?.let { product ->
                                        productDetails = TransactionBillGenerator.ProductBillDetails(
                                            chassisNumber = product.chassisNumber,
                                            brandId = product.brandId,
                                            productId = product.productId,
                                            type = product.type,
                                            year = product.year,
                                            colour = product.colour
                                        )
                                    }
                                }
                                
                                // Fetch EMI details if it's an EMI transaction
                                if (transaction.type == TransactionType.EMI_PAYMENT && sale.emiDetailsRef != null) {
                                    SaleRepository.getEmiDetailsByRef(sale.emiDetailsRef!!).getOrNull()?.let { emi ->
                                        emiDetails = emi
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Sale document fetch failed, continue without it
                        }
                    }
                }
            }
            
            // Fetch company details
            val companyDetails = try {
                AppPreferences.getCompanyDataSync(context)
            } catch (e: Exception) {
                Company() // Default empty company
            }
            
            Result.success(
                TransactionBillGenerator.BillRelatedData(
                    personDetails = personDetails,
                    purchaseDetails = purchaseDetails,
                    saleDetails = saleDetails,
                    emiDetails = emiDetails,
                    productDetails = productDetails,
                    companyDetails = companyDetails
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
}

// Need to make docToVehicleSale accessible - add extension or use reflection
// For now, we'll create a helper in SaleRepository or access it differently

