package softtest.domain.c.analysis;

import java.util.*;

import softtest.cfg.c.VexNode;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.symbolic.*;
import softtest.symboltable.c.*;

public class ValueSet {
	private Hashtable<VariableNameDeclaration, Expression> valuetable = new Hashtable<VariableNameDeclaration, Expression>();
	
	/** ȱʡ���캯�� */
	public ValueSet() {

	}

	/** �������캯�� ,��ǰ��û�н�����㿽��*/
	public ValueSet(ValueSet as) {
		Expression value = null;
		Hashtable<VariableNameDeclaration, Expression> table=as.valuetable;
		for(VariableNameDeclaration v : table.keySet()){
			value =table.get(v);
			addValue(v, value);
		}
	}

	/** ���ñ���v�ĳ���ȡֵ */
	public Expression addValue(VariableNameDeclaration v, Expression value) {
		return valuetable.put(v, value);
	}

	/** ��ñ���v�ĳ���ȡֵ */
	public Expression getValue(VariableNameDeclaration v) {
		//add by cmershen,2016.9.2
		if(valuetable!=null)
			return valuetable.get(v);
		else
			return null;	
	}
	
	/** �����ϣ�� */
	public void clearValueSet() {
		valuetable.clear();
	}

	/** ���ù�ϣ�� */
	public void setTable(Hashtable<VariableNameDeclaration, Expression> valuetable) {
		this.valuetable = valuetable;
	}

	/** ��ù�ϣ�� */
	public Hashtable<VariableNameDeclaration, Expression> getTable() {
		return valuetable;
	}
	
	/** �жϹ�ϣ���Ƿ�Ϊ�� */
	public boolean isEmpty() {
		return valuetable.isEmpty();
	}
	
	/** ��ӡ */
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		if (valuetable.size() == 0) {
			b.append("empty");
			return b.toString();
		}
		
		Expression d = null;
		for(VariableNameDeclaration v : valuetable.keySet()){
			d =valuetable.get(v);
			b.append(v.getImage() + ":" + d+ "  ");
		}
		return b.toString();
	}
	
	public HashSet<SymbolFactor> getAllSymbol(){
		HashSet<SymbolFactor> ret=new HashSet<SymbolFactor>();
		
		Expression d = null;
		for(VariableNameDeclaration v : valuetable.keySet()){
			d =valuetable.get(v);
			ret.addAll(d.getAllSymbol());
		}
		return ret;
	}
	/*
	 * �Ƴ���Щ������ǰ�������ڵ�������ı���
	 */
	public void removeOutScopeVar(VexNode vex) {
		ValueSet temp = new ValueSet();
		Set<Map.Entry<VariableNameDeclaration, Expression>> entryset = vex.getValueSet().getTable().entrySet();
		Iterator<Map.Entry<VariableNameDeclaration, Expression>> i = entryset.iterator();
		Expression d1 = null;
		VariableNameDeclaration v1 = null;
		while (i.hasNext()) {
			Map.Entry<VariableNameDeclaration, Expression> e = i.next();
			v1 = e.getKey();
			d1 = e.getValue();
			if (vex.getTreenode().getScope().isSelfOrAncestor(v1.getScope())) {
				temp.getTable().put(v1, d1);
			}
		}
		this.valuetable = temp.getTable();
	}
}
