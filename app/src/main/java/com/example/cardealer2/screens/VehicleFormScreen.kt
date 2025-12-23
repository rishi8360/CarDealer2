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
                year != initialYear ||
                type != initialType ||
                nocPdfs != initialNocPdfs ||
                rcPdfs != initialRcPdfs ||
                insurancePdfs != initialInsurancePdfs ||
                brokerOrMiddleMan != initialBrokerOrMiddleMan ||
                owner != initialOwner
        )
    }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = if (mode is VehicleFormMode.Add) "Add Vehicle" else "Edit Vehicle Details",
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
                    label = "Brand Name",
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

                FilterableDropdownField(
                    label = "Type of Vehicle",
                    items = listOf("Bike", "Car"),
                    selectedItem = type,
                    onItemSelected = { type = it },
                    itemToString = { it },
                    onExpandedChange = { expanded -> isDropdownExpanded = expanded }
                )

                FilterableDropdownField(
                    label = "Name of Model",
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
                    label = "Colour",
                    items = colourList,
                    selectedItem = colour,
                    onItemSelected = { colour = it },
                    onAddNewItemAsync = { newColour ->
                        runCatching {
                            viewModel.addNewColour(newColour)
                            colour = newColour
                        }.isSuccess
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

                FilterableDropdownField(
                    label = "Condition",
                    items = listOf("Excellent", "Good", "Fair", "Poor"),
                    selectedItem = condition,
                    onItemSelected = { condition = it },
                    itemToString = { it },
                    onExpandedChange = { expanded -> isDropdownExpanded = expanded }
                )
                if (uiState.conditionError != null) {
                    Text(text = uiState.conditionError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                ImagePickerField(
                    label = "Vehicle Images",
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
                        label = { Text("KMs *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.kmsError != null
                    )
                    YearPickerField(
                        label = "Year *",
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
                        label = { Text("Price *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.priceError != null
                    )
                    OutlinedTextField(
                        value = previousOwners,
                        onValueChange = { previousOwners = it },
                        label = { Text("Previous Owners") },
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

                DatePickerField(
                    label = "Last Service",
                    selectedDate = lastService,
                    onDateSelected = { lastService = it },
                    errorMessage = uiState.lastServiceError,
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
                    ) { Text("Cancel") }

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
                            Text(if (mode is VehicleFormMode.Add) "Add Vehicle" else "Update Vehicle")
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
            title = { Text("Delete Vehicle") },
            text = { Text("Are you sure you want to delete this vehicle? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { lastAction = "delete"; viewModel.deleteVehicle(mode.chassisNumber) }, enabled = !uiState.isLoading) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { if (!uiState.isLoading) showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showUnsavedDialog = false },
            icon = {},
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Do you want to discard them and go back?") },
            confirmButton = {
                Button(onClick = { showUnsavedDialog = false; navController.smartPopBack() }, enabled = !uiState.isLoading) { Text("Discard") }
            },
            dismissButton = {
                OutlinedButton(onClick = { if (!uiState.isLoading) showUnsavedDialog = false }) { Text("Keep Editing") }
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


