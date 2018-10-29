package softtest.rules.gcc.question;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.AbstractExpression;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Type.CType;

/*
 * 
 * ���ߣ�����
 * ģʽ��BSTR�����Ƚ�(Question:BSTR)
 * ��������Ҫ��run arguments�е�vm�м���-Xmx128m��������java heap space����
 * */
public class BSTR_OPS_COMPStateMachine {
	public static List<FSMMachineInstance> createBSTR_OPS_COMPMachines(SimpleNode node, FSMMachine fsm) {
		

		String xpath = ".//RelationalExpression";
		
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();	
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();	
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()){
			
			AbstractExpression RelationalExpression = (AbstractExpression)itr.next();	
			
			CType  type1 =  ((AbstractExpression)RelationalExpression.jjtGetChild(0)).getType();
			CType  type2 =  ((AbstractExpression)RelationalExpression.jjtGetChild(1)).getType();
			
			if(type1 == null || type2 == null  )
				continue;
			
			if("BSTR".equals(type1.getName())|| "BSTR".equals(type2.getName()))
				addFSM(list, RelationalExpression, fsm);
		}
	    return list;
	}	
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("BSTR value of type can not be with any other type of value less than, greater than, less than or equal to , greater than or equal to operator.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("��"+node.getBeginLine()+"�У�BSTR���͵�ֵ�����������κ����͵���ֵ����С�ڡ����ڡ�С�ڵ��ڡ����ڵ������㡣");
		}	
		
		list.add(fsminstance);
	}

}
