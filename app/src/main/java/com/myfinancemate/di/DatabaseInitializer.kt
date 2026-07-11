package com.myfinancemate.di

import com.myfinancemate.data.local.entity.CategoryEntity
import com.myfinancemate.data.local.entity.SmsRuleEntity
import com.myfinancemate.domain.repository.CategoryRepository
import com.myfinancemate.domain.repository.SmsRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val smsRuleRepository: SmsRuleRepository
) {
    suspend fun initializeDefaults() {
        initializeCategories()
        initializeSmsRules()
    }

    private suspend fun initializeCategories() {
        if (categoryRepository.getCount() > 0) return

        val defaultCategories = listOf(
            CategoryEntity(name = "Salary", icon = "ic_salary", color = "#4CAF50", isDefault = true, keywords = "salary,wages,payroll"),
            CategoryEntity(name = "Food", icon = "ic_food", color = "#FF9800", isDefault = true, keywords = "food,restaurant,cafe,zomato,swiggy"),
            CategoryEntity(name = "Shopping", icon = "ic_shopping", color = "#E91E63", isDefault = true, keywords = "shopping,amazon,flipkart,myntra"),
            CategoryEntity(name = "Transport", icon = "ic_transport", color = "#2196F3", isDefault = true, keywords = "transport,uber,ola,metro,bus,petrol"),
            CategoryEntity(name = "Bills", icon = "ic_bills", color = "#F44336", isDefault = true, keywords = "bill,electricity,water,gas,internet,recharge"),
            CategoryEntity(name = "Entertainment", icon = "ic_entertainment", color = "#9C27B0", isDefault = true, keywords = "entertainment,movie,netflix,prime,hotstar"),
            CategoryEntity(name = "Health", icon = "ic_health", color = "#00BCD4", isDefault = true, keywords = "health,hospital,doctor,medicine,pharmacy"),
            CategoryEntity(name = "Transfer", icon = "ic_transfer", color = "#607D8B", isDefault = true, keywords = "transfer,neft,imps,rtgs,upi,paytm,phonepe"),
            CategoryEntity(name = "Investment", icon = "ic_investment", color = "#795548", isDefault = true, keywords = "investment,mutual fund,sip,stocks,zerodha,groww"),
            CategoryEntity(name = "Other", icon = "ic_category", color = "#9E9E9E", isDefault = true, keywords = "")
        )

        categoryRepository.insertAll(defaultCategories)
    }

    private suspend fun initializeSmsRules() {
        if (smsRuleRepository.getCount() > 0) return

        val defaultRules = listOf(
            SmsRuleEntity(senderId = "HDFCBK", senderName = "HDFC Bank"),
            SmsRuleEntity(senderId = "SBIBANK", senderName = "SBI Bank"),
            SmsRuleEntity(senderId = "ICICIBK", senderName = "ICICI Bank"),
            SmsRuleEntity(senderId = "AXISBANK", senderName = "Axis Bank"),
            SmsRuleEntity(senderId = "YESBANK", senderName = "Yes Bank"),
            SmsRuleEntity(senderId = "KOTAKB", senderName = "Kotak Mahindra"),
            SmsRuleEntity(senderId = "INDUSIND", senderName = "IndusInd Bank"),
            SmsRuleEntity(senderId = "IDBIBK", senderName = "IDBI Bank"),
            SmsRuleEntity(senderId = "FEDBANK", senderName = "Federal Bank"),
            SmsRuleEntity(senderId = "PNB", senderName = "Punjab National Bank"),
            SmsRuleEntity(senderId = "BOB", senderName = "Bank of Baroda"),
            SmsRuleEntity(senderId = "CANBANK", senderName = "Canara Bank"),
            SmsRuleEntity(senderId = "UBIBANK", senderName = "Union Bank of India"),
            SmsRuleEntity(senderId = "RBLBANK", senderName = "RBL Bank"),
            SmsRuleEntity(senderId = "DBSBANK", senderName = "DBS Bank"),
            SmsRuleEntity(senderId = "SOIBANK", senderName = "South Indian Bank"),
            SmsRuleEntity(senderId = "BANDHAN", senderName = "Bandhan Bank"),
            SmsRuleEntity(senderId = "PAYTM", senderName = "Paytm Payments Bank"),
            SmsRuleEntity(senderId = "AIRTEL", senderName = "Airtel Payments Bank"),
            SmsRuleEntity(senderId = "JIOBANK", senderName = "Jio Payments Bank")
        )

        smsRuleRepository.insertAll(defaultRules)
    }
}
