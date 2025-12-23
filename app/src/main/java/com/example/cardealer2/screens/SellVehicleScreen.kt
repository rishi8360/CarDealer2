package com.example.cardealer2.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.SellVehicleViewModel
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Product
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.FilterableDropdownFieldWithDialog
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellVehicleScreen(
    chassisNumber: String,
    navController: NavController,
    viewModel: SellVehicleViewModel = viewModel()
) {
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val paymentType by viewModel.paymentType.collectAsState()
    val salePrice by viewModel.salePrice.collectAsState()
    val interestRate by viewModel.interestRate.collectAsState()
    val frequency by viewModel.frequency.collectAsState()
    val durationMonths by viewModel.durationMonths.collectAsState()
    val calculatedEmi by viewModel.calculatedEmi.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    
    var showPaymentTypeDialog by remember { mutableStateOf(false) }
    
    // Load vehicle on screen start
    LaunchedEffect(chassisNumber) {
        viewModel.loadVehicle(chassisNumber)
    }
    
    // Show success dialog and navigate back
    LaunchedEffect(success) {
        if (success) {
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = "Sell Vehicle",
                subtitle = selectedVehicle?.productId ?: "Loading...",
                navController = navController
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                isLoading && selectedVehicle == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                selectedVehicle == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Vehicle not found",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Vehicle Info Card
                        VehicleInfoCard(
                            vehicle = selectedVehicle!!,
                            salePrice = salePrice,
                            onSalePriceChange = { viewModel.setSalePrice(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Customer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterableDropdownFieldWithDialog(
                            label = "Select Customer",
                            items = customers,
                            selectedItem = selectedCustomer,
                            onItemSelected = { viewModel.setSelectedCustomer(it) },
                            onShowAddDialog = { navController.navigate("add_customer") },
                            itemToString = { customer -> "${customer.name} (${customer.phone})" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        selectedCustomer?.let { customer ->
                            Spacer(modifier = Modifier.height(12.dp))
                            SelectedCustomerInfoCard(customer = customer)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Payment Type Selection
                        if (selectedCustomer != null) {
                            PaymentTypeSelectionCard(
                                paymentType = paymentType,
                                onClick = { showPaymentTypeDialog = true }
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Payment Details Form
                            when (paymentType) {
                                "FULL_PAYMENT" -> {
                                    FullPaymentSummary(
                                        salePrice = salePrice,
                                        originalPrice = selectedVehicle!!.price
                                    )
                                }
                                "EMI" -> {
                                    EmiForm(
                                        interestRate = interestRate,
                                        onInterestRateChange = { viewModel.setInterestRate(it) },
                                        frequency = frequency,
                                        onFrequencyChange = { viewModel.setFrequency(it) },
                                        durationMonths = durationMonths,
                                        onDurationMonthsChange = { viewModel.setDurationMonths(it) },
                                        calculatedEmi = calculatedEmi
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Submit Button
                            Button(
                                onClick = { viewModel.completeSale() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = when (paymentType) {
                                    "FULL_PAYMENT" -> salePrice.toDoubleOrNull()?.let { it > 0 } ?: false
                                    "EMI" -> interestRate.isNotBlank() && durationMonths.isNotBlank() && calculatedEmi > 0
                                    else -> false
                                }
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Complete Sale")
                            }
                        }
                        
                        // Error Message
                        error?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    
    // Payment Type Selection Dialog
    if (showPaymentTypeDialog) {
        PaymentTypeSelectionDialog(
            onPaymentTypeSelected = { type ->
                viewModel.setPaymentType(type)
                showPaymentTypeDialog = false
            },
            onDismiss = { showPaymentTypeDialog = false }
        )
    }
}

@Composable
fun VehicleInfoCard(
    vehicle: Product,
    salePrice: String,
    onSalePriceChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Vehicle Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow("Model", vehicle.productId)
            InfoRow("Chassis Number", vehicle.chassisNumber)
            InfoRow("Listed Price", "₹${vehicle.price}")
            InfoRow("Year", vehicle.year.toString())
            InfoRow("Color", vehicle.colour)
            
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Sale Price",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = salePrice,
                onValueChange = onSalePriceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sale Price (₹)") },
                leadingIcon = { Icon(Icons.Outlined.AttachMoney, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Prefilled with the listed price. Update if selling at a different amount.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PaymentTypeSelectionCard(
    paymentType: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Payment Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (paymentType) {
                        "FULL_PAYMENT" -> "Pay in Full"
                        "EMI" -> "EMI"
                        else -> "Select Payment Type"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (paymentType != null) FontWeight.Medium else FontWeight.Normal,
                    color = if (paymentType != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = "Select Payment Type"
            )
        }
    }
}

@Composable
fun FullPaymentSummary(
    salePrice: String,
    originalPrice: Int
) {
    val amount = salePrice.toDoubleOrNull()?.takeIf { it > 0 }
    val priceDifference = amount?.minus(originalPrice) ?: 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Full Payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (amount != null) {
                Text(
                    text = "Customer pays ₹${String.format("%.2f", amount)} upfront.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (priceDifference != 0.0) {
                    val diffLabel = if (priceDifference > 0) "above" else "below"
                    Text(
                        text = "This is ₹${String.format("%.2f", abs(priceDifference))} $diffLabel the listed price (₹$originalPrice).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Matches the listed price of ₹$originalPrice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Enter a valid sale amount above to proceed with full payment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiForm(
    interestRate: String,
    onInterestRateChange: (String) -> Unit,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    durationMonths: String,
    onDurationMonthsChange: (String) -> Unit,
    calculatedEmi: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "EMI Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = interestRate,
                onValueChange = onInterestRateChange,
                label = { Text("Interest Rate (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Outlined.Percent, null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Frequency") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("MONTHLY", "QUARTERLY", "YEARLY").forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                onFrequencyChange(freq)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = durationMonths,
                onValueChange = onDurationMonthsChange,
                label = { Text("Duration (Months)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (calculatedEmi > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Calculated EMI",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%.2f", calculatedEmi)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total Amount: ₹${String.format("%.2f", calculatedEmi * (durationMonths.toIntOrNull() ?: 0))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentTypeSelectionDialog(
    onPaymentTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Payment Type") },
        text = {
            Column {
                PaymentTypeOption(
                    title = "Pay in Full",
                    description = "Customer pays the entire amount upfront",
                    onClick = {
                        onPaymentTypeSelected("FULL_PAYMENT")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PaymentTypeOption(
                    title = "EMI",
                    description = "Customer pays in installments with interest",
                    onClick = {
                        onPaymentTypeSelected("EMI")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaymentTypeOption(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SelectedCustomerInfoCard(customer: Customer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = customer.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (customer.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = customer.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

