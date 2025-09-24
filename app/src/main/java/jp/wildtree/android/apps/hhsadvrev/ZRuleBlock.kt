package jp.wildtree.android.apps.hhsadvrev

class ZRuleBlock(b: ByteArray) {
    val action: Int
    val op: Int
    val type: Int
    val id: Int

    val bodyType: Int
    val bodyId: Int
    val bodyValue: Int
    val bodyOffset: Int

    init {
        val header = (((b[0].toInt() and 0xff) shl 8) or (b[1].toInt() and 0xff))

        action = (header and 0x8000) shr 15
        op = (header and 0x7000) shr 12
        type = (header and 0x00e0) shr 5
        id = (header and 0x001f)

        bodyOffset = b[2].toUByte().toInt()
        bodyValue = b[3].toUByte().toInt()

        bodyType = (bodyOffset and 0xe0) shr 5
        bodyId = (bodyOffset and 0x1f)
    }
    fun getOperand1(p: ZSystemParams, u: ZUserData): Int {
        var v = 0
        when (type) {
            TYPE_NONE -> {}
            TYPE_FACT -> v = u.fact[id]
            TYPE_PLACE -> v = u.place[id]
            TYPE_SYSTEM -> {
                p.random(0)
                v = p.table[id].toInt()
            }

            TYPE_VECTOR -> v = u.map[bodyOffset - 1]!!.get(id)
            else -> {}
        }
        return v
    }

    fun getOperand2(p: ZSystemParams, u: ZUserData): Int {
        var v = bodyValue
        if (bodyType != TYPE_NONE && type != TYPE_VECTOR) {
            when (bodyType) {
                TYPE_FACT -> v = u.fact[bodyId]
                TYPE_PLACE -> v = u.place[bodyId]
                TYPE_SYSTEM -> {
                    p.random(0)
                    v = p.table[bodyId].toInt()
                }

                else -> {}
            }
        }
        return v
    }

    fun actCmp(): Boolean {
        return action == ACT_COMP
    }

    fun actAction(): Boolean {
        return action == ACT_ACTION
    }

    fun doCompare(p: ZSystemParams, u: ZUserData): Boolean {
        var ok: Boolean
        val v1 = getOperand1(p, u)
        val v2 = getOperand2(p, u)
        ok = when (op) {
            CMP_EQ -> (v1 == v2)
            CMP_NE -> (v1 != v2)
            CMP_GT -> (v1 > v2)
            CMP_GE -> (v1 >= v2)
            CMP_LT -> (v1 < v2)
            CMP_LE -> (v1 <= v2)
            else -> false
        }
        return ok
    }

    fun doAction(main: MainActivity): Boolean {
        val ok = false

        val u: ZUserData? = main.userData
        val p: ZSystemParams? = main.zSystem
        when (op) {
            ACT_MOVE -> {
                if (u!!.map[p!!.mapId() - 1]!!.get(bodyValue) != 0) {
                    p.mapId(u.map[p.mapId() - 1]!!.get(bodyValue)) // move!
                    return true
                }
                // check teacher
                if (u.fact[1] == p.mapId() && p.random() > 85) {
                    main.msgout(0xb5) // U are arrested by the teacher!!
                    u.fact[1] = 0 // teacher is gone.
                    main.setColorFilter(MainActivity.CF_MODE_SEPIA)
                    main.gameOver()
                    return false
                }
                main.msgout(0xb6) // you cannot move
                return true
            }

            ACT_ASGN -> {
                val v1 = bodyOffset
                val v2 = getOperand2(p!!, u!!)
                when (type) {
                    TYPE_FACT -> u.fact[id] = v2
                    TYPE_PLACE -> u.place[id] = v2
                    TYPE_SYSTEM -> p.table[id] = (v2 and 0xff).toByte()
                    TYPE_VECTOR -> u.map[v1 - 1]!!.set(id, v2)
                }
                if (type == TYPE_PLACE || type == TYPE_FACT) {
                    return true
                }
                if (type == TYPE_SYSTEM) {
                    if (id == 5) {
                        p.random(0)
                    }
                }
                return true
            }

            ACT_MESG -> {
                main.msgout(bodyValue)
                return true
            }

            ACT_DLOG -> {
                main.dialog(bodyValue)
                return true
            }

            ACT_LOOK -> {
                if (bodyValue == 0) {
                    p!!.mapId(p.mapView()) // back
                    p.mapView(0)
                } else {
                    p!!.mapView(p.mapId())
                    p.mapId(bodyValue)
                }
                return true
            }

            ACT_SND -> {
                // _body_value is sound number
                main.play(bodyValue)
                return true
            }

            ACT_OVER -> {
                when (bodyValue) {
                    0 -> {
                        main.setColorFilter(MainActivity.CF_MODE_SEPIA)
                        main.msgByResId(R.string.msg_gameover)
                        u!!.fact[1] = 0 // teacher has gone
                    }

                    1 -> {
                        main.setColorFilter(MainActivity.CF_MODE_RED)
                        main.msgByResId(R.string.msg_gameover)
                    }

                    2 -> {
                        main.endRoll()
                        main.setColorFilter(MainActivity.CF_MODE_NORMAL)
                        main.msgByResId(R.string.msg_finished)
                        main.gameCleared()
                        return false
                    }
                }
                main.gameOver()
                return false
            }
        }
        return ok
    }

    companion object {
        const val ACT_COMP: Int = 0
        const val ACT_ACTION: Int = 1

        const val CMP_NOP: Int = 0
        const val CMP_EQ: Int = 1
        const val CMP_NE: Int = 2
        const val CMP_GT: Int = 3
        const val CMP_GE: Int = 4
        const val CMP_LT: Int = 5
        const val CMP_LE: Int = 6

        const val ACT_NOP: Int = 0
        const val ACT_MOVE: Int = 1
        const val ACT_ASGN: Int = 2
        const val ACT_MESG: Int = 3
        const val ACT_DLOG: Int = 4
        const val ACT_LOOK: Int = 5
        const val ACT_SND: Int = 6
        const val ACT_OVER: Int = 7

        const val TYPE_NONE: Int = 0
        const val TYPE_FACT: Int = 1
        const val TYPE_PLACE: Int = 2
        const val TYPE_SYSTEM: Int = 3
        const val TYPE_VECTOR: Int = 4
    }
}
