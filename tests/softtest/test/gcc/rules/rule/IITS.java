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
public class IITS extends ModelTestBase {
	public IITS(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/rule/IITS-0.1.xml";
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
		            "struct s_type_a{int xs;float fs;};"                                   +"\n"+
		            "void static_p(void)"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "  struct s_type_a sta={3.14f,0.0f};"                                  +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "IITS"
		            ,
		            },
		/////////////////  1   ///////////////////	
		            {
		            "struct s_type_a{int xs;float fs;};"                                   +"\n"+
		            "void static_p(void)"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "  struct s_type_a sta={3,0.0f};"                                      +"\n"+
		            "  /*...*/"                                                            +"\n"+
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
