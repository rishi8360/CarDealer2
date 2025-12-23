package com.example.cardealer2.screens.catalog

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
import com.example.cardealer2.utility.ConsistentTopAppBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfFilePath: String,
    navController: NavController
) {
    val context = LocalContext.current
    val file = remember(pdfFilePath) { File(pdfFilePath) }
    val fileExists = remember(file) { file.exists() && file.length() > 0 }
    
    Scaffold(
        topBar = {
            ConsistentTopAppBar(
                title = "Catalog Generated",
                subtitle = if (fileExists) "PDF is ready to share" else "PDF generation completed",
                navController = navController,
                actions = {
                    if (fileExists) {
                        IconButton(
                            onClick = { sharePdf(context, pdfFilePath) }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share PDF",
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
                
                Text(
                    text = if (fileExists) "Catalog Generated Successfully!" else "PDF Generation Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (fileExists) 
                        "Your catalog PDF is ready to share. Tap the share button above to share it."
                    else 
                        "The PDF file could not be generated. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (fileExists) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { sharePdf(context, pdfFilePath) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share PDF")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "File: ${file.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
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
