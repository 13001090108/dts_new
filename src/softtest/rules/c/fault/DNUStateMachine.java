package softtest.rules.c.fault;

import java.util.*;


import softtest.ast.c.*;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.fsm.c.*;

/** 
 * @authored by DongNa
 */

public class DNUStateMachine {
	
	public static List<FSMMachineInstance> createEDNUStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	    String xpath = "";
	    Hashtable<VariableNameDeclaration, FSMMachineInstance> vfTable = new Hashtable<VariableNameDeclaration, FSMMachineInstance>();
	    List<SimpleNode> evaluationResults = null;
	   // ��ѯ������������ʽ�����������ľֲ�����(��������ʱ��ʼ���ı���)��Ϊ�䴴��״̬��ʵ��
	    //���ںܶຯ����ֻ��һ���䷵�أ��ܶ��������ʹ���βΣ���ģʽ�޸��£������βε�����
	    xpath = ".//ParameterList//ParameterDeclaration//Declarator/DirectDeclarator|"
	    	+".//Declaration[./DeclarationSpecifiers[./TypeSpecifier]]/InitDeclaratorList/InitDeclarator//Declarator/DirectDeclarator ";
	    evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
	   Iterator<SimpleNode> itr = evaluationResults.iterator();
	   while (itr.hasNext()) {
			ASTDirectDeclarator qualifiedID = (ASTDirectDeclarator) itr.next();
			//chh  ���Ժ�����������ģʽ����Ա���
			if(qualifiedID.isFunctionName())
			{
				continue;
			}
			VariableNameDeclaration variable = qualifiedID.getVariableNameDeclaration();
			
			if(variable==null){
				continue;
			}
//			if(variable != null && checkAdressType(variable)){//���˵�&�������͵ı���
//				continue;
//			}
			if (variable != null 
					&& !vfTable.containsKey(variable)
						&& variable.getScope()!=null 
							&& !(variable.getScope() instanceof ClassScope)
								&& !(variable.getScope() instanceof SourceFileScope)) {
				FSMMachineInstance fsmInstance = fsm.creatInstance();
				fsmInstance.setRelatedVariable(variable);
				fsmInstance.setRelatedASTNode(qualifiedID);
				vfTable.put(variable, fsmInstance);
			}
	   }

		for (Enumeration<FSMMachineInstance> enm = vfTable.elements(); enm
				.hasMoreElements();) {
			list.add(enm.nextElement());
		}
		return list;
	}
	
	/**��⵱ǰ����ı���û�б�ʹ�ù�*/
	public static boolean checkNoOccurence(List<SimpleNode> nodes,FSMMachineInstance fsmInst){
		boolean found = false;
		Iterator<SimpleNode> simpleNodeItr = nodes.iterator();
		
		while(simpleNodeItr.hasNext()){
			Object obj = simpleNodeItr.next();
			if(obj instanceof ASTDirectDeclarator)continue;
			if(obj instanceof ASTPrimaryExpression){
				ASTPrimaryExpression idExpression = (ASTPrimaryExpression)obj;
				if(idExpression.isMethod())
					continue;
				if(idExpression.getVariableNameDeclaration()!= null && idExpression.getVariableNameDeclaration() == fsmInst.getRelatedVariable() ){
					//SimpleNode b=fsmInst.getRelatedASTNode();//fsmInst.getRelatedASTNode()�Ƕ���ı�����idExpression��ʹ��
					found = true;
					break;
				}
			}
			
		}
//		if(!found){
//			SimpleNode funcDef =(SimpleNode) fsmInst.getRelatedASTNode().getFirstParentOfType(ASTFunctionDefinition.class);
//			if(funcDef != null){
//				Node com_statement = funcDef.getFirstChildOfType(ASTCompoundStatement.class);
//				if(com_statement != null && com_statement.jjtGetNumChildren()==0)
//					found = true;
//				}
//			}
			
		if(!found)
			addFSMDescription(fsmInst);
		return !found;
    }
	
//	/**��⵱ǰ�����Ƿ�Ϊ&����*/	 
//	public static boolean checkAdressType(VariableNameDeclaration var){
//		//System.out.println(var.toString());
//		CType varType = var.getType();
//		if(varType instanceof CType_Address)
//			return true;
//		else return false;
//	}
	
	private static void addFSMDescription(FSMMachineInstance fsmInstance) {
		VariableNameDeclaration varDecl = fsmInstance.getRelatedVariable();
		//SimpleNode node = fsmInstance.getRelatedASTNode();
		fsmInstance.setDesp("��������δʹ��: ���� \""+varDecl.getImage()+"\"�����δ��ʹ�ã����ڲ������룬�����ϵͳ�������в���Ӱ��");
		
		}
	}

