package softtest.rules.gcc.question;

import java.util.*;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.pretreatment.Pretreatment;
import softtest.rules.c.StateMachineUtils;
import softtest.fsm.c.*;
import softtest.interpro.c.InterContext;

/** 
 * @author liuli
 * HeadFileStatic(HFS)
 */
public class HFSStateMachine {
	
	public static List<FSMMachineInstance> createHFSStateMachines(SimpleNode node, FSMMachine fsm){
		//�ҵ�����ȫ�ֵľ�̬��������̬����
		String xpath = ".//ExternalDeclaration/Declaration[./DeclarationSpecifiers/StorageClassSpecifier[@Image='static']] | " +
					   ".//ExternalDeclaration/FunctionDefinition[./DeclarationSpecifiers/StorageClassSpecifier[@Image='static']] | " +
					   ".//ExternalDeclaration/FunctionDeclaration[./DeclarationSpecifiers/StorageClassSpecifier[@Image='static']]";
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		Hashtable<SimpleNode, FSMMachineInstance> resultsTable= new Hashtable<SimpleNode, FSMMachineInstance>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator<SimpleNode> itr = evaluationResults.iterator();	
		while(itr.hasNext()){				
			SimpleNode decl = (SimpleNode)itr.next();
			String fileName = decl.getFileName();
//			if(fileName==null || Pretreatment.isSysInc(fileName))//˵�������ڱ�����ļ���
//				continue;		
			if(fileName!=null && fileName.matches(InterContext.INCFILE_POSTFIX))//˵��������ͷ�ļ���
				addFSM(decl, fsm, resultsTable, list);
		}
	    return list;
	}
	
	
	private static void addFSM(SimpleNode node, FSMMachine fsm, Hashtable<SimpleNode, FSMMachineInstance> resultsTable, List<FSMMachineInstance> list) {	
		if(resultsTable.containsKey(node))
			return;
		FSMMachineInstance fsminstance = fsm.creatInstance();		
		fsminstance.setRelatedASTNode(node);
		resultsTable.put(node, fsminstance);
		if(node instanceof ASTDeclaration)
		{
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
				fsminstance.setDesp("Static variable"+node.getFileName()+" appears in head file. This behavior makes the size of executable file more larger. ");
			else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
				fsminstance.setDesp("ͷ�ļ�"+node.getFileName()+"�ж����˾�̬������\r��ᵼ�£�������ͷ�ļ�����һ�ļ���������һ�ݸñ����������˿�ִ���ļ��Ĵ�С��");	
		} 
		else 
		{
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
				fsminstance.setDesp("Static function"+node.getFileName()+" appears in head file. This behavior makes the size of executable file more larger. ");
			else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
				fsminstance.setDesp("ͷ�ļ�"+node.getFileName()+"�ж����˾�̬������\r��ᵼ�£�������ͷ�ļ�����һ�ļ���������һ�ݸñ����������˿�ִ���ļ��Ĵ�С��");
		}
		list.add(fsminstance);
	}
}
