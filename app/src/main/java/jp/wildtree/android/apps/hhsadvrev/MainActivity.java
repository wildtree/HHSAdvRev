package jp.wildtree.android.apps.hhsadvrev;

import static jp.wildtree.android.apps.hhsadvrev.R.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final String BASE_DIR = "HHSAdv";
    public static final String DATA_FILE = "UserData.dat";

    public static final int RECOGNIZER_ACTIVITY = 1;
    public static final int PREFERENCE_ACTIVITY = 1234;

    public static final Object[] fsync = new Object[0];

    public enum DataStorage {
        MEMORY,
        SDCARD,
    }
    private BackupManager bmgr;

    private ZWord[] cmdList;
    private ZWord[] objList;
    private ZRuleBase[] rules;
    public  ZMapData[] map;
    private ZObjectData[] objects;
    private ZTeacherData teacher;
    public  ZUserData userData;
    private ZUserData initData;
    public  ZSystemParams zSystem;
    private String[] msg;
    private ZGraphicDrawable zg;
    private ArrayList<String> mbuffer;
    private boolean starting;
    private boolean gameover;
    private boolean cleared;
    public  int cf_mode;

    private int dialog_id;

    public  Context mContext;
    private ScrollView sv;
    private TextView tv;
    private ImageView iv;

    private int fileno;
    private int cutline;

    private MediaPlayer mp;
    private AudioManager am;
    private SharedPreferences sp;

    private ProgressBar pd;
    private Handler mHandler;
    private Thread loader;
    private CountDownLatch signal;

    private final int MAX_VERBS = 100;
    private final int MAX_OBJS  = 100;
    private final int MAX_RULES = 256;
    private final int MAX_ROOMS = 100;

    private final int START_PAGE = 1;
    private final int TITLE_PAGE = 76;
    //private final int ISAKO_PAGE = 84; // Isako never used in the game.

    private final int WIDTH = 256;
    private final int HEIGHT = 152;

    public static final int cf_mode_normal = 0;
    public static final int cf_mode_blue = 1;
    public static final int cf_mode_red = 2;
    public static final int cf_mode_sepia = 3;

    private final String pref_key_playsounds = "playSounds";

    private final ActivityResultLauncher<Intent> prefActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                bmgr.dataChanged(); // backup prefs into cloud if enabled.
                // redraw if necessary
                zg.tiling(sp.getBoolean("prefUseTiling", false));
                draw(false);
                // sound management
                boolean ps = sp.getBoolean("playSounds", true);
                boolean fs = sp.getBoolean("prefFollowSilent", true);
                boolean rn = (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL);
                // stop if mode changed.
                if (!ps || (fs && !rn))
                {
                    if (mp != null && mp.isPlaying())
                    {
                        mp.stop();
                        mp.release();
                    }
                }
                // data management
                // copy data and remove old version.
                for (int i = 0 ; i < 3 ; i++)
                {
                    String name = String.format("data%d.dat", i + 1);
                    File oFile = new File(mContext.getFilesDir(), name);
                    File dir   = new File(getExternalFilesDir(null), BASE_DIR);
                    File iFile = new File(dir, name);
                    if (DataStorage.SDCARD.equals(DataStorage.valueOf(sp.getString("pref_use_sdcard", "MEMORY"))))
                    {
                        // to sdcard
                        File tmp = oFile;
                        oFile = iFile;
                        iFile = tmp;
                        if (!dir.exists())
                        {
                            dir.mkdirs();
                        }
                    }
                    if (!iFile.exists())
                    {
                        // skip copying.
                        continue;
                    }
                    try {
                        synchronized (MainActivity.fsync)
                        {
                            FileChannel iChannel = new FileInputStream(iFile).getChannel();
                            FileChannel oChannel = new FileOutputStream(oFile).getChannel();
                            iChannel.transferTo(0, iChannel.size(), oChannel);
                            iChannel.close();
                            oChannel.close();
                            iFile.delete(); // delete a source file.
                        }
                    } catch (FileNotFoundException e) {
                        // TODO  catch ubN
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO  catch ubN
                        e.printStackTrace();
                    }
                }
            });

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.main);

        mContext = this;
        mHandler = new Handler(Looper.getMainLooper());
        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        am = (AudioManager)getSystemService(AUDIO_SERVICE);
        bmgr = new BackupManager(mContext);

        sv = findViewById(id.scrollBox);
        tv = findViewById(id.messageBox);
        msg = getResources().getStringArray(array.messages);
        mbuffer = new ArrayList<>();
        gameover = true;
        starting = true;
        cleared = false;
        dialog_id = -1;

        Toolbar mtb = findViewById(id.toolbar);
        setSupportActionBar(mtb);

        EditText edit = findViewById(id.cmdline);
        edit.setOnEditorActionListener((view, actionId, event) -> {
            // TODO �����������ꂽ���\�b�h�E�X�^�u
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    actionId == EditorInfo.IME_ACTION_SEND)
            {
                keyToGo();
            }
            return true;
        });
        edit.setOnKeyListener((v, keyCode, event) -> {
            // TODO 自動生成されたメソッド・スタブ
            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER)
            {
                keyToGo();
            }
            return false;
        });
        edit.setVisibility(View.GONE);
        edit.setEnabled(false); // disabled until start
        edit.setFocusable(false);
        edit.setFocusableInTouchMode(false);

        final ActivityResultLauncher<Intent> actionRecognizerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK)
                    {
                        // success!!
                        ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String rs = results.get(0);
                        for (String r : results)
                        {
                            String[] list = r.split("\\s+");
                            int c = scanVerbs(list[0]);
                            int o = ZWord.INVALID_WORD;
                            if (list.length > 2)
                            {
                                o = scanObjs(list[1]);
                            }
                            if (c != ZWord.INVALID_WORD && o != ZWord.INVALID_WORD)
                            {
                                rs = list[0] + " " + list[1];
                                break;
                            }
                        }
                        // put recognized result into cmdline field and set a cursor at the tail of it.
                        EditText e = findViewById(id.cmdline);
                        e.setText(rs);
                        e.setSelection(rs.length());
                    }

                });
        ImageButton btnSpeak = findViewById(id.btnSpeak);
        btnSpeak.setOnClickListener(v -> {
            // TODO 自動生成されたメソッド・スタブ

            Intent si;
            si = new Intent();
            si.setAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            String locale = Locale.US.toString();
            if (sp.getString("prefRecEnglish", "US").equalsIgnoreCase("UK"))
            {
                locale = Locale.UK.toString();
            }
            si.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            si.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
            si.putExtra(RecognizerIntent.EXTRA_PROMPT, getText(string.label_prompt_recognizer));
            actionRecognizerLauncher.launch(si);
        });
        btnSpeak.setVisibility(View.GONE);
        btnSpeak.setEnabled(false);

        iv = findViewById(id.basicScreenView);
        zg = new ZGraphicDrawable(mContext.getResources(), Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888));
        iv.setImageDrawable(zg);
        zg.parent(iv);
        zg.initColorMatrices();
        zg.tiling(sp.getBoolean("prefUseTiling", false));
        cf_mode = 0;


        final boolean is_fresh = (savedInstanceState == null);
        signal = new CountDownLatch(1);
        loader = new Thread(() -> {
            // TODO �����������ꂽ���\�b�h�E�X�^�u
            init_data(is_fresh);
        });
        loader.start();

        pd = findViewById(id.progressBar);
        pd.setVisibility(View.GONE);
        pd.setMax(100);
        pd.setMin(0);
        //pd = new ProgressBar(this);
        //pd.setProgressStyle(ProgressBar.);
        //pd.setMessage(getText(R.string.progress_dialog_msg));
        //pd.setCancelable(false);
        //pd.show();
    }

    private void keyToGo()
    {
        // start parsing
        EditText edit = findViewById(id.cmdline);
        InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(edit.getWindowToken(), 0);
        parser(edit.getText());
        edit.setText("");
    }

    /* (�� Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        // TODO �����������ꂽ���\�b�h�E�X�^�u
        super.onDestroy();
//        if (loader.isAlive())
//        {
            // loader.stop() is deprecated.
//        }
//        if (pd.isShowing())
//        {
//            pd.dismiss();
//        }
    }

    public void init_data(boolean fresh)
    {
        zSystem = new ZSystemParams();

        readDictionary();
        readRulebase();
        readInitUserData();
        readMapdata();
        readObjects();

        userData = new ZUserData(initData); // avoid refernce
        pd.setVisibility(View.GONE);
        signal.countDown();

        if (fresh)
        {
            zSystem.mapId(TITLE_PAGE);

            mHandler.post(() -> {
                // TODO �����������ꂽ���\�b�h�E�X�^�u
                titleScreen();
                map[TITLE_PAGE].draw(zg);
            });
        }
    }

    public void setColorFilter(int mode)
    {
        switch (mode)
        {
            case cf_mode_normal: iv.clearColorFilter(); break;
            case cf_mode_blue:   iv.setColorFilter(zg.blueFilter()); break;
            case cf_mode_red:    iv.setColorFilter(zg.redFilter()); break;
            case cf_mode_sepia:  iv.setColorFilter(zg.sepiaFilter()); break;
        }
        cf_mode = mode;
        iv.invalidate();
    }

    private void titleScreen()
    {
        starting = true;
        mbuffer.clear();
        msgByResId(string.app_name);
        msgout("");
        msgByResId(string.copyright);
        msgout("");
        msgByResId(string.msg_tap_screen_to_start);

        EditText edit = findViewById(id.cmdline);
        edit.setVisibility(View.GONE);
        ImageButton btnSpeak = findViewById(id.btnSpeak);
        btnSpeak.setVisibility(View.GONE);

        iv.setClickable(true);
        iv.setOnClickListener(v -> {
            // TODO �����������ꂽ���\�b�h�E�X�^�u
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(getText(string.app_name));
            builder.setMessage(string.game_start);
            builder.setPositiveButton(string.label_start, (dialog, which) -> {
                // TODO �����������ꂽ���\�b�h�E�X�^�u
                iv.setClickable(false);
                start();
            });
            builder.show();
        });
    }

    private void msgflush()
    {
        if (mbuffer.size() > 0)
        {
            StringBuilder out = Optional.ofNullable(mbuffer.get(0)).map(StringBuilder::new).orElse(null);
            for (int i = 1 ; i < mbuffer.size() ; i++)
            {
                out = (out == null ? new StringBuilder("null") : out).append("\n").append(mbuffer.get(i));
            }
            tv.setText(out == null ? null : out.toString());
            sv.post(() -> {
                // TODO �����������ꂽ���\�b�h�E�X�^�u
                sv.scrollTo(0, tv.getHeight());

            });
        }
        else
        {
            tv.setText(null);
        }
    }

    public void msgout(String out)
    {
        if (out == null)
        {
            return;
        }

        mbuffer.add(out);
        while (mbuffer.size() > 100)
        {
            mbuffer.remove(0);
        }
        msgflush();
    }

    public void msgout(int id)
    {
        String out;
        if ((id & 0x80) == 0)
        {
            out = map[zSystem.mapId()].find(zSystem.cmdId(), zSystem.objId());
        }
        else
        {
            id &= 0x7f; // mask.
            out = msg[id];
        }
        msgout(out);
    }

    public void msgByResId(int resid)
    {
        msgout(getText(resid).toString());
    }

    public void drawObject(boolean withmsg)
    {
        for (int i = 0 ; i < 12 ; i++)
        {
            if (userData.place[i] == zSystem.mapId())
            {
                if (i == 1 && userData.fact[0] != 1) // UNIFORM
                {
                    objects[i + 1].draw(zg, 256);
                }
                else
                {
                    objects[i + 1].draw(zg);
                }
                if (withmsg && !gameover)
                {
                    msgout(0x96 + i);
                }
            }
        }
    }

    private void checkDarkness()
    {
        switch (zSystem.mapId())
        {
            case 47:
            case 48:
            case 49:
            case 61:
            case 64:
            case 65:
            case 67:
            case 68:
            case 69:
            case 71:
            case 74:
            case 75:
            case 77:
                if (userData.fact[7] != 0)
                {
                    if (userData.fact[6] != 0)
                    {
                        // blue
                        setColorFilter(cf_mode_blue);
                    }
                }
                else
                {
                    zSystem.mapView(zSystem.mapId());
                    zSystem.mapId(84);
                }
                break;
            default:
                if (userData.fact[6] != 0)
                {
                    // back to normal
                    iv.clearColorFilter();
                    cf_mode = 0;
                    iv.invalidate();
                }
                break;
        }
    }

    private void start()
    {
        starting = false;
        cleared = false;

        if (!sp.getBoolean("prefSkipOpening", false))
        {
            final Intent i = new Intent(mContext, ZOpeningActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("msgres", array.opening_message);
            startActivity(i);
        }
        EditText edit = findViewById(id.cmdline);
        edit.setVisibility(View.VISIBLE);
        edit.setEnabled(true);
        edit.setFocusable(true);
        edit.setFocusableInTouchMode(true);
        ImageButton btnSpeak = findViewById(id.btnSpeak);
        btnSpeak.setVisibility(View.VISIBLE);
        btnSpeak.setEnabled(true);

        userData = new ZUserData(initData);

        iv.clearColorFilter();
        cf_mode = 0;
        zSystem.mapId(START_PAGE);
        gameover = false;
        mbuffer.clear();
        msgflush();
        draw(true);
    }

    private void readInitUserData()
    {
        AssetManager as = getResources().getAssets();

        try
        {
            InputStream is = as.open("data.dat");
            byte[] buf = new byte [ZUserData.file_block_size];
            if (is.read(buf) > 0)
            {
                // load initial user data.
                initData = new ZUserData(buf);
            }
            is.close();
        } catch (Exception e) {
            // TODO �����������ꂽ catch �u���b�N
            e.printStackTrace();
        }


    }

    private void readDictionary()
    {
        cmdList = new ZWord[MAX_VERBS];
        objList = new ZWord[MAX_OBJS];
        AssetManager as = getResources().getAssets();
        try {
            InputStream is = as.open("highds.com");
            int len = 0;
            int sz;
            int i   = 0;
            byte[] buf = new byte[5];
            while ((sz = is.read(buf)) > 0)
            {
                len += sz;
                if(len >= 0x200 || buf[0] == 0)
                {
                    break;
                }
                cmdList[i++] = new ZWord(buf);
            }
            is.skip(0x200 - len);
            len = 0;
            i = 0;
            while ((sz = is.read(buf))> 0)
            {
                len += sz;
                if(len >= 0x200 || buf[0] == 0)
                {
                    break;
                }
                objList[i++] = new ZWord(buf);
            }
            is.close();
        } catch (IOException e) {
            // TODO �����������ꂽ catch �u���b�N
            e.printStackTrace();
        }

    }

    private void readRulebase()
    {
        AssetManager as = getResources().getAssets();
        rules = new ZRuleBase[MAX_RULES];
        try {
            InputStream is = as.open("rule.dat");
            byte[] buf = new byte[ZRuleBase.file_block_size];
            int i = 0;
            while (is.read(buf) > 0)
            {
                rules[i++] = new ZRuleBase(buf);
            }
            is.close();
        } catch (IOException e) {
            // TODO �����������ꂽ catch �u���b�N
            e.printStackTrace();
        }

    }

    private void readMapdata()
    {
        AssetManager as = getResources().getAssets();
        map = new ZMapData[MAX_ROOMS];
        try
        {
            InputStream is = as.open("map.dat");
            byte[] buf = new byte [ZMapData.file_block_size];
            int i = 0;
            while (is.read(buf) > 0)
            {
                map[i] = new ZMapData(buf);
                if (i == 0 || i == 84 || i == 85)
                {
                    map[i].isBlank(true); // Blank map
                    map[i].blankMessage(msg[0x4c]);
                }
                ++i;
            }
            is.close();
        } catch (Exception e) {
            // TODO �����������ꂽ catch �u���b�N
            e.printStackTrace();
        }

    }

    private void readObjects()
    {
        AssetManager as = getResources().getAssets();
        objects = new ZObjectData[MAX_OBJS];
        try {
            InputStream is = as.open("thin.dat");
            byte[] buf = new byte [ZObjectData.file_block_size];
            int i = 0;
            while (is.read(buf) > 0)
            {
                if (i == 14) // teacher data
                {
                    teacher = new ZTeacherData(buf);
                    objects[i++] = null;
                    continue;
                }
                objects[i++] = new ZObjectData(buf);
            }
            is.close();
        } catch (IOException e) {
            // TODO �����������ꂽ catch �u���b�N
            e.printStackTrace();
        }

    }

    /* (�� Javadoc)
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // TODO �����������ꂽ���\�b�h�E�X�^�u
        super.onRestoreInstanceState(savedInstanceState);

        try {
            signal.await();
        } catch (InterruptedException e) {
            // TODO �����������ꂽ catch �u���b�N
            e.printStackTrace();
        }

        zSystem = new ZSystemParams(savedInstanceState.getByteArray("sys"));
        userData = savedInstanceState.getParcelable("user");
        mbuffer = savedInstanceState.getStringArrayList("mbuffer");
        gameover = savedInstanceState.getBoolean("gameover");
        starting = savedInstanceState.getBoolean("starting");
        cleared = savedInstanceState.getBoolean("cleared");
        fileno = savedInstanceState.getInt("fileno");
        cutline = savedInstanceState.getInt("cutline");
        dialog_id = savedInstanceState.getInt("dialog_id");
        cf_mode = savedInstanceState.getInt("cf_mode");

        if (!gameover)
        {
            iv.setClickable(false);
            EditText edit = findViewById(id.cmdline);
            edit.setEnabled(true);
            edit.setFocusable(true);
            edit.setFocusableInTouchMode(true);
            edit.setTransitionVisibility(View.VISIBLE);
            ImageButton btnSpeak = findViewById(id.btnSpeak);
            btnSpeak.setEnabled(true);
            btnSpeak.setVisibility(View.VISIBLE);
        }
        else
        {
            if (starting)
            {
                titleScreen();
            }
            else
            {
                gameEnding();
            }
        }
        if (dialog_id >= 0)
        {
            dialog(dialog_id);
        }
        msgflush();
        draw(false);
        setColorFilter(cf_mode);
    }

    /* (�� Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // TODO �����������ꂽ���\�b�h�E�X�^�u
        super.onSaveInstanceState(outState);
        outState.putByteArray("sys", zSystem.pack());
        outState.putParcelable("user", userData);
        outState.putStringArrayList("mbuffer", mbuffer);
        outState.putBoolean("gameover", gameover);
        outState.putBoolean("starting", starting);
        outState.putBoolean("cleared", cleared);
        outState.putInt("fileno", fileno);
        outState.putInt("cutline", cutline);
        outState.putInt("dialog_id", dialog_id);
        outState.putInt("cf_mode", cf_mode);
    }

    private int scanVerbs(String s)
    {
        if (s == null)
        {
            return ZWord.INVALID_WORD;
        }
        for (int i = 0 ; i < MAX_VERBS ; i++)
        {
            if (cmdList[i] != null && cmdList[i].match(s))
            {
                return cmdList[i].id;
            }
        }
        return ZWord.INVALID_WORD;
    }

    private int scanObjs(String s)
    {
        if (s == null)
        {
            return ZWord.INVALID_WORD;
        }
        for (int i = 0 ; i < MAX_OBJS ; i++)
        {
            if (objList[i] != null && objList[i].match(s))
            {
                return objList[i].id;
            }
        }
        return ZWord.INVALID_WORD;
    }

    private void disableEditField()
    {
        EditText edit = findViewById(id.cmdline);
        edit.setEnabled(false);
        edit.setFocusable(false);
        edit.setFocusableInTouchMode(false);
        ImageButton btnSpeak = findViewById(id.btnSpeak);
        btnSpeak.setEnabled(false);
    }

    private void gameEnding()
    {
        disableEditField();
        iv.setClickable(true);

        iv.setOnClickListener(arg0 -> {
            // TODO �����������ꂽ���\�b�h�E�X�^�u
            if (cleared)
            {
                play(0); // school song!
                Intent i = new Intent(mContext, ZOpeningActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("msgres", array.credit);
                startActivity(i);
            }
            titleScreen();
            setColorFilter(cf_mode_normal);
            zSystem.mapId(TITLE_PAGE);
            map[TITLE_PAGE].draw(zg);
        });
        gameover = true;
    }

    public void gameOver()
    {
        msgByResId(string.msg_tap_screen_to_title);
        cleared = false;
        gameEnding();
    }

    public void gameCleared()
    {
        msgByResId(string.msg_tap_screen_to_title);
        cleared = true;
        gameEnding();
    }

    private void progress()
    {
        if (userData.fact[3] > 0 && userData.fact[7] == 1)
        { // light on
            --userData.fact[3]; /* battery */
            if (userData.fact[3] < 8 && userData.fact[3] > 0)
            {
                userData.fact[6] = 1; // dim
                msgout(0xd9);
            }
            if (userData.fact[3] == 0)
            { /* battery wear out */
                userData.fact[7] = 0; // light off
                msgout(0xc0);
            }
        }
        // count down
        if (userData.fact[11] > 0)
        { /* count down */
            --userData.fact[11];
            if (userData.fact[11] == 0)
            {
                play(2); // explosion sound.
                msgout(0xd8);
                if (userData.place[7] == 48)
                {
                    userData.map[75 - 1].n = 77;
                    userData.map[68 - 1].w = 77;
                    msgout(0xda);
                }
                if (userData.place[7] == 255 || userData.place[7] == zSystem.mapId())
                {
                    /* explosion within the room where you are */
                    /* change screen to red */
                    setColorFilter(cf_mode_red);
                    msgout(0xcf);
                    msgout(0xcb);
                    gameOver();
                }
                else
                {
                    userData.place[7] = 0; /* explosion ... lose bomber */
                }
            }
        }
    }

    private void checkTeacher()
    {
        if (gameover || userData.fact[1] == zSystem.mapId())
        {
            return;
        }

        int rd = 100 + zSystem.mapId() + ((userData.fact[1] > 0) ? 1000 : 0);
        int rz = zSystem.getRandom(3000);
        if (rd < rz)
        {
            userData.fact[1] = 0;
        }
        else
        {
            userData.fact[1] = zSystem.mapId();
        }
        switch(zSystem.mapId())
        {
            case 1:
            case 48:
            case 50:
            case 51:
            case 52:
            case 53:
            case 61:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 83:
            case 86:
                userData.fact[1] = 0;
        }
    }

    private void draw(boolean withmsg)
    {
        // �`��n�̏���
        zg.tiling(sp.getBoolean("prefUseTiling", false));
        checkDarkness();
        map[zSystem.mapId()].draw(zg);
        if (withmsg && !gameover)
        {
            msgout(map[zSystem.mapId()].mapMessage());
        }
        drawObject(withmsg);
        if (userData.fact[1] == zSystem.mapId())
        {
            teacher.draw(zg);
            if (withmsg && !gameover) {
                msgout(0xb4);
            }
        }
        iv.invalidate();
    }

    public void play(int id)
    {
        int m = am.getRingerMode();
        if (!sp.getBoolean(pref_key_playsounds, true) ||
                (sp.getBoolean("prefFollowSilent", true) && m != AudioManager.RINGER_MODE_NORMAL))
        {
            return;
        }
        int res_id = 0;
        switch (id)
        {
            case 0:
                // �Z��
                res_id = raw.highschool;
                break;
            case 1:
                res_id = raw.charumera;
                break;
            case 2:
                res_id = raw.explosion;
                break;
            case 4:
                res_id = raw.in_toilet;
                break;
            case 5:
                res_id = raw.acid;
        }
        if (res_id == 0)
        {
            return;
        }
        mp = MediaPlayer.create(mContext, res_id);
        if (mp.isPlaying())
        {
            mp.stop();
            mp.release();
        }
        mp.setOnCompletionListener(arg0 -> {
            // TODO �����������ꂽ���\�b�h�E�X�^�u
            arg0.stop();
            arg0.release();
        });
        mp.start();
    }

    private void loadGame(int file)
    {
        String name = "data" + file + ".dat";
        DataStorage dest = DataStorage.valueOf(sp.getString("pref_use_sdcard", "MEMORY"));
        boolean sdcard = dest.equals(DataStorage.SDCARD);
        try {
            byte[] sb = new byte [ZSystemParams.size];
            byte[] ub = new byte [ZUserData.packed_size];
            FileInputStream in;
            synchronized (MainActivity.fsync)
            {
                if (sdcard)
                {
                    File base_dir = new File(getExternalFilesDir(null), BASE_DIR);
                    File data = new File(base_dir, name);
                    in = new FileInputStream(data);
                }
                else
                {
                    in = openFileInput(name);
                }
                BufferedInputStream buf = new BufferedInputStream(in);
                buf.read(sb, 0, ZSystemParams.size);
                buf.read(ub, 0, ZUserData.packed_size);
                buf.close();
                in.close();
            }
            zSystem.unpack(sb);
            userData.unpack(ub);
            // DAN found the issue
            setColorFilter(cf_mode_normal);
            checkDarkness();
        } catch (FileNotFoundException e) {
            String err_msg = String.format(getString(string.err_file_not_found), file);
            Toast.makeText(mContext, err_msg, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch(IOException e)
        {
            e.printStackTrace();
        }
        msgout(String.valueOf(fileno) + getText(string.msg_selected_fileno));
        msgByResId(string.msg_loaded);

    }

    private void saveGame(int file)
    {
        String name = "data" + String.valueOf(file) + ".dat";
        DataStorage dest = DataStorage.valueOf(sp.getString("pref_use_sdcard", "MEMORY"));
        boolean sdcard = dest.equals(DataStorage.SDCARD);
        try {
            synchronized (MainActivity.fsync)
            {
                OutputStream out = null;
                if (sdcard)
                {
                    File dir = new File(getExternalFilesDir(null), BASE_DIR);
                    File data = new File(dir, name);
                    if (!dir.exists())
                    {
                        dir.mkdirs();
                    }
                    if (data.exists())
                    {
                        data.delete();
                    }
                    if (!data.createNewFile())
                    {
                        // failed to create a new file.
                        return;
                    }
                    out = new FileOutputStream(data);
                }
                else
                {
                    out = openFileOutput(name, MODE_PRIVATE);
                }
                BufferedOutputStream buf = new BufferedOutputStream(out);
                buf.write(zSystem.pack());
                buf.write(userData.pack());
                buf.flush();
                out.close();
            }
            bmgr.dataChanged(); // backup it!!
        } catch (FileNotFoundException e) {
            // TODO �����������ꂽ catch �u���b�N
            e.printStackTrace();
        } catch(IOException e)
        {
            e.printStackTrace();
        }
        msgout(String.valueOf(fileno) + getText(string.msg_selected_fileno));
        msgByResId(string.msg_saved);
    }

    private void dismissDialog()
    {
        dialog_id = -1;
        checkTeacher();
        draw(true);
    }

    public void dialog(int id)
    {
        dialog_id = id;
        switch (id)
        {
            case 0: // ����
                String[] genders = getResources().getStringArray(array.dialog_gender);
                userData.fact[0] = 1; // default is Boy
                new AlertDialog.Builder(mContext).setTitle(string.title_gender).setSingleChoiceItems(genders, 0, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    userData.fact[0] = which + 1;

                }).setPositiveButton(string.label_okay, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    if (userData.fact[0] == 0)
                    {
                        userData.fact[0] = 1;
                    }
                    zSystem.mapId(3); // enter the room.
                    dismissDialog();
                }).setNegativeButton(string.label_cancel, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    userData.fact[0] = 0;
                    dismissDialog();
                }).setOnCancelListener(dialog -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    dismissDialog();
                }).show();
                break;
            case 1: // �t�@�C��
                String[] files = getResources().getStringArray(array.dialog_file);
                fileno = 0;
                new AlertDialog.Builder(mContext).setTitle(string.title_file).setSingleChoiceItems(files, 0, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    fileno = which + 1;

                }).setPositiveButton(string.label_okay, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    if (fileno == 0)
                    {
                        fileno = 1;
                    }
                    if (zSystem.cmdId() == 0xf)
                    {
                        saveGame(fileno);
                    }
                    else
                    {
                        loadGame(fileno);
                    }
                    dismissDialog();
                }).setNegativeButton(string.label_cancel, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    dismissDialog();
                }).setOnCancelListener(dialog -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    dismissDialog();
                }).show();
                break;
            case 2: // ������
                String[] items = getResources().getStringArray(array.items);
                ArrayList<String> has = new ArrayList<>(items.length);

                for (int i = 0 ; i < items.length ; i++)
                {
                    if (userData.place[i] == 0xff)
                    {
                        has.add(items[i]);
                    }
                }
                if (has.isEmpty())
                {
                    new AlertDialog.Builder(mContext).setTitle(string.title_inventory).setMessage(string.item_nothing).setPositiveButton(string.label_okay, (dialog, which) -> {
                        // TODO �����������ꂽ���\�b�h�E�X�^�u
                        dismissDialog();
                    }).setOnCancelListener(dialog -> {
                        // TODO �����������ꂽ���\�b�h�E�X�^�u
                        dismissDialog();
                    }).show();
                }
                else
                {
                    String[] tmp = new String[has.size()];
                    new AlertDialog.Builder(mContext).setTitle(string.title_inventory).setItems(has.toArray(tmp), null).setPositiveButton(string.label_okay, (dialog, which) -> {
                        // TODO �����������ꂽ���\�b�h�E�X�^�u
                        dismissDialog();
                    }).setOnCancelListener(dialog -> {
                        // TODO �����������ꂽ���\�b�h�E�X�^�u
                        dismissDialog();
                    }).show();
                }
                break;
            case 3: // ��
                String[] lines = getResources().getStringArray(array.dialog_cut_line);
                cutline = -1;
                new AlertDialog.Builder(mContext).setTitle(string.title_cut_line).setSingleChoiceItems(lines, 0, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    cutline = which;

                }).setPositiveButton(string.label_okay, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    if (cutline < 0)
                    {
                        cutline = 0;
                    }
                    if (userData.place[11] != 0xff)
                    {
                        // �y���`�������Ă��Ȃ��̂Ő؂�܂���
                        msgout(0xe0);
                    }
                    if (cutline == 0 || userData.place[11] != 0xff)
                    {
                        // ���s
                        setColorFilter(cf_mode_red);
                        msgout(0xc7);
                        msgByResId(string.msg_gameover);
                        gameOver();
                    }
                    else
                    {
                        // ����
                        userData.place[11] = 0;
                        zSystem.mapId(74);
                        // play sound #3
                    }
                    dismissDialog();
                }).setNegativeButton(string.label_cancel, (dialog, which) -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    dismissDialog();
                }).setOnCancelListener(dialog -> {
                    // TODO �����������ꂽ���\�b�h�E�X�^�u
                    dismissDialog();
                }).show();
                break;
            default:
                dialog_id = -1;
        }
    }


    private void interpreter()
    {
        boolean ok = false;
        for (int i = 0 ; !rules[i].endOfRule() ; i++)
        {
            if (rules[i].run(this))
            {
                ok = true;
                break;
            }
        }
        if (dialog_id >= 0)
        {
            return;
        }
        if (!ok)
        {
            String msg = map[zSystem.mapId()].find(zSystem.cmdId(), zSystem.objId());
            if (msg == null)
            {
                msg = getText(string.msg_not_found).toString();
            }
            msgout(msg);
        }
        if (zSystem.mapId() == 74)
        {
            int msgid = 0;
            switch(++userData.fact[13])
            {
                case 4: msgid = 0xe2; break;
                case 6: msgid = 0xe3; break;
                case 10: msgid = 0xe4; break;
            }
            if (msgid != 0)
            {
                msgout(msgid);
            }
        }
    }

    private void parser(CharSequence line)
    {
        String[] argv = line.toString().trim().split("\\s+");
        String cmd = null;
        String obj = null;
        if (argv.length > 0)
        {
            cmd = argv[0];
            if (argv.length > 1)
            {
                obj = argv[1];
            }
        }

        msgout(">>> " + line);

        zSystem.cmdId(scanVerbs(cmd));
        zSystem.objId(scanObjs(obj));
        zSystem.random(0);

        progress();
        if (gameover)
        {
            return; // game over during 'progress' check.
        }
        interpreter();
        if (dialog_id >= 0)
        {
            return; // skip
        }
        checkTeacher();
        draw(true);
    }

    /* (�� Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO �����������ꂽ���\�b�h�E�X�^�u
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* (�� Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case id.menu_item_prefs:
                Intent prefs = new Intent(mContext, SettingsActivity.class);
                prefActivityLauncher.launch(prefs);
                break;
            case id.menu_item_about:
                new AlertDialog.Builder(mContext)
                        .setTitle(getText(string.title_about))
                        .setMessage(String.format(getResources().getString(string.msg_about), getResources().getString(string.version)))
                        .setIcon(drawable.isako)
                        .setPositiveButton(getText(string.label_okay), (dialog, which) -> {
                            // TODO �����������ꂽ���\�b�h�E�X�^�u

                        }).show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}