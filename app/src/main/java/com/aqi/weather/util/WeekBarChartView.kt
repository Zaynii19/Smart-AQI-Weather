package com.aqi.weather.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import com.aqi.weather.R

class WeekBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val totalBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAEAEA")   // grey
        style = Paint.Style.FILL
    }

    private val completedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 32f
        typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
        textAlign = Paint.Align.CENTER
    }

    private val radius = 40f

    private var dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private var valueList = List(7) { 0 }
    private var totalMax = 500
    private var barColors = List(7) { Color.parseColor("#8DBEA7") }  // Default green

    private var animationProgress = 1f

    fun setData(days: List<String>, value: List<Int>, colors: List<Int>) {
        this.dayLabels = days
        this.valueList = value
        this.barColors = colors
        invalidate()
    }

    fun startAnimation() {
        animationProgress = 0f

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 900L
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener {
            animationProgress = it.animatedValue as Float
            invalidate()
        }

        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Convert dp to px
        val margin = 20 * resources.displayMetrics.density

        val availableWidth = width - (margin * 2)
        val numberOfBars = 7
        val numberOfSpaces = numberOfBars - 1

        // Bar width will be 1 / 10 of screen by default then adjusted
        val barWidth = availableWidth / (numberOfBars * 3.0f + numberOfSpaces)
        val space = barWidth * 3.0f

        val barMaxHeight = height * 0.65f
        val bottom = height * 0.8f

        // CENTERING CALCULATION
        val totalGroupWidth = (barWidth * numberOfBars) + (space * numberOfSpaces)
        val startX = (width - totalGroupWidth) / 2f

        for (i in 0 until 7) {
            val xCenter = startX + i * (barWidth + space) + barWidth / 2f

            //val total = totalList[i].coerceAtLeast(1)
            val completed = valueList[i].coerceAtLeast(0)

            // Set the color for this bar
            completedPaint.color = barColors.getOrElse(i) { Color.parseColor("#8DBEA7") }

            // TOTAL CONTAINER HEIGHT
            val totalHeight = totalMax * animationProgress
            val topTotal = bottom - totalHeight

            // DRAW TOTAL BAR (rounded)
            val totalRect = RectF(xCenter - barWidth / 2, topTotal, xCenter + barWidth / 2, bottom)
            canvas.drawRoundRect(totalRect, radius, radius, totalBarPaint)

            // COMPLETED HEIGHT
            val completedHeight = (completed / totalMax.toFloat()) * barMaxHeight * animationProgress
            val topCompleted = bottom - completedHeight

            // DRAW COMPLETED BAR inside total
            val completedRect = RectF(xCenter - barWidth / 2, topCompleted, xCenter + barWidth / 2, bottom)
            canvas.save()

            // Clip inside container shape
            val clipPath = Path().apply {
                addRoundRect(totalRect, radius, radius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            canvas.drawRoundRect(completedRect, radius, radius, completedPaint)

            canvas.restore()
            val label = dayLabels[i].lowercase().replaceFirstChar { it.uppercase() }

            // DRAW DAY LABEL
            canvas.drawText(label, xCenter, bottom + 50f, textPaint)
        }
    }
}