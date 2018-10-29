package softtest.rules.gcc.question;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Type.CType;

/*
 * 
 * ���ߣ�Wangjing
 * ģʽ��BSTR���ɿ��ַ�����ʾ�����ĵ�һ���ַ������˸����ַ����ĳ��ȡ�ʹ�õ�ַ������������BSTR��ֵ�ᵼ���ڴ�ʹ�õĴ���
 * ��������Ҫ��run arguments�е�vm�м���-Xmx128m��������java heap space����
 * */
 
public class BSTR_OPS_ARITHMStateMachine {
	
	public static List<FSMMachineInstance> createBSTR_OPS_ARITHMMachines(SimpleNode node, FSMMachine fsm) {
		
		String xpath = ".//PostfixExpression[@Operators='--']|.//PostfixExpression[@Operators='++']";
		
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();	
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();	
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()){
			
			ASTPostfixExpression astpe = (ASTPostfixExpression)itr.next();	
			
			int childNum = astpe.jjtGetNumChildren();
			
			if (childNum == 1) {
				
				ASTPrimaryExpression astpe1 = (ASTPrimaryExpression)astpe.jjtGetChild(0);
				
				CType type = astpe1.getType();
				
				String typeName = type.toString();
				
				if (typeName.contains("BSTR"))
					addFSM(list, astpe, fsm);
			}
				
		}
	    return list;
	}	
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("Address arithmetic to seek a BSTR value will lead to memory error");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("��"+node.getBeginLine()+"�У�ʹ�õ�ַ������������BSTR��ֵ�ᵼ���ڴ�ʹ�õĴ���");
		}	
		
		list.add(fsminstance);
	}

}
