package softtest.rules.gcc.question;


import java.util.*;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTAssignmentOperator;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.ASTUnaryOperator;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_Array;
import softtest.symboltable.c.Type.CType_Pointer;



/** 
 * @author chh
 * IIEO ѭ�����������صĵ�Ч����
 */
public class IIEOStateMachine {
	
	public static List<FSMMachineInstance> createIIEOStateMachines(SimpleNode node, FSMMachine fsm) {
		String iteration_xpath = ".//Statement/IterationStatement";
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, iteration_xpath);
		if(evaluationResults == null)
			return null;
		for(SimpleNode iterator : evaluationResults) {
			
			//ѭ������г����Զ��庯��
			doFunctionSpecial(iterator, fsm, list);
		}
	    return list;
	}	
	
	/*
	 * node : ASTiteration_statment
	 */
	private static void doFunctionSpecial(SimpleNode iterator, FSMMachine fsm, List<FSMMachineInstance> list) {
		String xpath = "./Expression/AssignmentExpression/RelationalExpression//UnaryExpression/PostfixExpression/PrimaryExpression[@Method='true']";
		List<SimpleNode> results = StateMachineUtils.getEvaluationResults(iterator, xpath);	
		//step1�� ȷ��ѭ������е������ж�������к�������
		if(results == null || results.size() != 1)
			return ;
		ASTPrimaryExpression snode = (ASTPrimaryExpression)results.get(0); 
		if(snode.jjtGetParent().jjtGetNumChildren()==1)
			return;
		SimpleNode args = (SimpleNode) snode.jjtGetParent().jjtGetChild(1);
		if(args == null)
			return ;
		List<SimpleNode> argList = StateMachineUtils.getEvaluationResults(args, "./AssignmentExpression");
		//step2: �������������Ϊ1�ĺ���
		if(argList == null || argList.size() != 1 )
			return ;
		//��ȥfunc(*a),func(&a)�����Ͳ���
		String argXpath = "./AssignmentExpression/UnaryExpression/PostfixExpression/PrimaryExpression";
		argList = StateMachineUtils.getEvaluationResults(args, argXpath);	
		if(argList == null || argList.size() != 1)
			return ;
		SimpleNode node = argList.get(0); 
		//step3: �õ���ǰ�����Ĳα������ͣ�֧��char[] char *���֣�
		VariableNameDeclaration variable = node.getVariableNameDeclaration();
		if(variable == null || variable.getType() == null)
			return ;	
		CType type = variable.getType();
		if(!(( type instanceof CType_Pointer ||  type instanceof CType_Array ) && CType.getOrignType(type).getName().equals("char")))
			return ;
		//step4: �жϵ�ǰѭ��node��variable�ǲ��Ǳ��ı�
		if(checkNoChange(variable, iterator)) {
		
			addFSMDesp(node, variable, fsm, list);
		}
	}



	/**
	 * �ж�ѭ������г��ֵ����б���������ʹ���Ĳ���Variable��ͬһ������������isChange()�������ж�
	 * @param ��������
	 * @param iteration�ڵ�
	 * @return �����ǰѭ���ڵ�node��variableû�иı䣬����true�����򷵻�false
	 * */
	private static boolean checkNoChange(VariableNameDeclaration variable, SimpleNode ancestor) {
		String xpath = "./Statement//PrimaryExpression ";
		List<SimpleNode> results = null;
		results = StateMachineUtils.getEvaluationResults(ancestor, xpath);
		Iterator itr = results.iterator();
		while(itr.hasNext()) {
			SimpleNode snode = (SimpleNode) itr.next();
			if(snode.getVariableNameDeclaration()!= null && snode.getVariableNameDeclaration() == variable) {	
				if(isChange(snode,ancestor))
					return false;	
			}
		}
		return true;
	}

	/**
	 * str=��acf��;
	 * func(&str);
	 * i = func(&str);
	 * &str = cc;
	 * func(str);
	 * ����������������Ǹı���str
	 * *str='a';
	 * str[2]='a';
	 * �����������������ı���str
	 * */
	private static boolean isChange(SimpleNode node, SimpleNode ancestor) {
		
		SimpleNode parent = (SimpleNode) node.jjtGetParent();
		
		//1.�ж�func(&str)����
		SimpleNode arg = (SimpleNode) node.getFirstParentOfType(ASTArgumentExpressionList.class);
		if(arg != null && arg.jjtGetChild(0).jjtGetChild(0).jjtGetNumChildren()==2) {	
			SimpleNode ptr = (SimpleNode) arg.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0);
			if(ptr.getOperators().equals("&")) {	
				//System.out.println("------------------����func(&str)");
				return true;
			}
			if(ptr.getOperators().equals("*")) {	
				//System.out.println("------------------����func(*str)");
				return false;
			}
		}
		
		
		//2.��ȥstr[0]����
		
		if(parent instanceof ASTPostfixExpression && parent.jjtGetNumChildren() == 2) {	
			if(parent.jjtGetChild(1) instanceof ASTExpression) {	
				//System.out.println("------------------��ȥ����str[2]");
				return false;
			}
		}
		else {
			parent = (SimpleNode) node.getFirstParentOfType(ASTUnaryExpression.class);
		}
		
		if(arg != null && arg.jjtGetChild(0).jjtGetChild(0).jjtGetNumChildren()==1) {	
			return true;
		}
		
		//3.�ж�i=func(&str)���� ����&str=cc����
		
		if(parent!= null && parent.jjtGetParent() instanceof ASTUnaryExpression && parent.jjtGetParent().jjtGetNumChildren()==2) {
			SimpleNode child = (SimpleNode) parent.jjtGetParent().jjtGetChild(0);
			if(child instanceof ASTUnaryOperator && child.getImage().equals("&")) {
				//System.out.println("------------------����&str");
				return true;
			}
			if(child instanceof ASTUnaryOperator && child.getImage().equals("*")) {
				//System.out.println("------------------����*str");
				return false;
			}
				
		}
		
		//ͨ��123���˺󣬾�ʣ��str���͡�
		//4.�ж�str�Ƿ�λ�ڸ�ֵ���ʽ�����str="avc"
		// ��str[0]='a'����
		
		if(parent.jjtGetParent() instanceof ASTAssignmentExpression && parent.jjtGetParent().jjtGetNumChildren()==3) {
			parent = (SimpleNode) parent.jjtGetParent();
			
			if(parent.jjtGetChild(1)!=null && ((ASTAssignmentOperator)parent.jjtGetChild(1)).getOperators().equals("=")) {
				//System.out.println("------------------����str=");
				return true;
			}
		}	
				
		return false;
	}
	
	private static void addFSMDesp(SimpleNode node, VariableNameDeclaration variable, FSMMachine fsm, List<FSMMachineInstance> list) {	
		FSMMachineInstance fsminstance = fsm.creatInstance();		
		fsminstance.setRelatedASTNode(node);
		fsminstance.setRelatedVariable(variable);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
			fsminstance.setDesp("Inefficient operation exists in loop statement.");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsminstance.setDesp("��"+node.getBeginLine()+"�У�ѭ�����������صĵ�Ч������\r\n���ѭ����������һ���������ã��ú����ķ���ֵ�ǲ�����ѭ�������иı�ģ�һ��Ҫ����������ѭ���⡣");	
		
		list.add(fsminstance);
	}
}

