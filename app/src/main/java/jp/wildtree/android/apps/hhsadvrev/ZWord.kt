package jp.wildtree.android.apps.hhsadvrev

import java.util.Locale

class ZWord(b: ByteArray) {
    var id: Int = -1
        private set
    var word: String = ""
        private set

    init {
        for (i in 0..3) {
            val z = b[i].toUByte().toInt()
            if (z == 0) break
            word += String.format("%c", (z - 1).toByte())
        }
        id = b[4].toInt()
    }

    fun match(v: String?): Boolean {
        var z = "$v    "
        z = z.substring(0, 4).uppercase(Locale.getDefault()) // normalize
        return z.equals(word, ignoreCase = true)
    }

    companion object {
        const val INVALID_WORD: Int = 0
    }
}
