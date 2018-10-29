package softtest.rules.gcc.fault;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.*;
import softtest.cfg.c.VexNode;
import softtest.fsm.c.*;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;

/**
 * @author zx UnInited Variable[δ��ʼ������]
 * */
public class UVFStateMachine {

	/** Ϊÿ��û�г�ʼ���ķǾ�̬�����ⲿ�ľֲ���������״̬�� */
	public static List<FSMMachineInstance> createUVFStateMachines(
			SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		Hashtable<VariableNameDeclaration, FSMMachineInstance> fsmInsTable = new Hashtable<VariableNameDeclaration, FSMMachineInstance>();

		// ���ҵ�ǰ�����е����зǾ�̬�����ⲿ���͵ľֲ�����, û�г�ʼ�����ʽDeclarationList/
		String xpath = ".//Declaration[./DeclarationSpecifiers[not(./StorageClassSpecifier)]]/InitDeclaratorList/InitDeclarator[not(./Initializer)]/Declarator/DirectDeclarator";
		xpath += "| .//Declaration[./DeclarationSpecifiers[not(./StorageClassSpecifier)]]/InitDeclaratorList/InitDeclarator[./Initializer/AssignmentExpression/CastExpression//UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc' or @Image='realloc']]/Declarator/DirectDeclarator";
		List<SimpleNode> evaluationResults = StateMachineUtils
				.getEvaluationResults(node, xpath);
		/** Ϊ��ǰ��ֵ��ı�������״̬��ʵ�� */
		Iterator<SimpleNode> itr = evaluationResults.iterator();
		while (itr.hasNext()) {
			SimpleNode snode = itr.next();
			ASTDirectDeclarator primaryNode = (ASTDirectDeclarator) snode;
			VariableNameDeclaration variable = primaryNode
					.getVariableNameDeclaration();
			if (variable == null) {
				continue;
			}
			CType type = variable.getType();
			// ֻ���������ͨ���͵ı�������������
			if (!(type instanceof CType_BaseType || type instanceof CType_Array || type instanceof CType_Pointer)) {
				continue;
			}
			// ��������ֻ���ǣ������Ա�Ǽ����͵ı���
			if (type instanceof CType_Array
					&& !(((CType_Array) type).getOriginaltype() instanceof CType_BaseType)) {
				continue;
			}

			if (type instanceof CType_Pointer
					&& !(((CType_Pointer) type).getOriginaltype() instanceof CType_BaseType)) {
				continue;
			}
			// ��Ϊ��ǰ�����оֲ���������״̬��ʵ��
			if (!(variable.getScope() instanceof LocalScope || variable
					.getScope() instanceof MethodScope)) {
				continue;
			}
			if (variable.isParam()) {
				continue;
			}
			if (!(fsmInsTable.containsKey(variable))) {
				FSMMachineInstance fsmInstance = fsm.creatInstance();
				fsmInstance.setRelatedVariable(variable);
				fsmInstance.setRelatedASTNode(snode);
				fsmInsTable.put(variable, fsmInstance);
				list.add(fsmInstance);
			}
		}
		return list;
	}

	/** ���������ڵ����Ƿ����״̬����صı��� */
	public static boolean checkSameVariable(List<Node> nodeList,
			FSMMachineInstance fsmInst) {
		Iterator<Node> listItr = nodeList.iterator();
		while (listItr.hasNext()) {
			ASTDirectDeclarator checkNode = (ASTDirectDeclarator) listItr
					.next();
			VariableNameDeclaration variable = checkNode
					.getVariableNameDeclaration();
			if (variable != null && variable == fsmInst.getRelatedVariable()
					&& checkNode == fsmInst.getRelatedASTNode()) {
				return true;
			}
		}
		return false;
	}

	/** ���״̬����ر����Ƿ񱻳�ʼ�� */
	public static boolean checkInial(VexNode checkedVexNode,
			FSMMachineInstance fsmInst) {

		// chh ����**head�ڵ���ʱ��Ϊδ��ʼ������ΪgetTreenode()�����������飬�ں����checkUse�����д���
		SimpleNode releatedTreeNode = checkedVexNode.getTreenode();
		if (checkedVexNode.isBackNode()
				|| checkedVexNode.getName().startsWith("for_head")
				|| checkedVexNode.getName().startsWith("if_head")
				|| checkedVexNode.getName().startsWith("while_head")
				|| checkedVexNode.getName().startsWith("do_while_head")
				|| checkedVexNode.getName().startsWith("switch_head")
				|| checkedVexNode.getName().startsWith("label_head_case")) {// ����β�ڵ���д���
			String mayinitxpath = "./Expression";
			List<SimpleNode> mayinitList = StateMachineUtils
					.getEvaluationResults(releatedTreeNode, mayinitxpath);
			for (SimpleNode tnode : mayinitList) {
				ASTExpression checkExp = (ASTExpression) tnode;
				if (isInit(fsmInst.getRelatedVariable(), checkExp)) {
					fsmInst.setFlag(true);
					fsmInst.setReleatedVexNode(checkedVexNode);
					return true;
				}
			}
			return false;
		}
		if (isInit(fsmInst.getRelatedVariable(), releatedTreeNode)) {
			fsmInst.setFlag(true);
			fsmInst.setReleatedVexNode(checkedVexNode);
			return true;
		}
		return false;
	}

	/**
	 * ���״̬����ر����Ƿ񱻳�ʼ�� ��������ѭ������β�ڵ�while_out��for_out
	 * �ٶ��ܹ�����while/forѭ�������ұ��������г�ʼ���ˣ���ѭ����Ĭ��Ϊ�ñ����ѳ�ʼ
	 * 
	 * ����÷��������ڣ�ֻ����������checkInial����ΪUVF����Ϊ��·�����У����Կ��ܻ���Ϊ��һ��û�н�whileѭ����·���������󱨣�
	 * ·�����в��ᵼ��©���ģ� �Ӹ÷�����Ŀ�Ľ����Ǽ�����Ϊѭ����������̫����
	 * */
	public static boolean checkArrayInial(VexNode checVexNode,
			FSMMachineInstance fsmInst) {
		if (!(checVexNode.getName().startsWith("for_out") || checVexNode
				.getName().startsWith("while_out"))) {
			return false;
		}
		SimpleNode releatedTreeNode = checVexNode.getTreenode();
		VariableNameDeclaration varDecl = fsmInst.getRelatedVariable();

		if (isInit(varDecl, releatedTreeNode)) {
			fsmInst.setFlag(true);
			fsmInst.setReleatedVexNode(checVexNode);
			return true;
		}
		return false;
	}

	public static boolean isInit(VariableNameDeclaration varDecl,
			SimpleNode node) {

		// step1ͨ��ϵͳ������ʼ��
		// step1.1: ͨ������sscanf( const char *, const char *, ...);���г�ʼ��
		String ioInialXpath = ".//PostfixExpression[./PrimaryExpression[@Image='sscanf']]/ArgumentExpressionList/AssignmentExpression[last()]//PostfixExpression[not(./Expression)]/PrimaryExpression[count(*)=0]";
		List<SimpleNode> cinList = StateMachineUtils.getEvaluationResults(node,
				ioInialXpath);
		for (SimpleNode tnode : cinList) {
			ASTPrimaryExpression checkExp = (ASTPrimaryExpression) tnode;
			if (checkExp.getVariableNameDeclaration() == null) {
				continue;
			} else {
				// zx: �жϸýڵ�ɴ�񣬲��ɴ�ֱ�ӷ���false
				if (checkExp.getCurrentVexNode() != null
						&& checkExp.getCurrentVexNode().getContradict()) {
					return false;
				}
				if (checkExp.getVariableNameDeclaration() == varDecl) {
					return true;
				}
			}
		}

		// step1.2 �ȳ���strcpy(dest,src)�еڶ���Ԫ�ز�������г�ʼ����
		// ͬ��strncpy(dest,src)������ڶ���Ԫ�ؽ��г�ʼ������������Ϊ�ڶ�����������ʱ������false
		// step1.3 ͬ��ҲӦ�ó�ȥstrcmp(str1, str2)��strstr(str1, str2)����������������Ӧ�ó�ʼ��
		// add by zl,20111209, step 1.4ͬ��Ӧ�ó�ȥfprintf(stream, "%s%c", s1, c
		// ),����Ӧ��������������ʼ���������г�ʼ��
		String defaultFunList[] = {"strcpy", "strcat", "strncpy", "memcpy", "memmove",
				 "strncat" };//�ڶ�������
	
		String cmpFuncList[] = { "strstr", "strcmp", "strncmp", "memcmp" };
		String fprintfFuncList[] = { "fprintf", "wsprintf" };
		// String wsprintfFuncList[]={"wsprintf"};
		String ioNoInialxPath = ".//PostfixExpression[./ArgumentExpressionList[count(*)>=2]/AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression[@Image='"
				+ varDecl.getImage() + "']]/PrimaryExpression";
		List<SimpleNode> funInialList = StateMachineUtils.getEvaluationResults(
				node, ioNoInialxPath);
		// step1.2 ����defaultFunList��cmpFuncList�еĺ���,����λ���ض��βε�λ�ñ����ʼ����
		// chh ����cmpFuncList���͵ĺ������ж����defaultFunList���ͺ������жϷŵ�һ��forѭ����������ѭ��
		for (SimpleNode idexp : funInialList) {
			ASTPrimaryExpression func = (ASTPrimaryExpression) idexp;
			SimpleNode expList = (SimpleNode) idexp.jjtGetParent().jjtGetChild(
					1);
			if (expList == null) {
				continue;
			}

			String funcName = func.getImage();
			for (String name : defaultFunList) {
				if (funcName.equals(name)) {
					// �ڶ��β�λ�ñ���
					if(expList.jjtGetNumChildren()>=2)
					{
						ASTAssignmentExpression assignNode =(ASTAssignmentExpression) expList.jjtGetChild(1);
						List<SimpleNode> list = StateMachineUtils
								.getEvaluationResults(
										assignNode,
										"./UnaryExpression/PostfixExpression/PrimaryExpression");
						// zys:2010.7.22
						if (list.size() > 0) {
							SimpleNode arg1 = list.get(0);
							if (arg1.getImage().equals(varDecl.getImage())) {
								return false;
							}
						}
					}
					
				}
			}
			for (String name : cmpFuncList) {
				if (funcName.equals(name)) {
					List<SimpleNode> list = StateMachineUtils
							.getEvaluationResults(expList,
									"./AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression");
					SimpleNode arg = list.get(0);// ����λ�ڵ�һ�βε�λ��
					if (arg.getImage().equals(varDecl.getImage())) {
						return false;
					}
					arg = list.get(1);// ����λ�ڵڶ��βε�λ��
					if (arg.getImage().equals(varDecl.getImage())) {
						return false;
					}
				}
			}
			// step3.1.3 ����fprintfFuncList�еĺ���,����λ�ڵ����βε�λ����������βα����ʼ����
			for (String name : fprintfFuncList) {
				if (funcName.equals(name)) {
					int expListChildNum = expList.jjtGetNumChildren();
					for (int j = 2; j < expListChildNum; j++) {
						SimpleNode arg = (SimpleNode) expList.jjtGetChild(j);
						if (arg.getImage().equals(varDecl.getImage())) {
							return false;
						}
					}
				}
			}
			return true;
		}
		// step1.2_end
		// step2.1: ͨ���������ý��г�ʼ�������������ȡ��ַ���������뺯���У���Ĭ�ϸñ����ɵ�ǰ�������г�ʼ��
		// ����: func(&a); scanf("%d", &a); func(&a,
		// 1);�����﷨�����в�ͬ��C++�ض����﷨���(�˴��﷨������)
		String xPath = ".//PostfixExpression[./PrimaryExpression]/ArgumentExpressionList/AssignmentExpression/UnaryExpression[./UnaryOperator[@Operators='&']]/UnaryExpression/PostfixExpression/PrimaryExpression";
		xPath += " | .//PostfixExpression[./PrimaryExpression]/ArgumentExpressionList/AssignmentExpression/CastExpression[./TypeName]/UnaryExpression[./UnaryOperator[@Operators='&']]/UnaryExpression/PostfixExpression/PrimaryExpression";
		funInialList = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode idExp : funInialList) {
			ASTPrimaryExpression snode = (ASTPrimaryExpression) idExp;
			// zx: �жϸýڵ�ɴ�񣬲��ɴ�ֱ�ӷ���false
			if (idExp.getCurrentVexNode() != null
					&& idExp.getCurrentVexNode().getContradict()) {
				// return false;
				continue;
			}
			if (snode.getVariableNameDeclaration() != null
					&& snode.getVariableNameDeclaration() == varDecl) {
				return true;
			}
		}
		// step2.2: ͨ���������ý��г�ʼ���� ��������������ã���Ĭ�ϸñ����ɵ�ǰ�������г�ʼ��
		// ����void func(int &i, int b);
		// ������func(i, 0);ʱĬ��Ϊ��i�����˳�ʼ��(�޴�����)
		// step2.3: int * array;���� int array[10];ʱ
		// func(array, b);����ʱ��arrayΪָ�����ͣ���Ϊfunc���������ʼ��
		xPath = ".//PostfixExpression[./ArgumentExpressionList/AssignmentExpression//UnaryExpression/PostfixExpression/PrimaryExpression[@Image='"
				+ varDecl.getImage() + "']]/PrimaryExpression";
		funInialList = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode idexp : funInialList) {
			// zx: �жϸýڵ�ɴ�񣬲��ɴ�ֱ�ӷ���false
			if (idexp.getCurrentVexNode() != null
					&& idexp.getCurrentVexNode().getContradict()) {
				// return false;
				continue;
			}
			// ��������Ϊ�������ָ��ʱ����Ϊ�����ʼ��
			if (varDecl.getType() instanceof CType_AbstPointer) {
				return true;
			}
		}
		// step3.1:�������AST�������﷨void f(int &)���壬������f(i)ʱ�����Ǹ�i���и�ֵ����
		// ����void func(int &i);�������������
		// ������func(i);ʱĬ��Ϊ��i�����˳�ʼ��(��)
		// zx: step3.2: �������AST�������﷨void f(int
		// *)���壬������f(array)ʱ�����Ǹ�array���и�ֵ����[arrayΪ�����ָ������]
		// ����void func(int *array);

		// step4: ��=����ʾ��ֵ�ı���
		// �ӵ�һ�ж�λ��ת����󣬱���int i; i = i + 1;�������
		String assignInial = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='=']]";
		List<SimpleNode> assignNodeList = StateMachineUtils
				.getEvaluationResults(node, assignInial);
		Iterator<SimpleNode> assignItr = assignNodeList.iterator();
		while (assignItr.hasNext()) {
			ASTAssignmentExpression assignNode = (ASTAssignmentExpression) assignItr
					.next();
			// zx: �жϸýڵ�ɴ�񣬲��ɴֱ�ӷ���false
			if (assignNode.getCurrentVexNode() != null
					&& assignNode.getCurrentVexNode().getContradict()) {
				return false;

			}
			// �ҵ���ֵ���ʽ��ߵ��Ǹ�����
			String inialExpressionXpath = "./UnaryExpression/PostfixExpression[not(./FieldId)]/PrimaryExpression[@Image='"
					+ varDecl.getImage() + "']";
			List<SimpleNode> inialExpList = StateMachineUtils
					.getEvaluationResults(assignNode, inialExpressionXpath);
			Iterator<SimpleNode> itr = inialExpList.iterator();
			while (itr.hasNext()) {
				ASTPrimaryExpression idExp = (ASTPrimaryExpression) itr.next();
				if (idExp.getVariableNameDeclaration() != null
						&& idExp.getVariableNameDeclaration() == varDecl) {
					// zx: �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������i = i + 1;
					// С��int i; i = a.i;�������⡣�ʲ���ֱ��.//id_expression����������
					String sameVariableXpath = ".//UnaryExpression/PostfixExpression[not(./FieldId)]/PrimaryExpression[@Image='"
							+ varDecl.getImage() + "' and count(*)= 0]";
					List<SimpleNode> inialList = StateMachineUtils
							.getEvaluationResults(
									(SimpleNode) assignNode.jjtGetChild(2),
									sameVariableXpath);
					if (inialList != null && inialList.size() > 0) {
						// chh: �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������array[1] = array[0] = 0;
						// ����жϣ��ԡ�array[1] = array[0] = 0;���͡�array[1] =
						// array[0];���ֱ���������Ϊδ��ʼ��ʹ��
						if (varDecl.getType().isPointType()) {
							String othervarxpath = ".//UnaryExpression/PostfixExpression/PrimaryExpression[@Image!='"
									+ varDecl.getImage() + "' ]";
							List<SimpleNode> constantList = StateMachineUtils
									.getEvaluationResults(
											(SimpleNode) ((SimpleNode) inialList
													.get(0)).jjtGetParent()
													.jjtGetParent()
													.jjtGetParent(),
											othervarxpath);
							if (constantList != null
									&& constantList.size() > inialList.size())
								return true;
							else {
								String funpath = ".//UnaryExpression[@Image='sizeof' and @Image='strlen']//UnaryExpression/PostfixExpression[not(./FieldId)]/PrimaryExpression[@Image='"
										+ varDecl.getImage()
										+ "' and count(*)= 0]";
								List<SimpleNode> funList = StateMachineUtils
										.getEvaluationResults(
												(SimpleNode) assignNode
														.jjtGetChild(2),
												funpath);
								if (funList != null
										&& funList.size() >= inialList.size())
									return true;
								else
									return false;
							}

						} else {
							return false;

						}
					}
					return true;
				}
			}

		}
		return false;
	}

	/** ��⵱ǰ������û�г�ʼ�����ͱ�ʹ�� */
	public static boolean checkUse(VexNode checkedVexNode,
			FSMMachineInstance fsmInst) {

		if (checkedVexNode.isBackNode()) {// ����β�ڵ���д���
			return false;
		}
		SimpleNode releatedTreeNode = checkedVexNode.getTreenode();

		// ����ڸÿ������ڵ��϶Ա�����ʼ�����Ͳ�����ʹ�ü�飻
		if (/* fsmInst.getReleatedVexNode() == checkedVexNode && */fsmInst
				.getFlag()) {

			return false;
		} else if (checkInial(checkedVexNode, fsmInst)) {// �����ǰ�ڵ��ʼ������ȡ���ýڵ��״̬��ʵ���Ĺ���
			fsmInst.setFlag(false);
			fsmInst.setReleatedVexNode(null);
			return false;
		}

		// ʹ�ü�鿪ʼ
		String useXpath = ".//UnaryExpression/PostfixExpression[not(./FieldId) and not(./ArgumentExpressionList)]/PrimaryExpression[not(./Constant)]";
		List<SimpleNode> useList = StateMachineUtils.getEvaluationResults(
				releatedTreeNode, useXpath);
		Iterator<SimpleNode> useItr = useList.iterator();
		out: while (useItr.hasNext()) {
			ASTPrimaryExpression checkedNode = (ASTPrimaryExpression) useItr
					.next();
			// chh ���ڱ�����ʹ��ҲӦ�ü��ڵ��Ƿ�ɴ�
			if (checkedNode.getCurrentVexNode() != null
					&& checkedNode.getCurrentVexNode().getContradict()) {
				continue;

			}
			// end
			VariableNameDeclaration varDecl = checkedNode
					.getVariableNameDeclaration();
			if (varDecl == null || varDecl != fsmInst.getRelatedVariable()) {
				continue;
			}
			// chh int i;i&=0x03;�������������Ϊ��ַ���䣬����Ϊδ��ʼ��ʹ��
			if (checkedNode.jjtGetParent().jjtGetParent().jjtGetParent()
					.jjtGetNumChildren() > 1) {
				if (checkedNode.jjtGetParent().jjtGetParent().jjtGetParent()
						.jjtGetChild(1) instanceof ASTAssignmentOperator) {
					if (((ASTAssignmentOperator) (checkedNode.jjtGetParent()
							.jjtGetParent().jjtGetParent().jjtGetChild(1)))
							.getOperators().equals("&=")) {
						continue;
					}
				}
			}
			// end
			SimpleNode parent = (SimpleNode) checkedNode
					.getFirstParentOfType(ASTUnaryExpression.class);
			if (parent != null
					&& parent.jjtGetParent() instanceof ASTUnaryExpression) {
				String opxpath = "./UnaryOperator[@Operators='&']";
				if (StateMachineUtils.getEvaluationResults(
						(SimpleNode) parent.jjtGetParent(), opxpath) != null)
					continue;
			}
			// zx: ����int array[10]; int * p = array;��ʱ��array������δ��ʼ���������������⴦��
			// zx: int * p; p = array; ��������AST�﷨���ṹ��һ��
			if (varDecl.getType() instanceof CType_Array) {
				if (releatedTreeNode instanceof ASTDeclaration) {
					String assignInial = ".//InitDeclaratorList/InitDeclarator/Initializer/AssignmentExpression/UnaryExpression/PostfixExpression[not(./Expression)]/PrimaryExpression[@Image='"
							+ varDecl.getImage() + "' and not(./Constant)]";
					List<SimpleNode> assignNodeList = StateMachineUtils
							.getEvaluationResults(releatedTreeNode, assignInial);
					if (assignNodeList != null && assignNodeList.size() > 0) {
						continue;
					}
				} else {
					String assignInial = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='=']]/AssignmentExpression//UnaryExpression/PostfixExpression[not(./Expression)]/PrimaryExpression[@Image='"
							+ varDecl.getImage() + "' and not(./Constant)]";
					List<SimpleNode> assignNodeList = StateMachineUtils
							.getEvaluationResults(releatedTreeNode, assignInial);
					if (assignNodeList != null && assignNodeList.size() > 0) {
						continue;
					}
				}
			}
			// zx: end

			parent = checkedNode;
			while (!(parent instanceof ASTStatement)) {
				parent = (SimpleNode) parent.jjtGetParent();
				// ��������sizeof(*a)������
				if (parent instanceof ASTUnaryExpression
						&& parent.getImage().equals("sizeof")) {
					continue out;
				}
			}
			/**
			 * chh ����**head�ڵ㣬����getTreenode()�����������飬���û������ļ�⣬��������������ʹ��
			 * ���ʼ���������©��������Ҫ�������б���ʹ�ó��ּ�֮ǰ��statement�ڵ���г�ʼ����⣬�������
			 * ����Щ����г�ʼ��������תend״̬�����򱨸���ϣ�δ��ʼ��ʹ�á�
			 * 
			 */

			if (!(checkedVexNode.getName().startsWith("for_head")
					|| checkedVexNode.getName().startsWith("if_head")
					|| checkedVexNode.getName().startsWith("while_head")
					|| checkedVexNode.getName().startsWith("do_while_head")
					|| checkedVexNode.getName().startsWith("switch_head") || checkedVexNode
					.getName().startsWith("label_head_case"))
					&& (checkedNode
							.getFirstParentOfType(ASTSelectionStatement.class) == null && checkedNode
							.getFirstParentOfType(ASTIterationStatement.class) == null)) {
				fsmInst.setRelatedASTNode(checkedNode);
				fsmInst.setDesp("�ڵ� " + varDecl.getNode().getBeginLine()
						+ " �ж���ı���\"" + varDecl.getImage() + "\"����δ��ʼ���������ʹ�á�");
				return true;
			} else if (checkedVexNode.getName().startsWith("while_head")|| checkedVexNode.getName().startsWith("if_head")) {
				parent = (SimpleNode) checkedNode.jjtGetParent();
				while (parent != releatedTreeNode) {
					parent = (SimpleNode) parent.jjtGetParent();
					if (releatedTreeNode.jjtGetChild(0) instanceof ASTExpression
							&& parent == releatedTreeNode.jjtGetChild(0)) {
						fsmInst.setRelatedASTNode(checkedNode);
						fsmInst.setDesp("�ڵ� "
								+ varDecl.getNode().getBeginLine()
								+ " �ж���ı���\"" + varDecl.getImage()
								+ "\"����δ��ʼ���������ʹ�á�");
						return true;
					}

				}
				continue;
			} else if (!(checkedVexNode.getName().startsWith("for_head")
					|| checkedVexNode.getName().startsWith("if_head")
					|| checkedVexNode.getName().startsWith("while_head")
					|| checkedVexNode.getName().startsWith("do_while_head")
					|| checkedVexNode.getName().startsWith("switch_head") || checkedVexNode
					.getName().startsWith("label_head_case"))
					&& (checkedNode
							.getFirstParentOfType(ASTSelectionStatement.class) != null || checkedNode
							.getFirstParentOfType(ASTIterationStatement.class) != null)) {
				releatedTreeNode = (SimpleNode) ((checkedNode
						.getFirstParentOfType(ASTSelectionStatement.class) != null) ? checkedNode
						.getFirstParentOfType(ASTSelectionStatement.class)
						: checkedNode
								.getFirstParentOfType(ASTIterationStatement.class));
				Node statementList = releatedTreeNode
						.getFirstChildOfType(ASTStatementList.class);
				Node end = checkedNode.getFirstParentOfType(ASTStatement.class);
				if (statementList == null
						|| statementList.jjtGetNumChildren() == 0) {
					fsmInst.setRelatedASTNode(checkedNode);
					fsmInst.setDesp("�ڵ� " + varDecl.getNode().getBeginLine()
							+ " �ж���ı���\"" + varDecl.getImage()
							+ "\"����δ��ʼ���������ʹ�á�");
					return true;
				} else {

					for (int i = 0; i != statementList.jjtGetNumChildren(); i++) {
						Node temp = statementList.jjtGetChild(i);
						if (temp == end) {
							fsmInst.setRelatedASTNode(checkedNode);
							fsmInst.setDesp("�ڵ� "
									+ varDecl.getNode().getBeginLine()
									+ " �ж���ı���\"" + varDecl.getImage()
									+ "\"����δ��ʼ���������ʹ�á�");
							return true;
						}
						if (isInit(varDecl, (SimpleNode) temp)) {
							continue out;
						}

					}
				}
			}
		}
		return false;
	}

}
