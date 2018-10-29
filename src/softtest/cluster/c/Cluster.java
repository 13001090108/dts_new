/**
 * 
 */
package softtest.cluster.c;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import softtest.database.c.DBAccess;
import softtest.dts.c.DTSC;

/**
 * @author��JJL
 * @date: 2016��11��15��
 * @description:
 */
public class Cluster {
	int equalNum = 0;//�ȼ��������
	public Cluster() {
		
	}
	
	public int getEqualNum () {
		return this.equalNum;
	}
	//��list��ÿ�����������й�����ϵ����
	public void startCluster(List<SimpleBean> dblist, DBAccess dbAccess) {
		Equal en = new Equal();
		Similar rn = new Similar();
		Cluster c = new Cluster();
		for (int i = 0; i < dblist.size(); i++) {
			SimpleBean defect1 = dblist.get(i);
			for (int j = i + 1; j < dblist.size(); j++) {
				SimpleBean defect2 = dblist.get(j);
				if (isSameCategory(defect1.getCategory(), defect2.getCategory()) ) {
					if (isNotNull(defect1.getStringF()) && isNotNull(defect2.getStringF())) {
						if (defect1.getCategory().contains("RL")) {
							continue;
						} 
						en.computeEqual (defect1, defect2, c);
					}
				}
			}
		}
		
		//�洢ÿ��ȱ�ݶ�Ӧ��ȱ�ݸ���
		int[][] similarNum = new int[dblist.size()][2];
		
		for (int i = 0; i < dblist.size(); i++) {
			SimpleBean defect1 = dblist.get(i);
			for (int j = i + 1; j < dblist.size(); j++) {
				SimpleBean defect2 = dblist.get(j);
				if (isSameCategory(defect1.getCategory(), defect2.getCategory()) ) {
					if (isNotNull(defect1.getStringF()) && isNotNull(defect2.getStringF())) {
						//������IP��Ϊ�ȼ���ʱ�������ƹ�����equal��Ϊnull || equal�����
						if ( !isNotNull(defect1.getEqual()) || !isNotNull(defect2.getEqual()) 
								|| !isEqual(defect1.getEqual(), defect2.getEqual())) {//
							rn.computeRelated (defect1, defect2, dblist);
						}
					}
				}
			}
			
			similarNum[i][0] = i;
			similarNum[i][1] = rn.getSimilarNumber(defect1);			
		}
		
		HashMap<Integer, Integer> equal = en.equalRate(c, dblist);//��writeDB��ʵ��
		HashMap<Integer, Integer> similar = rn.relatedRate(similarNum, dblist);
		// д�����ݿ�
		writeDB(dblist, dbAccess, equal, similar);
	}
	//�ж�����ȱ��ģʽ�Ƿ���ͬ
	boolean isSameCategory (String c1, String c2) {
		List <Set> list = new ArrayList <Set>();
		Set <String> s1 = new HashSet<String>();
		s1.add("BO");
		s1.add("BO_PRE");
		list.add(s1);
		Set <String> s2 = new HashSet<String>();
		s2.add("IAO");
		s2.add("IAO_PRE");
		list.add(s2);
		Set <String> s3 = new HashSet<String>();
		s3.add("MLF");
		s3.add("MLF_PRE");
		s3.add("MLF_LOOP");
		s3.add("MLF_FREE");
		s3.add("MLF_WRONG");
		list.add(s3);
		Set <String> s4 = new HashSet<String>();
		s4.add("NPD");
		s4.add("NPD_Check");
		s4.add("NPD_EXP");
		s4.add("NPD_PARAM");
		s4.add("NPD_PRE");
		s4.add("NPD_POST");
		list.add(s4);
		Set <String> s5 = new HashSet<String>();
		s5.add("OOB");
		s5.add("OOB_Check");
		s5.add("OOB_PRE");
		list.add(s5);
		Set <String> s6 = new HashSet<String>();
		s6.add("RL");
		s6.add("RL_PRE");
		list.add(s6);
		Set <String> s7 = new HashSet<String>();
		s7.add("UFM");
		s7.add("UFM_PRE");
		s7.add("UFM_EXP");
		list.add(s7);
		Set <String> s8 = new HashSet<String>();
		s8.add("UVF");
		s8.add("UVF_EXP");
		list.add(s8);
		
		if (isEqual(c1, c2)) {
			return true;
		}
		for (int i = 0; i < list.size(); i++) {
			Set s = list.get(i);
			if (s.contains(c1) && s.contains(c2)) {
				return true;
			}
		}
		return false;
	}
	
	boolean isEqual(String str1, String str2) {
		if (str1 == null && str2 == null) {
			return true;
		} else if (str1 != null && str2 != null) {
			if (str1.equals(str2)) {
				return true;
			}
		}
		return false;
	}
	
	/** ���ַ������գ�����true*/
	boolean isNotNull(String str) {
		if (str == null) {
			return false;
		} else if (str.equals("")) {
			return false;
		}
		return true;
	}
	//�������Ч�ʲ����������д�����ݿ�
	public void writeDB (List<SimpleBean> dbList, DBAccess dbAccess, HashMap<Integer, Integer> equal, HashMap<Integer, Integer> similar) { 
		int IPNum = 0;
		Set<String> inclusionMainSet = new HashSet<String>();
		Set<String> inclusionTotalSet = new HashSet<String>();
		Map<String, Set<String>> inclusionMap = new HashMap<String, Set<String>>();
		
		for (int j = 0; j < dbList.size(); j++) {
			SimpleBean listSB = dbList.get(j);			
			//����ɶ�λIP
			if (listSB.getF() != null && !listSB.getF().getChild().isEmpty()) {
				IPNum += 1;
			}
			//�������ƹ�ϵ�̺�IP
			if (listSB.getSimilarInclusion()) {
				inclusionMainSet.add(listSB.getNum());
				Set<String> set = listSB.getSimilarInclusionAlarm();
				if(set != null) {
					for (String alarm: set) {
						inclusionTotalSet.add(alarm);
						if (inclusionMap.containsKey(listSB.getNum())) {
							inclusionMap.get(listSB.getNum()).add(alarm);
						} else {
							Set<String> addSet = new HashSet<String>();
							addSet.add(alarm);
							inclusionMap.put(listSB.getNum(), addSet);
						}
					}
				}
			}
		}
		dbAccess.writeAccessToCluster(dbList);
		dbAccess.closeDataBase();
		//System.out.println("����" + dbList.size() + "����¼, ���пɶ�λIP������" + IPNum);
		
		int equalSet = 0, equalNum = 0;
		for (Entry<Integer, Integer> entry : equal.entrySet()) {
			equalSet = entry.getKey();
			equalNum = entry.getValue();
			break;
		}
		
		//System.out.println("�ȼ��ࣺ"+ equalSet+"�У�����" + equalNum + "�������ȷ��Ч�ʣ�"+ (double)(equalNum-equalSet)/IPNum);
		
		int similarCtrl = 0, similarNum = 0;
		for (Entry<Integer, Integer> entry: similar.entrySet()) {
			similarCtrl = entry.getKey();
			similarNum = entry.getValue();
		}
		
		//System.out.println("�����ࣺ"+ similarCtrl+ "��,���ɴ�ȷ��"+ similarNum + "�������ȷ��Ч�ʣ�"+ (double)(similarNum-similarCtrl)/similarNum);
		
		//System.out.println("�������� �̺�IP������Ϊ��" + inclusionMainSet.size()+ "���ֱ�Ϊ��" + inclusionMainSet);
		
		//System.out.println("�������̺�IP�õ������ƹ�ϵ��������Ϊ��" + inclusionTotalSet.size()+ "���ֱ�Ϊ��" + inclusionTotalSet);
		Set<String> inclusionSet = new HashSet<String>();
		inclusionSet.addAll(inclusionMainSet);
		inclusionSet.addAll(inclusionTotalSet);
		//System.out.println("�̺����ƹ�����IP������"+inclusionSet.size() +", ӳ���ϵΪ��" + inclusionMap+", ռ����IP������"+(double)inclusionSet.size()/similarNum);
		//System.out.println("-----------------------------------------");
		
		Logger logger =Logger.getLogger(DTSC.class);
		logger.info("����" + dbList.size() + "����¼, ���пɶ�λIP������" + IPNum);
		logger.info("�ȼ��ࣺ"+ equalSet+"�У�����" + equalNum + "�������ȷ��Ч�ʣ�"+ (double)(equalNum-equalSet)/IPNum);
		logger.info("�����ࣺ"+ similarCtrl+ "��,���ɴ�ȷ��"+ similarNum + "�������ȷ��Ч�ʣ�"+ (double)(similarNum-similarCtrl)/similarNum);
		logger.info("�������� �̺�IP������Ϊ��" + inclusionMainSet.size()+ "���ֱ�Ϊ��" + inclusionMainSet);
		logger.info("�������̺�IP�õ������ƹ�ϵ��������Ϊ��" + inclusionTotalSet.size()+ "���ֱ�Ϊ��" + inclusionTotalSet);
		logger.info("�̺����ƹ�����IP������"+inclusionSet.size() +", ռ����IP������"+(double)inclusionSet.size()/similarNum +", ӳ���ϵΪ��" + inclusionMap);
	}
}
