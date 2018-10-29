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

@RunWith(Parameterized.class)
public class Test_URB {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/keilc/fault/URB-0.1.xml";
	private FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	public Test_URB(String source,String compiletype, String result)
	{
		this.source=source;	
		this.compiletype=compiletype;
		this.result = result;
	}
	
	@BeforeClass
	public static void setUpBase()
	{
		BasicConfigurator.configure();
        PropertyConfigurator.configure("log4j.properties") ;
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
		            "int i,j;"                                                             +"\n"+
		            "i=j+1;"                                                               +"\n"+
		            "if(i<j)"                                                              +"\n"+
		            "	i++;"                                                                +"\n"+
		            "else"                                                                 +"\n"+
		            "	return i;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "URB"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "f(int i){"                                                                 +"\n"+
		            "int j=5;"                                                             +"\n"+
		            "if(i<j)"                                                              +"\n"+
		            "	i++;"                                                                +"\n"+
		            "else"                                                                 +"\n"+
		            "	return i;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  2   ///////////////////	
		            {
		            "f(){"                                                                 +"\n"+
		            "int flag=1;"                                                          +"\n"+
		            "if(flag){"                                                            +"\n"+
		            "char c=125;"                                                          +"\n"+
		            "}else{"                                                               +"\n"+
		            "char c=256;"                                                          +"\n"+
		            "}"                                                                    +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "URB"
		            ,
		            },
	/////////////////  3   ///////////////////	
		            {
		            "f(){"                                                                 +"\n"+
		            "int i=1;"                                                             +"\n"+
		            "if(i!=1){"                                                            +"\n"+
		            "	i++;"                                                                +"\n"+
		            "	i=4;"                                                                +"\n"+
		            "}"                                                                    +"\n"+
		            "else"                                                                 +"\n"+
		            "	i=5;"                                                                +"\n"+
		            "return i;"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "URB"
		            ,
		            },

				 
				 
				 
		 });
	 }
}
