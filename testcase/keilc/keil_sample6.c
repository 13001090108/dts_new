/* ����ΪAT24C01��AT24C256�Ķ�д���򣬸��˿ɸ����Լ�����ҪӦ�á�
��buf1��������Ҫд������ݣ�buf2�Ĵ�С�ɸ�����Ҫ���塣
addr�ɸ���ʹ�õ�оƬѡ�񣬿ɴ��κ�λ�ö�д��ֻҪ�ڸ�оƬ�ķ�Χ�ڡ�
enumer=ATxxx������ʹ�õ�оƬ��ֵ���������е���ʽ��������ı䡣
������ֻҪ�ڵ��õĳ����ж���ʵ�ʲ������ɣ��������ӳ��򲻱ظĶ���*/

#include <reg52.h>
#include <intrins.h>
#define  ERROR 10     //����ERROR��������      
sbit     SDA=P3^0;
sbit     SCL=P3^1;
enum  eepromtype {AT2401,AT2402,AT2404,AT2408,AT2416,AT2432,AT2464,AT24128,AT24256};/*�������ͺ�*/
enum  eepromtype enumer;   //����һ��ö�ٱ���
unsigned char code buf1 []={1,3,5,7,9,10,11,12,13,15}; /* ���ͻ����� */
unsigned char buf2 [10]; /* ���ջ����� */

/* һ��ͨ�õ�24C01��24C256��9��EEPROM���ֽڶ�д��������
�˳������������������ֱ�Ϊ��д���ݻ�����ָ��,
���ж�д���ֽ�����EEPROM��ַ��EEPROM�����ֽڣ�
�Լ�EEPROM���͡��˳���ṹ�����ã����м��õ��ݴ��ԣ����������Ҳ����:
DataBuffΪ��д�������룯�������������ַ
Length ΪҪ��д���ݵ��ֽ�����
Addr ΪEEPROM��Ƭ�ڵ�ַ AT24256Ϊ0��32767
Control ΪEEPROM�Ŀ����ֽڣ�������ʽΪ(1)(0)(1)(0)(A2)(A1)(A0)(R/W),����R/W=1,
��ʾ������,R/W=0Ϊд����,A2,A1,A0ΪEEPROM��ҳѡ��Ƭѡ��ַ;
enumerΪö�ٱ���,��ΪAT2401��AT24256�е�һ��,�ֱ��ӦAT24C01��AT24C256;
��������ֵΪһ��λ������������1��ʾ�˴β���ʧЧ��0��ʾ�����ɹ�;
ERRORΪ������������������ERRORCOUNT�β���ʧЧ��������ֹ������������1
SDA��SCL���û��Զ��壬�����ݶ���ΪP3^0��P3^1; */
/*����1Kλ��2Kλ��4Kλ��8Kλ��16KλоƬ����һ��8λ�����ֽڵ�ַ�룬����32Kλ����
�Ĳ���2��8λ�����ֽڵ�ַ��ֱ��Ѱַ����4Kλ��8Kλ��16Kλ���ҳ���ַ��Ѱַ*/

/* ����������  AT24C01��AT24C256 �Ķ�д���� ������������ */
bit   RW24xx(unsigned char *DataBuff,unsigned char Length,unsigned int Addr,
                     unsigned char Control,enum eepromtype enumer)
{ void Delay(unsigned char DelayCount);  /*   ��ʱ   */
  void Start(void);  /*   ��������   */
  void Stop(void);   /*   ֹͣIIC����   */
  bit  RecAck(void); /*   ���Ӧ��λ   */
  void NoAck(void);  /*   ����IIC���߲���Ӧ��   */
  void Ack(void);    /*   ��IIC���߲���Ӧ��   */
  unsigned char Receive(void); /*   ��IIC�����϶������ӳ���  */
  void Send(unsigned char sendbyte); /*   ��IIC����д����   */
  unsigned char data j,i=ERROR;
  bit errorflag=1;  /*   �����־   */
  while(i--)
  { Start();  /*   ��������   */
    Send(Control & 0xfe); /*   ��IIC����д���ݣ�������ַ */
    if(RecAck()) continue; /*   ��д����ȷ��������ѭ��   */
    if(enumer > AT2416)
    { Send((unsigned char)(Addr >> 8));//����������ת��Ϊ�ַ������ݣ�����ȡ�ͣ�ֻȡ��8λ.�����������32Kλ��ʹ��16λ��ַѰַ��д��߰�λ��ַ
      if(RecAck())  continue;
    }
    Send((unsigned char)Addr); /*   ��IIC����д����   */
    if(RecAck())  continue; /*   ��д��ȷ��������ѭ��   */
    if(!(Control & 0x01))   //�ж��Ƕ���������д����
    { j=Length;
      errorflag=0;         /* ���������λ */
      while(j--)
      { Send(*DataBuff++); /*   ��IIC����д����   */
        if(!RecAck()) continue; /*   ��д��ȷ��������ѭ��   */
        errorflag=1;
        break;
      }
      if(errorflag==1) continue;
      break;
    }
    else
    { Start();  /*   ��������   */
      Send(Control); /*   ��IIC����д����   */
      if(RecAck()) continue;//����ûӦ��������α���ѭ��
      while(--Length)  /*   �ֽڳ�Ϊ0����   */
      { *DataBuff ++= Receive();
        Ack();   /*   ��IIC���߲���Ӧ��   */
      }
      *DataBuff=Receive(); /* �����һ���ֽ� */
      NoAck();  /*   ����IIC���߲���Ӧ��   */
      errorflag=0;
      break;
    }
  }
  Stop();  /*   ֹͣIIC����   */
  if(!(Control & 0x01))
  { Delay(255); Delay(255); Delay(255); Delay(255);
  }
  return(errorflag);
}

/* * * * * �����Ƕ�IIC���ߵĲ����ӳ��� * * * * */
/* * * * * * �������� * * * * */
void Start(void)
{ SCL=0; /* SCL���ڸߵ�ƽʱ,SDA�Ӹߵ�ƽת��͵�ƽ��ʾ */
  SDA=1; /* һ��"��ʼ"״̬,��״̬��������������֮ǰִ�� */
  SCL=1;
  _nop_(); _nop_(); _nop_();
  SDA=0;
  _nop_(); _nop_(); _nop_(); _nop_();
  SCL=0;
  SDA=1;    
}

/* * * * * ֹͣIIC���� * * * * */
void Stop(void)
{ SCL=0; /*SCL���ڸߵ�ƽʱ,SDA�ӵ͵�ƽת��ߵ�ƽ */
  SDA=0; /*��ʾһ��"ֹͣ"״̬,��״̬��ֹ����ͨѶ */
  SCL=1;
  _nop_(); _nop_(); _nop_(); /* �ղ��� */
  SDA=1;
  _nop_(); _nop_(); _nop_();
  SCL=0;
}

/* * * * * ���Ӧ��λ * * * * */
bit RecAck(void)
{ SCL=0;
  SDA=1;
  SCL=1;
  _nop_(); _nop_(); _nop_(); _nop_();
  CY=SDA;     /* ��Ϊ����ֵ���Ƿ���CY�е� */
  SCL=0;
  return(CY);
}

/* * * * *��IIC���߲���Ӧ�� * * * * */
void Ack(void)
{ SDA=0; /* EEPROMͨ�����յ�ÿ����ַ������֮��, */
  SCL=1; /* ��SDA�͵�ƽ�ķ�ʽȷ�ϱ�ʾ�յ���SDA��״̬ */
  _nop_(); _nop_(); _nop_(); _nop_();
  SCL=0;
  _nop_();
  SDA=1;
}

/* * * * * * * * * ����IIC���߲���Ӧ�� * * * * */
void NoAck(void)
{ SDA=1;
  SCL=1;
  _nop_(); _nop_(); _nop_(); _nop_();
  SCL=0;
}

/* * * * * * * * * ��IIC����д���� * * * * */
void Send(unsigned char sendbyte)
{ unsigned char data j=8;
  for(;j>0;j--)
  { SCL=0;
    sendbyte <<= 1; /* ʹCY=sendbyte^7; */
    SDA=CY; /* CY ��λ��־λ */
    SCL=1;
  }
  SCL=0;
}

/* * * * * * * * * ��IIC�����϶������ӳ��� * * * * */
unsigned char Receive(void)
{ register receivebyte,i=8;
  SCL=0;
  while(i--)
  { SCL=1;
    receivebyte = (receivebyte <<1 ) | SDA;
    SCL=0;
  }
  return(receivebyte);
}

/* * * * * * * * һ������ʱ���� * * * * * * * * * * * * */
void Delay(unsigned char DelayCount)
{ while(DelayCount--);
}

/* ����������  AT24C01��AT24C256 �Ķ�д���� ������������ */
void main()
{ unsigned char Control,*p1,*p2;
  unsigned char Length;
  unsigned int addr ; /* 24CxxƬ�ڵ�ַ */
  p1=buf1;p2=buf2;
  addr=0; /* Ƭ�ڵ�ַ AT24C256Ϊ0��32767 */
  Length=8; /* ��д���� */
  enumer=AT24256; /* ��дAT24C256 */
  Control=0xa0; /* д���� */
  RW24xx(p1,Length,addr,Control,enumer); /* д */
  Control=0xa1; /* ������ */
  RW24xx(p2,Length,addr,Control,enumer); /* �� */
}
