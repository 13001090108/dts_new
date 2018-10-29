package softtest.rules.gcc.fault;

import java.util.*;

import softtest.ast.c.*;
import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.gcc.fault.MethodUVF_PPreCondition;
import softtest.summary.gcc.fault.MethodUVF_PPreConditionVisitor;
import softtest.summary.gcc.fault.UVF_PInfo;
import softtest.summary.gcc.fault.UVF_PInfo.UVF_PType;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;

/**
 * @author zl UnInited Variable_Pointer ֻ���Ǽ򵥵�����Ϊָ�����
 */
public class UVF_PStateMachine extends BasicStateMachine {
	private static Set<VariableNameDeclaration> initedAsOne = null;
	private static Set<VariableNameDeclaration> inited = null;
	private static Set<UVF_PCheckInfo> initedVariable=null;
	
	@Override
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodUVF_PPreConditionVisitor.getInstance());
	}

	/** Ϊÿ��û�г�ʼ���ķǾ�̬�����ⲿ�ľֲ���������״̬�� */
	public static List<FSMMachineInstance> createUVF_PStateMachines(SimpleNode node, FSMMachine fsm) {
		initedVariable=new HashSet<UVF_PCheckInfo>();
		initedAsOne = new HashSet<VariableNameDeclaration>();
		inited = new HashSet<VariableNameDeclaration>();
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		Hashtable<VariableNameDeclaration, FSMMachineInstance> fsmInsTable = new Hashtable<VariableNameDeclaration, FSMMachineInstance>();
		/** ���ҵ�ǰ�����е����зǾ�̬�����ⲿ���͵ı�ʹ�õľֲ�ָ������;ֲ�������� */
		String xpath = ".//InitDeclarator[(.//Initializer/AssignmentExpression//UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc'])]/Declarator[(./Pointer)]/DirectDeclarator | .//InitDeclarator[count(*)=1]/Declarator[(./Pointer)]/DirectDeclarator";
		xpath += "|.//Declaration[./DeclarationSpecifiers[not(./StorageClassSpecifier)]]/InitDeclaratorList/InitDeclarator[not(./Initializer)]/Declarator/DirectDeclarator[count(*)!=0]"; 
		xpath+="| .//Declaration[./DeclarationSpecifiers[not(./StorageClassSpecifier)]]/InitDeclaratorList/InitDeclarator[./Initializer/AssignmentExpression/CastExpression//UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc' or @Image='realloc']]/Declarator/DirectDeclarator[count(*)!=0]";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator<SimpleNode> Itr = evaluationResults.iterator();
		while (Itr.hasNext()) {
			SimpleNode checkNode = Itr.next();
			if (null == checkNode|| (checkNode.getCurrentVexNode() != null && checkNode.getCurrentVexNode().getContradict())) {
				continue;
			}
			VariableNameDeclaration variable = checkNode.getVariableNameDeclaration();
			if (variable == null || variable.isParam()) {
				continue;
			}
			if (!(variable.getScope() instanceof LocalScope || variable
					.getScope() instanceof MethodScope)) {
				continue;
			}
			CType type = variable.getType();
			if (null == type
					|| !(type instanceof CType_Pointer || type instanceof CType_Array)
					&& !(type.getSimpleType() instanceof CType_BaseType)) {
				continue;
			}
			if (!(fsmInsTable.containsKey(variable))) {
				FSMMachineInstance fsmInstance = fsm.creatInstance();
				fsmInstance.setRelatedVariable(variable);
				fsmInstance.setRelatedASTNode(checkNode);
				fsmInsTable.put(variable, fsmInstance);
				list.add(fsmInstance);
			}
		}
		return list;
	}

	/** ���״̬����ر����Ƿ񱻳�ʼ�� */
	public static int checkInial(VexNode checkedVexNode,FSMMachineInstance fsmInst) {
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
			List<SimpleNode> mayInialList = StateMachineUtils.getEvaluationResults(releatedTreeNode, mayInialXpath);
			Iterator<SimpleNode> mayInialItr = mayInialList.iterator();
			int result=-1;
			while (mayInialItr.hasNext()) {
				result=checkAsOneMay(fsmInst.getRelatedVariable(), mayInialItr.next());
				if (result!=-1) {
					return result;
				}
			}
			return result;
		}
		return checkAsOne(fsmInst.getRelatedVariable(), releatedTreeNode);
	}

	/**
	 * ���״̬����ر����Ƿ񱻳�ʼ�� ��������ѭ������β�ڵ�while_out��for_out
	 * �ٶ��ܹ�����while/forѭ�������ұ��������г�ʼ���ˣ���ѭ����Ĭ��Ϊ�ñ����ѳ�ʼ
	 * ����÷��������ڣ�ֻ����������checkInial����ΪUVF����Ϊ��·������
	 * �����Կ��ܻ���Ϊ��һ��û�н�whileѭ����·���������󱨣�·�����в��ᵼ��©���ģ� �Ӹ÷�����Ŀ�Ľ����Ǽ�����Ϊѭ����������̫����
	 * */
	public static boolean checkArrayInial(VexNode checVexNode,FSMMachineInstance fsmInst) {
		if (!(checVexNode.getName().startsWith("for_out") || checVexNode.getName().startsWith("while_out"))) {
			return false;
		}
		SimpleNode releatedTreeNode = checVexNode.getTreenode();
		VariableNameDeclaration variable = fsmInst.getRelatedVariable();
		int result=checkAsOne(variable, releatedTreeNode);
		switch(result){
		case 1://��ʼ��
			if(!initedAsOne.contains(variable)){
				initedAsOne.add(variable);	
			}
			fsmInst.setFlag(true);
			return true;
		}
		return false;
	}

	public static int checkAsOneMay(VariableNameDeclaration variable,SimpleNode node) {
		String varImage = variable.getImage();
		if (null == varImage || varImage.equals("")) {
			return -1;
		}
		/**����ʽ*/
		int i=checkEqualExpression(node,variable,varImage);
		if(i!=-1){
			return i;
		}
		/**��麯�����ʽ*/
		i=checkFunctionExpression(node,variable,varImage);
		if(i!=-1){
			return i;
		}
		return i;
	}
	/**
	 * ���ָ��������ڴ�������Ƿ���Ϊ���屻��ʼ�� Ϊ�˼����󱨣���ָ��������·����ڴ�ռ�����ͷ�ָ�����Ϊ����״̬��Ҳ��Ϊָ������Ѿ��������ʼ��
	 * @return int -1|0|1|2 : ����|ʹ��|��ʼ��|���·����ڴ�(����������)
	 */
	public static int checkAsOne(VariableNameDeclaration variable,SimpleNode node) {
		VexNode vexNode = node.getCurrentVexNode();
		SimpleNode treeNode = vexNode.getTreenode();
		String varImage = variable.getImage();
		if (null == treeNode || null == varImage || varImage.equals("")) {
			return -1;
		}
		/**����ʽ*/
		int i=checkEqualExpression(treeNode,variable,varImage);
		if(i!=-1){
			return i;
		}
		/**��麯�����ʽ*/
		i=checkFunctionExpression(treeNode,variable,varImage);
		if(i!=-1){
			return i;
		}
		return i;
	}
	/**
	 * ��麯�����ʽ
	 * @param treeNode
	 * @param variable
	 * @param varImage
	 * @return int -1|0|1|2 : ����|ʹ��|��ʼ��|���·����ڴ�(����������)
	 */
	public static int checkFunctionExpression(SimpleNode simpleNode,VariableNameDeclaration variable, String varImage) {
		//scanf��sscanf
		String xpath = ".//PostfixExpression[(./PrimaryExpression[@Image='scanf' or @Image='sscanf'])]//ArgumentExpressionList/AssignmentExpression[last()]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']";
		List<SimpleNode> list = StateMachineUtils.getEvaluationResults(simpleNode, xpath);
		if(list!=null && list.size()>0) {
			return 1;
		}
		//��һ����Ϊ��ʼ�����ڶ�����Ϊʹ��
		String funcList[] = { "sprintf","fgets","strcpy", "strncpy", "memset","memcpy", "memmove","strcat", "strncat" };
		xpath = ".//PostfixExpression[./ArgumentExpressionList[count(*)>=2]/AssignmentExpression/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']]/PrimaryExpression";
		list = StateMachineUtils.getEvaluationResults(simpleNode, xpath);
		String funcName;
		for(SimpleNode snode:list){
			if(snode.jjtGetParent().jjtGetNumChildren()<2){
				continue;
			}
			SimpleNode expList = (SimpleNode)snode.jjtGetParent().jjtGetChild(1);
			if (null==expList) {
				continue;
			}
			funcName = ((ASTPrimaryExpression)snode).getImage();			
			for(String name : funcList) {
				if(funcName.equals("fgets")){
					if(expList.jjtGetNumChildren()<3){
						continue;
					}
					//��������ʹ��
					List<SimpleNode>  paramlist = StateMachineUtils.getEvaluationResults(expList, "./AssignmentExpression[3]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']");
					if(null!=paramlist && paramlist.size()>0){
							return 0;
					}
					//��һ������ʼ��
					paramlist = StateMachineUtils.getEvaluationResults(expList, "./AssignmentExpression[1]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']");
					if(null!=paramlist && paramlist.size()>0){
							return 1;
					}
				}
				if(funcName.equals(name)) {
					
					if(expList.jjtGetNumChildren()<2){
						continue;
					}
					//�ڶ�����
					List<SimpleNode>  paramlist = StateMachineUtils.getEvaluationResults(expList, "./AssignmentExpression[2]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']");
					if(null!=paramlist && paramlist.size()>0){
							return 0;
					}
					//��һ����
					paramlist = StateMachineUtils.getEvaluationResults(expList, "./AssignmentExpression[1]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']");
					if(null!=paramlist && paramlist.size()>0){
							return 1;
					}
				}
			}
		}
		return -1;
	}

	/**
	 * ����ʽ
	 * @param treeNode
	 * @param variable
	 * @param varImage
	 * @return int -1|0|1|2: ����|ʹ��|��ʼ��|���·����ڴ�(����������)
	 */
	public static int checkEqualExpression(SimpleNode treeNode,VariableNameDeclaration variable, String varImage) {
		String checkXpath=".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='=']]";
		List<SimpleNode> checkList=StateMachineUtils.getEvaluationResults(treeNode, checkXpath);
		Iterator<SimpleNode> checkItr=checkList.iterator();
		while(checkItr.hasNext()){
			ASTAssignmentExpression assignNode=(ASTAssignmentExpression)checkItr.next();
			if(null==assignNode || assignNode.jjtGetNumChildren()!=3){
				continue;
			}
			String leftXpath=".//PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+ varImage + "' ]";
			List<SimpleNode> leftList=StateMachineUtils.getEvaluationResults((SimpleNode)(assignNode.jjtGetChild(0)), leftXpath);
			if(null==leftList || 0==leftList.size()){
				//��ʽ���û��ָ�����
				ASTAssignmentExpression rightExp=(ASTAssignmentExpression) assignNode.jjtGetChild(2);
				if(null==rightExp){
					continue;
				}else if(1==rightExp.jjtGetNumChildren()){
					//��ʽ���û��ָ���������ʽ�ұߵ�ָ��������
					int i=checkRightEqualAsOneWithoutLeft(rightExp,variable,varImage);
					if(i!=-1){
						return i;
					}
				}else if(3==rightExp.jjtGetNumChildren()){
					//��������������ʽ�ұ߻��е�ʽǶ�׵�������� int* a=p=c;
					if(rightExp.jjtGetChild(1) instanceof ASTAssignmentOperator && ((ASTAssignmentOperator)rightExp.jjtGetChild(1)).getImage().equals("=")){
						//���ָ������Ƿ������Ƕ�׵�ʽ�����
						List<SimpleNode> equalList=StateMachineUtils.getEvaluationResults((SimpleNode)(rightExp.jjtGetChild(0)), leftXpath);
						if(null!=equalList && equalList.size()>0){
							//ָ�������Ƕ�׵�ʽ�����
							int i=checkRightEqualAsOneWithLeft((SimpleNode)rightExp.jjtGetChild(2),variable,varImage);
							if(i!=-1){
								return i;
							}
						}
					}
				}
			}else{
				//��ʽ�����ָ��������֣�����ʽ�ұ�
				int i=checkRightEqualAsOneWithLeft((SimpleNode)assignNode.jjtGetChild(2),variable,varImage);
				if(i!=-1){
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * ����ʽ�ұ�ָ����������ʹ�ó�ʼ��,��ʽ��ߺ���ָ�����
	 * @param simpleNode
	 * @param variable
	 * @return int -1|0|1|2: ����|ʹ��|��ʼ��|���·����ڴ�(����������)
	 */
	private static int checkRightEqualAsOneWithLeft(SimpleNode simpleNode,VariableNameDeclaration variable,String varImage) {
		String xpath = ".//UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+ varImage + "' ]";
		List<SimpleNode> list = StateMachineUtils.getEvaluationResults(simpleNode, xpath);
		//��ʽ�ұ���ָ���������
		if(null==list || list.size()==0){
			String reallocXpath="./UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc' or @Image='realloc']";
			reallocXpath+="| ./CastExpression//UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc' or @Image='realloc']";
			List<SimpleNode> reallocList = StateMachineUtils.getEvaluationResults(simpleNode, reallocXpath);
			if(null!=reallocList && reallocList.size()>0){
				return 2;
			}
			return 1;
		}
		//��ʽ�ұ���ָ���������
		Iterator<SimpleNode> equalItr = list.iterator();
		while (equalItr.hasNext()) {
			SimpleNode checkNode = equalItr.next();
			//realloc��������������a=(int *)realloc(p,10*sizeof(int));
			String reallocXpath = ".//UnaryExpression/PostfixExpression[./PrimaryExpression[@Image='realloc']]/ArgumentExpressionList//UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+ varImage + "' ]";
			List<SimpleNode> reallocList = StateMachineUtils.getEvaluationResults(simpleNode,reallocXpath);
			Iterator<SimpleNode> reallocItr=reallocList.iterator();
			while(reallocItr.hasNext()){
				if(checkNode.equals(reallocItr.next())){
					return 2;
				}
			}
		}
		return -1;
	}

	/**
	 * ����ʽ�ұ�ָ����������ʹ�ó�ʼ������ʽ���û��ָ�����
	 * @param simpleNode
	 * @param variable
	 * @return int -1|0|1|2: ����|ʹ��|��ʼ��|���·����ڴ�(����������)
	 */
	public static int checkRightEqualAsOneWithoutLeft(SimpleNode simpleNode,VariableNameDeclaration variable,String varImage) {
		/**���realloc���*/
		String reallocXpath = ".//UnaryExpression/PostfixExpression[./PrimaryExpression[@Image='realloc']]/ArgumentExpressionList/AssignmentExpression[1]//UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+ varImage + "' ]";
		List<SimpleNode> reallocList=StateMachineUtils.getEvaluationResults(simpleNode, reallocXpath);
		if(null!=reallocList && reallocList.size()>0){
			return 2;
		}
		return -1;
	}

	/** ��⵱ǰָ��������ڴ��������û�г�ʼ�����ͱ�ʹ�� */
	public static boolean checkUse(VexNode vexNode, FSMMachineInstance fsmInst) {
		/** ����β�ڵ���д��� */
		if (vexNode.isBackNode()) {
			return false;
		}
		SimpleNode treeNode = vexNode.getTreenode();
		VariableNameDeclaration variable=fsmInst.getRelatedVariable();
		if (null == treeNode || null==variable) {
			return false;
		}
		String varImage = variable.getImage();
		/** ����ڸÿ������ڵ��϶�ָ����������ʼ�����Ͳ�����ʹ�ü�� */ 
		if (null == varImage || varImage.equals("") || fsmInst.getFlag()) {
			return false;
		}
		for(UVF_PCheckInfo inited:initedVariable){
			if(varImage.equals(inited.getVariable().getImage()) && inited.isInitedAsOne()){
				return false;
			}
		}
		int result=checkInial(vexNode, fsmInst);
		switch(result){
		case 0://ʹ��
			boolean flag=false;
			for(UVF_PCheckInfo inited:initedVariable){
				if(varImage.equals(inited.getVariable().getImage())){
					flag=true;
					break;
				}
			}
			if(!flag){
			fsmInst.setRelatedASTNode(treeNode);
			fsmInst.setDesp("�ڵ� " + variable.getNode().getBeginLine() + " �ж���ı���\""+ varImage + "\"�����Ա�ڵ�"+treeNode.getBeginLine()+"�п���δ��ʼ���������ʹ�á�");		
			return true;
			}
			break;
		case 1://��ʼ��
			fsmInst.setFlag(true);
			for(UVF_PCheckInfo inited:initedVariable){
				if(varImage.equals(inited.getVariable().getImage()) && !inited.isInitedAsOne()){
					inited.setInitedAsOne(true);
					return false;
				}
			}
			initedVariable.add(new UVF_PCheckInfo(variable,true));
			return false;
		case 2://���·����ڴ�(����������)
			return false;
		}
		//��⺯������
		UVF_PInfo uvf_pInfo=checkMethod(vexNode.getTreenode(),variable);
		if(null != uvf_pInfo){
			addDomain(variable,uvf_pInfo.getInitedomain());
			IntegerDomain needInitedDomain=uvf_pInfo.getNeedInitedomain();
			if(!needInitedDomain.isEmpty()){
				//�鿴�������Ƿ��Ѿ�����ʼ��
				for(UVF_PCheckInfo inited:initedVariable){
					//�ҵ��Ѿ���ʼ��������
					if(varImage.equals(inited.getVariable().getImage())){
						if(inited.isInitedAsOne() || inited.getInitedDomian().contains(needInitedDomain)){
							return false;
						}
					}
				}
				fsmInst.setRelatedASTNode(treeNode);
				fsmInst.setDesp("�ڵ� " + variable.getNode().getBeginLine() + " �ж���ı���\""+ varImage + "\"�����Ա�ڵ�"+treeNode.getBeginLine()+"�п���δ��ʼ���������ʹ�á�");		
				return true;
			}
		}
		
		/**����Ƿ�ָ����������Ա�Ƿ�Ϊʹ��*/
		String useXpath = ".//UnaryExpression/PostfixExpression[count(*)=2 and ./PrimaryExpression[(@Image='"+ varImage + "') and not(./Constant)] and not(./FieldId) and not(./ArgumentExpressionList) and (./Expression)]";
		List<SimpleNode> useList = StateMachineUtils.getEvaluationResults(treeNode, useXpath);
		Iterator<SimpleNode> useItr = useList.iterator();
		while (useItr.hasNext()) {
			ASTPostfixExpression checkNode = (ASTPostfixExpression) useItr.next();
			if (null == checkNode || (checkNode.getCurrentVexNode() != null && checkNode.getCurrentVexNode().getContradict()) || checkNode.jjtGetNumChildren()!=2 || !(checkNode.jjtGetChild(1) instanceof ASTExpression)) {
				continue;
			}
			//��ȡ��������Ա����
			VariableNameDeclaration checkVariable=checkNode.getVariableDecl();
			if(null==checkVariable ||inited.contains(checkVariable)){//TODO ���϶Կɱ��±�Ĵ���
				continue;
			}
			String checkVarImage=checkVariable.getImage();
			if(null==checkVarImage || checkVarImage.equals("")){
				continue;
			}
			//��ȡ�����б������±��ȡֵ����
			ASTExpression expressionNode=(ASTExpression)checkNode.jjtGetChild(1);
			if(expressionNode==null){
				continue;
			}
			ExpressionVistorData exprdata = new ExpressionVistorData();
			exprdata.currentvex=expressionNode.getCurrentVexNode();
			exprdata.sideeffect=true;
				
			ExpressionValueVisitor eVisitor=new ExpressionValueVisitor();
			exprdata=(ExpressionVistorData)eVisitor.visit((ASTAssignmentExpression)expressionNode.getFirstDirectChildOfType(ASTAssignmentExpression.class), exprdata);									
			Domain domain=exprdata.value.getDomain(exprdata.currentvex.getSymDomainset());
			//vDomain���ǿɱ��±�Ŀ���ȡֵ��Χ
			IntegerDomain vDomain=Domain.castToIntegerDomain(domain);
			//�鿴�������Ƿ��Ѿ�����ʼ��
			if(vDomain!=null){
				for(UVF_PCheckInfo inited:initedVariable){
					//�ҵ��Ѿ���ʼ��������
					if(varImage.equals(inited.getVariable().getImage())){
						if(inited.isInitedAsOne() || inited.getInitedDomian().contains(vDomain)){
							return false;
						}
					}
				}
			}
			//������Ա�����Ƿ��ʼ��
			if (isInit(checkVariable, (SimpleNode) checkNode,treeNode)) {/**��������ʼ��*/
				//���������б����Ķ�Ӧ������
				addDomain(variable,vDomain);
				continue;
			}
			//������Ա������ʹ��
			/** p[0]&=0x03;�������������Ϊ��ַ���䣬����Ϊδ��ʼ��ʹ�� */
			if (checkNode.jjtGetParent().jjtGetParent().jjtGetParent().jjtGetNumChildren() > 1) {
				if (checkNode.jjtGetParent().jjtGetParent().jjtGetParent().jjtGetChild(1) instanceof ASTAssignmentOperator) {
					if (((ASTAssignmentOperator) (checkNode.jjtGetParent().jjtGetParent().jjtGetParent().jjtGetChild(1))).getOperators().equals("&=")) {
						continue;
					}
				}
			}
			SimpleNode parent = (SimpleNode) checkNode.getFirstParentOfType(ASTUnaryExpression.class);
			if(parent!=null&&parent.jjtGetParent() instanceof ASTUnaryExpression ){
				String opxpath = "./UnaryOperator[@Operators='&']";
				if(StateMachineUtils.getEvaluationResults((SimpleNode) parent.jjtGetParent(), opxpath)!=null)
					continue;
			}
			parent = checkNode;
			boolean flag=false;
			while (!(parent instanceof ASTStatement)) {
				parent = (SimpleNode)parent.jjtGetParent();
				/**��������sizeof(*a)������*/
				if (parent instanceof ASTUnaryExpression && parent.getImage().equals("sizeof")) {
					flag=true;
					break;
				}
			}
			if(flag){
				continue;
			}
			/**����**head�ڵ㣬����getTreenode()�����������飬���û������ļ�⣬��������������ʹ��
			 * ���ʼ���������©��������Ҫ�������б���ʹ�ó��ּ�֮ǰ��statement�ڵ���г�ʼ����⣬�������
			 * ����Щ����г�ʼ��������תend״̬�����򱨸���ϣ�δ��ʼ��ʹ�á�
			 */
			if(!(vexNode.getName().startsWith("for_head")||vexNode.getName().startsWith("if_head")
					||vexNode.getName().startsWith("while_head")||vexNode.getName().startsWith("do_while_head")
					||vexNode.getName().startsWith("switch_head")||vexNode.getName().startsWith("label_head_case"))
					&&(checkNode.getFirstParentOfType(ASTSelectionStatement.class)==null && checkNode.getFirstParentOfType(ASTIterationStatement.class)==null))
			{
				fsmInst.setRelatedASTNode(checkNode);
				fsmInst.setDesp("�ڵ� " + variable.getNode().getBeginLine() + " �ж���ı���\""+ varImage + "\"�����Ա����\""+ checkVarImage + "\"�ڵ�"+checkNode.getBeginLine()+"�п���δ��ʼ���������ʹ�á�");		
				return true;
			}
			else if(vexNode.getName().startsWith("while_head") ||vexNode.getName().startsWith("if_head")){
				parent = (SimpleNode) checkNode.jjtGetParent();
				while (parent != treeNode){
					parent = (SimpleNode) parent.jjtGetParent();
					if(treeNode.jjtGetChild(0) instanceof ASTExpression && parent == treeNode.jjtGetChild(0)){
						fsmInst.setRelatedASTNode(checkNode);
						fsmInst.setDesp("�ڵ� " + variable.getNode().getBeginLine() + " �ж���ı���\""+ varImage + "\"�����Ա����\""+ checkVarImage + "\"�ڵ�"+checkNode.getBeginLine()+"�п���δ��ʼ���������ʹ�á�");		
						return true;
					}
						
				}
				continue;
			}
			else if(!(vexNode.getName().startsWith("for_head")||vexNode.getName().startsWith("if_head")
					||vexNode.getName().startsWith("while_head")||vexNode.getName().startsWith("do_while_head")
					||vexNode.getName().startsWith("switch_head")||vexNode.getName().startsWith("label_head_case"))
					&&(checkNode.getFirstParentOfType(ASTSelectionStatement.class)!=null||checkNode.getFirstParentOfType(ASTIterationStatement.class)!=null)){
				treeNode = (SimpleNode) ((checkNode.getFirstParentOfType(ASTSelectionStatement.class)!=null)?checkNode.getFirstParentOfType(ASTSelectionStatement.class):checkNode.getFirstParentOfType(ASTIterationStatement.class));
				Node statementList=treeNode.getFirstChildOfType(ASTStatementList.class);
				Node end=checkNode.getFirstParentOfType(ASTStatement.class);
				if(statementList==null||statementList.jjtGetNumChildren()==0)
					{
					fsmInst.setRelatedASTNode(checkNode);
					fsmInst.setDesp("�ڵ� " + variable.getNode().getBeginLine() + " �ж���ı���\""+ varImage + "\"�����Ա����\""+ checkVarImage + "\"�ڵ�"+checkNode.getBeginLine()+"�п���δ��ʼ���������ʹ�á�");		
					return true;
					}
				else{
				for(int i=0;i!=statementList.jjtGetNumChildren();i++)
				{
					Node temp=statementList.jjtGetChild(i);
					if(temp == end ) 
						{
							fsmInst.setRelatedASTNode(checkNode);
							fsmInst.setDesp("�ڵ� " + variable.getNode().getBeginLine() + " �ж���ı���\""+ varImage + "\"�����Ա����\""+ checkVarImage + "\"�ڵ�"+checkNode.getBeginLine()+"�п���δ��ʼ���������ʹ�á�");		
							return true;
						}
				}
			}
			}
		}
		//����Ѿ���P[0]���г�ʼ�����򲻼��*p
		IntegerDomain vDomain=new IntegerDomain(0,0);
		//�鿴�������Ƿ��Ѿ�����ʼ��
		for(UVF_PCheckInfo inited:initedVariable){
			//�ҵ��Ѿ���ʼ��������
			if(varImage.equals(inited.getVariable().getImage())){
				if(inited.isInitedAsOne() || inited.getInitedDomian().contains(vDomain)){
					return false;
				}
			}
		}
		//������Ա����*p
		useXpath=".//UnaryExpression[(./UnaryOperator[@Operators='*'])]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']";
		useList = StateMachineUtils.getEvaluationResults(treeNode, useXpath);
		useItr = useList.iterator();
		while (useItr.hasNext()) {
			ASTUnaryExpression unary=(ASTUnaryExpression) ((SimpleNode) useItr.next().getFirstParentOfType(ASTUnaryExpression.class)).getFirstParentOfType(ASTUnaryExpression.class);
			if (null == unary || (unary.getCurrentVexNode() != null && unary.getCurrentVexNode().getContradict())) {
				continue;
			}
			VariableNameDeclaration checkVariable = unary.getVariableDecl();
			if (isInitUnary(checkVariable, (SimpleNode) unary,treeNode)) {/**��������ʼ��*/
				//���������б����Ķ�Ӧ������
				addDomain(variable,vDomain);
				continue;
			}
			if(!(vexNode.getName().startsWith("for_head")||vexNode.getName().startsWith("if_head")
					||vexNode.getName().startsWith("while_head")||vexNode.getName().startsWith("do_while_head")
					||vexNode.getName().startsWith("switch_head")||vexNode.getName().startsWith("label_head_case"))
					&&(unary.getFirstParentOfType(ASTSelectionStatement.class)==null&& unary.getFirstParentOfType(ASTIterationStatement.class)==null))
			{
				SimpleNode parent = unary;
				while (!(parent instanceof ASTStatement)) {
					parent = (SimpleNode)parent.jjtGetParent();
					/**��������sizeof(*a)������*/
					if (parent instanceof ASTUnaryExpression && parent.getImage().equals("sizeof")) {
						return false;
					}
				}
				fsmInst.setRelatedASTNode(unary);
				fsmInst.setDesp("�ڵ� " + variable.getNode().getBeginLine() + " �ж���ı���\""+ variable.getImage() + "\"�ĳ�Ա����"+unary.getImage()+"�ڵ�"+unary.getBeginLine()+"�п���δ��ʼ���������ʹ�á�");		
				return true;
			}
		}
		return false;
	}
	private static boolean isInitUnary(VariableNameDeclaration varDecl,SimpleNode checkNode, SimpleNode treeNode) {
		/** step1 ͨ��ϵͳ������ʼ�� */
		/** step1.1: ͨ������sscanf( const char *, const char *, ...);���г�ʼ�� */
		String ioInialXpath = ".//PostfixExpression[./PrimaryExpression[@Image='sscanf']]/ArgumentExpressionList/AssignmentExpression[last()]//PostfixExpression";
		List<SimpleNode> cinList = StateMachineUtils.getEvaluationResults(treeNode, ioInialXpath);
		for (SimpleNode tnode : cinList) {
			if (tnode.getCurrentVexNode() != null
					&& tnode.getCurrentVexNode().getContradict()) {
				continue;
			}
			if (checkNode.equals(tnode)) {
				return true;
			}
		}

		/**
		 * step1.2 �ȳ���strcpy(dest,src)�еڶ���Ԫ�ز�������г�ʼ����
		 * ͬ��strncpy(dest,src)������ڶ���Ԫ�ؽ��г�ʼ������������Ϊ�ڶ�����������ʱ������false step1.3
		 * ͬ��ҲӦ�ó�ȥstrcmp(str1, str2)��strstr(str1, str2)����������������Ӧ�ó�ʼ�� step
		 * 1.4ͬ��Ӧ�ó�ȥfprintf(stream, "%s%c", s1, c ),����Ӧ��������������ʼ���������г�ʼ��
		 */
		/** step1.2 ����defaultFunList��cmpFuncList�еĺ���,����λ���ض��βε�λ�ñ����ʼ���� */
		String defaultFunList[] = { "strcpy", "strncpy", "memcpy", "memmove",
				"strcat", "strncat" };
		String cmpFuncList[] = { "strstr", "strcmp", "strncmp", "memcmp" };
		String fprintfFuncList[] = { "fprintf", "wsprintf" };
		String ioNoInialxPath = ".//PostfixExpression[./ArgumentExpressionList[count(*)>=2]/AssignmentExpression/UnaryExpression[@Image='"
				+ varDecl.getImage() + "']]/PrimaryExpression";
		List<SimpleNode> funInialList = StateMachineUtils.getEvaluationResults(
				treeNode, ioNoInialxPath);
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
					/** �ڶ��β�λ�ñ��� */
					List<SimpleNode> list = StateMachineUtils
							.getEvaluationResults(expList,
									"./AssignmentExpression[last()]/UnaryExpression");
					if (list.size() > 0) {
						SimpleNode arg1 = list.get(0);
						if (arg1.getImage().equals(varDecl.getImage())) {
							return false;
						}
					}
				}
			}
			for (String name : cmpFuncList) {
				if (funcName.equals(name)) {
					List<SimpleNode> list = StateMachineUtils
							.getEvaluationResults(expList,
									"./AssignmentExpression/UnaryExpression");
					SimpleNode arg = list.get(0);
					/** ����λ�ڵ�һ�βε�λ�� */
					if (arg.getImage().equals(varDecl.getImage())) {
						return false;
					}
					arg = list.get(1);
					/** ����λ�ڵڶ��βε�λ�� */
					if (arg.getImage().equals(varDecl.getImage())) {
						return false;
					}
				}
			}
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
		/**
		 * step2.2: ͨ���������ý��г�ʼ���� ��������������ã���Ĭ�ϸñ����ɵ�ǰ�������г�ʼ��, ����void func(int &i,
		 * int b);������func(i, 0);ʱĬ��Ϊ��i�����˳�ʼ��(�޴�����) step2.3: int * array;���� int
		 * array[10];ʱ,func(array, b);����ʱ��arrayΪָ�����ͣ���Ϊfunc���������ʼ��
		 */
		String xPath = ".//PostfixExpression[./ArgumentExpressionList/AssignmentExpression//UnaryExpression[@Image='"
				+ varDecl.getImage() + "']]/PrimaryExpression";
		funInialList = StateMachineUtils.getEvaluationResults(treeNode, xPath);
		for (SimpleNode idexp : funInialList) {
			if (idexp.getCurrentVexNode() != null
					&& idexp.getCurrentVexNode().getContradict()) {
				continue;
			}
			/** ��������Ϊ�������ָ��ʱ����Ϊ�����ʼ�� */
			if (varDecl.getType() instanceof CType_AbstPointer) {
				return true;
			}
		}
		/**
		 * step3.1:�������AST�������﷨void f(int &)���壬������f(i)ʱ�����Ǹ�i���и�ֵ���� //����void
		 * func(int &i);������������� //������func(i);ʱĬ��Ϊ��i�����˳�ʼ��(��) //zx: step3.2:
		 * �������AST�������﷨void f(int
		 * *)���壬������f(array)ʱ�����Ǹ�array���и�ֵ����[arrayΪ�����ָ������]
		 */
		// ����void func(int *array);
		// step4: ��=����ʾ��ֵ�ı���
		// �ӵ�һ�ж�λ��ת����󣬱���int i; i = i + 1;�������
		String assignInial = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='=']]";
		List<SimpleNode> assignNodeList = StateMachineUtils
				.getEvaluationResults(treeNode, assignInial);
		Iterator<SimpleNode> assignItr = assignNodeList.iterator();
		while (assignItr.hasNext()) {
			ASTAssignmentExpression assignNode = (ASTAssignmentExpression) assignItr
					.next();
			if (assignNode.getCurrentVexNode() != null
					&& assignNode.getCurrentVexNode().getContradict()) {
				continue;
			}
			/** �ҵ���ֵ���ʽ��ߵ��Ǹ����� */
			String inialExpressionXpath = "./UnaryExpression[@Image='"+ varDecl.getImage() + "']";
			List<SimpleNode> inialExpList = StateMachineUtils
					.getEvaluationResults(assignNode, inialExpressionXpath);
			Iterator<SimpleNode> itr = inialExpList.iterator();
			while (itr.hasNext()) {
				SimpleNode idExp = itr.next();
				if (idExp.equals(checkNode)) {
					/** �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������i = i + 1; */
					String sameVariableXpath = ".//UnaryExpression[@Image='"
							+ varDecl.getImage() + "']";
					List<SimpleNode> inialList = StateMachineUtils
							.getEvaluationResults(
									(SimpleNode) assignNode.jjtGetChild(2),
									sameVariableXpath);
					if (inialList != null && inialList.size() > 0) {
						/**
						 * �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������array[1] = array[0] = 0;
						 * //����жϣ��ԡ�array[1] = array[0] = 0;���͡�array[1] =
						 * array[0];���ֱ���������Ϊδ��ʼ��ʹ��
						 */
						String othervarxpath = ".//UnaryExpression[@Image!='"
								+ varDecl.getImage() + "' ]";
						List<SimpleNode> constantList = StateMachineUtils
								.getEvaluationResults(
										(SimpleNode) ((SimpleNode) inialList
												.get(0)).jjtGetParent()
												.jjtGetParent().jjtGetParent(),
										othervarxpath);
						if (constantList != null
								&& constantList.size() > inialList.size())
							return true;
						else {
							String funpath = ".//UnaryExpression[@Image='sizeof' and @Image='strlen']//UnaryExpression/PostfixExpression[not(./FieldId)]/PrimaryExpression[@Image='"
									+ varDecl.getImage() + "' and count(*)= 0]";
							List<SimpleNode> funList = StateMachineUtils
									.getEvaluationResults(
											(SimpleNode) assignNode
													.jjtGetChild(2), funpath);
							if (funList != null
									&& funList.size() >= inialList.size())
								return true;
							else
								return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}

//����Զ��庯�����õ�
public static UVF_PInfo checkMethod(SimpleNode simpleNode,VariableNameDeclaration variable) {
	if(simpleNode==null || variable==null){
		return null;
	}
	String varImage=variable.getImage();
	if(varImage==null || varImage.equals("")){
		return null;
	}
	String xpath=".//UnaryExpression/PostfixExpression[./ArgumentExpressionList//AssignmentExpression/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']]/PrimaryExpression[@Method='true']";
	List<SimpleNode> list = StateMachineUtils.getEvaluationResults(simpleNode, xpath);
	int expNum=0;
	int paramIndex=0;
	List<SimpleNode> varList;
	for(SimpleNode snode:list){
		ASTPostfixExpression checkPost=(ASTPostfixExpression) snode.getFirstParentOfType(ASTPostfixExpression.class);
		if(checkPost==null || checkPost.jjtGetNumChildren()<2){
			continue;
		}
		ASTArgumentExpressionList expList=(ASTArgumentExpressionList) checkPost.getFirstChildOfType(ASTArgumentExpressionList.class);
		if(expList==null || expList.jjtGetNumChildren()==0){
			continue;
		}
		expNum=expList.jjtGetNumChildren();
		for(int i=0;i<expNum;i++){
			SimpleNode checkParam=(SimpleNode) expList.jjtGetChild(i);
			if(checkParam==null || checkParam.jjtGetNumChildren()==0){
				continue;
			}
			varList=StateMachineUtils.getEvaluationResults(checkParam, "./UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']");
			if(varList!=null && varList.size()>0){
					paramIndex=i;
					break;
			}
			paramIndex=-1;
		}
		if(paramIndex==-1){
			continue;
		}
		MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(snode);
		if(methodDecl == null || methodDecl.getMethodSummary() == null)
			 continue; 
		//����ú����й���ָ�����UVF_Pǰ����Ϣ 
		MethodUVF_PPreCondition uvf_pfeature = (MethodUVF_PPreCondition)methodDecl.getMethodSummary().findMethodFeature(MethodUVF_PPreCondition.class); 
		if (uvf_pfeature == null) 
			continue;
		Set<VariableNameDeclaration> variables = uvf_pfeature.getUVF_PVariables(); 
		for(VariableNameDeclaration var1 : variables) {
			UVF_PInfo uvf_pInfo = uvf_pfeature.getUVF_PInfo(var1);
			if(paramIndex==uvf_pInfo.getParamIndex()) {
				return uvf_pInfo;
			}
		}
	}
	//ff(&a);
	xpath=".//UnaryExpression/PostfixExpression[./ArgumentExpressionList//AssignmentExpression/UnaryExpression[./UnaryOperator[@Operators='&']]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression[@Image='"+varImage+"']]/PrimaryExpression";
	list = StateMachineUtils.getEvaluationResults(simpleNode, xpath);
	if(list!=null && list.size()>0){
		return new UVF_PInfo(variable.getImage(), -10, UVF_PType.NOT_NEED,IntegerDomain.getEmptyDomain(),IntegerDomain.getFullDomain());
	}
		return null;
	}

/**
 * �������б�������������кϲ�
 * @param variable
 * @param vDomain
 * @return true|false
 */
private static boolean addDomain(VariableNameDeclaration variable,IntegerDomain vDomain) {
	if(null==variable || null==vDomain){
		return false;
	}
	String varImage=variable.getImage();
	for(UVF_PCheckInfo inited:initedVariable){
		//�ҵ��Ѿ���ʼ��������
		if(varImage.equals(inited.getVariable().getImage())){
			if(inited.isInitedAsOne() ||inited.getInitedDomian().contains(vDomain)){
				return true;
			}
			inited.getInitedDomian().mergeWith(vDomain.jointoOneInterval());
			return true;
		}
	}
	//û���ҵ��Ѿ���ʼ��������
	initedVariable.add(new UVF_PCheckInfo(variable,vDomain));
	return true;
}
	/** ������Ա�����Ƿ񱻳�ʼ�� */
	public static boolean isInit(VariableNameDeclaration varDecl,SimpleNode checkNode, SimpleNode treeNode) {
		/** step1 ͨ��ϵͳ������ʼ�� */
		/** step1.1: ͨ������sscanf( const char *, const char *, ...);���г�ʼ�� */
		String ioInialXpath = ".//PostfixExpression[./PrimaryExpression[@Image='sscanf']]/ArgumentExpressionList/AssignmentExpression[last()]//PostfixExpression";
		List<SimpleNode> cinList = StateMachineUtils.getEvaluationResults(treeNode, ioInialXpath);
		for (SimpleNode tnode : cinList) {
			if (tnode.getCurrentVexNode() != null
					&& tnode.getCurrentVexNode().getContradict()) {
				continue;
			}
			if (checkNode.equals(tnode)) {
				return true;
			}
		}

		/**
		 * step1.2 �ȳ���strcpy(dest,src)�еڶ���Ԫ�ز�������г�ʼ����
		 * ͬ��strncpy(dest,src)������ڶ���Ԫ�ؽ��г�ʼ������������Ϊ�ڶ�����������ʱ������false step1.3
		 * ͬ��ҲӦ�ó�ȥstrcmp(str1, str2)��strstr(str1, str2)����������������Ӧ�ó�ʼ�� step
		 * 1.4ͬ��Ӧ�ó�ȥfprintf(stream, "%s%c", s1, c ),����Ӧ��������������ʼ���������г�ʼ��
		 */
		/** step1.2 ����defaultFunList��cmpFuncList�еĺ���,����λ���ض��βε�λ�ñ����ʼ���� */
		String defaultFunList[] = { "strcpy", "strncpy", "memcpy", "memmove",
				"strcat", "strncat" };
		String cmpFuncList[] = { "strstr", "strcmp", "strncmp", "memcmp" };
		String fprintfFuncList[] = { "fprintf", "wsprintf" };
		String ioNoInialxPath = ".//PostfixExpression[./ArgumentExpressionList[count(*)>=2]/AssignmentExpression/UnaryExpression/PostfixExpression[@Image='"
				+ varDecl.getImage() + "']]/PrimaryExpression";
		List<SimpleNode> funInialList = StateMachineUtils.getEvaluationResults(
				treeNode, ioNoInialxPath);
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
					/** �ڶ��β�λ�ñ��� */
					List<SimpleNode> list = StateMachineUtils
							.getEvaluationResults(expList,
									"./AssignmentExpression[last()]/UnaryExpression/PostfixExpression");
					if (list.size() > 0) {
						SimpleNode arg1 = list.get(0);
						if (arg1.getImage().equals(varDecl.getImage())) {
							return false;
						}
					}
				}
			}
			for (String name : cmpFuncList) {
				if (funcName.equals(name)) {
					List<SimpleNode> list = StateMachineUtils
							.getEvaluationResults(expList,
									"./AssignmentExpression/UnaryExpression/PostfixExpression");
					SimpleNode arg = list.get(0);
					/** ����λ�ڵ�һ�βε�λ�� */
					if (arg.getImage().equals(varDecl.getImage())) {
						return false;
					}
					arg = list.get(1);
					/** ����λ�ڵڶ��βε�λ�� */
					if (arg.getImage().equals(varDecl.getImage())) {
						return false;
					}
				}
			}
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
		/**
		 * step2.1: ͨ���������ý��г�ʼ�������������ȡ��ַ���������뺯���У���Ĭ�ϸñ����ɵ�ǰ�������г�ʼ�� ����: func(&a);
		 * scanf("%d", &a); func(&a, 1);�����﷨�����в�ͬ��C++�ض����﷨���(�˴��﷨������)
		 */
		String xPath = ".//PostfixExpression[./PrimaryExpression]/ArgumentExpressionList/AssignmentExpression/UnaryExpression[./UnaryOperator[@Operators='&']]/UnaryExpression/PostfixExpression";
		xPath += " | .//PostfixExpression[./PrimaryExpression]/ArgumentExpressionList/AssignmentExpression/CastExpression[./TypeName]/UnaryExpression[./UnaryOperator[@Operators='&']]/UnaryExpression/PostfixExpression";
		funInialList = StateMachineUtils.getEvaluationResults(treeNode, xPath);
		for (SimpleNode idExp : funInialList) {
			if (idExp.getCurrentVexNode() != null
					&& idExp.getCurrentVexNode().getContradict()) {
				continue;
			}
			if (idExp.equals(checkNode)) {
				return true;
			}
		}
		/**
		 * step2.2: ͨ���������ý��г�ʼ���� ��������������ã���Ĭ�ϸñ����ɵ�ǰ�������г�ʼ��, ����void func(int &i,
		 * int b);������func(i, 0);ʱĬ��Ϊ��i�����˳�ʼ��(�޴�����) step2.3: int * array;���� int
		 * array[10];ʱ,func(array, b);����ʱ��arrayΪָ�����ͣ���Ϊfunc���������ʼ��
		 */
		xPath = ".//PostfixExpression[./ArgumentExpressionList/AssignmentExpression//UnaryExpression/PostfixExpression[@Image='"
				+ varDecl.getImage() + "']]/PrimaryExpression";
		funInialList = StateMachineUtils.getEvaluationResults(treeNode, xPath);
		for (SimpleNode idexp : funInialList) {
			if (idexp.getCurrentVexNode() != null
					&& idexp.getCurrentVexNode().getContradict()) {
				continue;
			}
			/** ��������Ϊ�������ָ��ʱ����Ϊ�����ʼ�� */
			if (varDecl.getType() instanceof CType_AbstPointer) {
				return true;
			}
		}
		/**
		 * step3.1:�������AST�������﷨void f(int &)���壬������f(i)ʱ�����Ǹ�i���и�ֵ���� //����void
		 * func(int &i);������������� //������func(i);ʱĬ��Ϊ��i�����˳�ʼ��(��) //zx: step3.2:
		 * �������AST�������﷨void f(int
		 * *)���壬������f(array)ʱ�����Ǹ�array���и�ֵ����[arrayΪ�����ָ������]
		 */
		// ����void func(int *array);
		// step4: ��=����ʾ��ֵ�ı���
		// �ӵ�һ�ж�λ��ת����󣬱���int i; i = i + 1;�������
		String assignInial = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='=']]";
		List<SimpleNode> assignNodeList = StateMachineUtils
				.getEvaluationResults(treeNode, assignInial);
		Iterator<SimpleNode> assignItr = assignNodeList.iterator();
		while (assignItr.hasNext()) {
			ASTAssignmentExpression assignNode = (ASTAssignmentExpression) assignItr
					.next();
			if (assignNode.getCurrentVexNode() != null
					&& assignNode.getCurrentVexNode().getContradict()) {
				continue;
			}
			/** �ҵ���ֵ���ʽ��ߵ��Ǹ����� */
			String inialExpressionXpath = "./UnaryExpression/PostfixExpression[@Image='"+ varDecl.getImage() + "']";
			List<SimpleNode> inialExpList = StateMachineUtils
					.getEvaluationResults(assignNode, inialExpressionXpath);
			Iterator<SimpleNode> itr = inialExpList.iterator();
			while (itr.hasNext()) {
				SimpleNode idExp = itr.next();
				if (idExp.equals(checkNode)) {
					/** �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������i = i + 1; */
					String sameVariableXpath = ".//UnaryExpression/PostfixExpression[@Image='"
							+ varDecl.getImage() + "']";
					List<SimpleNode> inialList = StateMachineUtils
							.getEvaluationResults(
									(SimpleNode) assignNode.jjtGetChild(2),
									sameVariableXpath);
					if (inialList != null && inialList.size() > 0) {
						/**
						 * �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������array[1] = array[0] = 0;
						 * //����жϣ��ԡ�array[1] = array[0] = 0;���͡�array[1] =
						 * array[0];���ֱ���������Ϊδ��ʼ��ʹ��
						 */
						String othervarxpath = ".//UnaryExpression/PostfixExpression[@Image!='"
								+ varDecl.getImage() + "' ]";
						List<SimpleNode> constantList = StateMachineUtils
								.getEvaluationResults(
										(SimpleNode) ((SimpleNode) inialList
												.get(0)).jjtGetParent()
												.jjtGetParent().jjtGetParent(),
										othervarxpath);
						if (constantList != null
								&& constantList.size() > inialList.size())
							return true;
						else {
							String funpath = ".//UnaryExpression[@Image='sizeof' and @Image='strlen']//UnaryExpression/PostfixExpression[not(./FieldId)]/PrimaryExpression[@Image='"
									+ varDecl.getImage() + "' and count(*)= 0]";
							List<SimpleNode> funList = StateMachineUtils
									.getEvaluationResults(
											(SimpleNode) assignNode
													.jjtGetChild(2), funpath);
							if (funList != null
									&& funList.size() >= inialList.size())
								return true;
							else
								return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}
}
class UVF_PCheckInfo{
	/**�Ѿ�����ʼ��������*/
	private IntegerDomain initedDomian;
	/**ָ�����*/
	private VariableNameDeclaration variable;
	/**ָ������Ƿ������ʼ��*/
	private boolean initedAsOne;
	
	public UVF_PCheckInfo(VariableNameDeclaration variable){
		initedDomian=new IntegerDomain();
		this.variable=variable;
		initedAsOne=false;
	}
	public UVF_PCheckInfo(VariableNameDeclaration variable,boolean initedAsOne){
		initedDomian=new IntegerDomain();
		this.variable=variable;
		this.initedAsOne=initedAsOne;
	}
	public UVF_PCheckInfo(VariableNameDeclaration variable,IntegerDomain domain){
		initedDomian=domain;
		this.variable=variable;
		this.initedAsOne=false;
	}
	public IntegerDomain getInitedDomian() {
		return initedDomian;
	}
	public void setInitedDomian(IntegerDomain initedDomian) {
		this.initedDomian = initedDomian;
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
