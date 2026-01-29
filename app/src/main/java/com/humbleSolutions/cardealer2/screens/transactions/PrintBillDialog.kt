package com.humbleSolutions.cardealer2.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.humbleSolutions.cardealer2.data.PersonTransaction
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import com.humbleSolutions.cardealer2.utils.TransactionBillGenerator
import com.humbleSolutions.cardealer2.utils.PrintManagerUtil
import com.humbleSolutions.cardealer2.utils.pdf.CanvasPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintBillDialog(
    transaction: PersonTransaction,
    relatedData: TransactionBillGenerator.BillRelatedData,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    // Generate invoice number
    val invoiceNumber = remember(transaction.transactionId) {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        "CARDEALER/${transaction.transactionId.take(8)}/$dateStr"
    }
    
    // Form state
    var invoiceDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var buyerName by remember { mutableStateOf(transaction.personName) }
    var buyerAddress by remember { mutableStateOf(relatedData.personDetails?.address ?: "") }
    var buyerGstin by remember { mutableStateOf("") }
    var showWebView by remember { mutableStateOf(false) }
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var isGeneratingHtml by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TranslatedText(
                        englishText = "Generate Bill",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                if (!showWebView) {
                    // Input Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Invoice Number (read-only)
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = {},
                            label = { TranslatedText("Invoice Number") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )
                        
                        // Invoice Date
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = invoiceDate
                        )
                        
                        OutlinedTextField(
                            value = dateFormat.format(Date(invoiceDate)),
                            onValueChange = {},
                            label = { TranslatedText("Invoice Date") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    // Date picker would be shown here
                                    // For now, using current date
                                }) {
                                    Text("📅")
                                }
                            }
                        )
                        
                        // Buyer Name (required)
                        OutlinedTextField(
                            value = buyerName,
                            onValueChange = { buyerName = it },
                            label = { TranslatedText("Buyer Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Buyer Address (optional)
                        OutlinedTextField(
                            value = buyerAddress,
                            onValueChange = { buyerAddress = it },
                            label = { TranslatedText("Buyer Address") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                        
                        // Buyer GSTIN (optional)
                        OutlinedTextField(
                            value = buyerGstin,
                            onValueChange = { buyerGstin = it },
                            label = { TranslatedText("Buyer GSTIN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    // Generate Bill Button
                    Button(
                        onClick = {
                            if (buyerName.isNotBlank()) {
                                isGeneratingHtml = true
                                scope.launch {
                                    try {
                                        // Generate PDF using Canvas (no HTML)
                                        val pdfResult = withContext(Dispatchers.Default) {
                                            CanvasPdfGenerator.generatePdfFromData(
                                                context = context,
                                                transaction = transaction,
                                                relatedData = relatedData,
                                                invoiceNumber = invoiceNumber,
                                                invoiceDate = invoiceDate,
                                                buyerName = buyerName,
                                                buyerAddress = buyerAddress,
                                                buyerGstin = buyerGstin,
                                                fileName = invoiceNumber.replace("/", "_") // Sanitize filename
                                            )
                                        }
                                        
                                        isGeneratingHtml = false
                                        
                                        pdfResult.fold(
                                            onSuccess = { pdfFile ->
                                                // Share PDF immediately
                                                CanvasPdfGenerator.sharePdf(
                                                    context = context,
                                                    pdfFile = pdfFile,
                                                    subject = "Invoice $invoiceNumber"
                                                )
                                                // Close dialog after sharing
                                                onDismiss()
                                            },
                                            onFailure = { error ->
                                                // Log error
                                                android.util.Log.e("PrintBillDialog", "PDF generation failed: ${error.message}", error)
                                                isGeneratingHtml = false
                                                // Error state - could show snackbar here
                                            }
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e("PrintBillDialog", "Error generating PDF: ${e.message}", e)
                                        isGeneratingHtml = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = buyerName.isNotBlank() && !isGeneratingHtml,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGeneratingHtml) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        TranslatedText(
                            englishText = if (isGeneratingHtml) "Generating PDF..." else "Generate & Share PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // WebView Preview
                    BillPreviewWebView(
                        htmlContent = htmlContent,
                        onBack = { showWebView = false },
                        onPrint = {
                            // Show Android Print Dialog
                            htmlContent?.let { html ->
                                val jobName = "Bill_${invoiceNumber}_${System.currentTimeMillis()}"
                                PrintManagerUtil.printHtml(context, html, jobName)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BillPreviewWebView(
    htmlContent: String?,
    onBack: () -> Unit,
    onPrint: () -> Unit
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                TranslatedText("Back")
            }
            
            Button(
                onClick = onPrint,
                modifier = Modifier.weight(1f)
            ) {
                TranslatedText("Print / Save PDF")
            }
        }
        
        Divider()
        
        // WebView Preview
        if (htmlContent != null) {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        loadDataWithBaseURL(
                            null,
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
