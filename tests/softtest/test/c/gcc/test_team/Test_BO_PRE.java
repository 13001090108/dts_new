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
public class Test_BO_PRE extends ModelTestBase{
	public Test_BO_PRE(String source,String compiletype, String result)
	{
		super(source, compiletype, result);
	}
	
	@BeforeClass
	public static void setUpBaseChild()
	{
		fsmPath="softtest/rules/gcc/fault/BO_PRE-0.1.xml";
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
	

	///////////////  0   ///////////////////	
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <string.h>"                                                  +"\n"+
		            "void f1(char s[])"                                                    +"\n"+
		            "{"                                                                    +"\n"+
		            "    gets(s);//DEFECT//chh ����BO_PRE��get�������жϺͱ����޹أ�����Ҫ���ݺ���ժҪ,����ΪBO"                                                         +"\n"+
		            "}"                                                                    +"\n"+
		            "void f()"                                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "     char s[10];"                                                     +"\n"+
		            "     f1(s);//DEFECT"                                                          +"\n"+
		            "     strcat(s, \"a\");"                                                 +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },

	/////////////////  1   ///////////////////	
//		            {
//		            "#include <string.h>"                                                  +"\n"+
//		            "void f1(char* buf)"                                                   +"\n"+
//		            "{    "                                                                +"\n"+
//		            "    char s1[] = \"Hello \";"                                            +"\n"+
//		            "    strcpy(buf, s1);"                                                 +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f2(char* buf)"                                                   +"\n"+
//		            "{    "                                                                +"\n"+
//		            "    f1(buf);"                                                         +"\n"+
//		            "    char s2[] = \"world!\";"                                            +"\n"+
//		            "    strcat(buf, s2);"                                                 +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f3()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    char buf[10];"                                                    +"\n"+
//		            "    f2(buf);//DEFECT"                                                 +"\n"+
//		            "    char s3[] = \"OK!\";"                                               +"\n"+
//		            "    strcat(buf, s3);"                                                 +"\n"+
//		            "}"                                                                    
//		            ,
//		            "gcc"
//		            ,
//		            "BO_PRE"
//		            ,
//		            },
//
//
//	/////////////////  2   ///////////////////	
//		            {
//		            "#include <string.h>"                                                  +"\n"+
//		            "#include <stdio.h>"                                                   +"\n"+
//		            "void f1(char buf[])"                                                  +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    strcat(buf, \"XX\");//DEFECT"                                       +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f2(char buf[])"                                                  +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    strcat(buf, \"AAAA\");"                                             +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f3()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    char buf[10];"                                                    +"\n"+
//		            "    gets(buf);"                                                       +"\n"+
//		            "    if(strlen(buf) < 3)"                                              +"\n"+
//		            "        f2(buf);"                                                     +"\n"+
//		            "    else"                                                             +"\n"+
//		            "        f1(buf);//DEFECT"                                             +"\n"+
//		            "}"                                                                    
//		            ,
//		            "gcc"
//		            ,
//		            "BO_PRE"
//		            ,
//		            },
/*
 * ��������1,2��������ʵ�ϣ�Ŀǰϵͳ�������������û�ﵽ��������Ҫ��ľ�ȷ��
 * ����1����f2����f1��ϵͳ����֪��f1��buf�����ʲôӰ�죬buf��dimain��δ�ı�,������f3����f2ʱ��f3�����buf���ÿ�
 * ��Ϊ10���ж�f2��strcat����û�� ���
 * ����2:��f3�У�if�жϺ�����Ŀ���may��must���buf��domain���޹�����������gets��������bufд��������ݱ���ʱδ֪
 * ���������ж�f2��f1�Ƿ��������ʱbuf�Ŀռ�����10����
*/
	
//		        	/////////////////  3   ///////////////////	
//		            {
//		            "#include <string.h>"                                                  +"\n"+
//		            "char s[8];"                                                           +"\n"+
//		            "void f1()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    strcat(s, \"12\");     "                                            +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f2()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    f1();"                                                            +"\n"+
//		            "    strcat(s, \"54321\");//DEFECT     "                                         +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f3()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "     strcat(s, \"98\"); "                                               +"\n"+
//		            "     f2();//DEFECT          "                                                 +"\n"+
//		            "}"                                                                    
//		            ,
//		            "gcc"
//		            ,
//		            "BO_PRE"
//		            ,
//		            },
	/////////////////  4   ///////////////////	
//		            {
//		            "#include <string.h>"                                                  +"\n"+
//		            "char s[8];"                                                           +"\n"+
//		            "void f1()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    strcat(s, \"12345\");//DEFECT     "                                         +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f2()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    f1();//DEFECT"                                                            +"\n"+
//		            "    strcat(s, \"54321\");     "                                         +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f3()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "     strcat(s, \"9876\"); "                                             +"\n"+
//		            "     f2(); //DEFECT         "                                                 +"\n"+
//		            "}"                                                                    
//		            ,
//		            "gcc"
//		            ,
//		            "BO_PRE"
//		            ,
//		            },
/*
 * chh 3,4ͬ����1һ��������3,4Ҳ������strcat���������ý��û���ڱ�����domain�����ֳ��������½��������ж����
 * Ϊ������ȫ�ֱ����ĺ���ժҪ���ݽ��������������޸�����		            
 */
		            {
			            "#include <string.h>"                                                  +"\n"+
			            "char s[8];"                                                           +"\n"+
			            "void f1()"                                                            +"\n"+
			            "{"                                                                    +"\n"+
			            "    strcpy(s, \"12345678\");     "                                            +"\n"+
			            "}"                                                                    +"\n"+
			            "void f2()"                                                            +"\n"+
			            "{"                                                                    +"\n"+
			            "    f1();"                                                            +"\n"+
			            " "                                         +"\n"+
			            "}"                                                                    +"\n"+
			            "void f3()"                                                            +"\n"+
			            "{"                                                                    +"\n"+
			            " "                                               +"\n"+
			            "     f2();//DEFECT  "                                                 +"\n"+
			            "}"                                                                    
			            ,
			            "gcc"
			            ,
			            "BO_PRE"
			            ,
			            },				 				 
	/////////////////  5   ///////////////////	
//		            {
//		            "#include <string.h>"                                                  +"\n"+
//		            "char a[20];"                                                          +"\n"+
//		            "char b[5];"                                                           +"\n"+
//		            ""                                                                     +"\n"+
//		            "void f1()"                                                            +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    strcat(a, b);"                                                    +"\n"+
//		            "}"                                                                    +"\n"+
//		            "void f()"                                                             +"\n"+
//		            "{"                                                                    +"\n"+
//		            "    int i;"                                                           +"\n"+
//		            "    for(i=0; i<5; i++)"                                               +"\n"+
//		            "        f1(); //DEFECT   "                                                    +"\n"+
//		            "}"                                                                    
//		            ,
//		            "gcc"
//		            ,
//		            "BO_PRE"
//		            ,
//		            },

/*
 * ����5�޷��жϣ�����ͬ����1���ڵڶ���ѭ����ʱ�򣬵�һ��f1�ķ�����û�ж�a��domain��ɸı�
 */
 		            
	

		 });
	 }
}
