package com.deepmoneytracker.domain.service

import android.content.Context
import com.deepmoneytracker.data.local.entity.CategoryEntity
import com.deepmoneytracker.data.local.entity.Recurrence
import com.deepmoneytracker.data.local.entity.ReminderEntity
import com.deepmoneytracker.data.local.entity.ReminderType
import com.deepmoneytracker.data.local.entity.SmsRuleEntity
import com.deepmoneytracker.data.local.entity.TransactionEntity
import com.deepmoneytracker.data.local.entity.TransactionType
import com.deepmoneytracker.domain.repository.CategoryRepository
import com.deepmoneytracker.domain.repository.ReminderRepository
import com.deepmoneytracker.domain.repository.SmsRuleRepository
import com.deepmoneytracker.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.OutputStream
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val smsRuleRepository: SmsRuleRepository
) {
    data class BackupData(
        val transactions: List<TransactionEntity>,
        val categories: List<CategoryEntity>,
        val reminders: List<ReminderEntity>,
        val smsRules: List<SmsRuleEntity>
    )

    suspend fun exportToXml(outputStream: OutputStream) {
        val transactions = transactionRepository.getAllTransactionsList()
        val categories = categoryRepository.getAllCategoriesList()
        val reminders = reminderRepository.getAllRemindersList()
        val smsRules = smsRuleRepository.getAllRulesList()

        val serializer = XmlPullParserFactory.newInstance().newSerializer()
        val writer = StringWriter()
        serializer.setOutput(writer)

        serializer.startDocument("UTF-8", true)
        serializer.startTag("", "backup")
        serializer.attribute("", "version", "1")
        serializer.attribute("", "timestamp", System.currentTimeMillis().toString())

        // Categories
        serializer.startTag("", "categories")
        for (cat in categories) {
            serializer.startTag("", "category")
            serializer.attribute("", "id", cat.id.toString())
            serializer.attribute("", "name", cat.name)
            serializer.attribute("", "icon", cat.icon)
            serializer.attribute("", "color", cat.color)
            serializer.attribute("", "isDefault", cat.isDefault.toString())
            serializer.attribute("", "keywords", cat.keywords)
            serializer.endTag("", "category")
        }
        serializer.endTag("", "categories")

        // Transactions
        serializer.startTag("", "transactions")
        for (txn in transactions) {
            serializer.startTag("", "transaction")
            serializer.attribute("", "id", txn.id.toString())
            serializer.attribute("", "amount", txn.amount.toString())
            serializer.attribute("", "type", txn.type.name)
            serializer.attribute("", "categoryId", txn.categoryId?.toString() ?: "")
            serializer.attribute("", "description", txn.description)
            serializer.attribute("", "merchant", txn.merchant)
            serializer.attribute("", "senderInfo", txn.senderInfo)
            serializer.attribute("", "date", txn.date.toString())
            serializer.attribute("", "isFromSms", txn.isFromSms.toString())
            serializer.attribute("", "smsBody", txn.smsBody)
            serializer.endTag("", "transaction")
        }
        serializer.endTag("", "transactions")

        // Reminders
        serializer.startTag("", "reminders")
        for (rem in reminders) {
            serializer.startTag("", "reminder")
            serializer.attribute("", "id", rem.id.toString())
            serializer.attribute("", "title", rem.title)
            serializer.attribute("", "description", rem.description)
            serializer.attribute("", "amount", rem.amount?.toString() ?: "")
            serializer.attribute("", "type", rem.type.name)
            serializer.attribute("", "recurrence", rem.recurrence.name)
            serializer.attribute("", "nextTriggerTime", rem.nextTriggerTime.toString())
            serializer.attribute("", "isActive", rem.isActive.toString())
            serializer.endTag("", "reminder")
        }
        serializer.endTag("", "reminders")

        // SMS Rules
        serializer.startTag("", "sms_rules")
        for (rule in smsRules) {
            serializer.startTag("", "sms_rule")
            serializer.attribute("", "id", rule.id.toString())
            serializer.attribute("", "senderId", rule.senderId)
            serializer.attribute("", "senderName", rule.senderName)
            serializer.attribute("", "isActive", rule.isActive.toString())
            serializer.endTag("", "sms_rule")
        }
        serializer.endTag("", "sms_rules")

        serializer.endTag("", "backup")
        serializer.endDocument()

        outputStream.write(writer.toString().toByteArray())
    }

    suspend fun importFromXml(inputStream: InputStream): Result<BackupData> {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            val transactions = mutableListOf<TransactionEntity>()
            val categories = mutableListOf<CategoryEntity>()
            val reminders = mutableListOf<ReminderEntity>()
            val smsRules = mutableListOf<SmsRuleEntity>()

            var eventType = parser.eventType
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name ?: ""
                        when (currentTag) {
                            "category" -> categories.add(parseCategory(parser))
                            "transaction" -> transactions.add(parseTransaction(parser))
                            "reminder" -> reminders.add(parseReminder(parser))
                            "sms_rule" -> smsRules.add(parseSmsRule(parser))
                        }
                    }
                }
                eventType = parser.next()
            }

            // Clear existing data and insert new
            transactionRepository.deleteAll()
            reminderRepository.deleteAll()
            smsRuleRepository.deleteAll()

            for (cat in categories) {
                categoryRepository.insert(cat.copy(id = 0))
            }
            for (txn in transactions) {
                transactionRepository.insert(txn.copy(id = 0))
            }
            for (rem in reminders) {
                reminderRepository.insert(rem.copy(id = 0))
            }
            for (rule in smsRules) {
                smsRuleRepository.insert(rule.copy(id = 0))
            }

            Result.success(BackupData(transactions, categories, reminders, smsRules))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCategory(parser: XmlPullParser): CategoryEntity {
        return CategoryEntity(
            id = parser.getAttributeValue("", "id")?.toLongOrNull() ?: 0,
            name = parser.getAttributeValue("", "name") ?: "",
            icon = parser.getAttributeValue("", "icon") ?: "ic_category",
            color = parser.getAttributeValue("", "color") ?: "#6750A4",
            isDefault = parser.getAttributeValue("", "isDefault")?.toBooleanStrictOrNull() ?: false,
            keywords = parser.getAttributeValue("", "keywords") ?: ""
        )
    }

    private fun parseTransaction(parser: XmlPullParser): TransactionEntity {
        return TransactionEntity(
            id = parser.getAttributeValue("", "id")?.toLongOrNull() ?: 0,
            amount = parser.getAttributeValue("", "amount")?.toDoubleOrNull() ?: 0.0,
            type = try { TransactionType.valueOf(parser.getAttributeValue("", "type") ?: "EXPENSE") } catch (e: Exception) { TransactionType.EXPENSE },
            categoryId = parser.getAttributeValue("", "categoryId")?.toLongOrNull(),
            description = parser.getAttributeValue("", "description") ?: "",
            merchant = parser.getAttributeValue("", "merchant") ?: "",
            senderInfo = parser.getAttributeValue("", "senderInfo") ?: "",
            date = parser.getAttributeValue("", "date")?.toLongOrNull() ?: System.currentTimeMillis(),
            isFromSms = parser.getAttributeValue("", "isFromSms")?.toBooleanStrictOrNull() ?: false,
            smsBody = parser.getAttributeValue("", "smsBody") ?: ""
        )
    }

    private fun parseReminder(parser: XmlPullParser): ReminderEntity {
        return ReminderEntity(
            id = parser.getAttributeValue("", "id")?.toLongOrNull() ?: 0,
            title = parser.getAttributeValue("", "title") ?: "",
            description = parser.getAttributeValue("", "description") ?: "",
            amount = parser.getAttributeValue("", "amount")?.toDoubleOrNull(),
            type = try { ReminderType.valueOf(parser.getAttributeValue("", "type") ?: "EXPENSE") } catch (e: Exception) { ReminderType.EXPENSE },
            recurrence = try { Recurrence.valueOf(parser.getAttributeValue("", "recurrence") ?: "NONE") } catch (e: Exception) { Recurrence.NONE },
            nextTriggerTime = parser.getAttributeValue("", "nextTriggerTime")?.toLongOrNull() ?: System.currentTimeMillis(),
            isActive = parser.getAttributeValue("", "isActive")?.toBooleanStrictOrNull() ?: true
        )
    }

    private fun parseSmsRule(parser: XmlPullParser): SmsRuleEntity {
        return SmsRuleEntity(
            id = parser.getAttributeValue("", "id")?.toLongOrNull() ?: 0,
            senderId = parser.getAttributeValue("", "senderId") ?: "",
            senderName = parser.getAttributeValue("", "senderName") ?: "",
            isActive = parser.getAttributeValue("", "isActive")?.toBooleanStrictOrNull() ?: true
        )
    }
}
