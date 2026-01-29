package com.humbleSolutions.cardealer2.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPickerField(
    label: String,
    pdfUrls: List<String>,
    onPdfChange: (List<String>) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pdfToDelete by remember { mutableStateOf<String?>(null) }

    // PDF picker launcher (local URIs)
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Convert URI to string form (you can later upload these and replace with download URLs)
            val newUris = uris.map { it.toString() }
            onPdfChange(pdfUrls + newUris)
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = "${pdfUrls.size} PDF(s) selected",
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            isError = errorMessage != null,
            trailingIcon = {
                IconButton(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        if (pdfUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Selected PDFs (${pdfUrls.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier
                        .fillMaxWidth()
                    .heightIn(max = 300.dp) // restrict height

            ) {
                items(pdfUrls) { pdfUrl ->
                    PdfListItem(
                        pdfUrl = pdfUrl,
                        onDeleteClick = {
                            pdfToDelete = pdfUrl
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && pdfToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pdfToDelete = null
            },
            icon = {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove PDF?") },
            text = { Text("Are you sure you want to remove this PDF file?") },
            confirmButton = {
                Button(
                    onClick = {
                        pdfToDelete?.let { url ->
                            onPdfChange(pdfUrls.filter { it != url })
                        }
                        showDeleteDialog = false
                        pdfToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    pdfToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PdfListItem(
    pdfUrl: String,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = { /* maybe open preview */ }),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pdfUrl.substringAfterLast('/') ?: "Unnamed file",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete PDF",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
