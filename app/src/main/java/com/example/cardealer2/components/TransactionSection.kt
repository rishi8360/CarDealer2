package com.example.cardealer2.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardealer2.data.PersonTransaction
import com.example.cardealer2.data.TransactionType
import com.google.firebase.firestore.DocumentReference
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Ultra-minimalistic expandable transaction section
 */
@Composable
fun TransactionSection(
    personRef: DocumentReference?,
    personName: String,
    onLoadTransactions: suspend (DocumentReference) -> Result<List<PersonTransaction>>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var transactions by remember { mutableStateOf<List<PersonTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load transactions when expanded
    LaunchedEffect(isExpanded, personRef) {
        if (isExpanded && personRef != null && transactions.isEmpty() && !isLoading) {
            isLoading = true
            error = null
            val result = onLoadTransactions(personRef)
            if (result.isSuccess) {
                transactions = result.getOrNull() ?: emptyList()
            } else {
                error = result.exceptionOrNull()?.message ?: "Failed to load transactions"
            }
            isLoading = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Simple header - no card background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    error != null -> {
                        Text(
                            text = error ?: "Error loading",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                    transactions.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            transactions.forEach { transaction ->
                                TransactionItem(transaction = transaction)
                                if (transaction != transactions.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom divider
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

/**
 * Ultra-minimalistic transaction item - plain design
 */
@Composable
fun TransactionItem(
    transaction: PersonTransaction,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val dateInputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val dateText = try {
        val parsedDate = dateInputFormatter.parse(transaction.date) ?: Date()
        dateFormatter.format(parsedDate)
    } catch (e: Exception) {
        transaction.date // Fallback to raw string if parsing fails
    }

    // Simple color coding
    val amountColor = when (transaction.type) {
        TransactionType.SALE, TransactionType.EMI_PAYMENT ->
            MaterialTheme.colorScheme.primary
        else ->
            MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Transaction Details - Left side
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Description
            Text(
                text = transaction.description.ifBlank {
                    getTransactionTypeLabel(transaction.type)
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            // Date & Payment
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                if (transaction.paymentMethod.isNotBlank()) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )

                    Text(
                        text = when (transaction.paymentMethod) {
                            "MIXED" -> "Mixed"
                            "CASH" -> "Cash"
                            "BANK" -> "Bank"
                            "CREDIT" -> "Credit"
                            else -> transaction.paymentMethod
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Order number inline
                if (transaction.orderNumber != null && transaction.orderNumber > 0) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )

                    Text(
                        text = "#${transaction.orderNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Amount - Right side
        val amountSign = when (transaction.type) {
            TransactionType.SALE, TransactionType.EMI_PAYMENT -> "+"
            else -> "-"
        }

        Text(
            text = "$amountSign${currencyFormatter.format(transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = amountColor
        )
    }
}

/**
 * Ultra-minimalistic standalone transaction card
 */
@Composable
fun TransactionCard(
    transaction: PersonTransaction,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateInputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val dateText = try {
        val parsedDate = dateInputFormatter.parse(transaction.date) ?: Date()
        dateFormatter.format(parsedDate)
    } catch (e: Exception) {
        transaction.date // Fallback to raw string if parsing fails
    }
    
    // Since date is only a date string (no time), use createdAt timestamp for time
    val timeText = try {
        timeFormatter.format(Date(transaction.createdAt))
    } catch (e: Exception) {
        ""
    }

    val amountColor = when (transaction.type) {
        TransactionType.SALE, TransactionType.EMI_PAYMENT ->
            MaterialTheme.colorScheme.primary
        else ->
            MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Person name and amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = transaction.personName.ifBlank { "Unknown" },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            val amountSign = when (transaction.type) {
                TransactionType.SALE, TransactionType.EMI_PAYMENT -> "+"
                else -> "-"
            }

            Text(
                text = "$amountSign${currencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = amountColor
            )
        }

        // Transaction type
        Text(
            text = getTransactionTypeLabel(transaction.type),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        // Description (if different)
        if (transaction.description.isNotBlank() &&
            transaction.description != getTransactionTypeLabel(transaction.type)) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 2
            )
        }

        // Meta info
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$dateText · $timeText",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            if (transaction.paymentMethod.isNotBlank()) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                Text(
                    text = when (transaction.paymentMethod) {
                        "MIXED" -> "Mixed"
                        "CASH" -> "Cash"
                        "BANK" -> "Bank"
                        "CREDIT" -> "Credit"
                        else -> transaction.paymentMethod
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (transaction.orderNumber != null && transaction.orderNumber > 0) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                Text(
                    text = "#${transaction.orderNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun getTransactionTypeLabel(type: String): String {
    return when (type) {
        TransactionType.PURCHASE -> "Purchase"
        TransactionType.SALE -> "Sale"
        TransactionType.EMI_PAYMENT -> "EMI Payment"
        TransactionType.BROKER_FEE -> "Broker Fee"
        else -> type
    }
}