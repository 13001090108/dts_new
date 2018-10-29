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
public class Test_ANU {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/keilc/fault/ANU-0.1.xml";
	FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	public Test_ANU(String source,String compiletype, String result)
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
		astroot.jjtAccept(new DUAnalysisVisitor(), null);
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
////////////////	/  0   ///////////////////	
		            {
		            "void call(int a)"                                                     +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i;"                                                              +"\n"+
		            "	i = a; //DEFECT, i"                                                  +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "ANU"
		            ,
		            },
////////////////	/  1   ///////////////////	
		            {
		            "void f1()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int j, i;"                                                           +"\n"+
		            "	j = i = 3; //DEFECT, j"                                              +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "ANU"
		            ,
		            },
////////////////	/  2   ///////////////////	
		            {
		            "void f2()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i;"                                                              +"\n"+
		            "	i = 3; //FP"                                                         +"\n"+
		            "	if(i>0)"                                                             +"\n"+
		            "		return;"                                                            +"\n"+
		            "	else"                                                                +"\n"+
		            "		i++;"                                                               +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  3   ///////////////////	
		            {
		            "void f3()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int k;"                                                              +"\n"+
		            "	k = 3; //FP"                                                         +"\n"+
		            "	call(k);"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  4   ///////////////////	
		            {
		            "void f4()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i;"                                                              +"\n"+
		            "	i = 3; //FP"                                                         +"\n"+
		            "	i++;"                                                                +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },	

	/////////////////  5  chh ///////////////////	
		            {
		            " f5(int a)"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i;"                                                              +"\n"+
		            "	if((i=get())==0) return;//FP"                                            +"\n"+
		            "}	"                                                                   
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  6 chh  ///////////////////	
		            {
		            " f6(int a)"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i;"                                                              +"\n"+
		            
		            "	if((i=get())>0) return ;//FP"                                            +"\n"+
		            "}	"                                                                   
		            ,
		            "keil"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  7 chh  ///////////////////	
		            {
		            "void f2()"                                                            +"\n"+
		            "{"                                                                     +"\n"+
		            "long int i;"                                              +"\n"+        
		            "i=12345678;"                                                        +"\n"+ 
		            "}"                                                                   
		            ,
		            "keil"
		            ,
		            "ANU"
		            ,
		            },
////////////////	/  8  chh ///////////////////	
		            {
			            "f()"                                                                  +"\n"+
			            "{"                                                                    +"\n"+
			            "int a,b;"                                                             +"\n"+
			            "a=1;"                                                                 +"\n"+
			            "b=a;"                                                                 +"\n"+
			            "a=2;"                                                                 +"\n"+
			            "b=3;"                                                                 +"\n"+
			            ""                                                                     +"\n"+
			            "}"                                                                    
			            ,
			            "keil"
			            ,
			            "ANU"
			            ,
			            },
	/////////////////  9 chh   ///////////////////	
		       
		            {
			            "f(){"                                                             +"\n"+
			            "unsigned int i=10,j;"                                                            +"\n"+		           
			            "i=5;"                                                                +"\n"+
			            "}"                                                                    
			            ,
			            "keil"
			            ,
			            "ANU"
			            ,
			            },
/////////////////  10 chh   ///////////////////	
			            {
			            "void T2_viTmr(void) "                                      +"\n"+
			            "{"                                                                    +"\n"+
			            "	static unsigned char T2count='a';"                                +"\n"+
			            "} "                                                                   
			            ,
			            "all"
			            ,
			            "ANU"
			            ,
			            },
		/////////////////  11 chh   ///////////////////	
			            {
			            "void T2_viTmr(void)"                                      +"\n"+
			            "{"                                                                    +"\n"+
			            "	static unsigned char T2count='a';"                                              +"\n"+
			            "  if (TF2)"                                                           +"\n"+
			            "  {"                                                                  +"\n"+
			            "  if (T2count++ > 0x02) P3_DATA ^= 0x04; "    							+"\n"+
			            "  if (T2count > 0x04)  T2count = 0;"                                  +"\n"+
			            "    TF2 = 0;"                                                         +"\n"+
			            "  }"                                                                  +"\n"+
			            "} "                                                                   
			            ,
			            "all"
			            ,
			            "OK"
			            ,
			            },
		 });
	 }
}
