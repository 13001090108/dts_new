#include <stdlib.h>

int global;

void f1() //��¼IAO������Ϣ[global != 0]
{
	div(10, global); 
}

void f2()
{
	global = 0;
	f1(); //DEFECT, ����IAO������Ϣ
}

void f3() //��¼IAO������Ϣ[global != 0]
{
	f1();
}

void f4()
{
	global = 0;
}

void f5()
{
	f4();
	f3(); //DEFECT 
}
