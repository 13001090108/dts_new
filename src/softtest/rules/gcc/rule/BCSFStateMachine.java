package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;


/** 
 * @author 
 * Be Careful of Some Functions(�Ƽ���)
 * ����ʹ��abort��exit�Ⱥ��� (���̵�����)
 */
public class BCSFStateMachine {	
	
	public static List<FSMMachineInstance> createBCSFMachines(SimpleNode node, FSMMachine fsm){
		//�ҵ�abort��exit����
		String xpath = ".//PrimaryExpression[@Image='abort' and @Method='true']";
		String xpath1 = ".//PrimaryExpression[@Image='exit' and @Method='true']";
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath + " | " + xpath1);	
		
		for (SimpleNode snode : evaluationResults) {
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(snode);
			if (methodDecl != null && methodDecl.isLib() == true) //snode�ǿ⺯������
				addFSMDescription(snode, fsm, list);
		}
		
	    return list;
	}	

	private static void addFSMDescription(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Be Careful of Some Functions, such as " +	node.getImage() + " Function. The function will lead to the termination of program execution, should be used with caution.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ�� " + 	node.getImage() + " �Ⱥ�����\r\n��Щ�����ᵼ����ֹ����ִ�У�Ӧ����ʹ����Щ������");
			}	
		list.add(fsminstance);
	}
}
