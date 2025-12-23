package com.example.cardealer2.utility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChassisNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    onCheckClick: () -> Unit,
    isChecking: Boolean,
    validationResult: ChassisValidationState?,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    label: String = "Chassis Number"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text("Enter chassis number") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Show validation status icon
                    when (validationResult) {
                        is ChassisValidationState.Available -> {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Available",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        is ChassisValidationState.AlreadyExists -> {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Already exists",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        else -> {}
                    }

                    // Check button
                    IconButton(
                        onClick = onCheckClick,
                        enabled = value.isNotBlank() && !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Check chassis number",
                                tint = if (value.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            },
            isError = errorMessage != null || validationResult is ChassisValidationState.AlreadyExists,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (value.isNotBlank()) onCheckClick() }
            ),
            colors = OutlinedTextFieldDefaults.colors()
        )

        // Status message
        when (validationResult) {
            is ChassisValidationState.Available -> {
                Text(
                    text = "✓ Chassis number is available",
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            is ChassisValidationState.AlreadyExists -> {
                Text(
                    text = "✗ Chassis number already exists",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            is ChassisValidationState.Error -> {
                Text(
                    text = validationResult.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            else -> {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// 2. Validation State
// ============================================

sealed class ChassisValidationState {
    object Idle : ChassisValidationState()
    object Checking : ChassisValidationState()
    object Available : ChassisValidationState()
    object AlreadyExists : ChassisValidationState()
    data class Error(val message: String) : ChassisValidationState()
}