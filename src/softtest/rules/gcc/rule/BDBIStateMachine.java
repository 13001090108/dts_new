package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;


import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.pretreatment.Pretreatment;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;

/** 
 * @author LiuChang
 * Bit Define Be Int(unsigned or signed)
 * λ�Ķ���������з����������޷�������
 */
public class BDBIStateMachine {
	
	public static List<FSMMachineInstance> createBDBIMachines(SimpleNode node, FSMMachine fsm){
		//�ҵ����еĽṹ����
		String xpath = ".//DeclarationSpecifiers/TypeSpecifier/StructOrUnionSpecifier[./StructOrUnion[@Image='struct']]";
		//�ҵ��ṹ�е�λ��ĳ�Ա�����������ռλ�� int :2��ʽ
		String xpath1 = ".//StructDeclaration[./StructDeclaratorList/StructDeclarator/ConstantExpression/UnaryExpression/PostfixExpression/PrimaryExpression/Constant]/SpecifierQualifierList";
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		List<SimpleNode> members = new LinkedList<SimpleNode>();
		
		for (SimpleNode snode : evaluationResults) {
			members = StateMachineUtils.getEvaluationResults(snode, xpath1);//�ҵ��ṹ�е�����'a:3'�ĳ�Ա����
		
			for(SimpleNode member : members) {
				//����ǿ��ͷ�ļ�������֮
				String fileName = member.getFileName();
				if(fileName == null || isSysInc(fileName))//˵�������ڱ�����ļ���
					continue;	
				
				//�����int a:3 ����
				if(member.jjtGetNumChildren() == 1) {
					SimpleNode memberType = (SimpleNode) member.jjtGetChild(0);
					if(!(memberType.getImage().equalsIgnoreCase("int")))//���member����int����
						addFSMDescription(member, fsm, list);
				}
				//�����unsigned int a:4 ����
				else if(member.jjtGetNumChildren() == 2) {
					SimpleNode first = (SimpleNode) member.jjtGetChild(0);
					SimpleNode second = (SimpleNode) member.jjtGetChild(1);
					//���member����unsigned int
					if(!(first.getImage().equalsIgnoreCase("unsigned") && second.getImage().equalsIgnoreCase("int"))) { 
							addFSMDescription(member, fsm, list);
					}
				}	
			}
		}
	    return list;
	}	
	static String systemInc[] = new String[0];
	static boolean isSysInc(String fileName) {
		for (int i = 0; i < systemInc.length; i++) {
			if (fileName.startsWith(systemInc[i])) {
				return true;
			}
		}
		return false;
	}
	
	private static void addFSMDescription( SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" The Define of Bit filed should be int or unsigned int.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("λ�Ķ���������з����������޷���������\r\nλ���ܶ���Ϊ�з��Ż��޷�������֮����������͡�");
			}	
		
		list.add(fsminstance);
	}
}
