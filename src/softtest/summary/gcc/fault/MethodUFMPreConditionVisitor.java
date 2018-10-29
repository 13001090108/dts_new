package softtest.summary.gcc.fault;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.ASTUnaryOperator;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.PointerDomain;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.summary.c.MethodFeature;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.ClassScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;

public class MethodUFMPreConditionVisitor extends CParserVisitorAdapter implements MethodFeatureVisitor {

	private static MethodUFMPreConditionVisitor instance;
	
	private MethodNameDeclaration methodDecl;
	
	private MethodUFMPreConditionVisitor() {
	}
	
	public static MethodUFMPreConditionVisitor getInstance() {
		if(instance == null) {
			instance = new MethodUFMPreConditionVisitor();
		}
		return instance;
	}

	public void visit(VexNode vexNode) {
		if (vexNode.getTreenode() == null) {
			return;
		}
		MethodUFMPreCondition feature = new MethodUFMPreCondition();
		
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
			summary.addPreCondition(feature);
			if (Config.INTER_METHOD_TRACE) {
				if (methodDecl != null) {
					System.err.println(methodDecl.getFullName() + " " + feature);
				}
			}
		}
	}
	
	/**
	 * �����ָ��Ĳ���ȡֵ���磬*a
	 */
	@Override
	public Object visit(ASTUnaryExpression node, Object data) {
		MethodUFMPreCondition feature = (MethodUFMPreCondition)data;
		
		if (node.jjtGetNumChildren() != 2) {
			return super.visit(node, data);
		}
		if (!(node.jjtGetChild(0) instanceof ASTUnaryOperator) || !((ASTUnaryOperator) node.jjtGetChild(0)).getOperators().equals("*")) {
			return super.visit(node, data);
		}
		
		// ��ȡָ�����
		ExpressionValueVisitor exp = new ExpressionValueVisitor();
		ExpressionVistorData domaindata = new ExpressionVistorData();
		domaindata.currentvex = node.getCurrentVexNode();
		ASTUnaryExpression u = (ASTUnaryExpression) (SimpleNode)node.getFirstChildOfType(ASTUnaryExpression.class);
		VariableNameDeclaration re = null;
		if(u != null)
		  re = u.getVariableDecl();
		exp.visit((SimpleNode)node, domaindata);
		if (re== null) {
			return super.visit(node, data);
		}
		Domain domain = domaindata.currentvex.getDomain((VariableNameDeclaration) re);
		if (re != null && !isUFMVar(domain, re, node, feature)) {
			return super.visit(node, data);
		}
		Variable variable = Variable.getVariable((VariableNameDeclaration)re);
		if (variable != null) {
			feature.addVariable(variable, "���ļ�" + node.getFileName() + "�к���"  + methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
		}
		return super.visit(node, data);
	}
	
	/**
	 * ����Խṹ��ָ���ȡ��Ա�Ĳ����Լ�ָ�밴����ȡ�±����磬a->b, a[1]
	 */
	@Override
	public Object visit(ASTPostfixExpression node, Object data) {
		if (node.jjtGetNumChildren() <= 1) {
			return super.visit(node, data);
		}
		
		MethodUFMPreCondition feature = (MethodUFMPreCondition)data;
		// �ж��Ƿ���[], ->����
		ArrayList<String> operators = node.getOperatorType();
		if (operators.size() != 0) {
			if (!operators.get(0).equals("[") && !operators.get(0).equals("->")) {
				return super.visit(node, data);
			}
			// ��ȡָ�����
			ExpressionValueVisitor exp = new ExpressionValueVisitor();
			ExpressionVistorData domaindata = new ExpressionVistorData();
			domaindata.currentvex = node.getCurrentVexNode();
			Object re  = ((SimpleNode)node.jjtGetChild(0)).getVariableNameDeclaration();
			exp.visit((SimpleNode)node, domaindata);
			if (re== null) {
				return super.visit(node, data);
			}
			Domain domain = domaindata.currentvex.getDomain((VariableNameDeclaration) re);
			if (re != null && !isUFMVar(domain, re, node, feature)) {
				return super.visit(node, data);
			}
			
			Variable variable = Variable.getVariable((VariableNameDeclaration)re);
			if (variable != null) {
			    feature.addVariable(variable, "���ļ�" + node.getFileName() + "�к���"  + methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
			}
			return super.visit(node, data);
		}
		return super.visit(node, data);
	}
	
	/**
	 * ���������ù����У������ú�������Ҫ����UFMǰ������
	 */
	@Override
	public Object visit(ASTPrimaryExpression node, Object data) {
		MethodUFMPreCondition feature = (MethodUFMPreCondition)data;
		if (node.isMethod() && node.getMethodDecl() != null) {
			// �����ú������ܶ�ʵ����UFM��ǰ��Լ��
			
			Set<Variable> argVars = new HashSet<Variable>();	//���ܴ���UFMԼ����ʵ��
			MethodNameDeclaration methodDecl = (MethodNameDeclaration)node.getMethodDecl();
			MethodUFMPreCondition subUFMPreCond = null;
			// ��鱻���ú������Ƿ��ж��βε�UFMǰ��Լ��
			if (methodDecl.getMethodSummary() != null) {
				Set<MethodFeature> npdPreConds = methodDecl.getMethodSummary().getPreConditions();
				Set<Variable> vars = null;
				for (MethodFeature temp : npdPreConds) {
					if (temp instanceof MethodUFMPreCondition) {
						subUFMPreCond = (MethodUFMPreCondition)temp;
						vars = subUFMPreCond.getUFMVariables();
						break;
					}
				}
				if (vars != null) {
					for (Variable var : vars) {
//						if (var.isParam()) {
							argVars.add(var);
//						}
					}
				}
			}
			
			// ��麯�����ô������ݵĲ����Ƿ���βε�Ҫ��һ�£�һ�µĻ�����������UFMԼ��
			List<SimpleNode> argNodes = getArgsNode(node);
			for (Variable var : argVars) {
				if (argNodes.size() != 0) {     // ʵ��
				    if (var.getParamIndex() >= argNodes.size() || var.getParamIndex() == -1) {
				        break;
				    }
					ExpressionValueVisitor exp = new ExpressionValueVisitor();
					ExpressionVistorData domaindata = new ExpressionVistorData();
					domaindata.currentvex = node.getCurrentVexNode();
					Object re  = getArgsNode(node).get(var.getParamIndex()).getVariableNameDeclaration();
					exp.visit((SimpleNode)node, domaindata);
				    
					if (re == null) {
						continue;
					}
					Domain domain = domaindata.currentvex.getDomain((VariableNameDeclaration) re);
				    if (!isUFMVar(domain, re, node, feature)) {
				        return null;
				    }
				    
				    ArrayList<String> des = subUFMPreCond.getDesp(var);
				    if (methodDecl.isLib()) {
				        des = new ArrayList<String>();
				    }
				    des.add("���ļ�" + node.getFileName() + "�к���"  + this.methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
				    Variable variable = Variable.getVariable((VariableNameDeclaration)re);
				    if (variable != null) {
				        feature.addVariable(variable, des);
				    }
				} else {                        // ȫ�� ���Ա
                    ArrayList<String> des = subUFMPreCond.getDesp(var);
                    if (methodDecl.isLib()) {
                        des = new ArrayList<String>();
                    }
                    des.add("���ļ�" + node.getFileName() + "�к���"  + this.methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
                    feature.addVariable(var, des);
                }
			}
		}
		return super.visit(node, data);
	}

	
	/**
	 * �ж�ĳ�������Ƿ���϶�Ӧ��UFMǰ������
	 */
	private boolean isUFMVar(Domain domaindata, Object obj, SimpleNode node, MethodUFMPreCondition feature) {
		if (!(obj instanceof VariableNameDeclaration)) {
			return false;
		}
		
		VariableNameDeclaration var = (VariableNameDeclaration)obj;
		if (feature.contains(var)) {
			return false;
		}

		// ȷ�ϸñ���������ָ�룬���ǲ����������߳�Ա������ȫ�ֱ���
		//zys��������Ϣ�п���Ϊ�գ�����ʱ��ô�İ�
		if (var.getType()==null || !var.getType().isPointType()) {
			return false;
		}
		
		// ���ָ���ڴ˵��Ѿ��Ƿǿ�ֵ�򣬲���ǰ��Լ��
		if (domaindata != null && domaindata instanceof PointerDomain) {
			PointerDomain pDomain = (PointerDomain)domaindata;
			if (pDomain.getDomaintype() != DomainType.UNKNOWN && !pDomain.offsetRange.contains(0)) {
				return false;
			}
		}
		// �ж��Ƿ�Ϊ��Ա��ȫ�ֱ���������������,xwt��2012/11/21ע��
//		if (!var.isParam() && !(var.getScope() instanceof ClassScope || var.getScope() instanceof SourceFileScope)){
//			return false;
//		}
		
		// �жϸñ���֮ǰ��û�б����¸�ֵ
		VexNode vex = node.getCurrentVexNode();
		if (vex == null || vex.getContradict()) {
			return false;							//��ȷ�����ڲ��ɴ�·���ϣ��򲻼�¼ժҪ
		}
		for(NameOccurrence occ:vex.getOccurrences()){
			if(occ.getDeclaration()== var){
				List<NameOccurrence> occs = occ.getUse_def();
				if(occs!=null && !occs.isEmpty()){
					if (occs.size() == 1 && occs.get(0).getDeclaration() instanceof VariableNameDeclaration) {
						VariableNameDeclaration decl = (VariableNameDeclaration)occs.get(0).getDeclaration();
						if (decl.isParam()) {		//������һ��������ֵ������Ҫ��¼ժҪ
							return true;
						}
					}
					return false;					//��������ַ��ֵ�������·���ռ䣬����¼ժҪ
				}
			}
		}
		return true;
	}

	/**
	 * ��ȡ��ǰ�﷨���ڵ㺢���е�����ʵ�νڵ�
	 */
	private List<SimpleNode> getArgsNode(ASTPrimaryExpression node) {
		List<SimpleNode> argNodes = new ArrayList<SimpleNode>();
		ASTPrimaryExpression primary = null;
		ASTPostfixExpression postfix = null;
		if (node.jjtGetParent() instanceof ASTPrimaryExpression) {
			primary = (ASTPrimaryExpression) node.jjtGetParent();
			postfix = (ASTPostfixExpression) node.jjtGetParent().jjtGetParent();
		} else if (node.jjtGetParent() instanceof ASTPostfixExpression) {
			postfix = (ASTPostfixExpression) node.jjtGetParent();
			if (postfix.jjtGetNumChildren() > 1
					&& postfix.jjtGetChild(0) instanceof ASTPrimaryExpression) {
				primary = (ASTPrimaryExpression) postfix.jjtGetChild(0);
			}
		}
		ASTArgumentExpressionList args = null;
		if (primary != null && postfix != null) {
			for (int i = 0; i < postfix.jjtGetNumChildren(); i++) {
				if (postfix.jjtGetChild(i) == primary && i + 1 < postfix.jjtGetNumChildren() && 
						postfix.jjtGetChild(i+1) instanceof ASTArgumentExpressionList) {
					args = (ASTArgumentExpressionList)postfix.jjtGetChild(i + 1);
				}
			}
		}
		if (args == null) {
			return argNodes;
		}
		for (int i = 0; i < args.jjtGetNumChildren(); i++) {
			argNodes.add((SimpleNode)args.jjtGetChild(i));
		}
		return argNodes;
	}
	
}
