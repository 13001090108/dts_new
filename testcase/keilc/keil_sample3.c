/*==============================================================
1msʱ�� ���ʽ��������һ����ռʽ���񣬶������ʽ����
���ߣ�shadow.hu
===============================================================*/
#include<reg52.h>
#define uchar  unsigned char
#define ushort unsigned short
#define SCH_MAX_TASKS 9
#define ERROR_SCH_TOO_MANY_TASKS  9
#define ERROR_SCH_CANOT_DELETE_TASK 0
#define RETURN_ERROR 0
#define RETURN_NORMAL 1

#define INTERRPT_Timer_2_Overflow 5

#define SCH_REPORT_ERRORS
#ifdef  SCH_REPORT_ERRORS
#define Error_Port P1
#endif

typedef data struct
{
    void (code *pTask)(void);
 ushort Delay;
 ushort Period;
 ushort RunMe;
 uchar  Co_op;//��������Ǻ���ʽ�ģ�����Ϊ1�������������ռʽ�ģ�����Ϊ0
}sTask;
sTask SCH_tasks_G[SCH_MAX_TASKS];

void SCH_Init_T2(void);
uchar SCH_Add_Task(void (code * pFunction)(),const ushort Delay,   ushort PERIOD,	bit Co_op);
//                      ������ָ��                 ��ʱ��ʱ����    ִ�������ʱ����
//                                                 Ϊ0������ִ��   ���Ϊ0����ʾ��������  
void SCH_Dispatch_Tasks(void);
void SCH_Start(void);
bit SCH_Delete_Task(const ushort TASK_INDEX);
void SCH_Go_To_Sleep(void);
void SCH_Report_Status(void);//����ϵͳ״��
void LED_Flash_Init(void);
void LED_Flash_Update_A(void);
void LED_Flash_Update_B(void);
void LED_Flash_Update_C(void);
void LED_Flash_Update_D(void);
void LED_Flash_Update_E(void);
void LED_Flash_Update_F(void);
void LED_Flash_Update_G(void);
void LED_Flash_Update_H(void);

uchar Error_code_G = 0;//
static ushort Error_tick_count_G;//��ס�Դ���һ�μ�¼����������ʱ��
static uchar Last_error_code_G;//�ϴεĴ�����루��1����֮��λ��

uchar  LED_State_G_A = 0;
uchar  LED_State_G_B = 0;
uchar  LED_State_G_C = 0;
uchar  LED_State_G_D = 0;
uchar  LED_State_G_E = 0;
uchar  LED_State_G_F = 0;
uchar  LED_State_G_G = 0;
uchar  LED_State_G_H = 0;

sbit LED_pin_A = P1^0;
sbit LED_pin_B = P1^1;
sbit LED_pin_C = P1^2;
sbit LED_pin_D = P1^3;
sbit LED_pin_E = P1^4;
sbit LED_pin_F = P1^5;
sbit LED_pin_G = P1^6;
sbit LED_pin_H = P1^7;

//Error_code_G = ERROR_SCH_TOO_MANY_TASKS;
//Error_code_G = ERROR_SCH_WAITING_FOR_SLAVE_TO_ACK;
//Error_code_G = ERROR_SCH_WAITING_FOR_START_COMAND_FROM_MASTER;
//Error_code_G = ERROR_SCH_ONE_OR_MORE_SLAVES_DID_NOT_START;
//Error_code_G = ERROR_SCH_LOST_SLAVE;
//Error_code_G = ERROR_SCH_CAN_BUS_ERROR;
//Error_code_G = ERROR_I2C_WRITE_BYTE_AT24C64;

void main(void)
{
 SCH_Init_T2();
 LED_Flash_Init();
 SCH_Add_Task(LED_Flash_Update_A,0,1000);//���һ������
 SCH_Add_Task(LED_Flash_Update_B,0,2000);//���һ������
 SCH_Add_Task(LED_Flash_Update_C,0,3000);//���һ������
 SCH_Add_Task(LED_Flash_Update_D,0,4000);//���һ������
 SCH_Add_Task(LED_Flash_Update_E,0,5000);//���һ������
 SCH_Add_Task(LED_Flash_Update_F,0,6000);//���һ������
 SCH_Add_Task(LED_Flash_Update_G,0,7000);//���һ������
 SCH_Add_Task(LED_Flash_Update_H,0,8000);//���һ������

 SCH_Start();//��ȫ���ж�
 while(1)
 {
  SCH_Dispatch_Tasks();
 }
}
void calc (i,b) 
char i;
int b;

{
  
}

/*------------------------------------------------------------
 ���ǵ��������жϷ�����򣬳�ʼ�������еĶ�ʱ�����þ�������
�ĵ���Ƶ�ʣ�����汾�ĵ������ɶ�ʱ��2�����жϣ���ʱ���Զ���װ��
-------------------------------------------------------------*/
void SCH_Update(void) interrupt INTERRPT_Timer_2_Overflow
{
 //ˢ���������
 uchar Index;
 TF2 = 0;//�����ֹ����
 //ע�⣺���㵥λΪ"ʱ��"�����Ǻ��룩
 for(Index = 0;Index < SCH_MAX_TASKS;Index++)
 {   //��������Ƿ�������
  if(SCH_tasks_G[Index].pTask)
  { 
   if(SCH_tasks_G[Index].Delay == 0)
   {
    //������Ҫ���У������ʱ���Ѿ�����
    if(SCH_tasks_G[Index].Co_op)
    {
     //����Ǻ���ʽ����RunMe��־��1
     SCH_tasks_G[Index].RunMe += 1;//Ҫִ������ı�־��1
    }
    else//���������ռʽ��������������
    {
     (*SCH_tasks_G[Index].pTask)();//��������
     SCH_tasks_G[Index].RunMe -= 1;
     //�����Ե������Զ��ٴ����У����������ɾ��
     if(SCH_tasks_G[Index].Period == 0)
     {
      SCH_tasks_G[Index].pTask = 0;
     }
    }   
    if(SCH_tasks_G[Index].Period)//ʱ����������0
    {
     //���������Ե������ٴ����У�ÿ������̶���ʱ�곤��ִ��һ������
     SCH_tasks_G[Index].Delay = SCH_tasks_G[Index].Period;
    }
   }
   else //�������ӳ�ִ��Ҫ�󣬻�û�����ӳٵ�ʱ��
   {
    //��û��׼��������,�ӳټ�1
    SCH_tasks_G[Index].Delay -= 1;
   }
  }
 }
}

void SCH_Init_T2(void)
{
    uchar i;
 for(i=0;i<SCH_MAX_TASKS;i++)
 {
  SCH_Delete_Task(i);
 }
 Error_code_G = 0;
 T2CON = 0x04;
 TMOD = 0x00;
 TH2   = 0xfc;
 RCAP2H = 0xfc;
 TL2   = 0x18;
 RCAP2L = 0x18;
 ET2   = 1;
 TR2   = 1;
}
/*----------------------------------------------------------------------------
������ÿ��һ��ʱ���������û�������ӳ�֮������
pFunction -- �������õĺ������ơ�ע�⣺�������������ǡ�void void����
DELAY     -- �������һ�α�ִ��֮ǰ�ļ��
PERIOD    -- �����Ϊ0����ֻ���øú���һ�Σ���DELAYȷ������õ�ʱ��
             �����0����ô�����Ǳ��ظ����õ�ʱ����
Co_op     -- ����Ǻ���ʽ����������Ϊ1���������ռʽ����������Ϊ0.

ע�⣺����Ժ�Ҫɾ�����񣬽���Ҫ����ֵ
���ӣ�
Task_ID = SCH_Add_Task(Do_X,1000,0,0);
ʹ����Do_X()��1000��������ʱ��֮������һ�Σ���ռʽ����
Task_ID = SCH_Add_Task(Do_X,0,1000,1);
ʹ����Do_X()ÿ��1000��������ʱ������һ�Σ�����ʽ����
Task_ID = SCH_Add_Task(Do_X,300,1000,0);
ʹ����Do_X()ÿ��1000��������ʱ������һ�Σ�����������T=300��ʱ��ʱ��ִ��
   Ȼ����1300��ʱ��.........����ռʽ����

-----------------------------------------------------------------------------*/
uchar SCH_Add_Task(void (code * pFunction)(),const ushort DELAY, ushort PERIOD,bit Co_op)
{
 uchar Index = 0;
 //�����ڶ������ҵ�һ����϶������еĻ�������Ͳ����������
 while((SCH_tasks_G[Index].pTask != 0)&&(Index < SCH_MAX_TASKS))
 {
  Index++;//��һ����������ӣ���û�г�����������
 }
 //�Ƿ�ﵽ������еĽ�β��
 if(Index == SCH_MAX_TASKS)//���������ﵽ����
 {
  Error_code_G = ERROR_SCH_TOO_MANY_TASKS;
  return SCH_MAX_TASKS;//ֱ�ӷ��أ���������������
 }
 //��������е����˵������������п�϶���������
 SCH_tasks_G[Index].pTask = pFunction;
 SCH_tasks_G[Index].Delay = DELAY;
 SCH_tasks_G[Index].Period = PERIOD;
 SCH_tasks_G[Index].Co_op = Co_op;
 SCH_tasks_G[Index].RunMe  = 0;
 return Index;//���������λ�ã��Ա��Ժ�ɾ����
}

void SCH_Dispatch_Tasks(void)
{
 uchar Index;
 //���ȣ����У���һ��������������������
 for(Index = 0;Index < SCH_MAX_TASKS;Index++)
 {
  //ֻ���Ⱥ���ʽ����
  if((SCH_tasks_G[Index].RunMe > 0)&&(SCH_tasks_G[Index].Co_op))
  {
   (*SCH_tasks_G[Index].pTask)();//ִ������
   SCH_tasks_G[Index].RunMe -= 1;//���������Ҫִ�еı�־
  }
  //������Ǹ������Ρ����񣬽����Ӷ�����ɾ��
  if(SCH_tasks_G[Index].Period == 0)
  {
   SCH_tasks_G[Index].pTask = 0;// ��ͨ��������ɾ���������SCH_Delete_Task(Index);
  }
 }
 SCH_Report_Status();//����ϵͳ״��
 SCH_Go_To_Sleep();
}

void SCH_Start(void)
{
 EA = 1;
}

bit SCH_Delete_Task(const ushort TASK_INDEX)
{
 bit Return_code;
 if(SCH_tasks_G[TASK_INDEX].pTask == 0)
 {
  //����û�����񡣡�������ȫ�ִ������
  Error_code_G = ERROR_SCH_CANOT_DELETE_TASK;
  Return_code = RETURN_ERROR;//���ش������
 }
 else
 {
  Return_code = RETURN_NORMAL;
 }
 //ɾ������
 SCH_tasks_G[TASK_INDEX].pTask = 0x0000;
 SCH_tasks_G[TASK_INDEX].Delay = 0;
 SCH_tasks_G[TASK_INDEX].Period = 0;
 SCH_tasks_G[TASK_INDEX].RunMe = 0;
 return Return_code;
}

void SCH_Go_To_Sleep()
{
 PCON |= 0x01;//��������ģʽ
}

void SCH_Report_Status(void)
{
/* #ifdef SCH_REPORT_ERRORS
 if(Error_code_G != Last_error_code_G)
 {
  Error_Port = 255 - Error_code_G;
  Last_error_code_G = Error_code_G;
  if(Error_code_G != 0)
  {
   Error_tick_count_G = 60000;
  }
  else
  {
   Error_tick_count_G = 0;
  }
 }
 else
 {
  if(Error_tick_count_G != 0)
  {
   if(--Error_count_G == 0)
   {
    Error_code_G = 0;
   }
  }
 }
 #endif    */
}

void LED_Flash_Update_A(void)
{
 if(LED_State_G_A == 1)
 {
  LED_State_G_A = 0;
  LED_pin_A = 0;
 }
 else
 {
  LED_State_G_A = 1;
  LED_pin_A = 1;
 }
}

void LED_Flash_Update_B(void)
{
 if(LED_State_G_B == 1)
 {
  LED_State_G_B = 0;
  LED_pin_B = 0;
 }
 else
 {
  LED_State_G_B = 1;
  LED_pin_B = 1;
 }
}

void LED_Flash_Update_C(void)
{
 if(LED_State_G_C == 1)
 {
  LED_State_G_C = 0;
  LED_pin_C = 0;
 }
 else
 {
  LED_State_G_C = 1;
  LED_pin_C = 1;
 }
}
void LED_Flash_Update_D(void)
{
 if(LED_State_G_D == 1)
 {
  LED_State_G_D = 0;
  LED_pin_D = 0;
 }
 else
 {
  LED_State_G_D = 1;
  LED_pin_D = 1;
 }
}
void LED_Flash_Update_E(void)
{
 if(LED_State_G_E == 1)
 {
  LED_State_G_E = 0;
  LED_pin_E = 0;
 }
 else
 {
  LED_State_G_E = 1;
  LED_pin_E = 1;
 }
}
void LED_Flash_Update_F(void)
{
 if(LED_State_G_F == 1)
 {
  LED_State_G_F = 0;
  LED_pin_F = 0;
 }
 else
 {
  LED_State_G_F = 1;
  LED_pin_F = 1;
 }
}
void LED_Flash_Update_G(void)
{
 if(LED_State_G_G == 1)
 {
  LED_State_G_G = 0;
  LED_pin_G = 0;
 }
 else
 {
  LED_State_G_G = 1;
  LED_pin_G = 1;
 }
}
void LED_Flash_Update_H(void)
{
 if(LED_State_G_H == 1)
 {
  LED_State_G_H = 0;
  LED_pin_H = 0;
 }
 else
 {
  LED_State_G_H = 1;
  LED_pin_H = 1;
 }
}

void LED_Flash_Init(void)
{
 LED_State_G_A= 0;//��ʼ��LED״̬
 LED_State_G_B= 0;//��ʼ��LED״̬
 LED_State_G_C= 0;//��ʼ��LED״̬
}