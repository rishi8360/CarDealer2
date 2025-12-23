# Firebase Listeners Refactoring - Example Implementation

## Overview
This document explains the refactoring of `CustomerRepository` and related ViewModels to use Firebase Firestore listeners instead of manual caching and reloading.

## Key Changes

### 1. CustomerRepository Refactoring

#### Before (Manual Cache + Manual Reload)
- Maintained manual cache: `private var cachedCustomers: List<Customer>? = null`
- Required manual cache invalidation after mutations
- ViewModels had to call `getAllCustomers()` to refresh data
- No real-time updates

#### After (Firebase Listener + StateFlow)
- **Firebase Listener**: Automatically listens to collection changes
- **StateFlow Exposure**: `val customers: StateFlow<List<Customer>>` - exposed to ViewModels
- **Automatic Updates**: When Firestore changes, StateFlow updates automatically
- **No Manual Cache**: Firebase handles offline persistence
- **No Manual Reload**: ViewModels just observe the StateFlow

#### Key Implementation Details

```kotlin
// Listener starts automatically in init block
init {
    startListening()
}

// Listener updates StateFlow whenever Firestore changes
private fun startListening() {
    listenerRegistration = customerCollection.addSnapshotListener { snapshot, error ->
        if (snapshot != null) {
            val customers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Customer::class.java)?.copy(customerId = doc.id)
            }
            _customers.value = customers  // ✅ StateFlow updates automatically
        }
    }
}
```

### 2. ViewCustomersViewModel Refactoring

#### Before (Manual Loading)
```kotlin
fun loadCustomers() {
    viewModelScope.launch {
        val result = repository.getAllCustomers()
        _customers.value = result.getOrNull() ?: emptyList()
    }
}
```

#### After (Observing Repository StateFlow)
```kotlin
// Directly expose repository's StateFlow
val customers: StateFlow<List<Customer>> = repository.customers

// No loadCustomers() needed - data updates automatically!
```

### 3. PurchaseVehicleViewModel Updates

#### Before (Manual Reload After Add)
```kotlin
val result = customerRepository.addCustomer(customer)
if (result.isSuccess) {
    loadCustomerNames()  // ❌ Manual reload needed
}
```

#### After (Automatic Update)
```kotlin
val result = customerRepository.addCustomer(customer)
if (result.isSuccess) {
    // ✅ No reload needed - Firebase listener updates StateFlow automatically
    // UI updates automatically via StateFlow observation
}
```

#### StateFlow Collection
```kotlin
init {
    viewModelScope.launch {
        // Automatically update UiState when repository StateFlow changes
        customerRepository.customers.collect { customers ->
            _uiState.value = _uiState.value.copy(
                customers = customers,
                customerNames = customers.map { it.name }
            )
        }
    }
}
```

## Benefits

### ✅ Automatic Real-Time Updates
- Changes in Firestore automatically propagate to UI
- No manual reload calls needed
- Works across multiple ViewModels (since repository is singleton)

### ✅ No Cache Management
- Firebase handles offline persistence
- No manual cache invalidation
- No stale data issues

### ✅ Cleaner Code
- ViewModels just observe StateFlows
- Less boilerplate code
- More declarative approach

### ✅ Better Performance
- Firebase listeners are optimized
- Only updates when data actually changes
- Efficient network usage

## Architecture Flow

```
Firebase Firestore
    ↓ (listener)
CustomerRepository (singleton)
    ↓ (StateFlow)
ViewCustomersViewModel / PurchaseVehicleViewModel
    ↓ (StateFlow)
UI (Compose)
```

## Important Notes

1. **Repository Singleton**: Since repositories are `object` singletons, the listener is shared across all ViewModels
2. **Lifecycle**: Listener starts in repository `init` block and stays active (repository is singleton)
3. **Error Handling**: Repository exposes `error: StateFlow<String?>` for error states
4. **Loading States**: Repository exposes `isLoading: StateFlow<Boolean>` for loading states
5. **Document ID**: Always use `doc.id` as the source of truth, not the stored `customerId` field

## Next Steps

To apply this pattern to other repositories:

1. **BrokerRepository**: Add Firebase listener + StateFlow
2. **PurchaseRepository**: Add Firebase listener for purchases collection
3. **VehicleRepository**: Add Firebase listener for brands/products

## Migration Checklist

- [x] CustomerRepository - Refactored with Firebase listener
- [x] ViewCustomersViewModel - Observes repository StateFlow
- [x] PurchaseVehicleViewModel - Observes repository StateFlow for customers
- [ ] BrokerRepository - Add Firebase listener
- [ ] PurchaseRepository - Add Firebase listener
- [ ] VehicleRepository - Add Firebase listener
- [ ] Update other ViewModels that use these repositories

## Example: Adding a New Repository Listener

```kotlin
object YourRepository {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()
    
    private var listenerRegistration: ListenerRegistration? = null
    
    init {
        startListening()
    }
    
    private fun startListening() {
        listenerRegistration = collection.addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Item::class.java)?.copy(id = doc.id)
                }
                _items.value = items
            }
        }
    }
}
```

Then in ViewModel:
```kotlin
val items: StateFlow<List<Item>> = repository.items
// That's it! UI automatically updates when Firestore changes
```









