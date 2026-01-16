package com.example.cardealer2.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.PaymentsViewModel
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Product
import com.example.cardealer2.data.VehicleSale
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.DatePickerButton
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    navController: NavController,
    viewModel: PaymentsViewModel = viewModel()
) {
    val allSalesWithDetails by viewModel.salesWithDetails.collectAsState()
    val salesDueTodayWithDetails by viewModel.salesDueTodayWithDetails.collectAsState()
    val completedSalesWithDetails by viewModel.completedSalesWithDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val tabs = remember(isPunjabiEnabled) {
        listOf(
            TranslationManager.translate("All Purchases", isPunjabiEnabled),
            TranslationManager.translate("Due Today", isPunjabiEnabled),
            TranslationManager.translate("Completed", isPunjabiEnabled)
        )
    }
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Payments", isPunjabiEnabled),
                navController = navController,
                actions = {
                    IconButton(onClick = { navController.navigate("emi_due") }) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = TranslationManager.translate("View EMI schedule", isPunjabiEnabled)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tabs
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TranslatedText(
                                    englishText = "Error",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        val salesToShow = when (selectedTabIndex) {
                            0 -> allSalesWithDetails
                            1 -> salesDueTodayWithDetails
                            2 -> completedSalesWithDetails
                            else -> emptyList()
                        }
                        
                        if (salesToShow.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.Payment,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TranslatedText(
                                        englishText = when (selectedTabIndex) {
                                            0 -> "No purchases found"
                                            1 -> "No payments due today"
                                            2 -> "No completed purchases"
                                            else -> "No data"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(salesToShow) { saleDetail ->
                                    PaymentItemCard(
                                        saleDetail = saleDetail,
                                        onRecordPayment = { sale, emiDetails, cashAmount, bankAmount, note, date ->
                                            viewModel.recordEmiPayment(sale, emiDetails, cashAmount, bankAmount, note, date)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun PaymentItemCard(
    saleDetail: PaymentsViewModel.SaleWithDetails,
    onRecordPayment: (com.example.cardealer2.data.VehicleSale, com.example.cardealer2.data.EmiDetails, Double, Double, String, String) -> Unit
) {
    val sale = saleDetail.sale
    val customer = saleDetail.customer
    val vehicle = saleDetail.vehicle
    
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var showPaymentDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer?.customerName ?: TranslationManager.translate("Unknown Customer", isPunjabiEnabled),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = vehicle?.productId ?: TranslationManager.translate("Unknown Vehicle", isPunjabiEnabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        sale.status -> MaterialTheme.colorScheme.tertiaryContainer  // true = completed
                        else -> MaterialTheme.colorScheme.primaryContainer  // false = pending
                    }
                ) {
                    Text(
                        text = if (sale.status) TranslationManager.translate("Completed", isPunjabiEnabled) else TranslationManager.translate("Pending", isPunjabiEnabled),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            sale.status -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Payment Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    TranslatedText(
                        englishText = "Total Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%.2f", sale.totalAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                when {
                    !sale.emi -> {
                        // Full payment (not EMI)
                        Column(horizontalAlignment = Alignment.End) {
                            TranslatedText(
                                englishText = "Paid in Full",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%.2f", sale.totalAmount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    sale.emi -> {
                        // EMI payment
                        saleDetail.emiDetails?.let { emi ->
                            Column(horizontalAlignment = Alignment.End) {
                                TranslatedText(
                                    englishText = "Next Due Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDate(emi.nextDueDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val remainingText = "${TranslationManager.translate("Remaining:", isPunjabiEnabled)} ${emi.remainingInstallments}"
                                Text(
                                    text = remainingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // EMI Details
            if (sale.emi && !sale.status) {  // is EMI and pending (not completed)
                saleDetail.emiDetails?.let { emi ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            TranslatedText(
                                englishText = "EMI Amount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%.2f", emi.installmentAmount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (emi.remainingInstallments > 0) {
                            Button(
                                onClick = { showPaymentDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                TranslatedText("Record Payment")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Payment Dialog
    if (showPaymentDialog && saleDetail.emiDetails != null) {
        SplitPaymentDialog(
            sale = sale,
            emiDetails = saleDetail.emiDetails,
            onPaymentRecorded = { cashAmount, bankAmount, note, date ->
                onRecordPayment(sale, saleDetail.emiDetails!!, cashAmount, bankAmount, note, date)
                showPaymentDialog = false
            },
            onDismiss = { showPaymentDialog = false }
        )
    }
}

@Composable
fun SplitPaymentDialog(
    sale: VehicleSale,
    onPaymentRecorded: (Double, Double, String, String) -> Unit,
    onDismiss: () -> Unit,
    emiDetails: com.example.cardealer2.data.EmiDetails? = null
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var cashAmount by remember { mutableStateOf("") }
    var bankAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var cashError by remember { mutableStateOf<String?>(null) }
    var bankError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    val emiAmount = emiDetails?.installmentAmount ?: 0.0
    
    // Date picker state
    var selectedDate by remember { mutableStateOf("") }
    

    
    fun parseAmount(value: String): Double =
        value.toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0
    
    fun validateInputs() {
        cashError = null
        bankError = null
        totalError = null
        
        val cash = parseAmount(cashAmount)
        val bank = parseAmount(bankAmount)
        val total = cash + bank
        
        // Only validate, don't auto-calculate
        if (cash < 0) cashError = TranslationManager.translate("Cash cannot be negative", isPunjabiEnabled)
        if (bank < 0) bankError = TranslationManager.translate("Bank cannot be negative", isPunjabiEnabled)
        
        if (total <= 0) {
            totalError = TranslationManager.translate("Enter cash or bank amount", isPunjabiEnabled)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { TranslatedText("Record Payment") },
        text = {
            Column {
                val emiAmountText = "${TranslationManager.translate("EMI Amount:", isPunjabiEnabled)} ₹${String.format("%.2f", emiAmount)}"
                Text(
                    text = emiAmountText,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = cashAmount,
                    onValueChange = {
                        cashAmount = it
                        validateInputs()
                    },
                    label = { TranslatedText("Cash Amount") },
                    isError = cashError != null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, null) },
                    singleLine = true
                )
                cashError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bankAmount,
                    onValueChange = {
                        bankAmount = it
                        validateInputs()
                    },
                    label = { TranslatedText("Bank Amount") },
                    isError = bankError != null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, null) },
                    singleLine = true
                )
                bankError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                totalError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // Calculate and display payment difference
                val totalPaid = parseAmount(cashAmount) + parseAmount(bankAmount)
                val difference = totalPaid - emiAmount
                
                if (totalPaid > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Payment Summary Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Amount Paid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = TranslationManager.translate("Total Amount Paid:", isPunjabiEnabled),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", totalPaid)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Difference Amount
                            if (emiAmount > 0) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                difference > 0 -> Icons.Outlined.Add
                                                difference < 0 -> Icons.Outlined.Remove
                                                else -> Icons.Outlined.AttachMoney
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = when {
                                                difference > 0 -> Color(0xFF4CAF50) // Green for extra
                                                difference < 0 -> MaterialTheme.colorScheme.error // Red for short
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Text(
                                            text = when {
                                                difference > 0 -> TranslationManager.translate("Extra Payment:", isPunjabiEnabled)
                                                difference < 0 -> TranslationManager.translate("Short Payment:", isPunjabiEnabled)
                                                else -> TranslationManager.translate("Difference:", isPunjabiEnabled)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${if (difference >= 0) "+" else ""}₹${String.format(Locale.getDefault(), "%.2f", abs(difference))}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            difference > 0 -> Color(0xFF4CAF50) // Green for extra
                                            difference < 0 -> MaterialTheme.colorScheme.error // Red for short
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                
                                // Customer notification message
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = TranslationManager.translate("This amount will be shown to the customer", isPunjabiEnabled),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Date Picker Button
                DatePickerButton(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { TranslatedText("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    leadingIcon = { Icon(Icons.Outlined.Description, null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cash = cashAmount.toDoubleOrNull() ?: 0.0
                    val bank = bankAmount.toDoubleOrNull() ?: 0.0
                    onPaymentRecorded(cash, bank, note, selectedDate)
                },
                enabled = totalError == null && (
                    (cashAmount.toDoubleOrNull() ?: 0.0) +
                    (bankAmount.toDoubleOrNull() ?: 0.0) > 0.0 || emiAmount > 0
                )
            ) {
                TranslatedText("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                TranslatedText("Cancel")
            }
        }
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

