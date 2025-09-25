package jp.wildtree.android.apps.hhsadvrev

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.backup.BackupManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.createBitmap
import androidx.preference.PreferenceManager
import jp.wildtree.android.apps.hhsadvrev.R.array
import jp.wildtree.android.apps.hhsadvrev.R.drawable
import jp.wildtree.android.apps.hhsadvrev.R.id
import jp.wildtree.android.apps.hhsadvrev.R.layout
import jp.wildtree.android.apps.hhsadvrev.R.raw
import jp.wildtree.android.apps.hhsadvrev.R.string
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.function.Function

class MainActivity : AppCompatActivity() {
    enum class DataStorage {
        MEMORY,
        SDCARD,
    }

    private var bmgr: BackupManager? = null

    private lateinit var cmdList: Array<ZWord?>
    private lateinit var objList: Array<ZWord?>
    private lateinit var rules: Array<ZRuleBase?>
    //lateinit var map: Array<ZMapData?>
    val mapManager = ZMapManager()
    val objManager = ZObjectManager()
    var userData: ZUserData? = null
    private var initData: ZUserData? = null
    var zSystem: ZSystemParams? = null
    private lateinit var msg: Array<String?>
    private var zg: ZGraphicDrawable? = null
    private var mbuffer: ArrayList<String?>? = null
    private var starting = false
    private var gameover = false
    private var cleared = false
    var cfMode: Int = 0

    private var dialogId = 0

    var mContext: Context? = null
    private var sv: ScrollView? = null
    private var tv: TextView? = null
    private var iv: ImageView? = null

    private var fileno = 0
    private var cutline = 0

    private var mp: MediaPlayer? = null
    private var am: AudioManager? = null
    private var sp: SharedPreferences? = null

    private var pd: ProgressBar? = null
    private var mHandler: Handler? = null
    private var loader: Thread? = null
    private var signal: CountDownLatch? = null

    private val prefKeyPlaysounds = "playSounds"

    @SuppressLint("DefaultLocale")
    private val prefActivityLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        bmgr!!.dataChanged() // backup prefs into cloud if enabled.
        // redraw if necessary
        zg!!.tiling(sp!!.getBoolean("prefUseTiling", false))
        draw(false)
        // sound management
        val ps = sp!!.getBoolean("playSounds", true)
        val fs = sp!!.getBoolean("prefFollowSilent", true)
        val rn = (am!!.ringerMode == AudioManager.RINGER_MODE_NORMAL)
        // stop if mode changed.
        if (!ps || (fs && !rn)) {
            if (mp != null && mp!!.isPlaying) {
                mp!!.stop()
                mp!!.release()
            }
        }
        // data management
        // copy data and remove old version.
        for (i in 0..2) {
            val name = String.format("data%d.dat", i + 1)
            var oFile = File(mContext!!.filesDir, name)
            val dir = File(getExternalFilesDir(null), BASE_DIR)
            var iFile = File(dir, name)
            if (DataStorage.SDCARD == DataStorage.valueOf(
                    sp!!.getString(
                        "pref_use_sdcard",
                        "MEMORY"
                    )!!
                )
            ) {
                // to sdcard
                val tmp = oFile
                oFile = iFile
                iFile = tmp
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            if (!iFile.exists()) {
                // skip copying.
                continue
            }
            try {
                synchronized(fsync) {
                    val iChannel = FileInputStream(iFile).channel
                    val oChannel = FileOutputStream(oFile).channel
                    iChannel.transferTo(0, iChannel.size(), oChannel)
                    iChannel.close()
                    oChannel.close()
                    iFile.delete() // delete a source file.
                }
            } catch (e: FileNotFoundException) {
                // TODO  catch ubN
                e.printStackTrace()
            } catch (e: IOException) {
                // TODO  catch ubN
                e.printStackTrace()
            }
        }
    }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.main)

        mContext = this
        mHandler = Handler(Looper.getMainLooper())
        sp = PreferenceManager.getDefaultSharedPreferences(mContext!!)
        am = getSystemService(AUDIO_SERVICE) as AudioManager
        bmgr = BackupManager(mContext)

        sv = findViewById(id.scrollBox)
        tv = findViewById(id.messageBox)
        tv!!.textSize = sp!!.getInt("prefFontSize", 16).toFloat()
        msg = getResources().getStringArray(array.messages)
        mbuffer = ArrayList()
        gameover = true
        starting = true
        cleared = false
        dialogId = -1
        mapManager.resources = resources
        objManager.resources = resources


        val edit = findViewById<EditText>(id.cmdline)
        edit.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_SEND)
            {
                val ime = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                keyToGo()
                v.requestFocus()
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    ime.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
            }
            true
        }
        edit.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                val ime = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                keyToGo()
                v.requestFocus()
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    ime.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                true
            }
            else {
                false
            }
        }


        val mtb = findViewById<Toolbar?>(id.toolbar)
        setSupportActionBar(mtb)

        val actionRecognizerLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult? ->
            if (result!!.resultCode == RESULT_OK) {
                // success!!
                val results =
                    result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                var rs = results!![0]
                for (r in results) {
                    val list: Array<String?> =
                        r.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val c = scanVerbs(list[0])
                    var o = ZWord.INVALID_WORD
                    if (list.size > 2) {
                        o = scanObjs(list[1])
                    }
                    if (c != ZWord.INVALID_WORD && o != ZWord.INVALID_WORD) {
                        rs = list[0] + " " + list[1]
                        break
                    }
                }
                // put recognized result into cmdline field and set a cursor at the tail of it.
                edit.setText(rs)
                edit.setSelection(rs.length)
            }
        }
        val btnSpeak = findViewById<ImageButton>(id.btnSpeak)
        btnSpeak.setOnClickListener { v: View? ->
            // TODO 自動生成されたメソッド・スタブ
            val si = Intent()
            si.action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            var locale = Locale.US.toString()
            if (sp!!.getString("prefRecEnglish", "US").equals("UK", ignoreCase = true)) {
                locale = Locale.UK.toString()
            }
            si.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            si.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            si.putExtra(RecognizerIntent.EXTRA_PROMPT, getText(string.label_prompt_recognizer))
            actionRecognizerLauncher.launch(si)
        }
        btnSpeak.visibility = View.GONE
        btnSpeak.isEnabled = false

        iv = findViewById(id.basicScreenView)
        zg = ZGraphicDrawable(
            mContext!!.resources,
            createBitmap(WIDTH, HEIGHT)
        )
        iv!!.setImageDrawable(zg)
        zg!!.parent(iv)
        zg!!.initColorMatrices()
        zg!!.tiling(sp!!.getBoolean("prefUseTiling", false))
        cfMode = 0


        val isFresh = (savedInstanceState == null)
        signal = CountDownLatch(1)
        loader = Thread {
            initData(isFresh)
        }
        loader!!.start()

        pd = findViewById(id.progressBar)
        pd!!.visibility = View.GONE
        pd!!.max = 100
        pd!!.min = 0
    }

    private fun keyToGo() {
        // start parsing
        val edit = findViewById<EditText>(id.cmdline)
        val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(edit.windowToken, 0)
        parser(edit.text)
        edit.setText("")
    }

    override fun onDestroy() {
        super.onDestroy()
        // タイムスタンプとスレッド情報
        Log.w("LifecycleTrace", "onDestroy() called at ${System.currentTimeMillis()} on thread ${Thread.currentThread().name}")

        // 呼び出し元スタックを取得（OS kill の場合は空になることもある）
        val stackTrace = Throwable().stackTrace.joinToString("\n")
        Log.w("LifecycleTrace", "Call stack:\n$stackTrace")

        // プロセスの状態を確認
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = am.runningAppProcesses
        runningAppProcesses?.find { it.pid == android.os.Process.myPid() }?.let {
            Log.w("LifecycleTrace", "Process importance: ${it.importance}")
        }

        // メモリ状況を確認
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        Log.w("LifecycleTrace", "availMem=${memInfo.availMem} lowMemory=${memInfo.lowMemory} threshold=${memInfo.threshold}")

    }

    override fun onResume() {
        super.onResume()
        val fontSize = sp!!.getInt("prefFontSize", 16)
        tv!!.textSize = fontSize.toFloat()
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        if (!starting && !cleared)
        {
            val edit = findViewById<EditText>(id.cmdline)
            edit.visibility = View.VISIBLE
            edit.isEnabled = true
            edit.setFocusable(true)
            edit.isFocusableInTouchMode = true
            val btnSpeak = findViewById<ImageButton>(id.btnSpeak)
            btnSpeak.visibility = View.VISIBLE
            btnSpeak.isEnabled = true

            userData = ZUserData(initData!!)

            iv!!.clearColorFilter()
            cfMode = 0
            zSystem!!.mapId(START_PAGE)
            gameover = false
            mbuffer!!.clear()
            msgflush()
            draw(true)
        }
        else if (cleared)
        {
            setColorFilter(MainActivity.CF_MODE_NORMAL)
            msgByResId(R.string.msg_finished)
            msgByResId(string.msg_tap_screen_to_title)
            gameEnding()
            draw(false)
        }
    }

    fun initData(fresh: Boolean) {
        zSystem = ZSystemParams()

        readDictionary()
        readRulebase()
        readInitUserData()

        userData = ZUserData(initData!!) // avoid refernce
        pd!!.visibility = View.GONE
        signal!!.countDown()

        if (fresh) {
            zSystem!!.mapId(TITLE_PAGE)

            mHandler!!.post {
                titleScreen()
                mapManager.mapId = TITLE_PAGE
                mapManager.mapData!!.draw(zg!!)
            }
        }
    }

    fun setColorFilter(mode: Int) {
        when (mode) {
            CF_MODE_NORMAL -> iv!!.clearColorFilter()
            CF_MODE_BLUE -> iv!!.colorFilter = zg!!.blueFilter()
            CF_MODE_RED -> iv!!.colorFilter = zg!!.redFilter()
            CF_MODE_SEPIA -> iv!!.colorFilter = zg!!.sepiaFilter()
        }
        cfMode = mode
        iv!!.invalidate()
    }

    private fun titleScreen() {
        starting = true
        mbuffer!!.clear()
        msgByResId(string.app_name)
        msgout("")
        msgByResId(string.copyright)
        msgout("")
        msgByResId(string.msg_tap_screen_to_start)

        val edit = findViewById<EditText>(id.cmdline)
        edit.visibility = View.GONE
        val btnSpeak = findViewById<ImageButton>(id.btnSpeak)
        btnSpeak.visibility = View.GONE

        iv!!.isClickable = true
        iv!!.setOnClickListener { v: View? ->
            start()
        }
    }

    private fun msgflush() {
        if (mbuffer!!.isNotEmpty()) {
            var out = Optional.ofNullable<String?>(mbuffer!![0]).map(
                Function { str: String? -> StringBuilder(str!!) }).orElse(null)
            for (i in 1 until mbuffer!!.size) {
                out =
                    (out ?: java.lang.StringBuilder("null")).append("\n").append(
                        mbuffer!![i]
                    )
            }
            tv!!.text = out?.toString()
            sv!!.post {
                sv!!.scrollTo(0, tv!!.height)
            }
        } else {
            tv!!.text = null
        }
    }

    fun msgout(out: String?) {
        if (out == null) {
            return
        }

        mbuffer!!.add(out)
        while (mbuffer!!.size > 100) {
            mbuffer!!.removeAt(0)
        }
        msgflush()
    }

    fun msgout(id: Int) {
        var id = id
        val out: String?
        if ((id and 0x80) == 0) {
            mapManager.mapId = zSystem!!.mapId()
            out = mapManager.mapData!!.find(zSystem!!.cmdId(), zSystem!!.objId())
        } else {
            id = id and 0x7f // mask.
            out = msg[id]
        }
        msgout(out)
    }

    fun msgByResId(resid: Int) {
        msgout(getText(resid).toString())
    }

    fun drawObject(withmsg: Boolean) {
        for (i in 0..11) {
            if (userData!!.place[i] == zSystem!!.mapId()) {
                if (i == 1 && userData!!.fact[0] != 1)  // UNIFORM
                {
                    objManager.objId = i + 1
                    objManager.obj!!.draw(zg!!, 256)
                } else {
                    objManager.objId = i + 1
                    objManager.obj!!.draw(zg!!)
                }
                if (withmsg && !gameover) {
                    msgout(0x96 + i)
                }
            }
        }
    }

    private fun checkDarkness() {
        when (zSystem!!.mapId()) {
            47, 48, 49, 61, 64, 65, 67, 68, 69, 71, 74, 75, 77 -> if (userData!!.fact[7] != 0) {
                if (userData!!.fact[6] != 0) {
                    // blue
                    setColorFilter(CF_MODE_BLUE)
                }
            } else {
                zSystem!!.mapView(zSystem!!.mapId())
                zSystem!!.mapId(84)
            }

            else -> if (userData!!.fact[6] != 0) {
                // back to normal
                iv!!.clearColorFilter()
                cfMode = 0
                iv!!.invalidate()
            }
        }
    }

    private fun start() {
        starting = false
        cleared = false

        if (!sp!!.getBoolean("prefSkipOpening", false)) {
            val i = Intent(mContext, ZCreditRollActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.putExtra("credits", raw.opening)
            val o = ActivityOptions.makeCustomAnimation(
                this,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            startActivity(i, o.toBundle())
        }

    }

    private fun readInitUserData() {
        val `as` = getResources().assets

        try {
            val `is` = `as`.open("data.dat")
            val buf = ByteArray(ZUserData.FILE_BLOCK_SIZE)
            if (`is`.read(buf) > 0) {
                // load initial user data.
                initData = ZUserData(buf)
            }
            `is`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readDictionary() {
        cmdList = arrayOfNulls(MAX_VERBS)
        objList = arrayOfNulls(MAX_OBJS)
        val `as` = getResources().assets
        try {
            val `is` = `as`.open("highds.com")
            var len = 0
            var sz: Int
            var i = 0
            val buf = ByteArray(5)
            while ((`is`.read(buf).also { sz = it }) > 0) {
                len += sz
                if (len >= 0x200 || buf[0].toInt() == 0) {
                    break
                }
                cmdList[i++] = ZWord(buf)
            }
            `is`.skip((0x200 - len).toLong())
            len = 0
            i = 0
            while ((`is`.read(buf).also { sz = it }) > 0) {
                len += sz
                if (len >= 0x200 || buf[0].toInt() == 0) {
                    break
                }
                objList[i++] = ZWord(buf)
            }
            `is`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readRulebase() {
        val `as` = getResources().assets
        rules = arrayOfNulls(MAX_RULES)
        try {
            val `is` = `as`.open("rule.dat")
            val buf = ByteArray(ZRuleBase.FILE_BLOCK_SIZE)
            var i = 0
            while (`is`.read(buf) > 0) {
                rules[i++] = ZRuleBase(buf)
            }
            `is`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        try {
            signal!!.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        zSystem = ZSystemParams(savedInstanceState.getByteArray("sys")!!)
        userData = savedInstanceState.getParcelable("user", ZUserData::class.java)
        mbuffer = savedInstanceState.getStringArrayList("mbuffer")
        gameover = savedInstanceState.getBoolean("gameover")
        starting = savedInstanceState.getBoolean("starting")
        cleared = savedInstanceState.getBoolean("cleared")
        fileno = savedInstanceState.getInt("fileno")
        cutline = savedInstanceState.getInt("cutline")
        dialogId = savedInstanceState.getInt("dialogId")
        cfMode = savedInstanceState.getInt("cf_mode")

        if (!gameover) {
            iv!!.isClickable = false
            val edit = findViewById<EditText>(id.cmdline)
            edit.isEnabled = true
            edit.setFocusable(true)
            edit.isFocusableInTouchMode = true
            edit.setTransitionVisibility(View.VISIBLE)
            val btnSpeak = findViewById<ImageButton>(id.btnSpeak)
            btnSpeak.isEnabled = true
            btnSpeak.visibility = View.VISIBLE
        } else {
            if (starting) {
                titleScreen()
            } else {
                gameEnding()
            }
        }
        if (dialogId >= 0) {
            dialog(dialogId)
        }
        msgflush()
        draw(false)
        setColorFilter(cfMode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putByteArray("sys", zSystem!!.pack())
        outState.putParcelable("user", userData)
        outState.putStringArrayList("mbuffer", mbuffer)
        outState.putBoolean("gameover", gameover)
        outState.putBoolean("starting", starting)
        outState.putBoolean("cleared", cleared)
        outState.putInt("fileno", fileno)
        outState.putInt("cutline", cutline)
        outState.putInt("dialogId", dialogId)
        outState.putInt("cf_mode", cfMode)
    }

    private fun scanVerbs(s: String?): Int {
        if (s == null) {
            return ZWord.INVALID_WORD
        }
        for (i in 0 until MAX_VERBS) {
            if (cmdList[i] != null && cmdList[i]!!.match(s)) {
                return cmdList[i]!!.id
            }
        }
        return ZWord.INVALID_WORD
    }

    private fun scanObjs(s: String?): Int {
        if (s == null) {
            return ZWord.INVALID_WORD
        }
        for (i in 0 until MAX_OBJS) {
            if (objList[i] != null && objList[i]!!.match(s)) {
                return objList[i]!!.id
            }
        }
        return ZWord.INVALID_WORD
    }

    private fun disableEditField() {
        val edit = findViewById<EditText>(id.cmdline)
        edit.isEnabled = false
        edit.setFocusable(false)
        edit.isFocusableInTouchMode = false
        val btnSpeak = findViewById<ImageButton>(id.btnSpeak)
        btnSpeak.isEnabled = false
    }

    private fun gameEnding() {
        disableEditField()
        iv!!.isClickable = true

        iv!!.setOnClickListener { arg0: View? ->
            titleScreen()
            setColorFilter(CF_MODE_NORMAL)
            zSystem!!.mapId(TITLE_PAGE)
            mapManager.mapId = TITLE_PAGE
            mapManager.mapData!!.draw(zg!!)
        }
        gameover = true
    }

    fun gameOver() {
        msgByResId(string.msg_tap_screen_to_title)
        cleared = false
        gameEnding()
    }

    fun gameCleared() {
        //msgByResId(string.msg_tap_screen_to_title)
        cleared = true
        //gameEnding()
    }

    private fun progress() {
        if (userData!!.fact[3] > 0 && userData!!.fact[7] == 1) { // light on
            --userData!!.fact[3] /* battery */
            if (userData!!.fact[3] < 8 && userData!!.fact[3] > 0) {
                userData!!.fact[6] = 1 // dim
                msgout(0xd9)
            }
            if (userData!!.fact[3] == 0) { /* battery wear out */
                userData!!.fact[7] = 0 // light off
                msgout(0xc0)
            }
        }
        // count down
        if (userData!!.fact[11] > 0) { /* count down */
            --userData!!.fact[11]
            if (userData!!.fact[11] == 0) {
                play(2) // explosion sound.
                msgout(0xd8)
                if (userData!!.place[7] == 48) {
                    userData!!.map[75 - 1]!!.n = 77
                    userData!!.map[68 - 1]!!.w = 77
                    msgout(0xda)
                }
                if (userData!!.place[7] == 255 || userData!!.place[7] == zSystem!!.mapId()) {
                    /* explosion within the room where you are */
                    /* change screen to red */
                    setColorFilter(CF_MODE_RED)
                    msgout(0xcf)
                    msgout(0xcb)
                    gameOver()
                } else {
                    userData!!.place[7] = 0 /* explosion ... lose bomber */
                }
            }
        }
    }

    private fun checkTeacher() {
        if (gameover || userData!!.fact[1] == zSystem!!.mapId()) {
            return
        }

        val rd = 100 + zSystem!!.mapId() + (if (userData!!.fact[1] > 0) 1000 else 0)
        val rz = zSystem!!.getRandom(3000)
        if (rd < rz) {
            userData!!.fact[1] = 0
        } else {
            userData!!.fact[1] = zSystem!!.mapId()
        }
        when (zSystem!!.mapId()) {
            1, 48, 50, 51, 52, 53, 61, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 83, 86 -> userData!!.fact[1] =
                0
        }
    }

    private fun draw(withmsg: Boolean) {
        zg!!.tiling(sp!!.getBoolean("prefUseTiling", false))
        checkDarkness()
        mapManager.mapId = zSystem!!.mapId()
        mapManager.mapData!!.draw(zg!!)
        if (withmsg && !gameover) {
            msgout(mapManager.mapData!!.mapMessage())
        }
        drawObject(withmsg)
        if (userData!!.fact[1] == zSystem!!.mapId()) {
            objManager.objId = 14
            objManager.obj!!.draw(zg!!)
            if (withmsg && !gameover) {
                msgout(0xb4)
            }
        }
        iv!!.invalidate()
    }

    fun play(id: Int) {
        val m = am!!.ringerMode
        if (!sp!!.getBoolean(prefKeyPlaysounds, true) ||
            (sp!!.getBoolean("prefFollowSilent", true) && m != AudioManager.RINGER_MODE_NORMAL)
        ) {
            return
        }
        var resId = 0
        when (id) {
            0 ->                 // �Z��
                resId = raw.highschool

            1 -> resId = raw.charumera
            2 -> resId = raw.explosion
            4 -> resId = raw.in_toilet
            5 -> resId = raw.acid
        }
        if (resId == 0) {
            return
        }
        mp = MediaPlayer.create(mContext, resId)
        if (mp!!.isPlaying) {
            mp!!.stop()
            mp!!.release()
        }
        mp!!.setOnCompletionListener { arg0: MediaPlayer? ->
            arg0!!.stop()
            arg0.release()
        }
        mp!!.start()
    }

    private fun loadGame(file: Int) {
        val name = "data$file.dat"
        val dest = DataStorage.valueOf(sp!!.getString("pref_use_sdcard", "MEMORY")!!)
        val sdcard = dest == DataStorage.SDCARD
        try {
            val sb = ByteArray(ZSystemParams.SIZE)
            val ub = ByteArray(ZUserData.PACKED_SIZE)
            val `in`: FileInputStream
            synchronized(fsync) {
                if (sdcard) {
                    val baseDir = File(getExternalFilesDir(null), BASE_DIR)
                    val data = File(baseDir, name)
                    `in` = FileInputStream(data)
                } else {
                    `in` = openFileInput(name)
                }
                val buf = BufferedInputStream(`in`)
                buf.read(sb, 0, ZSystemParams.SIZE)
                buf.read(ub, 0, ZUserData.PACKED_SIZE)
                buf.close()
                `in`.close()
            }
            zSystem!!.unpack(sb)
            userData!!.unpack(ub)
            // DAN found the issue
            setColorFilter(CF_MODE_NORMAL)
            checkDarkness()
        } catch (e: FileNotFoundException) {
            val errMsg = String.format(getString(string.err_file_not_found), file)
            Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        msgout(fileno.toString() + getText(string.msg_selected_fileno))
        msgByResId(string.msg_loaded)
    }

    private fun saveGame(file: Int) {
        val name = "data$file.dat"
        val dest = DataStorage.valueOf(sp!!.getString("pref_use_sdcard", "MEMORY")!!)
        val sdcard = dest == DataStorage.SDCARD
        try {
            synchronized(fsync) {
                var out: OutputStream?
                if (sdcard) {
                    val dir = File(getExternalFilesDir(null), BASE_DIR)
                    val data = File(dir, name)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    if (data.exists()) {
                        data.delete()
                    }
                    if (!data.createNewFile()) {
                        // failed to create a new file.
                        return
                    }
                    out = FileOutputStream(data)
                } else {
                    out = openFileOutput(name, MODE_PRIVATE)
                }
                val buf = BufferedOutputStream(out)
                buf.write(zSystem!!.pack())
                buf.write(userData!!.pack())
                buf.flush()
                out.close()
            }
            bmgr!!.dataChanged() // backup it!!
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        msgout(fileno.toString() + getText(string.msg_selected_fileno))
        msgByResId(string.msg_saved)
    }

    private fun dismissDialog() {
        dialogId = -1
        checkTeacher()
        draw(true)
    }

    fun dialog(id: Int) {
        dialogId = id
        when (id) {
            0 -> {
                val genders = getResources().getStringArray(array.dialog_gender)
                userData!!.fact[0] = 1 // default is Boy
                AlertDialog.Builder(mContext).setTitle(string.title_gender).setSingleChoiceItems(
                    genders,
                    0
                ) { dialog: DialogInterface?, which: Int ->
                    userData!!.fact[0] = which + 1
                }.setPositiveButton(
                    string.label_okay
                ) { dialog: DialogInterface?, which: Int ->
                    if (userData!!.fact[0] == 0) {
                        userData!!.fact[0] = 1
                    }
                    zSystem!!.mapId(3) // enter the room.
                    dismissDialog()
                }.setNegativeButton(
                    string.label_cancel
                ) { dialog: DialogInterface?, which: Int ->
                    userData!!.fact[0] = 0
                    dismissDialog()
                }.setOnCancelListener { dialog: DialogInterface? ->
                    dismissDialog()
                }.show()
            }

            1 -> {
                val files = getResources().getStringArray(array.dialog_file)
                fileno = 0
                AlertDialog.Builder(mContext).setTitle(string.title_file).setSingleChoiceItems(
                    files,
                    0
                ) { dialog: DialogInterface?, which: Int ->
                    fileno = which + 1
                }.setPositiveButton(
                    string.label_okay
                ) { dialog: DialogInterface?, which: Int ->
                    if (fileno == 0) {
                        fileno = 1
                    }
                    if (zSystem!!.cmdId() == 0xf) {
                        saveGame(fileno)
                    } else {
                        loadGame(fileno)
                    }
                    dismissDialog()
                }.setNegativeButton(
                    string.label_cancel
                ) { dialog: DialogInterface?, which: Int ->
                    dismissDialog()
                }.setOnCancelListener { dialog: DialogInterface? ->
                    dismissDialog()
                }.show()
            }

            2 -> {
                val items = getResources().getStringArray(array.items)
                val has = ArrayList<String?>(items.size)

                var i = 0
                while (i < items.size) {
                    if (userData!!.place[i] == 0xff) {
                        has.add(items[i])
                    }
                    i++
                }
                if (has.isEmpty()) {
                    AlertDialog.Builder(mContext).setTitle(string.title_inventory)
                        .setMessage(string.item_nothing).setPositiveButton(
                            string.label_okay
                        ) { dialog: DialogInterface?, which: Int ->
                            dismissDialog()
                        }
                        .setOnCancelListener { dialog: DialogInterface? ->
                            dismissDialog()
                        }.show()
                } else {
                    val tmp = arrayOfNulls<String>(has.size)
                    AlertDialog.Builder(mContext).setTitle(string.title_inventory)
                        .setItems(has.toArray<String?>(tmp), null).setPositiveButton(
                            string.label_okay
                        ) { dialog: DialogInterface?, which: Int ->
                            dismissDialog()
                        }
                        .setOnCancelListener { dialog: DialogInterface? ->
                            dismissDialog()
                        }.show()
                }
            }

            3 -> {
                val lines = getResources().getStringArray(array.dialog_cut_line)
                cutline = -1
                AlertDialog.Builder(mContext).setTitle(string.title_cut_line).setSingleChoiceItems(
                    lines,
                    0
                ) { dialog: DialogInterface?, which: Int ->
                    cutline = which
                }.setPositiveButton(
                    string.label_okay
                ) { dialog: DialogInterface?, which: Int ->
                    if (cutline < 0) {
                        cutline = 0
                    }
                    if (userData!!.place[11] != 0xff) {
                        msgout(0xe0)
                    }
                    if (cutline == 0 || userData!!.place[11] != 0xff) {
                        setColorFilter(CF_MODE_RED)
                        msgout(0xc7)
                        msgByResId(string.msg_gameover)
                        gameOver()
                    } else {
                        userData!!.place[11] = 0
                        zSystem!!.mapId(74)
                        // play sound #3
                    }
                    dismissDialog()
                }.setNegativeButton(
                    string.label_cancel
                ) { dialog: DialogInterface?, which: Int ->
                    dismissDialog()
                }.setOnCancelListener { dialog: DialogInterface? ->
                    dismissDialog()
                }.show()
            }

            else -> dialogId = -1
        }
    }

    fun endRoll() {
        play(0) // school song!
        val i = Intent(mContext, ZCreditRollActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        i.putExtra("credits", raw.credits)
        i.putExtra("duration", 35000L)
        val o = ActivityOptions.makeCustomAnimation(
            this,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        startActivity(i, o.toBundle())
    }

    private fun interpreter() {
        var ok = false
        var i = 0
        while (!rules[i]!!.endOfRule()) {
            if (rules[i]!!.run(this)) {
                ok = true
                break
            }
            i++
        }
        if (dialogId >= 0) {
            return
        }
        if (!ok) {
            mapManager.mapId = zSystem!!.mapId()
            var msg = mapManager.mapData!!.find(zSystem!!.cmdId(), zSystem!!.objId())
            if (msg == null) {
                msg = getText(string.msg_not_found).toString()
            }
            msgout(msg)
        }
        if (zSystem!!.mapId() == 74) {
            val m = ++userData!!.fact[13]
            val msgId = when (m) {
                4 -> 0xe2
                6 -> 0xe3
                10 -> 0xe4
                else -> 0
            }
            if (msgId != 0) {
                msgout(msgId)
            }
        }
    }

    private fun parser(line: CharSequence) {
        if (line.toString().trim { it <= ' ' }.isEmpty()) return
        val argv: Array<String?> = line.toString().trim { it <= ' ' }.split("\\s+".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()
        var cmd: String? = null
        var obj: String? = null
        if (argv.isNotEmpty()) {
            cmd = argv[0]
            if (argv.size > 1) {
                obj = argv[1]
            }
        }

        msgout(">>> $line")

        zSystem!!.cmdId(scanVerbs(cmd))
        zSystem!!.objId(scanObjs(obj))
        zSystem!!.random(0)

        progress()
        if (gameover) {
            return  // game over during 'progress' check.
        }
        interpreter()
        if (dialogId >= 0) {
            return  // skip
        }
        checkTeacher()
        if (!cleared) {
            draw(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            id.menu_item_prefs -> {
                val prefs = Intent(
                    mContext,
                    SettingsActivity::class.java
                )
                prefActivityLauncher.launch(prefs)
            }

            id.menu_item_about -> AlertDialog.Builder(mContext)
                .setTitle(getText(string.title_about))
                .setMessage(
                    String.format(
                        getResources().getString(string.msg_about),
                        getResources().getString(string.version)
                    )
                )
                .setIcon(drawable.isako)
                .setPositiveButton(
                    getText(string.label_okay)
                ) { dialog: DialogInterface?, which: Int -> }
                .show()

            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val BASE_DIR: String = "HHSAdv"
        val fsync: Array<Any?> = arrayOfNulls<Any>(0)
        const val CF_MODE_NORMAL: Int = 0
        const val CF_MODE_BLUE: Int = 1
        const val CF_MODE_RED: Int = 2
        const val CF_MODE_SEPIA: Int = 3
        private const val MAX_VERBS = 100
        private const val MAX_OBJS = 100
        private const val MAX_RULES = 256
        //private val MAX_ROOMS = 100

        private const val START_PAGE = 1
        private const val TITLE_PAGE = 76

        //private const ISAKO_PAGE = 84; // Isako never used in the game.
        private const val WIDTH = 256
        private const val HEIGHT = 152
    }
}