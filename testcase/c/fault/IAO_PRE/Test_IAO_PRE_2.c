#include <stdlib.h>

void f1(int a) //��¼IAO������Ϣ
{
	div(10, a); 
}

void f2()
{
	int b = 0;
	f1(b); //DEFECT, ����IAO������Ϣ
}

void f3()
{
	int b = -2;
	f1(b+2); //DEFECT, ����IAO������Ϣ,
}
