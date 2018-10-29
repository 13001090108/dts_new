package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;


/** 
 * @author 
 * Avoid Comma Operator(�Ƽ���)
 * ����ʹ�ö��Ų�����(�����ڲ������ѭ���п���ʹ�ö��Ų�����)
 */
public class ACOStateMachine {	
	
	
	public static List<FSMMachineInstance> createACOMachines(SimpleNode node, FSMMachine fsm){
		//�ҵ����Ų������ı��ʽ
		String xpath = ".//Expression[count(child::AssignmentExpression)>1]";
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);	
		for (SimpleNode snode : evaluationResults) {
			//ѭ���п���ʹ�ö��Ų�����
			if(snode.jjtGetParent() instanceof ASTIterationStatement)
				continue;
			//��ȥnew AVIReadCache(fcc== 'sdiv' ? 131072 : 16384, streamno);���
			if(snode.jjtGetParent() instanceof ASTConditionalExpression)
				continue;
			//��ȥassert(f)���������Ԥ������Ϊ(void)( (f) || (_assert("f", "F:\\1.cpp", 6), 0) );
			if(snode.getImage()!= null && snode.getImage().equals("_assert"))
			//if(snode.getImageS()!= null && snode.getImages().size() >= 1 && snode.getImages().get(0).equals("_assert"))
				continue;
			
			addFSMDescription(snode,fsm, list);
		}
	    return list;
	}	

	private static void addFSMDescription(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("avoid using comma operator. Using comma operator will reduce the readability of the code.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����ʹ�ö��Ų�������\r\n�����ڲ������ѭ���п���ʹ�ö��Ų����������򶺺Ų�������ʹ�û�ʹ����Ŀɶ��Խ��͡�");
			}	
		
		list.add(fsminstance);
	}
}

