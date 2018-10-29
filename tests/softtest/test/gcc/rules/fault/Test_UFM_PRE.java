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
public class Test_UFM_PRE extends ModelTestBase {
	public Test_UFM_PRE(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/fault/UFM_PRE-0.1.xml";
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
		            "  #include <stdlib.h>"                                                +"\n"+
		            "  typedef struct x {"                                                 +"\n"+
		            "      char * field;"                                                  +"\n"+
		            "  } tx;"                                                              +"\n"+
		            "  "                                                                   +"\n"+
		            "  void release(tx * a){"                                              +"\n"+
		            "      a->field = \"ab\";"                                                       +"\n"+
		            " }"                                                                   +"\n"+
		            " "                                                                    +"\n"+
		            " int main() {"                                                        +"\n"+
		            "     tx *a = (tx *)malloc(sizeof(tx));"                               +"\n"+
		            "     if (a==0) return;"                                               +"\n"+
		            "     a->field = (char *)malloc(10);"                                  +"\n"+
		            "     free(a);"                                                    +"\n"+
		            "     release(a);"                                                     +"\n"+
		            "    "                                                                 +"\n"+
		            " }"                                                                   
		            ,
		            "gcc"
		            ,
		            "UFM_PRE"
		            ,
		            },

		 });
	 }
}
