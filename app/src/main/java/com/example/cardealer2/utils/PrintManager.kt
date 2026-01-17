package com.example.cardealer2.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.lang.ref.WeakReference

object PrintManagerUtil {
    
    // Keep a weak reference to WebView to prevent memory leaks
    private var webViewRef: WeakReference<WebView>? = null
    
    /**
     * Show Android Print Dialog for HTML content
     * This opens the system print dialog where user can:
     * - Preview the document
     * - Save as PDF to Downloads
     * - Adjust print settings
     */
    fun printHtml(
        context: Context,
        htmlContent: String,
        jobName: String = "Bill_${System.currentTimeMillis()}"
    ) {
        // Validate HTML content
        if (htmlContent.isBlank()) {
            return
        }
        
        // Get PrintManager
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return
        
        // Create a WebView to render HTML
        val webView = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.loadsImagesAutomatically = true
            settings.domStorageEnabled = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Page finished loading, create print adapter
                    view?.let { wv ->
                        createPrintAdapter(context, wv, jobName, printManager)
                    }
                }
            }
            
            // Load HTML content
            loadDataWithBaseURL(
                null,
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        }
        
        // Keep reference to prevent garbage collection
        webViewRef = WeakReference(webView)
    }
    
    private fun createPrintAdapter(
        context: Context,
        webView: WebView,
        jobName: String,
        printManager: PrintManager
    ) {
        try {
            // Create print document adapter from WebView
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            
            // Configure print attributes for A4 size with proper resolution
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(
                    PrintAttributes.Resolution(
                        "pdf",
                        "pdf",
                        600,
                        600
                    )
                )
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            
            // Show print dialog
            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            android.util.Log.e("PrintManagerUtil", "Error creating print adapter: ${e.message}", e)
        }
    }
}
