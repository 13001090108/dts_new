# 1 "E:\\TESTCASE\\\345\215\225\345\205\203\345\206\205\\1.c"
# 1 "<built-in>"
# 1 "<command line>"





# 1 "E:/lib/Workspace/DTSEmbed_LSC\\cpp\\defines.txt" 1
# 35 "E:/lib/Workspace/DTSEmbed_LSC\\cpp\\defines.txt"
 typedef char * va_list;
# 7 "<command line>" 2
# 1 "E:\\TESTCASE\\\345\215\225\345\205\203\345\206\205\\1.c"
int g=1;
void f1() {
   a=g;
}
typedef struct {
   int a;
   char b;
}A;
typedef struct {
   A a;
   A b[10];
}B;
void f2(A a1,A* p1) {
   int x=p1->a;
   char y=a1.b;
   A a2 = a1;
   A* p2 = p1;
   A aa=f4();
   aa.a=1;
   aa.b='1';
   int c=aa.a;
   char d=aa.b;
   A bb=aa;
}
void f3() {
   int a[5]={1,2,3,4,5};
   int *a2 = a;
   int b=a[1];
   int c=a[b];
   a[1]=1;
   a[b]=2;
   a[b+1]=3;
   b=a[b];
}
A f4() {
   A a;
   a.a=2;
   a.b='2';

   return a;
}
void f5() {
   B bb;
   bb.a.a=1;
   bb.b[1].a=2;
   A* a=bb.b;
}
void f6(int *a) {
   int *b=a;
   int c=*a;
   *a=1;
   *(a+1)=2;
   a=NULL;
   *a=1;
}
void f7() {
   int i, sum;
   sum=0;
   for(i=0;i<10;i++) {
      sum+=i;
   }
   printf("%d %d",sum,i);

   sum=0;
   i=0;
   while(i<10) {
      sum+=i;
      i++;
   }
   printf("%d %d",sum,i);

   sum=0;
   i=0;
   do {
      sum+=i;
      i++;
   }while(i<10) ;
   printf("%d %d",sum,i);

   for(i=0;i<10;i++) {
      for(i=0;i<10;i++) {
         sum+=i;
      }
   }
}
void f8() {
   int b;

   int a1=1;
   a1++;
   b=a1;

   int a2=2;
   a2+=2;
   b=a2;

   int a3=3;
   a3=a3+3;
   b=a3;
}

void f9() {
   enum P { A=1, B, C};
   enum P x,y;
   x=A;
}

void f10() {
   char* a="aaaaaaaa";
   char* b="bbbbbbbb";

   strcpy(a+b,b);
}

void f11() {
   int x;
   if ((x=f()) > 100) {
      int y=1;
      int z=2;
      int p=y+z+3;
   }
}

void f12() {
   int a,b,c;
   a=1,b=2,c=a+b;
   a=b=c;
}
