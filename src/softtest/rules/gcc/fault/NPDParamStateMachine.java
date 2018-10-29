package softtest.rules.gcc.fault;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.SimpleNode;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MethodFeature;
import softtest.summary.c.MethodSummary;
import softtest.summary.gcc.fault.MethodNPDPreCondition;
import softtest.summary.gcc.fault.MethodNPDPreConditionVisitor;
import softtest.symboltable.c.MethodNameDeclaration;

public class NPDParamStateMachine extends BasicStateMachine{
	@Override
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodNPDPreConditionVisitor.getInstance());
	}

	

	public static List<FSMMachineInstance> createNPDParamStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		String xPath = ".//PrimaryExpression[@Method='true' and following-sibling::ArgumentExpressionList//PrimaryExpression[@Method='true']]";
		List<SimpleNode> nodes = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode exp : nodes) {
			// ��ȡ���еı��ص��÷�������
			AbstractExpression funcNode = (AbstractExpression)exp;
			if (!funcNode.isMethod()) {
				continue;
			}
			MethodNameDeclaration methodDecl = null;
			if (exp instanceof ASTPrimaryExpression) {
				methodDecl = (MethodNameDeclaration)((ASTPrimaryExpression)exp).getMethodDecl();
			} 
			if (methodDecl == null) {
				continue;
			}
			
			// �Ӻ���ժҪ��NPD��ǰ�������У���ÿ���βν�����Ӧ�ļ���Զ���
			if (methodDecl.getMethod() != null) {
				MethodSummary summary =  methodDecl.getMethod().getMtSummmary();
				if (summary == null) {
					continue;
				}
				Set<MethodFeature> features = summary.getPreConditions();
				for (MethodFeature feature : features) {
					if (feature instanceof MethodNPDPreCondition) {
						MethodNPDPreCondition npdPreCond = (MethodNPDPreCondition)feature;
						Set<Variable> npdVars = npdPreCond.getNPDVariables();
						for (Variable var : npdVars) {
							if (var.isParam()) { // NPD�Բ�����Լ��
								xPath = "./ArgumentExpressionList/AssignmentExpression[" + (var.getParamIndex() + 1) + "]//PrimaryExpression[@Method='true']";
								List<SimpleNode> params = StateMachineUtils.getEvaluationResults((SimpleNode) exp.jjtGetParent(), xPath);
								if (params != null && params.size() >0) {
									MethodNameDeclaration methodDecl1 = null;
									if (params.get(0) instanceof ASTPrimaryExpression) {
										methodDecl1 = (MethodNameDeclaration)((ASTPrimaryExpression)params.get(0)).getMethodDecl();
									} 
									if (methodDecl1 == null) {
										continue;
									}
									// �Ӻ���ժҪ��NPD��ǰ�������У���ÿ���βν�����Ӧ�ļ���Զ���
									if (methodDecl1.getMethod() != null) {
										Domain domain =  methodDecl1.getMethod().getReturnDomain();
										if (domain instanceof PointerDomain) {
											PointerDomain pd = (PointerDomain) domain;
											if (pd.getValue() == PointerValue.NULL || pd.getValue() == PointerValue.NULL_OR_NOTNULL) {
												FSMMachineInstance fsmInstance = fsm.creatInstance();
												fsmInstance.setRelatedASTNode(exp);
												fsmInstance.setStateData(var);
												fsmInstance.setDesp("����" + exp.getImage() + "�ĵ�" + (var.getParamIndex() + 1) + "�������ķ���ֵ����Ϊ��");
												if (!methodDecl.isLib()) {
													fsmInstance.setTraceinfo(npdPreCond.getDespString(var));
												}
												list.add(fsmInstance);
											}
										}  else if (domain instanceof IntegerDomain) {
											IntegerDomain id = (IntegerDomain) domain;
											if (id.getMin() == id.getMax() && id.getMin() == 0) {
												FSMMachineInstance fsmInstance = fsm.creatInstance();
												fsmInstance.setRelatedASTNode(exp);
												fsmInstance.setStateData(var);
												fsmInstance.setDesp("����" + exp.getImage() + "�ĵ�" + (var.getParamIndex() + 1) + "�������ķ���ֵ����Ϊ��");
												if (!methodDecl.isLib()) {
													fsmInstance.setTraceinfo(npdPreCond.getDespString(var));
												}
												list.add(fsmInstance);
											}
										}
									}
								}
							} 
						}
					}
				}
			}
		}
		return list;
	}

}
