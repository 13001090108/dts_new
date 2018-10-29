package softtest.test.android.gcc;

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
public class NPD_PRE extends ModelTestBase {
	public NPD_PRE(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}

	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/fault/NPD_PRE-0.1.xml";
		FSMMachine fsm = FSMLoader.loadXML(fsmPath);
		fsm.setType("fault");
		//ÿ�μ����Զ���ǰ�����һ��ԭ����fsms
		FSMAnalysisVisitor.clearFSMS();
		FSMAnalysisVisitor.addFSMS(fsm);
		
		//���ؿ⺯��ժҪ
		LIB_SUMMARYS_PATH="gcc_lib/npd_summary.xml";
		libManager.loadSingleLibFile(LIB_SUMMARYS_PATH);
		libManager.loadSingleLibFile("gcc_lib/lib_summary.xml");
		Set<MethodNameDeclaration> libDecls = libManager.compileLib(pre.getLibIncludes());
		interContext = InterContext.getInstance();
		interContext.addLibMethodDecl(libDecls);
	}
	 @Parameters
	 public static Collection<Object[]> testcaseAndResults()
	 {
		 return Arrays.asList(new Object[][] {

					/////////////////  0 /////////////////////////	
				 //�������android���̺�DTS�����ܵ�ʱ�򱨳���OOB�����ڲ�����������������  -- nmh
		            {
		                "void f()"                                                             +"\n"+
		                "{"                                                                    +"\n"+
		                "    char *tag;"                                                       +"\n"+
		                "        if (!tag)"                                                    +"\n"+
		                "        tag = \"\";"                                                    +"\n"+
		                "		strlen(tag);"                                                       +"\n"+
		                "}"                                                                    
		                ,
						"gcc"
						,
						"OK"
						,
					},
					/////////////////  1 /////////////////////////	
					 //�������android���̺�DTS�����ܵ�ʱ�򱨳���OOB�����ڲ�����������������  -- nmh
			            {
			                "void f()"                                                             +"\n"+
			                "{"                                                                    +"\n"+
			                "    char *tag;"                                                       +"\n"+
			                "        if (!tag)"                                                    +"\n"+
			                "        tag = \"\";"                                                    +"\n"+
			                "		strcmp(tag, \"HTC_RIL\");"                                                       +"\n"+
			                "}"                                                                    
			                ,
							"gcc"
							,
							"OK"
							,
						},					

 });
	 }
}
