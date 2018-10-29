package softtest.test.gcc.rules.question;

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
public class MC extends ModelTestBase{
	public MC(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/MC-0.1.xml";
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
    /////////////////  0   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "void f_MC()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "    int i = abs(12);//MC,defect"                                      +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MC"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "#include <math.h>"                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "double f_MC_2()"                                                      +"\n"+
		            "{"                                                                    +"\n"+
		            "    double i = acos(1.0);//MC,defect"                                 +"\n"+
		            "    return i;"                                                        +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MC"
		            ,
		            },
		            
		            
				 
		 });
	 }
}


