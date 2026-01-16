package com.example.cardealer2.utility

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable date picker button component
 * Shows a button with the selected date and opens a date picker dialog when clicked
 * 
 * @param selectedDate The selected date in "yyyy-MM-dd" format
 * @param onDateSelected Callback when date is selected
 * @param label Optional label text (defaults to showing the formatted date)
 * @param modifier Modifier for the button
 * @param defaultToToday If true, defaults to today's date if selectedDate is blank
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    defaultToToday: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }
    
    // Initialize with today's date if blank and defaultToToday is true
    LaunchedEffect(Unit) {
        if (selectedDate.isBlank() && defaultToToday) {
            val todayDate = dateFormat.format(calendar.time)
            onDateSelected(todayDate)
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                if (selectedDate.isNotBlank()) {
                    dateFormat.parse(selectedDate)?.time ?: calendar.timeInMillis
                } else {
                    calendar.timeInMillis
                }
            } catch (e: Exception) {
                calendar.timeInMillis
            }
        )
        
        Dialog(
            onDismissRequest = { showDatePicker = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Date Picker
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    calendar.timeInMillis = millis
                                    val formattedDate = dateFormat.format(calendar.time)
                                    onDateSelected(formattedDate)
                                    showDatePicker = false
                                }
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
    
    // Date button
    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Outlined.Event,
            contentDescription = "Select Date",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label ?: try {
                if (selectedDate.isNotBlank()) {
                    displayDateFormat.format(dateFormat.parse(selectedDate) ?: Date())
                } else {
                    displayDateFormat.format(Date())
                }
            } catch (e: Exception) {
                if (selectedDate.isNotBlank()) selectedDate else displayDateFormat.format(Date())
            }
        )
    }
}

