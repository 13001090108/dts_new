package softtest.rules.gcc.question;

import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;

/*
 * @author liruitong
 * FA Function Address ����������ַ
 * ������ַ������ı������߼������������߼�����Ĳ�������������������������������������ȴȱʧ��Բ���š�
 * ʹ�ú�����ַ�����ǵ��ú������ܵ���δ֪������Ϊ���ڴ����
 */
public class FAStateMachine {

	public static List<FSMMachineInstance> createFAStateMachines(
			SimpleNode node, FSMMachine fsm) {
		String xpath = ".//Declarator[../CompoundStatement]/DirectDeclarator[@Method='true']"; //�ҵ����еĺ�������
		String xpath1 = null;  //�ҵ����еĺ�������
		String image1 = null;
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		ASTTranslationUnit  translationUnitNode = (ASTTranslationUnit)node.getFirstParentOfType(ASTTranslationUnit.class);
		if(translationUnitNode==null)
		{
			return list;
		}
		evaluationResults = StateMachineUtils.getEvaluationResults(translationUnitNode, xpath);
		
		for (SimpleNode snode : evaluationResults) {//���ڸ��ļ������������к���
			image1 = snode.getImage(); //��ú�����
			xpath1 = ".//Statement//Expression//UnaryExpression/PostfixExpression[@Image='"  + image1  + "']";
			List<SimpleNode> funcs = StateMachineUtils.getEvaluationResults(translationUnitNode, xpath1);
			//���ڸú�����ÿ�ε���
			if(funcs.size()>0)
			{			
				Iterator itr = funcs.iterator(); 
				while(itr.hasNext()){	
					AbstractExpression postfixExp =(AbstractExpression)itr.next(); 
			
					if(!postfixExp.getOperators().equals("("))
					{
						addFSM(list,postfixExp,fsm);
					}
					else
						continue;
					}
							
				}
			}
		
	    return list;
	}

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node,
			FSMMachine fsm) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
        
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
			fsmInstance.setDesp("Warning: Line"
					+ node.getBeginLine()
					+ " used function address "+ node.getImage()+ "(Missed brackets).This might lead to unknown program behavior or memeory error");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsmInstance.setDesp("���棺��"
					+ node.getBeginLine()
					+ " �в���������ַ������" +node.getImage() +"Բ����ȱʧ��������ܵ���δ֪������Ϊ���ڴ����");

		list.add(fsmInstance);
	}
}
