package softtest.SimDetection.c;

/**对于函数调用图中的节点其实就是函数
 * 成员变量包括函数的funName、结构特征值、功能特征值
 */
public class FunNode {
	/** 函数名字*/
	String funName = null;
	/** 函数的结构特征*/
	double structVal = 0;
	/** 函数的功能特征*/
	double functionVal = 0;
	/** 相邻的下一个节点*/
	FunNode next = null;
	/** 是否是循环头节点函数*/
	boolean loopHead = false;
	
	/** 访问标志 */
	boolean visited = false;
	/** 构造函数们*/
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
	/** 获取函数的结构特征*/
	public double getStuVal(){return this.structVal;}
	/** 获取函数的功能特征*/
	public double getFunVal(){return this.functionVal;}
	/** 设定函数的邻接节点*/
	public void setNext(FunNode f){
		f.next = this.next;
		this.next = f;
	}
	/** 判断两个函数是不是相似*/
	public boolean isMatch(FunNode f){
		return f.functionVal == this.functionVal && 
				f.structVal == this.structVal;
	}
}
