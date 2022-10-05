package com.kizitonwose.calendarview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.RecyclerView
import com.kizitonwose.calendarcore.CalendarDay
import com.kizitonwose.calendarcore.CalendarMonth
import com.kizitonwose.calendarcore.DayPosition
import com.kizitonwose.calendarcore.OutDateStyle
import com.kizitonwose.calendarinternal.checkDateRange
import com.kizitonwose.calendarview.internal.CalendarAdapter
import com.kizitonwose.calendarview.internal.CalendarLayoutManager
import com.kizitonwose.calendarview.internal.CalenderPageSnapHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

open class CalendarView : RecyclerView {

    /**
     * The [DayBinder] instance used for managing day cell views
     * creation and reuse. Changing the binder means that the view
     * creation logic could have changed too. We refresh the Calender.
     */
    var dayBinder: DayBinder<*>? = null
        set(value) {
            field = value
            invalidateViewHolders()
        }

    /**
     * The [MonthHeaderFooterBinder] instance used for managing header views.
     * The header view is shown above each month on the Calendar.
     */
    var monthHeaderBinder: MonthHeaderFooterBinder<*>? = null
        set(value) {
            field = value
            invalidateViewHolders()
        }

    /**
     * The [MonthHeaderFooterBinder] instance used for managing footer views.
     * The footer view is shown below each month on the Calendar.
     */
    var monthFooterBinder: MonthHeaderFooterBinder<*>? = null
        set(value) {
            field = value
            invalidateViewHolders()
        }

    /**
     * Called when the calender scrolls to a new month. Mostly beneficial
     * if [ScrollMode] is [ScrollMode.PAGED].
     */
    var monthScrollListener: MonthScrollListener? = null

    /**
     * The xml resource that is inflated and used as the day cell view.
     * This must be provided.
     */
    var dayViewResource = 0
        set(value) {
            if (field != value) {
                if (value == 0) throw IllegalArgumentException("'dayViewResource' attribute not provided.")
                field = value
                invalidateViewHolders()
            }
        }

    /**
     * The xml resource that is inflated and used as a header for every month.
     * Set zero to disable.
     */
    var monthHeaderResource = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateViewHolders()
            }
        }

    /**
     * The xml resource that is inflated and used as a footer for every month.
     * Set zero to disable.
     */
    var monthFooterResource = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateViewHolders()
            }
        }

    /**
     * A [ViewGroup] which is instantiated and used as the background for each month.
     * This class must have a constructor which takes only a [Context]. You should
     * exclude the name and constructor of this class from code obfuscation if enabled.
     */
    var monthViewClass: String? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateViewHolders()
            }
        }

    /**
     * The [RecyclerView.Orientation] used for the layout manager.
     * This determines the scroll direction of the the calendar.
     */
    @Orientation
    var orientation = HORIZONTAL
        set(value) {
            if (field != value) {
                field = value
                (layoutManager as? CalendarLayoutManager)?.orientation = value
            }
        }

    /**
     * The scrolling behavior of the calendar. If [ScrollMode.PAGED],
     * the calendar will snap to the nearest month after a scroll or swipe action.
     * If [ScrollMode.CONTINUOUS], the calendar scrolls normally.
     */
    var scrollPaged = true
        set(value) {
            if (field != value) {
                field = value
                pagerSnapHelper.attachToRecyclerView(if (scrollPaged) this else null)
            }
        }

    /**
     * Determines how outDates are generated for each month on the calendar.
     * If set to [OutDateStyle.END_OF_ROW], the calendar will generate outDates until
     * it reaches the first end of a row. This means that if a month has 6 rows,
     * it will display 6 rows and if a month has 5 rows, it will display 5 rows.
     * If set to [OutDateStyle.END_OF_GRID], the calendar will generate outDates until
     * it reaches the end of a 6 x 7 grid. This means that all months will have 6 rows.
     * If set to [OutDateStyle.NONE], no outDates will be generated.
     *
     * Note: This causes calendar data to be regenerated, consider using [updateMonthConfiguration]
     * if updating this value property [inDateStyle], [maxRowCount] or [hasBoundaries].
     */
    var outDateStyle = OutDateStyle.EndOfRow
        set(value) {
            if (field != value) {
                field = value
                if (adapter != null) updateAdapter()
            }
        }

    /**
     * If the day cells should have equal width and height.
     * if true, each view's width and height will be the
     * width of the calender divided by 7.
     */
    var daySizeSquare = true
        set(value) {
            if (field != value) {
                field = value
                invalidateViewHolders()
            }
        }

    private val scrollListenerInternal = object : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == SCROLL_STATE_IDLE) {
                calendarAdapter.notifyMonthScrollListenerIfNeeded()
            }
        }
    }

    private val pagerSnapHelper = CalenderPageSnapHelper()

    private var startMonth: YearMonth? = null
    private var endMonth: YearMonth? = null
    private var firstDayOfWeek: DayOfWeek? = null

    internal val isVertical: Boolean
        get() = orientation == VERTICAL

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr, defStyleAttr)
    }

    private fun init(attributeSet: AttributeSet, defStyleAttr: Int, defStyleRes: Int) {
        if (isInEditMode) return
        setHasFixedSize(true)
        context.withStyledAttributes(
            attributeSet,
            R.styleable.CalendarView,
            defStyleAttr,
            defStyleRes
        ) {
            dayViewResource =
                getResourceId(R.styleable.CalendarView_cvv_dayViewResource, dayViewResource)
            monthHeaderResource =
                getResourceId(R.styleable.CalendarView_cvv_monthHeaderResource, monthHeaderResource)
            monthFooterResource =
                getResourceId(R.styleable.CalendarView_cvv_monthFooterResource, monthFooterResource)
            orientation = getInt(R.styleable.CalendarView_cvv_orientation, orientation)
            scrollPaged = getBoolean(R.styleable.CalendarView_cvv_scrollPaged, scrollPaged)
            daySizeSquare = getBoolean(R.styleable.CalendarView_cvv_daySizeSquare, daySizeSquare)
            outDateStyle = OutDateStyle.values()[
                    getInt(R.styleable.CalendarView_cvv_outDateStyle, outDateStyle.ordinal)
            ]
            monthViewClass = getString(R.styleable.CalendarView_cvv_monthViewClass)
        }
        // Initial scroll setup since we check field when assigning and default value is `true`
        if (scrollPaged) pagerSnapHelper.attachToRecyclerView(this)
        check(dayViewResource != 0) { "No value set for `cv_dayViewResource` attribute." }
    }

    /**
     * The padding, in pixels to be applied
     * to the start of each month view.
     */
    @Px
    var monthPaddingStart = 0
        private set

    /**
     * The padding, in pixels to be applied
     * to the end of each month view.
     */
    @Px
    var monthPaddingEnd = 0
        private set

    /**
     * The padding, in pixels to be applied
     * to the top of each month view.
     */
    @Px
    var monthPaddingTop = 0
        private set

    /**
     * The padding, in pixels to be applied
     * to the bottom of each month view.
     */
    @Px
    var monthPaddingBottom = 0
        private set

    /**
     * The margin, in pixels to be applied
     * to the start of each month view.
     */
    @Px
    var monthMarginStart = 0
        private set

    /**
     * The margin, in pixels to be applied
     * to the end of each month view.
     */
    @Px
    var monthMarginEnd = 0
        private set

    /**
     * The margin, in pixels to be applied
     * to the top of each month view.
     */
    @Px
    var monthMarginTop = 0
        private set

    /**
     * The margin, in pixels to be applied
     * to the bottom of each month view.
     */
    @Px
    var monthMarginBottom = 0
        private set

    private val calendarLayoutManager: CalendarLayoutManager
        get() = layoutManager as CalendarLayoutManager

    private val calendarAdapter: CalendarAdapter
        get() = adapter as CalendarAdapter

    private fun invalidateViewHolders() {
        // This does not remove visible views.
        // recycledViewPool.clear()

        // This removes all views but is internal.
        // removeAndRecycleViews()

        if (adapter == null || layoutManager == null) return
        val state = layoutManager?.onSaveInstanceState()
        adapter = adapter
        layoutManager?.onRestoreInstanceState(state)
        post { calendarAdapter.notifyMonthScrollListenerIfNeeded() }
    }

    /**
     * Set the [monthPaddingStart], [monthPaddingTop], [monthPaddingEnd] and [monthPaddingBottom]
     * values without invalidating the view holders multiple times which would happen if these
     * values were set individually.
     */
    fun setMonthPadding(
        @Px start: Int = monthPaddingStart,
        @Px top: Int = monthPaddingTop,
        @Px end: Int = monthPaddingEnd,
        @Px bottom: Int = monthPaddingBottom,
    ) {
        monthPaddingStart = start
        monthPaddingTop = top
        monthPaddingEnd = end
        monthPaddingBottom = bottom
        invalidateViewHolders()
    }

    /**
     * Set the [monthMarginStart], [monthMarginTop], [monthMarginEnd] and [monthMarginBottom]
     * values without invalidating the view holders multiple times which would happen if these
     * values were set individually.
     */
    fun setMonthMargins(
        @Px start: Int = monthMarginStart,
        @Px top: Int = monthMarginTop,
        @Px end: Int = monthMarginEnd,
        @Px bottom: Int = monthMarginBottom,
    ) {
        monthMarginStart = start
        monthMarginTop = top
        monthMarginEnd = end
        monthMarginBottom = bottom
        invalidateViewHolders()
    }

    /**
     * Scroll to a specific month on the calendar. This only
     * shows the view for the month without any animations.
     * For a smooth scrolling effect, use [smoothScrollToMonth]
     */
    fun scrollToMonth(month: YearMonth) {
        calendarLayoutManager.scrollToMonth(month)
    }

    /**
     * Scroll to a specific month on the calendar using a smooth scrolling animation.
     * Just like [scrollToMonth], but with a smooth scrolling animation.
     */
    fun smoothScrollToMonth(month: YearMonth) {
        calendarLayoutManager.smoothScrollToMonth(month)
    }

    /**
     * Scroll to a specific [CalendarDay]. This brings the date cell
     * view's top to the top of the CalendarVew in vertical mode or
     * the cell view's left edge to the left edge of the CalendarVew
     * in horizontal mode. No animation is performed.
     * For a smooth scrolling effect, use [smoothScrollToDay].
     */
    fun scrollToDay(day: CalendarDay) {
        calendarLayoutManager.scrollToDay(day)
    }

    /**
     * Shortcut for [scrollToDay] with a [LocalDate] instance.
     */
    @JvmOverloads
    fun scrollToDate(date: LocalDate, position: DayPosition = DayPosition.MonthDate) {
        scrollToDay(CalendarDay(date, position))
    }

    /**
     * Scroll to a specific [CalendarDay] using a smooth scrolling animation.
     * Just like [scrollToDay], but with a smooth scrolling animation.
     */
    fun smoothScrollToDay(day: CalendarDay) {
        calendarLayoutManager.smoothScrollToDay(day)
    }

    /**
     * Shortcut for [smoothScrollToDay] with a [LocalDate] instance.
     */
    @JvmOverloads
    fun smoothScrollToDate(date: LocalDate, position: DayPosition = DayPosition.MonthDate) {
        smoothScrollToDay(CalendarDay(date, position))
    }

    /**
     * Notify the CalendarView to reload the cell for this [CalendarDay]
     * This causes [DayBinder.bind] to be called with the [ViewContainer]
     * at this position. Use this to reload a date cell on the Calendar.
     */
    fun notifyDayChanged(day: CalendarDay) {
        calendarAdapter.reloadDay(day)
    }

    /**
     * Shortcut for [notifyDayChanged] with a [LocalDate] instance.
     */
    @JvmOverloads
    fun notifyDateChanged(date: LocalDate, position: DayPosition = DayPosition.MonthDate) {
        notifyDayChanged(CalendarDay(date, position))
    }

    /**
     * Notify the CalendarView to reload the view for this [YearMonth]
     * This causes the following sequence pf events:
     * [DayBinder.bind] will be called for all dates in this month.
     * [MonthHeaderFooterBinder.bind] will be called for this month's header view if available.
     * [MonthHeaderFooterBinder.bind] will be called for this month's footer view if available.
     */
    fun notifyMonthChanged(month: YearMonth) {
        calendarAdapter.reloadMonth(month)
    }

    /**
     * Notify the CalendarView to reload all months.
     * Just like calling [notifyMonthChanged] for all months.
     */
    fun notifyCalendarChanged() {
        calendarAdapter.reloadCalendar()
    }

    /**
     * Find the first visible month on the CalendarView.
     *
     * @return The first visible month or null if not found.
     */
    fun findFirstVisibleMonth(): CalendarMonth? {
        return calendarAdapter.findFirstVisibleMonth()
    }

    /**
     * Find the last visible month on the CalendarView.
     *
     * @return The last visible month or null if not found.
     */
    fun findLastVisibleMonth(): CalendarMonth? {
        return calendarAdapter.findLastVisibleMonth()
    }

    /**
     * Find the first visible day on the CalendarView.
     * This is the day at the top-left corner of the calendar.
     *
     * @return The first visible day or null if not found.
     */
    fun findFirstVisibleDay(): CalendarDay? {
        return calendarAdapter.findFirstVisibleDay()
    }

    /**
     * Find the last visible day on the CalendarView.
     * This is the day at the bottom-right corner of the calendar.
     *
     * @return The last visible day or null if not found.
     */
    fun findLastVisibleDay(): CalendarDay? {
        return calendarAdapter.findLastVisibleDay()
    }

    /**
     * Setup the CalendarView.
     * See [updateMonthData] to change the [startMonth] and [endMonth] values.
     *
     * @param startMonth The first month on the calendar.
     * @param endMonth The last month on the calendar.
     * @param firstDayOfWeek An instance of [DayOfWeek] enum to be the first day of week.
     */
    fun setup(startMonth: YearMonth, endMonth: YearMonth, firstDayOfWeek: DayOfWeek) {
        checkDateRange(startMonth = startMonth, endMonth = endMonth)
        this.startMonth = startMonth
        this.endMonth = endMonth
        this.firstDayOfWeek = firstDayOfWeek

        removeOnScrollListener(scrollListenerInternal)
        addOnScrollListener(scrollListenerInternal)

        layoutManager = CalendarLayoutManager(calView = this, orientation)
        adapter = CalendarAdapter(
            calView = this,
            outDateStyle = outDateStyle,
            startMonth = startMonth,
            endMonth = endMonth,
            firstDayOfWeek = firstDayOfWeek,
        )
    }

    /**
     * Update the CalendarView's start or end month, and optionally the first day of week.
     * This can be called only if you have called [setup] in the past.
     * The calendar can handle really large date ranges so you may want to setup
     * the calendar with a large date range instead of updating the range frequently.
     */
    @JvmOverloads
    fun updateMonthData(
        startMonth: YearMonth = requireStartMonth(),
        endMonth: YearMonth = requireEndMonth(),
        firstDayOfWeek: DayOfWeek = requireFirstDayOfWeek(),
    ) {
        checkDateRange(startMonth = startMonth, endMonth = endMonth)
        this.startMonth = startMonth
        this.endMonth = endMonth
        this.firstDayOfWeek = firstDayOfWeek
        updateAdapter()
    }

    private fun updateAdapter() {
        calendarAdapter.updateData(
            startMonth = requireStartMonth(),
            endMonth = requireEndMonth(),
            outDateStyle = outDateStyle,
            firstDayOfWeek = requireFirstDayOfWeek(),
        )
    }

    private fun requireStartMonth(): YearMonth = startMonth ?: throw getFieldException("startMonth")

    private fun requireEndMonth(): YearMonth = endMonth ?: throw getFieldException("endMonth")

    private fun requireFirstDayOfWeek(): DayOfWeek =
        firstDayOfWeek ?: throw getFieldException("firstDayOfWeek")

    private fun getFieldException(field: String) =
        IllegalStateException("`$field` is not set. Have you called `setup()`?")
}
