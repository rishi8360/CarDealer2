package com.humbleSolutions.cardealer2.utils

import android.content.Context
import com.humbleSolutions.cardealer2.data.AppPreferences
import kotlinx.coroutines.flow.Flow

object TranslationManager {
    /**
     * Get translated text based on user preference
     * @param context Android context
     * @param englishText The English text to translate
     * @param isPunjabiEnabled Whether Punjabi translation is enabled
     * @return Translated text if enabled and available, otherwise original text
     */
    fun translate(
        englishText: String,
        isPunjabiEnabled: Boolean
    ): String {
        return if (isPunjabiEnabled) {
            TranslationDictionary.translate(englishText)
        } else {
            englishText
        }
    }
    
    /**
     * Flow that emits current translation preference
     */
    fun isPunjabiEnabled(context: Context): Flow<Boolean> {
        return AppPreferences.isPunjabiEnabled(context)
    }
    
    /**
     * Set translation preference
     */
    suspend fun setPunjabiEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setPunjabiEnabled(context, enabled)
    }
}

