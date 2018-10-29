package softtest.rules.gcc.rule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.CType_BaseType;



/** 
 * @author MaoJinyu
 * ѭ��������ȫ������
 * GlobleDeclarationLoopVariable
 */

public class GDLVStateMachine {

	public static List<FSMMachineInstance> createGDLVStateMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = null;
		
		String xPath=".//IterationStatement[@Image='for']";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		Iterator itr = evaluationResults.iterator();
		
		while(itr.hasNext()){
			ASTIterationStatement iter= (ASTIterationStatement)itr.next();
			int i=iter.jjtGetNumChildren();
			switch (i)
			{
			case 1: //for���������ʽ��ʡ��
				break;
			case 2://forʡ���������ʽ
				break;
									 
			case 3://forʡ��һ�����ʽ
				String relationCase3xPath = "./Expression//RelationalExpression|./Expression//EqualityExpression";
				List<SimpleNode> case3Result = null;
				case3Result = StateMachineUtils.getEvaluationResults(iter, relationCase3xPath);
				
				
				if (case3Result.isEmpty())
					//ʡ�Ե��ǵڶ����жϱ��ʽ
					break;
				
				Iterator case3itr = case3Result.iterator();
				 while(case3itr.hasNext()){
						SimpleNode exp= (SimpleNode)case3itr.next();
						List<Node> case3conList = exp.findChildrenOfType(ASTPrimaryExpression.class);
						if (case3conList.isEmpty())
							continue;
						
						//ʡ�Ե��ǵ������ʼ�����ʽ
						if(!exp.isSelOrAncestor((SimpleNode)iter.jjtGetChild(0)))
							break;
						
						//ʡ�Ե��ǵ�һ���ʼ�����ʽ
						List<Node> case3incList = ((SimpleNode)iter.jjtGetChild(1)).findChildrenOfType(ASTPrimaryExpression.class);
						if(case3incList.isEmpty())																		
							continue;	
						else{
							List<Node> idInBoth = findLoopIdList(case3conList,case3incList);
													
							if(idInBoth.isEmpty())									
								continue;	
							
							for(Node snode :idInBoth){
								
								ASTPrimaryExpression conId = (ASTPrimaryExpression)snode;
								VariableNameDeclaration var = conId.getVariableDecl();
								if(var != null 
										&& var.getScope() instanceof SourceFileScope )
								{
									addFSM(list,iter,var,fsm);
									break;
									}													
								}
							}	
				 }
						
				 break;
			
			case 4 ://for���������ʽ��δʡ�� 
					ASTExpression conditionExp = (ASTExpression)iter.jjtGetChild(1);
					ASTExpression incrementExp = (ASTExpression)iter.jjtGetChild(2);
					List<Node> case4conList = conditionExp.findChildrenOfType(ASTPrimaryExpression.class);
					
					/*�������ʽ����id_expression*/
					if (case4conList.isEmpty())
						break;
					
					List<Node> case4incList = incrementExp.findChildrenOfType(ASTPrimaryExpression.class);
					if(case4incList.isEmpty())						
						break;
										
					/*�������ʽ����id_expression��ѡȡ��ͬ��id*/
					List<Node> idInBoth = findLoopIdList(case4conList,case4incList);
					
					if(idInBoth.isEmpty())
						break;	
					
					/*����ͬ��id*/
					for(Node snode :idInBoth){
						
						ASTPrimaryExpression conId = (ASTPrimaryExpression)snode;
						VariableNameDeclaration var = conId.getVariableDecl();
						if(var != null 
								&& var.getScope() instanceof SourceFileScope )
						{
							addFSM(list,iter,var,fsm);
							break;
							}													
						}
					break;
			
			default:
					break;
			
			}
		}
		
			
		
		return list;
			
	
  }
	
	private static List<Node> findLoopIdList(List<Node> conList, List<Node> incList){
		List<Node> idInBoth = new ArrayList<Node>();
		for(Node snode : conList){
			ASTPrimaryExpression conId = (ASTPrimaryExpression)snode;
		
			if(conId.getType() instanceof CType_BaseType){
				{
			
					for(Node mnode :incList){
						
						if (conId.getImage().equals(((SimpleNode)mnode).getImage()))
							idInBoth.add(conId);										
					}
				}
			}
		}
		return idInBoth;
	}
	

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node,  VariableNameDeclaration var,FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		fsminstance.setRelatedVariable(var);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp("GlobleDeclarationLoopVariable: Loop variable should be ristrict in minimum scope, that means the scope of Loop variable should be at least,so the variable loop should be local.");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("ѭ��������ȫ������: ѭ������Ӧ�ö�������С�ķ�Χ�ڣ���ѭ��������������Ӧ��С������ѭ�����������Ǿֲ������ġ�");
			}	
		
		list.add(fsminstance);
	}
}

