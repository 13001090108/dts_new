package softtest.fsm.c;

import org.w3c.dom.Node;

import softtest.cfg.c.*;
/**
 * ״̬�����������жϵ�ǰ�������ڵ��Ƿ�Ϊ״̬�Ĳ����㡣
 * ͨ���ڿ�������״ֻ̬Ҫ����һ���ڵ��������ͻ᷵��true��
 * ��Ŀǰ��DTSCpp�汾�в�û��ʵ��ʹ�ø�������
 * @author zys	
 * 2010-4-21
 */
public class FSMNextVexCondition extends FSMCondition {
	@Override
	public boolean evaluate(FSMMachineInstance fsm, FSMStateInstance state,
			VexNode vex) {
		boolean b = false;
		if (state.getVexNode() == vex) {
			b = false;
		} else {
			b = true;
		}
		return b;
	}

	/** ����xml */
	@Override
	public void loadXML(Node n) {
		if (fsm != null && fsm.getRelatedClass() != null) {
			loadAction(n, fsm.getRelatedClass());
		}
	}
}
