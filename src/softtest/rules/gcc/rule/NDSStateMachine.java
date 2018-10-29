package softtest.rules.gcc.rule;

import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.*;
import softtest.rules.c.StateMachineUtils;

/**
 * @author zhouhb
 * No Default Switch
 * 4.3.1.4 ��switch����б�����default���
 **/
public class NDSStateMachine {
	
	public static List<FSMMachineInstance> createNDSStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		String xpath = ".//SelectionStatement[@Image='switch']";
		List<SimpleNode> result = StateMachineUtils.getEvaluationResults(node, xpath);
		
		for(Iterator itr = result.iterator(); itr.hasNext();) {
			ASTSelectionStatement selNode = (ASTSelectionStatement) itr.next();
			String defaultXpath = ".//LabeledStatement[@Image='default']";
			List<SimpleNode> defaultClause = StateMachineUtils.getEvaluationResults(selNode, defaultXpath);
			if(defaultClause.isEmpty())
				addFSM(list, selNode, fsm);
		}
		return list;
	}

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) 		
			fsmInstance.setDesp("switch in line " + node.getBeginLine() + "has no default branch. Use default branch can show that all possibilities hava been concerned.");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsmInstance.setDesp("�ڵ� "+node.getBeginLine()+" �е�switch���û��default��֧��" +
					"���switch�����ȱʡ��default��䣬�����е�case���ı��ʽֵ����ƥ��ʱ�������ת������switch�������һ�����ִ�С�ǿ��default����ʹ�����ֳ��ѿ����˸�������ı��˼�롣");
		list.add(fsmInstance);
	}
}
