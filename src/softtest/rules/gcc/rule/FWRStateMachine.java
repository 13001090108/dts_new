package softtest.rules.gcc.rule;


import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.pretreatment.Pretreatment;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.Type.*;


/**
 * @author maojinyu 
 * Function With Return Statement
 * ���������з������/��ֹvoid���͵Ĺ����е�return�����з���ֵ/�з���ֵ�ĺ�����return������з���ֵ/�����������ͱ���һ��
 * (���÷�����)
 */
public class FWRStateMachine {
	
	private enum Type {FIRST_TYPE, SECOND_TYPE, THIRD_TYPE, FOURTH_TYPE}
	//��ѯ���з������
	private static String xpath = ".//JumpStatement[@Image='return']";
	
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();	
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	private static CType returnType = null;
	private static String funcName = null;
	
	private static boolean isMultiplicative=false;
	
	public static List<FSMMachineInstance> createFWRMachines(SimpleNode node, FSMMachine fsm){

		if(!(node instanceof ASTFunctionDefinition)) {
			return null;
		}
		
		//�����ǰ�����ڱ�����ļ���ͷ�ļ���,����
		String filename = node.getFileName();
		if(filename==null || filename.matches(InterContext.INCFILE_POSTFIX))
			return null;
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		
		//��ѯ��������ֵ����
		MethodNameDeclaration methodDecl = StateMachineUtils.getMethodDefinition(node);
		if(methodDecl == null ||  methodDecl.isLib()==true)
			return null;
		
		CType_Function funType = (CType_Function)methodDecl.getType();
		returnType=funType.getReturntype();
		funcName = methodDecl.getFullName();
		
		
		
		//���static void f(){},��ô����ֵ����Ϊstatic:void,��Ҫ����һ��
		if(returnType instanceof CType_Qualified && returnType.getName().equals("static")) {
			returnType = returnType.getSimpleType();
		}
			
		
		//First_type ��������з���ֵ�����Ǻ���������return���
		if(returnType != CType_BaseType.voidType && evaluationResults.size() == 0) {
			addFSM(node, fsm, Type.FIRST_TYPE);
			return list;
		}
		
		//Second_type �����������ֵΪvoid�����Ǻ�������return����з���ֵ
		if(returnType == CType_BaseType.voidType && evaluationResults.size() > 0) {	
			for(SimpleNode snode : evaluationResults) {
				if(snode.jjtGetNumChildren() > 0) {
					addFSM(snode, fsm, Type.SECOND_TYPE);
					return list;
				}
			}
		}
		
		//Third_type ��������з���ֵ����ô��������return������Ҳ�з���ֵ
		if(returnType != CType_BaseType.voidType && evaluationResults.size() > 0) {
			for(SimpleNode snode : evaluationResults) {
				if(snode.jjtGetNumChildren() == 0) {
					addFSM(snode, fsm, Type.THIRD_TYPE);
					return list;
				}
			}
		}
		
		//Fourth_type ��������з���ֵ����ô��������return����ֵ����Ӧ�ͺ�������ķ���ֵ����һ��
		if(returnType != CType_BaseType.voidType && evaluationResults.size() > 0) {
			for(SimpleNode snode : evaluationResults) {
				if(!(snode.jjtGetChild(0) instanceof ASTExpression))
					continue;
				AbstractExpression retExp = (AbstractExpression) snode.jjtGetChild(0);
				CType type = retExp.getType();
				
			    if((retExp.findChildrenOfType(ASTPrimaryExpression.class)).size()>1)
			    	isMultiplicative=true;
				
			
				//First�����⴦��char* f(){ return NULL; }	  NULL����ΪΪint��
				if(returnType instanceof CType_Pointer && (retExp.getImage().equals("NULL") || retExp.getImage().equals("0"))) {
					continue;
				}
				
				//Second: ����return��ֵΪ������������������ֵΪ�޷����������unsigned int f(){ return 1; }
				String Xpath = "./AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression/Constant|./AssignmentExpression/UnaryExpression/UnaryExpression/PostfixExpression/PrimaryExpression/Constant";//˵��return��ı��ʽ�Ǹ�����
				List<SimpleNode> constants = StateMachineUtils.getEvaluationResults(retExp, Xpath);
				
				if(constants.size()>0) {	
					
					//1�� ����Ϊ������������ֵΪ�޷���������Ҫ����
					if(returnType instanceof CType_BaseType && returnType.getName() != null  && type != null && type.getName() != null && returnType.getName().startsWith("unsigned") && type.getName().equals("int")&&constants.size()>0) {
						String retValue = constants.get(0).getImage();
						int unary = retExp.jjtGetChild(0).jjtGetChild(0).jjtGetNumChildren();
						
						if(unary == 2) {
							addFSM2(snode, fsm, type, returnType);
							continue;
						}
						//2: ���������l/L����������ó���ΪLong��
						if(retValue != null && (retValue.endsWith("L") || retValue.endsWith("l"))) {					
							type = CType_BaseType.longType;
						}else {
							continue;
						}
					}
					
					//3�����ڳ���1.0��ʶ��Ϊdouble���� 
					//��������������ֵΪfloat������£� ����1.0����1��˵��������
					if(returnType instanceof CType_BaseType && (returnType.getName().equals("float") || returnType.getName().equals("double"))) {
							if(type != null && (type.getName().equals("double") || type.getName().equals("int"))) {
								continue;
							}
					}
					
					//4����������ֵΪlong�ͣ���return����int,Ҳ�㷵������һ��
					if(returnType instanceof CType_BaseType && returnType.getName().equals("long")) {
						if(type != null && type.getName().equals("int")) {
							continue;
						}
					}
				} 
				
				//String Xpath2 = "//AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression";//˵��return��ı��ʽ�Ǹ�����
				//List<SimpleNode> variables = StateMachineUtils.getEvaluationResults(retExp, Xpath);
				else{
					
				if(!isMatch(type, returnType)) { 
				
					addFSM2(snode, fsm, type, returnType);
				}}
			}
		}	
		
	    return list;
	}	

	

	private static boolean isMatch(CType atype, CType ptype) {
		
		if (atype == null || ptype == null) {			
			return false;
		}		
		if (atype == ptype) {
			return true;
		}
		// can not pass the const argument to the no-const parameter
		if (atype instanceof CType_Qualified && ptype instanceof CType_Qualified) {
			
			CType_Qualified atemp = (CType_Qualified)atype;
			CType_Qualified ptemp = (CType_Qualified)ptype;
			
			if (atemp.getName()==null && ptemp.getName()!=null) {
				return false;
			}
		}
		if (atype instanceof CType_Qualified) {
	
			atype = atype.getSimpleType();
		}
		
		if (ptype instanceof CType_Qualified ) {
		
			ptype = ptype.getSimpleType();
		}
		
		//����typedef
		if(atype instanceof CType_BaseType && ptype instanceof CType_Typedef){	
			if(isMultiplicative==true)
			return true;		
		}
		
		if (atype instanceof CType_BaseType && ptype instanceof CType_BaseType) {
		if(atype == ptype)
				return true;
		}
		
		// can convert the enum argument into int parameter
		if (atype instanceof CType_Enum && ptype instanceof CType_BaseType) {
			return true;
		}
		// array and pointer
		if (atype instanceof CType_AbstPointer && ptype instanceof CType_AbstPointer) {
			return isMatch(((CType_AbstPointer)atype).getOriginaltype(), ((CType_AbstPointer)ptype).getOriginaltype());
		} 
		
		if (atype == null || ptype == null) {
			return false;
		}

		// �����ǰ����δ֪������ж��Ƿ�������ͬ
		if ((atype instanceof CType_Unkown && !(ptype instanceof CType_Unkown))
			||(!(atype instanceof CType_Unkown) && ptype instanceof CType_Unkown)) {
			return false;
		}

		if (atype.getName().equals(ptype.getName())) {
			return true;
		}
		return false;
	}

	private static void addFSM(SimpleNode node, FSMMachine fsm, Type type) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if(type == Type.FIRST_TYPE) {
			
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
					fsminstance.setDesp(funcName + " Function with non-void return ,should has return statement. ");
				} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
					fsminstance.setDesp(funcName + " ���������з������.");
				}	
		}		
		else if(type == Type.SECOND_TYPE) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Avoid " + funcName + " function with void return type has return statement. ");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��ֹvoid���͵� " + funcName + " �����е�return�����з���ֵ");
			}
		}	
		else if(type == Type.THIRD_TYPE) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(funcName + " Function must has return statement with value. ");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("�з���ֵ�� " + funcName + " ������return������з���ֵ");
			}
		}
		
		list.add(fsminstance);
	}
	
	private static void addFSM2(SimpleNode node, FSMMachine fsm, CType type, CType returnType) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp(funcName +" Function must has the same return type. ");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			StringBuffer desp = new StringBuffer(funcName + " �����������ͱ���һ��.");
			if(returnType != null && type != null) {
				desp.append("\r��������ķ�������Ϊ" + returnType + ", return ���ص�����Ϊ " + type);
			}
			fsminstance.setDesp(desp.toString());
		}
		list.add(fsminstance);
	}
}
