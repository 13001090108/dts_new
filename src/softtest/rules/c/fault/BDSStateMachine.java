package softtest.rules.c.fault;


import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;



/**
 * @author dn
 * Break Dead Statement[break֮���������]
 * */
public class BDSStateMachine {
	
	/**
	 * �ҵ�break֮����������
	 * */
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	public static List<FSMMachineInstance> createBDSStateMachines(SimpleNode node, FSMMachine fsm){	
		String xpath =".//Statement[./JumpStatement[@Image='break']]";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		for (SimpleNode snode : evaluationResults) {
			if(snode.jjtGetParent() instanceof ASTLabeledStatement)
				snode = (SimpleNode) snode.jjtGetParent().jjtGetParent();
			
			//�ҵ��˵�ǰsnode�ڵ㣬Ȼ��find�Ƿ�ýڵ�����ֵܽڵ㣬����У�������labeled_statement����,���ܼ��뵽list��
			SimpleNode next = findNextStatement((ASTStatement)snode);
			if(next == null || next.jjtGetNumChildren() < 1)
				continue;
			SimpleNode child = (SimpleNode) next.jjtGetChild(0);
			if(child instanceof ASTLabeledStatement && (child.getImage().equals("case") || child.getImage().equals("default"))) {
				continue;
			} else {
				addFSMDesp(child, fsm);
			}
		}
	    return list;
	}
	

	/**
	 * Find the statement after this return statement
	 */
	private static SimpleNode findNextStatement(ASTStatement node) {
		SimpleNode parent = (SimpleNode) node.jjtGetParent();
		if(!(parent instanceof ASTStatementList))
			return null;
		int index = 0;
		for(index=0; index<parent.jjtGetNumChildren(); index++) {
			if(parent.jjtGetChild(index) == node) {
				break;
			}
		}
		index++;
		if(index<parent.jjtGetNumChildren()) {
			SimpleNode next = (SimpleNode) parent.jjtGetChild(index);
			return next;
		}
		return null;
	}


	private static void addFSMDesp(SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		fsminstance.setDesp(" ��" + node.getBeginLine() + "�����Ϊbreak�����������䣬�����롣");
		list.add(fsminstance);
	}
	
}
