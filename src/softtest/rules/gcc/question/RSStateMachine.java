package softtest.rules.gcc.question;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTJumpStatement;
import softtest.ast.c.ASTLabeledStatement;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTStatementList;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
/** 
 * @author ssj
 */
public class RSStateMachine {
	
	public static List<FSMMachineInstance> createRedundanceStatementMachines(SimpleNode node, FSMMachine fsm){	
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		
		//��return/break/continue֮��������������
		createRedundanceStatement1(node, fsm, list);	
		//���⴦���ҵ������ж�����е��������
		doSelectionSpecial(node, fsm, list);
		//���⴦�� switch���
		doSwitchSpecial(node, fsm, list);
		//���⴦�� ѭ��������һ�����Ϊreturn( �����if���Ļ�������ifAllReturn()����)
		doIterationSpecial(node, fsm, list);
	    return list;
	}
	
	/**
	 * ���⴦�� ѭ��������һ�����Ϊreturn
	 */
	private static void doIterationSpecial(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		//�ҵ�ѭ�����(���һ�����Ϊreturn)
		String iterationXpath = "//Statement[./IterationStatement/Statement/CompoundStatement/StatementList/Statement[last()]/JumpStatement[@Image='return']]";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, iterationXpath);
		if(evaluationResults == null)
			return ;
		for(SimpleNode snode : evaluationResults) {
			String xPath = ".//JumpStatement[Image='continue' or @Image='break']";
			List<SimpleNode> jumpResults = StateMachineUtils.getEvaluationResults(snode, xPath);
			if(jumpResults != null){
				continue;
			}
			SimpleNode next = findNextStatement((ASTStatement)snode);
			if(next!=null  && next.jjtGetNumChildren() >= 1 && !(next.jjtGetChild(0) instanceof ASTLabeledStatement))
				addFSMDesp(next, fsm, list);
		}
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
			if(parent.jjtGetChild(index) == node) 
				break;
		}
		index++;
		if(index<parent.jjtGetNumChildren()) {
			SimpleNode next = (SimpleNode) parent.jjtGetChild(index);
			return next;
		}
		return null;
	}

	/**
	 * ���⴦��switch��䣬���еı�Ƕ���return���
	 */
	private static void doSwitchSpecial(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		String xpath =".//Statement/SelectionStatement[@Image='switch']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		if(evaluationResults == null)
			return ;
		for(SimpleNode switchNode : evaluationResults) {
			//step1�� switch�Ƿ���default��ǽڵ㣬��default���ü���
			String defaultXpath ="./Statement/CompoundStatement/StatementList/Statement[./LabeledStatement[@Image='default']]";
			List<SimpleNode> results = StateMachineUtils.getEvaluationResults(switchNode, defaultXpath);
			if(results == null || results.size()==0)
				continue;
			//step2�� �ҵ�switch������еı�ǽڵ㣬����ÿ����ǽڵ㣬�ж��Ƿ���return���
			String labelXpath ="./Statement/CompoundStatement/StatementList/Statement[./LabeledStatement[@Image='case' or @Image='default']]";
			List<SimpleNode> labelResults = StateMachineUtils.getEvaluationResults(switchNode, labelXpath);
			for(SimpleNode label : labelResults) {
				//state1: ���֮�����if���
				if(label.getFirstChildOfType(ASTSelectionStatement.class) != null) {
					SimpleNode snode = (SimpleNode) label.getFirstChildOfType(ASTSelectionStatement.class); 
					if(isAllReturn(snode))
						continue;
				} 
				//state2:�����֮�����return���
				else if(label.getFirstChildOfType(ASTJumpStatement.class) != null) { 
					if(((SimpleNode)label.getFirstChildOfType(ASTJumpStatement.class)).getImage().equals("return"))
						continue;
					else  //�ñ������return��� 
						return ; 
				}
				//state��: ���֮���return���(�������ǽ������֮��)
				SimpleNode parent = (SimpleNode) label.jjtGetParent();
				int index = 0;
				for(index=0; index<parent.jjtGetNumChildren(); index++) {
					if(parent.jjtGetChild(index) == label) {
						break ;
					}
				}
				//�ҵ�������֮����������
				while(index+1 < parent.jjtGetNumChildren()) {
					index++;
					SimpleNode next = (SimpleNode) parent.jjtGetChild(index);
					if(next.getFirstChildOfType(ASTLabeledStatement.class) != null)  //nextΪ�ñ�ǽڵ�
						return ; 
					
					//state3.1: ���֮�����if���
					if(next.getFirstChildOfType(ASTSelectionStatement.class) != null) {
						SimpleNode snode = (SimpleNode) next.getFirstChildOfType(ASTSelectionStatement.class); 
						if(isAllReturn(snode))
							break;
					} 
					//state3.2: ���֮����if���
					else if(next.getFirstChildOfType(ASTJumpStatement.class) != null) {
						if(((SimpleNode)next.getFirstChildOfType(ASTJumpStatement.class)).getImage().equals("return"))
							break;
						else if(((SimpleNode)next.getFirstChildOfType(ASTJumpStatement.class)).getImage().equals("break"))
							return ;
					}
				}
			}
			//�õ�switch���ĸ��ڵ�
			ASTStatement curNode = (ASTStatement) switchNode.jjtGetParent();
			SimpleNode next = findNextStatement(curNode);
			if(next != null  && next.jjtGetNumChildren() > 0 && !(next.jjtGetChild(0) instanceof ASTLabeledStatement))
				addFSMDesp(next, fsm, list);
		}
	}

	/*
	 * ��return/break/continue֮��������������
	 */
	private static void createRedundanceStatement1(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		//�ҵ����е�break��䡢continue����return��� 
		String xpath =".//Statement[./JumpStatement[@Image='return' or @Image='break' or @Image='continue']] ";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		for (SimpleNode snode : evaluationResults) {
			SimpleNode presnode = snode;
			if(snode.jjtGetParent() instanceof ASTLabeledStatement)
				snode = (SimpleNode) snode.jjtGetParent().jjtGetParent();
			
			//�ҵ��˵�ǰsnode�ڵ㣬Ȼ��find�Ƿ�ýڵ�����ֵܽڵ㣬����У�������ASTLabeledStatement����,���ܼ��뵽list��
			SimpleNode next = findNextStatement((ASTStatement)snode);
			if(next == null || next.jjtGetNumChildren() < 1)
				continue;
			SimpleNode child = (SimpleNode) next.jjtGetChild(0);
			if(child instanceof ASTLabeledStatement && (child.getImage().equals("case") || child.getImage().equals("default")))
				continue;
			else if(((SimpleNode)presnode.jjtGetChild(0)).getImage().equals("return") && child instanceof ASTLabeledStatement) //���⴦��return֮���б����䣬���ñ����֮ǰgoto label1��Ӧ�������.
				continue;
			else
				addFSMDesp(child, fsm, list);
		}
	}


	/*
	 * ��������ж����ķ�֧�У�������з����˳���䣬������������������
	 */
	public static void doSelectionSpecial(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		//�ҵ����е�if���
		String xpath =".//SelectionStatement[@Image='if']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		if(evaluationResults == null || evaluationResults.size()==0)
			return ;
		for (SimpleNode snode : evaluationResults) {
			if(isAllReturn(snode)) {
				SimpleNode parent = (SimpleNode) snode.jjtGetParent(); //�õ�Statement�ڵ�
				SimpleNode  ancestor = (SimpleNode) parent.jjtGetParent();//����õ�snode���游�ڵ�StatementList
				while(!(ancestor instanceof ASTStatementList)) {
					parent = ancestor;
					ancestor = (SimpleNode) parent.jjtGetParent();
				}
				//��if���֮���нڵ�,�õ�if����next���
				SimpleNode next = findNextStatement((ASTStatement) parent);
				if(next != null &&  next.jjtGetNumChildren() >= 1 && !(next.jjtGetChild(0) instanceof ASTLabeledStatement)) { //���if֮����case��䣬���ܱ�
					addFSMDesp(next, fsm, list);
				}
			}
		}
	}
	
	/*
	 * @param ASTSelectionStatement
	 * �жϸýڵ���������Ƿ���з���ֵ
	 */
	private static boolean isAllReturn(SimpleNode snode) {
		if(snode.jjtGetNumChildren() < 3)//���if���û��if-else��֧�Ļ�
			return false;		
		SimpleNode ifchild = (SimpleNode) snode.jjtGetChild(1);
		SimpleNode elsechild = (SimpleNode) snode.jjtGetChild(2);
		if(ifchild.jjtGetNumChildren()> 0 && ifchild.jjtGetChild(0) instanceof ASTSelectionStatement && !isAllReturn(ifchild))
			return false;	
		if(elsechild.jjtGetNumChildren()> 0 && elsechild.jjtGetChild(0) instanceof ASTSelectionStatement && !isAllReturn(elsechild))
			return false;

		String hasReturn = "./JumpStatement[@Image='return'] | ./CompoundStatement/StatementList/Statement/JumpStatement[@Image='return']";
		List  ifResult = StateMachineUtils.getEvaluationResults(ifchild, hasReturn);
		List elseResult = StateMachineUtils.getEvaluationResults(elsechild, hasReturn);
		if( ifResult == null || elseResult == null )
			return false;
		if( ifResult.isEmpty()!=true && elseResult.isEmpty()!=true ) 
			return true;
		return false;
	}


	private static void addFSMDesp(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" This Line belongs to resudance statement which is after return, break or continue statement. This code belongs to dead code.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��"+node.getBeginLine()+" ��Ϊreturn��break��continue�����������䡣\r\n����䲻�ᱻִ�У������롣");
			}	
		list.add(fsminstance);
	}
}
