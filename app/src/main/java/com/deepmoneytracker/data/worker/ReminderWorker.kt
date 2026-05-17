package com.deepmoneytracker.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepmoneytracker.R
import com.deepmoneytracker.domain.repository.ReminderRepository
import com.deepmoneytracker.domain.service.ReminderScheduler
import com.deepmoneytracker.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val CHANNEL_NAME = "Reminders"
    }

    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val dueReminders = reminderRepository.getDueReminders(currentTime)

        for (reminder in dueReminders) {
            showNotification(reminder.id, reminder.title, reminder.description, reminder.amount ?: 0.0)

            // Schedule next occurrence
            if (reminder.recurrence != com.deepmoneytracker.data.local.entity.Recurrence.NONE) {
                val nextTime = reminderScheduler.calculateNextTriggerTime(reminder.recurrence, currentTime)
                reminderRepository.update(reminder.copy(nextTriggerTime = nextTime, updatedAt = currentTime))
                reminderScheduler.schedule(reminder.copy(nextTriggerTime = nextTime))
            } else {
                reminderRepository.update(reminder.copy(isActive = false, updatedAt = currentTime))
            }
        }

        return Result.success()
    }

    private fun showNotification(id: Long, title: String, description: String, amount: Double) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (amount > 0) {
            "$description\nAmount: ₹${"%.2f".format(amount)}"
        } else {
            description
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
