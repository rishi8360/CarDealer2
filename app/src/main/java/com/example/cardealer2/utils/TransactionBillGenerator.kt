package com.example.cardealer2.utils

import com.example.cardealer2.data.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating HTML bills from transaction data
 */
object TransactionBillGenerator {
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private val dateInputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateDisplayFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy 'at' h:mm a", Locale.getDefault())
    private val invoiceDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    
    /**
     * Data class to hold all related information for a bill
     */
    data class BillRelatedData(
        val personDetails: PersonBillDetails? = null,
        val purchaseDetails: Purchase? = null,
        val saleDetails: VehicleSale? = null,
        val emiDetails: EmiDetails? = null,
        val productDetails: ProductBillDetails? = null,
        val companyDetails: Company? = null
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
     * Generate HTML bill for a transaction
     */
    fun generateBillHTML(
        transaction: PersonTransaction,
        relatedData: BillRelatedData,
        invoiceNumber: String,
        invoiceDate: Long = System.currentTimeMillis(),
        buyerName: String = "",
        buyerAddress: String = "",
        buyerGstin: String = ""
    ): String {
        val company = relatedData.companyDetails
        val companyName = company?.name ?: "Car Dealer"
        val companyGstin = company?.gstin ?: ""
        val companyAddress = company?.phone ?: "" // Using phone field for address as per Company model
        
        // Calculate amounts based on transaction type
        val baseAmount = transaction.amount
        val discount = 0.0 // Transactions don't have discount in current model
        val afterDiscount = baseAmount - discount
        
        // GST calculation - if purchase has GST, use it; otherwise 0
        val gstAmount = relatedData.purchaseDetails?.gstAmount ?: 0.0
        val gstPercentage = if (gstAmount > 0 && afterDiscount > 0) {
            (gstAmount / afterDiscount) * 100.0
        } else {
            0.0
        }
        
        val cgstAmount = gstAmount / 2.0
        val sgstAmount = gstAmount / 2.0
        
        val grandTotal = afterDiscount + gstAmount
        val roundedTotal = Math.round(grandTotal).toLong()
        val roundedOff = roundedTotal - grandTotal
        
        val amountInWords = convertNumberToWords(roundedTotal.toDouble())
        
        // Get service description based on transaction type
        val serviceDescription = when (transaction.type) {
            TransactionType.PURCHASE -> "Vehicle Purchase"
            TransactionType.SALE -> "Vehicle Sale"
            TransactionType.EMI_PAYMENT -> "EMI Payment"
            TransactionType.BROKER_FEE -> "Broker Fee"
            else -> "Transaction"
        }
        
        // Get product details for description
        val productInfo = relatedData.productDetails
        val productDescription = buildString {
            append(serviceDescription)
            productInfo?.let {
                if (it.chassisNumber.isNotBlank()) {
                    append(" - Chassis: ${it.chassisNumber}")
                }
                if (it.type != null) {
                    append(" - Type: ${it.type}")
                }
                if (it.year != null) {
                    append(" - Year: ${it.year}")
                }
            }
        }
        
        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
* {
margin: 0;
padding: 0;
box-sizing: border-box;
}
body {
font-family: Arial, sans-serif;
font-size: 12px;
padding: 20px;
background: white;
}
.header {
text-align: center;
margin-bottom: 20px;
}
.header h1 {
font-size: 24px;
font-weight: bold;
margin-bottom: 10px;
}
.original-copy {
text-align: right;
font-weight: bold;
margin-bottom: 10px;
}
.company-info {
margin-bottom: 20px;
}
.company-info h2 {
font-size: 18px;
margin-bottom: 10px;
}
.info-row {
margin: 5px 0;
}
.two-columns {
display: flex;
justify-content: space-between;
margin-bottom: 20px;
}
.column {
width: 48%;
}
.invoice-details {
margin-bottom: 20px;
}
.invoice-details table {
width: 100%;
border-collapse: collapse;
}
.invoice-details td {
padding: 5px;
border: 1px solid #ddd;
}
.invoice-details td:first-child {
font-weight: bold;
width: 40%;
}
.service-table {
width: 100%;
border-collapse: collapse;
margin-bottom: 20px;
}
.service-table th,
.service-table td {
border: 1px solid #000;
padding: 8px;
text-align: left;
}
.service-table th {
background-color: #f0f0f0;
font-weight: bold;
text-align: center;
}
.service-table td {
text-align: right;
}
.service-table td:first-child,
.service-table td:nth-child(2) {
text-align: left;
}
.summary {
margin-bottom: 20px;
}
.summary table {
width: 100%;
border-collapse: collapse;
}
.summary td {
padding: 5px;
border: 1px solid #ddd;
}
.summary td:first-child {
font-weight: bold;
width: 70%;
}
.summary td:last-child {
text-align: right;
}
.tax-summary {
margin-bottom: 20px;
}
.tax-summary table {
width: 100%;
border-collapse: collapse;
}
.tax-summary th,
.tax-summary td {
border: 1px solid #000;
padding: 8px;
text-align: center;
}
.tax-summary th {
background-color: #f0f0f0;
font-weight: bold;
}
.amount-in-words {
margin: 20px 0;
padding: 10px;
border: 1px solid #000;
font-weight: bold;
}
@media print {
body {
padding: 10px;
}
}
</style>
</head>
<body>
<div class="original-copy">ORIGINAL COPY</div>
<div class="header">
<h1>TAX INVOICE</h1>
</div>
<div class="company-info">
<h2>$companyName</h2>
${if (companyAddress.isNotBlank()) "<div class=\"info-row\">$companyAddress</div>" else ""}
${if (companyGstin.isNotBlank()) "<div class=\"info-row\">GSTIN: $companyGstin</div>" else ""}
</div>
<div class="two-columns">
<div class="column">
<h3>Party Details (Buyer):</h3>
<div class="info-row"><strong>Name:</strong> ${if (buyerName.isNotBlank()) buyerName else (relatedData.personDetails?.name ?: transaction.personName)}</div>
${if (buyerAddress.isNotBlank() || relatedData.personDetails?.address?.isNotBlank() == true) {
    "<div class=\"info-row\"><strong>Address:</strong> ${buyerAddress.ifBlank { relatedData.personDetails?.address ?: "" }}</div>"
} else ""}
${if (buyerGstin.isNotBlank()) "<div class=\"info-row\"><strong>GSTIN / UIN:</strong> $buyerGstin</div>" else ""}
</div>
<div class="column">
<div class="invoice-details">
<table>
<tr>
<td>Invoice No.:</td>
<td>$invoiceNumber</td>
</tr>
<tr>
<td>Dated:</td>
<td>${invoiceDateFormat.format(Date(invoiceDate))}</td>
</tr>
<tr>
<td>Place of Supply:</td>
<td>Haryana (06)</td>
</tr>
</table>
</div>
</div>
</div>
<table class="service-table">
<thead>
<tr>
<th>S.N.</th>
<th>Description of Service</th>
<th>Amount (₹)</th>
</tr>
</thead>
<tbody>
<tr>
<td>1</td>
<td>$productDescription</td>
<td>${formatCurrencyAmount(baseAmount)}</td>
</tr>
</tbody>
</table>
<div class="summary">
<table>
<tr>
<td>Subtotal:</td>
<td>${formatCurrencyAmount(baseAmount)}</td>
</tr>
${if (discount > 0) {
    "<tr><td>Less: Discount:</td><td>-${formatCurrencyAmount(discount)}</td></tr>"
} else ""}
<tr>
<td>Taxable Amount:</td>
<td>${formatCurrencyAmount(afterDiscount)}</td>
</tr>
${if (gstPercentage > 0) {
    "<tr><td>Add: GST (${String.format("%.2f", gstPercentage)}%):</td><td>${formatCurrencyAmount(gstAmount)}</td></tr>"
} else if (gstAmount > 0) {
    "<tr><td>Add: GST:</td><td>${formatCurrencyAmount(gstAmount)}</td></tr>"
} else ""}
${if (Math.abs(roundedOff) > 0.01) {
    "<tr><td>Add: Rounded Off (${if (roundedOff > 0) "+" else ""}):</td><td>${formatCurrencyAmount(roundedOff)}</td></tr>"
} else ""}
<tr style="font-weight: bold; font-size: 14px;">
<td>Grand Total:</td>
<td>${formatCurrencyAmount(roundedTotal.toDouble())}</td>
</tr>
</table>
</div>
${if (gstPercentage > 0 || gstAmount > 0) {
    """
<div class="tax-summary">
<table>
<thead>
<tr>
<th>Taxable Value</th>
<th>CGST</th>
<th>SGST</th>
<th>Total GST</th>
</tr>
</thead>
<tbody>
<tr>
<td>${formatCurrencyAmount(afterDiscount)}</td>
<td>${formatCurrencyAmount(cgstAmount)}</td>
<td>${formatCurrencyAmount(sgstAmount)}</td>
<td>${formatCurrencyAmount(gstAmount)}</td>
</tr>
</tbody>
</table>
</div>
"""
} else ""}
<div class="amount-in-words">
<strong>Amount in Words:</strong> Rupees $amountInWords Only
</div>
<div style="margin-top: 60px; text-align: center; padding: 30px 20px;">
<h2 style="color: #1976d2; margin-bottom: 10px; font-size: 20px;">Thank You!</h2>
<p style="font-size: 14px; color: #666; margin-top: 10px;">
This is an E-Bill generated electronically and does not require a physical signature.
</p>
</div>
</body>
</html>
""".trimIndent()
    }
    
    /**
     * Format currency amount without currency symbol for HTML display
     */
    private fun formatCurrencyAmount(amount: Double): String {
        return currencyFormatter.format(amount).replace("₹", "").trim()
    }
    
    /**
     * Convert number to words (Indian numbering system)
     */
    private fun convertNumberToWords(number: Double): String {
        val rupees = number.toLong()
        val paise = ((number - rupees) * 100).toInt()
        val rupeesInWords = numberToWords(rupees)
        val paiseInWords = if (paise > 0) {
            " and ${numberToWords(paise.toLong())} Paise"
        } else {
            ""
        }
        return "$rupeesInWords$paiseInWords"
    }
    
    /**
     * Convert number to words
     */
    private fun numberToWords(number: Long): String {
        if (number == 0L) return "Zero"
        
        val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        
        fun convertHundreds(n: Long): String {
            var num = n
            var result = ""
            if (num >= 100) {
                result += ones[(num / 100).toInt()] + " Hundred "
                num %= 100
            }
            if (num >= 20) {
                result += tens[(num / 10).toInt()] + " "
                num %= 10
            }
            if (num > 0) {
                result += ones[num.toInt()] + " "
            }
            return result.trim()
        }
        
        var num = number
        var result = ""
        
        if (num >= 10000000) {
            result += convertHundreds(num / 10000000) + " Crore "
            num %= 10000000
        }
        if (num >= 100000) {
            result += convertHundreds(num / 100000) + " Lakh "
            num %= 100000
        }
        if (num >= 1000) {
            result += convertHundreds(num / 1000) + " Thousand "
            num %= 1000
        }
        if (num > 0) {
            result += convertHundreds(num)
        }
        
        return result.trim().replace("  ", " ")
    }
    
    // Legacy function for backward compatibility
    fun generateBillHtml(
        transaction: PersonTransaction,
        relatedData: BillRelatedData,
        isPunjabiEnabled: Boolean
    ): String {
        // Generate invoice number
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val invoiceNumber = "CARDEALER/${transaction.transactionId.take(8)}/$dateStr"
        
        return generateBillHTML(
            transaction = transaction,
            relatedData = relatedData,
            invoiceNumber = invoiceNumber,
            invoiceDate = System.currentTimeMillis(),
            buyerName = relatedData.personDetails?.name ?: transaction.personName,
            buyerAddress = relatedData.personDetails?.address ?: "",
            buyerGstin = ""
        )
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

