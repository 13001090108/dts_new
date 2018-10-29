package softtest.rules.gcc.rule;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.ASTRelationalExpression;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.SimpleNode;
import softtest.ast.gccparser.*;

import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Type.*;
/** 
 * @author Zhouhb
 * ����ʹ��ָ����߼��Ƚ�
 * LogicalCompareofPointer 
 */

public class LCPStateMachine {

	public static List<FSMMachineInstance> createLCPStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = null;
		
		String xPath=".//Expression/AssignmentExpression/RelationalExpression";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()){
			ASTRelationalExpression relationalExp= (ASTRelationalExpression)itr.next();
			if(relationalExp.jjtGetNumChildren() != 2)
				continue;
			AbstractExpression left = (AbstractExpression) relationalExp.jjtGetChild(0);
			AbstractExpression right = (AbstractExpression) relationalExp.jjtGetChild(0);
			if(left.getType() != null && right.getType() != null)
			{
				if (left.getType() instanceof CType_Pointer 
						&& right.getType() instanceof CType_Pointer)	
				{
					String a = left.getType().toString();
					String b = right.getType().toString();
					addFSM(list,relationalExp,fsm,a,b);
				}
			}
		}
			
		return list;
	     
  }

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm, String left, String right) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" LogicalCompareofPointer: Using logical relation copmarison to two pointer is dangerous,so it should be use carefully.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ��ָ����߼��Ƚ�:��" +node.getBeginLine()+"�д�ָ�����ͽ����߼��Ƚϣ�ʹ�ô��ں�С�ڵĲ�������ָ����бȽ��Ǿ��нϴ���յģ�Ӧ����ʹ��ָ����߼��Ƚϡ�");
			}	
		
		list.add(fsminstance);
	}
}
