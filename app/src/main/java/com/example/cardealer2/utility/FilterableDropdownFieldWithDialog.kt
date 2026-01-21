package com.example.cardealer2.utility

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterableDropdownFieldWithDialog(
    label: String,
    items: List<T>,
    selectedItem: T? = null,
    onItemSelected: (T) -> Unit,
    onShowAddDialog: (String) -> Unit,
    itemToString: (T) -> String,
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
            hasAutoSelected = false // Reset flag for new item
            showSuccessDialog = true
            expanded = false
            onExpandedChange?.invoke(false)
            
            // Show success for 0.5 seconds, then update search text and notify
            kotlinx.coroutines.delay(500)
            showSuccessDialog = false
            searchText = addedItemName
            onSuccessHandled()
        }
    }

    // Auto-select the newly added item when it appears in the items list
    LaunchedEffect(items, addedItemName, lastAddedItemName, selectedItem) {
        if (addedItemName != null && lastAddedItemName == addedItemName && !hasAutoSelected) {
            // Find the item in the list that matches the added name
            val matchingItem = items.find { itemToString(it).equals(addedItemName, ignoreCase = true) }
            if (matchingItem != null) {
                // Check if it's already selected to avoid unnecessary re-selection
                val isAlreadySelected = selectedItem?.let { 
                    itemToString(it).equals(addedItemName, ignoreCase = true) 
                } ?: false
                
                if (!isAlreadySelected) {
                    // Small delay to ensure UI is ready
                    kotlinx.coroutines.delay(100)
                    onItemSelected(matchingItem)
                    searchText = itemToString(matchingItem)
                }
                // Mark as done regardless of whether we selected or it was already selected
                hasAutoSelected = true
            }
        }
    }

    // Create interaction source to detect clicks (but not drags/scrolls)
    val interactionSource = remember { MutableInteractionSource() }

    // Track press and release to detect actual clicks vs scrolls
    LaunchedEffect(interactionSource) {
        var pressOffset: Offset? = null

        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    // Store the press position
                    pressOffset = interaction.pressPosition
                }
                is PressInteraction.Release -> {
                    // Only open dropdown if press and release are close (not a scroll)
                    pressOffset?.let { startOffset ->
                        val distance = kotlin.math.sqrt(
                            (interaction.press.pressPosition.x - startOffset.x).let { it * it } +
                                    (interaction.press.pressPosition.y - startOffset.y).let { it * it }
                        )
                        // If movement is less than 10 pixels, it's a click not a scroll
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

    // Update search text when selected item changes
    LaunchedEffect(selectedItem) {
        if (selectedItem != null && searchText.isEmpty()) {
            searchText = itemToString(selectedItem)
        }
    }

    val filteredItems = remember(items, searchText) {
        if (searchText.isBlank()) items
        else items.filter { itemToString(it).contains(searchText, ignoreCase = true) }
    }

    val exactMatch = remember(items, searchText) {
        items.any { itemToString(it).equals(searchText, ignoreCase = true) }
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
                    selectedItem?.let { itemToString(it) } ?: "Select or type $label",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            ),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            expanded = false
                            onExpandedChange?.invoke(false)
                        }) {
                            Icon(
                                Icons.Default.Clear, "Clear",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = {
                        if (!expanded) {
                            // When opening dropdown, clear search to show all items
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
            val totalItems = filteredItems.size + if (showAddButton) 1 else 0
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
                    if (filteredItems.isNotEmpty()) {
                        items(filteredItems) { item ->
                            DropdownItem(
                                text = itemToString(item),
                                isSelected = selectedItem?.let {
                                    itemToString(it) == itemToString(item)
                                } ?: false,
                                onClick = {
                                    onItemSelected(item)
                                    searchText = itemToString(item)
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

                    if (searchText.isEmpty() && filteredItems.isEmpty()) {
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
                    "Item Added!",
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

