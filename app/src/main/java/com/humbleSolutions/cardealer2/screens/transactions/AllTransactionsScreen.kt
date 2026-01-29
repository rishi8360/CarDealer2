package com.humbleSolutions.cardealer2.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.humbleSolutions.cardealer2.ViewModel.LedgerViewModel
import com.humbleSolutions.cardealer2.data.Customer
import com.humbleSolutions.cardealer2.data.PersonTransaction
import com.humbleSolutions.cardealer2.data.TransactionType
import com.humbleSolutions.cardealer2.repository.CustomerRepository
import com.humbleSolutions.cardealer2.repository.BrokerRepository
import com.humbleSolutions.cardealer2.data.Broker
import com.humbleSolutions.cardealer2.utility.ConsistentTopAppBar
import com.humbleSolutions.cardealer2.utility.CustomerSearchableDropdown
import com.humbleSolutions.cardealer2.utility.BrokerSearchableDropdown
import com.humbleSolutions.cardealer2.utility.DatePickerButton
import com.humbleSolutions.cardealer2.components.PdfPickerField
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    navController: NavController,
    viewModel: LedgerViewModel = viewModel()
) {
    val cashBalance by viewModel.cashBalance.collectAsState()
    val bankBalance by viewModel.bankBalance.collectAsState()
    val creditBalance by viewModel.creditBalance.collectAsState()
    val isLoadingBalances by viewModel.isLoadingBalances.collectAsState()
    
    val moneyReceivedGroups by viewModel.moneyReceivedGroups.collectAsState()
    val moneyPaidGroups by viewModel.moneyPaidGroups.collectAsState()
    val emiOutstanding by viewModel.emiOutstanding.collectAsState()
    val creditOwedGroups by viewModel.creditOwedGroups.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val hasMoreTransactions by viewModel.hasMoreTransactions.collectAsState()
    
    // Note: Transactions are already loaded in ViewModel.init, so we don't need to load again
    // This prevents duplicate loading and glitch effects
    
    // Customers and brokers list for transfer
    val customers by CustomerRepository.customers.collectAsState()
    val brokers by BrokerRepository.brokers.collectAsState()
    
    // Transfer card state - FROM selection
    var selectedFromType by remember { mutableStateOf<String?>(null) } // "Customer", "Broker", "Cash", "Bank"
    var selectedFromCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedFromBroker by remember { mutableStateOf<Broker?>(null) }
    
    // Transfer card state - TO selection
    var selectedToType by remember { mutableStateOf<String?>(null) }
    var selectedToCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedToBroker by remember { mutableStateOf<Broker?>(null) }
    
    // Transfer form state
    var transferAmount by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }
    var transferDate by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddBrokerDialog by remember { mutableStateOf(false) }
    var addCustomerName by remember { mutableStateOf("") }
    var addBrokerName by remember { mutableStateOf("") }
    var addSuccess by remember { mutableStateOf(false) }
    var addedCustomerName by remember { mutableStateOf<String?>(null) }
    var addedBrokerName by remember { mutableStateOf<String?>(null) }
    var isTransferring by remember { mutableStateOf(false) }
    
    // Calculate available TO options based on FROM selection
    val availableToOptions = remember(selectedFromType) {
        when (selectedFromType) {
            "Bank" -> listOf("Cash", "Customer", "Broker")
            "Cash" -> listOf("Bank", "Customer", "Broker")
            "Customer", "Broker" -> listOf("Bank", "Cash")
            else -> emptyList()
        }
    }
    
    // Calculate FROM balance (memoized to prevent recalculation)
    val fromBalance = remember(selectedFromType, selectedFromCustomer, selectedFromBroker, cashBalance, bankBalance) {
        when (selectedFromType) {
            "Customer" -> selectedFromCustomer?.amount?.toDouble() ?: 0.0
            "Broker" -> selectedFromBroker?.amount?.toDouble() ?: 0.0
            "Cash" -> cashBalance ?: 0.0
            "Bank" -> bankBalance ?: 0.0
            else -> 0.0
        }
    }
    
    var showBalanceAdjustDialog by remember { mutableStateOf<String?>(null) }
    var expandedSections by remember { mutableStateOf<Set<String>>(setOf("A")) } // Section A expanded by default
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    // Payment method options
    val paymentMethods = remember(isPunjabiEnabled) {
        listOf(
            TranslationManager.translate("Myself Bank", isPunjabiEnabled),
            TranslationManager.translate("Myself Cash", isPunjabiEnabled)
        )
    }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Ledger", isPunjabiEnabled),
                subtitle = TranslationManager.translate("Complete Financial Overview", isPunjabiEnabled),
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Transfer Card
                item(key = "transfer_card") {
                    TransferCard(
                        customers = customers,
                        brokers = brokers,
                        selectedFromType = selectedFromType,
                        onFromTypeSelected = { type ->
                            selectedFromType = type
                            selectedFromCustomer = null
                            selectedFromBroker = null
                            selectedToType = null
                            selectedToCustomer = null
                            selectedToBroker = null
                        },
                        selectedFromCustomer = selectedFromCustomer,
                        onFromCustomerSelected = { selectedFromCustomer = it },
                        selectedFromBroker = selectedFromBroker,
                        onFromBrokerSelected = { selectedFromBroker = it },
                        selectedToType = selectedToType,
                        onToTypeSelected = { type ->
                            selectedToType = type
                            if (type != "Customer") selectedToCustomer = null
                            if (type != "Broker") selectedToBroker = null
                        },
                        selectedToCustomer = selectedToCustomer,
                        onToCustomerSelected = { selectedToCustomer = it },
                        selectedToBroker = selectedToBroker,
                        onToBrokerSelected = { selectedToBroker = it },
                        availableToOptions = availableToOptions,
                        transferAmount = transferAmount,
                        onAmountChange = { transferAmount = it },
                        transferNote = transferNote,
                        onNoteChange = { transferNote = it },
                        transferDate = transferDate,
                        onDateChange = { transferDate = it },
                        onTransferClick = {
                                val amount = transferAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && selectedFromType != null && selectedToType != null) {
                                // Validate FROM selection
                                val fromValid = when (selectedFromType) {
                                    "Customer" -> selectedFromCustomer != null
                                    "Broker" -> selectedFromBroker != null
                                    "Cash", "Bank" -> true
                                    else -> false
                                }
                                
                                // Validate TO selection
                                val toValid = when (selectedToType) {
                                    "Customer" -> selectedToCustomer != null
                                    "Broker" -> selectedToBroker != null
                                    "Cash", "Bank" -> true
                                    else -> false
                                }
                                
                                if (fromValid && toValid) {
                                    isTransferring = true
                                    val description = "Transfer from ${
                                        when (selectedFromType) {
                                            "Customer" -> selectedFromCustomer!!.name
                                            "Broker" -> selectedFromBroker!!.name
                                            else -> selectedFromType
                                        }
                                    } to ${
                                        when (selectedToType) {
                                            "Customer" -> selectedToCustomer!!.name
                                            "Broker" -> selectedToBroker!!.name
                                            else -> selectedToType
                                        }
                                    }"
                                    
                                    viewModel.transferBetweenSources(
                                        fromType = selectedFromType!!,
                                        fromCustomer = selectedFromCustomer,
                                        fromBroker = selectedFromBroker,
                                        toType = selectedToType!!,
                                        toCustomer = selectedToCustomer,
                                        toBroker = selectedToBroker,
                                        amount = amount,
                                        description = description,
                                        note = transferNote,
                                        date = transferDate,
                                        onSuccess = {
                                            isTransferring = false
                                            selectedFromType = null
                                            selectedFromCustomer = null
                                            selectedFromBroker = null
                                            selectedToType = null
                                            selectedToCustomer = null
                                            selectedToBroker = null
                                            transferAmount = ""
                                            transferNote = ""
                                            transferDate = ""
                                        },
                                        onError = { error ->
                                            isTransferring = false
                                            // Handle error - could show a snackbar
                                        }
                                    )
                                }
                            }
                        },
                        onShowAddCustomerDialog = { name ->
                            addCustomerName = name
                            showAddCustomerDialog = true
                        },
                        onShowAddBrokerDialog = { name ->
                            addBrokerName = name
                            showAddBrokerDialog = true
                        },
                        addSuccess = addSuccess,
                        addedCustomerName = addedCustomerName,
                        addedBrokerName = addedBrokerName,
                        onSuccessHandled = {
                            addSuccess = false
                            addedCustomerName = null
                            addedBrokerName = null
                        },
                        isTransferring = isTransferring,
                        currencyFormatter = currencyFormatter,
                        cashBalance = cashBalance ?: 0.0,
                        bankBalance = bankBalance ?: 0.0,
                        fromBalance = fromBalance,
                        isPunjabiEnabled = isPunjabiEnabled
                    )
                }
                
                // Transaction History Section
                item(key = "transaction_history") {
                    TransactionHistorySection(
                        transactions = transactions,
                        currencyFormatter = currencyFormatter,
                        navController = navController
                    )
                }

                // Load More Button
                if (hasMoreTransactions) {
                    item(key = "load_more") {
                        Button(
                            onClick = { viewModel.loadMoreTransactions() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            TranslatedText(
                                englishText = "Load More Transactions",
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                // Section A: Money Balance
                item(key = "section_a") {
                    SectionA_MoneyBalance(
                        cashBalance = cashBalance ?: 0.0,
                        bankBalance = bankBalance ?: 0.0,
                        creditBalance = creditBalance ?: 0.0,
                        isLoading = isLoadingBalances,
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { type -> showBalanceAdjustDialog = type },
                        onEditAllClick = { showBalanceAdjustDialog = "ALL" },
                        isExpanded = expandedSections.contains("A"),
                        onToggleExpand = {
                            expandedSections = if (expandedSections.contains("A")) {
                                expandedSections - "A"
                            } else {
                                expandedSections + "A"
                            }
                        }
                    )
                }


            }
        }
    }
    
    // Balance Edit Dialog - Shows all balances for direct editing
    showBalanceAdjustDialog?.let { type ->
        if (type == "ALL") {
            BalanceEditDialog(
                cashBalance = cashBalance ?: 0.0,
                bankBalance = bankBalance ?: 0.0,
                creditBalance = creditBalance ?: 0.0,
                currencyFormatter = currencyFormatter,
                onDismiss = { showBalanceAdjustDialog = null },
                onSave = { cash, bank, credit, description, reason ->
                    // Save all balances
                    var savedCount = 0
                    val totalCount = 3
                    
                    fun checkComplete() {
                        savedCount++
                        if (savedCount == totalCount) {
                            showBalanceAdjustDialog = null
                        }
                    }
                    
                    viewModel.setCapitalBalance(
                        type = "Cash",
                        newBalance = cash,
                        description = description,
                        reason = reason,
                        onSuccess = { checkComplete() },
                        onError = { checkComplete() }
                    )
                    viewModel.setCapitalBalance(
                        type = "Bank",
                        newBalance = bank,
                        description = description,
                        reason = reason,
                        onSuccess = { checkComplete() },
                        onError = { checkComplete() }
                    )
                    viewModel.setCapitalBalance(
                        type = "Credit",
                        newBalance = credit,
                        description = description,
                        reason = reason,
                        onSuccess = { checkComplete() },
                        onError = { checkComplete() }
                    )
                }
            )
        } else {
            // Single balance edit dialog (direct editing)
        BalanceAdjustmentDialog(
            type = type,
            currentBalance = when (type) {
                "Cash" -> cashBalance ?: 0.0
                "Bank" -> bankBalance ?: 0.0
                "Credit" -> creditBalance ?: 0.0
                else -> 0.0
            },
            currencyFormatter = currencyFormatter,
                onDismiss = { showBalanceAdjustDialog = null },
                onAdjust = { newBalance, description, reason ->
                    // newBalance is the direct value to set
                    viewModel.setCapitalBalance(
                        type = type,
                        newBalance = newBalance,
                        description = description,
                        reason = reason,
                        onSuccess = { showBalanceAdjustDialog = null },
                        onError = { /* Handle error */ }
                    )
                }
        )
        }
    }
    
    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { TranslatedText("Add Customer") },
            text = {
                Column {
                    val customerNotFoundText = if (isPunjabiEnabled) {
                        "${TranslationManager.translate("Customer", isPunjabiEnabled)} \"$addCustomerName\" ${TranslationManager.translate("not found.", isPunjabiEnabled)}"
                    } else {
                        "Customer \"$addCustomerName\" not found."
                    }
                    Text(customerNotFoundText)
                    Spacer(modifier = Modifier.height(8.dp))
                    TranslatedText("Would you like to add this customer?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddCustomerDialog = false
                        navController.navigate("add_customer")
                    }
                ) {
                    TranslatedText("Add Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    TranslatedText("Cancel")
                }
            }
        )
    }
    
    // Add Broker Dialog
    val scope = rememberCoroutineScope()
    if (showAddBrokerDialog) {
        AddBrokerDialogForTransactions(
            onDismiss = { showAddBrokerDialog = false },
            onAddSuccess = { name ->
                addedBrokerName = name
                addSuccess = true
                // Reset success flag after a delay to allow BrokerSearchableDropdown to auto-select
                scope.launch {
                    kotlinx.coroutines.delay(1000)
                    addSuccess = false
                    addedBrokerName = null
                }
            }
        )
    }
}

// Section A: Money Balance Cards
@Composable
fun SectionA_MoneyBalance(
    cashBalance: Double,
    bankBalance: Double,
    creditBalance: Double,
    isLoading: Boolean,
    currencyFormatter: NumberFormat,
    onAdjustClick: (String) -> Unit,
    onEditAllClick: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    TranslatedText(
                        englishText = "💰 A. How much money do I have?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit All Button
                    IconButton(onClick = onEditAllClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = TranslationManager.translate("Edit All Balances", isPunjabiEnabled),
                            tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) 
                            TranslationManager.translate("Collapse", isPunjabiEnabled) 
                        else 
                            TranslationManager.translate("Expand", isPunjabiEnabled)
                    )
                    }
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    // Cash Balance Card
                    BalanceCard(
                        title = TranslationManager.translate("Cash Balance", isPunjabiEnabled),
                        balance = cashBalance,
                        icon = Icons.Default.Money,
                        iconColor = Color(0xFF34A853),
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { onAdjustClick("Cash") }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Bank Balance Card
                    BalanceCard(
                        title = TranslationManager.translate("Bank Balance", isPunjabiEnabled),
                        balance = bankBalance,
                        icon = Icons.Default.AccountBalance,
                        iconColor = Color(0xFF4285F4),
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { onAdjustClick("Bank") }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Credit Balance Card
                    BalanceCard(
                        title = TranslationManager.translate("Credit Balance", isPunjabiEnabled),
                        balance = creditBalance,
                        icon = Icons.Default.CreditCard,
                        iconColor = Color(0xFFEA4335),
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { onAdjustClick("Credit") },
                        description = TranslationManager.translate("Money you owe to suppliers/brokers", isPunjabiEnabled)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceCard(
    title: String,
    balance: Double,
    icon: ImageVector,
    iconColor: Color,
    currencyFormatter: NumberFormat,
    onAdjustClick: () -> Unit,
    description: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.12f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = currencyFormatter.format(balance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            IconButton(onClick = onAdjustClick) {
                val context = LocalContext.current
                val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
                    .collectAsState(initial = false)
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = TranslationManager.translate("Adjust Balance", isPunjabiEnabled),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Section B: Who gave me money
@Composable
fun SectionB_MoneyReceived(
    moneyReceivedGroups: List<com.humbleSolutions.cardealer2.ViewModel.MoneyReceivedGroup>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    ExpandableSectionCard(
        title = TranslationManager.translate("🤝 B. Who gave me money?", isPunjabiEnabled),
        subtitle = "${moneyReceivedGroups.size} ${TranslationManager.translate("customers", isPunjabiEnabled)}",
        icon = Icons.Default.TrendingUp,
        iconColor = Color(0xFF34A853),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (moneyReceivedGroups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Person,
                message = TranslationManager.translate("No money received yet", isPunjabiEnabled)
            )
        } else {
            moneyReceivedGroups.forEach { group ->
                MoneyReceivedGroupItem(
                    group = group,
                    currencyFormatter = currencyFormatter
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun MoneyReceivedGroupItem(
    group: com.humbleSolutions.cardealer2.ViewModel.MoneyReceivedGroup,
    currencyFormatter: NumberFormat
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = group.personType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
            Text(
                        text = currencyFormatter.format(group.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34A853)
                    )
            Text(
                        text = "${group.transactions.size} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                group.transactions.forEach { transaction ->
                    TransactionRowItem(transaction, currencyFormatter, dateFormatter)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Section C: Whom did I give money to
@Composable
fun SectionC_MoneyPaid(
    moneyPaidGroups: List<com.humbleSolutions.cardealer2.ViewModel.MoneyPaidGroup>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    ExpandableSectionCard(
        title = TranslationManager.translate("📤 C. Whom did I give money to?", isPunjabiEnabled),
        subtitle = "${moneyPaidGroups.size} ${TranslationManager.translate("people", isPunjabiEnabled)}",
        icon = Icons.Default.ShoppingCart,
        iconColor = Color(0xFFEA4335),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (moneyPaidGroups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ShoppingCart,
                message = TranslationManager.translate("No payments made yet", isPunjabiEnabled)
            )
        } else {
            moneyPaidGroups.forEach { group ->
                MoneyPaidGroupItem(
                    group = group,
                    currencyFormatter = currencyFormatter
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun MoneyPaidGroupItem(
    group: com.humbleSolutions.cardealer2.ViewModel.MoneyPaidGroup,
    currencyFormatter: NumberFormat
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = group.personType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(group.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA4335)
                    )
                    val transactionsText = "${group.transactions.size} ${TranslationManager.translate("transactions", isPunjabiEnabled)}"
                    Text(
                        text = transactionsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                group.transactions.forEach { transaction ->
                    TransactionRowItem(transaction, currencyFormatter, dateFormatter)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Section D: Who owes me money (EMI Outstanding)
@Composable
fun SectionD_EmiOutstanding(
    emiOutstanding: List<com.humbleSolutions.cardealer2.ViewModel.EmiOutstanding>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val totalOutstanding = emiOutstanding.sumOf { it.outstandingAmount }
    
    ExpandableSectionCard(
        title = TranslationManager.translate("🧾 D. Who owes me money?", isPunjabiEnabled),
        subtitle = "${emiOutstanding.size} ${TranslationManager.translate("active EMI customers", isPunjabiEnabled)} • ${currencyFormatter.format(totalOutstanding)} ${TranslationManager.translate("outstanding", isPunjabiEnabled)}",
        icon = Icons.Default.Payment,
        iconColor = Color(0xFFFBBC04),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (emiOutstanding.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Payment,
                message = TranslationManager.translate("No outstanding EMI payments", isPunjabiEnabled)
            )
        } else {
            emiOutstanding.forEach { emi ->
                EmiOutstandingItem(emi, currencyFormatter)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun EmiOutstandingItem(
    emi: com.humbleSolutions.cardealer2.ViewModel.EmiOutstanding,
    currencyFormatter: NumberFormat
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val nextDueDateText = if (emi.nextDueDate > 0) {
        dateFormatter.format(emi.nextDueDate)
    } else {
        TranslationManager.translate("Not set", isPunjabiEnabled)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = emi.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val remainingText = "${TranslationManager.translate("Remaining:", isPunjabiEnabled)} ${emi.remainingInstallments} ${TranslationManager.translate("installments", isPunjabiEnabled)}"
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val nextDueText = "${TranslationManager.translate("Next due:", isPunjabiEnabled)} $nextDueDateText"
                    Text(
                        text = nextDueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(emi.outstandingAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBC04)
                    )
                    val installmentText = "${currencyFormatter.format(emi.installmentAmount)}${TranslationManager.translate("/installment", isPunjabiEnabled)}"
                    Text(
                        text = installmentText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Section E: Who do I owe money to
@Composable
fun SectionE_CreditOwed(
    creditOwedGroups: List<com.humbleSolutions.cardealer2.ViewModel.CreditOwed>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    ExpandableSectionCard(
        title = TranslationManager.translate("📉 E. Who do I owe money to?", isPunjabiEnabled),
        subtitle = "${creditOwedGroups.size} ${TranslationManager.translate("suppliers/brokers", isPunjabiEnabled)}",
        icon = Icons.Default.CreditCard,
        iconColor = Color(0xFFEA4335),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (creditOwedGroups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.CreditCard,
                message = TranslationManager.translate("No credit purchases", isPunjabiEnabled)
            )
        } else {
            creditOwedGroups.forEach { group ->
                CreditOwedItem(group, currencyFormatter)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun CreditOwedItem(
    group: com.humbleSolutions.cardealer2.ViewModel.CreditOwed,
    currencyFormatter: NumberFormat
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = group.personType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(group.totalCreditAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA4335)
                    )
                    val purchasesText = "${group.transactions.size} ${TranslationManager.translate("purchases", isPunjabiEnabled)}"
                    Text(
                        text = purchasesText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                group.transactions.forEach { transaction ->
                    TransactionRowItem(transaction, currencyFormatter, dateFormatter)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Helper Components
@Composable
fun ExpandableSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) 
                            TranslationManager.translate("Collapse", isPunjabiEnabled) 
                        else 
                            TranslationManager.translate("Expand", isPunjabiEnabled)
                    )
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: PersonTransaction,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getTransactionTypeLabel(transaction.type),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateFormatter.format(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            transaction.orderNumber?.let {
                Text(
                    text = "Order #$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = currencyFormatter.format(transaction.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun getTransactionTypeLabel(type: String): String {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    return when (type) {
        TransactionType.PURCHASE -> TranslationManager.translate("Purchase", isPunjabiEnabled)
        TransactionType.SALE -> TranslationManager.translate("Sale", isPunjabiEnabled)
        TransactionType.EMI_PAYMENT -> TranslationManager.translate("EMI Payment", isPunjabiEnabled)
        TransactionType.BROKER_FEE -> TranslationManager.translate("Broker Fee", isPunjabiEnabled)
        else -> type
    }
}

// Balance Edit Dialog - Direct editing of all balances
@Composable
fun BalanceEditDialog(
    cashBalance: Double,
    bankBalance: Double,
    creditBalance: Double,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, String, String?) -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var cashText by remember { mutableStateOf(cashBalance.toString()) }
    var bankText by remember { mutableStateOf(bankBalance.toString()) }
    var creditText by remember { mutableStateOf(creditBalance.toString()) }
    var description by remember { mutableStateOf(TranslationManager.translate("Manual Balance Edit", isPunjabiEnabled)) }
    var reason by remember { mutableStateOf("") }
    
    val cashAmount = cashText.toDoubleOrNull() ?: 0.0
    val bankAmount = bankText.toDoubleOrNull() ?: 0.0
    val creditAmount = creditText.toDoubleOrNull() ?: 0.0
    
    val isValid = cashText.isNotBlank() && bankText.isNotBlank() && creditText.isNotBlank() &&
            cashAmount >= 0 && bankAmount >= 0 && creditAmount >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = TranslationManager.translate("Edit Balances", isPunjabiEnabled),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TranslatedText(
                    englishText = "Enter the exact amounts for each balance type:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Cash Balance Input
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                            cashText = newValue
                        }
                    },
                    label = { TranslatedText("Cash Balance *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Money,
                            contentDescription = null,
                            tint = Color(0xFF34A853)
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                // Bank Balance Input
                OutlinedTextField(
                    value = bankText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                            bankText = newValue
                        }
                    },
                    label = { TranslatedText("Bank Balance *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF4285F4)
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                // Credit Balance Input
                OutlinedTextField(
                    value = creditText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                            creditText = newValue
                        }
                    },
                    label = { TranslatedText("Credit Balance *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFFEA4335)
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                HorizontalDivider()
                
                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { TranslatedText("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Reason Input (Optional)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { TranslatedText("Reason (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onSave(cashAmount, bankAmount, creditAmount, description, reason.takeIf { it.isNotBlank() })
                    }
                },
                enabled = isValid
            ) {
                TranslatedText("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                TranslatedText("Cancel")
            }
        }
    )
}

// Balance Adjustment Dialog - Direct editing (replaces add/subtract)
@Composable
fun BalanceAdjustmentDialog(
    type: String,
    currentBalance: Double,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onAdjust: (Double, String, String?) -> Unit // Now expects newBalance, not difference
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    var balanceText by remember { mutableStateOf(currentBalance.toString()) }
    var description by remember { mutableStateOf(TranslationManager.translate("Manual Balance Edit", isPunjabiEnabled)) }
    var reason by remember { mutableStateOf("") }
    
    val newBalance = balanceText.toDoubleOrNull() ?: 0.0
    val isValid = balanceText.isNotBlank() && newBalance >= 0
    
    val typeTranslated = when(type) {
        "Cash" -> TranslationManager.translate("Cash", isPunjabiEnabled)
        "Bank" -> TranslationManager.translate("Bank", isPunjabiEnabled)
        "Credit" -> TranslationManager.translate("Credit", isPunjabiEnabled)
        else -> type
    }
    val editBalanceText = "${TranslationManager.translate("Edit", isPunjabiEnabled)} $typeTranslated ${TranslationManager.translate("Balance", isPunjabiEnabled)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = editBalanceText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Balance Display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        TranslatedText(
                            englishText = "Current Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                Text(
                            text = currencyFormatter.format(currentBalance),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // New Balance Input (Direct Edit)
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                            balanceText = newValue
                        }
                    },
                    label = { TranslatedText("New Balance *") },
                    placeholder = { TranslatedText("Enter new balance") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                Text(
                            text = "₹",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { TranslatedText("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Reason Input (Optional)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { TranslatedText("Reason (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Show difference if changed
                if (newBalance != currentBalance) {
                    val difference = newBalance - currentBalance
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (difference > 0) 
                                MaterialTheme.colorScheme.secondaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            TranslatedText(
                                englishText = if (difference > 0) "Increase" else "Decrease",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (difference > 0) 
                                    MaterialTheme.colorScheme.onSecondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = currencyFormatter.format(kotlin.math.abs(difference)),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (difference > 0) 
                                    MaterialTheme.colorScheme.onSecondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        // Pass the new balance directly
                        onAdjust(newBalance, description, reason.takeIf { it.isNotBlank() })
                    }
                },
                enabled = isValid
            ) {
                TranslatedText("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                TranslatedText("Cancel")
            }
        }
    )
}

// Transfer Card Component
@Composable
fun TransferCard(
    customers: List<Customer>,
    brokers: List<Broker>,
    selectedFromType: String?,
    onFromTypeSelected: (String?) -> Unit,
    selectedFromCustomer: Customer?,
    onFromCustomerSelected: (Customer?) -> Unit,
    selectedFromBroker: Broker?,
    onFromBrokerSelected: (Broker?) -> Unit,
    selectedToType: String?,
    onToTypeSelected: (String?) -> Unit,
    selectedToCustomer: Customer?,
    onToCustomerSelected: (Customer?) -> Unit,
    selectedToBroker: Broker?,
    onToBrokerSelected: (Broker?) -> Unit,
    availableToOptions: List<String>,
    transferAmount: String,
    onAmountChange: (String) -> Unit,
    transferNote: String,
    onNoteChange: (String) -> Unit,
    transferDate: String,
    onDateChange: (String) -> Unit,
    onTransferClick: () -> Unit,
    onShowAddCustomerDialog: (String) -> Unit,
    onShowAddBrokerDialog: (String) -> Unit,
    addSuccess: Boolean,
    addedCustomerName: String?,
    addedBrokerName: String?,
    onSuccessHandled: () -> Unit,
    isTransferring: Boolean,
    currencyFormatter: NumberFormat,
    cashBalance: Double,
    bankBalance: Double,
    fromBalance: Double,
    isPunjabiEnabled: Boolean
) {
    // Color helpers based on balance
    val getBalanceColor: @Composable (Double) -> Color = { balance ->
        when {
            balance > 0 -> Color(0xFF2E7D32) // Green for positive
            balance < 0 -> Color(0xFFD32F2F) // Red for negative
            else -> MaterialTheme.colorScheme.onSurfaceVariant // Gray for zero
        }
    }

    val getBalanceContainerColor: @Composable (Double) -> Color = { balance ->
        when {
            balance > 0 -> Color(0xFF2E7D32).copy(alpha = 0.15f) // Light green
            balance < 0 -> Color(0xFFD32F2F).copy(alpha = 0.15f) // Light red
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                TranslatedText(
                    englishText = "Transfer Money",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // FROM Section - Compact
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TranslatedText(
                    englishText = "From",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // FROM Type Buttons - LazyRow with natural sizing
                val fromTypes = listOf("Customer", "Broker", "Cash", "Bank")
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(fromTypes) { type ->
                        FilterChip(
                            selected = selectedFromType == type,
                            onClick = { onFromTypeSelected(type) },
                            label = {
                                TranslatedText(
                                    type,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.wrapContentWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                // FROM Customer/Broker Dropdown
                AnimatedVisibility(
                    visible = selectedFromType == "Customer",
                    enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f))
                ) {
            CustomerSearchableDropdown(
                        label = TranslationManager.translate("Select Customer", isPunjabiEnabled),
                customers = customers,
                        selectedCustomer = selectedFromCustomer,
                        onCustomerSelected = onFromCustomerSelected,
                        onShowAddDialog = onShowAddCustomerDialog,
                addSuccess = addSuccess,
                        addedItemName = addedCustomerName,
                onSuccessHandled = onSuccessHandled
            )
                }

                AnimatedVisibility(
                    visible = selectedFromType == "Broker",
                    enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f))
                ) {
                    BrokerSearchableDropdown(
                        label = TranslationManager.translate("Select Broker", isPunjabiEnabled),
                        brokers = brokers,
                        selectedBroker = selectedFromBroker,
                        onBrokerSelected = onFromBrokerSelected,
                        onShowAddDialog = onShowAddBrokerDialog,
                        addSuccess = addSuccess,
                        addedItemName = addedBrokerName,
                        onSuccessHandled = onSuccessHandled
                    )
                }

                // FROM Balance Display - With Red/Green Colors
                AnimatedVisibility(
                    visible = selectedFromType != null,
                    enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f))
                ) {
                    val icon = when (selectedFromType) {
                        "Bank" -> Icons.Default.AccountBalance
                        "Cash" -> Icons.Default.Money
                        else -> Icons.Default.Person
                    }

                    val balanceColor = getBalanceColor(fromBalance)
                    val containerColor = getBalanceContainerColor(fromBalance)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = balanceColor.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
            Icon(
                                imageVector = icon,
                contentDescription = null,
                                tint = balanceColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                TranslatedText(
                                    englishText = "Available Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = (fromBalance).toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = balanceColor
                                    )
                                    // Show indicator icon based on balance
                                    if (fromBalance > 0) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = balanceColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else if (fromBalance < 0) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = balanceColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Compact Arrow Divider
            AnimatedVisibility(
                visible = selectedFromType != null,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = spring(dampingRatio = 0.8f)),
                exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = spring(dampingRatio = 0.8f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // TO Section - Compact
            AnimatedVisibility(
                visible = selectedFromType != null,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f)),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TranslatedText(
                        englishText = "To",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // TO Type Buttons - Fixed alignment
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableToOptions.forEach { option ->
                            val translatedOption = TranslationManager.translate(option, isPunjabiEnabled)
                            FilterChip(
                                selected = selectedToType == option,
                                onClick = { onToTypeSelected(option) },
                                label = {
                                    Text(
                                        translatedOption,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    // TO Customer/Broker Dropdown
                    AnimatedVisibility(
                        visible = selectedToType == "Customer",
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f)),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f))
                    ) {
                        CustomerSearchableDropdown(
                            label = TranslationManager.translate("Select Customer", isPunjabiEnabled),
                            customers = customers,
                            selectedCustomer = selectedToCustomer,
                            onCustomerSelected = onToCustomerSelected,
                            onShowAddDialog = onShowAddCustomerDialog,
                            addSuccess = false,
                            addedItemName = null,
                            onSuccessHandled = {}
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedToType == "Broker",
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f)),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f))
                    ) {
                        BrokerSearchableDropdown(
                            label = TranslationManager.translate("Select Broker", isPunjabiEnabled),
                            brokers = brokers,
                            selectedBroker = selectedToBroker,
                            onBrokerSelected = onToBrokerSelected,
                            onShowAddDialog = onShowAddBrokerDialog,
                            addSuccess = false,
                            addedItemName = null,
                            onSuccessHandled = {}
                        )
                    }
                }
            }

            // Compact Amount Field
            OutlinedTextField(
                value = transferAmount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                        onAmountChange(newValue)
                    }
                },
                label = { TranslatedText("Amount *", style = MaterialTheme.typography.labelMedium) },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                )
            )
            
            // Compact Date Picker
            DatePickerButton(
                selectedDate = transferDate,
                onDateSelected = onDateChange,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Compact Note Field
            OutlinedTextField(
                value = transferNote,
                onValueChange = onNoteChange,
                label = { TranslatedText("Note (Optional)", style = MaterialTheme.typography.labelMedium) },
                placeholder = { TranslatedText("Add note...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 2,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Compact Transfer Button
            val isFormValid = selectedFromType != null &&
                    (selectedFromType == "Cash" || selectedFromType == "Bank" ||
                            (selectedFromType == "Customer" && selectedFromCustomer != null) ||
                            (selectedFromType == "Broker" && selectedFromBroker != null)) &&
                    selectedToType != null &&
                    (selectedToType == "Cash" || selectedToType == "Bank" ||
                            (selectedToType == "Customer" && selectedToCustomer != null) ||
                            (selectedToType == "Broker" && selectedToBroker != null)) &&
                         transferAmount.isNotEmpty() && 
                         transferAmount.toDoubleOrNull()?.let { it > 0 } == true &&
                         !isTransferring

            Button(
                onClick = onTransferClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                if (isTransferring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatedText(
                        "Transferring...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatedText(
                        "Transfer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Balance Display Box Component (matching image design)
@Composable
fun BalanceDisplayBox(
    balance: Double,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Money
) {
    val isNegative = balance < 0
    val amountColor = if (isNegative) Color(0xFFEA4335) else Color(0xFF34A853) // Red if negative, Green if positive
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "Balances:" text in a capsule
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.wrapContentWidth()
        ) {
            val context = LocalContext.current
            val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
                .collectAsState(initial = false)
            
            TranslatedText(
                englishText = "Balances:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontWeight = FontWeight.Medium
            )
        }
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                // Format amount (extract numeric value)
                val formattedBalance = currencyFormatter.format(balance)
                val amountText = formattedBalance.replace(currencyFormatter.currency?.symbol ?: "₹", "").trim()
                
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor // Red if negative, Green if positive
                )
            }
        }
    }
}

// Transaction History Section (shows latest transactions)
@Composable
fun TransactionHistorySection(
    transactions: List<PersonTransaction>,
    currencyFormatter: NumberFormat,
    navController: NavController
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    if (transactions.isEmpty()) {
        return
    }
    
    // Group transactions by date (memoized to prevent recalculation)
    val groupedByDate = remember(transactions) {
        transactions.groupBy { it.date }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                TranslatedText(
                    englishText = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grouped transactions by date (sorted by date descending, then by createdAt descending within each date)
            val sortedDateGroups = remember(groupedByDate) {
                groupedByDate.toList().sortedByDescending { it.first }
            }
            sortedDateGroups.forEach { (date, dateTransactions) ->
                // Sort transactions within the date group by createdAt descending (latest first)
                val sortedTransactions = dateTransactions.sortedByDescending { it.createdAt }
                
                DateGroupHeader(date = date, count = dateTransactions.size)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                sortedTransactions.forEachIndexed { index, transaction ->
                    TransactionCard(
                        transaction = transaction,
                        currencyFormatter = currencyFormatter,
                        navController = navController
                    )
                    if (index < sortedTransactions.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// Date Group Header
@Composable
fun DateGroupHeader(
    date: String,
    count: Int
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()) }
    
    val formattedDate = try {
        val parsedDate = dateFormat.parse(date) ?: Date()
        displayDateFormat.format(parsedDate)
    } catch (e: Exception) {
        date
    }
    
    val transactionText = if (count > 1) {
        "$count ${TranslationManager.translate("transactions", isPunjabiEnabled)}"
    } else {
        "$count ${TranslationManager.translate("transaction", isPunjabiEnabled)}"
    }
    
    Column {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = transactionText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Transaction Card
@Composable
fun TransactionCard(
    transaction: PersonTransaction,
    currencyFormatter: NumberFormat,
    navController: NavController
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateDisplayFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    // Parse date and format
    val dateText = try {
        val parsedDate = dateFormat.parse(transaction.date) ?: Date()
        dateDisplayFormat.format(parsedDate)
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
    val transactionTypeLabel = getTransactionTypeLabel(transaction.type)
    
    // Determine payment method display
    val paymentMethodDisplay = when {
        transaction.paymentMethod == "BANK" -> TranslationManager.translate("Bank", isPunjabiEnabled)
        transaction.paymentMethod == "CASH" -> TranslationManager.translate("Cash", isPunjabiEnabled)
        transaction.paymentMethod == "CREDIT" -> TranslationManager.translate("Credit (Auto)", isPunjabiEnabled)
        transaction.paymentMethod == "MIXED" -> "Mixed"
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Person name and Transaction type badge
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = transaction.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = transaction.personType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Transaction Type Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = amountColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, amountColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = transactionTypeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Date, Time, Amount
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Amount in colored box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = amountColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, amountColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = currencyFormatter.format(transaction.amount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    
                    // Payment method
                    if (paymentMethodDisplay.isNotBlank()) {
                        val paymentText = "${TranslationManager.translate("Payment:", isPunjabiEnabled)} $paymentMethodDisplay"
                        Text(
                            text = paymentText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Right side: Transaction details
                Column(
                    modifier = Modifier.width(120.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    transaction.orderNumber?.let {
                        val orderText = "${TranslationManager.translate("Order #", isPunjabiEnabled)}$it"
                        Text(
                            text = orderText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (transaction.description.isNotBlank()) {
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Notes section (if present)
            if (transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val notesText = "${TranslationManager.translate("Notes:", isPunjabiEnabled)} ${transaction.note}"
                    Text(
                        text = notesText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            navController.navigate("transaction_detail/${transaction.transactionId}")
                        }
                    ) {
                        TranslatedText(
                            englishText = "View Details >",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            navController.navigate("transaction_detail/${transaction.transactionId}")
                        }
                    ) {
                        TranslatedText(
                            englishText = "View Details >",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Add Broker Dialog for Transactions Screen
@Composable
fun AddBrokerDialogForTransactions(
    onDismiss: () -> Unit,
    onAddSuccess: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    
    // Dialog state
    var brokerName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var idProof by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var brokerBill by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var amount by rememberSaveable { mutableStateOf("") }
    
    // Loading and error state
    var isAddingBroker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TranslatedText(
                        englishText = "Add New Broker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = TranslationManager.translate("Close", isPunjabiEnabled),
                            modifier = Modifier.rotate(45f)
                        )
                    }
                }
                
                Divider()
                
                // Form Fields
                OutlinedTextField(
                    value = brokerName,
                    onValueChange = { brokerName = it },
                    label = { TranslatedText("Broker Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAddingBroker
                )
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { TranslatedText("Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isAddingBroker
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { TranslatedText("Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !isAddingBroker
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
                
                HorizontalDivider()
                
                TranslatedText(
                    englishText = "Broker Amount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { TranslatedText("Broker Fee Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isAddingBroker
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isAddingBroker
                    ) {
                        TranslatedText("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (brokerName.isBlank() || phoneNumber.isBlank() || address.isBlank()) {
                                errorMessage = TranslationManager.translate("Please fill in all required fields", isPunjabiEnabled)
                                return@Button
                            }
                            
                            isAddingBroker = true
                            errorMessage = null
                            
                            scope.launch {
                                try {
                                    val broker = Broker(
                                        brokerId = "",
                                        name = brokerName.trim(),
                                        phoneNumber = phoneNumber.trim(),
                                        idProof = idProof,
                                        address = address.trim(),
                                        brokerBill = brokerBill,
                                        amount = amount.toIntOrNull() ?: 0,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    
                                    val result = BrokerRepository.addBroker(broker)
                                    if (result.isSuccess) {
                                        onAddSuccess(brokerName)
                                        onDismiss()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: TranslationManager.translate("Failed to add broker", isPunjabiEnabled)
                                        isAddingBroker = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: TranslationManager.translate("An unexpected error occurred", isPunjabiEnabled)
                                    isAddingBroker = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isAddingBroker && brokerName.isNotBlank() && phoneNumber.isNotBlank() && address.isNotBlank()
                    ) {
                        if (isAddingBroker) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            TranslatedText("Add")
                        }
                    }
                }
            }
        }
    }
}
