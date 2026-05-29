package com.myfinancemate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myfinancemate.data.local.entity.Recurrence
import com.myfinancemate.data.local.entity.ReminderEntity
import com.myfinancemate.data.local.entity.ReminderType
import com.myfinancemate.domain.repository.ReminderRepository
import com.myfinancemate.domain.service.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val reminders: StateFlow<List<ReminderEntity>> = reminderRepository.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReminder(
        title: String,
        description: String,
        amount: Double?,
        type: ReminderType,
        recurrence: Recurrence,
        triggerTime: Long
    ) {
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = title,
                description = description,
                amount = amount,
                type = type,
                recurrence = recurrence,
                nextTriggerTime = triggerTime
            )
            val id = reminderRepository.insert(reminder)
            reminderScheduler.schedule(reminder.copy(id = id))
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepository.update(reminder.copy(updatedAt = System.currentTimeMillis()))
            if (reminder.isActive) {
                reminderScheduler.schedule(reminder)
            } else {
                reminderScheduler.cancel(reminder.id)
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderScheduler.cancel(reminder.id)
            reminderRepository.delete(reminder)
        }
    }

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isActive = !reminder.isActive, updatedAt = System.currentTimeMillis())
            reminderRepository.update(updated)
            if (updated.isActive) {
                reminderScheduler.schedule(updated)
            } else {
                reminderScheduler.cancel(updated.id)
            }
        }
    }
}
