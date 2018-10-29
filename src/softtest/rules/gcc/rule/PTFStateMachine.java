package softtest.rules.gcc.rule;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.SimpleNode;

import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Type.*;
import softtest.symboltable.c.*;

/** 
 * @author DongNa
 * ��ֹ������ָ�븳ֵ������ָ��
 * ParameterPointerToFunction
 */

public class PTFStateMachine {

	public static List<FSMMachineInstance> createPTFStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = null;
		List<SimpleNode> assignment = null;
		String xPath=".//ParameterDeclaration/Declarator[/Pointer]"; 
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()){
			ASTDeclarator pointerArgument = (ASTDeclarator)itr.next();
			if(pointerArgument==null)
				continue;
			xPath = ".//AssignmentExpression[/AssignmentOperator[@Operators='=']]/UnaryExpression/PostfixExpression/PrimaryExpression";
			SimpleNode defNode = (SimpleNode)pointerArgument.getFirstParentOfType(ASTFunctionDefinition.class);
			if(defNode==null)
				continue;
			assignment = StateMachineUtils.getEvaluationResults(defNode, xPath);
				if(assignment == null)
					continue;
				
				for(SimpleNode assignNode : assignment){
					if((assignNode.getFirstParentOfType(ASTPostfixExpression.class)).jjtGetNumChildren()!=1)
						continue;
					if(assignNode.getImage().equals(pointerArgument.getImage())){
						addFSM(list,assignNode,fsm);
						break;
					}
				}
			
		
		}
		return list;
		} 
  

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" ParameterPointerToFunction: Return parameter pointer to function may lead to unexpected consequences.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��"+node.getBeginFileLine()+"�н�����ָ�븳ֵ������ָ��: ������ָ�븳ֵ������ָ��ᵼ�²���Ԥ�ϵĽ������˽�ֹ������ָ�븳ֵ������ָ�롣");
			}	
		
		list.add(fsminstance);
	}
}
