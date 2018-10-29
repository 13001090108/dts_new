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
public class FDT extends ModelTestBase{
	public FDT(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/FDT-0.1.xml";
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
		            "void foo(int i) {"                                                    +"\n"+
		            "      char c;"                                                        +"\n"+
		            "      c = i;"                                                         +"\n"+
		            "    }"                                                                
		            ,
		            "gcc"
		            ,
		            "FDT"
		            ,
		            },

	/////////////////  2   ///////////////////	
		            {
		            "  	void func ()"                                                      +"\n"+
		            "  	{"                                                                 +"\n"+
		            "  	  int n;"                                                          +"\n"+
		            "  	  scanf( \"%c\", &n );"                                              +"\n"+
		            "  	  printf(\"the n is %d\",n);"                                        +"\n"+
		            "  	  return;"                                                         +"\n"+
		            "  	}"                                                                 
		            ,
		            "gcc"
		            ,
		            "FDT"
		            ,
		            },
			 
				 
		 });
	 }
}

