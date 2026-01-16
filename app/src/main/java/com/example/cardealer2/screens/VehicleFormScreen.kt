package com.example.cardealer2.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.HomeScreenViewModel
import com.example.cardealer2.ViewModel.VehicleFormViewModel
import com.example.cardealer2.components.PdfPickerField
import com.example.cardealer2.data.Brand
import com.example.cardealer2.utility.*
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.DeleteActionButton
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext

sealed interface VehicleFormMode {
    data class Add(val defaultBrandId: String?) : VehicleFormMode
    data class Edit(val chassisNumber: String, val brandId: String) : VehicleFormMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    mode: VehicleFormMode,
    navController: NavController,
    homeScreenViewModel: HomeScreenViewModel,
    viewModel: VehicleFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val models by viewModel.modelsList.collectAsState()
    val colourList by viewModel.colourList.collectAsState()
    val chassisNumber by viewModel.chassisNumber.collectAsState()
    val validationState by viewModel.chassisValidationState.collectAsState()
    val product by viewModel.product.collectAsState()

    val scrollState = rememberScrollState()
    val brands by homeScreenViewModel.brands.collectAsState()
    val brandNames = remember(brands) { brands.map { it.brandId } }

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
    var brokerOrMiddleMan by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf("") }
    var isDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var lastAction by rememberSaveable { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Initial snapshot to detect dirty state
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
    var initialBrokerOrMiddleMan by rememberSaveable { mutableStateOf("") }
    var initialOwner by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(mode) {
        when (mode) {
            is VehicleFormMode.Add -> {
                brandName = mode.defaultBrandId.orEmpty()
                viewModel.loadModels(brandName)
                viewModel.loadColours()

                // Set initial snapshot for Add mode
                initialBrandName = brandName
                initialModelName = ""
                initialColour = ""
                initialCondition = ""
                initialImages = emptyList()
                initialKms = ""
                initialLastService = ""
                initialPreviousOwners = ""
                initialPrice = ""
                initialSellingPrice = ""
                initialYear = ""
                initialType = ""
                initialNocPdfs = emptyList()
                initialRcPdfs = emptyList()
                initialInsurancePdfs = emptyList()
                initialBrokerOrMiddleMan = ""
                initialOwner = ""
            }
            is VehicleFormMode.Edit -> {
                brandName = mode.brandId
                viewModel.loadProduct(mode.chassisNumber)
                viewModel.loadModels(brandName)
                viewModel.loadColours()
            }
        }
    }

    LaunchedEffect(product) {
        product?.let { p ->
            modelName = p.productId
            colour = p.colour
            viewModel.updateChassisNumber(p.chassisNumber)
            condition = p.condition
            images = p.images.map { it.toUri() }
            kms = p.kms.toString()
            lastService = p.lastService
            previousOwners = p.previousOwners.toString()
            price = p.price.toString()
            sellingPrice = p.sellingPrice.toString()
            year = p.year.toString()
            type = p.type
            nocPdfs = p.noc
            rcPdfs = p.rc
            insurancePdfs = p.insurance
            brokerOrMiddleMan = p.brokerOrMiddleMan
            owner = p.owner

            // Capture initial snapshot after load for Edit mode
            initialBrandName = brandName
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
            initialBrokerOrMiddleMan = p.brokerOrMiddleMan
            initialOwner = p.owner
        }
    }

    // Compute dirty state
    val isDirty by remember(
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
        brokerOrMiddleMan,
        owner,
        initialBrandName,
        initialModelName,
        initialColour,
        initialCondition,
        initialImages,
        initialKms,
        initialLastService,
        initialPreviousOwners,
        initialPrice,
        initialYear,
        initialType,
        initialNocPdfs,
        initialRcPdfs,
        initialInsurancePdfs,
        initialBrokerOrMiddleMan,
        initialOwner
    ) {
        mutableStateOf(
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
                brokerOrMiddleMan != initialBrokerOrMiddleMan ||
                owner != initialOwner
        )
    }

    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = if (mode is VehicleFormMode.Add) 
                    TranslationManager.translate("Add Vehicle", isPunjabiEnabled) 
                else 
                    TranslationManager.translate("Edit Vehicle Details", isPunjabiEnabled),
                navController = navController,
                onBackClick = {
                    if (isDirty && !uiState.isLoading) showUnsavedDialog = true else navController.smartPopBack()
                },
                actions = {
                    if (mode is VehicleFormMode.Edit) {
                        DeleteActionButton(
                            onClick = { if (!uiState.isLoading) showDeleteDialog = true },
                            enabled = !uiState.isLoading
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilterableDropdownField(
                    label = TranslationManager.translate("Brand Name", isPunjabiEnabled),
                    items = brandNames,
                    selectedItem = brandName,
                    onItemSelected = { brand ->
                        brandName = brand
                        viewModel.loadModels(brandName)
                    },
                    onAddNewItemAsync = { newBrandName ->
                        val newBrand = Brand(brandId = newBrandName, modelNames = emptyList(), vehicle = emptyList())
                        runCatching {
                            viewModel.addNewBrand(newBrand)
                            brandName = newBrand.brandId
                            viewModel.loadModels(brandName)
                        }.isSuccess
                    },
                    itemToString = { it },
                    onExpandedChange = { expanded -> isDropdownExpanded = expanded }
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
                    selectedItem = type,
                    onItemSelected = { 
                        // Convert translated text back to English for internal use
                        type = when (it) {
                            TranslationManager.translate("Bike", isPunjabiEnabled) -> "Bike"
                            TranslationManager.translate("Car", isPunjabiEnabled) -> "Car"
                            else -> it
                        }
                    },
                    itemToString = { it },
                    onExpandedChange = { expanded -> isDropdownExpanded = expanded }
                )

                FilterableDropdownField(
                    label = TranslationManager.translate("Name of Model", isPunjabiEnabled),
                    items = models,
                    selectedItem = modelName,
                    onItemSelected = { modelName = it },
                    onAddNewItemAsync = { newModel ->
                        runCatching {
                            viewModel.addNewModel(brandName, newModel)
                            modelName = newModel
                        }.isSuccess
                    },
                    itemToString = { it },
                    onExpandedChange = { expanded -> isDropdownExpanded = expanded }
                )
                if (uiState.modelNameError != null) {
                    Text(text = uiState.modelNameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                FilterableDropdownField(
                    label = TranslationManager.translate("Colour", isPunjabiEnabled),
                    items = colourList,
                    selectedItem = colour,
                    onItemSelected = { colour = it },
                    onAddNewItemAsync = { newColour ->
                        runCatching {
                            viewModel.addNewColour(newColour)
                            colour = newColour}
                            .isSuccess

                    },
                    itemToString = { it },
                    onExpandedChange = { expanded -> isDropdownExpanded = expanded }
                )
                if (uiState.colourError != null) {
                    Text(text = uiState.colourError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                ChassisNumberField(
                    value = chassisNumber,
                    onValueChange = { viewModel.updateChassisNumber(it) },
                    onCheckClick = { viewModel.checkChassisNumber() },
                    isChecking = validationState is ChassisValidationState.Checking,
                    validationResult = validationState,
                    errorMessage = uiState.chassisNumberError,
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
                    onExpandedChange = { expanded -> isDropdownExpanded = expanded }
                )
                if (uiState.conditionError != null) {
                    Text(text = uiState.conditionError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                ImagePickerField(
                    label = TranslationManager.translate("Vehicle Images", isPunjabiEnabled),
                    images = images,
                    onImagesChanged = { images = it },
                    errorMessage = uiState.imageError,
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.kmsError != null
                    )
                    YearPickerField(
                        label = TranslationManager.translate("Year *", isPunjabiEnabled),
                        selectedYear = year,
                        onYearSelected = { year = it },
                        errorMessage = uiState.yearError,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (uiState.kmsError != null) {
                    Text(text = uiState.kmsError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (uiState.yearError != null) {
                    Text(text = uiState.yearError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { TranslatedText("Purchase Price *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.priceError != null
                    )
                    OutlinedTextField(
                        value = previousOwners,
                        onValueChange = { previousOwners = it },
                        label = { TranslatedText("Previous Owners") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.previousOwnersError != null
                    )
                }
                if (uiState.priceError != null) {
                    Text(text = uiState.priceError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (uiState.previousOwnersError != null) {
                    Text(text = uiState.previousOwnersError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { TranslatedText("Selling Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                DatePickerField(
                    label = TranslationManager.translate("Last Service", isPunjabiEnabled),
                    selectedDate = lastService,
                    onDateSelected = { lastService = it },
                    errorMessage = uiState.lastServiceError,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                TranslatedText(
                    englishText = "Document Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                PdfPickerField(
                    label = "NOC PDFs",
                    pdfUrls = nocPdfs,
                    onPdfChange = { nocPdfs = it },
                    modifier = Modifier.fillMaxWidth()
                )

                PdfPickerField(
                    label = "RC PDFs",
                    pdfUrls = rcPdfs,
                    onPdfChange = { rcPdfs = it },
                    modifier = Modifier.fillMaxWidth()
                )

                PdfPickerField(
                    label = "Insurance PDFs",
                    pdfUrls = insurancePdfs,
                    onPdfChange = { insurancePdfs = it },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Purchase Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = brokerOrMiddleMan,
                    onValueChange = { brokerOrMiddleMan = it },
                    label = { Text("Broker/Middle Man") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (isDirty && !uiState.isLoading) showUnsavedDialog = true else navController.smartPopBack() },
                        modifier = Modifier.weight(1f)
                    ) { TranslatedText("Cancel") }

                    Button(
                        onClick = {
                            when (mode) {
                                is VehicleFormMode.Add -> {
                                    viewModel.addVehicle(
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
                                        sellingPrice = sellingPrice,
                                        year = year,
                                        type = type,
                                        nocPdfs = nocPdfs,
                                        rcPdfs = rcPdfs,
                                        insurancePdfs = insurancePdfs,
                                        brokerOrMiddleMan = brokerOrMiddleMan,
                                        owner = owner
                                    )
                                }
                                is VehicleFormMode.Edit -> {
                                    lastAction = "update"
                                    viewModel.updateVehicle(
                                        originalChassisNumber = mode.chassisNumber,
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
                                        sellingPrice = sellingPrice,
                                        year = year,
                                        type = type,
                                        nocPdfs = nocPdfs,
                                        rcPdfs = rcPdfs,
                                        insurancePdfs = insurancePdfs,
                                        brokerOrMiddleMan = brokerOrMiddleMan,
                                        owner = owner
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            val buttonText = if (mode is VehicleFormMode.Add) 
                                TranslationManager.translate("Add Vehicle", isPunjabiEnabled) 
                            else 
                                TranslationManager.translate("Update Vehicle", isPunjabiEnabled)
                            Text(buttonText)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && mode is VehicleFormMode.Edit) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showDeleteDialog = false },
            icon = {},
            title = { TranslatedText("Delete Vehicle") },
            text = { TranslatedText("Are you sure you want to delete this vehicle? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { lastAction = "delete"; viewModel.deleteVehicle(mode.chassisNumber) }, enabled = !uiState.isLoading) { 
                    TranslatedText("Delete") 
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { if (!uiState.isLoading) showDeleteDialog = false }) { 
                    TranslatedText("Cancel") 
                }
            }
        )
    }

    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showUnsavedDialog = false },
            icon = {},
            title = { TranslatedText("Discard changes?") },
            text = { TranslatedText("You have unsaved changes. Do you want to discard them and go back?") },
            confirmButton = {
                Button(onClick = { showUnsavedDialog = false; navController.smartPopBack() }, enabled = !uiState.isLoading) { 
                    TranslatedText("Discard") 
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { if (!uiState.isLoading) showUnsavedDialog = false }) { 
                    TranslatedText("Keep Editing") 
                }
            }
        )
    }

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            // Reload brands to get updated vehicle counts before navigating
            homeScreenViewModel.loadBrandsSuspend()
            when (mode) {
                is VehicleFormMode.Add -> {
                    navController.smartPopBack()
                }
                is VehicleFormMode.Edit -> {
                    if (lastAction == "delete") {
                        if (brandName.isNotBlank() && modelName.isNotBlank()) {
                            navController.navigate("brand_Vehicle/$modelName/$brandName") {
                                popUpTo("vehicle_detail/${mode.chassisNumber}") { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.smartPopBack()
                        }
                    } else {
                        // For updates, return to details and signal refresh
                        navController.previousBackStackEntry?.savedStateHandle?.set("vehicle_updated", true)
                        navController.smartPopBack()
                    }
                }
            }
        }
    }

    // Handle system back press with dirty check
    BackHandler(enabled = true) {
        if (isDirty && !uiState.isLoading) {
            showUnsavedDialog = true
        } else {
            navController.smartPopBack()
            navController.smartPopBack()
        }
    }
}


