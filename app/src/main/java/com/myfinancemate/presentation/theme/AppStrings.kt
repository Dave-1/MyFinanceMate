package com.myfinancemate.presentation.theme

import com.myfinancemate.R

/**
 * Centralized string resource constants.
 * Use these instead of R.string.xxx directly — if architecture changes,
 * only this file needs updating.
 */
object AppStrings {
    // App
    val app_name = R.string.app_name

    // Common labels (reused across screens)
    val label_income = R.string.label_income
    val label_expense = R.string.label_expense
    val label_categories = R.string.label_categories
    val label_delete = R.string.label_delete
    val label_cancel = R.string.label_cancel
    val label_add = R.string.label_add
    val label_save = R.string.label_save
    val label_close = R.string.label_close
    val label_yes = R.string.label_yes
    val label_back = R.string.label_back
    val label_all = R.string.label_all
    val label_notifications = R.string.label_notifications
    val label_reports = R.string.label_reports
    val currency_symbol = R.string.currency_symbol

    // Dashboard
    val dashboard_subtitle = R.string.dashboard_subtitle
    val dashboard_total_balance = R.string.dashboard_total_balance
    val dashboard_recent_transactions = R.string.dashboard_recent_transactions
    val dashboard_view_all = R.string.dashboard_view_all
    val dashboard_no_transactions = R.string.dashboard_no_transactions
    val dashboard_add_first = R.string.dashboard_add_first
    val dashboard_expense_by_category = R.string.dashboard_expense_by_category

    // Backup
    val backup_reminder_title = R.string.backup_reminder_title
    val backup_reminder_text = R.string.backup_reminder_text
    val backup_reminder_short = R.string.backup_reminder_short
    val backup_now = R.string.backup_now
    val backup_later = R.string.backup_later
    val backup_complete = R.string.backup_complete
    val backup_bank_breakdown = R.string.backup_bank_breakdown
    val backup_parse_button = R.string.backup_parse_button
    val backup_parse_desc = R.string.backup_parse_desc
    val backup_view_files = R.string.backup_view_files
    val backup_total_sms = R.string.backup_total_sms
    val backup_bank_transactions = R.string.backup_bank_transactions
    val backup_notifications_count = R.string.backup_notifications_count
    val backup_bank_entry = R.string.backup_bank_entry

    // Notifications
    val notifications_title = R.string.notifications_title
    val notifications_clear_read = R.string.notifications_clear_read
    val notifications_empty = R.string.notifications_empty
    val notifications_empty_desc = R.string.notifications_empty_desc
    val notifications_empty_category = R.string.notifications_empty_category
    val notifications_swipe_read = R.string.notifications_swipe_read

    // Notification categories
    val notif_recharge = R.string.notif_recharge
    val notif_expiry = R.string.notif_expiry
    val notif_promotion = R.string.notif_promotion
    val notif_otp = R.string.notif_otp
    val notif_delivery = R.string.notif_delivery
    val notif_appointment = R.string.notif_appointment
    val notif_other = R.string.notif_other

    // Transactions
    val transactions_title = R.string.transactions_title
    val transactions_search = R.string.transactions_search
    val transactions_filter_sms = R.string.transactions_filter_sms
    val transactions_filter_past_sms = R.string.transactions_filter_past_sms

    // Transaction fields
    val field_type = R.string.field_type
    val field_amount = R.string.field_amount
    val field_description = R.string.field_description
    val field_merchant = R.string.field_merchant

    // Add/Edit Transaction
    val add_transaction_title = R.string.add_transaction_title
    val edit_transaction_title = R.string.edit_transaction_title

    // Reminders
    val reminders_title = R.string.reminders_title
    val reminders_count = R.string.reminders_count
    val reminders_count_plural = R.string.reminders_count_plural
    val reminders_empty = R.string.reminders_empty
    val reminders_empty_desc = R.string.reminders_empty_desc
    val add_reminder_title = R.string.add_reminder_title
    val reminder_type = R.string.reminder_type
    val reminder_details = R.string.reminder_details
    val reminder_title_label = R.string.reminder_title_label
    val reminder_amount_label = R.string.reminder_amount_label
    val reminder_desc_label = R.string.reminder_desc_label
    val reminder_recurrence = R.string.reminder_recurrence
    val reminder_save = R.string.reminder_save
    val reminder_next = R.string.reminder_next
    val reminder_delete_title = R.string.reminder_delete_title
    val reminder_delete_confirm = R.string.reminder_delete_confirm
    val recurrence_one_time = R.string.recurrence_one_time
    val recurrence_daily = R.string.recurrence_daily
    val recurrence_weekly = R.string.recurrence_weekly
    val recurrence_monthly = R.string.recurrence_monthly
    val recurrence_yearly = R.string.recurrence_yearly

    // Categories
    val category_name_label = R.string.category_name_label
    val category_keywords_label = R.string.category_keywords_label

    // PIN / Auth
    val pin_set_title = R.string.pin_set_title
    val pin_set_desc = R.string.pin_set_desc
    val pin_label = R.string.pin_label
    val pin_confirm_label = R.string.pin_confirm_label
    val pin_verify = R.string.pin_verify
    val pin_locked = R.string.pin_locked
    val pin_locked_desc = R.string.pin_locked_desc
    val pin_setup = R.string.pin_setup
    val pin_setup_desc = R.string.pin_setup_desc
    val pin_error_length = R.string.pin_error_length
    val pin_error_mismatch = R.string.pin_error_mismatch
    val pin_error_wrong = R.string.pin_error_wrong
    val pin_biometric_subtitle = R.string.pin_biometric_subtitle

    // Settings
    val settings_title = R.string.settings_title
    val settings_theme = R.string.settings_theme
    val settings_theme_desc = R.string.settings_theme_desc
    val settings_dark_mode = R.string.settings_dark_mode
    val settings_data = R.string.settings_data
    val settings_categories_desc = R.string.settings_categories_desc
    val settings_sms_rules = R.string.settings_sms_rules
    val settings_add_rule = R.string.settings_add_rule
    val settings_add_rule_desc = R.string.settings_add_rule_desc
    val settings_sender_id_label = R.string.settings_sender_id_label
    val settings_bank_name_label = R.string.settings_bank_name_label
    val settings_backup_restore = R.string.settings_backup_restore
    val settings_export = R.string.settings_export
    val settings_import = R.string.settings_import
    val settings_sms_utilities = R.string.settings_sms_utilities
    val settings_security = R.string.settings_security
    val settings_app_lock = R.string.settings_app_lock
    val settings_change_pin = R.string.settings_change_pin
    val settings_remove_pin = R.string.settings_remove_pin
    val settings_about = R.string.settings_about
    val settings_version = R.string.settings_version
    val settings_open_source = R.string.settings_open_source

    // Welcome / Setup
    val welcome_title = R.string.welcome_title
    val welcome_sms_title = R.string.welcome_sms_title
    val welcome_sms_desc = R.string.welcome_sms_desc
    val welcome_sms_grant = R.string.welcome_sms_grant
    val welcome_sms_granted = R.string.welcome_sms_granted
    val welcome_backup_title = R.string.welcome_backup_title
    val welcome_backup_desc = R.string.welcome_backup_desc
    val welcome_backup_now = R.string.welcome_backup_now
    val welcome_backup_in_progress = R.string.welcome_backup_in_progress
    val welcome_pin_title = R.string.welcome_pin_title
    val welcome_pin_desc = R.string.welcome_pin_desc
    val welcome_pin_set = R.string.welcome_pin_set
    val welcome_pin_done = R.string.welcome_pin_done
    val welcome_biometric_title = R.string.welcome_biometric_title
    val welcome_biometric_desc = R.string.welcome_biometric_desc
    val welcome_biometric_unavailable = R.string.welcome_biometric_unavailable
    val welcome_biometric_enable = R.string.welcome_biometric_enable
    val welcome_biometric_enabled = R.string.welcome_biometric_enabled
    val welcome_skip = R.string.welcome_skip

    // Reports
    val reports_title = R.string.reports_title
    val reports_weekly_trend = R.string.reports_weekly_trend
    val reports_no_data = R.string.reports_no_data
}
