package softtest.dscvp.c;

/*
 * ר���е�DSCVP�ṹ���ɲ�α���DSCVPElement��ɣ���ͬ��ε���ϳ�һ��DSCVP�ṹ
 */
public class DSCVP {
	//��ǰ���
	int layer;
	//IP�����Ϣ��Ӧ�ð����ļ���λ�ã����͵�
	String IPinfo;
	//��Ϻ��dscvp�ַ���
	StringBuilder dscvp;
	//ָ����һ���dscvp�ṹ
	DSCVP nextLayer;
	//���캯��
	public DSCVP (){
		dscvp = new StringBuilder();
	}
	//����DSCVP����Ҫ����
	public void appendDSCVPString(String str){
		dscvp.append(str);
	}
	
	public StringBuilder getDscvp() {
		return dscvp;
	}
	
	public DSCVP getNextLayer() {
		return nextLayer;
	}
	
}
