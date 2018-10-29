package softtest.callgraph.c;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import softtest.callgraph.c.CEdge;
import softtest.callgraph.c.CVexNode;
import softtest.config.c.Config;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.interpro.c.Method;
import softtest.symboltable.c.*;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.tools.c.testcasegenerator.TestCaseGeneratorForCallGraphVisitor;

/** ������ù�ϵͼ���� */
public class CGraph extends CElement {
	/** ����ʵ�ʵļ��������ڲ������� */
	private int nodecount = 0;

	/** ����ʵ�ʵļ��������ڲ������� */
	private int edgecount = 0;

	/** ��㼯�� */
	public Hashtable<String, CVexNode> nodes = new Hashtable<String, CVexNode>();
	protected AnalysisElement element=null;
	/** �߼��� */
	public Hashtable<String, CEdge> edges = new Hashtable<String, CEdge>();


	/** ���ù�ϵͼ���ļ���*/
	public String name = new String();

	/** ȱʡ�������캯�� */
	public CGraph() {

	}

	/** ����һ���ڵ㣬����ýڵ��Ѿ����ڣ����׳��쳣 */
	public CVexNode addVex(CVexNode vex) {
		if (nodes.get(vex.name) != null) {
			throw new RuntimeException("The vexnode has already existed.");
		}
		nodes.put(vex.name, vex);
		vex.snumber = nodecount++;
		return vex;
	}

	/** ����һ��ָ�����ƵĽڵ㣬���趨�ýڵ�����ĺ������������յ����ƽ�Ϊname+nodecount */
	public CVexNode addVex(String name, MethodNameDeclaration mnd) {
		CVexNode vex = new CVexNode(name + nodecount, mnd);
		return addVex(vex);
	}

	/** ����һ���ڵ� ���趨�ýڵ�����ĺ������� */
	public CVexNode addVex(MethodNameDeclaration mnd) {
		String name = "" + nodecount;
		return addVex(name, mnd);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨���� */
	public CEdge addEdge(CVexNode tailnode, CVexNode headnode, String name) {
		if (headnode == null || tailnode == null) {
			throw new RuntimeException("An edge's head or tail cannot be null.");
		}
		if (edges.get(name) != null) {
			throw new RuntimeException("The edge has already existed.");
		}
		if (nodes.get(headnode.name) != headnode || nodes.get(tailnode.name) != tailnode) {
			throw new RuntimeException("There is a contradiction.");
		}

		if (headnode.inedges.get(name) != null) {
			throw new RuntimeException("There is a contradiction.");
		}

		if (tailnode.outedges.get(name) != null) {
			throw new RuntimeException("There is a contradiction.");
		}

		CEdge e = new CEdge(name, tailnode, headnode);
		edges.put(name, e);
		e.snumber = edgecount++;

		return e;
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨���� */
	public CEdge addEdge(String tail, String head, String name) {
		CVexNode tailnode = nodes.get(tail);
		CVexNode headnode = nodes.get(head);
		return addEdge(tailnode, headnode, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ı� */
	public CEdge addEdge(String tail, String head) {
		String name = "" + edgecount;
		return addEdge(tail, head, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ı� */
	public CEdge addEdge(CVexNode tailnode, CVexNode headnode) {
		String name = "" + edgecount;
		return addEdge(tailnode, headnode, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨����Ϊname+edgecount */
	public CEdge addEdge(String name, CVexNode tailnode, CVexNode headnode) {
		name = name + edgecount;
		return addEdge(tailnode, headnode, name);
	}

	/** ɾ��ָ���ıߣ�����Ҳ����ñ߻��߸ñ߲���ͼ�еı����׳��쳣 */
	public void removeEdge(CEdge e) {
		if (edges.get(e.name) != e || e == null) {
			throw new RuntimeException("Cannot find the edge.");
		}
		if (e.headnode == null || e.tailnode == null) {
			throw new RuntimeException("There is a contradiction.");
		}
		if (e.headnode.inedges.get(e.name) != e || e.tailnode.outedges.get(e.name) != e) {
			throw new RuntimeException("There is a contradiction.");
		}

		e.headnode.inedges.remove(e.name);
		e.tailnode.outedges.remove(e.name);
		edges.remove(e.name);
	}

	/** ɾ��ָ���ı� */
	public void removeEdge(String name) {
		CEdge e = edges.get(name);
		removeEdge(e);
	}

	/** ɾ��ָ���ڵ��������� */
	public void removeInedges(CVexNode vex) {
		LinkedList<CEdge> temp = new LinkedList<CEdge>();
		temp.clear();
		for (Enumeration<CEdge> e = vex.inedges.elements(); e.hasMoreElements();) {
			temp.add(e.nextElement());
		}
		ListIterator<CEdge> i = temp.listIterator();
		while (i.hasNext()) {
			CEdge edge = i.next();
			removeEdge(edge);
		}
	}

	/** ɾ��ָ���ڵ�����г��� */
	public void removeOutedges(CVexNode vex) {
		LinkedList<CEdge> temp = new LinkedList<CEdge>();
		temp.clear();
		for (Enumeration<CEdge> e = vex.outedges.elements(); e.hasMoreElements();) {
			temp.add(e.nextElement());
		}
		ListIterator<CEdge> i = temp.listIterator();
		while (i.hasNext()) {
			CEdge edge = i.next();
			removeEdge(edge);
		}
	}

	/**
	 * @author wangy
	 *2017-10-26
	 */
	public Long getnodecount(){
		return (long)nodes.size();
	}
	
	/**
	 * @author wangy
	 *2017-10-26
	 */
	public Long getedgecount(){
		return (long)edges.size();
	}
	
	/**
	 * @author wangy
	 *2017-10-26
	 */
	public String getname(){
		return this.name;
	}
	
	


	/** ɾ��ָ���ڵ㼰������ı� */
	public void removeVex(CVexNode vex) {
		if (nodes.get(vex.name) != vex || vex == null) {
			throw new RuntimeException("Cannot find the vexnode.");
		}
		removeInedges(vex);
		removeOutedges(vex);
		nodes.remove(vex.name);
	}

	/** ɾ��ָ���ڵ��������� */
	public void removeVex(String name) {
		CVexNode vex = nodes.get(name);
		removeVex(vex);
	}

	/** ������ͼ�����ߵ�accept���� */
	@Override
	public void accept(CGraphVisitor visitor, Object data) {
		visitor.visit(this, data);
	}

	/** ���ָ���ڵ��һ��û�з��ʵ����ڽڵ㣬���ܷ���null,�ڵ�˳������Ȼ˳����� */
	private CVexNode getAdjUnvisitedVertex(CVexNode v) {
		if (v.outedges.size() <= 0) {
			return null;
		}
		List<CVexNode> list = new ArrayList<CVexNode>();
		for (Enumeration<CEdge> e = v.outedges.elements(); e.hasMoreElements();) {
			CEdge edge = e.nextElement();
			if (!edge.headnode.getVisited()) {
				list.add(edge.headnode);
			}
		}
		Collections.sort(list);
		if (!list.isEmpty()) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/** ������ȱ�����visitorΪ�Խڵ�ķ����ߣ�dataΪ���� */
	public void dfs(CGraphVisitor visitor, Object data) {
		// �ҵ�ͼ��ڿ�ʼ
		CVexNode first = null;
		Stack<CVexNode> stack = new Stack<CVexNode>();
		for (Enumeration e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = (CVexNode) e.nextElement();
			if (!n.getVisited() && n.inedges.size() == 0) {
				first = n;
				break;
			}
		}
		if (first == null) {
			throw new RuntimeException("������ͼ��ڴ���");
		}
		first.accept(visitor, data);
		first.setVisited(true);
		stack.push(first);
		while (!stack.isEmpty()) {
			CVexNode next = getAdjUnvisitedVertex(stack.peek());
			if (next == null) {
				stack.pop();
			} else {
				next.accept(visitor, data);
				next.setVisited(true);
				stack.push(next);
			}
		}
		// ������Щͼ��ڵ��ﲻ�˵Ľڵ㣬�����÷�����
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = e.nextElement();
			if (!n.getVisited()) {
				// first.accept(visitor, data);
				first.setVisited(true);
				stack.push(first);
				while (!stack.isEmpty()) {
					CVexNode next = getAdjUnvisitedVertex(stack.peek());
					if (next == null) {
						stack.pop();
					} else {
						// next.accept(visitor, data);
						next.setVisited(true);
						stack.push(next);
					}
				}
			}
		}

		// �����ʱ�־�������û�false
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = e.nextElement();
			n.setVisited(false);
		}
	}

	/** �ڵ�˳����� */
	public void numberOrderVisit(CGraphVisitor visitor, Object data) {
		List<CVexNode> list = new ArrayList<CVexNode>();
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode vex = e.nextElement();
			list.add(vex);
		}
		Collections.sort(list);

		Iterator<CVexNode> i = list.iterator();
		while (i.hasNext()) {
			i.next().accept(visitor, data);
		}
	}

	/** ������нڵ�ķ��ʱ�־ */
	public void clearVisited() {
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = e.nextElement();
			n.setVisited(false);
		}
	}

	/** �õ����нڵ�����������б����ڴ��ڻ�·�������ѡȡ�����С�Ľڵ㣬ɾ������ƻ���· */
	public List<CVexNode> getTopologicalOrderList() {
		Stack<CVexNode> stack = new Stack<CVexNode>();
		ArrayList<CVexNode> list = new ArrayList<CVexNode>();
		// ��ʼ�����
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = e.nextElement();
			n.indegree = 0;
			for (Enumeration<CEdge> e1 = n.getInedges().elements(); e1.hasMoreElements();) {
				CEdge edge = e1.nextElement();
				if (edge.getTailNode() != n) {
					// �Ի��������
					n.indegree++;
				}
			}
			if (n.indegree == 0) {
				// ���Ϊ0�Ľڵ���ջ
				stack.push(n);
			}
		}
		while (list.size() < nodes.size()) {
			while (!stack.empty()) {
				CVexNode n = stack.pop();
				list.add(n);

				for (Enumeration<CEdge> e = n.getOutedges().elements(); e.hasMoreElements();) {
					CEdge edge = e.nextElement();
					CVexNode headnode = edge.getHeadNode();
					// ���Ի�
					if (headnode != n) {
						// ͷ�ڵ����-1
						if (headnode.indegree > 0) {
							headnode.indegree--;
							if (headnode.indegree == 0) {
								// ���Ϊ0����ջ
								stack.push(headnode);
							}
						}
					}
				}
			}

			if (list.size() < nodes.size()) {
				// ���ڻ�·
				CVexNode mindegreenode = null;
				// ѡȡ���>0������С�Ľڵ�
				for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
					CVexNode n = e.nextElement();
					if (n.indegree > 0) {
						if (mindegreenode == null || mindegreenode.indegree > n.indegree) {
							mindegreenode = n;
						}
					}
				}
				// �ƻ���·
				mindegreenode.indegree = 0;
				stack.push(mindegreenode);
			}
		}
		return list;
	}
	public List<CVexNode> getTopologicalOrderList(AnalysisElement element) {
		// ���������������������û�еĻ����Զ���Ӻϳ���������
		this.element=element;
		if(!Config.SKIP_METHODANALYSIS){
			if(Config.MethodAnalysisInterMethodVisitor){
				dumpfunctioncall(nodes);
			}
		}
	
		Stack<CVexNode> stack = new Stack<CVexNode>();
		ArrayList<CVexNode> list = new ArrayList<CVexNode>();
		// ��ʼ�����
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = e.nextElement();
			n.indegree = 0;
			for (Enumeration<CEdge> e1 = n.getInedges().elements(); e1.hasMoreElements();) {
				CEdge edge = e1.nextElement();
				if (edge.getTailNode() != n) {
					// �Ի��������
					n.indegree++;
				}
			}
			if (n.indegree == 0) {
				// ���Ϊ0�Ľڵ���ջ
				stack.push(n);
			}
		}
		while (list.size() < nodes.size()) {
			while (!stack.empty()) {
				CVexNode n = stack.pop();
				list.add(n);

				for (Enumeration<CEdge> e = n.getOutedges().elements(); e.hasMoreElements();) {
					CEdge edge = e.nextElement();
					CVexNode headnode = edge.getHeadNode();
					// ���Ի�
					if (headnode != n) {
						// ͷ�ڵ����-1
						if (headnode.indegree > 0) {
							headnode.indegree--;
							if (headnode.indegree == 0) {
								// ���Ϊ0����ջ
								stack.push(headnode);
							}
						}
					}
				}
			}

			if (list.size() < nodes.size()) {
				// ���ڻ�·
				CVexNode mindegreenode = null;
				// ѡȡ���>0������С�Ľڵ�
				for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
					CVexNode n = e.nextElement();
					if (n.indegree > 0) {
						if (mindegreenode == null || mindegreenode.indegree > n.indegree) {
							mindegreenode = n;
						}
					}
				}
				// �ƻ���·
				mindegreenode.indegree = 0;
				stack.push(mindegreenode);
			}
		}
		return list;
	}
	
	/**
	 *@author liuli	2010.4.9
	 **/
	private void appendTab(StringBuffer tb, int times){
		for(int i=0;i<TestCaseGeneratorForCallGraphVisitor.TAB_SIZE*times;i++){
			tb.append(" ");
		}
	}
	/**
	 * @author Liuli��Ϊ����ͼCallGraphVisitor���ɲ�������
	 *2010-4-9
	 */
	public String printGraphForTestCaseGeneratorForCG(){
		StringBuffer sb = new StringBuffer();
		
		sb.append("Graph {\"");
		appendTab(sb,14);
		sb.append("+\"\\n\"+\n");
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = e.nextElement();
			appendTab(sb,4);
			sb.append("\"  "+n.name+"\"");
			appendTab(sb,10);
			sb.append("+\"\\n\"+\n");
		}
		
		for (Enumeration<CEdge> e = edges.elements(); e.hasMoreElements();) {
			CEdge edge = e.nextElement();
			appendTab(sb,4);
			sb.append("\"  "+edge.tailnode.name + " -> " + edge.headnode.name+"\"");
			appendTab(sb,10);
			sb.append("+\"\\n\"+\n");
		}
		
		appendTab(sb,3);
		sb.append("\"}");

		return sb.toString();	
	}
	/**
	 * @author Liuli����ӡ����ͼ
	 *2010-4-9
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("Graph {\n");
		for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
			CVexNode n = e.nextElement();
			sb.append("  "+n.name+"\n");
		}

		for (Enumeration<CEdge> e = edges.elements(); e.hasMoreElements();) {
			CEdge edge = e.nextElement();
			sb.append("  "+edge.tailnode.name + " -> " + edge.headnode.name+"\n");
		}
				
		sb.append("}");

		return sb.toString();
	}
	private  void dumpfunctioncall(Hashtable<String, CVexNode> nodes) {
		try {
			
			String filepaths[]=element.getFileName().split("\\\\");
			String filepath=filepaths[filepaths.length-1];
			String str = Config.TRACEPATH + "\\" + filepath +".txt";
			FileWriter out = new FileWriter(str);
			BufferedWriter bf = new BufferedWriter(out);
			bf.write("��ʼ���"+filepath+"�ļ��ں�������Ϣ"+"\r\n");
			bf.write("1���ļ��ڴ���������������Ϊ"+nodes.size()+"\r\n");
			bf.write("2: ����������ļ��ں��������ù�ϵ"+"\r\n");
			for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
				CVexNode n = e.nextElement();
				for (Enumeration<CEdge> e1 = n.getInedges().elements(); e1.hasMoreElements();) {
					CEdge edge = e1.nextElement();
					String headfunctionnameresult[]=getEdgeheadfunctionName(edge);
					String tailfunctionnameresult[]=getEdgetailfunctionName(edge);
					bf.write(headfunctionnameresult[0]+"(),"+"( "+"line "+headfunctionnameresult[1]+", "+headfunctionnameresult[2]+")"+"called by ��������"+tailfunctionnameresult[0]+"(),"+"( "+"line "+tailfunctionnameresult[1]+", "+tailfunctionnameresult[2]+")"+"\r\n"+"\r\n");
					bf.flush();
				}
			}
			Stack<CVexNode> stack = new Stack<CVexNode>();
			ArrayList<CVexNode> list = new ArrayList<CVexNode>();
			// ��ʼ�����
			for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
				CVexNode n = e.nextElement();
				n.indegree = 0;
				for (Enumeration<CEdge> e1 = n.getInedges().elements(); e1.hasMoreElements();) {
					CEdge edge = e1.nextElement();
					if (edge.getTailNode() != n) {
						// �Ի��������
						n.indegree++;
					}
				}
				if (n.indegree == 0) {
					// ���Ϊ0�Ľڵ���ջ
					stack.push(n);
				}
			}
			while (list.size() < nodes.size()) {
				while (!stack.empty()) {
					CVexNode n = stack.pop();
					list.add(n);

					for (Enumeration<CEdge> e = n.getOutedges().elements(); e.hasMoreElements();) {
						CEdge edge = e.nextElement();
						CVexNode headnode = edge.getHeadNode();
						// ���Ի�
						if (headnode != n) {
							// ͷ�ڵ����-1
							if (headnode.indegree > 0) {
								headnode.indegree--;
								if (headnode.indegree == 0) {
									// ���Ϊ0����ջ
									stack.push(headnode);
								}
							}
						}
					}
				}

				if (list.size() < nodes.size()) {
					// ���ڻ�·
					CVexNode mindegreenode = null;
					// ѡȡ���>0������С�Ľڵ�
					for (Enumeration<CVexNode> e = nodes.elements(); e.hasMoreElements();) {
						CVexNode n = e.nextElement();
						if (n.indegree > 0) {
							if (mindegreenode == null || mindegreenode.indegree > n.indegree) {
								mindegreenode = n;
							}
						}
					}
					// �ƻ���·
					mindegreenode.indegree = 0;
					stack.push(mindegreenode);
				}
			}
			bf.write("3: ���ݺ������ù�ϵͼ�õ��ĺ�����������Ϊ"+"\r\n");
			for(int i=0;i<list.size();i++){
				String functionnameresult[]=getCexNodename(list.get(i));
				bf.write("��"+(i+1)+"�������ĺ���Ϊ"+functionnameresult[0]+"(),"+"( "+"line "+functionnameresult[1]+", "+functionnameresult[2]+")"+"\r\n");
			}
			bf.close();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private String[] getEdgeheadfunctionName(CEdge edge){
		String Headfunctions[]=edge.getHeadNode().toString().split("CallVexNode:");
		String Headfunction=Headfunctions[Headfunctions.length-1];
		String splitfunctions1[]=Headfunction.split(" in line ");
		String splitresulttemp=splitfunctions1[0];
		String splittemp[]=splitresulttemp.split("\\(");
		String splitresult1=splittemp[0];
		String splitfunction2=splitfunctions1[1];
		String splitfunctions2[]=splitfunction2.split(" in file ");
		String splitresult2=splitfunctions2[0];
		String splitfunction3=splitfunctions2[1];
		String splitfunctions3[]=splitfunction3.split(" :");
		String splitresult3=splitfunctions3[0];
		String splitresult[]={splitresult1,splitresult2,splitresult3};
		return splitresult;
	}
	private String[] getEdgetailfunctionName(CEdge edge){
		String Headfunctions[]=edge.getTailNode().toString().split("CallVexNode:");
		String Headfunction=Headfunctions[Headfunctions.length-1];
		String splitfunctions1[]=Headfunction.split(" in line ");
		String splitresulttemp=splitfunctions1[0];
		String splittemp[]=splitresulttemp.split("\\(");
		String splitresult1=splittemp[0];
		String splitfunction2=splitfunctions1[1];
		String splitfunctions2[]=splitfunction2.split(" in file ");
		String splitresult2=splitfunctions2[0];
		String splitfunction3=splitfunctions2[1];
		String splitfunctions3[]=splitfunction3.split(" :");
		String splitresult3=splitfunctions3[0];
		String splitresult[]={splitresult1,splitresult2,splitresult3};
		return splitresult;
	}
	private String[] getCexNodename(CVexNode node){
		String functions[]=node.toString().split("CallVexNode:");
		String function=functions[functions.length-1];
		String splitfunctions1[]=function.split(" in line ");
		String splitresulttemp=splitfunctions1[0];
		String splittemp[]=splitresulttemp.split("\\(");
		String splitresult1=splittemp[0];
		String splitfunction2=splitfunctions1[1];
		String splitfunctions2[]=splitfunction2.split(" in file ");
		String splitresult2=splitfunctions2[0];
		String splitfunction3=splitfunctions2[1];
		String splitfunctions3[]=splitfunction3.split(" :");
		String splitresult3=splitfunctions3[0];
		String splitresult[]={splitresult1,splitresult2,splitresult3};
		return splitresult;
	}
}