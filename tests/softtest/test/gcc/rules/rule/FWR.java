package softtest.test.gcc.rules.rule;

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
public class FWR extends ModelTestBase {
	public FWR(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/rule/FWR-0.1.xml";
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
		            "unsigned int static_p(unsigned int p_1, unsigned int p_2)"            +"\n"+
		            "{"                                                                    +"\n"+
		            "  unsigned int y=p_1;"                                                +"\n"+
		            "  /*Not returning a value*/"                                          +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "FWR"
		            ,
		            },
		/////////////////  1   ///////////////////	
		            {
		            "void static_p(unsigned int p_1, unsigned int p_2)"                    +"\n"+
		            "{"                                                                    +"\n"+
		            "  unsigned int result;"                                               +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "  result=p_1+p_2;"                                                    +"\n"+
		            "  return result;"                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "FWR"
		            ,
		            },
		/////////////////  2   ///////////////////	
		            {
		            "unsigned int static_p(unsigned int p_1, unsigned int p_2)"            +"\n"+
		            "{"                                                                    +"\n"+
		            "  /*...*/"                                                            +"\n"+
		            "  return;"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "FWR"
		            ,
		            },
		/////////////////  3   ///////////////////	
		            {
		            "unsigned int static_p(unsigned int par_1)"                            +"\n"+
		            "{"                                                                    +"\n"+
		            "  switch(par_1){"                                                     +"\n"+
		            "    case 0:"                                                          +"\n"+
		            "      return -1;"                                                     +"\n"+
		            "      break;"                                                         +"\n"+
		            "    case 1:"                                                          +"\n"+
		            "      return 1u;"                                                     +"\n"+
		            "      break;"                                                         +"\n"+
		            "    case 2:"                                                          +"\n"+
		            "      return 1L;"                                                     +"\n"+
		            "      break;"                                                         +"\n"+
		            "    case 3:"                                                          +"\n"+
		            "      return 1.0f;"                                                   +"\n"+
		            "      break;"                                                         +"\n"+
		            "    default:"                                                         +"\n"+
		            "      break;"                                                         +"\n"+
		            "  }"                                                                  +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "FWR"
		            ,
		            },
		/////////////////  4   ///////////////////	
		            {
		            "int f()"                                                              +"\n"+
		            "{"                                                                    +"\n"+
		            "  return 1;"                                                          +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
		/////////////////  5   ///////////////////	
		            {
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "  return;"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  7   ///////////////////	
		            {
		            ""                                                                     +"\n"+
		            "typedef float F32;"                                                   +"\n"+
		            "F32 f(F32 x,F32 y){"                                                  +"\n"+
		            "return (x+y)/2;}"                                                     
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },

		/////////////////  ��ʱ�޷����   ///////////////////	
		           /* {
		            "int f(int x)"                                                         +"\n"+
		            "{"                                                                    +"\n"+
		            "    if(x)"                                                            +"\n"+
		            "      return 1;"                                                      +"\n"+
		            "    else"                                                             +"\n"+
		            "      x++;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "FWR"
		            ,
		            },*/
		 });
	 }
}
