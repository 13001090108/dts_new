package softtest.rules.gcc.rule;

import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;

/**
 * @author suntao
 * Blank case
 * 4.3.1.8 ��ֹswitch��case��������κο�ִ����� 
 **/
public class BCStateMachine {

	public static List<FSMMachineInstance> createBCStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		String xpath = ".//LabeledStatement[@Image='case']";
		List<SimpleNode> result = StateMachineUtils.getEvaluationResults(node, xpath);
	
		
		for(Iterator<SimpleNode> itr = result.iterator(); itr.hasNext();) {
			SimpleNode statement = (ASTLabeledStatement) itr.next();
			SimpleNode childStatement = statement;
			do {
				if(childStatement != statement) {
					addFSM(list, statement, fsm);
					statement = childStatement;
				}
				
				if(childStatement.jjtGetChild(0) instanceof ASTStatement)
					childStatement = (SimpleNode) childStatement.jjtGetChild(0).jjtGetChild(0);
				else if(childStatement.jjtGetChild(1) instanceof ASTStatement)
					childStatement = (SimpleNode) childStatement.jjtGetChild(1).jjtGetChild(0);
			}while(childStatement != null && (childStatement.getImage().equals("case") || childStatement.getImage().equals("default")));
		}
		
		return list;
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) 		
			fsmInstance.setDesp("case in line " + node.getBeginLine() + "is blank. Avoid to use this.");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsmInstance.setDesp("�ڵ� "+node.getBeginFileLine()+" �е�case����޿�ִ����䡣" +
					"���ĳ��case��������κο�ִ����䣬�������������case����е�ִ����䡣��������������ڴ��벻������ɵģ����Ǳ����������Ƶġ�Ϊ�˷�ֹ�����������Ĵ��룬��˽�ֹswitch��case��������κο�ִ����䡣");
		list.add(fsmInstance);
	}
}
