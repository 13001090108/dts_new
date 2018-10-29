package softtest.rules.gcc.fault;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Method;
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

/**
 * �������ָ�����͹���
 * @author cjie
 * 
 */
public class UFM_PREStateMachine extends BasicStateMachine {

	@Override
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodUFMPreConditionVisitor.getInstance());
		InterContext.addSideEffectVisitor(MethodMMFeatureVisitor.getInstance());
	    InterContext.addPostConditionVisitor(MethodUFMPostConditionVisitor.getInstance());
	}

	public static List<FSMMachineInstance> createUFMStateMachines(
			SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();

		Set<VariableNameDeclaration> reoccured = new HashSet<VariableNameDeclaration>();

		
		// ��������ʱ����״̬��ʵ��
		String xPath=".//PostfixExpression/PrimaryExpression[@Method='true']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode snode : evaluationResults) {
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(snode);
			if (methodDecl == null) {
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
					addFSM(snode, reoccured, list, varDecl, fsm);
				}
				continue;
			}
			Method method = methodDecl.getMethod();
			if (method == null) {
				continue;
			}
			if (method.getMtSummmary() == null) {
				continue;
			}
			MethodMMFeature mmFeture = (MethodMMFeature)method.getMtSummmary().findMethodFeature(MethodMMFeature.class);
			if (mmFeture != null) {
			    HashMap<Variable, MMFetureType> mmFetures = mmFeture.getMMFetures();
			    for (Variable variable : mmFetures.keySet()) {
			        MMFetureType type = mmFetures.get(variable);
			        if (variable.isParam() &&  type == MMFetureType.FREE) {
//			            if (snode instanceof ASTqualified_id) {
//			                varDecl = MethodMMFeatureVisitor.findArgDeclInQual(snode);
//			            } else 
			            if (snode instanceof ASTPrimaryExpression && snode.jjtGetParent().jjtGetNumChildren() >1 && snode.jjtGetParent().jjtGetChild(1) instanceof ASTArgumentExpressionList) {
			                varDecl = MethodMMFeatureVisitor.getArgDecl((ASTArgumentExpressionList)snode.jjtGetParent().jjtGetChild(1), variable.getParamIndex());
			            }
			            if (varDecl != null) {
			                FSMMachineInstance fsmins = addFSM(snode, reoccured, list, varDecl, fsm);
			                if (fsmins != null) {
			                    fsmins.setTraceinfo(mmFeture.getDesp(variable));
			                }
			            }
			        }
			    }
			}
//            //add post condition by suntao
            MethodUFMPostCondition post = (MethodUFMPostCondition) methodDecl.getMethodSummary().findMethodFeature(MethodUFMPostCondition.class);
            if(post == null) {
                continue;
            }
			for(Variable var : post.getUFMVariables()) {
                if (!reoccured.contains(var)) {
                    FSMMachineInstance fsmInstance = fsm.creatInstance();
                    fsmInstance.setRelatedASTNode(snode);
                    fsmInstance.setReleatedVexNode(snode.getCurrentVexNode());
                    fsmInstance.setStateData(var);
                    fsmInstance.setDesp("����" + var.getName() + "�ں���" + methodDecl.getImage() + "�����ú��ѱ��ͷ�");
                	list.add(fsmInstance);
                }
            }
		}
		return list;
	}
	private static VariableNameDeclaration findSingleParam(SimpleNode t, Variable npdVar) {
		int argIndex = -1;
		SimpleNode argNode = null;
		if (t instanceof ASTPrimaryExpression) {
			ASTPostfixExpression postfix = null;
             if (t.jjtGetParent() instanceof ASTPostfixExpression) {
				postfix = (ASTPostfixExpression) t.jjtGetParent();
			}
			if (postfix == null) {
				return null;
			}
			
			// ��ȡ�βνڵ㣬�����ֻ����postfix����ֻ��һ�κ�������
			List re = postfix.findDirectChildOfType(ASTArgumentExpressionList.class);
			if (re == null || re.size() <=0 ) {
				return null;
			}
			
			argNode = (ASTArgumentExpressionList)re.get(0);
//			
			argIndex = npdVar.getParamIndex();
			if (argIndex < 0 || argIndex >= argNode.jjtGetNumChildren()) {
				return null;
			}
			List<Node> varsNode = argNode.findChildrenOfType(ASTPrimaryExpression.class);
			if (varsNode.size() > argIndex) {
				return ((SimpleNode)varsNode.get(argIndex)).getVariableNameDeclaration();
			}
		} 
		return null;
	}
	private static FSMMachineInstance addFSM(SimpleNode snode, Set<VariableNameDeclaration> reoccur, List<FSMMachineInstance> list, NameDeclaration varDecl, FSMMachine fsm) {
		if (snode != null && varDecl instanceof VariableNameDeclaration 
				&& !reoccur.contains(varDecl)) {
			FSMMachineInstance fsmInstance = fsm.creatInstance();
			fsmInstance.setRelatedVariable((VariableNameDeclaration)varDecl);
			fsmInstance.setRelatedASTNode(snode);
			String desp="";
			if (varDecl.getNode() != null) {
				desp="�ڵ� "+varDecl.getNode().getBeginLine()+" �ж���ı��� \""+varDecl.getImage()+"\"�� "+snode.getBeginLine()+" �б��ͷ����ڴ� ";
			} else {
				desp="\""+varDecl.getImage()+"\"�� "+snode.getBeginLine()+" �б��ͷ����ڴ� ";
			}
			fsmInstance.setStateData(((VariableNameDeclaration)varDecl).getVariable());
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
	
	

    // ���ȫ�ֱ��� ���Ա�ں����е�ʹ��
    public static boolean checkMethodSummary(List nodes, FSMMachineInstance fsmin) {
        Iterator it = nodes.iterator();
        while (it.hasNext()) {
            SimpleNode node = (SimpleNode) it.next();
            MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(node);
            if(methodDecl == null) {
                continue;
            }
            MethodSummary mtSummary = methodDecl.getMethodSummary();
            if(mtSummary == null) {
                continue;
            }
            for(MethodFeature feature : mtSummary.getPreConditions()) {
                if(feature instanceof MethodUFMPreCondition) {
                    MethodUFMPreCondition pre = (MethodUFMPreCondition) feature;
                    for(Variable ufmVar : pre.getUFMVariables()) {
                    	if (ufmVar.getParamIndex() == -1) {
                    		if (ufmVar.equals(fsmin.getStateData())||ufmVar.equals(fsmin.getRelatedVariable())) {
                    			fsmin.setDesp(fsmin.getDesp() + "���ڵ�" + node.getBeginLine() + "���ٴα�ʹ��");
                    			return true;
                    		}
                    	}
                    	VariableNameDeclaration varDecl = findSingleParam(node, ufmVar);
                        if(varDecl != null && varDecl == fsmin.getRelatedVariable()) {
                        	fsmin.setDesp(fsmin.getDesp() + "���ڵ�" + node.getBeginLine() + "���ٴα�ʹ��");
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
	/**
	 * ��ȡ��ǰ�﷨���ڵ㺢���е�����ʵ�νڵ�
	 * @param node
	 * @return
	 */

}
