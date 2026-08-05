package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.SmsRuleEntity
import com.myfinancemate.data.local.entity.SmsRuleType
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.domain.repository.SmsRuleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryDetectorTest {

    private val smsRuleRepository = mockk<SmsRuleRepository>()
    private val detector = SalaryDetector(smsRuleRepository)

    private fun income(merchant: String, sender: String = merchant) = TransactionEntity(
        amount = 50000.0,
        type = TransactionType.INCOME,
        description = merchant,
        merchant = merchant,
        senderInfo = sender,
        date = System.currentTimeMillis()
    )

    private fun salaryRule(senderId: String) = SmsRuleEntity(
        senderId = senderId,
        senderName = senderId,
        ruleType = SmsRuleType.SALARY,
        isActive = true
    )

    @Test
    fun `income from sender with SALARY rule is salary`() = runTest {
        coEvery { smsRuleRepository.getSalaryRules() } returns listOf(salaryRule("ACME-CORP"))

        val txn = income(merchant = "Salary", sender = "ACME-CORP")
        assertTrue(detector.isSalary(txn))
        coVerify { smsRuleRepository.getSalaryRules() }
    }

    @Test
    fun `expense is never salary`() = runTest {
        coEvery { smsRuleRepository.getSalaryRules() } returns listOf(salaryRule("ACME-CORP"))

        val expense = TransactionEntity(
            amount = 1000.0,
            type = TransactionType.EXPENSE,
            description = "Groceries",
            merchant = "BigBasket",
            senderInfo = "BB-BASKET",
            date = System.currentTimeMillis()
        )
        assertFalse(detector.isSalary(expense))
    }

    @Test
    fun `income without salary rule is not salary`() = runTest {
        coEvery { smsRuleRepository.getSalaryRules() } returns emptyList()

        val txn = income(merchant = "Salary", sender = "ACME-CORP")
        assertFalse(detector.isSalary(txn))
    }

    @Test
    fun `income from unrelated sender is not salary`() = runTest {
        coEvery { smsRuleRepository.getSalaryRules() } returns listOf(salaryRule("ACME-CORP"))

        val txn = income(merchant = "UPI Credit", sender = "UPI-PAY")
        assertFalse(detector.isSalary(txn))
    }

    @Test
    fun `inactive salary rule is ignored`() = runTest {
        coEvery { smsRuleRepository.getSalaryRules() } returns
            listOf(salaryRule("ACME-CORP").copy(isActive = false))

        val txn = income(merchant = "Salary", sender = "ACME-CORP")
        assertFalse(detector.isSalary(txn))
    }
}
