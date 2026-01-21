package com.example.cardealer2.screens.transactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.cardealer2.ViewModel.LedgerViewModel
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.TransactionType
import com.example.cardealer2.data.Purchase
import com.example.cardealer2.data.VehicleSale
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Broker
import com.example.cardealer2.repository.PurchaseRepository
import com.example.cardealer2.repository.SaleRepository
import com.example.cardealer2.repository.VehicleRepository
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
    var showRelatedDetailsSheet by remember { mutableStateOf(false) }
    var isLoadingRelatedDetails by remember { mutableStateOf(false) }
    var relatedDetailsError by remember { mutableStateOf<String?>(null) }
    
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
                                isPunjabiEnabled = isPunjabiEnabled,
                                onViewRelatedDetails = {
                                    showRelatedDetailsSheet = true
                                    isLoadingRelatedDetails = true
                                    relatedDetailsError = null
                                }
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
    
    // Related Details Bottom Sheet
    if (showRelatedDetailsSheet && transaction != null) {
        RelatedDetailsBottomSheet(
            transaction = transaction!!,
            onDismiss = { showRelatedDetailsSheet = false },
            isLoading = isLoadingRelatedDetails,
            onLoadingChange = { isLoadingRelatedDetails = it },
            error = relatedDetailsError,
            onErrorChange = { relatedDetailsError = it },
            currencyFormatter = currencyFormatter,
            isPunjabiEnabled = isPunjabiEnabled
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
    isPunjabiEnabled: Boolean,
    onViewRelatedDetails: () -> Unit = {}
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

    // Determine amount color and icon based on transaction type
    val (amountColor, transactionIcon) = when (transaction.type) {
        TransactionType.SALE, TransactionType.EMI_PAYMENT ->
            Color(0xFF34A853) to Icons.Default.TrendingUp // Green for money received
        TransactionType.PURCHASE, TransactionType.BROKER_FEE ->
            Color(0xFFEA4335) to Icons.Default.TrendingDown // Red for money paid
        else -> MaterialTheme.colorScheme.primary to Icons.Default.SwapVert
    }

    val statusColor = when (transaction.status) {
        "COMPLETED" -> Color(0xFF34A853)
        "PENDING" -> Color(0xFFFBBC04)
        "CANCELLED" -> Color(0xFFEA4335)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Transaction Type Badge with Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = amountColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, amountColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = transactionIcon,
                            contentDescription = null,
                            tint = amountColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = transactionTypeLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Related Details Button (if relatedRef or personRef exists)
                    if (transaction.relatedRef != null || transaction.personRef != null) {
                        IconButton(
                            onClick = onViewRelatedDetails,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = TranslationManager.translate("View Related Details", isPunjabiEnabled),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = when (transaction.status) {
                                "COMPLETED" -> TranslationManager.translate("Completed", isPunjabiEnabled)
                                "PENDING" -> TranslationManager.translate("Pending", isPunjabiEnabled)
                                "CANCELLED" -> TranslationManager.translate("Cancelled", isPunjabiEnabled)
                                else -> transaction.status
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Transaction Amount (Prominent Display)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = amountColor.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        TranslatedText(
                            englishText = "Transaction Amount",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = currencyFormatter.format(transaction.amount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = amountColor,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // Primary Information Section
            InfoSection(
                title = TranslationManager.translate("Transaction Details", isPunjabiEnabled)
            ) {
                InfoItem(
                    icon = Icons.Default.Tag,
                    label = TranslationManager.translate("Transaction ID", isPunjabiEnabled),
                    value = transaction.transactionId
                )

                transaction.transactionNumber?.let {
                    InfoItem(
                        icon = Icons.Default.Numbers,
                        label = TranslationManager.translate("Transaction Number", isPunjabiEnabled),
                        value = it.toString()
                    )
                }

                transaction.orderNumber?.let {
                    InfoItem(
                        icon = Icons.Default.Receipt,
                        label = TranslationManager.translate("Order Number", isPunjabiEnabled),
                        value = it.toString()
                    )
                }
            }

            // Person Information Section
            InfoSection(
                title = TranslationManager.translate("Person Information", isPunjabiEnabled)
            ) {
                InfoItem(
                    icon = Icons.Default.Person,
                    label = TranslationManager.translate("Person", isPunjabiEnabled),
                    value = transaction.personName,
                    isHighlighted = true
                )

                InfoItem(
                    icon = Icons.Default.Badge,
                    label = TranslationManager.translate("Person Type", isPunjabiEnabled),
                    value = when (transaction.personType) {
                        "CUSTOMER" -> TranslationManager.translate("Customer", isPunjabiEnabled)
                        "BROKER" -> TranslationManager.translate("Broker", isPunjabiEnabled)
                        "MIDDLE_MAN" -> TranslationManager.translate("Middle Man", isPunjabiEnabled)
                        else -> transaction.personType
                    }
                )
            }

            // Date & Time Section
            InfoSection(
                title = TranslationManager.translate("Date & Time", isPunjabiEnabled)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = TranslationManager.translate("Date", isPunjabiEnabled),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = TranslationManager.translate("Time", isPunjabiEnabled),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Payment Section
            InfoSection(
                title = TranslationManager.translate("Payment Information", isPunjabiEnabled)
            ) {
                InfoItem(
                    icon = Icons.Default.Payment,
                    label = TranslationManager.translate("Payment Method", isPunjabiEnabled),
                    value = paymentMethodDisplay
                )

                // Payment breakdown (if mixed or multiple methods)
                if (transaction.paymentMethod == "MIXED" ||
                    (transaction.cashAmount > 0 || transaction.bankAmount > 0 || transaction.creditAmount > 0)) {

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (transaction.cashAmount > 0) {
                            PaymentBreakdownItem(
                                icon = Icons.Default.Payments,
                                method = TranslationManager.translate("Cash", isPunjabiEnabled),
                                amount = currencyFormatter.format(transaction.cashAmount),
                                color = Color(0xFF34A853)
                            )
                        }

                        if (transaction.bankAmount > 0) {
                            PaymentBreakdownItem(
                                icon = Icons.Default.AccountBalance,
                                method = TranslationManager.translate("Bank", isPunjabiEnabled),
                                amount = currencyFormatter.format(transaction.bankAmount),
                                color = Color(0xFF4285F4)
                            )
                        }

                        if (transaction.creditAmount > 0) {
                            PaymentBreakdownItem(
                                icon = Icons.Default.CreditCard,
                                method = TranslationManager.translate("Credit", isPunjabiEnabled),
                                amount = currencyFormatter.format(transaction.creditAmount),
                                color = Color(0xFFFBBC04)
                            )
                        }
                    }
                }
            }

            // Additional Information (if available)
            if (transaction.description.isNotBlank() || transaction.note.isNotBlank()) {
                InfoSection(
                    title = TranslationManager.translate("Additional Information", isPunjabiEnabled)
                ) {
                    if (transaction.description.isNotBlank()) {
                        InfoItem(
                            icon = Icons.Default.Description,
                            label = TranslationManager.translate("Description", isPunjabiEnabled),
                            value = transaction.description,
                            maxLines = 3
                        )
                    }

                    if (transaction.note.isNotBlank()) {
                        InfoItem(
                            icon = Icons.Default.StickyNote2,
                            label = TranslationManager.translate("Note", isPunjabiEnabled),
                            value = transaction.note,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isHighlighted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isHighlighted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PaymentBreakdownItem(
    icon: ImageVector,
    method: String,
    amount: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = method,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun DeletionImpactCard(
    transactionType: String,
    isPunjabiEnabled: Boolean
) {
    val impactItems = when (transactionType) {
        TransactionType.PURCHASE -> listOf(
            TranslationManager.translate("Reverse person balance", isPunjabiEnabled),
            TranslationManager.translate("Reverse capital balances", isPunjabiEnabled),
            TranslationManager.translate("Delete Product and Chassis documents", isPunjabiEnabled),
            TranslationManager.translate("Update Brand inventory", isPunjabiEnabled),
            TranslationManager.translate("Delete Purchase document", isPunjabiEnabled)
        )
        TransactionType.SALE -> listOf(
            TranslationManager.translate("Reverse person balance", isPunjabiEnabled),
            TranslationManager.translate("Reverse capital balances", isPunjabiEnabled),
            TranslationManager.translate("Set product.sold = false", isPunjabiEnabled),
            TranslationManager.translate("Update Brand inventory", isPunjabiEnabled)
        )
        TransactionType.EMI_PAYMENT -> listOf(
            TranslationManager.translate("Reverse person balance", isPunjabiEnabled),
            TranslationManager.translate("Reverse capital balances", isPunjabiEnabled),
            TranslationManager.translate("Revert EMI details (paid/remaining installments)", isPunjabiEnabled),
            TranslationManager.translate("Revert VehicleSale status if completed", isPunjabiEnabled)
        )
        TransactionType.BROKER_FEE -> listOf(
            TranslationManager.translate("Reverse broker balance", isPunjabiEnabled),
            TranslationManager.translate("Reverse capital balances", isPunjabiEnabled)
        )
        else -> listOf(
            TranslationManager.translate("Reverse all related changes", isPunjabiEnabled)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                TranslatedText(
                    englishText = "Deletion Impact",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                impactItems.forEach { impact ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(6.dp)
                            )
                        }
                        Text(
                            text = impact,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        AnimatedContent(
            targetState = isGenerating,
            label = "generate_bill_animation"
        ) { generating ->
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
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
                        modifier = Modifier.size(22.dp)
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
    }
}

@Composable
fun DeleteTransactionButton(
    onClick: () -> Unit,
    isDeleting: Boolean,
    isPunjabiEnabled: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isDeleting,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(14.dp)
    ) {
        AnimatedContent(
            targetState = isDeleting,
            label = "delete_animation"
        ) { deleting ->
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (deleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TranslatedText(
                        englishText = "Deleting...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TranslatedText(
                        englishText = "Delete Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
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
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp).size(28.dp)
                )
            }
        },
        title = {
            TranslatedText(
                englishText = "Delete Transaction?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = TranslationManager.translate(
                        "Are you sure you want to delete this transaction? This action cannot be undone.",
                        isPunjabiEnabled
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DialogDetailRow(
                            label = TranslationManager.translate("Type", isPunjabiEnabled),
                            value = transactionTypeLabel
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        DialogDetailRow(
                            label = TranslationManager.translate("Person", isPunjabiEnabled),
                            value = transaction.personName
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        DialogDetailRow(
                            label = TranslationManager.translate("Amount", isPunjabiEnabled),
                            value = currencyFormatter.format(transaction.amount),
                            isAmount = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TranslatedText(
                    englishText = "Delete",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                shape = RoundedCornerShape(10.dp)
            ) {
                TranslatedText(
                    englishText = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DialogDetailRow(
    label: String,
    value: String,
    isAmount: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = if (isAmount) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isAmount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Related Details Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatedDetailsBottomSheet(
    transaction: PersonTransaction,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    error: String?,
    onErrorChange: (String?) -> Unit,
    currencyFormatter: NumberFormat,
    isPunjabiEnabled: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var purchaseDetails by remember { mutableStateOf<Purchase?>(null) }
    var vehicleSaleDetails by remember { mutableStateOf<VehicleSale?>(null) }
    var customerDetails by remember { mutableStateOf<Customer?>(null) }
    var brokerDetails by remember { mutableStateOf<Broker?>(null) }
    var ownerDetails by remember { mutableStateOf<Customer?>(null) }
    var vehicleCustomerDetails by remember { mutableStateOf<Customer?>(null) }
    
    val dateDisplayFormatter = remember { SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()) }
    
    // Fetch related documents
    LaunchedEffect(transaction.transactionId) {
        onLoadingChange(true)
        onErrorChange(null)
        
        try {
            // Fetch relatedRef document (Purchase or VehicleSale)
            transaction.relatedRef?.let { ref ->
                val collectionName = ref.path.split("/").firstOrNull() ?: ""
                when {
                    collectionName == "Purchase" || transaction.type == TransactionType.PURCHASE || transaction.type == TransactionType.BROKER_FEE -> {
                        PurchaseRepository.getPurchaseByReference(ref).fold(
                            onSuccess = { purchase ->
                                purchaseDetails = purchase
                                // Fetch owner and broker if they exist
                                purchase.ownerRef?.let { ownerRef ->
                                    VehicleRepository.getCustomerByReference(ownerRef).getOrNull()?.let {
                                        ownerDetails = it
                                    }
                                }
                                purchase.brokerRef?.let { brokerRef ->
                                    VehicleRepository.getBrokerByReference(brokerRef).getOrNull()?.let {
                                        brokerDetails = it
                                    }
                                }
                            },
                            onFailure = { e ->
                                onErrorChange(e.message ?: "Failed to load Purchase details")
                            }
                        )
                    }
                    collectionName == "VehicleSales" || transaction.type == TransactionType.SALE || transaction.type == TransactionType.EMI_PAYMENT -> {
                        SaleRepository.getVehicleSaleByReference(ref).fold(
                            onSuccess = { sale ->
                                vehicleSaleDetails = sale
                                // Fetch customer and vehicle if they exist
                                sale.customerRef?.let { customerRef ->
                                    VehicleRepository.getCustomerByReference(customerRef).getOrNull()?.let {
                                        vehicleCustomerDetails = it
                                    }
                                }
                            },
                            onFailure = { e ->
                                onErrorChange(e.message ?: "Failed to load VehicleSale details")
                            }
                        )
                    }
                }
            }
            
            // Fetch personRef document (Customer or Broker)
            transaction.personRef?.let { personRef ->
                when (transaction.personType) {
                    "CUSTOMER", "MIDDLE_MAN" -> {
                        VehicleRepository.getCustomerByReference(personRef).fold(
                            onSuccess = { customer ->
                                customerDetails = customer
                            },
                            onFailure = { e ->
                                // Don't set error if personRef fetch fails, just log it
                            }
                        )
                    }
                    "BROKER" -> {
                        VehicleRepository.getBrokerByReference(personRef).fold(
                            onSuccess = { broker ->
                                brokerDetails = broker
                            },
                            onFailure = { e ->
                                // Don't set error if personRef fetch fails, just log it
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            onErrorChange(e.message ?: "Failed to load related details")
        } finally {
            onLoadingChange(false)
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TranslatedText(
                        englishText = "Related Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = TranslationManager.translate("Close", isPunjabiEnabled))
                    }
                }
            }
            
            // Loading State
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // Error State
            error?.let { errorMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Purchase Details
            purchaseDetails?.let { purchase ->
                item {
                    PurchaseDetailsSection(
                        purchase = purchase,
                        ownerDetails = ownerDetails,
                        brokerDetails = brokerDetails,
                        currencyFormatter = currencyFormatter,
                        dateDisplayFormatter = dateDisplayFormatter,
                        isPunjabiEnabled = isPunjabiEnabled
                    )
                }
            }
            
            // VehicleSale Details
            vehicleSaleDetails?.let { sale ->
                item {
                    VehicleSaleDetailsSection(
                        sale = sale,
                        customerDetails = vehicleCustomerDetails,
                        currencyFormatter = currencyFormatter,
                        dateDisplayFormatter = dateDisplayFormatter,
                        isPunjabiEnabled = isPunjabiEnabled
                    )
                }
            }
            
            // Customer Details (from personRef)
            customerDetails?.let { customer ->
                item {
                    CustomerDetailsSection(
                        customer = customer,
                        isPunjabiEnabled = isPunjabiEnabled
                    )
                }
            }
            
            // Broker Details (from personRef)
            brokerDetails?.let { broker ->
                item {
                    BrokerDetailsSection(
                        broker = broker,
                        isPunjabiEnabled = isPunjabiEnabled
                    )
                }
            }
            
            // Empty State
            if (!isLoading && error == null && purchaseDetails == null && vehicleSaleDetails == null && customerDetails == null && brokerDetails == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            TranslatedText(
                                englishText = "No related details available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Purchase Details Section
@Composable
fun PurchaseDetailsSection(
    purchase: Purchase,
    ownerDetails: Customer?,
    brokerDetails: Broker?,
    currencyFormatter: NumberFormat,
    dateDisplayFormatter: SimpleDateFormat,
    isPunjabiEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                TranslatedText(
                    englishText = "Purchase Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            InfoItem(
                icon = Icons.Default.Tag,
                label = TranslationManager.translate("Purchase ID", isPunjabiEnabled),
                value = purchase.purchaseId
            )
            
            InfoItem(
                icon = Icons.Default.Receipt,
                label = TranslationManager.translate("Order Number", isPunjabiEnabled),
                value = purchase.orderNumber.toString()
            )
            
            purchase.transactionNumber?.let {
                InfoItem(
                    icon = Icons.Default.Numbers,
                    label = TranslationManager.translate("Transaction Number", isPunjabiEnabled),
                    value = it.toString()
                )
            }
            
            InfoItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = TranslationManager.translate("Grand Total", isPunjabiEnabled),
                value = currencyFormatter.format(purchase.grandTotal),
                isHighlighted = true
            )
            
            InfoItem(
                icon = Icons.Default.Receipt,
                label = TranslationManager.translate("GST Amount", isPunjabiEnabled),
                value = currencyFormatter.format(purchase.gstAmount)
            )
            
            // Payment Methods
            if (purchase.paymentMethods.isNotEmpty()) {
                HorizontalDivider()
                TranslatedText(
                    englishText = "Payment Methods",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                purchase.paymentMethods.forEach { (method, amount) ->
                    InfoItem(
                        icon = when (method.lowercase()) {
                            "cash" -> Icons.Default.Money
                            "bank" -> Icons.Default.AccountBalance
                            "credit" -> Icons.Default.CreditCard
                            else -> Icons.Default.Payment
                        },
                        label = TranslationManager.translate(method.replaceFirstChar { it.uppercase() }, isPunjabiEnabled),
                        value = currencyFormatter.format(amount.toDoubleOrNull() ?: 0.0)
                    )
                }
            }
            
            // Vehicle Details
            if (purchase.vehicle.isNotEmpty()) {
                HorizontalDivider()
                TranslatedText(
                    englishText = "Vehicle Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                purchase.vehicle.forEach { (key, value) ->
                    InfoItem(
                        icon = Icons.Default.DirectionsCar,
                        label = TranslationManager.translate(key.replaceFirstChar { it.uppercase() }, isPunjabiEnabled),
                        value = value
                    )
                }
            }
            
            if (purchase.middleMan.isNotBlank()) {
                InfoItem(
                    icon = Icons.Default.Person,
                    label = TranslationManager.translate("Middle Man", isPunjabiEnabled),
                    value = purchase.middleMan
                )
            }
            
            // Owner Details
            ownerDetails?.let { owner ->
                HorizontalDivider()
                TranslatedText(
                    englishText = "Owner Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                InfoItem(
                    icon = Icons.Default.Person,
                    label = TranslationManager.translate("Name", isPunjabiEnabled),
                    value = owner.name
                )
                InfoItem(
                    icon = Icons.Default.Phone,
                    label = TranslationManager.translate("Phone", isPunjabiEnabled),
                    value = owner.phone
                )
                InfoItem(
                    icon = Icons.Default.LocationOn,
                    label = TranslationManager.translate("Address", isPunjabiEnabled),
                    value = owner.address,
                    maxLines = 3
                )
            }
            
            // Broker Details
            brokerDetails?.let { broker ->
                HorizontalDivider()
                TranslatedText(
                    englishText = "Broker Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                InfoItem(
                    icon = Icons.Default.Person,
                    label = TranslationManager.translate("Name", isPunjabiEnabled),
                    value = broker.name
                )
                InfoItem(
                    icon = Icons.Default.Phone,
                    label = TranslationManager.translate("Phone", isPunjabiEnabled),
                    value = broker.phoneNumber
                )
                InfoItem(
                    icon = Icons.Default.LocationOn,
                    label = TranslationManager.translate("Address", isPunjabiEnabled),
                    value = broker.address,
                    maxLines = 3
                )
            }
            
            InfoItem(
                icon = Icons.Default.Schedule,
                label = TranslationManager.translate("Created At", isPunjabiEnabled),
                value = dateDisplayFormatter.format(Date(purchase.createdAt))
            )
        }
    }
}

// VehicleSale Details Section
@Composable
fun VehicleSaleDetailsSection(
    sale: VehicleSale,
    customerDetails: Customer?,
    currencyFormatter: NumberFormat,
    dateDisplayFormatter: SimpleDateFormat,
    isPunjabiEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                TranslatedText(
                    englishText = "Vehicle Sale Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            InfoItem(
                icon = Icons.Default.Tag,
                label = TranslationManager.translate("Sale ID", isPunjabiEnabled),
                value = sale.saleId
            )
            
            InfoItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = TranslationManager.translate("Total Amount", isPunjabiEnabled),
                value = currencyFormatter.format(sale.totalAmount),
                isHighlighted = true
            )
            
            InfoItem(
                icon = Icons.Default.CalendarToday,
                label = TranslationManager.translate("Purchase Date", isPunjabiEnabled),
                value = dateDisplayFormatter.format(Date(sale.purchaseDate))
            )
            
            InfoItem(
                icon = Icons.Default.Payment,
                label = TranslationManager.translate("EMI", isPunjabiEnabled),
                value = if (sale.emi) TranslationManager.translate("Yes", isPunjabiEnabled) else TranslationManager.translate("No", isPunjabiEnabled)
            )
            
            InfoItem(
                icon = Icons.Default.CheckCircle,
                label = TranslationManager.translate("Status", isPunjabiEnabled),
                value = if (sale.status) TranslationManager.translate("Completed", isPunjabiEnabled) else TranslationManager.translate("Pending", isPunjabiEnabled)
            )
            
            // Customer Details
            customerDetails?.let { customer ->
                HorizontalDivider()
                TranslatedText(
                    englishText = "Customer Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                InfoItem(
                    icon = Icons.Default.Person,
                    label = TranslationManager.translate("Name", isPunjabiEnabled),
                    value = customer.name
                )
                InfoItem(
                    icon = Icons.Default.Phone,
                    label = TranslationManager.translate("Phone", isPunjabiEnabled),
                    value = customer.phone
                )
                InfoItem(
                    icon = Icons.Default.LocationOn,
                    label = TranslationManager.translate("Address", isPunjabiEnabled),
                    value = customer.address,
                    maxLines = 3
                )
            }
            
            // Document Handover Flags
            HorizontalDivider()
            TranslatedText(
                englishText = "Document Handover",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            InfoItem(
                icon = Icons.Default.Description,
                label = TranslationManager.translate("NOC Handed Over", isPunjabiEnabled),
                value = if (sale.nocHandedOver) TranslationManager.translate("Yes", isPunjabiEnabled) else TranslationManager.translate("No", isPunjabiEnabled)
            )
            InfoItem(
                icon = Icons.Default.Description,
                label = TranslationManager.translate("RC Handed Over", isPunjabiEnabled),
                value = if (sale.rcHandedOver) TranslationManager.translate("Yes", isPunjabiEnabled) else TranslationManager.translate("No", isPunjabiEnabled)
            )
            InfoItem(
                icon = Icons.Default.Description,
                label = TranslationManager.translate("Insurance Handed Over", isPunjabiEnabled),
                value = if (sale.insuranceHandedOver) TranslationManager.translate("Yes", isPunjabiEnabled) else TranslationManager.translate("No", isPunjabiEnabled)
            )
            InfoItem(
                icon = Icons.Default.Description,
                label = TranslationManager.translate("Other Docs Handed Over", isPunjabiEnabled),
                value = if (sale.otherDocsHandedOver) TranslationManager.translate("Yes", isPunjabiEnabled) else TranslationManager.translate("No", isPunjabiEnabled)
            )
        }
    }
}

// Customer Details Section
@Composable
fun CustomerDetailsSection(
    customer: Customer,
    isPunjabiEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                TranslatedText(
                    englishText = "Customer Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            InfoItem(
                icon = Icons.Default.Tag,
                label = TranslationManager.translate("Customer ID", isPunjabiEnabled),
                value = customer.customerId
            )
            
            InfoItem(
                icon = Icons.Default.Person,
                label = TranslationManager.translate("Name", isPunjabiEnabled),
                value = customer.name,
                isHighlighted = true
            )
            
            InfoItem(
                icon = Icons.Default.Phone,
                label = TranslationManager.translate("Phone", isPunjabiEnabled),
                value = customer.phone
            )
            
            InfoItem(
                icon = Icons.Default.LocationOn,
                label = TranslationManager.translate("Address", isPunjabiEnabled),
                value = customer.address,
                maxLines = 3
            )
            
            if (customer.idProofType.isNotBlank()) {
                InfoItem(
                    icon = Icons.Default.Badge,
                    label = TranslationManager.translate("ID Proof Type", isPunjabiEnabled),
                    value = customer.idProofType
                )
            }
            
            if (customer.idProofNumber.isNotBlank()) {
                InfoItem(
                    icon = Icons.Default.Numbers,
                    label = TranslationManager.translate("ID Proof Number", isPunjabiEnabled),
                    value = customer.idProofNumber
                )
            }
            
            InfoItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = TranslationManager.translate("Balance", isPunjabiEnabled),
                value = "${if (customer.amount >= 0) "+" else ""}${customer.amount}",
                isHighlighted = true
            )
        }
    }
}

// Broker Details Section
@Composable
fun BrokerDetailsSection(
    broker: Broker,
    isPunjabiEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                TranslatedText(
                    englishText = "Broker Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            InfoItem(
                icon = Icons.Default.Tag,
                label = TranslationManager.translate("Broker ID", isPunjabiEnabled),
                value = broker.brokerId
            )
            
            InfoItem(
                icon = Icons.Default.Person,
                label = TranslationManager.translate("Name", isPunjabiEnabled),
                value = broker.name,
                isHighlighted = true
            )
            
            InfoItem(
                icon = Icons.Default.Phone,
                label = TranslationManager.translate("Phone Number", isPunjabiEnabled),
                value = broker.phoneNumber
            )
            
            InfoItem(
                icon = Icons.Default.LocationOn,
                label = TranslationManager.translate("Address", isPunjabiEnabled),
                value = broker.address,
                maxLines = 3
            )
            
            InfoItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = TranslationManager.translate("Amount", isPunjabiEnabled),
                value = "${if (broker.amount >= 0) "+" else ""}${broker.amount}",
                isHighlighted = true
            )
        }
    }
}