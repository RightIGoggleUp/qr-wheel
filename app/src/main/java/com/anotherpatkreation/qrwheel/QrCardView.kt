package com.anotherpatkreation.qrwheel

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders the branded signup QR card: white rounded card, full-width Lenovo
 * red box on top, Round-style QR below (rounded modules, Lenovo-red finder
 * rings), error-correction level H. Regenerated live whenever the URL changes.
 */
class QrCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111418") }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2231A") }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val logo = BitmapFactory.decodeResource(resources, R.drawable.lenovo_logo)
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

        // Card geometry: width-limited, logo band above the code.
        val availW = width.toFloat()
        val availH = height.toFloat()
        var cardW = availW
        var cardH = cardW * (1f + 1f / 3f) + cardW * 0.18f // side + band + gaps (approx)
        if (cardH > availH) {
            cardW *= availH / cardH
            cardH = availH
        }
        val left = (availW - cardW) / 2f
        val top = (availH - cardH) / 2f

        val padF = cardW * 0.06f
        val sideF = cardW - 2 * padF
        val bandHF = sideF * logo.height / logo.width
        val gapF = sideF * 0.06f
        val m = sideF / grid

        // White card
        tmp.set(left, top, left + cardW, top + cardH)
        canvas.drawRoundRect(tmp, cardW * 0.06f, cardW * 0.06f, white)

        // Lenovo red box, full code width, on top
        val bandLeft = left + padF
        val bandTop = top + padF
        tmp.set(bandLeft, bandTop, bandLeft + sideF, bandTop + bandHF)
        canvas.drawBitmap(logo, null, tmp, white)

        // QR code below
        val qrLeft = left + padF
        val qrTop = bandTop + bandHF + gapF
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
