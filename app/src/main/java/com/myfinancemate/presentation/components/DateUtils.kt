package com.myfinancemate.presentation.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getDateHeader(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val today = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = cal.timeInMillis

    val itemCal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val itemDay = itemCal.timeInMillis

    return when {
        itemDay == today -> "Today"
        itemDay == yesterday -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

fun <T> List<T>.groupByDateHeader(getDate: (T) -> Long): Map<String, List<T>> {
    return sortedByDescending { getDate(it) }
        .groupBy { getDateHeader(getDate(it)) }
}
