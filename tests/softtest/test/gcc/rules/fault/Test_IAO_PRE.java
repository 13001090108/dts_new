package softtest.test.gcc.rules.fault;


import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.FSMAnalysisVisitor;
import softtest.interpro.c.InterContext;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.test.c.rules.ModelTestBase;

@RunWith(Parameterized.class)
public class Test_IAO_PRE extends ModelTestBase{
	public Test_IAO_PRE(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/fault/IAO_PRE-0.1.xml";
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		fsm.setType("fault");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
		
		//���ؿ⺯��ժҪ
		LIB_SUMMARYS_PATH="gcc_lib/lib_summary.xml";
		libManager.loadSingleLibFile(LIB_SUMMARYS_PATH);
		Set<MethodNameDeclaration> libDecls = libManager.compileLib(pre.getLibIncludes());
		interContext = InterContext.getInstance();
		interContext.addLibMethodDecl(libDecls);
	}

	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {
	/////////////////  0   ///////////////////	
		            {
		            "void f1(int i) //��¼IAOǰ����Ϣ"                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b;"                                                              +"\n"+
		            "	b = 2/i;"                                                            +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f2(int j) //��¼IAOǰ����Ϣ"                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "	f1(j);"                                                              +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f3()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b = 2;"                                                          +"\n"+
		            "	f1(b);"                                                              +"\n"+
		            "	f1(0);	//DEFECT, ����ǰ����Ϣ"                                             +"\n"+
		            "	f2(2);"                                                              +"\n"+
		            "	f2(0); //DEFECT, ����ǰ����Ϣ"                                             +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "IAO_PRE"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "int g1 ;"                                                             +"\n"+
		            ""                                                                     +"\n"+
		            "void f1() //��¼ǰ����Ϣ"                                                   +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b = 1/g1; "                                                      +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f2()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	g1 = 0;"                                                             +"\n"+
		            "	f1();	//DEFECT, ����ǰ����Ϣ"                                              +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f3(int i, int j) //��¼ǰ����Ϣi != 0"                                 +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b = 1/i; "                                                       +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f4(int j) //��¼ǰ����Ϣj != 0"                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "	f3(j, 5);"                                                           +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f5()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int i=0;"                                                            +"\n"+
		            "	f3(i, 4); //DEFECT, IAO"                                             +"\n"+
		            "	f4(i); //DEFECT, IAO"                                                +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "IAO_PRE"
		            ,
		            },
	/////////////////  2   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "void f1(int a) //��¼IAO������Ϣ"                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "	div(10, a); "                                                        +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f2()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b = 0;"                                                          +"\n"+
		            "	f1(b); //DEFECT, ����IAO������Ϣ"                                          +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f3()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	int b = -2;"                                                         +"\n"+
		            "	f1(b+2); //DEFECT, ����IAO������Ϣ,"                                       +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "IAO_PRE"
		            ,
		            },
	/////////////////  3   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "int global;"                                                          +"\n"+
		            ""                                                                     +"\n"+
		            "void f1() //��¼IAO������Ϣ[global != 0]"                                   +"\n"+
		            "{"                                                                    +"\n"+
		            "	div(10, global); "                                                   +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f2()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	global = 0;"                                                         +"\n"+
		            "	f1(); //DEFECT, ����IAO������Ϣ"                                           +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f3() //��¼IAO������Ϣ[global != 0]"                                   +"\n"+
		            "{"                                                                    +"\n"+
		            "	f1();"                                                               +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f4()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	global = 0;"                                                         +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f5()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	f4();"                                                               +"\n"+
		            "	f3(); //DEFECT "                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "IAO_PRE"
		            ,
		            },
	/////////////////  4   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "int global;"                                                          +"\n"+
		            ""                                                                     +"\n"+
		            "void f1(int a) //�����Ǹ��������Ǹ����ʽ����¼IAO������Ϣ[global != 0] "                +"\n"+
		            "{"                                                                    +"\n"+
		            "	if(a == 0)"                                                          +"\n"+
		            "		div(10, global); "                                                  +"\n"+
		            "	if(a == 1)"                                                          +"\n"+
		            "		div(10, global+3);"                                                 +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f2()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "	global = 0;"                                                         +"\n"+
		            "	f1(0); //DEFECT"                                              +"\n"+
		            "}"                                                                     
		            ,
		            "gcc"
		            ,
		            "IAO_PRE"
		            ,
		            },
	/////////////////  5   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            "f1(int i){"                                                           +"\n"+
		            "	div(10,i);"                                                          +"\n"+
		            "}"                                                                    +"\n"+
		            "f2(int i){"                                                           +"\n"+
		            "	div(2,i);"                                                           +"\n"+
		            "}"                                                                    +"\n"+
		            "f3(int i){"                                                           +"\n"+
		            "	f1(i+1);"                                                              +"\n"+
		            "	f2(i);"                                                              +"\n"+
		            "}f4(){f3(0);}"                                                                    
		            ,
		            "gcc"
		            ,
		            "IAO_PRE"
		            ,
		            },
	/////////////////  6   ///////////////////	
		            {
		            "#include<stdlib.h>"                                                   +"\n"+
		            "void f1(int a){"                                                      +"\n"+
		            "div(10,a);"                                                           +"\n"+
		            "}"                                                                    +"\n"+
		            "void f2()"                                                            +"\n"+
		            "{"                                                                    +"\n"+
		            "int i=10;"                                                            +"\n"+
		            "i-=10;"                                                               +"\n"+
		            "i++;"                                                                 +"\n"+
		            "i--;"                                                                 +"\n"+
		            "f1(i);"                                                               +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "IAO_PRE"
		            ,
		            },



		 });
	 }
}
