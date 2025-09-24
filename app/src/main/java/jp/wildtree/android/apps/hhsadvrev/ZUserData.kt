package jp.wildtree.android.apps.hhsadvrev

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator

class ZUserData : Parcelable {
    class ZMapLink {

		var n: Int
        var s: Int
		var w: Int
        var e: Int
        var u: Int
        var d: Int
        var i: Int
        var o: Int

        constructor() {
            o = 0
            i = o
            d = i
            u = d
            e = u
            w = e
            s = w
            n = s
        }

        constructor(b: ByteArray) {
            n = b[0].toUByte().toInt()
            s = b[1].toUByte().toInt()
            w = b[2].toUByte().toInt()
            e = b[3].toUByte().toInt()
            u = b[4].toUByte().toInt()
            d = b[5].toUByte().toInt()
            i = b[6].toUByte().toInt()
            o = b[7].toUByte().toInt()
        }

        fun pack(): ByteArray {
            val b = ByteArray(8)
            b[0] = (n and 0xff).toByte()
            b[1] = (s and 0xff).toByte()
            b[2] = (w and 0xff).toByte()
            b[3] = (e and 0xff).toByte()
            b[4] = (u and 0xff).toByte()
            b[5] = (d and 0xff).toByte()
            b[6] = (i and 0xff).toByte()
            b[7] = (o and 0xff).toByte()
            return b
        }

        fun set(id: Int, value: Int) {
            when (id) {
                0 -> n = value
                1 -> s = value
                2 -> w = value
                3 -> e = value
                4 -> u = value
                5 -> d = value
                6 -> i = value
                7 -> o = value
            }
        }

        fun get(id: Int): Int {
            var v = -1
            when (id) {
                0 -> v = n
                1 -> v = s
                2 -> v = w
                3 -> v = e
                4 -> v = u
                5 -> v = d
                6 -> v = i
                7 -> v = o
            }
            return v
        }
    }


	var map: Array<ZMapLink?>
	var place: IntArray
	var fact: IntArray

    constructor(b: ByteArray) {
        map = arrayOfNulls<ZMapLink>(LINKS)
        place = IntArray(ITEMS)
        fact = IntArray(FLAGS)

        for (i in 0 until LINKS) {
            val buf = ByteArray(LINK_SIZE)
            System.arraycopy(b, i * LINK_SIZE, buf, 0, LINK_SIZE)
            map[i] = ZMapLink(buf)
        }

        for (i in 0 until ITEMS) {
            place[i] = b[ITEMS_BEGIN + i].toUByte().toInt()
        }

        for (i in 0 until FLAGS) {
            fact[i] = b[FLAGS_BEGIN + i].toUByte().toInt()
        }
    }

    constructor(source: ZUserData) {
        map = arrayOfNulls<ZMapLink>(LINKS)
        place = IntArray(ITEMS)
        fact = IntArray(FLAGS)

        for (i in 0 until LINKS) {
            map[i] = ZMapLink(source.map[i]!!.pack())
        }
        for (i in 0 until ITEMS) {
            place[i] = source.place[i]
        }
        for (i in 0 until FLAGS) {
            fact[i] = source.fact[i]
        }
    }

    private constructor(source: Parcel) {
        map = arrayOfNulls<ZMapLink>(LINKS)
        place = IntArray(ITEMS)
        fact = IntArray(FLAGS)
        val buf = ByteArray(LINK_SIZE)
        for (i in 0 until LINKS) {
            source.readByteArray(buf)
            map[i] = ZMapLink(buf)
        }
        source.readIntArray(place)
        source.readIntArray(fact)
    }

    fun pack(): ByteArray {
        val buf = ByteArray(PACKED_SIZE)
        for (i in 0 until LINKS) {
            System.arraycopy(map[i]!!.pack(), 0, buf, i * LINK_SIZE, LINK_SIZE)
        }
        for (i in 0 until ITEMS) {
            buf[LINKS * LINK_SIZE + i] = (place[i] and 0xff).toByte()
        }
        for (i in 0 until FLAGS) {
            buf[LINKS * LINK_SIZE + ITEMS + i] = (fact[i] and 0xff).toByte()
        }
        return buf
    }

    fun unpack(b: ByteArray) {
        val tmp = ByteArray(LINK_SIZE)
        for (i in 0 until LINKS) {
            System.arraycopy(b, i * LINK_SIZE, tmp, 0, LINK_SIZE)
            map[i] = ZMapLink(tmp)
        }
        for (i in 0 until ITEMS) {
            place[i] = b[LINKS * LINK_SIZE + i].toUByte().toInt()
        }
        for (i in 0 until FLAGS) {
            fact[i] = b[LINKS * LINK_SIZE + ITEMS + i].toUByte().toInt()
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        for (i in 0 until LINKS) {
            dest.writeByteArray(map[i]!!.pack())
        }
        dest.writeIntArray(place)
        dest.writeIntArray(fact)
    }


    companion object {
        const val LINK_SIZE: Int = 8
        const val LINKS: Int = 87
        const val ITEMS: Int = 12
        const val FLAGS: Int = 15
        const val ITEMS_BEGIN: Int = 0x301
        const val FLAGS_BEGIN: Int = 0x311
        const val FILE_BLOCK_SIZE: Int = 0x800
        const val PACKED_SIZE: Int = LINKS * LINK_SIZE + ITEMS + FLAGS

        @JvmField
        val CREATOR: Creator<ZUserData?> = object : Creator<ZUserData?> {
            override fun createFromParcel(source: Parcel): ZUserData {
                return ZUserData(source)
            }

            override fun newArray(size: Int): Array<ZUserData?> {
                return arrayOfNulls(size)
            }
        }
    }
}
