#include <stdlib.h>

int global;

void f1(int a) //�����Ǹ��������Ǹ����ʽ����¼IAO������Ϣ[global != 0] 
{
	if(a == 0)
		div(10, global); 
	if(a == 1)
		div(10, global+3);
}

void f2()
{
	global = 0;
	f1(0); //DEFECT
}
