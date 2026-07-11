package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.Recurrence
import com.myfinancemate.data.local.entity.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderFromSmsCreatorTest {

    @Test
    fun `smsWithDueDateKeyword creates reminder with EXPENSE type`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Your credit card bill payment of Rs.5000 is due on 15th July 2026",
            senderId = "HDFCBK",
            smsTimestamp = 1720000000000L
        )

        assertNotNull(result)
        assertEquals(ReminderType.EXPENSE, result?.type)
        assertEquals(Recurrence.NONE, result?.recurrence)
    }

    @Test
    fun `smsWithExpiryKeyword creates reminder`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Your insurance policy will expire on 31-Dec-2026. Renew now to avoid lapse.",
            senderId = "ICICIBK",
            smsTimestamp = 1720000000000L
        )

        assertNotNull(result)
        assertEquals(ReminderType.EXPENSE, result?.type)
        assertEquals(Recurrence.NONE, result?.recurrence)
    }

    @Test
    fun `smsWithEMIKeyword creates monthly reminder`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Your monthly EMI of Rs.15000 is due on 10th of every month.",
            senderId = "AXISBANK",
            smsTimestamp = 1720000000000L
        )

        assertNotNull(result)
        assertEquals(Recurrence.MONTHLY, result?.recurrence)
    }

    @Test
    fun `otpSms returns null`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Your OTP for transaction is 123456. Do not share.",
            senderId = "HDFCBK",
            smsTimestamp = 1720000000000L
        )

        assertNull(result)
    }

    @Test
    fun `pureTransactionSmsWithoutDueDate returns null`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Rs.5000 debited from a/c XX1234 on 10-Jul-2026 at AMAZON. Available bal: Rs.25000.",
            senderId = "HDFCBK",
            smsTimestamp = 1720000000000L
        )

        assertNull(result)
    }

    @Test
    fun `smsWithNoReminderKeywords returns null`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Welcome to HDFC Bank. Your account has been opened successfully.",
            senderId = "HDFCBK",
            smsTimestamp = 1720000000000L
        )

        assertNull(result)
    }

    @Test
    fun `smsWithRenewalKeyword creates reminder`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Your domain registration is up for renewal. Renew now to avoid disruption.",
            senderId = "GODADDY",
            smsTimestamp = 1720000000000L
        )

        assertNotNull(result)
    }

    @Test
    fun `smsWithPaymentDueKeyword creates reminder`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Reminder: Your HDFC Bank credit card payment of Rs.12000 is due on 25-Jul-2026.",
            senderId = "HDFCBK",
            smsTimestamp = 1720000000000L
        )

        assertNotNull(result)
    }

    @Test
    fun `smsWithAmountIncludedInBody provides amount in reminder`() {
        val result = ReminderFromSmsCreator.analyzeSmsForReminder(
            smsBody = "Your credit card bill of Rs.8500 is due on 15-Aug-2026. Pay on time.",
            senderId = "SBIBANK",
            smsTimestamp = 1720000000000L
        )

        assertNotNull(result)
    }
}
