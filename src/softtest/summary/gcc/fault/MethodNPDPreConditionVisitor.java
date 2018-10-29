package softtest.summary.gcc.fault;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTEqualityExpression;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.ASTUnaryOperator;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ConditionData;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.StateMachineUtils;
import softtest.rules.gcc.fault.NPDStateMachine;
import softtest.summary.c.MethodFeature;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.ClassScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * 
 * @author cjie
 *
 */
public class MethodNPDPreConditionVisitor extends CParserVisitorAdapter implements MethodFeatureVisitor {

	private static MethodNPDPreConditionVisitor instance;
	
	private MethodNPDPreConditionVisitor() {	
	}
	MethodNameDeclaration methodDecl = null;
	
	public static MethodNPDPreConditionVisitor getInstance() {
		if (instance == null) {
			instance = new MethodNPDPreConditionVisitor(); 
		}
		return instance;
	}
	
	public void visit(VexNode vexNode) {
		if (vexNode.getTreenode() == null) {
			return;
		}
		MethodNPDPreCondition feature = new MethodNPDPreCondition();
		
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
		
		// ��������ĺ���������ӵ�����ժҪ��
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
	 * �ж�ĳ�������Ƿ���϶�Ӧ��NPDǰ������
	 * @param var
	 * @param node
	 * @param feature
	 * @return
	 */
	private boolean isNpdVar(Domain domain, Object obj, SimpleNode node, MethodNPDPreCondition feature) {
		if (!(obj instanceof VariableNameDeclaration)) {
			return false;
		}
		
		VariableNameDeclaration var = (VariableNameDeclaration)obj;
		if (feature.contains(var)) {
			return false;
		}

		// ȷ�ϸñ���������ָ�룬���ǲ����������߳�Ա����
		if (var.getType()!=null&&!(var.getType().isPointType())) {
			return false;
		}

		if (domain != null && !domain.isUnknown() /*&& !(domain instanceof PointerDomain)*/) {
			return false;
		}
		// ���ָ���ڴ˵��Ѿ��Ƿǿ�ֵ�򣬲���ǰ��Լ��
		if (domain != null && !domain.isUnknown()&& domain instanceof PointerDomain) {
			PointerDomain pDomain = (PointerDomain)domain;
			if (pDomain.getValue() !=  null) {
				return false;
			}
		}
		// �ж��Ƿ�Ϊ��Ա������������
		if (!var.isParam() && !(var.getScope() instanceof ClassScope || var.getScope() instanceof SourceFileScope)){
			return false;
		}
		
		if (!NPDStateMachine.confirmNPD(node, (VariableNameDeclaration)obj)) {
			return false;
		}
		// �жϸñ���֮ǰ��û�б����¸�ֵ
		VexNode vex= node.getCurrentVexNode();
		if (vex == null || vex.getContradict()) {
			return false;
		}
		for(NameOccurrence occ:vex.getOccurrences()){
			if(occ.getDeclaration()== var){
				List<NameOccurrence> occs = occ.getUse_def();
				if(occs!=null && !occs.isEmpty()){
					if (occs.size() == 1 && occs.get(0).getDeclaration() instanceof VariableNameDeclaration) {
						VariableNameDeclaration decl = (VariableNameDeclaration)occs.get(0).getDeclaration();
						if (decl.isParam()) {
							return true;
						}
					}
					return false;
				}
			}
		}
        // add by suntao �жϸñ����Ƿ�����NPD_PreCheck������(���Ա�����if�����жϣ���0�Ƚ�)������ǣ��򲻼�¼ժҪ
		String xpath = ".//PrimaryExpression[@Method='false'][@Image='" + var.getImage() + "']";
		List funcNodes = node.getParentsOfType(ASTFunctionDefinition.class);
        List<SimpleNode> results = StateMachineUtils.getEvaluationResults((SimpleNode)funcNodes.get(0), xpath);
        for(SimpleNode snode : results) {
        	if (!node.isSelOrAncestor(snode)) {
        		continue;
        	}
            VexNode vexnode = snode.getCurrentVexNode();
            if(vexnode != null && vexnode.getName().startsWith("if_head")) {
                List equality = snode.getParentsOfType(ASTEqualityExpression.class);
                if(equality.size() != 0) {      // if(p != 0)
                	ASTEqualityExpression equalityExp = (ASTEqualityExpression) equality.get(0);
                    if(equalityExp.jjtGetNumChildren() == 2 && equalityExp.getOperators().equals("!=")) {
                        SimpleNode child1 = (SimpleNode) equalityExp.jjtGetChild(0);
                        SimpleNode child2 = (SimpleNode) equalityExp.jjtGetChild(1);
                        if(/*(child1.getImage().equals("0") || child2.getImage().equals("0"))
                                &&*/ (child1.getImage().equals(var.getImage())) || child2.getImage().equals(var.getImage())) {
                            if(child1.findChildrenOfType(ASTPrimaryExpression.class).size() == 1 && child2.findChildrenOfType(ASTPrimaryExpression.class).size() == 1) {
                                String path = ".//JumpStatement|.//PrimaryExpression[@Image='exit'][@Method='true']";
                                if(StateMachineUtils.getEvaluationResults(snode, path).isEmpty()) {
                                    return false;
                                }
                            }
                        }
                    }
                } else {                        // if(p)
                    ConditionData condition = vexnode.getCondata();
                    if(condition != null) {
                    	Expression ve=vexnode.getValue(var);
                    	if (ve == null || ve.getSingleFactor() == null) {
                    		return false;
                    	}
                        Domain may = condition.getMayDomain((SymbolFactor) ve.getSingleFactor());
                        Domain must = condition.getMustDomain((SymbolFactor) ve.getSingleFactor());
                        if(may != null && must != null && may instanceof PointerDomain) {
                        	PointerDomain pDomain = (PointerDomain) may;
                        	if (pDomain.getValue() == PointerValue.NOTNULL) {
                        		return false;
                        	}
                        	return true;
                        }
                    }
                }
            }
        }
        return true;
	}
	
	/**
	 * ��ȡ��ǰ�﷨���ڵ㺢���е�����ʵ�νڵ�
	 * @param node
	 * @return
	 */
	private List<SimpleNode> getArgsNode(ASTPrimaryExpression node) {
		List<SimpleNode> argNodes = new ArrayList<SimpleNode>();
		ASTPrimaryExpression primary = null;
		ASTPostfixExpression postfix = null;
		if (node.jjtGetParent() instanceof ASTPostfixExpression) {
			primary = (ASTPrimaryExpression) node;
			postfix = (ASTPostfixExpression) node.jjtGetParent();
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
	
	/**
	 * �����ָ��Ĳ���ȡֵ���磬*a
	 */
	@Override
	public Object visit(ASTUnaryExpression node, Object data) {
		MethodNPDPreCondition feature = (MethodNPDPreCondition)data;
		
		if (node.jjtGetNumChildren() != 2) {
			return super.visit(node, data);
		}
		if (!(node.jjtGetChild(0) instanceof ASTUnaryOperator) || !((ASTUnaryOperator) node.jjtGetChild(0)).getOperators().equals("*")) {
			return super.visit(node, data);
		}
		
		if (!(node.jjtGetChild(1) instanceof ASTUnaryExpression) ) {
			return super.visit(node, data);
		}
		
		// ��ȡָ�����
		ExpressionValueVisitor exp = new ExpressionValueVisitor();
		ExpressionVistorData domaindata = new ExpressionVistorData();
		domaindata.currentvex = node.getCurrentVexNode();
		
		//modified by nmh ���ǵ����ӽṹ������
		//Object re  = ((SimpleNode)node.jjtGetChild(1).jjtGetChild(0)).getVariableNameDeclaration();
		Object re = null;
		ASTUnaryExpression unarynode = (ASTUnaryExpression)node.jjtGetChild(1);
		NameDeclaration decl = null;
		VariableNameDeclaration paramDecl = null;
		decl = unarynode.getVariableDecl();
		if (decl instanceof VariableNameDeclaration) {
			paramDecl = (VariableNameDeclaration) decl;
		}
		if(paramDecl != null)
			re = paramDecl;
		
		exp.visit((SimpleNode)node, domaindata);
		if (re== null) {
			return super.visit(node, data);
		}
		Expression value1 = domaindata.value;
		Domain mydomain=null;
		if(value1!=null)
		mydomain = value1.getDomain(domaindata.currentvex.getSymDomainset());
		if (!isNpdVar(mydomain, re, node, feature)) {
			return super.visit(node, data);
		}
		Variable variable = Variable.getVariable((VariableNameDeclaration)re);
		if (variable != null) {
			feature.addVariable(variable, "���ļ�" + node.getFileName() + "�к���"  + methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
		}
		return super.visit(node, data);
	}
	
	/**
	 * 
	 * ���������ù����У������ú�������Ҫ����NPDǰ��ǰ������
	 */
	@Override
	public Object visit(ASTPrimaryExpression node, Object data) {
		MethodNPDPreCondition feature = (MethodNPDPreCondition)data;
		if (node.isMethod() && node.getMethodDecl() != null) {
			// �����ú������ܶ�ʵ����NPD��ǰ��Լ��
			
			Set<Variable> argVars = new HashSet<Variable>();	//���ܴ���NPDԼ����ʵ��
			MethodNameDeclaration methodDecl = (MethodNameDeclaration)node.getMethodDecl();
			MethodNPDPreCondition subNpdPreCond = null;
			// ��鱻���ú������Ƿ��ж��βε�NPDǰ��Լ��
			if (methodDecl.getMethodSummary() != null) {
				Set<MethodFeature> npdPreConds = methodDecl.getMethodSummary().getPreConditions();
				Set<Variable> vars = null;
				for (MethodFeature temp : npdPreConds) {
					if (temp instanceof MethodNPDPreCondition) {
						subNpdPreCond = (MethodNPDPreCondition)temp;
						vars = subNpdPreCond.getNPDVariables();
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
			
			// ��麯�����ô������ݵĲ����Ƿ���βε�Ҫ��һ�£�һ�µĻ�����������NPDԼ��
			List<SimpleNode> argNodes = getArgsNode(node);
			for (Variable var : argVars) {
				if (argNodes.size() != 0) {
				    if (var.getParamIndex() >= argNodes.size() || var.getParamIndex() == -1) {
				        break;
				    }
					ExpressionValueVisitor exp = new ExpressionValueVisitor();
					ExpressionVistorData domaindata = new ExpressionVistorData();
					domaindata.currentvex = node.getCurrentVexNode();
					Object re  = ((SimpleNode)argNodes.get(var.getParamIndex())).getVariableNameDeclaration();
					exp.visit((SimpleNode)argNodes.get(var.getParamIndex()), domaindata);
				    
					
					if (re != null) {
						Expression value1 = domaindata.value;
						Domain mydomain=null;
						if(value1!=null)
						mydomain = value1.getDomain(domaindata.currentvex.getSymDomainset());
						if (!isNpdVar(mydomain, re,
								node, feature)) {
							return super.visit(node, data);
						}
					}
				    ArrayList<String> des = subNpdPreCond.getDesp(var);
				    if (methodDecl.isLib()) {
				        des = new ArrayList<String>();
				    }
				    des.add("���ļ�" + node.getFileName() + "�к���"  + methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
				    Variable variable = Variable.getVariable((VariableNameDeclaration)re);
				    if (variable != null) {
				        feature.addVariable(variable, des);
				    }
				} else {
                    ArrayList<String> des = subNpdPreCond.getDesp(var);
                    if (methodDecl.isLib()) {
                        des = new ArrayList<String>();
                    }
                    des.add("���ļ�" + node.getFileName() + "�к���"  + this.methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
                    feature.addVariable(var, des);
                }
			}
		}
		return null;
	}
	
	/**
	 * ����Խṹ��ָ���ȡ��Ա�Ĳ����Լ�ָ�밴����ȡ�±����磬a->b, a[1]
	 */
	@Override
	public Object visit(ASTPostfixExpression node, Object data) {
		if (node.jjtGetNumChildren() <= 1) {
			return super.visit(node, data);
		}
		
		MethodNPDPreCondition feature = (MethodNPDPreCondition)data;
		// �ж��Ƿ���[], ->����
		ArrayList<String> operators = node.getOperatorType();
		if (operators.size() != 0) {
			if (!operators.get(0).equals("[") && !operators.get(0).equals("->")) {
				return super.visit(node, data);
			}
			// ��ȡָ�����
			ExpressionValueVisitor exp = new ExpressionValueVisitor();
			ExpressionVistorData domaindata = new ExpressionVistorData();
			domaindata.currentvex = ((SimpleNode)node.jjtGetChild(0)).getCurrentVexNode();
			Object re  = ((SimpleNode) node.jjtGetChild(0)).getVariableNameDeclaration();
			exp.visit((SimpleNode)node.jjtGetChild(0), domaindata);
			
			if (re != null) {
				Expression value1 = domaindata.value;
				Domain mydomain=null;
				if(value1!=null)
				mydomain = value1.getDomain(domaindata.currentvex.getSymDomainset());
				if (!isNpdVar(mydomain, re, node, feature)) {
					return super.visit(node, data);
				}
			} else {
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
	 * Ŀǰ�﷨����Щ�������ñ�ʶ��������������Ҫ�ڴ������⻯����,��Щ���ö���һ�������ĵ���
	 */ 
//	@Override
//	public Object visit(ASTqualified_id node, Object data) {
//		MethodNPDPreCondition feature = (MethodNPDPreCondition)data;
//		if (node.isMethod() && node.getNameDeclaration() != null) {
//			// �����ú������ܶ�ʵ����NPD��ǰ��Լ��
//			
//			Variable argVar = null;	//���ܴ���NPDԼ����ʵ��
//			MethodNameDeclaration methodDecl = (MethodNameDeclaration)node.getNameDeclaration();
//			MethodNPDPreCondition subNpdPreCond = null;
//			// ��鱻���ú������Ƿ��ж��βε�NPDǰ��Լ��
//			if (methodDecl.getMethodSummary() != null) {
//				Set<MethodFeature> npdPreConds = methodDecl.getMethodSummary().getPreConditions();
//				Set<Variable> vars = null;
//				for (MethodFeature temp : npdPreConds) {
//					if (temp instanceof MethodNPDPreCondition) {
//						subNpdPreCond = (MethodNPDPreCondition)temp;
//						vars = subNpdPreCond.getNPDVariables();
//						break;
//					}
//				}
//				if (vars != null) {
//					for (Variable var : vars) {
//						if (var.getParamIndex() == 0) {
//							argVar = var;
//						}
//					}
//				}
//			}
//			if (argVar == null) {
//				return null;
//			}
//			
//			// ��麯�����ô������ݵĲ����Ƿ���βε�Ҫ��һ�£�һ�µĻ�����������NPDԼ��
//			List funcNode = node.getParentsOfType(ASTDeclaration.class);
//			if (funcNode == null || funcNode.size() <= 0) {
//				return null;
//			}
//			ASTDeclaration decNode = (ASTDeclaration)funcNode.get(0);
//			if (decNode.jjtGetNumChildren() != 2) {
//				return null;
//			}
//			ExpressionDomainVisitor exp = new ExpressionDomainVisitor();
//			DomainData domaindata = new DomainData(false);
//			Object re = exp.visit((SimpleNode)decNode.jjtGetChild(1), domaindata);
//			if (!isNpdVar(domaindata, re, node, feature)) {
//				return null;
//			}
//			ArrayList<String> des = subNpdPreCond.getDesp(argVar);
//			if (methodDecl.isLib()) {
//				des = new ArrayList<String>();
//			}
//		    des.add("���ļ�" + node.getFileName() + "�к���"  + methodDecl.getImage() + "��" + node.getBeginLine() + "�д�");
//			Variable variable = Variable.getVariable((VariableNameDeclaration)re);
//			if (variable != null) {
//				feature.addVariable(variable, des);
//			}
//		}
//		return null;
//	}
}
