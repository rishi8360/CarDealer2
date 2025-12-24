package com.example.cardealer2.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.LedgerViewModel
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.TransactionType
import com.example.cardealer2.utility.ConsistentTopAppBar
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
    
    var showBalanceAdjustDialog by remember { mutableStateOf<String?>(null) }
    var expandedSections by remember { mutableStateOf<Set<String>>(setOf("A")) } // Section A expanded by default
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = "Ledger",
                subtitle = "Complete Financial Overview",
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
                // Section A: Money Balance
                item {
                    SectionA_MoneyBalance(
                        cashBalance = cashBalance ?: 0.0,
                        bankBalance = bankBalance ?: 0.0,
                        creditBalance = creditBalance ?: 0.0,
                        isLoading = isLoadingBalances,
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { type -> showBalanceAdjustDialog = type },
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
                
                // Section B: Who gave me money
                item {
                    SectionB_MoneyReceived(
                        moneyReceivedGroups = moneyReceivedGroups,
                        currencyFormatter = currencyFormatter,
                        isExpanded = expandedSections.contains("B"),
                        onToggleExpand = {
                            expandedSections = if (expandedSections.contains("B")) {
                                expandedSections - "B"
                            } else {
                                expandedSections + "B"
                            }
                        }
                    )
                }
                
                // Section C: Whom did I give money to
                item {
                    SectionC_MoneyPaid(
                        moneyPaidGroups = moneyPaidGroups,
                        currencyFormatter = currencyFormatter,
                        isExpanded = expandedSections.contains("C"),
                        onToggleExpand = {
                            expandedSections = if (expandedSections.contains("C")) {
                                expandedSections - "C"
                            } else {
                                expandedSections + "C"
                            }
                        }
                    )
                }
                
                // Section D: Who owes me money
                item {
                    SectionD_EmiOutstanding(
                        emiOutstanding = emiOutstanding,
                        currencyFormatter = currencyFormatter,
                        isExpanded = expandedSections.contains("D"),
                        onToggleExpand = {
                            expandedSections = if (expandedSections.contains("D")) {
                                expandedSections - "D"
                            } else {
                                expandedSections + "D"
                            }
                        }
                    )
                }
                
                // Section E: Who do I owe money to
                item {
                    SectionE_CreditOwed(
                        creditOwedGroups = creditOwedGroups,
                        currencyFormatter = currencyFormatter,
                        isExpanded = expandedSections.contains("E"),
                        onToggleExpand = {
                            expandedSections = if (expandedSections.contains("E")) {
                                expandedSections - "E"
                            } else {
                                expandedSections + "E"
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Balance Adjustment Dialog
    showBalanceAdjustDialog?.let { type ->
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
            onAdjust = { amount, description, reason ->
                viewModel.adjustCapitalBalance(
                    type = type,
                    amount = amount,
                    description = description,
                    reason = reason,
                    onSuccess = { showBalanceAdjustDialog = null },
                    onError = { /* Handle error */ }
                )
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
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
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
                    Text(
                        text = "💰 A. How much money do I have?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    // Cash Balance Card
                    BalanceCard(
                        title = "Cash Balance",
                        balance = cashBalance,
                        icon = Icons.Default.Money,
                        iconColor = Color(0xFF34A853),
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { onAdjustClick("Cash") }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Bank Balance Card
                    BalanceCard(
                        title = "Bank Balance",
                        balance = bankBalance,
                        icon = Icons.Default.AccountBalance,
                        iconColor = Color(0xFF4285F4),
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { onAdjustClick("Bank") }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Credit Balance Card
                    BalanceCard(
                        title = "Credit Balance",
                        balance = creditBalance,
                        icon = Icons.Default.CreditCard,
                        iconColor = Color(0xFFEA4335),
                        currencyFormatter = currencyFormatter,
                        onAdjustClick = { onAdjustClick("Credit") },
                        description = "Money you owe to suppliers/brokers"
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
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Adjust Balance",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Section B: Who gave me money
@Composable
fun SectionB_MoneyReceived(
    moneyReceivedGroups: List<com.example.cardealer2.ViewModel.MoneyReceivedGroup>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    ExpandableSectionCard(
        title = "🤝 B. Who gave me money?",
        subtitle = "${moneyReceivedGroups.size} customers",
        icon = Icons.Default.TrendingUp,
        iconColor = Color(0xFF34A853),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (moneyReceivedGroups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Person,
                message = "No money received yet"
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
    group: com.example.cardealer2.ViewModel.MoneyReceivedGroup,
    currencyFormatter: NumberFormat
) {
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
    moneyPaidGroups: List<com.example.cardealer2.ViewModel.MoneyPaidGroup>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    ExpandableSectionCard(
        title = "📤 C. Whom did I give money to?",
        subtitle = "${moneyPaidGroups.size} people",
        icon = Icons.Default.ShoppingCart,
        iconColor = Color(0xFFEA4335),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (moneyPaidGroups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ShoppingCart,
                message = "No payments made yet"
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
    group: com.example.cardealer2.ViewModel.MoneyPaidGroup,
    currencyFormatter: NumberFormat
) {
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

// Section D: Who owes me money (EMI Outstanding)
@Composable
fun SectionD_EmiOutstanding(
    emiOutstanding: List<com.example.cardealer2.ViewModel.EmiOutstanding>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val totalOutstanding = emiOutstanding.sumOf { it.outstandingAmount }
    
    ExpandableSectionCard(
        title = "🧾 D. Who owes me money?",
        subtitle = "${emiOutstanding.size} active EMI customers • ${currencyFormatter.format(totalOutstanding)} outstanding",
        icon = Icons.Default.Payment,
        iconColor = Color(0xFFFBBC04),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (emiOutstanding.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Payment,
                message = "No outstanding EMI payments"
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
    emi: com.example.cardealer2.ViewModel.EmiOutstanding,
    currencyFormatter: NumberFormat
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val nextDueDateText = if (emi.nextDueDate > 0) {
        dateFormatter.format(emi.nextDueDate)
    } else {
        "Not set"
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
                    Text(
                        text = "Remaining: ${emi.remainingInstallments} installments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Next due: $nextDueDateText",
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
                    Text(
                        text = "${currencyFormatter.format(emi.installmentAmount)}/installment",
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
    creditOwedGroups: List<com.example.cardealer2.ViewModel.CreditOwed>,
    currencyFormatter: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    ExpandableSectionCard(
        title = "📉 E. Who do I owe money to?",
        subtitle = "${creditOwedGroups.size} suppliers/brokers",
        icon = Icons.Default.CreditCard,
        iconColor = Color(0xFFEA4335),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand
    ) {
        if (creditOwedGroups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.CreditCard,
                message = "No credit purchases"
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
    group: com.example.cardealer2.ViewModel.CreditOwed,
    currencyFormatter: NumberFormat
) {
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
                    Text(
                        text = "${group.transactions.size} purchases",
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
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
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
    return when (type) {
        TransactionType.PURCHASE -> "Purchase"
        TransactionType.SALE -> "Sale"
        TransactionType.EMI_PAYMENT -> "EMI Payment"
        TransactionType.BROKER_FEE -> "Broker Fee"
        else -> type
    }
}

// Balance Adjustment Dialog
@Composable
fun BalanceAdjustmentDialog(
    type: String,
    currentBalance: Double,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onAdjust: (Double, String, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("Manual Adjustment") }
    var reason by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(true) }
    
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val adjustmentAmount = if (isAdding) amount else -amount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adjust $type Balance",
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
                        Text(
                            text = "Current Balance",
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
                
                // Add/Subtract Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isAdding,
                        onClick = { isAdding = true },
                        label = { Text("Add") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isAdding,
                        onClick = { isAdding = false },
                        label = { Text("Subtract") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    placeholder = { Text("Enter amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                Text(
                            text = "₹",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                )
                
                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Reason Input (Optional)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Preview New Balance
                if (amount > 0) {
                    val newBalance = currentBalance + adjustmentAmount
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "New Balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = currencyFormatter.format(newBalance),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (amount > 0) {
                        onAdjust(adjustmentAmount, description, reason.takeIf { it.isNotBlank() })
                    }
                },
                enabled = amount > 0
            ) {
                Text("Adjust")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
