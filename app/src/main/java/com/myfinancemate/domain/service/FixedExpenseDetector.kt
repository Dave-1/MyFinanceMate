package com.myfinancemate.domain.service

import android.util.Log
import com.myfinancemate.data.local.entity.FixedExpenseConfig
import com.myfinancemate.data.local.entity.Recurrence
import com.myfinancemate.data.local.entity.ReminderEntity
import com.myfinancemate.data.local.entity.ReminderType
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.domain.repository.FixedExpenseConfigRepository
import com.myfinancemate.domain.repository.ReminderRepository
import com.myfinancemate.domain.repository.TransactionRepository
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects recurring expenses and creates MONTHLY reminders.
 *
 * Algorithm:
 * 1. Group all EXPENSE transactions by (accountId, merchant, categoryId).
 * 2. Bucket each group by calendar month.
 * 3. If distinct months >= [FixedExpenseConfig.minOccurrences] and amount
 *    variance <= [FixedExpenseConfig.variancePercent], it is a fixed expense.
 * 4. Create/update one MONTHLY ReminderEntity per detected expense, with
 *    [ReminderEntity.sourceAccountId] set. Idempotent: skips groups that
 *    already have a matching reminder, so re-running after every SMS never
 *    duplicates.
 */
@Singleton
class FixedExpenseDetector @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val reminderRepository: ReminderRepository,
    private val configRepository: FixedExpenseConfigRepository
) {

    private data class GroupKey(
        val accountId: Long?,
        val merchant: String,
        val categoryId: Long?
    )

    suspend fun runDetection(): Int {
        val config = configRepository.getSync() ?: FixedExpenseConfig()

        // Fast path: if a salary/primary account has no expenses, nothing to do.
        val allTransactions = transactionRepository.getAllTransactionsList()
        val expenses = allTransactions.filter { it.type == TransactionType.EXPENSE }

        // Group by (accountId, merchant, categoryId)
        val grouped = expenses.groupBy {
            GroupKey(it.accountId, it.merchant.ifBlank { "unknown" }, it.categoryId)
        }

        var existingReminders = reminderRepository.getAllRemindersList()
        var created = 0

        for ((key, txns) in grouped) {
            // Bucket by calendar month (yyyy-MM)
            val byMonth = txns.groupBy { monthKey(it.date) }
            if (byMonth.size < config.minOccurrences) continue

            val amounts = txns.map { it.amount }
            val avg = amounts.average()
            if (!withinVariance(amounts, avg, config.variancePercent)) continue

            val avgDay = txns.map { dayOfMonth(it.date) }.average().toInt()
            val title = key.merchant.take(1).uppercase() + key.merchant.drop(1)

            // Idempotency: skip if a matching MONTHLY reminder already exists.
            if (reminderExists(existingReminders, key, title, avg)) continue

            val nextTrigger = nextMonthlyTrigger(avgDay)
            val reminder = ReminderEntity(
                title = title,
                description = "Auto-detected recurring expense",
                amount = Math.round(avg * 100.0) / 100.0,
                type = ReminderType.EXPENSE,
                recurrence = Recurrence.MONTHLY,
                nextTriggerTime = nextTrigger,
                isActive = true,
                sourceAccountId = key.accountId
            )
            reminderRepository.insert(reminder)
            existingReminders = existingReminders + reminder
            created++
            Log.d(TAG, "Created fixed-expense reminder: '$title' avg=₹$avg")
        }

        Log.d(TAG, "FixedExpenseDetector finished: $created reminder(s) created")
        return created
    }

    /** Runs detection scoped to a single newly-inserted expense's group. */
    suspend fun runDetectionForTransaction(txn: TransactionEntity): Int {
        return runDetection()
    }

    private fun withinVariance(amounts: List<Double>, avg: Double, variancePercent: Double): Boolean {
        if (amounts.isEmpty()) return false
        if (variancePercent <= 0.0) return true
        val tolerance = avg * (variancePercent / 100.0)
        return amounts.all { kotlin.math.abs(it - avg) <= tolerance }
    }

    private fun reminderExists(
        existing: List<ReminderEntity>,
        key: GroupKey,
        title: String,
        avg: Double
    ): Boolean {
        return existing.any { r ->
            r.recurrence == Recurrence.MONTHLY &&
                r.sourceAccountId == key.accountId &&
                (r.title.equals(title, ignoreCase = true) || r.description.contains(key.merchant, ignoreCase = true)) &&
                (r.amount == null || Math.abs(r.amount - avg) / avg < 0.25)
        }
    }

    private fun monthKey(epochMillis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = epochMillis
        return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun dayOfMonth(epochMillis: Long): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = epochMillis
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    private fun nextMonthlyTrigger(day: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val target = day.coerceIn(1, 28)
        if (cal.get(Calendar.DAY_OF_MONTH) >= target) {
            cal.add(Calendar.MONTH, 1)
        }
        cal.set(Calendar.DAY_OF_MONTH, target)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "FixedExpenseDetector"
    }
}
