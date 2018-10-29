package softtest.test.c.hangtiancheng;

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
public class SUMU extends ModelTestBase{
	public SUMU(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/SUMU-0.1.xml";
		//fsmPath="softtest/rules/gcc/fault/BO_PRE-0.1.xml";
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		fsm.setType("fault");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
	}
	
	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {
	/////////////////  0   ///////////////////
				 //�����Ӳ������󱨣����������÷��ں���ǹ�������ձ飬���ܲ�̫�ʺϼ���乤�̡�
		            {
		            "void f_SUMU_2(){"                                                     +"\n"+
		            "     unsigned int counter;"                                           +"\n"+
		            "     if(counter>104){//SUMU,defect"                              +"\n"+
		            "          //..."                                                      +"\n"+
		            "     }"                                                               +"\n"+
		            "} "                                                                   
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "void f_SUMU_3(){"                                                     +"\n"+
		            "     unsigned int counter;"                                           +"\n"+
		            "     int number=0;"                                                   +"\n"+
		            "     if(counter<=number){//SUMU,defect"                           +"\n"+
		            "          //..."                                                      +"\n"+
		            "     }"                                                               +"\n"+
		            "} "                                                                   
		            ,
		            "gcc"
		            ,
		            "SUMU"
		            ,
		            },				 
				 
				 

		 });
	 }
}
