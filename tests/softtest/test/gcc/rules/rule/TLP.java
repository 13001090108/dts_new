package softtest.test.gcc.rules.rule;

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
public class TLP extends ModelTestBase{
	public TLP(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/rule/TLP-0.1.xml";
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
		            "void f_TLP_1() {"                                                     +"\n"+
		            "  int a[10] = {0};"                                                   +"\n"+
		            "  int *p1_ptr,**p2_ptr;"                                              +"\n"+
		            "  int ***p3_ptr;      "                                               +"\n"+
		            "  int w;"                                                             +"\n"+
		            "  p1_ptr = a;"                                                        +"\n"+
		            "  p2_ptr = &p1_ptr;"                                                  +"\n"+
		            "  p3_ptr = &p2_ptr;    "                                              +"\n"+
		            "  w = ***p3_ptr;"                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "TLP"
		            ,
		            },				 
				 
				 
				 
				 
		 });
	 }
}

