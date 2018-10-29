package softtest.fsm.c;

import org.w3c.dom.Node;

import softtest.cfg.c.*; 
import softtest.ast.c.*;
import softtest.symboltable.c.*;

/** 
 * ��������������ֻ�ܳ����ڱ�����ص�״̬���У��жϵ�ǰ�������ڵ��Ӧ���������ڱ����������������滹�����档
 * isout���ָʾȡ�����滹�������������Ϊevaluate����ֵ���ء� 
 * */
public class FSMScopeCondition extends FSMCondition {
	private boolean isout = false;

	/** ���������м��㣬�ж����Ƿ����� */
	@Override
	public boolean evaluate(FSMMachineInstance fsm, FSMStateInstance state,
			VexNode vex) {
		boolean b = false;
		VariableNameDeclaration v = fsm.getRelatedVariable();
		if (v != null) {
			Scope delscope = v.getScope();
			SimpleNode astnode = vex.getTreenode();
			if (isout) {
				if (!astnode.getScope().isSelfOrAncestor(delscope)) {
					// �����������Ѿ����ǵ�ǰ�������Լ�������
					b = true;
				} else if (delscope.isSelfOrAncestor(astnode.getScope())
						&& delscope != astnode.getScope() && vex.isBackNode()) {
					// ��ǰ�������������������Լ����߸��ף����ǵ�ǰ�ڵ���Ҫ��ֹ��ǰ������
					b = true;
				} else {
					b = false;
				}
			} else {
				if (astnode.getScope().isSelfOrAncestor(delscope)) {
					// ��ǰ����������������������
					b = true;
				} else {
					b = false;
				}
			}
		}
		if (b) {
			if (relatedmethod == null) {
				b = true;
			} else {
				Object[] args = new Object[2];
				args[0] = vex;
				args[1] = fsm;
				try {
					Boolean r = (Boolean) relatedmethod.invoke(null, args);
					b = r;
				} catch (Exception e) {
					 e.printStackTrace();
					throw new RuntimeException("action error",e);
				}
			}
		}
		return b;
	}

	/** ����xml */
	@Override
	public void loadXML(Node n) {
		Node type = n.getAttributes().getNamedItem("Type");
		if (type.getNodeValue().equals("out")) {
			isout = true;
		}

		if (fsm != null && fsm.getRelatedClass() != null) {
			loadAction(n, fsm.getRelatedClass());
		}
	}
}
