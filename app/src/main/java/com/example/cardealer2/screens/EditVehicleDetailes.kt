package com.example.cardealer2.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import com.example.cardealer2.repository.PasswordRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.HomeScreenViewModel
import com.example.cardealer2.ViewModel.PurchaseVehicleViewModel
import com.example.cardealer2.ViewModel.VehicleDetailViewModel
import com.example.cardealer2.ViewModel.VehicleFormViewModel
import com.example.cardealer2.components.PdfPickerField
import com.example.cardealer2.utility.AmountInputWithStatus
import com.example.cardealer2.utility.ChassisNumberField
import com.example.cardealer2.utility.ChassisValidationState
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.DatePickerField
import com.example.cardealer2.utility.DeleteActionButton
import com.example.cardealer2.utility.FilterableDropdownField
import com.example.cardealer2.utility.FilterableDropdownFieldWithDialog
import com.example.cardealer2.utility.ImagePickerField
import com.example.cardealer2.utility.YearPickerField
import com.example.cardealer2.utility.smartPopBack
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVehicleScreen(
    navController: NavController,
    detailViewModel: VehicleDetailViewModel,
    chassisNumber: String,
    homeScreenViewModel: HomeScreenViewModel = viewModel(),
    purchaseViewModel: PurchaseVehicleViewModel = viewModel(),
    vehicleFormViewModel: VehicleFormViewModel = viewModel()
) {
    val product by detailViewModel.product.collectAsState()
    val uiState by purchaseViewModel.uiState.collectAsState()
    val vehicleUiState by vehicleFormViewModel.uiState.collectAsState()
    
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddBrokerDialog by remember { mutableStateOf(false) }
    var showMiddleManDialog by remember { mutableStateOf(false) }
    
    // Track added item names for success dialogs
    var lastAddedCustomerName by remember { mutableStateOf<String?>(null) }
    var lastAddedBrokerName by remember { mutableStateOf<String?>(null) }
    var lastAddedMiddleManName by remember { mutableStateOf<String?>(null) }
    
    // Track selection type
    var purchaseType by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Track selected customer/middle man/broker and owner
    var selectedCustomerMiddleManBroker by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedOwner by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Load brands when screen appears
    LaunchedEffect(Unit) {
        homeScreenViewModel.loadBrands()
    }

    // Load product when screen appears (since we're not sharing ViewModels anymore)
    LaunchedEffect(chassisNumber) {
        detailViewModel.reloadProductFromRepository(chassisNumber)
    }

    // Vehicle detail fields
    val brands by homeScreenViewModel.brands.collectAsState()
    val brandNames = remember(brands) { brands.map { it.brandId } }
    val models by vehicleFormViewModel.modelsList.collectAsState()
    val colourList by vehicleFormViewModel.colourList.collectAsState()
    val chassisNumberState by vehicleFormViewModel.chassisNumber.collectAsState()
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
    var sellingPrice by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var nocPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var rcPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var insurancePdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var vehicleOtherDocPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    
    // Unsaved changes dialog
    var showUnsavedDialog by remember { mutableStateOf(false) }
    
    // Delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var lastAction by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Initial snapshot to detect dirty state
    var initialPurchaseType by rememberSaveable { mutableStateOf<String?>(null) }
    var initialSelectedCustomerMiddleManBroker by rememberSaveable { mutableStateOf<String?>(null) }
    var initialSelectedOwner by rememberSaveable { mutableStateOf<String?>(null) }
    var initialBrandName by rememberSaveable { mutableStateOf("") }
    var initialModelName by rememberSaveable { mutableStateOf("") }
    var initialColour by rememberSaveable { mutableStateOf("") }
    var initialCondition by rememberSaveable { mutableStateOf("") }
    var initialImages by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var initialKms by rememberSaveable { mutableStateOf("") }
    var initialLastService by rememberSaveable { mutableStateOf("") }
    var initialPreviousOwners by rememberSaveable { mutableStateOf("") }
    var initialPrice by rememberSaveable { mutableStateOf("") }
    var initialSellingPrice by rememberSaveable { mutableStateOf("") }
    var initialYear by rememberSaveable { mutableStateOf("") }
    var initialType by rememberSaveable { mutableStateOf("") }
    var initialNocPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var initialRcPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var initialInsurancePdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var initialVehicleOtherDocPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var initialChassisNumber by rememberSaveable { mutableStateOf("") }
    
    // Load product data into form fields
    LaunchedEffect(product) {
        product?.let { p ->
            brandName = p.brandId
            modelName = p.productId
            colour = p.colour
            vehicleFormViewModel.updateChassisNumber(p.chassisNumber)
            condition = p.condition
            images = p.images.map { it.toUri() }
            kms = p.kms.toString()
            lastService = p.lastService
            previousOwners = p.previousOwners.toString()
            price = p.price.toString()
            sellingPrice = p.sellingPrice.toString()
            year = p.year.toString()
            type = p.type.lowercase() // Normalize to lowercase
            nocPdfs = p.noc
            rcPdfs = p.rc
            insurancePdfs = p.insurance
            vehicleOtherDocPdfs = p.vehicleOtherDoc
            
            // Set initial snapshot after loading
            initialBrandName = p.brandId
            initialModelName = p.productId
            initialColour = p.colour
            initialCondition = p.condition
            initialImages = p.images.map { it.toUri() }
            initialKms = p.kms.toString()
            initialLastService = p.lastService
            initialPreviousOwners = p.previousOwners.toString()
            initialPrice = p.price.toString()
            initialSellingPrice = p.sellingPrice.toString()
            initialYear = p.year.toString()
            initialType = p.type
            initialNocPdfs = p.noc
            initialRcPdfs = p.rc
            initialInsurancePdfs = p.insurance
            initialVehicleOtherDocPdfs = p.vehicleOtherDoc
            initialChassisNumber = p.chassisNumber
        }
    }
    
    // Determine purchase type from brokerOrMiddleMan and owner (after lists are loaded)
    LaunchedEffect(product, uiState.customerNames, uiState.brokerNames) {
        product?.let { p ->
            // Determine purchase type from brokerOrMiddleMan and owner
            when {
                p.brokerOrMiddleMan.isBlank() -> {
                    purchaseType = "Direct"
                    selectedCustomerMiddleManBroker = p.owner
                    selectedOwner = null
                }
                uiState.brokerNames.contains(p.brokerOrMiddleMan) -> {
                    purchaseType = "Broker"
                    selectedCustomerMiddleManBroker = p.brokerOrMiddleMan
                    selectedOwner = p.owner
                }
                uiState.customerNames.contains(p.brokerOrMiddleMan) -> {
                    purchaseType = "Middle Man"
                    selectedCustomerMiddleManBroker = p.brokerOrMiddleMan
                    selectedOwner = p.owner
                }
                else -> {
                    // If not found in either list, default to Direct if owner is set
                    purchaseType = if (p.owner.isNotBlank()) "Direct" else null
                    selectedCustomerMiddleManBroker = p.owner.takeIf { it.isNotBlank() }
                    selectedOwner = null
                }
            }
            
            // Set initial snapshot for purchase type fields
            initialPurchaseType = purchaseType
            initialSelectedCustomerMiddleManBroker = selectedCustomerMiddleManBroker
            initialSelectedOwner = selectedOwner
        }
    }
    
    // Load initial data
    LaunchedEffect(Unit) {
        vehicleFormViewModel.loadColours()
    }
    
    LaunchedEffect(brandName) {
        if (brandName.isNotBlank()) {
            vehicleFormViewModel.loadModels(brandName)
        }
    }
    
    // Navigate back on success
    LaunchedEffect(vehicleUiState.shouldNavigateBack) {
        if (vehicleUiState.shouldNavigateBack) {
            if (lastAction == "delete") {
                // Navigate to brand/model screen after deletion
                homeScreenViewModel.loadBrandsSuspend()
                if (brandName.isNotBlank() && modelName.isNotBlank()) {
                    navController.navigate("brand_Vehicle/$modelName/$brandName") {
                        popUpTo("vehicle_detail/$chassisNumber") { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.smartPopBack()
                }
            } else {
                // Reload the product in detailViewModel to show updated values
                // Use the updated chassis number (which might have changed)
                val finalChassisNumber = chassisNumberState.ifBlank { chassisNumber }
                detailViewModel.reloadProductFromRepository(finalChassisNumber)
                navController.previousBackStackEntry?.savedStateHandle?.set("vehicle_updated", true)
                navController.smartPopBack()
            }
        }
    }

    // Compute dirty state
    val isDirty by remember(
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
        sellingPrice,
        year,
        type,
        nocPdfs,
        rcPdfs,
        insurancePdfs,
        chassisNumberState,
        initialPurchaseType,
        initialSelectedCustomerMiddleManBroker,
        initialSelectedOwner,
        initialBrandName,
        initialModelName,
        initialColour,
        initialCondition,
        initialImages,
        initialKms,
        initialLastService,
        initialPreviousOwners,
        initialPrice,
        initialSellingPrice,
        initialYear,
        initialType,
        initialNocPdfs,
        initialRcPdfs,
        initialInsurancePdfs,
        initialChassisNumber
    ) {
        mutableStateOf(
            purchaseType != initialPurchaseType ||
                selectedCustomerMiddleManBroker != initialSelectedCustomerMiddleManBroker ||
                selectedOwner != initialSelectedOwner ||
                brandName != initialBrandName ||
                modelName != initialModelName ||
                colour != initialColour ||
                condition != initialCondition ||
                images.map { it.toString() } != initialImages.map { it.toString() } ||
                kms != initialKms ||
                lastService != initialLastService ||
                previousOwners != initialPreviousOwners ||
                price != initialPrice ||
                sellingPrice != initialSellingPrice ||
                year != initialYear ||
                type != initialType ||
                nocPdfs != initialNocPdfs ||
                rcPdfs != initialRcPdfs ||
                insurancePdfs != initialInsurancePdfs ||
                vehicleOtherDocPdfs != initialVehicleOtherDocPdfs ||
                chassisNumberState != initialChassisNumber
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var backButtonClicked by remember { mutableStateOf(false) }
    
    // Password protection state
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isPriceVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val passwordRepository = remember { PasswordRepository }
    val storedPassword by passwordRepository.password.collectAsState()
    
    // Load password when component loads
    LaunchedEffect(Unit) {
        passwordRepository.fetchPassword(context)
    }
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Edit Vehicle", isPunjabiEnabled),
                navController = navController,
                onBackClick = {
                    if (!backButtonClicked) {
                        backButtonClicked = true
                        if (isDirty && !vehicleUiState.isLoading) {
                            showUnsavedDialog = true
                        } else {
                            navController.smartPopBack()
                        }
                    }
                },
                actions = {
                    DeleteActionButton(
                        onClick = { 
                            if (!vehicleUiState.isLoading) {
                                showDeleteDialog = true
                            }
                        },
                        enabled = !vehicleUiState.isLoading
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Purchase Type Selection
            TranslatedText(
                englishText = "Purchase Type",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            val purchaseTypeItems = remember(isPunjabiEnabled) {
                listOf(
                    TranslationManager.translate("Direct", isPunjabiEnabled),
                    TranslationManager.translate("Middle Man", isPunjabiEnabled),
                    TranslationManager.translate("Broker", isPunjabiEnabled)
                )
            }
            
            FilterableDropdownField(
                label = TranslationManager.translate("Select Purchase Type", isPunjabiEnabled),
                items = purchaseTypeItems,
                selectedItem = purchaseType,
                onItemSelected = { 
                    // Convert translated text back to English for internal use
                    purchaseType = when (it) {
                        TranslationManager.translate("Direct", isPunjabiEnabled) -> "Direct"
                        TranslationManager.translate("Middle Man", isPunjabiEnabled) -> "Middle Man"
                        TranslationManager.translate("Broker", isPunjabiEnabled) -> "Broker"
                        else -> it
                    }
                },
                itemToString = { it },
                modifier = Modifier.fillMaxWidth()
            )
            
            val currentPurchaseType = remember(purchaseType, isPunjabiEnabled) {
                when (purchaseType) {
                    "Direct" -> TranslationManager.translate("Direct", isPunjabiEnabled)
                    "Middle Man" -> TranslationManager.translate("Middle Man", isPunjabiEnabled)
                    "Broker" -> TranslationManager.translate("Broker", isPunjabiEnabled)
                    else -> purchaseType
                }
            }
            
            when (purchaseType) {
                "Direct" -> {
                    // Direct customer selection
                    FilterableDropdownFieldWithDialog(
                        label = TranslationManager.translate("Select Owner", isPunjabiEnabled),
                        items = uiState.customerNames,
                        selectedItem = selectedCustomerMiddleManBroker,
                        onItemSelected = { selectedCustomerMiddleManBroker = it },
                        onShowAddDialog = { newName ->
                            showAddCustomerDialog = true
                        },
                        itemToString = { it },
                        modifier = Modifier.fillMaxWidth(),
                        addSuccess = uiState.addCustomerSuccess,
                        addedItemName = lastAddedCustomerName,
                        onSuccessHandled = {
                            purchaseViewModel.clearAddCustomerError()
                        }
                    )
                }
                "Middle Man" -> {
                    // Middle Man selection - show dropdown
                    FilterableDropdownFieldWithDialog(
                        label = TranslationManager.translate("Select Middle Man", isPunjabiEnabled),
                        items = uiState.customerNames,
                        selectedItem = selectedCustomerMiddleManBroker,
                        onItemSelected = { selectedCustomerMiddleManBroker = it },
                        onShowAddDialog = { newName ->
                            showMiddleManDialog = true
                        },
                        itemToString = { it },
                        modifier = Modifier.fillMaxWidth(),
                        addSuccess = uiState.addCustomerSuccess,
                        addedItemName = lastAddedMiddleManName,
                        onSuccessHandled = {
                            purchaseViewModel.clearAddCustomerError()
                        }
                    )

                    // Owner Name field
                    FilterableDropdownFieldWithDialog(
                        label = TranslationManager.translate("Select Owner (Customer)", isPunjabiEnabled),
                        items = uiState.customerNames,
                        selectedItem = selectedOwner,
                        onItemSelected = { selectedOwner = it },
                        onShowAddDialog = { newName ->
                            showAddCustomerDialog = true
                        },
                        itemToString = { it },
                        modifier = Modifier.fillMaxWidth(),
                        addSuccess = uiState.addCustomerSuccess,
                        addedItemName = lastAddedCustomerName,
                        onSuccessHandled = {
                            purchaseViewModel.clearAddCustomerError()
                        }
                    )
                }
                "Broker" -> {
                    // Broker selection
                    FilterableDropdownFieldWithDialog(
                        label = TranslationManager.translate("Select Broker", isPunjabiEnabled),
                        items = uiState.brokerNames,
                        selectedItem = selectedCustomerMiddleManBroker,
                        onItemSelected = { selectedCustomerMiddleManBroker = it },
                        onShowAddDialog = { newName ->
                            showAddBrokerDialog = true
                        },
                        itemToString = { it },
                        modifier = Modifier.fillMaxWidth(),
                        addSuccess = uiState.addBrokerSuccess,
                        addedItemName = lastAddedBrokerName,
                        onSuccessHandled = {
                            purchaseViewModel.clearAddBrokerError()
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Owner Name field
                    FilterableDropdownFieldWithDialog(
                        label = TranslationManager.translate("Select Owner (Customer)", isPunjabiEnabled),
                        items = uiState.customerNames,
                        selectedItem = selectedOwner,
                        onItemSelected = { selectedOwner = it },
                        onShowAddDialog = { newName ->
                            showAddCustomerDialog = true
                        },
                        itemToString = { it },
                        modifier = Modifier.fillMaxWidth(),
                        addSuccess = uiState.addCustomerSuccess,
                        addedItemName = lastAddedCustomerName,
                        onSuccessHandled = {
                            purchaseViewModel.clearAddCustomerError()
                        }
                    )
                }
            }
            
            HorizontalDivider()
            
            // Vehicle Details Section
            TranslatedText(
                englishText = "Vehicle Details",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            FilterableDropdownField(
                label = TranslationManager.translate("Brand Name", isPunjabiEnabled),
                items = brandNames,
                selectedItem = brandName,
                onItemSelected = { brandName = it },
                itemToString = { it },
                modifier = Modifier.fillMaxWidth()
            )
            
            val vehicleTypeItems = remember(isPunjabiEnabled) {
                listOf(
                    TranslationManager.translate("Bike", isPunjabiEnabled),
                    TranslationManager.translate("Car", isPunjabiEnabled)
                )
            }
            
            FilterableDropdownField(
                label = TranslationManager.translate("Type of Vehicle", isPunjabiEnabled),
                items = vehicleTypeItems,
                selectedItem = when (type.lowercase()) {
                    "car" -> TranslationManager.translate("Car", isPunjabiEnabled)
                    "bike" -> TranslationManager.translate("Bike", isPunjabiEnabled)
                    else -> type
                },
                onItemSelected = { 
                    // Convert translated text back to English and normalize to lowercase
                    type = when (it) {
                        TranslationManager.translate("Bike", isPunjabiEnabled) -> "bike"
                        TranslationManager.translate("Car", isPunjabiEnabled) -> "car"
                        else -> it.lowercase()
                    }
                },
                itemToString = { 
                    // Display translated version for UI
                    when (type.lowercase()) {
                        "car" -> TranslationManager.translate("Car", isPunjabiEnabled)
                        "bike" -> TranslationManager.translate("Bike", isPunjabiEnabled)
                        else -> type
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            FilterableDropdownField(
                label = TranslationManager.translate("Name of Model", isPunjabiEnabled),
                items = models,
                selectedItem = modelName,
                onItemSelected = { modelName = it },
                itemToString = { it },
                modifier = Modifier.fillMaxWidth()
            )
            
            FilterableDropdownField(
                label = TranslationManager.translate("Colour", isPunjabiEnabled),
                items = colourList,
                selectedItem = colour,
                onItemSelected = { colour = it },
                itemToString = { it },
                modifier = Modifier.fillMaxWidth()
            )
            
            ChassisNumberField(
                value = chassisNumberState,
                onValueChange = { vehicleFormViewModel.updateChassisNumber(it) },
                onCheckClick = { vehicleFormViewModel.checkChassisNumber() },
                isChecking = validationState is ChassisValidationState.Checking,
                validationResult = validationState,
                modifier = Modifier.fillMaxWidth()
            )
            
            val conditionItems = remember(isPunjabiEnabled) {
                listOf(
                    TranslationManager.translate("Excellent", isPunjabiEnabled),
                    TranslationManager.translate("Good", isPunjabiEnabled),
                    TranslationManager.translate("Fair", isPunjabiEnabled),
                    TranslationManager.translate("Poor", isPunjabiEnabled)
                )
            }
            
            FilterableDropdownField(
                label = TranslationManager.translate("Condition", isPunjabiEnabled),
                items = conditionItems,
                selectedItem = condition,
                onItemSelected = { 
                    // Convert translated text back to English for internal use
                    condition = when (it) {
                        TranslationManager.translate("Excellent", isPunjabiEnabled) -> "Excellent"
                        TranslationManager.translate("Good", isPunjabiEnabled) -> "Good"
                        TranslationManager.translate("Fair", isPunjabiEnabled) -> "Fair"
                        TranslationManager.translate("Poor", isPunjabiEnabled) -> "Poor"
                        else -> it
                    }
                },
                itemToString = { it },
                modifier = Modifier.fillMaxWidth()
            )
            
            ImagePickerField(
                label = TranslationManager.translate("Vehicle Images", isPunjabiEnabled),
                images = images,
                onImagesChanged = { images = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = kms,
                    onValueChange = { kms = it },
                    label = { TranslatedText("KMs *") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                YearPickerField(
                    label = TranslationManager.translate("Year *", isPunjabiEnabled),
                    selectedYear = year,
                    onYearSelected = { year = it },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (isPriceVisible || storedPassword.isEmpty()) price else "****",
                    onValueChange = { 
                        if (isPriceVisible || storedPassword.isEmpty()) {
                            price = it
                        } else if (storedPassword.isNotEmpty()) {
                            showPasswordDialog = true
                        }
                    },
                    label = { TranslatedText("Purchase Price *") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    readOnly = !isPriceVisible && storedPassword.isNotEmpty(),
                    trailingIcon = {
                        if (storedPassword.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    if (isPriceVisible) {
                                        isPriceVisible = false
                                    } else {
                                        showPasswordDialog = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPriceVisible) 
                                        Icons.Outlined.Visibility 
                                    else 
                                        Icons.Outlined.VisibilityOff,
                                    contentDescription = if (isPriceVisible) "Hide price" else "Show price"
                                )
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = previousOwners,
                    onValueChange = { previousOwners = it },
                    label = { TranslatedText("Previous Owners") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            OutlinedTextField(
                value = if (isPriceVisible || storedPassword.isEmpty()) sellingPrice else "****",
                onValueChange = { 
                    if (isPriceVisible || storedPassword.isEmpty()) {
                        sellingPrice = it
                    } else if (storedPassword.isNotEmpty()) {
                        showPasswordDialog = true
                    }
                },
                label = { TranslatedText("Selling Price") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                readOnly = !isPriceVisible && storedPassword.isNotEmpty(),
                trailingIcon = {
                    if (storedPassword.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (isPriceVisible) {
                                    isPriceVisible = false
                                } else {
                                    showPasswordDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPriceVisible) 
                                    Icons.Outlined.Visibility 
                                else 
                                    Icons.Outlined.VisibilityOff,
                                contentDescription = if (isPriceVisible) "Hide price" else "Show price"
                            )
                        }
                    }
                }
            )
            
            DatePickerField(
                label = TranslationManager.translate("Last Service", isPunjabiEnabled),
                selectedDate = lastService,
                onDateSelected = { lastService = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            HorizontalDivider()
            
            TranslatedText(
                englishText = "Document Details",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            PdfPickerField(
                label = TranslationManager.translate("NOC PDFs", isPunjabiEnabled),
                pdfUrls = nocPdfs,
                onPdfChange = { nocPdfs = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            PdfPickerField(
                label = TranslationManager.translate("RC PDFs", isPunjabiEnabled),
                pdfUrls = rcPdfs,
                onPdfChange = { rcPdfs = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            PdfPickerField(
                label = TranslationManager.translate("Insurance PDFs", isPunjabiEnabled),
                pdfUrls = insurancePdfs,
                onPdfChange = { insurancePdfs = it },
                modifier = Modifier.fillMaxWidth()
            )

            PdfPickerField(
                label = TranslationManager.translate("Other Vehicle Documents", isPunjabiEnabled),
                pdfUrls = vehicleOtherDocPdfs,
                onPdfChange = { vehicleOtherDocPdfs = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isDirty && !vehicleUiState.isLoading) {
                            showUnsavedDialog = true
                        } else {
                            navController.smartPopBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !vehicleUiState.isLoading
                ) {
                    TranslatedText("Cancel")
                }

                Button(
                    onClick = {
                        // Determine brokerOrMiddleMan and owner based on purchase type
                        val brokerOrMiddleMan = when (purchaseType) {
                            "Direct" -> ""
                            "Middle Man", "Broker" -> selectedCustomerMiddleManBroker ?: ""
                            else -> ""
                        }
                        
                        val owner = when (purchaseType) {
                            "Direct" -> selectedCustomerMiddleManBroker ?: ""
                            "Middle Man", "Broker" -> selectedOwner ?: ""
                            else -> ""
                        }
                        
                        vehicleFormViewModel.updateVehicle(
                            originalChassisNumber = initialChassisNumber.ifBlank { chassisNumber },
                            brandId = brandName,
                            modelName = modelName,
                            colour = colour,
                            chassisNumber = chassisNumberState,
                            condition = condition,
                            images = images,
                            kms = kms,
                            lastService = lastService,
                            previousOwners = previousOwners,
                            price = price,
                            sellingPrice = sellingPrice,
                            year = year,
                            type = type,
                            nocPdfs = nocPdfs,
                            rcPdfs = rcPdfs,
                            insurancePdfs = insurancePdfs,
                            vehicleOtherDocPdfs = vehicleOtherDocPdfs,
                            brokerOrMiddleMan = brokerOrMiddleMan,
                            owner = owner
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !vehicleUiState.isLoading
                ) {
                    if (vehicleUiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        TranslatedText("Update Vehicle")
                    }
                }
            }

            if (vehicleUiState.errorMessage != null) {
                Text(
                    text = vehicleUiState.errorMessage!!,
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

            if (vehicleUiState.isLoading || uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog2(
            viewModel = purchaseViewModel,
            onDismiss = { showAddCustomerDialog = false },
            onAddSuccess = { name ->
                lastAddedCustomerName = name
            }
        )
    }
    
    // Add Broker Dialog
    if (showAddBrokerDialog) {
        AddBrokerDialog2(
            viewModel = purchaseViewModel,
            onDismiss = { showAddBrokerDialog = false },
            onAddSuccess = { name ->
                lastAddedBrokerName = name
            }
        )
    }
    
    // Middle Man Dialog
    if (showMiddleManDialog) {
        AddMiddleManDialog2(
            viewModel = purchaseViewModel,
            onDismiss = { showMiddleManDialog = false },
            onAddSuccess = { name ->
                lastAddedMiddleManName = name
            }
        )
    }
    
    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!vehicleUiState.isLoading) showDeleteDialog = false },
            icon = {},
            title = { TranslatedText("Delete Vehicle") },
            text = { TranslatedText("Are you sure you want to delete this vehicle? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        lastAction = "delete"
                        vehicleFormViewModel.deleteVehicle(initialChassisNumber.ifBlank { chassisNumber })
                        showDeleteDialog = false
                    },
                    enabled = !vehicleUiState.isLoading
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!vehicleUiState.isLoading) showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { if (!vehicleUiState.isLoading) showUnsavedDialog = false },
            icon = {},
            title = { TranslatedText("Discard changes?") },
            text = { TranslatedText("You have unsaved changes. Do you want to discard them and go back?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        navController.smartPopBack()
                    },
                    enabled = !vehicleUiState.isLoading
                ) {
                    TranslatedText("Discard")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!vehicleUiState.isLoading) showUnsavedDialog = false }
                ) {
                    TranslatedText("Keep Editing")
                }
            }
        )
    }
    
    // Password Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordInput = ""
                passwordError = null
            },
            title = {
                TranslatedText(
                    englishText = "Enter Password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { TranslatedText("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError != null,
                        singleLine = true
                    )
                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val isValid = passwordRepository.verifyPassword(context, passwordInput)
                            if (isValid) {
                                isPriceVisible = true
                                showPasswordDialog = false
                                passwordInput = ""
                                passwordError = null
                            } else {
                                passwordError = "Incorrect password"
                            }
                        }
                    }
                ) {
                    TranslatedText("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        passwordInput = ""
                        passwordError = null
                    }
                ) {
                    TranslatedText("Cancel")
                }
            }
        )
    }
    
    // Handle system back press with dirty check
    BackHandler(enabled = true) {
        if (isDirty && !vehicleUiState.isLoading) {
            showUnsavedDialog = true
        } else {
            navController.smartPopBack()
        }
    }
}

// Reuse dialogs from PurchaseVehicleScreen
@Composable
fun AddCustomerDialog2(
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
fun AddBrokerDialog2(
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
                                brokerBill = brokerBill
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
fun AddMiddleManDialog2(
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
