package jp.wildtree.android.apps.hhsadvrev;

public class ZWord {
	public String word;
	public int id;
	
	public static final int INVALID_WORD = 0;
	
	public ZWord(byte[] b)
	{
		word = "";
		id   = -1;
		for (int i = 0 ; i < 4 ; i++)
		{
			int z = (b[i] & 0xff);
			if (z == 0) break;
			word += String.format("%c", (byte)(z - 1));
		}
		id = b[4];
	}
	
	public boolean match(String v)
	{
		String z = v + "    ";
		z = z.substring(0, 4).toUpperCase(); // normalie
		return z.equalsIgnoreCase(word);
	}
}
