package question;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;

import softtest.fsmanalysis.c.FSMAnalysisVisitor;

import softtest.test.c.rules.ModelTestBase;

@RunWith(Parameterized.class)
public class NBCL extends ModelTestBase{
	public NBCL(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/NBCL-0.1.xml";
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		fsm.setType("question");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
	}
	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {

	/////////////////  1   ///////////////////	
		            {
		            "#include<stdbool.h>"                                                  +"\n"+
		            "void foo(bool debug) {"                                               +"\n"+
		            "      		"                                                             +"\n"+
		            "      		if (debug); "                                                 +"\n"+
		            "		{ "                                                                 +"\n"+
		            "      			printf(\"Enter foo\");"                                        +"\n"+
		            "      		}"                                                            +"\n"+
		            "      }"                                                              
		            ,
		            "gcc"
		            ,
		            "NBCL"
		            ,
		            },
			 
				 
		 });
	 }
}



