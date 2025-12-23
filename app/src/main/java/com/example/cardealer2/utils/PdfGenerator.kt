package com.example.cardealer2.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.math.ceil

object PdfGenerator {

    suspend fun generatePdfFromHtml(
        context: Context,
        htmlContent: String,
        fileName: String = "catalog_${System.currentTimeMillis()}"
    ): Result<File> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->

            // Validate HTML content
            if (htmlContent.isBlank()) {
                continuation.resume(Result.failure(Exception("HTML content is empty")))
                return@suspendCancellableCoroutine
            }

            android.util.Log.d("PdfGenerator", "Starting PDF generation. HTML length: ${htmlContent.length}")
            android.util.Log.d("PdfGenerator", "HTML preview (first 500 chars): ${htmlContent.take(500)}")

            // Create a parent container for WebView to ensure proper rendering
            val container = FrameLayout(context.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                visibility = View.INVISIBLE  // Hide container but keep WebView renderable
            }

            val webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true  // ✅ Enable JavaScript for evaluateJavascript()
                settings.loadWithOverviewMode = false
                settings.useWideViewPort = false
                settings.blockNetworkImage = false
                settings.loadsImagesAutomatically = true
                settings.setSupportZoom(false)
                settings.allowFileAccess = true  // ✅ Allow file access
                settings.allowContentAccess = true  // ✅ Allow content access
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW  // ✅ Allow mixed content
                // Use software rendering for off-screen rendering
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                // Make WebView invisible but functional
                visibility = View.INVISIBLE  // ✅ INVISIBLE (still renders) vs GONE (doesn't render)

                // Set initial scale to ensure 1:1 pixel mapping (595px = 595px)

                // Set layout params - ensure exact width match
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            // Attach WebView to container for proper rendering
            container.addView(webView)

            // Force container to be measured and laid out (even though invisible)
            container.measure(
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST)
            )
            container.layout(0, 0, 2000, 2000)

            var hasResumed = false
            var imageLoadAttempts = 0
            val maxImageLoadAttempts = 5  // Increased attempts
            val imageLoadTimeout = 10000L // 10 seconds max wait

            // Add timeout to prevent infinite hanging
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!hasResumed) {
                    android.util.Log.e("PdfGenerator", "PDF generation timeout! Proceeding anyway...")
                    generatePdfPages(webView, context, fileName, continuation) { result ->
                        if (!hasResumed) {
                            hasResumed = true
                            continuation.resume(result)
                        }
                    }
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 30000L) // 30 second timeout

            fun resumeOnce(result: Result<File>) {
                if (!hasResumed) {
                    hasResumed = true
                    timeoutHandler.removeCallbacks(timeoutRunnable) // Cancel timeout
                    continuation.resume(result)
                }
            }

            fun checkImagesAndProceed(view: WebView) {
                imageLoadAttempts++

                // Check if images are loaded
                view.evaluateJavascript(
                    "(function() { " +
                            "var imgs = document.getElementsByTagName('img'); " +
                            "var total = imgs.length; " +
                            "var loaded = 0; " +
                            "var failed = 0; " +
                            "for(var i = 0; i < total; i++) { " +
                            "  if(imgs[i].complete) { " +
                            "    if(imgs[i].naturalHeight > 0) loaded++; " +
                            "    else if(imgs[i].naturalWidth === 0 && imgs[i].naturalHeight === 0) failed++; " +
                            "  } " +
                            "} " +
                            "return loaded + '/' + total + '/' + failed; " +
                            "})();"
                ) { result ->
                    android.util.Log.d("PdfGenerator", "Image check attempt $imageLoadAttempts: $result")

                    try {
                        val parts = result?.removeSurrounding("\"")?.split("/") ?: listOf("0", "0", "0")
                        val loaded = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val total = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val failed = parts.getOrNull(2)?.toIntOrNull() ?: 0

                        android.util.Log.d("PdfGenerator", "Images: loaded=$loaded, total=$total, failed=$failed")

                        // If all images loaded (or no images), proceed
                        // Also proceed if max attempts reached (don't wait forever)
                        if (total == 0 || loaded == total || imageLoadAttempts >= maxImageLoadAttempts) {
                            if (failed > 0) {
                                android.util.Log.w("PdfGenerator", "Some images failed to load ($failed), but proceeding with PDF generation")
                            }

                            android.util.Log.d("PdfGenerator", "All images loaded or max attempts reached. Proceeding to PDF generation...")

                            // Additional delay for final rendering - use Handler to avoid nested posts
                            Handler(Looper.getMainLooper()).postDelayed({
                                android.util.Log.d("PdfGenerator", "Calling generatePdfPages now...")
                                generatePdfPages(view, context, fileName, continuation, ::resumeOnce)
                            }, 500L) // Reduced delay for faster generation
                        } else {
                            // More images to load, wait and retry
                            val delay = if (imageLoadAttempts < 3) 2000L else 1500L
                            android.util.Log.d("PdfGenerator", "Images still loading ($loaded/$total). Retrying in ${delay}ms...")
                            Handler(Looper.getMainLooper()).postDelayed({
                                checkImagesAndProceed(view)
                            }, delay)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PdfGenerator", "Error parsing image load result: ${e.message}")
                        // On error, proceed anyway after delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            generatePdfPages(view, context, fileName, continuation, ::resumeOnce)
                        }, 1000L)
                    }
                }
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    android.util.Log.d("PdfGenerator", "Page finished loading: $url")

                    // Give WebView time to fully render content and start loading images
                    Handler(Looper.getMainLooper()).postDelayed({
                        val webViewToUse = view ?: webView

                        // First check if content has height
                        webViewToUse.evaluateJavascript(
                            "(function() { " +
                                    "var height = Math.max(" +
                                    "  document.body.scrollHeight || 0, " +
                                    "  document.body.offsetHeight || 0, " +
                                    "  document.documentElement.scrollHeight || 0, " +
                                    "  document.documentElement.offsetHeight || 0, " +
                                    "  document.documentElement.clientHeight || 0" +
                                    "); " +
                                    "return height; " +
                                    "})();"
                        ) { heightResult ->
                            val contentHeightFromJs = try {
                                heightResult?.removeSurrounding("\"")?.toIntOrNull() ?: 0
                            } catch (e: Exception) {
                                0
                            }
                            android.util.Log.d("PdfGenerator", "Initial content height from JS: $contentHeightFromJs")

                            // Check images and proceed
                            checkImagesAndProceed(webViewToUse)
                        }
                    }, 1000L) // Increased initial delay for page render
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    android.util.Log.w("PdfGenerator", "WebView error (non-critical): $description at $failingUrl")
                    // Don't fail PDF generation on image errors - images might fail but content should still render
                    // Only fail on critical page load errors
                    if (failingUrl != null && !failingUrl.contains("http") && !failingUrl.contains("https")) {
                        // Main page error, not image error
                        android.util.Log.e("PdfGenerator", "Critical page load error")
                        webView.destroy()
                        resumeOnce(Result.failure(Exception("WebView page load error: $description")))
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?
                ) {
                    val url = request?.url?.toString() ?: "unknown"
                    android.util.Log.w("PdfGenerator", "HTTP error loading resource: $url (${errorResponse?.statusCode})")
                    // Don't fail on HTTP errors - images might 404 but PDF should still generate with text content
                }
            }

            // Use https:// as base URL to ensure Firebase HTTPS URLs load properly
            // This helps WebView understand it can load external HTTPS resources
            webView.loadDataWithBaseURL(
                "https://",  // Base URL helps WebView load HTTPS images from Firebase
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )

            continuation.invokeOnCancellation {
                hasResumed = true
                container.removeView(webView)
                webView.destroy()
            }
        }
    }

    private fun generatePdfPages(
        webView: WebView,
        context: Context,
        fileName: String,
        continuation: kotlinx.coroutines.CancellableContinuation<Result<File>>,
        resumeOnce: (Result<File>) -> Unit
    ) {
        try {
            val file = File(context.cacheDir, "$fileName.pdf")
            file.parentFile?.mkdirs()

            // A4 size in points (595 x 842)
            val pageWidth = 595
            val pageHeight = 842

            // Ensure WebView has proper layout params and is measured correctly
            // Convert points to pixels (assuming 72 DPI: 1 point = 1 pixel)
            val widthPixels = pageWidth
            val heightPixels = pageHeight

            // Measure WebView with A4 width - use a large height initially
            val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                widthPixels,
                View.MeasureSpec.EXACTLY
            )
            val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED
            )

            // Force layout before measuring - ensure WebView is properly attached
            webView.requestLayout()
            webView.invalidate()  // Force redraw

            android.util.Log.d("PdfGenerator", "Starting PDF page generation...")

            // Simplified flow - avoid nested posts that might hang
            Handler(Looper.getMainLooper()).postDelayed({
                android.util.Log.d("PdfGenerator", "Measuring WebView...")

                // Ensure WebView width is exactly 595px (A4 width in points)
                // Measure WebView directly on main thread with exact width
                webView.measure(widthMeasureSpec, heightMeasureSpec)

                android.util.Log.d("PdfGenerator", "WebView measured dimensions: ${webView.measuredWidth}x${webView.measuredHeight}")

                // Get content height - use measured height or try to get from JS
                var contentHeight = webView.measuredHeight
                android.util.Log.d("PdfGenerator", "Initial measured height: $contentHeight")

                // If measured height is 0, use JavaScript height from earlier (we already got it)
                if (contentHeight <= 0) {
                    android.util.Log.w("PdfGenerator", "Measured height is 0, using JavaScript height or minimum")
                    // Try to get from document as fallback
                    webView.evaluateJavascript(
                        "(function() { return Math.max(document.body.scrollHeight || 0, document.documentElement.scrollHeight || 0, document.body.offsetHeight || 0, document.documentElement.offsetHeight || 0); })();"
                    ) { jsHeight ->
                        val jsHeightValue = try {
                            jsHeight?.removeSurrounding("\"")?.toIntOrNull() ?: 0
                        } catch (e: Exception) {
                            0
                        }
                        val finalHeight = jsHeightValue.takeIf { it > 0 } ?: pageHeight
                        android.util.Log.d("PdfGenerator", "Final content height: $finalHeight")

                        // Layout and proceed - ensure WebView starts at origin
                        webView.layout(0, 0, widthPixels, finalHeight.coerceAtLeast(pageHeight))
                        webView.scrollTo(0, 0)  // Reset scroll position

                        // Wait for layout to complete using ViewTreeObserver
                        val observer = webView.viewTreeObserver
                        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                webView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                                android.util.Log.d("PdfGenerator", "Layout complete (fallback path). layoutRequested=${webView.isLayoutRequested}")

                                Handler(Looper.getMainLooper()).postDelayed({
                                    proceedWithPdfGeneration(webView, finalHeight, pageWidth, pageHeight, file, resumeOnce)
                                }, 200L)
                            }
                        })

                        // Fallback timeout
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                webView.viewTreeObserver.removeOnGlobalLayoutListener(observer as? ViewTreeObserver.OnGlobalLayoutListener ?: return@postDelayed)
                            } catch (e: Exception) {}
                            proceedWithPdfGeneration(webView, finalHeight, pageWidth, pageHeight, file, resumeOnce)
                        }, 1000L)
                    }
                } else {
                    // Layout the webView with measured dimensions - ensure it starts at origin
                    webView.layout(0, 0, widthPixels, contentHeight.coerceAtLeast(pageHeight))
                    webView.scrollTo(0, 0)  // Reset scroll position to top
                    android.util.Log.d("PdfGenerator", "WebView measured - Width: $widthPixels, Height: $contentHeight")

                    // Force layout pass and wait for it to complete
                    var layoutListenerAdded = false
                    val observer = webView.viewTreeObserver
                    val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (!layoutListenerAdded) return
                            layoutListenerAdded = false

                            try {
                                webView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            } catch (e: Exception) {
                                // Already removed
                            }

                            android.util.Log.d("PdfGenerator", "Layout complete. layoutRequested=${webView.isLayoutRequested}")

                            // Proceed after layout is complete
                            Handler(Looper.getMainLooper()).postDelayed({
                                proceedWithPdfGeneration(webView, contentHeight, pageWidth, pageHeight, file, resumeOnce)
                            }, 200L)
                        }
                    }

                    layoutListenerAdded = true
                    observer.addOnGlobalLayoutListener(layoutListener)

                    // Force a layout pass
                    webView.requestLayout()

                    // Fallback timeout in case layout listener doesn't fire
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (layoutListenerAdded) {
                            layoutListenerAdded = false
                            try {
                                webView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                            } catch (e: Exception) {}

                            android.util.Log.w("PdfGenerator", "Fallback timeout: Proceeding with PDF generation (layoutRequested=${webView.isLayoutRequested})")
                            proceedWithPdfGeneration(webView, contentHeight, pageWidth, pageHeight, file, resumeOnce)
                        }
                    }, 1500L)
                }
            }, 300L) // Reduced delay

        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Failed to generate PDF: ${e.message}", e)
            webView.destroy()
            resumeOnce(Result.failure(e))
        }
    }

    private fun proceedWithPdfGeneration(
        webView: WebView,
        contentHeight: Int,
        pageWidth: Int,
        pageHeight: Int,
        file: File,
        resumeOnce: (Result<File>) -> Unit
    ) {
        try {
            // Ensure minimum height
            val actualContentHeight = contentHeight.coerceAtLeast(pageHeight)

            // Create PDF document
            val pdfDocument = PdfDocument()

            // Calculate number of pages
            val numberOfPages = ceil(actualContentHeight.toFloat() / pageHeight.toFloat()).toInt().coerceAtLeast(1)

            // Limit pages to prevent ANR (increased for larger catalogs)
            val maxPages = 1000
            val actualPages = numberOfPages.coerceAtMost(maxPages)

            android.util.Log.d("PdfGenerator", "Generating $actualPages pages for content height: $actualContentHeight")

            // Generate each page
            for (i in 0 until actualPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    i + 1
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Fill background with white
                canvas.drawColor(android.graphics.Color.WHITE)

                // Save canvas state
                canvas.save()

                // Calculate which portion of content this page should show
                val pageTop = i * pageHeight
                val pageBottom = ((i + 1) * pageHeight).coerceAtMost(actualContentHeight)

                // Clip to page bounds first (in original coordinate system)
                canvas.clipRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())

                // Then translate canvas to position content correctly
                // Move content up by the amount already shown on previous pages
                val translateY = -pageTop.toFloat()
                canvas.translate(0f, translateY)

                // Draw WebView content
                try {
                    // Force layout to complete if still requested by measuring and laying out again
                    if (webView.isLayoutRequested) {
                        android.util.Log.w("PdfGenerator", "Page $i: Layout still requested, forcing synchronous layout...")
                        // Force synchronous measure and layout
                        webView.measure(
                            View.MeasureSpec.makeMeasureSpec(webView.width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(webView.height, View.MeasureSpec.EXACTLY)
                        )
                        webView.layout(webView.left, webView.top, webView.right, webView.bottom)
                        // Clear the layout requested flag
                        webView.requestLayout()
                    }

                    // Draw the WebView - try even if layoutRequested (sometimes it works)
                    if (webView.width > 0 && webView.height > 0) {
                        // Draw the WebView (already using software rendering)
                        webView.draw(canvas)

                        android.util.Log.d("PdfGenerator", "Successfully drew page $i (showing content from $pageTop to $pageBottom, width=${webView.width}, height=${webView.height})")
                    } else {
                        android.util.Log.w("PdfGenerator", "WebView not ready for page $i: width=${webView.width}, height=${webView.height}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfGenerator", "Error drawing page $i: ${e.message}", e)
                }

                // Restore canvas
                canvas.restore()

                pdfDocument.finishPage(page)
            }

            // Write to file
            try {
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                android.util.Log.d("PdfGenerator", "PDF generated successfully: ${file.absolutePath}, Size: ${file.length()} bytes")
                webView.destroy()
                resumeOnce(Result.success(file))
            } catch (e: Exception) {
                android.util.Log.e("PdfGenerator", "Failed to write PDF: ${e.message}", e)
                pdfDocument.close()
                webView.destroy()
                resumeOnce(Result.failure(e))
            }

        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Failed to generate PDF: ${e.message}", e)
            webView.destroy()
            resumeOnce(Result.failure(e))
        }
    }
}