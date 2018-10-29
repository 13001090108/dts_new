package softtest.rules.gcc.question;
import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;

/*
 * @author lrt
 * CCT: constant condition three orders ��Ŀ���ʽ�еĳ�������
 */
public class CCTStateMachine {

	public static List<FSMMachineInstance> createCCTStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		String xpath1 = ".//AssignmentExpression/ConditionalExpression";
		String xpath2= ".//PostfixExpression/PrimaryExpression";
		evaluationResults = StateMachineUtils.getEvaluationResults(node,xpath1);
		Iterator<SimpleNode> itr = evaluationResults.iterator(); 
	    boolean findVariable;
	    ASTAssignmentExpression assignExp=null;
		while(itr.hasNext()){	
			//�ӵ�ǰ��expression ���������Ϊ0��primaryexpression,���Ҳ������򱨴� ������������
			SimpleNode snode = (SimpleNode) itr.next();
			if(snode.jjtGetNumChildren()>0)
			{	 
				SimpleNode cn = (SimpleNode) snode.jjtGetChild(0);
				List<SimpleNode> peResults = new LinkedList<SimpleNode>();
				peResults = StateMachineUtils.getEvaluationResults(cn,xpath2); //�ҵ����е�primaryexpression
				findVariable = false;
				for(SimpleNode sn: peResults)
				{
					if(sn.getDescendantDepth()==1)
						continue;
					else
					{
						assignExp=null;
						findVariable =true;
						assignExp=(ASTAssignmentExpression)sn.getFirstParentOfType(ASTAssignmentExpression.class);
						 if(assignExp!=null && assignExp.getFirstChildOfType(ASTAssignmentOperator.class)!=null && assignExp.jjtGetParent().jjtGetParent() instanceof ASTConditionalExpression)
							 findVariable=false;
						 else
						 {
							 ASTAssignmentExpression pAssignExp = (ASTAssignmentExpression)assignExp.getFirstParentOfType(ASTAssignmentExpression.class);
							 if(pAssignExp!=null && pAssignExp.getFirstChildOfType(ASTAssignmentOperator.class)!=null && pAssignExp.jjtGetParent().jjtGetParent() instanceof ASTConditionalExpression)
								 findVariable = false;
						 }
						if(findVariable)
							break;
					}
				}
				
				if(!findVariable)//ao!=null  ��Ȼ�ҵ��˱��� ���Ǳ���Ϊ��ֵ���ʽ
				  addFSM(list,snode,fsm);
			}
		}	
	    return list;
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) 		
			fsmInstance.setDesp("Warning: Line" + fsmInstance.getRelatedASTNode().getBeginLine()+" Constant condition");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsmInstance.setDesp("���棺��" + fsmInstance.getRelatedASTNode().getBeginLine()+" ����Ŀ���ʽΪ��������");
		
		list.add(fsmInstance);
	}
}
