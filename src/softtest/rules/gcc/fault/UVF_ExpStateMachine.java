package softtest.rules.gcc.fault;


import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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
 * @author zl
 * UnInited Variable[δ��ʼ������]
 * */
public class UVF_ExpStateMachine {
	private static Hashtable<VariableNameDeclaration,VariableNameDeclaration> varTable=new Hashtable<VariableNameDeclaration,VariableNameDeclaration>();
	/**Ϊÿ��û�г�ʼ���ķǾ�̬�����ⲿ�ľֲ���������״̬��*/
	public static List<FSMMachineInstance> createUVF_ExpStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		Hashtable<VariableNameDeclaration, FSMMachineInstance> fsmInsTable = new Hashtable<VariableNameDeclaration, FSMMachineInstance>();
		   
		 //���ҵ�ǰ�����е����зǾ�̬�����ⲿ���͵ľֲ�����, û�г�ʼ�����ʽDeclarationList/
		String xpath = ".//Declaration[./DeclarationSpecifiers[not(./StorageClassSpecifier)]]/InitDeclaratorList/InitDeclarator[not(./Initializer)]/Declarator/DirectDeclarator"; 
		xpath+="| .//ExpressionStatement/Expression/AssignmentExpression[./AssignmentOperator and ./AssignmentExpression/CastExpression/UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc' or @Image='realloc']]/UnaryExpression/PostfixExpression/PrimaryExpression";
		xpath+="| .//Declaration[./DeclarationSpecifiers[not(./StorageClassSpecifier)]]/InitDeclaratorList/InitDeclarator[./Initializer/AssignmentExpression//UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc' or @Image='realloc' ]]/Declarator/DirectDeclarator";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		/**Ϊ��ǰ��ֵ��ı�������״̬��ʵ��*/
		Iterator<SimpleNode> itr = evaluationResults.iterator();
		while(itr.hasNext()) {
			SimpleNode snode = itr.next();
			VariableNameDeclaration variable=null;
			if(snode instanceof ASTDirectDeclarator){
				ASTDirectDeclarator primaryNode = (ASTDirectDeclarator)snode;
				variable = primaryNode.getVariableNameDeclaration();
			}else if(snode instanceof ASTPrimaryExpression){
				ASTPrimaryExpression priExp=(ASTPrimaryExpression)snode;
				variable = priExp.getVariableNameDeclaration();
			}
			if(variable == null) {
				continue;
			}
			CType type = variable.getType();
			if(!((type instanceof CType_Struct) || (type instanceof CType_Pointer) || (type instanceof CType_Typedef))){
				continue;
			}
			if(type instanceof CType_Pointer)	{
				if(!((((CType_Pointer)type).getOriginaltype() instanceof CType_Struct) || (((CType_Pointer)type).getOriginaltype() instanceof CType_Typedef))){
					continue;
				}
				if(((CType_Pointer)type).getOriginaltype() instanceof CType_Typedef){
					if(!(((CType_Typedef)((CType_Pointer)type).getOriginaltype()).getOriginaltype() instanceof CType_Struct)){
						continue;
					}
				}
			}
			if(type instanceof CType_Typedef)	{
				if(!(((CType_Typedef)type).getOriginaltype() instanceof CType_Struct)){
					continue;
				}				
			}
			ArrayList<VariableNameDeclaration> variablemems=variable.mems;
			Iterator<VariableNameDeclaration> itrvariablemems=variablemems.iterator();
			while(itrvariablemems.hasNext()){
				//dongyk 20120420 �������쳣
				try
				{
					VariableNameDeclaration memsvariable=itrvariablemems.next();
					CType memsvariabletype=memsvariable.getType();
					if(memsvariabletype instanceof CType_Struct || (memsvariabletype instanceof CType_Typedef && (((CType_Typedef)memsvariabletype).getOriginaltype() instanceof CType_Struct))){
						ArrayList<VariableNameDeclaration> variablememsmem=memsvariable.mems;
						Iterator<VariableNameDeclaration> itrvariablememsmem=variablememsmem.iterator();
						while(itrvariablememsmem.hasNext()){
							variablemems.add(itrvariablememsmem.next());
						}
					}
				}catch(ConcurrentModificationException eCon)
				{
					break;
				}
			}
			Iterator<VariableNameDeclaration> itrvariablemems1=variablemems.iterator();
			while(itrvariablemems1.hasNext()){
				VariableNameDeclaration memsvariable=itrvariablemems1.next();
				CType memsvariabletype=memsvariable.getType();
				//����ṹ���Ա����Ҳ�Ǹ����������͵Ļ�
				if(!(memsvariabletype instanceof CType_BaseType || memsvariabletype instanceof CType_Array || memsvariabletype instanceof CType_Pointer)) {
					continue;
				}
				//��������ֻ���ǣ������Ա�Ǽ����͵ı���
		    	if (memsvariabletype instanceof CType_Array && !(((CType_Array)memsvariabletype).getOriginaltype() instanceof CType_BaseType)) {
		    		continue;
		    	}
		    	//��Ϊ��ǰ�����оֲ���������״̬��ʵ��
		    	if(!(memsvariable.getScope() instanceof LocalScope || memsvariable.getScope() instanceof MethodScope)){
		    		continue;
		    	}
		    	if (memsvariable.isParam()) {
		    		continue;
		    	}
		    	//��ӳ�Ա�������丸����֮��Ĺ�ϵ
		    	if(!varTable.containsKey(memsvariable)){
		    		varTable.put(memsvariable, variable);
		    	}
		    	if(!(fsmInsTable.containsKey(memsvariable))){
		    		FSMMachineInstance fsmInstance = fsm.creatInstance();
					fsmInstance.setRelatedVariable(memsvariable);
					fsmInstance.setRelatedASTNode(snode);
					fsmInsTable.put(memsvariable, fsmInstance);
					list.add(fsmInstance);
			}
		}
		}
		return list;
	}
	
	/**���������ڵ����Ƿ����״̬����صı���*/
	public static boolean checkSameVariable(List<Node> nodeList, FSMMachineInstance fsmInst) {
		Iterator<Node> listItr = nodeList.iterator();
		while(listItr.hasNext()) {
			SimpleNode checkNode=(SimpleNode)listItr.next();
			VariableNameDeclaration variable = checkNode.getVariableNameDeclaration(); 
			if(variable != null && variable ==  (fsmInst.getRelatedASTNode()).getVariableNameDeclaration() && checkNode == fsmInst.getRelatedASTNode()) {
				//VariableNameDeclaration aavariable = fsmInst.getRelatedVariable();
				//ASTDirectDeclarator aacheckNode =(ASTDirectDeclarator) fsmInst.getRelatedASTNode();
				//if(checkNode!=aacheckNode)
					//return false;
				//VariableNameDeclaration aabbvariable= ((ASTDirectDeclarator) fsmInst.getRelatedASTNode()).getVariableNameDeclaration();
				return true;
			}			
		}
		return false;
	}
	
	/**���״̬����ر����Ƿ񱻳�ʼ��*/
	public static boolean checkInial(VexNode checkedVexNode, FSMMachineInstance fsmInst) {
		
		//chh  ����**head�ڵ���ʱ��Ϊδ��ʼ������ΪgetTreenode()�����������飬�ں����checkUse�����д���
		SimpleNode releatedTreeNode = checkedVexNode.getTreenode();
		if(checkedVexNode.isBackNode()||
				checkedVexNode.getName().startsWith("for_head")||checkedVexNode.getName().startsWith("if_head")
				||checkedVexNode.getName().startsWith("while_head")||checkedVexNode.getName().startsWith("do_while_head")
				||checkedVexNode.getName().startsWith("switch_head")||checkedVexNode.getName().startsWith("label_head_case")){//����β�ڵ���д���
			String mayinitxpath = "./Expression";
			List<SimpleNode> mayinitList = StateMachineUtils.getEvaluationResults(releatedTreeNode,mayinitxpath);
			for( SimpleNode tnode : mayinitList){			
				ASTExpression checkExp = (ASTExpression)tnode;
				if(isInit(fsmInst.getRelatedVariable(), checkExp)){
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
	 * ���״̬����ر����Ƿ񱻳�ʼ��
	 * ��������ѭ������β�ڵ�while_out��for_out
	 * �ٶ��ܹ�����while/forѭ�������ұ��������г�ʼ���ˣ���ѭ����Ĭ��Ϊ�ñ����ѳ�ʼ
	 * 
	 * ����÷��������ڣ�ֻ����������checkInial����ΪUVF����Ϊ��·�����У����Կ��ܻ���Ϊ��һ��û�н�whileѭ����·���������󱨣�·�����в��ᵼ��©���ģ�
	 * �Ӹ÷�����Ŀ�Ľ����Ǽ�����Ϊѭ����������̫����
	 * */
	public static boolean checkArrayInial(VexNode checVexNode, FSMMachineInstance fsmInst) {
		if (!(checVexNode.getName().startsWith("for_out") || checVexNode.getName().startsWith("while_out"))) {
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
	

	public static boolean isInit(VariableNameDeclaration varDecl, SimpleNode node) {
		if(varDecl==null){
			return false;
		}
		//add by zl 20120904,��ӳ�Ա�����ĸ���������С��ʼ���жϵ�����
		if(!varTable.containsKey(varDecl)){
			return false;
		}
		VariableNameDeclaration varDeclParent=varTable.get(varDecl);
		if(null==varDeclParent){
			return false;
		}
		String varDeclName=varDecl.getImage();
		if(varDeclName==null){
			return false;
		}
		String mainVarName=varDeclName.split("->")[0];
		/**���ڵ��Ƿ�Խṹ�����������г�ʼ��*/
		String mainVarXpath=".//Expression/AssignmentExpression[./AssignmentOperator]/UnaryExpression/PostfixExpression/PrimaryExpression[@Image='"+mainVarName+"']";
		List<SimpleNode> mainVarList = StateMachineUtils.getEvaluationResults(node,mainVarXpath);	
		if(mainVarList!=null && mainVarList.size()>0){
			return true;
		}
		
		//step1ͨ��ϵͳ������ʼ��
		
		//step1.1: ͨ������sscanf( const char *, const char *, ...);���г�ʼ��
		String ioInialXpath = ".//PostfixExpression[./PrimaryExpression[@Image='sscanf']]/ArgumentExpressionList/AssignmentExpression[last()]//PostfixExpression[not(./Expression)]/PrimaryExpression[count(*)=0]";
		List<SimpleNode> cinList = StateMachineUtils.getEvaluationResults(node,ioInialXpath);	
		for( SimpleNode tnode : cinList){			
			ASTPostfixExpression checkExp = (ASTPostfixExpression)(tnode.jjtGetParent());
			if(checkExp.getVariableNameDeclaration() == null){
				continue;
			}else{
				//zx: �жϸýڵ�ɴ�񣬲��ɴ�ֱ�ӷ���false
				if(checkExp.getCurrentVexNode() != null && checkExp.getCurrentVexNode().getContradict()) {
					return false;
				}
				if(checkExp.getVariableNameDeclaration() == varDecl) {
					return true;
				}
			}
		}	
		String memsetXpath=".//PostfixExpression[./ArgumentExpressionList[count(*)=3]]/PrimaryExpression[@Image='memset']";
		List<SimpleNode> memsetList= StateMachineUtils.getEvaluationResults(node,memsetXpath);	
		for(SimpleNode tnode:memsetList){
			ASTPostfixExpression checkExp = (ASTPostfixExpression)(tnode.jjtGetParent());
			if(checkExp.getCurrentVexNode() != null && checkExp.getCurrentVexNode().getContradict()) {
				return false;
			}
			String aaxpath=".//UnaryExpression/PostfixExpression[@Image='"+varDecl.getNode().getImage()+"']";
			List<SimpleNode> aaList= StateMachineUtils.getEvaluationResults((SimpleNode)checkExp.jjtGetChild(1).jjtGetChild(0),aaxpath);	
			if(aaList!=null && aaList.size()>0){
				return true;
			}
		}
		//step1.2 �ȳ���strcpy(dest,src)�еڶ���Ԫ�ز�������г�ʼ����
		//ͬ��strncpy(dest,src)������ڶ���Ԫ�ؽ��г�ʼ������������Ϊ�ڶ�����������ʱ������false
		//step1.3 ͬ��ҲӦ�ó�ȥstrcmp(str1, str2)��strstr(str1, str2)����������������Ӧ�ó�ʼ��
		String defaultFunList[] = {"strcpy", "strncpy","memcpy", "memmove", "strcat", "strncat"};
		String cmpFuncList[] = {"strstr", "strcmp", "strncmp", "memcmp"};
		  
		String ioNoInialxPath=".//PostfixExpression[./ArgumentExpressionList[count(*)=2]/AssignmentExpression/UnaryExpression/PostfixExpression[@Image='"+varDecl.getImage()+"']]/PrimaryExpression";
		//String ioNoInialxPath=".//PostfixExpression[./ArgumentExpressionList[count(*)=2]/AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression[@Image='"+varDecl.getImage()+"']]/PrimaryExpression";
		List<SimpleNode> funInialList = StateMachineUtils.getEvaluationResults(node, ioNoInialxPath);
		//step1.2  ����defaultFunList��cmpFuncList�еĺ���,����λ���ض��βε�λ�ñ����ʼ����
		//chh  ����cmpFuncList���͵ĺ������ж����defaultFunList���ͺ������жϷŵ�һ��forѭ����������ѭ��
		for (SimpleNode idexp : funInialList) {	 
			ASTPrimaryExpression func = (ASTPrimaryExpression)idexp;
			SimpleNode expList = (SimpleNode) idexp.jjtGetParent().jjtGetChild(1);
			if (expList == null) {
				continue;
			}
			
			String funcName = func.getImage();			
			for(String name : defaultFunList) {
				if(funcName.equals(name)) {
					//�ڶ��β�λ�ñ���
					List<SimpleNode>  list = StateMachineUtils.getEvaluationResults(expList, "./AssignmentExpression[last()]/UnaryExpression/PostfixExpression/PrimaryExpression");
					//zys:2010.7.22	
					if(list.size()>0){
						SimpleNode arg1 = list.get(0);
						SimpleNode aaarg1=(SimpleNode)arg1.jjtGetParent();
						if(aaarg1.getImage().equals(varDecl.getImage())) {
							return false;
						}
					}
				}
			}
			for(String name : cmpFuncList) {
				if(funcName.equals(name)) {
					List<SimpleNode>  list = StateMachineUtils.getEvaluationResults(expList, "./AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression");
					SimpleNode arg = list.get(0);//����λ�ڵ�һ�βε�λ��
					SimpleNode aaarg=(SimpleNode)arg.jjtGetParent();
					if(aaarg.getImage().equals(varDecl.getImage())) {
						return false;
					}
					arg = list.get(1);//����λ�ڵڶ��βε�λ��
					SimpleNode aarg=(SimpleNode)arg.jjtGetParent();
					if(aarg.getImage().equals(varDecl.getImage())) {
						return false;
					}
				}
			}
			return true;
		}
		//step1.2_end
		
		
		// step2.1: ͨ���������ý��г�ʼ�������������ȡ��ַ���������뺯���У���Ĭ�ϸñ����ɵ�ǰ�������г�ʼ��
		// ����: func(&a); scanf("%d", &a); func(&a, 1);�����﷨�����в�ͬ��C++�ض����﷨���(�˴��﷨������)
		String xPath = ".//PostfixExpression[./PrimaryExpression]/ArgumentExpressionList/AssignmentExpression/UnaryExpression[./UnaryOperator[@Operators='&']]/UnaryExpression/PostfixExpression";///PrimaryExpression";
		funInialList = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode idExp : funInialList) {
			ASTPostfixExpression snode = (ASTPostfixExpression)idExp;
			//zx: �жϸýڵ�ɴ�񣬲��ɴ�ֱ�ӷ���false
			if(idExp.getCurrentVexNode()!= null && idExp.getCurrentVexNode().getContradict()) {
				//return false;
				continue;
			}
			if (snode.getVariableNameDeclaration() != null) {
				if (snode.getVariableNameDeclaration() == varDeclParent || snode.getVariableNameDeclaration() == varDecl) {
					return true;
				}
			}
		}
		
		
		//step2.2: ͨ���������ý��г�ʼ���� ��������������ã���Ĭ�ϸñ����ɵ�ǰ�������г�ʼ��
		//����void func(int &i, int b);
		//������func(i, 0);ʱĬ��Ϊ��i�����˳�ʼ��(�޴�����)
		//step2.3: int * array;���� int array[10];ʱ
		//func(array, b);����ʱ��arrayΪָ�����ͣ���Ϊfunc���������ʼ��
		xPath=".//PostfixExpression[./ArgumentExpressionList/AssignmentExpression//UnaryExpression/PostfixExpression[@Image='" + varDecl.getImage() + "']]/PrimaryExpression";
		funInialList = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode idexp : funInialList) {
			//zx: �жϸýڵ�ɴ�񣬲��ɴ�ֱ�ӷ���false
			if(idexp.getCurrentVexNode()!= null && idexp.getCurrentVexNode().getContradict()) {
				//return false;
				continue;
			}
			// ��������Ϊ�������ָ��ʱ����Ϊ�����ʼ��
			if (varDecl.getType() instanceof CType_AbstPointer) { 
				return true;
			}
		}
		
		
		//step3.1:�������AST�������﷨void f(int &)���壬������f(i)ʱ�����Ǹ�i���и�ֵ����
		//����void func(int &i);�������������
		//������func(i);ʱĬ��Ϊ��i�����˳�ʼ��(��)
		//zx: step3.2: �������AST�������﷨void f(int *)���壬������f(array)ʱ�����Ǹ�array���и�ֵ����[arrayΪ�����ָ������]
		//����void func(int *array);
		
		
		//step4: ��=����ʾ��ֵ�ı���
		// �ӵ�һ�ж�λ��ת����󣬱���int i; i = i + 1;�������
		String assignInial = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='=']]";
		List<SimpleNode> assignNodeList = StateMachineUtils.getEvaluationResults(node, assignInial);
		Iterator<SimpleNode> assignItr = assignNodeList.iterator();
		while(assignItr.hasNext()){
			ASTAssignmentExpression assignNode = (ASTAssignmentExpression) assignItr.next();
			//zx: �жϸýڵ�ɴ�񣬲��ɴֱ�ӷ���false
			if(assignNode.getCurrentVexNode() != null && assignNode.getCurrentVexNode().getContradict()) {
				return false;
			}
			//�ҵ���ֵ���ʽ��ߵ��Ǹ�����
			String inialExpressionXpath = "./UnaryExpression/PostfixExpression[@Image='"+ varDecl.getImage() +"' or @Image='"+varDeclParent.getImage()+"']";
			List<SimpleNode> inialExpList = StateMachineUtils.getEvaluationResults(assignNode, inialExpressionXpath);
			Iterator<SimpleNode> itr = inialExpList.iterator();
			while(itr.hasNext()){
			ASTPostfixExpression idExp = (ASTPostfixExpression) itr.next();
			if (idExp.getVariableDecl() != null) {
				if (idExp.getVariableDecl().equals(varDecl)) {
					//zx: �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������i = i + 1;
					//С��int i; i = a.i;�������⡣�ʲ���ֱ��.//id_expression����������
					String sameVariableXpath = ".//UnaryExpression/PostfixExpression[@Image='"+ varDecl.getImage() +"' and count(*)= 0]";
					List<SimpleNode> inialList = StateMachineUtils.getEvaluationResults((SimpleNode)assignNode.jjtGetChild(2), sameVariableXpath);
					if(inialList != null && inialList.size() > 0) {
						//chh: �жϡ�=���ұ��Ƿ�Ҳ������ͬһ����������array[1] = array[0] = 0;
						//����жϣ��ԡ�array[1] = array[0] = 0;���͡�array[1] = array[0];���ֱ���������Ϊδ��ʼ��ʹ��
						if(varDecl.getType().isPointType()) {
							String othervarxpath=".//UnaryExpression/PostfixExpression[@Image!='"+ varDecl.getImage() +"' ]";
							List<SimpleNode> constantList=StateMachineUtils.getEvaluationResults((SimpleNode)((SimpleNode)inialList.get(0)).jjtGetParent().jjtGetParent(), othervarxpath);
							if(constantList!=null&&constantList.size()>inialList.size())
								return true;
							else {
								String funpath=".//UnaryExpression[@Image='sizeof' and @Image='strlen']//UnaryExpression/PostfixExpression[@Image='"+ varDecl.getImage() +"' and count(*)= 0]";
								List<SimpleNode> funList =StateMachineUtils.getEvaluationResults((SimpleNode)assignNode.jjtGetChild(2), funpath);
								if(funList!=null&&funList.size()>=inialList.size())
									return  true;
								else 
									return false;
							}
							
						} else {
							return false;
							
						}
					}
					return true;
				}else if(idExp.getVariableDecl().equals(varDeclParent)){
					return true;
				}
				}
			}
		
		}
		return false;
	}
	
	
	/**��⵱ǰ������û�г�ʼ�����ͱ�ʹ��*/
	public static boolean checkUse(VexNode checkedVexNode, FSMMachineInstance fsmInst){
		
		if(checkedVexNode.isBackNode() ){//����β�ڵ���д���
			return false;
		}
		SimpleNode releatedTreeNode = checkedVexNode.getTreenode();	
		
		//����ڸÿ������ڵ��϶Ա�����ʼ�����Ͳ�����ʹ�ü�飻
		if (fsmInst.getFlag()){
			
			return false;
		} else if (checkInial(checkedVexNode,fsmInst)){//�����ǰ�ڵ��ʼ������ȡ���ýڵ��״̬��ʵ���Ĺ���
			fsmInst.setFlag(false);
			fsmInst.setReleatedVexNode(null);
			return false;
		}
		
		//ʹ�ü�鿪ʼ
		//String useXpath = ".//UnaryExpression/PostfixExpression[not(./FieldId) and not(./ArgumentExpressionList)]/PrimaryExpression[not(./Constant)]";
		String useXpath=".//UnaryExpression/PostfixExpression[./FieldId and not(./ArgumentExpressionList) and not(./Constant) and ./PrimaryExpression[@DescendantDepth='0']]";//�ṹ���Ա��ʹ�ýڵ�
		List<SimpleNode> useList = StateMachineUtils.getEvaluationResults(releatedTreeNode,useXpath);
		Iterator<SimpleNode> useItr = useList.iterator();
out:	while(useItr.hasNext()){
			ASTPostfixExpression checkedNode = (ASTPostfixExpression)useItr.next();
			//chh  ���ڱ�����ʹ��ҲӦ�ü��ڵ��Ƿ�ɴ�
			if(checkedNode.getCurrentVexNode() != null && checkedNode.getCurrentVexNode().getContradict()) {
				continue;
				
			}
			//end
			if(checkedNode.jjtGetChild(1) instanceof ASTFieldId){
				VariableNameDeclaration varDecl =((ASTFieldId)(checkedNode.jjtGetChild(1))).getVariableNameDeclaration();
				String varImage=checkedNode.getImage();
				String relatedVariableImage=fsmInst.getRelatedVariable().getImage();
				if(varImage == null || !(varImage.equals(relatedVariableImage))){
					continue;
				}
				//chh   int i;i&=0x03;�������������Ϊ��ַ���䣬����Ϊδ��ʼ��ʹ��
				if(checkedNode.jjtGetParent().jjtGetParent().jjtGetNumChildren()>1)
				{if(checkedNode.jjtGetParent().jjtGetParent().jjtGetChild(1) instanceof ASTAssignmentOperator)
				{
					if(((ASTAssignmentOperator)(checkedNode.jjtGetParent().jjtGetParent().jjtGetChild(1))).getOperators().equals("&="))
					{
						continue;
					}
				}}
				//end
				SimpleNode parent = (SimpleNode) checkedNode.getFirstParentOfType(ASTUnaryExpression.class);
				if(parent!=null&&parent.jjtGetParent() instanceof ASTUnaryExpression ){
					String opxpath = "./UnaryOperator[@Operators='&']";
					if(StateMachineUtils.getEvaluationResults((SimpleNode) parent.jjtGetParent(), opxpath)!=null)
						continue;
				}
				//zx: ����int array[10]; int * p = array;��ʱ��array������δ��ʼ���������������⴦��
				//zx: int * p; p = array; ��������AST�﷨���ṹ��һ��
				if(varDecl!=null && varDecl.getType() instanceof CType_Array) {
					if(releatedTreeNode instanceof ASTDeclaration) {
						//String assignInial = ".//InitDeclaratorList/InitDeclarator/Initializer/AssignmentExpression/UnaryExpression/PostfixExpression[not(./Expression)]/PrimaryExpression[@Image='"+ varDecl.getImage() +"' and not(./Constant)]";
						String assignInial = ".//InitDeclaratorList/InitDeclarator/Initializer/AssignmentExpression/UnaryExpression/PostfixExpression[not(./Expression) and @Image='"+ varDecl.getImage() +"' and ./PrimaryExpression[@DescendantDepth='0']]";
						List<SimpleNode> assignNodeList = StateMachineUtils.getEvaluationResults(releatedTreeNode, assignInial);
						if(assignNodeList != null && assignNodeList.size() > 0) {
							continue;
							}
						} else {
							String assignInial = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='=']]/AssignmentExpression//UnaryExpression/PostfixExpression[not(./Expression) and @Image='"+ varDecl.getImage() +"' and ./PrimaryExpression[@DescendantDepth='0']]";
							List<SimpleNode> assignNodeList = StateMachineUtils.getEvaluationResults(releatedTreeNode, assignInial);
							if(assignNodeList != null && assignNodeList.size() > 0) {
								continue;
							}
						}
				}
				//zx: end
						
				parent = checkedNode;
				while (!(parent instanceof ASTStatement)) {
					parent = (SimpleNode)parent.jjtGetParent();
					// ��������sizeof(*a)������
					if (parent instanceof ASTUnaryExpression && parent.getImage().equals("sizeof")) {
						continue out;
					}
				}
				/**chh  ����**head�ڵ㣬����getTreenode()�����������飬���û������ļ�⣬��������������ʹ��
				 * ���ʼ���������©��������Ҫ�������б���ʹ�ó��ּ�֮ǰ��statement�ڵ���г�ʼ����⣬�������
				 * ����Щ����г�ʼ��������תend״̬�����򱨸���ϣ�δ��ʼ��ʹ�á�
				 * 
				 */
				
				if(!(checkedVexNode.getName().startsWith("for_head")||checkedVexNode.getName().startsWith("if_head")
						||checkedVexNode.getName().startsWith("while_head")||checkedVexNode.getName().startsWith("do_while_head")
						||checkedVexNode.getName().startsWith("switch_head")||checkedVexNode.getName().startsWith("label_head_case"))
						&&(checkedNode.getFirstParentOfType(ASTSelectionStatement.class)==null&&checkedNode.getFirstParentOfType(ASTIterationStatement.class)==null))
				{
				fsmInst.setRelatedASTNode(checkedNode);
				fsmInst.setDesp("�ڵ� " + fsmInst.getRelatedASTNode().getBeginLine()+ " �еĸ��ӽṹ����\""+ ((ASTPrimaryExpression)checkedNode.jjtGetChild(0)).getImage()+ "\"�ĳ�Ա����\""+ varImage + "\"����δ��ʼ���������ʹ�á�");		
				return true;
				}
				else if(checkedVexNode.getName().startsWith("while_head")){
					parent = (SimpleNode) checkedNode.jjtGetParent();
					while (parent != releatedTreeNode){
						parent = (SimpleNode) parent.jjtGetParent();
						if(releatedTreeNode.jjtGetChild(0) instanceof ASTExpression && parent == releatedTreeNode.jjtGetChild(0)){
							fsmInst.setRelatedASTNode(checkedNode);
							fsmInst.setDesp("�ڵ� " + fsmInst.getRelatedASTNode().getBeginLine()+ " �еĸ��ӽṹ����\""+ ((ASTPrimaryExpression)checkedNode.jjtGetChild(0)).getImage()+ "\"�ĳ�Ա����\""+ varImage + "\"����δ��ʼ���������ʹ�á�");		
							return true;
						}
							
					}
					continue;
				}
				else if(!(checkedVexNode.getName().startsWith("for_head")||checkedVexNode.getName().startsWith("if_head")
						||checkedVexNode.getName().startsWith("while_head")||checkedVexNode.getName().startsWith("do_while_head")
						||checkedVexNode.getName().startsWith("switch_head")||checkedVexNode.getName().startsWith("label_head_case"))
						&&(checkedNode.getFirstParentOfType(ASTSelectionStatement.class)!=null||checkedNode.getFirstParentOfType(ASTIterationStatement.class)!=null)){
					releatedTreeNode = (SimpleNode) ((checkedNode.getFirstParentOfType(ASTSelectionStatement.class)!=null)?checkedNode.getFirstParentOfType(ASTSelectionStatement.class):checkedNode.getFirstParentOfType(ASTIterationStatement.class));
					Node statementList=releatedTreeNode.getFirstChildOfType(ASTStatementList.class);
					Node end=checkedNode.getFirstParentOfType(ASTStatement.class);
					if(statementList==null||statementList.jjtGetNumChildren()==0)
						{
						fsmInst.setRelatedASTNode(checkedNode);
						fsmInst.setDesp("�ڵ� " + fsmInst.getRelatedASTNode().getBeginLine()+ " �еĸ��ӽṹ����\""+ ((ASTPrimaryExpression)checkedNode.jjtGetChild(0)).getImage()+ "\"�ĳ�Ա����\""+ varImage + "\"����δ��ʼ���������ʹ�á�");		
						return true;
						}
					else{
					
					for(int i=0;i!=statementList.jjtGetNumChildren();i++)
					{
						Node temp=statementList.jjtGetChild(i);
						if(temp == end ) 
							{
								fsmInst.setRelatedASTNode(checkedNode);
								String expVar=null;
								if(((ASTPrimaryExpression)checkedNode.jjtGetChild(0))!=null){
									expVar=((ASTPrimaryExpression)checkedNode.jjtGetChild(0)).getImage();
								}
								fsmInst.setDesp("�ڵ� " + fsmInst.getRelatedASTNode().getBeginLine()+ " �еĸ��ӽṹ����\""+ expVar+ "\"�ĳ�Ա����\""+ varImage + "\"����δ��ʼ���������ʹ�á�");		
								return true;
							}
						if(isInit(varDecl,(SimpleNode)temp))
						{
							continue out;
						}

					}
				}
				}
			}
		}
		return false;
	}

}

