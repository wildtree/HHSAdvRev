package jp.wildtree.android.apps.hhsadvrev

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream

class ZCreditRollActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_zcredit_roll)

        val i = intent
        val resId = i.getIntExtra("credits", R.raw.opening)
        val duration = i.getLongExtra("duration", 20000L)

        // rawリソースのInputStreamを取得
        val inputStream: InputStream = resources.openRawResource(resId)
        val creditsText: String = inputStream.bufferedReader().use { it.readText() }
        val creditsView: ZCreditsView = findViewById(R.id.creditsView)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        creditsView.post {
            creditsView.setCreditsText(creditsText, creditsView.width)
            creditsView.startScroll(duration = duration) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                finish()
            }
        }
    }
}