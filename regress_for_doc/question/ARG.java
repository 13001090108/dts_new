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
public class ARG extends ModelTestBase{
	public ARG(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/ARG-0.1.xml";
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
		            "#include <stdlib.h>"                                                  +"\n"+
		            "  	void func_ARG(int **pp, unsigned n)"                               +"\n"+
		            "{"                                                                    +"\n"+
		            "      			int aux;"                                                    +"\n"+
		            "      			if (n == 1) {"                                               +"\n"+
		            "          				*pp = &aux;   //defect"                                 +"\n"+
		            "      			} else {"                                                    +"\n"+
		            "          				*pp = (int *)malloc(n * sizeof(int));"                  +"\n"+
		            "      			}"                                                           +"\n"+
		            " }"                                                                   
		            ,
		            "gcc"
		            ,
		            "ARG"
		            ,
		            },

		
		 });
	 }
}

