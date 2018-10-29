//������ϵ綯С��C51����
#include"reg51.h"
#include<intrins.h>
#define uchar unsigned char
#define uint  unsigned int
#define left_infrare  0
#define right_infrare 1
#define dj_state1        0X5F      //ǰ��
#define dj_state2        0X4F      //��ת 
#define dj_state3        0X1F      //��ת 
#define dj_state4        0X0F      //���� 
#define dj_state5        0XfF      //ͣ��
#define light_off        0x0f      //��ת���
#define left_light       0X5F      //��ת���    ������5f
#define right_light      0XaF      //��ת���0xaf��������0xbf
#define back_light       0XcF      //ɲ���Ƽ����
#define front_light      0x3f      //ǰ��
#define light_on         0xff      //�����е�
#define true  1
#define false 0
#define LCD_Data  P0
#define Busy  0x80            //���ڼ��LCD״̬���е�Busy��ʶ
sbit    c=P1^2;              //ת���ʹ�ܶ�
uchar code talk1[]={"backward"};
uchar code talk2[]={"forward"};
uchar code talk3[]={"Turnleft"};
uchar code talk4[]={"Turn right"};
uchar flage =0x00;
sbit  ledcs=P1^2;         //74H573��Ƭѡ�ź�
//sbit  left_led=P0^2;     //����ⷢ���
//sbit  right_led=P0^3;    //�Һ��ⷢ���

sbit  LCD_RS = P1^5;     //LCD��������
sbit  LCD_RW = P1^6;     //
sbit  LCD_E  = P1^7 ;
void Delay5Ms(void)
{
 uint TempCyc = 5552;
 while(TempCyc--);
}
//400ms��ʱ
void Delay400Ms(void)
{uchar TempCycA = 5;
 uint TempCycB;
 while(TempCycA--)
   { TempCycB=7269;
     while(TempCycB--);
    }
}
//LCD��״̬
unsigned char ReadStatusLCD(void)
{
 LCD_Data = 0xFF;
 LCD_RS = 0;
 LCD_RW = 1;
 LCD_E = 0;
 LCD_E = 0;
 LCD_E = 1;
 while (LCD_Data & Busy);   //���æ�ź�
 return(LCD_Data);
}
//LCDд����
void WriteDataLCD(unsigned char WDLCD )
{
 ReadStatusLCD();  //���æ
 LCD_Data = WDLCD;
 LCD_RS=1;
 LCD_RW =0;
 LCD_E = 0; //�������ٶ�̫�߿���������С����ʱ
 LCD_E = 0; //��ʱ ,Ϊ�˰�ȫ
 LCD_E = 0; //��ʱ
 LCD_E = 1;
}
//LCDдָ��
void WriteCommandLCD(unsigned char WCLCD,BuysC)
{
 if (BuysC) ReadStatusLCD();   //������Ҫ���æ,BuysCΪ0ʱ����æ���
 LCD_Data = WCLCD;
 LCD_RS= 0;
 LCD_RW= 0;
 LCD_E = 0;  //��ʱ ,Ϊ�˰�ȫ
 LCD_E = 0;
 LCD_E = 0; //��ʱ
 LCD_E = 1;
}
void LCDInit(void)         //LCD��ʼ��
{
 Delay400Ms();
 LCD_Data = 0;
 WriteCommandLCD(0x38,0);  //������ʾģʽ���ã������æ�ź�
 Delay5Ms();
 WriteCommandLCD(0x38,0);
 Delay5Ms();
 WriteCommandLCD(0x38,0);
 Delay5Ms();

 WriteCommandLCD(0x38,1); //��ʾģʽ����,��ʼҪ��ÿ�μ��æ�ź�
 WriteCommandLCD(0x08,1); //�ر���ʾ
 WriteCommandLCD(0x01,1); //��ʾ����
 WriteCommandLCD(0x06,1); // ��ʾ����ƶ�����
 WriteCommandLCD(0x0C,1); // ��ʾ�����������
}
//��ָ��λ����ʾһ���ַ�
void DisplayOneChar(uchar X, uchar Y, uchar DData)
{
 Y &= 0x1;
 X &= 0xF;                 //����X���ܴ���15��Y���ܴ���1
 if (Y)
 X |= 0x40;               //��Ҫ��ʾ�ڶ���ʱ��ַ��+0x40;
 X |= 0x80;               // ���LCD��ָ����
 WriteCommandLCD(X, 0);   //���ﲻ���æ�źţ����͵�ַ��
 WriteDataLCD(DData);
}
//��ָ��λ����ʾһ���ַ�(ֻ��дһ��)��
void DisplayListChar(uchar X, uchar Y,uchar ListLength, uchar  *DData,uchar n)
{ uchar i;
 Y &= 0x01;
 X &= 0x0F;                 //����X���ܴ���15��Y���ܴ���1
 for(i=0;i<ListLength;i++)
 { if (X <= 0x0F) //X����ӦС��0xF
    {   DisplayOneChar(X, Y, DData[i]); //��ʾ�����ַ�
         if(n==true)Delay400Ms();
          X++;
    }
  }
}
/****************************
 �����߽����ӳ���,�������жϵ��½��ش�����ʽ
****************************/
void infrared_ray()interrupt 0  using 3
{  uchar i=90;
   flage=0x01;             //���ܱ�־λ
   while(i--);            //��С������
   EX0=0;               //�ص��жϣ��ȵ����䷽����ſ��������ڱ�
}
// ��ʱ�ӳ���
void delay(uint n)
{
  while(--n);
}
//�жϳ�ʼ��
void Init0(void)
{  EA=1;
   IT0=1;
    }
/***************************************
/*ԭ���ǰ��жϴ򿪲� ���䷽����
�����ж�ʱ��ת���жϲ����ж�λΪ��һ��ת����׼����
��û���ǹر��ж�
****************************************/
void seng_wave(uchar timer,bit n)//timerͨ���ز������źŵ�ʱ�䣬n->���ҷ���ܵ�ѡ��
{  uchar i;
   P1 |= 0X04;      //ledcs=1Ϊ74ls573Ϊ11��Ϊ�ߵ�ƽʱ����ֱ�������Ϊ��ʱ����������ס��������
   IE |= 0X01;
   P0 |=0x0c;   //04
   for(i=timer;i>0;i--)
     { if(n)P0^=0x08;                      // �ҷ����ͨ���ز������ź�//00
        else P0^=0x04;                    // �����ͨ���ز������ź�//0c
         delay(100);                     //��������������ȣ�����38khz�ķ����Ķ��٣��;���
      }                                     //timer*delay(x)��Ϊ����ܵõ���ƽ������
    P1 &= 0Xfb;
    IE &= 0Xfe;
}
//ledת���ָʾ�ӳ���
void light_control(uchar deng)
{   ledcs=1;
    P0 =deng;
    ledcs=0;  //11111011
}
//����͵ƹ�Ŀ��Ʋ���
void  control(uchar n,uchar dj_state,uchar light)
{   uchar i;
   // P1|=0x04;
     light_control(light);    //ledת��ָʾ��
     delay(100);
     P2 =dj_state;              //����ķ������
    WriteCommandLCD(0x01,1); //LCD��ʾ����

          switch(dj_state)
          { case dj_state2 :{ DisplayListChar(3,1,10,talk4,false);}break;
            case dj_state3: { DisplayListChar(3,1,8,talk3,false);}break;
            case dj_state4: { DisplayListChar(3,1,7,talk1,false); }break;
             default :break;
             }
          for(i=n;i>0;i--)
          {delay(2000);}
           P2=dj_state5;               //ͣ��
         light_control(light_off);       //led�ر�
        WriteCommandLCD(0x01,1); //LCD��ʾ����
         P2=dj_state1;                     //ǰ��
        if(dj_state1)
           { P1|=0X04;          //ledcs=1;
             P0=0x0f;
             P1&=0XFB;
             delay(100);
             DisplayListChar(0,0,7,talk2,false);
             }
     }
/****************************************
������Ҫ���Ʋ���
*****************************************/
void move_car(void)
 {
   uchar temp =0x00;
   //��ߺ���ܷ���
       seng_wave(1,left_infrare);     //����Ϊ�жϿ����йرպ�Ҫִ�е����
        if(flage==0x01){temp|=0x01;flage=0x00;}
       //�ұߺ���ܷ���
         delay(30);
        seng_wave(1,right_infrare);    //����Ϊ�жϿ����йرպ�Ҫִ�е����
        if(flage==0x01){temp|=0x02;flage=0x00;}

      //������ϰ����ת
     if(temp==0x01){control(2,dj_state2,left_light); temp =0x00;}
      //�ұ����ϰ����ת
      else if(temp==0x02) {control(2,dj_state3,right_light ); temp =0x00;}
       //�����������ϰ�����ˣ���ת
        else if(temp==0x03) {control(10,dj_state4,back_light );
              control(5,dj_state2,right_light ); temp =0x00;}       

    }
void main(void)
{  Init0();       //�жϳ�ʼ��
   P1 |= 0X04;    //���������Ŀ���λ
   P0 = 0xFf;     //���ݿڵ�����
   P1&=0XFB;      //���������Ŀ���λ
   LCDInit();     //LCD��ʼ��
   WriteCommandLCD(0x01,1);   //��ʾ����
   delay(100);
   P2=dj_state1;
   DisplayListChar(0,0,8,talk2,false);
 while(1)
   {    move_car();  //��Ҫ���Ʋ���
        delay(200000);//��ʱ
    }
 }