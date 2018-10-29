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
public class Test_MLF extends ModelTestBase {
	public Test_MLF(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/fault/MLF-0.1.xml";
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

				// �����������ӦģʽMLF_FREE����û���ҵ�
				// ��ģʽ�����ȷ��ڱ��ļ��У����Ժ�ʹ�á�
				// 2011��12��30�� zhhailon
//				//////////////////////////0//////////////////////////////////////////////
//				{
//					"void my_free(char *p, int flag) {"                                    +"\n"+
//							"		      if (flag == 17) {"                                            +"\n"+
//							"		          p = 0;"                                                   +"\n"+
//							"		          return;   //defect"                                       +"\n"+
//							"		      }"                                                            +"\n"+
//							"		      if (flag == 34) {"                                            +"\n"+
//							"		          return;   //defect"                                       +"\n"+
//							"		      }"                                                            +"\n"+
//							"		      free(p);"                                                     +"\n"+
//							"		 }"                                                            
//							,
//							"gcc"
//							,
//							"MLF"
//							,
//				},

		});
	}
}
