package softtest.test.c.gcc.test_team;

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
public class Test_NPD extends ModelTestBase {
	public Test_NPD(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/fault/NPD-0.1.xml";
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		fsm.setType("fault");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
		
		//���ؿ⺯��ժҪ
		LIB_SUMMARYS_PATH="gcc_lib/npd_summary.xml";
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
		            "#include <stdlib.h>"                                                  +"\n"+
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "     int* p = NULL;"                                                  +"\n"+
		            "     if(*p>5)//DEFECT"                                                +"\n"+
		            "        *p++;"                                                        +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NPD"
		            ,
		            },
	///////////////// 1   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            "void f(int i)"                                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "     int *p, *q = NULL;"                                              +"\n"+
		            "     if(i == 7)"                                                      +"\n"+
		            "         p = q;"                                                      +"\n"+
		            "     *p = 10;//DEFECT"                                                +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NPD"
		            ,
		            },
	///////////////// 2   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            "void f(int i)"                                                        +"\n"+
		            "{"                                                                    +"\n"+
		            "     int* p = NULL;"                                                  +"\n"+
		            "     if(i>5)"                                                         +"\n"+
		            "        *p++;//DEFECT"                                                +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "NPD"
		            ,
		            },
		            

		 });
	 }
}
