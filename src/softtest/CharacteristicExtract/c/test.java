package softtest.CharacteristicExtract.c;

import java.io.*;

import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.callgraph.c.DumpCGraphVisitor;
import softtest.cfg.c.*;
import softtest.DefUseAnalysis.c.DUAnalysisVisitor;
import softtest.ast.c.*;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.config.c.SuccessRe;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.symboltable.c.*;

import java.util.*;

public class test {
	public test(){
		
	}
	
	public static void main(String args[]) throws Exception{
		getInfo("testcase/callgraph/testCallGraph.c");
//		String name ="wangy/wangy1111";
//		CGraph g = new CGraph();
//		g.accept(new DumpCGraphVisitor(), name + ".dot");
//		System.out.println("main����ͼͼ��������ļ�" + name + ".dot");
//		try {
//			java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + name + ".jpg " + name + ".dot").waitFor();
//		} catch (IOException e1) {
//			System.out.println(e1);
//		} catch (InterruptedException e2) {
//			System.out.println(e2);
//		}
//		System.out.println("main����ͼ��ӡ�����ļ�" + name + ".jpg");
	}
	
	public static void getInfo(String parsefilename) throws Exception {
		//SuccessRe.check();
	
		System.out.println("���ɳ����﷨��...");
		CParser parser = CParser.getParser(new CCharStream(new FileInputStream(parsefilename)));
		ASTTranslationUnit root=parser.TranslationUnit();
		
		//�������ű�
		System.out.println("���ɷ��ű�...");
		ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
		root.jjtAccept(sc, null);
		OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
		root.jjtAccept(o, null);
		
		CGraph g = new CGraph();
		((AbstractScope)root.getScope()).resolveCallRelation(g);
		
		List<CVexNode> list=g.getTopologicalOrderList();
		Collections.reverse(list);
		/*System.out.println("���ù�ϵ��������");
		for(CVexNode n:list){
			System.out.print(n.getName()+"  ");
		}
		System.out.println();*/
		//dump(g,list);
		String name ="wangy/wangy";
		g.accept(new DumpCGraphVisitor(), name + ".dot");
		System.out.println("����Ҫ�������ù�ϵͼ" + name + ".dot");
		try {
			java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + name + ".jpg " + name + ".dot").waitFor();
		} catch (IOException e1) {
			System.out.println(e1);
		} catch (InterruptedException e2) {
			System.out.println(e2);
		}
		System.out.println("����Ҫ�������ù�ϵͼ" + name + ".jpg");
		
		
	
		
		//added by wangy
		//���Ժ������ù�ϵ���Ƿ�ɹ�����controlflowgraph
		for(CVexNode n:list){
			ControlFlowVisitor cfv = new ControlFlowVisitor(parsefilename);
			ControlFlowData flow = new ControlFlowData();
			SimpleNode node = n.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				//cfv.visit((ASTFunctionDefinition)node, flow);
			cfv.visit((ASTFunctionDefinition)node, flow);
			Graph graph = ((ASTFunctionDefinition) node).getGraph();
				
				
			//Graph graph = n.getGraph();
			String name_g = name + n.name;
			graph.accept(new DumpGraphVisitor(), name_g + ".dot");
			System.out.println("����ͼ��" + n.name + "�ڵ�Ŀ�����ͼ������ļ�" + name_g + ".dot");
			//System.out.println("�����Ķ�����ϢΪ��" + n.metric);
			try {
				java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + name_g + ".jpg " + name_g + ".dot").waitFor();
			} catch (IOException e1) {
				System.out.println(e1);
			} catch (InterruptedException e2) {
				System.out.println(e2);
			}
			System.out.println("����ͼ��ӡ�����ļ�" + name_g + ".jpg");
			System.out.println();
		 }
		}
		System.out.println();
		//return g;
	}
	//public static List<Graph> call(AnalysisElement element){}
}
