package softtest.rules.gcc.rule;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.pretreatment.Pretreatment;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;


/** 
 * @author nieminhui
 * �����б���ʹ�ù���Ĳ��������鲻Ҫ����20�� (����������)
 */
public class RDDStateMachine {	
	
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	public static List<FSMMachineInstance> createRDDMachines(SimpleNode node, FSMMachine fsm){
		//�ҵ����еĺ�������������ڵ�
		String xpath = ".//FunctionDefinition";	
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator<SimpleNode> itr = evaluationResults.iterator();
		while(itr.hasNext()) {
			SimpleNode snode = itr.next();
			//�����������������ֱ������
			if(snode.jjtGetNumChildren() == 2)
				continue;
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodDefinition(snode);	
			//����ǿ⺯��ֱ������
			if (methodDecl == null || methodDecl.isLib()) {
				continue;
			}	
			if(methodDecl.getParameterCount() > 20) {
				addFSMDescription(snode, fsm, methodDecl.getParameterCount());
			}
		}
	    return list;
	}	

	private static void addFSMDescription(SimpleNode node, FSMMachine fsm, int num) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Avoid using  too many parameters in a process, it is recommended no more than 20.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("�����б���ʹ�ù���Ĳ��������鲻Ҫ����20����\r\n�÷����Ĳ���������" + num);
			}	
		
		list.add(fsminstance);
	}
}
