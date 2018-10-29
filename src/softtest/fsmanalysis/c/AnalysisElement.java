package softtest.fsmanalysis.c;

/**
 * DTSCһ�η�������С��Ԫ��.C�ļ�������������������Ƿ���Ԥ����õ����м��ļ���
 * ���������¼������Ԫ��Ӧ����Ϣ
 * @author 
 *
 */
public class AnalysisElement implements Comparable<AnalysisElement> {

	private String fileName;
	
	private String interFileName;
	
	private boolean cppError = false;
	
	/**
	 * �÷�����Ԫ�ڵ�ĳ��ȣ���ǰ������Ԫ���������ٸ��ⲿ�ķ�����Ԫ
	 */
	private int outDegree;
	/**
	 * �÷�����Ԫ�ڵ����ȣ���ǰ������Ԫ�����ٸ��ⲿ�ķ�����Ԫ����
	 */
	private int inDegree;
	
	/**
	 * ��ȱ������ڲ��һ���
	 */
	private int color;
	
	public AnalysisElement(String fileName, String interFileName) {
		this.fileName = fileName;
		this.interFileName = interFileName;
		outDegree = 0;
		inDegree = 0;
		color = 0;
	}
	
	public AnalysisElement(String fileName) {
		this(fileName, "");
		outDegree = 0;
		inDegree = 0;
		color = 0;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getInterFileName() {
		return interFileName;
	}

	public void setInterFileName(String interFileName) {
		this.interFileName = interFileName;
	}
	
	public void setCError(boolean cppError) {
		this.cppError = cppError;
	}
	
	public boolean isCError() {
		return cppError;
	}
	

	public int getOutDegree() {
		return outDegree;
	}
	
	public void setOutDegree(int degree) {
		this.outDegree = degree;
	}
	
	/**
	 * ���º����ڵ�����
	 */
	public void incOutDegree() {
		this.outDegree++;
	}
	
	public void decOutDegree() {
		this.outDegree--;
	}
	
	public int getInDegree() {
		return inDegree;
	}
	
	/**
	 * ���º����ڵ�����
	 */
	public void incInDegree() {
		this.inDegree++;
	}
	
	public void decInDegree() {
		this.inDegree--;
	}
	
	public void setColor(int color) {
		this.color = color;
	}
	
	public int getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return fileName;
	}

	public int compareTo(AnalysisElement o) {
		return fileName.compareTo(o.getFileName());
	}
	
	@Override
	public int hashCode() {
		return fileName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof AnalysisElement))
			return false;
		AnalysisElement ae=(AnalysisElement)obj;
		if(fileName.equals(ae.fileName))
			return true;
		else 
			return false;
	}
}
