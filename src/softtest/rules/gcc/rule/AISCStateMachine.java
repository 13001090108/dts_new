package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;


/** 
 * @author nieminhui
 * Assignment operation In Short Circult operation
 * ��ֹ��ֵ�������롰&&���򡰡�������
 * (���ڶ�·����������ԣ���ֵ��������������ڷǵ�һ�����ʽ�У����ڸ�ֵ�����������п��ܸ�ֵ���û��ִ��)
 */
public class AISCStateMachine {	
	
	
	public static List<FSMMachineInstance> createAISCMachines(SimpleNode node, FSMMachine fsm){
		//�ҵ����е�&&��||���ʽ 
		String xpath = ".//LogicalANDExpression | .//LogicalORExpression";
		//�ҵ����ʽ�к��и�ֵ�����"="�ı��ʽ
		String xpath1 = ".//AssignmentExpression[/AssignmentOperator[@Operators='=']]";
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);	
		for (SimpleNode snode : evaluationResults) {
			int nCount = snode.jjtGetNumChildren();
			//����a && b && c�������b,c���迼�ǣ�
			for(int i=1; i<nCount; i++) {
				SimpleNode child = (SimpleNode) snode.jjtGetChild(i);
				List<SimpleNode> assign = StateMachineUtils.getEvaluationResults(child, xpath1);
				if(assign==null || assign.size()==0)
					continue;
				addFSMDescription(snode, child, fsm, list);
			}
		}
	    return list;
	}	

	private static void addFSMDescription(SimpleNode parent, SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		String engDes = "||";
		if(parent instanceof ASTLogicalANDExpression) 
			engDes = "&&";
		
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Assignment operation In Short Circult operation " +  engDes + ". This is a bad programming habit.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��ֹ��ֵ�������� "+ engDes + " ���á�\r\n����һ�ֲ��õı��ϰ�ߣ���Ϊ��ֵ���������ˣ�ԭ�����ĸ�ֵδ���ܱ�ִ�С�");
			}	
		
		list.add(fsminstance);
	}
}
