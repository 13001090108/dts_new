package softtest.summary.gcc.fault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTAssignmentOperator;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTIterationStatement;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTStatementList;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.interpro.c.InterContext;
import softtest.rules.c.StateMachineUtils;
import softtest.rules.gcc.fault.UVF_PStateMachine;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.summary.gcc.fault.UVF_PInfo.UVF_PType;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;

class UVF_PCheckInfo {
	/** �Ѿ�����ʼ�������� */
	private IntegerDomain initedDomian;
	/**��Ҫ����ʼ��������*/
	private IntegerDomain needInitedDomain;
	/** ָ����� */
	private VariableNameDeclaration variable;
	/** ָ������Ƿ������ʼ�� */
	private boolean initedAsOne;

	public UVF_PCheckInfo(VariableNameDeclaration variable) {
		initedDomian = new IntegerDomain();
		needInitedDomain = new IntegerDomain();
		this.variable = variable;
		initedAsOne = false;
	}

	public UVF_PCheckInfo(VariableNameDeclaration variable, boolean initedAsOne) {
		initedDomian = new IntegerDomain();
		needInitedDomain = new IntegerDomain();
		this.variable = variable;
		this.initedAsOne = initedAsOne;
	}

	public UVF_PCheckInfo(VariableNameDeclaration variable, IntegerDomain initedDomian,IntegerDomain needInitedDomain) {
		this.initedDomian = initedDomian;
		this.needInitedDomain = needInitedDomain;
		this.variable = variable;
		this.initedAsOne = false;
	}

	public IntegerDomain getInitedDomian() {
		return initedDomian;
	}

	public void setInitedDomian(IntegerDomain initedDomian) {
		this.initedDomian = initedDomian;
	}
	
	public IntegerDomain getNeedInitedDomian() {
		return needInitedDomain;
	}

	public void setNeedinitedDomian(IntegerDomain needInitedDomain) {
		this.needInitedDomain = needInitedDomain;
	}

	public VariableNameDeclaration getVariable() {
		return variable;
	}

	public void setVariable(VariableNameDeclaration variable) {
		this.variable = variable;
	}

	public boolean isInitedAsOne() {
		return initedAsOne;
	}

	public void setInitedAsOne(boolean initedAsOne) {
		this.initedAsOne = initedAsOne;
	}
}

public class MethodUVF_PPreConditionVisitor extends CParserVisitorAdapter implements MethodFeatureVisitor {
	private static MethodUVF_PPreConditionVisitor instance;
	private MethodNameDeclaration methodDecl = null;
	private Set<VariableNameDeclaration> varSet = null;
	private static Set<UVF_PCheckInfo> initedVariable = null;

	private MethodUVF_PPreConditionVisitor() {
	}

	public static MethodUVF_PPreConditionVisitor getInstance() {
		if (instance == null) {
			instance = new MethodUVF_PPreConditionVisitor();
		}
		return instance;
	}

	public void visit(VexNode vexNode) {
		if (vexNode.getTreenode() == null) {
			return;
		}
		MethodUVF_PPreCondition feature = new MethodUVF_PPreCondition();
		SimpleNode node = vexNode.getTreenode();
		methodDecl = InterContext.getMethodDecl(vexNode);
		varSet = new HashSet<VariableNameDeclaration>();
		initedVariable = new HashSet<UVF_PCheckInfo>();
		// 1����麯����ָ�����͵Ĳ���������ȫ�ֱ���
		if (methodDecl != null && !isContainParam(methodDecl, node, varSet)) {
			return;
		}
		// ����varSet�б����ֱ���
		Iterator<VariableNameDeclaration> itr = varSet.iterator();
		while (itr.hasNext()) {
			VariableNameDeclaration var = itr.next();
			List<NameOccurrence> occs = getOccs(var);
			if (occs == null) {
				continue;
			}
			// ���տ�����ͼ�ķ���ʼ���ָ������Ƿ���Ҫ��ʼ��
			checkFunction(vexNode, var);
		}
		//��ӱ�����Լ����Ϣ
		UVF_PType type;
		VariableNameDeclaration variable;
		int paramIndex;
		for (UVF_PCheckInfo inited : initedVariable) {
			type=UVF_PType.NOT_NEED;
			if(inited.isInitedAsOne() || !inited.getInitedDomian().isEmpty()|| !inited.getNeedInitedDomian().isEmpty()){
				type=UVF_PType.NEED;
			}
			variable = inited.getVariable();
			if(!varSet.contains(variable)){
				continue;
			}
			if(variable.isParam()){
				paramIndex=variable.getParamIndex();
			}else{
				paramIndex=-10;
			}
			feature.addVariable(inited.getVariable(), paramIndex, type, inited.getNeedInitedDomian(), inited.getInitedDomian());
		}
		//����ǰ��Լ����Ϣ
		MethodSummary summary = InterContext.getMethodSummary(vexNode); 
		if (summary != null) {
			summary.addSideEffect(feature); 
			if (Config.INTER_METHOD_TRACE) { 
				if(methodDecl != null) { 
					System.err.println(methodDecl.getFullName() +" " + feature); 
				} 
			} 
		} 
	}

	/**
	 * ��������б����ں������Ƿ����ʹ��δ��ʼ��ǰ������������·�����еķ�ʽ�����⺯���д���������֧���
	 * 
	 * @param vexNode
	 * @param variable
	 */
	public static void checkFunction(VexNode vexNode,VariableNameDeclaration variable) {
		if (null == vexNode || null == variable || null==vexNode.getTreenode()) {
			return;
		}
		String varImage = variable.getImage();
		if (null == varImage || varImage.equals("")) {
			return;
		}
		List<SimpleNode> statementList = StateMachineUtils.getEvaluationResults(vexNode.getTreenode(),".//Statement");
		Iterator<SimpleNode> statementListIterator = statementList.iterator();
		VexNode checkVexNode = null;
		SimpleNode statement = null;
		while (statementListIterator.hasNext()) {
			statement = statementListIterator.next();
			if (null == statement) {
				continue;
			}
			checkVexNode = null;
			if (statement.jjtGetNumChildren() > 0) {
				checkVexNode = ((SimpleNode) statement.jjtGetChild(0))
						.getCurrentVexNode();
			}
			if (null == statement || null == checkVexNode || checkVexNode.isBackNode()) {
				continue;
			}
			/** ����ڸÿ������ڵ��϶�ָ����������ʼ�����Ͳ�����ʹ�ü�� */
			for (UVF_PCheckInfo inited : initedVariable) {
				if (varImage.equals(inited.getVariable().getImage()) && inited.isInitedAsOne()) {
					return;
				}
			}
			IntegerDomain result = checkUse(checkVexNode, variable);
			if (null == result) {
				continue;
			}
			addNeedInitedDomain(variable, result);
		}
	}



	private List<NameOccurrence> getOccs(VariableNameDeclaration variable) {
		Map<VariableNameDeclaration, ArrayList<NameOccurrence>> variableNames = new HashMap<VariableNameDeclaration, ArrayList<NameOccurrence>>();
		if (variable == null || variable.getScope() == null)
			return null;
		variableNames = variable.getScope().getVariableDeclarations();
		List<NameOccurrence> occs = variableNames.get(variable);
		return occs;
	}

	/**
	 * �жϵ�ǰ�����Ƿ��л�����������ָ��α���,ȫ��/���Աָ����� �����Ļ�����true
	 */
	private boolean isContainParam(MethodNameDeclaration methodDecl,SimpleNode node, Set<VariableNameDeclaration> varSet) {
		if (null ==methodDecl) {
			return false;
		}
		// 1.����ָ�����͵Ĳα���
		List<CType> paramList = methodDecl.getParams();
		Iterator<CType> itr = paramList.iterator();
		int index = 0;
		boolean flag = false;
		while (itr.hasNext()) {
			CType type = itr.next();
			if (type.isPointType()) {
				VariableNameDeclaration v = getParam(node, index);
				varSet.add(v);
				flag = true;
			}
			index++;
		}
		// 2.��ȫ��ָ�����ͱ���
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, ".//PrimaryExpression");
		Iterator<SimpleNode> varItr = evaluationResults.iterator();
		while (varItr.hasNext()) {
			VariableNameDeclaration var = varItr.next().getVariableNameDeclaration();
			if (null == var || varSet.contains(var)) {
				continue;
			}
			CType type = var.getType();
			if (type.isPointType()) {
				if (var.getScope() instanceof softtest.symboltable.c.LocalScope || var.getScope() instanceof softtest.symboltable.c.MethodScope) {
					continue;
				}
				varSet.add(var);
				flag = true;
			}
		}
		return flag;
	}

	private VariableNameDeclaration getParam(SimpleNode node, int index) {
		String xpath1 = ".//Declarator//DirectDeclarator//ParameterTypeList//ParameterDeclaration";
		List<SimpleNode> evaluationResults = StateMachineUtils
				.getEvaluationResults(node, xpath1);
		// formual ��������Ѱ�ҵ�index�α���
		int i = 0;
		for (SimpleNode snode : evaluationResults) {
			if (i == index) {
				String xpathparam = ".//Declarator//DirectDeclarator";
				List<SimpleNode> paramList = StateMachineUtils
						.getEvaluationResults(snode, xpathparam);
				if (paramList != null && paramList.size() > 0) {
					ASTDirectDeclarator param = (ASTDirectDeclarator) paramList
							.get(0);
					VariableNameDeclaration v = param
							.getVariableNameDeclaration();
					return v;
				}
			}
			i++;
		}
		return null;
	}

	/**
	 * ���ָ�����ʹ��δ��ʼ����ǰ������
	 * 
	 * @param vexNode
	 * @param variable
	 * @return 1: �Ѿ��������ʼ�� 0: û�м���� -1: ������ʹ��
	 */
	public static IntegerDomain checkUse(VexNode vexNode,VariableNameDeclaration variable) {
		if (null == vexNode || null == variable) {
			return null;
		}
		String varImage = variable.getImage();
		SimpleNode treeNode = vexNode.getTreenode();
		if (null == varImage || varImage.equals("") || null == treeNode) {
			return null;
		}
		// ָ����������廯���
		int result = checkInial(vexNode, variable);
		switch (result) {
			case 0:// ָ����������廯ʹ�ã���Ҫ���Ϊǰ������
				for (UVF_PCheckInfo inited : initedVariable) {
					if (varImage.equals(inited.getVariable().getImage()) && inited.isInitedAsOne()) {
					break;
				}
			}
			return IntegerDomain.getFullDomain();
		case 1:// ָ������������ʼ�����򲻻����ǰ������
			for (UVF_PCheckInfo inited : initedVariable) {
				if (varImage.equals(inited.getVariable().getImage()) && !inited.isInitedAsOne()) {
					inited.setInitedAsOne(true);
					return null;
				}
			}
			initedVariable.add(new UVF_PCheckInfo(variable, true));
			return null;
		case 2:// ���·����ڴ�(����������)
			return null;//TODO
		}
		//��⺯������
		//��⺯������
		UVF_PInfo uvf_pInfo=UVF_PStateMachine.checkMethod(vexNode.getTreenode(),variable);
		if(null != uvf_pInfo){
			addInitedDomain(variable,uvf_pInfo.getInitedomain());
			IntegerDomain needInitedDomain=uvf_pInfo.getNeedInitedomain();
			if(!needInitedDomain.isEmpty()){
				//�鿴�������Ƿ��Ѿ�����ʼ��
				for(UVF_PCheckInfo inited:initedVariable){
					//�ҵ��Ѿ���ʼ��������
					if(varImage.equals(inited.getVariable().getImage())){
						if(inited.isInitedAsOne() || inited.getInitedDomian().contains(needInitedDomain)){
							break;
						}
					}
				}
				return needInitedDomain;
			}
		}
		// ����Ƿ�ָ����������Ա�Ƿ�Ϊʹ��
		String useXpath = ".//UnaryExpression/PostfixExpression[count(*)=2 and ./PrimaryExpression[(@Image='"
				+ varImage+ "') and not(./Constant)] and not(./FieldId) and not(./ArgumentExpressionList) and (./Expression)]";
		List<SimpleNode> useList = StateMachineUtils.getEvaluationResults(
				treeNode, useXpath);
		Iterator<SimpleNode> useItr = useList.iterator();
		while (useItr.hasNext()) {
			ASTPostfixExpression checkNode = (ASTPostfixExpression) useItr
					.next();
			if (null == checkNode
					|| (checkNode.getCurrentVexNode() != null && checkNode
							.getCurrentVexNode().getContradict())
					|| checkNode.jjtGetNumChildren() != 2
					|| !(checkNode.jjtGetChild(1) instanceof ASTExpression)) {
				continue;
			}
			// ��ȡ��������Ա����
			VariableNameDeclaration checkVariable = checkNode.getVariableDecl();
			if (null == checkVariable) {
				continue;
			}
			String checkVarImage = checkVariable.getImage();
			if (null == checkVarImage || checkVarImage.equals("")) {
				continue;
			}
			// ��ȡ�����б������±��ȡֵ����
			ASTExpression expressionNode = (ASTExpression) checkNode
					.jjtGetChild(1);
			if (expressionNode == null) {
				continue;
			}
			ExpressionVistorData exprdata = new ExpressionVistorData();
			exprdata.currentvex = expressionNode.getCurrentVexNode();
			exprdata.sideeffect = true;

			ExpressionValueVisitor eVisitor = new ExpressionValueVisitor();
			exprdata = (ExpressionVistorData) eVisitor
					.visit((ASTAssignmentExpression) expressionNode
							.getFirstDirectChildOfType(ASTAssignmentExpression.class),
							exprdata);
			if(exprdata==null || exprdata.currentvex==null || exprdata.currentvex.getSymDomainset()==null || exprdata.value==null){
				continue;
			}
			Domain domain = exprdata.value.getDomain(exprdata.currentvex
					.getSymDomainset());
			// vDomain���ǿɱ��±�Ŀ���ȡֵ��Χ
			IntegerDomain vDomain = Domain.castToIntegerDomain(domain);
			// �鿴�������Ƿ��Ѿ�����ʼ��
			for (UVF_PCheckInfo inited : initedVariable) {
				if (varImage.equals(inited.getVariable().getImage())) {
					if (inited.isInitedAsOne()
							|| inited.getInitedDomian().contains(vDomain)) {
						continue;
					}
				}
			}
			// ������Ա�����Ƿ��ʼ��
			if (UVF_PStateMachine.isInit(checkVariable, (SimpleNode) checkNode,
					treeNode)) {
				/** ��������ʼ�� */
				// ���������б����Ķ�Ӧ������
				addInitedDomain(variable, vDomain);
				continue;
			}
			// ������Ա������ʹ��
			// p[0]&=0x03;�������������Ϊ��ַ���䣬����Ϊδ��ʼ��ʹ��
			if (checkNode.jjtGetParent().jjtGetParent().jjtGetParent()
					.jjtGetNumChildren() > 1) {
				if (checkNode.jjtGetParent().jjtGetParent().jjtGetParent()
						.jjtGetChild(1) instanceof ASTAssignmentOperator) {
					if (((ASTAssignmentOperator) (checkNode.jjtGetParent()
							.jjtGetParent().jjtGetParent().jjtGetChild(1)))
							.getOperators().equals("&=")) {
						continue;
					}
				}
			}
			SimpleNode parent = (SimpleNode) checkNode
					.getFirstParentOfType(ASTUnaryExpression.class);
			if (parent != null
					&& parent.jjtGetParent() instanceof ASTUnaryExpression) {
				if (StateMachineUtils.getEvaluationResults(
						(SimpleNode) parent.jjtGetParent(),
						"./UnaryOperator[@Operators='&']") != null)
					continue;
			}
			parent = checkNode;
			boolean flag = false;
			while (!(parent instanceof ASTStatement)) {
				parent = (SimpleNode) parent.jjtGetParent();
				// ��������sizeof(*a)������
				if (parent instanceof ASTUnaryExpression
						&& parent.getImage().equals("sizeof")) {
					flag = true;
					break;
				}
			}
			if (flag) {
				continue;
			}
			/**
			 * ����**head�ڵ㣬����getTreenode()�����������飬���û������ļ�⣬��������������ʹ��
			 * ���ʼ���������©��������Ҫ�������б���ʹ�ó��ּ�֮ǰ��statement�ڵ���г�ʼ����⣬�������
			 * ����Щ����г�ʼ��������תend״̬�����򱨸���ϣ�δ��ʼ��ʹ�á�
			 */
			if (!(vexNode.getName().startsWith("for_head")
					|| vexNode.getName().startsWith("if_head")
					|| vexNode.getName().startsWith("while_head")
					|| vexNode.getName().startsWith("do_while_head")
					|| vexNode.getName().startsWith("switch_head") || vexNode
					.getName().startsWith("label_head_case"))
					&& (checkNode
							.getFirstParentOfType(ASTSelectionStatement.class) == null && checkNode
							.getFirstParentOfType(ASTIterationStatement.class) == null)) {
				return vDomain;
			} else if (vexNode.getName().startsWith("while_head")) {
				parent = (SimpleNode) checkNode.jjtGetParent();
				while (parent != treeNode) {
					parent = (SimpleNode) parent.jjtGetParent();
					if (treeNode.jjtGetChild(0) instanceof ASTExpression
							&& parent == treeNode.jjtGetChild(0)) {
						return vDomain;
					}
				}
				continue;
			} else if (!(vexNode.getName().startsWith("for_head")
					|| vexNode.getName().startsWith("if_head")
					|| vexNode.getName().startsWith("while_head")
					|| vexNode.getName().startsWith("do_while_head")
					|| vexNode.getName().startsWith("switch_head") || vexNode
					.getName().startsWith("label_head_case"))
					&& (checkNode
							.getFirstParentOfType(ASTSelectionStatement.class) != null || checkNode
							.getFirstParentOfType(ASTIterationStatement.class) != null)) {
				treeNode = (SimpleNode) ((checkNode
						.getFirstParentOfType(ASTSelectionStatement.class) != null) ? checkNode
						.getFirstParentOfType(ASTSelectionStatement.class)
						: checkNode
								.getFirstParentOfType(ASTIterationStatement.class));
				Node statementList = treeNode
						.getFirstChildOfType(ASTStatementList.class);
				Node end = checkNode.getFirstParentOfType(ASTStatement.class);
				if (statementList == null
						|| statementList.jjtGetNumChildren() == 0) {
					return vDomain;
				} else {
					for (int i = 0; i != statementList.jjtGetNumChildren(); i++) {
						Node temp = statementList.jjtGetChild(i);
						if (temp == end) {
							return vDomain;
						}
					}
				}
			}
		}
		// ����Ѿ���P[0]���г�ʼ�����򲻼��*p
		IntegerDomain vDomain = new IntegerDomain(0, 0);
		// �鿴�������Ƿ��Ѿ�����ʼ��
		for (UVF_PCheckInfo inited : initedVariable) {
			// �ҵ��Ѿ���ʼ��������
			if (varImage.equals(inited.getVariable().getImage())) {
				if (inited.isInitedAsOne()
						|| inited.getInitedDomian().contains(vDomain)) {
					return null;
				}
			}
		}
		// ������Ա����*p
		useXpath = ".//UnaryExpression[(./UnaryOperator[@Operators='*'])]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"
				+ varImage + "']";
		useList = StateMachineUtils.getEvaluationResults(treeNode, useXpath);
		useItr = useList.iterator();
		while (useItr.hasNext()) {
			ASTUnaryExpression unary = (ASTUnaryExpression) ((SimpleNode) useItr
					.next().getFirstParentOfType(ASTUnaryExpression.class))
					.getFirstParentOfType(ASTUnaryExpression.class);
			if (null == unary
					|| (unary.getCurrentVexNode() != null && unary
							.getCurrentVexNode().getContradict())) {
				continue;
			}
			if (!(vexNode.getName().startsWith("for_head")
					|| vexNode.getName().startsWith("if_head")
					|| vexNode.getName().startsWith("while_head")
					|| vexNode.getName().startsWith("do_while_head")
					|| vexNode.getName().startsWith("switch_head") || vexNode
					.getName().startsWith("label_head_case"))
					&& (unary.getFirstParentOfType(ASTSelectionStatement.class) == null && unary
							.getFirstParentOfType(ASTIterationStatement.class) == null)) {
				return vDomain;
			}
		}
		return null;
	}

	/** ���״̬����ر����Ƿ������ʼ�� */
	public static int checkInial(VexNode checkedVexNode,
			VariableNameDeclaration variable) {
		SimpleNode releatedTreeNode = checkedVexNode.getTreenode();
		if (checkedVexNode.isBackNode()
				|| checkedVexNode.getName().startsWith("for_head")
				|| checkedVexNode.getName().startsWith("if_head")
				|| checkedVexNode.getName().startsWith("while_head")
				|| checkedVexNode.getName().startsWith("do_while_head")
				|| checkedVexNode.getName().startsWith("switch_head")
				|| checkedVexNode.getName().startsWith("label_head_case")) {
			/** ����β�ڵ���д��� */
			String mayInialXpath = "./Expression";
			List<SimpleNode> mayInialList = StateMachineUtils
					.getEvaluationResults(releatedTreeNode, mayInialXpath);
			Iterator<SimpleNode> mayInialItr = mayInialList.iterator();
			int result = -1;
			while (mayInialItr.hasNext()) {
				result = UVF_PStateMachine.checkAsOne(variable,
						(ASTExpression) mayInialItr.next());
				if (result != -1) {
					return result;
				}
			}
			return result;
		}
		return UVF_PStateMachine.checkAsOne(variable, releatedTreeNode);
	}

	/**
	 * ���״̬����ر����Ƿ񱻳�ʼ�� ��������ѭ������β�ڵ�while_out��for_out
	 * �ٶ��ܹ�����while/forѭ�������ұ��������г�ʼ���ˣ���ѭ����Ĭ��Ϊ�ñ����ѳ�ʼ
	 * ����÷��������ڣ�ֻ����������checkInial����ΪUVF����Ϊ��·������
	 * �����Կ��ܻ���Ϊ��һ��û�н�whileѭ����·���������󱨣�·�����в��ᵼ��©���ģ� �Ӹ÷�����Ŀ�Ľ����Ǽ�����Ϊѭ����������̫����
	 * */
	/*public static boolean checkArrayInial(VexNode checVexNode,
			FSMMachineInstance fsmInst) {
		if (!(checVexNode.getName().startsWith("for_out") || checVexNode
				.getName().startsWith("while_out"))) {
			return false;
		}
		SimpleNode releatedTreeNode = checVexNode.getTreenode();
		VariableNameDeclaration variable = fsmInst.getRelatedVariable();
		int result = UVF_PStateMachine.checkAsOne(variable, releatedTreeNode);
		switch (result) {
		case 1:// ��ʼ��
			if (!initedAsOne.contains(variable)) {
				initedAsOne.add(variable);
			}
			return true;
		}
		return false;
	}*/

	public static boolean addInitedDomain(VariableNameDeclaration variable,
			IntegerDomain vDomain) {
		if (null == variable || null == vDomain) {
			return false;
		}
		String varImage = variable.getImage();
		for (UVF_PCheckInfo inited : initedVariable) {
			// �ҵ��Ѿ���ʼ��������
			if (varImage.equals(inited.getVariable().getImage())) {
				if (inited.isInitedAsOne()
						|| inited.getInitedDomian().contains(vDomain)) {
					return true;
				}
				inited.getInitedDomian().mergeWith(vDomain.jointoOneInterval());
				return true;
			}
		}
		// û���ҵ��Ѿ���ʼ��������
		initedVariable.add(new UVF_PCheckInfo(variable, vDomain,new IntegerDomain()));
		return true;
	}
	
	private static boolean addNeedInitedDomain(VariableNameDeclaration variable,
			IntegerDomain vDomain) {
		if (null == variable || null == vDomain) {
			return false;
		}
		String varImage = variable.getImage();
		for (UVF_PCheckInfo inited : initedVariable) {
			// �ҵ���Ҫ��ʼ��������
			if (varImage.equals(inited.getVariable().getImage())) {
				if (inited.isInitedAsOne()) {
					return false;
				}
				if (inited.getNeedInitedDomian().contains(vDomain)) {
					return true;
				}
				inited.getNeedInitedDomian().mergeWith(vDomain.jointoOneInterval());
				return true;
			}
		}
		// û���ҵ���ʼ��������
		initedVariable.add(new UVF_PCheckInfo(variable, new IntegerDomain(),vDomain));
		return true;
	}
}
