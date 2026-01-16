package com.example.cardealer2.utils

import com.example.cardealer2.data.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating HTML bills from transaction data
 */
object TransactionBillGenerator {
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateInputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateDisplayFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy 'at' h:mm a", Locale.getDefault())
    
    /**
     * Data class to hold all related information for a bill
     */
    data class BillRelatedData(
        val personDetails: PersonBillDetails? = null,
        val purchaseDetails: Purchase? = null,
        val saleDetails: VehicleSale? = null,
        val emiDetails: EmiDetails? = null,
        val productDetails: ProductBillDetails? = null
    )
    
    data class PersonBillDetails(
        val name: String,
        val phone: String,
        val address: String
    )
    
    data class ProductBillDetails(
        val chassisNumber: String,
        val brandId: String? = null,
        val productId: String? = null,
        val type: String? = null,
        val year: Int? = null,
        val colour: String? = null
    )
    
    data class CompanyDetails(
        val name: String,
        val address: String,
        val phone: String,
        val email: String
    )
    
    private fun getDefaultCompanyDetails(): CompanyDetails {
        return CompanyDetails(
            name = "Car Dealer",
            address = "Your Business Address",
            phone = "Your Phone Number",
            email = "your.email@example.com"
        )
    }
    
    /**
     * Generate HTML bill content from transaction and related data
     */
    fun generateBillHtml(
        transaction: PersonTransaction,
        relatedData: BillRelatedData,
        isPunjabiEnabled: Boolean,
        companyDetails: CompanyDetails = getDefaultCompanyDetails()
    ): String {
        val transactionTypeLabel = getTransactionTypeLabel(transaction.type, isPunjabiEnabled)
        val formattedDate = formatDate(transaction.date)
        val formattedTime = formatTime(transaction.createdAt)
        
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                ${getBillCss()}
            </style>
        </head>
        <body>
            <div class="bill-container">
                <!-- Header Section -->
                <div class="header">
                    <h1 class="company-name">${companyDetails.name}</h1>
                    <p class="company-address">${companyDetails.address}</p>
                    <p class="company-contact">${companyDetails.phone} | ${companyDetails.email}</p>
                </div>
                
                <hr class="divider">
                
                <!-- Bill Title -->
                <div class="bill-title">
                    <h2>${transactionTypeLabel} ${TranslationManager.translate("Bill", isPunjabiEnabled)}</h2>
                    <p class="bill-number">${TranslationManager.translate("Bill No", isPunjabiEnabled)}: ${transaction.transactionNumber ?: transaction.transactionId.take(8).uppercase()}</p>
                    <p class="bill-date">${TranslationManager.translate("Date", isPunjabiEnabled)}: $formattedDate</p>
                    <p class="bill-time">${TranslationManager.translate("Time", isPunjabiEnabled)}: $formattedTime</p>
                </div>
                
                <!-- Transaction Details -->
                <div class="section">
                    <h3>${TranslationManager.translate("Transaction Details", isPunjabiEnabled)}</h3>
                    <table class="details-table">
                        <tr>
                            <td>${TranslationManager.translate("Transaction ID", isPunjabiEnabled)}</td>
                            <td>${transaction.transactionId}</td>
                        </tr>
                        <tr>
                            <td>${TranslationManager.translate("Type", isPunjabiEnabled)}</td>
                            <td>${transactionTypeLabel}</td>
                        </tr>
                        <tr>
                            <td>${TranslationManager.translate("Status", isPunjabiEnabled)}</td>
                            <td>${translateStatus(transaction.status, isPunjabiEnabled)}</td>
                        </tr>
                    </table>
                </div>
                
                <!-- Person Details -->
                ${relatedData.personDetails?.let { person ->
                    """
                    <div class="section">
                        <h3>${getPersonTypeLabel(transaction.personType, isPunjabiEnabled)} ${TranslationManager.translate("Details", isPunjabiEnabled)}</h3>
                        <table class="details-table">
                            <tr>
                                <td>${TranslationManager.translate("Name", isPunjabiEnabled)}</td>
                                <td>${person.name}</td>
                            </tr>
                            ${if (person.phone.isNotBlank()) """
                            <tr>
                                <td>${TranslationManager.translate("Phone", isPunjabiEnabled)}</td>
                                <td>${person.phone}</td>
                            </tr>
                            """ else ""}
                            ${if (person.address.isNotBlank()) """
                            <tr>
                                <td>${TranslationManager.translate("Address", isPunjabiEnabled)}</td>
                                <td>${person.address}</td>
                            </tr>
                            """ else ""}
                        </table>
                    </div>
                    """
                } ?: ""}
                
                <!-- Vehicle/Product Details -->
                ${relatedData.productDetails?.let { product ->
                    """
                    <div class="section">
                        <h3>${TranslationManager.translate("Vehicle Details", isPunjabiEnabled)}</h3>
                        <table class="details-table">
                            <tr>
                                <td>${TranslationManager.translate("Chassis Number", isPunjabiEnabled)}</td>
                                <td>${product.chassisNumber}</td>
                            </tr>
                            ${product.type?.let { """
                            <tr>
                                <td>${TranslationManager.translate("Type", isPunjabiEnabled)}</td>
                                <td>$it</td>
                            </tr>
                            """ } ?: ""}
                            ${product.year?.let { """
                            <tr>
                                <td>${TranslationManager.translate("Year", isPunjabiEnabled)}</td>
                                <td>$it</td>
                            </tr>
                            """ } ?: ""}
                            ${product.colour?.let { """
                            <tr>
                                <td>${TranslationManager.translate("Colour", isPunjabiEnabled)}</td>
                                <td>$it</td>
                            </tr>
                            """ } ?: ""}
                        </table>
                    </div>
                    """
                } ?: ""}
                
                <!-- Purchase Details -->
                ${relatedData.purchaseDetails?.let { purchase ->
                    """
                    <div class="section">
                        <h3>${TranslationManager.translate("Purchase Details", isPunjabiEnabled)}</h3>
                        <table class="details-table">
                            ${if (purchase.orderNumber > 0) """
                            <tr>
                                <td>${TranslationManager.translate("Order Number", isPunjabiEnabled)}</td>
                                <td>${purchase.orderNumber}</td>
                            </tr>
                            """ else ""}
                            ${if (purchase.gstAmount > 0) """
                            <tr>
                                <td>${TranslationManager.translate("GST Amount", isPunjabiEnabled)}</td>
                                <td>${currencyFormatter.format(purchase.gstAmount)}</td>
                            </tr>
                            """ else ""}
                            ${if (purchase.grandTotal > 0) """
                            <tr>
                                <td>${TranslationManager.translate("Grand Total", isPunjabiEnabled)}</td>
                                <td class="amount">${currencyFormatter.format(purchase.grandTotal)}</td>
                            </tr>
                            """ else ""}
                        </table>
                    </div>
                    """
                } ?: ""}
                
                <!-- Sale Details -->
                ${relatedData.saleDetails?.let { sale ->
                    """
                    <div class="section">
                        <h3>${TranslationManager.translate("Sale Details", isPunjabiEnabled)}</h3>
                        <table class="details-table">
                            <tr>
                                <td>${TranslationManager.translate("Total Amount", isPunjabiEnabled)}</td>
                                <td class="amount">${currencyFormatter.format(sale.totalAmount)}</td>
                            </tr>
                            <tr>
                                <td>${TranslationManager.translate("EMI", isPunjabiEnabled)}</td>
                                <td>${if (sale.emi) TranslationManager.translate("Yes", isPunjabiEnabled) else TranslationManager.translate("No", isPunjabiEnabled)}</td>
                            </tr>
                            <tr>
                                <td>${TranslationManager.translate("Status", isPunjabiEnabled)}</td>
                                <td>${if (sale.status) TranslationManager.translate("Completed", isPunjabiEnabled) else TranslationManager.translate("Pending", isPunjabiEnabled)}</td>
                            </tr>
                        </table>
                    </div>
                    """
                } ?: ""}
                
                <!-- EMI Details -->
                ${relatedData.emiDetails?.let { emi ->
                    """
                    <div class="section">
                        <h3>${TranslationManager.translate("EMI Details", isPunjabiEnabled)}</h3>
                        <table class="details-table">
                            <tr>
                                <td>${TranslationManager.translate("Total Installments", isPunjabiEnabled)}</td>
                                <td>${emi.installmentsCount}</td>
                            </tr>
                            <tr>
                                <td>${TranslationManager.translate("Paid Installments", isPunjabiEnabled)}</td>
                                <td>${emi.paidInstallments}</td>
                            </tr>
                            <tr>
                                <td>${TranslationManager.translate("Remaining Installments", isPunjabiEnabled)}</td>
                                <td>${emi.remainingInstallments}</td>
                            </tr>
                            <tr>
                                <td>${TranslationManager.translate("Installment Amount", isPunjabiEnabled)}</td>
                                <td>${currencyFormatter.format(emi.installmentAmount)}</td>
                            </tr>
                            ${if (emi.interestRate > 0) """
                            <tr>
                                <td>${TranslationManager.translate("Interest Rate", isPunjabiEnabled)}</td>
                                <td>${emi.interestRate}%</td>
                            </tr>
                            """ else ""}
                            ${if (emi.priceWithInterest > 0) """
                            <tr>
                                <td>${TranslationManager.translate("Total with Interest", isPunjabiEnabled)}</td>
                                <td class="amount">${currencyFormatter.format(emi.priceWithInterest)}</td>
                            </tr>
                            """ else ""}
                        </table>
                    </div>
                    """
                } ?: ""}
                
                <!-- Payment Details -->
                <div class="section payment-section">
                    <h3>${TranslationManager.translate("Payment Details", isPunjabiEnabled)}</h3>
                    <table class="payment-table">
                        <tr>
                            <td>${TranslationManager.translate("Total Amount", isPunjabiEnabled)}</td>
                            <td class="amount highlight">${currencyFormatter.format(transaction.amount)}</td>
                        </tr>
                        <tr>
                            <td>${TranslationManager.translate("Payment Method", isPunjabiEnabled)}</td>
                            <td>${translatePaymentMethod(transaction.paymentMethod, isPunjabiEnabled)}</td>
                        </tr>
                    </table>
                    
                    ${if (transaction.paymentMethod == "MIXED" || transaction.cashAmount > 0 || transaction.bankAmount > 0 || transaction.creditAmount > 0) {
                        """
                        <h4>${TranslationManager.translate("Payment Breakdown", isPunjabiEnabled)}</h4>
                        <table class="breakdown-table">
                            ${if (transaction.cashAmount > 0) """
                            <tr>
                                <td>${TranslationManager.translate("Cash", isPunjabiEnabled)}</td>
                                <td>${currencyFormatter.format(transaction.cashAmount)}</td>
                            </tr>
                            """ else ""}
                            ${if (transaction.bankAmount > 0) """
                            <tr>
                                <td>${TranslationManager.translate("Bank", isPunjabiEnabled)}</td>
                                <td>${currencyFormatter.format(transaction.bankAmount)}</td>
                            </tr>
                            """ else ""}
                            ${if (transaction.creditAmount > 0) """
                            <tr>
                                <td>${TranslationManager.translate("Credit", isPunjabiEnabled)}</td>
                                <td>${currencyFormatter.format(transaction.creditAmount)}</td>
                            </tr>
                            """ else ""}
                        </table>
                        """
                    } else ""}
                </div>
                
                <!-- Additional Information -->
                ${if (transaction.description.isNotBlank()) """
                <div class="section">
                    <h3>${TranslationManager.translate("Description", isPunjabiEnabled)}</h3>
                    <p class="description">${transaction.description}</p>
                </div>
                """ else ""}
                
                ${if (transaction.note.isNotBlank()) """
                <div class="section">
                    <h3>${TranslationManager.translate("Note", isPunjabiEnabled)}</h3>
                    <p class="note">${transaction.note}</p>
                </div>
                """ else ""}
                
                <!-- Footer -->
                <div class="footer">
                    <hr class="divider">
                    <p class="footer-text">${TranslationManager.translate("Thank you for your business!", isPunjabiEnabled)}</p>
                    <p class="footer-note">${TranslationManager.translate("This is a computer generated bill.", isPunjabiEnabled)}</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
    
    private fun getBillCss(): String {
        return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        @page {
            size: A4;
            margin: 15mm 15mm 15mm 15mm;
        }
        
        body {
            font-family: 'Arial', 'Helvetica', sans-serif;
            font-size: 10pt;
            line-height: 1.5;
            color: #333;
            background: white;
            width: 180mm;
            margin: 0 auto;
        }
        
        .bill-container {
            width: 180mm;
            max-width: 180mm;
            margin: 0 auto;
            padding: 0;
            background: white;
            min-height: 267mm;
        }
        
        .header {
            text-align: center;
            margin-bottom: 12pt;
            border-bottom: 2pt solid #2563eb;
            padding-bottom: 10pt;
        }
        
        .company-name {
            font-size: 20pt;
            font-weight: bold;
            color: #2563eb;
            margin-bottom: 6pt;
            line-height: 1.2;
        }
        
        .company-address {
            font-size: 9pt;
            color: #666;
            margin: 3pt 0;
            line-height: 1.4;
        }
        
        .company-contact {
            font-size: 9pt;
            color: #666;
            margin: 3pt 0;
            line-height: 1.4;
        }
        
        .divider {
            border: none;
            border-top: 1pt solid #e5e7eb;
            margin: 10pt 0;
        }
        
        .bill-title {
            text-align: center;
            margin: 15pt 0;
        }
        
        .bill-title h2 {
            font-size: 16pt;
            color: #1f2937;
            margin-bottom: 8pt;
            line-height: 1.3;
        }
        
        .bill-number, .bill-date, .bill-time {
            font-size: 9pt;
            color: #6b7280;
            margin: 2pt 0;
            line-height: 1.4;
        }
        
        .section {
            margin: 12pt 0;
            padding: 10pt;
            background: #f9fafb;
            border-radius: 4pt;
            border-left: 3pt solid #2563eb;
            page-break-inside: avoid;
        }
        
        .payment-section {
            border-left-color: #059669;
        }
        
        .section h3 {
            font-size: 11pt;
            color: #1f2937;
            margin-bottom: 8pt;
            font-weight: bold;
            line-height: 1.3;
        }
        
        .section h4 {
            font-size: 10pt;
            color: #374151;
            margin: 8pt 0 6pt 0;
            font-weight: bold;
            line-height: 1.3;
        }
        
        .details-table, .payment-table, .breakdown-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 6pt;
            font-size: 9pt;
        }
        
        .details-table td, .payment-table td, .breakdown-table td {
            padding: 5pt 6pt;
            border-bottom: 0.5pt solid #e5e7eb;
            line-height: 1.4;
            vertical-align: top;
        }
        
        .details-table td:first-child {
            font-weight: 600;
            color: #4b5563;
            width: 40%;
        }
        
        .payment-table td:first-child {
            font-weight: 600;
            color: #4b5563;
            width: 50%;
        }
        
        .amount {
            font-size: 13pt;
            font-weight: bold;
            color: #059669;
            line-height: 1.3;
        }
        
        .amount.highlight {
            font-size: 15pt;
            color: #047857;
        }
        
        .description, .note {
            padding: 8pt;
            background: white;
            border-radius: 3pt;
            margin-top: 6pt;
            color: #374151;
            white-space: pre-wrap;
            word-wrap: break-word;
            font-size: 9pt;
            line-height: 1.5;
        }
        
        .footer {
            margin-top: 20pt;
            text-align: center;
            page-break-inside: avoid;
        }
        
        .footer-text {
            font-size: 10pt;
            font-weight: bold;
            color: #1f2937;
            margin: 8pt 0;
            line-height: 1.4;
        }
        
        .footer-note {
            font-size: 8pt;
            color: #9ca3af;
            font-style: italic;
            line-height: 1.4;
        }
        
        @media print {
            body {
                width: 180mm;
            }
            .bill-container {
                width: 180mm;
                padding: 0;
            }
            .section {
                page-break-inside: avoid;
            }
        }
        """.trimIndent()
    }
    
    // Helper functions
    private fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val parsedDate = dateInputFormatter.parse(dateString) ?: Date()
            dateDisplayFormatter.format(parsedDate)
        } catch (e: Exception) {
            dateString
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        return try {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeFormat.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun getTransactionTypeLabel(type: String, isPunjabiEnabled: Boolean): String {
        return when (type) {
            TransactionType.PURCHASE -> TranslationManager.translate("Purchase", isPunjabiEnabled)
            TransactionType.SALE -> TranslationManager.translate("Sale", isPunjabiEnabled)
            TransactionType.EMI_PAYMENT -> TranslationManager.translate("EMI Payment", isPunjabiEnabled)
            TransactionType.BROKER_FEE -> TranslationManager.translate("Broker Fee", isPunjabiEnabled)
            else -> type
        }
    }
    
    private fun getPersonTypeLabel(personType: String, isPunjabiEnabled: Boolean): String {
        return when (personType) {
            "CUSTOMER" -> TranslationManager.translate("Customer", isPunjabiEnabled)
            "BROKER" -> TranslationManager.translate("Broker", isPunjabiEnabled)
            "MIDDLE_MAN" -> TranslationManager.translate("Middle Man", isPunjabiEnabled)
            else -> personType
        }
    }
    
    private fun translatePaymentMethod(method: String, isPunjabiEnabled: Boolean): String {
        return when (method) {
            "BANK" -> TranslationManager.translate("Bank", isPunjabiEnabled)
            "CASH" -> TranslationManager.translate("Cash", isPunjabiEnabled)
            "CREDIT" -> TranslationManager.translate("Credit", isPunjabiEnabled)
            "MIXED" -> TranslationManager.translate("Mixed", isPunjabiEnabled)
            else -> method
        }
    }
    
    private fun translateStatus(status: String, isPunjabiEnabled: Boolean): String {
        return when (status) {
            "COMPLETED" -> TranslationManager.translate("Completed", isPunjabiEnabled)
            "PENDING" -> TranslationManager.translate("Pending", isPunjabiEnabled)
            "CANCELLED" -> TranslationManager.translate("Cancelled", isPunjabiEnabled)
            else -> status
        }
    }
}

