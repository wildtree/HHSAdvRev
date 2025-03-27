package jp.wildtree.android.apps.hhsadvrev;

import android.os.Parcel;
import android.os.Parcelable;

public class ZUserData implements Parcelable {
	
	public class ZMapLink {
		public int n;
		public int s;
		public int w;
		public int e;
		public int u;
		public int d;
		public int i;
		public int o;
		
		public ZMapLink()
		{
			n = s = w = e = u = d = i = o = 0;
		}
		public ZMapLink(byte[] b)
		{
			n = b[0] & 0xff;
			s = b[1] & 0xff;
			w = b[2] & 0xff;
			e = b[3] & 0xff;
			u = b[4] & 0xff;
			d = b[5] & 0xff;
			i = b[6] & 0xff;
			o = b[7] & 0xff;
		}
		
		public byte[] pack()
		{
			byte[] b = new byte[8];
			b[0] = (byte)(n & 0xff);
			b[1] = (byte)(s & 0xff);
			b[2] = (byte)(w & 0xff);
			b[3] = (byte)(e & 0xff);
			b[4] = (byte)(u & 0xff);
			b[5] = (byte)(d & 0xff);
			b[6] = (byte)(i & 0xff);
			b[7] = (byte)(o & 0xff);
			return b;
		}
		
		public void set(int id, int value)
		{
			switch(id)
			{
			case 0: n = value; break;
			case 1: s = value; break;
			case 2: w = value; break;
			case 3: e = value; break;
			case 4: u = value; break;
			case 5: d = value; break;
			case 6: i = value; break;
			case 7: o = value; break;
			}
		}
		
		public int get(int id)
		{
			int v = -1;
			switch(id)
			{
			case 0: v = n; break;
			case 1: v = s; break;
			case 2: v = w; break;
			case 3: v = e; break;
			case 4: v = u; break;
			case 5: v = d; break;
			case 6: v = i; break;
			case 7: v = o; break;
			}
			return v;
		}
		
	}
	

	public ZMapLink[] map;
	public int[] place;
	public int[] fact;
	
	public static final int LINK_SIZE = 8;
	public static final int LINKS = 87;
	public static final int ITEMS = 12;
	public static final int FLAGS = 15;
	public static final int ITEMS_BEGIN = 0x301;
	public static final int FLAGS_BEGIN = 0x311;
	public static final int file_block_size = 0x800;
	public static final int packed_size = LINKS * LINK_SIZE + ITEMS + FLAGS;
	
	public ZUserData(byte[] b)
	{
		map = new ZMapLink[LINKS];
		place = new int [ITEMS];
		fact = new int [FLAGS];
		
		for (int i = 0 ; i < LINKS ; i++)
		{
			byte[] buf = new byte [LINK_SIZE];
			System.arraycopy(b, i * LINK_SIZE, buf, 0, LINK_SIZE);
			map[i] = new ZMapLink(buf);
		}
		
		for (int i = 0 ; i < ITEMS ; i++)
		{
			place[i] = (int)(b[ITEMS_BEGIN + i] & 0xff);
		}
		
		for (int i = 0 ; i < FLAGS ; i++)
		{
			fact[i] = (int)(b[FLAGS_BEGIN + i] & 0xff);
		}
	}
	
	public ZUserData(ZUserData source)
	{
		map = new ZMapLink[LINKS];
		place = new int [ITEMS];
		fact = new int [FLAGS];
		
		for (int i = 0 ; i < LINKS ; i++)
		{
			map[i] = new ZMapLink(source.map[i].pack());
		}
		for (int i = 0 ; i < ITEMS ; i++)
		{
			place[i] = source.place[i];
		}
		for (int i = 0 ; i < FLAGS ; i++)
		{
			fact[i] = source.fact[i];
		}
	}
	
	private ZUserData(Parcel source)
	{
		map = new ZMapLink[LINKS];
		place = new int [ITEMS];
		fact = new int [FLAGS];
		byte[] buf = new byte [LINK_SIZE];
		for (int i = 0 ; i < LINKS ; i++)
		{
			source.readByteArray(buf);
			map[i] = new ZMapLink(buf);
		}
		source.readIntArray(place);
		source.readIntArray(fact);
	}
	
	public static final Parcelable.Creator<ZUserData> CREATOR = new Parcelable.Creator<ZUserData>()
	{

		public ZUserData createFromParcel(Parcel source) {
			// TODO �����������ꂽ���\�b�h�E�X�^�u
			return new ZUserData(source);
		}

		public ZUserData[] newArray(int size) {
			// TODO �����������ꂽ���\�b�h�E�X�^�u
			return new ZUserData[size];
		}
	};
	
	public byte[] pack()
	{
		byte[] buf = new byte [packed_size];
		for (int i = 0 ; i < LINKS ; i++)
		{
			System.arraycopy(map[i].pack(), 0, buf, i * LINK_SIZE, LINK_SIZE);
		}
		for (int i = 0 ; i < ITEMS ; i++)
		{
			buf[LINKS * LINK_SIZE + i] = (byte)(place[i] & 0xff);
		}
		for (int i = 0 ; i < FLAGS ; i++)
		{
			buf[LINKS * LINK_SIZE + ITEMS + i] = (byte)(fact[i] & 0xff);
		}
		return buf;
		
	}
	
	public void unpack(byte[] b)
	{
		byte[] tmp = new byte [LINK_SIZE];
		for (int i = 0 ; i < LINKS ; i++)
		{
			System.arraycopy(b, i * LINK_SIZE, tmp, 0, LINK_SIZE);
			map[i] = new ZMapLink(tmp);
		}
		for (int i = 0 ; i < ITEMS ; i++)
		{
			place[i] = (int)(b[LINKS * LINK_SIZE + i] & 0xff);
		}
		for (int i = 0 ; i < FLAGS ; i++)
		{
			fact[i] = (int)(b[LINKS * LINK_SIZE + ITEMS + i] & 0xff);
		}
	}
	
	public int describeContents() {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		for (int i = 0 ; i < LINKS ; i++)
		{
			dest.writeByteArray(map[i].pack());
		}
		dest.writeIntArray(place);
		dest.writeIntArray(fact);
	}
	

}
