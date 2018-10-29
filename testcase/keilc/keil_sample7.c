 /* 
  ������������ǿ�����Ŀ����õ��ĳ������ĺû���ֱ��Ӱ���������������ڽ���һ��ʹ�÷ǳ��㷺��Keil C51�����������
 ���������в��ϵ���KeyBord()�����Բ�ͣ��ɨ����̣�
 �ڹ����ӳ����е��ã�unsigned charJB_KeyData()���õ���ֵ
 */
#include <REG52.H>
 
// ������ͷ�ļ���
/***************************
���̵ļ�ֵ����
***************************/
   fun();
#define DubClick 0x40    //����
#define HaveKey 0x80    //�м�
#define SeeKey (0x01|HaveKey)
#define SetKey (0x02|HaveKey)
#define RRKey (0x03|HaveKey)
#define UpKey (0x04|HaveKey)
#define RetKey (0x05|HaveKey)
#define RLKey (0x02|HaveKey)

//#define UseKey (0x06|HaveKey)
#define SeeKey_Dub (SeeKey | DubClick | HaveKey)
#define SetKey_Dub (SetKey | DubClick | HaveKey)//˫��
#define RRKey_Dub (RRKey | DubClick | HaveKey)
#define UpKey_Dub (UpKey | DubClick | HaveKey)
#define RetKey_Dub (RetKey | DubClick | HaveKey)
//#define UseKey_Dub (UseKey | DubClick | HaveKey)

//--------------------------
typedef struct{
  unsigned char KeyPower;  //������
  unsigned int KeyDog;//��ʱ
  unsigned char KeyData;//��ֵ
}KEY;
 
#define TK  8         //������ִ��ʱ��8ms
#define Timer20ms  (30/TK)   //��ʱʱ��20ms
#define Timer2S     (1200/TK) //��ʱʱ��2S
#define Timer100ms   (1000/TK)  //��ʱʱ��100ms
#define Port P2 //���̿�
//****************************
KEY  KeyDat;        //�������ݽṹ
//---------------------------
//5������ռ�õ�IO��
//---------------------------
sbit ko=Port^0;
sbit ka=Port^1;
sbit kb=Port^2;
sbit kc=Port^3;
sbit kd=Port^4;

/***************************
����Ӳ����·��ͬ��ֻ�޸���γ��򼴿ɣ�����
***************************/
unsigned char ReadKey(void){
    ko=0;
    if(!ka) return 2;//Ka
    if(!kb) return 3;//Kb
    if(!kc) return 4;//Kc
    if(!kd) return 5;//Kd
    return 0;
}

/***************************
�б��Ƿ��н�
***************************/
void GetKey(void){
    if(ReadKey()!=0){
    KeyDat.KeyDog=Timer20ms;
    KeyDat.KeyPower++;
    }
}

/***************************
��ʱȥ����
***************************/
void KeyDog(void){
    if(0==-KeyDat.KeyDog){
    KeyDat.KeyData=ReadKey(); //������
    if(KeyDat.KeyData!=0){
    KeyDat.KeyPower++;
    KeyDat.KeyDog=1;//Timer3S;
/***************************
//    BeepPower=1;//����������
***************************/
    }else{
    KeyDat.KeyPower=0;
    KeyDat.KeyData=0;    //����
    }
  }
}

/***************************
�б��Ƿ��ɿ�
***************************/
void KeyOff1(void){
  if(0==0){    //�ɿ�����
    KeyDat.KeyPower=0;
    KeyDat.KeyData |=HaveKey;//�����־
    }else{
    if(0==-KeyDat.KeyDog){ //3����ʱ��
    KeyDat.KeyDog=Timer100ms;
    KeyDat.KeyPower++;
    }
    }
}

/***************************
�����Ƿ��ɿ�
***************************/
void KeyOff2(void)
{
    if(ReadKey()!=0){
    if (0==--KeyDat.KeyDog){
    KeyDat.KeyData |=DubClick | HaveKey;  //������־
    KeyDat.KeyDog=Timer100ms;
/***************************
//    BeepPower=1;//���ͷ�������������
***************************/
    }
    }else{
    KeyDat.KeyData=0;  //?|=HaveKey;//һ�ΰ���
    KeyDat.KeyPower=0;
    }
}
/***************************
����ָ�붨��
***************************/
code void(code *SubKey[])()={
    GetKey,KeyDog,KeyOff1,KeyOff2
};
/***************************
����������û���������ֻ�費�ϵ�����!
***************************/
void KeyBord(void){
   (*SubKey[KeyDat.KeyPower])();
}
/***************************
�û��ڹ��ܺ����е��ã����ؼ�����������ֵ
***************************/
unsigned char JB_KeyData(void){
    unsigned char i=0;
    if (KeyDat.KeyData>DubClick){
    i=KeyDat.KeyData;
    KeyDat.KeyData=0;
    }
    return i;
}