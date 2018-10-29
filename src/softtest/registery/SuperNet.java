package softtest.registery;

import org.longmai.SmartNet.SmartNetLib;

public class SuperNet {
	private SmartNetLib smartnet = null;
	private static int port = 8088;
	private static int heartbeat = 70;
	
	public SuperNet() {
		smartnet = SmartNetLib.getInstance();
	}

	private int validation() {
		int rtn = smartnet.SmartNetOpen(1639095144, -1355634854, -1485101938, 1049275498);
		if(rtn != 0){
			System.out.println("��֤����������ʧ��,�����룺"+smartnet.SmartNetGetLastError());
		}else{
			System.out.println("��֤����������ɹ�");
		}
		return rtn;
	}
	
	public SmartNetLib getSmartnet() {
		return smartnet;
	}
	
	public int connect(String ip) {
		//smartnet = SmartNetLib.getInstance();
		int rtn = smartnet.SmartNetConnect(ip, port, heartbeat);
		if(rtn != 0){
			System.out.println("δ���ӳɹ�,�����룺"+smartnet.SmartNetGetLastError());
		}else{
			System.out.println("���ӳɹ�");
		}
		return rtn;
	}
	
	public int readData(int cell) {
		validation();
		byte [] pBuffer = new byte[4];
		for(int i=0; i<pBuffer.length; i++) {
			pBuffer[i] = 0;
		}
		int rtn=smartnet.SmartNetReadStorage(cell, 4, pBuffer);
		if(rtn!=0){
	    	System.out.println("��ȡ�洢��ʧ��,������Ϊ��"+smartnet.SmartNetGetLastError());
	    }else{
	    	System.out.println("��ȡ�洢���ɹ�"); 
	    }
		int ans = 0;
		ans = ((int)pBuffer[3]) & 0xFF;
		ans <<= 8;
		ans = ans ^ (((int)pBuffer[2]) & 0xFF);
		ans <<= 8;
		ans = ans ^ (((int)pBuffer[1]) & 0xFF);
		ans <<= 8;
		ans = ans ^ (((int)pBuffer[0]) & 0xFF);
		
		return ans;
	}

	public void writeData(int data, int cell) {
		validation();
		byte[] pBuffer = new byte[4];
		pBuffer[0] = (byte)data;
		pBuffer[1] = (byte)((data>>8) & 0xFF);
		pBuffer[2] = (byte)((data>>16) & 0xFF);
		pBuffer[3] = (byte)((data>>24) & 0xFf);
	    int rtn=smartnet.SmartNetWriteStorage(cell, 4, pBuffer);
	    if(rtn!=0){
	    	System.out.println("д�洢��ʧ��,������Ϊ��"+smartnet.SmartNetGetLastError());
	    }else if(rtn==0){
	    	System.out.println("д�洢���ɹ�");
	    } 
	}
	
	public int disConnect() {
		int rtn = smartnet.SmartNetDisConnect();
		return rtn;
	}
}
