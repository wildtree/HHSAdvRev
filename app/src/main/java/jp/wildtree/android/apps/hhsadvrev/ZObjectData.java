package jp.wildtree.android.apps.hhsadvrev;

import android.graphics.Color;
import android.graphics.Paint;

public class ZObjectData {
	private final byte[] vector;
	
	public static final int file_block_size = 0x200;
	
	public ZObjectData(byte[] b)
	{
		vector = new byte [file_block_size];
		System.arraycopy(b, 0, vector, 0, file_block_size);
	}
	
	public byte[] vector()
	{
		return vector;
	}
	
	protected int drawOutline(ZGraphicDrawable g, int offset, int c, int ox, int oy)
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
			g.drawLine(x0 + ox, y0 + oy, x1 + ox, y1 + oy, paint);
			//pc.validateNow();
			x0 = x1;
			y0 = y1;
		}
		return p;		
	}
	
	private void _draw(ZGraphicDrawable g, boolean pre, int offset)
	{
		int o = offset;
		int b = g.color(vector[o++] & 0xff);
		int xs = (vector[o++] & 0xff) / 2;
		int ys = (vector[o++] & 0xff);
		
		if (pre)
		{
			b = Color.rgb(0xcc, 0xcc, 0); // dark yellow
		}
		
		o = drawOutline(g, o, b, xs, ys);
		int x0 = (vector[o++] & 0xff);
		int y0 = (vector[o++] & 0xff);
		while (x0 != 0xff || y0 != 0xff)
		{
			int c = g.color(vector[o++] & 0xff);
			if (pre)
			{
				c = b;
			}
			g.paint(xs + x0, ys + y0, c, b);
			x0 = (vector[o++] & 0xff);
			y0 = (vector[o++] & 0xff);
		}
	}
	
	public void draw(ZGraphicDrawable g)
	{
		draw(g, 0);
	}
	
	public void draw(ZGraphicDrawable g, int offset)
	{
		_draw(g, true, offset);
		_draw(g, false, offset);
	}
	
}
