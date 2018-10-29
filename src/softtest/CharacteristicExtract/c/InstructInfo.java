package softtest.CharacteristicExtract.c;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import softtest.DefUseAnalysis.c.DUAnalysisVisitor;
import softtest.SDataBase.c.DataBaseAccess;
import softtest.SDataBase.c.FileInformatica;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.callgraph.c.DumpCGraphVisitor;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.DumpGraphVisitor;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import softtest.database.c.DBAccess;
import softtest.depchain.c.DepChain;
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.pretreatment.Pretreatment;
import softtest.scvp.c.SCVPString;
import softtest.scvp.c.SCVPVisitor;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

public class InstructInfo {
	
	
	
	public static void main(String[] args) throws Exception{
		InstructInfo in = new InstructInfo();
		String path = "C:/Users/Miss_Lizi/Desktop/depchain/depchain.c";	
		
		try {
			//产生抽象语法树
			System.out.println("生成抽象语法树...");
			CParser parser=CParser.getParser(new CCharStream(new FileInputStream(path)));
			ASTTranslationUnit root=parser.TranslationUnit();
			//astmap.put(srcFile, root);//把语法树扔内存里，通过文件名检索
			
			//产生符号表
			System.out.println("生成符号表...");
			ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
			root.jjtAccept(sc, null);
			OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
			root.jjtAccept(o, null);
			
//			//生成全局函数调用关系
//			System.out.println("生成全局函数调用关系...");
//			root.jjtAccept(new InterMethodVisitor(), path);
//			
			//文件内函数调用关系图
			System.out.println("生成文件内函数调用关系...");
			CGraph g = new CGraph();
			((AbstractScope)(root.getScope())).resolveCallRelation(g);
			List<CVexNode> list = g.getTopologicalOrderList();
			Collections.reverse(list);
			//cgMap.put(srcFile, g);
			
			//生成控制流图
			System.out.println("生成控制流图...");
			ControlFlowVisitor cfv = new ControlFlowVisitor(path);
			ControlFlowData flow = new ControlFlowData();
			for (CVexNode cvnode : list) {
				SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
				if (node instanceof ASTFunctionDefinition) {
					cfv.visit((ASTFunctionDefinition)node, flow);
					Graph graph = ((ASTFunctionDefinition)node).getGraph();
					in.getInstructinfo(graph);
				} 
			}
			
			//生成定义使用链
			System.out.println("生成定义使用链...");
			root.jjtAccept(new DUAnalysisVisitor(), null);
			
			//区间运算
			System.out.println("区间运算...");
			ControlFlowDomainVisitor cfd = new ControlFlowDomainVisitor();
			for (CVexNode cvnode : list) {
				SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
				if (node instanceof ASTFunctionDefinition) {
					cfd.visit((ASTFunctionDefinition)node, null);
				} 
			}
			
			//计算SCVP
			System.out.println("计算scvp...");
			root.jjtAccept(new SCVPVisitor(), null);
			
			System.out.println("OK.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void preanalysis(String path)  throws Exception{
		//String temp=path.getFileName();
		//产生抽象语法树
		System.out.println("生成抽象语法树...");
		CParser parser = CParser.getParser(new CCharStream(new FileInputStream(path)));
		ASTTranslationUnit root=parser.TranslationUnit();
		//path.put(srcFile, root);//把语法树扔内存里，通过文件名检索
		
		//产生符号表
		System.out.println("生成符号表...");
		ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
		root.jjtAccept(sc, null);
		OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
		root.jjtAccept(o, null);
		
		//生成全局函数调用关系
		System.out.println("生成全局函数调用关系...");
		root.jjtAccept(new InterMethodVisitor(), path);
		
		//文件内函数调用关系图
		System.out.println("生成文件内函数调用关系...");
		CGraph g = new CGraph();
		((AbstractScope)(root.getScope())).resolveCallRelation(g);
		List<CVexNode> list = g.getTopologicalOrderList();
		Collections.reverse(list);
		
		
		//生成控制流图
		System.out.println("生成控制流图...");
		ControlFlowVisitor cfv = new ControlFlowVisitor(path);
		ControlFlowData flow = new ControlFlowData();
		for (CVexNode cvnode : list) {
			SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition) {
				cfv.visit((ASTFunctionDefinition)node, flow);
				
			} 
		}
		
		//生成定义使用链
		System.out.println("生成定义使用链...");
		root.jjtAccept(new DUAnalysisVisitor(), null);
		
		//区间运算
		System.out.println("区间运算...");
		ControlFlowDomainVisitor cfd = new ControlFlowDomainVisitor();
		for (CVexNode cvnode : list) {
			SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition) {
				cfd.visit((ASTFunctionDefinition)node, null);
			} 
		}
		
		//计算SCVP
		System.out.println("计算scvp...");
		root.jjtAccept(new SCVPVisitor(), null);
		
		System.out.println("OK.");
	}
	
	public List<SCVPString> getInstructinfo(Graph g){
		List<VexNode> nlist = g.getAllnodes();
		List<SCVPString> list = new ArrayList<SCVPString>();
		for(VexNode node : nlist){
			List<NameOccurrence> occs = node.getOccurrences();
			for(NameOccurrence occ : occs){
				if(occ.getSCVP() != null){
					list.add(occ.getSCVP());
					System.out.println(occ.getSCVP());
				}
			}
		}
		return list;
	}
}
