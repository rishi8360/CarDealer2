package com.example.cardealer2.screens.customer

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.CustomerFormViewModel
import com.example.cardealer2.components.PdfPickerField
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.DeleteActionButton
import com.example.cardealer2.utility.AmountInputWithStatus
import com.example.cardealer2.utility.FilterableDropdownField
import com.example.cardealer2.utility.ImagePickerField
import com.example.cardealer2.utility.smartPopBack
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext

sealed interface CustomerFormMode {
    data object Add : CustomerFormMode
    data class Edit(val customerId: String) : CustomerFormMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    mode: CustomerFormMode,
    navController: NavController,
    viewModel: CustomerFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadedCustomer by viewModel.customer.collectAsState()

    val scrollState = rememberScrollState()

    var customerName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var photoUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var idProofType by rememberSaveable { mutableStateOf("") }
    var idProofNumber by rememberSaveable { mutableStateOf("") }
    var idProofPdfs by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var paymentStatus by rememberSaveable { mutableStateOf("To Receive") }
    var amount by rememberSaveable { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        when (mode) {
            is CustomerFormMode.Edit -> viewModel.loadCustomer(mode.customerId)
            is CustomerFormMode.Add -> Unit
        }
    }

    LaunchedEffect(loadedCustomer) {
        loadedCustomer?.let { c ->
            customerName = c.name
            phoneNumber = c.phone
            address = c.address
            photoUris = c.photoUrl.map { Uri.parse(it) }
            idProofType = c.idProofType
            idProofNumber = c.idProofNumber
            idProofPdfs = c.idProofImageUrls
            paymentStatus = when {
                c.amount < 0 -> "To Give"
                c.amount > 0 -> "To Receive"
                else -> "To Receive"
            }
            // Display absolute value for editing (user enters positive, we convert based on type)
            amount = kotlin.math.abs(c.amount).toString()
        }
    }

    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = if (mode is CustomerFormMode.Add) 
                    TranslationManager.translate("Add Customer", isPunjabiEnabled) 
                else 
                    TranslationManager.translate("Edit Customer Details", isPunjabiEnabled),
                navController = navController,
                actions = {
                    if (mode is CustomerFormMode.Edit) {
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
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { TranslatedText("Customer Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.nameError != null
                )
                if (uiState.nameError != null) {
                    Text(text = uiState.nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { TranslatedText("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.phoneError != null
                )
                if (uiState.phoneError != null) {
                    Text(text = uiState.phoneError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { TranslatedText("Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                ImagePickerField(
                    label = TranslationManager.translate("Person Photo", isPunjabiEnabled),
                    images = photoUris,
                    onImagesChanged = { photoUris = it },
                    errorMessage = uiState.photoError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.photoError != null) {
                    Text(text = uiState.photoError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TranslatedText(
                    englishText = "Identity Proof",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val idProofTypes = remember(isPunjabiEnabled) {
                    listOf(
                        TranslationManager.translate("Aadhaar Card", isPunjabiEnabled),
                        TranslationManager.translate("Passport", isPunjabiEnabled),
                        TranslationManager.translate("PAN Card", isPunjabiEnabled),
                        TranslationManager.translate("Driving License", isPunjabiEnabled),
                        TranslationManager.translate("Voter ID", isPunjabiEnabled)
                    )
                }
                
                val currentIdProofTypeDisplay = remember(idProofType, isPunjabiEnabled) {
                    if (isPunjabiEnabled && idProofType.isNotEmpty()) {
                        when(idProofType) {
                            "Aadhaar Card" -> TranslationManager.translate("Aadhaar Card", true)
                            "Passport" -> TranslationManager.translate("Passport", true)
                            "PAN Card" -> TranslationManager.translate("PAN Card", true)
                            "Driving License" -> TranslationManager.translate("Driving License", true)
                            "Voter ID" -> TranslationManager.translate("Voter ID", true)
                            else -> idProofType
                        }
                    } else idProofType
                }
                
                FilterableDropdownField(
                    label = TranslationManager.translate("ID Proof Type", isPunjabiEnabled),
                    items = idProofTypes,
                    selectedItem = currentIdProofTypeDisplay,
                    onItemSelected = { translatedType ->
                        val englishType = when(translatedType) {
                            TranslationManager.translate("Aadhaar Card", true) -> "Aadhaar Card"
                            TranslationManager.translate("Passport", true) -> "Passport"
                            TranslationManager.translate("PAN Card", true) -> "PAN Card"
                            TranslationManager.translate("Driving License", true) -> "Driving License"
                            TranslationManager.translate("Voter ID", true) -> "Voter ID"
                            else -> translatedType
                        }
                        idProofType = englishType
                    },
                    onAddNewItem = { newType -> idProofType = newType },
                    itemToString = { it },
                    onExpandedChange = { }
                )

                OutlinedTextField(
                    value = idProofNumber,
                    onValueChange = { idProofNumber = it },
                    label = { TranslatedText("ID Proof Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { TranslatedText("Enter ID number") }
                )

                PdfPickerField(
                    label = TranslationManager.translate("ID Proof PDFs", isPunjabiEnabled),
                    pdfUrls = idProofPdfs,
                    onPdfChange = { idProofPdfs = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TranslatedText(
                    englishText = "Payment Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                AmountInputWithStatus(
                    amount = amount,
                    onAmountChange = { amount = it },
                    paymentStatus = paymentStatus,
                    onPaymentStatusChange = { paymentStatus = it },
                    error = uiState.amountError
                )
                if (uiState.amountError != null) {
                    Text(text = uiState.amountError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { navController.smartPopBack() }, modifier = Modifier.weight(1f)) { 
                        TranslatedText("Cancel") 
                    }

                    Button(
                        onClick = {
                            val amountValue = amount.toIntOrNull() ?: 0
                            val normalizedAmount = when (paymentStatus) {
                                "To Give" -> -kotlin.math.abs(amountValue)
                                "To Receive" -> kotlin.math.abs(amountValue)
                                else -> amountValue
                            }
                            when (mode) {
                                is CustomerFormMode.Add -> viewModel.addCustomer(
                                    name = customerName,
                                    phone = phoneNumber,
                                    address = address,
                                    photoUris = photoUris,
                                    idProofType = idProofType,
                                    idProofNumber = idProofNumber,
                                    idProofImageUrls = idProofPdfs,
                                    amount = normalizedAmount
                                )
                                is CustomerFormMode.Edit -> viewModel.updateCustomer(
                                    customerId = mode.customerId,
                                    name = customerName,
                                    phone = phoneNumber,
                                    address = address,
                                    photoUrls = photoUris.map { it.toString() },
                                    idProofType = idProofType,
                                    idProofNumber = idProofNumber,
                                    idProofImageUrls = idProofPdfs,
                                    amount = normalizedAmount
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            TranslatedText(
                                if (mode is CustomerFormMode.Add) "Add Customer" else "Update Customer"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && mode is CustomerFormMode.Edit) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showDeleteDialog = false },
            icon = {},
            title = { TranslatedText("Delete Customer") },
            text = { TranslatedText("Are you sure you want to delete this customer? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteCustomer(mode.customerId) }, enabled = !uiState.isLoading) { 
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

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            navController.smartPopBack()
        }
    }
}


