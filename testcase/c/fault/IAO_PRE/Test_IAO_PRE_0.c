void f1(int i) //��¼IAOǰ����Ϣ
{
	int b;
	b = 2/i;
}

void f2(int j) //��¼IAOǰ����Ϣ
{
	f1(j);
}

void f3()
{
	int b = 2;
	f1(b);
	f1(0);	//DEFECT, ����ǰ����Ϣ
	f2(2);
	f2(0); //DEFECT, ����ǰ����Ϣ
}
