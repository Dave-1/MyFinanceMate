package com.myfinancemate.domain.service

import android.util.Log
import com.myfinancemate.data.local.entity.AccountEntity
import com.myfinancemate.domain.repository.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves an SMS sender ID to an AccountEntity.
 *
 * If no account exists for the sender, one is auto-created (name defaults to
 * the sender ID). The first-ever account becomes primary automatically.
 * The user can rename accounts and override the primary later in AccountsScreen.
 */
@Singleton
class AccountResolutionService @Inject constructor(
    private val accountRepository: AccountRepository
) {

    suspend fun resolve(senderId: String): AccountEntity? {
        if (senderId.isBlank()) return null

        val existing = accountRepository.getBySenderId(senderId)
        if (existing != null) return existing

        // First-ever account auto-becomes primary.
        val isFirst = accountRepository.count() == 0

        val account = AccountEntity(
            name = senderId,
            bankName = "",
            senderId = senderId,
            accountSuffix = "",
            isPrimary = isFirst
        )
        val id = accountRepository.insert(account)
        Log.d(TAG, "Auto-created account '$senderId' id=$id isPrimary=$isFirst")
        return accountRepository.getById(id)
    }

    suspend fun resolveWithSuffix(senderId: String, body: String): AccountEntity? {
        val base = resolve(senderId) ?: return null
        if (base.accountSuffix.isNotEmpty()) return base

        val suffix = extractSuffix(body)
        if (suffix != null) {
            accountRepository.update(base.copy(accountSuffix = suffix))
            return accountRepository.getById(base.id)
        }
        return base
    }

    private fun extractSuffix(body: String): String? {
        // "A/C xxxx1234", "ac no. 1234", "...1234" (last 4 digits after "acc")
        val patterns = listOf(
            Regex("""(?i)(?:a/c|acct|account|acc)\s*(?:no\.?|number)?\s*[:.\s]*([0-9]{3,4})"""),
            Regex("""(?i)(?:a/c|acct|account|acc)\s*(?:no\.?|number)?\s*[:.\s]*x+([0-9]{4})"""),
            Regex("""(?i)(?:a/c|acct|account|acc)[\s#.-]*([0-9]{4})\b""")
        )
        for (p in patterns) {
            val m = p.find(body) ?: continue
            val digits = m.groupValues[1].filter { it.isDigit() }
            if (digits.length >= 4) return digits.takeLast(4)
            if (digits.length == 3) return digits // tolerate 3-digit suffix
        }
        return null
    }

    companion object {
        private const val TAG = "AccountResolutionService"
    }
}
