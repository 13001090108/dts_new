package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;



/** 
 * @author LiuChang
 * Forbid Constant Logic Not operation
 * ��ֹ�Գ���ֵ���߼��ǵ����� 
 * (���㴦����)
 */
public class FCLNStateMachine {
	
	//�ҵ����еĶԳ������߼�������ı��ʽ
	private static String xpath = ".//UnaryExpression[./UnaryOperator[@Operators='!'] and ./UnaryExpression/PostfixExpression/PrimaryExpression/Constant]";
	
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	public static List<FSMMachineInstance> createFCLNMachines(SimpleNode node, FSMMachine fsm){
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);	
		for (SimpleNode snode : evaluationResults) {
			addFSMDescription(snode, fsm);
		}
	    return list;
	}	
	

	private static void addFSMDescription( SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Forbid logic not operation on constant.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��ֹ�Գ���ֵ���߼��ǵ����㡣\r\n�Գ���ֵ���߼��ǵ������ʹ���߼��б�˼·���ҡ�");
			}	
		
		list.add(fsminstance);
	}
}
