package softtest.symboltable.c;

import java.io.Serializable;
import java.util.*;

import softtest.ast.c.*;
import softtest.callgraph.c.*;
import softtest.cfg.c.VexNode;
import softtest.rules.gcc.fault.OOB_CheckStateMachine;
import softtest.symboltable.c.Type.*;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.ClassScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.MethodScope;

public abstract class AbstractScope implements Scope,Serializable {

	private Scope parent;

	protected SimpleNode node;

	protected String name;
	
	public int varIndex=0;
	
	public int getVarIndex() {
		return varIndex++;
	}
	
	public SimpleNode getNode() {
		return node;
	}
	
	public void setNode(SimpleNode node) {
		this.node = node;
	}

	public String getName(){
		return name;
	}

	public Map<VariableNameDeclaration, ArrayList<NameOccurrence>> getVariableDeclarations(){
		return null;
	}
	
	public Map<MethodNameDeclaration, ArrayList<NameOccurrence>> getMethodDeclarations(){
		return null;
	}
	
	public Map<ClassNameDeclaration, ArrayList<NameOccurrence>> getClassDeclarations(){
		return null;
	}

	public MethodScope getEnclosingMethodScope() {
		if(parent==null){
			return null;
		}
		return parent.getEnclosingMethodScope();
	}

	public ClassScope getEnclosingClassScope() {
		if(parent==null){
			return null;
		}
		return parent.getEnclosingClassScope();
	}

	public SourceFileScope getEnclosingSourceFileScope() {
		if(parent==null){
			return null;
		}
		return parent.getEnclosingSourceFileScope();
	}

	public void setParent(Scope parent) {
		this.parent = parent;
		parent.getChildrens().add(this);
	}

	public Scope getParent() {
		return parent;
	}

	public void addDeclaration(MethodNameDeclaration methodDecl) {
		if (parent != null) {
			parent.addDeclaration(methodDecl);
		}
	}
	
	public void addDeclaration(VariableNameDeclaration variableDecl){
		if (parent != null) {
			parent.addDeclaration(variableDecl);
		}
	}

	public void addDeclaration(ClassNameDeclaration classDecl) {
		if (parent != null) {
			parent.addDeclaration(classDecl);
		}
	}

	protected String glomNames(Iterator i) {
		StringBuffer result = new StringBuffer();
		while (i.hasNext()) {
			result.append(i.next().toString());
			result.append(',');
		}
		return result.length() == 0 ? "" : result.toString().substring(0,
				result.length() - 1);
	}

	/** ���������� */
	private List<Scope> childrens = new ArrayList<Scope>();

	/** ��ú��������� */
	public List<Scope> getChildrens() {
		return childrens;
	}

	public boolean isSelfOrAncestor(Scope ancestor) {
		Scope parent = this;
		while (parent != null) {
			if (parent == ancestor) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}
	
	
	protected HashMap<String,CType> types = new HashMap<String,CType>();
	
	public CType getType(String name) {
		if(types.containsKey(name)){
			return types.get(name);
		}else if(parent!=null){
			return parent.getType(name);
		}
		return null;
	}
	
	public void addType(String name, CType type) {
		types.put(name, type);
		type.calClassSize(this);
	}
	
	public static int depth=0;
	
	public String dump(){
		return "";
	}
	
	public String print(){
		StringBuffer b=new StringBuffer();
		for(int i=0;i<depth;i++){
			b.append("  ");
		}
		b.append(dump()+"\n");
		depth++;
		
		List childrens = this.getChildrens();
		Iterator i = childrens.iterator();
		while (i.hasNext()) {
			AbstractScope s = (AbstractScope) i.next();
			b.append(s.print());
		}
		depth--;
		return b.toString();
	}
	
	/**
	 * @author zys 2010.01.13
	 * <p>Usage:
	 * ΪTestCaseGeneratorForScope�����ض���ʽ�Ĳ�������
	 * </p>
	 * @return TestCaseGeneratorForScope�е��Generate��ťʱ���ɵ�Scope��������
	 */
	public String printTestCaseGeneratorForScope(){
		StringBuffer sb=new StringBuffer();
		String b=print();
		String temp[]=b.split("\n");
		int len=temp.length;
		
		//��print()���ɵ��ַ��������Ŀ�ʼ��һ��˫���ţ���������
		sb.append(temp[0]);
		sb.append("\"");
		sb.append("+\"\\n\"+\n");
		
		for(int i=1;i<len-1;i++)
		{
			sb.append("\"");
			sb.append(temp[i]);
			sb.append("\"");
			sb.append("+\"\\n\"+\n");
		}
		
		//��print()���ɵ��ַ���������ĩβ��һ��˫���ţ���������
		sb.append("\"");
		sb.append(temp[len-1]);
		sb.append("\"");
		sb.append("+\"\\n\"");
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	/**��ʼ���������ֵĶ���ʹ������*/
	public void initDefUse(){
		Map<VariableNameDeclaration, ArrayList<NameOccurrence>> variableNames = null;
		variableNames = getVariableDeclarations();
		if (variableNames != null) {//�������쳣����ñ�����
			Iterator i = variableNames.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry e=(Map.Entry)i.next();
				//VariableNameDeclaration v = (VariableNameDeclaration) e.getKey();
				List occs = (List) e.getValue();
				for (Object o : occs) {
					NameOccurrence occ = (NameOccurrence) o;
					occ.setOccurrenceType(occ.checkOccurrenceType());
					//add test by lsc 2018/9/21
//					System.out.println(occ.getOccurrenceType()+":"+occ.toString());
					
					VexNode vex=occ.getLocation().getCurrentVexNode();
					if(vex!=null){
						vex.getOccurrences().add(occ);
					}
				}
			}
		}
		
		//	�ݹ鴦����������
		List childrens = this.getChildrens();
		Iterator i = childrens.iterator();
		while (i.hasNext()) {
			AbstractScope s = (AbstractScope) i.next();
			s.initDefUse();
		}
	}
	
	/** ���������ù�ϵ */
	public void resolveCallRelation(CGraph g) {
		Map<MethodNameDeclaration, ArrayList<NameOccurrence>> methodNames = null;
		methodNames = getMethodDeclarations();
		if (methodNames != null) {//�������쳣����ú���
			for(MethodNameDeclaration callee:methodNames.keySet()){
				//zys:����Ǻ��������ڵ㣬�����ɺ������ý��;
				//�����extern�ⲿ�������ã�����ȫ�ֺ������ù�ϵ��������2010.4.13
				if(callee != null){
					if(!(callee.getNode() instanceof ASTNestedFunctionDefinition) && !(callee.getNode() instanceof ASTNestedFunctionDeclaration)){
						if(callee.isLib() || callee.getMethodNameDeclaratorNode()==null)
							continue;
					}			
				}
				
				CVexNode ncallee = callee.getCallGraphVex();
				if(ncallee == null) {
					//�����ڵ�
					String str = null;
					str = callee.getImage() + "_" + callee.getParameterCount() + "_";
					ncallee = g.addVex(str, callee);
				}
				List<NameOccurrence> occs = methodNames.get(callee);
				for (Object o : occs) {
					NameOccurrence occ = (NameOccurrence) o;
					MethodScope mscope = occ.getLocation().getScope().getEnclosingMethodScope();
					if(!(occ.getLocation() instanceof ASTPrimaryExpression)){
						continue;
					}
					
					if (mscope != null && !(mscope.getAstTreeNode() instanceof ASTNestedFunctionDefinition)) {//TODO
						ASTFunctionDefinition method = (ASTFunctionDefinition) mscope.getAstTreeNode();
						MethodNameDeclaration caller = method.getDecl();
						CVexNode ncaller = caller.getCallGraphVex();
						if (ncaller == null) {
							//�����ڵ�
							String str = caller.getImage() + "_" + caller.getParameterCount() + "_";
							ncaller = g.addVex(str, caller);
						}

						if (!ncallee.isPreNode(ncaller)) {
							//��ӵ��ù�ϵ
							g.addEdge(ncaller, ncallee);
						}
					}
				}
			}
		}

		// �ݹ鴦����������
		for(Scope s : this.getChildrens()){
			s.resolveCallRelation(g);
		}
	}
	public void updateDeclaration(VariableNameDeclaration varDecl) {
	}
}
