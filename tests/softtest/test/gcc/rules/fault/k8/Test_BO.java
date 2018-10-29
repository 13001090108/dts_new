package softtest.test.gcc.rules.fault.k8;




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
public class Test_BO extends ModelTestBase{
	public Test_BO(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/fault/BO-0.1.xml";
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		fsm.setType("fault");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
		
		//���ؿ⺯��ժҪ
		LIB_SUMMARYS_PATH="gcc_lib/bo_summary.xml";
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
				 //ע��ͷ�ļ�string.h
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <string.h>"                                                   +"\n"+
		            "		  "                                                                 +"\n"+
		            "		  int main()"                                                       +"\n"+
		            "		  {"                                                                +"\n"+
		            "		      char buf[8] = \"a\";"                                           +"\n"+
		            "		      char tgt[1024];"                                              +"\n"+
		            "		      char * src = \"abcdef\";"                                       +"\n"+
		            "		  "                                                                 +"\n"+
		            "		      strncpy(buf, src, 3);"                                        +"\n"+
		            "		      strcpy(buf, tgt);   //defect"                                 +"\n"+
		            "		     return 0;"                                                     +"\n"+
		            "		 }"                                                                 
		            ,
		            "gcc"
		            ,
		            "BO"
		            ,
		            },
		            
   /////////////////  1  K8�е����ӣ�Ӧ�ñ�OK�� ///////////////////	
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <string.h>"                                                   +"\n"+
		            "		  "                                                                 +"\n"+
		            "		  int main()"                                                       +"\n"+
		            "		  {"                                                                +"\n"+
		            "		      char buf[8] = \"a\";"                                           +"\n"+
		            "		      char tgt[1024];"                                              +"\n"+
		            "		      char * src = \"abcdef\";"                                       +"\n"+
		            "		  "                                                                 +"\n"+
		            "		      strncpy(buf, src, 3);"                                        +"\n"+
		            "		      strcpy(tgt, buf);   //defect"                                 +"\n"+
		            "		     return 0;"                                                     +"\n"+
		            "		 }"                                                                 
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
/////////////////  2  ///////////////////	
		            //����ƫ�Ƽ��������⡣strncpy����֮��buf����EVAL����Ӧ��Ϊ[3,3]
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <string.h>"                                                   +"\n"+
		            "		  "                                                                 +"\n"+
		            "		  int main()"                                                       +"\n"+
		            "		  {"                                                                +"\n"+
		            "		      char buf[8]=\"a\";"                                           +"\n"+
		            "		      char tgt[2];"                                              +"\n"+
		            "		      char * src = \"abcdef\";"                                       +"\n"+
		            "		  "                                                                 +"\n"+
		            "		      strncpy(buf, src, 3);"                                        +"\n"+
		            "		      strcpy(tgt, buf);   //defect"                                 +"\n"+
		            "		     return 0;"                                                     +"\n"+
		            "		 }"                                                                 
		            ,
		            "gcc"
		            ,
		            "BO"
		            ,
		            },
		 });
	 }
}

