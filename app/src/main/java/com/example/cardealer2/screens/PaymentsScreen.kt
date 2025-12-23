package com.example.cardealer2.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Payment
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
import java.text.SimpleDateFormat
import java.util.*

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
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val tabs = listOf("All Purchases", "Due Today", "Completed")
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = "Payments",
                navController = navController,
                actions = {
                    IconButton(onClick = { navController.navigate("emi_due") }) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = "View EMI schedule"
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
                                Text(
                                    text = "Error",
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
                                    Text(
                                        text = when (selectedTabIndex) {
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
                                        onRecordPayment = { saleId, cashAmount, bankAmount ->
                                            viewModel.recordEmiPayment(saleId, cashAmount, bankAmount)
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

@Composable
fun PaymentItemCard(
    saleDetail: PaymentsViewModel.SaleWithDetails,
    onRecordPayment: (String, Double, Double) -> Unit
) {
    val sale = saleDetail.sale
    val customer = saleDetail.customer
    val vehicle = saleDetail.vehicle
    
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
                        text = customer?.customerName ?: "Unknown Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = vehicle?.productId ?: "Unknown Vehicle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (sale.status) {
                        "Active" -> MaterialTheme.colorScheme.primaryContainer
                        "Completed" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = sale.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when (sale.status) {
                            "Active" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "Completed" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%.2f", sale.totalAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                when (sale.purchaseType) {
                    "FULL_PAYMENT" -> {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Paid in Full",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val paidAmount = if (sale.downPayment > 0) sale.downPayment else sale.totalAmount
                            Text(
                                text = "₹${String.format("%.2f", paidAmount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    "DOWN_PAYMENT" -> {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Down Payment",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%.2f", sale.downPayment)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    "EMI" -> {
                        sale.emiDetails?.let { emi ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Next Due Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDate(emi.nextDueDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remaining: ${emi.remainingInstallments}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // EMI Details
            if (sale.purchaseType == "EMI" && sale.status == "Active") {
                sale.emiDetails?.let { emi ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "EMI Amount",
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
                                Text("Record Payment")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Payment Dialog
    if (showPaymentDialog) {
        SplitPaymentDialog(
            sale = sale,
            onPaymentRecorded = { cashAmount, bankAmount ->
                onRecordPayment(sale.saleId, cashAmount, bankAmount)
                showPaymentDialog = false
            },
            onDismiss = { showPaymentDialog = false }
        )
    }
}

@Composable
fun SplitPaymentDialog(
    sale: VehicleSale,
    onPaymentRecorded: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var cashAmount by remember { mutableStateOf("") }
    var bankAmount by remember { mutableStateOf("") }
    var cashError by remember { mutableStateOf<String?>(null) }
    var bankError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    val emiAmount = sale.emiDetails?.installmentAmount ?: 0.0
    
    LaunchedEffect(emiAmount) {
        if (emiAmount > 0 && cashAmount.isEmpty() && bankAmount.isEmpty()) {
            bankAmount = String.format(Locale.getDefault(), "%.2f", emiAmount)
        }
    }
    
    fun parseAmount(value: String): Double =
        value.toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0
    
    fun normalizeValues(changedCash: Boolean) {
        cashError = null
        bankError = null
        totalError = null
        
        val cash = parseAmount(cashAmount)
        val bank = parseAmount(bankAmount)
        val total = cash + bank
        
        if (changedCash) {
            if (emiAmount > 0) {
                val remaining = (emiAmount - cash).coerceAtLeast(0.0)
                bankAmount = String.format(Locale.getDefault(), "%.2f", remaining)
            }
        } else {
            if (emiAmount > 0) {
                val remaining = (emiAmount - bank).coerceAtLeast(0.0)
                cashAmount = String.format(Locale.getDefault(), "%.2f", remaining)
            }
        }
        
        if (cash < 0) cashError = "Cash cannot be negative"
        if (bank < 0) bankError = "Bank cannot be negative"
        
        if (emiAmount > 0) {
            if (parseAmount(cashAmount) + parseAmount(bankAmount) > emiAmount + 0.01) {
                totalError = "Total exceeds EMI amount (₹${String.format(Locale.getDefault(), "%.2f", emiAmount)})"
            }
        } else if (total <= 0) {
            totalError = "Enter cash or bank amount"
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column {
                Text(
                    text = "EMI Amount: ₹${String.format("%.2f", emiAmount)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = cashAmount,
                    onValueChange = {
                        cashAmount = it
                        normalizeValues(changedCash = true)
                    },
                    label = { Text("Cash Amount") },
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
                        normalizeValues(changedCash = false)
                    },
                    label = { Text("Bank Amount") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cash = cashAmount.toDoubleOrNull() ?: 0.0
                    val bank = bankAmount.toDoubleOrNull() ?: (if (cash <= 0.0) emiAmount else 0.0)
                    onPaymentRecorded(cash, bank)
                },
                enabled = totalError == null && (
                    (cashAmount.toDoubleOrNull() ?: 0.0) +
                    (bankAmount.toDoubleOrNull() ?: 0.0) > 0.0 || emiAmount > 0
                )
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

