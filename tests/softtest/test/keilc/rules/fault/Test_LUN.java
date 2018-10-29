package softtest.test.keilc.rules.fault;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.ParseException;

import softtest.cfg.c.ControlFlowVisitor;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.fsmanalysis.c.Report;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

@RunWith(Parameterized.class)
public class Test_LUN {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/keilc/fault/LUN-0.1.xml";
	FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	public Test_LUN(String source,String compiletype, String result)
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
			analysisResult=r.getFsmName();
			System.out.println("\t"+r.getFsmName()+" : "+r.getDesp());
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
		            "int func() {"                                                         +"\n"+
		            "    int i, j;"                                                        +"\n"+
		            "    i = intLock();"                                                   +"\n"+
		            "    intUnlock(i);"                                                    +"\n"+
		            "    j = intLock();"                                                   +"\n"+
		            "    intUnlock(i);"                                                    +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            ""                                                                     +"\n"+
		            "int func2() {"                                                        +"\n"+
		            "    intUnlock(1);"                                                    +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "int func3() {"                                                        +"\n"+
		            "    int i = intLock();"                                               +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "LUN"
		            ,
		            },
//			          ����Ϊliuli���
////////////////	/  1   ///////////////////			        
		            {                                                                 
			        "int func() {"                                                         +"\n"+
			        "    int j;"                                                        +"\n"+
			        "    int i = intLock();"                                                   +"\n"+
			        "    intUnlock(i++);"                                                    +"\n"+                                                
			        "}"  
		            ,
			        "keil"
		            ,
		            "LUN"
		            ,
		            },
////////////////	/  2   ///////////////////			        
		            {                                                                 
			        "int func() {"                                                         +"\n"+
			        "    int j;"                                                        +"\n"+
			        "    int i = intLock();"                                                   +"\n"+
			        "    --i;"                                                    +"\n"+                                                
			        "    intUnlock(i);"                                                    +"\n"+                                                
			        "}"  
		            ,
			        "keil"
		            ,
		            "LUN"
		            ,
		            },
////////////////	/  3   ///////////////////			        
		            {                                                                 
			        "int func() {"                                                         +"\n"+
			        "    int j;"                                                        +"\n"+
			        "    int i = intLock();"                                                   +"\n"+
			        "    i += 3;"                                                    +"\n"+                                                
			        "    intUnlock(i);"                                                    +"\n"+                                                
			        "}"  
		            ,
			        "keil"
		            ,
		            "LUN"
		            ,
		            },
////////////////	/  4   ///////////////////			        
		            {                                                                 
			        "int func() {"                                                         +"\n"+
			        "    int j ,i=5;"                                                        +"\n"+
			        "    i = intLock();"                                                   +"\n"+
			        "    j = 3 ;"                                                    +"\n"+                                                
			        "    i = j ;"                                                    +"\n"+                                                
			        "    intUnlock(i);"                                                    +"\n"+                                                
			        "}"  
		            ,
			        "keil"
		            ,
		            "LUN"
		            ,
		            },
////////////////	/  5   ///////////////////			        
		            {                                                                 
			        "int func() {"                                                         +"\n"+
			        "    int j = 3;"                                                        +"\n"+
			        "    int i = intLock();"                                                   +"\n"+
			        "    i = j+1;"                                                    +"\n"+                                                
			        "    intUnlock(i);"                                                    +"\n"+                                                
			        "}"  
		            ,
			        "keil"
		            ,
		            "LUN"
		            ,
		            },	
////////////////	/  6   ///////////////////			        
		            {                                                                 
			        "int func() {"                                                         +"\n"+
			        "    int j ;"                                                        +"\n"+
			        "    int i = intLock();"                                                   +"\n"+
			        "    i = j = 4;"                                                    +"\n"+                                                
			        "    intUnlock(i);"                                                    +"\n"+                                                
			        "}"  
		            ,
			        "keil"
		            ,
		            "LUN"
		            ,
		            },
		            
////////////////	/  7   ///////////////////	
		            {
		            "int func() {                                                         "+"\n"+
		            " int i, j,b;                                                      "     +"\n"+
		            "j = intLock();                                                  "     +"\n"+
		            "i = intLock();"                                                        +"\n"+
		            "      b =3 ;                                       "                         +"\n"+
		            " intUnlock(i); "                                                     +"\n"+
		          //  "i = j ;                                                 "          +"\n"+
		            "intUnlock(j);                                                    "    +"\n"+
		            "}"                                              
		            ,
		            "keil"
		            ,
		            "OK"//Ƕ�׵�LUN�����ܴ�����ȷ��LUN�Ƿ���Ƕ��ʹ��
		            ,
		            },		          		        
////////////////	/ 8   ///////////////////			        
		            {                                                                 
			        "int func() {"                                                         +"\n"+
			        "    int j ;"                                                        +"\n"+
			        "    int i = intLock();"                                                   +"\n"+
			        "    j = i;"                                                    +"\n"+                                                
			        "    intUnlock(j);"                                                    +"\n"+                                                
			        "}"  
		            ,
			        "keil"
		            ,
		            "LUN"//Ϊ���Ͻ������intLock��intUnlock�Ĳ�������һ��
		            ,
		            },
		        
////////////////	/  9   ///////////////////	
		            {
		            "int func() {                                                         "+"\n"+
		            " int i, j,b;                                                      "     +"\n"+
		            "j = intLock();                                                  "     +"\n"+
		            "      b =3 ;                                       "                         +"\n"+
		            "intUnlock(j); "                                                        +"\n"+

		            "i = intLock(); "                                                     +"\n"+
		          //  "i = j ;                                                 "          +"\n"+
		            "      b =3 ;                                       "                         +"\n"+
		            "  intUnlock(i);                                                 "    +"\n"+
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
