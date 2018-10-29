package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.Method;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.Type.CType_BaseType;


/** 
 * @author nieminhui
 * Void Function In Expression
 * ��ֹvoid���͵Ĺ������ڱ��ʽ��ʹ�� (���̵�����)
 */
public class VFIEStateMachine {	
	//�ҵ����еĸ�ֵ���ʽ
	private static String xpath = ".//Statement/ExpressionStatement/Expression/AssignmentExpression";
	//�ҵ����еĺ���ʹ�ýڵ�
	private static String xpath_func = ".//PrimaryExpression[@Method='true']";
	
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	public static List<FSMMachineInstance> createVFIEMachines(SimpleNode node, FSMMachine fsm){
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);	
		for (SimpleNode snode : evaluationResults) {
			List<SimpleNode> funcList = new LinkedList<SimpleNode>();
			funcList = StateMachineUtils.getEvaluationResults(snode, xpath_func);
			for(SimpleNode func: funcList) {

				MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(func);
				if(methodDecl==null){continue;}
				Method method = methodDecl.getMethod();
				//����assert��
				if(method==null || method.getName().equals("_assert") ||method.getReturnType() != CType_BaseType.voidType) 
					continue;

				SimpleNode parent = (SimpleNode) func.getFirstParentOfType(ASTAssignmentExpression.class);
				SimpleNode ancestor = (SimpleNode) parent.getFirstParentOfType(ASTAssignmentExpression.class, node);

				if(ancestor != null) {
					String secondImage = ancestor.getImage();
					if(secondImage == null || !(secondImage.equals("?"))) {
						addFSMDescription(snode,fsm);
						break;
					}
				}
			}
		}
	    return list;
	}	

	private static void addFSMDescription( SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("avoid using function with its return is void in expression. Process with return type of void used in the expression is dangerous.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("��ֹvoid���͵Ĺ������ڱ��ʽ��ʹ�á�\r\n��������Ϊvoid�Ĺ������ڱ��ʽ��ʹ����Σ�յġ�");
		}	
		list.add(fsminstance);
	}
}
