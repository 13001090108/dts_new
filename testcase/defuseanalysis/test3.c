//Ŀǰ�����⣺�ֲ��������˳�����ʹ����û������
void f1(int i){}

void f2()
{
	 int i;
	 i = 3;
	 f1(i);
	 if(1)
	 {
	 	 int i;
	 	 i = 3;
	 	 f1(i);
	 }
	 int j=i+5;
}
int main()
{
	return 0;
}