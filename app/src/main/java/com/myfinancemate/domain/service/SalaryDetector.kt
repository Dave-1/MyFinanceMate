package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.SmsRuleEntity
import com.myfinancemate.data.local.entity.SmsRuleType
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.domain.repository.SmsRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether a parsed INCOME transaction matches a SALARY rule.
 *
 * Salary rules are regular SmsRuleEntity rows whose [SmsRuleEntity.ruleType]
 * is SALARY — the user creates them in the existing SMS Rules UI. A match is
 * a sender that has an active SALARY rule; optionally the rule's senderName
 * (bank name) can be cross-checked against the transaction.
 */
@Singleton
class SalaryDetector @Inject constructor(
    private val smsRuleRepository: SmsRuleRepository
) {

    /**
     * Returns true when the transaction is INCOME and its sender has an
     * active SALARY rule. When no rules exist, every income is treated as
     * non-salary (false).
     */
    suspend fun isSalary(txn: TransactionEntity): Boolean {
        if (txn.type != TransactionType.INCOME) return false

        val rules = smsRuleRepository.getSalaryRules()
        if (rules.isEmpty()) return false

        val sender = txn.senderInfo.ifBlank { return false }
        return rules.any { rule ->
            rule.isActive && matchesRule(rule, sender, txn)
        }
    }

    suspend fun isSalary(senderId: String, merchant: String): Boolean {
        val rules = smsRuleRepository.getSalaryRules()
        if (rules.isEmpty()) return false
        return rules.any { rule ->
            rule.isActive && matchesSender(rule, senderId, merchant)
        }
    }

    private fun matchesRule(rule: SmsRuleEntity, senderId: String, txn: TransactionEntity): Boolean {
        if (!matchesSender(rule, senderId, txn.merchant)) return false
        // Optional heuristic: salary tends to be a credit with a round-ish amount.
        // Only applied when the rule has no explicit amount, so we don't reject
        // real salaries that differ slightly.
        return true
    }

    private fun matchesSender(rule: SmsRuleEntity, senderId: String, merchant: String): Boolean {
        val ruleKey = rule.senderId.trim().lowercase()
        if (ruleKey.isBlank()) return false

        val senderKey = senderId.trim().lowercase()
        if (senderKey.isNotEmpty() && (senderKey == ruleKey || senderKey.contains(ruleKey) || ruleKey.contains(senderKey))) {
            return true
        }

        val merchantKey = merchant.trim().lowercase()
        if (merchantKey.isNotEmpty() && rule.senderName.isNotBlank()) {
            val ruleName = rule.senderName.trim().lowercase()
            if (merchantKey.contains(ruleName) || ruleName.contains(merchantKey)) return true
        }
        return false
    }
}
