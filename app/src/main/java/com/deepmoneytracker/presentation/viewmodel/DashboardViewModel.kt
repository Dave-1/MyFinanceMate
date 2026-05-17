package com.deepmoneytracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepmoneytracker.data.local.entity.CategoryEntity
import com.deepmoneytracker.data.local.entity.TransactionEntity
import com.deepmoneytracker.data.local.entity.TransactionType
import com.deepmoneytracker.domain.repository.CategoryRepository
import com.deepmoneytracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DashboardState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categoryTotals: List<CategoryTotalUi> = emptyList(),
    val isLoading: Boolean = true
)

data class CategoryTotalUi(
    val category: CategoryEntity?,
    val total: Double,
    val percentage: Float
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private val monthStart = calendar.timeInMillis

    private val endCalendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, 1)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private val monthEnd = endCalendar.timeInMillis

    val state: StateFlow<DashboardState> = combine(
        transactionRepository.getTotalByTypeAndDateRange(TransactionType.INCOME, monthStart, monthEnd),
        transactionRepository.getTotalByTypeAndDateRange(TransactionType.EXPENSE, monthStart, monthEnd),
        transactionRepository.getRecent(10),
        transactionRepository.getCategoryWiseTotal(TransactionType.EXPENSE, monthStart, monthEnd)
    ) { income, expense, recent, categoryTotals ->
        val totalIncome = income ?: 0.0
        val totalExpense = expense ?: 0.0
        val categories = categoryRepository.getAllCategoriesList()
        val total = categoryTotals.sumOf { it.total }

        DashboardState(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = totalIncome - totalExpense,
            recentTransactions = recent,
            categoryTotals = categoryTotals.map { ct ->
                CategoryTotalUi(
                    category = categories.find { it.id == ct.categoryId },
                    total = ct.total,
                    percentage = if (total > 0) (ct.total / total * 100).toFloat() else 0f
                )
            },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
}
