package softtest.rules.gcc.question;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.rules.c.StateMachineUtils;


/*
 * @author Yanxin
 * δ֪��ӡ��ʽ 
 */
public class UKFStateMachine {

	public static List<FSMMachineInstance> createUKFStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = null;
		
		
//		ASTTranslationUnit  translationUnitNode = (ASTTranslationUnit)node.getFirstParentOfType(ASTTranslationUnit.class);
//		if(translationUnitNode==null)
//		{
//			return list;
//		}
		
		String xpath =".//PostfixExpression";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		Iterator<SimpleNode> itr = evaluationResults.iterator();
		while(itr.hasNext())
		{
			SimpleNode s = (SimpleNode)itr.next();
			//�ҳ�����printf�ַ��ĺ���
		   if (s.getImage().contains("printf"))
		   {
			   List primaryList = s.findChildrenOfType(ASTPrimaryExpression.class);
			   Iterator priItr = primaryList.iterator();
			   while(priItr.hasNext())
			   {
				   SimpleNode primaryExpression = (SimpleNode)priItr.next();
				   if(primaryExpression.jjtGetNumChildren()!=0)
				   {
					   SimpleNode con = (SimpleNode)primaryExpression.jjtGetChild(0);
					   //�ҳ����������ַ�����printf�еĴ�ӡ��ʽ
					   String total = con.getImage();
					   String[] a = total.split("%");
					   for(int i = 1; i < a.length;i++)
					   {   
						   int t = 0;
						  //�жϴ�ӡ��ʽ���ҳ���ӡ��ʽ���Ʒ���λ�ã�������ǰ�������жϴ�ӡ��ʽ���Ʒ��Ƿ�Ϸ�
						   if((t=a[i].indexOf("d"))>-1||(t = a[i].indexOf("i"))>-1||(t=a[i].indexOf("o"))>-1||(t=a[i].indexOf("u"))>-1||(t=a[i].indexOf("x"))>-1||(t=a[i].indexOf("X"))>-1)
						   {     t--; 
							  for(;t>=0;t--)
							  {   
								 String n = a[i];
								 char p[] = n.toCharArray();
								 if(p[t]=='-'||p[t]=='n'||p[t]=='m'||p[t]=='0'||p[t]=='%'||p[t]=='l'||p[t]=='h'||('0'<=p[t]&&p[t]<='9')||p[t]=='.')
								 {
									 continue;
								 }
								 else
								 {
									 addFSM(list,primaryExpression,fsm);
								       break; 
								 }
								
							  }
							  continue;
						   }
						   if((t = a[i].indexOf("e"))>-1||(t=a[i].indexOf("E"))>-1||(t = a[i].indexOf("f"))>-1||(t = a[i].indexOf("L"))>-1)
						   {        t--;
							   for(;t>=0;t--)
								  {   
									 String n = a[i];
									 char p[] = n.toCharArray();
									 if(p[t]=='-'||p[t]=='n'||p[t]=='m'||p[t]=='0'||p[t]=='%'||p[t]=='l'||p[t]=='h'||('0'<=p[t]&&p[t]<='9')||p[t]=='.')
									 {
										 continue;
									 }
									 else
									 {
										 addFSM(list,primaryExpression,fsm);
									       break; 
									 }
									
								  }
							   continue;
						   }
						   if((t = a[i].indexOf("g"))>-1||(t=a[i].indexOf("G"))>-1||(t =a[i].indexOf("p"))>-1||(t=a[i].indexOf("n"))>-1||(t=a[i].indexOf("%"))>-1||(t=a[i].indexOf("c"))>-1||(t=a[i].indexOf("s"))>-1)
						   {     t--;
							   for(;t>=0;t--)
								  {
									 String n = a[i];
									 char p[] = n.toCharArray();
									 if(p[t]=='-'||p[t]=='n'||p[t]=='m'||p[t]=='0'||p[t]=='%'||p[t]=='l'||p[t]=='h'||('0'<=p[t]&&p[t]<='9')||p[t]=='.')
									 {
										 continue;
									 }
									 else
									 {
										 addFSM(list,primaryExpression,fsm);
									       break; 
									 }
								  }
							   continue;
						   }
						   addFSM(list,primaryExpression,fsm);
					       break;
					   }
				 
				   
				   }
			   
			 
			   
			   }
		   }
		}
	
		
		 return list;
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
		
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) 		
			fsmInstance.setDesp("Warning: Line" + fsmInstance.getRelatedASTNode().getBeginLine()+" print unknow format");
		else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
			fsmInstance.setDesp("���棺��" + fsmInstance.getRelatedASTNode().getBeginLine()+" ��δ֪��ӡ��ʽ");
		
		list.add(fsmInstance);
	}
}
