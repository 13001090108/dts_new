package softtest.test.keilc.rules.fault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.ParseException;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.fsmanalysis.c.Report;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;
/**
 * �Ƿ��������Կ��   ���������������ƽ��
 * @author zys	
 * 2010-5-10
 */
@RunWith(Parameterized.class)
public class Test_ILC {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/keilc/fault/ILC-0.1.xml";
	private FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	public Test_ILC(String source,String compiletype, String result)
	{
		this.source=source;	
		this.compiletype=compiletype;
		this.result = result;
	}
	
	@BeforeClass
	public static void setUpBase()
	{
//		BasicConfigurator.configure();
//        PropertyConfigurator.configure("log4j.properties") ;
		//���ݴ�����ģʽXML�ļ�·����ʼ���Զ����б�
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		
		//ZYS:��ø���״̬�������ļ�XML�е�����ֶζ�ȡ����ģʽ�����ڵĹ������
		fsm.setType("fault");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
		
		Config.REGRESS_RULE_TEST=true;
	}
	
	private void analysis(ASTTranslationUnit astroot){
		astroot.jjtAccept(new ScopeAndDeclarationFinder(), null);
		astroot.jjtAccept(new OccurrenceAndExpressionTypeFinder(), null);
		astroot.jjtAccept(new ControlFlowVisitor(), null);
		astroot.jjtAccept(new ControlFlowDomainVisitor(), null);
		astroot.jjtAccept(fsmAnalysis, cfData);
		
		assertEquals(result,getFSMAnalysisResult());
	}
	
	private String getFSMAnalysisResult()
	{
		List<Report> reports=cfData.getReports();
		String analysisResult=null;
		if(reports.size()==0)
		{
			analysisResult="OK";
			return analysisResult;
		}
		System.out.println("���μ�⹲������"+reports.size()+"�����ϵ�");
		for(Report r:reports)
		{
			System.out.println("\t"+r.getFsmName()+" : "+r.getDesp());
			analysisResult=r.getFsmName();
		}
		
		return analysisResult;
	}
	
	@Before
	public void init() {
		cfData = new FSMControlFlowData();
		List<Report> reports = new ArrayList<Report>();
		cfData.setReports(reports);
		fsmAnalysis=new FSMAnalysisVisitor(cfData); 
	}
	
	@Test
	public void test() {
		CParser.setType("gcc");
		CParser parser_gcc = CParser.getParser(new CCharStream(new ByteArrayInputStream(source.getBytes())));
		CParser.setType("keil");
		CParser parser_keil = CParser.getParser(new CCharStream(new ByteArrayInputStream(source.getBytes())));
		ASTTranslationUnit gcc_astroot=null,keil_astroot=null;
		if(compiletype.equals("gcc")){
			CParser.setType("gcc");
			try {
				gcc_astroot = parser_gcc.TranslationUnit();
			} catch (ParseException e) {
				e.printStackTrace();
				fail("parse error");
			}
			analysis(gcc_astroot); 
		}else if(compiletype.equals("keil")){
			CParser.setType("keil");
			try {
				keil_astroot= parser_keil.TranslationUnit();
			} catch (ParseException e) {
				e.printStackTrace();
				fail("parse error");
			}
			analysis(keil_astroot);
		}else{
			CParser.setType("gcc");
			try {
				gcc_astroot = parser_gcc.TranslationUnit();
			} catch (ParseException e) {
				e.printStackTrace();
				fail("parse error");
			}
			analysis(gcc_astroot);
			
			CParser.setType("keil");
			try {
				keil_astroot= parser_keil.TranslationUnit();
			} catch (ParseException e) {
				e.printStackTrace();
				fail("parse error");
			}
			analysis(keil_astroot);
		}
	}
	
	/**  
	#      * ׼�����ݡ����ݵ�׼����Ҫ��һ�������н��У��÷�����Ҫ����һ����Ҫ��  
	#   
	#          1���÷���������Parametersע������  
	#          2���÷�������Ϊpublic static��  
	#          3���÷������뷵��Collection����  
	#          4���÷��������ֲ���Ҫ��  
	#          5���÷���û�в���  
	#      * @return  
	#      */  
	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {
				 /////////////////  0   ///////////////////	
		            {
		            "f(){"                                                                 +"\n"+
		            "	int i,j;"                                                            +"\n"+
		            "	i=0;"                                                                +"\n"+
		            "	j/=i;"                                                               +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "ILC"
		            ,
		            },
		        	/////////////////  1   ///////////////////	
		            {
		            "f(){"                                                                 +"\n"+
		            "	int i,j;"                                                            +"\n"+
		            "	i=0;"                                                                +"\n"+
		            "	j=j/i;"                                                               +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "ILC"
		            ,
		            },			 
	/////////////////  2   ///////////////////	
		            {
		            "f(){"                                                                 +"\n"+
		            "	int i,j;"                                                            +"\n"+
		            "	i=j;"                                                                +"\n"+
		            "	j/=i-j;"                                                             +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "ILC"
		            ,
		            },
	/////////////////  3   ///////////////////	
		            {
		            "f(){"                                                                 +"\n"+
		            "	int i,j;"                                                            +"\n"+
		            "	i=j;"                                                                +"\n"+
		            "	j=j/(i-j);"                                                             +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "ILC"
		            ,
		            },
		        	/////////////////  4   ///////////////////	
		            {
		            "f(){"                                                                 +"\n"+
		            "	int i,j;"                                                            +"\n"+
		            "	i=j;"                                                                +"\n"+
		            "	j=j/(i-j+1);"                                                             +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  5   ///////////////////	
		            {
		            ""                                                                     +"\n"+
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <reg51.h>"                                                   +"\n"+
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "#define ZERO 0"                                                       +"\n"+
		            ""                                                                     +"\n"+
		            "int test1 _at_ 0x40;"                                                 +"\n"+
		            "int test2 _at_ 0x46;"                                                 +"\n"+
		            ""                                                                     +"\n"+
		            "void main( void )"                                                    +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i = 0;"                                                          +"\n"+
		            ""                                                                     +"\n"+
		            "	SCON=0x52;			   //���пڿ�����"                              +"\n"+
		            "	TMOD=0x20;			   //12Mʱ��ʱ������Ϊ2400"                         +"\n"+
		            "	TCON = 0x69;		   // ��ʱ��/���������ƼĴ���"                    +"\n"+
		            "	TH1=0xf3;			   //"                                          +"\n"+
		            "	EX0 = 1;"                                                            +"\n"+
		            "	EA = 1;	"                                                            +"\n"+
		            "	"                                                                    +"\n"+
		            "	test1 = 20;"                                                         +"\n"+
		            "	test2 = 20;"                                                         +"\n"+
		            ""                                                                     +"\n"+
		            "	test2 = test1 - test2; "                                             +"\n"+
		            "	//test1 = test1 / (test1 - test2) ;"                                 +"\n"+
		            "	test1 /= test2;"                                                     +"\n"+
		            ""                                                                     +"\n"+
		            "	while (1);"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },

////////////////	/  6  ///////////////////	
		            {
		            ""                                                                     +"\n"+
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <reg51.h>"                                                   +"\n"+
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "#define ZERO 0"                                                       +"\n"+
		            ""                                                                     +"\n"+
		            "int test1 _at_ 0x40;"                                                 +"\n"+
		            "int test2 _at_ 0x46;"                                                 +"\n"+
		            ""                                                                     +"\n"+
		            "void main( void )"                                                    +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i = 0;"                                                          +"\n"+
		            ""                                                                     +"\n"+
		            "	SCON=0x52;			   //���пڿ�����"                              +"\n"+
		            "	TMOD=0x20;			   //12Mʱ��ʱ������Ϊ2400"                      +"\n"+
		            "	TCON = 0x69;		   // ��ʱ��/���������ƼĴ���"                   +"\n"+
		            "	TH1=0xf3;			   //"                                           +"n"+
		            "	EX0 = 1;"                                                            +"\n"+
		            "	EA = 1;	"                                                            +"\n"+
		            "	"                                                                    +"\n"+
		            "	test1 = 20;"                                                         +"\n"+
		            "	test2 = 20;"                                                         +"\n"+
		            ""                                                                     +"\n"+
		            "	//test2 = test1 - test2; "                                           +"\n"+
		            "	test1 = test1 / (test1 - test2) ;"                                   +"\n"+
		            "	//test1 /= test2;"                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "	while (1);"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },
		 });
	 }
}
