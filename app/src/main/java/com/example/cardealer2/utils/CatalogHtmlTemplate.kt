package com.example.cardealer2.utils

import com.example.cardealer2.data.Product
import java.text.SimpleDateFormat
import java.util.Date
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
        
        @page {
            size: A4;
            margin: 0;
        }
        
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            max-width: 100%;
            overflow-x: hidden;
            overflow-y: visible;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: #fafafa;
            color: #1a1a1a;
            width: 100%;
            max-width: 100%;
            padding: 0;
            margin: 0;
            overflow-x: hidden;
            overflow-y: visible;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }
        
        .page-container {
            width: 100%;
            max-width: 1200px;
            padding: 16px;
            min-height: 100vh;
            box-sizing: border-box;
            margin: 0 auto;
            background: #fafafa;
        }
        
        @media screen and (max-width: 768px) {
            .page-container {
                padding: 12px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .page-container {
                padding: 8px;
            }
        }
        
        /* PDF-specific styles (for print/PDF generation) */
        @media print {
            html, body {
                width: 595px !important;
                max-width: 595px !important;
            }
            
            body {
                width: 595px !important;
                max-width: 595px !important;
            }
            
            .page-container {
                width: 595px !important;
                max-width: 595px !important;
                padding: 12px;
                min-height: 842px;
            }
        }
        
        /* Brand Header - Enhanced with logo support */
        .brand-header {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            margin-bottom: 20px;
            padding: 14px;
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        }
        
        .brand-logo-container {
            width: 40px;
            height: 40px;
            border-radius: 10px;
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
            font-size: 16px;
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
            font-size: 22px;
            font-weight: 700;
            color: #1a1a1a;
            margin-bottom: 4px;
            letter-spacing: -0.5px;
            line-height: 1.2;
        }
        
        .brand-count {
            font-size: 13px;
            color: #666;
            font-weight: 500;
        }
        
        @media screen and (max-width: 768px) {
            .brand-header {
                padding: 12px;
                gap: 8px;
                margin-bottom: 16px;
            }
            
            .brand-logo-container {
                width: 36px;
                height: 36px;
            }
            
            .brand-name {
                font-size: 18px;
            }
            
            .brand-count {
                font-size: 12px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .brand-header {
                padding: 10px;
                flex-direction: row;
            }
            
            .brand-logo-container {
                width: 32px;
                height: 32px;
            }
            
            .brand-name {
                font-size: 16px;
            }
            
            .brand-count {
                font-size: 11px;
            }
        }
        
        @media print {
            .brand-header {
                margin-bottom: 12px;
                padding: 10px;
                background: white;
            }
            
            .brand-logo-container {
                width: 32px;
                height: 32px;
            }
            
            .brand-name {
                font-size: 16px;
            }
            
            .brand-count {
                font-size: 10px;
            }
        }
        
        /* Vehicle Cards List */
        .vehicles-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
            width: 100%;
            box-sizing: border-box;
        }
        
        @media print {
            .vehicles-list {
                gap: 10px;
            }
        }
        
        /* Vehicle Card - Enhanced design */
        .vehicle-card {
            background: white;
            border-radius: 16px;
            padding: 16px;
            display: flex;
            flex-direction: row;
            gap: 16px;
            align-items: center;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            page-break-inside: avoid;
            break-inside: avoid;
            width: 100%;
            box-sizing: border-box;
            transition: transform 0.2s ease, box-shadow 0.2s ease;
            border: 1px solid #f0f0f0;
        }
        
        @media screen and (max-width: 768px) {
            .vehicle-card {
                padding: 14px;
                gap: 14px;
                border-radius: 14px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .vehicle-card {
                padding: 12px;
                gap: 12px;
                border-radius: 12px;
            }
        }
        
        @media screen {
            .vehicle-card:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
            }
        }
        
        .vehicle-image-container {
            width: 110px;
            min-width: 110px;
            height: 110px;
            border-radius: 12px;
            background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
            overflow: hidden;
            display: flex;
            align-items: center;
            justify-content: center;
            box-sizing: border-box;
            flex-shrink: 0;
            position: relative;
        }
        
        @media screen and (max-width: 768px) {
            .vehicle-image-container {
                width: 100px;
                min-width: 100px;
                height: 100px;
                border-radius: 10px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .vehicle-image-container {
                width: 90px;
                min-width: 90px;
                height: 90px;
                border-radius: 10px;
            }
        }
        
        .vehicle-image-container::after {
            content: '';
            position: absolute;
            inset: 0;
            border-radius: 12px;
            padding: 1px;
            background: linear-gradient(135deg, rgba(0,0,0,0.05) 0%, rgba(0,0,0,0.02) 100%);
            -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
            -webkit-mask-composite: xor;
            mask-composite: exclude;
        }
        
        .vehicle-image {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }
        
        .no-image-icon {
            font-size: 50px;
            color: #ccc;
            opacity: 0.6;
        }
        
        @media screen and (max-width: 768px) {
            .no-image-icon {
                font-size: 45px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .no-image-icon {
                font-size: 40px;
            }
        }
        
        .vehicle-details {
            flex: 1;
            min-width: 0;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        
        .vehicle-name {
            font-size: 18px;
            font-weight: 700;
            color: #1a1a1a;
            line-height: 1.3;
            word-wrap: break-word;
            word-break: break-word;
            overflow-wrap: break-word;
            margin-bottom: 4px;
        }
        
        @media screen and (max-width: 768px) {
            .vehicle-name {
                font-size: 17px;
                line-height: 1.3;
            }
        }
        
        @media screen and (max-width: 480px) {
            .vehicle-name {
                font-size: 16px;
                line-height: 1.3;
                margin-bottom: 4px;
            }
        }
        
        .vehicle-chips {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
        }
        
        @media screen and (max-width: 480px) {
            .vehicle-chips {
                gap: 6px;
            }
        }
        
        .chip {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            line-height: 1.3;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
            white-space: nowrap;
        }
        
        @media screen and (max-width: 768px) {
            .chip {
                padding: 5px 10px;
                font-size: 12px;
                gap: 4px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .chip {
                padding: 4px 9px;
                font-size: 12px;
                gap: 4px;
            }
        }
        
        .chip-color {
            background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
            color: #1565c0;
            border: 1px solid rgba(25, 118, 210, 0.2);
        }
        
        .chip-year {
            background: linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%);
            color: #6a1b9a;
            border: 1px solid rgba(123, 31, 162, 0.2);
        }
        
        .chip-icon {
            font-size: 14px;
            line-height: 1;
            filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.1));
        }
        
        @media screen and (max-width: 768px) {
            .chip-icon {
                font-size: 13px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .chip-icon {
                font-size: 13px;
            }
        }
        
        .vehicle-condition {
            font-size: 14px;
            color: #555;
            font-weight: 500;
            line-height: 1.5;
            margin-top: 4px;
            word-break: break-word;
        }
        
        @media screen and (max-width: 768px) {
            .vehicle-condition {
                font-size: 13px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .vehicle-condition {
                font-size: 13px;
                margin-top: 4px;
            }
        }
        
        .condition-value {
            color: #2e7d32;
            font-weight: 600;
        }
        
        .vehicle-extra-info {
            margin-top: 10px;
            padding-top: 10px;
            border-top: 1px solid #f0f0f0;
            display: flex;
            flex-wrap: wrap;
            gap: 14px;
            font-size: 12px;
            color: #777;
        }
        
        @media screen and (max-width: 768px) {
            .vehicle-extra-info {
                margin-top: 8px;
                padding-top: 8px;
                gap: 12px;
                font-size: 11px;
            }
        }
        
        @media screen and (max-width: 480px) {
            .vehicle-extra-info {
                margin-top: 8px;
                padding-top: 8px;
                gap: 10px;
                font-size: 11px;
            }
        }
        
        .info-item {
            display: flex;
            align-items: center;
            gap: 4px;
        }
        
        .info-label {
            font-weight: 500;
            color: #666;
        }
        
        .info-value {
            font-weight: 600;
            color: #333;
        }
        
        @media print {
            .vehicle-card {
                padding: 12px;
                gap: 12px;
                border-radius: 10px;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
            }
            
            .vehicle-image-container {
                width: 80px;
                min-width: 80px;
                height: 80px;
                border-radius: 8px;
            }
            
            .vehicle-name {
                font-size: 14px;
            }
            
            .chip {
                padding: 4px 8px;
                font-size: 10px;
                gap: 3px;
                box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
            }
            
            .chip-icon {
                font-size: 11px;
            }
            
            .vehicle-condition {
                font-size: 11px;
            }
            
            .vehicle-extra-info {
                margin-top: 8px;
                padding-top: 8px;
                gap: 10px;
                font-size: 10px;
            }
        }
        
        /* PDF Pagination - Max 3 cards per page */
        .page-wrapper {
            min-height: auto;
        }
        
        .page-wrapper:not(:last-child) {
            page-break-after: always;
            break-after: page;
        }
        
        /* Print Optimizations */
        @media print {
            /* Force new page for each brand */
            .brand-section {
                page-break-before: always;
                break-before: page;
            }
            
            .brand-section:first-child {
                page-break-before: auto;
                break-before: auto;
            }
            
            /* Prevent cards from being cut */
            .vehicle-card {
                page-break-inside: avoid;
                break-inside: avoid;
            }
            
            /* Page wrapper for grouping 3 cards */
            .page-wrapper {
                page-break-inside: avoid;
                break-inside: avoid;
            }
            
            .page-wrapper:not(:last-child) {
                page-break-after: always !important;
                break-after: page !important;
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