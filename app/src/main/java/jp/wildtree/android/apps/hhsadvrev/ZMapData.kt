package jp.wildtree.android.apps.hhsadvrev

import android.graphics.Color
import android.graphics.Paint
import java.nio.charset.StandardCharsets


class ZMapData(b: ByteArray) {
    internal class ZMessageMap {
        var cmdId: Int = 0
        var objId: Int = 0
        var message: String?

        init {
            cmdId = objId
            message = null
        }
    }

    private val vector: ByteArray = ByteArray(VECTOR_SIZE)
    private val map: ArrayList<ZMessageMap>
    private val msg: String?
    private var bmsg: String?
    var isBlank: Boolean = false
        private set

    init {
        map = ArrayList<ZMessageMap>()
        val message = ArrayList<String>()
        bmsg = null

        System.arraycopy(b, 0, vector, 0, VECTOR_SIZE)
        var m = VECTOR_SIZE + RELATION_SIZE
        var n = 0
        while (m < FILE_BLOCK_SIZE) {
            val len = (b[m++].toUByte().toInt() shl 8) or b[m++].toUByte().toInt()
            if (len == 0) {
                break
            }
            val bmsg = ByteArray(len)
            System.arraycopy(b, m, bmsg, 0, len)
            message.add(String(bmsg, StandardCharsets.UTF_8))
            m += len
            n++
        }
        msg = if (message.isNotEmpty()) message[0] else ""

        var j = VECTOR_SIZE
        for (i in 0..n) {
            val m = ZMessageMap()
            m.cmdId = b[j++].toUByte().toInt()
            if (m.cmdId == 0) {
                break // end of data.
            }
            m.objId = b[j++].toUByte().toInt()
            m.message = message[b[j++].toUByte().toInt() - 1]
            map.add(m)
        }
    }
    fun isBlank(bf: Boolean) {
        isBlank = bf
    }
    fun blankMessage(b: String?) {
        bmsg = b
    }
    fun blankMessage(): String? {
        return bmsg
    }
    fun find(cmdId: Int, objId: Int): String? {
        for(m in map) {
            if (m.cmdId == ZWord.INVALID_WORD) continue
            if (m.cmdId == cmdId && m.objId == objId) {
                return m.message
            }
        }
        return null
    }

    fun mapMessage(): String? {
        if (isBlank) {
            return bmsg
        }
        return msg
    }

    private fun drawOutline(
        g: ZGraphicDrawable,
        offset: Int,
        c: Int
    ): Int {
        var x0: Int
        var y0: Int
        var x1: Int
        var y1: Int
        var p = offset

        val paint = Paint()
        paint.color = c
        paint.isAntiAlias = false
        paint.style = Paint.Style.STROKE

        x0 = vector[p++].toUByte().toInt()
        y0 = vector[p++].toUByte().toInt()
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
            g.drawLine(x0, y0, x1, y1, paint)
            //pc.validateNow();
            x0 = x1
            y0 = y1
        }
        return p
    }

    private fun idraw(g: ZGraphicDrawable) {
        var i = vector[0].toUByte().toInt() * 3 + 1 // skip HALF tone data
        g.drawColor(Color.BLUE)
        //pc.validateNow();
        i = drawOutline(g, i, Color.WHITE)
        var x0 = vector[i++].toUByte().toInt()
        var y0 = vector[i++].toUByte().toInt()
        while (x0 != 0xff || y0 != 0xff) {
            val c: Int = g.color(vector[i++].toUByte().toInt())
            g.paint(x0, y0, c, Color.WHITE)
            x0 = vector[i++].toUByte().toInt()
            y0 = vector[i++].toUByte().toInt()
        }
        if (vector[i].toUByte().toInt() != 0xff || vector[i + 1].toUByte().toInt() != 0xff) {
            i = drawOutline(g, i, Color.WHITE)
        }
        else {
            i += 2
        }
        if (vector[i].toUByte().toInt() != 0xff || vector[i + 1].toUByte().toInt() != 0xff) {
            drawOutline(g, i, Color.BLACK)
        }
        g.paint(vector)
    }

    fun draw(g: ZGraphicDrawable) {
        if (isBlank) {
            g.drawColor(Color.BLACK)
            return
        }
        idraw(g)
    }

    fun forceDraw(g: ZGraphicDrawable) {
        idraw(g)
    }

    companion object {
        const val FILE_BLOCK_SIZE: Int = 0xa00
        private const val VECTOR_SIZE = 0x400
        private const val RELATION_SIZE = 0x100
        private const val MESSAGE_SIZE = 0x500
        private const val MAX_MAP_ELEMENTS = 0x100
    }
}
