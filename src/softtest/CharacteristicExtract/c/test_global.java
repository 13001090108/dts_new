package softtest.CharacteristicExtract.c;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import softtest.rules.c.StateMachineUtils;

//把所有的变量加到一个list中，然后当出现使用的是，去看看在不在list，不在就说明是全局的
public class test_global {
	
	private List<String> list = new ArrayList<String>();
	
	public static void main(String[] args) throws Exception{
		Graph_Info h = new Graph_Info();
		StatementFeature sf = new StatementFeature();
		String filePath = "C:/Users/Miss_Lizi/Desktop/global.c";
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
		
		test_global tg = new test_global();
		//System.out.println(sf.getSelfStatementsFeature(filePath, "switchtest", 48, 57));
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		//System.out.println(list_cvex.size());
		//ASTTranslationUnit aa = new ASTTranslationUnit();
		
	
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			List<String> a = new ArrayList<String>();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				String Xpath = ".//DirectDeclarator";
				List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
				evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
				for(SimpleNode s : evaluationResults){
					tg.list.add(s.getImage());
				}
			}
			
		}
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				String Xpath1 = ".//PrimaryExpression";
				List<SimpleNode> evaluationResults1 = new LinkedList<SimpleNode>();
				evaluationResults1 = StateMachineUtils.getEvaluationResults(node, Xpath1);
				for(SimpleNode s : evaluationResults1){
					String str = s.getImage();
					if(!tg.list.contains(str) && str != ""){
						System.out.println(str + "是全局变量");
					}
				}
			}
			
		}
		for(String str : tg.list){
			System.out.println(str);	
		}
	}
}
