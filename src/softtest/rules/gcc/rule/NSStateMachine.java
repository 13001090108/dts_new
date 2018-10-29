package softtest.rules.gcc.rule;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTStatementList;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;

/**
 * @author suntao
 * Nonsense switch
 * 4.3.1.6 ��ֹswitch�����ֻ����default���(�������������case���)
 **/
public class NSStateMachine {

	public static List<FSMMachineInstance> createNSStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		String xpath = ".//SelectionStatement[@Image='switch']/Statement/CompoundStatement/StatementList";
		List<SimpleNode> result = StateMachineUtils.getEvaluationResults(node, xpath);
		boolean onlyDefault = false;
			
		for(Iterator<SimpleNode> itr = result.iterator();itr.hasNext();) {
			
			ASTStatementList stateList = (ASTStatementList) itr.next();
			int n = stateList.jjtGetNumChildren();
			boolean noCase = true;
			
			for(int i = 0; i < n; i++) {
				ASTStatement statement = (ASTStatement) stateList.jjtGetChild(i);
				if(((SimpleNode)(statement.jjtGetChild(0))).getImage().equals("case")){
					noCase=false;
					break;}
				}
			
			if(noCase==true)
			for(int i = 0; i < n; i++){
				ASTStatement statement = (ASTStatement) stateList.jjtGetChild(i);
				if(((SimpleNode)(statement.jjtGetChild(0))).getImage().equals("default")){
					onlyDefault=true;
					break;
					
				
		}}
			
		}

		if(onlyDefault){
			addFSM(list, (SimpleNode)result.get(0).getFirstParentOfType(ASTSelectionStatement.class), fsm);
		
			}
		
		
		return list;
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) 		
			fsmInstance.setDesp("switch in line " + node.getBeginFileLine() + "is nonsense. A switch statement which only contains " +
					"a default claus doesn't make any sense, so don't use it.");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsmInstance.setDesp("�ڵ� "+node.getBeginFileLine()+" �е�switch���û�����塣" +
					"���switch�����ֻ����default��䣬���switch����ʹ�����κ�ʵ�ʼ�ֵ����˽�ֹswitch�����ֻ����default��䡣");
		list.add(fsmInstance);
	}
}
