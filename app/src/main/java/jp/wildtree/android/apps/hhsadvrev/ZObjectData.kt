package jp.wildtree.android.apps.hhsadvrev

import android.graphics.Color
import android.graphics.Paint

open class ZObjectData(b: ByteArray?) {
    private val vector: ByteArray = ByteArray(FILE_BLOCK_SIZE)

    init {
        if (b != null) {
            System.arraycopy(b, 0, vector, 0, FILE_BLOCK_SIZE)
        }
    }

    fun vector(): ByteArray {
        return vector
    }

    protected fun drawOutline(g: ZGraphicDrawable, offset: Int, c: Int, ox: Int, oy: Int): Int {
        var x0: Int
        var y0: Int
        var x1: Int
        var y1: Int
        var p = offset

        val paint = Paint()
        paint.color = c
        paint.isAntiAlias = false
        paint.style = Paint.Style.STROKE

        x0 = (vector[p++].toInt() and 0xff)
        y0 = (vector[p++].toInt() and 0xff)
        while (true) {
            x1 = vector[p++].toUByte().toInt()
            y1 = vector[p++].toUByte().toInt()
            if (y1 == 0xff) {
                if (x1 == 0xff) {
                    // end of lines.
                    break
                }
                x0 = vector[p++].toUByte().toInt()
                y0 = vector[p++].toUByte().toInt()
                continue
            }
            g.drawLine(x0 + ox, y0 + oy, x1 + ox, y1 + oy, paint)
            //pc.validateNow();
            x0 = x1
            y0 = y1
        }
        return p
    }

    private fun idraw(
        g: ZGraphicDrawable,
        pre: Boolean,
        offset: Int
    ) {
        var o = offset
        var b: Int = g.color(vector[o++].toUByte().toInt())
        val xs = vector[o++].toUByte().toInt() / 2
        val ys = vector[o++].toUByte().toInt()

        if (pre) {
            b = Color.rgb(0xcc, 0xcc, 0) // dark yellow
        }

        o = drawOutline(g, o, b, xs, ys)
        var x0 = vector[o++].toUByte().toInt()
        var y0 = vector[o++].toUByte().toInt()
        while (x0 != 0xff || y0 != 0xff) {
            var c: Int = g.color(vector[o++].toUByte().toInt())
            if (pre) {
                c = b
            }
            g.paint(xs + x0, ys + y0, c, b)
            x0 = vector[o++].toUByte().toInt()
            y0 = vector[o++].toUByte().toInt()
        }
    }

    open fun draw(g: ZGraphicDrawable) {
        draw(g, 0)
    }

    fun draw(g: ZGraphicDrawable, offset: Int) {
        idraw(g, true, offset)
        idraw(g, false, offset)
    }

    companion object {
        const val FILE_BLOCK_SIZE: Int = 0x200
    }
}
