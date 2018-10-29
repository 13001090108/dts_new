package softtest.test.keilc.rules.fault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.ParseException;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.fsmanalysis.c.Report;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterMethodVisitor;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

@RunWith(Parameterized.class)
public class Test_OOB {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/keilc/fault/OOB-0.1.xml";
	FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	public Test_OOB(String source,String compiletype, String result)
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
		
		//���ԭ��ȫ�ַ����в����ĺ���ժҪ��Ϣ
		InterCallGraph.getInstance().clear();
		astroot.jjtAccept(new InterMethodVisitor(), null);
		
		CGraph g = new CGraph();
		((AbstractScope)(astroot.getScope())).resolveCallRelation(g);
		List<CVexNode> list = g.getTopologicalOrderList();
		Collections.reverse(list);
		
		ControlFlowData flow = new ControlFlowData();
		ControlFlowVisitor cfv = new ControlFlowVisitor();
		ControlFlowDomainVisitor cfd = new ControlFlowDomainVisitor();
		
		for (CVexNode cvnode : list) {
			SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition) {
				cfv.visit((ASTFunctionDefinition)node, flow);
				cfd.visit((ASTFunctionDefinition)node, null);
			} 
		}
		
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
////////////////////  0   ///////////////////	
		            {
		            "int f()"                                                              +"\n"+
		            "{"                                                                    +"\n"+
		            "  static   char buf[10] = \"\";"                                        +"\n"+
		            "  buf[9] = 'A';   //FP,OOB"                                           +"\n"+
		            "  return 0;"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },
////////////////////  1   ///////////////////	
		            {
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "  int a[5] = {1,2,3,4,2};"                                            +"\n"+
		            "  a[4]=1,a[6] = 1;  //DEFECT,OOB,a"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },  
////////////////////  2   ///////////////////	
		            {
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "  int a[] = {1,2,3,4,2};"                                             +"\n"+
		            "  a[5] = 1;  //DEFECT,OOB,a"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////////  3   ///////////////////	
		            {
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "  int a[] = {1,2,3,4,2};"                                             +"\n"+
		            "  a[4] = 1;  //FP,OOB"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },		            
////////////////	/  4   ///////////////////	
		            {
		            "int c[5];"                                                            +"\n"+
		            "void f () {"                                                          +"\n"+
		            "  int b[5];"                                                          +"\n"+
		            "  b[4]; //FP,OOB"                                               +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  5   ///////////////////	
		            {
		            "int c[5];"                                                            +"\n"+
		            "void f () {"                                                          +"\n"+
		            "  int b[5];"                                                          +"\n"+
		            "  b[6]; //DEFECT,OOB,b"                                               +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },		            
////////////////	/  6   ///////////////////	
		            {
		            "int c[5];"                                                            +"\n"+
		            "void f () {"                                                          +"\n"+
		            "  c[6];  //DEFECT,OOB,c"                                              +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  7   ///////////////////	
		            {
		            "int c[7];"                                                            +"\n"+
		            "void f () {"                                                          +"\n"+
		            "  c[6];  //FP,OOB"                                              +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },       
////////////////	/  8   ///////////////////	
		            {
		            "int c[]={1,2,3,4,5,};"                                                +"\n"+
		            "void f () {"                                                          +"\n"+
		            "  c[4];  //FP,OOB"                                              +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },	      
////////////////	/  9   ///////////////////	
		            {
		            "int c[]={1,2,3,4,5,};"                                                +"\n"+
		            "void f () {"                                                          +"\n"+
		            "  c[5];  //DEFECT,OOB,c"                                              +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },	     
////////////////	/  10   ///////////////////	
		            {
		            "struct str{"                                                          +"\n"+
		            "	char*s;"                                                             +"\n"+
		            "	char a[100];"                                                        +"\n"+
		            "};"                                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "void test5(int i){"                                                   +"\n"+
		            "      int a[120];"                                                    +"\n"+
		            "	  struct str s;"                                                     +"\n"+
		            "      a[110]=1;//FP,OOB"                                              +"\n"+
		            "	  if(i<=100){"                                                       +"\n"+
		            "		  s.a[i]=1;//DEFECT,OOB,a"                                          +"\n"+
		            "	  }"                                                                 +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  11   ///////////////////	
		            {
		            "struct str{"                                                          +"\n"+
		            "	char*s;"                                                             +"\n"+
		            "	char a[100];"                                                        +"\n"+
		            "};"                                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "void test5(int i){"                                                   +"\n"+
		            "      int a[120];"                                                    +"\n"+
		            "	  struct str s;"                                                     +"\n"+
		            "      a[110]=1;//FP,OOB"                                              +"\n"+
		            "	  if(i<100){"                                                       +"\n"+
		            "		  s.a[i]=1;//FP,OOB"                                          +"\n"+
		            "	  }"                                                                 +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  12   ///////////////////	
		            {
		            "int test1 () {"                                                       +"\n"+
		            "  int a[4] ;"                                                         +"\n"+
		            "  int i = 4;"                                                         +"\n"+
		            "  a[i] = 1;	//DEFECT,OOB,a"                                           +"\n"+
		            "  return 0;"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },     
////////////////	/  13   ///////////////////	
		            {
		            "int test1 () {"                                                       +"\n"+
		            "  int a[4] ;"                                                         +"\n"+
		            "  int i = 3;"                                                         +"\n"+
		            "  a[i] = 1;	//DEFECT,OOB,a"                                           +"\n"+
		            "  return 0;"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },  	    
////////////////	/  14   ///////////////////	
		            {
		            "int test1 () {"                                                       +"\n"+
		            "  int i ;"                                                            +"\n"+
		            "  int a[4] = {1,2,3,4};"                                              +"\n"+
		            "  if(i > 2)"                                                          +"\n"+
		            "  { "                                                                 +"\n"+
		            "     a[i] = 1; //DEFECT,OOB,a"                                        +"\n"+
		            "  }	"                                                                 +"\n"+
		            "  return 0;"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  15   ///////////////////	
		            {
		            "int test3 () {"                                                       +"\n"+
		            "  int i ;"                                                            +"\n"+
		            "  int a[4] = {1,2,3,4};"                                              +"\n"+
		            "  if(i > 0 || i < 3)"                                                 +"\n"+
		            "  {"                                                                  +"\n"+
		            "     a[i] = 1; //DEFECT,OOB,a"                                        +"\n"+
		            "  }"                                                                  +"\n"+
		            "  return 0;"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  16   ///////////////////	
		            {
		            "int test2 () {"                                                       +"\n"+
		            "  int i ;"                                                            +"\n"+
		            "  int a[4] = {1,2,3,4};"                                              +"\n"+
		            "  if(i> 0 && i < 3)"                                                  +"\n"+
		            "  {"                                                                  +"\n"+
		            "     a[i] = 1; //FP,OOB"                                              +"\n"+
		            "  }"                                                                  +"\n"+
		            "  return 0;"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },	            
////////////////	/  17   ///////////////////	
		            {
		            "int c[3][3];"                                                         +"\n"+
		            "void f(){"                                                            +"\n"+
		            "  int b[3][3];"                                                       +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[3][1]=0;  //DEFECT,OOB,b"                                         +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  18   ///////////////////	
		            {
		            "int c[3][3];"                                                         +"\n"+
		            "void f(){"                                                            +"\n"+
		            "  int b[3][3];"                                                       +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[2][2]=0;  //FP,OOB"                                         +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },	            
////////////////	/ 19   ///////////////////	
		            {
		            "int c[3][3];"                                                         +"\n"+
		            "void f(){"                                                            +"\n"+
		            "  int b[3][3];"                                                       +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[2][3]=0;  //DEFECT,OOB,b"                                         +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },         
////////////////	/  20   ///////////////////	
		            {
		            "int c[3][3];"                                                         +"\n"+
		            "void f(){"                                                            +"\n"+
		            "  c[1][3]=0;  //DEFECT,OOB,c"                                         +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            }, 
////////////////	/  21   ///////////////////	
		            {
		            "int c[3][3];"                                                         +"\n"+
		            "void f(){"                                                            +"\n"+
		            "  c[2][2]=0;  //FP,OOB"                                         +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            }, 
////////////////	/  22   ///////////////////	
		            {
		            "int c[3][3];"                                                         +"\n"+
		            "void f(){"                                                            +"\n"+
		            "  c[3][1]=0;  //DEFECT,OOB,c"                                         +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            }, 	            
////////////////	/  23   ///////////////////	
		            {
		            "void f(){"                                                            +"\n"+
		            "  int b[][]={{1,2,4},{1,2,3},{1,2,3}};"                              +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[3][1]=0; //DEFECT,OOB,b"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },      
////////////////	/  24   ///////////////////	
		            {
		            "void f(){"                                                            +"\n"+
		            "  int b[][]={{1,2,4},{1,2,3},{1,2,3}};"                              +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[1][3]=0; //DEFECT,OOB,b"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },              
////////////////	/  25   ///////////////////	
		            {
		            "void f(){"                                                            +"\n"+
		            "  int b[][]={{1,2,4},{1,2,3},{1,2,3}};"                              +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[2][2]=0; //FP,OOB"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },             
////////////////	/  26   ///////////////////	
		            {
		            "void f(){"                                                            +"\n"+
		            "  int b[4][]={{1,2,4},{1,2,3},{1,2,3}};"                              +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[3][1]=0; //FP,OOB"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },      
////////////////	/  27   ///////////////////	
		            {
		            "void f(){"                                                            +"\n"+
		            "  int b[][4]={{1,2,4},{1,2,3},{1,2,3}};"                              +"\n"+
		            "  b[1][1]=0;"                                                         +"\n"+
		            "  b[1][2]=0;"                                                         +"\n"+
		            "  b[1][3]=0; //FP,OOB"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },    
////////////////	/  28   ///////////////////	
		            {
		            "int func1(int a) {"                                                   +"\n"+
		            "	int array[4];"                                                       +"\n"+
		            "	if (a > 4 || a < 0) {"                                               +"\n"+
		            "		return 0;"                                                          +"\n"+
		            "	}"                                                                   +"\n"+
		            "	array[a] = 1;  //DEFECT,OOB,array"                                   +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  29   ///////////////////	
		            {
		            "int func1(int a) {"                                                   +"\n"+
		            "	int array[4];"                                                       +"\n"+
		            "	if (a >= 4 || a < 0) {"                                               +"\n"+
		            "		return 0;"                                                          +"\n"+
		            "	}"                                                                   +"\n"+
		            "	array[a] = 1;  //FP,OOB"                                   +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  30   ///////////////////	
		            {
		            "int func2(int a) {"                                                   +"\n"+
		            "	int array[4];"                                                       +"\n"+
		            "	if (a>=0 && a<=4) {"                                                 +"\n"+
		            "		array[a] = 1;  //DEFECT,OOB,array"                                  +"\n"+
		            "		"                                                                   +"\n"+
		            "	}"                                                                   +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },	            
////////////////	/  31   ///////////////////	
		            {
		            "int main()"                                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "	int a[10]={0,1,2,3,4,5,6,7,8,9};"                                    +"\n"+
		            "	a[10] = 1;  //DEFECT,OOB,a"                                          +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  32   ///////////////////	
		            {
		            "int main()"                                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b[10]={0};"                                                      +"\n"+
		            "	b[10]=0;//DEFECT,OOB,b"                                              +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  33   ///////////////////	
		            {
		            "int main()"                                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b[10]={0};"                                                      +"\n"+
		            "	b[9]=0;//FP,OOB"                                              +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  34   ///////////////////	
		            {
		            "typedef struct _Test {"                                               +"\n"+
		            "	int a[2];"                                                           +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "int test1()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	Test st[2] = {0};"                                                   +"\n"+
		            "	st[0].a[0] = 1;"                                                     +"\n"+
		            "	st[0].a[1] = 1;"                                                     +"\n"+
		            "	st[1].a[2] = 1;  //DEFECT,OOB,a"                                    +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  35   ///////////////////	
		            {
		            "typedef struct _Test {"                                               +"\n"+
		            "	int a[2];"                                                           +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "int test1()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	Test st[2] = {0};"                                                   +"\n"+
		            "	st[0].a[0] = 1;"                                                     +"\n"+
		            "	st[1].a[1] = 1;  //FP,OOB"                                    +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  36   ///////////////////	
		            {
		            "typedef struct _Test {"                                               +"\n"+
		            "	int a[2];"                                                           +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "int test1()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	Test *p;"                                                            +"\n"+
		            "	p->a[2] = 1;  //DEFECT,OOB,a"                                        +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  37   ///////////////////	
		            {
		            "typedef struct _Test {"                                               +"\n"+
		            "	int a[2];"                                                           +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "int test1()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	Test *p;"                                                            +"\n"+
		            "	p->a[1] = 1;  //FP,OOB"                                        +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },	            
////////////////	/  38   ///////////////////	
		            {
		            "typedef struct _Test {"                                               +"\n"+
		            "	int a[3][3];"                                                        +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "int test1()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	Test st[2] = {0};"                                                   +"\n"+
		            "	st[1].a[0][3] = 1;  //DEFECT,OOB,a"                                  +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },   
////////////////	/  39   ///////////////////	
		            {
		            "typedef struct _Test {"                                               +"\n"+
		            "	int a[3][3];"                                                        +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "int test1()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	Test st[2] = {0};"                                                   +"\n"+
		            "	st[1].a[3][0] = 1;  //DEFECT,OOB,a"                                  +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            }, 	            
////////////////	/  40   ///////////////////	
		            {
		            "typedef struct _Test {"                                               +"\n"+
		            "	int a[3][3];"                                                        +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "int test1()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	Test st[2] = {0};"                                                   +"\n"+
		            "	st[1].a[2][2] = 1;  //FP,OOB"                                  +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            }, 	            
////////////////	/  41   ///////////////////	
		            {
		            "char *ghx_oob_1_f1()"                                                 +"\n"+
		            "{"                                                                    +"\n"+
		            "    char mm[200];"                                                    +"\n"+
		            "	int cc;"                                                             +"\n"+
		            "	char ff;"                                                            +"\n"+
		            ""                                                                     +"\n"+
		            "	for (cc=0; cc<=200; cc++)"                                            +"\n"+
		            "	{"                                                                   +"\n"+
		            "		ff=mm[cc]; //DEFECT,OOB,mm"                                                +"\n"+
		            "	}"                                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
////////////////	/  42   ///////////////////	
		            {
		            "char *ghx_oob_1_f1()"                                                 +"\n"+
		            "{"                                                                    +"\n"+
		            "    char mm[200];"                                                    +"\n"+
		            "	int cc;"                                                             +"\n"+
		            "	char ff;"                                                            +"\n"+
		            ""                                                                     +"\n"+
		            "	for (cc=0; cc<200; cc++)"                                            +"\n"+
		            "	{"                                                                   +"\n"+
		            "		ff=mm[cc]; //FP,OOB"                                                +"\n"+
		            "	}"                                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OK"
		            ,
		            },            
		        	/////////////////  43   ///////////////////	
		            {//zys:�����ɺ���ժҪ������ͨ��OOB���м�⣬���Ի��γ���
		            "int k;"                                                               +"\n"+
		            "f1(int i){"                                                           +"\n"+
		            "char buf[10];"                                                        +"\n"+
		            "k=10;i=k;buf[i]=1;"                                                            +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "f2(){"                                                                +"\n"+
		            "k=9;"                                                                +"\n"+
		            "f1(k);"                                                                +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OOB"
		            ,
		            },		
	/////////////////  44   ///////////////////	
		            {
		            ""                                                                     +"\n"+
		            "void yxh_oob_5_f1()"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "	int buf[4];"                                                         +"\n"+
		            "	char *p = (char *)buf;"                                              +"\n"+
		            "	p[15] = 'c';"                                                        +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OOB"
		            ,
		            },
	/////////////////  45   ///////////////////	
		            {
		            "void yxh_oob_f1()"                                                    +"\n"+
		            "{"                                                                    +"\n"+
		            "	char buf[13];"                                                       +"\n"+
		            "	int arr[4];"                                                         +"\n"+
		            ""                                                                     +"\n"+
		            "	((int *)buf)[4] = 1; //DEFECT"                                       +"\n"+
		            ""                                                                     +"\n"+
		            "	((char *)arr)[15] = 'c';"                                            +"\n"+
		            "	((char *)arr)[16] = 'c'; //DEFECT"                                   +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
	/////////////////  46   ///////////////////	
		            {
		            "void yxh_oob_f1()"                                                    +"\n"+
		            "{"                                                                    +"\n"+
		            "	char buf[4];"                                                        +"\n"+
		            "	int i = 1, j = 2;"                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "	char c = (buf+j)[j]; //DEFECT"                                       +"\n"+
		            ""                                                                     +"\n"+
		            "	(buf+1)[3] = 'c'; //DEFECT"                                          +"\n"+
		            "	(buf+j)[i] = 'c';"                                                   +"\n"+
		            "}"                                                                    
		            ,
		            "all"
		            ,
		            "OOB"
		            ,
		            },
	/////////////////  47   ///////////////////	
		            {
		            "void yxh_oob_f1()"                                                    +"\n"+
		            "{"                                                                    +"\n"+
		            "	char buf[4];"                                                        +"\n"+
		            "	"                                                                    +"\n"+
		            "	*(buf+4) = 'c'; //DEFECT"                                            +"\n"+
		            "	char c = *(buf+4); //DEFECT"                                         +"\n"+
		            "	"                                                                    +"\n"+
		            "	char *q = buf+4; //DEFECT"                                           +"\n"+
		            "	char *p = (buf+4); //DEFECT"                                         +"\n"+
		            "	char *r;"                                                            +"\n"+
		            "	r = buf+4; //DEFECT"                                                 +"\n"+
		            "	r = (buf+4); //DEFECT"                                               +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OOB"
		            ,
		            },

/////////////////  48   ///////////////////	
		            {//zys:�����ɺ���ժҪ������ͨ��OOB���м�⣬���Ի��γ���
		            "int k;"                                                               +"\n"+
		            "f1(int i){"                                                           +"\n"+
		            "char buf[10];"                                                        +"\n"+
		            "k=10;i=k;buf[i]=1;"                                                            +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "f2(){"                                                                +"\n"+
		            "k=9;"                                                                +"\n"+
		            "f1(k);"                                                                +"\n"+
		            "}"                                                                    
		            ,
		            "keil"
		            ,
		            "OOB"
		            ,
		            },
	/////////////////  49����ԭ��zys:���������еķ������ܽ���������⡣����   ///////////////////	
		            {
		            "int k;"                                                               +"\n"+
		            "f1(){"                                                           +"\n"+
		            "k++;"                                                            +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "f2(){"                                                                +"\n"+
		            "int a[10];k=5;"                                                                +"\n"+
		            "f1();a[k]=5;"                                                                +"\n"+
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
