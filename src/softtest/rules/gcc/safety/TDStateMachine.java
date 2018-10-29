package softtest.rules.gcc.safety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTAssignmentOperator;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTInitDeclarator;
import softtest.ast.c.ASTInitializer;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.Edge;
import softtest.cfg.c.VexNode;
//import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.fsm.c.FSMRelatedCalculation;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.MethodNameDeclaration;

public class TDStateMachine {
	
	/** �ܱ�ʶ������뺯�����Ƽ��� */
	private final static String[] INPUT_FUNC = {"getenv", "scanf", "gets", "read", "fread", "fgets"};

	private final static int[] INPUT_FUNC_INEX = {0, 2, 1, 2, 1, 1}; 

	/** ����״̬��ʵ��������Ϊÿ�����뺯������һ��״̬��ʵ�����������Ⱦ�ı������� */
	public static List<FSMMachineInstance> createTDStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		// �ھ����������ʱ������
		
		StringBuffer sb = new StringBuffer();
		sb.append(".//UnaryExpression/PostfixExpression[./PrimaryExpression[");
		for (int i = 0; i < INPUT_FUNC.length; i++) {
			sb.append("@Image='" + INPUT_FUNC[i] + "' or ");
		}
		sb.delete(sb.length() - 4, sb.length());
		sb.append("]]");
		List evaluationResults = null;

		evaluationResults = StateMachineUtils.getEvaluationResults(node, sb.toString());
		Iterator i = evaluationResults.iterator();
		while (i.hasNext()) {
			ASTPostfixExpression post = (ASTPostfixExpression) i.next();
//mark			if (!post.getIsFun() && post.jjtGetNumChildren() == 0) {
			if (post.jjtGetNumChildren() == 0) {
				continue;
			}
			FSMMachineInstance fsminstance = fsm.creatInstance();
			// ��������Ⱦ�ı������ϣ�����Ӽ��ϣ���Inputed�����
			TaintedSet tainted = new TaintedSet();
			// ���ñ�ǽڵ�
			tainted.setTagTreeNode(post);
			fsminstance.setRelatedObject(tainted);
			fsminstance.setDesp("�� " + post.getBeginLine() + " ��ͨ������  " +post.getImage()+" �õ����ⲿ����");
			if (!post.hasLocalMethod(node)) {
				list.add(fsminstance);
			}
			
		}
		
		/**
		 * ����main�����Ĳ�����Ϊ����ʹ�õ�״̬������int main(int argc, char **argv);
		 */
		if (node instanceof ASTFunctionDefinition) {
			ASTFunctionDefinition mainNode = (ASTFunctionDefinition)node;
			MethodNameDeclaration mDecl = mainNode.getDecl();
			
			if (mDecl != null && mDecl.getName().equals("main")) {
				String xpath = ".//Declarator/DirectDeclarator//ParameterList/ParameterDeclaration[position()>1]//DirectDeclarator";
				List<SimpleNode> args = StateMachineUtils.getEvaluationResults(mainNode, xpath);
				
				if (args != null && args.size() != 0) {
					for (SimpleNode arg : args) {
						VariableNameDeclaration varDecl = arg.getVariableNameDeclaration();
						if (varDecl != null) {
							FSMMachineInstance fsminstance = fsm.creatInstance();
							TaintedSet tdSet = new TaintedSet();
							tdSet.add(varDecl);
							fsminstance.setResultString(varDecl.getName());
							tdSet.setTagTreeNode(varDecl.getNode());
							fsminstance.setRelatedObject(tdSet);
							fsminstance.setRelatedASTNode((SimpleNode)node.jjtGetChild(1));
							fsminstance.setRelatedVariable(varDecl);
							list.add(fsminstance);
						}
					}
				}
			}
		}
		

		return list;
	}

	/** ����Ƿ�Ϊͬһ��input���� */
	public static boolean checkSameInput(List nodes, FSMMachineInstance fsmin) {
		
		Iterator i = nodes.iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			TaintedSet tainted = (TaintedSet) fsmin.getRelatedObject();
			if (tainted.getTagTreeNode() == obj) {
				// ��Ӽ���
				if (obj instanceof ASTPostfixExpression) {
					ASTPostfixExpression func = (ASTPostfixExpression) obj;
					for (int index = 0; index < INPUT_FUNC.length; index++) {
						if (func.getImage().equals(INPUT_FUNC[index])) {
							if (INPUT_FUNC_INEX[index] == 0) {
								SimpleNode father = (SimpleNode)func.jjtGetParent();
								while (!(father instanceof ASTFunctionDefinition)) {
									if (father instanceof ASTAssignmentExpression && father.jjtGetNumChildren() == 3) {
										ASTAssignmentExpression assign = (ASTAssignmentExpression)father;
										String xpath = ".//UnaryExpression/PostfixExpression/PrimaryExpression[not (./Expression)]";
										List left = StateMachineUtils.getEvaluationResults(assign, xpath);
										if (left.size() > 0) {
											ASTPrimaryExpression leftPrimary = (ASTPrimaryExpression)left.get(0);
											if (leftPrimary.getVariableDecl() instanceof VariableNameDeclaration) {
												tainted.add((VariableNameDeclaration)leftPrimary.getVariableDecl());
											}
										}
										break;
									}
									if (father instanceof ASTInitDeclarator && father.jjtGetNumChildren() == 2) {
										ASTInitDeclarator init = (ASTInitDeclarator)father;
										String xpath = ".//Declarator/DirectDeclarator";
										List left = StateMachineUtils.getEvaluationResults(init, xpath);
										if (left.size() > 0) {
											ASTDirectDeclarator decl = (ASTDirectDeclarator)left.get(0);
											if (decl.getDecl() instanceof VariableNameDeclaration) {
												tainted.add((VariableNameDeclaration)decl.getDecl());
											}
										}
										break;
									}
									father = (SimpleNode)father.jjtGetParent();
								}
								
								
							} else {
								ASTPrimaryExpression tagNode = StateMachineUtils.getArgumentnew(func, INPUT_FUNC_INEX[index]);
								if (tagNode.getVariableDecl() instanceof VariableNameDeclaration) {
									tainted.add((VariableNameDeclaration)tagNode.getVariableDecl());
								}
							}
						}
					}
					
				} 
				return true;
			}
		}
		return false;
	}

	/** ��鵱ǰ����Ⱦ�ı��������Ƿ�Ϊ�ռ� */
	public static boolean checkTaintedSetEmpty(VexNode vex, FSMMachineInstance fsmin) {
		TaintedSet tainted = (TaintedSet) fsmin.getRelatedObject();
		if (tainted.isEmpty()) {
			return true;
		}
		return false;
	}

	/** ��鵱ǰ�ڵ��Ƿ�ʹ���˱���Ⱦ���������еı��� */
	public static boolean checkTaintedDataUsed(VexNode vex, FSMMachineInstance fsmin) {
		TaintedSet tainted = (TaintedSet) fsmin.getRelatedObject();
		try {
			if (tainted.checkUsed(vex,fsmin)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean checkMainArgs(List nodes, FSMMachineInstance fsmin) 
	{
		Iterator i = nodes.iterator();
		SimpleNode mainNode = (SimpleNode)i.next();
		
		if (mainNode == fsmin.getRelatedASTNode())
			return true;
					
		return false;
	}
}

class TaintedSet extends FSMRelatedCalculation {
	public TaintedSet() {
	}

	public TaintedSet(FSMRelatedCalculation FSMo) {
		super(FSMo);
		if (!(FSMo instanceof TaintedSet)) {
			return;
		}
		TaintedSet t = (TaintedSet) FSMo;
		for (Enumeration<VariableNameDeclaration> e = t.table.elements(); e.hasMoreElements();) {
			VariableNameDeclaration v = e.nextElement();
			table.put(v, v);
		}
	}

	/** ���� */
	public FSMRelatedCalculation copy() {
		FSMRelatedCalculation r = new TaintedSet(this);
		return r;
	}

	/** ����Ⱦ������ */
	private Hashtable<VariableNameDeclaration, VariableNameDeclaration> table = new Hashtable<VariableNameDeclaration, VariableNameDeclaration>();

	/** ��ӱ���Ⱦ������ֻ����������ֵ�ͺ�String���ͱ��� */
	public void add(VariableNameDeclaration v) {
		String VariableType = v.getType().getName();
		//if(VariableType.equalsIgnoreCase("int") || VariableType.equalsIgnoreCase("pointer"))
			table.put(v, v);				
	}

	/** �Ӽ������Ƴ����� */
	public void remove(VariableNameDeclaration v) {
		table.remove(v);
	}

	public boolean isEmpty() {
		return table.isEmpty();
	}

	public boolean contains(VariableNameDeclaration v) {
		return table.containsKey(v);
	}

	public Hashtable<VariableNameDeclaration, VariableNameDeclaration> getTable() {
		return table;
	}

	public void setTable(Hashtable<VariableNameDeclaration, VariableNameDeclaration> table) {
		this.table = table;
	}

	/** ���������������е�IN */
	public void calculateIN(FSMMachineInstance fsmin, VexNode n, Object data) {
		if (fsmin.getRelatedObject() != this) {
			throw new RuntimeException("TaintedSet error");
		}
		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e.hasMoreElements();) {
			list.add(e.nextElement());
		}
		Collections.sort(list);

		boolean bfirst = true;
		Iterator<Edge> iter = list.iterator();
		while (iter.hasNext()) {
			Edge edge = iter.next();
			VexNode pre = edge.getTailNode();

			if (pre.getFSMMachineInstanceSet() != null) {
				FSMMachineInstance prefsmin = pre.getFSMMachineInstanceSet().getTable().get(fsmin);
				if (prefsmin != null) {
					if (bfirst) {
						bfirst = false;
						Hashtable<VariableNameDeclaration, VariableNameDeclaration> table1, newtable;
						TaintedSet s1 = (TaintedSet) prefsmin.getRelatedObject();
						table1 = s1.getTable();
						newtable = new Hashtable<VariableNameDeclaration, VariableNameDeclaration>();

						for (Enumeration<VariableNameDeclaration> e = table1.elements(); e.hasMoreElements();) {
							VariableNameDeclaration v = e.nextElement();
							newtable.put(v, v);
						}
						setTable(newtable);
					} else {
						Hashtable<VariableNameDeclaration, VariableNameDeclaration> table1, table2, newtable;
						TaintedSet s1 = (TaintedSet) fsmin.getRelatedObject();
						TaintedSet s2 = (TaintedSet) prefsmin.getRelatedObject();
						table1 = s1.getTable();
						table2 = s2.getTable();
						newtable = new Hashtable<VariableNameDeclaration, VariableNameDeclaration>();
						// �󲢼�
						for (Enumeration<VariableNameDeclaration> e = table1.elements(); e.hasMoreElements();) {
							VariableNameDeclaration v = e.nextElement();
							newtable.put(v, v);
						}
						for (Enumeration<VariableNameDeclaration> e = table2.elements(); e.hasMoreElements();) {
							VariableNameDeclaration v = e.nextElement();
							newtable.put(v, v);
						}
						setTable(newtable);
					}
				}
			}
		}
	}

	/** �жϱ���Ⱦ�ı����Ƿ�����ڽڵ�node�� */
	private boolean hasTainedOccurenceIn(SimpleNode node) {
		List list = StateMachineUtils.getEvaluationResults(node, ".//PrimaryExpression");
		Iterator i = list.iterator();
		while (i.hasNext()) {
			ASTPrimaryExpression name = (ASTPrimaryExpression) i.next();
			if (!(name.getVariableDecl() instanceof VariableNameDeclaration)) {
				continue;
			}
			VariableNameDeclaration v = (VariableNameDeclaration) name.getVariableDecl();
			if (contains(v)) {
				return true;
			}
		}
		return false;
	}

	private final static String[] STRING_USE_FUNCTION = { 
		"printf", "sprintf", "fprintf", "fopen", "strcpy", "system", "ldap_search_ext_s", "ldap_search_ext_s", 
		"gethostbyaddr", "gethostbyname", "sethostname","execl"
		};
	
	private final static int[] STRING_USE_FUNC_INDEX = {
		1, 1, 1, 1, 2, 1, 2, 4,
		1, 1, 1,1
		};
	
	private final static String[] INT_USE_FUNCTION = {
		"sleep", "malloc", "fread", "fwrite", "fgets", "gets", "read", "write",
		"setpgid", "setpgrp", "setpid", "setpid", "setpriority"
	};
	
	private final static int[] INT_USE_FUNC_INDEX = {
		1, 1, 3, 3, 2, 2, 3, 3,
		2, 2, 1, 1, 3
	};
	
	/** ���ʹ�� */
	public boolean checkUsed(VexNode n,FSMMachineInstance fsmin) {
		String xpath = "";
		StringBuffer sb;
		List list = null;
		Iterator i = null;
		if (n.isBackNode()) {
			return false;
		}
		SimpleNode treenode=n.getTreenode();
		if(treenode==null){
			return false;
		}
		for (Enumeration<VariableNameDeclaration> e = table.elements(); e.hasMoreElements();) {
			VariableNameDeclaration v = e.nextElement();
			String VariableType = v.getType().getName();
			if(VariableType.equalsIgnoreCase("int")){
				
				/**
				 * ���磺 a[v];
				 */
				xpath = ".//UnaryExpression/PostfixExpression[.//PrimaryExpression and .//Expression]";
				list = StateMachineUtils.getEvaluationResults(treenode, xpath);
				i = list.iterator();
				while (i.hasNext()) {
					ASTPostfixExpression array = (ASTPostfixExpression) i.next();
					if (array.getOperatorType().size() == 0) {
						break;
					}
					String operator = array.getOperatorType().get(0).trim();
					if (operator.equals("[")) {
						xpath = "//PostfixExpression/PrimaryExpression[not (./Expression)]";
						List indexList = StateMachineUtils.getEvaluationResults((SimpleNode)array.jjtGetChild(1), xpath);
						Iterator j = indexList.iterator();
						while (j.hasNext()) {
							ASTPrimaryExpression index = (ASTPrimaryExpression)j.next(); 
							if (v == index.getVariableDecl()) {
								fsmin.setDesp(fsmin.getDesp() + " �� " + index.getBeginLine() + " �б���Ⱦ");
								return true;
							}
						}
					}
				}
				
				/**
				 * 		for (int i = 0; i < num; i++)
				 * or 
				 * 		while(i < num)
				 */
				if (n.getName().startsWith("for_head") || n.getName().startsWith("while_head")) {
					xpath = ".//RelationalExpression//UnaryExpression/PostfixExpression/PrimaryExpression[not (./Expression)]";
					list = StateMachineUtils.getEvaluationResults(treenode, xpath);
					i = list.iterator();
					while (i.hasNext()) {
						ASTPrimaryExpression primary = (ASTPrimaryExpression)i.next();
						if (primary.getVariableDecl() == v) {
							fsmin.setResultString(primary.getImage());							
							return true;
						}
					}
				}
				
				/**
				 * int user function
				 */
				sb = new StringBuffer();
				sb.append(".//UnaryExpression/PostfixExpression[./PrimaryExpression[");
				for (int k = 0; k < INT_USE_FUNCTION.length; k++) {
					sb.append("@Image='" + INT_USE_FUNCTION[k] + "' or ");
				}
				sb.delete(sb.length() - 4, sb.length());
				sb.append("]]");
				list = StateMachineUtils.getEvaluationResults(treenode, sb.toString());
				i = list.iterator();
				while (i.hasNext()) {
					ASTPostfixExpression func = (ASTPostfixExpression)i.next();
					for (int k = 0; k < INT_USE_FUNCTION.length; k++) {
						if (func.getImage().equals(INT_USE_FUNCTION[k])) {
							ASTPrimaryExpression paramNode = StateMachineUtils.getArgumentnew(func, INT_USE_FUNC_INDEX[k]);
							if (v == paramNode.getVariableDecl()) {
								fsmin.setResultString(v.getImage());
								fsmin.setDesp(fsmin.getDesp() + " �� " + func.getBeginLine() + " �б���Ⱦ");
								return true;
							}
						}
					}
				}
			}
			else if(VariableType.equalsIgnoreCase("pointer") || VariableType.equalsIgnoreCase("array")){
				sb = new StringBuffer();
				sb.append(".//UnaryExpression/PostfixExpression[./PrimaryExpression[");
				for (int k = 0; k < STRING_USE_FUNCTION.length; k++) {
					sb.append("@Image='" + STRING_USE_FUNCTION[k] + "' or ");
				}
				sb.delete(sb.length() - 4, sb.length());
				sb.append("]]");
				list = StateMachineUtils.getEvaluationResults(treenode, sb.toString());
				i = list.iterator();
				while (i.hasNext()) {
					ASTPostfixExpression func = (ASTPostfixExpression)i.next();
					for (int k = 0; k < STRING_USE_FUNCTION.length; k++) {
						if (func.getImage().equals(STRING_USE_FUNCTION[k])) {
							ASTPrimaryExpression paramNode = StateMachineUtils.getArgumentnew(func, STRING_USE_FUNC_INDEX[k]);
							if (paramNode != null && v == paramNode.getVariableDecl()) {
								fsmin.setResultString(v.getImage());
								fsmin.setDesp(fsmin.getDesp() + " �� " + func.getBeginLine() + " �б���Ⱦ");
								return true;
							}
						}
					}
				}
				break;
			}
		}
		return false;
	}
	/** �����飬�Ӽ������Ƴ���Щ�����ı��� */
	public void check(VexNode n) {
		String xpath = "";
		StringBuffer buffer = null;
		List list = null;
		Iterator i = null;
		for (Enumeration<VariableNameDeclaration> e = table.elements(); e.hasMoreElements();) {
			VariableNameDeclaration v = e.nextElement();
			boolean bchecked = false;
			String VariableType = v.getType().getName();
			if(VariableType.equalsIgnoreCase("int")){
				if (n.getInedges().size() == 1) {
					for (Edge edge : n.getInedges().values()) {
						VexNode pre = edge.getTailNode();
						if (edge.getName().startsWith("T") || edge.getName().startsWith("F")) {
							SimpleNode treenode = pre.getTreenode();
							if (treenode != null) {
								/**
								 * ���磺 if( v<10 || v == 0){}
								 */
								xpath = ".//RelationalExpression/UnaryExpression/PostfixExpression/PrimaryExpression|" +
										".//EqualityExpression/UnaryExpression/PostfixExpression/PrimaryExpression";
								list = StateMachineUtils.getEvaluationResults(treenode, xpath);
								i = list.iterator();
								while (i.hasNext()) {
									ASTPrimaryExpression name = (ASTPrimaryExpression) i.next();
									if (v == name.getVariableDecl()) {
										bchecked = true;
										break;
									}
								}
							}
						}
					}
				}
			}
			else if(VariableType.equalsIgnoreCase("pointer") || VariableType.equalsIgnoreCase("array")){
				
				SimpleNode treenode = n.getTreenode();
				if (!n.isBackNode()&&treenode != null) {
					StringBuffer sb = new StringBuffer();
					sb.append(".//UnaryExpression/PostfixExpression[./PrimaryExpression[");
					for (int k = 0; k < STRING_TRUE_CHECK.length; k++) {
						sb.append("@Image='" + STRING_TRUE_CHECK[k] + "' or ");
					}
					sb.delete(sb.length() - 4, sb.length());
					sb.append("]]");
					list = StateMachineUtils.getEvaluationResults(treenode, sb.toString());
					i = list.iterator();
					while (i.hasNext()) {
						ASTPostfixExpression func = (ASTPostfixExpression)i.next();
						xpath = ".//ArgumentExpressionList/AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression[not (./Expression)]";
						List indexList = StateMachineUtils.getEvaluationResults(func, xpath);
						Iterator j = indexList.iterator();
						while (j.hasNext()) {
							ASTPrimaryExpression paramNode = (ASTPrimaryExpression)j.next(); 
							if (v == paramNode.getVariableDecl()) {
								bchecked = true;
							}
						}
					}				
				if (bchecked) {
					remove(v);
				}
				}
			}
		}
	}

	private static String[] STRING_TRUE_CHECK = { "strcmp", "strcmpn", "strlen"};
	
	/** ���������������е�OUT */

	public void calculateOUT(FSMMachineInstance fsmin, VexNode n, Object data) {
		if (fsmin.getRelatedObject() != this) {
			throw new RuntimeException("TaintedSet error");
		}
		List evaluationResults = new LinkedList();
		SimpleNode treenode = n.getTreenode();

		// xpath��������Щβ�ڵ�
		if (!n.isBackNode()) {
			// ����ֵ
			if (treenode != null) {
				evaluationResults = StateMachineUtils.getEvaluationResults(treenode, ".//AssignmentOperator[@Image=\'=\']");
			}
			Iterator i = evaluationResults.iterator();
			while (i.hasNext()) {
				ASTAssignmentOperator assign = (ASTAssignmentOperator) i.next();
				SimpleNode parent = (SimpleNode) assign.jjtGetParent();
				if (parent.jjtGetNumChildren() != 3) {
					continue;
				}
				SimpleNode left = (SimpleNode) parent.jjtGetChild(0);
				SimpleNode right = (SimpleNode) parent.jjtGetChild(2);

				ASTPrimaryExpression leftname = (ASTPrimaryExpression)left.getSingleChildofType(ASTPrimaryExpression.class);
				VariableNameDeclaration leftv = null;
				if (leftname == null || !(leftname.getVariableDecl() instanceof VariableNameDeclaration)) {
					continue;
				}
				leftv = (VariableNameDeclaration) leftname.getVariableDecl();
				if (hasTainedOccurenceIn(right)) {
					add(leftv);
				} else {
					remove(leftv);
				}
			}

			// �����ʼ����ֵ
			if (treenode != null) {
				evaluationResults = StateMachineUtils.getEvaluationResults(treenode, ".//InitDeclarator[./Initializer]");
			}
			i = evaluationResults.iterator();
			while (i.hasNext()) {
				ASTInitDeclarator initializer = (ASTInitDeclarator) i.next();

				ASTDeclarator left = (ASTDeclarator) initializer.jjtGetChild(0);
				ASTInitializer right = (ASTInitializer)initializer.jjtGetChild(1);

				VariableNameDeclaration leftv = null;
				if (left == null || !(left.getDeclList().get(0) instanceof VariableNameDeclaration)) {
					continue;
				}
				leftv = left.getDeclList().get(0);
				if (hasTainedOccurenceIn(right)) {
					add(leftv);
				} else {
					remove(leftv);
				}
			}
		}
		// ����������

		ArrayList<VariableNameDeclaration> todelete = new ArrayList<VariableNameDeclaration>();
		Hashtable<VariableNameDeclaration, VariableNameDeclaration> table = getTable();
		for (Enumeration<VariableNameDeclaration> e = table.keys(); e.hasMoreElements();) {
			VariableNameDeclaration v = e.nextElement();
			Scope delscope = v.getScope();
			SimpleNode astnode = n.getTreenode();

			boolean b = false;
			if (astnode == null) {
				continue;
			}
			if (!astnode.getScope().isSelfOrAncestor(delscope)) {
				// �����������Ѿ����ǵ�ǰ�������Լ�������
				b = true;
			} else if (delscope.isSelfOrAncestor(astnode.getScope()) && n.isBackNode()) {
				// ��ǰ�������������������Լ����߸��ף����ǵ�ǰ�ڵ���Ҫ��ֹ��ǰ������
				b = true;
			} else {
				b = false;
			}
			if (b) {
				todelete.add(v);
			}
		}

		for (VariableNameDeclaration v : todelete) {
			remove(v);
		}

		// ������
		check(n);
	}

}
