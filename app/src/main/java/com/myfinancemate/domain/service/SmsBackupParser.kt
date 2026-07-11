package com.myfinancemate.domain.service

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.myfinancemate.data.local.entity.SmsNotificationEntity
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.domain.repository.SmsNotificationRepository
import com.myfinancemate.domain.repository.TransactionRepository
import com.myfinancemate.domain.repository.SmsRuleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BackupResult(
    val totalSms: Int,
    val bankTransactions: Int,
    val notifications: Int,
    val bankBreakdown: Map<String, Int>, // senderName -> count
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SmsItem(
    val address: String,
    val body: String,
    val date: Long,
    val type: Int // 1 = received, 2 = sent
)

@Singleton
class SmsBackupParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsParser: SmsParser,
    private val smsRuleRepository: SmsRuleRepository,
    private val transactionRepository: TransactionRepository,
    private val categorizationEngine: CategorizationEngine,
    private val smsNotificationRepository: SmsNotificationRepository,
    private val smsNotificationClassifier: SmsNotificationClassifier,
    private val reminderFromSmsCreator: ReminderFromSmsCreator
) {
    private val backupDir: File
        get() = File(context.getExternalFilesDir(null), "sms_backups").also { it.mkdirs() }

    suspend fun backupAndParse(
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): BackupResult = withContext(Dispatchers.IO) {
        // 1. Load active rules
        val rules = smsRuleRepository.getActiveRules()
        val senderIdToName = rules.associate { it.senderId.uppercase() to it.senderName }
        val activeSenderIds = rules.map { it.senderId.uppercase() }

        // 2. Read ALL SMS from device
        val allSms = readAllSms()
        val totalSms = allSms.size

        // 3. Write XML backup
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "sms_backup_${dateFormat.format(Date(timestamp))}.xml"
        val xmlFile = File(backupDir, fileName)
        writeXmlBackup(xmlFile, allSms)

        // 4. Parse bank SMS and create transactions
        var processed = 0
        var bankTransactions = 0
        var notifications = 0
        val bankBreakdown = mutableMapOf<String, Int>()
        val processedBodies = mutableSetOf<String>() // Track which SMS were bank transactions

        for (sms in allSms) {
            processed++
            if (processed % 50 == 0 || processed == totalSms) {
                onProgress(processed, totalSms)
            }

            // Validate SMS
            if (!validateSms(sms)) continue

            // Match sender against active rules
            val matchedSenderId = matchSender(sms.address, activeSenderIds)

            if (matchedSenderId != null) {
                // This is a bank SMS — parse as transaction
                if (transactionRepository.existsBySmsBody(sms.body)) {
                    processedBodies.add(sms.body)
                    continue
                }

                val parsed = smsParser.parse(sms.body, sms.address)
                if (parsed != null && validateParsedTransaction(parsed)) {
                    val transaction = TransactionEntity(
                        amount = parsed.amount,
                        type = parsed.type,
                        description = parsed.description,
                        merchant = parsed.merchant,
                        senderInfo = parsed.senderInfo,
                        date = parsed.date,
                        isFromSms = true,
                        smsBody = sms.body
                    )
                    val id = transactionRepository.insert(transaction)
                    try {
                        val categoryId = categorizationEngine.categorize(transaction)
                        if (categoryId != null) {
                            transactionRepository.update(transaction.copy(id = id, categoryId = categoryId))
                        }
                    } catch (_: Exception) {}
                    reminderFromSmsCreator.maybeCreateReminder(
                        smsBody = sms.body,
                        senderId = sms.address,
                        smsTimestamp = sms.date,
                        amount = parsed.amount
                    )
                    bankTransactions++
                    val bankName = senderIdToName[matchedSenderId] ?: sms.address
                    bankBreakdown[bankName] = (bankBreakdown[bankName] ?: 0) + 1
                }
                processedBodies.add(sms.body)
            }
        }

        // 5. Second pass — classify non-bank SMS as notifications
        for (sms in allSms) {
            if (sms.body in processedBodies) continue
            if (!validateSms(sms)) continue

            // Check duplicate
            if (smsNotificationRepository.existsByBody(sms.body)) continue

            val category = smsNotificationClassifier.classify(sms.body)
            val isExpired = smsNotificationClassifier.isExpiredNotification(sms.body, sms.date)

            val notification = SmsNotificationEntity(
                sender = sms.address,
                body = sms.body,
                smsDate = sms.date,
                category = category,
                isExpired = isExpired
            )
            smsNotificationRepository.insert(notification)
            reminderFromSmsCreator.maybeCreateReminder(
                smsBody = sms.body,
                senderId = sms.address,
                smsTimestamp = sms.date
            )
            notifications++
        }

        onProgress(totalSms, totalSms)

        BackupResult(
            totalSms = totalSms,
            bankTransactions = bankTransactions,
            notifications = notifications,
            bankBreakdown = bankBreakdown,
            filePath = xmlFile.absolutePath,
            timestamp = timestamp
        )
    }

    private fun readAllSms(): List<SmsItem> {
        val smsList = mutableListOf<SmsItem>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        context.contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} ASC")?.use { cursor ->
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (cursor.moveToNext()) {
                smsList.add(
                    SmsItem(
                        address = cursor.getString(addressIdx) ?: "",
                        body = cursor.getString(bodyIdx) ?: "",
                        date = cursor.getLong(dateIdx),
                        type = cursor.getInt(typeIdx)
                    )
                )
            }
        }

        return smsList
    }

    private fun writeXmlBackup(file: File, smsList: List<SmsItem>) {
        file.bufferedWriter().use { writer ->
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            writer.write("<smses count=\"${smsList.size}\">\n")
            for (sms in smsList) {
                val escapedBody = xmlEscape(sms.body)
                val escapedAddress = xmlEscape(sms.address)
                writer.write("<sms protocol=\"0\" address=\"$escapedAddress\" date=\"${sms.date}\" type=\"${sms.type}\" body=\"$escapedBody\"/>\n")
            }
            writer.write("</smses>")
        }
    }

    private fun xmlEscape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun validateSms(sms: SmsItem): Boolean {
        if (sms.body.isBlank()) return false
        if (sms.address.isBlank()) return false
        // Date must be within last 10 years and not in the future
        val tenYearsAgo = System.currentTimeMillis() - (10L * 365 * 24 * 60 * 60 * 1000)
        if (sms.date < tenYearsAgo || sms.date > System.currentTimeMillis() + 86400000) return false
        return true
    }

    private fun validateParsedTransaction(parsed: ParsedTransaction): Boolean {
        // Amount must be positive and reasonable (max ₹1 Crore)
        if (parsed.amount <= 0 || parsed.amount > 10_000_000) return false
        return true
    }

    private fun matchSender(sender: String, activeSenderIds: List<String>): String? {
        val upperSender = sender.uppercase()
        return activeSenderIds.firstOrNull { ruleId ->
            upperSender.contains(ruleId) || ruleId.contains(upperSender)
        }
    }

    fun getBackupFiles(): List<File> {
        return backupDir.listFiles()?.filter { it.extension == "xml" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getLastBackupTimestamp(): Long {
        val prefs = context.getSharedPreferences("sms_backup_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_backup_timestamp", 0)
    }

    fun saveBackupResult(result: BackupResult) {
        val prefs = context.getSharedPreferences("sms_backup_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_backup_timestamp", result.timestamp)
            .putInt("last_backup_sms_count", result.totalSms)
            .putInt("last_backup_txn_count", result.bankTransactions)
            .putString("last_backup_file_path", result.filePath)
            .apply()
    }

    fun getLastBackupInfo(): BackupInfo? {
        val prefs = context.getSharedPreferences("sms_backup_prefs", Context.MODE_PRIVATE)
        val timestamp = prefs.getLong("last_backup_timestamp", 0)
        if (timestamp == 0L) return null
        return BackupInfo(
            timestamp = timestamp,
            smsCount = prefs.getInt("last_backup_sms_count", 0),
            transactionCount = prefs.getInt("last_backup_txn_count", 0),
            filePath = prefs.getString("last_backup_file_path", "") ?: ""
        )
    }

    fun isBackupNeeded(): Boolean {
        val timestamp = getLastBackupTimestamp()
        if (timestamp == 0L) return true
        val twoDaysMs = 2L * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - timestamp > twoDaysMs
    }

    fun getBackupAgeDays(): Int {
        val timestamp = getLastBackupTimestamp()
        if (timestamp == 0L) return -1
        return ((System.currentTimeMillis() - timestamp) / (24 * 60 * 60 * 1000)).toInt()
    }
}

data class BackupInfo(
    val timestamp: Long,
    val smsCount: Int,
    val transactionCount: Int,
    val filePath: String
)
