package com.humbleSolutions.cardealer2.utils

import com.humbleSolutions.cardealer2.data.Product
import java.util.Locale

object CatalogHtmlTemplate {

    data class BrandModelProducts(
        val brandId: String,
        val brandLogo: String,
        val models: Map<String, List<Product>>
    )

    fun generateCatalogHtml(
        brandModelProducts: List<BrandModelProducts>,
        dealerName: String = "Car Dealer"
    ): String {
        val brandSections = brandModelProducts.joinToString("\n") { brandData ->
            generateBrandSection(brandData)
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vehicle Catalog</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        /* A4 Paper: 210mm × 297mm = 595px × 842px at 72 DPI */
        @page {
            size: A4;
            margin: 15mm; /* Standard margins */
        }
        
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            overflow-x: hidden;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: #fafafa;
            color: #1a1a1a;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
            line-height: 1.4;
        }
        
        .page-container {
            width: 100%;
            max-width: 1200px;
            padding: 20px;
            margin: 0 auto;
            background: #fafafa;
        }
        
        /* PDF/Print-specific optimizations */
        @media print {
            html, body {
                width: 210mm;
                height: 297mm;
            }
            
            body {
                background: white;
            }
            
            .page-container {
                width: 100%;
                max-width: 100%;
                padding: 15mm;
                margin: 0;
                background: white;
            }
            
            /* Remove page margins as we handle them in container */
            @page {
                margin: 0;
            }
        }
        
        /* Brand Header - Compact and clear */
        .brand-header {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12px;
            margin-bottom: 16px;
            padding: 12px 16px;
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
            page-break-inside: avoid;
            break-inside: avoid;
        }
        
        .brand-logo-container {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            background: #f5f5f5;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            flex-shrink: 0;
        }
        
        .brand-logo {
            width: 100%;
            height: 100%;
            object-fit: contain;
        }
        
        .brand-logo-placeholder {
            font-size: 14px;
            font-weight: 700;
            color: #999;
        }
        
        .brand-info {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
        }
        
        .brand-name {
            font-size: 20px;
            font-weight: 700;
            color: #1a1a1a;
            margin-bottom: 2px;
            letter-spacing: -0.3px;
            line-height: 1.2;
        }
        
        .brand-count {
            font-size: 12px;
            color: #666;
            font-weight: 500;
        }
        
        @media print {
            .brand-header {
                margin-bottom: 12mm;
                padding: 3mm 4mm;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
            }
            
            .brand-logo-container {
                width: 30px;
                height: 30px;
            }
            
            .brand-name {
                font-size: 18px;
            }
            
            .brand-count {
                font-size: 11px;
            }
        }
        
        /* Vehicle Cards List - Optimized spacing */
        .vehicles-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
            width: 100%;
        }
        
        @media print {
            .vehicles-list {
                gap: 3mm; /* 3mm gap between cards */
            }
        }
        
        /* Vehicle Card - Optimized for A4 */
        .vehicle-card {
            background: white;
            border-radius: 10px;
            padding: 12px;
            display: flex;
            flex-direction: row;
            gap: 12px;
            align-items: flex-start;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
            page-break-inside: avoid;
            break-inside: avoid;
            width: 100%;
            border: 1px solid #e8e8e8;
        }
        
        @media screen {
            .vehicle-card {
                padding: 14px;
                gap: 14px;
                transition: transform 0.2s ease, box-shadow 0.2s ease;
            }
            
            .vehicle-card:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
            }
        }
        
        @media print {
            .vehicle-card {
                padding: 3mm;
                gap: 3mm;
                border-radius: 2mm;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                border: 0.5pt solid #e0e0e0;
            }
        }
        
        /* Vehicle Image Container - Compact size */
        .vehicle-image-container {
            width: 85px;
            min-width: 85px;
            height: 85px;
            border-radius: 8px;
            background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
            overflow: hidden;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            position: relative;
        }
        
        @media print {
            .vehicle-image-container {
                width: 22mm;
                min-width: 22mm;
                height: 22mm;
                border-radius: 2mm;
            }
        }
        
        .vehicle-image-container::after {
            content: '';
            position: absolute;
            inset: 0;
            border-radius: 8px;
            border: 1px solid rgba(0, 0, 0, 0.06);
        }
        
        .vehicle-image {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }
        
        .no-image-icon {
            font-size: 40px;
            color: #ccc;
            opacity: 0.5;
        }
        
        @media print {
            .no-image-icon {
                font-size: 32px;
            }
        }
        
        /* Vehicle Details - Optimized layout */
        .vehicle-details {
            flex: 1;
            min-width: 0;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }
        
        @media print {
            .vehicle-details {
                gap: 1.5mm;
            }
        }
        
        /* Vehicle Name - Clear and readable */
        .vehicle-name {
            font-size: 16px;
            font-weight: 700;
            color: #1a1a1a;
            line-height: 1.3;
            word-wrap: break-word;
            word-break: break-word;
            overflow-wrap: break-word;
            margin-bottom: 2px;
        }
        
        @media print {
            .vehicle-name {
                font-size: 13pt; /* 13pt = ~17.3px, clear for PDF */
                line-height: 1.25;
                margin-bottom: 1mm;
            }
        }
        
        /* Chips - Compact badges */
        .vehicle-chips {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
            align-items: center;
        }
        
        @media print {
            .vehicle-chips {
                gap: 1.5mm;
            }
        }
        
        .chip {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
            line-height: 1.2;
            white-space: nowrap;
        }
        
        @media print {
            .chip {
                padding: 1mm 2mm;
                font-size: 9pt; /* Clear text size for PDF */
                border-radius: 3mm;
                gap: 1mm;
            }
        }
        
        .chip-color {
            background: #e3f2fd;
            color: #1565c0;
            border: 1px solid rgba(25, 118, 210, 0.3);
        }
        
        .chip-year {
            background: #f3e5f5;
            color: #6a1b9a;
            border: 1px solid rgba(123, 31, 162, 0.3);
        }
        
        .chip-icon {
            font-size: 12px;
            line-height: 1;
        }
        
        @media print {
            .chip-icon {
                font-size: 10pt;
            }
        }
        
        /* Condition Text - Clear readability */
        .vehicle-condition {
            font-size: 13px;
            color: #555;
            font-weight: 500;
            line-height: 1.4;
            margin-top: 2px;
            word-break: break-word;
        }
        
        @media print {
            .vehicle-condition {
                font-size: 10pt; /* 10pt = ~13.3px, very readable */
                line-height: 1.4;
                margin-top: 1mm;
            }
        }
        
        .condition-value {
            color: #2e7d32;
            font-weight: 600;
        }
        
        /* Extra Info - Compact metadata */
        .vehicle-extra-info {
            margin-top: 6px;
            padding-top: 6px;
            border-top: 1px solid #f0f0f0;
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            font-size: 11px;
            color: #777;
        }
        
        @media print {
            .vehicle-extra-info {
                margin-top: 1.5mm;
                padding-top: 1.5mm;
                gap: 2.5mm;
                font-size: 9pt;
                border-top: 0.5pt solid #e8e8e8;
            }
        }
        
        .info-item {
            display: flex;
            align-items: center;
            gap: 3px;
        }
        
        .info-label {
            font-weight: 500;
            color: #666;
        }
        
        .info-value {
            font-weight: 600;
            color: #333;
        }
        
        /* Responsive adjustments for screen */
        @media screen and (max-width: 768px) {
            .page-container {
                padding: 16px;
            }
            
            .brand-header {
                padding: 12px;
                gap: 10px;
            }
            
            .vehicle-card {
                padding: 12px;
                gap: 12px;
            }
            
            .vehicle-image-container {
                width: 80px;
                min-width: 80px;
                height: 80px;
            }
            
            .vehicle-name {
                font-size: 15px;
            }
            
            .chip {
                font-size: 11px;
                padding: 4px 8px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .page-container {
                padding: 12px;
            }
            
            .vehicle-image-container {
                width: 70px;
                min-width: 70px;
                height: 70px;
            }
            
            .vehicle-name {
                font-size: 14px;
            }
        }
        
        /* Page Break Control for Multi-page PDFs */
        .brand-section {
            page-break-inside: avoid;
            break-inside: avoid;
        }
        
        .brand-section:not(:first-child) {
            page-break-before: always;
            break-before: page;
        }
        
        /* Ensure proper pagination */
        .page-wrapper {
            page-break-inside: avoid;
            break-inside: avoid;
        }
        
        .page-wrapper:not(:last-child) {
            page-break-after: always;
            break-after: page;
        }
        
        /* Print color accuracy */
        @media print {
            * {
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
                color-adjust: exact !important;
            }
        }
    </style>
</head>
<body>
    <div class="page-container">
        $brandSections
    </div>
</body>
</html>
""".trimIndent()
    }

    private fun generateBrandSection(brandData: BrandModelProducts): String {
        if (brandData.models.isEmpty()) return ""

        // Collect all products from all models under this brand
        val allProducts = brandData.models.values.flatten()
        val totalVehicles = allProducts.size

        // Group products into pages of max 3 cards each for PDF
        val productPages = allProducts.chunked(3)

        val productPagesHtml = productPages.joinToString("\n") { pageProducts ->
            val cards = pageProducts.joinToString("\n") { product ->
                generateVehicleCard(product)
            }
            """
            <div class="page-wrapper">
                <div class="vehicles-list">
                    $cards
                </div>
            </div>
            """.trimIndent()
        }

        val logoHtml = if (brandData.brandLogo.isNotEmpty()) {
            """<img src="${brandData.brandLogo}" alt="${brandData.brandId}" class="brand-logo" />"""
        } else {
            """<div class="brand-logo-placeholder">${brandData.brandId.take(2).uppercase(Locale.getDefault())}</div>"""
        }

        return """
        <div class="brand-section">
            <div class="brand-header">
                <div class="brand-logo-container">
                    $logoHtml
                </div>
                <div class="brand-info">
                    <div class="brand-name">${brandData.brandId}</div>
                    <div class="brand-count">$totalVehicles vehicle${if (totalVehicles != 1) "s" else ""} available</div>
                </div>
            </div>
            
            $productPagesHtml
        </div>
        """.trimIndent()
    }

    private fun generateVehicleCard(product: Product): String {
        // Product display name - use productId or model name
        val vehicleName = if (product.productId.isNotEmpty()) {
            product.productId
        } else if (product.chassisNumber.isNotEmpty()) {
            product.chassisNumber
        } else {
            "Vehicle"
        }

        // Image handling
        val imageHtml = if (product.images.isNotEmpty()) {
            val imageUrl = product.images.first()
            """<img src="$imageUrl" alt="$vehicleName" class="vehicle-image" />"""
        } else {
            """<span class="no-image-icon">🚗</span>"""
        }

        // Format values
        val year = if (product.year > 0) product.year.toString() else null
        val color = if (product.colour.isNotEmpty()) {
            product.colour.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            null
        }
        val condition = if (product.condition.isNotEmpty()) {
            product.condition.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            "N/A"
        }

        // Color chip
        val colorChip = if (color != null) {
            """<span class="chip chip-color"><span class="chip-icon">🎨</span><span>$color</span></span>"""
        } else {
            ""
        }

        // Year chip
        val yearChip = if (year != null) {
            """<span class="chip chip-year"><span class="chip-icon">📅</span><span>$year</span></span>"""
        } else {
            ""
        }

        return """
        <div class="vehicle-card">
            <div class="vehicle-image-container">
                $imageHtml
            </div>
            <div class="vehicle-details">
                <div class="vehicle-name">$vehicleName</div>
                <div class="vehicle-chips">
                    $colorChip
                    $yearChip
                </div>
                <div class="vehicle-condition">Condition: $condition</div>
            </div>
        </div>
        """.trimIndent()
    }
}