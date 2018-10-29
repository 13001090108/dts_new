package softtest.rules.gcc.rule;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.pretreatment.Pretreatment;

/**
 * @author Maojinyu
 * ����һ�����̻����еĳ������в�����200��
 * ����һ���ļ��еĳ������в�����2000��
 * (������д��)
 */
public class LLStateMachine {	
	
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	//�洢�Ѿ���¼���ļ��ڵ㣨���ļ��еĳ������в�����2000�У�
	private static Set<ASTTranslationUnit> files = new HashSet<ASTTranslationUnit>();
	
	public static List<FSMMachineInstance> createLLMachines(SimpleNode node, FSMMachine fsm){
		
		//1. ��Թ��̻����еĳ������в�����200��
		String fileName = node.getFileName();
		int line = node.getEndLine() - node.getBeginLine();
		if(line > 200) {
			if(fileName==null || fileName.matches(InterContext.INCFILE_POSTFIX)) {//˵�������ڱ�����ļ���
				return list;
			}
			FSMMachineInstance fsminstance = fsm.creatInstance();
			fsminstance.setRelatedASTNode(node);
			if(Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Proposed a method or function no more than 200 lines.");
			} else if(Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����һ���ļ��еĹ��̻��������в�����200�С�\r\n�ڵ�"+node.getBeginFileLine()+"�еĺ���"+node.getImage()+"��������Ϊ" + line + "��");
			}	
			list.add(fsminstance);
		}
	
		//2. ����ļ��еĳ������в�����2000��		
		ASTTranslationUnit snode = (ASTTranslationUnit) node.getFirstParentOfType(ASTTranslationUnit.class);
		SimpleNode lastFunc = (SimpleNode) snode.jjtGetChild(snode.jjtGetNumChildren()-1);
		if(lastFunc.getEndLine() > 2000) {
			if(files.contains(snode)) {
				return list;
			} else {
				files.add(snode);
			}
			FSMMachineInstance fsminstance = fsm.creatInstance();
			if(Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Proposed a file no more than 2000 lines.");
			} else if(Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("����һ���ļ��еĳ������в�����2000�С�\r\n�ú������ڵ��ļ� " + fileName + "������Ϊ" + lastFunc.getEndLine()+ "��");
			}	
			list.add(fsminstance);
		}	
		return list;
	}	
}
