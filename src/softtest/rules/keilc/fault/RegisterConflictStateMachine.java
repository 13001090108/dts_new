package softtest.rules.keilc.fault;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTInterrupt;
import softtest.ast.c.ASTPRAGMA;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTUsing;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * �жϺ����е���������������ͨ��������������ж�ʹ����ͬ�ļĴ�����
 * @author zys	
 * 2010-3-11
 */
public class RegisterConflictStateMachine {
	
	private static final String ARGS="NOAREGS";
	private static boolean isCallOK=false;
	/**
	 * ��⵱ǰ��������ڵ��Ƿ�Ϊ�жϺ���
	 * @param nodes
	 * @param fsmin
	 * @return
	 */
	public static List<FSMMachineInstance> checkIsInterruptMethod(SimpleNode node, FSMMachine fsm)
	{
		List<FSMMachineInstance> list=new LinkedList<FSMMachineInstance>();
		
		String usingRegisterNum="0";
		String interruptPriority;
			if(node instanceof ASTFunctionDefinition)
			{
				//�ж��Ƿ�Ϊ�жϺ��������������Ƿ����interrupt
				List temp=node.findChildrenOfType(ASTInterrupt.class);
				if(temp.size()>0)
				{
					//�ж����ȼ�
					ASTInterrupt interruptNode=(ASTInterrupt)temp.get(0);
					interruptPriority=interruptNode.getInternum();
					
					//�鿴�жϺ����Ƿ�ָ������������ͬ�ļĴ����飬Ŀǰ������Ĵ�����Ĭ��Ϊ0
					Node usingNode=interruptNode.getNextSibling();
					if(usingNode instanceof ASTUsing)
					{
						usingRegisterNum=((ASTUsing)usingNode).getUsingnum();
						if(Integer.parseInt(usingRegisterNum)==0){
							isCallOK=true;
							return list;
						}
						addFSM(list, node, fsm, Integer.parseInt(interruptPriority), Integer.parseInt(usingRegisterNum));
					}
				}
			}
		return list;
	}
	
	/**
	 * ������жϺ����ڲ������ⲿ��ͨ����ʱ���Ƿ�����˿��Ʋ�����
	 * @param nodes
	 * @param fsmin
	 * @return
	 */
	public static boolean checkCallOuterFunctionOK(List nodes, FSMMachineInstance fsmin)
	{
		return isCallOuterFunctionOK(nodes, fsmin);
	}
	
	public static boolean checkCallOuterFunctionError(List nodes, FSMMachineInstance fsmin)
	{
		
		return !isCallOuterFunctionOK(nodes, fsmin);
	}
	
	private static int num=1;
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm,int priority,int usingRegister) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		MethodNameDeclaration varDecl = ((ASTFunctionDefinition)node).getDecl();
		fsminstance.setRelatedASTNode(node);
		fsminstance.setReleatedVexNode(node.getCurrentVexNode());
		fsminstance.setDesp("����" + varDecl.getImage() + "����Ϊ�жϺ���,�ж����ȼ�Ϊ"+priority
				+",ʹ�õļĴ�����Ϊ"+(usingRegister==0?"0(Ĭ�ϼĴ�����)":usingRegister));
		list.add(fsminstance);
	}
	
	private static boolean isCallOuterFunctionOK(List nodes, FSMMachineInstance fsmin)
	{
		if(isCallOK)
			return true;
		for(Object node:nodes)
		{
			if(node instanceof ASTPrimaryExpression)
			{
				Node statementNode=((ASTPrimaryExpression)node).getFirstParentOfType(ASTStatement.class);
				Node prevStatementNode=((ASTStatement)statementNode).getPrevSibling();
				if(prevStatementNode instanceof ASTStatement)
				{
					Node pragmaNode=prevStatementNode.jjtGetChild(0);
					if(pragmaNode instanceof ASTPRAGMA)
					{
						boolean b=((ASTPRAGMA)pragmaNode).pragmaInfoStringList().trim().equals(ARGS);
						return b;
					}
				}
				
			}
		}
		return false;
	}
}
