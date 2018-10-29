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
public class NSJ extends ModelTestBase{
	public NSJ(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/rule/NSJ-0.1.xml";
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
		            "#include<setjmp.h>"                                                   +"\n"+
		            "void f_NSJ_1(jmp_buf mark,unsigned int val) {"                        +"\n"+
		            "     longjmp(mark,val);//NSJ"                                         +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NSJ"
		            ,
		            },			 
	/////////////////  1   ///////////////////	
		            {
		            "#include<setjmp.h>"                                                   +"\n"+
		            "void f_NSJ_2(jmp_buf mark) {"                                         +"\n"+
		            "     setjmp(mark);//NSJ"                                              +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NSJ"
		            ,
		            },
		            
		            
				 
		 });
	 }
}



