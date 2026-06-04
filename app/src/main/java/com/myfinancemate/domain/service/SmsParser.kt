package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.TransactionType
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val merchant: String,
    val senderInfo: String,
    val date: Long
)

@Singleton
class SmsParser @Inject constructor() {

    // Common patterns for Indian bank SMS
    private val debitPatterns = listOf(
        Pattern.compile("(?i)(?:debited|debit|spent|paid|withdrawn|purchase|sent|transferred).*?(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*).*?(?:debited|debit|spent|paid|withdrawn|purchase)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
    )

    private val creditPatterns = listOf(
        Pattern.compile("(?i)(?:credited|credit|received|deposited|refund).*?(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*).*?(?:credited|credit|received|deposited|refund)", Pattern.CASE_INSENSITIVE)
    )

    // Patterns to extract merchant/payee
    private val merchantPatterns = listOf(
        Pattern.compile("(?i)(?:at|to|from|for|via)\\s+([A-Za-z0-9\\s&.'-]+?)(?:\\s+(?:on|ref|txn|via|card|a/c|account|upi|neft|imps|rtgs))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:at|to|from)\\s+([A-Za-z0-9\\s&.'-]+?)(?:\\s+on\\s+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:UPI|NEFT|IMPS|RTGS).*?(?:to|from)\\s+([A-Za-z0-9\\s&.'-]+?)(?:\\s|\\.|$)", Pattern.CASE_INSENSITIVE)
    )

    // Reference number pattern
    private val refPattern = Pattern.compile("(?i)(?:ref|reference|txn|transaction)\\s*(?:no|number|id|#)?[:.\\s]*([A-Za-z0-9]+)")

    fun parse(smsBody: String, senderId: String): ParsedTransaction? {
        val amount = extractAmount(smsBody) ?: return null
        val type = determineType(smsBody)
        val merchant = extractMerchant(smsBody)
        val description = buildDescription(smsBody, merchant)

        return ParsedTransaction(
            amount = amount,
            type = type,
            description = description,
            merchant = merchant,
            senderInfo = senderId,
            date = System.currentTimeMillis()
        )
    }

    private fun extractAmount(smsBody: String): Double? {
        val allPatterns = debitPatterns + creditPatterns
        for (pattern in allPatterns) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull()
                if (amount != null && amount > 0) return amount
            }
        }
        return null
    }

    private fun determineType(smsBody: String): TransactionType {
        val lowerBody = smsBody.lowercase()
        val creditKeywords = listOf("credited", "credit", "received", "deposited", "refund", "salary")
        val debitKeywords = listOf("debited", "debit", "spent", "paid", "withdrawn", "purchase", "sent", "transferred")

        for (keyword in creditKeywords) {
            if (lowerBody.contains(keyword)) return TransactionType.INCOME
        }
        for (keyword in debitKeywords) {
            if (lowerBody.contains(keyword)) return TransactionType.EXPENSE
        }

        // Check credit patterns
        for (pattern in creditPatterns) {
            if (pattern.matcher(smsBody).find()) return TransactionType.INCOME
        }

        return TransactionType.EXPENSE
    }

    private fun extractMerchant(smsBody: String): String {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }
        return ""
    }

    private fun buildDescription(smsBody: String, merchant: String): String {
        if (merchant.isNotBlank()) return merchant

        // Try to build a meaningful description from the SMS
        val refMatch = refPattern.matcher(smsBody)
        val ref = if (refMatch.find()) "Ref: ${refMatch.group(1)}" else ""

        // Take first 100 chars of SMS as description
        val truncated = if (smsBody.length > 100) smsBody.take(100) + "..." else smsBody
        return if (ref.isNotBlank()) "$truncated ($ref)" else truncated
    }
}
