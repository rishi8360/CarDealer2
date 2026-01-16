package com.example.cardealer2.ViewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardealer2.data.Broker
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Product
import com.example.cardealer2.repository.BrokerRepository
import com.example.cardealer2.repository.CustomerRepository
import com.example.cardealer2.repository.PurchaseRepository
import com.example.cardealer2.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PurchaseVehicleUiState(
    val isLoading: Boolean = false,
    val customerNames: List<String> = emptyList(),
    val brokerNames: List<String> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val brokers: List<Broker> = emptyList(),
    val maxOrderNo: Int = 0,
    val maxTransactionNo: Int = 0,
    val error: String? = null,
    val isAddingCustomer: Boolean = false,
    val addCustomerSuccess: Boolean = false,
    val addCustomerError: String? = null,
    val isAddingBroker: Boolean = false,
    val addBrokerSuccess: Boolean = false,
    val addBrokerError: String? = null,
    val isPurchasingVehicle: Boolean = false,
    val purchaseVehicleSuccess: Boolean = false,
    val purchaseVehicleError: String? = null
)

class PurchaseVehicleViewModel(
    private val customerRepository: CustomerRepository = CustomerRepository,
    private val brokerRepository: BrokerRepository = BrokerRepository,
    private val vehicleRepository: VehicleRepository = VehicleRepository,
    private val purchaseRepository: PurchaseRepository = PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseVehicleUiState())
    val uiState: StateFlow<PurchaseVehicleUiState> = _uiState

    init {
        // 🔹 Observe repository StateFlows and automatically update UiState
        viewModelScope.launch {
            // Collect customers from repository StateFlow (updated via Firebase listener)
            customerRepository.customers.collect { customers ->
                _uiState.value = _uiState.value.copy(
                    customers = customers,
                    customerNames = customers.map { it.name }
                )
            }
        }
        
        viewModelScope.launch {
            // Collect brokers from repository StateFlow (updated via Firebase listener)
            brokerRepository.brokers.collect { brokers ->
                _uiState.value = _uiState.value.copy(
                    brokers = brokers,
                    brokerNames = brokers.map { it.name }
                )
            }
        }
        
        viewModelScope.launch {
            // Collect maxOrderNo from repository StateFlow (updated via Firebase listener)
            purchaseRepository.maxOrderNo.collect { maxOrderNo ->
                _uiState.value = _uiState.value.copy(maxOrderNo = maxOrderNo)
            }
        }
        
        viewModelScope.launch {
            // Collect maxTransactionNo from repository StateFlow (updated via Firebase listener)
            purchaseRepository.maxTransactionNo.collect { maxTransactionNo ->
                _uiState.value = _uiState.value.copy(maxTransactionNo = maxTransactionNo)
            }
        }
    }
    
    /**
     * @deprecated No longer needed - customers are automatically loaded via Firebase listener in repository
     * Customers StateFlow updates automatically when Firestore changes
     */
    @Deprecated("Customers are automatically loaded via Firebase listener in repository")
    fun loadCustomerNames() {
        // No-op: Repository listener handles this automatically
    }

    /**
     * @deprecated No longer needed - brokers are automatically loaded via Firebase listener in repository
     * Brokers StateFlow updates automatically when Firestore changes
     */
    @Deprecated("Brokers are automatically loaded via Firebase listener in repository")
    fun loadBrokerNames() {
        // No-op: Repository listener handles this automatically
    }

    /**
     * @deprecated No longer needed - maxOrderNo is automatically loaded via Firebase listener in repository
     * MaxOrderNo StateFlow updates automatically when Firestore changes
     */
    @Deprecated("MaxOrderNo is automatically loaded via Firebase listener in repository")
    fun loadMaxOrderNo() {
        // No-op: Repository listener handles this automatically
    }

    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        photoUris: List<Uri>,
        idProofType: String,
        idProofNumber: String,
        idProofImageUrls: List<String>,
        amount: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAddingCustomer = true,
                addCustomerError = null,
                addCustomerSuccess = false
            )
            try {
                val customer = Customer(
                    customerId = "",
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    photoUrl = photoUris.map { it.toString() },
                    idProofType = idProofType,
                    idProofNumber = idProofNumber.trim(),
                    idProofImageUrls = idProofImageUrls,
                    amount = amount,
                    createdAt = System.currentTimeMillis()
                )
                val result = customerRepository.addCustomer(customer)
                if (result.isSuccess) {
                    // ✅ No need to reload - Firebase listener will automatically update StateFlow
                    _uiState.value = _uiState.value.copy(
                        isAddingCustomer = false,
                        addCustomerSuccess = true,
                        addCustomerError = null
                    )
                    // Reset success flag after a short delay
                    kotlinx.coroutines.delay(1000)
                    _uiState.value = _uiState.value.copy(addCustomerSuccess = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAddingCustomer = false,
                        addCustomerError = result.exceptionOrNull()?.message ?: "Failed to add customer"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingCustomer = false,
                    addCustomerError = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun addBroker(
        name: String,
        phoneNumber: String,
        address: String,
        idProof: List<String>,
        brokerBill: List<String>,
        amount: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAddingBroker = true,
                addBrokerError = null,
                addBrokerSuccess = false
            )
            try {
                val broker = Broker(
                    brokerId = "",
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    idProof = idProof,
                    address = address.trim(),
                    brokerBill = brokerBill,
                    amount = amount,
                    createdAt = System.currentTimeMillis()
                )
                val result = brokerRepository.addBroker(broker)
                if (result.isSuccess) {
                    // ✅ No need to reload - Firebase listener will automatically update StateFlow
                    _uiState.value = _uiState.value.copy(
                        isAddingBroker = false,
                        addBrokerSuccess = true,
                        addBrokerError = null
                    )
                    // Reset success flag after a short delay
                    kotlinx.coroutines.delay(1000)
                    _uiState.value = _uiState.value.copy(addBrokerSuccess = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAddingBroker = false,
                        addBrokerError = result.exceptionOrNull()?.message ?: "Failed to add broker"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingBroker = false,
                    addBrokerError = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun clearAddCustomerError() {
        _uiState.value = _uiState.value.copy(addCustomerError = null)
    }

    fun clearAddBrokerError() {
        _uiState.value = _uiState.value.copy(addBrokerError = null)
    }
    
    fun purchaseVehicle(
        brandId: String,
        modelName: String,
        colour: String,
        chassisNumber: String,
        condition: String,
        images: List<Uri>,
        kms: String,
        lastService: String,
        previousOwners: String,
        price: String,
        sellingPrice: String,
        year: String,
        type: String,
        purchaseType: String?,
        selectedCustomerMiddleManBroker: String?,
        selectedOwner: String?,
        nocPdfs: List<String>,
        rcPdfs: List<String>,
        insurancePdfs: List<String>,
        vehicleOtherDocPdfs: List<String>,
        gstIncluded: Boolean,
        gstPercentage: String,
        brokerFeeIncluded: Boolean,
        brokerFeeAmount: String,
        brokerFeeCash: String,
        brokerFeeBank: String,
        cashAmount: String,
        bankAmount: String,
        creditAmount: String,
        note: String = "",
        date: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPurchasingVehicle = true,
                purchaseVehicleError = null,
                purchaseVehicleSuccess = false
            )
            try {
                // Calculate price with GST (only if GST checkbox is checked)
                val basePrice = price.toDoubleOrNull() ?: 0.0
                val gstRate = (gstPercentage.toDoubleOrNull() ?: 0.0) / 100.0
                val gstAmount = if (gstIncluded && gstRate > 0) {
                    basePrice * gstRate // GST amount = basePrice * GST rate
                } else {
                    0.0 // No GST if checkbox is not checked
                }
                // Grand total = basePrice + GST (if GST is included)
                val grandTotal = if (gstIncluded && gstRate > 0) {
                    basePrice + gstAmount // Add GST to base price
                } else {
                    basePrice // No GST added
                }
                
                // Calculate broker fee amounts (only if brokerFeeIncluded is true)
                val brokerFee = if (brokerFeeIncluded) (brokerFeeAmount.toDoubleOrNull() ?: 0.0) else 0.0
                val brokerFeeCashAmount = if (brokerFeeIncluded) (brokerFeeCash.toDoubleOrNull() ?: 0.0) else 0.0
                val brokerFeeBankAmount = if (brokerFeeIncluded) (brokerFeeBank.toDoubleOrNull() ?: 0.0) else 0.0
                val brokerFeeCreditAmount = if (brokerFeeIncluded) {
                    maxOf(0.0, brokerFee - (brokerFeeCashAmount + brokerFeeBankAmount))
                } else {
                    0.0
                }
                
                // Total payment amounts (vehicle + broker fee)
                val vehicleCashAmount = cashAmount.toDoubleOrNull() ?: 0.0
                val vehicleBankAmount = bankAmount.toDoubleOrNull() ?: 0.0
                val vehicleCreditAmount = creditAmount.toDoubleOrNull() ?: 0.0
                val totalCash = vehicleCashAmount + brokerFeeCashAmount
                val totalBank = vehicleBankAmount + brokerFeeBankAmount
                val totalCredit = vehicleCreditAmount + brokerFeeCreditAmount
                
                // Determine brokerOrMiddleMan and owner based on purchase type
                val brokerOrMiddleMan = when (purchaseType) {
                    "Direct" -> ""
                    "Middle Man", "Broker" -> selectedCustomerMiddleManBroker ?: ""
                    else -> ""
                }
                
                val owner = when (purchaseType) {
                    "Direct", "Middle Man", "Broker" -> selectedOwner ?: ""
                    else -> ""
                }
                
                // Resolve IDs from names
                val brokerOrMiddleManId = when (purchaseType) {
                    "Direct" -> null
                    "Middle Man" -> {
                        // Middle Man is a customer
                        val customer = _uiState.value.customers.find { it.name == selectedCustomerMiddleManBroker }
                        val id = customer?.customerId
                        println("🔍 Middle Man - Selected: $selectedCustomerMiddleManBroker, Found customer: ${customer?.name}, ID: $id")
                        id
                    }
                    "Broker" -> {
                        // Broker is a broker
                        val broker = _uiState.value.brokers.find { it.name == selectedCustomerMiddleManBroker }
                        val id = broker?.brokerId
                        println("🔍 Broker - Selected: $selectedCustomerMiddleManBroker, Found broker: ${broker?.name}, ID: $id")
                        id
                    }
                    else -> null
                }
                
                val ownerId = when (purchaseType) {
                    "Direct", "Middle Man", "Broker" -> {
                        // Owner is a customer
                        val customer = _uiState.value.customers.find { it.name == selectedOwner }
                        val id = customer?.customerId
                        println("🔍 Owner - Selected: $selectedOwner, Found customer: ${customer?.name}, ID: $id")
                        id
                    }
                    else -> null
                }
                
                println("📦 Purchase Type: $purchaseType, brokerOrMiddleManId: $brokerOrMiddleManId, ownerId: $ownerId")
                
                // 1️⃣ Upload images and PDFs first (cannot be in transaction)
                val imageUploadResult = vehicleRepository.uploadImagesToStorage(brandId, modelName, images)
                if (imageUploadResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isPurchasingVehicle = false,
                        purchaseVehicleError = imageUploadResult.exceptionOrNull()?.message ?: "Failed to upload images"
                    )
                    return@launch
                }
                val imageUrls = imageUploadResult.getOrThrow()
                
                // Upload PDFs
                val nocUrlsResult = vehicleRepository.uploadPdfsToStorage(nocPdfs, brandId, modelName, "noc")
                val rcUrlsResult = vehicleRepository.uploadPdfsToStorage(rcPdfs, brandId, modelName, "rc")
                val insuranceUrlsResult = vehicleRepository.uploadPdfsToStorage(insurancePdfs, brandId, modelName, "insurance")
                val vehicleOtherDocUrlsResult = vehicleRepository.uploadPdfsToStorage(vehicleOtherDocPdfs, brandId, modelName, "vehicleOtherDoc")
                
                val nocUrls = if (nocUrlsResult.isSuccess) nocUrlsResult.getOrThrow() else emptyList()
                val rcUrls = if (rcUrlsResult.isSuccess) rcUrlsResult.getOrThrow() else emptyList()
                val insuranceUrls = if (insuranceUrlsResult.isSuccess) insuranceUrlsResult.getOrThrow() else emptyList()
                val vehicleOtherDocUrls = if (vehicleOtherDocUrlsResult.isSuccess) vehicleOtherDocUrlsResult.getOrThrow() else emptyList()
                
                // 2️⃣ Resolve all document references
                val brandRefsResult = vehicleRepository.resolveBrandReferences(brandId)
                if (brandRefsResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isPurchasingVehicle = false,
                        purchaseVehicleError = brandRefsResult.exceptionOrNull()?.message ?: "Failed to resolve brand references"
                    )
                    return@launch
                }
                val (brandDocRef, brandNameRef) = brandRefsResult.getOrThrow()
                
                // Resolve broker/middle man reference
                val brokerOrMiddleManRef: com.google.firebase.firestore.DocumentReference? = when {
                    !brokerOrMiddleManId.isNullOrBlank() && purchaseType == "Broker" -> {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("Broker")
                            .document(brokerOrMiddleManId)
                    }
                    !brokerOrMiddleManId.isNullOrBlank() && purchaseType == "Middle Man" -> {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("Customer")
                            .document(brokerOrMiddleManId)
                    }
                    else -> null
                }
                
                // Resolve owner reference (always a customer)
                val ownerRef: com.google.firebase.firestore.DocumentReference? = if (!ownerId.isNullOrBlank()) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("Customer")
                        .document(ownerId)
                } else {
                    null
                }
                
                // 3️⃣ Create vehicle map for Purchase
                val vehicleMap = mapOf(
                    "brandId" to brandId,
                    "modelName" to modelName,
                    "colour" to colour,
                    "chassisNumber" to chassisNumber,
                    "condition" to condition,
                    "kms" to kms,
                    "lastService" to lastService,
                    "previousOwners" to previousOwners,
                    "price" to price,
                    "year" to year,
                    "type" to type
                )
                
                // 4️⃣ Create payment methods map (includes broker fee if applicable)
                val paymentMethods = mapOf(
                    "cash" to totalCash.toString(),
                    "bank" to totalBank.toString(),
                    "credit" to totalCredit.toString()
                )
                
                // 5️⃣ Create product object
                val product = Product(
                    brandId = brandId,
                    productId = modelName,
                    colour = colour,
                    chassisNumber = chassisNumber,
                    condition = condition,
                    images = imageUrls, // Already uploaded
                    kms = kms.toIntOrNull() ?: 0,
                    lastService = lastService,
                    previousOwners = previousOwners.toIntOrNull() ?: 0,
                    price = price.toIntOrNull() ?: 0,
                    sellingPrice = sellingPrice.toIntOrNull() ?: 0,
                    year = year.toIntOrNull() ?: 0,
                    type = type,
                    noc = nocUrls, // Already uploaded
                    rc = rcUrls, // Already uploaded
                    insurance = insuranceUrls, // Already uploaded
                    vehicleOtherDoc = vehicleOtherDocUrls, // Already uploaded
                    brokerOrMiddleMan = brokerOrMiddleMan,
                    owner = owner
                )
                
                // 6️⃣ Calculate total grand total (vehicle + broker fee)
                val totalGrandTotal = grandTotal + brokerFee
                
                // 7️⃣ Call atomic transaction method - everything happens in one transaction
                val purchaseResult = purchaseRepository.addPurchaseWithVehicleAtomic(
                    grandTotal = totalGrandTotal,
                    gstAmount = gstAmount,
                    paymentMethods = paymentMethods,
                    vehicle = vehicleMap,
                    middleMan = brokerOrMiddleMan,
                    brandId = brandId,
                    product = product,
                    imageUrls = imageUrls,
                    nocUrls = nocUrls,
                    rcUrls = rcUrls,
                    insuranceUrls = insuranceUrls,
                    vehicleOtherDocUrls = vehicleOtherDocUrls,
                    brandDocRef = brandDocRef,
                    brandNameRef = brandNameRef,
                    brokerOrMiddleManRef = brokerOrMiddleManRef,
                    ownerRef = ownerRef,
                    note = note,
                    date = date
                )
                
                if (purchaseResult.isSuccess) {
                    // ✅ No need to reload - Firebase listener will automatically update StateFlow
                    _uiState.value = _uiState.value.copy(
                        isPurchasingVehicle = false,
                        purchaseVehicleSuccess = true,
                        purchaseVehicleError = null
                    )
                    kotlinx.coroutines.delay(1000)
                    _uiState.value = _uiState.value.copy(purchaseVehicleSuccess = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPurchasingVehicle = false,
                        purchaseVehicleError = purchaseResult.exceptionOrNull()?.message ?: "Failed to create purchase and vehicle"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPurchasingVehicle = false,
                    purchaseVehicleError = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
}

