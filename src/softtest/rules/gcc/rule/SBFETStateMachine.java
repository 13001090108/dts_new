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
 * @author LiuChang
 * Signed Bit Field Exceed Two
 * �з������͵�λ���ȱ�����ڵ�����λ(char�����ݵ��з�����������˵���ݱ���������)
 * (���㴦����)
 */
public class SBFETStateMachine {
	
	//�ҵ����еĽṹ����
	private static String xpath = ".//DeclarationSpecifiers/TypeSpecifier/StructOrUnionSpecifier[./StructOrUnion[@Image='struct']]";
	//�ҵ��ṹ�е�����'a:1','a:0'�ĳ�Ա�����������ռλ���� int :2��ʽ��
	private static String xpath1 = ".//StructDeclaration[./StructDeclaratorList/StructDeclarator/ConstantExpression/UnaryExpression/PostfixExpression/PrimaryExpression/Constant[@Image='0' or @Image='1']]/SpecifierQualifierList";
	
	private static List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
	private static List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
	
	public static List<FSMMachineInstance> createSBFETMachines(SimpleNode node, FSMMachine fsm){
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		List<SimpleNode> members = new LinkedList<SimpleNode>();
		
		for (SimpleNode snode : evaluationResults) {
			members = StateMachineUtils.getEvaluationResults(snode, xpath1);//�ҵ��ṹ�е�����'a:1','a:0'�ĳ�Ա����
		
			for(SimpleNode member : members) {
				//�����short a:1 �� int a:0 ����
				if(member.jjtGetNumChildren() == 1) {
					SimpleNode memberType = (SimpleNode) member.jjtGetChild(0);
					if(isSigned(memberType.getImage())) //���a���з�����,��a��λ������1��0
						addFSMDescription(member, fsm);
				}
				//�����unsigned short a:1 �� signed int a:1 ����
				else if(member.jjtGetNumChildren() > 1) {
					SimpleNode first = (SimpleNode) member.jjtGetChild(0);
					if(first.getImage()!=null  && first.getImage().equalsIgnoreCase("signed"))
							addFSMDescription(member, fsm);
				}	
			}
		}
	    return list;
	}	
	
	/*
	 * ��Ϊ���и�����Ϊλ�Ķ���������з����������޷���������
	 * �ݿ���long,longLong���ͣ��¸�����ģʽ�Ὣ�䱨����
	 */
	private static boolean isSigned(String str) {
		if("int".equalsIgnoreCase(str) || "short".equalsIgnoreCase(str)  || "long".equalsIgnoreCase(str) || "longLong".equalsIgnoreCase(str) || "char".equalsIgnoreCase(str)) 
			return true;
		else 
			return false;
	}

	private static void addFSMDescription( SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("Bit Filed of Signed Number should exceed or equal to Two");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("�з�������λ����Ӧ�ô��ڵ���2��\r\n�з�������ֻ��һλ�ĳ�����û������ġ�");
			}	
		
		list.add(fsminstance);
	}
}
