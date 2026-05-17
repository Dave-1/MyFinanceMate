package com.deepmoneytracker.domain.service

import com.deepmoneytracker.data.local.entity.CategoryEntity
import com.deepmoneytracker.data.local.entity.TransactionEntity
import com.deepmoneytracker.data.local.entity.TransactionType
import com.deepmoneytracker.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorizationEngine @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    // Default category keywords mapping
    private val defaultKeywords = mapOf(
        "Salary" to listOf("salary", "wages", "payroll", "stipend", "honorarium"),
        "Food" to listOf("food", "restaurant", "cafe", "zomato", "swiggy", "dominos", "pizza", "burger", "meal", "lunch", "dinner", "breakfast", "grocery", "groceries", "supermarket", "bigbasket", "dmart", "reliance fresh"),
        "Shopping" to listOf("shopping", "amazon", "flipkart", "myntra", "ajio", "mall", "store", "mart", "clothing", "fashion", "electronics", "lifestyle", "westside", "pantaloons"),
        "Transport" to listOf("transport", "uber", "ola", "rapido", "metro", "bus", "train", "flight", "petrol", "diesel", "fuel", "parking", "toll", "irctc", "makemytrip", "redbus"),
        "Bills" to listOf("bill", "electricity", "water", "gas", "internet", "broadband", "mobile", "recharge", "postpaid", "prepaid", "wifi", "dth", "tata sky", "dish tv", "jio", "airtel", "bsnl", "vodafone"),
        "Entertainment" to listOf("entertainment", "movie", "netflix", "prime", "hotstar", "spotify", "youtube", "gaming", "bookmyshow", "pvr", "inox", "concert", "show"),
        "Health" to listOf("health", "hospital", "doctor", "medicine", "pharmacy", "medical", "insurance", "apollo", "pharmeasy", "netmeds", "1mg", "clinic", "diagnostic", "lab", "test"),
        "Transfer" to listOf("transfer", "neft", "imps", "rtgs", "upi", "sent", "received", "paytm", "phonepe", "gpay", "google pay", "bhim"),
        "Investment" to listOf("investment", "mutual fund", "sip", "stocks", "shares", "trading", "zerodha", "groww", "upstox", "angel", "fd", "fixed deposit", "rd", "recurring deposit", "ppf", "nps", "lic")
    )

    suspend fun categorize(transaction: TransactionEntity): Long? {
        val categories = categoryRepository.getAllCategoriesList()
        val description = transaction.description.lowercase()
        val merchant = transaction.merchant.lowercase()
        val searchText = "$description $merchant"

        // First, check user-defined category keywords
        for (category in categories) {
            if (category.keywords.isNotBlank()) {
                val keywords = category.keywords.split(",").map { it.trim().lowercase() }
                for (keyword in keywords) {
                    if (keyword.isNotBlank() && searchText.contains(keyword)) {
                        return category.id
                    }
                }
            }
        }

        // Then, check default keywords
        for ((categoryName, keywords) in defaultKeywords) {
            for (keyword in keywords) {
                if (searchText.contains(keyword)) {
                    val category = categories.find { it.name.equals(categoryName, ignoreCase = true) }
                    if (category != null) return category.id
                }
            }
        }

        // Return "Other" category if found
        return categories.find { it.name.equals("Other", ignoreCase = true) }?.id
    }

    fun categorizeByKeywords(description: String, merchant: String, categories: List<CategoryEntity>): Long? {
        val searchText = "$description $merchant".lowercase()

        for (category in categories) {
            if (category.keywords.isNotBlank()) {
                val keywords = category.keywords.split(",").map { it.trim().lowercase() }
                for (keyword in keywords) {
                    if (keyword.isNotBlank() && searchText.contains(keyword)) {
                        return category.id
                    }
                }
            }
        }

        for ((categoryName, keywords) in defaultKeywords) {
            for (keyword in keywords) {
                if (searchText.contains(keyword)) {
                    val category = categories.find { it.name.equals(categoryName, ignoreCase = true) }
                    if (category != null) return category.id
                }
            }
        }

        return categories.find { it.name.equals("Other", ignoreCase = true) }?.id
    }
}
