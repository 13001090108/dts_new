package softtest.fsm.c;

import java.util.Hashtable;

import softtest.ast.c.*;
import softtest.cfg.c.*;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ConditionData;
import softtest.domain.c.analysis.ConditionDomainVisitor;
import softtest.domain.c.analysis.DomainVexVisitor;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.ValueSet;
import softtest.domain.c.analysis.VarDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.NumberFactor;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_Array;
import softtest.symboltable.c.Type.CType_Pointer;

/** ״̬ʵ�� */
public class FSMStateInstance {
	/** ��ǰ״̬ */
	private FSMState state = null;

	/** ��ǰ״̬���������������� */
	private VarDomainSet vardomainset;
	private ValueSet valueSet=new ValueSet();
	private SymbolDomainSet symbolDomainSet=new SymbolDomainSet();
	/** ��ָ��״̬����״̬ʵ�� */
	public FSMStateInstance(FSMState state) {
		this.state = state;
	}

	/** ������״̬�Ŀ������ڵ� */
	private VexNode sitenode = null;

	/** ���ò�����״̬�Ŀ������ڵ� */
	public void setSiteNode(VexNode sitenode) {
		this.sitenode = sitenode;
	}

	/** ��ò�����״̬�Ŀ������ڵ� */
	public VexNode getVexNode() {
		return sitenode;
	}

	/** �������캯�� */
	public FSMStateInstance(FSMStateInstance instance) {
		this.state = instance.state;
		if(instance.getState().fsm.isPathSensitive()){
			symbolDomainSet=new SymbolDomainSet(instance.symbolDomainSet);
			valueSet=new ValueSet(instance.valueSet);
			vardomainset = getVarDomainSet();
		}
		this.sitenode = instance.sitenode;
	}

	/** ���õ�ǰ״̬ */
	public void setState(FSMState state) {
		this.state = state;
	}

	/** ��õ�ǰ״̬ */
	public FSMState getState() {
		return state;
	}

	/** ���ָ���״̬��ȣ�����Ϊ��� */
	@Override
	public boolean equals(Object o) {
		if ((o == null) || !(o instanceof FSMStateInstance)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		FSMStateInstance x = (FSMStateInstance) o;
		if (state != x.state) {
			return false;
		}
		if(Config.PATH_SENSITIVE==0){
			return true;
		}else{
			//�жϵ�ǰ״̬�����Ƿ���ͬ
			if(vardomainset.equals(x.vardomainset))
				return true;
		}
		
		return false;
	}

	/** ����hash���key����Ҫ��ֵ֤�����hashcode��� */
	@Override
	public int hashCode() {
		return state.hashCode();
	}

	/** ��ӡ */
	@Override
	//zys:���޸�
	public String toString() {
		return state.getName() + " =( " + vardomainset + ")  ";
	}
	
	private void updateVarDomainSet(){
		if(vardomainset==null)
			vardomainset=new VarDomainSet();
		else
			vardomainset.clearDomainSet();

		Hashtable<VariableNameDeclaration,Expression> table=valueSet.getTable();
		for(VariableNameDeclaration v: table.keySet())
		{
			Expression value=table.get(v);
			Domain domain=value.getDomain(symbolDomainSet);
			CType type=v.getType();
			if(domain!=null){
				if(type!=null&&!(type instanceof CType_Pointer)&&!(type instanceof CType_Array)){
					domain=Domain.castToType(domain, type);
				}
				vardomainset.addDomain(v, domain);
			}
		}
	}
	
	public VarDomainSet getVarDomainSet(){
		updateVarDomainSet();
		return vardomainset;
	}
	
	/** ����caselabel�ڵ��switch�ڵ㣬����״̬��״̬���� */
	public void calSwitch(VexNode n, VexNode pre) {
		ValueSet oldValueSet=pre.getValueSet();
		SymbolDomainSet oldSymbolDomainSet=pre.getSymDomainset();

		// ����״̬�������м��㣬����״̬����
		pre.setValueSet(valueSet);
		pre.setSymDomainset(symbolDomainSet);
		
		//��ѯswitch(n)�еı���n
		SimpleNode expnode = (SimpleNode) pre.getTreenode().jjtGetChild(0);
		ASTPrimaryExpression pri=(ASTPrimaryExpression) expnode.getSingleChildofType(ASTPrimaryExpression.class);
		if(pri==null || pri.getVariableNameDeclaration()==null)
			return;
		VariableNameDeclaration v=pri.getVariableNameDeclaration();
		
		if(n.getName().startsWith("label_head_case")){
			caseDomain(pre,n,v);
		}else{
			//�����default��֧����ȡswitch(n)����n������Case��֧�ķ���
			defaultDomain(n,pre,v);
		}
		
		updateVarDomainSet();
		// �ָ�ԭ������
		pre.setValueSet(oldValueSet);
		pre.setSymDomainset(oldSymbolDomainSet);
	}

	private void defaultDomain(VexNode n, VexNode pre, VariableNameDeclaration v) {
		Domain caseDomainSet = null,switchHeadDomain = null;
		ExpressionVistorData exprdata = new ExpressionVistorData();
		exprdata.currentvex=n;
		exprdata.sideeffect=true;
		ExpressionValueVisitor exprvisitor = new ExpressionValueVisitor();
		
		//�ȼ��������case�������䣬��ȡ��
		for (Edge edge : pre.getOutedges().values()) {
			VexNode head = edge.getHeadNode();
			if(head.getName().startsWith("label_head_case_")){
				((SimpleNode) head.getTreenode().jjtGetChild(0)).jjtAccept(exprvisitor, exprdata);
				Expression e = exprdata.value;
				Domain dm = e.getDomain(head.getSymDomainset());
				if(dm != null){
					caseDomainSet = Domain.union(caseDomainSet, dm, v.getType());	
				}  							
			}
		}
		caseDomainSet = Domain.inverse(caseDomainSet);
		
		//Ȼ������switchͷ�����������䣬����������ȡ��
		SymbolFactor s=SymbolFactor.genSymbol(v.getType(),v.getImage());
		Expression exp1=new Expression(s);
		valueSet.addValue(v, exp1);
		switchHeadDomain=pre.getDomain(v);
		if(switchHeadDomain == null){//���switchͷ����������������δ֪
			switchHeadDomain=s.getDomainWithoutNull(n.getSymDomainset());			
			switchHeadDomain = Domain.intersect(switchHeadDomain, caseDomainSet, v.getType());
		}else{
			switchHeadDomain = Domain.intersect(switchHeadDomain, caseDomainSet, v.getType());
		}
		symbolDomainSet.addDomain(s, switchHeadDomain); 
	}

	/** ����ǰ���������жϽڵ���Ƿ�Ϊ���֧��־������״̬��״̬���� */
	public void calCondition(VexNode preVex, boolean istruebranch) {
		// ������
		ValueSet oldValueSet=preVex.getValueSet();
		SymbolDomainSet oldSymbolDomainSet=preVex.getSymDomainset();

		// ����״̬�������м��㣬����״̬����
		preVex.setValueSet(valueSet);
		preVex.setSymDomainset(symbolDomainSet);
		
		SimpleNode treenode = preVex.getTreenode();
		if (treenode == null)
			return;
		if (!preVex.isBackNode()) {
			
			String name = preVex.getName();
			ConditionData condata=new ConditionData(preVex);
			ConditionDomainVisitor convisitor = new ConditionDomainVisitor();
			if (name.startsWith("if_head")) {
				convisitor.visit((ASTExpression) treenode.jjtGetChild(0), condata);
				//preVex.setCondata(condata);
//				condata=preVex.getCondata();
			}else if (name.startsWith("while_head")) {
				DomainVexVisitor.iterationCalculate(preVex, condata, convisitor, treenode.jjtGetChild(0));
			}else if (name.startsWith("for_head")) {
				Node con=null;
				if(treenode.forChild[1]){
					if(treenode.forChild[0]){
						con=treenode.jjtGetChild(1);
					}else{
						con=treenode.jjtGetChild(0);
					}
				}
				if(con!=null)
					DomainVexVisitor.iterationCalculate(preVex, condata, convisitor, con);
			}else if (name.startsWith("do_while_out1")) {
				DomainVexVisitor.iterationCalculate(preVex, condata, convisitor, treenode);
			}
			
			if(condata!=null){
				SymbolDomainSet ds = null;
				if (istruebranch) {
					ds=condata.getTrueMayDomainSet();
					SymbolDomainSet symDomainSet=preVex.getSymDomainset();
					ds=SymbolDomainSet.intersect(symDomainSet, ds);
				} else {
					ds=condata.getFalseMayDomainSet();
					ds=SymbolDomainSet.intersect(preVex.getSymDomainset(), ds);
				}
				symbolDomainSet=ds;
				updateVarDomainSet();
			}
		}
		
		// �ָ�ԭ������
		preVex.setValueSet(oldValueSet);
		preVex.setSymDomainset(oldSymbolDomainSet);
	}

	/**zys:2010.7.30 ·������״̬�����������ԭ��
	 * ���ݵ�ǰ�������ڵ㣬����״̬��״̬���� */
	public void calDomainSet(VexNode vex) {
		// ������
		ValueSet oldValueSet=vex.getValueSet();
		SymbolDomainSet oldSymbolDomainSet=vex.getSymDomainset();

		// ����״̬�������м��㣬����״̬����
		vex.setValueSet(valueSet);
		vex.setSymDomainset(symbolDomainSet);
		
		SimpleNode treenode = vex.getTreenode();
		ExpressionVistorData exprdata = new ExpressionVistorData();
		exprdata.currentvex=vex;
		exprdata.sideeffect=true;
		ExpressionValueVisitor exprvisitor = new ExpressionValueVisitor();
		if (treenode.getFirstVexNode() == vex) {
			// ȷ���Ǹ��﷨���ڵ��Ӧ�ĵ�һ���������ڵ�
			if(vex.getName().startsWith("if_head")){
//				System.out.println("����if_head�ڵ�,λ�ڣ�"+treenode.getBeginFileLine());
				treenode.jjtGetChild(0).jjtAccept(exprvisitor, exprdata);
			}else if (vex.getName().startsWith("switch_head_")
							|| vex.getName().startsWith("label_head_case_")) {
				treenode.jjtGetChild(0).jjtAccept(exprvisitor, exprdata);
			}else if (vex.getName().startsWith("for_head_") || vex.getName().startsWith("do_while_out1_")
					|| vex.getName().startsWith("while_head_")) {
				if(oldValueSet!=null)
					valueSet=new ValueSet(oldValueSet);
				if(oldSymbolDomainSet!=null)
					symbolDomainSet=new SymbolDomainSet(oldSymbolDomainSet);
			}else {
				treenode.jjtAccept(exprvisitor, exprdata);
			}
		}
//		valueSet=oldValueSet;
//		symbolDomainSet=SymbolDomainSet.union(symbolDomainSet,oldSymbolDomainSet);
		// ȥ����Щ����������ı���
		ValueSet temp = new ValueSet();
		Hashtable<VariableNameDeclaration, Expression> table = valueSet.getTable();
		for (VariableNameDeclaration var : table.keySet()) {
			if (vex.getTreenode().getScope().isSelfOrAncestor(var.getScope())) {
				temp.getTable().put(var, table.get(var));
			}
		}
		valueSet=temp;
		
		updateVarDomainSet();
		
		// �ָ�ԭ������
		vex.setValueSet(oldValueSet);
		vex.setSymDomainset(oldSymbolDomainSet);
	}

	public void setVardomainset(VarDomainSet vardomainset) {
		this.vardomainset = vardomainset;
	}

	public ValueSet getValueSet() {
		return valueSet;
	}

	public void setValueSet(ValueSet valueSet) {
		this.valueSet = valueSet;
	}

	public SymbolDomainSet getSymbolDomainSet() {
		return symbolDomainSet;
	}

	public void setSymbolDomainSet(SymbolDomainSet symbolDomainSet) {
		this.symbolDomainSet = symbolDomainSet;
	}

	public void mergeValueSet(ValueSet vs,SymbolDomainSet ds) {
		ValueSet newset=new ValueSet();
		
		Expression value1 = null,value2=null;
		Hashtable<VariableNameDeclaration,Expression> table=valueSet.getTable();
		for(VariableNameDeclaration v: table.keySet())
		{
			value1=table.get(v);
			value2=vs.getValue(v);
			CType type=v.getType();
			
			if(value2==null || value1.equals(value2)){
				newset.addValue(v, value1);
			}else{
				SymbolFactor sym=SymbolFactor.genSymbol(type,v.getImage());
				addSymbolDomain(sym, Domain.union(value1.getDomain(symbolDomainSet), value2.getDomain(ds), type));
				newset.addValue(v, new Expression(sym));
			}
		}
		
		table=vs.getTable();
		for(VariableNameDeclaration v : table.keySet()) {
			value2 =table.get(v);
			value1=valueSet.getValue(v);
									
			if(value1==null){
				newset.addValue(v, value2);
				mergeSymbolDomainSet(ds);
			}			
		}
		
		valueSet=newset;
	}
	
	public void addSymbolDomain(SymbolFactor s,Domain domain){
		if(domain==null){
			return;
		}
		if(symbolDomainSet==null){
			symbolDomainSet=new SymbolDomainSet();
		}
		symbolDomainSet.addDomain(s, domain);
	}
	
	public void mergeSymbolDomainSet(SymbolDomainSet ds){
		symbolDomainSet=SymbolDomainSet.union(symbolDomainSet, ds);
	}
	
	private void caseDomain(VexNode pre, VexNode n, VariableNameDeclaration v)
	{		
		//����case����ֵ
		ASTConstantExpression constantExpression=(ASTConstantExpression)n.getTreenode().jjtGetChild(0);
		ExpressionVistorData exprdata = new ExpressionVistorData();
		exprdata.currentvex=n;
		exprdata.sideeffect=true;
		ExpressionValueVisitor exprvisitor = new ExpressionValueVisitor();
		constantExpression.jjtAccept(exprvisitor, exprdata);
		Expression caseValue=exprdata.value;
		if(caseValue==null)
			return;
		
		//zys:2010.8.4	ȡ��switch(n)�еı�����������ֵ��Ϊcase i:��i��ֵ
		Expression switchHeadValue =pre.getValue(v);
		if(switchHeadValue==null)
			return;
		
		Expression exp =switchHeadValue.sub(caseValue);//switch�б�����case�б�����������ֵ
		if(exp.getSingleFactor() instanceof NumberFactor){
			NumberFactor f=(NumberFactor)exp.getSingleFactor();
			if(f.getDoubleValue()==0){
				valueSet.addValue(v, caseValue);
			}
		}else{
			Domain d=caseValue.getDomain(n.getSymDomainset());
			if(d==null)
				return;
			SymbolFactor s=SymbolFactor.genSymbol(v.getType(), v.getImage());
			Expression expr=new Expression(s);
			valueSet.addValue(v, expr);
			symbolDomainSet.addDomain(s, d);
		}
	}
}