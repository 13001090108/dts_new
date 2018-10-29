package softtest.rules.keilc.fault;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import softtest.ast.c.*;
import softtest.fsm.c.*;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.CType_BaseType;



/**
 * @author zx
 * Assigned Not Used [Embed]
 * */
public class ANUStateMachine {
	
	/**
	 * Ϊ��������Щ��ʾ��ֵ�Ļ������ͱ�������״̬�������� d = 10��
	 * a[1]=1, a.b=1, a->b = 1������
	 * */
	public static List<FSMMachineInstance> createANUStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		//chh  ��������ʱ��ʼ���ı���Ҳ����״̬��
		String xpath = ".//AssignmentExpression[./AssignmentOperator[@Operators='= ' or @Operators='='] ]/UnaryExpression/PostfixExpression[count(*)=1]/PrimaryExpression|"
			+ ".//Declaration[./DeclarationSpecifiers[./TypeSpecifier]]/InitDeclaratorList/InitDeclarator[./Initializer]//Declarator/DirectDeclarator|"
			+ ".//Declaration[./DeclarationSpecifiers[./DeclarationSpecifiers[./TypeSpecifier]]]/InitDeclaratorList/InitDeclarator[./Initializer]//Declarator/DirectDeclarator ";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator<SimpleNode> itr = evaluationResults.iterator();
			while(itr.hasNext()) {
			SimpleNode snode = itr.next();
			ASTPrimaryExpression primaryNode;
			VariableNameDeclaration variable;
			ASTDirectDeclarator qualifiedID;
			// chh ��������ʱ��ʼ���ͱ�����ֵ�ֱ���
			if(snode instanceof ASTDirectDeclarator)
				{
				qualifiedID = (ASTDirectDeclarator) snode;
				variable = qualifiedID.getVariableNameDeclaration();
				}
			else {
				primaryNode = (ASTPrimaryExpression)snode;
				variable = primaryNode.getVariableDecl();
			}
			//end
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
				String str="static";
				String xpathstatic=".//Declaration[./DeclarationSpecifiers[./StorageClassSpecifier[@Image='"+str+"']]]/InitDeclaratorList/InitDeclarator//Declarator/DirectDeclarator[@Image='"+variable.getImage()+"']";
				List<SimpleNode> evaluationResultstatic = StateMachineUtils.getEvaluationResults(node, xpathstatic);
				//chh  static���͵ĵ���������Ϊstatic���͵ı�����ʹ���һ�θ�ֵ��δʹ�ÿ����´ε��ô˺���ʱ��ʹ��
				if(!(evaluationResultstatic==null||evaluationResultstatic.size()==0)) {
					if(checkAssignNoUse(snode, variable,1)) {
						FSMMachineInstance fsmInstance = fsm.creatInstance();
						fsmInstance.setRelatedVariable(variable);
						fsmInstance.setRelatedASTNode(snode);
						//System.out.println("chuangjian"+variable.getImage());
						addFSMDescription(fsmInstance);
						list.add(fsmInstance);
						}
				}//end
				else{
					if(checkAssignNoUse(snode, variable,0)) {
						FSMMachineInstance fsmInstance = fsm.creatInstance();
						fsmInstance.setRelatedVariable(variable);
						fsmInstance.setRelatedASTNode(snode);
						//System.out.println("chuangjian"+variable.getImage());
						addFSMDescription(fsmInstance);
						list.add(fsmInstance);
					}
				}
			}
		}
		return list;
	}
	
	/**��⵱ǰ��ֵ����û���ں����б�ʹ�ù�*/
	
	private static boolean checkAssignNoUse(SimpleNode node, VariableNameDeclaration variable,int flag) {
		
		Scope scope = variable.getScope();
		Map<VariableNameDeclaration, ArrayList<NameOccurrence>> varOccs = scope.getVariableDeclarations();
    	List<NameOccurrence> occs = varOccs.get(variable);
		List<NameOccurrence> list1 = new LinkedList<NameOccurrence>();//����ȡ����
		List<NameOccurrence> list2 = new LinkedList<NameOccurrence>();//����ʹ����
		if(occs==null)
			return false;
		//chh ����static���͵ı���ֻҪ��һ��ʹ�ó��־���Ϊ��ʹ�ù�
		if(flag==1)
		{
			if(occs.size()>1)return false;
			else return true;
		}
		//chh ���ڷ�static���͵ı���������Ƿ�ÿ�θ�ֵ����ʹ��
		for(NameOccurrence occ : occs) {

			if(occ.getLocation() == node) {
							list1 = occ.getDef_undef();
							list2 = occ.getDef_use();
							if((list1==null||list1.size()==0)&&(list2==null||list2.size()==0))
								return true;
							break;
			}
		}
		return false;
	
	}
	
	
	/**
	 * �������������ȡ���� 1. i = j = k = 0; if(!(i=get())){} 2. if((i = get())!=0){}
	 * if((i = get())==0){} if((i = get())>0){}
	 */
	private static boolean checkSpecialUse(SimpleNode node) {
		if(node instanceof ASTDirectDeclarator) return false;
		else
		{
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
	}
	
	
	/**
	 * add FSMInstance description
	 */
	private static void addFSMDescription(FSMMachineInstance fsmInstance) {
		VariableNameDeclaration varDecl = fsmInstance.getRelatedVariable();
		if (varDecl.getNode() != null) {
			fsmInstance.setDesp("�ڵ� " + varDecl.getNode().getBeginLine() + " �ж���ı��� \"" + varDecl.getImage() + "\" �ڱ��б���ֵ������û�б�ʹ�ù���\r\n�ø�ֵ�������ø�ֵ���������롣");
		} else {
			fsmInstance.setDesp("���� \"" + varDecl.getImage() + "\"�ڱ��б���ֵ������û�б�ʹ�ù���\r\n�ø�ֵ�������ø�ֵ���������롣");
		}
	}
	
}


