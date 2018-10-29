package softtest.rules.gcc.question;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTEqualityExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Type.CType;

/*
 * 
 * ���ߣ�����
 * ģʽ��BSTR�����е�(Question:BSTR)
 * ��������Ҫ��run configures->arguments�е�vm�м���-Xmx128m��������java heap space����
 * */
public class BSTR_OPS_EQSStateMachine {
	
	public static List<FSMMachineInstance> createBSTR_OPS_EQSMachines(SimpleNode node, FSMMachine fsm) {
		
		String xpath = ".//EqualityExpression";
		
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();	
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();	
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()){
			
			ASTEqualityExpression expression = (ASTEqualityExpression)itr.next();	
			
			if (expression == null)
				continue;
			int childNum = expression.jjtGetNumChildren();
			if (childNum != 2)
				continue;
			CType  type1 = null;
			if (expression.jjtGetChild(0) instanceof ASTUnaryExpression)
				type1 =  ((ASTUnaryExpression)expression.jjtGetChild(0)).getType();
			else
				continue;
			if (type1 == null)
				continue;
			String name1 = ((ASTUnaryExpression)expression.jjtGetChild(0)).getImage();
			CType  type2 = null;
			
			if (expression.jjtGetChild(1) instanceof ASTUnaryExpression)
				type2 =  ((ASTUnaryExpression)expression.jjtGetChild(1)).getType();
			else
				continue;
			if (type2 == null)
				continue;
			String name2 = ((ASTUnaryExpression)expression.jjtGetChild(1)).getImage();
			
			if(type1 == null || type2 == null  )
				continue;
			
			if("BSTR".equals(type1.getName())&& !("BSTR".equals(type2.getName()) 
					|| "CComBSTR".equals(type2.getName()) 
					|| "CComBSTR".equals(type2.getName())
					|| "bstr_t".equals(type2.getName())
					|| "".equals(name2)))
				addFSM(list, expression, fsm);
			else if ("BSTR".equals(type2.getName())&& !("BSTR".equals(type1.getName()) 
					|| "CComBSTR".equals(type1.getName()) 
					|| "CComBSTR".equals(type1.getName())
					|| "bstr_t".equals(type1.getName())
					|| "".equals(name1)))
				addFSM(list, expression, fsm);
		}
	    return list;
	}	
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("Equal to the operand in the operator BSTR type ( == ,! =) , The other operand is NULL or a BSTR , CcomBSTR bstr_t type value.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE) {
			fsminstance.setDesp("��"+node.getBeginLine()+"�У�������������е�һ����������BSTR�ͣ�==����=��������һ��������Ӧ����NULL��BSTR��CcomBSTR��bstr_t��ֵ��");
		}	
		
		list.add(fsminstance);
	}

}
