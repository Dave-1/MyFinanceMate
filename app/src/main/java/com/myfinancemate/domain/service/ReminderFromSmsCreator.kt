package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.Recurrence
import com.myfinancemate.data.local.entity.ReminderEntity
import com.myfinancemate.data.local.entity.ReminderType
import com.myfinancemate.domain.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ReminderInfo(
    val title: String,
    val description: String,
    val amount: Double?,
    val type: ReminderType,
    val recurrence: Recurrence,
    val nextTriggerTime: Long
) {
    fun toEntity() = ReminderEntity(
        title = title,
        description = description,
        amount = amount,
        type = type,
        recurrence = recurrence,
        nextTriggerTime = nextTriggerTime
    )
}

@Singleton
class ReminderFromSmsCreator @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend fun maybeCreateReminder(
        smsBody: String,
        senderId: String,
        smsTimestamp: Long,
        amount: Double? = null
    ): ReminderEntity? {
        val info = analyzeSmsForReminder(smsBody, senderId, smsTimestamp, amount) ?: return null
        val id = reminderRepository.insert(info.toEntity())
        return info.toEntity().copy(id = id)
    }

    companion object {
        private val reminderKeywords = listOf(
            "due date", "payment due", "due on", "due by",
            "expiry", "expire", "expiring", "expires", "will expire",
            "renewal", "renew", "up for renewal",
            "emi", "insurance due", "overdue",
            "subscription", "subscription end", "subscription expiry",
            "validity end", "plan expire"
        )

        private val monthlyKeywords = listOf("emi", "monthly")

        private val amountPattern = Regex(
            """(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )

        private val dateAfterKeyword = Regex(
            """(?:due\s+(?:on|by|date)\s*[:.]?\s*|expires?\s+on\s+|expiry\s*[:.]?\s*)(\d{1,2})(?:st|nd|rd|th)?\s*[-/]\s*([a-zA-Z]{3,9}|\d{1,2})\s*[-/]\s*(\d{4})""",
            RegexOption.IGNORE_CASE
        )

        fun analyzeSmsForReminder(
            smsBody: String,
            senderId: String,
            smsTimestamp: Long,
            amount: Double? = null
        ): ReminderInfo? {
            val lower = smsBody.lowercase()

            if (lower.contains("otp") || lower.contains("one time password")) {
                return null
            }

            val matchedKeyword = reminderKeywords.firstOrNull { lower.contains(it) } ?: return null

            val title = when {
                lower.contains("emi") -> "EMI Due - $senderId"
                lower.contains("renewal") || lower.contains("renew") -> "Renewal Due - $senderId"
                lower.contains("expir") -> "Expiry Alert - $senderId"
                lower.contains("subscription") || lower.contains("subscription") -> "Subscription Due - $senderId"
                else -> "Payment Due - $senderId"
            }

            val recurrence = if (monthlyKeywords.any { lower.contains(it) }) {
                Recurrence.MONTHLY
            } else {
                Recurrence.NONE
            }

            val extractedAmount = amount ?: amountPattern.find(smsBody)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()

            val triggerTime = extractTriggerDate(smsBody) ?: (smsTimestamp + 3 * 24 * 60 * 60 * 1000L)

            return ReminderInfo(
                title = title,
                description = smsBody.take(200),
                amount = extractedAmount,
                type = ReminderType.EXPENSE,
                recurrence = recurrence,
                nextTriggerTime = triggerTime
            )
        }

        private fun extractTriggerDate(smsBody: String): Long? {
            val match = dateAfterKeyword.find(smsBody) ?: return null
            val day = match.groupValues[1].padStart(2, '0')
            val monthStr = match.groupValues[2]
            val year = match.groupValues[3]

            val month = if (monthStr.length <= 2) {
                monthStr.padStart(2, '0')
            } else {
                val monthMap = mapOf(
                    "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
                    "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
                    "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12"
                )
                monthMap[monthStr.lowercase().take(3)] ?: return null
            }

            val dateStr = "$day-$month-$year"
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            return sdf.parse(dateStr)?.time
        }
    }
}
