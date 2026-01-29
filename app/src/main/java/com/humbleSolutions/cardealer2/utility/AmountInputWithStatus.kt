package com.humbleSolutions.cardealer2.utility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp
import com.humbleSolutions.cardealer2.utils.TranslationManager
import com.humbleSolutions.cardealer2.utils.TranslatedText
import androidx.compose.ui.platform.LocalContext


@Composable
fun AmountInputWithStatus(
    amount: String,
    onAmountChange: (String) -> Unit,
    paymentStatus: String,
    onPaymentStatusChange: (String) -> Unit,
    error: String? = null
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    Column(modifier = Modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { TranslatedText("Amount *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = { Text("₹ ") },
            isError = error != null,
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    // To Receive Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPaymentStatusChange("To Receive") }
                            .background(
                                if (paymentStatus == "To Receive") Color(0xFF2E7D32)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (paymentStatus == "To Receive") Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = TranslationManager.translate("Receive", isPunjabiEnabled),
                            color = if (paymentStatus == "To Receive") Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // To Give Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPaymentStatusChange("To Give") }
                            .background(
                                if (paymentStatus == "To Give") Color(0xFFD32F2F)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (paymentStatus == "To Give") Color(0xFFD32F2F)
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = TranslationManager.translate("Give", isPunjabiEnabled),
                            color = if (paymentStatus == "To Give") Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = when (paymentStatus) {
                    "To Receive" -> Color(0xFF2E7D32)
                    "To Give" -> Color(0xFFD32F2F)
                    else -> MaterialTheme.colorScheme.primary
                },
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Error message below field
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Description Card (optional)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (paymentStatus) {
                    "To Receive" -> Color(0xFFE8F9E9)
                    "To Give" -> Color(0xFFFFE5E5)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = when (paymentStatus) {
                    "To Receive" -> TranslationManager.translate("💰 Amount customer needs to pay you", isPunjabiEnabled)
                    "To Give" -> TranslationManager.translate("💵 Amount you need to pay customer", isPunjabiEnabled)
                    else -> TranslationManager.translate("Select payment type", isPunjabiEnabled)
                },
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = when (paymentStatus) {
                    "To Receive" -> Color(0xFF1B5E20)
                    "To Give" -> Color(0xFFB71C1C)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
