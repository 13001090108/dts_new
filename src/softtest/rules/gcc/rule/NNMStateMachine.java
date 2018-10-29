package softtest.rules.gcc.rule;
import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.rules.c.StateMachineUtils;


/*
 * @author liruitong
 * NNM: No New Malloc
 * ʹ�����������ڴ���䣬������malloc����realloc��calloc��̬�ڴ���亯��
 */
public class NNMStateMachine {

	public static List<FSMMachineInstance> createNNMStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		if(!node.getFileName().contains(".h"))
		{
			List<SimpleNode> evaluationResults = null;
			ASTTranslationUnit  translationUnitNode = (ASTTranslationUnit)node.getFirstParentOfType(ASTTranslationUnit.class);
			if(translationUnitNode==null)
			{
				return list;
			}	
			String xpath=".//UnaryExpression/PostfixExpression/PrimaryExpression[@Image='malloc' or @Image='calloc' or @Image='realloc' ][@Method='true']";
			evaluationResults = StateMachineUtils.getEvaluationResults(translationUnitNode, xpath);
			if (evaluationResults.size()!=0)
			{
				Iterator<SimpleNode> itr = evaluationResults.iterator();
				while(itr.hasNext()) {
					SimpleNode sNode = (SimpleNode)itr.next();
					addFSM(list, sNode, fsm);
				}
			}
		}
	  return list;
		
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
		String methodName = node.getImage();
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) 		
			fsmInstance.setDesp("Warning: Line" + fsmInstance.getRelatedASTNode().getBeginLine()+" uses " + methodName + "to allocate memmory");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsmInstance.setDesp("���棺��" + fsmInstance.getRelatedASTNode().getBeginLine()+" ��ʹ�ú���" + methodName + "�������ڴ����");
		
		list.add(fsmInstance);
	}
}
