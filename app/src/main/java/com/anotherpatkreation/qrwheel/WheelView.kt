package com.anotherpatkreation.qrwheel

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import kotlin.math.min

class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val segColors = intArrayOf(
        0xFFE5484D.toInt(), 0xFFF97316.toInt(), 0xFFF5A623.toInt(),
        0xFF22C55E.toInt(), 0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt()
    )

    private val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 10f
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF14141B.toInt()
    }
    private val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 6f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val hubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var names: List<String> = emptyList()
    private var rotation = 0f
    private var animator: ValueAnimator? = null
    var onWinner: ((Int) -> Unit)? = null
    var spinning = false
        private set

    fun setNames(list: List<String>) {
        if (!spinning) {
            names = list
            invalidate()
        }
    }

    fun spin() {
        if (spinning || names.isEmpty()) return
        spinning = true
        val n = names.size
        val winner = (0 until n).random()
        val seg = 360f / n
        // pointer sits at the top (270 degrees in sweep coordinates)
        val targetMod = mod360(270f - (winner * seg + seg / 2f))
        val currentMod = mod360(rotation)
        val delta = mod360(targetMod - currentMod)
        val turns = 5f * 360f
        val start = rotation
        val end = rotation + turns + delta
        animator?.cancel()
        animator = ValueAnimator.ofFloat(start, end).apply {
            duration = 4200
            interpolator = Interpolator { t -> 1f - (1f - t) * (1f - t) * (1f - t) }
            addUpdateListener {
                rotation = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    spinning = false
                    onWinner?.invoke(winner)
                }
                override fun onAnimationCancel(animation: Animator) {
                    spinning = false
                }
            })
            start()
        }
    }

    private fun mod360(v: Float): Float {
        val r = v % 360f
        return if (r < 0) r + 360f else r
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) / 2f - 26f
        if (radius <= 0) return

        // pointer triangle at the top, pointing down at the wheel
        val pointer = Path().apply {
            moveTo(cx - 22f, cy - radius - 20f)
            lineTo(cx + 22f, cy - radius - 20f)
            lineTo(cx, cy - radius + 16f)
            close()
        }
        canvas.drawPath(pointer, pointerPaint)

        if (names.isEmpty()) {
            // idle ring when there is nothing to show yet
            segPaint.color = 0xFF1F1F28.toInt()
            canvas.drawCircle(cx, cy, radius, segPaint)
            textPaint.textSize = radius * 0.16f
            canvas.drawText("No signups yet", cx, cy + textPaint.textSize / 3f, textPaint)
            return
        }

        val n = names.size
        val seg = 360f / n
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        textPaint.textSize = when {
            n <= 8 -> radius * 0.15f
            n <= 16 -> radius * 0.11f
            else -> radius * 0.08f
        }
        for (i in 0 until n) {
            segPaint.color = segColors[i % segColors.size]
            canvas.drawArc(rect, rotation + i * seg, seg, true, segPaint)
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // names along each segment
        for (i in 0 until n) {
            val angle = rotation + i * seg + seg / 2f
            canvas.save()
            canvas.rotate(angle, cx, cy)
            val label = names[i].let { if (it.length > 14) it.substring(0, 13) + "…" else it }
            canvas.drawText(label, cx + radius * 0.62f, cy + textPaint.textSize / 3f, textPaint)
            canvas.restore()
        }

        // hub
        val hubR = radius * 0.24f
        canvas.drawCircle(cx, cy, hubR, hubPaint)
        canvas.drawCircle(cx, cy, hubR, hubRingPaint)
        hubTextPaint.textSize = hubR * 0.55f
        canvas.drawText("SPIN", cx, cy + hubTextPaint.textSize / 3f, hubTextPaint)
    }
}
