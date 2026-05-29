package com.myfinancemate.domain.service

import com.myfinancemate.data.local.entity.SmsNotificationCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsNotificationClassifier @Inject constructor() {

    private val otpKeywords = listOf(
        "otp", "one time password", "verification code", "verification pin",
        "code is", "is your otp", "do not share", "authenticate"
    )

    private val rechargeKeywords = listOf(
        "recharge", "recharged", "top-up", "top up", "data pack",
        "plan activated", "pack activated", "talktime", "validity extended",
        "recharge successful", "prepaid recharge"
    )

    private val expiryKeywords = listOf(
        "expired", "expiry", "expiring", "will expire", "expires on",
        "due date", "renewal", "renew", "overdue", "payment due",
        "insurance due", "emi due", "subscription end",
        "validity end", "plan expire", "will be deactivated"
    )

    private val promotionKeywords = listOf(
        "offer", "cashback", "discount", "coupon", "sale", "win",
        "congratulations", "lucky", "reward", "bonus", "flat rs",
        "off on", "get up to", "limited time", "hurry", "shop now",
        "exclusive deal", "special offer", "save rs"
    )

    private val deliveryKeywords = listOf(
        "delivered", "out for delivery", "shipped", "dispatched",
        "order placed", "order confirmed", "tracking", "arriving",
        "package", "delivery expected", "your order", "shipment"
    )

    private val appointmentKeywords = listOf(
        "appointment", "booking confirmed", "scheduled", "booking slot",
        "your visit", "check-in", "reservation", "consultation",
        "meeting confirmed", "session booked"
    )

    fun classify(body: String): SmsNotificationCategory {
        val lower = body.lowercase()

        // Check OTP first (most specific)
        if (otpKeywords.any { lower.contains(it) }) {
            return SmsNotificationCategory.OTP
        }

        // Check expiry (high priority — actionable)
        if (expiryKeywords.any { lower.contains(it) }) {
            return SmsNotificationCategory.EXPIRY
        }

        // Check recharge
        if (rechargeKeywords.any { lower.contains(it) }) {
            return SmsNotificationCategory.RECHARGE
        }

        // Check delivery
        if (deliveryKeywords.any { lower.contains(it) }) {
            return SmsNotificationCategory.DELIVERY
        }

        // Check appointment
        if (appointmentKeywords.any { lower.contains(it) }) {
            return SmsNotificationCategory.APPOINTMENT
        }

        // Check promotion (most common spam — check last)
        if (promotionKeywords.any { lower.contains(it) }) {
            return SmsNotificationCategory.PROMOTION
        }

        return SmsNotificationCategory.OTHER
    }

    fun isExpiredNotification(body: String, smsDate: Long): Boolean {
        val lower = body.lowercase()
        // If the SMS mentions expiry and the date is in the past, it's expired
        val isExpiryType = expiryKeywords.any { lower.contains(it) } ||
                           rechargeKeywords.any { lower.contains(it) }
        if (!isExpiryType) return false
        // SMS is older than 1 day and mentions expiry/recharge
        val oneDayMs = 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - smsDate > oneDayMs
    }
}
