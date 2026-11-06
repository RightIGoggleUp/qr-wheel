package com.anotherpatkreation.qrwheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders the custom QR card: white rounded card (all four corners), custom
 * Round-style QR (rounded dark modules, red finder rings), error-correction
 * level H. Card is as close to square as the space allows so the code fills
 * it. No brand banner (Patrick 10:47 AM). Regenerated live on URL change.
 */
class QrCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111418") }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2231A") }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val tmp = RectF()

    private var matrix: Array<BooleanArray>? = null
    private var modules = 0

    fun setUrl(url: String) {
        try {
            val qr = Encoder.encode(url, ErrorCorrectionLevel.H, null)
            val m = qr.matrix
            modules = m.width
            matrix = Array(m.height) { y -> BooleanArray(m.width) { x -> m.get(x, y).toInt() == 1 } }
        } catch (e: Exception) {
            matrix = null
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mat = matrix ?: return
        val n = modules
        val grid = n + 8 // 4-module quiet zone each side

        // Card geometry: largest square that fits, centered. No banner - the
        // QR spans nearly the whole card (quiet zone preserved via grid = n+8).
        val availW = width.toFloat()
        val availH = height.toFloat()
        val cardS = minOf(availW, availH) * 0.98f
        val left = (availW - cardS) / 2f
        val top = (availH - cardS) / 2f

        val padF = cardS * 0.055f
        val m = (cardS - 2 * padF) / grid

        // White card, all four corners rounded (visible radius)
        tmp.set(left, top, left + cardS, top + cardS)
        canvas.drawRoundRect(tmp, cardS * 0.085f, cardS * 0.085f, white)

        // QR code fills the card
        val qrLeft = left + padF
        val qrTop = top + padF
        fun inFinder(r: Int, c: Int) =
            (r < 7 && c < 7) || (r < 7 && c >= n - 7) || (r >= n - 7 && c < 7)

        for (r in 0 until n) {
            for (c in 0 until n) {
                if (!mat[r][c] || inFinder(r, c)) continue
                val x0 = qrLeft + (c + 4) * m
                val y0 = qrTop + (r + 4) * m
                tmp.set(x0 + 1, y0 + 1, x0 + m - 1, y0 + m - 1)
                canvas.drawRoundRect(tmp, m * 0.3f, m * 0.3f, ink)
            }
        }
        // Finder patterns: Lenovo-red ring, white middle, dark core
        for ((r0, c0) in listOf(0 to 0, 0 to n - 7, n - 7 to 0)) {
            val x0 = qrLeft + (c0 + 4) * m
            val y0 = qrTop + (r0 + 4) * m
            tmp.set(x0, y0, x0 + 7 * m, y0 + 7 * m)
            canvas.drawRoundRect(tmp, m * 0.6f, m * 0.6f, red)
            tmp.set(x0 + m, y0 + m, x0 + 6 * m, y0 + 6 * m)
            canvas.drawRoundRect(tmp, m * 0.1f, m * 0.1f, white)
            tmp.set(x0 + 2 * m, y0 + 2 * m, x0 + 5 * m, y0 + 5 * m)
            canvas.drawRect(tmp, ink)
        }
    }
}
