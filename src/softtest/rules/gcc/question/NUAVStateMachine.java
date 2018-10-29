package softtest.rules.gcc.question;

import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;
import softtest.fsm.c.*;

/** 
 * @author liuli
 * 
 */

public class NUAVStateMachine {

	public static List<FSMMachineInstance> createNUAVStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		
		//��ѯ���������г����ڸ�ֵ���ʽ(����=�������ǡ�+=��*=�ȡ�)��ߵı������ʽ
		String xpath = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='='] ]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		/**Ϊ��ǰ��ֵ��ı�������״̬��ʵ��*/
		Iterator<SimpleNode> itr = evaluationResults.iterator();
		while(itr.hasNext()) {
			SimpleNode snode = itr.next();
			ASTPrimaryExpression primaryNode = (ASTPrimaryExpression)snode;
			VariableNameDeclaration variable = primaryNode.getVariableDecl();	
			if(variable == null || variable.getScope() == null) {
				continue;
			}
			//step1: ����Ϊ��������,���˵��ǻ�������������
			if(variable.getType() == null ||!(variable.getType() instanceof CType_BaseType)) {
				continue;
			}
			if(checkSpecialUse(snode)) {
				continue;
			}
			
			//step2:����Ϊ�ֲ�����
			if(!(variable.getScope() instanceof ClassScope) && !(variable.getScope() instanceof SourceFileScope)){	
				//�õ���ǰ��ֵ���������ѭ���ڵ�
				SimpleNode itrNode = getIteration(snode);				
				//step2.1 �����ǰ��ֵ�����������¶��壬������ʹ�ó��֣�����
				if(checkAssignedAgain(snode, variable)) {
					FSMMachineInstance fsmInstance = fsm.creatInstance();
					fsmInstance.setRelatedVariable(variable);
					fsmInstance.setRelatedASTNode(snode);
					addFSMDescription(fsmInstance);
					list.add(fsmInstance);
					continue;
				}
				//step2.2 �����ǰ��ֵ����Ǵ���ѭ����,�����ѭ���г����˸ñ�����ʹ�ã��ӵ�ǰ��ֵ������ʹ��
				else if(itrNode != null) {
					int beginLine = itrNode.getBeginLine();
					int endLine = itrNode.getEndLine();
					if(!checkHasUsed(variable, beginLine, endLine)) {
						FSMMachineInstance fsmInstance = fsm.creatInstance();
						fsmInstance.setRelatedVariable(variable);
						fsmInstance.setRelatedASTNode(snode);
						addFSMDescription(fsmInstance);
						list.add(fsmInstance);
					}
				}
			}
		}
		
		return list;
	}


	/**
	 * ����ѭ����䣨for��while��do-while���е��Ƿ���ڶԱ�����ʹ�ó���
	 * */
	private static boolean checkHasUsed(VariableNameDeclaration variable, int beginLine, int endLine) {
		Scope scope = variable.getScope();
		Map<VariableNameDeclaration, ArrayList<NameOccurrence>> varOccs = scope.getVariableDeclarations();
    	List<NameOccurrence> occs = varOccs.get(variable);
    	for(NameOccurrence occ : occs) {
    		//�˴γ��ַǶ������
    		if(occ.checkOccurrenceType() != NameOccurrence.OccurrenceType.DEF) {
    			SimpleNode snode = occ.getLocation();
        		int curLine = snode.getBeginLine();
        		if(curLine >= beginLine && curLine <= endLine) {
        			return true;
        		}
    		}
    	}
		return false;
	}

	/**�������д�����ѭ����䣨for��while��do-while���еĸ�ֵ��䣬��Ϊ������ܶ����*/
	private static SimpleNode getIteration(SimpleNode node) {
		SimpleNode result = null;
		SimpleNode ancestor = (SimpleNode) node.jjtGetParent();
		while(!(ancestor instanceof ASTTranslationUnit)) {
			if(ancestor instanceof ASTIterationStatement) {
				result = ancestor;
			}
			ancestor = (SimpleNode) ancestor.jjtGetParent();
		}
		return result;
	}
	
	/**
	 * �������������ȡ���� 1. i = j = k = 0; if(!(i=get())){} 2. if((i = get())!=0){}
	 * if((i = get())==0){} if((i = get())>0){}
	 */
	private static boolean checkSpecialUse(SimpleNode node) {
		ASTAssignmentExpression assignment = (ASTAssignmentExpression) node.getFirstParentOfType(ASTAssignmentExpression.class);
		// i = j = k = 0; if(!(i=get())){}
		if (assignment.jjtGetParent() instanceof ASTAssignmentExpression) {
			return true;
		}
		// if((i = get())==0){} if((i = get())!=0){}
		SimpleNode equal = (SimpleNode) assignment.getFirstParentOfType(ASTEqualityExpression.class);
		if (equal != null) {
			return true;
		}
		// if((i = get())>0){}
		SimpleNode relation = (SimpleNode) assignment.getFirstParentOfType(ASTRelationalExpression.class);
		if (relation != null) {
			return true;
		}
		else
			return false;
	}

	/**��⵱ǰ��ֵ����û��ʹ�õ��ٴθ�ֵ������true*/
	private static boolean checkAssignedAgain(SimpleNode node, VariableNameDeclaration variable) {
		Scope scope = variable.getScope();
		Map<VariableNameDeclaration, ArrayList<NameOccurrence>> varOccs = scope.getVariableDeclarations();
    	List<NameOccurrence> occs = varOccs.get(variable);
		List<NameOccurrence> list1 = new LinkedList<NameOccurrence>();//����ȡ����
		List<NameOccurrence> list2 = new LinkedList<NameOccurrence>();//����ʹ����
		if(occs==null)
			return false;
		for(NameOccurrence occ : occs) {
			if(occ.getLocation() == node) {
				list1 = occ.getDef_undef();
				list2 = occ.getDef_use();
				if(list2==null||list2.size()==0)  
					{
					break;
					}
				
				else 
					{
					if(list2.size() >0)
						{
							if(list2.size()==list1.size()&&list1.size()>0&&list2.size()>0)
							{
								//chh  ����i=i+*;���Ƶ���ʹ�ú�ֵ����䣬��������ٶ�i��ֵ��Ϊ���ำֵ��
								if(list2.get(0).getLocation().getBeginLine()==occ.getLocation().getBeginLine()) 
									{
									
									if(list2.get(0).getLocation().getBeginColumn()-occ.getLocation().getEndColumn()<=2)
										return true;
									}
								if(list2.get(0).getLocation().getBeginLine()>list1.get(0).getLocation().getBeginLine()) 
									return true;
								//end
							}
							//chh �����ڱ��ζ������ �ɵ���� �´�ʹ�ó���֮ǰ�� ������� ��Ϊ�Ե�ǰ�������ظ���ֵ
							if(list1.size()>0&&list2.get(0).getLocation().getBeginLine()>list1.get(0).getLocation().getBeginLine()) 
								return true;
						return false;
						}
					else {
						return true;
						}
					}
			}
		}
		if(list1!=null&&list1.size()>0 &&(list2==null|| list2.size()==0))
			return true;
		else
			return false;
	}
	
	/**
	 * add FSMInstance description
	 */
	private static void addFSMDescription(FSMMachineInstance fsmInstance) {
		VariableNameDeclaration varDecl = fsmInstance.getRelatedVariable();
		if (varDecl.getNode() != null) {
			fsmInstance.setDesp("�ڵ� " + varDecl.getNode().getBeginLine() + " �ж���ı��� \"" + varDecl.getImage() + "\" �ڱ��б���ֵ����û��ʹ�ù����ٴθ�ֵ��");
		} else {
			fsmInstance.setDesp("���� \"" + varDecl.getImage() + "\"�ڱ��б���ֵ������û�б�ʹ�ù����ٴθ�ֵ��");
		}
	}

}
