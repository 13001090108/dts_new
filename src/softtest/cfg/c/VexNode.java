package softtest.cfg.c;

import softtest.DefUseAnalysis.c.*;
import softtest.ast.c.*;
import softtest.domain.c.analysis.*;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.fsm.c.*;
import softtest.fsmanalysis.c.Report;
import softtest.symboltable.c.Type.*;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.VariableNameDeclaration;

import java.util.*;



/** ͼ�Ķ����� */
public class VexNode extends Element implements Comparable<VexNode> {
	/**zys:2010.5.17	����ѭ��ʱѭ��ͷ�ڵ���while_head,for_head,do_while_out1��������
	 * widening��narrowing����*/
	IterationCalculation iterationCal=null;
	
	/** �ܹ����ﵱǰ�ڵ�ı���������� */
	LiveDefsSet liveDefs = new LiveDefsSet();
	
	/** ���� */
	String name;

	/** ��߼��� */
	Hashtable<String, Edge> inedges = new Hashtable<String, Edge>();

	/** ���߼��� */
	Hashtable<String, Edge> outedges = new Hashtable<String, Edge>();
	
	/** �ڵ��Ӧ�ĳ����﷨���ڵ� */
	SimpleNode treenode = null;

	/** ���֧��־ */
	public boolean truetag = false;

	/** �ٷ�֧��־ */
	public boolean falsetag = false;

	/** ���ʱ�־ */
	boolean visited = false;

	/** ���ڱȽϵ����� */
	int snumber = 0;

	/** �ýڵ��Ƿ�ì�ܱ�־�����ڿ�����ͼ�еĲ��ɴ�·�� */
	boolean contradict = false;
	
	/** �Ƿ���ѭ��ͷ�ڵ��־*/
	boolean loopHead = false;
	/** ��ѭ��ͷ�ڵ㣬ѭ���е�ѭ�����Ƿ�������ִ��һ��*/
	boolean loopExecuteAtleastOnce=false;
	
	private Graph graph=null;
	
	transient ValueSet valueset=new ValueSet();
	
	transient SymbolDomainSet symboldomainset=new SymbolDomainSet();
	
	/**zys:2010.5.10 �ڹ���ģʽ����ʱ����ȡ��һ�ڵ�ķ���ȡֵ��Ϣ */
	transient ValueSet lastvalueset;
	transient SymbolDomainSet lastsymboldomainset;
	
	transient VarDomainSet varDomainSet;
	/** �ڵ��ϵı������ּ��� */
	ArrayList<NameOccurrence> occs=new ArrayList<NameOccurrence>();

	//zys
	/** ״̬��ʵ������ */
	transient FSMMachineInstanceSet fsminstanceset = new FSMMachineInstanceSet();
	
	/** �����޶��� */
	transient ConditionData condata = null;

	/**zys:����ǹ���ģʽ���㣬��ͨ��lastvalueset���� */
	private boolean fsmCompute=false;

	public boolean isFsmCompute() {
		return fsmCompute;
	}

	public void setfsmCompute(boolean fsmCompute){
		this.fsmCompute=fsmCompute;
	}
	
	/** ���״̬��ʵ������ */
	public FSMMachineInstanceSet getFSMMachineInstanceSet() {
		return fsminstanceset;
	}

	/** ����״̬��ʵ������ */
	public void setFSMMachineInstanceSet(FSMMachineInstanceSet fsminstanceset) {
		this.fsminstanceset = fsminstanceset;
	}
	
	/** ɾ����Щ�յ�״̬��	 */
	public void rmFSMMachineInstanceSet(FSMMachineInstance fsminstance) {
		this.fsminstanceset.getTable().remove(fsminstance);
	}
	
	/** ��״̬��ʵ������set�ϲ���fsminstanceset�� */
	public void mergeFSMMachineInstances(FSMMachineInstanceSet set) {
		fsminstanceset.mergeFSMMachineInstances(set);
	}
	
	public void mergFSMMachineInstancesWithoutConditon(FSMMachineInstanceSet set){
		fsminstanceset.mergFSMMachineInstancesWithoutConditon(set);
	}
	
	/**�����������*/
	public void setSnumber(int snumber){
		this.snumber=snumber;
	}
	
	/**����������*/
	public int getSnumber(){
		return this.snumber;
	}

	/** ��ָ�������ֺ��﷨���ڵ㴴��������ͼ�ڵ� */
	public VexNode(String name, SimpleNode treenode) {
		this.name = name;
		this.treenode = treenode;
		treenode.addVexNode(this);
	}

	/** ������ͼ�����ߵ�accept */
	@Override
	public void accept(GraphVisitor visitor, Object data) {
		visitor.visit(this, data);
	}
	
	/** ���ýڵ���ʱ�־ */
	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	/** ��ýڵ���ʱ�־ */
	public boolean getVisited() {
		return visited;
	}

	/** ��ýڵ��Ƿ�ì�ܱ�־ */
	public boolean getContradict() {
		return contradict;
	}

	/** ���ýڵ��Ƿ�ì�ܱ�־ */
	public void setContradict(boolean contradict) {
		this.contradict = contradict;
	}
	
	public boolean isLoopHead(){
		return loopHead;
	}
	
	public void setLoopHead(boolean loopHead){
		this.loopHead=loopHead;
	}
	public boolean getLoopExecuteAtleastOnce(){
		
		return loopExecuteAtleastOnce;
	}
	
	public void setLoopExecuteAtleastOnce(boolean loopExecuteAtleastOnce){
		this.loopExecuteAtleastOnce=loopExecuteAtleastOnce;
	}

	/** ���ýڵ�������﷨���ڵ� */
	public void setTreenode(SimpleNode treenode) {
		this.treenode = treenode;
	}

	/** ��ýڵ�������﷨���ڵ� */
	public SimpleNode getTreenode() {
		return treenode;
	}

	/** ��ýڵ����� */
	public String getName() {
		return name;
	}

	/** �����߼��� */
	public Hashtable<String, Edge> getInedges() {
		return inedges;
	}

	/** ��ó��߼��� */
	public Hashtable<String, Edge> getOutedges() {
		return outedges;
	}

	/** �Ƚ������˳���������� */
	public int compareTo(VexNode e) {
		if (snumber == e.snumber) {
			return 0;
		} else if (snumber > e.snumber) {
			return 1;
		} else {
			return -1;
		}
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(""+this.snumber+":");
		sb.append(name);
		return sb.toString();
	}
	
	/** �жϽڵ��ǲ�������ĩβ�ڵ㣬��if��out */
	public boolean isBackNode(){
		if(name.startsWith("if_out")||name.startsWith("while_out")
		||name.startsWith("for_out")||name.startsWith("switch_out")
		||name.startsWith("do_while_out2")||name.startsWith("func_out")){
			return true;
		}
		return false;
	}	
	
	/**
	 * ���ҵ�ǰ�ڵ㵽��һ���ڵ�֮��ı�
	 * @param head ָ������һ���ڵ�
	 * @return ��ǰ�ڵ㵽��һ���ڵ��������һ���ߣ��򷵻ظı䣬���򷵻�null
	 */
	public Edge getEdgeByHead(VexNode head){
		for(Edge e:outedges.values()){
			if(e.headnode==head){
				return e;
			}
		}
		return null;
	}

	public Graph getGraph() {
		return graph;
	}

	public void setGraph(Graph g) {
		this.graph = g;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/** ��ýڵ��ϵı������ּ��� */
	public ArrayList<NameOccurrence> getOccurrences(){
		return occs;
	}
	
	//add by zhouhb
	/** ��ýڵ���ĳ�������ĳ���*/
	public NameOccurrence getVariableNameOccurrence(VariableNameDeclaration decl) {
		for(NameOccurrence occ : occs) {
			if(occ.getDeclaration().equals(decl))
				return occ;
		}
		return null;
	}
	
	/** ��ýڵ㵽�ﶨ�弯�ϣ�������ܹ����ﵱǰ�ڵ�ı���������ֵļ��� */
	public LiveDefsSet getLiveDefs(){
		return liveDefs;
	}
	
	public ValueSet getValueSet() {
		return valueset;
	}

	public void setValueSet(ValueSet abstractvalues) {
		this.valueset = abstractvalues;
	}
	
	/** �޸ı���v�ĳ���ȡֵ */
	public Expression addValue(VariableNameDeclaration v, Expression value) {
		if (valueset == null) {
			valueset = new ValueSet();
		}
		return valueset.addValue(v, value);
	}

	/** ���ر���v�ĳ���ȡֵ���������v�����ڽڵ���������򷵻�null */
	public Expression getValue(VariableNameDeclaration v) {
		//�����ģʽ����׶Σ�
		if(fsmCompute){
			if (lastvalueset == null||v==null) {
				return null;
			} else {
				Expression e=lastvalueset.getValue(v);
				if(e!=null)
					return e;
			}
		}
		if (valueset == null||v==null) {
			return null;
		} else {
			return valueset.getValue(v);
		}
	}
	
	public Domain getDomain(VariableNameDeclaration v) {
		Domain ret=null;
		Expression e=getValue(v);
		if(e!=null){
			if(fsmCompute){
				ret= e.getDomain(lastsymboldomainset);
			}else{
				ret= e.getDomain(symboldomainset);
			}
			CType type=v.getType();
			if(type!=null){
				ret=Domain.castToType(ret, type);
			}
		}
		return ret;
	}	
	
	
	public ConditionData getCondata() {
		return condata;
	}

	public void setCondata(ConditionData condata) {
		this.condata = condata;
	}
/**
	public void clear() {
		inedges.clear();
		outedges.clear();
		if(fsminstanceset!=null){
			//System.err.println(name+"�ڵ�״̬��ʵ������δ��գ�");
			fsminstanceset.clear();
			//fsminstanceset=null;
		}
		liveDefs.clear();
		occs.clear();
		//treenode = null;	
		valueset.clearValueSet();
		if(symboldomainset!=null)
			symboldomainset.clearDomainSet();
		if(condata!=null)
			condata.clearDomains();
		if(iterationCal!=null){
			iterationCal.clear();
		}
		if(lastvalueset!=null)
			lastvalueset.clearValueSet();
		if(lastsymboldomainset!=null)
			lastsymboldomainset.clearDomainSet();
		if(varDomainSet!=null)
			varDomainSet.clearDomainSet();
	}
*/	
	//modify by JJL,2016-1-25
	public void clear() {
		if(fsminstanceset!=null){
			fsminstanceset.clear();
		}
	}
	
	public SymbolDomainSet getSymDomainset() {
		return symboldomainset;
	}

	public void setSymDomainset(SymbolDomainSet oldset) {
		this.symboldomainset = oldset;
	}
	
	public Domain getSymbolDomain(SymbolFactor s){
		if(symboldomainset==null){
			return null;
		}
		return symboldomainset.getDomain(s);
	}
	
	public void addSymbolDomain(SymbolFactor s,Domain domain){
		if(domain==null){
			return;
		}
		if(symboldomainset==null){
			symboldomainset=new SymbolDomainSet();
		}
		symboldomainset.addDomain(s, domain);
	}
	
	public void mergeSymbolDomainSet(SymbolDomainSet ds){
		symboldomainset=SymbolDomainSet.union(symboldomainset, ds);
	}
	
	public void mergeValueSet(ValueSet vs,SymbolDomainSet ds){
		//zys:2010.9.16	ѭ��ͷ�ڵ������ϲ���Ҫ���⴦�����ѭ��������к�������δ��ʼ�������ر�����Ӧ�������������
		//�õ�ĳ�м������ϲ�ʱ�ᶪʧ��ߵ����䣨��ʱ��ߵ�����Ӧ���Ǹ������������͵�ȫ���������´���
		if (name.startsWith("while_head") || name.startsWith("for_head") || name.startsWith("do_while_out1")
				|| name.startsWith("if_out") || name.startsWith("switch_out")) 
		{
			//2����ȡѭ����߽ڵ�֮ǰ�Ѿ�������û�г�ʼ���ı��������������ʼ��Ϊȫ����
			Set<VariableNameDeclaration> localVarList=getTreenode().getScope().getParent().getVariableDeclarations().keySet();
			for(VariableNameDeclaration localVar : localVarList)
			{
				CType type=localVar.getType();
				//��ֹ����ָ�����͵Ĳ�����ʼ��������NPD�ȵ�ģʽ��
				if(type==null || type instanceof CType_AbstPointer || type instanceof CType_Union 
						|| type instanceof CType_Struct || type instanceof CType_Enum)
					continue;
				
				//��ֹ��ѭ��֮�������ı������������ʼ��
				int location=localVar.getNode().getBeginFileLine();
				int thisLocation=treenode.getBeginFileLine();
				if(location>thisLocation)
					continue;
				Expression expr=vs.getValue(localVar);
				if(expr==null){
					SymbolFactor sym=SymbolFactor.genSymbol(type, localVar.getImage());
					Domain d=Domain.getFullDomainFromType(type);
					expr=new Expression(sym);
					ds.addDomain(sym, d);
					vs.addValue(localVar, expr);
				}
			}
		}
		
		ValueSet newset=new ValueSet();
		
		Expression value1 = null,value2=null;
		Hashtable<VariableNameDeclaration,Expression> table=valueset.getTable();
		for(VariableNameDeclaration v: table.keySet())
		{
			value1=table.get(v);
			value2=vs.getValue(v);
			CType type=v.getType();
			
			if(value2==null || value1.equals(value2)){
				newset.addValue(v, value1);
			}else{
				SymbolFactor sym=SymbolFactor.genSymbol(type,v.getImage());
				addSymbolDomain(sym, Domain.union(value1.getDomain(symboldomainset), value2.getDomain(ds), type));
				newset.addValue(v, new Expression(sym));
			}
		}
		
		table=vs.getTable();
		for(VariableNameDeclaration v : table.keySet()) {
			value2 =table.get(v);
			value1=valueset.getValue(v);
									
			if(value1==null){
				newset.addValue(v, value2);
				mergeSymbolDomainSet(ds);
			}			
		}
		
		valueset=newset;
		//�Ƴ���Щ������ǰ�������ڵ�������ı���
		valueset.removeOutScopeVar(this);
	}
	
	public VarDomainSet getVarDomainSet(){
		if(varDomainSet==null)
			varDomainSet=new VarDomainSet();
		else
			varDomainSet.clearDomainSet();

		Hashtable<VariableNameDeclaration,Expression> table=valueset.getTable();
		for(VariableNameDeclaration v: table.keySet())
		{
			Expression value=table.get(v);
			Domain domain=value.getDomain(symboldomainset);
			CType type=v.getType();
			if(domain!=null){
				if(type!=null&&!(type instanceof CType_Pointer)&&!(type instanceof CType_Array)){
					domain=Domain.castToType(domain, type);
				}
				varDomainSet.addDomain(v, domain);
			}
		}
		return varDomainSet;
	}
	
	public void setVarDomainSet(VarDomainSet varDomainSet) {
		this.varDomainSet = varDomainSet;
	}
	
	public void removeUnusedSymbols(){
		if(symboldomainset==null){
			return;
		}
		HashSet<SymbolFactor> syms=valueset.getAllSymbol();
		ArrayList<SymbolFactor> toremove=new ArrayList<SymbolFactor>();
		for(SymbolFactor s : symboldomainset.getTable().keySet())
		{
			if(!syms.contains(s)){
				toremove.add(s);
			}
		}
		for(SymbolFactor sym:toremove){
			symboldomainset.getTable().remove(sym);
		}
	}

	public ValueSet getLastvalueset() {
		return lastvalueset;
	}

	public void setLastvalueset(ValueSet lastvalueset) {
		this.lastvalueset = lastvalueset;
	}

	public SymbolDomainSet getLastsymboldomainset() {
		return lastsymboldomainset;
	}

	public void setLastsymboldomainset(SymbolDomainSet lastsymboldomainset) {
		this.lastsymboldomainset = lastsymboldomainset;
	}

	public IterationCalculation getIterationCal() {
		return iterationCal;
	}

	public void setIterationCal(IterationCalculation iterationCal) {
		this.iterationCal = iterationCal;
	}
}
