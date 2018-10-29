package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;


/**
 * @author xiangwentao
 * Avoid Register Variable
 * ����ʹ�üĴ�������  (�Ƽ���)
 * (���ʹ����)
 */
public class ARVStateMachine {
	
	public static List<FSMMachineInstance> createARVMachines(SimpleNode node, FSMMachine fsm){
		//��ѯ���мĴ�����������
		String xPath = ".//Declaration[./DeclarationSpecifiers/StorageClassSpecifier[@Image='register']]";
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResult = StateMachineUtils.getEvaluationResults(node, xPath);
		
		for(SimpleNode snode : evaluationResult){
			addFSM(snode, fsm, list);
		}
		return list;
	}

	private static void addFSM(SimpleNode node, FSMMachine fsm,List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if(Config.DTS_LANGUAGE == Config.LANG_ENGLISH){
			fsminstance.setDesp("Avoid using Register Variable. Using register variable would produce an unexpected reslut.");
		}
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("����ʹ�üĴ���������\r\n�Ĵ���������ʹ�ÿ��ܻ��������Ԥ�ϵĽ����Ӧ����ʹ�á�");
		}
		list.add(fsminstance);
	}
	
}
