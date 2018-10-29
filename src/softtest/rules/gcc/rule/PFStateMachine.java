package softtest.rules.gcc.rule;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;

/** 
 * @author DongNa
 * ��ֹ����������Ϊָ������
 * ProcedurePointer
 */

public class PFStateMachine {

	public static List<FSMMachineInstance> createPFStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = null;
		
		/*��ѯ����ָ�����͵�����������Ϊ�պͲ�Ϊ�յģ�
	  	��ʽ1����������(*������)(������) 
		��ʽ2��typedef ��������(*������)(������) ??
		
	*/		
		String xPath=".//Declaration/InitDeclaratorList/InitDeclarator/Declarator/DirectDeclarator/Declarator[/Pointer]";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		Iterator itr = evaluationResults.iterator();
		                                                                                                                                                                                                                                                                                                                                                                                                                                                
		while(itr.hasNext()){
			ASTDeclarator id = (ASTDeclarator)itr.next();
			addFSM(list,id,fsm);
			}
		return list;
		
  }
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("ProcedurePointer: Using function pointer is dangerous,so it's banded to use function pointer.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("�ڵ�"+node.getBeginFileLine()+"�г��ֹ�������Ϊָ������: ʹ�ù���ָ���Ǿ��нϴ���յģ���˽�ֹ����������Ϊָ�����͡�");
			}	
		
		list.add(fsminstance);
	}
}



