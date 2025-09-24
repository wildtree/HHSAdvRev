/**
 *
 */
package jp.wildtree.android.apps.hhsadvrev

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import java.io.InputStream
import androidx.core.graphics.set
import androidx.core.graphics.get


/**
 * @author araki
 */
class ZGraphicDrawable : BitmapDrawable {
    private external fun ndkPset(bitmap: Bitmap?, x: Int, y: Int, c: Int)
    private external fun ndkLine(bitmap: Bitmap?, sx: Int, sy: Int, ex: Int, ey: Int, c: Int)
    private external fun gpaint(bitmap: Bitmap?, x: Int, y: Int, fgc: Int, bgc: Int)
    private external fun gtonepaint(bitmap: Bitmap?, tone: ByteArray?, t: Boolean)
    private external fun ndkFillRectangle(
        bitmap: Bitmap?,
        sx: Int,
        sy: Int,
        ex: Int,
        ey: Int,
        c: Int
    )

    private external fun ndkDrawRectangle(
        bitmap: Bitmap?,
        sx: Int,
        sy: Int,
        ex: Int,
        ey: Int,
        c: Int
    )

    private var _parent: View? = null
    private var blue: ColorMatrix? = null
    private var red: ColorMatrix? = null
    private var sepia: ColorMatrix? = null
    private var tiling = false

    /**
     * @param res
     * @param bitmap
     */
    constructor(res: Resources?, bitmap: Bitmap?) : super(res, bitmap)

    /**
     * @param res
     * @param filepath
     */
    constructor(res: Resources?, filepath: String?) : super(res, filepath)

    /**
     * @param res
     * @param is
     */
    constructor(res: Resources?, `is`: InputStream?) : super(res, `is`)

    fun parent(v: View?) {
        _parent = v
    }

    fun initColorMatrices() {
        val bf = floatArrayOf(
            0.0f, 0.0f, 0.1f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.2f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.7f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        blue = ColorMatrix(bf)

        val rf = floatArrayOf(
            0.0f, 0.0f, 0.7f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.2f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.1f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        red = ColorMatrix(rf)

        val sf = floatArrayOf(
            0.269021f, 0.527950f, 0.103030f, 0.0f, 0.0f,
            0.209238f, 0.410628f, 0.080135f, 0.0f, 0.0f,
            0.119565f, 0.234644f, 0.045791f, 0.0f, 0.0f,
            0.000000f, 0.000000f, 0.000000f, 1.0f, 0.0f
        )

        sepia = ColorMatrix(sf)
    }

    fun pset(sx: Int, sy: Int, c: Int) {
        try {
            bitmap[sx, sy] = c
        } catch (e: Exception) {
            Log.d(
                "ZGraphicDrawable",
                "pset(" + sx.toString() + "," + sy.toString() + ") " + e.localizedMessage
            )
        }
    }

    fun pset(sx: Int, sy: Int, paint: Paint) {
        pset(sx, sy, paint.color)
    }

    fun pget(x: Int, y: Int): Int {
        return bitmap[x, y]
    }

    fun drawLine(sx: Int, sy: Int, ex: Int, ey: Int, paint: Paint) {
        ndkLine(bitmap, sx, sy, ex, ey, paint.color)
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


    fun paint(x: Int, y: Int, fgc: Int, bc: Int) {
        gpaint(bitmap, x, y, fgc, bc)
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

    fun paint(tone: ByteArray?) {
        gtonepaint(bitmap, tone, tiling)
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

    fun drawColor(color: Int) {
        bitmap.eraseColor(color)
        if (_parent != null) {
            _parent!!.invalidate()
        }
    }

    fun fillRectangle(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        ndkFillRectangle(bitmap, x0, y0, x1, y1, c)
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

    fun drawRectangle(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        ndkDrawRectangle(bitmap, x0, y0, x1, y1, c)
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

    fun color(c: Int): Int {
        return _colors!![c]
    }

    fun blueFilter(): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(blue!!)
    }

    fun redFilter(): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(red!!)
    }

    fun sepiaFilter(): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(sepia!!)
    }

    fun tiling(b: Boolean) {
        tiling = b
    }

    fun tiling(): Boolean {
        return tiling
    }

    companion object {
        init {
            System.loadLibrary("graphics")
        }

        private val _colors: IntArray? = intArrayOf(
            Color.BLACK,
            Color.BLUE,
            Color.RED,
            Color.MAGENTA,
            Color.GREEN,
            Color.CYAN,
            Color.YELLOW,
            Color.WHITE,
        )
    }
}
