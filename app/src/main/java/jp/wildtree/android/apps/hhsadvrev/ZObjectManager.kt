package jp.wildtree.android.apps.hhsadvrev

import android.content.res.Resources

class ZObjectManager {
    var resources: Resources?= null
    var obj: ZObjectData? = null
        private set
    var objId: Int = 0
        set(value) {
            try {
                val `is` = resources!!.assets.open("thin.dat")
                val buf = ByteArray(ZObjectData.FILE_BLOCK_SIZE)
                `is`.skip((value * ZObjectData.FILE_BLOCK_SIZE).toLong())
                `is`.read(buf)
                `is`.close()
                obj = if (value == 14) ZTeacherData(buf) else ZObjectData(buf)
                field = value
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
}