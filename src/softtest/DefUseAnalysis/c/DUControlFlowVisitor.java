package softtest.DefUseAnalysis.c;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTIterationStatement;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.GraphVisitor;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.symboltable.c.ClassScope;
import softtest.symboltable.c.LocalScope;
import softtest.symboltable.c.MethodScope;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.NameOccurrence.DefinitionType;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;

public class DUControlFlowVisitor implements GraphVisitor
{
	private HashSet<NameOccurrence> deflist = new HashSet<NameOccurrence>();
	private int usepoints=0,defpoints=0;
	private int func=0,entrance=0,global=0;//�����ڣ�������ȫ��
	private int simple=0,complex=0;//�򵥱��������ӱ���
	private int assign=0,condition=0,loop=0,function=0,param=0,other=0;
	private int[] def = new int[4];
	/*
	public void visit(VexNode n, Object data)
	{
		try {
			FileWriter fw = new FileWriter("E:\\��������\\du_statistics.txt", true);
			if(n.getName().startsWith("func_head")) {
				ASTFunctionDefinition astnode =(ASTFunctionDefinition)n.getTreenode();
				String funcname = astnode.getImage();
				//fw.append("File name = "+n.getTreenode().getFileName()+", Function name = "+funcname+"\n");
			}
			calculateIN(n,data);
			calculateOUT(n,data);	
			calculateEntrance(n,data);
			count(n);
			
			if(n.getName().startsWith("func_out")) {
//				fw.append("DUControlFlowVisitor [assign=" + assign + ", condition=" + condition + ", loop=" + loop + ", function=" + function
//						+ ", param=" + param + ", other=" + other +"]\n");
				fw.append(usepoints+","+simple+","+complex+"\n");
				
				DUStatistics.usepoints+=usepoints;
				DUStatistics.defpoints+=defpoints;
				DUStatistics.func+=func;
				DUStatistics.entrance+=entrance;
				DUStatistics.global+=global;
				DUStatistics.simple+=simple;
				DUStatistics.complex+=complex;
				DUStatistics.assign+=assign;
				DUStatistics.condition+=condition;
				DUStatistics.loop+=loop;
				DUStatistics.function+=function;
				DUStatistics.param+=param;
				DUStatistics.other+=other;
				DUStatistics.def[0]+=def[0];
				DUStatistics.def[1]+=def[1];
				DUStatistics.def[2]+=def[2];
				DUStatistics.def[3]+=def[3];
			}

			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	*/
	public void visit(VexNode n, Object data)
	{
		//regenerateNameOccurrence(n);
		calculateIN(n,data);
		calculateOUT(n,data);	
		calculateEntrance(n,data);
		//count(n);		                       //д����                       
	}
	//����ָ�롢���顢�ṹ��ķ��ų��֣���ʱ�������ƫ���������
	private void regenerateNameOccurrence(VexNode n) {
		System.out.println("������ͼ�ڵ�ţ�"+n+"���ų��֣�"+n.getOccurrences());
		
	}

	public void visit(Edge e, Object data)
	{
		
	}

	public void visit(Graph g, Object data)
	{
		
	}

	
	//change by lsc 2018/9/13
	
	/** ����In ��Դ���ִ��� */
	public void calculateIN(VexNode n, Object data) {
		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e.hasMoreElements();) {
			list.add(e.nextElement());
		}
		Collections.sort(list);				//���Ե��ȥ�鿴������򣬽��бߵ�����

		// ����ǰ���ڵ��U(in)
		VexNode pre = null;
		Iterator<Edge> iter = list.iterator();					//׼��������߼���
		while (iter.hasNext()) {
			Edge edge = iter.next();							//��ʼ����ȡ�����
			pre = edge.getTailNode();							//��ñߵ�β�ڵ�(����ǰ�ڵ�n��ǰ���ڵ�,ע����������ͷ��β�ĸ���)
			//added by cmershen,2016.3.14
			if(pre.getName().startsWith("func_head")) {
				for(NameOccurrence occ : pre.getOccurrences())			//ѭ������ǰ���ڵ��ϳ��ֵı�������
					if(occ.getOccurrenceType() == OccurrenceType.ENTRANCE) {	//���˱�����������ENTRANCE,���Ǻ�����������
						n.getLiveDefs().addLiveDef(occ);			//���ӱ������ﶨ�����
						//defpoints++;
						classify(occ);								//�ж�һ�������Ķ�ֵ��ʽ������¼
					}
			}
			if (edge.getName().startsWith("F")) {
				//����һ��ѭ��
				if (n.getName().startsWith("while_out") || n.getName().startsWith("for_out")) {
					visit(pre, data);
				}
			}
			
			//��Ͽ��������󲢼�
			n.getLiveDefs().mergeLiveDefs(pre.getLiveDefs());
		}
	}

	/** ����Out�������Ӱ����Ĵ��� */
	public void calculateOUT(VexNode n, Object data) {
		for(NameOccurrence occ:n.getOccurrences()){
			if(!(occ.getDeclaration() instanceof VariableNameDeclaration)){
				continue;
			}
			LiveDefsSet  livedefs=n.getLiveDefs();
			VariableNameDeclaration v=(VariableNameDeclaration)occ.getDeclaration();
			if(occ.getOccurrenceType()==NameOccurrence.OccurrenceType.USE || occ.getOccurrenceType() == OccurrenceType.DEF_AFTER_USE){
				//usepoints++;
				
				for(NameOccurrence o: livedefs.getVariableLiveDefs(v)){
					o.addDefUse(occ);
					occ.addUseDef(o);
					//defpoints++;
					classify(o);
				}
//				List<NameOccurrence> newOcc = new ArrayList<NameOccurrence>(occ.getUse_def());
//				for(NameOccurrence o: newOcc) {
//					if(o.toString().equals(occ.toString())) {
//						List<NameOccurrence> usedef = new ArrayList<NameOccurrence>(occ.getUse_def());
//						occ.getUse_def().remove(o);
//						for(NameOccurrence o1 : usedef) {
//							for(NameOccurrence o11 : o1.getUndef_def())
//								occ.addUseDef(o11);
//						}
//					}
//				}
			}
			//zys:·����֧������ɣ���ʱ������
			if (occ.getOccurrenceType() == NameOccurrence.OccurrenceType.DEF || occ.getOccurrenceType() == OccurrenceType.DEF_AFTER_USE)  {
				livedefs.setNewDef(occ);
				classify(occ);
			}
			
		}
		//System.out.println(n+","+n.getOccurrences());
		if(Config.TRACE){
			System.out.println(n.getName()+"    "+n.getLiveDefs());
		}
	}

	/**added by cmershen,2016.3.14,�ж���֮�󣬶Զ���ʹ������������������Ƿ�����ڲ���*/
	private void calculateEntrance(VexNode n, Object data) {
		List<NameOccurrence> occList = n.getOccurrences(); 
		for(NameOccurrence occ : occList) {
			if(occ.getOccurrenceType() == OccurrenceType.USE) { 
				if(occ.getUse_def()==null || occ.getUse_def().isEmpty()) {
					VexNode head = n.getGraph().getEntryNode();			//��ȡ��ڽڵ�
					for(NameOccurrence occ2 : head.getOccurrences()) {
						if(occ2.getImage().equals(occ.getImage()) && occ2.getOccurrenceType() == OccurrenceType.ENTRANCE) {
							if(occ2.getDef_use() == null) {
								occ2.setDef_use(new LinkedList<NameOccurrence>());
							}
							occ2.addDefUse(occ);
							occ.addUseDef(occ2);
							if(deflist.add(occ2)) {
								defpoints++;
								classify(occ2);
								//System.out.println("def point: "+occ2);
							}
						}
					}
				}   
			}
			//add by cmershen,2016.10.10
			//����*p=1;�����������p�Ǵ�����������������Ƕ����
			else if(occ.getOccurrenceType() == OccurrenceType.DEF) {
				if(occ.getUndef_def()==null || occ.getUndef_def().isEmpty()) {
					VexNode head = n.getGraph().getEntryNode();
					for(NameOccurrence occ2 : head.getOccurrences()) {
						if(occ2.getImage().equals(occ.getImage()) && occ2.getOccurrenceType() == OccurrenceType.ENTRANCE) {
							occ.addUndefDef(occ2);
						}
					}
				}
			}
		}
	}
	
	/**2018/9/13lsc,Ӧ����д����*/
	private void count(VexNode n) {								
		List<NameOccurrence> occList = n.getOccurrences();
		for(NameOccurrence occ : occList) {
			if(occ.getOccurrenceType()==OccurrenceType.DEF) {
				if(deflist.add(occ)) {
					defpoints++;
					classify(occ);
					System.out.println("def point: "+occ);
				}
			}
			else if(occ.getOccurrenceType()==OccurrenceType.USE) {
				usepoints++;
				int size=occ.getUse_def().size();
				if(size>3)
					size=3;
				def[size]++;
				//System.out.println("use point: "+occ);
				VariableNameDeclaration decl = (VariableNameDeclaration)occ.getDeclaration();
				if(decl.isArray()) {
					complex++;
				}
				else if(!decl.mems.isEmpty() && decl.mems.size()>0) {
					complex++;
				}
				else
					simple++;
				if(decl.getScope() instanceof LocalScope || decl.getScope() instanceof ClassScope)
					func++;
				else if(decl.getScope() instanceof SourceFileScope ) {
					global++;
					if(size==0) {
						def[0]--;
						def[1]++;
					}
				}
				else if(decl.getScope() instanceof MethodScope ){
					entrance++;
				}
			}
			else if(occ.getOccurrenceType()==OccurrenceType.DEF_AFTER_USE) {
				//����ѭ����䣬sry 2016.3.30
				boolean flag = true;
				for(NameOccurrence occ2:deflist) {
					if(occ2.getLocation().getFirstParentOfType(ASTIterationStatement.class) == occ.getLocation().getFirstParentOfType(ASTIterationStatement.class))
						flag=false;
				}
				if(flag && !deflist.add(occ)) {
					defpoints++;
					System.out.println("2def point: "+occ);
				}
				usepoints++;
				int size=occ.getUse_def().size();
				if(size>3)
					size=3;
				def[size]++;
				VariableNameDeclaration decl = (VariableNameDeclaration)occ.getDeclaration();
				if(decl.isArray()) {
					complex++;
				}
				else if(!decl.mems.isEmpty() && decl.mems.size()>0) {
					complex++;
				}
				else
					simple++;
				if(decl.getScope() instanceof LocalScope || decl.getScope() instanceof ClassScope)
					func++;
				else if(decl.getScope() instanceof SourceFileScope ) {
					global++;
					if(size==0) {
						def[0]--;
						def[1]++;
					}
				}
				else if(decl.getScope() instanceof MethodScope ){
					entrance++;
					if(size==0) {
						def[0]--;
						def[1]++;
					}
				}
			}
			
		}
	}
	/**add by cmershen,2016.4.6 �ж�һ�������Ķ�ֵ��ʽ*/
	private void classify(NameOccurrence def) {
		if(def.isOnLeftHandSide() || def.isSelfAssignment()) {
			SimpleNode tempNode = def.getLocation();
//<<<<<<< DUControlFlowVisitor.java
			while (tempNode!=null && !(tempNode instanceof ASTStatement)) { //�����п����׳��쳣�����������ԡ�modified by cmershen,2016.12.5
				
				tempNode=(SimpleNode) tempNode.jjtGetParent();
//=======
//			while (!(tempNode instanceof ASTStatement)) {
//				tempNode=(SimpleNode) tempNode.jjtGetParent();//null-uucp
//>>>>>>> 1.3.44.6.2.1
			}	
			boolean isAssign = false;
			boolean isIteration = false;
			boolean isCondition = false;
			if(tempNode==null)
				return;
			if(tempNode.containsChildOfType(ASTAssignmentExpression.class))
				isAssign = true;
			if(tempNode.containsChildOfType(ASTIterationStatement.class)) {			
				isAssign = false;
				isIteration = true;
			}
			else if(tempNode.containsParentOfType(ASTSelectionStatement.class)) {
				isAssign = false;
				isCondition = true;
			}
			if(isAssign) { //���ڶ��Ʒ���1-��ֵ���
				//System.out.println(def+",��ֵ����1-��ֵ���");
				def.definitionType=DefinitionType.ASSIGN;
				assign++;
			}
			else if(isIteration) {
				//System.out.println(def+",��ֵ����3-ѭ�����");
				def.definitionType=DefinitionType.LOOP;
				loop++;
			}
			else if(isCondition) {
				//System.out.println(def+",��ֵ����2-�������");
				def.definitionType=DefinitionType.CONDITION;
				condition++;
			}
		}
		else {
			if(!def.methodName.isEmpty()) {
				//System.out.println(def+",��ֵ����4-�⺯��");
				def.definitionType=DefinitionType.LIB;
				function++;
			}
			else if(def.getOccurrenceType() == OccurrenceType.ENTRANCE) {
				//System.out.println(def+",��ֵ����5-��������");
				def.definitionType=DefinitionType.PARAMETER;
				param++;
			}
			else {
				//System.out.println(def+",�޷�����");
				other++;
			}
			
		}
	}
	public HashSet<NameOccurrence> getDeflist() {
		return deflist;
	}

	public void setDeflist(HashSet<NameOccurrence> deflist) {
		this.deflist = deflist;
	}

	public int getUsepoints() {
		return usepoints;
	}

	public void setUsepoints(int usepoints) {
		this.usepoints = usepoints;
	}

	public int getDefpoints() {
		return defpoints;
	}

	public void setDefpoints(int defpoints) {
		this.defpoints = defpoints;
	}

	public int getFunc() {
		return func;
	}

	public void setFunc(int func) {
		this.func = func;
	}

	public int getEntrance() {
		return entrance;
	}

	public void setEntrance(int entrance) {
		this.entrance = entrance;
	}

	public int getGlobal() {
		return global;
	}

	public void setGlobal(int global) {
		this.global = global;
	}

	public int getSimple() {
		return simple;
	}

	public void setSimple(int simple) {
		this.simple = simple;
	}

	public int getComplex() {
		return complex;
	}

	public void setComplex(int complex) {
		this.complex = complex;
	}

	@Override
	public String toString() {
		return "DUControlFlowVisitor [usepoints=" + usepoints + ", defpoints=" + defpoints + ", func=" + func
				+ ", entrance=" + entrance + ", global=" + global + ", simple=" + simple + ", complex=" + complex
				+ ", assign=" + assign + ", condition=" + condition + ", loop=" + loop + ", function=" + function
				+ ", param=" + param + ", other=" + other + ", def=" + Arrays.toString(def) + "]";
	}


}
