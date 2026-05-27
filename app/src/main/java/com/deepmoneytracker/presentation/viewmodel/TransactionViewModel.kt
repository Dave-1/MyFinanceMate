package com.deepmoneytracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepmoneytracker.data.local.entity.CategoryEntity
import com.deepmoneytracker.data.local.entity.TransactionEntity
import com.deepmoneytracker.data.local.entity.TransactionType
import com.deepmoneytracker.domain.repository.CategoryRepository
import com.deepmoneytracker.domain.repository.SmsRuleRepository
import com.deepmoneytracker.domain.repository.TransactionRepository
import com.deepmoneytracker.domain.service.CategorizationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val categorizationEngine: CategorizationEngine,
    private val smsRuleRepository: SmsRuleRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType

    val transactions: StateFlow<List<TransactionEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            transactionRepository.getAllTransactions()
        } else {
            transactionRepository.search(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bankNames: StateFlow<List<String>> = smsRuleRepository.getAllRules()
        .map { rules -> rules.map { it.senderName }.filter { it.isNotBlank() }.distinct() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TransactionType?) {
        _filterType.value = type
    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        description: String,
        merchant: String = "",
        categoryId: Long? = null,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                amount = amount,
                type = type,
                description = description,
                merchant = merchant,
                date = date
            )
            val id = transactionRepository.insert(transaction)

            val resolvedCategoryId = categoryId ?: categorizationEngine.categorize(transaction)
            if (resolvedCategoryId != null) {
                transactionRepository.update(transaction.copy(id = id, categoryId = resolvedCategoryId))
            }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.update(transaction.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteById(id)
        }
    }
}
