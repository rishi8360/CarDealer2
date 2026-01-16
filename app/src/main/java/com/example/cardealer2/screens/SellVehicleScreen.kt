package com.example.cardealer2.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.SellVehicleViewModel
import com.example.cardealer2.data.Customer
import com.example.cardealer2.data.Product
import com.example.cardealer2.utility.ConsistentTopAppBar
import com.example.cardealer2.utility.CustomerSearchableDropdown
import com.example.cardealer2.utility.DatePickerButton
import com.example.cardealer2.utils.TranslationManager
import com.example.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext
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
    val downPayment by viewModel.downPayment.collectAsState()
    val cashDownPayment by viewModel.cashDownPayment.collectAsState()
    val bankDownPayment by viewModel.bankDownPayment.collectAsState()
    val interestRate by viewModel.interestRate.collectAsState()
    val frequency by viewModel.frequency.collectAsState()
    val durationMonths by viewModel.durationMonths.collectAsState()
    val calculatedEmi by viewModel.calculatedEmi.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val nocHandedOver by viewModel.nocHandedOver.collectAsState()
    val rcHandedOver by viewModel.rcHandedOver.collectAsState()
    val insuranceHandedOver by viewModel.insuranceHandedOver.collectAsState()
    val otherDocsHandedOver by viewModel.otherDocsHandedOver.collectAsState()
    
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
    
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Sell Vehicle", isPunjabiEnabled),
                subtitle = selectedVehicle?.productId ?: TranslationManager.translate("Loading...", isPunjabiEnabled),
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
                            TranslatedText(
                                englishText = "Vehicle not found",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                TranslatedText("Go Back")
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
                            .imePadding()
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Vehicle Info Card
                        VehicleInfoCard(
                            vehicle = selectedVehicle!!,
                            salePrice = salePrice,
                            onSalePriceChange = { viewModel.setSalePrice(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        TranslatedText(
                            englishText = "Customer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CustomerSearchableDropdown(
                            label = TranslationManager.translate("Select Customer", isPunjabiEnabled),
                            customers = customers,
                            selectedCustomer = selectedCustomer,
                            onCustomerSelected = { viewModel.setSelectedCustomer(it) },
                            onShowAddDialog = { _ -> navController.navigate("add_customer") },
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
                                        salePrice = salePrice,
                                        downPayment = downPayment,
                                        onDownPaymentChange = { viewModel.setDownPayment(it) },
                                        cashDownPayment = cashDownPayment,
                                        onCashDownPaymentChange = { viewModel.setCashDownPayment(it) },
                                        bankDownPayment = bankDownPayment,
                                        onBankDownPaymentChange = { viewModel.setBankDownPayment(it) },
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

                            // Documents handed over section
                            TranslatedText(
                                englishText = "Documents Handed Over",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = nocHandedOver,
                                    onCheckedChange = { viewModel.setNocHandedOver(it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TranslatedText(englishText = "NOC")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rcHandedOver,
                                    onCheckedChange = { viewModel.setRcHandedOver(it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TranslatedText(englishText = "RC")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = insuranceHandedOver,
                                    onCheckedChange = { viewModel.setInsuranceHandedOver(it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TranslatedText(englishText = "Insurance")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = otherDocsHandedOver,
                                    onCheckedChange = { viewModel.setOtherDocsHandedOver(it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TranslatedText(englishText = "Other Documents")
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Date Picker
                            var selectedDate by remember { mutableStateOf("") }
                            DatePickerButton(
                                selectedDate = selectedDate,
                                onDateSelected = { selectedDate = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Note Field
                            var note by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { TranslatedText("Note (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                leadingIcon = { Icon(Icons.Outlined.Description, null) }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Submit Button
                            Button(
                                onClick = { viewModel.completeSale(note, selectedDate) },
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
                                TranslatedText("Complete Sale")
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
        val context = LocalContext.current
        val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
            .collectAsState(initial = false)
        
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            TranslatedText(
                englishText = "Vehicle Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(TranslationManager.translate("Model", isPunjabiEnabled), vehicle.productId, isPunjabiEnabled)
            InfoRow(TranslationManager.translate("Chassis Number", isPunjabiEnabled), vehicle.chassisNumber, isPunjabiEnabled)
            InfoRow(TranslationManager.translate("Purchase Price", isPunjabiEnabled), "₹${vehicle.price}", isPunjabiEnabled)
            if (vehicle.sellingPrice > 0) {
                InfoRow(TranslationManager.translate("Selling Price", isPunjabiEnabled), "₹${vehicle.sellingPrice}", isPunjabiEnabled)
            }
            InfoRow(TranslationManager.translate("Year", isPunjabiEnabled), vehicle.year.toString(), isPunjabiEnabled)
            InfoRow(TranslationManager.translate("Color", isPunjabiEnabled), vehicle.colour, isPunjabiEnabled)
            
            Spacer(modifier = Modifier.height(20.dp))
            TranslatedText(
                englishText = "Sale Price",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = salePrice,
                onValueChange = onSalePriceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { TranslatedText("Sale Price (₹)") },
                leadingIcon = { Icon(Icons.Outlined.AttachMoney, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TranslatedText(
                englishText = if (vehicle.sellingPrice > 0) {
                    "Prefilled with the selling price. Update if selling at a different amount."
                } else {
                    "Prefilled with the purchase price. Update if selling at a different amount."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isPunjabiEnabled: Boolean = false) {
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
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
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
                TranslatedText(
                    englishText = "Payment Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                TranslatedText(
                    englishText = when (paymentType) {
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
                contentDescription = TranslationManager.translate("Select Payment Type", isPunjabiEnabled)
            )
        }
    }
}

@Composable
fun FullPaymentSummary(
    salePrice: String,
    originalPrice: Int
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
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
            TranslatedText(
                englishText = "Full Payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (amount != null) {
                val upfrontText = if (isPunjabiEnabled) {
                    "${TranslationManager.translate("Customer pays", isPunjabiEnabled)} ₹${String.format("%.2f", amount)} ${TranslationManager.translate("upfront", isPunjabiEnabled)}."
                } else {
                    "Customer pays ₹${String.format("%.2f", amount)} upfront."
                }
                Text(
                    text = upfrontText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (priceDifference != 0.0) {
                    val diffLabel = if (priceDifference > 0) TranslationManager.translate("above", isPunjabiEnabled) else TranslationManager.translate("below", isPunjabiEnabled)
                    val listedPriceText = TranslationManager.translate("the listed price", isPunjabiEnabled)
                    Text(
                        text = "${TranslationManager.translate("This is ₹", isPunjabiEnabled)}${String.format("%.2f", abs(priceDifference))} $diffLabel $listedPriceText (₹$originalPrice).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val matchesText = "${TranslationManager.translate("Matches the listed price of ₹", isPunjabiEnabled)}$originalPrice."
                    Text(
                        text = matchesText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                TranslatedText(
                    englishText = "Enter a valid sale amount above to proceed with full payment.",
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
    salePrice: String,
    downPayment: String,
    onDownPaymentChange: (String) -> Unit,
    cashDownPayment: String,
    onCashDownPaymentChange: (String) -> Unit,
    bankDownPayment: String,
    onBankDownPaymentChange: (String) -> Unit,
    interestRate: String,
    onInterestRateChange: (String) -> Unit,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    durationMonths: String,
    onDurationMonthsChange: (String) -> Unit,
    calculatedEmi: Double
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val currentFrequencyDisplay = remember(frequency, isPunjabiEnabled) {
        when(frequency) {
            "MONTHLY" -> TranslationManager.translate("MONTHLY", isPunjabiEnabled)
            "QUARTERLY" -> TranslationManager.translate("QUARTERLY", isPunjabiEnabled)
            "SEMI_ANNUALLY" -> TranslationManager.translate("SEMI_ANNUALLY", isPunjabiEnabled)
            "YEARLY" -> TranslationManager.translate("YEARLY", isPunjabiEnabled)
            else -> frequency
        }
    }
    
    // Calculate breakdown for display
    val salePriceAmount = salePrice.toDoubleOrNull() ?: 0.0
    val downPaymentAmount = downPayment.toDoubleOrNull() ?: 0.0
    val principal = salePriceAmount - downPaymentAmount
    val months = durationMonths.toIntOrNull() ?: 0
    val rate = interestRate.toDoubleOrNull() ?: 0.0
    
    // Calculate periods based on frequency (round down)
    val periods = remember(frequency, months) {
        when (frequency) {
            "MONTHLY" -> months
            "QUARTERLY" -> months / 3
            "SEMI_ANNUALLY" -> months / 6
            "YEARLY" -> months / 12
            else -> months
        }
    }
    
    val totalInterest = remember(principal, rate, periods) {
        if (principal > 0 && rate >= 0 && periods > 0) {
            principal * (rate / 100.0) * periods
        } else {
            0.0
        }
    }
    
    val totalAmount = principal + totalInterest
    
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
            TranslatedText(
                englishText = "EMI Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Down Payment Field
            OutlinedTextField(
                value = downPayment,
                onValueChange = onDownPaymentChange,
                label = { TranslatedText("Down Payment (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Outlined.Payment, null) },
                singleLine = true
            )
            
            // Cash and Bank Down Payment Fields (only show if down payment > 0)
            if (downPaymentAmount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                TranslatedText(
                    englishText = "Down Payment Split",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = cashDownPayment,
                        onValueChange = onCashDownPaymentChange,
                        label = { TranslatedText("Cash (₹)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Outlined.Money, null) },
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = bankDownPayment,
                        onValueChange = onBankDownPaymentChange,
                        label = { TranslatedText("Bank (₹)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Outlined.AccountBalance, null) },
                        singleLine = true
                    )
                }
                
                // Validation message
                val cashAmount = cashDownPayment.toDoubleOrNull() ?: 0.0
                val bankAmount = bankDownPayment.toDoubleOrNull() ?: 0.0
                val totalSplit = cashAmount + bankAmount
                val difference = kotlin.math.abs(totalSplit - downPaymentAmount)
                
                if (downPaymentAmount > 0 && difference > 0.01) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPunjabiEnabled) {
                            "ਕੁੱਲ ₹${String.format("%.2f", totalSplit)} ਹੈ, ਪਰ ਡਾਊਨ ਪੇਮੈਂਟ ₹${String.format("%.2f", downPaymentAmount)} ਹੈ।"
                        } else {
                            "Total is ₹${String.format("%.2f", totalSplit)}, but Down Payment is ₹${String.format("%.2f", downPaymentAmount)}."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (downPaymentAmount > 0 && totalSplit > 0 && difference <= 0.01) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPunjabiEnabled) {
                            "✓ ਰਕਮ ਮੈਚ ਕਰਦੀ ਹੈ"
                        } else {
                            "✓ Amounts match"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = interestRate,
                onValueChange = onInterestRateChange,
                label = { TranslatedText("Interest Rate (%)") },
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
                    value = currentFrequencyDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { TranslatedText("Payment Frequency") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("MONTHLY", "QUARTERLY", "SEMI_ANNUALLY", "YEARLY").forEach { freq ->
                        val displayText = when(freq) {
                            "MONTHLY" -> TranslationManager.translate("MONTHLY", isPunjabiEnabled)
                            "QUARTERLY" -> TranslationManager.translate("QUARTERLY", isPunjabiEnabled)
                            "SEMI_ANNUALLY" -> TranslationManager.translate("SEMI_ANNUALLY", isPunjabiEnabled)
                            "YEARLY" -> TranslationManager.translate("YEARLY", isPunjabiEnabled)
                            else -> freq
                        }
                        DropdownMenuItem(
                            text = { Text(displayText) },
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
                label = { TranslatedText("Duration (Months)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (calculatedEmi > 0 && principal > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        TranslatedText(
                            englishText = "EMI Calculation Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Principal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TranslatedText(
                                englishText = "Principal (Sale Price - Down Payment)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "₹${String.format("%.2f", principal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // Total Interest
                        if (totalInterest > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TranslatedText(
                                    englishText = "Total Interest (${rate}% × $periods periods)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "₹${String.format("%.2f", totalInterest)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        // Total Amount
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TranslatedText(
                                englishText = "Total Amount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // EMI per payment (frequency-based)
                        val emiLabel = remember(frequency, isPunjabiEnabled) {
                            when(frequency) {
                                "MONTHLY" -> TranslationManager.translate("EMI per Month", isPunjabiEnabled)
                                "QUARTERLY" -> TranslationManager.translate("EMI per Quarter", isPunjabiEnabled)
                                "SEMI_ANNUALLY" -> TranslationManager.translate("EMI per Semi-Annual Period", isPunjabiEnabled)
                                "YEARLY" -> TranslationManager.translate("EMI per Year", isPunjabiEnabled)
                                else -> TranslationManager.translate("EMI per Payment", isPunjabiEnabled)
                            }
                        }
                        Text(
                            text = emiLabel,
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
                        Spacer(modifier = Modifier.height(4.dp))
                        val paymentInfoText = remember(frequency, periods, months, isPunjabiEnabled) {
                            val paymentsText = TranslationManager.translate("payments", isPunjabiEnabled)
                            val monthsText = TranslationManager.translate("months", isPunjabiEnabled)
                            when(frequency) {
                                "MONTHLY", "QUARTERLY", "SEMI_ANNUALLY", "YEARLY" -> {
                                    "${TranslationManager.translate("For", isPunjabiEnabled)} $periods $paymentsText ($months $monthsText)"
                                }
                                else -> "${TranslationManager.translate("For", isPunjabiEnabled)} $periods $paymentsText"
                            }
                        }
                        Text(
                            text = paymentInfoText,
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
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { TranslatedText("Select Payment Type") },
        text = {
            Column {
                PaymentTypeOption(
                    title = TranslationManager.translate("Pay in Full", isPunjabiEnabled),
                    description = TranslationManager.translate("Customer pays the entire amount upfront", isPunjabiEnabled),
                    onClick = {
                        onPaymentTypeSelected("FULL_PAYMENT")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PaymentTypeOption(
                    title = TranslationManager.translate("EMI", isPunjabiEnabled),
                    description = TranslationManager.translate("Customer pays in installments with interest", isPunjabiEnabled),
                    onClick = {
                        onPaymentTypeSelected("EMI")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                TranslatedText("Cancel")
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

