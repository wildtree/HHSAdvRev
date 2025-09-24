package jp.wildtree.android.apps.hhsadvrev

import android.content.res.Resources

class ZMapManager {
    var mapData: ZMapData? = null
        private set

    var resources: Resources? = null
    var mapId: Int = 0
        set(value)
        {
            try {
                val `is` = resources!!.assets.open("map.dat")
                val buf = ByteArray(ZMapData.FILE_BLOCK_SIZE)
                `is`.skip((value * ZMapData.FILE_BLOCK_SIZE).toLong())
                `is`.read(buf)
                mapData = ZMapData(buf)
                if (value == 0 || value == 84 || value == 85) {
                    val msg = resources!!.getStringArray(R.array.messages)
                    mapData!!.isBlank(true)
                    mapData!!.blankMessage(msg[0x4c])
                }
                `is`.close()
                field = value
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

}