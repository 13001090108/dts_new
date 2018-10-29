
package softtest.registery;

import javax.swing.JOptionPane;

import softtest.config.c.Config;
import LONGMAI.NoxTimerKey;

public class SysInfo {
	public static boolean checkPermission() {
		NoxTimerKey aNox = new NoxTimerKey();
		int [] keyHandles =  new int[8];
		int [] nKeyNum = new int[1];
		int nAppID = 0xFFFFFFFF;
		//���Ҽ�����
        if( 0 != aNox.NoxFind(nAppID, keyHandles, nKeyNum))
        {
        	JOptionPane.showMessageDialog(null, "������������");
            return false;
        }
        //System.out.println("�ҵ�"+nKeyNum[0]+"ֻ��");
        //System.out.println(UID);
       //�򿪵�һ��������3e50fbc633fbfe24
        if( 0 != aNox.NoxOpen(keyHandles[0], "3e50fbc633fbfe24"))
        {
        	 JOptionPane.showMessageDialog(null, "��������������Ѿ����ڣ�");
             System.out.println(aNox.NoxGetLastError());
             return false;
        }
        aNox.NoxClose(keyHandles[0]);
        //System.out.println("�������ѹر�");
        return true;
	}
	public static int getPhase() {
		NoxTimerKey aNox = new NoxTimerKey();
		int [] keyHandles =  new int[8];
		int [] nKeyNum = new int[1];
		int nAppID = 0xFFFFFFFF;
		int nRtn = 0;
		//��ô���������Ϣ
        int [] nRemain = new int[1];
        int [] nMax = new int[1];
        int [] Mode2 = new int[1];
        
      //���Ҽ�����        if( 0 != aNox.NoxFind(nAppID, keyHandles, nKeyNum))
        if( 0 != aNox.NoxFind(nAppID, keyHandles, nKeyNum))
        {
        	    JOptionPane.showMessageDialog(null, "�Ҳ�����������");
                return -1;
        }
        //System.out.println("�ҵ�"+nKeyNum[0]+"ֻ��");
        nRtn =aNox.NoxGetRemnantCount(keyHandles[0],nRemain,nMax,Mode2);
        if(nRtn != 0)
        {
                System.out.println("��ô���������Ϣʧ��");
                System.out.println(aNox.NoxGetLastError());
                return -1;
        }
    	int all =nMax[0];
		int left =nRemain[0];	
		int num = all/Config.PHASE_NUMUBER;
	    int total=(all-left) /num +1;
	    if(total>Config.PHASE_NUMUBER){
	    	 JOptionPane.showMessageDialog(null, "�������ѹ��ڣ�");
             System.exit(0);
	    }
		return total;  
	}
	
	public static String getHardSN() {
		if (Config.PHASE_REGISTER) {
			int phase = getPhase();
			return getHardDiskSN() + phase;
		}
        return getHardDiskSN();
	}
	
	public static String getHardDiskSN() {
		String ret = "AFWG9DKDY5W7";
		String hardDiskSN=HardDiskUtils.getHardDiskSN().trim();   //��ȡӲ�����к�
		if(null!=hardDiskSN&&!hardDiskSN.equals(""))
		{
			if (hardDiskSN.length() >= ret.length()) {
				ret = hardDiskSN.substring(0,12);
			} else {
				ret = hardDiskSN + ret.substring(hardDiskSN.length());
			}
		}
		return ret;
	}
}
