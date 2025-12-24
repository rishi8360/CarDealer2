package com.example.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.repository.CustomerRepository
import com.example.cardealer2.repository.TransactionRepository
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel that observes CustomerRepository's StateFlow
 * No need for manual loading or cache invalidation - repository handles it via Firebase listeners
 */
class ViewCustomersViewModel : ViewModel() {
    private val repository = CustomerRepository

    // 🔹 Directly expose repository's StateFlow - automatically updates when Firestore changes
    val customers: StateFlow<List<Customer>> = repository.customers
    
    // 🔹 Expose repository's loading and error states
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    /**
     * Clear error state
     */
    fun clearError() {
        repository.clearError()
    }

    /**
     * Get customer by ID - repository will check StateFlow first, then Firestore if needed
     * Note: Once fetched, the listener will automatically add it to StateFlow
     */
    suspend fun getCustomerById(customerId: String): Result<Customer> {
        return repository.getCustomerById(customerId)
    }
    
    /**
     * @deprecated No longer needed - customers StateFlow updates automatically via Firebase listener
     * The repository listener is active from the moment the app starts (repository is singleton)
     */
    @Deprecated("Customers are automatically loaded via Firebase listener in repository")
    fun loadCustomers() {
        // No-op: Repository listener handles this automatically
    }
    
    /**
     * Load transactions for a customer
     */
    suspend fun loadCustomerTransactions(customerRef: DocumentReference): Result<List<PersonTransaction>> {
        return TransactionRepository.getTransactionsByPerson(customerRef)
    }
}
