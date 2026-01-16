package com.example.cardealer2.utility

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cardealer2.data.Customer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSearchableDropdown(
    label: String,
    customers: List<Customer>,
    selectedCustomer: Customer? = null,
    onCustomerSelected: (Customer?) -> Unit,
    onShowAddDialog: (String) -> Unit,
    modifier: Modifier = Modifier,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    addSuccess: Boolean = false,
    addedItemName: String? = null,
    onSuccessHandled: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Track success dialog state
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastAddedItemName by remember { mutableStateOf<String?>(null) }
    var hasAutoSelected by remember { mutableStateOf(false) }

    // Watch for success and show success dialog
    LaunchedEffect(addSuccess, addedItemName) {
        if (addSuccess && addedItemName != null && lastAddedItemName != addedItemName) {
            lastAddedItemName = addedItemName
            hasAutoSelected = false
            showSuccessDialog = true
            expanded = false
            onExpandedChange?.invoke(false)
            
            kotlinx.coroutines.delay(500)
            showSuccessDialog = false
            searchText = addedItemName
            onSuccessHandled()
        }
    }

    // Clear searchText when selectedCustomer becomes null (e.g., after successful transfer)
    LaunchedEffect(selectedCustomer) {
        if (selectedCustomer == null) {
            searchText = ""
            expanded = false
            onExpandedChange?.invoke(false)
        }
    }

    // Auto-select the newly added customer when it appears in the list
    LaunchedEffect(customers, addedItemName, lastAddedItemName, selectedCustomer) {
        if (addedItemName != null && lastAddedItemName == addedItemName && !hasAutoSelected) {
            val matchingCustomer = customers.find { 
                it.name.equals(addedItemName, ignoreCase = true) ||
                it.phone.equals(addedItemName, ignoreCase = true) ||
                it.customerId.equals(addedItemName, ignoreCase = true)
            }
            if (matchingCustomer != null) {
                val isAlreadySelected = selectedCustomer?.let { 
                    it.customerId == matchingCustomer.customerId
                } ?: false
                
                if (!isAlreadySelected) {
                    kotlinx.coroutines.delay(100)
                    onCustomerSelected(matchingCustomer)
                    searchText = matchingCustomer.name
                }
                hasAutoSelected = true
            }
        }
    }

    // Create interaction source to detect clicks (but not drags/scrolls)
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    // Track press and release to detect actual clicks vs scrolls
    LaunchedEffect(interactionSource) {
        var pressOffset: Offset? = null

        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressOffset = interaction.pressPosition
                }
                is PressInteraction.Release -> {
                    pressOffset?.let { startOffset ->
                        val distance = kotlin.math.sqrt(
                            (interaction.press.pressPosition.x - startOffset.x).let { it * it } +
                                    (interaction.press.pressPosition.y - startOffset.y).let { it * it }
                        )
                        if (distance < 10f && !expanded) {
                            expanded = true
                            onExpandedChange?.invoke(true)
                        }
                    }
                    pressOffset = null
                }
                is PressInteraction.Cancel -> {
                    pressOffset = null
                }
            }
        }
    }

    // Update search text when selected customer changes
    LaunchedEffect(selectedCustomer) {
        if (selectedCustomer != null && searchText.isEmpty()) {
            searchText = selectedCustomer.name
        }
    }

    // Search across all customer fields but filter customers
    val filteredCustomers = remember(customers, searchText) {
        if (searchText.isBlank()) {
            customers
        } else {
            customers.filter { customer ->
                customer.name.contains(searchText, ignoreCase = true) ||
                customer.phone.contains(searchText, ignoreCase = true) ||
                customer.address.contains(searchText, ignoreCase = true) ||
                customer.idProofNumber.contains(searchText, ignoreCase = true) ||
                customer.idProofType.contains(searchText, ignoreCase = true) ||
                customer.customerId.contains(searchText, ignoreCase = true)
            }
        }
    }

    val exactMatch = remember(customers, searchText) {
        customers.any { customer ->
            customer.name.equals(searchText, ignoreCase = true) ||
            customer.phone.equals(searchText, ignoreCase = true) ||
            customer.customerId.equals(searchText, ignoreCase = true)
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                if (!expanded) {
                    expanded = true
                    onExpandedChange?.invoke(true)
                }
            },
            label = { Text(label) },
            placeholder = {
                Text(
                    selectedCustomer?.name ?: "Select or type $label",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCustomer != null) {
                        IconButton(onClick = {
                            onCustomerSelected(null)
                            searchText = ""
                            expanded = false
                            onExpandedChange?.invoke(false)
                        }) {
                            Icon(
                                Icons.Default.Clear,
                                "Deselect",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (searchText.isNotEmpty() && selectedCustomer == null) {
                        IconButton(onClick = {
                            searchText = ""
                            expanded = false
                            onExpandedChange?.invoke(false)
                        }) {
                            Icon(
                                Icons.Default.Clear,
                                "Clear",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = {
                        if (!expanded) {
                            searchText = ""
                        }
                        expanded = !expanded
                        onExpandedChange?.invoke(expanded)
                    }) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(if (expanded) 180f else 0f),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            interactionSource = interactionSource
        )

        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically { -it / 2 } + fadeIn() + expandVertically(),
            exit = slideOutVertically { -it / 2 } + fadeOut() + shrinkVertically()
        ) {
            val showAddButton = searchText.isNotEmpty() && !exactMatch
            val totalItems = filteredCustomers.size + if (showAddButton) 1 else 0
            val height = 56.dp * minOf(totalItems, 3) + 16.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
                    if (filteredCustomers.isNotEmpty()) {
                        items(filteredCustomers) { customer ->
                            DropdownItem(
                                text = customer.name, // Only show name in UI
                                isSelected = selectedCustomer?.customerId == customer.customerId,
                                onClick = {
                                    onCustomerSelected(customer)
                                    searchText = customer.name
                                    expanded = false
                                    onExpandedChange?.invoke(false)
                                }
                            )
                        }
                    }

                    if (showAddButton) {
                        item {
                            AddNewButton(
                                text = searchText,
                                onClick = {
                                    onShowAddDialog(searchText)
                                }
                            )
                        }
                    }

                    if (searchText.isEmpty() && filteredCustomers.isEmpty()) {
                        item { EmptyState(label) }
                    }
                }
            }
        }
    }

    // Success dialog
    if (showSuccessDialog && addedItemName != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFF4CAF50)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp).size(32.dp),
                        tint = Color.White
                    )
                }
            },
            title = {
                Text(
                    "Customer Added!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "\"$addedItemName\" has been successfully added.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = { },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DropdownItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = onClick),
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "✓",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun AddNewButton(text: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Add \"$text\"",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun EmptyState(label: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📋", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "No $label available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

