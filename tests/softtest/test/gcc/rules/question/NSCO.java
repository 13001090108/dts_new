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
public class NSCO extends ModelTestBase{
	public NSCO(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/question/NSCO-0.1.xml";
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
		            "#include <stdio.h>"                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "static void check(int arr[]) {"                                       +"\n"+
		            "	if (arr!=NULL & sizeof(arr)!=0) {//NSCO,defect"                      +"\n"+
		            "        printf(\"OK\\n\"); "                                             +"\n"+
		            "	}"                                                                   +"\n"+
		            "	return;"                                                             +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NSCO"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <string.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "void f_NSCO_2(char name[])"                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "     if(name==NULL){"                                                 +"\n"+
		            "         return;"                                                     +"\n"+
		            "     }"                                                               +"\n"+
		            "     "                                                                +"\n"+
		            "     if(strcmp(name,\"seed\") | !strcmp(name,\"kira\")){// NSCO,defect"   +"\n"+
		            "         printf(\"OK\");"                                               +"\n"+
		            "     }"                                                               +"\n"+
		            "     return;"                                                         +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NSCO"
		            ,
		            },				 
				 
		 });
	 }
}

