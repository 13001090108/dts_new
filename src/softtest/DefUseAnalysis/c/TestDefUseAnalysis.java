package softtest.DefUseAnalysis.c;

import java.io.*;

import softtest.ast.c.*;
import softtest.symboltable.c.*;
import softtest.cfg.c.*;
import softtest.config.c.Config;

public class TestDefUseAnalysis {
	public static void main(String args[])throws Exception{
		String parsefilename="testcase/defuseanalysis/test3.c";
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
		
		//������ʹ��
		System.out.println("���ɶ���ʹ����...");
		Config.TRACE=true;
		root.jjtAccept(new DUAnalysisVisitor() , null);
		
		System.out.println("�������.");
	}
}
