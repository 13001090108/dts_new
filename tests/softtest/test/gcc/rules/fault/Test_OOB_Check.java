package softtest.test.gcc.rules.fault;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
import java.util.Set;

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
import softtest.fsmanalysis.c.CAnalysis;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.fsmanalysis.c.Report;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;
import softtest.summary.lib.c.LibManager;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

@RunWith(Parameterized.class)
public class Test_OOB_Check {
	private String source = null;
	private String compiletype=null;
	private String result = null;
	private static final String fsmPath="softtest/rules/gcc/fault/OOB_Check-0.1.xml";
	FSMAnalysisVisitor fsmAnalysis; 
	private FSMControlFlowData cfData;
	
	static Pretreatment pre=new Pretreatment();
	static InterContext interContext = InterContext.getInstance();
	static int testcaseNum=0;
	String temp;//Ԥ�������м��ļ�
	
	public Test_OOB_Check(String source,String compiletype, String result)
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
		astroot.jjtAccept(new DUAnalysisVisitor(), null);
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
		            {
		            ""                                                       +"\n"+
		            "void f(int i){"                                                       +"\n"+
		            "   int a[10];"                                                        +"\n"+
		            "   a[i]=1; //defect"                                                  +"\n"+
		            "   if(i>10)"                                                          +"\n"+
		            "     i++;"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OOB_Check"
		            ,
		            },
////////////////	/  1 ///////////////////	
		            {
		            ""                                                       +"\n"+
		            "void f(int i){"                                                       +"\n"+
		            "   int a[10],b;"                                                        +"\n"+
		            "   if(i>10)"                                                          +"\n"+
		            "     b=i;"                                                            +"\n"+
		            "   a[i]=1; //defect"                                                  +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OOB_Check"
		            ,
		            },	  
////////////////	/  2 ///////////////////	
		            {
		            ""                                                       +"\n"+
		            "void f(int i){"                                                       +"\n"+
		            "   int a[10];"                                                        +"\n"+
		            "   if(i>10)"                                                          +"\n"+
		            "     i++;"                                                            +"\n"+
		            "   a[i]=1; //defect"                                                  +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  3///////////////////	
		            {
		            ""                                                       +"\n"+
		            "void f(int i){"                                                       +"\n"+
		            "   int a[10];"                                                        +"\n"+
		            "   //a[i]=1; //defect"                                                  +"\n"+
		            "   i+=5;"                                                          +"\n"+
		            "   if(i>10)"                                                          +"\n"+
		            "     a[i]=1;"                                                            +"\n"+
		            "   else{a[i]=1;}"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
////////////////	/  4 ///////////////////	
		            {
		            ""                                                       +"\n"+
		            "void f(int i){"                                                       +"\n"+
		            "   int a[10];"                                                        +"\n"+
		            "   a[i]=1; //defect"                                                  +"\n"+
		            "   if(i>10)"                                                          +"\n"+
		            "     i++;"                                                            +"\n"+
		            "}"                                                                     +"\n"+
		            "void f1(int i){"                                                       +"\n"+
		            "   int a[10];"                                                        +"\n"+
		            "   a[i]=1; //defect"                                                  +"\n"+
		            "   if(i>10)"                                                          +"\n"+
		            "     i++;"                                                            +"\n"+
		            "}"
		            ,
		            "gcc"
		            ,
		            "OOB_Check"
		            ,
		            },
///////////////// 5   ///////////////////	
		            {
		            "void f(int i){"                                                       +"\n"+
		            "     int a[10];"                                                      +"\n"+
		            "     a[i]=1;"                                                         +"\n"+
		            "}"                                                                    +"\n"+
		            "void f1(int i){"                                                      +"\n"+
		            "     f(i);"                                                           +"\n"+
		            "     if(i>10)"                                                        +"\n"+
		            "     i++;"                                                            +"\n"+
		            "} "                                                                   
		            ,
		            "gcc"
		            ,
		            "OOB_Check"
		            ,
		            },		
	///////////////// 6   ///////////////////	
		            {
		            "void f(int i){"                                                       +"\n"+
		            "     int a[10];"                                                      +"\n"+
		            "     a[i]=1;"                                                         +"\n"+
		            "}"                                                                    +"\n"+
		            "void f1(int i){"                                                      +"\n"+
		            "     int k;"                                                           +"\n"+
		            "     if(i>10)"                                                           +"\n"+
		            "     	k=i;"                                                        +"\n"+
		            "     f(i);"                                                            +"\n"+
		            "} "                                                                   
		            ,
		            "gcc"
		            ,
		            "OOB_Check"
		            ,
		            },		
///////////////// 7   ///////////////////	
		            {
		            "void f(int i){"                                                       +"\n"+
		            "     int a[10];"                                                      +"\n"+
		            "     a[i]=1;"                                                         +"\n"+
		            "}"                                                                    +"\n"+
		            "void f1(int i){"                                                      +"\n"+
		            "     if(i>10)"                                                           +"\n"+
		            "     i++;"                                                        +"\n"+
		            "     i++;f(i);"                                                            +"\n"+
		            "} "                                                                   
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },	            
	 });
 }
}
	