package softtest.rules.gcc.question;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;



/** 
 * @author LRT
 * CL: Condition Logic ����������е��߼�ֵ����
 * �߼�ֵ�����У�> < >= <=(relational expression) == !=(equality expression) ;   ||(LogicalORExpression) &&(LogicalANDExpression) !(UnaryOperator)
 */
public class CLStateMachine {

	
	// �ҵ����еĺ��� > < <= >=�Ľڵ�
	private static String xpath1 = ".//Expression[./AssignmentExpression/RelationalExpression[@Operators='<' " + 
			" or @Operators='>' or @Operators='<=' or @Operators='>=']]";
	//�ҵ����к� == != �Ľڵ�
	private static String xpath2 = ".//Expression[./AssignmentExpression/EqualityExpression[@Operators='!=' " +
								" or @Operators='==']]";
    //�ҵ����к� && || �� & |�Ľڵ�
	private static String xpath3 =".//Expression[./AssignmentExpression/LogicalANDExpression[@Operators='&&']]" ;
	private static String xpath4 = ".//Expression[./AssignmentExpression/ANDExpression[@Operators='&']]";
	private static String xpath5 = ".//Expression[./AssignmentExpression/LogicalORExpression[@Operators='||']]";
	private static String xpath6 = ".//Expression[./AssignmentExpression/InclusiveORExpression[@Operators='|']]";
	private static String xpath7 = ".//Expression[./AssignmentExpression/UnaryExpression/UnaryOperator[@Operators='!']";

	private static String xpath = xpath1 + " | " + xpath2 + " | " + xpath3 + " | " + xpath4 + " | " + xpath5 + " | " + xpath6 + " | " + xpath7;
	
	public static List<FSMMachineInstance> createCLStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node,xpath);
		Iterator<SimpleNode> itr = evaluationResults.iterator();
		while(itr.hasNext()){	
			SimpleNode exp = itr.next();
			SimpleNode snode =(SimpleNode) exp.jjtGetParent();
		    if(snode instanceof ASTExpressionStatement)
//		        continue;
//		    else
		    	addFSM(snode,fsm,list);
		}	
	    return list;
	}	
	private static void addFSM(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" Line "+node.getBeginLine()+": meaningless logical expression");
				} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
					fsminstance.setDesp("��"+node.getBeginLine()+"��:�ڷ���������н������߼�����");
				}		
			list.add(fsminstance);
		}
}