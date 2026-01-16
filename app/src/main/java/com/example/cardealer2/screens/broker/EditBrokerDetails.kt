package com.example.cardealer2.screens.broker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.components.PdfPickerField
import com.example.cardealer2.ViewModel.ViewBrokersViewModel
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.DeleteActionButton
import com.example.cardealer2.utility.smartPopBack
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBrokerDetails(
    navController: NavController,
    brokerId: String,
    viewModel: ViewBrokersViewModel = viewModel()
) {
    val brokers by viewModel.brokers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val broker = brokers.firstOrNull { it.brokerId == brokerId }
    
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)

    // Form state
    var brokerName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var idProof by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var brokerBill by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Initial snapshot for dirty state detection
    var initialBrokerName by rememberSaveable { mutableStateOf("") }
    var initialPhoneNumber by rememberSaveable { mutableStateOf("") }
    var initialAddress by rememberSaveable { mutableStateOf("") }
    var initialIdProof by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var initialBrokerBill by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    // Load broker data
    LaunchedEffect(Unit) {
        viewModel.loadBrokers()
    }

    // Pre-fill form when broker is loaded
    LaunchedEffect(broker) {
        broker?.let { b ->
            brokerName = b.name
            phoneNumber = b.phoneNumber
            address = b.address
            idProof = b.idProof
            brokerBill = b.brokerBill

            // Set initial snapshot
            initialBrokerName = b.name
            initialPhoneNumber = b.phoneNumber
            initialAddress = b.address
            initialIdProof = b.idProof
            initialBrokerBill = b.brokerBill
        }
    }

    // Compute dirty state
    val isDirty by remember(
        brokerName,
        phoneNumber,
        address,
        idProof,
        brokerBill,
        initialBrokerName,
        initialPhoneNumber,
        initialAddress,
        initialIdProof,
        initialBrokerBill
    ) {
        mutableStateOf(
            brokerName != initialBrokerName ||
                phoneNumber != initialPhoneNumber ||
                address != initialAddress ||
                idProof != initialIdProof ||
                brokerBill != initialBrokerBill
        )
    }

    var updateStarted by remember { mutableStateOf(false) }
    var deleteStarted by remember { mutableStateOf(false) }

    // Navigate back after successful update
    LaunchedEffect(isLoading, error, updateStarted) {
        if (updateStarted && !isLoading && error == null) {
            // Update completed successfully
            // Signal that broker was updated
            navController.currentBackStackEntry?.savedStateHandle?.set(
                "broker_updated_id",
                brokerId
            )
            navController.smartPopBack()
            updateStarted = false
        } else if (updateStarted && !isLoading && error != null) {
            // Update failed, reset flag
            updateStarted = false
        }
    }

    // Navigate to view_broker screen after successful delete
    LaunchedEffect(isLoading, error, deleteStarted) {
        if (deleteStarted && !isLoading && error == null) {
            // Delete completed successfully, navigate to view_broker screen
            // Clear back stack up to view_broker (but keep view_broker)
            navController.navigate("view_broker") {
                popUpTo("view_broker") { inclusive = false }
                launchSingleTop = true
            }
            deleteStarted = false
        } else if (deleteStarted && !isLoading && error != null) {
            // Delete failed, reset flag
            deleteStarted = false
        }
    }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Edit Broker Details", isPunjabiEnabled),
                navController = navController,
                onBackClick = {
                    if (isDirty && !isLoading) {
                        showUnsavedDialog = true
                    } else {
                        navController.smartPopBack()
                    }
                },
                actions = {
                    if (broker != null) {
                        DeleteActionButton(
                            onClick = {
                                if (!isLoading) {
                                    showDeleteDialog = true
                                }
                            },
                            enabled = !isLoading
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (broker == null && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TranslatedText(
                    englishText = "Broker not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Fields
            OutlinedTextField(
                value = brokerName,
                onValueChange = { brokerName = it },
                label = { TranslatedText("Broker Name *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { TranslatedText("Phone Number *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { TranslatedText("Address *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = !isLoading
            )

            HorizontalDivider()

            TranslatedText(
                englishText = "ID Proof",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            PdfPickerField(
                label = TranslationManager.translate("ID Proof PDFs", isPunjabiEnabled),
                pdfUrls = idProof,
                onPdfChange = { idProof = it },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            TranslatedText(
                englishText = "Broker Bill",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            PdfPickerField(
                label = TranslationManager.translate("Broker Bill PDFs", isPunjabiEnabled),
                pdfUrls = brokerBill,
                onPdfChange = { brokerBill = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isDirty && !isLoading) {
                            showUnsavedDialog = true
                        } else {
                            navController.smartPopBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    TranslatedText("Cancel")
                }

                Button(
                    onClick = {
                        updateStarted = true
                        viewModel.updateBroker(
                            brokerId = brokerId,
                            name = brokerName,
                            phoneNumber = phoneNumber,
                            address = address,
                            idProof = idProof,
                            brokerBill = brokerBill
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && brokerName.isNotBlank() && phoneNumber.isNotBlank() && address.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        TranslatedText("Update Broker")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showDeleteDialog = false },
            icon = {},
            title = { TranslatedText("Delete Broker") },
            text = { TranslatedText("Are you sure you want to delete this broker? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteStarted = true
                        viewModel.deleteBroker(brokerId)
                        showDeleteDialog = false
                    },
                    enabled = !isLoading
                ) {
                    TranslatedText("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!isLoading) showDeleteDialog = false },
                    enabled = !isLoading
                ) {
                    TranslatedText("Cancel")
                }
            }
        )
    }

    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showUnsavedDialog = false },
            icon = {},
            title = { TranslatedText("Discard changes?") },
            text = { TranslatedText("You have unsaved changes. Do you want to discard them and go back?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        navController.smartPopBack()
                    },
                    enabled = !isLoading
                ) {
                    TranslatedText("Discard")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!isLoading) showUnsavedDialog = false },
                    enabled = !isLoading
                ) {
                    TranslatedText("Keep Editing")
                }
            }
        )
    }

    // Handle system back press with dirty check
    BackHandler(enabled = true) {
        if (isDirty && !isLoading) {
            showUnsavedDialog = true
        } else {
            navController.smartPopBack()
        }
    }
}

