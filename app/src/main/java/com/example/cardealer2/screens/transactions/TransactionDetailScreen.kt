package com.example.cardealer2.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.cardealer2.ViewModel.LedgerViewModel
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.TransactionType
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import com.example.cardealer2.utils.TransactionBillGenerator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    navController: NavController,
    transactionId: String,
    viewModel: LedgerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var transaction by remember { mutableStateOf<PersonTransaction?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isGeneratingBill by remember { mutableStateOf(false) }
    var showPrintBillDialog by remember { mutableStateOf(false) }
    var billRelatedData by remember { mutableStateOf<TransactionBillGenerator.BillRelatedData?>(null) }
    
    val scope = rememberCoroutineScope()
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateInputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateDisplayFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    // Load transaction details
    LaunchedEffect(transactionId) {
        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                viewModel.getTransactionById(transactionId)
            }
            result.fold(
                onSuccess = { 
                    transaction = it
                    isLoading = false
                },
                onFailure = { exception ->
                    errorMessage = exception.message ?: "Failed to load transaction"
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load transaction"
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Transaction Details", isPunjabiEnabled),
                subtitle = "",
                navController = navController
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = errorMessage ?: "Error loading transaction",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { navController.popBackStack() }) {
                                TranslatedText("Go Back")
                            }
                        }
                    }
                }
                transaction == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TranslatedText("Transaction not found")
                            Button(onClick = { navController.popBackStack() }) {
                                TranslatedText("Go Back")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            TransactionDetailsCard(
                                transaction = transaction!!,
                                currencyFormatter = currencyFormatter,
                                dateInputFormatter = dateInputFormatter,
                                dateDisplayFormatter = dateDisplayFormatter,
                                timeFormat = timeFormat,
                                isPunjabiEnabled = isPunjabiEnabled
                            )
                        }
                        
                        item {
                            DeletionImpactCard(
                                transactionType = transaction!!.type,
                                isPunjabiEnabled = isPunjabiEnabled
                            )
                        }
                        
                        item {
                            GenerateBillButton(
                                transaction = transaction!!,
                                onClick = {
                                    // Fetch related data before showing dialog
                                    scope.launch {
                                        isGeneratingBill = true
                                        try {
                                            val relatedDataResult = withContext(Dispatchers.IO) {
                                                viewModel.fetchBillRelatedData(context, transaction!!)
                                            }
                                            relatedDataResult.fold(
                                                onSuccess = { relatedData ->
                                                    billRelatedData = relatedData
                                                    isGeneratingBill = false
                                                    showPrintBillDialog = true
                                                },
                                                onFailure = { error ->
                                                    errorMessage = TranslationManager.translate(
                                                        "Failed to load related data: ${error.message}",
                                                        isPunjabiEnabled
                                                    )
                                                    isGeneratingBill = false
                                                }
                                            )
                                        } catch (e: Exception) {
                                            errorMessage = TranslationManager.translate(
                                                "Error: ${e.message}",
                                                isPunjabiEnabled
                                            )
                                            isGeneratingBill = false
                                        }
                                    }
                                },
                                isGenerating = isGeneratingBill,
                                isPunjabiEnabled = isPunjabiEnabled
                            )
                        }
                        
                        item {
                            DeleteTransactionButton(
                                onClick = { showDeleteDialog = true },
                                isDeleting = isDeleting,
                                isPunjabiEnabled = isPunjabiEnabled
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && transaction != null) {
        DeleteConfirmationDialog(
            transaction = transaction!!,
            isDeleting = isDeleting,
            onConfirm = {
                isDeleting = true
                viewModel.deleteTransaction(
                    transactionId = transactionId,
                    onSuccess = {
                        navController.popBackStack()
                    },
                    onError = { error ->
                        isDeleting = false
                        errorMessage = error
                        showDeleteDialog = false
                    }
                )
            },
            onDismiss = { 
                if (!isDeleting) {
                    showDeleteDialog = false
                }
            },
            currencyFormatter = currencyFormatter,
            isPunjabiEnabled = isPunjabiEnabled
        )
    }
    
    // Print Bill Dialog
    if (showPrintBillDialog && transaction != null && billRelatedData != null) {
        PrintBillDialog(
            transaction = transaction!!,
            relatedData = billRelatedData!!,
            onDismiss = { showPrintBillDialog = false }
        )
    }
}

@Composable
fun TransactionDetailsCard(
    transaction: PersonTransaction,
    currencyFormatter: NumberFormat,
    dateInputFormatter: SimpleDateFormat,
    dateDisplayFormatter: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    isPunjabiEnabled: Boolean
) {
    // Parse and format date
    val dateText = try {
        val parsedDate = dateInputFormatter.parse(transaction.date) ?: Date()
        dateDisplayFormatter.format(parsedDate)
    } catch (e: Exception) {
        transaction.date
    }
    
    // Format time from createdAt timestamp
    val timeText = try {
        timeFormat.format(Date(transaction.createdAt))
    } catch (e: Exception) {
        ""
    }
    
    // Determine transaction type display
    val transactionTypeLabel = when (transaction.type) {
        TransactionType.PURCHASE -> TranslationManager.translate("Purchase", isPunjabiEnabled)
        TransactionType.SALE -> TranslationManager.translate("Sale", isPunjabiEnabled)
        TransactionType.EMI_PAYMENT -> TranslationManager.translate("EMI Payment", isPunjabiEnabled)
        TransactionType.BROKER_FEE -> TranslationManager.translate("Broker Fee", isPunjabiEnabled)
        else -> transaction.type
    }
    
    // Determine payment method display
    val paymentMethodDisplay = when {
        transaction.paymentMethod == "BANK" -> TranslationManager.translate("Bank", isPunjabiEnabled)
        transaction.paymentMethod == "CASH" -> TranslationManager.translate("Cash", isPunjabiEnabled)
        transaction.paymentMethod == "CREDIT" -> TranslationManager.translate("Credit", isPunjabiEnabled)
        transaction.paymentMethod == "MIXED" -> TranslationManager.translate("Mixed", isPunjabiEnabled)
        else -> transaction.paymentMethod
    }
    
    // Determine amount color based on transaction type
    val amountColor = when (transaction.type) {
        TransactionType.SALE, TransactionType.EMI_PAYMENT -> Color(0xFF34A853) // Green for money received
        TransactionType.PURCHASE, TransactionType.BROKER_FEE -> Color(0xFFEA4335) // Red for money paid
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Transaction Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = amountColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, amountColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = transactionTypeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
            
            HorizontalDivider()
            
            // Transaction Amount (Large)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TranslatedText(
                    englishText = "Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormatter.format(transaction.amount),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
            
            HorizontalDivider()
            
            // Details Section
            DetailRow(
                label = TranslationManager.translate("Transaction ID", isPunjabiEnabled),
                value = transaction.transactionId
            )
            
            transaction.transactionNumber?.let {
                DetailRow(
                    label = TranslationManager.translate("Transaction Number", isPunjabiEnabled),
                    value = it.toString()
                )
            }
            
            DetailRow(
                label = TranslationManager.translate("Person", isPunjabiEnabled),
                value = transaction.personName
            )
            
            DetailRow(
                label = TranslationManager.translate("Person Type", isPunjabiEnabled),
                value = when (transaction.personType) {
                    "CUSTOMER" -> TranslationManager.translate("Customer", isPunjabiEnabled)
                    "BROKER" -> TranslationManager.translate("Broker", isPunjabiEnabled)
                    "MIDDLE_MAN" -> TranslationManager.translate("Middle Man", isPunjabiEnabled)
                    else -> transaction.personType
                }
            )
            
            DetailRow(
                label = TranslationManager.translate("Date", isPunjabiEnabled),
                value = dateText
            )
            
            DetailRow(
                label = TranslationManager.translate("Time", isPunjabiEnabled),
                value = timeText
            )
            
            DetailRow(
                label = TranslationManager.translate("Payment Method", isPunjabiEnabled),
                value = paymentMethodDisplay
            )
            
            // Payment breakdown (if mixed)
            if (transaction.paymentMethod == "MIXED" || (transaction.cashAmount > 0 || transaction.bankAmount > 0 || transaction.creditAmount > 0)) {
                HorizontalDivider()
                TranslatedText(
                    englishText = "Payment Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (transaction.cashAmount > 0) {
                    DetailRow(
                        label = TranslationManager.translate("Cash", isPunjabiEnabled),
                        value = currencyFormatter.format(transaction.cashAmount)
                    )
                }
                
                if (transaction.bankAmount > 0) {
                    DetailRow(
                        label = TranslationManager.translate("Bank", isPunjabiEnabled),
                        value = currencyFormatter.format(transaction.bankAmount)
                    )
                }
                
                if (transaction.creditAmount > 0) {
                    DetailRow(
                        label = TranslationManager.translate("Credit", isPunjabiEnabled),
                        value = currencyFormatter.format(transaction.creditAmount)
                    )
                }
            }
            
            transaction.orderNumber?.let {
                HorizontalDivider()
                DetailRow(
                    label = TranslationManager.translate("Order Number", isPunjabiEnabled),
                    value = it.toString()
                )
            }
            
            if (transaction.description.isNotBlank()) {
                HorizontalDivider()
                DetailRow(
                    label = TranslationManager.translate("Description", isPunjabiEnabled),
                    value = transaction.description
                )
            }
            
            if (transaction.note.isNotBlank()) {
                HorizontalDivider()
                DetailRow(
                    label = TranslationManager.translate("Note", isPunjabiEnabled),
                    value = transaction.note
                )
            }
            
            DetailRow(
                label = TranslationManager.translate("Status", isPunjabiEnabled),
                value = when (transaction.status) {
                    "COMPLETED" -> TranslationManager.translate("Completed", isPunjabiEnabled)
                    "PENDING" -> TranslationManager.translate("Pending", isPunjabiEnabled)
                    "CANCELLED" -> TranslationManager.translate("Cancelled", isPunjabiEnabled)
                    else -> transaction.status
                }
            )
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DeletionImpactCard(
    transactionType: String,
    isPunjabiEnabled: Boolean
) {
    val impactText = when (transactionType) {
        TransactionType.PURCHASE -> TranslationManager.translate(
            "Deleting this purchase transaction will:\n" +
            "• Reverse person balance\n" +
            "• Reverse capital balances\n" +
            "• Delete Product and Chassis documents\n" +
            "• Update Brand inventory\n" +
            "• Delete Purchase document",
            isPunjabiEnabled
        )
        TransactionType.SALE -> TranslationManager.translate(
            "Deleting this sale transaction will:\n" +
            "• Reverse person balance\n" +
            "• Reverse capital balances\n" +
            "• Set product.sold = false\n" +
            "• Update Brand inventory",
            isPunjabiEnabled
        )
        TransactionType.EMI_PAYMENT -> TranslationManager.translate(
            "Deleting this EMI payment will:\n" +
            "• Reverse person balance\n" +
            "• Reverse capital balances\n" +
            "• Revert EMI details (paid/remaining installments)\n" +
            "• Revert VehicleSale status if completed",
            isPunjabiEnabled
        )
        TransactionType.BROKER_FEE -> TranslationManager.translate(
            "Deleting this broker fee transaction will:\n" +
            "• Reverse broker balance\n" +
            "• Reverse capital balances",
            isPunjabiEnabled
        )
        else -> TranslationManager.translate(
            "Deleting this transaction will reverse all related changes.",
            isPunjabiEnabled
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                TranslatedText(
                    englishText = "Deletion Impact",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = impactText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GenerateBillButton(
    transaction: PersonTransaction,
    onClick: () -> Unit,
    isGenerating: Boolean,
    isPunjabiEnabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isGenerating,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            TranslatedText(
                englishText = "Generating Bill...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TranslatedText(
                englishText = "Generate Bill",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DeleteTransactionButton(
    onClick: () -> Unit,
    isDeleting: Boolean,
    isPunjabiEnabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isDeleting,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onError,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            TranslatedText(
                englishText = "Deleting...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TranslatedText(
                englishText = "Delete Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    transaction: PersonTransaction,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    currencyFormatter: NumberFormat,
    isPunjabiEnabled: Boolean
) {
    val transactionTypeLabel = when (transaction.type) {
        TransactionType.PURCHASE -> TranslationManager.translate("Purchase", isPunjabiEnabled)
        TransactionType.SALE -> TranslationManager.translate("Sale", isPunjabiEnabled)
        TransactionType.EMI_PAYMENT -> TranslationManager.translate("EMI Payment", isPunjabiEnabled)
        TransactionType.BROKER_FEE -> TranslationManager.translate("Broker Fee", isPunjabiEnabled)
        else -> transaction.type
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            TranslatedText(
                englishText = "Delete Transaction?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = TranslationManager.translate(
                        "Are you sure you want to delete this transaction?",
                        isPunjabiEnabled
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailRow(
                            label = TranslationManager.translate("Type", isPunjabiEnabled),
                            value = transactionTypeLabel
                        )
                        DetailRow(
                            label = TranslationManager.translate("Person", isPunjabiEnabled),
                            value = transaction.personName
                        )
                        DetailRow(
                            label = TranslationManager.translate("Amount", isPunjabiEnabled),
                            value = currencyFormatter.format(transaction.amount)
                        )
                    }
                }
                
                Text(
                    text = TranslationManager.translate(
                        "This action cannot be undone.",
                        isPunjabiEnabled
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    TranslatedText(
                        englishText = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                TranslatedText("Cancel")
            }
        }
    )
}

