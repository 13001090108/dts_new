package softtest.domain.c.analysis;

import java.io.FileInputStream;

import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.config.c.Config;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

public class TestIntervalAnalysis {
	public static void main(String[] args) throws Exception{
		String parsefilename="testcase/intervalanalysis/TestIntervalAnalysis.c";
		Config.TRACE=true;
		//���������﷨��
		System.out.println("���ɳ����﷨��...");
		CParser parser = CParser.getParser(new CCharStream(new FileInputStream(parsefilename)));
		ASTTranslationUnit root=parser.TranslationUnit();
		
		//�������ű�
		System.out.println("���ɷ��ű�...");
		ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
		root.jjtAccept(sc, null);
		OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
		root.jjtAccept(o, null);
		
		//����������ͼ
		System.out.println("���ɿ�����ͼ...");
		root.jjtAccept(new ControlFlowVisitor(), null);
		
		//�������
		root.jjtAccept(new ControlFlowDomainVisitor(), null);
		
		System.out.println("�������.");
	}

}
