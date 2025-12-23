# CarDealer2 - Data Structure & Management

## Overview
This application uses **Firebase Firestore** as the backend database and **Firebase Storage** for file storage (images and PDFs). The architecture follows a **Repository Pattern** with in-memory caching for performance optimization.

---

## 🗄️ Firebase Collections Structure

### 1. **Brand Collection** (`Brand`)
Stores vehicle brand information.

**Document Structure:**
```kotlin
{
  brandId: String,              // Brand name (e.g., "Toyota", "Honda")
  brandRef: DocumentReference,  // Reference to BrandNames collection
  logo: String,                 // Logo image URL
  modelNames: List<String>,     // List of model names (e.g., ["Camry", "Corolla"])
  vehicle: List<VehicleSummary> // Summary of vehicles in this brand
}
```

**Relationships:**
- References `BrandNames` collection via `brandRef`
- Contains `VehicleSummary` objects (embedded)

---

### 2. **BrandNames Collection** (`BrandNames`)
Normalized collection for unique brand names.

**Document Structure:**
```kotlin
{
  name: String  // Unique brand name
}
```

**Purpose:** Ensures brand name uniqueness and provides reference points for relationships.

---

### 3. **Product Collection** (`Product`)
Stores individual vehicle/product details.

**Document Structure:**
```kotlin
{
  brandId: String,                    // Brand name (legacy field)
  brandRef: DocumentReference?,       // Reference to BrandNames (new field)
  productId: String,                  // Model name (e.g., "Camry")
  chassisNumber: String,              // Unique chassis number
  chassisReference: DocumentReference,// Reference to ChassisNumber collection
  colour: String,
  condition: String,
  images: List<String>,               // Firebase Storage URLs
  kms: Int,
  lastService: String,
  previousOwners: Int,
  price: Int,
  type: String,                       // Vehicle type
  year: Int,
  noc: List<String>,                  // NOC document URLs
  rc: List<String>,                   // RC document URLs
  insurance: List<String>,            // Insurance document URLs
  brokerOrMiddleMan: String,          // Name (legacy)
  brokerOrMiddleManRef: DocumentReference?, // Reference to Broker/Customer
  owner: String,                      // Name (legacy)
  ownerRef: DocumentReference?        // Reference to Customer
}
```

**Relationships:**
- References `BrandNames` via `brandRef`
- References `ChassisNumber` via `chassisReference`
- References `Broker` or `Customer` via `brokerOrMiddleManRef` (depending on purchase type)
- References `Customer` via `ownerRef`

---

### 4. **ChassisNumber Collection** (`ChassisNumber`)
Tracks unique chassis numbers to prevent duplicates.

**Document Structure:**
```kotlin
{
  chassisNumber: String,
  createdAt: Timestamp
}
```

**Purpose:** Ensures chassis number uniqueness across the system.

---

### 5. **Customer Collection** (`Customer`)
Stores customer information.

**Document Structure:**
```kotlin
{
  customerId: String,              // Auto-generated document ID
  name: String,
  phone: String,
  address: String,
  photoUrl: List<String>,          // Firebase Storage URLs
  idProofType: String,             // Type of ID proof
  idProofNumber: String,
  idProofImageUrls: List<String>,  // Firebase Storage URLs
  amount: Int,                     // Positive => customer owes you, negative => you owe customer
  createdAt: Long
}
```

**Relationships:**
- Referenced by `Product` via `ownerRef` and `brokerOrMiddleManRef` (when purchase type is "Middle Man")

---

### 6. **Broker Collection** (`Broker`)
Stores broker information.

**Document Structure:**
```kotlin
{
  brokerId: String,                // Auto-generated document ID
  name: String,
  phoneNumber: String,
  idProof: List<String>,           // Firebase Storage URLs
  address: String,
  brokerBill: List<String>,        // Firebase Storage URLs
  createdAt: Long
}
```

**Relationships:**
- Referenced by `Product` via `brokerOrMiddleManRef` (when purchase type is "Broker")

---

### 7. **Catalog Collection** (`Catalog`)
Stores catalog information for generated catalogs.

**Document Structure:**
```kotlin
{
  products: List<String>,          // List of Product document IDs
  createdAt: Long,
  productCount: Int
}
```

---

### 8. **Colour Collection** (`Colour`)
Stores available vehicle colors.

**Document Structure:**
```kotlin
{
  colour: List<String>  // Array of color names
}
```

**Note:** Uses a single document with ID `unfVwvmNspz7mhoyGz9z`

---

### 9. **Purchase Collection** (`Purchase`)
Stores purchase transaction information.

**Document Structure:**
```kotlin
{
  purchaseId: String,                    // Auto-generated document ID
  grandTotal: Double,                    // Total purchase amount
  gstAmount: Double,                     // GST/TAX amount
  orderNumber: Int,                      // Unique order number (incremented)
  paymentMethods: Map<String, String>,   // Payment breakdown: {"bank": "10000", "cash": "5000", "credit": "0"}
  vehicle: Map<String, String>,          // Vehicle details as map
  middleMan: String,                     // Middle man name
  createdAt: Long                        // Timestamp
}
```

**Relationships:**
- Referenced by `Capital` transactions via `purchaseReference`
- Order numbers are managed via `MaxOrderNo` collection

---

### 10. **Capital Collection** (`Capital`)
Tracks capital transactions for Cash, Bank, and Credit accounts.

**Document Structure:**
- Documents named: `Cash`, `Bank`, `Credit`
- Each document contains an array of transactions:

```kotlin
// Capital/{Cash|Bank|Credit}
{
  transactions: List<{
    transactionDate: Long,                 // Transaction date timestamp
    createdAt: Long,                       // Creation timestamp
    purchaseReference: DocumentReference?, // Reference to Purchase document
    orderNumber: Int                       // Associated order number
  }>
}
```

**Purpose:** Tracks financial transactions separately for Cash, Bank, and Credit accounts, linked to purchase orders.

---

### 11. **MaxOrderNo Collection** (`MaxOrderNo`)
Tracks the maximum order number used for purchase orders.

**Document Structure:**
```kotlin
{
  maxOrderNo: Int  // Current maximum order number (starts at 0)
}
```

**Purpose:** Ensures unique, sequential order numbers for purchases. The value is atomically incremented when creating new purchases.

**Note:** Contains a single document with auto-generated ID. Initial value is 0.

---

## 📦 Data Models (Kotlin)

### 1. **Brand**
```kotlin
data class Brand(
    val vehicle: List<VehicleSummary> = emptyList(),
    val logo: String = "",
    val brandId: String = "",
    val modelNames: List<String> = emptyList()
)
```

### 2. **Product**
```kotlin
data class Product(
    val brandId: String = "",
    val productId: String = "",
    val colour: String = "",
    val chassisNumber: String = "",
    val condition: String = "",
    val images: List<String> = emptyList(),
    val kms: Int = 0,
    val lastService: String = "",
    val previousOwners: Int = 0,
    val price: Int = 0,
    val year: Int = 0,
    val type: String = "",
    val noc: List<String> = emptyList(),
    val rc: List<String> = emptyList(),
    val insurance: List<String> = emptyList(),
    val brokerOrMiddleMan: String = "",
    val owner: String = "",
    val brokerOrMiddleManRef: DocumentReference? = null,
    val ownerRef: DocumentReference? = null
)
```

### 3. **VehicleSummary**
```kotlin
data class VehicleSummary(
    val productId: String = "",
    val type: String = "",
    val imageUrl: String = "",
    val quantity: Int = 0
)
```

### 4. **Customer**
```kotlin
data class Customer(
    val customerId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val photoUrl: List<String> = emptyList(),
    val idProofType: String = "",
    val idProofNumber: String = "",
    val idProofImageUrls: List<String> = emptyList(),
    val amount: Int = 0,
    val createdAt: Long = 0L
)
```

### 5. **Broker**
```kotlin
data class Broker(
    val brokerId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val idProof: List<String> = emptyList(),
    val address: String = "",
    val brokerBill: List<String> = emptyList(),
    val createdAt: Long = 0L
)
```

### 6. **Purchase**
```kotlin
data class Purchase(
    val purchaseId: String = "",
    val grandTotal: Double = 0.0,
    val gstAmount: Double = 0.0,
    val orderNumber: Int = 0,
    val paymentMethods: Map<String, String> = emptyMap(),
    val vehicle: Map<String, String> = emptyMap(),
    val middleMan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

### 7. **CapitalTransaction**
```kotlin
data class CapitalTransaction(
    val transactionDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val purchaseReference: DocumentReference? = null,
    val orderNumber: Int = 0
)
```

### 8. **MaxOrderNo**
```kotlin
data class MaxOrderNo(
    val maxOrderNo: Int = 0
)
```

---

## 🏗️ Repository Pattern

### **VehicleRepository** (Singleton Object)
- **Collections:** `Brand`, `Product`, `BrandNames`, `ChassisNumber`, `Catalog`, `Colour`
- **Caching:** In-memory cache for `Brand` and `Product` lists
- **Key Features:**
  - Cache-first data fetching
  - Automatic cache invalidation on updates
  - Image/PDF upload to Firebase Storage
  - Transaction-based operations for data consistency

**Main Operations:**
- `getBrands()` - Fetch all brands (with cache)
- `getAllProducts()` - Fetch all products (with cache)
- `getProductsByBrandId()` - Get products by brand
- `addVehicleToBrand()` - Add new vehicle with image/PDF uploads
- `updateVehicle()` - Update vehicle details
- `deleteVehicleByChassis()` - Delete vehicle
- `addNewBrand()` - Add new brand
- `addModelToBrand()` - Add model to existing brand
- `checkChassisNumberExists()` - Validate chassis uniqueness

### **CustomerRepository** (Singleton Object)
- **Collection:** `Customer`
- **Caching:** In-memory cache for customer list
- **Key Features:**
  - Image/PDF upload to Firebase Storage
  - Customer CRUD operations

**Main Operations:**
- `addCustomer()` - Add new customer
- `getAllCustomers()` - Fetch all customers (with cache)
- `getCustomerById()` - Get customer by ID
- `updateCustomer()` - Update customer details
- `deleteCustomer()` - Delete customer

### **BrokerRepository** (Singleton Object)
- **Collection:** `Broker`
- **Caching:** In-memory cache for broker list
- **Key Features:**
  - PDF upload to Firebase Storage
  - Broker CRUD operations

**Main Operations:**
- `addBroker()` - Add new broker
- `getAllBrokers()` - Fetch all brokers (with cache)
- `getBrokerById()` - Get broker by ID
- `updateBroker()` - Update broker details
- `deleteBroker()` - Delete broker

### **PurchaseRepository** (Singleton Object)
- **Collections:** `Purchase`, `Capital`, `MaxOrderNo`
- **Caching:** In-memory cache for purchases and MaxOrderNo
- **Key Features:**
  - Automatic order number generation
  - Atomic order number incrementing
  - Capital transaction tracking for Cash, Bank, and Credit
  - Purchase and capital transaction creation

**Main Operations:**
- `initializeMaxOrderNo()` - Initialize MaxOrderNo collection (sets to 0)
- `getMaxOrderNo()` - Get current maximum order number
- `getNextOrderNumber()` - Atomically increment and get next order number
- `addPurchase()` - Add new purchase with auto-generated order number
- `getPurchaseById()` - Get purchase by ID
- `getAllPurchases()` - Fetch all purchases (with cache)
- `addCapitalTransaction()` - Add transaction to Cash, Bank, or Credit account
- `getCapitalTransactions()` - Get all transactions for a capital type
- `addPurchaseWithCapitalTransactions()` - Add purchase and automatically create capital transactions for payment methods

---

## 💾 Caching Strategy

### **In-Memory Caching**
Each repository maintains in-memory caches:
- `VehicleRepository`: `cachedBrands`, `cachedProducts`
- `CustomerRepository`: `cachedCustomers`
- `BrokerRepository`: `cachedBrokers`
- `PurchaseRepository`: `cachedPurchases`, `cachedMaxOrderNo`

### **Cache Behavior:**
1. **Read Operations:** Check cache first, fallback to Firestore if cache is empty
2. **Write Operations:** Update cache after successful Firestore write
3. **Cache Invalidation:** Manual invalidation via `clearCache()` or automatic on updates

### **Cache Benefits:**
- Reduced Firestore reads (cost savings)
- Faster data access
- Offline-like experience for cached data

---

## 📁 Firebase Storage Structure

### **Image Storage:**
```
vehicle_images/
  └── {brandId}/
      └── {productId}/
          └── {timestamp}.jpg

customers/
  └── {customerId}/
      ├── photos/
      │   └── image_{timestamp}_{index}.jpg
      └── id_proofs/
          └── document_{timestamp}_{index}.pdf
```

### **Document Storage:**
```
vehicle_documents/
  └── {brandId}/
      └── {productId}/
          ├── noc_{timestamp}_{index}.pdf
          ├── rc_{timestamp}_{index}.pdf
          └── insurance_{timestamp}_{index}.pdf

brokers/
  └── {brokerId}/
      ├── id_proofs/
      │   └── document_{timestamp}_{index}.pdf
      └── broker_bills/
          └── document_{timestamp}_{index}.pdf
```

---

## 🔗 Data Relationships

```
BrandNames (normalized)
    ↑
    │ brandRef
    │
Brand ──→ VehicleSummary (embedded)
    │
    │ brandRef / brandId
    │
Product ──→ ChassisNumber (via chassisReference)
    │
    ├──→ Broker (via brokerOrMiddleManRef, if purchaseType = "Broker")
    ├──→ Customer (via brokerOrMiddleManRef, if purchaseType = "Middle Man")
    └──→ Customer (via ownerRef)

Purchase ──→ MaxOrderNo (for order number management)
    │
    └──→ Capital/{Cash|Bank|Credit} (via purchaseReference in transactions array)
```

---

## 🔄 Data Flow

### **Adding a Vehicle:**
1. Upload images to Firebase Storage → Get URLs
2. Upload PDFs (NOC, RC, Insurance) to Firebase Storage → Get URLs
3. Resolve brand reference from `BrandNames`
4. Resolve broker/customer references (if provided)
5. Create `ChassisNumber` document
6. Create `Product` document with all references
7. Update `Brand` document's `vehicle` array
8. Update in-memory caches

### **Updating a Vehicle:**
1. Check for new images/PDFs (local URIs vs Firebase URLs)
2. Upload new files to Firebase Storage
3. Update `Product` document
4. Update `Brand` document's `vehicle` array if needed
5. Update in-memory caches

### **Querying Products:**
1. Check cache first
2. If cache miss, query Firestore
3. Support both `brandId` (legacy) and `brandRef` (new) queries
4. Update cache with results

---

## 🎯 Key Design Patterns

1. **Repository Pattern:** Centralized data access layer
2. **Singleton Objects:** Single instance repositories
3. **Result Type:** All operations return `Result<T>` for error handling
4. **Caching:** In-memory cache for performance
5. **Reference Normalization:** Using `BrandNames` for brand uniqueness
6. **Document References:** Using Firestore references for relationships
7. **Transaction Safety:** Critical operations use Firestore transactions

---

## 📊 Data Consistency

### **Transactions Used For:**
- Adding vehicles (updates Brand and creates Product atomically)
- Deleting vehicles (updates Brand and deletes Product atomically)
- Adding brands (creates BrandNames and Brand atomically)

### **Uniqueness Constraints:**
- Chassis numbers: Enforced via `ChassisNumber` collection
- Brand names: Enforced via `BrandNames` collection

---

## 🔍 Query Patterns

### **Brand Queries:**
- By `brandId` (legacy string field)
- By `brandRef` (new DocumentReference field)
- Fallback mechanism: Try `brandRef` first, then `brandId`

### **Product Queries:**
- By `brandId` + `productId`
- By `brandId` only
- By `brandId` + multiple `productId`s (using `whereIn`, batched if > 10)
- By `chassisNumber`

### **Limitations:**
- Firestore `whereIn` has a limit of 10 items, so queries are batched for larger sets

---

## 🚀 Performance Optimizations

1. **In-Memory Caching:** Reduces Firestore reads
2. **Batch Queries:** Handles `whereIn` limitations for large model lists
3. **Reference-Based Queries:** Faster than string-based queries
4. **Selective Cache Updates:** Only updates affected cache entries
5. **Parallel Uploads:** Images and PDFs uploaded concurrently where possible

---

## 📝 Notes

- **Legacy Support:** Both `brandId` (string) and `brandRef` (DocumentReference) are maintained for backward compatibility
- **File Uploads:** All local URIs are automatically uploaded to Firebase Storage, existing Firebase URLs are preserved
- **Error Handling:** All repository methods return `Result<T>` for consistent error handling
- **Cache Management:** Manual cache clearing available via `clearCache()` methods

