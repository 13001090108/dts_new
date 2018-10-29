package softtest.rules.c.fault;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTANDExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_Pointer;


/**
 *	@author st
 *	OP - Operator Priority 
 */
public class OPStateMachine {
	
	public static List<FSMMachineInstance> createOPStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		String xpath = ".//ANDExpression[@Operators='& '][/AdditiveExpression[@Operators='+ ']]";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		
		for(Iterator<SimpleNode> itr = evaluationResults.iterator(); itr.hasNext(); ) {
			ASTANDExpression andNode = (ASTANDExpression) itr.next();
			ASTPrimaryExpression primaryNode = DMStateMachine.getPrimarynode(andNode);
			if(primaryNode != null) {
				VariableNameDeclaration varDecl = primaryNode.getVariableDecl();
				if(varDecl != null && varDecl.getType() instanceof CType_Pointer) {
					addFSM(list, andNode, fsm);
				}
			}
		}
		return list;
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		fsminstance.setDesp("ע�⣬��" + node.getBeginLine() + "�еı��ʽ��&�������ȼ�����+����������");
		list.add(fsminstance);
	}
	
}
