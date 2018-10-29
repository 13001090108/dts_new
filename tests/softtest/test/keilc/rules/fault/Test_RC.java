package softtest.test.keilc.rules.fault;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
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
import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.fsmanalysis.c.Report;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;
 /**  
  * ���������Ե��������Parameterized��������������  
  * ���⣺Ŀǰ�Ļع����û�н���Ԥ��������
  *  @author zys
  */ 
@RunWith(Parameterized.class)
public class Test_RC{
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/keilc/fault/RC-0.1.xml";
	private FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	public Test_RC(String source,String compiletype, String result)
	{
		//System.out.println("���๹�캯����SubClass()!");
		this.source=source;	
		this.compiletype=compiletype;
		this.result = result;
	}

	@AfterClass
	public static void tearDownBase() {
		//System.out.println("@AfterClass in ���� SubClass!");
	}
	@BeforeClass
	public static void setUpBase()
	{
		//System.out.println("@BeforeClass in ���� SubClass!");
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
		//System.out.println("@Before in ���� SubClass!");
		cfData = new FSMControlFlowData();
		List<Report> reports = new ArrayList<Report>();
		cfData.setReports(reports);
		fsmAnalysis=new FSMAnalysisVisitor(cfData); 
	}

	@After
	public void shutdown() {
		//�����ʱ�ļ���
		//System.out.println("@After in ���� SubClass!");
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
		            "int g(){}"                                                            +"\n"+
		            "int f() interrupt 1 using 1"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "int i=a+3;"                                                           +"\n"+
		            ""                                                                     +"\n"+
		            "g();"                                                                 +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "RC"
		            ,
		            },
    /////////////////  1   ///////////////////	
		            {
		            "int g(){}"                                                            +"\n"+
		            "int f() interrupt 1 using 50"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "int i=a+3;"                                                           +"\n"+
		            "#pragma NOAREGS"                                                      +"\n"+
		            "g();"                                                                 +"\n"+
		            "#pragma AREGS"                                                                        +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  2   ///////////////////	
		            {
		            "int g(){}"                                                            +"\n"+
		            "int f() interrupt 1 using 0"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "int i=a+3;"                                                           +"\n"+
		            "g();"                                                                 +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  3   ///////////////////	
		            {
		            "int g(){}"                                                            +"\n"+
		            "int f() interrupt 1 using 0"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "int i=a+3;"                                                           +"\n"+
		            "#pragma NOAREGS"                                                      +"\n"+
		            "g();"                                                                 +"\n"+
		            "#pragma AREGS"                                                        +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
		        	/////////////////  4   ///////////////////	
		            {
		            "int g(){}"                                                            +"\n"+
		            "int f()"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "int i=a+3;"                                                           +"\n"+
		            "g();"                                                                 +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },


				 
				 
		 });
	 }
}
