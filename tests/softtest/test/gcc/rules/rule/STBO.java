package softtest.test.gcc.rules.rule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.interpro.c.InterContext;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.test.c.rules.ModelTestBase;

@RunWith(Parameterized.class)
public class STBO extends ModelTestBase {
	public STBO(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/rule/STBO-0.1.xml";
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		fsm.setType("rule");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
	}
	
	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {
	/////////////////  0   ///////////////////	
		            {
		            "void static_p(void)"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "  int b=1;"                                                           +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "  b = b|1;"                                                           +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "STBO"
		            ,
		            },
		/////////////////  1   ///////////////////	
		            {
		            "void static_p(void)"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "  int b=1;"                                                           +"\n"+
		            "  unsigned int a=1u;"                                                 +"\n"+
		            "  b &= a;"                                                            +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "STBO"
		            ,
		            },
		/////////////////  2   ///////////////////	
		            {
		            "void static_p(void)"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "  int b=1;"                                                           +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "  b = b^1;"                                                           +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "STBO"
		            ,
		            },
		/////////////////  3   ///////////////////	
		            {
		            "void static_p(void)"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "  int b=1;"                                                           +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "  b = ~b;"                                                           +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "STBO"
		            ,
		            },
		 });
	 }
}
