package softtest.test.c.gcc.test_team;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.config.c.Config;
import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.CAnalysis;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.fsmanalysis.c.Report;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

@RunWith(Parameterized.class)
public class Test_UVF {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/gcc/fault/UVF-0.1.xml";
	FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	static Pretreatment pre=new Pretreatment();
	static InterContext interContext = InterContext.getInstance();
	static int testcaseNum=0;
	String temp;//Ԥ�������м��ļ�
	
	public Test_UVF(String source,String compiletype, String result)
	{
		this.source=source;	
		this.compiletype=compiletype;
		this.result = result;
	}

	@BeforeClass
	public static void setUpBase()
	{
		//���ݴ�����ģʽXML�ļ�·����ʼ���Զ����б�
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		CAnalysis.deleteAndCreateTemp(Config.PRETREAT_DIR);
		pre.setPlatform(PlatformType.GCC);
		String INCLUDE = System.getenv("GCCINC");
		if(INCLUDE==null){
			throw new RuntimeException("System environment variable \"GCCINC\" error!");
		}
		String[] Inctemp = INCLUDE.split(";");
		//��GCCINC�е�ͷ�ļ�Ŀ¼���Զ�ʶ��Ϊͷ�ļ�Ŀ¼
		List<String> include = new ArrayList<String>();
		for(int i = 0;i<Inctemp.length;i++){
			Pretreatment.systemInc.add(Inctemp[i]);
			include.add(Inctemp[i]);
		}
		pre.setInclude(include);
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
		
		//�����������еĴ����У�д��temp���γ�.cԴ�ļ���
		String tempName="testcase_"+ (testcaseNum++) +".c";
		File tempFile=new File(Config.PRETREAT_DIR +"\\"+ tempName);
		if (Config.DELETE_PRETREAT_FILES) {
			tempFile.deleteOnExit();
		}
		FileWriter fw;
		try {
			fw = new FileWriter(tempFile);
			fw.write(source);
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		temp=pre.pretreat(tempFile.getAbsolutePath(),  pre.getInclude(), new ArrayList<String>());
		
		//���ݵ�ǰ���Ĳ���������������صĿ⺯��ժҪ
		
	}

	@After
	public void shutdown() {
	
	}

	@Test
	public void test() {
		try {
			CParser.setType("gcc");
			CParser parser_gcc;
			parser_gcc = CParser.getParser(new CCharStream(new FileInputStream(temp)));
			CParser.setType("keil");
			CParser parser_keil = CParser.getParser(new CCharStream(new FileInputStream(temp)));
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
				pre.setPlatform(PlatformType.GCC);
				analysis(gcc_astroot);
				
				CParser.setType("keil");
				try {
					keil_astroot= parser_keil.TranslationUnit();
				} catch (ParseException e) {
					e.printStackTrace();
					fail("parse error");
				}
				pre.setPlatform(PlatformType.KEIL);
				analysis(keil_astroot);
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}
	
	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {
	/////////////////  0   ///////////////////	
		            {
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     x++;//DEFECT     "                                               +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "void f(int i)"                                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "     int b;"                                                          +"\n"+
		            "     if(i)"                                                           +"\n"+
		            "         b=100;"                                                      +"\n"+
		            "     b--;//DEFECT"                                                    +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  2   ///////////////////	
		            {
		            "int f1(int i)"                                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "     return i;     "                                                  +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x, y;"                                                       +"\n"+
		            "     y = f1(x);  //DEFECT  "                                          +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  3   ///////////////////	
		            {
		            "int f1(int i)"                                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "     i++;"                                                            +"\n"+
		            "     return i;     "                                                  +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "int f2(int j)"                                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "     return f1(j);    "                                               +"\n"+
		            "}"                                                                    +"\n"+
		            "void f3(int a)"                                                       +"\n"+
		            "{"                                                                    +"\n"+
		            "     int b;"                                                          +"\n"+
		            "     a = f2(b);//DEFECT"                                              +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  4   ///////////////////	
		            {
		            "int f(int i)"                                                         +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     if(i)"                                                           +"\n"+
		            "         i++;"                                                        +"\n"+
		            "     else"                                                            +"\n"+
		            "         x = 10;"                                                     +"\n"+
		            "     return x+i;//DEFECT"                                             +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  5   ///////////////////	
		            {
		  //ϵͳ�ж�·��ʱ�����ı����Ƿ�ö�����ͣ�ֻҪ������ֵ��֪������ôswitch������·�����ǿ��ܵ�
		            	//Ҳ����x�п���û�г�ʼ��,����x--Ҳ����cl�������жϣ�������15���򲻻ᱨuvf
		            "enum colors {r=1, b=2, y=3};"                                         +"\n"+
		            "void f(enum colors cl)"                                               +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     switch(cl){"                                                     +"\n"+
		            "         case 1:"                                                     +"\n"+
		            "             x = 1;"                                                  +"\n"+
		            "             break;"                                                  +"\n"+
		            "         case 2:"                                                     +"\n"+
		            "             x = 2;"                                                  +"\n"+
		            "             break;   "                                               +"\n"+
		            "         case 3:"                                                     +"\n"+
		            "             x = 3;"                                                  +"\n"+
		            "             break;"                                                  +"\n"+
		            "         default:"                                                    +"\n"+
		            "             break;"                                                  +"\n"+
		            "            }"                                                        +"\n"+
		            "     x--;          "                                                  +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  6   ///////////////////	
		            {
		            "enum colors {r=1, b=2, y=3};"                                         +"\n"+
		            "void f(enum colors cl)"                                               +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     switch(cl){"                                                     +"\n"+
		            "         case 1:"                                                     +"\n"+
		            "             x = 1;"                                                  +"\n"+
		            "             break;"                                                  +"\n"+
		            "         case 2:"                                                     +"\n"+
		            "             x = 2;"                                                  +"\n"+
		            "             break;   "                                               +"\n"+
		            "         default:"                                                    +"\n"+
		            "             break;"                                                  +"\n"+
		            "            }"                                                        +"\n"+
		            "     x--;//DEFECT"                                                    +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  7   ///////////////////	
		            {
		            "enum colors {r=1, b=2, y=3};"                                         +"\n"+
		            "void f(enum colors cl)"                                               +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x, y;"                                                       +"\n"+
		            "     switch(cl){"                                                     +"\n"+
		            "         case 1:"                                                     +"\n"+
		            "             y = 1;"                                                  +"\n"+
		            "             break;"                                                  +"\n"+
		            "         case 2:"                                                     +"\n"+
		            "             x = 2;"                                                  +"\n"+
		            "             break;   "                                               +"\n"+
		            "         case 3:"                                                     +"\n"+
		            "             x = 3;"                                                  +"\n"+
		            "             break;"                                                  +"\n"+
		            "         default:"                                                    +"\n"+
		            "             break;"                                                  +"\n"+
		            "            }"                                                        +"\n"+
		            "     x--; //DEFECT"                                                   +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  8   ///////////////////	
		            {
		            "typedef struct{"                                                      +"\n"+
		            "    double a;"                                                        +"\n"+
		            "    int b;            "                                               +"\n"+
		            "} Test;"                                                              +"\n"+
		            ""                                                                     +"\n"+
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "    Test x, y;"                                                       +"\n"+
		            "    x.a = 1.3;"                                                       +"\n"+
		            "    y.b = x.b;//DEFECT"                                               +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  9   ///////////////////	
		            {
		            "#include <string.h>"                                                  +"\n"+
		            "void f1(char* a)"                                                     +"\n"+
		            "{"                                                                    +"\n"+
		            "     strcpy(a, \"abc\");"                                             +"\n"+
		            "}"                                                                    +"\n"+
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "     char s[100];"                                                    +"\n"+
		            "     f1(s);"                                                          +"\n"+
		            "     strlen(s);"                                                      +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },	
	/////////////////  10   ///////////////////	
		            {
		            "void f1(int* a){}"                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "    int a[10];"                                                       +"\n"+
		            "    a[1]++;  //DEFECT  "                                              +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
	/////////////////  11   ///////////////////	
		            {
		            "void f(int x)"                                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "    int i;"                                                           +"\n"+
		            "    if(x>0)"                                                          +"\n"+
		            "        i = 1;"                                                       +"\n"+
		            "    if(x<0)"                                                          +"\n"+
		            "        i = -1;"                                                      +"\n"+
		            "    i++;//DEFECT"                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },

/////////////////  12   ///////////////////	
		            {
		            "void f(int a, int b)"                                                 +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     if(a > 10)"                                                      +"\n"+
		            "         { x= 4;}"                                                      +"\n"+
		            "     if(b > 2)"                                                       +"\n"+
		            "         if(a <5)"                                                  +"\n"+
		            "            {a=2; x=4 ;x++;}"                                                +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
/////////////////  13   ///////////////////	
		            //·��������ʱΪuvf��·������ʱ�ڵ�1��if_outʱ�����֧��״̬Ϊend����Ϊx��ֵ�ˣ����ٷ�֧
		            //�����������ٷ�֧�����a�����䡾-inf��10�������Ե�����if�ж�x++���ɴ����uvf����Ϊ����
		            //14�����,�ͻᱨuvf
		            {
		            "void f(int a, int b)"                                                 +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     if(a > 10)"                                                      +"\n"+
		            "          x= 4;"                                                      +"\n"+
		            "     if(b > 2)"                                                       +"\n"+
		            "         if(a > 11)"                                                  +"\n"+
		            "           x++; "                                                +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
/////////////////  14   ///////////////////	
		   //�Ա�����13
		            {
		            "void f(int a, int b)"                                                 +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     if(a > 10)"                                                      +"\n"+
		            "          x= 4;"                                                      +"\n"+
		            "     if(b > 2)"                                                       +"\n"+
		            "         if(a<11)"                                                  +"\n"+
		            "           x++; "                                                +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "UVF"
		            ,
		            },
/////////////////  15   ///////////////////	
		            {
		  //�Ա�����5
		            "enum colors {r=1, b=2, y=3};"                                         +"\n"+
		            "void f(enum colors cl)"                                               +"\n"+
		            "{"                                                                    +"\n"+
		            "     int x;"                                                          +"\n"+
		            "     switch(cl){"                                                     +"\n"+
		            "         case 1:"                                                     +"\n"+
		            "             x = 1;"                                                  +"\n"+
		            "             break;"                                                  +"\n"+
		            "         case 2:"                                                     +"\n"+
		            "             x = 2;"                                                  +"\n"+
		            "             break;   "                                               +"\n"+
		            "         case 3:"                                                     +"\n"+
		            "             x = 3;"                                                  +"\n"+
		            "             break;"                                                  +"\n"+
		            "         default:"                                                    +"\n"+
		            "             break;"                                                  +"\n"+
		            "            }"                                                        +"\n"+
		            "     if(cl==1||cl==2||cl==3)x--;          "                                                  +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
		 });
	 }
}


