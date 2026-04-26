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

class MonthChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAEAEA")
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

    private var labels = listOf("Week 1", "Week 2", "Week 3", "Week 4")
    private var completedList = List(4) { 0 }
    private var totalMax = 500
    private var barColors = List(4) { Color.parseColor("#8DBEA7") }

    private var animationProgress = 1f

    fun setData(completed: List<Int>, colors: List<Int>) {
        this.completedList = completed
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

        val margin = 20 * resources.displayMetrics.density

        val availableWidth = width - margin * 2
        val numberOfBars = 4
        val numberOfSpaces = numberOfBars - 1

        val barWidth = availableWidth / (numberOfBars * 3f + numberOfSpaces)
        val space = barWidth * 3f

        val barMaxHeight = height * 0.65f
        val bottom = height * 0.8f

        val groupWidth = (barWidth * numberOfBars) + (space * numberOfSpaces)
        val startX = (width - groupWidth) / 2f

        for (i in 0 until 4) {
            val xCenter = startX + i * (barWidth + space) + barWidth / 2f

            val completed = completedList[i].coerceAtLeast(0)

            // Set the color for this bar
            completedPaint.color = barColors.getOrElse(i) { Color.parseColor("#8DBEA7") }

            // TOTAL CONTAINER HEIGHT (full bar)
            val totalHeight = barMaxHeight * animationProgress
            val topTotal = bottom - totalHeight

            // DRAW TOTAL BAR (rounded background)
            val totalRect = RectF(
                xCenter - barWidth / 2f,
                topTotal,
                xCenter + barWidth / 2f,
                bottom
            )
            canvas.drawRoundRect(totalRect, radius, radius, totalPaint)

            // COMPLETED HEIGHT (based on AQI value / 500)
            val completedHeight = (completed / totalMax.toFloat()) * barMaxHeight * animationProgress
            val topCompleted = bottom - completedHeight

            // DRAW COMPLETED BAR inside total
            val completedRect = RectF(
                xCenter - barWidth / 2f,
                topCompleted,
                xCenter + barWidth / 2f,
                bottom
            )

            // Clip inside rounded total bar (same as week chart)
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(totalRect, radius, radius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            canvas.drawRoundRect(completedRect, radius, radius, completedPaint)
            canvas.restore()

            // Label below bar
            val label = labels[i]
            canvas.drawText(
                label,
                xCenter,
                bottom + 50f,
                textPaint
            )
        }
    }
}