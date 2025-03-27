/**
 * 
 */
package jp.wildtree.android.apps.hhsadvrev;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * @author araki
 *
 */
public class ZOpeningActivity extends Activity {

	private ArrayList<String> buffer;
	private Handler handler;
	private Runnable runnable;
	private TextView tv;
	private ScrollView sv;
	private String[] res;
	private int pointer;
//	private PowerManager.WakeLock wl;
	
	private static final int INTERVAL = 1500;
	/* (�� Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		super.onCreate(savedInstanceState);
		setContentView(R.layout.opening);
		
		//wl = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE, "HHS");
		
		Intent i = getIntent();
		int res_id = i.getIntExtra("msgres", R.array.opening_message);
		
		tv = findViewById(R.id.openingMessage);
		sv = findViewById(R.id.openingScrollView);
		res = getResources().getStringArray(res_id);
		pointer = 0;
		
		tv.setOnClickListener(arg0 -> {
			// TODO �����������ꂽ���\�b�h�E�X�^�u
			handler.removeCallbacks(runnable);
			finish();
		});
		
		handler = new Handler();
		buffer = new ArrayList<>();
		runnable = new Runnable()
		{

			public void run() {
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				if (pointer >= res.length)
				{
					finish();
					return;
				}
				putMessage(res[pointer++]);
				handler.postDelayed(this, INTERVAL);
			}
			
		};
		handler.postDelayed(runnable, INTERVAL);
		//wl.acquire();
	}

	/* (�� Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		super.onDestroy();
		//wl.release();
	}

	/* (�� Javadoc)
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		super.onRestoreInstanceState(savedInstanceState);
		pointer = savedInstanceState.getInt("pointer");
		buffer = savedInstanceState.getStringArrayList("buffer");
		res = savedInstanceState.getStringArray("res");
		flush();
	}

	/* (�� Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		super.onSaveInstanceState(outState);
		outState.putInt("pointer", pointer);
		outState.putStringArrayList("buffer", buffer);
		outState.putStringArray("res", res);
		handler.removeCallbacks(runnable);
	}
	private void flush()
	{
		if (buffer.size() > 0)
		{
			String out = buffer.get(0);
			for (int i = 1 ; i < buffer.size() ; i++)
			{
				out += "\n" + buffer.get(i);
			}
			tv.setText(out);
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
	
	public void putMessage(String out)
	{
		if (out == null)
		{
			return;
		}
		
		buffer.add(out);
		flush();
	}


}
