# 📋 Complete Purchase & Sale Flow Documentation

## 🔵 **PURCHASE VEHICLE FLOW**

### **Entry Point:** `PurchaseRepository.addPurchaseWithVehicleAtomic()`

This is an **atomic transaction** that ensures all operations succeed or fail together.

---

### **Step-by-Step Purchase Flow:**

#### **PHASE 1: PREPARATION (Before Transaction)**
1. **Get MaxOrderNo Document Reference**
   - Checks if `maxOrderNoDocRef` exists in memory
   - If not, queries `MaxOrderNo` collection and gets first document
   - If no document exists, creates one with `maxOrderNo = 0`

2. **Get Capital Document References**
   - `Cash` document: `Capital/Cash`
   - `Bank` document: `Capital/Bank`
   - `Credit` document: `Capital/Credit`

3. **Determine Which Capital Documents Need Updates**
   - Analyzes `paymentMethods` map (keys: "cash", "bank", "credit")
   - Only includes capital documents that have payment amounts > 0

4. **Get Collection References**
   - `Product` collection
   - `ChassisNumber` collection

---

#### **PHASE 2: ATOMIC TRANSACTION** (`db.runTransaction`)

**All operations happen in ONE transaction - either all succeed or all fail!**

##### **2.1 READ PHASE (All reads must happen first)**

1. **Read MaxOrderNo Document**
   ```kotlin
   val maxOrderNoSnapshot = transaction.get(maxOrderNoDocRefLocal)
   val currentMax = getIntFromFirestore(maxOrderNoSnapshot.get("maxOrderNo"))
   val orderNumber = currentMax + 1
   ```

2. **Read Capital Documents**
   - Reads all capital documents that will be updated (Cash/Bank/Credit)
   - Stores snapshots for later use

3. **Read Brand Document**
   - Reads the brand document to update its vehicle array

##### **2.2 WRITE PHASE (All writes happen after reads)**

1. **Update MaxOrderNo** ✅
   ```kotlin
   transaction.update(maxOrderNoDocRefLocal, "maxOrderNo", orderNumber)
   ```
   - Increments order number atomically

2. **Create Purchase Document** ✅
   ```kotlin
   val purchaseDocRef = purchaseCollection.document()
   val purchaseData = hashMapOf(
       "purchaseId" to purchaseIdValue,
       "grandTotal" to grandTotal,        // Total including GST + broker fee
       "gstAmount" to gstAmount,          // GST amount only
       "orderNumber" to orderNumber,
       "paymentMethods" to paymentMethods, // Map: {"cash": "1000", "bank": "5000"}
       "vehicle" to vehicle,               // Vehicle details map
       "middleMan" to middleMan,
       "createdAt" to System.currentTimeMillis()
   )
   transaction.set(purchaseDocRef, purchaseData)
   ```

3. **Update Capital Documents** ✅ (MONEY GOES OUT - Purchase reduces capital)
   - For each payment method (cash/bank/credit):
     - Gets current balance from snapshot
     - **Subtracts** amount from balance: `newBalance = currentBalance - amount`
     - Adds transaction record to `transactions` array:
       ```kotlin
       transactionData = {
           "transactionDate": timestamp,
           "createdAt": timestamp,
           "orderNumber": orderNumber,
           "amount": amount,
           "reference": purchaseDocRef  // Links to purchase
       }
       ```
     - Updates capital document with new balance and transaction

4. **Update Brand's Vehicle Array** ✅
   - Gets current `vehicle` array from brand snapshot
   - Checks if productId already exists:
     - **If exists**: Increments `quantity` by 1
     - **If new**: Adds new vehicle entry with `quantity = 1`
   - Updates brand document

5. **Create Chassis Document** ✅
   ```kotlin
   val chassisRef = chassisCollection.document()
   transaction.set(chassisRef, {
       "chassisNumber": product.chassisNumber,
       "createdAt": FieldValue.serverTimestamp()
   })
   ```

6. **Create Product Document** ✅
   ```kotlin
   val newProductRef = productCollection.document()
   transaction.set(newProductRef, {
       "brandId": brandId,
       "productId": product.productId,
       "chassisNumber": product.chassisNumber,
       "chassisReference": chassisRef,
       "colour": product.colour,
       "condition": product.condition,
       "images": imageUrls,              // Already uploaded to Firebase Storage
       "kms": product.kms,
       "lastService": product.lastService,
       "previousOwners": product.previousOwners,
       "price": product.price,
       "type": product.type,
       "year": product.year,
       "noc": nocUrls,                    // PDF URLs
       "rc": rcUrls,                      // PDF URLs
       "insurance": insuranceUrls,        // PDF URLs
       "brokerOrMiddleMan": product.brokerOrMiddleMan,
       "owner": product.owner,
       "brandRef": brandNameRef,          // Optional reference
       "brokerOrMiddleManRef": brokerOrMiddleManRef,  // Optional
       "ownerRef": ownerRef               // Optional
   })
   ```

---

### **📊 Purchase Data Structure:**

**Purchase Document:**
```json
{
  "purchaseId": "abc123",
  "grandTotal": 50000.0,      // Base price + GST + broker fee
  "gstAmount": 9000.0,         // GST amount only
  "orderNumber": 1,
  "paymentMethods": {
    "cash": "10000",
    "bank": "30000",
    "credit": "10000"
  },
  "vehicle": {
    "brand": "Toyota",
    "model": "Camry",
    "chassisNumber": "CH123456"
  },
  "middleMan": "John Broker",
  "createdAt": 1234567890
}
```

**Capital Document (Cash/Bank/Credit):**
```json
{
  "balance": 100000.0,         // Current balance
  "transactions": [
    {
      "transactionDate": 1234567890,
      "createdAt": 1234567890,
      "orderNumber": 1,
      "amount": 10000.0,
      "reference": <DocumentReference to Purchase>
    }
  ]
}
```

---

## 🟢 **SELL VEHICLE FLOW**

### **Entry Point:** `SaleRepository.addSale()`

---

### **Step-by-Step Sale Flow:**

#### **1. Create VehicleSales Document** ✅
```kotlin
val saleDocRef = saleCollection.document()
val saleData = hashMapOf(
    "saleId": saleId,
    "customerRef": customerRef,          // Reference to Customer document
    "vehicleRef": vehicleRef,             // Reference to Product document
    "purchaseType": purchaseType,        // "FULL_PAYMENT", "DOWN_PAYMENT", or "EMI"
    "purchaseDate": Timestamp.now(),
    "totalAmount": totalAmount,          // Total sale price
    "downPayment": downPayment,          // Only for DOWN_PAYMENT or FULL_PAYMENT
    "status": "Active",
    "createdAt": Timestamp.now()
)
```

**If EMI:**
```kotlin
"emiDetails": {
    "interestRate": 12.0,
    "frequency": "MONTHLY",              // MONTHLY, QUARTERLY, YEARLY
    "installmentsCount": 12,
    "installmentAmount": 5000.0,
    "nextDueDate": Timestamp,
    "remainingInstallments": 12,
    "paidInstallments": 0,
    "lastPaidDate": null
}
```

#### **2. Add Down Payment to Capital** ✅ (MONEY COMES IN - Sale increases capital)
- **Only if** `purchaseType == "DOWN_PAYMENT"` or `"FULL_PAYMENT"` AND `downPayment > 0`
- Calls `addToCapital("Bank", downPayment, saleDocRef)`
- **Adds** amount to Bank balance: `newBalance = currentBalance + amount`
- Adds transaction record to Bank's transactions array

#### **3. Update Vehicle Sold Status** ✅
- Gets product by vehicle reference
- Updates product: `sold = true`

---

### **📊 Sale Data Structure:**

**VehicleSales Document:**
```json
{
  "saleId": "sale123",
  "customerRef": <DocumentReference to Customer>,
  "vehicleRef": <DocumentReference to Product>,
  "purchaseType": "EMI",
  "purchaseDate": Timestamp,
  "totalAmount": 500000.0,
  "downPayment": 100000.0,
  "status": "Active",
  "emiDetails": {
    "interestRate": 12.0,
    "frequency": "MONTHLY",
    "installmentsCount": 12,
    "installmentAmount": 35000.0,
    "nextDueDate": Timestamp,
    "remainingInstallments": 12,
    "paidInstallments": 0,
    "lastPaidDate": null
  },
  "createdAt": Timestamp
}
```

---

## 💰 **EMI PAYMENT FLOW**

### **Entry Point:** `SaleRepository.recordEmiPayment()`

#### **Step-by-Step EMI Payment:**

1. **Validate Payment** ✅
   - Checks `cashAmount + bankAmount > 0`

2. **Update EMI Details** ✅
   ```kotlin
   saleDocRef.update({
       "emiDetails.paidInstallments": newPaidInstallments,      // +1
       "emiDetails.remainingInstallments": newRemainingInstallments,  // -1
       "emiDetails.lastPaidDate": Timestamp(Date(paymentDate)),
       "emiDetails.nextDueDate": Timestamp(Date(nextDueDate)),  // Calculated based on frequency
       "status": newStatus  // "Completed" if remainingInstallments <= 0, else "Active"
   })
   ```

3. **Calculate Next Due Date** ✅
   - Based on `frequency`:
     - `MONTHLY`: Add 1 month
     - `QUARTERLY`: Add 3 months
     - `YEARLY`: Add 1 year

4. **Add Payment to Capital** ✅ (MONEY COMES IN)
   - If `cashAmount > 0`: Adds to Cash capital
   - If `bankAmount > 0`: Adds to Bank capital
   - **Adds** amount to balance: `newBalance = currentBalance + amount`
   - Adds transaction record with sale reference

---

## 📊 **TAXATION MANAGEMENT**

### **GST Calculation (Purchase Only)**

#### **Location:** `PurchaseVehicleViewModel` and `PurchaseVehicleScreen`

#### **GST Calculation Logic:**

```kotlin
// Base price (vehicle price)
val basePrice = price.toDoubleOrNull() ?: 0.0

// GST rate (user enters percentage, e.g., 18%)
val gstRate = (gstPercentage.toDoubleOrNull() ?: 0.0) / 100.0

// GST amount calculation (only if GST checkbox is checked)
val gstAmount = if (gstIncluded && gstRate > 0) {
    basePrice * gstRate  // GST = Base Price × GST Rate
} else {
    0.0  // No GST if checkbox not checked
}

// Grand total (base price + GST)
val grandTotal = if (gstIncluded && gstRate > 0) {
    basePrice + gstAmount  // Base + GST
} else {
    basePrice  // No GST added
}
```

#### **GST Storage:**

**In Purchase Document:**
- `gstAmount`: Stores the GST amount separately (e.g., 9000.0)
- `grandTotal`: Stores total including GST (e.g., 59000.0 = 50000 + 9000)

#### **Important Notes:**

1. **GST is Optional:**
   - User can check/uncheck "Include GST" checkbox
   - Default GST percentage: 18%
   - If unchecked, `gstAmount = 0` and `grandTotal = basePrice`

2. **GST Only on Purchase:**
   - **Purchase**: GST is calculated and stored
   - **Sale**: No GST calculation (sale price is the final amount)

3. **GST Tracking:**
   - `gstAmount` is stored separately for reporting/accounting
   - Can be used to calculate total GST paid over time

4. **Broker Fee Handling:**
   - Broker fee is separate from GST
   - Broker fee is added to `grandTotal` but NOT included in `gstAmount`
   - Formula: `totalGrandTotal = grandTotal + brokerFee`

---

## 🔄 **CAPITAL MANAGEMENT**

### **Capital Flow:**

#### **Purchase (Money Goes Out):**
- **Capital Balance DECREASES** ❌
- `newBalance = currentBalance - amount`
- Transaction type: **DEBIT**

#### **Sale (Money Comes In):**
- **Capital Balance INCREASES** ✅
- `newBalance = currentBalance + amount`
- Transaction type: **CREDIT**

#### **Capital Documents:**
- `Capital/Cash` - Cash transactions
- `Capital/Bank` - Bank transactions
- `Capital/Credit` - Credit transactions

Each document contains:
- `balance`: Current balance (Double)
- `transactions`: Array of transaction records with:
  - `transactionDate`: Timestamp
  - `createdAt`: Timestamp
  - `orderNumber`: Int (links to Purchase/Sale)
  - `amount`: Double
  - `reference`: DocumentReference (links to Purchase or Sale document)

---

## 📈 **COMPLETE FLOW SUMMARY**

### **Purchase Flow:**
```
1. User enters vehicle details + payment methods
2. GST calculated (if enabled)
3. Atomic transaction:
   - Increment order number
   - Create Purchase document (with GST)
   - Subtract from Capital (Cash/Bank/Credit)
   - Update Brand inventory
   - Create Chassis document
   - Create Product document
4. All succeed or all fail (atomicity)
```

### **Sale Flow:**
```
1. User selects customer + vehicle
2. Choose payment type (Full/Down Payment/EMI)
3. Create VehicleSales document
4. Add down payment to Capital (if applicable)
5. Mark vehicle as sold
```

### **EMI Payment Flow:**
```
1. User records EMI payment (cash + bank amounts)
2. Update EMI details (paid/remaining installments)
3. Calculate next due date
4. Add payment to Capital (Cash/Bank)
5. Mark sale as "Completed" if all installments paid
```

---

## 🎯 **KEY DIFFERENCES**

| Aspect | Purchase | Sale |
|--------|----------|------|
| **GST** | ✅ Calculated & stored | ❌ Not calculated |
| **Capital** | ❌ Decreases (money out) | ✅ Increases (money in) |
| **Order Number** | ✅ Uses MaxOrderNo | ❌ Not used |
| **Vehicle Status** | ✅ Added to inventory | ✅ Marked as sold |
| **Transaction Type** | DEBIT | CREDIT |

---

## 🔐 **ATOMICITY GUARANTEES**

### **Purchase (`addPurchaseWithVehicleAtomic`):**
- ✅ All 6 operations in single transaction
- ✅ If any fails, entire transaction rolls back
- ✅ Prevents data inconsistency

### **Sale (`addSale`):**
- ⚠️ **NOT atomic** - multiple separate operations
- Sale document creation
- Capital update
- Vehicle status update
- If one fails, others may still succeed (potential inconsistency)

---

## 📝 **IMPORTANT NOTES**

1. **GST is only calculated during purchase, not sale**
2. **Capital decreases on purchase, increases on sale**
3. **Purchase uses atomic transaction for data consistency**
4. **Sale operations are separate (not atomic)**
5. **Order numbers are sequential and managed via MaxOrderNo collection**
6. **All capital transactions are linked to Purchase/Sale via DocumentReference**


