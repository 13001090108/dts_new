# 1 "E:\\TESTCASE\\\345\215\225\345\205\203\345\206\205\\classic100inC\\001-010\\001.c"
# 1 "<built-in>"
# 1 "<command line>"





# 1 "E:/lib/Workspace/DTSEmbed_LSC\\cpp\\defines.txt" 1
# 35 "E:/lib/Workspace/DTSEmbed_LSC\\cpp\\defines.txt"
 typedef char * va_list;
# 7 "<command line>" 2
# 1 "E:\\TESTCASE\\\345\215\225\345\205\203\345\206\205\\classic100inC\\001-010\\001.c"



main()
{
int i,j,k;
printf("\n");
for(i=1;i<5;i++)
　for(j=1;j<5;j++)　
　　for (k=1;k<5;k++)
　　　{
　　　　if (i!=k&&i!=j&&j!=k)
　　　　printf("%d,%d,%d\n",i,j,k);
　　　}
}
