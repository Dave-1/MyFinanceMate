package com.myfinancemate.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myfinancemate.domain.repository.ReminderRepository
import com.myfinancemate.domain.service.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = reminderRepository.getAllRemindersList()
                for (reminder in reminders) {
                    if (reminder.isActive) {
                        reminderScheduler.schedule(reminder)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
