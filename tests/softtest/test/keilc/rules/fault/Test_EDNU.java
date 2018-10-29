package softtest.test.keilc.rules.fault;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import softtest.DefUseAnalysis.c.DUAnalysisVisitor;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.ParseException;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.config.c.Config;
import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.fsmanalysis.c.Report;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

@RunWith(Parameterized.class)
public class Test_EDNU {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/keilc/fault/EDNU-0.1.xml";
	FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	public Test_EDNU(String source,String compiletype, String result)
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
	
	//���ݲ�ͬ��ģʽ�������з��䵱ǰAST�������Ĳ���
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
		for(Report r:reports)
		{
			analysisResult=r.getFsmName();
			System.out.println(r.getFsmName()+" : "+r.getDesp());
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

	@After
	public void shutdown() {
		//�����ʱ�ļ���
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
	
	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {
////////////////	/  0  chh ///////////////////
		            {
		            "void fun()"                                                           +"\n"+
		            "{	"                                                                   +"\n"+
		            "	int i;//DEFECT, i"                                                              +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "EDNU"
		            ,
		            
		            },
/////////////////  1  chh ///////////////////
//(������鶨��ʱдΪtype a[5];��variable==null�����账��)
		          	{
		          	"void  fun(int b)"                                                           +"\n"+
		          	"{	"                                                                   +"\n"+
		          	"	int a[];//DEFECT, a[]"                                                           +"\n"+
		          	//"	int i=b;"                                                                    +"\n"+
		          	"}"                                                                    
		          	,
		          	"keil"
		          	,
		          	"EDNU"
		          	,
		          	},
	/////////////////  2  chh ///////////////////	
		           {
		            "int call(int i);"                                                      +"\n"+
		            "void fun()"                                                           +"\n"+
		            "{	"                                                                   +"\n"+
		            "	int i;"                                                              +"\n"+
		            "	i=3;"                                                                +"\n"+
		            "	call(i);//FP"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  3  chh ///////////////////	
		            {
		            "int  fun()"                                                           +"\n"+
		            "{	"                                                                   +"\n"+
		            "	int i,k;"                                                            +"\n"+
		            "	i=3;"                                                                +"\n"+
		            "	k=i;//FP"                                                                +"\n"+
		            "    return k;"                                                        +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },

	/////////////////  4  chh ///////////////////	
		            {
		            "int call(int j);"                                                        +"\n"+
		            "int  fun()"                                                           +"\n"+
		            "{	"                                                                   +"\n"+
		            "	int i,k=0;"                                                          +"\n"+
		            "	call(k);"                                                               +"\n"+
		            "	return k;"                                                           +"\n"+
		            "	"                                                                    +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "EDNU"
		            ,
		            },
	///////////////// 5   chh///////////////////	
	
		            {
		            "int  fun()"                                                           +"\n"+
		            "{	"                                                                   +"\n"+
		            "	int i=0,j;"                                                          +"\n"+
		            "	for(j=0;j<5;i++,j++)"                                                    +"\n"+
		            "	return i;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  6  chh ///////////////////	
		            {
		            " int f(int a)"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i=5;"                                                                    +"\n"+
		            "	return i*i;"                                                                      +"\n"+
		            "}"
		            ,
		            "keil"
		            ,
		            "EDNU"
		            ,
		            },
///////////////// 7   chh///////////////////	
		            {
		            "void  fun()"                                                           +"\n"+
		            "{	"                                                                   +"\n"+
		            "	int i=0;"                                                            +"\n"+
		            "	int f();"                                                          +"\n"+
		            "	 //i++;"                                                          +"\n"+
		            "}"                                                                   
		            ,
		            "keil"
		            ,
		            "EDNU"
		            ,
		            },
		 });
	 }
}
		