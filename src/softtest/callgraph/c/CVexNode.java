package softtest.callgraph.c;

import java.util.*;

import softtest.symboltable.c.*;
import softtest.ast.c.*;
import softtest.cfg.c.*;


/** ���ù�ϵͼ�Ķ����� */
public class CVexNode extends CElement implements Comparable<CVexNode> {
	/**��Ӧ�ĺ���������ͼ*/
	public Graph graphnode;
	
	/** ���� */
	public String name;

	/** ��߼��� */
	Hashtable<String, CEdge> inedges = new Hashtable<String, CEdge>();

	/** ���߼��� */
	public Hashtable<String, CEdge> outedges = new Hashtable<String, CEdge>();

	/** ���ʱ�־ */
	boolean visited = false;

	/** ���ڱȽϵ����� */
	public int snumber = 0;
	
	/** ���������������õ�����ȼ���*/
	int indegree = 0;
	
	/** ��Ӧ�ĺ������� */
	MethodNameDeclaration mnd = null;
	
	//added by wangy ����������Ϣ
	public String metric;
	
	/** ��ָ�������ִ������ù�ϵͼ�ڵ� */
	public CVexNode(String name,MethodNameDeclaration mnd) {
		this.name = name;
		this.mnd=mnd;
		mnd.setCallGraphVex(this);	
	}
	
	
	/** ���ú������� */
	public void setMethodNameDeclaration(MethodNameDeclaration mnd){
		this.mnd=mnd;
	}
	
	
	/** ��ú������� */
	public MethodNameDeclaration getMethodNameDeclaration(){
		return this.mnd;
	}
	
	/**��ú����﷨���ڵ�*/
	public ASTFunctionDefinition getMethodDeclaration(){
		return (ASTFunctionDefinition)mnd.getMethodNameDeclaratorNode();
	}

	/** ����������ͼ�����ߵ�accept */
	@Override
	public void accept(CGraphVisitor visitor, Object data) {
		visitor.visit(this, data);
	}
	
	/** ���ýڵ���ʱ�־ */
	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	/** ��ýڵ���ʱ�־ */
	public boolean getVisited() {
		return visited;
	}

	/** ��ýڵ����� */
	public String getName() {
		return name;
	}

	/** �����߼��� */
	public Hashtable<String, CEdge> getInedges() {
		return inedges;
	}

	/** ��ó��߼��� */
	public Hashtable<String, CEdge> getOutedges() {
		return outedges;
	}

	/**��øýڵ�ĺ���������ͼ
	 * added by Miss_lizi*/
	public Graph getGraph(){
		return graphnode;
	}
	
	/** �Ƚ������˳���������� */
	public int compareTo(CVexNode e) {
		if (snumber == e.snumber) {
			return 0;
		} else if (snumber > e.snumber) {
			return 1;
		} else {
			return -1;
		}
	}
	
	/** ���һ���ڵ��Ƿ���ǰ�� */
	public boolean isPreNode(CVexNode p){
		for(Enumeration e=inedges.elements();e.hasMoreElements();){
			CEdge edge=(CEdge)e.nextElement();
			if(p==edge.getTailNode()){
				return true;
			}
		}
		return false;
	}
	@Override
	public String toString() {
		return "CallVexNode: " + mnd;
	}
}
