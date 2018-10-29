package softtest.test.c.gcc.regression;

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
public class MLF extends ModelTestBase {
	public MLF(String source,String compiletype, String result)
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
		LIB_SUMMARYS_PATH="gcc_lib/mm_summary.xml";
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
		            "#include \"stdlib.h\""                                                  +"\n"+
		            "void ghx_mlf_3_f3()"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "	int *m=NULL;"                                                        +"\n"+
		            "	if((m=(int*)malloc(100))!=NULL)"                                     +"\n"+
		            "	{"                                                                   +"\n"+
		            "		return;//DEFECT"                                                    +"\n"+
		            "	}"                                                                   +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  1   ///////////////////	
		            {
		            "#include \"stdlib.h\""                                                  +"\n"+
		            "void ghx_mlf_4_f4(int i)"                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "	int a[5]={4,4,4,4,4};"                                              +"\n"+
		            "	int* m4=NULL;"                                                       +"\n"+
		            "	if(i>0){"                                                            +"\n"+
		            "		m4=a;"                                                              +"\n"+
		            "	}else{"                                                              +"\n"+
		            "		m4=(int*)malloc(100);"                                              +"\n"+
		            "	}"                                                                   +"\n"+
		            "	if(i>0)"                                                     +"\n"+
		            "	{"                                                                   +"\n"+
		            "		free(m4);"                                                          +"\n"+
		            "	}"                                                                   +"\n"+
		            "}"                                                                    +"\n"+
		            "void ghx_mlf_4_f5(int i)"                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  2   ///////////////////	
		            {
		            "#include <malloc.h>"                                                  +"\n"+
		            "#include <stdio.h>"                                                   +"\n"+
		            "void ghx_mlf_5_f5()"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "char *p = (char*)malloc(100);"                                        +"\n"+
		            ""                                                                     +"\n"+
		            "delete(p); //DEFECT"                                                  +"\n"+
		            "  "                                                                   +"\n"+
		            "}"                                                                    +"\n"+
		            "void ghx_mlf_5_f1(int i)"                                             +"\n"+
		            "{"                                                                    +"\n"+
		            "char *p=(char*)malloc(100);"                                          +"\n"+
		            "free(p);"                                                             +"\n"+
		            "if(i>0)"                                                              +"\n"+
		            "free(p);//DEFECT"                                                     +"\n"+
		            "}"                                                                    +"\n"+
		            "void ghx_mlf_5_f2()"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "char *p = (char*)malloc(100);"                                        +"\n"+
		            ""                                                                     +"\n"+
		            "free(p); //FP"                                                        +"\n"+
		            "  "                                                                   +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  3   ///////////////////	
		            {
		            "#include \"stdio.h\""                                                   +"\n"+
		            "#include \"stdlib.h\""                                                  +"\n"+
		            "void ghx_mlf_6_f6()"                                                  +"\n"+
		            "{"                                                                    +"\n"+
		            "int i;"                                                               +"\n"+
		            "char* p2 = (char*)malloc( 5*sizeof(char));"                           +"\n"+
		            ""                                                                     +"\n"+
		            "for( i=0; i< 5; i++ )"                                                +"\n"+
		            "{"                                                                    +"\n"+
		            "printf(\"abc\");"                                                       +"\n"+
		            "}"                                                                    +"\n"+
		            "free (p2);//DEFECT"                                                   +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  4   ///////////////////	
		            {
		            	"#include<stdlib.h>"                                                  +"\n"+
		            "void fun1(int flag,int *p){"                                          +"\n"+
		            "	if(flag)"                                                            +"\n"+
		            "		free(p);"                                                           +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    +"\n"+
		            "void fun2(int *p){"                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "		free(p);"                                                           +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    +"\n"+
		            "void f(int flag){"                                                    +"\n"+
		            "	int *p=malloc(sizeof(int));"                                         +"\n"+
		            "	int *q=malloc(sizeof(int));"                                         +"\n"+
		            "	fun1(flag,p);  //DEFECT"                                             +"\n"+
		            "	fun2(q);   //FP"                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  5   ///////////////////	
		            {
		            "#include <malloc.h>"                                                  +"\n"+
		            "#include <stdio.h>"                                                   +"\n"+
		            "void jhb_mlf_4_f1(unsigned int i)"                                    +"\n"+
		            "{"                                                                    +"\n"+
		            "char *p = (char*)malloc(12);"                                         +"\n"+
		            "if(i>0) {"                                                            +"\n"+
		            "free(p); "                                                            +"\n"+
		            "}"                                                                    +"\n"+
		            "return;   //DEFECT"                                                       +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  6   ///////////////////	
		            {
		            "#include <malloc.h>"                                                  +"\n"+
		            "#include <stdio.h>"                                                   +"\n"+
		            "void f(unsigned int i){"                                              +"\n"+
		            "	int *memleak_error=malloc(sizeof(int));"                             +"\n"+
		            "    if (i>0) return;"                                                 +"\n"+
		            "	free (memleak_error);   //DEFECT"                                        +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  7   ///////////////////	
		            {
		            "void fun2(int *p){"                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "		free(p);"                                                           +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    +"\n"+
		            "void fun3(int *p){"                                                   +"\n"+
		            "	fun2(p);"                                                            +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f(int flag){"                                                    +"\n"+
		            "	int *p=malloc(sizeof(int));"                                         +"\n"+
		            "	"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "	fun3(p);   //FP"                                                     +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  8   ///////////////////	
		            {
		            "int flag;"                                                            +"\n"+
		            "void fun2(int *p){"                                                   +"\n"+
		            "        if(p)"                                                        +"\n"+
		            "		   free(p);"                                                        +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f(){"                                                            +"\n"+
		            "	int *p=malloc(sizeof(int));"                                         +"\n"+
		            "	fun2(p);"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
	/////////////////  9   ///////////////////	
		            {
		            	"#include<stdlib.h>"                                                  +"\n"+
		            "   void foo()"                                                        +"\n"+
		            "   {"                                                                 +"\n"+
		            "     int *ptr = malloc(sizeof(int));"                                 +"\n"+
		            "     *ptr = 25;"                                                      +"\n"+
		            "     ptr = malloc(sizeof(int)); //DEFECT"                             +"\n"+
		            "     *ptr = 35;"                                                      +"\n"+
		            "  } //"                                                               
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  10   ///////////////////	
		            {
		            "#include<stdlib.h>"                                                  +"\n"+	
		            "#include <assert.h>"                                                  +"\n"+
		            "#include <stdio.h>"                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "void zk_mlf_2_f1(int flag,char *p)"                                   +"\n"+
		            "{   "                                                                 +"\n"+
		            "     if(flag)"                                                        +"\n"+
		            "         free(p);"                                                    +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void zk_mlf_2_f2(int flag)"                                           +"\n"+
		            "{"                                                                    +"\n"+
		            "	char *buf = malloc(10*sizeof(char));"                                +"\n"+
		            ""                                                                     +"\n"+
		            "	zk_mlf_2_f1(flag,buf); //DEFECT"                                     +"\n"+
		            ""                                                                     +"\n"+
		            "	return;"                                                             +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  11   ///////////////////	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            "int* func()"                                                          +"\n"+
		            "{"                                                                    +"\n"+
		            "	int *ptr = (int*)malloc(sizeof(int));"                               +"\n"+
		            ""                                                                     +"\n"+
		            "	return ptr ? ptr : (int*)malloc(sizeof(int));"                       +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            //"MLF"
		            "OK"
		            ,
		            },
	/////////////////  12  ///////////////////	
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "typedef struct{"                                                      +"\n"+
		            "   char a[12];"                                                       +"\n"+
		            "   char b[34];"                                                       +"\n"+
		            "   int i;"                                                            +"\n"+
		            "   int *p;"                                                           +"\n"+
		            "}S;"                                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "void f(){"                                                            +"\n"+
		            "   "                                                                  +"\n"+
		            "   S  *s;"                                                            +"\n"+
		            "   s->p=(int*)malloc(10);"                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  13   ///////////////////	
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "typedef struct{"                                                   +"\n"+
		            "   char a[12];"                                                       +"\n"+
		            "   char b[34];"                                                       +"\n"+
		            "   int i;"                                                            +"\n"+
		            "   char *p;"                                                          +"\n"+
		            "}S;"                                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "char* f(){"                                                           +"\n"+
		            "   "                                                                  +"\n"+
		            "   return (char*)malloc(10);"                                         +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f1(){"                                                           +"\n"+
		            "   S  *s;"                                                            +"\n"+
		            "   s->p=f(); "                                                        +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  14  ///////////////////	
		            //xwt:������MLF��ʶ��ȫ�ֱ����������ж�ԭ��MLFPost���Ķ���������������
		            //xwt:�������������ԣ�ֻ������variable��ȱ�ٴ���״̬����ϢVariableNameDeclaration����Ҫ�Ķ��ĵط��࣬������
		            /*��ʱ�벻����ȫ�����ķ�����
		             * ����������һ������VariableNameDeclaration���˱������﷨���ϵ�λ����Ϣ�����ڵ���Ϣ
		             * 1��״̬��������ת����ҪVariableNameDeclaration����ΪҪ�õ��������﷨���ڵ���Ϣ������������Ϣ��������
		             *    �еĲ��ұȶԴ洢���ƾ�Χ��VariableNameDeclarationչ����Ҫ�Ķ��ǲ����ǵ�
		             * 2��һ���������������MethodMMFeatureȷʵֻҪvariable��Ϣ���ɣ�û��Ҫȥ��VariableNameDeclaration�ͽ�
		             *    ����Ϣ����Ϊ�ǿ纯���ķ��䣬����˵�����Ͳ��ò��ں���Щλ����Ϣ��ֻҪ֪����ĳĳ���������ľ����ˣ����
		             *    ��ͨ�������򷵻�ֵ�����䣬���Ǽ�¼���������λ����Ϣû���������������ڷ�ȫ�ֱ�������Ĵ󲿷������
		             *    ����Ҳ�����ࡣ
		             * 3�����Թ���f1�е�f()�ڵ��������﷨����Ϊ�ҵ�variable�Ľڵ���Ϣ����״̬�����ڵ��������������ġ���ô���Ѿ�
		             *    ����������ˣ���ģ��֮���ּ���Ī�����������ԡ����Ҳ����鷳����ɵĺ��Ҳδ֪
		             * ���Ժ�������ǣ�*/
		            //xwt:�������������ɻ��Ҿ����ǲ��ǻ��ǵ���MLFPostģʽ��������⣬֮ǰȡ��post��Ϊʲô��
		            {
		            "#include <stdio.h>"                                                   +"\n"+
		            "#include <stdlib.h>"                                                  +"\n"+
		            ""                                                                     +"\n"+
		            "typedef struct {"                                                     +"\n"+
		            "   char a[12];"                                                       +"\n"+
		            "   char b[34];"                                                       +"\n"+
		            "   int i;"                                                            +"\n"+
		            "   char *p;"                                                          +"\n"+
		            "}S;"                                                                  +"\n"+
		            "S  s;"                                                                +"\n"+
		            "char* f(){"                                                           +"\n"+
		            "   "                                                                  +"\n"+
		            "   s.p=(char*)malloc(10);"                                            +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f1(){"                                                           +"\n"+
		            "   f();"                                                              +"\n"+
		            "   s.p=(char*)malloc(10);"                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "MLF"
		            ,
		            },
	/////////////////  15   ///////////////////	
	
		            {
		            "#include <stdlib.h>"                                                  +"\n"+
		            "int flag;"                                                            +"\n"+
		            "void fun2(int *p){"                                                   +"\n"+
		            "        if(p)"                                                        +"\n"+
		            "		   free(p);"                                                        +"\n"+
		            ""                                                                     +"\n"+
		            "}"                                                                    +"\n"+
		            ""                                                                     +"\n"+
		            "void f(){"                                                            +"\n"+
		            "	int *p=(int*)malloc(1);"                                             +"\n"+
		            "	fun2(p);"                                                            +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
//////////////////////////16//////////////////////////////////////////////
					//jdh������©�����⣬����ѭ���ṹ��Ӧ�ÿ��Խ��
					//st: �ظ�����ռ����⣬Ŀǰδ����
		            {
		            "#include \"stdlib.h\""                                                  +"\n"+
		            "void *ghx_mlf_1_f1()"                                                 +"\n"+
		            "{"                                                                    +"\n"+
		            "	int cc;"                                                             +"\n"+
		            "	char *ff;"                                                           +"\n"+
		            ""                                                                     +"\n"+
		            "	for (cc=0; cc<500; cc++)"                                            +"\n"+
		            "	{"                                                                   +"\n"+
		            "		ff=(char*)malloc(10);"                                              +"\n"+
		            "	}"                                                                   +"\n"+
		            ""                                                                     +"\n"+
		            "	free (ff);//DEFECT"                                                  +"\n"+
		            "	return 0;"                                                           +"\n"+
		            "}"                                                                    
		            ,
		            "gcc"
		            ,
		            "OK"
		            ,
		            },
/////////////////  17   ///////////////////	
	            {
	            "#include<stdlib.h>"                                                   +"\n"+
	            "int * xfree(int * p){"                                                +"\n"+
	            "    free(p);"                                                         +"\n"+
	            "    return NULL;    "                                                 +"\n"+
	            "}"                                                                    +"\n"+
	            "void func1(){"                                                        +"\n"+
	            "    int *p = (int *)malloc(10);"                                      +"\n"+
	            "    p = xfree(p);    "                                                +"\n"+
	            "}"                                                                    
	            ,
	            "gcc"
	            ,
	            "OK"
	            ,
	            },	
/////////////////  18   ///////////////////	
	            {
	            "#include <stdlib.h>"                                                  +"\n"+
	            "int foo_4_4(int i, int j) {"                                          +"\n"+
	            "    char * p[10];"                                                     +"\n"+
	            "    if (i)"                                                           +"\n"+
	            "    p[0] = 0;"                                                        +"\n"+
	            "    if(j)"                                                            +"\n"+
	            "         p[0] = (char*)malloc(sizeof(char));"                         +"\n"+
	            "    return 0;"                                                        +"\n"+
	            "}"                                                                    
	            ,
	            "gcc"
	            ,
	            "MLF"
	            ,
	            },
/////////////////  19   ///////////////////	
	            {
	            "#include <stdlib.h>"                                                  +"\n"+
	            ""                                                                     +"\n"+
	            "void foo_6_1(char *p) {"                                              +"\n"+
	            "    p = (char*)malloc(sizeof(char));"                                 +"\n"+
	            "}"                                                                    +"\n"+
	            "int bar_6_1(int argc, char **argv) {"                                 +"\n"+
	            "    char *p;"                                                         +"\n"+
	            "    foo_6_1(p);"                                                      +"\n"+
	            "    return 0;"                                                        +"\n"+
	            "}"                                                                    
	            ,
	            "gcc"
	            ,
	            "MLF"
	            ,
	            },
/////////////////  20   ///////////////////	
	            {
	            "#include <stdlib.h>"                                                  +"\n"+
	            ""                                                                     +"\n"+
	            "char *p_6_2;"                                                         +"\n"+
	            "void foo_6_2() {"                                                     +"\n"+
	            "    p_6_2 =(char*)malloc(sizeof(char));"                              +"\n"+
	            "}"                                                                    +"\n"+
	            "int main_6_2(int argc, char **argv) {"                                +"\n"+
	            "    foo_6_2();"                                                       +"\n"+
	            "    p_6_2 =(char*)malloc(sizeof(char));"                              +"\n"+
	            "    return 0;"                                                        +"\n"+
	            "}"                                                                    
	            ,
	            "gcc"
	            ,
	            "MLF"
	            ,
	            },
/////////////////  21   ///////////////////	
	            {
	            "#include <stdlib.h>"                                                  +"\n"+
	            ""                                                                     +"\n"+
	            "char *p_7_2;"                                                         +"\n"+
	            "void foo_7_2() {"                                                     +"\n"+
	            "    p_7_2 =(char*)malloc(sizeof(char));"                              +"\n"+
	            "}"                                                                    +"\n"+
	            "void bar_7_2 () {"                                                    +"\n"+
	            "    foo_7_2 ();"                                                      +"\n"+
	            "}"                                                                    +"\n"+
	            "int main_7_2 (int argc, char **argv) {"                               +"\n"+
	            "    bar_7_2();"                                                       +"\n"+
	            "    p_7_2 =(char*)malloc(sizeof(char));"                              +"\n"+
	            "    return 0;"                                                        +"\n"+
	            "}"                                                                    
	            ,
	            "gcc"
	            ,
	            "MLF"
	            ,
	            },



		 });
	 }
}
