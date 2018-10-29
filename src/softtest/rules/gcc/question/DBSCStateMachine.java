package softtest.rules.gcc.question;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTLabeledStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTStatementList;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
/** 
 * @author ssj
 */
public class DBSCStateMachine {

	public static List<FSMMachineInstance> createDBSCStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> switchResults = null;
		List<SimpleNode> ifResults = null;
		//��ѯ���������жϺͿ������
		String ifxPath = ".//SelectionStatement[@Image='if']";
		String switchxPath = ".//SelectionStatement[@Image='switch']";
		
		//�ж�if������Ƿ�����ͬ��֧/
		ifResults = StateMachineUtils.getEvaluationResults(node, ifxPath);
		Iterator ifitr = ifResults.iterator();
		while(ifitr.hasNext()){
			SimpleNode SelectionStatement = (SimpleNode)ifitr.next();
			if(SelectionStatement.jjtGetNumChildren()!= 3){		//��ƥ���else
				continue;
			}else if(!(SelectionStatement.jjtGetChild(1) instanceof ASTStatement )||!(SelectionStatement.jjtGetChild(2) instanceof ASTStatement )){
				continue;
			}else{
				SimpleNode a = (SimpleNode)SelectionStatement.jjtGetChild(1);
				SimpleNode b = (SimpleNode)SelectionStatement.jjtGetChild(2);
				if(!checkEqual(a,b)){
					continue;
				}else{
					addFSM(list,SelectionStatement,fsm);
				}
			}
		}		

		//�ж�switch������Ƿ�����ͬ��֧/
		switchResults = StateMachineUtils.getEvaluationResults(node, switchxPath);
		Iterator switchitr = switchResults.iterator();
		while(switchitr.hasNext()){
			SimpleNode SelectionStatement = (SimpleNode)switchitr.next();
			SimpleNode ss = (SimpleNode)SelectionStatement.jjtGetChild(1);
			SimpleNode ASTStatementList = (SimpleNode)ss.getFirstChildOfType(ASTStatementList.class);
			int count = -1;	//case��֧��
			if (ASTStatementList == null)
				continue;
			int n = ASTStatementList.jjtGetNumChildren();
			ArrayList<List<SimpleNode>> switchlist = new ArrayList<List<SimpleNode>>();
			SimpleNode sta = null;
			int countnull = 0;//���磺case 1�����κδ���ķ�֧��
			
			//Ϊ����֧����,���ų����ַ�֧�ṹ����
			for(int i=0;i<n;i++){
				sta = (SimpleNode)ASTStatementList.jjtGetChild(i);
				//������case��ͷ�����
				if(count < 0 && !(sta.jjtGetChild(0) instanceof ASTLabeledStatement))
					continue;
				if(sta.jjtGetChild(0) instanceof ASTLabeledStatement){
					ASTLabeledStatement labeledStatement = (ASTLabeledStatement)sta.jjtGetChild(0);
//					if(labeledStatement.getFirstChildOfType(ASTLabeledStatement.class) != null){
					if((labeledStatement.jjtGetNumChildren() > 1) && (labeledStatement.jjtGetChild(1).jjtGetChild(0) instanceof ASTLabeledStatement)){
						countnull++;
						labeledStatement = (ASTLabeledStatement)labeledStatement.jjtGetChild(1).jjtGetChild(0);
						if((labeledStatement.jjtGetNumChildren() > 1) && (labeledStatement.jjtGetChild(1).jjtGetChild(0) instanceof ASTLabeledStatement)){
							countnull++;
							break;
//							labeledStatement = (ASTLabeledStatement)labeledStatement.jjtGetChild(1).jjtGetChild(0);
						}else{
							count++;
							sta = (SimpleNode)labeledStatement.getFirstDirectChildOfType(ASTStatement.class);
//							sta = (SimpleNode)labeledStatement.jjtGetChild(1);
							List<SimpleNode> xlist = new ArrayList<SimpleNode>(); 
							xlist.add(sta);
							switchlist.add(count,xlist);
						}
					}else{
						count++;
						sta = (SimpleNode)labeledStatement.getFirstDirectChildOfType(ASTStatement.class);
						List<SimpleNode> xlist = new ArrayList<SimpleNode>(); 
						xlist.add(sta);
						switchlist.add(count,xlist);
					}
				}else{
					switchlist.get(count).add(sta);
				}	
			}
			
			boolean stag = false;
			if(countnull>=2)
				addFSM(list,SelectionStatement,fsm);
			else {
				if(count < 2){
					continue;
				}else{
					for(int i=0;i<=count;i++){
						if (stag) 
						break;
						else{
							for(int j=i+1;j<=count;j++){
								if(checklistequal(i,j,switchlist)){
									stag=true;
									break;
								}
							}
						}
					}
					if(stag)
						addFSM(list,SelectionStatement,fsm);			
				}
			}	
		}
	    return list;
    }
	
	
	/**���switchlist�еĵ�i���͵�j�����Ƿ���ͬ
	 * ��switch����i����j��case��֧�Ƿ���ͬ*/
	private static boolean checklistequal(int i,int j,ArrayList<List<SimpleNode>> switchlist){
		
		Iterator itr1 = switchlist.get(i).iterator();	
		Iterator itr2 = switchlist.get(j).iterator();	
		
		int num1=0,num2=0;

		while(itr1.hasNext()){
			num1++;
			itr1.next();
		}
		while(itr2.hasNext()){
			num2++;
			itr2.next();
		}
		if(num1!=num2)
		return false;
		else{
			itr1 = switchlist.get(i).iterator();	
			itr2 = switchlist.get(j).iterator();
			
			while(itr1.hasNext()){
				SimpleNode sta1=(SimpleNode)itr1.next();
				SimpleNode sta2=(SimpleNode)itr2.next();
				if(!checkEqual(sta1,sta2))
					return false;	
			}
			return true;
		}	
	}
	
	/**���a,b�ڵ㼰���ж�Ӧ�����Ƿ���ͬ*/
	private static boolean checkEqual(SimpleNode a,SimpleNode b){
		
		if(!a.toString().equals(b.toString()) || !a.getImage().equals(b.getImage() ) )
			return false;
		String s1 = a.getOperators();
		String s2 = b.getOperators();
		
		/*getOperators()�������ص�ֵӦ��null�������
		 *ʵ�ʻ�ֵ������[]�����ǡ�null��.�ڴ˽���ͳһ  */
		
		if (s1.equals("[]"))
			s1="null";
		if (s2.equals("[]"))
			s2="null";
		
		if(!s1.equals(s2)){		
			return false;
		}
		if(a.jjtGetNumChildren() != b.jjtGetNumChildren())
			return false;
		if(a.jjtGetNumChildren()==0 && b.jjtGetNumChildren()==0)
			return true;
		for(int i=0;i<a.jjtGetNumChildren();i++){
			if(!checkEqual((SimpleNode)a.jjtGetChild(i),(SimpleNode)b.jjtGetChild(i)))
				return false;
			}
		if(a.getImage().equalsIgnoreCase("goto")){
			if(!a.getImage().equals(b.getImage()))
				return false;
			}
		return true;
	}
		
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" DifferentBranchSameCode: The codes of different branch of selection statement are same.This is an ill conditioned flow control.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("�ڵ�"+node.getBeginLine()+"�е������жϡ��������ķ�֧����ͬ�Ĵ���: �������жϺͿ������ķ�֧�У�ʹ������ͬ�Ĵ��룬����һ�ֲ�̬�Ŀ�������" );
						}	
		list.add(fsminstance);
	
	}
}
