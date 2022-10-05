package com.kizitonwose.calendarinternal

import com.kizitonwose.calendarcore.WeekDay
import com.kizitonwose.calendarcore.WeekDayPosition
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WeekDateRange(
    val startDateAdjusted: LocalDate,
    val endDateAdjusted: LocalDate,
)

fun getWeekCalendarAdjustedRange(
    startDate: LocalDate,
    endDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
): WeekDateRange {
    val inDays = firstDayOfWeek.daysUntil(startDate.dayOfWeek)
    val startDateAdjusted = startDate.minusDays(inDays.toLong())
    val weeksBetween =
        ChronoUnit.WEEKS.between(startDateAdjusted, endDate).toInt()
    val endDateAdjusted = startDateAdjusted.plusWeeks(weeksBetween.toLong()).plusDays(6)
    return WeekDateRange(startDateAdjusted = startDateAdjusted, endDateAdjusted = endDateAdjusted)
}

fun getWeekCalendarData(
    startDateAdjusted: LocalDate,
    offset: Int,
    calendarStartDate: LocalDate,
    calendarEndDate: LocalDate,
): WeekData {
    val firstDayInWeek = startDateAdjusted.plusWeeks(offset.toLong())
    return WeekData(firstDayInWeek, calendarStartDate, calendarEndDate)
}

data class WeekData internal constructor(
    val firstDayInWeek: LocalDate,
    val calendarStartDate: LocalDate,
    val calendarEndDate: LocalDate,
) {
    val days = (0 until 7).map { dayOffset -> getDay(dayOffset) }

    private fun getDay(dayOffset: Int): WeekDay {
        val date = firstDayInWeek.plusDays(dayOffset.toLong())
        val position = when {
            date < calendarStartDate -> WeekDayPosition.InDate
            date > calendarEndDate -> WeekDayPosition.OutDate
            else -> WeekDayPosition.RangeDate
        }
        return WeekDay(date, position)
    }
}

fun getWeekIndex(startDateAdjusted: LocalDate, date: LocalDate): Int {
    return ChronoUnit.WEEKS.between(startDateAdjusted, date).toInt()
}

fun getWeekIndicesCount(startDateAdjusted: LocalDate, endDateAdjusted: LocalDate): Int {
    // Add one to include the start week itself!
    return getWeekIndex(startDateAdjusted, endDateAdjusted) + 1
}
