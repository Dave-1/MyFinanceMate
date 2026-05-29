package com.myfinancemate.di

import com.myfinancemate.data.local.entity.CategoryEntity
import com.myfinancemate.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend fun initializeDefaults() {
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
}
