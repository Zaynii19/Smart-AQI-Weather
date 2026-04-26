package com.aqi.weather.admin.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.CalendarView
import com.aqi.weather.R
import com.aqi.weather.data.local.database.entity.AQI
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.databinding.FragmentAdminStatsBinding
import com.aqi.weather.sharedViewModels.LocalAqiViewModel
import com.aqi.weather.util.getCircularAqiDrawable
import com.aqi.weather.util.getDayString
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import androidx.core.graphics.toColorInt

class AdminStatsFragment : Fragment() {
    private val binding by lazy {
        FragmentAdminStatsBinding.inflate(layoutInflater)
    }
    private val localAqiViewModel: LocalAqiViewModel by viewModels()
    private var userId: String = ""
    private var aqiList: List<AQI> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefsManager = UserPreferencesManager(requireContext())
        userId = prefsManager.userId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAQIs()
        setWeekChart()
        onChartSelection()

        binding.showCalender.setOnClickListener {
            if (binding.calendarView.isVisible) {
                binding.calenderText.text = "Open Calender"
                collapseItem(binding.calendarView)
            } else {
                binding.calenderText.text = "Close Calender"
                expandItem(binding.calendarView)
            }
        }

    }

    private fun expandItem(calendarView: CalendarView) {
        calendarView.visibility = View.VISIBLE
        val slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        calendarView.startAnimation(slideDown)
    }

    private fun collapseItem(calendarView: CalendarView) {
        val slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        slideUp.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                calendarView.visibility = View.GONE
            }
        })
        calendarView.startAnimation(slideUp)
    }

    private val highLightedDays: List<CalendarDay>
        get() {
            val days: MutableList<CalendarDay> = ArrayList()
            for (aqi in aqiList) {
                val calendar = Calendar.getInstance().apply {
                    val date = Date.from(LocalDate.parse(aqi.date)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant())
                    time = date
                }

                days.add(
                    CalendarDay(calendar).apply {
                        imageDrawable = getCircularAqiDrawable(requireContext(), aqi)
                    }
                )
            }
            return days
        }

    private fun onChartSelection() {
        binding.weekBtn.setOnClickListener {
            binding.weekBtn.setBackgroundResource(R.drawable.toggle_btn_selected)
            binding.monthBtn.setBackgroundResource(R.drawable.toggle_btn_unselected)
            binding.weekBtn.setTextColor(Color.WHITE)
            binding.monthBtn.setTextColor(getColor(requireContext(),R.color.dark_gray))
            binding.weekChart.visibility = View.VISIBLE
            binding.monthChart.visibility = View.GONE
            setWeekChart()
        }

        binding.monthBtn.setOnClickListener {
            binding.monthBtn.setBackgroundResource(R.drawable.toggle_btn_selected)
            binding.weekBtn.setBackgroundResource(R.drawable.toggle_btn_unselected)
            binding.monthBtn.setTextColor(Color.WHITE)
            binding.weekBtn.setTextColor(getColor(requireContext(),R.color.dark_gray))
            binding.monthChart.visibility = View.VISIBLE
            binding.weekChart.visibility = View.GONE
            setMonthChart()
        }
    }

    private fun observeAQIs() {
        localAqiViewModel.observeAQIs(userId)
            .observe(viewLifecycleOwner) { aqis ->
                if (aqis != null) {
                    aqiList = aqis
                    binding.calendarView.setCalendarDays(highLightedDays)

                    val lastTwoAqis = aqis.take(2)
                    setupLogs(lastTwoAqis)
                }
            }
    }

    private fun setWeekChart() {
        localAqiViewModel.getLast7DaysAQI(userId).observe(viewLifecycleOwner) { aqiList ->
            // Get last 7 days (including today)
            val today = LocalDate.now()

            val dayLabels = mutableListOf<String>()
            val aqiValues = mutableListOf<Int>()
            val barColors = mutableListOf<Int>()

            for (i in 6 downTo 0) {
                val currentDate = today.minusDays(i.toLong())
                val dateStr = currentDate.toString()

                // Find AQI for this date
                val aqiForDay = aqiList.find { it.date == dateStr }

                dayLabels.add(currentDate.dayOfWeek.name.take(3))
                aqiValues.add(aqiForDay?.aqi ?: 0)
                barColors.add(aqiForDay?.color ?: "#CCCCCC".toColorInt())
            }

            // Set data to week chart
            binding.weekChart.setData(dayLabels, aqiValues, barColors)
            binding.weekChart.startAnimation()
        }
    }

    private fun setMonthChart() {
        localAqiViewModel.getCurrentMonthAQI(userId).observe(viewLifecycleOwner) { aqiList ->

            // Get current month info
            val today = LocalDate.now()
            val currentMonth = today.monthValue
            val currentYear = today.year

            // Initialize weekly data
            val weeklyAverages = mutableListOf<Int>()
            val weeklyColors = mutableListOf<Int>()

            // Group AQIs by week (4 weeks)
            for (weekNum in 1..4) {
                var totalAqi = 0
                var count = 0
                var weekColor = "#CCCCCC".toColorInt() // Default color

                // Calculate date range for this week
                val weekStartDay = (weekNum - 1) * 7 + 1
                val weekEndDay = minOf(weekNum * 7, today.lengthOfMonth())

                // Filter AQIs for this week
                val weekAqis = aqiList.filter { aqi ->
                    val date = LocalDate.parse(aqi.date)
                    date.year == currentYear &&
                            date.monthValue == currentMonth &&
                            date.dayOfMonth in weekStartDay..weekEndDay
                }

                if (weekAqis.isNotEmpty()) {
                    // Calculate average AQI for the week
                    totalAqi = weekAqis.sumOf { it.aqi }
                    count = weekAqis.size

                    // Get the most frequent or average color for the week
                    val colors = weekAqis.map { it.color }
                    weekColor = getMostFrequentColor(colors)
                }

                val avgAqi = if (count > 0) totalAqi / count else 0
                weeklyAverages.add(avgAqi)
                weeklyColors.add(weekColor)
            }

            // Set data to month chart
            binding.monthChart.setData(weeklyAverages, weeklyColors)
            binding.monthChart.startAnimation()
        }
    }

    // Helper function to get most frequent color
    private fun getMostFrequentColor(colors: List<Int>): Int {
        if (colors.isEmpty()) return "#CCCCCC".toColorInt()
        if (colors.size == 1) return colors.first()

        val frequency = colors.groupingBy { it }.eachCount()
        return frequency.maxByOrNull { it.value }?.key ?: colors.first()
    }

    private fun setupLogs(lastTwoAqis: List<AQI>) {
        if (lastTwoAqis.isNotEmpty()) {
            val log1 = lastTwoAqis.getOrNull(0)
            val log2 = lastTwoAqis.getOrNull(1)

            if (log1 == null) {
                binding.log1Card.visibility = View.GONE
            } else {
                binding.log1Color.setBackgroundColor(log1.color)
                binding.log1Category.text = log1.label
                val day = getDayString(log1.date)
                val log1Value = "$day, ${log1.updatedAt}-AQI ${log1.aqi}"
                binding.log1Value.text = log1Value
            }

            if (log2 == null) {
                binding.log2Card.visibility = View.GONE
            } else {
                binding.log2Color.setBackgroundColor(log2.color)
                binding.log2Category.text = log2.label
                val day = getDayString(log2.date)
                val log2Value = "$day, ${log2.updatedAt}-AQI ${log2.aqi}"
                binding.log2Value.text = log2Value
            }
        } else {
            binding.recentLogs.visibility = View.GONE
            binding.noDataText.visibility = View.VISIBLE
        }
    }
}