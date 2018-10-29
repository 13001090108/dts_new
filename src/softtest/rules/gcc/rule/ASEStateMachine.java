package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.ast.c.ASTIterationStatement;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;

/**
 * @author xiangwentao
 * Avoid Some Expression
 * ����ʹ�����ر��ʽ/����ʹ�ÿ����/����ʹ�ò������õ����/����ʹ��continue���  (�Ƽ���)
 * (���ʹ����)
 */

public class ASEStateMachine {
	private enum Type {TYPE_1, TYPE_2, TYPE_3, TYPE_4};
	
	//��ѯ���ر��ʽ
	private static final String xPath_1 = ".//ConditionalExpression[count(child::*)=3 and ./UnaryExpression] ";
	//��ѯ�����
	private static final String xPath_2 = ".//Statement[@EndColumn=@BeginColumn and ./ExpressionStatement] ";
	//��ѯ���в������õ����
	private static final String xPath_3 = ".//Statement/ExpressionStatement/Expression/AssignmentExpression[count(*)=1 and ./UnaryExpression[count(*)=1]] ";
	//��ѯcontinue���
	private static final String xPath_4 = ".//Statement[./JumpStatement[@Image='continue']] ";
	
	public static List<FSMMachineInstance> createASEMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();	
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		//Type.first_type ��ѯ�������ر��ʽ,��x==0?f1():f2();
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath_1);
		for(SimpleNode snode : evaluationResults) {
			addFSM(snode, fsm, Type.TYPE_1, list);
		}
		
		//Type.second_type ��ѯ���п����
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath_2);
		for(SimpleNode snode : evaluationResults) {
			addFSM(snode, fsm, Type.TYPE_2, list);
		}
		
		//Type.third_type ��ѯ����ʹ�ò������õ����,��x;
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath_3);
		for(SimpleNode snode : evaluationResults) {
			ASTUnaryExpression UnaryExp = (ASTUnaryExpression)snode.jjtGetChild(0);
			if(UnaryExp.getOperators().length() == 0 ){
				SimpleNode PostfixExp = (SimpleNode)UnaryExp.jjtGetChild(0);
				if(PostfixExp.getOperators().length() == 0 && PostfixExp.jjtGetNumChildren() == 1){
					SimpleNode PrimaryExp = (SimpleNode)PostfixExp.jjtGetChild(0);
					if(PrimaryExp.jjtGetNumChildren() == 0)
			            addFSM(snode, fsm, Type.TYPE_3, list);
				}
			}
		}
	    
	    //Type.fourth_type ��ѯ����continue���
	    evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath_4);
		for(SimpleNode snode : evaluationResults) {
			if(isIterationEnd(snode,node))
				addFSM(snode, fsm, Type.TYPE_4, list);
		}
	    return list;
	}
	
	/**
	 * @param ASTstatement
	 * @return ��������Ϊѭ���������һ�䣬����true;���򷵻�false
	 */
	private static boolean isIterationEnd(SimpleNode snode,Node node) {

		SimpleNode child = snode;
		SimpleNode iteration = (SimpleNode) child.getFirstParentOfType(ASTIterationStatement.class,node);
		if(iteration == null)
			return false;
		SimpleNode parent = (SimpleNode)snode.jjtGetParent();
		if(iteration.getImage().equals("while") || iteration.getImage().equals("for")) {
			do{
				int index = parent.jjtGetNumChildren()-1;
				if(parent.jjtGetChild(index) != child)
					return false;
				child = parent;
				parent = (SimpleNode)child.jjtGetParent();
			}while(parent != iteration);
		}
		else if(iteration.getImage().equals("do")) { //����do-whileѭ��
			iteration = (SimpleNode)iteration.jjtGetChild(0); //do-while�Ļ���������ĵ�һ���ӽڵ㣬��ASTstatement
			do{
				int index = parent.jjtGetNumChildren()-1;
				if(parent.jjtGetChild(index) != child)
					return false;
				child = parent;
				parent = (SimpleNode)child.jjtGetParent();
			}while(parent != iteration);
		}
		return true;
	}
	
	private static void addFSM(SimpleNode node, FSMMachine fsm, Type type, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if(type == Type.TYPE_1) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
					fsminstance.setDesp("Avoid using Triple Expressions. It will reduce the readabilty of the code.");
				} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
					fsminstance.setDesp("����ʹ�����ر��ʽ��\r\n���ر��ʽ��ʹ�ûή�ͳ���Ŀɶ��ԣ����׳��ֱ��ʧ��");
				}	
		}		
		else if(type == Type.TYPE_2) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Avoid using empty statement. Some compiler couldn't handle empty statement, so it is recommended to avoid using it.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ�ÿ���䡣\r\n��Щ�������ǲ��ܴ�������ģ����Խ������ʹ�ÿ���䡣");
			}
		}	
		else if(type == Type.TYPE_3) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Avoid using useless expression.It has no effect of the context, it belongs to useless statement.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ�ò������õ���䡣\r\n������û�иı�ֲ�������ȫ�ֱ�����ֵ��Ҳû��Ӱ�����������ô��Щ���Ϊ������䡣");
			}
		}
		else if(type == Type.TYPE_4) {
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Avoid using continue expression. Sometimes the use of continue statement will lead to a meaningless programm code. ");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ��continue��䡣\r\n���continue��ʹ����ʱ�ᵼ����ʵ������ĳ�����룬Ӧ����ʹ�á�");
			}
		}
		list.add(fsminstance);
	}
}


