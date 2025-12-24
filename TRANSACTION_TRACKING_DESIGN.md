# 💳 Transaction Tracking System Design

## 🎯 **Goal**
Track all financial transactions involving customers and brokers, making them accessible when viewing customer/broker details or through a dedicated transactions screen.

---

## 📊 **Transaction Types to Track**

### 1. **PURCHASE Transactions**
- **When:** Vehicle purchase is made
- **Who Involved:**
  - **Broker** (if purchase type = "Broker")
  - **Middle Man** (if purchase type = "Middle Man" - stored as Customer)
  - **Owner** (if owner is specified - stored as Customer)
- **Details:**
  - Broker fee amount
  - Payment methods (Cash/Bank/Credit)
  - Order number
  - Purchase reference

### 2. **SALE Transactions**
- **When:** Vehicle is sold
- **Who Involved:**
  - **Customer** (buyer)
- **Details:**
  - Total sale amount
  - Down payment amount
  - Payment type (Full Payment/Down Payment/EMI)
  - Sale reference

### 3. **EMI_PAYMENT Transactions**
- **When:** EMI installment is paid
- **Who Involved:**
  - **Customer** (buyer making payment)
- **Details:**
  - Payment amount (Cash + Bank)
  - Installment number
  - Sale reference

### 4. **BROKER_FEE Transactions**
- **When:** Broker fee is paid during purchase
- **Who Involved:**
  - **Broker** (receiving fee)
- **Details:**
  - Broker fee amount
  - Payment methods (Cash/Bank/Credit)
  - Purchase reference

---

## 🗂️ **Recommended Architecture: Separate Transactions Collection**

### **Why Separate Collection?**
✅ **Scalability** - Doesn't bloat Customer/Broker documents  
✅ **Flexibility** - Easy to query, filter, and sort  
✅ **Performance** - Can index by person, date, type  
✅ **Maintainability** - Centralized transaction logic  
✅ **Reporting** - Easy to generate transaction reports  

---

## 📋 **Data Structure**

### **Transaction Document** (`Transactions/{transactionId}`)

```kotlin
data class PersonTransaction(
    val transactionId: String = "",              // Auto-generated document ID
    val type: TransactionType,                  // PURCHASE, SALE, EMI_PAYMENT, BROKER_FEE
    val personType: PersonType,                  // CUSTOMER, BROKER, MIDDLE_MAN
    val personRef: DocumentReference,           // Reference to Customer or Broker document
    val personName: String,                     // Cached name for quick display
    val relatedRef: DocumentReference,         // Reference to Purchase, Sale, or Product
    val amount: Double,                         // Transaction amount
    val paymentMethod: PaymentMethod,           // CASH, BANK, CREDIT, MIXED
    val cashAmount: Double = 0.0,               // Cash portion
    val bankAmount: Double = 0.0,               // Bank portion
    val creditAmount: Double = 0.0,            // Credit portion
    val date: Timestamp,                        // Transaction date
    val orderNumber: Int? = null,               // Order number (for purchases)
    val description: String,                    // Human-readable description
    val status: String = "COMPLETED",           // COMPLETED, PENDING, CANCELLED
    val createdAt: Timestamp                    // Record creation timestamp
)

enum class TransactionType {
    PURCHASE,           // Vehicle purchase
    SALE,               // Vehicle sale
    EMI_PAYMENT,        // EMI installment payment
    BROKER_FEE         // Broker fee payment
}

enum class PersonType {
    CUSTOMER,           // Regular customer
    BROKER,             // Broker
    MIDDLE_MAN          // Middle man (stored as Customer)
}

enum class PaymentMethod {
    CASH,
    BANK,
    CREDIT,
    MIXED               // Multiple payment methods
}
```

### **Example Transaction Documents:**

#### **Purchase Transaction (Broker Fee):**
```json
{
  "transactionId": "txn_abc123",
  "type": "BROKER_FEE",
  "personType": "BROKER",
  "personRef": <DocumentReference to Broker/broker123>,
  "personName": "John Broker",
  "relatedRef": <DocumentReference to Purchase/purchase456>,
  "amount": 5000.0,
  "paymentMethod": "MIXED",
  "cashAmount": 2000.0,
  "bankAmount": 2000.0,
  "creditAmount": 1000.0,
  "date": Timestamp,
  "orderNumber": 42,
  "description": "Broker fee for vehicle purchase - Order #42",
  "status": "COMPLETED",
  "createdAt": Timestamp
}
```

#### **Sale Transaction (Customer):**
```json
{
  "transactionId": "txn_xyz789",
  "type": "SALE",
  "personType": "CUSTOMER",
  "personRef": <DocumentReference to Customer/customer789>,
  "personName": "Jane Customer",
  "relatedRef": <DocumentReference to VehicleSales/sale123>,
  "amount": 500000.0,
  "paymentMethod": "BANK",
  "cashAmount": 0.0,
  "bankAmount": 100000.0,
  "creditAmount": 0.0,
  "date": Timestamp,
  "orderNumber": null,
  "description": "Vehicle sale - Down payment",
  "status": "COMPLETED",
  "createdAt": Timestamp
}
```

#### **EMI Payment Transaction:**
```json
{
  "transactionId": "txn_emi001",
  "type": "EMI_PAYMENT",
  "personType": "CUSTOMER",
  "personRef": <DocumentReference to Customer/customer789>,
  "personName": "Jane Customer",
  "relatedRef": <DocumentReference to VehicleSales/sale123>,
  "amount": 35000.0,
  "paymentMethod": "MIXED",
  "cashAmount": 10000.0,
  "bankAmount": 25000.0,
  "creditAmount": 0.0,
  "date": Timestamp,
  "orderNumber": null,
  "description": "EMI Payment - Installment 3/12",
  "status": "COMPLETED",
  "createdAt": Timestamp
}
```

---

## 🔧 **Implementation Plan**

### **Step 1: Create TransactionRepository**

**File:** `app/src/main/java/com/example/cardealer2/repository/TransactionRepository.kt`

**Key Functions:**
```kotlin
object TransactionRepository {
    // Create transaction record
    suspend fun createTransaction(transaction: PersonTransaction): Result<String>
    
    // Get all transactions for a person (Customer or Broker)
    suspend fun getTransactionsByPerson(personRef: DocumentReference): Result<List<PersonTransaction>>
    
    // Get transactions by type
    suspend fun getTransactionsByType(type: TransactionType): Result<List<PersonTransaction>>
    
    // Get transactions by date range
    suspend fun getTransactionsByDateRange(startDate: Timestamp, endDate: Timestamp): Result<List<PersonTransaction>>
    
    // Get all transactions (for separate screen)
    val transactions: StateFlow<List<PersonTransaction>>
    
    // Start listening to transactions
    fun startListening()
}
```

### **Step 2: Modify Purchase Flow**

**In `PurchaseRepository.addPurchaseWithVehicleAtomic()`:**

After creating purchase, create transaction records:

```kotlin
// After purchase is created successfully:

// 1. Create BROKER_FEE transaction (if broker involved)
brokerOrMiddleManRef?.let { ref ->
    // Check if it's a broker (not middle man)
    if (purchaseType == "Broker") {
        val brokerFeeAmount = brokerFee.toDoubleOrNull() ?: 0.0
        if (brokerFeeAmount > 0) {
            TransactionRepository.createTransaction(
                PersonTransaction(
                    type = TransactionType.BROKER_FEE,
                    personType = PersonType.BROKER,
                    personRef = ref,
                    personName = middleMan, // broker name
                    relatedRef = purchaseDocRef,
                    amount = brokerFeeAmount,
                    paymentMethod = determinePaymentMethod(paymentMethods),
                    cashAmount = brokerFeeCashAmount,
                    bankAmount = brokerFeeBankAmount,
                    creditAmount = brokerFeeCreditAmount,
                    date = Timestamp.now(),
                    orderNumber = orderNumber,
                    description = "Broker fee - Order #$orderNumber"
                )
            )
        }
    }
}

// 2. Create PURCHASE transaction for Middle Man (if middle man involved)
if (purchaseType == "Middle Man" && brokerOrMiddleManRef != null) {
    TransactionRepository.createTransaction(
        PersonTransaction(
            type = TransactionType.PURCHASE,
            personType = PersonType.MIDDLE_MAN,
            personRef = brokerOrMiddleManRef,
            personName = middleMan,
            relatedRef = purchaseDocRef,
            amount = grandTotal,
            paymentMethod = determinePaymentMethod(paymentMethods),
            cashAmount = cashAmount,
            bankAmount = bankAmount,
            creditAmount = creditAmount,
            date = Timestamp.now(),
            orderNumber = orderNumber,
            description = "Vehicle purchase - Order #$orderNumber"
        )
    )
}

// 3. Create PURCHASE transaction for Owner (if owner specified)
ownerRef?.let { ref ->
    TransactionRepository.createTransaction(
        PersonTransaction(
            type = TransactionType.PURCHASE,
            personType = PersonType.CUSTOMER,
            personRef = ref,
            personName = product.owner,
            relatedRef = purchaseDocRef,
            amount = grandTotal,
            paymentMethod = determinePaymentMethod(paymentMethods),
            cashAmount = cashAmount,
            bankAmount = bankAmount,
            creditAmount = creditAmount,
            date = Timestamp.now(),
            orderNumber = orderNumber,
            description = "Vehicle purchase - Order #$orderNumber"
        )
    )
}
```

### **Step 3: Modify Sale Flow**

**In `SaleRepository.addSale()`:**

After sale is created, create transaction record:

```kotlin
// After sale is created successfully:

customerRef?.let { ref ->
    // Get customer name
    val customerName = getCustomerName(ref) // Helper function
    
    TransactionRepository.createTransaction(
        PersonTransaction(
            type = TransactionType.SALE,
            personType = PersonType.CUSTOMER,
            personRef = ref,
            personName = customerName,
            relatedRef = saleDocRef,
            amount = if (purchaseType == "FULL_PAYMENT") totalAmount else downPayment,
            paymentMethod = PaymentMethod.BANK, // Down payment goes to Bank
            cashAmount = 0.0,
            bankAmount = if (purchaseType == "FULL_PAYMENT") totalAmount else downPayment,
            creditAmount = 0.0,
            date = Timestamp.now(),
            orderNumber = null,
            description = when (purchaseType) {
                "FULL_PAYMENT" -> "Vehicle sale - Full payment"
                "DOWN_PAYMENT" -> "Vehicle sale - Down payment"
                else -> "Vehicle sale - EMI initiated"
            }
        )
    )
}
```

### **Step 4: Modify EMI Payment Flow**

**In `SaleRepository.recordEmiPayment()`:**

After EMI payment is recorded, create transaction record:

```kotlin
// After EMI payment is recorded:

// Get sale document to find customer
val sale = docToVehicleSale(saleDoc)
sale?.customerRef?.let { customerRef ->
    val customerName = getCustomerName(customerRef)
    val installmentNumber = emiDetails.paidInstallments + 1
    val totalInstallments = emiDetails.installmentsCount
    
    TransactionRepository.createTransaction(
        PersonTransaction(
            type = TransactionType.EMI_PAYMENT,
            personType = PersonType.CUSTOMER,
            personRef = customerRef,
            personName = customerName,
            relatedRef = saleDocRef,
            amount = cashAmount + bankAmount,
            paymentMethod = if (cashAmount > 0 && bankAmount > 0) PaymentMethod.MIXED 
                           else if (cashAmount > 0) PaymentMethod.CASH 
                           else PaymentMethod.BANK,
            cashAmount = cashAmount,
            bankAmount = bankAmount,
            creditAmount = 0.0,
            date = Timestamp(Date(paymentDate)),
            orderNumber = null,
            description = "EMI Payment - Installment $installmentNumber/$totalInstallments"
        )
    )
}
```

---

## 🖥️ **UI Implementation Options**

### **Option 1: Embedded in Customer/Broker Detail Screen** ✅ **Recommended**

**Location:** Customer/Broker detail/compose screen

**Layout:**
```
┌─────────────────────────────────┐
│ Customer/Broker Details         │
│ Name, Phone, Address, etc.      │
├─────────────────────────────────┤
│ Transaction History             │
│ ┌─────────────────────────────┐ │
│ │ ₹50,000 - Sale - 2024-01-15 │ │
│ │ ₹35,000 - EMI - 2024-02-15  │ │
│ │ ₹35,000 - EMI - 2024-03-15  │ │
│ └─────────────────────────────┘ │
│ [View All Transactions]         │
└─────────────────────────────────┘
```

**Benefits:**
- Quick access to recent transactions
- Contextual information
- Easy to navigate

### **Option 2: Separate Transactions Screen**

**Navigation:** 
- From Customer/Broker list → "View Transactions"
- From Customer/Broker detail → "All Transactions"
- Main menu → "All Transactions"

**Features:**
- Filter by person, type, date range
- Sort by date, amount
- Search functionality
- Export to PDF/Excel

**Layout:**
```
┌─────────────────────────────────┐
│ All Transactions                │
├─────────────────────────────────┤
│ [Filter: All] [Type ▼] [Date ▼]│
├─────────────────────────────────┤
│ John Broker                     │
│ ₹5,000 - Broker Fee - Jan 15    │
│                                 │
│ Jane Customer                   │
│ ₹500,000 - Sale - Jan 10        │
│ ₹35,000 - EMI - Feb 15          │
└─────────────────────────────────┘
```

### **Option 3: Both** ✅ **Best User Experience**

- **Embedded list** in detail screen (last 5-10 transactions)
- **Separate screen** for full transaction history with filters

---

## 📱 **UI Components Needed**

### **1. Transaction List Item**
```kotlin
@Composable
fun TransactionItem(
    transaction: PersonTransaction,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = getTransactionTypeColor(transaction.type)
                )
            }
            Text(
                text = "₹${formatAmount(transaction.amount)}",
                style = MaterialTheme.typography.titleLarge,
                color = getTransactionAmountColor(transaction.type)
            )
        }
    }
}
```

### **2. Transaction Detail Screen**
```kotlin
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit
) {
    // Show full transaction details:
    // - Person name
    // - Transaction type
    // - Amount breakdown (Cash/Bank/Credit)
    // - Date
    // - Related purchase/sale details
    // - Status
}
```

### **3. Transaction Filter Dialog**
```kotlin
@Composable
fun TransactionFilterDialog(
    onFilterApplied: (filter: TransactionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    // Filter options:
    // - Transaction type
    // - Date range
    // - Amount range
    // - Payment method
}
```

---

## 🔍 **Query Patterns**

### **Get Transactions for Customer:**
```kotlin
transactionsCollection
    .whereEqualTo("personRef", customerRef)
    .orderBy("date", Query.Direction.DESCENDING)
    .limit(10)
    .get()
```

### **Get Transactions for Broker:**
```kotlin
transactionsCollection
    .whereEqualTo("personRef", brokerRef)
    .whereEqualTo("personType", PersonType.BROKER)
    .orderBy("date", Query.Direction.DESCENDING)
    .get()
```

### **Get All Transactions (with pagination):**
```kotlin
transactionsCollection
    .orderBy("date", Query.Direction.DESCENDING)
    .limit(50)
    .get()
```

### **Get Transactions by Date Range:**
```kotlin
transactionsCollection
    .whereGreaterThanOrEqualTo("date", startDate)
    .whereLessThanOrEqualTo("date", endDate)
    .orderBy("date", Query.Direction.DESCENDING)
    .get()
```

---

## 📊 **Firestore Indexes Needed**

```javascript
// Index 1: Query by person and date
{
  collectionGroup: "Transactions",
  fields: [
    { fieldPath: "personRef", order: "ASCENDING" },
    { fieldPath: "date", order: "DESCENDING" }
  ]
}

// Index 2: Query by type and date
{
  collectionGroup: "Transactions",
  fields: [
    { fieldPath: "type", order: "ASCENDING" },
    { fieldPath: "date", order: "DESCENDING" }
  ]
}

// Index 3: Query by person type and date
{
  collectionGroup: "Transactions",
  fields: [
    { fieldPath: "personType", order: "ASCENDING" },
    { fieldPath: "date", order: "DESCENDING" }
  ]
}
```

---

## ✅ **Implementation Checklist**

### **Phase 1: Backend (Repository)**
- [ ] Create `PersonTransaction` data class
- [ ] Create `TransactionRepository` with CRUD operations
- [ ] Add transaction creation in `addPurchaseWithVehicleAtomic()`
- [ ] Add transaction creation in `addSale()`
- [ ] Add transaction creation in `recordEmiPayment()`
- [ ] Add StateFlow listener for real-time updates

### **Phase 2: UI Components**
- [ ] Create `TransactionItem` composable
- [ ] Create `TransactionList` composable
- [ ] Create `TransactionDetailScreen` composable
- [ ] Create `TransactionFilterDialog` composable

### **Phase 3: Integration**
- [ ] Add transaction list to Customer detail screen
- [ ] Add transaction list to Broker detail screen
- [ ] Create separate "All Transactions" screen
- [ ] Add navigation routes
- [ ] Add filtering and sorting

### **Phase 4: Testing**
- [ ] Test transaction creation for purchases
- [ ] Test transaction creation for sales
- [ ] Test transaction creation for EMI payments
- [ ] Test querying transactions by person
- [ ] Test filtering and sorting

---

## 🎨 **UI Mockup Suggestions**

### **Customer Detail Screen with Transactions:**
```
┌─────────────────────────────────────┐
│ ← Back    Customer Details          │
├─────────────────────────────────────┤
│ 👤 Jane Customer                    │
│ 📞 +91 9876543210                   │
│ 📍 123 Main St, City                │
│                                     │
│ 💰 Balance: ₹-350,000 (Owed)       │
├─────────────────────────────────────┤
│ Transaction History                 │
│ ┌─────────────────────────────────┐ │
│ │ Sale                            │ │
│ │ ₹500,000 • Jan 10, 2024         │ │
│ │ Down Payment                    │ │
│ ├─────────────────────────────────┤ │
│ │ EMI Payment                     │ │
│ │ ₹35,000 • Feb 15, 2024          │ │
│ │ Installment 1/12                │ │
│ ├─────────────────────────────────┤ │
│ │ EMI Payment                     │ │
│ │ ₹35,000 • Mar 15, 2024          │ │
│ │ Installment 2/12                │ │
│ └─────────────────────────────────┘ │
│ [View All Transactions →]           │
└─────────────────────────────────────┘
```

---

## 💡 **Additional Features (Future Enhancements)**

1. **Transaction Summary**
   - Total amount received/paid
   - Outstanding balance
   - Payment trends

2. **Notifications**
   - Remind customers about upcoming EMI payments
   - Notify about overdue payments

3. **Reports**
   - Monthly transaction reports
   - Customer payment history PDF
   - Broker commission reports

4. **Search**
   - Search transactions by description
   - Search by order number
   - Search by amount range

---

## 🚀 **Quick Start Implementation Order**

1. **Start with TransactionRepository** - Core functionality
2. **Add to Purchase flow** - Most complex, sets pattern
3. **Add to Sale flow** - Simpler, follows pattern
4. **Add to EMI flow** - Simplest, completes the cycle
5. **Create UI components** - Build reusable components
6. **Integrate into existing screens** - Add to detail screens
7. **Create separate screen** - Full transaction management

---

This design provides a complete transaction tracking system that integrates seamlessly with your existing purchase and sale flows! 🎉


