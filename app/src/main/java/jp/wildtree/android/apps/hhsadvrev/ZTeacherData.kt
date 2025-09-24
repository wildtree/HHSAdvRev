/**
 *
 */
package jp.wildtree.android.apps.hhsadvrev

import android.graphics.Color
import android.graphics.Paint

/**
 * @author araki
 */
class ZTeacherData(b: ByteArray?) : ZObjectData(b) {
    override fun draw(g: ZGraphicDrawable) {
        val ox = -32
        var x0: Int
        var x1: Int
        var y0: Int
        var y1: Int
        val r1 = intArrayOf(18, 24, 2, 2, 2, 22, 9, 0xffff)
        val r2 = intArrayOf(148, 14, 126, 6, 0, 0)
        y0 = 63

        val paint = Paint()
        paint.color = Color.BLUE
        paint.isAntiAlias = false
        paint.style = Paint.Style.STROKE
        var i = 0
        while (i <= 172) {
            x0 = vector()[i].toUByte().toInt()
            x1 = vector()[i + 1].toUByte().toInt()
            g.drawLine(ox + x0, y0, ox + x1, y0, paint)
            ++y0
            i += 2
        }
        var c = 0
        run {
            var j = 0
            while (r1[j] != 0xffff) {
                c = g.color(vector()[i++].toUInt().toInt())
                if (c == Color.RED) {
                    c = Color.rgb(0xcc, 0x00, 0x00)
                }
                x0 = vector()[i++].toUByte().toInt()
                y0 = vector()[i++].toUByte().toInt()
                (0..r1[j] + 1).forEach { k ->
                    x1 = vector()[i++].toUByte().toInt()
                    y1 = vector()[i++].toUByte().toInt()
                    paint.color = c
                    g.drawLine(ox + x0, y0, ox + x1, y1, paint)
                    x0 = x1
                    y0 = y1
                }
                x0 = vector()[i++].toUByte().toInt()
                y0 = vector()[i++].toUByte().toInt()
                g.paint(ox + x0, y0, c, c)
                j++
            }
        }
        x0 = vector()[i++].toUByte().toInt()
        y0 = vector()[i++].toUByte().toInt()
        g.paint(ox + x0, y0, c, c)
        for (j in 120..123) {
            paint.color = Color.YELLOW
            g.drawLine(ox + j, 64, ox + j + 8, 110, paint)
            paint.color = Color.WHITE
            g.drawLine(ox + j + 9, 110, ox + j + 11, 126, paint)
        }
        paint.color = Color.RED
        g.drawLine(ox + 125, 111, ox + 133, 109, paint)
        g.drawLine(ox + 133, 109, ox + 134, 110, paint)
        g.drawLine(ox + 134, 110, ox + 125, 112, paint)
        g.drawLine(ox + 125, 112, ox + 125, 111, paint)
        paint.color = Color.WHITE
        g.drawLine(ox + 120, 65, ox + 123, 64, paint)
        g.drawLine(ox + 123, 64, ox + 121, 62, paint)
        g.drawLine(ox + 121, 62, ox + 120, 65, paint)

        g.paint(ox + 122, 63, Color.WHITE, Color.WHITE)

        var k = 0
        while (r2[k + 1] != 0) {
            x0 = r2[k]
            paint.color = Color.rgb(0xff, 0xaa, 0xaa)
            var j = 0
            while (j < r2[k + 1]) {
                y0 = vector()[i++].toUByte().toInt()
                y1 = vector()[i++].toUByte().toInt()
                if (g.tiling()) {
                    paint.color = Color.MAGENTA
                }
                g.drawLine(ox + x0, y0, ox + x0, y1, paint)
                ++x0
                if (g.tiling()) {
                    paint.color = Color.YELLOW
                }
                g.drawLine(ox + x0, y0, ox + x0, y1, paint)
                ++x0
                y0 = vector()[i++].toUByte().toInt()
                y1 = vector()[i++].toUByte().toInt()
                if (g.tiling()) {
                    paint.color = Color.WHITE
                }
                g.drawLine(ox + x0, y0, ox + x0, y1, paint)
                ++x0
                j += 2
            }
            k += 2
        }
        g.drawRectangle(ox + 148, 78, ox + 164, 84, Color.BLACK)
        g.fillRectangle(ox + 149, 79, ox + 163, 83, Color.WHITE)
        g.fillRectangle(ox + 155, 78, ox + 156, 84, Color.BLACK)

        while (true) {
            x1 = vector()[i++].toUByte().toInt()
            y1 = vector()[i++].toUByte().toInt()
            if (y1 == 0xff) {
                if (x1 == 0xff) {
                    break
                }
                x0 = vector()[i++].toUByte().toInt()
                y0 = vector()[i++].toUByte().toInt()
                continue
            }
            paint.color = Color.BLACK
            g.drawLine(ox + x0, y0, ox + x1, y1, paint)
            x0 = x1
            y0 = y1
        }
    }
}
