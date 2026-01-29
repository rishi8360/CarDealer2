package com.humbleSolutions.cardealer2.utils

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

/**
 * Composable that automatically translates text based on user preference
 * 
 * Usage:
 * ```
 * TranslatedText(
 *     englishText = "Car Dealer",
 *     style = MaterialTheme.typography.headlineMedium
 * )
 * ```
 */
@Composable
fun TranslatedText(
    englishText: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: androidx.compose.ui.graphics.Color? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
) {
    val context = LocalContext.current
    val isPunjabiEnabled by TranslationManager.isPunjabiEnabled(context)
        .collectAsState(initial = false)
    
    val translatedText = remember(englishText, isPunjabiEnabled) {
        TranslationManager.translate(englishText, isPunjabiEnabled)
    }
    
    Text(
        text = translatedText,
        modifier = modifier,
        style = style ?: LocalTextStyle.current,
        color = color ?: LocalTextStyle.current.color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}

