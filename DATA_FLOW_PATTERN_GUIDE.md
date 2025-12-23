# Data Flow Pattern Guide for AI Agents

## 🎯 Core Architecture Pattern

**This app uses: Repository Pattern with Real-time Firebase Listeners + StateFlow**

### Key Principles:
1. **Repositories are `object` singletons** - Single instance shared across entire app
2. **Firebase Firestore listeners** - Automatically listen to collection changes
3. **StateFlow for reactive state** - ViewModels observe StateFlow, UI updates automatically
4. **No manual cache management** - StateFlow acts as in-memory cache
5. **No manual reloads** - Firebase listeners handle all updates automatically

---

## 📊 Data Flow Diagram

```
Firebase Firestore Collection
    ↓ (Real-time listener - starts in repository init)
Repository (object singleton)
    ↓ (Updates StateFlow when Firestore changes)
ViewModel (observes repository StateFlow)
    ↓ (Exposes StateFlow to UI)
UI (Jetpack Compose - collects StateFlow)
```

---

## 🏗️ Repository Structure

### Required Pattern for ALL Repositories:

```kotlin
object YourRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = db.collection("YourCollection")
    
    // 🔹 MutableStateFlow (private)
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    
    // 🔹 Public StateFlow (exposed to ViewModels)
    val items: StateFlow<List<Item>> = _items.asStateFlow()
    
    // 🔹 Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // 🔹 Listener registration
    private var listenerRegistration: ListenerRegistration? = null
    
    // 🔹 CRITICAL: Start listener in init block
    init {
        startListening()
    }
    
    // 🔹 Start Firebase listener
    private fun startListening() {
        listenerRegistration = collection.addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Item::class.java)?.copy(id = doc.id)
                }
                _items.value = items  // ✅ Updates StateFlow automatically
            }
        }
    }
    
    // 🔹 Mutation methods (add, update, delete)
    suspend fun addItem(item: Item): Result<Unit> {
        // Add to Firestore
        // Listener will automatically update StateFlow - NO manual cache update needed
    }
    
    // 🔹 Cleanup (optional)
    fun stopListening() {
        listenerRegistration?.remove()
    }
}
```

### Key Points:
- ✅ Repository MUST be `object` (singleton)
- ✅ Listener MUST start in `init` block
- ✅ StateFlow MUST be exposed as public `val`
- ✅ Listener updates `_items.value` directly
- ✅ NO manual cache management needed
- ✅ NO manual reload calls after mutations

---

## 📱 ViewModel Structure

### Pattern 1: Direct StateFlow Exposure (Simplest)

**Use when:** ViewModel only needs to expose repository data directly

```kotlin
class YourViewModel : ViewModel() {
    private val repository = YourRepository
    
    // 🔹 Directly expose repository's StateFlow
    val items: StateFlow<List<Item>> = repository.items
    
    // 🔹 Expose repository states
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error
    
    // ✅ That's it! Data updates automatically when Firestore changes
    // ✅ NO loadItems() method needed
    // ✅ NO manual refresh needed
}
```

### Pattern 2: Collect into UiState (For Combined State)

**Use when:** ViewModel needs to combine multiple StateFlows or add UI-specific state

```kotlin
data class YourUiState(
    val items: List<Item> = emptyList(),
    val filteredItems: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class YourViewModel : ViewModel() {
    private val repository = YourRepository
    
    private val _uiState = MutableStateFlow(YourUiState())
    val uiState: StateFlow<YourUiState> = _uiState
    
    init {
        // 🔹 Collect from repository StateFlow and update UiState
        viewModelScope.launch {
            repository.items.collect { items ->
                _uiState.value = _uiState.value.copy(
                    items = items,
                    filteredItems = items.filter { /* your filter */ }
                )
            }
        }
        
        // 🔹 Collect loading state
        viewModelScope.launch {
            repository.isLoading.collect { isLoading ->
                _uiState.value = _uiState.value.copy(isLoading = isLoading)
            }
        }
    }
    
    // 🔹 Mutation methods
    fun addItem(item: Item) {
        viewModelScope.launch {
            val result = repository.addItem(item)
            // ✅ NO reload needed - Firebase listener updates StateFlow automatically
            // ✅ UiState will update automatically via collection in init
        }
    }
}
```

### Key Points:
- ✅ ViewModels observe repository StateFlow (directly or via collection)
- ✅ NO `loadXxx()` methods needed - data loads automatically
- ✅ NO manual refresh after mutations - Firebase listener handles it
- ✅ Multiple ViewModels can observe same repository StateFlow
- ✅ All ViewModels get updates simultaneously when Firestore changes

---

## 🚫 What NOT to Do

### ❌ DON'T: Manual Cache Management
```kotlin
// ❌ WRONG
private var cachedItems: List<Item>? = null

fun getAllItems(): Result<List<Item>> {
    if (cachedItems != null) {
        return Result.success(cachedItems!!)
    }
    // ... fetch from Firestore
    cachedItems = items
    return Result.success(items)
}
```

### ❌ DON'T: Manual Reload After Mutations
```kotlin
// ❌ WRONG
fun addItem(item: Item) {
    viewModelScope.launch {
        repository.addItem(item)
        loadItems()  // ❌ Don't do this!
    }
}
```

### ❌ DON'T: One-Time Queries Without Listeners
```kotlin
// ❌ WRONG - This won't get real-time updates
suspend fun getItems(): List<Item> {
    return collection.get().await().documents.map { ... }
}
```

### ✅ DO: Use Listeners + StateFlow
```kotlin
// ✅ CORRECT - Gets real-time updates automatically
val items: StateFlow<List<Item>> = repository.items
```

---

## 🔄 How Updates Work

### Scenario: Adding a New Item

1. **User Action:** ViewModel calls `repository.addItem(item)`
2. **Repository:** Adds item to Firestore
3. **Firebase Listener:** Detects Firestore change automatically
4. **Repository StateFlow:** Updates `_items.value` automatically
5. **ViewModel:** Receives update via StateFlow observation
6. **UI:** Updates automatically via StateFlow collection

**Result:** No manual reload needed! Updates propagate automatically through the chain.

### Multiple ViewModels Scenario

If you have:
- `ViewModelA` observing `repository.items`
- `ViewModelB` observing `repository.items`
- `ViewModelC` observing `repository.items`

When Firestore changes:
- ✅ All 3 ViewModels get updates simultaneously
- ✅ All 3 UIs update automatically
- ✅ No coordination needed between ViewModels
- ✅ Repository singleton ensures single source of truth

---

## 📋 Checklist for Implementing New Repository

- [ ] Repository is `object` (singleton)
- [ ] Has private `MutableStateFlow<List<T>>` for data
- [ ] Has public `StateFlow<List<T>>` exposed to ViewModels
- [ ] Has `isLoading` and `error` StateFlows
- [ ] Listener starts in `init` block
- [ ] Listener updates `_items.value` when Firestore changes
- [ ] Mutation methods don't manually update cache
- [ ] Mutation methods don't trigger manual reloads

---

## 📋 Checklist for Implementing New ViewModel

- [ ] ViewModel observes repository StateFlow (directly or via collection)
- [ ] NO `loadXxx()` methods (data loads automatically)
- [ ] NO manual refresh after mutations
- [ ] Mutation methods call repository methods only
- [ ] UI collects from ViewModel StateFlow

---

## 🎯 Summary for AI Agents

**When working on this codebase:**

1. **Repositories:** Always use Firebase listeners + StateFlow pattern
2. **ViewModels:** Always observe repository StateFlow, never call load methods
3. **Mutations:** Just call repository methods - no manual reloads needed
4. **UI:** Collect StateFlow from ViewModel
5. **Updates:** Automatic via Firebase listeners - don't add manual refresh logic

**Key Rule:** If you see `loadXxx()` method or manual cache management, it's outdated. Use StateFlow observation instead.

**Remember:** 
- Firebase listeners → Repository StateFlow → ViewModel StateFlow → UI
- All updates are automatic
- No manual cache or reload needed
- Single source of truth (repository singleton)

---

## 📚 Reference Examples

See these files for reference:
- `CustomerRepository.kt` - Simple collection listener
- `BrokerRepository.kt` - Simple collection listener  
- `VehicleRepository.kt` - Complex multi-listener pattern
- `ViewCustomersViewModel.kt` - Direct StateFlow exposure
- `PurchaseVehicleViewModel.kt` - Collecting into UiState
- `FIREBASE_LISTENERS_REFACTOR.md` - Migration examples
- `VEHICLE_REPOSITORY_FLOW.md` - Detailed flow documentation

---

## ❓ Common Questions

**Q: Do I need to call loadItems() when screen opens?**  
A: No! Repository listener starts in `init` block. Just observe StateFlow.

**Q: Do I need to reload after adding an item?**  
A: No! Firebase listener detects the change and updates StateFlow automatically.

**Q: Can multiple ViewModels observe the same repository StateFlow?**  
A: Yes! All ViewModels get updates simultaneously.

**Q: What if I need to filter the data?**  
A: Collect repository StateFlow in ViewModel and filter it, then expose filtered StateFlow.

**Q: What if listener is expensive?**  
A: Firebase listeners are optimized. For on-demand listeners (like VehicleRepository products), start/stop them as needed.

**Q: How do I handle errors?**  
A: Repository exposes `error: StateFlow<String?>`. ViewModel exposes it to UI.

**Q: How do I handle loading states?**  
A: Repository exposes `isLoading: StateFlow<Boolean>`. ViewModel exposes it to UI.







