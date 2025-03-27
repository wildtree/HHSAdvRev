/**
 * 
 */
package jp.wildtree.android.apps.hhsadvrev;

import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;

/**
 * @author araki
 *
 */
public class ZGraphicDrawable extends BitmapDrawable {

	static 
	{
		System.loadLibrary("graphics");
	}
	
	private native void ndkPset(Bitmap bitmap, int x, int y, int c);
	private native void ndkLine(Bitmap bitmap, int sx, int sy, int ex, int ey, int c);
	private native void gpaint(Bitmap bitmap, int x, int y, int fgc, int bgc);
	private native void gtonepaint(Bitmap bitmap, byte[] tone, boolean t);
	private native void ndkFillRectangle(Bitmap bitmap, int sx, int sy, int ex, int ey, int c);
	private native void ndkDrawRectangle(Bitmap bitmap, int sx, int sy, int ex, int ey, int c);
	
	private static final int _colors[];

	static {
		_colors = new int[]{
				Color.BLACK,
				Color.BLUE,
				Color.RED,
				Color.MAGENTA,
				Color.GREEN,
				Color.CYAN,
				Color.YELLOW,
				Color.WHITE,
		};
	}

	private View _parent = null;
	private ColorMatrix blue, red, sepia;
	private boolean tiling = false;
	
	/**
	 * @param res
	 * @param bitmap
	 */
	public ZGraphicDrawable(Resources res, Bitmap bitmap) {
		super(res, bitmap);
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
	}

	/**
	 * @param res
	 * @param filepath
	 */
	public ZGraphicDrawable(Resources res, String filepath) {
		super(res, filepath);
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
	}

	/**
	 * @param res
	 * @param is
	 */
	public ZGraphicDrawable(Resources res, InputStream is) {
		super(res, is);
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
	}
	
	public void parent(View v)
	{
		_parent = v;
	}
	
	public void initColorMatrices()
	{
		float[] bf = {0.0F, 0.0F, 0.1F, 0.0F, 0.0F,
					  0.0F, 0.0F, 0.2F, 0.0F, 0.0F,
					  0.0F, 0.0F, 0.7F, 0.0F, 0.0F,
					  0.0F, 0.0F, 0.0F, 1.0F, 0.0F
		};
		blue = new ColorMatrix(bf);
		
		float[] rf = {0.0F, 0.0F, 0.7F, 0.0F, 0.0F,
					  0.0F, 0.0F, 0.2F, 0.0F, 0.0F,
					  0.0F, 0.0F, 0.1F, 0.0F, 0.0F,
					  0.0F, 0.0F, 0.0F, 1.0F, 0.0F
		};
		red = new ColorMatrix(rf);
		
		float[] sf = {0.269021F, 0.527950F, 0.103030F, 0.0F, 0.0F,
					  0.209238F, 0.410628F, 0.080135F, 0.0F, 0.0F,
					  0.119565F, 0.234644F, 0.045791F, 0.0F, 0.0F,
					  0.000000F, 0.000000F, 0.000000F, 1.0F, 0.0F
		};
		
		sepia = new ColorMatrix(sf);
	}
	
	public void pset(int sx, int sy, int c)
	{
		try
		{
			getBitmap().setPixel(sx, sy, c);
		}
		catch(Exception e)
		{
			Log.d("ZGraphicDrawable", "pset(" + String.valueOf(sx) + "," + String.valueOf(sy) + ") " + e.getLocalizedMessage());
		}
	}
	
	public void pset(int sx, int sy, Paint paint)
	{
		pset(sx, sy, paint.getColor());
	}
	
	public int pget(int x, int y)
	{
		return getBitmap().getPixel(x, y);
	}
	
	public void drawLine(int sx, int sy, int ex ,int ey, Paint paint)
	{
		ndkLine(getBitmap(), sx, sy, ex, ey, paint.getColor());
		/*
		int dy = ey - sy;
		int ddy = 1;
		if (dy < 0)
		{
			dy = -dy;
			ddy = -1;
		}
		int wy = dy / 2;
		int dx = ex - sx;
		int ddx = 1;
		if (dx < 0)
		{
			dx = -dx;
			ddx = -1;
		}
		int wx = dx / 2;
		pset(sx, sy, paint);
		if (dx > dy)
		{
			int y = sy;
			for (int x = sx ; x != ex ; x += ddx)
			{
				pset(x, y, paint);
				
				wx -= dy;
				if (wx < 0)
				{
					wx += dx;
					y += ddy;
				}
			}
		}
		else
		{
			int x = sx;
			for (int y = sy ; y != ey ; y += ddy)
			{
				pset(x, y, paint);

				wy -= dx;
				if (wy < 0)
				{
					wy += dy;
					x += ddx;
				}
			}
		}
		pset(ex, ey, paint);

		if (_parent != null)
		{
			_parent.invalidate();
		}
		*/
	}
	

	public void paint(int x, int y, int fgc, int bc)
	{
		gpaint(getBitmap(), x, y, fgc, bc);
		/*
		Paint paint = new Paint();
		paint.setColor(fgc);
		paint.setAntiAlias(false);
		paint.setDither(false);
		paint.setStyle(Paint.Style.STROKE);
		if (pget(x, y) == fgc || pget(x, y) == bc)
		{
			return; // needless to paint.
		}
		LinkedList<Point> fifo = new LinkedList<Point>();
		fifo.addLast(new Point(x, y));
		while (fifo.size() > 0)
		{
			Point p = fifo.removeFirst();
			int c = pget(p.x, p.y);
			if (c == fgc || c == bc)
			{
				continue;
			}
			int l = 0;
			int r = 0;
			for(l = p.x - 1 ; l >= 0 ; l--)
			{
				c = pget(l, p.y);
				if (c == fgc || c == bc)
				{
					break;
				}
			}
			++l;
			for (r = p.x + 1 ; r < getBitmap().getWidth() ; r++)
			{
				c = pget(r, p.y);
				if (c == fgc || c == bc)
				{
					break;
				}
			}
			--r;
			this.drawLine(l, p.y, r, p.y, paint);
			// validateNow();
			for (int wx = l ; wx <= r ; wx++)
			{
				// scan upper line
				int uy = p.y - 1;
				if (uy >= 0)
				{
					c = pget(wx, uy);
					if (c != fgc && c != bc)
					{
						if (wx == r)
						{
							fifo.addLast(new Point(wx, uy));
						}
						else
						{
							c = pget(wx + 1, uy);
							if (c == fgc || c == bc)
							{
								fifo.addLast(new Point(wx, uy));
							}
						}
					}
				}
				// scan lower line
				int ly = p.y + 1;
				if (ly < getBitmap().getHeight())
				{
					c = pget(wx, ly);
					if (c != fgc && c != bc)
					{
						if (wx == r)
						{
							fifo.addLast(new Point(wx, ly));
						}
						else
						{
							c = pget(wx + 1, ly);
							if (c == fgc || c == bc)
							{
								fifo.addLast(new Point(wx, ly));
							}
						}
					}
				}
			}
		}
		*/
	}
	
	public void paint(byte[] tone)
	{
		gtonepaint(getBitmap(), tone, tiling);
		/*
		int pat[][] = {
				{0x00, 0x00, 0x00},
				{0xff, 0x00, 0x00},
				{0x00, 0xff, 0x00},
				{0xff, 0xff, 0x00},
				{0x00, 0x00, 0xff},
				{0xff, 0x00, 0xff},
				{0x00, 0xff, 0xff},
				{0xff, 0xff, 0xff},
		};
		int[] col = {
				Color.BLACK,
				Color.BLUE,
				Color.RED,
				Color.MAGENTA,
				Color.GREEN,
				Color.CYAN,
				Color.YELLOW,
				Color.WHITE,
		};

		int p = 0;
		int n = tone[p++];
		for (int i = 1 ; i <= n ; i++)
		{
			pat[i][0] = (tone[p++] & 0xff);
			pat[i][1] = (tone[p++] & 0xff);
			pat[i][2] = (tone[p++] & 0xff);
			int b = 0, r = 0, g = 0;
			for (int bit = 0 ; bit < 8 ; bit++)
			{
				int mask = (1 << bit);
				if ((pat[i][0] & mask) != 0)
				{
					++b;
				}
				if ((pat[i][1] & mask) != 0)
				{
					++r;
				}
				if ((pat[i][2] & mask) != 0)
				{
					++g;
				}
			}
			r = (r == 0) ? 0 : 32 * r - 1;
			g = (g == 0) ? 0 : 32 * g - 1;
			b = (b == 0) ? 0 : 32 * b - 1;
			col[i] = Color.rgb(r, g, b); 
		}
		for (int wy = 0 ; wy < getBitmap().getHeight() ; wy++)
		{
			for (int wx = 0 ; wx < getBitmap().getWidth() ; wx++)
			{
				int c = pget(wx, wy);
				int ci = 0;
				for (ci = 0 ; ci < _colors.length ; ci++)
				{
					if (c == _colors[ci])
					{
						int cc = col[ci];
						if (tiling)
						{
							int b = (pat[ci][0] >> (7 - wx % 8)) & 1;
							int r = (pat[ci][1] >> (7 - wx % 8)) & 1;
							int g = (pat[ci][2] >> (7 - wx % 8)) & 1;
							cc = _colors[(g << 2)|(r << 1)|b];
						}
						pset(wx, wy, cc);
						break;
					}
				}
			}
			if (_parent != null)
			{
				_parent.invalidate();
			}
		}
		*/
	}
	
	public void drawColor(int color)
	{
		getBitmap().eraseColor(color);
		if (_parent != null)
		{
			_parent.invalidate();
		}
	}
	
	public void fillRectangle(int x0, int y0, int x1, int y1, int c)
	{
		ndkFillRectangle(getBitmap(), x0, y0, x1, y1, c);
		/*
		Paint paint = new Paint();
		paint.setColor(c);
		paint.setAntiAlias(false);
		paint.setStyle(Paint.Style.STROKE);
		
		if (y0 > y1)
		{
			int d = y0;
			y0 = y1;
			y1 = d;
		}
		
		for (int y = y0 ; y < y1 ; y++)
		{
			drawLine(x0, y, x1, y, paint);
		}
		drawLine(x0, y1, x1, y1, paint);
		*/
	}
	
	public void drawRectangle(int x0, int y0, int x1, int y1, int c)
	{
		ndkDrawRectangle(getBitmap(), x0, y0, x1, y1, c);
		/*
		Paint paint = new Paint();
		paint.setColor(c);
		paint.setAntiAlias(false);
		paint.setStyle(Paint.Style.STROKE);
		
		if (x0 > x1)
		{
			int d = x0;
			x0 = x1;
			x1 = d;
		}
		if (y0 > y1)
		{
			int d = y0;
			y0 = y1;
			y1 = d;
		}
		
		drawLine(x0, y0, x1, y0, paint);
		drawLine(x1, y0, x1, y1, paint);
		drawLine(x0, y0, x0, y1, paint);
		drawLine(x0, y1, x1, y1, paint);
		*/
	}
	
	public int color(int c)
	{
		return _colors[c];
	}
	
	public ColorMatrixColorFilter blueFilter()
	{
		return new ColorMatrixColorFilter(blue);
	}
	
	public ColorMatrixColorFilter redFilter()
	{
		return new ColorMatrixColorFilter(red);
	}
	
	public ColorMatrixColorFilter sepiaFilter()
	{
		return new ColorMatrixColorFilter(sepia);
	}
	
	public void tiling(boolean b)
	{
		tiling = b;
	}
	
	public boolean tiling()
	{
		return tiling;
	}
	
}
