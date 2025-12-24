package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.TransactionType
import com.example.cardealer2.data.VehicleSale
import com.example.cardealer2.repository.PurchaseRepository
import com.example.cardealer2.repository.SaleRepository
import com.example.cardealer2.repository.TransactionRepository
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
    
    // Transactions from repository
    val transactions: StateFlow<List<PersonTransaction>> = TransactionRepository.transactions
    val sales: StateFlow<List<VehicleSale>> = SaleRepository.sales
    
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
                    sale.status == "Active" && sale.purchaseType == "EMI" && sale.emiDetails != null
                }
                .mapNotNull { sale ->
                    val emiDetails = sale.emiDetails ?: return@mapNotNull null
                    val outstandingAmount = emiDetails.remainingInstallments * emiDetails.installmentAmount
                    
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
                        downPayment = sale.downPayment,
                        remainingInstallments = emiDetails.remainingInstallments,
                        installmentAmount = emiDetails.installmentAmount,
                        outstandingAmount = outstandingAmount,
                        nextDueDate = emiDetails.nextDueDate,
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
    
    init {
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
    
}

