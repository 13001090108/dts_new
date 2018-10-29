package softtest.rules.gcc.rule;

import java.util.*;

import softtest.ast.c.*;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.pretreatment.Pretreatment;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;



/** 
 * @author maojinyu
 * Function Parameter According 2
 * ���������뺯�������Ĳ������������ͱ���һ�£����������ͣ� (���̵�����)
 */
public class FPA2StateMachine {	
	
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	private static List<SimpleNode> evaluationResults2 = new LinkedList<SimpleNode>();
	private static List<SimpleNode> evaluationResults3 = new LinkedList<SimpleNode>();
	private static List<SimpleNode> evaluationResults4 = new LinkedList<SimpleNode>();
	private static List<SimpleNode> evaluationResults5 = new LinkedList<SimpleNode>();
	private static List<SimpleNode> evaluationResults6 = new LinkedList<SimpleNode>();
	
	
	public static List<FSMMachineInstance> createFPA2Machines(SimpleNode node, FSMMachine fsm){
		
		//�����ǰ�����ڱ�����ļ���ͷ�ļ���,����
		String filename = node.getFileName();
		if(filename==null || filename.matches(InterContext.INCFILE_POSTFIX))
			return null;
		
		//�ҵ����еĺ�������ڵ�
		
		String xpath = ".//FunctionDefinition";	
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		
		//�ҵ����к��������ڵ�
		String xpath2 = ".//FunctionDeclaration";
		evaluationResults2 =StateMachineUtils.getEvaluationResults(node, xpath2);
		
		
		for(SimpleNode snode : evaluationResults) {	
			MethodNameDeclaration methodDef = StateMachineUtils.getMethodDefinition(snode);
			String curMethodName = methodDef.getImage();
			//�ȽϺ��������뺯�������ڵ�Ĳ��������������Ƿ�һ��
			Iterator<SimpleNode> itr2 = evaluationResults2.iterator();
			while(itr2.hasNext()) {
				SimpleNode methodDecl = itr2.next();
				String methodName = methodDecl.getImage();
				if( methodName.equals(curMethodName)) {
				
					//������������뺯�������ڵ㲻һ��
					if(!compareTwoFunc(snode, methodDecl, methodDef, fsm)) {
						itr2.remove();
						break;
					}
				}
			}
		}
	    return list;
	}	
	
	/*
	 * �ȽϺ��������뺯�������ڵ�Ĳ��������������Ƿ�һ��,���ز�һ���򷵻�false
	 * methodDecl ��������
	 * methodDef ��������
	 */
	private static boolean compareTwoFunc(SimpleNode snode, SimpleNode methodDecl, MethodNameDeclaration methodDef, FSMMachine fsm) {		
		
		boolean flag = true;
		
		
	
		
	
		String paramxpath = "//ParameterTypeList/ParameterList/ParameterDeclaration/DeclarationSpecifiers";	
		evaluationResults3 = StateMachineUtils.getEvaluationResults(methodDecl, paramxpath);
		evaluationResults4 = StateMachineUtils.getEvaluationResults(snode, paramxpath);
		int defNum = evaluationResults4.size();
		
		int declNum =evaluationResults3.size();
		
		
		String returnxpath = "//DeclarationSpecifiers";
		evaluationResults5 = StateMachineUtils.getEvaluationResults(methodDecl, returnxpath);
		evaluationResults6 = StateMachineUtils.getEvaluationResults(snode, returnxpath);
		

		//�������������ƥ��ֱ���˳�
		if(declNum!=defNum){
			
			addNumDesp(methodDecl, fsm, methodDef, declNum, defNum);
			flag = false;
		}
		
		else{
			//�õ��������弰���������Ĳ�������
			List<CType> declparams = new ArrayList<CType>();
			List<CType> defparams = new ArrayList<CType>();
						
		for(int i=0;i<evaluationResults3.size();i++){
			
			ASTDeclarationSpecifiers declparam = (ASTDeclarationSpecifiers)evaluationResults3.get(i); 
			ASTDeclarationSpecifiers defparam = (ASTDeclarationSpecifiers)evaluationResults4.get(i);
			declparams.add(declparam.getType());
			defparams.add(defparam.getType());
			
		}
		
		//�ȽϺ�������ͺ��������Ĳ�������
		if(declparams!=null)
			if(!(declparams.equals(defparams))){
				
				addTypeDesp(methodDecl, fsm, declparams, defparams);
				flag = false;
		}
		
		//�õ���������ͺ��������ķ�������
		if(evaluationResults5.size()>0&&evaluationResults6.size()>0){
		ASTDeclarationSpecifiers declreturn = (ASTDeclarationSpecifiers)evaluationResults5.get(0); 
		ASTDeclarationSpecifiers defreturn = (ASTDeclarationSpecifiers)evaluationResults6.get(0);
		
		CType declreturntype=declreturn.getType();
		CType defreturntype=defreturn.getType();
		
		//�ȽϺ�������ͺ��������ķ�������
		if(!(declreturntype.equals(defreturntype))){
			
			addReturnDesp(methodDecl, fsm, declreturntype, defreturntype);
			flag = false;
		
		}}
		
		
		
			}

	
		return flag;
	}


	/*
	 * ���������뺯������Ĳ���������ƥ��
	 */
	private static void addNumDesp(SimpleNode node, FSMMachine fsm, MethodNameDeclaration methodDef,int declNum, int defNum) {	
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		String defDesp = methodDef.getImage();
	
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp(node.getImage() + " Function parameter number must be according.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("���� "+defDesp+"�ĺ��������뺯������Ĳ���������ƥ�䡣" );
		}		
		list.add(fsminstance);
	}
	
	/*
	 * ���������뺯������Ĳ������Ͳ�ƥ��
	 */
	private static void addTypeDesp(SimpleNode node, FSMMachine fsm, List<CType> declTypes, List<CType> defTypes) {	
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp(node.getImage() + " Function parameter Type must be according.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("����"+node.getImage()+"�ĺ��������뺯������Ĳ������Ͳ�ƥ�䡣����������������Ϊ "+ declTypes.toString()+",�����������������Ϊ "+defTypes.toString());
		}	
		list.add(fsminstance);
	}
	
	/*
	 * ���������뺯������ķ���ֵ���Ͳ�ƥ��
	 */
	private static void addReturnDesp(SimpleNode node, FSMMachine fsm, CType declType, CType defType) {	
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp(node.getImage() + " Function parameter Type must be according.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("����"+node.getImage()+"�ĺ��������뺯������ķ���ֵ���Ͳ�ƥ�䡣����������������Ϊ "+ declType.toString()+",�����������������Ϊ "+defType.toString());
		}	
		list.add(fsminstance);
	}

}
