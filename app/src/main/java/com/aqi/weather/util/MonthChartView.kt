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
        color = Color.parseColor("#EAEAEA")   // same as week
        style = Paint.Style.FILL
    }

    private val completedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8DBEA7")   // same as week
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
    private var totalList = List(4) { 5 }

    // Animation (same as week chart)
    private var animationProgress = 1f

    fun setData(completed: List<Int>, total: List<Int>) {
        this.completedList = completed
        this.totalList = total
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

        // same bar width logic as week (just reduced for 4 bars)
        val barWidth = availableWidth / (numberOfBars * 3f + numberOfSpaces)
        val space = barWidth * 3f

        val barMaxHeight = height * 0.65f
        val bottom = height * 0.8f

        val groupWidth = (barWidth * numberOfBars) + (space * numberOfSpaces)
        val startX = (width - groupWidth) / 2f

        for (i in 0 until 4) {

            val xCenter = startX + i * (barWidth + space) + barWidth / 2f

            val total = totalList[i].coerceAtLeast(1)
            val completed = completedList[i].coerceAtLeast(0)

            // SAME WEEK PROGRESS STYLE
            val totalHeight = ((total / 5f) * barMaxHeight) * animationProgress
            val completedHeight = ((completed / 5f) * barMaxHeight) * animationProgress

            val topTotal = bottom - totalHeight

            // TOTAL CONTAINER RECT
            val totalRect = RectF(
                xCenter - barWidth / 2f,
                topTotal,
                xCenter + barWidth / 2f,
                bottom
            )
            canvas.drawRoundRect(totalRect, radius, radius, totalPaint)

            val topCompleted = bottom - completedHeight

            // COMPLETED PROGRESS RECT
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
            canvas.drawText(
                labels[i],
                xCenter,
                bottom + 50f,
                textPaint
            )
        }
    }
}
