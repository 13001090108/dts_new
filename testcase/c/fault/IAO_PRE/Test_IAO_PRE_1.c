int g1 ;

void f1() //��¼ǰ����Ϣ
{
	int b = 1/g1; 
}

void f2()
{
	g1 = 0;
	f1();	//DEFECT, ����ǰ����Ϣ
}

void f3(int i, int j) //��¼ǰ����Ϣi != 0
{
	int b = 1/i; 
}

void f4(int j) //��¼ǰ����Ϣj != 0
{
	f3(j, 5);
}

void f5()
{
	int i=0;
	f3(i, 4); //DEFECT, IAO
	f4(i); //DEFECT, IAO
}
