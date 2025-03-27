package jp.wildtree.android.apps.hhsadvrev;

import java.util.Random;

import android.util.Log;

public class ZSystemParams {
	public static final int size = 8;	
	
	public byte[] table;
	private final Random r;
	
	public ZSystemParams()
	{
		table = new byte[size];
		r = new Random();
		r.setSeed(System.currentTimeMillis());
	}
	public ZSystemParams(byte[] b)
	{
		table = new byte[size];
		System.arraycopy(b, 0, table, 0, size);
		r = new Random();
	}
	
	public byte[] pack()
	{
		return table;
	}
	
	public void unpack(byte[] b)
	{
		System.arraycopy(b, 0, table, 0, size);
		random(0);
	}
	
	private int getInt(int index)
	{
		return (table[index] & 0xff);
	}
	private void setInt(int index, int v)
	{
		table[index] = (byte)(v & 0xff);
	}
	public int mapId()
	{
		return getInt(0);
	}
	public void mapId(int v)
	{
		setInt(0, v);
	}
	public int mapView()
	{
		return getInt(1);
	}
	public void mapView(int v)
	{
		setInt(1, v);
	}
	public int cmdId()
	{
		return getInt(2);
	}
	public void cmdId(int v)
	{
		setInt(2, v);
	}
	public int objId()
	{
		return getInt(3);
	}
	public void objId(int v)
	{
		setInt(3, v);
	}
	public int dlgres()
	{
		return getInt(4);
	}
	public void dlgres(int v)
	{
		setInt(4, v);
	}
	public int random()
	{
		Log.d("System", String.valueOf(getInt(5)));
		random(0);
		return getInt(5);
	}
	public void random(long seed)
	{
		if (seed != 0)
		{
			r.setSeed(seed);
		}
		setInt(5, r.nextInt(256));
	}
	public int dlgOk()
	{
		return getInt(6);
	}
	public void dlgOk(int v)
	{
		setInt(6, v);
	}
	public int dlgMessage()
	{
		return getInt(7);
	}
	public void dlgMessage(int v)
	{
		setInt(7, v);
	}
	
	public int getRandom(int d)
	{
		return r.nextInt(d);
	}
}
