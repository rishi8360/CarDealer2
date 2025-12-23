# VehicleRepository.kt - Flow Documentation

## 🏗️ Architecture Overview

**Pattern**: Repository Pattern with Real-time Firestore Listeners + StateFlow
- **Object Singleton**: Single instance shared across the app
- **Real-time Updates**: Firestore snapshot listeners automatically update StateFlow
- **Reactive State**: ViewModels observe StateFlow for automatic UI updates
- **In-memory Cache**: StateFlow acts as cache, eliminating manual cache management

---

## 🔄 Initialization Flow

```
App Start
    ↓
VehicleRepository accessed (first time)
    ↓
init block executes
    ↓
startBrandListening() called
    ↓
Brand collection listener registered
    ↓
Brands StateFlow automatically updates when Firestore changes
```

### Key Initialization Steps:
1. **Firebase Setup** (Lines 29-35)
   - Initialize Firestore instance
   - Setup collection references (Brand, Product, BrandNames)
   - Setup Firebase Storage reference

2. **StateFlow Initialization** (Lines 37-49)
   - `_brands`: MutableStateFlow for brands
   - `_products`: MutableStateFlow for products
   - `_isLoading`: Loading state
   - `_error`: Error state

3. **Listener Registration** (Lines 61-93)
   - Brand listener starts automatically in `init`
   - Product listeners start on-demand when queries are made

---

## 📊 StateFlow Management Flow

### Brands StateFlow
```
Firestore Brand Collection
    ↓ (Real-time listener)
Brand changes detected
    ↓
Snapshot received
    ↓
Documents converted to Brand objects
    ↓
_brands.value updated
    ↓
ViewModels observing brands StateFlow automatically get updates
```

### Products StateFlow
```
Multiple Product Listeners (by brandId, brandId+productId, etc.)
    ↓
Each listener updates listenerProducts map
    ↓
updateProductsFromListeners() called
    ↓
Merge all products (deduplicate by chassisNumber)
    ↓
_products.value updated
    ↓
ViewModels observing products StateFlow automatically get updates
```

**Deduplication Strategy**: Uses `chassisNumber` as unique key to prevent duplicate products from multiple listeners.

---

## 🔍 Real-time Listener System

### 1. Brand Listener (Always Active)
```kotlin
startBrandListening()
├─ Listens to entire Brand collection
├─ Updates _brands StateFlow automatically
└─ Runs in init block (always active)
```

### 2. Product Listeners (On-Demand)
Three types of product listeners can be started:

#### A. By Brand ID
```kotlin
startListeningToProductsByBrandId(brandId)
├─ Query: Product where brandId = X OR brandRef = X
├─ Key: "brand_$brandId"
└─ Stores results in listenerProducts map
```

#### B. By Brand ID + Product ID
```kotlin
startListeningToProductsByBrandIdAndProductId(brandId, productId)
├─ Query: Product where (brandId = X OR brandRef = X) AND productId = Y
├─ Key: "brand_${brandId}_product_$productId"
└─ Stores results in listenerProducts map
```

#### C. By Brand ID + Multiple Models
```kotlin
startListeningToProductsByBrandIdAndModels(brandId, productIds)
├─ Handles Firestore's 10-item limit for whereIn
├─ Chunks productIds into groups of 10
├─ Creates separate listener for each chunk
├─ Keys: "brand_${brandId}_models_${index}_${chunk}"
└─ Stores results in listenerProducts map
```

### Listener Management
- **Active Listeners Map**: `activeProductListeners` tracks all active listeners
- **Products Cache**: `listenerProducts` stores products per listener
- **Merge Function**: `updateProductsFromListeners()` merges all listener results
- **Cleanup**: Stop methods remove listeners and update StateFlow

---

## 📖 READ Operations Flow

### Get Brands
```
getBrands() [Deprecated]
    ↓
Returns _brands.value (StateFlow value)
    ↓
Recommended: Observe brands StateFlow directly in ViewModel
```

### Get Products
All product queries follow this pattern:
```
1. Start appropriate listener (if not already active)
2. Check StateFlow cache first
3. If not in cache, query Firestore once (immediate result)
4. Listener updates StateFlow for future changes
```

#### A. Get Products by Brand ID
```kotlin
getProductsByBrandId(brandId)
├─ startListeningToProductsByBrandId(brandId)
├─ Check _products.value.filter { it.brandId == brandId }
├─ If found → return immediately
└─ If not found → query Firestore once, return results
```

#### B. Get Products by Brand ID + Product ID
```kotlin
getProductByBrandIdProductId(brandId, productId)
├─ startListeningToProductsByBrandIdAndProductId(brandId, productId)
├─ Check _products.value.filter { ... }
├─ If found → return immediately
└─ If not found → query Firestore once, return results
```

#### C. Get Products by Brand ID + Models
```kotlin
getProductsByBrandIdAndModels(brandId, modelNames)
├─ startListeningToProductsByBrandIdAndModels(brandId, modelNames)
├─ Handles chunking (if > 10 models)
├─ Check _products.value.filter { ... }
├─ If found → return immediately
└─ If not found → query Firestore (batched if needed), return results
```

#### D. Get Product by Chassis Number
```kotlin
getProductFeatureByChassis(chassisNumber)
├─ Check _products.value.find { it.chassisNumber == chassisNumber }
├─ If found → return immediately
└─ If not found → query Firestore once
```

### Get Brand by ID
```kotlin
getBrandById(brandId)
├─ Check _brands.value.find { it.brandId == brandId }
├─ If found → return immediately
└─ If not found → query Firestore once
```

### Get Models by Brand ID
```kotlin
getModelsByBrandId(brandId)
├─ Query Brand collection (by brandRef or brandId)
├─ Extract modelNames array from document
└─ Return list of model names
```

---

## ✏️ CREATE Operations Flow

### Add New Brand
```kotlin
addNewBrand(brand)
├─ Check if BrandNames entry exists (by name)
│  ├─ If exists → use existing reference
│  └─ If not → create new BrandNames document
├─ Check if Brand document already exists (by brandRef)
│  └─ If exists → return error
├─ Run Firestore transaction
│  └─ Create new Brand document with:
│     ├─ brandId (legacy string)
│     ├─ brandRef (DocumentReference)
│     ├─ logo
│     ├─ modelNames
│     └─ vehicle (VehicleSummary list)
└─ Listener automatically updates _brands StateFlow
```

### Add Model to Brand
```kotlin
addModelToBrand(brandId, modelName)
├─ Find Brand document (by brandRef or brandId)
├─ Update modelNames array using FieldValue.arrayUnion()
└─ Listener automatically updates _brands StateFlow
```

### Add Vehicle to Brand
```kotlin
addVehicleToBrand(brandId, product, imageUris, pdfs, ...)
│
├─ 1️⃣ Upload Files to Storage
│  ├─ Upload images → get image URLs
│  ├─ Upload NOC PDFs → get NOC URLs
│  ├─ Upload RC PDFs → get RC URLs
│  └─ Upload Insurance PDFs → get insurance URLs
│
├─ 2️⃣ Resolve References
│  ├─ Resolve BrandNames reference
│  ├─ Resolve Brand document reference
│  ├─ Resolve Broker/Customer reference (brokerOrMiddleManRef)
│  └─ Resolve Owner reference (ownerRef)
│
└─ 3️⃣ Run Firestore Transaction
   ├─ Update Brand document:
   │  ├─ If vehicle summary exists → increment quantity
   │  └─ If not → create new vehicle summary
   │
   ├─ Create ChassisNumber document:
   │  ├─ Document ID: auto-generated
   │  ├─ Fields: chassisNumber, createdAt
   │  └─ Reference stored in Product
   │
   └─ Create Product document:
      ├─ All product fields
      ├─ brandRef (DocumentReference)
      ├─ chassisReference (DocumentReference)
      ├─ brokerOrMiddleManRef (DocumentReference?)
      ├─ ownerRef (DocumentReference?)
      └─ Uploaded file URLs
   
   └─ Listeners automatically update StateFlows
```

**Key Features**:
- Transaction ensures atomicity (Brand update + Product creation)
- Creates ChassisNumber document to track uniqueness
- Updates Brand's vehicle summary with quantity
- Handles both legacy (string) and new (reference) fields

---

## 🔄 UPDATE Operations Flow

### Update Vehicle
```kotlin
updateVehicle(originalChassisNumber, updatedProduct)
│
├─ 1️⃣ Find Product Document
│  └─ Query by originalChassisNumber
│
├─ 2️⃣ Upload New Files
│  ├─ Upload images (preserves existing Firebase URLs, uploads new local URIs)
│  ├─ Upload NOC PDFs
│  ├─ Upload RC PDFs
│  └─ Upload Insurance PDFs
│
├─ 3️⃣ Resolve References
│  ├─ Resolve brandRef for updated brand
│  └─ Use existing brokerOrMiddleManRef and ownerRef if available
│
└─ 4️⃣ Update Product Document
   ├─ Update all product fields
   ├─ Update file URLs (images, NOC, RC, insurance)
   ├─ Update brandRef if changed
   └─ Update reference fields if changed
   
   └─ Listener automatically updates _products StateFlow
```

**Smart Upload Logic**:
- If URL starts with "http://" or "https://" → preserve as-is (already uploaded)
- If URI is local (content:// or file://) → upload to Firebase Storage

---

## 🗑️ DELETE Operations Flow

### Delete Vehicle
```kotlin
deleteVehicleByChassis(chassisNumber)
│
├─ 1️⃣ Find Product Document
│  └─ Query by chassisNumber
│
├─ 2️⃣ Find Brand Document
│  └─ Query by brandRef (prefer) or brandId (fallback)
│
└─ 3️⃣ Run Firestore Transaction
   ├─ Update Brand document:
   │  ├─ Find vehicle summary by productId
   │  ├─ If quantity > 1 → decrement quantity
   │  └─ If quantity = 1 → remove vehicle summary
   │
   └─ Delete Product document
   
   └─ Listeners automatically update StateFlows
```

**Note**: ChassisNumber document is NOT deleted (for historical tracking)

---

## 📁 File Storage Operations

### Upload Images
```kotlin
uploadImagesToStorage(brandId, productId, imageUris)
├─ For each URI:
│  ├─ Create storage path: "vehicle_images/$brandId/$productId/${timestamp}.jpg"
│  ├─ Upload file to Firebase Storage
│  └─ Get download URL
└─ Return list of download URLs
```

### Upload Images from Strings
```kotlin
uploadImagesToStorageFromStrings(imageStrings, brandId, productId)
├─ For each image string:
│  ├─ If starts with "http://" or "https://" → keep as-is
│  ├─ If local URI → upload to Firebase Storage
│  └─ Get download URL
└─ Return list of download URLs (mix of existing + new)
```

### Upload PDFs
```kotlin
uploadPdfsToStorage(pdfUris, brandId, productId, documentType)
├─ For each PDF URI:
│  ├─ If starts with "http://" or "https://" → keep as-is
│  ├─ If local URI:
│  │  ├─ Create storage path: "vehicle_documents/$brandId/$productId/${documentType}_${timestamp}_${index}.pdf"
│  │  ├─ Set content type: "application/pdf"
│  │  ├─ Upload file to Firebase Storage
│  │  └─ Get download URL
│  └─ Skip if invalid URI
└─ Return list of download URLs
```

---

## 🔧 Helper Methods

### Resolve Brand Reference
```kotlin
resolveBrandRefByName(brandName)
├─ Query BrandNames collection where name = brandName
├─ Return DocumentReference if found
└─ Return null if not found
```

### Resolve Brand References
```kotlin
resolveBrandReferences(brandId)
├─ Resolve BrandNames reference
├─ Resolve Brand document reference
└─ Return Pair<brandDocRef, brandNameRef>
```

### Get Customer by Reference
```kotlin
getCustomerByReference(customerRef)
├─ Fetch document from Firestore
├─ Convert to Customer object
├─ Set customerId from document ID
└─ Return Customer
```

### Get Broker by Reference
```kotlin
getBrokerByReference(brokerRef)
├─ Fetch document from Firestore
├─ Convert to Broker object
├─ Set brokerId from document ID
└─ Return Broker
```

### Check Chassis Number Exists
```kotlin
checkChassisNumberExists(chassisNumber)
├─ Query ChassisNumber collection
└─ Return true if exists, false otherwise
```

### Get Colours
```kotlin
getColours()
├─ Fetch Colour document (fixed document ID)
├─ Extract colour array
└─ Return list of colour strings
```

### Add Colour
```kotlin
addColour(newColour)
├─ Update Colour document
├─ Use FieldValue.arrayUnion() to add colour
└─ Return success/error
```

### Create Catalog
```kotlin
createCatalog(productIds)
├─ Create new Catalog document
├─ Store product IDs, createdAt, productCount
└─ Return catalog document ID
```

### Get Product IDs by Brand and Models
```kotlin
getProductIdsByBrandAndModels(brandId, modelNames)
├─ Handle chunking (if > 10 models)
├─ Query Product collection
├─ Extract document IDs
└─ Return list of product document IDs
```

---

## 🎯 Key Design Patterns

### 1. **Reactive State Management**
- StateFlow provides reactive updates
- ViewModels observe StateFlow for automatic UI updates
- No manual cache invalidation needed

### 2. **Real-time Sync**
- Firestore listeners automatically sync changes
- Multiple listeners can be active simultaneously
- Products are merged and deduplicated

### 3. **Backward Compatibility**
- Supports both legacy (string) and new (reference) fields
- Queries try reference first, fallback to string field

### 4. **Transaction Safety**
- Critical operations use Firestore transactions
- Ensures data consistency (e.g., Brand update + Product creation)

### 5. **Smart File Upload**
- Preserves existing Firebase URLs
- Only uploads new local files
- Handles both images and PDFs

### 6. **Reference-Based Relationships**
- Uses DocumentReference for relationships
- Resolves references when needed
- Supports Broker, Customer, Brand, ChassisNumber references

---

## 🔄 Complete Flow Example: Adding a Vehicle

```
1. User fills form → ViewModel calls addVehicleToBrand()
   │
2. Repository.uploadImagesToStorage()
   ├─ Upload images to Firebase Storage
   └─ Get download URLs
   │
3. Repository.uploadPdfsToStorage() (x3 for NOC, RC, Insurance)
   ├─ Upload PDFs to Firebase Storage
   └─ Get download URLs
   │
4. Repository.resolveBrandRefByName()
   ├─ Query BrandNames collection
   └─ Get BrandNames DocumentReference
   │
5. Repository resolves Brand document reference
   │
6. Repository resolves Broker/Customer references
   │
7. Repository runs Firestore transaction:
   ├─ Update Brand document (vehicle summary)
   ├─ Create ChassisNumber document
   └─ Create Product document
   │
8. Firestore listeners detect changes
   ├─ Brand listener updates _brands StateFlow
   └─ Product listener updates _products StateFlow
   │
9. ViewModels observing StateFlow receive updates
   │
10. UI automatically refreshes
```

---

## 🛑 Cleanup Flow

### Stop Listening
```kotlin
stopListening()
├─ Remove brand listener
├─ Stop all product listeners
├─ Clear activeProductListeners map
├─ Clear listenerProducts map
└─ Clear _products StateFlow
```

### Stop Specific Listeners
- `stopListeningToProductsByBrandId(brandId)`
- `stopListeningToProductsByBrandIdAndProductId(brandId, productId)`
- `stopListeningToProductsByBrandIdAndModels(brandId, productIds)`
- `stopAllProductListeners()`

---

## 📝 Important Notes

1. **StateFlow as Cache**: StateFlow acts as an in-memory cache. No manual cache management needed.

2. **Listener Lifecycle**: 
   - Brand listener: Always active (started in init)
   - Product listeners: Started on-demand, should be stopped when not needed

3. **Deduplication**: Products are deduplicated by `chassisNumber` when merging from multiple listeners.

4. **Firestore Limits**: 
   - `whereIn` has a 10-item limit (handled by chunking)
   - Transactions have timeout limits

5. **Error Handling**: All operations return `Result<T>` for error handling.

6. **Reference Resolution**: Always tries reference-based lookup first, falls back to string field for backward compatibility.

7. **File Upload Strategy**: Smart upload that preserves existing URLs and only uploads new files.

---

## 🎓 Best Practices

1. **Observe StateFlow in ViewModels**: Don't call get methods repeatedly, observe StateFlow instead.

2. **Start Listeners Early**: Start product listeners when screen loads, not on every query.

3. **Stop Listeners When Done**: Clean up listeners when leaving screens to save resources.

4. **Use Transactions**: Always use transactions for operations that modify multiple documents.

5. **Handle Errors**: Always handle Result.failure() cases in ViewModels.

6. **Reference vs String**: Prefer using references for relationships, but support both for migration.







