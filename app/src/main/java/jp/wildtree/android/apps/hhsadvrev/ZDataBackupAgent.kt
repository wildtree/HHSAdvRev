/**
 *
 */
package jp.wildtree.android.apps.hhsadvrev

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper
import android.content.Context
import android.content.SharedPreferences
import android.os.ParcelFileDescriptor
import androidx.preference.PreferenceManager
import java.io.IOException

/**
 * @author araki
 */
class ZDataBackupAgent : BackupAgentHelper() {
    private var prefs: SharedPreferences? = null

    @Throws(IOException::class)
    override fun onBackup(
        oldState: ParcelFileDescriptor?, data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        // TODO 自動生成されたメソッド・スタブ
        if (!prefs!!.getBoolean("pref_use_cloud", false)) {
            return  // nothing to do!
        }
        synchronized(MainActivity.fsync) {
            super.onBackup(oldState, data, newState)
        }
    }

    @Throws(IOException::class)
    override fun onRestore(
        data: BackupDataInput?, appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        // TODO 自動生成されたメソッド・スタブ
        synchronized(MainActivity.fsync) {
            super.onRestore(data, appVersionCode, newState)
        }
    }

    override fun onCreate() {
        // TODO 自動生成されたメソッド・スタブ
        val context: Context = this
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sph = SharedPreferencesBackupHelper(this, context.packageName + "_preferences")
        val fbh = FileBackupHelper(this, "data1.dat", "data2.dat", "data3.dat")
        addHelper(PREFS_BACKUP_KEY, sph)
        addHelper(DATA_BACKUP_KEY, fbh)
        super.onCreate()
    }

    companion object {
        const val PREFS_BACKUP_KEY: String = "HHSAdvPrefs"
        const val DATA_BACKUP_KEY: String = "HHSAdvData"
    }
}
