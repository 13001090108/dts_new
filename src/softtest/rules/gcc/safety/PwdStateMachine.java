package softtest.rules.gcc.safety;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.MethodNameDeclaration;

/**
 * Create state machine to check some password functions.
 * @author liuyan
 *
 */
public class PwdStateMachine {
	private final static String HARD_CODE = "�÷�����4��ʵ�δ�������,ʹ����Ӳ�������룺Ӳ����ή��ϵͳ��ȫ�ԣ��������Բ��ȡ�";
	private final static String PLAINTEXT_CODE = "�÷�����4��ʵ�δ�������,ʹ�����������룺ʹ�������ļ���ע���洢������߳����ַ�����Ϊ����ᵼ��ϵͳ��ȫ�Խ��͡�";
	private final static String EMPTYP_CODE = "�÷�����4��ʵ�δ�������,ʹ���˿�����:ʹ�ÿ������ǲ���ȫ�ġ�";
	private final static String FILE_RECORD = "����洢�ڱ����У�����ϵͳ��ȫ�Խ��ͣ����ǲ���ȫ�ġ�";
	private final static String LINUX_VERB_RECORD = "ʹ�����뺯��getgrent(),����洢���ļ�������У����ܽ����ļ�������й¶������ϵͳ�İ�ȫ�Խ��ͣ����ǲ���ȫ�ġ�";
	
	public static List<FSMMachineInstance> createPwdStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		String xPath=".//UnaryExpression/PostfixExpression/PrimaryExpression[@Method='true'] " +
				"|.//Expression/AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression[@Method='true']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		String isNULL = null;
		
		for (SimpleNode funcNode : evaluationResults) {
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(funcNode);	
			if(methodDecl == null ){
				continue;
			}
			//Linux C�����뺯�����
			if(methodDecl.getImage().equals("getgrent")){
				SimpleNode getParent = (SimpleNode) funcNode.jjtGetParent().jjtGetParent().jjtGetParent().jjtGetParent();
				if(getParent != null && getParent instanceof ASTAssignmentExpression){
					SimpleNode oper = (SimpleNode) getParent.jjtGetChild(1);
					if(oper.getOperators().equals("=")){//ʹ��getgrent�����ظ�һ����������״̬��
						FSMMachineInstance fsmInstance = fsm.creatInstance();
						fsmInstance.setRelatedASTNode(funcNode);
						fsmInstance.setResultString(funcNode.getImage());
						fsmInstance.setDesp(LINUX_VERB_RECORD);
						list.add(fsmInstance);
					}
				}
			}
			
			//WIN�����뺯�����
			//����洢���ļ���
			if(!methodDecl.getImage().equals("getgrent")){
				if(methodDecl.getImage().equals("mysql_real_connect")){
					SimpleNode getParents = (SimpleNode) funcNode.jjtGetParent().jjtGetParent().jjtGetParent().jjtGetParent();
					if(getParents != null && getParents instanceof ASTAssignmentExpression){
						SimpleNode opr = (SimpleNode) getParents.jjtGetChild(1);
						if(opr.getOperators().equals("=")){//��������洢���ļ��е�״̬��
							FSMMachineInstance fsmInstance = fsm.creatInstance();
							fsmInstance.setRelatedASTNode(funcNode);
							fsmInstance.setResultString(funcNode.getImage());
							fsmInstance.setDesp(FILE_RECORD);
							list.add(fsmInstance);
						}
					}
				}

			}
			
			String argXpath ="../ArgumentExpressionList/AssignmentExpression[4]/UnaryExpression/PostfixExpression/PrimaryExpression";
			List<SimpleNode> argNodeList = StateMachineUtils.getEvaluationResults(funcNode, argXpath);
			if (argNodeList == null || argNodeList.size() == 0) {
				continue;
			}
			//�����жϳ�������������֤����
			if(methodDecl.getImage().equals("mysql_real_connect") ){
				if(argNodeList != null || argNodeList.size() != 0){
					SimpleNode argNode = (SimpleNode)argNodeList.get(0);
					if(argNode instanceof ASTPrimaryExpression){
						if(argNode.jjtGetNumChildren() == 0){//��������
							FSMMachineInstance fsmInstance = fsm.creatInstance();
							fsmInstance.setRelatedASTNode(funcNode);
							fsmInstance.setResultString(funcNode.getImage());
							fsmInstance.setDesp(PLAINTEXT_CODE);
							list.add(fsmInstance);
						}else if(argNode.jjtGetNumChildren() == 1){	//Ӳ������߿�����
							if( argNode.jjtGetChild(0) instanceof ASTConstant){//Ӳ����
								FSMMachineInstance fsmInstance = fsm.creatInstance();
								fsmInstance.setRelatedASTNode(funcNode);
								fsmInstance.setResultString(funcNode.getImage());
								fsmInstance.setDesp(HARD_CODE);
								list.add(fsmInstance);
							}
							//�ж�����ΪNULL
							else if( argNode.jjtGetNumChildren() == 1 && argNode.jjtGetChild(0) instanceof ASTExpression){
								SimpleNode temp = (SimpleNode) argNode.jjtGetChild(0);
								for( int k = 0; k < 30; k++ ){
									isNULL = temp.getImage();
									if( isNULL.equals("void") ){
										break;
									}else{
										temp = (SimpleNode) temp.jjtGetChild(0);
									}
								}
								if(isNULL.equals("void")){
									FSMMachineInstance fsmInstance = fsm.creatInstance();
									fsmInstance.setRelatedASTNode(funcNode);
									fsmInstance.setResultString(funcNode.getImage());
									fsmInstance.setDesp(EMPTYP_CODE);
									list.add(fsmInstance);
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

