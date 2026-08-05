package com.myfinancemate.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.domain.repository.SmsRuleRepository
import com.myfinancemate.domain.repository.TransactionRepository
import com.myfinancemate.domain.service.AccountResolutionService
import com.myfinancemate.domain.service.CategorizationEngine
import com.myfinancemate.domain.service.FixedExpenseDetector
import com.myfinancemate.domain.service.ReminderFromSmsCreator
import com.myfinancemate.domain.service.SalaryDetector
import com.myfinancemate.domain.service.SmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var smsParser: SmsParser
    @Inject lateinit var categorizationEngine: CategorizationEngine
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var smsRuleRepository: SmsRuleRepository
    @Inject lateinit var reminderFromSmsCreator: ReminderFromSmsCreator
    @Inject lateinit var accountResolutionService: AccountResolutionService
    @Inject lateinit var salaryDetector: SalaryDetector
    @Inject lateinit var fixedExpenseDetector: FixedExpenseDetector

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val pendingResult = goAsync()

        scope.launch {
            try {
                val activeRules = smsRuleRepository.getActiveRules()
                val activeSenderIds = activeRules.map { it.senderId.uppercase() }

                for (smsMessage in messages) {
                    val sender = smsMessage.displayOriginatingAddress ?: continue
                    val body = smsMessage.messageBody ?: continue

                    // Check if sender matches any active rule
                    val isRegistered = activeSenderIds.any { senderId ->
                        sender.uppercase().contains(senderId) || senderId.contains(sender.uppercase())
                    }

                    if (!isRegistered) continue

                    // Try to parse as transaction
                    val parsed = smsParser.parse(body, sender, smsMessage.timestampMillis)

                    if (parsed != null) {
                        // Resolve/auto-create account for this sender
                        val account = accountResolutionService.resolveWithSuffix(sender, body)

                        val baseTransaction = TransactionEntity(
                            amount = parsed.amount,
                            type = parsed.type,
                            description = parsed.description,
                            merchant = parsed.merchant,
                            senderInfo = parsed.senderInfo,
                            date = parsed.date,
                            isFromSms = true,
                            smsBody = body,
                            accountId = account?.id
                        )

                        // Salary detection: only INCOME can be salary
                        val isSalary = parsed.type == TransactionType.INCOME &&
                            salaryDetector.isSalary(sender, parsed.merchant)
                        val transaction = baseTransaction.copy(isSalary = isSalary)

                        val id = transactionRepository.insert(transaction)

                        // Categorize
                        val categoryId = categorizationEngine.categorize(transaction)
                        if (categoryId != null) {
                            transactionRepository.update(transaction.copy(id = id, categoryId = categoryId))
                        }

                        // Fixed-expense auto-detection on new EXPENSE
                        if (parsed.type == TransactionType.EXPENSE) {
                            fixedExpenseDetector.runDetectionForTransaction(transaction.copy(id = id))
                        }
                    }

                    // Also create reminder if SMS contains due/expiry keywords
                    reminderFromSmsCreator.maybeCreateReminder(
                        smsBody = body,
                        senderId = sender,
                        smsTimestamp = System.currentTimeMillis(),
                        amount = parsed?.amount
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
