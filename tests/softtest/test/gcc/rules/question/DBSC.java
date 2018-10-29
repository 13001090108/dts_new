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
public class DBSC extends ModelTestBase{
	public DBSC(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/DBSC-0.1.xml";
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
		            "void setValue_1(int a){"                                              +"\n"+
		            "	int num=0;"                                                          +"\n"+
		            "	if(a<=0){//DBSC,defect"                                              +"\n"+
		            "		num++;"                                                             +"\n"+
		            "	}"                                                                   +"\n"+
		            "	else{"                                                               +"\n"+
		            "		num++;"                                                             +"\n"+
		            "	}"                                                                   +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "DBSC"
		            ,
		            },
    /////////////////  1   ///////////////////	
		            {
		            "void setValue_2(int a){"                                              +"\n"+
		            "	int num=0;"                                                          +"\n"+
		            "	switch(a){//DBSC,defect"                                             +"\n"+
		            "	case 1:"                                                             +"\n"+
		            "		num++;"                                                             +"\n"+
		            "		break;"                                                             +"\n"+
		            "	case 2:"                                                             +"\n"+
		            "		num++;"                                                             +"\n"+
		            "		break;"                                                             +"\n"+
		            "    default:"                                                         +"\n"+
		            "		num--;"                                                             +"\n"+
		            "    }"                                                                +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "DBSC"
		            ,
		            },
				 
				 
				 
				 
		 });
	 }
}

