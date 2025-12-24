package com.example.cardealer2.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cardealer2.ViewModel.PaymentsViewModel
import com.example.cardealer2.utility.ConsistentTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiDueScreen(
    navController: NavController,
    viewModel: PaymentsViewModel = viewModel()
) {
    val emiSchedule by viewModel.emiSchedule.collectAsState()
    val overdue by viewModel.overdueEmi.collectAsState()
    val upcoming by viewModel.upcomingEmi.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val tabs = listOf("Upcoming", "Overdue", "All")
    var selectedTab by remember { mutableStateOf(0) }
    val listToShow = remember(selectedTab, emiSchedule, overdue, upcoming) {
        when (selectedTab) {
            0 -> upcoming
            1 -> overdue
            else -> emiSchedule
        }
    }

    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = "EMI Schedule",
                subtitle = "Track dues & installments",
                navController = navController
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
                // Modern Tab Design
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = when {
                        isLoading -> "loading"
                        error != null -> "error"
                        listToShow.isEmpty() -> "empty"
                        else -> "content"
                    },
                    transitionSpec = {
                        fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                    },
                    label = "content_animation"
                ) { state ->
                    when (state) {
                        "loading" -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        "Loading EMI schedules...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        "error" -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.padding(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "⚠️",
                                            style = MaterialTheme.typography.displaySmall
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = error ?: "Unknown error",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                        "empty" -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Event,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = when (selectedTab) {
                                            0 -> "No upcoming EMIs"
                                            1 -> "No overdue EMIs"
                                            else -> "No EMI purchases found"
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (selectedTab) {
                                            0 -> "All payments are up to date"
                                            1 -> "Great! No overdue payments"
                                            else -> "Start by adding EMI purchases"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        else -> {
                            EmiDueList(
                                schedule = listToShow,
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

@Composable
private fun EmiDueList(
    schedule: List<PaymentsViewModel.SaleWithDetails>,
    onRecordPayment: (String, Double, Double) -> Unit
) {
    var selectedSale by remember { mutableStateOf<PaymentsViewModel.SaleWithDetails?>(null) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var selectedSaleForDialog by remember { mutableStateOf<PaymentsViewModel.SaleWithDetails?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val todayStart = remember {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(schedule) { detail ->
            val emi = detail.sale.emiDetails
            val dueDate = emi?.nextDueDate ?: 0L
            val isOverdue = dueDate in 1 until todayStart
            val isDueToday = dueDate == todayStart
            val dueText = if (dueDate > 0) dateFormatter.format(Date(dueDate)) else "Not set"
            val amountText = emi?.installmentAmount ?: 0.0

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Column {
                    // Header with gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = when {
                                        isOverdue -> listOf(
                                            MaterialTheme.colorScheme.errorContainer,
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        )
                                        isDueToday -> listOf(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        )
                                        else -> listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = detail.customer?.customerName ?: "Unknown Customer",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                            isDueToday -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                    if (detail.customer != null) {
                                        IconButton(
                                            onClick = {
                                                selectedSaleForDialog = detail
                                                showCustomerDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = "View Customer Info",
                                                tint = when {
                                                    isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                                    isDueToday -> MaterialTheme.colorScheme.onSecondaryContainer
                                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detail.vehicle?.productId ?: "Unknown Vehicle",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = when {
                                        isOverdue -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        isDueToday -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    }
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isOverdue -> MaterialTheme.colorScheme.error
                                    isDueToday -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ) {
                                Text(
                                    text = when {
                                        isOverdue -> "OVERDUE"
                                        isDueToday -> "TODAY"
                                        else -> "UPCOMING"
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Content
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Due Date and Amount Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Due Date Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Next Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = dueText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // EMI Amount Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AttachMoney,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "EMI Amount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "₹${String.format(Locale.getDefault(), "%.2f", amountText)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Remaining Installments",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${emi?.remainingInstallments ?: 0}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "left",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { selectedSale = detail },
                                enabled = emi?.remainingInstallments ?: 0 > 0,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Payment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Record Payment",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val saleForPayment = selectedSale
    if (saleForPayment != null) {
        SplitPaymentDialog(
            sale = saleForPayment.sale,
            onPaymentRecorded = { cash, bank ->
                onRecordPayment(saleForPayment.sale.saleId, cash, bank)
                selectedSale = null
            },
            onDismiss = {
                selectedSale = null
            }
        )
    }

    if (showCustomerDialog && selectedSaleForDialog != null) {
        CustomerInfoDialog(
            saleWithDetails = selectedSaleForDialog,
            onDismiss = { showCustomerDialog = false }
        )
    }
}

private fun openPdf(context: android.content.Context, pdfUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(pdfUrl), "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback: try to open with browser
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            context.startActivity(browserIntent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}



private fun openWhatsApp(context: android.content.Context, phoneNumber: String, message: String) {
    try {
        // Clean phone number - remove spaces, dashes, and ensure it starts with country code
        val cleanPhone = phoneNumber.replace(Regex("[\\s\\-\\(\\)]"), "")
        val phoneWithCode = if (cleanPhone.startsWith("+")) cleanPhone else "+91$cleanPhone" // Default to India +91 if no country code
        
        // Encode message for URL
        val encodedMessage = Uri.encode(message)
        
        // Create WhatsApp intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://wa.me/$phoneWithCode?text=$encodedMessage".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        // Check if WhatsApp is available
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: open in browser
            val browserIntent = Intent(Intent.ACTION_VIEW,
                "https://wa.me/$phoneWithCode?text=$encodedMessage".toUri())
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerInfoDialog(
    saleWithDetails: PaymentsViewModel.SaleWithDetails?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val customer = saleWithDetails?.customer
    val sale = saleWithDetails?.sale
    val vehicle = saleWithDetails?.vehicle
    val emi = sale?.emiDetails
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                if (customer != null) {
                    // Profile Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = customer.customerName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Contact Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Contact Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Divider()

                            // Phone
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Call,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Phone Number",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = customer.phoneNumber,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                // WhatsApp Button
                                if (customer.phoneNumber.isNotBlank() && emi != null && emi.remainingInstallments > 0) {
                                    IconButton(
                                        onClick = {
                                            val dueDateText = if (emi.nextDueDate > 0) {
                                                dateFormatter.format(Date(emi.nextDueDate))
                                            } else {
                                                "Not set"
                                            }
                                            val totalRemaining = emi.remainingInstallments * emi.installmentAmount
                                            val message = """
                                                Hello ${customer.customerName},
                                                
                                                This is a reminder about your EMI payment:
                                                
                                                Vehicle: ${vehicle?.productId ?: "N/A"}
                                                Next Due Date: $dueDateText
                                                EMI Amount: ₹${String.format(Locale.getDefault(), "%.2f", emi.installmentAmount)}
                                                Remaining Installments: ${emi.remainingInstallments}
                                                Total Amount Remaining: ₹${String.format(Locale.getDefault(), "%.2f", totalRemaining)}
                                                
                                                Please make the payment at your earliest convenience.
                                                
                                                Thank you!
                                            """.trimIndent()
                                            openWhatsApp(context, customer.phoneNumber, message)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Message,
                                            contentDescription = "Send WhatsApp Message",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Address
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Address",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = customer.address.ifBlank { "No address provided" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // ID Proof Card
                    if (customer.idProofImageUrls.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Identity Proof",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Divider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = customer.idProofType.ifBlank { "Not specified" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = customer.idProofNumber.ifBlank { "Not provided" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Text(
                                    text = "Documents",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    customer.idProofImageUrls.forEachIndexed { index, url ->
                                        PdfDocumentCard(
                                            title = "ID Proof Document ${index + 1}",
                                            pdfUrl = url,
                                            onClick = {
                                                openPdf(context, url)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No customer data available",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}