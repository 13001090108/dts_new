package softtest.dscvp.c;

import java.util.LinkedList;
import java.util.List;

/* 
 * ���������DSCVP�ṹ����������IP���
 * ���ɷ�������α���DSCVP������ͬ��ε���ϣ�ÿ�����һ��
 * 
 */
public class DSCVPOutput {
	//����IP��ĵ�һ��DSCVPָ��
	List<DSCVP> dscvplist = new LinkedList<DSCVP>();
	//�����������Ԫ��
	public void add(DSCVP dscvp){
		dscvplist.add(dscvp);
	}
	//��������ɾ��Ԫ��
	public void remove(DSCVP dscvp){
		dscvplist.remove(dscvp);
	}
	
}
