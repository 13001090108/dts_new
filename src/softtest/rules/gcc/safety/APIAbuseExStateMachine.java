package softtest.rules.gcc.safety;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.Method;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MethodAPIAbuseFeature;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.Type.CType;

/**
 * <p>
 * Create the state machines to check API abuse. 
 * </p>
 * @author liuyan
 *
 */
public class APIAbuseExStateMachine {

	public static List<FSMMachineInstance> createAPIAbuseStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		
		String xPath=".//UnaryExpression/PostfixExpression/PrimaryExpression[@Method='true'] " +
			"|.//Expression/AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression[@Method='true']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);

		for (SimpleNode funcNode : evaluationResults) {
			
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(funcNode);
			if (methodDecl == null)
				continue;
			
			if(methodDecl.getImage().equals("GetTempFileNameA")){
				String desp = "��ȫ�Ĵ�����ʱ�ļ�����Щ��ʱ�ļ����Ǻ�Σ�յģ���Ϊ�������ܻ��úڿ���ǰ�µ����ǵ����֣��Ӷ����ºڿͿ��Է���ר������";
				FSMMachineInstance fsmInstance = fsm.creatInstance();
				fsmInstance.setRelatedASTNode(funcNode);
				fsmInstance.setResultString(funcNode.getImage());
				fsmInstance.setDesp(desp);
				list.add(fsmInstance);
			}
			
			if (methodDecl.getMethodSummary() == null)
				continue;
			int a=1;
			int b=2;
			
			
			
			MethodAPIAbuseFeature apiAbuseFeature = (MethodAPIAbuseFeature)methodDecl.getMethodSummary().findMethodFeature(MethodAPIAbuseFeature.class);
			if (apiAbuseFeature == null)
				continue;
			
			FSMMachineInstance fsmInstance = fsm.creatInstance();
			fsmInstance.setRelatedASTNode(funcNode);
			fsmInstance.setResultString(funcNode.getImage());
			fsmInstance.setDesp(apiAbuseFeature.getDescription());
			
			list.add(fsmInstance);
		}

		return list;
	}

}


