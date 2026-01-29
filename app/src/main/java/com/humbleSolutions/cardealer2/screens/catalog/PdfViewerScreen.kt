package com.humbleSolutions.cardealer2.screens.catalog

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.humbleSolutions.cardealer2.utility.ConsistentTopAppBar
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfFilePath: String,
    navController: NavController,
    catalogId: String? = null
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val file = remember(pdfFilePath) { File(pdfFilePath) }
    val fileExists = remember(file) { file.exists() && file.length() > 0 }
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = TranslationManager.translate("Catalog Generated", isPunjabiEnabled),
                subtitle = if (fileExists) 
                    TranslationManager.translate("PDF is ready to share", isPunjabiEnabled) 
                else 
                    TranslationManager.translate("PDF generation completed", isPunjabiEnabled),
                navController = navController,
                actions = {
                    if (fileExists) {
                        IconButton(
                            onClick = { shareCatalogLinkViaWhatsApp(context, catalogId) }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = TranslationManager.translate("Share PDF", isPunjabiEnabled),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = if (fileExists) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(
                        if (fileExists) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = if (fileExists) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TranslatedText(
                    englishText = if (fileExists) "Catalog Generated Successfully!" else "PDF Generation Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TranslatedText(
                    englishText = if (fileExists) 
                        "Your catalog PDF is ready to share. Tap the share button above to share it."
                    else 
                        "The PDF file could not be generated. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (fileExists) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { shareCatalogLinkViaWhatsApp(context, catalogId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TranslatedText("Share PDF")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val fileText = "${TranslationManager.translate("File:", isPunjabiEnabled)} ${file.name}"
                    Text(
                        text = fileText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

fun shareCatalogLinkViaWhatsApp(context: android.content.Context, catalogId: String?) {
    try {
        // Build the catalog URL with ID
        val baseUrl = "https://smartattend.me/CarDealer/?id="
        val catalogUrl = if (!catalogId.isNullOrBlank()) {
            "$baseUrl$catalogId"
        } else {
            "${baseUrl}e2H81AOv3EmeHea6IRON"  // Fallback to default ID if not provided
        }
        
        // Create message text with the link
        val messageText = "Check out our vehicle catalog: $catalogUrl"
        
        // Create intent to share via WhatsApp
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, messageText)
            // Try to open WhatsApp directly
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Check if WhatsApp is installed, otherwise show chooser
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp not installed, show chooser with all apps
            val chooserIntent = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                },
                "Share catalog link via"
            )
            context.startActivity(chooserIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun sharePdf(context: android.content.Context, pdfFilePath: String) {
    val file = File(pdfFilePath)
    if (!file.exists() || file.length() == 0L) {
        return
    }
    
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Vehicle Catalog PDF")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Catalog PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
