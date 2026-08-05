package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.FixedExpenseConfig
import com.myfinancemate.data.local.entity.Recurrence
import com.myfinancemate.data.local.entity.ReminderEntity
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.domain.repository.FixedExpenseConfigRepository
import com.myfinancemate.domain.repository.ReminderRepository
import com.myfinancemate.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedExpenseDetectorTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val reminderRepository = mockk<ReminderRepository>()
    private val configRepository = mockk<FixedExpenseConfigRepository>()

    private val detector = FixedExpenseDetector(
        transactionRepository,
        reminderRepository,
        configRepository
    )

    private val defaultConfig = FixedExpenseConfig()

    private fun expense(
        month: Int,
        amount: Double,
        merchant: String = "Netflix",
        accountId: Long = 1L
    ): TransactionEntity {
        val millis = java.util.Calendar.getInstance().apply {
            set(2026, month, 5, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return TransactionEntity(
            amount = amount,
            type = TransactionType.EXPENSE,
            description = merchant,
            merchant = merchant,
            senderInfo = "NETFLIX",
            date = millis,
            accountId = accountId
        )
    }

    @Test
    fun `recurring expense across 3 months creates monthly reminder`() = runTest {
        val txns = listOf(
            expense(0, 499.0),
            expense(1, 501.0),
            expense(2, 499.0)
        )
        coEvery { transactionRepository.getAllTransactionsList() } returns txns
        coEvery { configRepository.getSync() } returns defaultConfig
        coEvery { reminderRepository.getAllRemindersList() } returns emptyList()
        coEvery { reminderRepository.insert(any()) } returns 1L

        val created = detector.runDetection()

        assertTrue(created == 1)
        coVerify(exactly = 1) {
            reminderRepository.insert(match {
                it.recurrence == Recurrence.MONTHLY && it.sourceAccountId == 1L
            })
        }
    }

    @Test
    fun `expense seen only twice is not a fixed expense`() = runTest {
        val txns = listOf(
            expense(0, 499.0),
            expense(1, 501.0)
        )
        coEvery { transactionRepository.getAllTransactionsList() } returns txns
        coEvery { configRepository.getSync() } returns defaultConfig
        coEvery { reminderRepository.getAllRemindersList() } returns emptyList()

        val created = detector.runDetection()

        assertTrue(created == 0)
        coVerify(exactly = 0) { reminderRepository.insert(any()) }
    }

    @Test
    fun `high variance amount is not a fixed expense`() = runTest {
        val txns = listOf(
            expense(0, 100.0),
            expense(1, 900.0),
            expense(2, 100.0)
        )
        coEvery { transactionRepository.getAllTransactionsList() } returns txns
        coEvery { configRepository.getSync() } returns defaultConfig
        coEvery { reminderRepository.getAllRemindersList() } returns emptyList()

        val created = detector.runDetection()

        assertTrue(created == 0)
        coVerify(exactly = 0) { reminderRepository.insert(any()) }
    }

    @Test
    fun `existing matching reminder prevents duplicate`() = runTest {
        val txns = listOf(
            expense(0, 499.0),
            expense(1, 501.0),
            expense(2, 499.0)
        )
        val existing = ReminderEntity(
            id = 7,
            title = "Netflix",
            description = "Auto-detected recurring expense",
            amount = 499.7,
            recurrence = Recurrence.MONTHLY,
            nextTriggerTime = System.currentTimeMillis(),
            sourceAccountId = 1L
        )
        coEvery { transactionRepository.getAllTransactionsList() } returns txns
        coEvery { configRepository.getSync() } returns defaultConfig
        coEvery { reminderRepository.getAllRemindersList() } returns listOf(existing)

        val created = detector.runDetection()

        assertTrue(created == 0)
        coVerify(exactly = 0) { reminderRepository.insert(any()) }
    }

    @Test
    fun `income transactions are never grouped as expenses`() = runTest {
        val income = expense(0, 50000.0).copy(type = TransactionType.INCOME)
        coEvery { transactionRepository.getAllTransactionsList() } returns listOf(income, income.copy(date = income.date + 86400000L))
        coEvery { configRepository.getSync() } returns defaultConfig
        coEvery { reminderRepository.getAllRemindersList() } returns emptyList()

        val created = detector.runDetection()

        assertTrue(created == 0)
    }
}
