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
public class NG extends ModelTestBase{
	public NG(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/rule/NG-0.1.xml";
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
		            "void f_NG_1(int a) {"                                                 +"\n"+
		            "     start:"                                                          +"\n"+
		            "           if(a >0)"                                                  +"\n"+
		            "           goto start;//NG"                                           +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NG"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "void f_NG_2(int a) {"                                                 +"\n"+
		            "     start:"                                                          +"\n"+
		            "           if(a >0)"                                                  +"\n"+
		            "           goto loop;//NG"                                            +"\n"+
		            "     loop:"                                                           +"\n"+
		            "           if(a >0)"                                                  +"\n"+
		            "           goto start;//NG"                                           +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NG"
		            ,
		            },				 
				 
				 
		 });
	 }
}


