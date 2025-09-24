package jp.wildtree.android.apps.hhsadvrev


class ZRuleBase(b: ByteArray) {
    private val mapId: Int = (b[0].toInt() and 0xff)
    private val cmdId: Int = (b[1].toInt() and 0xff)
    private val objId: Int = (b[2].toInt() and 0xff)
    private val rules: Array<ZRuleBlock?> = arrayOfNulls(RULE_BLOCK_LENGTH)

    init {
        for (i in 0 until RULE_BLOCK_LENGTH) {
            val rb = ByteArray(4)
            System.arraycopy(b, 4 + 4 * i, rb, 0, 4)
            rules[i] = ZRuleBlock(rb)
        }
    }

    fun endOfRule(): Boolean {
        return (mapId == 0xff)
    }

    fun about(m: Int, c: Int, o: Int): Boolean {
        if (m == mapId || mapId == 0) {
            if (c == cmdId || cmdId == 0) {
                if (o == objId || objId == 0) {
                    return true
                }
            }
        }
        return false
    }

    fun about(p: ZSystemParams): Boolean {
        return about(p.mapId(), p.cmdId(), p.objId())
    }

    fun run(main: MainActivity): Boolean {
        main.zSystem?.let {
            if (about(it)) {
                var condOk = true
                var actOk = true
                var i = 0
                while (rules[i]!!.actCmp()) {
                    main.userData?.let { u -> condOk = rules[i++]!!.doCompare(main.zSystem!!, u) }
                    if (!condOk) {
                        return false // cond fail.
                    }
                }
                while (rules[i]!!.op != ZRuleBlock.ACT_NOP) {
                    actOk = rules[i++]!!.doAction(main) && actOk
                }
                if (actOk) {
                    main.msgByResId(R.string.msg_okay)
                }
                return true
            }
        }
        return false
    }

    companion object {
        const val FILE_BLOCK_SIZE: Int = 96
        const val END_OF_RULE: Int = 0xff
        private const val RULE_BLOCK_LENGTH = (FILE_BLOCK_SIZE / 4 - 1)
    }
}
