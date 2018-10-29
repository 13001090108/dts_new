package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.SimpleNode;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;

/**
 * @author xiangwentao
 * Avoid Some Operation
 * ����ʹ�á�+=����-=����+=����--��������  (�Ƽ���)
 * (���ʹ����)
 * 
 * ��++����--��һ��ֻ��һ�������ı��ʽ��ѭ��������ʹ��
 */
public class ASOStateMachine {
	
	private enum Type {TYPE_1, TYPE_2}
	
	public static List<FSMMachineInstance> createASOMachines(SimpleNode node, FSMMachine fsm){
		/**��ѯ���г���'+='��'-='�ı��ʽ*/
		String xPath = ".//Statement//Expression/AssignmentExpression/AssignmentOperator[@Operators='+=' or @Operators='-=']";
		/**��ѯ���г���'++'��'--'�ı��ʽ,ǰ׺�ͺ�׺*/
		String xPath_1 = ".//UnaryExpression[@Operators='++' or @Operators='--'] | .//PostfixExpression[@Operators='++' or @Operators='--'] ";
		
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		//��ѯ���г���'+='��'-='�ı��ʽ
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode snode : evaluationResults) {
			addFSMDescription(snode, fsm, Type.TYPE_1, list);
		}
		
		//��ѯ���г���'++'��'--'�ı��ʽ,ǰ׺�ͺ�׺
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath_1);
		for (SimpleNode snode : evaluationResults) {
			//1.	ȥ�������ı��ʽ���ͣ���a++;
			//2.	ȥ��������forѭ���Ķ����ڵ㣬��for(i=0; i<10; i++){}
			SimpleNode parent = (SimpleNode) snode.getFirstParentOfType(ASTAssignmentExpression.class, node);
			SimpleNode ancestor = (SimpleNode) parent.getFirstParentOfType(ASTAssignmentExpression.class, node);
			if(ancestor != null)
				addFSMDescription(snode, fsm, Type.TYPE_2, list);
		}
		return list;
	}
	
	private static void addFSMDescription(SimpleNode node, FSMMachine fsm, Type type, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if(type == Type.TYPE_1) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Avoid Some Operation, such as ��+=�� and '-='");
			} 
			else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ�á�+=����-=����������\r\n��+=����-=��������ʹ�ü�࣬��ҲӰ���˿ɶ��ԣ����׳��ֱ��ʧ��");
			}	
		}
		if(type == Type.TYPE_2) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Avoid Some Operation, such as ��++�� and '--'");
			} 
			else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ�á�++����--����������\r\n�ڱ��ʽ��ʹ����Щ�������Ǻ�Σ�յģ�һ��ֻ��һ�������ı��ʽ��ѭ��������ʹ������");
			}
		}
		list.add(fsminstance);
	}
}
