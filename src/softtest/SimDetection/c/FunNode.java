package softtest.SimDetection.c;

/**���ں�������ͼ�еĽڵ���ʵ���Ǻ���
 * ��Ա��������������funName���ṹ����ֵ����������ֵ
 */
public class FunNode {
	/** ��������*/
	String funName = null;
	/** �����Ľṹ����*/
	double structVal = 0;
	/** �����Ĺ�������*/
	double functionVal = 0;
	/** ���ڵ���һ���ڵ�*/
	FunNode next = null;
	/** �Ƿ���ѭ��ͷ�ڵ㺯��*/
	boolean loopHead = false;
	
	/** ���ʱ�־ */
	boolean visited = false;
	/** ���캯����*/
	public FunNode(){};
	public FunNode(double structVal, double functionVal){
		this.structVal = structVal;
		this.functionVal = functionVal;
	}
	public FunNode(String funName,double structVal, double functionVal){
		this.funName = funName;
		this.structVal = structVal;
		this.functionVal = functionVal;
	}
	/** ��ȡ�����Ľṹ����*/
	public double getStuVal(){return this.structVal;}
	/** ��ȡ�����Ĺ�������*/
	public double getFunVal(){return this.functionVal;}
	/** �趨�������ڽӽڵ�*/
	public void setNext(FunNode f){
		f.next = this.next;
		this.next = f;
	}
	/** �ж����������ǲ�������*/
	public boolean isMatch(FunNode f){
		return f.functionVal == this.functionVal && 
				f.structVal == this.structVal;
	}
}
