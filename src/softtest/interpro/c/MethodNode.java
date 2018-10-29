package softtest.interpro.c;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import softtest.fsmanalysis.c.AnalysisElement;

/**
 * MethodNode�����ڱ���ȫ�ֵĺ������ù�ϵ�ͺ���ժҪ
 * @author zys	
 * 2010-3-6
 */
public class MethodNode {

	/**
	 * ����ú�������Ӧ�ķ�����Ԫ�������ú�����Ӧ��Դ�ļ������м��ļ���
	 */
	private AnalysisElement element;
	
	/**
	 * �����ú����ڵ���ļ���
	 */
	private String tempFileName="MaybeLibMethod";
	
	/**
	 * �����ڵ��Ӧ��C�еĺ�������
	 */
	private Method method;
	
	/**
	 * �ú����ڵ���õĺ�������
	 */
	private LinkedHashSet<MethodNode> calls;
	
	/**
	 * �ú����ڵ���õĺ����б�������ظ� 
	 */
	private List<MethodNode> orderCalls;
	
	/**
	 * �ú����ڵ�ĳ��ȣ��������˶��ٸ���������
	 */
	private int degree;
	
	/**
	 * �ú����ڵ����������еı�ţ����ڹ���ȫ�ֺ�������ͼ 
	 */
	private int toponum;
	// add by ALUO
	/** ��� ���ڻ�ȡ�ڽӾ��� **/
	int matrixNumber; 
	
	//add by ALUO
	/** �ڵ��Ѿ����ʹ��Ķ�����List **/
	ArrayList<Integer> allVisitedList;
	
	//add by ALUO
	public boolean visited = false;
	
	//add by ALUO
	public void setAllVisitedList(ArrayList<Integer> allVisitedList) {  
        this.allVisitedList = allVisitedList;  
    }  
	
	//add by ALUO
	public ArrayList<Integer> getAllVisitedList() {  
        return allVisitedList;  
    }  
	
	//add by ALUO
	public void setVisited(int j) {  
        allVisitedList.set(j, 1);  
    }
	public MethodNode(AnalysisElement element, Method method) {
		this.element = element;
		this.method = method;
		calls = new LinkedHashSet<MethodNode>();
		orderCalls = new LinkedList<MethodNode>();
		degree = 0;
	}
	
	/**
	 * Ŀǰû��ȷ��������Ӧ�ķ�����Ԫ������ͨ��ͷ�ļ�������ⲿ�ļ�����ĺ�������������¼��ʱ�ļ���
	 */
	public MethodNode(String tempFileName, Method method) {
		this.tempFileName = tempFileName;
		this.method = method;
		calls = new LinkedHashSet<MethodNode>();
		orderCalls = new LinkedList<MethodNode>();
		degree = 0;
	}
	
	/**
	 * ���½ڵ��Ӧ���ļ���
	 * ����֮ǰ������ֻ��ͷ�ļ�����������������ĺ�����ʱ����Ҫ���½ڵ��Ӧ���ļ���
	 * 
	 * @param element
	 */
	public void updateElement(AnalysisElement element) {
		if (element == null) {
			this.element = element;
		}
		String fileName = element.getFileName();
		if (fileName.endsWith("\"")) {
			fileName = fileName.substring(0, fileName.length() - 1);
		}
		if (fileName.matches(InterContext.SRCFILE_POSTFIX) || fileName.matches(InterContext.INCFILE_POSTFIX)) {
			this.element = element;
		}
	}
	
	public void addCall(MethodNode mtNode) {
		calls.add(mtNode);
		orderCalls.add(mtNode);
	}
	
	@Override
	public String toString() {
		if (element != null) {
			return "MethodNode in file " + element.getFileName() + " : " + method;
		} else {
			return "MethodNode in file " + tempFileName + " : " + method + "(temp)";
		}
	}
	
	public AnalysisElement getElement() {
		return element;
	}
	
	public String getFileName() {
		if (element != null) {
			return element.getFileName();
		}
		return tempFileName;
	}

	public LinkedHashSet<MethodNode> getCalls() {
		return calls;
	}

	public Method getMethod() {
		return method;
	}

	public int getDegree() {
		return degree;
	}
	
	public void setDegree(int degree) {
		this.degree = degree;
	}
	
	/**
	 * ���º����ڵ�ĳ���
	 */
	public void addDegree() {
		this.degree++;
	}
	
	public void decDegree() {
		this.degree--;
	}

	public int getToponum() {
		return toponum;
	}

	public void setToponum(int toponum) {
		this.toponum = toponum;
	}

	public List<MethodNode> getOrderCalls() {
		return orderCalls;
	}
	
}
