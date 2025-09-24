package jp.wildtree.android.apps.hhsadvrev

import android.util.Log
import java.util.Random


class ZSystemParams {
    var table: ByteArray
    private val r: Random

    constructor() {
        table = ByteArray(SIZE)
        r = Random()
        r.setSeed(System.currentTimeMillis())
    }

    constructor(b: ByteArray) {
        table = ByteArray(SIZE)
        System.arraycopy(b, 0, table, 0, SIZE)
        r = Random()
    }

    fun pack(): ByteArray {
        return table
    }

    fun unpack(b: ByteArray) {
        System.arraycopy(b, 0, table, 0, SIZE)
        random(0)
    }

    private fun getInt(index: Int): Int {
        return (table[index].toInt() and 0xff)
    }

    private fun setInt(index: Int, v: Int) {
        table[index] = (v and 0xff).toByte()
    }

    fun mapId(): Int {
        return getInt(0)
    }

    fun mapId(v: Int) {
        setInt(0, v)
    }

    fun mapView(): Int {
        return getInt(1)
    }

    fun mapView(v: Int) {
        setInt(1, v)
    }

    fun cmdId(): Int {
        return getInt(2)
    }

    fun cmdId(v: Int) {
        setInt(2, v)
    }

    fun objId(): Int {
        return getInt(3)
    }

    fun objId(v: Int) {
        setInt(3, v)
    }

    fun dlgres(): Int {
        return getInt(4)
    }

    fun dlgres(v: Int) {
        setInt(4, v)
    }

    fun random(): Int {
        Log.d("System", getInt(5).toString())
        random(0)
        return getInt(5)
    }

    fun random(seed: Long) {
        if (seed != 0L) {
            r.setSeed(seed)
        }
        setInt(5, r.nextInt(256))
    }

    fun dlgOk(): Int {
        return getInt(6)
    }

    fun dlgOk(v: Int) {
        setInt(6, v)
    }

    fun dlgMessage(): Int {
        return getInt(7)
    }

    fun dlgMessage(v: Int) {
        setInt(7, v)
    }

    fun getRandom(d: Int): Int {
        return r.nextInt(d)
    }

    companion object {
        const val SIZE: Int = 8
    }
}
