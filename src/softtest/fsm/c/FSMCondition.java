package softtest.fsm.c;

import softtest.cfg.c.*;

/** ת������ */
public abstract class FSMCondition extends FSMElement {
	/** ���������м��㣬�ж����Ƿ����� */
	public abstract boolean evaluate(FSMMachineInstance fsm,
			FSMStateInstance state, VexNode vex);

	/** ״̬�������ߵ�accept���� */
	@Override
	public void accept(FSMVisitor visitor, Object data) {
		visitor.visit(this, data);
	}
}