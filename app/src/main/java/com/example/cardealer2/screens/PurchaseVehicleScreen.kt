package com.example.cardealer2.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.HomeScreenViewModel
import com.example.cardealer2.ViewModel.PurchaseVehicleViewModel
import com.example.cardealer2.ViewModel.VehicleFormViewModel
import com.example.cardealer2.components.PdfPickerField
import com.example.cardealer2.data.Brand
import com.example.cardealer2.utility.AmountInputWithStatus
import com.example.cardealer2.utility.ChassisNumberField
import com.example.cardealer2.utility.ChassisValidationState
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.DatePickerField
import com.example.cardealer2.utility.FilterableDropdownField
import com.example.cardealer2.utility.FilterableDropdownFieldWithDialog
import com.example.cardealer2.utility.ImagePickerField
import com.example.cardealer2.utility.YearPickerField
import com.example.cardealer2.utility.smartPopBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseVehicleScreen(
    navController: NavController,
    homeScreenViewModel: HomeScreenViewModel,
    viewModel: PurchaseVehicleViewModel = viewModel(),
    vehicleFormViewModel: VehicleFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by rememberSaveable { mutableStateOf(1) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddBrokerDialog by remember { mutableStateOf(false) }
    var showMiddleManDialog by remember { mutableStateOf(false) }

    // Track added item names for success dialogs
    var lastAddedCustomerName by remember { mutableStateOf<String?>(null) }
    var lastAddedBrokerName by remember { mutableStateOf<String?>(null) }
    var lastAddedMiddleManName by remember { mutableStateOf<String?>(null) }

    // Step 1: Purchase Type and Owner/Middle Man/Broker
    var purchaseType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCustomerMiddleManBroker by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedOwner by rememberSaveable { mutableStateOf<String?>(null) }

    // Step 2: Vehicle Details
    val brands by homeScreenViewModel.brands.collectAsState()
    val brandNames = remember(brands) { brands.map { it.brandId } }
    val models by vehicleFormViewModel.modelsList.collectAsState()
    val colourList by vehicleFormViewModel.colourList.collectAsState()
    val chassisNumber by vehicleFormViewModel.chassisNumber.collectAsState()
    val validationState by vehicleFormViewModel.chassisValidationState.collectAsState()

    var brandName by rememberSaveable { mutableStateOf("") }
    var modelName by rememberSaveable { mutableStateOf("") }
    var colour by rememberSaveable { mutableStateOf("") }
    var condition by rememberSaveable { mutableStateOf("") }
    var images by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var kms by rememberSaveable { mutableStateOf("") }
    var lastService by rememberSaveable { mutableStateOf("") }
    var previousOwners by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var nocPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var rcPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var insurancePdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    // Step 3: Payment Details
    var gstIncluded by rememberSaveable { mutableStateOf(false) }
    var gstPercentage by rememberSaveable { mutableStateOf("18") }
    var brokerFeeIncluded by rememberSaveable { mutableStateOf(false) }
    var brokerFeeAmount by rememberSaveable { mutableStateOf("") }
    var brokerFeeCash by rememberSaveable { mutableStateOf("") }
    var brokerFeeBank by rememberSaveable { mutableStateOf("") }
    var brokerFeeCredit by rememberSaveable { mutableStateOf("") }
    var cashAmount by rememberSaveable { mutableStateOf("") }
    var bankAmount by rememberSaveable { mutableStateOf("") }
    var creditAmount by rememberSaveable { mutableStateOf("") }

    // Calculate price with GST (only if GST checkbox is checked)
    val basePrice = price.toDoubleOrNull() ?: 0.0
    val gstRate = (gstPercentage.toDoubleOrNull() ?: 0.0) / 100.0
    val calculatedGstAmount = if (gstIncluded && gstRate > 0) {
        basePrice * gstRate // GST amount = basePrice * GST rate
    } else {
        0.0 // No GST if checkbox is not checked
    }
    // Final price = basePrice + GST (if GST is included)
    val finalPrice = if (gstIncluded && gstRate > 0) {
        basePrice + calculatedGstAmount // Add GST to base price
    } else {
        basePrice // No GST added
    }

    // Calculate broker/middle man fee total (only if brokerFeeIncluded is true)
    val brokerFee = if (brokerFeeIncluded) (brokerFeeAmount.toDoubleOrNull() ?: 0.0) else 0.0
    val brokerFeeCashAmount =
        if (brokerFeeIncluded) (brokerFeeCash.toDoubleOrNull() ?: 0.0) else 0.0
    val brokerFeeBankAmount =
        if (brokerFeeIncluded) (brokerFeeBank.toDoubleOrNull() ?: 0.0) else 0.0
    val brokerFeeCreditAmount = if (brokerFeeIncluded) {
        maxOf(0.0, brokerFee - (brokerFeeCashAmount + brokerFeeBankAmount))
    } else {
        0.0
    }

    // Calculate vehicle payment amounts
    val vehicleCashAmount = cashAmount.toDoubleOrNull() ?: 0.0
    val vehicleBankAmount = bankAmount.toDoubleOrNull() ?: 0.0
    val vehicleCreditAmount = maxOf(0.0, finalPrice - (vehicleCashAmount + vehicleBankAmount))

    // Total amounts
    val totalCash = vehicleCashAmount + brokerFeeCashAmount
    val totalBank = vehicleBankAmount + brokerFeeBankAmount
    val totalCredit = vehicleCreditAmount + brokerFeeCreditAmount
    val totalGrandTotal = finalPrice + brokerFee

    // Validation: Cash + Bank should not exceed Grand Total
    // Vehicle payment validation: cash + bank should not exceed finalPrice (which includes GST)
    val isVehiclePaymentValid = (vehicleCashAmount + vehicleBankAmount) <= finalPrice
    val isBrokerFeePaymentValid = if (brokerFeeIncluded) {
        (brokerFeeCashAmount + brokerFeeBankAmount) <= brokerFee && brokerFee > 0
    } else {
        true // No broker fee, so validation passes
    }
    // Total payment validation: total cash + bank should not exceed total grand total
    val isTotalPaymentValid = totalCash + totalBank <= totalGrandTotal
    val isPaymentValid = isVehiclePaymentValid && isBrokerFeePaymentValid && isTotalPaymentValid

    // Unsaved changes dialog
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Load initial data
    LaunchedEffect(Unit) {
        homeScreenViewModel.loadBrands()
        vehicleFormViewModel.loadColours()
    }

    LaunchedEffect(brandName) {
        if (brandName.isNotBlank()) {
            vehicleFormViewModel.loadModels(brandName)
        }
    }

    // Navigate back on success
    LaunchedEffect(uiState.purchaseVehicleSuccess) {
        if (uiState.purchaseVehicleSuccess) {
            navController.smartPopBack()
        }
    }

    // Compute dirty state
    val isDirty = remember(
        purchaseType,
        selectedCustomerMiddleManBroker,
        selectedOwner,
        brandName,
        modelName,
        colour,
        condition,
        images,
        kms,
        lastService,
        previousOwners,
        price,
        year,
        type,
        nocPdfs,
        rcPdfs,
        insurancePdfs,
        gstIncluded,
        gstPercentage,
        brokerFeeIncluded,
        brokerFeeAmount,
        brokerFeeCash,
        brokerFeeBank,
        cashAmount,
        bankAmount,
        creditAmount,
        chassisNumber
    ) {
        purchaseType != null ||
                selectedCustomerMiddleManBroker != null ||
                selectedOwner != null ||
                brandName.isNotBlank() ||
                modelName.isNotBlank() ||
                colour.isNotBlank() ||
                condition.isNotBlank() ||
                images.isNotEmpty() ||
                kms.isNotBlank() ||
                lastService.isNotBlank() ||
                previousOwners.isNotBlank() ||
                price.isNotBlank() ||
                year.isNotBlank() ||
                type.isNotBlank() ||
                nocPdfs.isNotEmpty() ||
                rcPdfs.isNotEmpty() ||
                insurancePdfs.isNotEmpty() ||
                gstIncluded ||
                gstPercentage.isNotBlank() ||
                brokerFeeIncluded ||
                brokerFeeAmount.isNotBlank() ||
                brokerFeeCash.isNotBlank() ||
                brokerFeeBank.isNotBlank() ||
                cashAmount.isNotBlank() ||
                bankAmount.isNotBlank() ||
                creditAmount.isNotBlank() ||
                chassisNumber.isNotBlank()
    }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = "Purchase Vehicle - Step $currentStep of 3",
                navController = navController,
                onBackClick = {
                    if (isDirty && !uiState.isPurchasingVehicle) {
                        showUnsavedDialog = true
                    } else {
                        navController.smartPopBack()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step Indicator
            StepIndicator(
                currentStep = currentStep,
                totalSteps = 3,
                stepLabels = listOf("Purchase Type", "Vehicle Details", "Payment"),
                onStepClick = { step ->
                    // Allow navigation to any step up to current step (can go back to edit)
                    if (step <= currentStep) {
                        currentStep = step
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(), // 👈 pushes content above keyboard
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> Step1Content(
                        maxOrderNo = uiState.maxOrderNo,
                        purchaseType = purchaseType,
                        onPurchaseTypeSelected = { purchaseType = it },
                        selectedCustomerMiddleManBroker = selectedCustomerMiddleManBroker,
                        onCustomerMiddleManBrokerSelected = {
                            selectedCustomerMiddleManBroker = it
                        },
                        selectedOwner = selectedOwner,
                        onOwnerSelected = { selectedOwner = it },
                        customerNames = uiState.customerNames,
                        brokerNames = uiState.brokerNames,
                        onShowAddCustomerDialog = { showAddCustomerDialog = true },
                        onShowAddBrokerDialog = { showAddBrokerDialog = true },
                        onShowMiddleManDialog = { showMiddleManDialog = true },
                        addCustomerSuccess = uiState.addCustomerSuccess,
                        addedCustomerName = lastAddedCustomerName,
                        onCustomerSuccessHandled = {
                            viewModel.clearAddCustomerError()
                        },
                        addBrokerSuccess = uiState.addBrokerSuccess,
                        addedBrokerName = lastAddedBrokerName,
                        onBrokerSuccessHandled = {
                            viewModel.clearAddBrokerError()
                        },
                        addMiddleManSuccess = uiState.addCustomerSuccess,
                        addedMiddleManName = lastAddedMiddleManName,
                        onMiddleManSuccessHandled = {
                            viewModel.clearAddCustomerError()
                        },
                        onNextClick = {
                            // Validate step 1
                            if (purchaseType == null) {
                                return@Step1Content
                            }
                            if (selectedOwner == null) {
                                return@Step1Content
                            }
                            if ((purchaseType == "Middle Man" || purchaseType == "Broker") && selectedCustomerMiddleManBroker == null) {
                                return@Step1Content
                            }
                            currentStep = 2
                        }
                    )

                    2 -> Step2Content(
                        brands = brandNames,
                        brandName = brandName,
                        onBrandSelected = { brandName = it },
                        onAddNewBrand = { newBrandName ->
                            val newBrand = Brand(
                                brandId = newBrandName,
                                modelNames = emptyList(),
                                vehicle = emptyList()
                            )
                            runCatching {
                                vehicleFormViewModel.addNewBrand(newBrand)
                                brandName = newBrand.brandId
                                vehicleFormViewModel.loadModels(brandName)
                            }.isSuccess
                        },
                        type = type,
                        onTypeSelected = { type = it },
                        models = models,
                        modelName = modelName,
                        onModelSelected = { modelName = it },
                        onAddNewModel = { newModel ->
                            runCatching {
                                vehicleFormViewModel.addNewModel(brandName, newModel)
                                modelName = newModel
                            }.isSuccess
                        },
                        colourList = colourList,
                        colour = colour,
                        onColourSelected = { colour = it },
                        onAddNewColour = { newColour ->
                            runCatching {
                                vehicleFormViewModel.addNewColour(newColour)
                                colour = newColour
                            }.isSuccess
                        },
                        chassisNumber = chassisNumber,
                        onChassisNumberChange = { vehicleFormViewModel.updateChassisNumber(it) },
                        onCheckChassis = { vehicleFormViewModel.checkChassisNumber() },
                        isCheckingChassis = validationState is ChassisValidationState.Checking,
                        validationResult = validationState,
                        condition = condition,
                        onConditionSelected = { condition = it },
                        images = images,
                        onImagesChanged = { images = it },
                        kms = kms,
                        onKmsChange = { kms = it },
                        year = year,
                        onYearSelected = { year = it },
                        price = price,
                        onPriceChange = { price = it },
                        previousOwners = previousOwners,
                        onPreviousOwnersChange = { previousOwners = it },
                        lastService = lastService,
                        onLastServiceSelected = { lastService = it },
                        nocPdfs = nocPdfs,
                        onNocPdfsChange = { nocPdfs = it },
                        rcPdfs = rcPdfs,
                        onRcPdfsChange = { rcPdfs = it },
                        insurancePdfs = insurancePdfs,
                        onInsurancePdfsChange = { insurancePdfs = it },
                        onBackClick = { currentStep = 1 },
                        onNextClick = {
                            // Validate step 2
                            if (brandName.isBlank() || modelName.isBlank() || type.isBlank() ||
                                colour.isBlank() || chassisNumber.isBlank() || condition.isBlank() ||
                                kms.isBlank() || year.isBlank() || price.isBlank()
                            ) {
                                return@Step2Content
                            }
                            currentStep = 3
                        }
                    )

                    3 -> Step3Content(
                        brandName = brandName,
                        modelName = modelName,
                        chassisNumber = chassisNumber,
                        basePrice = basePrice,
                        gstIncluded = gstIncluded,
                        onGstIncludedChange = { gstIncluded = it },
                        gstPercentage = gstPercentage,
                        onGstPercentageChange = { gstPercentage = it },
                        gstAmount = calculatedGstAmount,
                        finalPrice = finalPrice,
                        purchaseType = purchaseType,
                        brokerFeeIncluded = brokerFeeIncluded,
                        onBrokerFeeIncludedChange = { brokerFeeIncluded = it },
                        brokerFeeAmount = brokerFeeAmount,
                        onBrokerFeeAmountChange = { brokerFeeAmount = it },
                        brokerFee = brokerFee,
                        brokerFeeCash = brokerFeeCash,
                        onBrokerFeeCashChange = { brokerFeeCash = it },
                        brokerFeeBank = brokerFeeBank,
                        onBrokerFeeBankChange = { brokerFeeBank = it },
                        brokerFeeCredit = brokerFeeCreditAmount,
                        cashAmount = cashAmount,
                        onCashAmountChange = {
                            cashAmount = it
                            // Auto-update credit
                            val cash = it.toDoubleOrNull() ?: 0.0
                            val bank = bankAmount.toDoubleOrNull() ?: 0.0
                            val credit = finalPrice - (cash + bank)
                            creditAmount = if (credit >= 0) String.format("%.2f", credit) else "0"
                        },
                        bankAmount = bankAmount,
                        onBankAmountChange = {
                            bankAmount = it
                            // Auto-update credit
                            val cash = cashAmount.toDoubleOrNull() ?: 0.0
                            val bank = it.toDoubleOrNull() ?: 0.0
                            val credit = finalPrice - (cash + bank)
                            creditAmount = if (credit >= 0) String.format("%.2f", credit) else "0"
                        },
                        creditAmount = creditAmount,
                        isPaymentValid = isPaymentValid,
                        totalGrandTotal = totalGrandTotal,
                        totalCash = totalCash,
                        totalBank = totalBank,
                        totalCredit = totalCredit,
                        isPurchasing = uiState.isPurchasingVehicle,
                        onBackClick = { currentStep = 2 },
                        onAddClick = {
                            if (!isPaymentValid) return@Step3Content
                            viewModel.purchaseVehicle(
                                brandId = brandName,
                                modelName = modelName,
                                colour = colour,
                                chassisNumber = chassisNumber,
                                condition = condition,
                                images = images,
                                kms = kms,
                                lastService = lastService,
                                previousOwners = previousOwners,
                                price = price,
                                year = year,
                                type = type,
                                purchaseType = purchaseType,
                                selectedCustomerMiddleManBroker = selectedCustomerMiddleManBroker,
                                selectedOwner = selectedOwner,
                                nocPdfs = nocPdfs,
                                rcPdfs = rcPdfs,
                                insurancePdfs = insurancePdfs,
                                gstIncluded = gstIncluded,
                                gstPercentage = gstPercentage,
                                brokerFeeIncluded = brokerFeeIncluded,
                                brokerFeeAmount = brokerFeeAmount,
                                brokerFeeCash = brokerFeeCash,
                                brokerFeeBank = brokerFeeBank,
                                cashAmount = cashAmount,
                                bankAmount = bankAmount,
                                creditAmount = creditAmount
                            )
                        }
                    )
                }

                if (uiState.purchaseVehicleError != null) {
                    Text(
                        text = uiState.purchaseVehicleError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Add Customer Dialog
        if (showAddCustomerDialog) {
            AddCustomerDialog(
                viewModel = viewModel,
                onDismiss = { showAddCustomerDialog = false },
                onAddSuccess = { name ->
                    lastAddedCustomerName = name
                }
            )
        }

        // Add Broker Dialog
        if (showAddBrokerDialog) {
            AddBrokerDialog(
                viewModel = viewModel,
                onDismiss = { showAddBrokerDialog = false },
                onAddSuccess = { name ->
                    lastAddedBrokerName = name
                }
            )
        }

        // Middle Man Dialog
        if (showMiddleManDialog) {
            AddMiddleManDialog(
                viewModel = viewModel,
                onDismiss = { showMiddleManDialog = false },
                onAddSuccess = { name ->
                    lastAddedMiddleManName = name
                }
            )
        }

        // Unsaved changes dialog
        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { if (!uiState.isPurchasingVehicle) showUnsavedDialog = false },
                icon = {},
                title = { Text("Discard changes?") },
                text = { Text("You have unsaved changes. Do you want to discard them and go back?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showUnsavedDialog = false
                            navController.smartPopBack()
                        },
                        enabled = !uiState.isPurchasingVehicle
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { if (!uiState.isPurchasingVehicle) showUnsavedDialog = false }
                    ) {
                        Text("Keep Editing")
                    }
                }
            )
        }

        // Handle system back press with dirty check
        BackHandler(enabled = true) {
            if (currentStep > 1) {
                currentStep--
            } else {
                if (isDirty && !uiState.isPurchasingVehicle) {
                    showUnsavedDialog = true
                } else {
                    navController.smartPopBack()
                }
            }
        }
    }
}
@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepLabels: List<String>,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { step ->
                val stepNumber = step + 1
                val isActive = stepNumber == currentStep
                val isCompleted = stepNumber < currentStep
                val isClickable = stepNumber <= currentStep

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step Circle/Bubble
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .then(
                                if (isClickable) {
                                    Modifier.clickable { onStepClick(stepNumber) }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isActive -> MaterialTheme.colorScheme.primary
                                    isCompleted -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isActive) 4.dp else 2.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    // Show checkmark for completed steps
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    // Show step number
                                    Text(
                                        text = stepNumber.toString(),
                                        color = when {
                                            isActive -> MaterialTheme.colorScheme.onPrimary
                                            isCompleted -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Step Label
                    Text(
                        text = stepLabels.getOrNull(step) ?: "Step $stepNumber",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                // Connecting Line (except after last step)
                if (step < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (stepNumber + 1 <= currentStep) {
                                MaterialTheme.colorScheme.primary // Line filled if next step is reached
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant // Line unfilled for future steps
                            },
                            thickness = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Step1Content(
    maxOrderNo: Int,
    purchaseType: String?,
    onPurchaseTypeSelected: (String) -> Unit,
    selectedCustomerMiddleManBroker: String?,
    onCustomerMiddleManBrokerSelected: (String?) -> Unit,
    selectedOwner: String?,
    onOwnerSelected: (String?) -> Unit,
    customerNames: List<String>,
    brokerNames: List<String>,
    onShowAddCustomerDialog: () -> Unit,
    onShowAddBrokerDialog: () -> Unit,
    onShowMiddleManDialog: () -> Unit,
    addCustomerSuccess: Boolean,
    addedCustomerName: String?,
    onCustomerSuccessHandled: () -> Unit,
    addBrokerSuccess: Boolean,
    addedBrokerName: String?,
    onBrokerSuccessHandled: () -> Unit,
    addMiddleManSuccess: Boolean,
    addedMiddleManName: String?,
    onMiddleManSuccessHandled: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order Number Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "OrdNo ${maxOrderNo + 1}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Text(
            text = "Purchase Type",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        FilterableDropdownField(
            label = "Select Purchase Type",
            items = listOf("Direct", "Middle Man", "Broker"),
            selectedItem = purchaseType,
            onItemSelected = { onPurchaseTypeSelected(it) },
            itemToString = { it },
            modifier = Modifier.fillMaxWidth()
        )

        when (purchaseType) {
            "Direct" -> {
                // Direct mode - no additional field needed
            }
            "Middle Man" -> {
                FilterableDropdownFieldWithDialog(
                    label = "Select Middle Man",
                    items = customerNames,
                    selectedItem = selectedCustomerMiddleManBroker,
                    onItemSelected = { onCustomerMiddleManBrokerSelected(it) },
                    onShowAddDialog = { onShowMiddleManDialog() },
                    itemToString = { it },
                    modifier = Modifier.fillMaxWidth(),
                    addSuccess = addMiddleManSuccess,
                    addedItemName = addedMiddleManName,
                    onSuccessHandled = onMiddleManSuccessHandled
                )
            }
            "Broker" -> {
                FilterableDropdownFieldWithDialog(
                    label = "Select Broker",
                    items = brokerNames,
                    selectedItem = selectedCustomerMiddleManBroker,
                    onItemSelected = { onCustomerMiddleManBrokerSelected(it) },
                    onShowAddDialog = { onShowAddBrokerDialog() },
                    itemToString = { it },
                    modifier = Modifier.fillMaxWidth(),
                    addSuccess = addBrokerSuccess,
                    addedItemName = addedBrokerName,
                    onSuccessHandled = onBrokerSuccessHandled
                )
            }
        }

        // Owner Name field - always shown
        FilterableDropdownFieldWithDialog(
            label = "Select Owner (Customer)",
            items = customerNames,
            selectedItem = selectedOwner,
            onItemSelected = { onOwnerSelected(it) },
            onShowAddDialog = { onShowAddCustomerDialog() },
            itemToString = { it },
            modifier = Modifier.fillMaxWidth(),
            addSuccess = addCustomerSuccess,
            addedItemName = addedCustomerName,
            onSuccessHandled = onCustomerSuccessHandled
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNextClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}

@Composable
fun Step2Content(
    brands: List<String>,
    brandName: String,
    onBrandSelected: (String) -> Unit,
    onAddNewBrand: (String) -> Boolean,
    type: String,
    onTypeSelected: (String) -> Unit,
    models: List<String>,
    modelName: String,
    onModelSelected: (String) -> Unit,
    onAddNewModel: (String) -> Boolean,
    colourList: List<String>,
    colour: String,
    onColourSelected: (String) -> Unit,
    onAddNewColour: (String) -> Boolean,
    chassisNumber: String,
    onChassisNumberChange: (String) -> Unit,
    onCheckChassis: () -> Unit,
    isCheckingChassis: Boolean,
    validationResult: ChassisValidationState,
    condition: String,
    onConditionSelected: (String) -> Unit,
    images: List<Uri>,
    onImagesChanged: (List<Uri>) -> Unit,
    kms: String,
    onKmsChange: (String) -> Unit,
    year: String,
    onYearSelected: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    previousOwners: String,
    onPreviousOwnersChange: (String) -> Unit,
    lastService: String,
    onLastServiceSelected: (String) -> Unit,
    nocPdfs: List<String>,
    onNocPdfsChange: (List<String>) -> Unit,
    rcPdfs: List<String>,
    onRcPdfsChange: (List<String>) -> Unit,
    insurancePdfs: List<String>,
    onInsurancePdfsChange: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Vehicle Details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        FilterableDropdownField(
            label = "Brand Name",
            items = brands,
            selectedItem = brandName,
            onItemSelected = { onBrandSelected(it) },
            onAddNewItemAsync = onAddNewBrand,
            itemToString = { it },
            modifier = Modifier.fillMaxWidth()
        )

        FilterableDropdownField(
            label = "Type of Vehicle",
            items = listOf("Bike", "Car"),
            selectedItem = type,
            onItemSelected = { onTypeSelected(it) },
            itemToString = { it },
            modifier = Modifier.fillMaxWidth()
        )

        FilterableDropdownField(
            label = "Name of Model",
            items = models,
            selectedItem = modelName,
            onItemSelected = { onModelSelected(it) },
            onAddNewItemAsync = onAddNewModel,
            itemToString = { it },
            modifier = Modifier.fillMaxWidth()
        )

        FilterableDropdownField(
            label = "Colour",
            items = colourList,
            selectedItem = colour,
            onItemSelected = { onColourSelected(it) },
            onAddNewItemAsync = onAddNewColour,
            itemToString = { it },
            modifier = Modifier.fillMaxWidth()
        )

        ChassisNumberField(
            value = chassisNumber,
            onValueChange = onChassisNumberChange,
            onCheckClick = onCheckChassis,
            isChecking = isCheckingChassis,
            validationResult = validationResult,
            modifier = Modifier.fillMaxWidth()
        )

        FilterableDropdownField(
            label = "Condition",
            items = listOf("Excellent", "Good", "Fair", "Poor"),
            selectedItem = condition,
            onItemSelected = { onConditionSelected(it) },
            itemToString = { it },
            modifier = Modifier.fillMaxWidth()
        )

        ImagePickerField(
            label = "Vehicle Images",
            images = images,
            onImagesChanged = onImagesChanged,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = kms,
                onValueChange = onKmsChange,
                label = { Text("KMs *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            YearPickerField(
                label = "Year *",
                selectedYear = year,
                onYearSelected = onYearSelected,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text("Price *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = previousOwners,
                onValueChange = onPreviousOwnersChange,
                label = { Text("Previous Owners") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        DatePickerField(
            label = "Last Service",
            selectedDate = lastService,
            onDateSelected = onLastServiceSelected,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Text(
            text = "Document Details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        PdfPickerField(
            label = "NOC PDFs",
            pdfUrls = nocPdfs,
            onPdfChange = onNocPdfsChange,
            modifier = Modifier.fillMaxWidth()
        )

        PdfPickerField(
            label = "RC PDFs",
            pdfUrls = rcPdfs,
            onPdfChange = onRcPdfsChange,
            modifier = Modifier.fillMaxWidth()
        )

        PdfPickerField(
            label = "Insurance PDFs",
            pdfUrls = insurancePdfs,
            onPdfChange = onInsurancePdfsChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNextClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Step3Content(
    brandName: String,
    modelName: String,
    chassisNumber: String,
    basePrice: Double,
    gstIncluded: Boolean,
    onGstIncludedChange: (Boolean) -> Unit,
    gstPercentage: String,
    onGstPercentageChange: (String) -> Unit,
    gstAmount: Double,
    finalPrice: Double,
    purchaseType: String?,
    brokerFeeIncluded: Boolean,
    onBrokerFeeIncludedChange: (Boolean) -> Unit,
    brokerFeeAmount: String,
    onBrokerFeeAmountChange: (String) -> Unit,
    brokerFee: Double,
    brokerFeeCash: String,
    onBrokerFeeCashChange: (String) -> Unit,
    brokerFeeBank: String,
    onBrokerFeeBankChange: (String) -> Unit,
    brokerFeeCredit: Double,
    cashAmount: String,
    onCashAmountChange: (String) -> Unit,
    bankAmount: String,
    onBankAmountChange: (String) -> Unit,
    creditAmount: String,
    isPaymentValid: Boolean,
    totalGrandTotal: Double,
    totalCash: Double,
    totalBank: Double,
    totalCredit: Double,
    isPurchasing: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Payment Summary",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Vehicle Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Vehicle Details",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Brand: $brandName")
                Text("Model: $modelName")
                Text("Chassis: $chassisNumber")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Base Price: ₹${String.format("%.2f", basePrice)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // GST Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = gstIncluded,
                onCheckedChange = onGstIncludedChange
            )
            Text(
                text = "Apply GST",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (gstIncluded) {
            OutlinedTextField(
                value = gstPercentage,
                onValueChange = onGstPercentageChange,
                label = { Text("GST Percentage (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Text("%", modifier = Modifier.padding(end = 8.dp)) }
            )
        }

        // Price Calculation Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Vehicle Price Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Base Price: ₹${String.format("%.2f", basePrice)}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (gstIncluded && gstAmount > 0) {
                    Text(
                        text = "GST (${gstPercentage}%): ₹${String.format("%.2f", gstAmount)}",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Vehicle Total (Grand Total): ₹${String.format("%.2f", finalPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        HorizontalDivider()

        // Broker/Middle Man Fee Section
        if (purchaseType == "Broker" || purchaseType == "Middle Man") {
            Text(
                text = if (purchaseType == "Broker") "Broker Fee" else "Middle Man Fee",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = brokerFeeIncluded,
                    onCheckedChange = onBrokerFeeIncludedChange
                )
                Text(
                    text = "Include ${if (purchaseType == "Broker") "Broker" else "Middle Man"} Fee",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (brokerFeeIncluded) {
                OutlinedTextField(
                    value = brokerFeeAmount,
                    onValueChange = onBrokerFeeAmountChange,
                    label = { Text("${if (purchaseType == "Broker") "Broker" else "Middle Man"} Fee Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = "${if (purchaseType == "Broker") "Broker" else "Middle Man"} Fee Payment",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = brokerFeeCash,
                        onValueChange = {
                            onBrokerFeeCashChange(it)
                            // Auto-update broker fee credit
                            val cash = it.toDoubleOrNull() ?: 0.0
                            val bank = brokerFeeBank.toDoubleOrNull() ?: 0.0
                            val credit = brokerFee - (cash + bank)
                            // Credit is auto-calculated, no need to update here
                        },
                        label = { Text("Cash") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = brokerFeeBank,
                        onValueChange = {
                            onBrokerFeeBankChange(it)
                            // Auto-update broker fee credit
                            val cash = brokerFeeCash.toDoubleOrNull() ?: 0.0
                            val bank = it.toDoubleOrNull() ?: 0.0
                            val credit = brokerFee - (cash + bank)
                            // Credit is auto-calculated, no need to update here
                        },
                        label = { Text("Bank") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = String.format("%.2f", brokerFeeCredit),
                    onValueChange = { }, // Read-only, auto-calculated
                    label = { Text("Credit (Auto)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Text(
            text = "Vehicle Payment Methods",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = cashAmount,
                onValueChange = onCashAmountChange,
                label = { Text("Cash Amount") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = bankAmount,
                onValueChange = onBankAmountChange,
                label = { Text("Bank Amount") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        OutlinedTextField(
            value = creditAmount,
            onValueChange = { }, // Read-only, auto-calculated
            label = { Text("Credit Amount (Auto)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Payment Validation Error
        if (!isPaymentValid) {
            Text(
                text = "⚠️ Cash + Bank amount cannot exceed Vehicle Grand Total (₹${String.format("%.2f", finalPrice)})",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Total Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Total Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Vehicle Total: ₹${String.format("%.2f", finalPrice)}",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (brokerFeeIncluded && brokerFee > 0) {
                    Text(
                        text = "${if (purchaseType == "Broker") "Broker" else "Middle Man"} Fee: ₹${String.format("%.2f", brokerFee)}",
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Grand Total: ₹${String.format("%.2f", totalGrandTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Total Cash: ₹${String.format("%.2f", totalCash)}",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Total Bank: ₹${String.format("%.2f", totalBank)}",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Total Credit: ₹${String.format("%.2f", totalCredit)}",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
                enabled = !isPurchasing
            ) {
                Text("Back")
            }
            Button(
                onClick = onAddClick,
                modifier = Modifier.weight(1f),
                enabled = !isPurchasing && isPaymentValid
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add Vehicle")
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    viewModel: PurchaseVehicleViewModel,
    onDismiss: () -> Unit,
    onAddSuccess: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog state
    var customerName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var photoUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var idProofType by rememberSaveable { mutableStateOf("") }
    var idProofNumber by rememberSaveable { mutableStateOf("") }
    var idProofPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var paymentStatus by rememberSaveable { mutableStateOf("To Receive") }
    var amount by rememberSaveable { mutableStateOf("") }

    // Close dialog when customer is successfully added
    LaunchedEffect(uiState.addCustomerSuccess) {
        if (uiState.addCustomerSuccess && customerName.isNotBlank()) {
            onAddSuccess(customerName)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Customer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = MaterialTheme.typography.headlineSmall.fontWeight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Close",
                            modifier = Modifier.rotate(45f)
                        )
                    }
                }

                Divider()

                // Form Fields
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAddingCustomer
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !uiState.isAddingCustomer
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !uiState.isAddingCustomer
                )

                ImagePickerField(
                    label = "Person Photo",
                    images = photoUris,
                    onImagesChanged = { photoUris = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Identity Proof",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FilterableDropdownField(
                    label = "ID Proof Type",
                    items = listOf("Aadhaar Card", "Passport", "PAN Card", "Driving License", "Voter ID"),
                    selectedItem = idProofType,
                    onItemSelected = { idProofType = it },
                    onAddNewItem = { newType -> idProofType = newType },
                    itemToString = { it },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = idProofNumber,
                    onValueChange = { idProofNumber = it },
                    label = { Text("ID Proof Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAddingCustomer
                )

                PdfPickerField(
                    label = "ID Proof PDFs",
                    pdfUrls = idProofPdfs,
                    onPdfChange = { idProofPdfs = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Payment Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                AmountInputWithStatus(
                    amount = amount,
                    onAmountChange = { amount = it },
                    paymentStatus = paymentStatus,
                    onPaymentStatusChange = { paymentStatus = it }
                )

                if (uiState.addCustomerError != null) {
                    Text(
                        text = uiState.addCustomerError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAddingCustomer
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amountValue = amount.toIntOrNull() ?: 0
                            val normalizedAmount = when (paymentStatus) {
                                "To Give" -> -kotlin.math.abs(amountValue)
                                "To Receive" -> kotlin.math.abs(amountValue)
                                else -> amountValue
                            }
                            viewModel.addCustomer(
                                name = customerName,
                                phone = phoneNumber,
                                address = address,
                                photoUris = photoUris,
                                idProofType = idProofType,
                                idProofNumber = idProofNumber,
                                idProofImageUrls = idProofPdfs,
                                amount = normalizedAmount
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAddingCustomer
                    ) {
                        if (uiState.isAddingCustomer) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddBrokerDialog(
    viewModel: PurchaseVehicleViewModel,
    onDismiss: () -> Unit,
    onAddSuccess: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog state
    var brokerName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var idProof by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var brokerBill by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var amount by rememberSaveable { mutableStateOf("") }

    // Close dialog when broker is successfully added
    LaunchedEffect(uiState.addBrokerSuccess) {
        if (uiState.addBrokerSuccess && brokerName.isNotBlank()) {
            onAddSuccess(brokerName)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Broker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = MaterialTheme.typography.headlineSmall.fontWeight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Close",
                            modifier = Modifier.rotate(45f)
                        )
                    }
                }

                Divider()

                // Form Fields
                OutlinedTextField(
                    value = brokerName,
                    onValueChange = { brokerName = it },
                    label = { Text("Broker Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAddingBroker
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !uiState.isAddingBroker
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !uiState.isAddingBroker
                )

                HorizontalDivider()

                Text(
                    text = "ID Proof",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                PdfPickerField(
                    label = "ID Proof PDFs",
                    pdfUrls = idProof,
                    onPdfChange = { idProof = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Broker Bill",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                PdfPickerField(
                    label = "Broker Bill PDFs",
                    pdfUrls = brokerBill,
                    onPdfChange = { brokerBill = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Broker Amount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Broker Fee Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !uiState.isAddingBroker
                )

                if (uiState.addBrokerError != null) {
                    Text(
                        text = uiState.addBrokerError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAddingBroker
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.addBroker(
                                name = brokerName,
                                phoneNumber = phoneNumber,
                                address = address,
                                idProof = idProof,
                                brokerBill = brokerBill,
                                amount = amount.toIntOrNull() ?: 0
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAddingBroker
                    ) {
                        if (uiState.isAddingBroker) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMiddleManDialog(
    viewModel: PurchaseVehicleViewModel,
    onDismiss: () -> Unit,
    onAddSuccess: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Middle Man Dialog state
    var customerName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var photoUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var idProofType by rememberSaveable { mutableStateOf("") }
    var idProofNumber by rememberSaveable { mutableStateOf("") }
    var idProofPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var brokerPaymentStatus by rememberSaveable { mutableStateOf("To Receive") }
    var amount by rememberSaveable { mutableStateOf("") }

    // Owner Details state
    var ownerName by rememberSaveable { mutableStateOf("") }
    var ownerPhoneNumber by rememberSaveable { mutableStateOf("") }
    var ownerAddress by rememberSaveable { mutableStateOf("") }
    var ownerPhotoUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var ownerIdProofType by rememberSaveable { mutableStateOf("") }
    var ownerIdProofNumber by rememberSaveable { mutableStateOf("") }
    var ownerIdProofPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    // Close dialog when customer is successfully added
    LaunchedEffect(uiState.addCustomerSuccess) {
        if (uiState.addCustomerSuccess && customerName.isNotBlank()) {
            onAddSuccess(customerName)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Middle Man Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = MaterialTheme.typography.headlineSmall.fontWeight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Close",
                            modifier = Modifier.rotate(45f)
                        )
                    }
                }

                Divider()

                // Form Fields
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAddingCustomer
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !uiState.isAddingCustomer
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !uiState.isAddingCustomer
                )

                ImagePickerField(
                    label = "Person Photo",
                    images = photoUris,
                    onImagesChanged = { photoUris = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Identity Proof",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FilterableDropdownField(
                    label = "ID Proof Type",
                    items = listOf("Aadhaar Card", "Passport", "PAN Card", "Driving License", "Voter ID"),
                    selectedItem = idProofType,
                    onItemSelected = { idProofType = it },
                    onAddNewItem = { newType -> idProofType = newType },
                    itemToString = { it },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = idProofNumber,
                    onValueChange = { idProofNumber = it },
                    label = { Text("ID Proof Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAddingCustomer
                )

                PdfPickerField(
                    label = "ID Proof PDFs",
                    pdfUrls = idProofPdfs,
                    onPdfChange = { idProofPdfs = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Owner Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAddingCustomer
                )

                OutlinedTextField(
                    value = ownerPhoneNumber,
                    onValueChange = { ownerPhoneNumber = it },
                    label = { Text("Owner Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !uiState.isAddingCustomer
                )

                OutlinedTextField(
                    value = ownerAddress,
                    onValueChange = { ownerAddress = it },
                    label = { Text("Owner Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !uiState.isAddingCustomer
                )

                ImagePickerField(
                    label = "Owner Photo",
                    images = ownerPhotoUris,
                    onImagesChanged = { ownerPhotoUris = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Owner Identity Proof",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FilterableDropdownField(
                    label = "Owner ID Proof Type",
                    items = listOf("Aadhaar Card", "Passport", "PAN Card", "Driving License", "Voter ID"),
                    selectedItem = ownerIdProofType,
                    onItemSelected = { ownerIdProofType = it },
                    onAddNewItem = { newType -> ownerIdProofType = newType },
                    itemToString = { it },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ownerIdProofNumber,
                    onValueChange = { ownerIdProofNumber = it },
                    label = { Text("Owner ID Proof Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAddingCustomer
                )

                PdfPickerField(
                    label = "Owner ID Proof PDFs",
                    pdfUrls = ownerIdProofPdfs,
                    onPdfChange = { ownerIdProofPdfs = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Payment Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                AmountInputWithStatus(
                    amount = amount,
                    onAmountChange = { amount = it },
                    paymentStatus = brokerPaymentStatus,
                    onPaymentStatusChange = { brokerPaymentStatus = it }
                )

                if (uiState.addCustomerError != null) {
                    Text(
                        text = uiState.addCustomerError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAddingCustomer
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amountValue = amount.toIntOrNull() ?: 0
                            val normalizedAmount = when (brokerPaymentStatus) {
                                "To Give" -> -kotlin.math.abs(amountValue)
                                "To Receive" -> kotlin.math.abs(amountValue)
                                else -> amountValue
                            }
                            viewModel.addCustomer(
                                name = customerName,
                                phone = phoneNumber,
                                address = address,
                                photoUris = photoUris,
                                idProofType = idProofType,
                                idProofNumber = idProofNumber,
                                idProofImageUrls = idProofPdfs,
                                amount = normalizedAmount
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAddingCustomer
                    ) {
                        if (uiState.isAddingCustomer) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
