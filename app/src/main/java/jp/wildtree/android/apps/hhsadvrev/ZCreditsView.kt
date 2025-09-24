package jp.wildtree.android.apps.hhsadvrev

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.animation.LinearInterpolator
import android.util.AttributeSet
import androidx.core.graphics.createBitmap

class ZCreditsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var creditsBitmap: Bitmap? = null
    private var scrollYPos = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        //olor = Color.WHITE
        color = 0xffc0c0c0.toInt()
        textSize = 48f
    }

    fun setCreditsText(text: String, width: Int) {
        val textPaint = TextPaint(paint)
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        val bmp = createBitmap(width, staticLayout.height)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLACK)
        staticLayout.draw(canvas)

        creditsBitmap = bmp
        scrollYPos = height.toFloat()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        creditsBitmap?.let {
            canvas.drawBitmap(it, 0f, scrollYPos, null)
        }
    }

    fun startScroll(duration: Long, onFinished: (() -> Unit)? = null) {
        creditsBitmap?.let { bmp ->
            val animator = ValueAnimator.ofFloat(height.toFloat(), -bmp.height.toFloat())
            animator.duration = duration
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                scrollYPos = it.animatedValue as Float
                invalidate()
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onFinished?.invoke()
                }
            })
            animator.start()
        }
    }


}
