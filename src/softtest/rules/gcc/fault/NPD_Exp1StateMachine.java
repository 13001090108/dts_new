package softtest.rules.gcc.fault;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTAdditiveExpression;
import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTFieldId;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.domain.c.symbolic.Expression;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.Method;
import softtest.interpro.c.Variable;
import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.gcc.fault.MethodNPDPreCondition;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_AbstPointer;

/**
 * ����ָ����ʽ��NPDʽ����
 * �������磺
 * 1��st->i���򵥽ṹ������Ŀ�ָ�����ô���ֻ�����һ����Ա����
 * 2��p+i, ָ����ʽ�Ŀ�ָ�����ã�ֻ����ָ�������ͣ�+��-��������ʽ�Ŀ�ָ������
 * 3��func(),����������ֵ�Ŀ�ָ������
 */
public class NPD_Exp1StateMachine extends BasicStateMachine {
	
	/**
	 * NPD���ı��ʽΪ�ṹ��ĳ�Ա��������ֵ
	 */
	private final static int FIELD_NPDEXP_TYPE = 0;
	
	/**
	 * NPD���ı��ʽΪָ�������������������ʽ���ߺ������ñ��ʽ����ֵ
	 */
	private final static int ASSGIN_NPDEXP_TYPE = 1;
	
	/**
	 * NPD���ı��ʽΪ�ṹ��ĳ�Ա��������Ϊ�����Ĳ���
	 */
	private final static int PARAM_NPDEXP_TYPE = 2;
	
	
	public static List<FSMMachineInstance> createNPD_ExpStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new ArrayList<FSMMachineInstance>();
		Set<String> expStrs = new HashSet<String>();
		String xPath = 
			".//UnaryExpression/UnaryExpression//AssignmentExpression/UnaryExpression/UnaryExpression/PostfixExpression[ends-with(@Operators,'->')]/PrimaryExpression|" +
			".//AssignmentExpression//UnaryExpression/PostfixExpression[count(*)>2 and ends-with(@Operators,'[')]/PrimaryExpression|" +
			".//AssignmentExpression//UnaryExpression/PostfixExpression/PrimaryExpression[../../../UnaryOperator[@Operators='*']]|" +
			//���ӽṹ��༶��Ա�����������÷���ֵ�Ĵ��� ����a->b->c����a->func()->b 
			".//AssignmentExpression//UnaryExpression/PostfixExpression[ends-with(@Operators,']') or ends-with(@Operators,'->')][count(./PrimaryExpression)>=2]/PrimaryExpression[position()!=last()][@Method='true']";
			
		List<SimpleNode> nodes = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode snode : nodes) {
			FSMMachineInstance fsmInstance = fsm.creatInstance();
			fsmInstance.setRelatedASTNode(snode);
			checkExpType(fsmInstance, snode, expStrs, list);
		}
		return list;
	}
	
	/**
	 * ���ݵ�ǰָ�����õı��ʽ�����﷨���ڵ����ͣ�ȷ����ǰ��ָ�����õ����ͣ�����ֵ���������ʽ��������ֵ�Ľṹ���Ա
	 * ͬʱ���ݳ����﷨���ڵ㣬ȷ�����ʽ�����ݣ�ͬ�����ݵı��ʽ���ظ�����״̬�����м��
	 * @param fsmin
	 * @param expNode
	 * @param expStrs
	 * @param list
	 */
	private static void checkExpType(FSMMachineInstance fsmin, SimpleNode expNode, Set<String> expStrs, List<FSMMachineInstance> list) {
		String expStr = null;
		if(expNode instanceof ASTPrimaryExpression  && expNode.jjtGetParent() instanceof ASTPostfixExpression) {
			
			if (expNode.jjtGetNumChildren() ==0 && expNode.jjtGetParent().jjtGetChild(0) instanceof ASTPrimaryExpression) {
				ASTPrimaryExpression idExp = (ASTPrimaryExpression)expNode.jjtGetParent().jjtGetChild(0);
				if (idExp.isMethod()) {
					// *func()
					expStr = getExpStr(idExp);
					if (expStr != null && !expStrs.contains(expStr)) {
						fsmin.setStateData(new ExpInfo(ASSGIN_NPDEXP_TYPE, true));
						fsmin.setResultString(expStr);
						expStrs.add(expStr);
						list.add(fsmin);
					}
				} else {
					// field npd, ���� p->a, s.i
					expStr = getExpStr(expNode);
					if (expStr != null && !expStrs.contains(expStr)) {
						fsmin.setStateData(new ExpInfo(FIELD_NPDEXP_TYPE, false));
						fsmin.setResultString(expStr);
						expStrs.add(expStr);
						list.add(fsmin);
					} else {
						SimpleNode parent = (SimpleNode)expNode.jjtGetParent();
						if (parent.jjtGetNumChildren() == 2 /*&& (parent.getSecondImage().equals(".") || parent.getSecondImage().equals("->"))*/) {
							// check if the npd exp is a function parameter
							ASTArgumentExpressionList expListNode = (ASTArgumentExpressionList)parent.getFirstParentOfType(ASTArgumentExpressionList.class);
							if (expListNode != null) {
								AbstractExpression abstExp = (AbstractExpression)parent.jjtGetChild(1);
								if (abstExp.getType() instanceof CType_AbstPointer) {
									//expStr = expNode.getImage() + expNode.getSecondImage() + abstExp.getImage();
									fsmin.setStateData(new ExpInfo(PARAM_NPDEXP_TYPE, false));
									fsmin.setResultString(expStr);
									expStrs.add(expStr);
									list.add(fsmin);
								}
							}
						}
					}
				}
			} else {
				String xPath = "/Expression/AssignmentExpression/AdditiveExpression[count(*)>=2]";
				List<SimpleNode> re = StateMachineUtils.getEvaluationResults(expNode, xPath);
				if (re != null && re.size() == 1) {
					expStr = getExpStr((ASTAdditiveExpression)re.get(0));
					if (expStr != null && !expStrs.contains(expStr)) {
						fsmin.setStateData(new ExpInfo(ASSGIN_NPDEXP_TYPE, false));
						fsmin.setResultString(expStr);
						expStrs.add(expStr);
						list.add(fsmin);
					}
				}
				
				// ����*(a)
//				ASTPrimaryExpression idNode = (ASTPrimaryExpression)expNode.getSingleChildofType(ASTPrimaryExpression.class);
//				if (idNode != null) {
//					expStr = idNode.getImage();
//					if (!expStrs.contains(expStr)) {
//						fsmin.setStateData(new ExpInfo(ASSGIN_NPDEXP_TYPE, false));
//						fsmin.setResultString(expStr);
//						expStrs.add(expStr);
//						list.add(fsmin);
//					}
//				}
				
			}
		} else if(expNode instanceof ASTPrimaryExpression) {		//����ṹ��༶��Ա�������ߺ�������ֵ��ָ������
			ASTPrimaryExpression idExp = (ASTPrimaryExpression) expNode;
			if(idExp.isMethod()) {				//a->func()->b;
				expStr = getExpStr(idExp);
				if (expStr != null && !expStrs.contains(expStr)) {
					fsmin.setStateData(new ExpInfo(ASSGIN_NPDEXP_TYPE, true));
					fsmin.setResultString(expStr);
					expStrs.add(expStr);
					list.add(fsmin);
				}
			} else {							//a->b->c
				//�����
			}
		}
	}
	
	private static String getExpStr(SimpleNode node) {
		return getExpStr(node, 3);
	}
	/**
	 * ���ݳ����﷨���ڵ㣬ȷ�����ʽ�����ݣ�ͬ�����ݵı��ʽ���ظ�����״̬�����м��
	 */
	private static String getExpStr(SimpleNode node, int childNum) {
		if (node.jjtGetParent() instanceof ASTPostfixExpression) {
			ASTPostfixExpression postfix = (ASTPostfixExpression) node.jjtGetParent();
			 if (postfix.jjtGetNumChildren() >= childNum && postfix.getOperators().startsWith(".") || postfix.getOperators().equals("->")) {
				 ASTFieldId abstExp = (ASTFieldId) postfix.jjtGetChild(1);
				if (abstExp.getType() instanceof CType_AbstPointer) {
					if (postfix.getOperators().startsWith(".")) {
						return node.getImage() + "." + abstExp.getImage();
					}
					return node.getImage() + postfix.getOperators()	+ abstExp.getImage();
				}
			 } else if (node instanceof ASTPrimaryExpression) {
					return node.getImage();
			}
		} else if (node instanceof ASTAdditiveExpression) {
			AbstractExpression leftExp = (AbstractExpression) node
					.jjtGetChild(0);
			String str = leftExp.getImage();
			ArrayList<String> opts = node.getOperatorType();
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				leftExp = (AbstractExpression) node.jjtGetChild(i);
				str += opts.get(i - 1);
				str += leftExp.getImage();
			}
			return str;
		} else if (node instanceof ASTPrimaryExpression) {
			return node.getImage();
		}
		return null;
	}
	
	/**
	 * ��⵱ǰ��ָ����ʽ�������Ƿ����Ϊ��
	 * @param nodes
	 * @param fsmin
	 * @return
	 */
	public static boolean checkNPDExp(List nodes, FSMMachineInstance fsmin) {
		Iterator listIter = nodes.iterator();
		while (listIter.hasNext()) {
			SimpleNode expNode = (SimpleNode) listIter.next();
			if (fsmin.getRelatedASTNode() != expNode) {
				continue;
			}
			ExpInfo expinfo = (ExpInfo)fsmin.getStateData();
			boolean isError = false;
			if (expinfo.type == ASSGIN_NPDEXP_TYPE) {
				if (expNode instanceof ASTPrimaryExpression) {
					ASTPrimaryExpression idExp = (ASTPrimaryExpression)expNode;
					if (idExp.isMethod() && expinfo.isnull) {
						MethodNameDeclaration methodDecl = (MethodNameDeclaration)idExp.getMethodDecl();
						if (methodDecl != null && methodDecl.getMethod() != null) {
							Method method = methodDecl.getMethod();
							if (Config.USE_SUMMARY) {
								if (method.getReturnDomain() instanceof PointerDomain) {
									PointerDomain pd = (PointerDomain)method.getReturnDomain();
									if (pd.getValue() == PointerValue.NULL) {
										isError = true;
									}
								}
								if (method.getReturnDomain() instanceof IntegerDomain) {
									IntegerDomain id = (IntegerDomain)method.getReturnDomain();
									if (id.getMin() == id.getMax() && id.getMin() == 0) {
										isError = true;
									}
								}
							}
						}
					} else {
//						ExpressionDomainVisitor expVisitor = new ExpressionDomainVisitor();
//						DomainData domaindata = new DomainData(false);
//						expVisitor.visit(expNode, domaindata);
						String xPath = ".//PrimaryExpression";
						List<SimpleNode> rer = StateMachineUtils.getEvaluationResults(expNode, xPath);
						if (rer == null || rer.size() ==0) {
							continue;
						}
						ExpressionValueVisitor exp = new ExpressionValueVisitor();
						ExpressionVistorData domaindata = new ExpressionVistorData();
						domaindata.currentvex = rer.get(0).getCurrentVexNode();
						Object re  = rer.get(0).getVariableNameDeclaration();
						exp.visit((SimpleNode)rer.get(0), domaindata);
						if (re != null) {
							Domain domain = domaindata.currentvex.getDomain((VariableNameDeclaration)re);
							if (!(domain instanceof PointerDomain)) {
								continue;
							}
							PointerDomain pd = (PointerDomain)domain;
							if (pd.getValue() == PointerValue.NULL || pd.getValue() == PointerValue.NULL_OR_NOTNULL) {
								isError = true;
							}
						}
					}
				} else {									// a->func()->b;
					ASTPrimaryExpression idExp = (ASTPrimaryExpression)expNode;
					if (idExp.isMethod() && expinfo.isnull) {
						MethodNameDeclaration methodDecl = (MethodNameDeclaration)idExp.getMethodDecl();
						if (methodDecl != null && methodDecl.getMethod() != null) {
							Method method = methodDecl.getMethod();
							if (Config.USE_SUMMARY) {
								if (method.getReturnDomain() instanceof PointerDomain) {
									PointerDomain pd = (PointerDomain)method.getReturnDomain();
									if (pd.getValue() == PointerValue.NULL) {
										isError = true;
									}
								}
							}
						}
					}
				}
			} else if (expinfo.type == FIELD_NPDEXP_TYPE) {
				if (expinfo.isnull) {
					isError = true;
				}
			} else if (expinfo.type == PARAM_NPDEXP_TYPE && expinfo.isnull) {
				SimpleNode expListNode = (SimpleNode)expNode.jjtGetParent();
				SimpleNode expListNodeChild = expNode;
				while (!(expListNode instanceof ASTStatement)) {
					if (expListNode instanceof ASTArgumentExpressionList) {
						isError = true;
						break;
					}
					expListNodeChild = expListNode;
					expListNode = (SimpleNode)expListNode.jjtGetParent();
				}
				if (!(expListNode instanceof ASTStatement))
				{
					int index = -1;
					for (int i = 0; i < expListNode.jjtGetNumChildren(); i++) {
						if (expListNode.jjtGetChild(i) == expListNodeChild) {
							index = i;
							break;
						}
					}
					try {
						ASTPrimaryExpression idExp = (ASTPrimaryExpression)expListNode.jjtGetParent().jjtGetChild(0).jjtGetChild(0);
						if (idExp.getMethodDecl() != null) {
							MethodNameDeclaration methodDecl = (MethodNameDeclaration)idExp.getMethodDecl();
							if (methodDecl.getMethodSummary() != null) {
								MethodNPDPreCondition npdPreCond = (MethodNPDPreCondition)methodDecl.getMethodSummary().findMethodFeature(MethodNPDPreCondition.class);
								if (npdPreCond != null) {
									Set<Variable> npdVars = npdPreCond.getNPDVariables();
									for (Variable var : npdVars) {
										if (var.isParam() && index == var.getParamIndex()) {
											isError = true;
											break;
										}
									}
								}
							}
						}
					} catch (Exception e) {
						isError = false;
					}
				}
			}
			if (isError) {
				if (expinfo.assignLine != -1) {
					fsmin.setDesp("���ʽ\"" + fsmin.getResultString() + "\"�ڵ� "
							+ expinfo.assignLine + " �и�ֵ�����Ϊ��ָ�룬�Ժ�ᱻ������");
				} else {
					fsmin.setDesp("���ʽ\"" + fsmin.getResultString()
							+ "\"����Ϊ��ָ�룬�Ժ�ᱻ������");
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * ����Գ�Ա��������ֵ���ʽ��ֵ
	 * @param nodes
	 * @param fsmin
	 * @return
	 */
	public static boolean checkAssignFieldExp(List nodes, FSMMachineInstance fsmin) {
		ExpInfo expInfo = (ExpInfo)fsmin.getStateData();
		if (expInfo.type == ASSGIN_NPDEXP_TYPE) {
			return false;
		}
		Iterator listIter = nodes.iterator();
		while (listIter.hasNext()) {
			ASTPrimaryExpression expNode = (ASTPrimaryExpression) listIter.next();
			String expStr = getExpStr(expNode, 2);
			if (expStr == null) {
				continue;
			}
			
			if (fsmin.getResultString().equals(expStr)) {
				ASTAssignmentExpression assignNode = (ASTAssignmentExpression)expNode.getFirstParentOfType(ASTAssignmentExpression.class);
				if (assignNode.jjtGetNumChildren() != 3) {
					continue;
				}
				ASTAssignmentExpression rightNode = (ASTAssignmentExpression)assignNode.jjtGetChild(2);
				ExpressionValueVisitor expvst = new ExpressionValueVisitor();
				ExpressionVistorData visitdata = new ExpressionVistorData();
				visitdata.currentvex = rightNode.getCurrentVexNode();
				expvst.visit(rightNode, visitdata);
				Expression value1 = visitdata.value;
				Domain mydomain=null;
				if(value1!=null)
				   mydomain = value1.getDomain(visitdata.currentvex.getSymDomainset());
				if (!(mydomain instanceof PointerDomain)) {
					continue;
				}
				
				PointerDomain pDomain = (PointerDomain)mydomain;
				if (pDomain.getValue() == PointerValue.NULL || pDomain.getValue() == PointerValue.NULL_OR_NOTNULL) {
					expInfo.assignLine = assignNode.getBeginLine();
					expInfo.isnull = true;
					ExpInfo newInfo = new ExpInfo(expInfo.type, true);
					newInfo.assignLine = assignNode.getBeginLine();
					fsmin.setStateData(newInfo);
					return true;
				} else {
					ExpInfo newInfo = new ExpInfo(expInfo.type, false);
					newInfo.assignLine = expInfo.assignLine;
					fsmin.setStateData(newInfo);
				}
			}
		}
		return false;
	}
	
	public static boolean checkIfStmt(List nodes, FSMMachineInstance fsmin) {
		ExpInfo expInfo = (ExpInfo)fsmin.getStateData();
		Iterator listIter = nodes.iterator();
		while (listIter.hasNext()) {
			ASTPrimaryExpression expNode = (ASTPrimaryExpression) listIter.next();
			String expStr = getExpStr(expNode, 2);
			if (expStr == null) {
				continue;
			}
			if (fsmin.getResultString().equals(expStr)) {
				// Ĭ�Ͻṹ���Ա����if�ж�֮����Ĭ�ϲ�Ϊ��
				if (expInfo.type == FIELD_NPDEXP_TYPE || expInfo.type == PARAM_NPDEXP_TYPE) {
					return true;
				}
				// ��������ֵ����if�ж���Ĭ�ϲ�Ϊ��
				if (expNode.jjtGetNumChildren() == 1 && expNode.jjtGetChild(0) instanceof ASTPrimaryExpression) {
					ASTPrimaryExpression idExp = (ASTPrimaryExpression)expNode.jjtGetChild(0);
					if (idExp.isMethod()) {
						return true;
					}
				}
			}
		}
		return false;
	}
}

class ExpInfo {
	int type;
	int assignLine = -1;
	boolean isnull;
	public ExpInfo(int type, boolean isnull) {
		this.type = type;
		this.isnull = isnull;
	}
}