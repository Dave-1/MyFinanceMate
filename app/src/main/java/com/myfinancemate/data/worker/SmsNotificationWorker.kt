package com.myfinancemate.data.worker

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
import com.myfinancemate.R
import com.myfinancemate.data.local.entity.SmsNotificationEntity
import com.myfinancemate.domain.repository.SmsNotificationRepository
import com.myfinancemate.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SmsNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsNotificationRepository: SmsNotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "sms_notification_channel"
        const val CHANNEL_NAME = "SMS Notifications"
        const val WORK_NAME = "sms_notification_check"
    }

    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()

        // Get all expired notifications that haven't been notified yet
        val expiredNotifications = smsNotificationRepository.getExpiredNotifications().first()

        for (notification in expiredNotifications) {
            if (!notification.isRead) {
                showNotification(notification)
                // Mark as read so we don't re-notify
                smsNotificationRepository.markAsRead(notification.id)
            }
        }

        // Also check for upcoming notifications (within next 24 hours)
        val allNotifications = smsNotificationRepository.getAll().first()
        val upcoming = allNotifications.filter {
            !it.isRead &&
            it.smsDate > currentTime &&
            it.smsDate < currentTime + 24 * 60 * 60 * 1000 &&
            (it.category.name == "RECHARGE" || it.category.name == "EXPIRY")
        }

        for (notification in upcoming) {
            showUpcomingNotification(notification)
        }

        return Result.success()
    }

    private fun showNotification(notification: SmsNotificationEntity) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (notification.category.name) {
            "RECHARGE" -> "Recharge Reminder"
            "EXPIRY" -> "Expiry Alert"
            else -> "SMS Notification"
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(notification.body.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notification.id.toInt(), notificationBuilder.build())
    }

    private fun showUpcomingNotification(notification: SmsNotificationEntity) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (notification.id + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming: ${notification.sender}")
            .setContentText(notification.body.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((notification.id + 100000).toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "SMS-related notifications for recharges, expiry alerts, and upcoming events"
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
