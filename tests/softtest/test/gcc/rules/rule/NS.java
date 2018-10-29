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
public class NS extends ModelTestBase{
	public NS(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/rule/NS-0.1.xml";
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
		            "void f_NS_1() {"                                                      +"\n"+
		            "     int a = 1;"                                                      +"\n"+
		            "     switch(a) {//NS"                                                 +"\n"+
		            "         default: "                                                   +"\n"+
		            "         a++;"                                                        +"\n"+
		            "         break;    "                                                  +"\n"+
		            "     }"                                                               +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NS"
		            ,
		            },	 
				 
				 
				 
		 });
	 }
}


