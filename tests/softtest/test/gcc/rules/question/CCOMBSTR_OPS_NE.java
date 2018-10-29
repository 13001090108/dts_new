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
public class CCOMBSTR_OPS_NE extends ModelTestBase{

	public CCOMBSTR_OPS_NE(String source,String compiletype, String result) {
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/CCOMBSTR_OPS_NE-0.1.xml";
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
		            "	#include<wtypes.h>"                                                  +"\n"+
		            "	#include<atlbase.h>"                                                 +"\n"+
		            "	#include<atlcom.h>"                                                  +"\n"+
		            "	void ccombstr_ne() {"                                                +"\n"+
		            "	  				CComBSTR foo, bar;"                                                 +"\n"+
		            "	  				if (foo != bar)  // defect in comparison"                      +"\n"+
		            "	  					return;"                                                      +"\n"+
		            "	}"                                                                   
		            ,
		            "gcc"
		            ,
		            "CCOMBSTR_OPS_NE"
		            ,
		            },

		});
	 }
}
