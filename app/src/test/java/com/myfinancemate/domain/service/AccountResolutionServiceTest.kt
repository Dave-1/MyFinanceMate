package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.AccountEntity
import com.myfinancemate.domain.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountResolutionServiceTest {

    private val accountRepository = mockk<AccountRepository>()
    private val service = AccountResolutionService(accountRepository)

    private fun existingAccount() = AccountEntity(
        id = 1,
        name = "HDFC",
        senderId = "HDFCBK",
        isPrimary = true
    )

    @Test
    fun `existing sender returns existing account without creating new`() = runTest {
        coEvery { accountRepository.getBySenderId("HDFCBK") } returns existingAccount()

        val result = service.resolve("HDFCBK")

        assertNotNull(result)
        assertEquals(1L, result?.id)
        coVerify(exactly = 0) { accountRepository.insert(any()) }
    }

    @Test
    fun `first-ever account becomes primary`() = runTest {
        coEvery { accountRepository.getBySenderId("HDFCBK") } returns null
        coEvery { accountRepository.count() } returns 0
        coEvery { accountRepository.insert(any()) } returns 42L
        coEvery { accountRepository.getById(42L) } returns existingAccount().copy(id = 42L)

        val result = service.resolve("HDFCBK")

        assertNotNull(result)
        assertTrue(result!!.isPrimary)
        coVerify { accountRepository.insert(match { it.isPrimary }) }
    }

    @Test
    fun `later account is not primary by default`() = runTest {
        coEvery { accountRepository.getBySenderId("AXIS") } returns null
        coEvery { accountRepository.count() } returns 3
        coEvery { accountRepository.insert(any()) } returns 99L
        coEvery { accountRepository.getById(99L) } returns existingAccount().copy(id = 99L, isPrimary = false)

        val result = service.resolve("AXIS")

        assertNotNull(result)
        assertFalse(result!!.isPrimary)
    }

    @Test
    fun `blank sender returns null`() = runTest {
        val result = service.resolve("")
        assertNull(result)
    }

    @Test
    fun `account suffix extracted from A-C marker`() = runTest {
        coEvery { accountRepository.getBySenderId("HDFCBK") } returns null
        coEvery { accountRepository.count() } returns 1
        coEvery { accountRepository.insert(any()) } returns 1L
        coEvery { accountRepository.getById(1L) } returns existingAccount().copy(id = 1L, isPrimary = false)
        coEvery { accountRepository.update(any()) } returns Unit
        coEvery { accountRepository.getBySenderId("HDFCBK") } returns null

        val result = service.resolveWithSuffix("HDFCBK", "Debited A/C xxxx1234 for Rs.500")

        assertNotNull(result)
        coVerify { accountRepository.update(match { it.accountSuffix == "1234" }) }
    }
}
