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
 * @author LiuChang
 * ö��Ԫ�صĳ�ʼ�������� 
 * IncompleteInitialOfEnum
 */

public class IIOEStateMachine {

	public static List<FSMMachineInstance> createIIOEStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = null;
		
		String xPath=".//EnumSpecifier/EnumeratorList";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		Iterator itr = evaluationResults.iterator();
		                                                                                                                                                                                                                                                                                                                                                                                                                                                
		while(itr.hasNext()){
			SimpleNode enumList = (SimpleNode)itr.next();
			int i = enumList.jjtGetNumChildren();
			if (i == 0 )
				continue;
			ASTEnumerator firstEnum = (ASTEnumerator)enumList.jjtGetChild(0);
			//��һ��
			if(firstEnum.jjtGetNumChildren() == 0)
			{
				addFSM(list,enumList,fsm);
				continue;	
			}
			if(i <= 2)
				continue;
			ASTEnumerator secEnum =(ASTEnumerator)enumList.jjtGetChild(1);
			if(secEnum.jjtGetNumChildren() == 0){
				for(int j = 2;j < i;j++){
					ASTEnumerator enode =(ASTEnumerator)enumList.jjtGetChild(j);
					if(enode.jjtGetNumChildren() != 0){
						addFSM(list,enumList,fsm);
						break;	
				}
				}
			}
			else{
				for(int j = 2;j < i;j++){
					ASTEnumerator enode =(ASTEnumerator)enumList.jjtGetChild(j);
					if(enode.jjtGetNumChildren() == 0){
						addFSM(list,enumList,fsm);
						break;	
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
				fsminstance.setDesp("IncompleteInitialOfEnum: The form of enum initiation is safe only in to circumstances��one is to initiate all the element,the other is to initiate the first one.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("ö��Ԫ�صĳ�ʼ��������: ö�����͵ĳ�ʼ��ֻ��������ʽ�ǰ�ȫ�ġ�һ�ǳ�ʼ�����е�Ԫ�أ�����ֻ��ʼ����һ��Ԫ�ء�");
			}	
		
		list.add(fsminstance);
	}
}