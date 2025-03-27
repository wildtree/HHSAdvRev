package jp.wildtree.android.apps.hhsadvrev;

public class ZRuleBlock {
	private final int _action;
	private final int _op;
	private final int _type;
	private final int _id;
	
	private final int _body_type;
	private final int _body_id;
	private final int _body_value;
	private final int _body_offset;
	
	public static final int ACT_COMP = 0;
	public static final int ACT_ACTION = 1;
	
	public static final int CMP_NOP = 0;
	public static final int CMP_EQ = 1;
	public static final int CMP_NE = 2;
	public static final int CMP_GT = 3;
	public static final int CMP_GE = 4;
	public static final int CMP_LT = 5;
	public static final int CMP_LE = 6;
	
	public static final int ACT_NOP = 0;
	public static final int ACT_MOVE = 1;
	public static final int ACT_ASGN = 2;
	public static final int ACT_MESG = 3;
	public static final int ACT_DLOG = 4;
	public static final int ACT_LOOK = 5;
	public static final int ACT_SND = 6;
	public static final int ACT_OVER = 7;

	public static final int TYPE_NONE = 0;
	public static final int TYPE_FACT = 1;
	public static final int TYPE_PLACE = 2;
	public static final int TYPE_SYSTEM = 3;
	public static final int TYPE_VECTOR = 4;
	
	public ZRuleBlock(byte[] b)
	{
		int header = (((b[0] & 0xff) << 8) | (b[1] & 0xff));
		
		_action = (header & 0x8000) >> 15;
		_op     = (header & 0x7000) >> 12;
		_type   = (header & 0x00e0) >>  5;
		_id     = (header & 0x001f);

		_body_offset = (b[2] & 0xff);
		_body_value  = (b[3] & 0xff);
		
		_body_type  = (_body_offset & 0xe0) >> 5;
		_body_id    = (_body_offset & 0x1f);
	}

	public int action()
	{
		return _action;
	}
	
	public int op()
	{
		return _op;
	}
	public int type()
	{
		return _type;
	}
	public int id()
	{
		return _id;
	}
	public int body_type()
	{
		return _body_type;
	}
	public int body_id()
	{
		return _body_id;
	}
	public int value()
	{
		return _body_value;
	}
	public int offset()
	{
		return _body_offset;
	}
	
	public int getOperand1(ZSystemParams p, ZUserData u)
	{
		int v = 0;
		switch (_type)
		{
		case TYPE_NONE:
			break;
		case TYPE_FACT:
			v = u.fact[_id];
			break;
		case TYPE_PLACE:
			v = u.place[_id];
			break;
		case TYPE_SYSTEM:
			p.random(0);
			v = p.table[_id];
			break;
		case TYPE_VECTOR:
			v = u.map[_body_offset -1].get(_id);
			break;
		default:
			break;
		}
		return v;
	}
	
	public int getOperand2(ZSystemParams p, ZUserData u)
	{
		int v = _body_value;
		if (_body_type != TYPE_NONE && _type != TYPE_VECTOR)
		{
			switch (_body_type)
			{
			case TYPE_FACT:
				v = u.fact[_body_id];
				break;
			case TYPE_PLACE:
				v = u.place[_body_id];
				break;
			case TYPE_SYSTEM:
				p.random(0);
				v = p.table[_body_id];
				break;
			default:
				break;
			}
		}
		return v;
	}
	
	public boolean actCmp()
	{
		return _action == ACT_COMP;
	}
	
	public boolean actAction()
	{
		return _action == ACT_ACTION;
	}
	
	public boolean doCompare(ZSystemParams p, ZUserData u)
	{
		boolean ok = false;
		int v1 = getOperand1(p, u);
		int v2 = getOperand2(p, u);
		switch (_op)
		{
		case CMP_EQ: ok = (v1 == v2); break;
		case CMP_NE: ok = (v1 != v2); break;
		case CMP_GT: ok = (v1 > v2);  break;
		case CMP_GE: ok = (v1 >= v2); break;
		case CMP_LT: ok = (v1 < v2);  break;
		case CMP_LE: ok = (v1 <= v2); break;
		default:     ok = false;      break;
		}
		return ok;
	}
	
	public boolean doAction(MainActivity main)
	{
		boolean ok = false;
		
		ZUserData u = main.userData;
		ZSystemParams p = main.zSystem;
		switch (_op)
		{
		case ACT_MOVE:
			if (u.map[p.mapId() - 1].get(_body_value) != 0)
			{
				p.mapId(u.map[p.mapId() - 1].get(_body_value)); // move!
				return true;
			}
			// check teacher
			if (u.fact[1] == p.mapId() && p.random() > 85)
			{
				main.msgout(0xb5); // U are arrested by the teacher!!
				u.fact[1] = 0; // teacher is gone.
				main.setColorFilter(MainActivity.cf_mode_sepia);
				main.gameOver();
				return false;
			}
			main.msgout(0xb6); // you cannot move
			return true;
		case ACT_ASGN:
			int v1 = _body_offset;
			int v2 = getOperand2(p, u);
			switch (_type)
			{
			case TYPE_FACT:
				u.fact[_id] = v2;
				break;
			case TYPE_PLACE:
				u.place[_id] = v2;
				break;
			case TYPE_SYSTEM:
				p.table[_id] = (byte)(v2 & 0xff);
				break;
			case TYPE_VECTOR:
				u.map[v1 - 1].set(_id, v2);
				break;
			}
			if (_type == TYPE_PLACE || _type == TYPE_FACT)
			{
				return true;
			}
			if(_type == TYPE_SYSTEM)
			{
				if (_id == 5)
				{
					p.random(0);
				}
			}
			return true;
		case ACT_MESG:
			main.msgout(_body_value);
			return true;
		case ACT_DLOG:
			main.dialog(_body_value);
			return true;
		case ACT_LOOK:
			if (_body_value == 0)
			{
				p.mapId(p.mapView()); // back
				p.mapView(0);
			}
			else
			{
				p.mapView(p.mapId());
				p.mapId(_body_value);
			}
			return true;
		case ACT_SND:
			// _body_value is sound number
			main.play(_body_value);
			return true;
		case ACT_OVER:
			switch(_body_value)
			{
			case 0:
				main.setColorFilter(MainActivity.cf_mode_sepia);
				main.msgByResId(R.string.msg_gameover);
				u.fact[1] = 0; // teacher has gone
				break;
			case 1:
				main.setColorFilter(MainActivity.cf_mode_red);
				main.msgByResId(R.string.msg_gameover);
				break;
			case 2:
				main.setColorFilter(MainActivity.cf_mode_normal);
				main.msgByResId(R.string.msg_finished);
				main.gameCleared();
				return false;
			}
			main.gameOver();
			return false;
				
		}
		return ok;
	}
}
