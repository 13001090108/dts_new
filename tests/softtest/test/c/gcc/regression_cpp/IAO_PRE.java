package softtest.test.c.gcc.regression_cpp;

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
public class IAO_PRE extends ModelTestBase{
	public IAO_PRE(String source,String compiletype, String result)
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
				//��������23
				//δͨ��������0
				//δͨ���������:0
				//////////////////////////0//////////////////////////////////////////////
				{
					"void ZX_IAO_FC_1_f1(int i) //��¼IAOǰ����Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	int b;"                                                              +"\n"+
							"	b = 2/i;"                                                            +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_1_f2(int j) //��¼IAOǰ����Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_1_f1(j);"                                                  +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_1_f3()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 2;"                                                          +"\n"+
							"	ZX_IAO_FC_1_f1(b);"                                                  +"\n"+
							"	ZX_IAO_FC_1_f1(0);	//DEFECT, ����ǰ����Ϣ"                                 +"\n"+
							"	ZX_IAO_FC_1_f2(2);"                                                  +"\n"+
							"	ZX_IAO_FC_1_f2(0); //DEFECT, ����ǰ����Ϣ"                                 +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				//////////////////////////1//////////////////////////////////////////////
				{
					"#include<math.h>"                                                     +"\n"+
							""                                                                     +"\n"+
							"int ZX_IAO_FC_2_g1 ;"                                                 +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f1() //��¼ǰ����Ϣ"                                       +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 1/ZX_IAO_FC_2_g1; "                                          +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_2_g1 = 0;"                                                 +"\n"+
							"	ZX_IAO_FC_2_f1();	//DEFECT, ����ǰ����Ϣ"                                  +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f3(int i, int j) //��¼ǰ����Ϣi != 0"                     +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 1/i; "                                                       +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f4(int j) //��¼ǰ����Ϣj != 0"                            +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_2_f3(j, 5);"                                               +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f5()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int i=0;"                                                            +"\n"+
							"	ZX_IAO_FC_2_f3(i, 4); //DEFECT, IAO"                                 +"\n"+
							"	ZX_IAO_FC_2_f4(i); //DEFECT, IAO"                                    +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				//////////////////////////2//////////////////////////////////////////////
				{
					"#include <stdlib.h>"                                                  +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f1(int a) //��¼IAO������Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	div(10, a); "                                                        +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 0;"                                                          +"\n"+
							"	ZX_IAO_FC_4_f1(b); //DEFECT, ����IAO������Ϣ"                              +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f3()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = -2;"                                                         +"\n"+
							"	ZX_IAO_FC_4_f1(b+2); //DEFECT, ����IAO������Ϣ,"                           +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},

				//////////////////////////3//////////////////////////////////////////////
				{
					"#include <stdlib.h>"                                                  +"\n"+
							""                                                                     +"\n"+
							"int ZX_IAO_FC_5_global;"                                              +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_5_f1() //��¼IAO������Ϣ[global != 0]"                       +"\n"+
							"{"                                                                    +"\n"+
							"	div(10, ZX_IAO_FC_5_global); "                                       +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_5_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_5_global = 0;"                                             +"\n"+
							"	ZX_IAO_FC_5_f1(); //DEFECT, ����IAO������Ϣ"                               +"\n"+
							"}"                                                                     
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},

				//////////////////////////4//////////////////////////////////////////////
				{
					"#include <stdlib.h>"                                                  +"\n"+
							""                                                                     +"\n"+
							"int ZX_IAO_FC_6_global;"                                              +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_6_f1(int a) //�����Ǹ��������Ǹ����ʽ����¼IAO������Ϣ[global != 0] "    +"\n"+
							"{"                                                                    +"\n"+
							"	if(a == 0)"                                                          +"\n"+
							"		div(10, ZX_IAO_FC_6_global); "                                      +"\n"+
							"	if(a == 1)"                                                          +"\n"+
							"		div(10, ZX_IAO_FC_6_global+3);"                                     +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_6_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_6_global = 0;"                                             +"\n"+
							"	ZX_IAO_FC_6_f1(0); //DEFECT, ������(Ф��)"                                +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},

				//////////////////////////5//////////////////////////////////////////////
				{
					"void func1(int i)"                                                    +"\n"+
							"{"                                                                    +"\n"+
							"    int j = i;"                                                       +"\n"+
							"}"                                                                    +"\n"+
							"int func2(int a)"                                                     +"\n"+
							"{"                                                                    +"\n"+
							"    int b = 13;"                                                      +"\n"+
							"    return b/a;       "                                               +"\n"+
							"}"                                                                    +"\n"+
							"void func(int x)"                                                     +"\n"+
							"{"                                                                    +"\n"+
							"    func2(x);"                                                        +"\n"+
							"    if(x != 0)"                                                       +"\n"+
							"        func1(x);                 "                                   +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"OK"
							,
				},

				

				//////////////////////////6//////////////////////////////////////////////
				{
					"int i;"                                                               +"\n"+
							"void f(){"                                                            +"\n"+
							"    int a=1/i;"                                                       +"\n"+
							"}"                                                                    +"\n"+
							"void f1(){"                                                           +"\n"+
							"    f();"                                                             +"\n"+
							"}"                                                                    +"\n"+
							"void f2(){"                                                           +"\n"+
							"    i=0;"                                                             +"\n"+
							"    f1();"                                                            +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},

				

				//////////////////////////7//////////////////////////////////////////////
				{//
					"#include <math.h>"                                                    +"\n"+
					""                                                                     +"\n"+
					"int g_val1 = 0;"                                                      +"\n"+
					"int g_val2 = 0;"                                                      +"\n"+
					""                                                                     +"\n"+
					"void func2();"                                                        +"\n"+
					"int func3();"                                                         +"\n"+
					""                                                                     +"\n"+
					"void func1()"                                                         +"\n"+
					"{"                                                                    +"\n"+
					"	func3();"                                                            +"\n"+
					"	func2();"                                                            +"\n"+
					"	func3();"                                                            +"\n"+
					"}"                                                                    +"\n"+
					""                                                                     +"\n"+
					"void func2()"                                                         +"\n"+
					"{"                                                                    +"\n"+
					"	g_val1 = g_val2 = 1;"                                                +"\n"+
					"}"                                                                    +"\n"+
					""                                                                     +"\n"+
					"int func3()"                                                          +"\n"+
					"{"                                                                    +"\n"+
					"	return atan2(g_val1, g_val2); //FP"                              +"\n"+
					"}"                                                                    
					,
					"gcc"
					,
					"OK"
					,
				},

				
				/////////////////////////8/////////////////////////////////////////////
				//jdh  ��Ҫ�޸�  zl
				{
					"#include <math.h>"                                                    +"\n"+
							""                                                                     +"\n"+
							"void func1(int var)"                                                  +"\n"+
							"{"                                                                    +"\n"+
							"	func2(var);"                                                         +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"int func2(int var)"                                                   +"\n"+
							"{"                                                                    +"\n"+
							"	if (var < 0) {"                                                      +"\n"+
							"		return func3(var); //DEFECT"                                        +"\n"+
							"	} else {"                                                            +"\n"+
							"		return 0;"                                                          +"\n"+
							"	}"                                                                   +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"int func3(int var)"                                                   +"\n"+
							"{"                                                                    +"\n"+
							"	return sqrt(var);"                                                   +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},

				/////////////////////////9/////////////////////////////////////////////
				{
					"void ZX_IAO_FC_1_f1(int i) //��¼IAOǰ����Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	int b;"                                                              +"\n"+
							"	b = 2/i;"                                                            +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_1_f2(int j) //��¼IAOǰ����Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_1_f1(j);"                                                  +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_1_f3()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 2;"                                                          +"\n"+
							"	ZX_IAO_FC_1_f1(b);"                                                  +"\n"+
							"	ZX_IAO_FC_1_f1(0);	//DEFECT, ����ǰ����Ϣ"                                 +"\n"+
							"	ZX_IAO_FC_1_f2(2);"                                                  +"\n"+
							"	ZX_IAO_FC_1_f2(0); //DEFECT, ����ǰ����Ϣ"                                 +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				/////////////////////////10/////////////////////////////////////////////
				{
					"#include<math.h>"                                                     +"\n"+
							""                                                                     +"\n"+
							"int ZX_IAO_FC_2_g1 ;"                                                 +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f1() //��¼ǰ����Ϣ"                                       +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 1/ZX_IAO_FC_2_g1; "                                          +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_2_g1 = 0;"                                                 +"\n"+
							"	ZX_IAO_FC_2_f1();	//DEFECT, ����ǰ����Ϣ"                                  +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f3(int i, int j) //��¼ǰ����Ϣi != 0"                     +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 1/i; "                                                       +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f4(int j) //��¼ǰ����Ϣj != 0"                            +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_2_f3(j, 5);"                                               +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f5()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int i=0;"                                                            +"\n"+
							"	ZX_IAO_FC_2_f3(i, 4); //DEFECT, IAO"                                 +"\n"+
							"	ZX_IAO_FC_2_f4(i); //DEFECT, IAO"                                    +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				
				/////////////////////////11/////////////////////////////////////////////
				{
					"#include <stdlib.h>"                                                  +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f1(int a) //��¼IAO������Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	div(10, a); "                                                        +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 0;"                                                          +"\n"+
							"	ZX_IAO_FC_4_f1(b); //DEFECT, ����IAO������Ϣ"                              +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f3()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = -2;"                                                         +"\n"+
							"	ZX_IAO_FC_4_f1(b+2); //DEFECT, ����IAO������Ϣ,"                           +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},

				/////////////////////////12/////////////////////////////////////////////
				{
					"#include <stdlib.h>"                                                  +"\n"+
							""                                                                     +"\n"+
							"int ZX_IAO_FC_6_global;"                                              +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_6_f1(int a) //�����Ǹ��������Ǹ����ʽ����¼IAO������Ϣ[global != 0] "    +"\n"+
							"{"                                                                    +"\n"+
							"	if(a == 0)"                                                          +"\n"+
							"		div(10, ZX_IAO_FC_6_global); "                                      +"\n"+
							"	if(a == 1)"                                                          +"\n"+
							"		div(10, ZX_IAO_FC_6_global+3);"                                     +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_6_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_6_global = 0;"                                             +"\n"+
							"	ZX_IAO_FC_6_f1(0); //DEFECT, ������(Ф��)"                                +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				/////////////////////////13/////////////////////////////////////////////
				{
					"#include <stdlib.h>"                                                  +"\n"+
							""                                                                     +"\n"+
							"int ZX_IAO_FC_6_global;"                                              +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_6_f1(int a) //�����Ǹ��������Ǹ����ʽ����¼IAO������Ϣ[global != 0] "    +"\n"+
							"{"                                                                    +"\n"+
							"	if(a == 0)"                                                          +"\n"+
							"		div(10, ZX_IAO_FC_6_global); "                                      +"\n"+
							"	if(a == 1)"                                                          +"\n"+
							"		div(10, ZX_IAO_FC_6_global+3);"                                     +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_6_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_6_global = 0;"                                             +"\n"+
							"	ZX_IAO_FC_6_f1(0); //DEFECT, ������(Ф��)"                                +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				/////////////////////////14/////////////////////////////////////////////
				{
					"int jhb_iao_2_f1(int t){"                                             +"\n"+
							"		int x=10;"                                                          +"\n"+
							"		return (x/t);"                                                      +"\n"+
							"		"                                                                   +"\n"+
							"	}"                                                                   +"\n"+
							"void jhb_iao_2_f2(){"                                                 +"\n"+
							"		int i;"                                                             +"\n"+
							"		i=jhb_iao_2_f1(0);   //DEFECT"                                      +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				////////////////////////15////////////////////////////////////////////
				{
					//���������ʱ��������ص����������޷���ȷ
					//noted by zhouhb
					//jdh  �ݲ�����
					"int jhb_iao_3_f1(int b){"                                             +"\n"+
					"	return 2*b;"                                                         +"\n"+
					"}"                                                                    +"\n"+
					"int jhb_iao_3_f2(int t,int m){"                                       +"\n"+
					"		int x=jhb_iao_3_f1(m);"                                             +"\n"+
					"		return (x/(t-x));"                                                  +"\n"+
					"}"                                                                    +"\n"+
					"void jhb_iao_3_f1(){"                                                 +"\n"+
					"		int i;"                                                             +"\n"+
					"		i=jhb_iao_3_f2(10,5);  //DEFECT"                                    +"\n"+
					"}"                                                                    
					,
					"gcc"
					,
					"OK"
					,
				},
				/////////////////////////16/////////////////////////////////////////////
				{
					//������������ȫ�ֱ������ʱ����Լ������������������
					//jdh  �ݲ�����
					"int t=5;"                                                            +"\n"+
					"int jhb_iao_3_f2(int x){"                                             +"\n"+
					"		"                                                                   +"\n"+
					"		return (x/(t-x));"                                                  +"\n"+
					"}"                                                                    +"\n"+
					"void jhb_iao_3_f1(){"                                                 +"\n"+
					"		int i;"                                                             +"\n"+
					"		i=jhb_iao_3_f2(5);  //DEFECT"                                       +"\n"+
					"}"                                                                    
					,
					"gcc"
					,
					"OK"
					,
				},
				/////////////////////////17/////////////////////////////////////////////
				{
					"int jhb_iao_2_f1(int t){"                                             +"\n"+
							"		int x=10;"                                                          +"\n"+
							"		return (x/t);"                                                      +"\n"+
							"		"                                                                   +"\n"+
							"	}"                                                                   +"\n"+
							"void jhb_iao_2_f2(){"                                                 +"\n"+
							"		int i;"                                                             +"\n"+
							"		i=jhb_iao_2_f1(0);   //DEFECT"                                      +"\n"+
							"}"                                                                    
							,
							"gcc","IAO_PRE"
							,
				},
				
				
				
				/////////////////////////18/////////////////////////////////////////////
				{
					"void ZX_IAO_FC_1_f1(int i) //��¼IAOǰ����Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	int b;"                                                              +"\n"+
							"	b = 2/i;"                                                            +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_1_f2(int j) //��¼IAOǰ����Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_1_f1(j);"                                                  +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_1_f3()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 2;"                                                          +"\n"+
							"	ZX_IAO_FC_1_f1(b);"                                                  +"\n"+
							"	ZX_IAO_FC_1_f1(0);	//DEFECT, ����ǰ����Ϣ"                                 +"\n"+
							"	ZX_IAO_FC_1_f2(2);"                                                  +"\n"+
							"	ZX_IAO_FC_1_f2(0); //DEFECT, ����ǰ����Ϣ"                                 +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				/////////////////////////19/////////////////////////////////////////////
				{
					"#include<math.h>"                                                     +"\n"+
							""                                                                     +"\n"+
							"int ZX_IAO_FC_2_g1 ;"                                                 +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f1() //��¼ǰ����Ϣ"                                       +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 1/ZX_IAO_FC_2_g1; "                                          +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_2_g1 = 0;"                                                 +"\n"+
							"	ZX_IAO_FC_2_f1();	//DEFECT, ����ǰ����Ϣ"                                  +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f3(int i, int j) //��¼ǰ����Ϣi != 0"                     +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 1/i; "                                                       +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f4(int j) //��¼ǰ����Ϣj != 0"                            +"\n"+
							"{"                                                                    +"\n"+
							"	ZX_IAO_FC_2_f3(j, 5);"                                               +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_2_f5()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int i=0;"                                                            +"\n"+
							"	ZX_IAO_FC_2_f3(i, 4); //DEFECT, IAO"                                 +"\n"+
							"	ZX_IAO_FC_2_f4(i); //DEFECT, IAO"                                    +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				

				////////////////////////20/////////////////////////////////////////////
				{
					"#include <stdlib.h>"                                                  +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f1(int a) //��¼IAO������Ϣ"                               +"\n"+
							"{"                                                                    +"\n"+
							"	div(10, a); "                                                        +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f2()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = 0;"                                                          +"\n"+
							"	ZX_IAO_FC_4_f1(b); //DEFECT, ����IAO������Ϣ"                              +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void ZX_IAO_FC_4_f3()"                                                +"\n"+
							"{"                                                                    +"\n"+
							"	int b = -2;"                                                         +"\n"+
							"	ZX_IAO_FC_4_f1(b+2); //DEFECT, ����IAO������Ϣ,"                           +"\n"+
							"}"                                                                    
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				//////////////////////////////////////////////21/////////////////////////////////////////////
				{
					"int aa(){"                                                            +"\n"+
							"	return 0;"                                                           +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"int ff(int b){"                                                           +"\n"+
							"	  int c;"                                                              +"\n"+
							"    c=1/b;"                                                           +"\n"+
							"    return c;"                                                        +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void hh(){"                                                           +"\n"+
							"	 int h;"                                                     +"\n"+
							"	 h=ff(aa());"                                                     +"\n"+
							"} "                                                                   
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},
				//////////////////////////////////////22/////////////////////////////////////////////////////
				//jdh  ��Ҫ�޸�  zl
				//zl,20111102��zl�����޸�
				{
					"struct aa{"                                                           +"\n"+
							"int x;"                                                             +"\n"+
							"};"                                                                   +"\n"+
							""                                                                     +"\n"+
							"int ff(int b){"                                                       +"\n"+
							"	int c;"                                                              +"\n"+
							"    c=1/b;"                                                           +"\n"+
							"    return c;"                                                        +"\n"+
							"}"                                                                    +"\n"+
							""                                                                     +"\n"+
							"void hh(){"                                                           +"\n"+
							"	int h;"                                                              +"\n"+
							"    struct aa y; y.x=0;"                                                            +"\n"+
							"    h=ff(y.x); "                                                      +"\n"+
							"} "                                                                   
							,
							"gcc"
							,
							"IAO_PRE"
							,
				},


		});
	}
}
