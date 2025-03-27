package jp.wildtree.android.apps.hhsadvrev;

import java.nio.charset.StandardCharsets;

import android.graphics.Color;
import android.graphics.Paint;

public class ZMapData {
	static class ZMessageMap
	{
		public int cmdId;
		public int objId;
		public String message;
		
		public ZMessageMap()
		{
			cmdId = objId = 0;
			message = null;
		}
	}
	private final byte[] vector;
	private final ZMessageMap[] map;
	private final String msg;
	private String bmsg;
	private boolean isBlank;
	
	private final int VECTOR_SIZE = 0x400;
	private final int RELATION_SIZE = 0x100;
	private final int MESSAGE_SIZE = 0x500;
	private final int MAX_MAP_ELEMENTS = 0x100;
	
	public static final int file_block_size = 0xa00;
	
	public ZMapData(byte[] b)
	{
		isBlank = false;
		vector = new byte[VECTOR_SIZE];
		map    = new ZMessageMap[MAX_MAP_ELEMENTS];
		String[] message = new String[MAX_MAP_ELEMENTS];
		bmsg = null;
		
		System.arraycopy(b, 0, vector, 0, VECTOR_SIZE);
		
		int n;
		int m = VECTOR_SIZE + RELATION_SIZE;
		for (n = 0 ; m < file_block_size  ; n++)
		{
			int len = ((b[m++] & 0xff) << 8)|(b[m++] & 0xff) ;
			if (len == 0)
			{
				break;
			}
			byte[] bmsg = new byte[len];
			System.arraycopy(b, m, bmsg, 0, len);
			message[n] = new String(bmsg, StandardCharsets.UTF_8);
			m += len;
		}
		msg = message[0];
		
		int j = VECTOR_SIZE;
		for (int i = 0 ; i <= n ; i++)
		{
			map[i] = new ZMessageMap();
			map[i].cmdId = (b[j++] & 0xff);
			if (map[i].cmdId == 0)
			{
				break; // end of data.
			}
			map[i].objId = (b[j++] & 0xff);
			map[i].message = message[(b[j++] & 0xff) - 1];
		}
	}
	
	public byte[] vector()
	{
		return vector;
	}
	
	public boolean isBlank()
	{
		return isBlank;
	}
	
	public void isBlank(boolean bf)
	{
		isBlank = bf;
	}
	public void blankMessage(String b)
	{
		bmsg = b;
	}
	public String blankMessage()
	{
		return bmsg;
	}
	
	public String find(int cmdId, int objId)
	{
		for (int i = 0 ; map[i] != null && map[i].cmdId != 0 ; i++)
		{
			if (map[i].cmdId == cmdId && map[i].objId == objId)
			{
				return map[i].message;
			}
		}
		return null;
	}
	
	public String mapMessage()
	{
		if (isBlank)
		{
			return bmsg;
		}
		return msg;
	}
	
	private int drawOutline(ZGraphicDrawable g, int offset, int c)
	{
		int x0, y0, x1, y1;
		int p = offset;
		
		Paint paint = new Paint();
		paint.setColor(c);
		paint.setAntiAlias(false);
		paint.setStyle(Paint.Style.STROKE);
		
		x0 = (vector[p++] & 0xff);
		y0 = (vector[p++] & 0xff);
		while (true)
		{
			x1 = (vector[p++] & 0xff);
			y1 = (vector[p++] & 0xff);
			if (y1 == 0xff)
			{
				if (x1 == 0xff)
				{
					// end of lines.
					break;
				}
				x0 = (vector[p++] & 0xff);
				y0 = (vector[p++] & 0xff);
				continue;
			}
			g.drawLine(x0, y0, x1, y1, paint);
			//pc.validateNow();
			x0 = x1;
			y0 = y1;
		}
		return p;
	}
	
	private void _draw(ZGraphicDrawable g)
	{
		int i = (vector[0] & 0xff) * 3 + 1; // skip HALF tone data
		g.drawColor(Color.BLUE);
		//pc.validateNow();
		i = drawOutline(g, i, Color.WHITE);
		int x0 = (vector[i++] & 0xff);
		int y0 = (vector[i++] & 0xff);
		while (x0 != 0xff || y0 != 0xff)
		{
			int c = g.color(vector[i++] & 0xff);
			g.paint(x0, y0, c, Color.WHITE);
			x0 = (vector[i++] & 0xff);
			y0 = (vector[i++] & 0xff);
		}
		if ((vector[i] & 0xff) == 0xff && (vector[i + 1] & 0xff) == 0xff)
		{
			i += 2;
		}
		else
		{
			i = drawOutline(g, i, Color.WHITE);
		}
		if ((vector[i] & 0xff) == 0xff && (vector[i + 1] & 0xff) == 0xff)
		{
			i += 2;
		}
		else
		{
			i = drawOutline(g, i, Color.BLACK);
		}
		g.paint(vector);		
	}
	
	public void draw(ZGraphicDrawable g)
	{
		if (isBlank)
		{
			g.drawColor(Color.BLACK);
			return;
		}
		_draw(g);
	}
	
	public void force_draw(ZGraphicDrawable g)
	{
		_draw(g);
	}
	
}
