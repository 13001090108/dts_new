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
 * Assign Negative To Unsigned variable
 * ��ֹ���޷��ű�������ֵ
 */
public class ANTUStateMachine {
	
	//�ҵ����и�ֵ����
	private static final String xpath = ".//AssignmentExpression";
	//��ֵ���ʽ����ֵ
	private static final String xpath1 = "./UnaryExpression/PostfixExpression/PrimaryExpression[@DescendantDepth='0']";
	//��ֵ���ʽ��ֵΪ����Constant��
	private static final String xpath2 = "./AssignmentExpression/UnaryExpression[./UnaryOperator[@Operators='-']]/UnaryExpression/PostfixExpression/PrimaryExpression/Constant";
	//��ֵ���ʽ��ֵΪ����unsigned��
	private static final String xpath3 = "./AssignmentExpression/UnaryExpression[./UnaryOperator[@Operators='-']]/UnaryExpression/PostfixExpression/PrimaryExpression[@DescendantDepth='0']";

	public static List<FSMMachineInstance> createANTUMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		SimpleNode var = null; //��ֵ���ʽ����ֵ
		
		for (SimpleNode snode : evaluationResults) {
			List<SimpleNode> vars = StateMachineUtils.getEvaluationResults(snode, xpath1);
			if(vars!=null && vars.size()==1)
				var = vars.get(0);
			else 
				continue;
			
			VariableNameDeclaration variable = var.getVariableNameDeclaration();
			if(!(variable!=null && isUnsignedType(variable.getType()))) //��ֵ��Unsigned����
				continue;
			
			List<SimpleNode> constants = StateMachineUtils.getEvaluationResults(snode, xpath2);//��ֵ�Ǹ���Constant����
			if(constants!=null && constants.size()==1) {	
				addFSMDescription(snode, variable, fsm, list);
				continue;
			}	
			
			List<SimpleNode> values = StateMachineUtils.getEvaluationResults(snode, xpath3);//��ֵ�Ǹ���Unsigned����
			if(values!=null && values.size()==1) {
				SimpleNode rightVar = values.get(0);
				VariableNameDeclaration rightVarDel = rightVar.getVariableNameDeclaration();
				if(rightVarDel!=null && isUnsignedType(rightVarDel.getType())) 
					addFSMDescription(snode, variable, fsm, list);
			}
		}
	    return list;
	}	
	
	private static boolean isUnsignedType(CType type) {
		if(!(type instanceof CType_BaseType))
			return false;
		if(type == CType_BaseType.uCharType || type == CType_BaseType.uIntType || type == CType_BaseType.uLongLongType || type == CType_BaseType.uLongType || type == CType_BaseType.uShortType )
			return true;
		return false;
	}

	private static void addFSMDescription( SimpleNode node, VariableNameDeclaration variable, FSMMachine fsm, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" Assign Negative To Unsigned Variable " + variable.getImage() + ". This will lead to unpredictable result.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��ֹ���޷��ű���" + variable.getImage() + "����ֵ��\r\n���޷��ű�������ֵ�ᵼ�²���Ԥ�ϵĽ����");
			}	
		
		list.add(fsminstance);
	}
}
