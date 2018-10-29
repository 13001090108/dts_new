package softtest.fsm.c;

import java.util.*;

import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.ValueSet;
import softtest.fsm.c.FSMStateInstance;

/** ״̬ʵ������ */
public class FSMStateInstanceSet {
	/** ״̬ʵ��hash�� */
	private Hashtable<FSMStateInstance, FSMStateInstance> table = new Hashtable<FSMStateInstance, FSMStateInstance>();

	/** ���캯�� */
	public FSMStateInstanceSet() {

	}

	/** �������캯�� */
	public FSMStateInstanceSet(FSMStateInstanceSet set) {
		for (Enumeration<FSMStateInstance> e = set.table.elements(); e
				.hasMoreElements();) {
			FSMStateInstance newinstance = new FSMStateInstance(e.nextElement());
			table.put(newinstance, newinstance);
		}
	}

	/** ����״̬ʵ�� */
	public void addStateInstance(FSMStateInstance stateinstance) {
		if (stateinstance.getState().isFinal()) {
			// ������ս�״̬�򲻼���
			return;
		}
		FSMStateInstance si = table.get(stateinstance);
		if (si != null) {
			// ��״̬ʵ���Ѿ�������,Ӧ�ÿ��������ĺϲ�
			si.setSymbolDomainSet(SymbolDomainSet.union(si.getSymbolDomainSet(), stateinstance.getSymbolDomainSet()));
			si.mergeValueSet(stateinstance.getValueSet(),stateinstance.getSymbolDomainSet());
			si.getVarDomainSet();
		} else {
			// ֱ�Ӽ���
			si = new FSMStateInstance(stateinstance);
			table.put(si, si);
		}

	}
	
	/**
	 * ����״̬��������״̬��Ӧ����ĺϲ�
	 */
	public void addStateInstanceWithoutConditon(FSMStateInstance stateinstance) {
		if (stateinstance.getState().isFinal()) {
			// ������ս�״̬�򲻼���
			return;
		}
		FSMStateInstance si = table.get(stateinstance);
		if (si == null) {
			si= new FSMStateInstance(stateinstance);
			table.put(si, si);
		}
	}

	/** �Ƴ�״̬ʵ�� */
	public void removeStateInstance(FSMStateInstance stateinstance) {
		table.remove(stateinstance);
	}

	/** ���״̬ʵ����ϣ�� */
	public Hashtable<FSMStateInstance, FSMStateInstance> getTable() {
		return table;
	}

	/** ���ݵ�ǰ�������ڵ㣬����״̬���ϵ�����״̬���� */
	public void calDomainSet(VexNode vex) {
		List<FSMStateInstance> todelete = new ArrayList<FSMStateInstance>();
		for (Enumeration<FSMStateInstance> e = table.elements(); e
				.hasMoreElements();) {
			FSMStateInstance s = e.nextElement();
			s.calDomainSet(vex);
			if (s.getVarDomainSet().isContradict()) {
				todelete.add(s);
			}
		}
		// ɾ����Щì��״̬
		Iterator<FSMStateInstance> i = todelete.iterator();
		while (i.hasNext()) {
			table.remove(i.next());
		}
	}
	
	/** ����ǰ���������жϽڵ���Ƿ�Ϊ���֧��־������״̬���ϵ�����״̬���� */
	public void calCondition(VexNode pre,boolean istruebranch) {
		List<FSMStateInstance> todelete = new ArrayList<FSMStateInstance>();
		for (Enumeration<FSMStateInstance> e = table.elements(); e
				.hasMoreElements();) {
			FSMStateInstance s = e.nextElement();
			s.calCondition(pre,istruebranch);
			if (s.getVarDomainSet().isContradict()) {
				todelete.add(s);
			}
		}
		// ɾ����Щì��״̬
		Iterator<FSMStateInstance> i = todelete.iterator();
		while (i.hasNext()) {
			table.remove(i.next());
		}
	}
	
	/** ����caselabel�ڵ��switch�ڵ㣬����״̬���ϵ�����״̬���� */
	public void calSwitch(VexNode n, VexNode pre) {
		List<FSMStateInstance> todelete = new ArrayList<FSMStateInstance>();
		for (Enumeration<FSMStateInstance> e = table.elements(); e
				.hasMoreElements();) {
			FSMStateInstance s = e.nextElement();
			s.calSwitch(n, pre);
			if (s.getVarDomainSet().isContradict()) {
				todelete.add(s);
			}
		}
		// ɾ����Щì��״̬
		Iterator<FSMStateInstance> i = todelete.iterator();
		while (i.hasNext()) {
			table.remove(i.next());
		}
	}
	
	/** ��ӡ */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		boolean b = false;
		for (Enumeration<FSMStateInstance> e = table.elements(); e
				.hasMoreElements();) {
			FSMStateInstance s = e.nextElement();
			sb.append(s + ",");
			b = true;
		}
		if (b) {
			sb.setCharAt(sb.length() - 1, '}');
		} else {
			sb.append("}");
		}
		return sb.toString();
	}

	/** �ж�״̬�����Ƿ�Ϊ�ռ� */
	public boolean isEmpty() {
		return table.isEmpty();
	}
}
