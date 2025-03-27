/**
 * 
 */
package jp.wildtree.android.apps.hhsadvrev;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import androidx.preference.PreferenceManager;

/**
 * @author araki
 *
 */
public class ZDataBackupAgent extends BackupAgentHelper {

	public static final String PREFS_BACKUP_KEY = "HHSAdvPrefs";
	public static final String DATA_BACKUP_KEY = "HHSAdvData";
	
	private SharedPreferences prefs;
	
	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		if (!prefs.getBoolean("pref_use_cloud", false))
		{
			return; // nothing to do!
		}
		synchronized(MainActivity.fsync)
		{
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		synchronized(MainActivity.fsync)
		{
			super.onRestore(data, appVersionCode, newState);
		}
	}

	@Override
	public void onCreate() {
		// TODO 自動生成されたメソッド・スタブ
		Context context = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferencesBackupHelper sph = new SharedPreferencesBackupHelper(this, context.getPackageName() + "_preferences");
		FileBackupHelper fbh = new FileBackupHelper(this, "data1.dat", "data2.dat", "data3.dat");
		addHelper(PREFS_BACKUP_KEY, sph);
		addHelper(DATA_BACKUP_KEY, fbh);
		super.onCreate();
	}

}
