package softtest.rules.gcc.rule;

import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;



/** 
 * @author
 * Related Functions In Expression
 * ��ֹͬһ�����ʽ�е��ö����غ��� (���̵�����)
 */

public class RFIEStateMachine {	
	//�ҵ����еĸ�ֵ���ʽ
	private static String xpath = ".//Statement/ExpressionStatement/Expression/AssignmentExpression";
	//�ҵ����ʽ����Ϊ�������ݵı����������Ǵ���ַ&a�������
	private static String xpath2 = ".//ArgumentExpressionList/AssignmentExpression/UnaryExpression/UnaryExpression/PostfixExpression/PrimaryExpression[@Method='false']";
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	public static List<FSMMachineInstance> createRFIEMachines(SimpleNode node, FSMMachine fsm){
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);			
		for (SimpleNode snode : evaluationResults) {
			Set<VariableNameDeclaration> variables = new HashSet<VariableNameDeclaration>();//��ű��ʽ���õ����еı���(��Ϊ����)
			List<SimpleNode> argList= new ArrayList<SimpleNode>();
			argList = StateMachineUtils.getEvaluationResults(snode, xpath2);
			
			for(SimpleNode arg: argList) {
				VariableNameDeclaration getVariableNameDeclaration = arg.getVariableNameDeclaration();
				if(getVariableNameDeclaration != null && variables.contains(getVariableNameDeclaration)) {	
					addFSMDescription(snode,fsm,getVariableNameDeclaration);
					break;
				}
				else if(getVariableNameDeclaration != null) {
					variables.add(getVariableNameDeclaration);
				}
			}
		}
	    return list;
	}	
	
	private static void addFSMDescription( SimpleNode node, FSMMachine fsm,VariableNameDeclaration variable ) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		fsminstance.setRelatedVariable(variable);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Forbid related functions in  one expression. If an expression is calling a number of related functions, it may be executed in a different order and produces different results.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��ֹͬһ�����ʽ�е��ö����غ�����\r\n���ͬһ�����ʽ�е��ö����غ��������ܻ���ִ�е�˳��ͬ��������ͬ�Ľ����");
			}	
		
		list.add(fsminstance);
	}
}
