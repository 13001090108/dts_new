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
			//���������﷨��
			System.out.println("���ɳ����﷨��...");
			CParser parser=CParser.getParser(new CCharStream(new FileInputStream(path)));
			ASTTranslationUnit root=parser.TranslationUnit();
			//astmap.put(srcFile, root);//���﷨�����ڴ��ͨ���ļ�������
			
			//�������ű�
			System.out.println("���ɷ��ű�...");
			ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
			root.jjtAccept(sc, null);
			OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
			root.jjtAccept(o, null);
			
//			//����ȫ�ֺ������ù�ϵ
//			System.out.println("����ȫ�ֺ������ù�ϵ...");
//			root.jjtAccept(new InterMethodVisitor(), path);
//			
			//�ļ��ں������ù�ϵͼ
			System.out.println("�����ļ��ں������ù�ϵ...");
			CGraph g = new CGraph();
			((AbstractScope)(root.getScope())).resolveCallRelation(g);
			List<CVexNode> list = g.getTopologicalOrderList();
			Collections.reverse(list);
			//cgMap.put(srcFile, g);
			
			//���ɿ�����ͼ
			System.out.println("���ɿ�����ͼ...");
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
			
			//���ɶ���ʹ����
			System.out.println("���ɶ���ʹ����...");
			root.jjtAccept(new DUAnalysisVisitor(), null);
			
			//��������
			System.out.println("��������...");
			ControlFlowDomainVisitor cfd = new ControlFlowDomainVisitor();
			for (CVexNode cvnode : list) {
				SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
				if (node instanceof ASTFunctionDefinition) {
					cfd.visit((ASTFunctionDefinition)node, null);
				} 
			}
			
			//����SCVP
			System.out.println("����scvp...");
			root.jjtAccept(new SCVPVisitor(), null);
			
			System.out.println("OK.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void preanalysis(String path)  throws Exception{
		//String temp=path.getFileName();
		//���������﷨��
		System.out.println("���ɳ����﷨��...");
		CParser parser = CParser.getParser(new CCharStream(new FileInputStream(path)));
		ASTTranslationUnit root=parser.TranslationUnit();
		//path.put(srcFile, root);//���﷨�����ڴ��ͨ���ļ�������
		
		//�������ű�
		System.out.println("���ɷ��ű�...");
		ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
		root.jjtAccept(sc, null);
		OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
		root.jjtAccept(o, null);
		
		//����ȫ�ֺ������ù�ϵ
		System.out.println("����ȫ�ֺ������ù�ϵ...");
		root.jjtAccept(new InterMethodVisitor(), path);
		
		//�ļ��ں������ù�ϵͼ
		System.out.println("�����ļ��ں������ù�ϵ...");
		CGraph g = new CGraph();
		((AbstractScope)(root.getScope())).resolveCallRelation(g);
		List<CVexNode> list = g.getTopologicalOrderList();
		Collections.reverse(list);
		
		
		//���ɿ�����ͼ
		System.out.println("���ɿ�����ͼ...");
		ControlFlowVisitor cfv = new ControlFlowVisitor(path);
		ControlFlowData flow = new ControlFlowData();
		for (CVexNode cvnode : list) {
			SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition) {
				cfv.visit((ASTFunctionDefinition)node, flow);
				
			} 
		}
		
		//���ɶ���ʹ����
		System.out.println("���ɶ���ʹ����...");
		root.jjtAccept(new DUAnalysisVisitor(), null);
		
		//��������
		System.out.println("��������...");
		ControlFlowDomainVisitor cfd = new ControlFlowDomainVisitor();
		for (CVexNode cvnode : list) {
			SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition) {
				cfd.visit((ASTFunctionDefinition)node, null);
			} 
		}
		
		//����SCVP
		System.out.println("����scvp...");
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
