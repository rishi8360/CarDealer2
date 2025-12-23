# Quick Instructions for AI Agents

## Data Flow Pattern in This App

**This app uses: Repository Pattern with Firebase Listeners + StateFlow**

### Core Rules:

1. **Repositories are `object` singletons** with Firebase listeners that automatically update StateFlow
2. **ViewModels observe repository StateFlow** - NO manual load methods needed
3. **Updates are automatic** - Firebase listeners handle everything
4. **NO manual cache management** - StateFlow is the cache
5. **NO manual reloads** - After mutations, listener updates StateFlow automatically

### Repository Pattern:

```kotlin
object YourRepository {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()
    
    init {
        startListening()  // Firebase listener starts automatically
    }
    
    private fun startListening() {
        collection.addSnapshotListener { snapshot, _ ->
            _items.value = snapshot.documents.map  // Updates StateFlow
        }
    }
}
```

### ViewModel Pattern:

```kotlin
// Option 1: Direct exposure (simplest)
val items: StateFlow<List<Item>> = repository.items

// Option 2: Collect into UiState
init {
    viewModelScope.launch {
        repository.items.collect { items ->
            _uiState.value = _uiState.value.copy(items = items)
        }
    }
}
```

### What NOT to Do:

- ❌ NO `loadXxx()` methods - data loads automatically
- ❌ NO manual cache (`private var cachedItems`)
- ❌ NO manual reload after mutations
- ❌ NO one-time queries without listeners

### What TO Do:

- ✅ Observe repository StateFlow in ViewModel
- ✅ Call repository mutation methods
- ✅ Let Firebase listener handle updates automatically
- ✅ Expose StateFlow to UI

### Data Flow:

```
Firestore → Listener → Repository StateFlow → ViewModel StateFlow → UI
```

**All updates are automatic. No manual coordination needed.**

---

**Reference:** See `DATA_FLOW_PATTERN_GUIDE.md` for detailed documentation.







