/**
 * 
 */
package jp.wildtree.android.apps.hhsadvrev;

import android.graphics.Color;
import android.graphics.Paint;

/**
 * @author araki
 *
 */
public class ZTeacherData extends ZObjectData {

	public ZTeacherData(byte[] b) {
		super(b);
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
	}

	/* (�� Javadoc)
	 * @see jp.wildtree.android.app.hhsadventure.ZObjectData#draw(jp.wildtree.android.app.hhsadventure.ZGraphicDrawable)
	 */
	@Override
	public void draw(ZGraphicDrawable g) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		final int ox = -32;
		int x0, x1, y0, y1;
		int[] r1 = { 18, 24, 2, 2, 2, 22, 9, 0xffff, };
		int[] r2 = { 148, 14, 126, 6, 0, 0, };
		y0 = 63;

		Paint paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setAntiAlias(false);
		paint.setStyle(Paint.Style.STROKE);

		int i;
		for (i = 0 ; i <= 172 ; i += 2)
		{
			x0 = (vector()[i] & 0xff);
			x1 = (vector()[i + 1] & 0xff);
			g.drawLine(ox + x0, y0, ox + x1, y0, paint);
			++y0;
		}
		int c = 0;
		for (int j = 0 ; r1[j] != 0xffff ; j++)
		{
			c = g.color(vector()[i++] & 0xff);
			if (c == Color.RED)
			{
				c = Color.rgb(0xcc, 0x00, 0x00);
			}
			x0 = (vector()[i++] & 0xff);
			y0 = (vector()[i++] & 0xff);
			for (int k = 0 ; k <= r1[j] + 1 ; k++)
			{
				x1 = (vector()[i++] & 0xff);
				y1 = (vector()[i++] & 0xff);
				paint.setColor(c);
				g.drawLine(ox + x0, y0, ox + x1, y1, paint);
				x0 = x1;
				y0 = y1;
			}
			x0 = (vector()[i++] & 0xff);
			y0 = (vector()[i++] & 0xff);
			g.paint(ox + x0, y0, c, c);
		}
		x0 = (vector()[i++] & 0xff);
		y0 = (vector()[i++] & 0xff);
		g.paint(ox + x0, y0, c, c);
		for (int j = 120 ; j < 124 ; j++)
		{
			paint.setColor(Color.YELLOW);
			g.drawLine(ox + j, 64, ox + j + 8, 110, paint);
			paint.setColor(Color.WHITE);
			g.drawLine(ox + j + 9, 110, ox + j + 11, 126, paint);
		}
		paint.setColor(Color.RED);
		g.drawLine(ox + 125, 111, ox + 133, 109, paint);
		g.drawLine(ox + 133, 109, ox + 134, 110, paint);
		g.drawLine(ox + 134, 110, ox + 125, 112, paint);
		g.drawLine(ox + 125, 112, ox + 125, 111, paint);
		paint.setColor(Color.WHITE);
		g.drawLine(ox + 120, 65, ox + 123, 64, paint);
		g.drawLine(ox + 123, 64, ox + 121, 62, paint);
		g.drawLine(ox + 121, 62, ox + 120, 65, paint);

		g.paint(ox + 122, 63, Color.WHITE, Color.WHITE);

	    for (int k = 0 ; r2[k + 1] != 0 ; k += 2) {
	    	x0 = r2[k];
	    	paint.setColor(Color.rgb(0xff,0xaa,0xaa));
	        for (int j = 0 ; j < r2[k + 1] ; j += 2) {
	        	y0 = (vector()[i++] & 0xff);
	        	y1 = (vector()[i++] & 0xff);
	        	if (g.tiling())
	        	{
	        		paint.setColor(Color.MAGENTA);
	        	}
	        	g.drawLine(ox + x0, y0, ox + x0, y1, paint); 
	        	++x0;
	        	if (g.tiling())
	        	{
	        		paint.setColor(Color.YELLOW);
	        	}
	        	g.drawLine(ox + x0, y0, ox + x0, y1, paint);
	        	++x0;
	        	y0 = (vector()[i++] & 0xff);
	        	y1 = (vector()[i++] & 0xff);
	        	if (g.tiling())
	        	{
	        		paint.setColor(Color.WHITE);
	        	}
	        	g.drawLine(ox + x0, y0, ox + x0, y1, paint);
	        	++x0;
	        }
	    }
	    g.drawRectangle(ox + 148, 78, ox + 164, 84, Color.BLACK);
	    g.fillRectangle(ox + 149, 79, ox + 163, 83, Color.WHITE);
	    g.fillRectangle(ox + 155, 78, ox + 156, 84, Color.BLACK);

	    for (;;) {
	    	x1 = (vector()[i++] & 0xff);
	    	y1 = (vector()[i++] & 0xff);
	        if (y1 == 0xff) {
	        	if (x1 == 0xff) {
	        		break;
	        	}
	        	x0 = (vector()[i++] & 0xff);
	        	y0 = (vector()[i++] & 0xff);
	        	continue;
	        }
	        paint.setColor(Color.BLACK);
	        g.drawLine(ox + x0, y0, ox + x1, y1, paint);
	        x0 = x1;
	        y0 = y1;
	    }
	}
	

}
