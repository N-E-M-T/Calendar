package com.kizitonwose.calendarview.adapter

import android.view.View
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import com.kizitonwose.calendarview.model.CalendarDay

class WeekHolder(
    @LayoutRes dayViewRes: Int,
    dateClickListener: DateClickListener,
    dateViewBinder: DateViewBinder
) {

    private val dayHolders = (1..7).map { DayHolder(dayViewRes, dateClickListener, dateViewBinder) }

    private lateinit var container: LinearLayout

    fun inflateWeekView(parent: LinearLayout): View {
        if (::container.isInitialized.not()) {
            container = LinearLayout(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.HORIZONTAL
                weightSum = dayHolders.count().toFloat()
                for (holder in dayHolders) {
                    addView(holder.inflateDayView(this))
                }
            }
        }
        return container
    }

    fun bindWeekView(daysOfWeek: List<CalendarDay>) {
        container.visibility = if (daysOfWeek.isEmpty()) View.GONE else View.VISIBLE
        for (i in daysOfWeek.indices) {
            dayHolders[i].bindDayView(daysOfWeek[i])
        }
    }
}