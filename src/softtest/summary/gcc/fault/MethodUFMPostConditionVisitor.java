package softtest.summary.gcc.fault;

import java.util.ArrayList;
import java.util.List;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.domain.c.symbolic.Expression;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.summary.c.MethodFeature;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.ClassScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_Pointer;

public class MethodUFMPostConditionVisitor extends CParserVisitorAdapter implements MethodFeatureVisitor {

	private static MethodUFMPostConditionVisitor instance;
	
	/**
	 * ժҪ�����ߵ�ǰ���ڵĺ�����Ӧ�ĺ������� 
	 */
	private MethodNameDeclaration methodDecl;
	
	private MethodUFMPostConditionVisitor() {
	}
	
	public static MethodUFMPostConditionVisitor getInstance() {
		if(instance == null) {
			instance = new MethodUFMPostConditionVisitor();
		}
		return instance;
	}
	
	/**
	 * ����������out������ժҪ��vexnodeһ��Ϊfunc_out
	 */
	public void visit(VexNode vexNode) {
		if (vexNode.getTreenode() == null) {
			return;
		}
		MethodUFMPostCondition feature = new MethodUFMPostCondition();
		
		//�������������Ӧ���﷨���ڵ�һ��Ϊfunction_definition
		SimpleNode node = vexNode.getTreenode();
		if (node == null) {
			return;
		}
		methodDecl = InterContext.getMethodDecl(vexNode);
		if (methodDecl != null) {
			node.jjtAccept(this, feature);
		}
		if (feature.isEmpty()) {
			return;
		}
		
		MethodSummary summary = InterContext.getMethodSummary(vexNode);
		if (summary != null) {
			summary.addPostCondition(feature);
			if (Config.INTER_METHOD_TRACE) {
				if (methodDecl != null) {
					System.err.println(methodDecl.getFullName() + " " + feature);
				}
			}
		}
	}

	@Override
	public Object visit(ASTAssignmentExpression node, Object data) {
		MethodUFMPostCondition feature = (MethodUFMPostCondition) data;
		//����Ƿ��ȫ�ֻ������Աָ��������¸�ֵ
		String oper = node.getOperators();
		if(oper.equals("=")) {		//���� p = xxx�ĸ�ֵ���
			if(node.jjtGetNumChildren() == 3
					&& node.jjtGetChild(2) instanceof ASTAssignmentExpression) {
				ExpressionValueVisitor exp = new ExpressionValueVisitor();
				ExpressionVistorData domaindata = new ExpressionVistorData();
				Object re  = node.getVariableNameDeclaration();
				exp.visit((SimpleNode)node.jjtGetChild(0), domaindata);
				if(re instanceof VariableNameDeclaration) {
					Variable variable = Variable.getVariable((VariableNameDeclaration)re);
					if(variable != null) {
						feature.removeVariable(variable);	//����ǰ������Ϣ���д˱�������ɾ��
					}
				}
			}
		}
		return super.visit(node, data);
	}

//	@Override
//	public Object visit(ASTdelete_expression node, Object data) {
//		MethodUFMPostCondition feature = (MethodUFMPostCondition) data;
//		if(node.jjtGetNumChildren() == 1 && node.jjtGetChild(0) instanceof ASTunary_expression) {
//			//��ȡָ�����
//			ExpressionValueVisitor exp = new ExpressionValueVisitor();
//			ExpressionVistorData domaindata = new ExpressionVistorData();
//			Object re  = node.getVariableNameDeclaration();
//			exp.visit((SimpleNode)node.jjtGetChild(0), domaindata);
//			if(isUFMPostVar(domainData, re, node, feature)) {
//				Variable variable = Variable.getVariable((VariableNameDeclaration)re);
//				if(variable != null) {
//					feature.addVariable(variable, "���ļ�" + node.getFileName() + "�к���"  + methodDecl.getName() + "��" + node.getBeginLine() + "�д����ͷ�");
//				}
//			}
//		}
//		return super.visit(node, data);
//	}

	@Override
	public Object visit(ASTPrimaryExpression node, Object data) {
		MethodUFMPostCondition feature = (MethodUFMPostCondition)data;
		if(node.isMethod() && node.getMethodDecl() != null) {
			MethodNameDeclaration methodDecl = (MethodNameDeclaration) node.getMethodDecl();
			MethodSummary mtSummary = methodDecl.getMethodSummary();
			if (node.isMethod() && methodDecl.getImage().equals("free")) {
				// ϵͳ�⺯����free(p)
				List funcNode = ((SimpleNode)node.jjtGetParent()).findChildrenOfType(ASTArgumentExpressionList.class);
				if (funcNode.size() == 0) {
					return null;
				}
				ASTArgumentExpressionList decNode = (ASTArgumentExpressionList) funcNode.get(0);
				if (decNode.jjtGetNumChildren() != 1) {
					return null;
				}
				List paramsNode = decNode.findChildrenOfType(ASTUnaryExpression.class);
				if (paramsNode.size() == 0) {
					return null;
				}
				ASTUnaryExpression paramNode = (ASTUnaryExpression) paramsNode.get(0);
				ExpressionValueVisitor exp = new ExpressionValueVisitor();
				ExpressionVistorData domaindata = new ExpressionVistorData();
				domaindata.currentvex = node.getCurrentVexNode();
				Object re = paramNode.getVariableDecl();
				exp.visit((SimpleNode) paramNode, domaindata);

				Expression value1 = domaindata.value;
				Domain mydomain = null;
				if (value1 != null)
					mydomain = value1.getDomain(domaindata.currentvex
							.getSymDomainset());
				if (isUFMPostVar(mydomain, re, node, feature)) {
					Variable variable = Variable
							.getVariable((VariableNameDeclaration) re);
					if (variable != null) {
						feature.addVariable(variable, "���ļ�"
								+ node.getFileName() + "�к���"
								+ this.methodDecl.getImage() + "��"
								+ node.getBeginLine() + "�д����ͷ�");
					}
				}
			} else {
				if (mtSummary != null) {
					for (MethodFeature postFeature : mtSummary
							.getPostConditions()) {
						if (postFeature instanceof MethodUFMPostCondition) { // �˺�����UFM������Ϣ
							MethodUFMPostCondition postUFMCondition = (MethodUFMPostCondition) postFeature; // �˺�����UFM������Ϣ
							for (Variable variable : postUFMCondition
									.getUFMVariables()) {
								if (feature.contains(variable)) { // ����ǰ�������д˱���������Ϣ�����ټ�¼
									continue;
								}
								ArrayList<String> desp = postUFMCondition
										.getDesp(variable); // �˱����ĵ�ǰ������Ϣ����
								// ������������
								desp.add("���ļ�" + node.getFileName() + "�к���"
										+ this.methodDecl.getImage() + "��"
										+ node.getBeginLine() + "�д�");
								feature.addVariable(variable, desp);
							}
						}
					}
				}
			}
		}
		return null;
	}

//	@Override
//	public Object visit(ASTqualified_id node, Object data) {
//		MethodUFMPostCondition feature = (MethodUFMPostCondition)data;
//		//���ܵĺ������ý�㣬һ����free(p)�����p�Ƿ�Ϊ������������ı�������һ�����Ǻ�����Ϣ�Ĵ���
//		if(node.isMethod() && node.getNameDeclaration() != null) {
//			MethodNameDeclaration methodDecl = (MethodNameDeclaration) node.getNameDeclaration();
//			if(methodDecl.isLib() && methodDecl.getName().equals("free")) {
//				//ϵͳ�⺯����free(p)
//				List funcNode = node.getParentsOfType(ASTdeclaration.class);
//				if (funcNode.size() == 0) {
//					return null;
//				}
//				ASTdeclaration decNode = (ASTdeclaration)funcNode.get(0);
//				if (decNode.jjtGetNumChildren() != 2) {
//					return null;
//				}
//				ExpressionValueVisitor exp = new ExpressionValueVisitor();
//				ExpressionVistorData domaindata = new ExpressionVistorData();
//				Object re  = node.getVariableNameDeclaration();
//				exp.visit((SimpleNode)node.jjtGetChild(0), domaindata);
//				if(isUFMPostVar(domainData, re, node, feature)) {
//					Variable variable = Variable.getVariable((VariableNameDeclaration)re);
//					if(variable != null) {
//						feature.addVariable(variable, "���ļ�" + node.getFileName() + "�к���"  + this.methodDecl.getName() + "��" + node.getBeginLine() + "�д����ͷ�");
//					}
//				}
//			} else {
//				if(methodDecl.isLib()) {
//					return null;
//				}
//				//�û��Զ��庯��
//				MethodSummary mtSummary = methodDecl.getMethodSummary();
//				if(mtSummary != null) {
//					for(MethodFeature postFeature : mtSummary.getPostConditions()) {
//						if(postFeature instanceof MethodUFMPostCondition) {		//�˺�����UFM������Ϣ
//							MethodUFMPostCondition postUFMCondition = (MethodUFMPostCondition) postFeature;		//�˺�����UFM������Ϣ
//							for(Variable variable : postUFMCondition.getUFMVariables()) {
//								if(feature.contains(variable)) {				//����ǰ�������д˱���������Ϣ�����ټ�¼
//									continue;
//								}
//								ArrayList<String> desp = postUFMCondition.getDesp(variable);	//�˱����ĵ�ǰ������Ϣ����
//									//������������
//								desp.add("���ļ�" + node.getFileName() + "�к���"  + this.methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
//								feature.addVariable(variable, desp);
//							}
//						}
//					}
//				}
//			}
//		}
//		return null;
//	}
	
	/**
	 *	���obj�Ƿ�Ϊ�������������ָ����� 
	 */
	private boolean isUFMPostVar(Domain domaindata, Object obj, SimpleNode node, MethodUFMPostCondition feature) {
		if(!(obj instanceof VariableNameDeclaration)) {		//�Ƿ�Ϊ����
			return false;
		}
		VariableNameDeclaration var = (VariableNameDeclaration) obj;
		if(feature.contains(var)) {							
			return false;
		}
		if(!(var.getType() instanceof CType_Pointer)) {	//�Ƿ�Ϊָ������
			return false;
		}
		if(!(var.getScope() instanceof ClassScope || var.getScope() instanceof SourceFileScope)) {		//�Ƿ�Ϊȫ�ֻ������Ա����
			return false;
		}
		if(domaindata != null && domaindata instanceof PointerDomain) {
			PointerDomain pd = (PointerDomain) domaindata;
			if(pd.getDomaintype() != DomainType.UNKNOWN && pd.getValue() != PointerValue.NULL) {			//�����Ƿ�Ϊ��
				return false;								
			}
		}
		VexNode vex = node.getCurrentVexNode();
		if(vex == null || vex.getContradict()) {			//�Ƿ��ڲ��ɴ�·��
			return false;
		}
		return true;
	}
	
}
