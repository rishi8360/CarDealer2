package com.humbleSolutions.cardealer2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humbleSolutions.cardealer2.data.Broker
import com.humbleSolutions.cardealer2.data.PersonTransaction
import com.humbleSolutions.cardealer2.repository.BrokerRepository
import com.humbleSolutions.cardealer2.repository.TransactionRepository
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewBrokersViewModel : ViewModel() {
    private val repository = BrokerRepository

    private val _brokers = MutableStateFlow<List<Broker>>(emptyList())
    val brokers = _brokers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadBrokers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                // fetch from Firestore
                val result = repository.getAllBrokers()
                if (result.isSuccess) {
                    _brokers.value = result.getOrNull() ?: emptyList()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBrokerById(brokerId: String) {
        viewModelScope.launch {
            // First check if broker is already in the loaded list
            val currentBrokers = _brokers.value
            val existingBroker = currentBrokers.find { it.brokerId == brokerId }
            
            if (existingBroker != null) {
                // Broker already loaded, no need to call repository
                return@launch
            }
            
            // Broker not found in loaded list, fetch from repository
            try {
                _isLoading.value = true
                _error.value = null
                val result = repository.getBrokerById(brokerId)
                if (result.isSuccess) {
                    val updated = result.getOrNull()
                    if (updated != null) {
                        // Add the broker to the list
                        _brokers.value = currentBrokers + updated
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateBroker(
        brokerId: String,
        name: String,
        phoneNumber: String,
        address: String,
        idProof: List<String>,
        brokerBill: List<String>
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Get existing broker to preserve createdAt
                val existingBroker = _brokers.value.find { it.brokerId == brokerId }
                val broker = Broker(
                    brokerId = brokerId,
                    name = name,
                    phoneNumber = phoneNumber,
                    address = address,
                    idProof = idProof,
                    brokerBill = brokerBill,
                    createdAt = existingBroker?.createdAt ?: System.currentTimeMillis()
                )
                
                val result = repository.updateBroker(broker)
                if (result.isSuccess) {
                    // Update local list
                    val current = _brokers.value
                    _brokers.value = current.map { 
                        if (it.brokerId == brokerId) broker else it 
                    }
                    // Clear error on success
                    _error.value = null
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteBroker(brokerId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val result = repository.deleteBroker(brokerId)
                if (result.isSuccess) {
                    // Remove from local list
                    _brokers.value = _brokers.value.filter { it.brokerId != brokerId }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load transactions for a broker
     */
    suspend fun loadBrokerTransactions(brokerRef: DocumentReference): Result<List<PersonTransaction>> {
        return TransactionRepository.getTransactionsByPerson(brokerRef)
    }
}
