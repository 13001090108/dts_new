package softtest.domain.c.analysis;

import java.util.*;

import softtest.ast.c.Node;
import softtest.cfg.c.*;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_AllocType;
import softtest.symboltable.c.Type.CType_Pointer;


/** �����޶��� */
public class ConditionData {

	/** �����޶��� */
	class ConditionDomains {
		/** ������ */
		Domain may;

		/** �϶��� */
		Domain must;
	}
	public ConditionData (VexNode currentvex){
		this.currentvex=currentvex;
	}
	
//	/**�������캯�� */
//	public ConditionData(ConditionData oldcondata) {
//		this.currentvex=oldcondata.currentvex;
//		Hashtable<SymbolFactor, ConditionDomains> oldtable=oldcondata.getDomainsTable();
//		for(SymbolFactor s : oldtable.keySet())
//		{
//			ConditionDomains condoms=oldtable.get(s);
//			domainstable.put(s, condoms);
//		}
//	}

	VexNode currentvex=null;
	
	public VexNode getCurrentVex(){
		return currentvex;
	}
	
	public void setCurrentVex(VexNode currentvex){
		this.currentvex=currentvex;
	}

	/** �ӱ����������޶���Ĺ�ϣ�� */
	private Hashtable<SymbolFactor, ConditionDomains> domainstable = new Hashtable<SymbolFactor, ConditionDomains>();

	/** ���ù�ϣ�� */
	public void setDomainsTable(Hashtable<SymbolFactor, ConditionDomains> domainstable) {
		this.domainstable = domainstable;
	}

	/** ��ù�ϣ�� */
	public Hashtable<SymbolFactor, ConditionDomains> getDomainsTable() {
		return domainstable;
	}
	
	/** �ж�ָ�������Ƿ��ڵ�ǰ�����޶����� */
	public boolean isSymbolContained(SymbolFactor s){
		return domainstable.containsKey(s);
	}

	/** ���÷���s�Ŀ����� */
	public void addMayDomain(SymbolFactor s, Domain domain) {
		ConditionDomains domains = null;
		if (domainstable.containsKey(s)) {
			domains = domainstable.get(s);
		} else {
			domains = new ConditionDomains();
			domainstable.put(s, domains);
		}
		//��vΪ�޷�������ʱ������������ĸ���������ȥ
		//modified by zhouhb
		//2011.10.25
		if(s.getType().getName().contains("unsigned")){
			//dongyk 20120614
			if(domain instanceof PointerDomain)
			{
				domain=PointerDomain.castToIntegerDomain(domain);
			}
			domain=IntegerDomain.intersect(new IntegerDomain(0,IntegerDomain.DEFAULT_MAX), (IntegerDomain)domain);
			domains.may = domain;
		}else{
			domains.may = domain;
		}
	}

	/** ���÷���s�Ŀ϶��� */
	public void addMustDomain(SymbolFactor s, Domain domain) {
		ConditionDomains domains = null;
		if (domainstable.containsKey(s)) {
			domains = domainstable.get(s);
		} else {
			domains = new ConditionDomains();
			domainstable.put(s, domains);
		}
		//��vΪ�޷�������ʱ������������ĸ���������ȥ
		//modified by zhouhb
		//2011.10.25
		if(s.getType().getName().contains("unsigned")){
			//dongyk 20120614
			if(domain instanceof PointerDomain)
			{
				domain=PointerDomain.castToIntegerDomain(domain);
			}
			domain=IntegerDomain.intersect(new IntegerDomain(0,IntegerDomain.DEFAULT_MAX), (IntegerDomain)domain);
			domains.must = domain;
		}else{
			domains.must = domain;
		}
	}

	/** ��÷���s�Ŀ����� */
	public Domain getMayDomain(SymbolFactor s) {
		if(s==null)
			return null;
		ConditionDomains domains = domainstable.get(s);
		if (domains == null) {
			return null;
		}
		return domains.may;
	}

	/** ��÷���s�Ŀ϶��� */
	public Domain getMustDomain(SymbolFactor s) {
		ConditionDomains domains = domainstable.get(s);
		if (domains == null) {
			return null;
		}
		return domains.must;
	}

	/** �����ϣ�� */
	public void clearDomains() {
		domainstable.clear();
	}

	/** �������Ϊ��ǰ���µ������޶������� */
	public SymbolDomainSet getTrueMayDomainSet() {
		SymbolDomainSet ret= new SymbolDomainSet();
		Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
		Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
			if (e.getValue().may != null) {
				ret.addDomain(e.getKey(), e.getValue().may);
			}
		}
		return ret;
	}

	/** �������Ϊ��ǰ���µ������޶������� */
	public SymbolDomainSet getFalseMayDomainSet() {
		SymbolDomainSet ret= new SymbolDomainSet();
		Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
		Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
			SymbolFactor s=e.getKey();
			if (e.getValue().must != null) {
				Domain a=s.getDomainWithoutNull(currentvex.getSymDomainset());
				CType type=s.getType();
				Domain b = e.getValue().must;
				Domain result=Domain.substract(a, b, type);
				if(result instanceof PointerDomain){
					if(((PointerDomain)result).getValue().toString().equals("NOTNULL")&&((PointerDomain)result).Type.isEmpty())
						((PointerDomain)result).Type.add(CType_AllocType.NotNull);
					else if(((PointerDomain)result).getValue().toString().equals("NULL")&&((PointerDomain)result).Type.isEmpty())
						((PointerDomain)result).Type.add(CType_AllocType.Null);
				}
				//��ָ����������ж�
				//add by zhouhb 2010/7/19
				if(a instanceof PointerDomain && result instanceof PointerDomain &&((PointerDomain)result).allocRange.isEmpty()){
					((PointerDomain)result).allocRange=new IntegerDomain(0,0);
				}
				ret.addDomain(s, result);
			}
		}
		return ret;
	}

	/** �������Ϊ��ǰ���µ������޶��϶��� */
	public SymbolDomainSet getTrueMustDomainSet() {
		SymbolDomainSet ret= new SymbolDomainSet();
		Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
		Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
			if (e.getValue().must != null) {
				ret.addDomain(e.getKey(), e.getValue().must);
			}
		}
		return ret;
	}

	/** �������Ϊ��ǰ���µ������޶��϶��� */
	public SymbolDomainSet getFalseMustDomainSet() {
		SymbolDomainSet ret= new SymbolDomainSet();
		Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
		Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
			SymbolFactor s=e.getKey();
			if (e.getValue().may != null) {
				Domain a=s.getDomainWithoutNull(currentvex.getSymDomainset());
				CType type=s.getType();
				Domain b = e.getValue().may;
				Domain result=Domain.substract(a, b, type);
				if(result instanceof PointerDomain){
					if(((PointerDomain)result).getValue().toString().equals("NOTNULL")&&((PointerDomain)result).Type.isEmpty())
						((PointerDomain)result).Type.add(CType_AllocType.NotNull);
					else if(((PointerDomain)result).getValue().toString().equals("NULL")&&((PointerDomain)result).Type.isEmpty())
						((PointerDomain)result).Type.add(CType_AllocType.Null);
				}
				//��ָ����������ж�
				//add by zhouhb 2010/7/19
				if(a instanceof PointerDomain){					
					if(result instanceof IntegerDomain)
					{
						result=Domain.castToType(result, new CType_Pointer());
					}
					if(result instanceof PointerDomain&&((PointerDomain)result).allocRange.isEmpty())
					{
						((PointerDomain)result).allocRange=new IntegerDomain(0,0);
					}
				}
				ret.addDomain(s, result);
			}
		}
		return ret;
	}

	/** �ж������޶��������Ƿ�ì�� */
	public boolean isMayContradict() {
		SymbolDomainSet ds=getTrueMayDomainSet();
		return ds.isContradict();
	}

	/** �ж������޶��϶����Ƿ�ì�� */
	public boolean isMustContradict() {
		SymbolDomainSet ds=getTrueMustDomainSet();
		return ds.isContradict();
	}
	
	/** ��ӡ */
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		Set<Map.Entry<SymbolFactor, ConditionDomains>> entryset = domainstable.entrySet();
		Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entryset.iterator();
		SymbolFactor v = null;
		ConditionDomains d = null;
		while (i.hasNext()) {
			Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
			v = e.getKey();
			d = e.getValue();
			b.append("" + v.getSymbol() + ":{may:" + d.may + "must:" + d.must + "}");
		}
		return b.toString();
	}
	
	private boolean isLogicallyTrueBut(SymbolDomainSet ds,SymbolFactor v){
		SymbolFactor v1 = null;
		Domain d1 = null,d2=null;
		Set<Map.Entry<SymbolFactor,Domain>> entryset = ds.getTable().entrySet();
		Iterator<Map.Entry<SymbolFactor,Domain>> i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e = i.next();
			v1 = e.getKey();
			d1= e.getValue();
			if(v1==v){
				continue;
			}
			d2=v1.getDomainWithoutNull(currentvex.getSymDomainset());
			if(d2.equals(d1)){
				return true;
			}
		}		
		return false;
	}
	
	private boolean isContradicBut(SymbolDomainSet ds,SymbolFactor v){
		SymbolFactor v1 = null;
		Domain d1 = null;
		Set<Map.Entry<SymbolFactor,Domain>> entryset = ds.getTable().entrySet();
		Iterator<Map.Entry<SymbolFactor,Domain>> i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e = i.next();
			v1 = e.getKey();
			d1=e.getValue();
			if(v1==v){
				continue;
			}
			if(Domain.isEmpty(d1)){
				return true;
			}
		}
		return false;
	}
	
	/** �����е�����������Ϊ��Ӧ�������޶��϶��� */
	public void addMustDomain(SymbolDomainSet ds) {
		Set<Map.Entry<SymbolFactor,Domain>> entryset = ds.getTable().entrySet();
		Iterator<Map.Entry<SymbolFactor,Domain>> i = entryset.iterator();
		SymbolFactor v = null;
		Domain d = null;
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e =  i.next();
			v = (SymbolFactor) e.getKey();
			d = e.getValue();
			addMustDomain(v, d);
		}
	}

	/** �����е�����������Ϊ��Ӧ�������޶������� */
	public void addMayDomain(SymbolDomainSet ds) {
		Set<Map.Entry<SymbolFactor,Domain>> entryset = ds.getTable().entrySet();
		Iterator<Map.Entry<SymbolFactor,Domain>> i = entryset.iterator();
		SymbolFactor v = null;
		Domain d = null;
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e =  i.next();
			v = (SymbolFactor) e.getKey();
			d = e.getValue();
			addMayDomain(v, d);
		}
	}
	
	public ConditionData calLogicalAndExpression(ConditionData leftdata,ConditionData rightdata){
		ConditionData r=new ConditionData(currentvex);
		SymbolDomainSet may1 = leftdata.getTrueMayDomainSet();
		SymbolDomainSet may2 = rightdata.getTrueMayDomainSet();

		SymbolDomainSet must1 = leftdata.getTrueMustDomainSet();
		SymbolDomainSet must2 = rightdata.getTrueMustDomainSet();
		
		SymbolDomainSet may=new SymbolDomainSet();
		SymbolDomainSet must=new SymbolDomainSet();
		
		SymbolFactor v1 = null, v2 = null;
		Domain d1 = null, d2 = null;
		Set<Map.Entry<SymbolFactor,Domain>> entryset = may1.getTable().entrySet();
		Iterator<Map.Entry<SymbolFactor,Domain>> i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e = i.next();
			v1 = (SymbolFactor) e.getKey();
			d1 = e.getValue();
			// ��domainset�в���
			d2 = may2.getDomain(v1);
			CType type=v1.getType();
			if (d2 == null) {
				if(!isContradicBut(may2,v1)){
					Domain d=v1.getDomainWithoutNull(currentvex.getSymDomainset());
					may.addDomain(v1,Domain.intersect(d, d1, type));
				}else{
					//always FALSE, add EMPTY
					may.addDomain(v1,Domain.getEmptyDomainFromType(type));
				}
			} else {
				// ȡintersect
				may.addDomain(v1, Domain.intersect(d1, d2,v1.getType()));
			}
		}

		// ��b����Щaû�г��ֵĲ��뵽table��
		entryset = may2.getTable().entrySet();
		i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e =  i.next();
			v2 = e.getKey();
			d2 = e.getValue();
			// ��domainset�в���
			d1 = may1.getDomain(v2);
			CType type=v2.getType();
			if (d1 == null) {
				if(!isContradicBut(may1,v2)){
					Domain d=v2.getDomainWithoutNull(currentvex.getSymDomainset());
					may.addDomain(v2,Domain.intersect(d, d2, type));
				}else{
					//always FALSE, add EMPTY
					may.addDomain(v2,Domain.getEmptyDomainFromType(type));
				}				
			}
		}		
		
		r.addMayDomain(may);
		//����must
		entryset = must1.getTable().entrySet();
		i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e = i.next();
			v1 = (SymbolFactor) e.getKey();
			d1 = e.getValue();
			// ��domainset�в���
			d2 = must2.getDomain(v1);
			CType type=v1.getType();
			if (d2 == null) {
				if(isLogicallyTrueBut(must2,v1)){
					//���棬ȡ��ǰȡ���������must
					must.addDomain(v1, d1);
				}else{
					must.addDomain(v1,Domain.getEmptyDomainFromType(type));
				}
			} else {
				// ȡintersect
				must.addDomain(v1,Domain.intersect(d1, d2,v1.getType()));
			}
		}

		// ��b����Щaû�г��ֵĲ��뵽table��
		entryset = must2.getTable().entrySet();
		i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e = i.next();
			v2 = e.getKey();
			d2 = e.getValue();
			// ��domainset�в���
			d1 = must1.getDomain(v2);
			if (d1 == null) {
				if(isLogicallyTrueBut(must1,v2)){
					//���棬ȡ���������must
					must.addDomain(v2, d2);
				}else{				
					must.addDomain(v2, Domain.getEmptyDomainFromType(v2.getType()));
				}			
			}else{
				must.addDomain(v2,Domain.intersect(d1, d2,v2.getType()));
			}
		}		
		
		r.addMustDomain(must);			
		return r;
	}
	
	public ConditionData calLogicalOrExpression(ConditionData leftdata,ConditionData rightdata){
		ConditionData r=new ConditionData(currentvex);
		SymbolDomainSet may1 = leftdata.getTrueMayDomainSet();
		SymbolDomainSet may2 = rightdata.getTrueMayDomainSet();

		SymbolDomainSet must1 = leftdata.getTrueMustDomainSet();
		SymbolDomainSet must2 = rightdata.getTrueMustDomainSet();
		
		SymbolDomainSet may=new SymbolDomainSet();
		SymbolDomainSet must=new SymbolDomainSet();
		
		SymbolFactor v1 = null, v2 = null;
		Domain d1 = null, d2 = null;
		Set<Map.Entry<SymbolFactor,Domain>> entryset = may1.getTable().entrySet();
		Iterator<Map.Entry<SymbolFactor,Domain>> i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e = i.next();
			v1 =  e.getKey();
			d1 = e.getValue();
			// ��domainset�в���
			d2 = may2.getDomain(v1);
			CType type=v1.getType();
			if (d2 == null) {
				if(!isContradicBut(may2,v1)){
					//��ì�ܣ�ȡȫ��
					Domain d=v1.getDomainWithoutNull(currentvex.getSymDomainset());
					may.addDomain(v1,Domain.intersect(d, Domain.getFullDomainFromType(type), type));
				}else{
					//ì�ܣ�ȡ���������may
					may.addDomain(v1, d1);
				}
			} else {
				// ȡunion��
				may.addDomain(v1, Domain.union(d1, d2,v1.getType()));
			}
		}

		// ��b����Щaû�г��ֵĲ��뵽table��
		entryset = may2.getTable().entrySet();
		i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e =  i.next();
			v2 =  e.getKey();
			d2 = e.getValue();
			// ��domainset�в���
			d1 = may1.getDomain(v2);
			CType type=v2.getType();
			if (d1 == null) {
				if(!isContradicBut(may1,v2)){
					//��ì�ܣ�ȡȫ��
					Domain d=v2.getDomainWithoutNull(currentvex.getSymDomainset());
					may.addDomain(v2,Domain.intersect(d, Domain.getFullDomainFromType(type), type));
				}else{
					//ì�ܣ�ȡ���������may
					may.addDomain(v2, d2);
				}
			}
		}		
		
		r.addMayDomain(may);
		//����must
		entryset = must1.getTable().entrySet();
		i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain> e = i.next();
			v1 = e.getKey();
			d1 = e.getValue();
			// ��domainset�в���
			d2 = must2.getDomain(v1);
			CType type=v1.getType();
			if (d2 == null) {
				if (isLogicallyTrueBut(must2, v1)) {
					// ���棬ȡȫ��
					Domain d=v1.getDomainWithoutNull(currentvex.getSymDomainset());
					may.addDomain(v1,Domain.intersect(d, Domain.getFullDomainFromType(type), type));
				}else{
					//�����棬ȡ���������must
					must.addDomain(v1, d1);
				}
			} else {
				// ȡunion���������⴦��
				must.addDomain(v1, Domain.union(d1, d2,v1.getType()));
			}
		}

		// ��b����Щaû�г��ֵĲ��뵽table��
		entryset = must2.getTable().entrySet();
		i = entryset.iterator();
		while (i.hasNext()) {
			Map.Entry<SymbolFactor,Domain>e =  i.next();
			v2 =  e.getKey();
			d2 = e.getValue();
			// ��domainset�в���
			d1 = must1.getDomain(v2);
			CType type=v2.getType();
			if (d1 == null) {
				if(isLogicallyTrueBut(must1,v2)){
					//���棬ȡȫ��	
					Domain d=v2.getDomainWithoutNull(currentvex.getSymDomainset());
					may.addDomain(v2,Domain.intersect(d, Domain.getFullDomainFromType(type), type));
				}else{
					//�����棬ȡ���������must
					must.addDomain(v2, d2);
				}
			}
		}		
		
		r.addMustDomain(must);		
		return r;
	}
	
	public static ConditionData calLoopCondtion(ConditionData data,VexNode vex,ConditionDomainVisitor convisitor,Node treenode){
		treenode.jjtAccept(convisitor, data);
		if(!Config.LOOPCAL || DomainVexVisitor.looplayer > Config.LOOPLAYER){
			//zys:2010.11.11	���û�ж�ѭ��������ȷ����,����Ҫ������ȷ��MUST���䣻
			//���ִ�����CPP��ֻ��ѭ������һ�ε���������Ȼ�������䱣�أ���ѭ���ڵ������Ǵ����
			SymbolDomainSet old=vex.getSymDomainset();
			ConditionData data1=new ConditionData(vex);
			vex.setSymDomainset(null);
			treenode.jjtAccept(convisitor, data1);
			vex.setSymDomainset(old);
			
			Set entryset = data1.domainstable.entrySet();
			Iterator i = entryset.iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry) i.next();
				SymbolFactor v = (SymbolFactor) e.getKey();
				ConditionDomains d = (ConditionDomains)e.getValue();
				data.addMustDomain(v, d.must);
			}
		}
		return data;
	}
}
