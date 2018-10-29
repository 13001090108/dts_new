package softtest.rules.gcc.fault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTEqualityExpression;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MMFetureType;
import softtest.summary.c.MethodFeature;
import softtest.summary.c.MethodSummary;
import softtest.summary.gcc.fault.MethodMMFeature;
import softtest.summary.gcc.fault.MethodMMFeatureVisitor;
import softtest.summary.gcc.fault.MethodUFMPostCondition;
import softtest.summary.gcc.fault.MethodUFMPostConditionVisitor;
import softtest.summary.gcc.fault.MethodUFMPreCondition;
import softtest.summary.gcc.fault.MethodUFMPreConditionVisitor;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_Pointer;

/**
 * �������ָ�����͹���
 * @author cjie
 * 
 */
public class UFMStateMachine extends BasicStateMachine {

	@Override
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodUFMPreConditionVisitor.getInstance());
		InterContext.addPostConditionVisitor(MethodUFMPostConditionVisitor.getInstance());
		InterContext.addPostConditionVisitor(MethodMMFeatureVisitor.getInstance());
	}

	public static List<FSMMachineInstance> createUFMStateMachines(
			SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		Set<VariableNameDeclaration> reoccured = new HashSet<VariableNameDeclaration>();
		// ��������ʱ����״̬��ʵ��
		String xPath=".//PostfixExpression/PrimaryExpression[@Method='true']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode snode : evaluationResults) {
			//MethodNameDeclaration mnd=((ASTPrimaryExpression)snode).getMethodDecl();
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(snode);
			if (methodDecl == null || methodDecl.getImage() == null) {
				continue;
			}
			
			NameDeclaration varDecl = null;
			if (methodDecl.getImage().equals("free")) {
				/**modified by xwt 2011.7.1*/
				if(snode.jjtGetParent() instanceof ASTPostfixExpression){
					if(snode.jjtGetParent().jjtGetNumChildren() == 2){
						ASTArgumentExpressionList ae = (ASTArgumentExpressionList) snode.getNextSibling();
						ASTUnaryExpression ue = (ASTUnaryExpression) ae.getFirstChildOfType(ASTUnaryExpression.class);
						varDecl = ue.getVariableDecl();
					}
					else
						continue;
				}

				if (varDecl != null) {
					addFSM(snode, methodDecl, reoccured, list, varDecl, fsm);
				}
				continue;
			}
			if (methodDecl.getMethodSummary() == null) {
				continue;
			}
			MethodMMFeature mmFeture = (MethodMMFeature)methodDecl.getMethodSummary().findMethodFeature(MethodMMFeature.class);
			if (mmFeture != null) {
			    HashMap<Variable, MMFetureType> mmFetures = mmFeture.getMMFetures();
			    for (Variable variable : mmFetures.keySet()) {
			        MMFetureType type = mmFetures.get(variable);
			        if (variable.isParam() &&   type == MMFetureType.FREE) {
			            if (snode instanceof ASTPrimaryExpression && snode.jjtGetParent().jjtGetNumChildren() >1 && snode.jjtGetParent().jjtGetChild(1) instanceof ASTArgumentExpressionList) {
			                varDecl = MethodMMFeatureVisitor.getArgDecl((ASTArgumentExpressionList)snode.jjtGetParent().jjtGetChild(1), variable.getParamIndex());
			            }
			            if (varDecl != null) {
			                FSMMachineInstance fsmins = addFSM(snode, methodDecl,reoccured, list, varDecl, fsm);
			                if (fsmins != null) {
			                    fsmins.setTraceinfo(mmFeture.getDesp(variable));
			                }
			            }
			        }
			    }
			}
		}
		return list;
	}
	private static FSMMachineInstance addFSM(SimpleNode snode, MethodNameDeclaration methodDecl, Set<VariableNameDeclaration> reoccur, List<FSMMachineInstance> list, NameDeclaration varDecl, FSMMachine fsm) {
		if (snode != null && varDecl instanceof VariableNameDeclaration && !reoccur.contains(varDecl)) {
			FSMMachineInstance fsmInstance = fsm.creatInstance();
			fsmInstance.setRelatedVariable((VariableNameDeclaration)varDecl);
			fsmInstance.setRelatedASTNode(snode);
			String desp="";
			if (varDecl.getNode() != null) {
				desp="�ڵ� "+varDecl.getNode().getBeginLine()+" �ж���ı��� \""+varDecl.getImage()+"\"�� "+snode.getBeginLine()+" ��ͨ������"+methodDecl.getImage()+"���ͷ����ڴ� ";
			} else {
				desp="\""+varDecl.getImage()+"\"�� "+snode.getBeginLine()+" �б��ͷ����ڴ� ";
			}
			fsmInstance.setDesp(desp);
			list.add(fsmInstance);
			reoccur.add((VariableNameDeclaration)varDecl);
			return fsmInstance;
		}
		return null;
	}
	
	public static boolean checkFree(List nodes, FSMMachineInstance fsmin) {
		Iterator it = nodes.iterator();
		while (it.hasNext()) {
			SimpleNode idNode = (SimpleNode) it.next();
			if(fsmin.getRelatedASTNode() == idNode){
				return true;
			}
		}
		return false;
	}
	
	public static boolean checkAssign(List nodes, FSMMachineInstance fsmin) {
		Iterator it = nodes.iterator();
		while (it.hasNext()) {
			ASTPrimaryExpression idNode = (ASTPrimaryExpression) it.next();
			if (idNode.getVariableDecl() == fsmin.getRelatedVariable()) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean checkUse(List nodes, FSMMachineInstance fsmin) {
		Iterator it = nodes.iterator();
		while (it.hasNext()) {
			SimpleNode node = (SimpleNode) it.next();
			/**add by xwt 2011.7.10*/
			if(node instanceof ASTPrimaryExpression){
				if(node.jjtGetNumChildren()!=0)
				    node = (ASTUnaryExpression) node.getFirstChildOfType(ASTUnaryExpression.class);
			}
			/**modified by xwt 2011.7.1*/
			if(node instanceof ASTUnaryExpression) {
				ASTUnaryExpression idNode = (ASTUnaryExpression) node;
                if(fsmin.getStateData() instanceof Variable) {      // post condition
                    VariableNameDeclaration varDecl = idNode.getVariableNameDeclaration();
                    if(varDecl != null) {
                        Variable var = Variable.getVariable(varDecl);
                        if(var != null && var.equals((Variable)fsmin.getStateData())) {
                            fsmin.setDesp(fsmin.getDesp() + ",�ڵ�" + idNode.getBeginLine() + "�б��ٴ�ʹ��");
                            //fsmin.setTraceinfo(desp);
                            return true;
                        }
                    }
                } else {
                    if(idNode.getParentsOfType(ASTArgumentExpressionList.class).size() == 0 ) {	//idNode���ǲ������
                        if (idNode.getVariableDecl() == fsmin.getRelatedVariable() && fsmin.getRelatedASTNode() != idNode) {
                            //add by zhouhb
                        	//2011.3.10
                        	//�޸ĵ���p�����ͷź��ٴν���p=NULL����ʱ����
                        	ASTAssignmentExpression assign=(ASTAssignmentExpression)idNode.getFirstParentOfType(ASTAssignmentExpression.class);
                        	if(assign!=null&&(assign.getType()instanceof CType_Pointer)){
                        		//�жϵ�ǰ�����Ƿ�Ϊp=NULL����
                        		if(assign.containsChildOfType(ASTConstant.class)&&!assign.containsParentOfType(ASTEqualityExpression.class)&&((ASTConstant)assign.getFirstChildOfType(ASTConstant.class)).getImage().equals("0")){
                        			return false;
                        		}
                        	}
                        	//end by zhouhb
                        	fsmin.setDesp(fsmin.getDesp() + "���ڵ�" + idNode.getBeginLine() + "���ٴα�ʹ��");
                            return true;
                        }
                    } 
                    //��Ϊ����ʹ����UFM_PRE�м��
//                    else {		//������Ϊʵ�α�ʹ��
//                        String desp = checkUseInFunction(idNode);
//                        if(desp != null) {
//                            if (idNode.getVariableDecl() == fsmin.getRelatedVariable() && fsmin.getRelatedASTNode() != idNode) {
//                                 fsmin.setDesp(fsmin.getDesp() + "���ڵ�" + idNode.getBeginLine() + "���ٴα�ʹ��");
//                                 fsmin.setTraceinfo(desp);
//                                return true;
//                            }
//                        }
//                    }
                }
			} 
//			else if(node instanceof ASTqualified_id) {
//				ASTqualified_id idNode = (ASTqualified_id) node;
//				String desp = checkUseInFunction(idNode);
//				if(desp != null) {
//					if (idNode.getNameDeclaration() == fsmin.getRelatedVariable() && fsmin.getRelatedASTNode() != idNode) {
//						fsmin.setDesp(fsmin.getDesp() + "���ڵ�" + idNode.getBeginLine() + "���ٴα�ʹ��");
//						fsmin.setTraceinfo(desp);
//						return true;
//					}						
//				}
//			}
		}
		return false;
	}
	
	private static String checkUseInFunction(SimpleNode node) {
		if(node instanceof ASTUnaryExpression) {
			ASTArgumentExpressionList paramsNode = (ASTArgumentExpressionList) node.getParentsOfType(ASTArgumentExpressionList.class).get(0);
			int paramindex = -1;
			int n = paramsNode.jjtGetNumChildren();
			for(int i = 0; i < n; i++) {
				SimpleNode paramAss = (SimpleNode) paramsNode.jjtGetChild(i);
				if(paramAss.findChildrenOfType(ASTPrimaryExpression.class).size() != 0) {
					ASTUnaryExpression param = (ASTUnaryExpression) paramAss.findChildrenOfType(ASTUnaryExpression.class).get(0);
					if(param == node) {
						paramindex = i;
						break;
					}
				}
			}
			if(paramsNode.jjtGetParent() instanceof ASTPostfixExpression) {
				ASTPostfixExpression post = (ASTPostfixExpression) paramsNode.jjtGetParent();
				if(post.jjtGetNumChildren() == 2 && 
						post.jjtGetChild(0) instanceof ASTPrimaryExpression) {
					ASTPrimaryExpression funcNode = (ASTPrimaryExpression) post.jjtGetChild(0);
					if(funcNode.isMethod() && funcNode.getMethodDecl() != null) {
						MethodNameDeclaration funcDecl = (MethodNameDeclaration) funcNode.getMethodDecl();
						if(funcDecl.getMethodSummary() != null) {
							Set<MethodFeature> features = funcDecl.getMethodSummary().getPreConditions();
							for(MethodFeature feature : features) {
								if(feature instanceof MethodUFMPreCondition) {
									MethodUFMPreCondition ufmPre = (MethodUFMPreCondition) feature;
									Set<Variable> vars = ufmPre.getUFMVariables();
									for(Variable var : vars) {
										if(var.getParamIndex() == paramindex) {
											return ufmPre.getDespString(var);
										}
									}
								}
							}
						}
					}
				}
			}
		} 
//		else if(node instanceof ASTqualified_id) { 	//��qualified_id��ʽ���ֵı���ʹ��ֻ�����ǵ��κ����Ĳ���
//			ASTdeclaration decl = (ASTdeclaration) node.getParentsOfType(ASTdeclaration.class).get(0);
//			if(decl.jjtGetChild(0) instanceof ASTdeclaration_specifiers) {
//				ASTdeclaration_specifiers declsp = (ASTdeclaration_specifiers) decl.jjtGetChild(0);
//				ASTqualified_id funcNode = (ASTqualified_id) declsp.findChildrenOfType(ASTqualified_id.class).get(0);
//				if(funcNode.isMethod() && funcNode.getNameDeclaration() instanceof MethodNameDeclaration) {
//					MethodNameDeclaration funcDecl = (MethodNameDeclaration) funcNode.getNameDeclaration();
//					//free(p)��ָ��p����use
//					if(!funcDecl.getName().equals("free")) {
//						//��麯���Ƿ��жԸò�����ǰ��Լ��
//						if(funcDecl.getMethodSummary() != null) {
//							Set<MethodFeature> features = funcDecl.getMethodSummary().getPreConditions();
//							for(MethodFeature feature : features) {
//								if(feature instanceof MethodUFMPreCondition) {
//									MethodUFMPreCondition ufmPre = (MethodUFMPreCondition) feature;
//									Set<Variable> vars = ufmPre.getUFMVariables();
//									for(Variable var : vars) {
//										return ufmPre.getDespString(var);
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
		return null;
	}
}
