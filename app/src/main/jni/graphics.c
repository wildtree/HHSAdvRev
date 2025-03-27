/*
 * High speed graphic library powered by NDK
 * 		Copyright(c)2012 ZOBplus hiro <hiro@zob.jp>
 */

#include <jni.h>
#include <android/bitmap.h>
#include <stdlib.h>

typedef struct tagPoint
{
	int x;
	int y;
} Point;
#define FIFO_SIZE (1024)

typedef struct tagFifo
{
	int head;
	int tail;
	Point point[1];
} Fifo;

void
gpset(AndroidBitmapInfo info, unsigned char *pixels, int x, int y, unsigned char r, unsigned char g, unsigned char b, unsigned char a)
{
	if (y >= info.height || y < 0 || x >= info.width || x < 0) return; // Out of bounds
	pixels += info.stride * y + 4 * x;
	*pixels++ = r;
	*pixels++ = g;
	*pixels++ = b;
	*pixels++ = a;
}

int
gpget(AndroidBitmapInfo info, unsigned char *pixels, int x, int y)
{
	int i, c = 0;
	unsigned char r, g, b, a;
	pixels += info.stride * y + 4 * x;
	r = *pixels++;
	g = *pixels++;
	b = *pixels++;
	a = *pixels++;
	c = (a << 24) | (r << 16) | (g << 8) | b;

	return c;
}

void
gpdrawline(AndroidBitmapInfo info, unsigned char *pixels, int sx, int sy, int ex, int ey, int c)
{
	int dx, ddx, dy, ddy;
	int wx, wy;
	int x, y;
	unsigned char r, g, b, a;

	b = (unsigned char)(c & 0xff);
	g = (unsigned char)((c >>  8) & 0xff);
	r = (unsigned char)((c >> 16) & 0xff);
	a = (unsigned char)((c >> 24) & 0xff);

	dy = ey - sy;
	ddy = 1;
	if (dy < 0)
	{
		dy = -dy;
		ddy = -1;
	}
	wy = dy / 2;
	dx = ex - sx;
	ddx = 1;
	if (dx < 0)
	{
		dx = -dx;
		ddx = -1;
	}
	wx = dx / 2;
	gpset(info, pixels, sx, sy, r, g, b, a);
	if (dx > dy)
	{
		y = sy;
		for (x = sx ; x != ex ; x += ddx)
		{
			gpset(info, pixels, x, y, r, g, b, a);

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
		x = sx;
		for (y = sy ; y != ey ; y += ddy)
		{
			gpset(info, pixels, x, y, r, g, b, a);

			wy -= dx;
			if (wy < 0)
			{
				wy += dy;
				x += ddx;
			}
		}
	}
	gpset(info, pixels, ex, ey, r, g, b, a);
}

void
Java_jp_wildtree_android_apps_hhsadvrev_ZGraphicDrawable_ndkPset(JNIEnv *env, jobject thiz, jobject bitmap, jint x, jint y, jint c)
{
	AndroidBitmapInfo info;
	unsigned char *   pixels;
	unsigned char     r,g,b,a;
	int               ret;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
	{
		abort();
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap,  (void**)&pixels)) < 0)
	{
		abort();
	}
	b = (unsigned char)(c & 0xff);
	g = (unsigned char)((c >>  8) & 0xff);
	r = (unsigned char)((c >> 16) & 0xff);
	a = (unsigned char)((c >> 24) & 0xff);
	if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888)
	{
		gpset(info, pixels, x, y, r, g, b, a);
	}
	AndroidBitmap_unlockPixels(env, bitmap);
}

void
Java_jp_wildtree_android_apps_hhsadvrev_ZGraphicDrawable_ndkLine(JNIEnv *env, jobject thiz, jobject bitmap, jint sx, jint sy, jint ex, jint ey, jint c)
{
	unsigned char *pixels;
	AndroidBitmapInfo info;
	int ret;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
	{
		abort();
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap,  (void**)&pixels)) < 0)
	{
		abort();
	}

	gpdrawline(info, pixels, sx, sy, ex, ey, c);

	AndroidBitmap_unlockPixels(env, bitmap);
}

void
init(Fifo *fifo)
{
	fifo->head = fifo->tail = 0;
}

void
add(Fifo *fifo, int x, int y)
{
	fifo->point[fifo->tail].x = x;
	fifo->point[fifo->tail].y = y;
	if (++(fifo->tail) == FIFO_SIZE)
	{
		fifo->tail = 0;
	}
}

Point
get(Fifo *fifo)
{
	Point p = fifo->point[(fifo->head)++];
	if (fifo->head == FIFO_SIZE)
	{
		fifo->head = 0;
	}
	return p;
}

int
isEmpty(Fifo *fifo)
{
	return fifo->head == fifo->tail;
}

void
Java_jp_wildtree_android_apps_hhsadvrev_ZGraphicDrawable_gpaint(JNIEnv *env, jobject thiz, jobject bitmap, jint x, jint y, jint fgc, jint bgc)
{
	AndroidBitmapInfo info;
	unsigned char *   pixels;
	int               ret;
	int               l, r;
	int               wx, wy;
	int               uy, ly;
	int               c;
	Point             p;
	Fifo              *fifo;

	if ((fifo = (Fifo*)malloc(sizeof(Fifo) + sizeof(Point) * (FIFO_SIZE - 1))) == NULL)
	{
		abort();
	}
	init(fifo);

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
	{
		abort();
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap,  (void**)&pixels)) < 0)
	{
		abort();
	}
	c = gpget(info, pixels, x, y);
	if (c == fgc || c == bgc)
	{
		return; // needless to paint.
	}
	add(fifo, x, y);
	while (!isEmpty(fifo))
	{
		p = get(fifo);

		int c = gpget(info, pixels, p.x, p.y);
		if (c == fgc || c == bgc)
		{
			continue;
		}
		for(l = p.x - 1 ; l >= 0 ; l--)
		{
			c = gpget(info, pixels, l, p.y);
			if (c == fgc || c == bgc)
			{
				break;
			}
		}
		++l;
		for (r = p.x + 1 ; r < info.width ; r++)
		{
			c = gpget(info, pixels, r, p.y);
			if (c == fgc || c == bgc)
			{
				break;
			}
		}
		--r;
		gpdrawline(info, pixels, l, p.y, r, p.y, fgc);
		for (wx = l ; wx <= r ; wx++)
		{
			uy = p.y - 1;
			if (uy >= 0)
			{
				c = gpget(info, pixels, wx, uy);
				if (c != fgc && c != bgc)
				{
					if (wx == r)
					{
						add(fifo, wx, uy);
					}
					else
					{
						c = gpget(info, pixels, wx + 1, uy);
						if (c == fgc || c == bgc)
						{
							add(fifo, wx, uy);
						}
					}
				}
			}
			// scan lower line
			ly = p.y + 1;
			if (ly < info.height)
			{
				c = gpget(info, pixels, wx, ly);
				if (c != fgc && c != bgc)
				{
					if (wx == r)
					{
						add(fifo, wx, ly);
					}
					else
					{
						c = gpget(info, pixels, wx + 1, ly);
						if (c == fgc || c == bgc)
						{
							add(fifo, wx, ly);
						}
					}
				}
			}
		}
	}

	AndroidBitmap_unlockPixels(env, bitmap);
	free(fifo);
}

void
Java_jp_wildtree_android_apps_hhsadvrev_ZGraphicDrawable_gtonepaint(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray tone, jboolean tiling)
{
	int p, n, i, j, x, y, wx, wy;
	int r, g, b, c, cc, ci;
	int mask, bit;
	jbyte *tonep;
	jboolean is_copy;
	AndroidBitmapInfo info;
	unsigned char *   pixels;
	int               ret;
	int pat[][3] = {
			{0x00, 0x00, 0x00},
			{0xff, 0x00, 0x00},
			{0x00, 0xff, 0x00},
			{0xff, 0xff, 0x00},
			{0x00, 0x00, 0xff},
			{0xff, 0x00, 0xff},
			{0x00, 0xff, 0xff},
			{0xff, 0xff, 0xff},
	};
	int col[] = {
			0xff000000,
			0xff0000ff,
			0xffff0000,
			0xffff00ff,
			0xff00ff00,
			0xff00ffff,
			0xffffff00,
			0xffffffff,
	};

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
	{
		abort();
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap,  (void**)&pixels)) < 0)
	{
		abort();
	}
	if ((tonep = (*env)->GetPrimitiveArrayCritical(env, tone, &is_copy)) == NULL)
	{
		abort();
	}
	p = 0;
	n = tonep[p++];
	for (i = 1 ; i <= n ; i++)
	{
		pat[i][0] = (tonep[p++] & 0xff);
		pat[i][1] = (tonep[p++] & 0xff);
		pat[i][2] = (tonep[p++] & 0xff);
		b = 0, r = 0, g = 0;
		for (bit = 0 ; bit < 8 ; bit++)
		{
			mask = (1 << bit);
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
		col[i] = 0xff000000 | (r << 16) | (g << 8) | b;
	}
	for (wy = 0 ; wy < info.height ; wy++)
	{
		for (wx = 0 ; wx < info.width ; wx++)
		{
			c = gpget(info, pixels, wx, wy);
			b = c & 0xff;
			g = (c >>  8) & 0xff;
			r = (c >> 16) & 0xff;
			ci = ((b == 0) ? 0 : 1) + ((r == 0) ? 0 : 2) + ((g == 0) ? 0 : 4);
			cc = col[ci];
			b = cc & 0xff;
			g = (cc >>  8) & 0xff;
			r = (cc >> 16) & 0xff;
			if (tiling)
			{
				b = ((pat[ci][0] >> (7 - wx % 8)) & 1) == 0 ? 0 : 0xff;
				r = ((pat[ci][1] >> (7 - wx % 8)) & 1) == 0 ? 0 : 0xff;
				g = ((pat[ci][2] >> (7 - wx % 8)) & 1) == 0 ? 0 : 0xff;
			}
			gpset(info, pixels, wx, wy, r, g, b, 0xff);
		}
	}
	(*env)->ReleasePrimitiveArrayCritical(env, tone, tonep, JNI_ABORT); /* never modified */
	AndroidBitmap_unlockPixels(env, bitmap);

}

void
Java_jp_wildtree_android_apps_hhsadvrev_ZGraphicDrawable_ndkFillRectangle(JNIEnv *env, jobject thiz, jobject bitmap, jint sx, jint sy, jint ex, jint ey, jint c)
{
	int ret;
	int y;
	AndroidBitmapInfo info;
	unsigned char *pixels;
	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
	{
		abort();
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap,  (void**)&pixels)) < 0)
	{
		abort();
	}

	if (sy > ey)
	{
		int tmp = sy;
		sy = ey;
		ey = tmp;
	}
	for (y = sy ; y <= ey ; y++)
	{
		gpdrawline(info, pixels, sx, y, ex, y, c);
	}

	AndroidBitmap_unlockPixels(env, bitmap);
}

void
Java_jp_wildtree_android_apps_hhsadvrev_ZGraphicDrawable_ndkDrawRectangle(JNIEnv *env, jobject thiz, jobject bitmap, jint sx, jint sy, jint ex, jint ey, jint c)
{
	int ret;
	int y;
	AndroidBitmapInfo info;
	unsigned char *pixels;
	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
	{
		abort();
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap,  (void**)&pixels)) < 0)
	{
		abort();
	}

	if (sy > ey)
	{
		int tmp = sy;
		sy = ey;
		ey = tmp;
	}
	if (sx > ex)
	{
		int tmp = sx;
		sx = ex;
		ex = tmp;
	}
	gpdrawline(info, pixels, sx, sy, ex, sy, c);
	gpdrawline(info, pixels, sx, sy, sx, ey, c);
	gpdrawline(info, pixels, ex, sy, ex, ey, c);
	gpdrawline(info, pixels, sx, ey, ex, ey, c);

	AndroidBitmap_unlockPixels(env, bitmap);
}
