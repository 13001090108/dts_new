package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;


/** 
 * @author zhouhb
 * Signed Variable Shift Operator
 * ��ֹ���з������ͽ�����λ����(char�����ݵ��з�����������˵���ݱ���������)
 * (���㴦����)
 */
public class SVSOStateMachine {
	
	//��λ����ֻ�����ӽڵ㣬ͬʱ�Ҳ�����Ϊconstant.
	//add by zhouhb
	//2010.3.1
	////��λ��Ϊ��ֵ����<<=����>>=
	private static String xpath = ".//ShiftExpression[count(child::UnaryExpression)>1 and ./UnaryExpression[2]/PostfixExpression/PrimaryExpression/Constant]/UnaryExpression[1]|"+
	                              ".//AssignmentExpression[/AssignmentOperator[@Operators='<<=']";
	//��λ����������Ǹ�&����(��������unsigned int & 0X3E -> unsigned int)���ʽ����ʱͳһ���䵱��int
	private static String xpath1 = "./UnaryExpression/PostfixExpression/PrimaryExpression/Expression/AssignmentExpression/ANDExpression[count(child::UnaryExpression)=2]";
    
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	public static List<FSMMachineInstance> createSVSOMachines(SimpleNode node, FSMMachine fsm){
		AbstractExpression result = null;
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		/*
		 * �汾2���������еı��ʽ���ͣ����Ƕ԰�λ������ı��ʽ�����������ʽ���ͼ���ʱͳһ��&��������int�ͣ�
		 * modified by zx 09.09.18
		 */
		for (SimpleNode snode : evaluationResults) {
			AbstractExpression exp = (AbstractExpression)snode;
			//��ֵΪ�з�������
			if(isSigned(exp)) {
				if(!(exp instanceof ASTAssignmentExpression)){
					if(exp.getParentsOfType(ASTANDExpression.class)==null){
						List<SimpleNode> noConstants = StateMachineUtils.getEvaluationResults(snode, ".//Constant");
						if(noConstants!=null&& noConstants.size()!=0) { //��λ��������߽����Ǹ�����
							continue;
						}
					}
				}
				
				if(exp instanceof ASTUnaryExpression){
					if(exp.getParentsOfType(ASTANDExpression.class)==null){
						ASTShiftExpression shift = (ASTShiftExpression) exp.getFirstParentOfType(ASTShiftExpression.class);
						if(shift!=null&&(shift.getOperators().equals("<<")||shift.getOperators().equals(">>"))){
							addFSMDescription(snode,fsm);
						}
					}else{
						if(exp.jjtGetParent().jjtGetParent() instanceof ASTANDExpression)
							addFSMDescription(snode,fsm);
						}
					//����������&��������
					List<SimpleNode> ands = StateMachineUtils.getEvaluationResults(snode, xpath1);
					if(ands==null || ands.size()==0) {
						//addFSMDescription(snode,fsm);
					}else if(ands.size()==1) { //��λ����������Ǹ�&����(��������)
						result =(AbstractExpression) ands.get(0);//��������Ǹ���λ������ı��ʽ����Ϊ���ʽ���ͼ��㲻׼������������
						if(result instanceof ASTANDExpression) {
							ASTUnaryExpression left = (ASTUnaryExpression) result.jjtGetChild(0);
							ASTUnaryExpression right = (ASTUnaryExpression) result.jjtGetChild(1);
							if(isSigned(left) && isConstant(right))
								addFSMDescription(snode,fsm);
							else if(isSigned(right) && isConstant(left))
								addFSMDescription(snode,fsm);
						}
					}	
				
				}//add by zhouhb
				//2010.3.1
				//������λ��ֵ
				else if(exp instanceof ASTAssignmentExpression&&exp.jjtGetNumChildren()==3){
					ASTAssignmentOperator opt=(ASTAssignmentOperator)exp.jjtGetChild(1);
					if(opt.getOperators().equals("<<=")||opt.getOperators().equals(">>="))
						addFSMDescription(snode,fsm);
				}
			}	
		}
		
		/*
		 * �汾1�� �����ǿ����������
		 * ������ �� ��λ������ı��ʽ
		 */	
//		for (SimpleNode snode : evaluationResults) {
//			List<SimpleNode> exps = StateMachineUtils.getEvaluationResults(snode, xpath1);
//			List<SimpleNode> ands = StateMachineUtils.getEvaluationResults(snode, xpath2);
//			
//			if(exps!=null && exps.size()==1) {
//				result =(AbstractExpression) exps.get(0); 
//			}else if(ands!=null && ands.size()==1) {
//				result =(AbstractExpression) ands.get(0);
//			}else 
//				continue;
//			/*
//			 * case1���������Ϊ������
//			 * case2����������Ǹ���λ������ı��ʽ����Ϊ���ʽ���ͼ��㲻׼������������
//			 * unsigned short s;
//			 * s&0XB3 Ӧ������unsigned
//			 * ���ʽ����ʱͳһ��sת����int����
//			 */
//			if((result instanceof ASTid_expression) && isSigned(result)) {
//				addFSMDescription(snode,fsm);
//			}else if(result instanceof ASTand_expression) {
//				ASTUnaryExpression left = (ASTUnaryExpression) result.jjtGetChild(0);
//				ASTUnaryExpression right = (ASTUnaryExpression) result.jjtGetChild(1);
//				if(isSigned(left) && isConstant(right))
//					addFSMDescription(snode,fsm);
//				else if(isSigned(right) && isConstant(left))
//					addFSMDescription(snode,fsm);
//			}
//		}
	    return list;
	}	
	
	private static boolean isSigned(AbstractExpression node) {
		CType type = node.getType();
		if(type == CType_BaseType.intType || type == CType_BaseType.shortType || type == CType_BaseType.longType || type == CType_BaseType.longLongType || type == CType_BaseType.charType) 
			return true;
		else 
			return false;
	}

	private static boolean isConstant(ASTUnaryExpression node) {
		String xpath = "/UnaryExpression/PostfixExpression/PrimaryExpression/Constant";
		List<SimpleNode> exps = StateMachineUtils.getEvaluationResults(node, xpath);
		if(exps!=null && exps.size()==1)
			return true;
		else
			return false;
	}

	private static void addFSMDescription( SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" Use Signed Number with Shift Operator. It will lead to unpredictable consequences using shift opeator with signed number.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��"+node.getBeginLine()+"��ʹ�����з�����������λ������\r\n���з������ͽ�����λ����ᵼ�²���Ԥ�ϵĺ����");
			}	
		
		list.add(fsminstance);
	}
}
