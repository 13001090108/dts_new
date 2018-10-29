package softtest.rules.gcc.question;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTConditionalExpression;
import softtest.ast.c.ASTEqualityExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.Type.CType;

/*
 * author:Wangjing
 * ģʽ��BSTR.FUNC.FREE���Ժ���SysFreeString���ò���ȷ
 * 
 * */
public class CCOMBSTR_OPS_TERNStateMachine {

	public static List<FSMMachineInstance> createCCOMBSTR_OPS_TERNStateMachines(SimpleNode node, FSMMachine fsm) {
		
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		String xpath = ".//ConditionalExpression";
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()) {
			
			ASTConditionalExpression expression = (ASTConditionalExpression)itr.next();
			
			int childNum = expression.jjtGetNumChildren();
			
			if (childNum == 3) {
				
				List<Node> ll = expression.findChildrenOfType(ASTPrimaryExpression.class);
				
				if (ll.size() == 3) {
					
					ASTPrimaryExpression ae1 = (ASTPrimaryExpression)ll.get(0);
					ASTPrimaryExpression ae2 = (ASTPrimaryExpression)ll.get(1);
					ASTPrimaryExpression ae3 = (ASTPrimaryExpression)ll.get(2);
					
					String name1 = ae1.getType().getName();
					String name2 = ae2.getType().getName();
					String name3 = ae3.getType().getName();
					
					if ("CComBSTR".equals(name1) && "CComBSTR".equals(name2) && "BSTR".equals(name3)) {						
						addFSM(list, expression, fsm);
					}
				}				
			}												
		}		
		return list;
		
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("The CComBSTR did not expected to convert the BSTR type.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("��"+node.getBeginLine()+"�У�CComBSTR��û����Ԥ�ڵ�ת����BSTR�͡�");
		}	
	
	}
	
}
