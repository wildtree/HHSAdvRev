package jp.wildtree.android.apps.hhsadvrev;

public class ZRuleBase {
	
	private final int _map_id;
	private final int _cmd_id;
	private final int _obj_id;
    private final ZRuleBlock[] _rules;

	public static final int file_block_size = 96;
	public static final int EndOfRule = 0xff;
	private static final int RULE_BLOCK_LENGTH = (file_block_size / 4 - 1);
	
	public ZRuleBase(byte[] b)
	{
		_rules  = new ZRuleBlock[RULE_BLOCK_LENGTH];
		
		_map_id = (b[0] & 0xff);
		_cmd_id = (b[1] & 0xff);
		_obj_id = (b[2] & 0xff);
		for (int i = 0 ; i < RULE_BLOCK_LENGTH ; i++)
		{
			byte[] rb = new byte[4];
			System.arraycopy(b, 4 + 4 * i, rb, 0, 4);
			_rules[i] = new ZRuleBlock(rb);
		}
	}
	
	public boolean endOfRule()
	{
		return (_map_id == 0xff);
	}
	
	public int mapId()
	{
		return _map_id;
	}
	
	public int cmdId()
	{
		return _cmd_id;
	}
	
	public int objId()
	{
		return _obj_id;
	}
	
	public boolean about(int mapId, int cmdId, int objId)
	{
		if (mapId == _map_id || _map_id == 0)
		{
			if (cmdId == _cmd_id || _cmd_id == 0)
			{
				if (objId == _obj_id || _obj_id == 0)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean about(ZSystemParams p)
	{
		return about(p.mapId(), p.cmdId(), p.objId());
	}
	
	public boolean condBlock(ZSystemParams p, ZUserData u)
	{
		boolean ok = true;
		
		
		
		return ok;
	}
	
	public boolean run(MainActivity main)
	{
		if (about(main.zSystem))
		{
			boolean cond_ok = true;
			boolean act_ok = true;
			int i = 0;
			while (_rules[i].actCmp())
			{
				cond_ok = _rules[i++].doCompare(main.zSystem, main.userData);
				if (!cond_ok)
				{
					return false; // cond fail.
				}
			}
			if (cond_ok) // all clear. go action phase
			{
				while (_rules[i].op() != ZRuleBlock.ACT_NOP)
				{
					act_ok = _rules[i++].doAction(main) && act_ok;
				}
				if (act_ok)
				{
					main.msgByResId(R.string.msg_okay);
				}
			}
			return cond_ok;
		}
		return false;
	}
}
