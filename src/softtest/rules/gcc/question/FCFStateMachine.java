package softtest.rules.gcc.question;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;

import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Type.CType_BaseType;

/** 
 * @author YangRui
 */

public class FCFStateMachine {

	public static List<FSMMachineInstance> createFCFStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = null;
		
		String xPath=".//AssignmentExpression/EqualityExpression";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()){
			SimpleNode equal= (SimpleNode)itr.next();
			AbstractExpression left=(AbstractExpression)equal.jjtGetChild(0);
			AbstractExpression right=(AbstractExpression)equal.jjtGetChild(1);
			if(!(left.getType() instanceof CType_BaseType)|| !(right.getType() instanceof CType_BaseType))
				continue;
			if(left.getType().toString().equals("float")&&right.getType().toString().equals("float"))	
				addFSM(list,equal,fsm);
			if(left.getType().toString().equals("double")&&right.getType().toString().equals("double"))
				addFSM(list,equal,fsm);
		}
			
		return list;
	     
  }

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" Fault Comparison Of Float�� The computing of float is related to accuracy (etc.roundoff),So the comparision of two floating number is inaccurate.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("�������Ĵ���Ƚ�: ��������������ȣ���Ϊ�������ļ����漰����ȷ�Է��棨����ȣ������ԱȽ�������������������ǲ�׼ȷ�ġ�");
			}	
		
		list.add(fsminstance);
	}
}

