package softtest.cfg.c;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import com.sun.org.apache.xpath.internal.Expression;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsmanalysis.c.FSMPathInsensitiveVisitor;
import softtest.interpro.c.MethodNode;
import softtest.symboltable.c.NameOccurrence;
import softtest.tools.c.testcasegenerator.TestCaseGeneratorForControlFlowVisitor;

/** ���������ͼ���� */
public class Graph extends Element {
	static Logger logger = Logger.getLogger(Graph.class);
	
	private void appendTab(StringBuffer tb, int times){
		for(int i=0;i<TestCaseGeneratorForControlFlowVisitor.TAB_SIZE*times;i++){
			tb.append(" ");
		}
	}
	
	/**
	 * �붨��ʹ����DUAnalysisVisitor���ɵĲ����������бȽ�
	 * @return
	 */
	public String printForDefUse() {
		StringBuffer sb = new StringBuffer();
		ArrayList<VexNode> listnodes=new ArrayList<VexNode>();
		listnodes.addAll(nodes.values());
		Collections.sort(listnodes);
		sb.append("Graph {\n");
		for(VexNode v:listnodes){
			sb.append("  "+v.getName()+":"+v.getLiveDefs()+"\n");
		}
		
		sb.append("}");

		return sb.toString();
	}
	
	/**
	 * Ϊ����ʹ����DUAnalysisVisitor���ɲ�������
	 * @return
	 */
	public String printGraphForTestCaseGeneratorForDUAnalysis()
	{
		StringBuffer sb = new StringBuffer();
		ArrayList<VexNode> listnodes=new ArrayList<VexNode>();
		listnodes.addAll(nodes.values());
		Collections.sort(listnodes);
		sb.append("Graph {\"");
		appendTab(sb,14);
		sb.append("+\"\\n\"+\n");
		for(VexNode v:listnodes){
			appendTab(sb,4);
			sb.append("\"  "+v.getName()+":"+v.getLiveDefs()+"\"");
			appendTab(sb,10);
			sb.append("+\"\\n\"+\n");
		}
		
		appendTab(sb,3);
		sb.append("\"}");
		
		
		return sb.toString();
	}
	
	//Ϊ������ͼControlFlowVisitor���ɲ�������
	public String printGraphForTestCaseGeneratorForCFG()
	{
		StringBuffer sb = new StringBuffer();
		ArrayList<VexNode> listnodes=new ArrayList<VexNode>();
		listnodes.addAll(nodes.values());
		Collections.sort(listnodes);
		sb.append("Graph {\"");
		appendTab(sb,14);
		sb.append("+\"\\n\"+\n");
		for(VexNode v:listnodes){
			appendTab(sb,4);
			sb.append("\"  "+v.toString()+"\"");
			appendTab(sb,10);
			sb.append("+\"\\n\"+\n");
		}
		
		ArrayList<Edge> listedges=new ArrayList<Edge>();
		listedges.addAll(edges.values());
		Collections.sort(listedges);
		for(Edge e:listedges){
			appendTab(sb,4);
			sb.append("\"  "+e.toString()+"\"");
			appendTab(sb,10);
			sb.append("+\"\\n\"+\n");
		}
		
		appendTab(sb,3);
		sb.append("\"}");
		//added by liuyan 2015/10/31
//		if(Config.DU){
//			logger.info("���Բ��Բ��ԣ���������");
//		}

		return sb.toString();
	}
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		ArrayList<VexNode> listnodes=new ArrayList<VexNode>();
		listnodes.addAll(nodes.values());
		Collections.sort(listnodes);
		sb.append("Graph {\n");
		for(VexNode v:listnodes){
			sb.append("  "+v.toString()+"\n");
		}
		
		ArrayList<Edge> listedges=new ArrayList<Edge>();
		listedges.addAll(edges.values());
		Collections.sort(listedges);
		for(Edge e:listedges){
			sb.append("  "+e.toString()+"\n");
		}
		sb.append("}");

		return sb.toString();
	}

	//added by wangy 2017-10-26 
	/** �ÿ�����ͼ��Ӧ����������*/
	public String name = new String();
		
	/** ·������(ɾ��ì�ܱߺ��) */
	private int pathcount = 0;
	
	/** ����ʵ�ʵļ��������ڲ������� */
	private int nodecount = 0;

	/** ����ʵ�ʵļ��������ڲ������� */
	private int edgecount = 0;

	/** ��㼯�� */
	public Hashtable<String, VexNode> nodes = new Hashtable<String, VexNode>();

	//added by wangy
	/** ���ڵ�ļ���*/
	private  List<VexNode>  nodelist = new ArrayList<VexNode>();
	
	/** �߼��� */
	public Hashtable<String, Edge> edges = new Hashtable<String, Edge>();

	/** String to NameOcc, ytang add 20161108*/
	public Hashtable<String, NameOccurrence> occtable= new Hashtable<String, NameOccurrence>();
	
	/** ȱʡ�������캯�� */
	public Graph() {

	}

	public Hashtable<String, NameOccurrence> getOcctable(){
		return occtable;
	}
	
	/** ����һ���ڵ㣬����ýڵ��Ѿ����ڣ����׳��쳣 */
	VexNode addVex(VexNode vex) {
		if (nodes.get(vex.name) != null) {
			throw new RuntimeException("The vexnode has already existed.");
		}
		nodes.put(vex.name, vex);
		nodelist.add(vex);
		vex.snumber = nodecount++;
		return vex;
	}

	/** ����һ��ָ�����ƵĽڵ㣬���趨�ýڵ�����ĳ����﷨�ڵ㣬���յ����ƽ�Ϊname+nodecount */
	VexNode addVex(String name, SimpleNode treenode) {
		VexNode vex = new VexNode(name + nodecount, treenode);
		vex.setGraph(this);
		return addVex(vex);
	}

	/** ����һ���ڵ㣬���趨�ýڵ�����ĳ����﷨�ڵ� */
	VexNode addVex(SimpleNode treenode) {
		String name = "" + nodecount;
		return addVex(name, treenode);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨���� */
	Edge addEdge(VexNode tailnode, VexNode headnode, String name) {
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

		Edge e = new Edge(name, tailnode, headnode);
		edges.put(name, e);
		e.snumber = edgecount++;

		return e;
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨���� */
	Edge addEdge(String tail, String head, String name) {
		VexNode tailnode = nodes.get(tail);
		VexNode headnode = nodes.get(head);
		return addEdge(tailnode, headnode, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ı� */
	Edge addEdge(String tail, String head) {
		String name = "" + edgecount;
		return addEdge(tail, head, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ı� */
	Edge addEdge(VexNode tailnode, VexNode headnode) {
		String name = "" + edgecount;
		return addEdge(tailnode, headnode, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨����Ϊname+edgecount */
	Edge addEdge(String name, VexNode tailnode, VexNode headnode) {
		name = name + edgecount;
		return addEdge(tailnode, headnode, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨����ܵ���ٷ�֧��������֧������T_��ͷ���ٷ�֧��F_��ͷ */
	Edge addEdgeWithFlag(VexNode tailnode, VexNode headnode) {
		String name = "";
		Edge edge = null;
		if(tailnode.getName().startsWith("assert_")){
			if(headnode.getName().startsWith("func_"))
				tailnode.falsetag = true;
			else
				tailnode.truetag = true;
		}
		if (tailnode.truetag) {
			name = "T_";
			tailnode.truetag = false;
		} else if (tailnode.falsetag) {
			name = "F_";
			tailnode.falsetag = false;
		}
		edge = addEdge(name, tailnode, headnode);
		return edge;
	}	

	/** ɾ��ָ���ıߣ�����Ҳ����ñ߻��߸ñ߲���ͼ�еı����׳��쳣 */
	void removeEdge(Edge e) {
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
	void removeEdge(String name) {
		Edge e = edges.get(name);
		removeEdge(e);
	}

	/** ɾ��ָ���ڵ��������� */
	void removeInedges(VexNode vex) {
		LinkedList<Edge> temp = new LinkedList<Edge>();
		temp.clear();
		for (Enumeration<Edge> e = vex.inedges.elements(); e.hasMoreElements();) {
			temp.add(e.nextElement());
		}
		ListIterator<Edge> i = temp.listIterator();
		while (i.hasNext()) {
			Edge edge = i.next();
			removeEdge(edge);
		}
	}

	/** ɾ��ָ���ڵ�����г��� */
	void removeOutedges(VexNode vex) {
		LinkedList<Edge> temp = new LinkedList<Edge>();
		temp.clear();
		for (Enumeration<Edge> e = vex.outedges.elements(); e.hasMoreElements();) {
			temp.add(e.nextElement());
		}
		ListIterator<Edge> i = temp.listIterator();
		while (i.hasNext()) {
			Edge edge = i.next();
			removeEdge(edge);
		}
	}

	/** ɾ��ָ���ڵ㼰������ı� */
	void removeVex(VexNode vex) {
		if (nodes.get(vex.name) != vex || vex == null) {
			throw new RuntimeException("Cannot find the vexnode.");
		}
		removeInedges(vex);
		removeOutedges(vex);
		nodes.remove(vex.name);
	}

	/** ɾ��ָ���ڵ��������� */
	void removeVex(String name) {
		VexNode vex = nodes.get(name);
		removeVex(vex);
	}

	/** ������ͼ�����ߵ�accept���� */
	@Override
	public void accept(GraphVisitor visitor, Object data) {
		visitor.visit(this, data);
	}

	/** ���ָ���ڵ��һ��û�з��ʵ����ڽڵ㣬���ܷ���null,�ڵ�˳������Ȼ˳����� */
	private VexNode getAdjUnvisitedVertex(VexNode v) {
		if (v.outedges.size() <= 0) {
			return null;
		}
		List<VexNode> list = new ArrayList<VexNode>();
		for (Enumeration<Edge> e = v.outedges.elements(); e.hasMoreElements();) {
			Edge edge = e.nextElement();
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
	public void dfs(GraphVisitor visitor, Object data) {
		// �ҵ�������ͼ��ڿ�ʼ
		VexNode first = null;
		Stack<VexNode> stack = new Stack<VexNode>();
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode n = (VexNode) e.nextElement();
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
			VexNode next = getAdjUnvisitedVertex(stack.peek());
			if (next == null) {
				stack.pop();
			} else {
				next.accept(visitor, data);
				next.setVisited(true);
				stack.push(next);
			}
		}
		// ������Щ������ͼ��ڵ��ﲻ�˵Ľڵ㣬�����÷�����
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode n = e.nextElement();
			if (!n.getVisited()) {
				// first.accept(visitor, data);
				first.setVisited(true);
				stack.push(first);
				while (!stack.isEmpty()) {
					VexNode next = getAdjUnvisitedVertex(stack.peek());
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
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode n = e.nextElement();
			n.setVisited(false);
		}
	}

	/** �ڵ�˳����� */
	public void numberOrderVisit(GraphVisitor visitor, Object data) {
		List<VexNode> list = new ArrayList<VexNode>();
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode vex = e.nextElement();
			list.add(vex);
		}
		Collections.sort(list);

		Iterator<VexNode> i = list.iterator();
		while (i.hasNext()) {
			i.next().accept(visitor, data);
		}
	}
	
	/** ������нڵ�ķ��ʱ�־ */
	public void clearVisited(){
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode n = e.nextElement();
			n.setVisited(false);
		}
	}

	/** ������б��ϵ�ì�ܱ�־ */
	public void clearEdgeContradict() {
		for (Enumeration<Edge> e = edges.elements(); e.hasMoreElements();) {
			Edge n = e.nextElement();
			n.setContradict(false);
		}
	}

	/** ������нڵ��ϵ�ì�ܱ�־ */
	public void clearVexNodeContradict() {
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode n = e.nextElement();
			n.setContradict(false);
		}
	}
	
	/**
	 * @param fromvex �����ڵ�
	 * @param tovex	Ŀ�Ľڵ�
	 * @param tempe ���ܾ����ı�
	 * @return ����ɴ��򷵻�true�����򷵻�false
	 */
	public static boolean checkVexReachable(VexNode fromvex,VexNode tovex,Edge tempe){
		if(fromvex==null||tovex==null||tempe==null){
			return false;
		}
		if(fromvex==tovex){
			return true;
		}
		Stack<VexNode> stack = new Stack<VexNode>();
		HashSet<VexNode> table=new HashSet<VexNode>();
		stack.push(fromvex);
		table.add(fromvex);
		while (!stack.isEmpty()) {
			VexNode v=stack.pop();
			for (Enumeration<Edge> e = v.outedges.elements(); e.hasMoreElements();) {
				Edge edge = e.nextElement();
				if(edge==tempe){
					continue;
				}
				VexNode tempnode=edge.headnode;
				if (!table.contains(tempnode)) {
					if(tempnode==tovex){
						return true;
					}
					table.add(tempnode);
					stack.push(tempnode);
				}
			}
		}
		return false;
	}
	
	/**
	 * @param fromvex �����ڵ�
	 * @param tovex	Ŀ�Ľڵ�
	 * @return һ��·��
	 */
	public static List<VexNode> findAPath(VexNode fromvex,VexNode tovex){
		List<VexNode> ret=new ArrayList<VexNode>();
		if(fromvex==null||tovex==null){
			return ret;
		}
		if(fromvex==tovex){
			ret.add(fromvex);
			return ret;
		}
		Stack<VexNode> stack = new Stack<VexNode>();
		HashSet<VexNode> table=new HashSet<VexNode>();
		stack.push(fromvex);
		table.add(fromvex);
		
		while (!stack.isEmpty()) {
			VexNode current=stack.peek();
			VexNode next=null;
			for (Enumeration<Edge> e = current.outedges.elements(); e.hasMoreElements();) {
				Edge edge = e.nextElement();
				VexNode tempnode=edge.headnode;
				if(!table.contains(tempnode)){
					next=tempnode;
					//zys:2010.4.22	����㷨�ǲ��������⣿Ӧ�����break?
					//break;
				}
			}
			if (next == null) {
				stack.pop();
			} else {
				if(next==tovex){
					for(VexNode v:stack){
						ret.add(v);
					}
					ret.add(tovex);
					return ret;
				}else{
					table.add(next);
					stack.push(next);
				}
			}
		}		
		
		
		return ret;
	}
	
	/**
	 * @return ������ڽڵ�
	 */
	public VexNode getEntryNode(){
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode vex = e.nextElement();
			if(vex.getName().startsWith("func_head_")){
				return vex;
			}
		}
		throw new RuntimeException("Cannot find entry node.");
	}
	
	/**
	 * @return ���س��ڽڵ�
	 */
	public VexNode getExitNode(){
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode vex = e.nextElement();
			if(vex.getName().startsWith("func_out_")){
				return vex;
			}
		}
		throw new RuntimeException("Cannot find exit node.");
	}

	/**
	 * @return the pathcount
	 */
	public int getPathcount() {
		return pathcount;
	}

	/**
	 * @param pathcount the pathcount to set
	 */
	public void setPathcount(int pathcount) {
		this.pathcount = pathcount;
	}

	public void clear() {
		for (String key: edges.keySet()) {
			edges.get(key).clear();
		}
		for (String key: nodes.keySet()) {
			nodes.get(key).clear();
		}
		edges.clear();
		nodes.clear();
	}
	
	/**��ȡ������ͼ�еĽڵ���� */
	public int getVexNum(){
		return nodes.size();
	}
	
	//added by wangy
	/**
	 * @return the number of edge
	*/
	public long getedgecount(){
		return (long)edges.size();
	}
		
	//added by wangy
	/**
	* @return the number of edge
	*/
	public String getname(){
		return this.name;
	}
	
	//added by wangy
	/**
	* @return the number of edge
	*/
	public List<VexNode> getAllnodes(){
		return this.nodelist;
	}
		
	//add by ALUO
	/** �ҵ���һ���������õ��ڱ�����������ͼ�еĽڵ� **/
	public HashMap<VexNode, ArrayList<ArrayList<VexNode>>> visitBeginToTargetPointBasisPath(MethodNode methodNode){
		String MethodName = methodNode.getMethod().getName();
		//String MethodName = str;
		HashMap<VexNode, ArrayList<ArrayList<VexNode>>> hashMap = new HashMap<>();
		ArrayList<VexNode> target = new ArrayList<>();
		for(VexNode vexNode : nodes.values()){
			// ������ͼ�Ͻڵ��Ӧ�ĳ����﷨���ڵ㲻׼ȷ �еĶ�Ӧ�ĳ����﷨���ڵ��θ��� ������Ҫ��ϸ����
			if(vexNode.name.contains("func_head_") || vexNode.name.contains("func_out_") 
					|| vexNode.name.contains("if_out_") || vexNode.name.contains("for_out_")
					|| vexNode.name.contains("while_out_") || vexNode.name.contains("do_while_out2_")
					|| vexNode.name.contains("do_while_head_") || vexNode.name.contains("switch_out_")
					|| vexNode.name.contains("label_head_case_") || vexNode.name.contains("label_head_default_")){
				continue;
			}
			if(vexNode.name.contains("if_head_") || vexNode.name.contains("switch_head_")){
				ASTSelectionStatement astExpression = (ASTSelectionStatement)vexNode.treenode;
				ASTExpression expression = (ASTExpression)astExpression.jjtGetChild(0);
				List<Node> list = expression.findChildrenOfType(ASTPrimaryExpression.class);
				for(Node node : list){
					ASTPrimaryExpression astPrimaryExpression = (ASTPrimaryExpression)node;
					if(astPrimaryExpression.isMethod()){
						if(astPrimaryExpression.getImage().equals(MethodName)){
							target.add(vexNode);
							break;
						}
					}
				}
			}else if(vexNode.name.contains("while_head_")){
				ASTIterationStatement astIterationStatement = (ASTIterationStatement)vexNode.treenode;
				ASTExpression expression = (ASTExpression)astIterationStatement.jjtGetChild(0);
				List<Node> list = expression.findChildrenOfType(ASTPrimaryExpression.class);
				for(Node node : list){
					ASTPrimaryExpression astPrimaryExpression = (ASTPrimaryExpression)node;
					if(astPrimaryExpression.isMethod()){
						if(astPrimaryExpression.getImage().equals(MethodName)){
							target.add(vexNode);
							break;
						}
					}
				}
			}else if(vexNode.name.contains("for_head_")){
				ASTIterationStatement astIterationStatement = (ASTIterationStatement)vexNode.treenode;
				ASTExpression expression = (ASTExpression)astIterationStatement.jjtGetChild(1);
				List<Node> list = expression.findChildrenOfType(ASTPrimaryExpression.class);
				for(Node node : list){
					ASTPrimaryExpression astPrimaryExpression = (ASTPrimaryExpression)node;
					if(astPrimaryExpression.isMethod()){
						if(astPrimaryExpression.getImage().equals(MethodName)){
							target.add(vexNode);
							break;
						}
					}
				}
			}else{
				List<Node> list = vexNode.treenode.findChildrenOfType(ASTPrimaryExpression.class);
				for(Node node : list){
					ASTPrimaryExpression astPrimaryExpression = (ASTPrimaryExpression)node;
					if(astPrimaryExpression.isMethod()){
						if(astPrimaryExpression.getImage().equals(MethodName)){
							target.add(vexNode);
							break;
						}
					}
				}
			}
		}
		for(VexNode vexNode : target){
			ArrayList<ArrayList<VexNode>> allBeginToTarget = new ArrayList<>();
			ArrayList<VexNode> beginToTarget = new ArrayList<>();
			visitBeginToTargetPointBasisPathHelp(allBeginToTarget, beginToTarget, vexNode, vexNode);
			hashMap.put(vexNode, allBeginToTarget);
			// �����ʱ�־�������û�false
			for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
				VexNode n = e.nextElement();
				n.setVisited(false);
			}
		}
		
		//���Դ�ӡ���
		Iterator iterator = hashMap.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry entry = (Map.Entry)iterator.next();
			VexNode key = (VexNode)entry.getKey();
			ArrayList<ArrayList<VexNode>> list = (ArrayList<ArrayList<VexNode>>)entry.getValue();
			System.out.println("�ڵ㣺" + key.name);
			int i = 1;
			for(ArrayList<VexNode> list2 : list){
				System.out.print("��" + i + "����");
				for(VexNode nodee : list2){
					System.out.print(nodee.name + "->");
				}
				System.out.println();
				i++;
			}
		}
		
		return hashMap;
	}
	
	//add by ALUO
	/** ��ȡ����������ͼ�к�����ڵ�������������һ���������õ�Ļ���·�� **/
	public void visitBeginToTargetPointBasisPathHelp(ArrayList<ArrayList<VexNode>> allBasisPathList,
			ArrayList<VexNode> basisPathList, VexNode curNode, VexNode targetNode){
		if(curNode == null) {
			System.out.println("��ȡ�ڵ���ִ���");
			return;
		}
		if(curNode == targetNode && curNode.visited){
			return;
		}
		if(curNode.inedges.size() == 0){
			curNode.setVisited(true);
			basisPathList.add(curNode);
			Collections.reverse(basisPathList);
			allBasisPathList.add(basisPathList);
		} else if(curNode.inedges.size() == 1){  //˳��ִ��
			curNode.setVisited(true);
			basisPathList.add(curNode);
			Enumeration<String> e = curNode.inedges.keys();
			while(e.hasMoreElements()){
				Edge curEdge = (Edge)curNode.inedges.get(e.nextElement());
				VexNode nextNode = curEdge.tailnode;
				visitBeginToTargetPointBasisPathHelp(allBasisPathList, basisPathList, nextNode, targetNode);
			}
		} else if(curNode.inedges.size() == 2){  //�������������һ�ַ�֧��䣬 �ڶ���ѭ�����
			if(curNode.visited){  //�Ѿ����ʹ��ķ�֧�ڵ㣬�����һ����֧
				if(curNode.name.contains("while_head_") 
						|| curNode.name.contains("do_while_head_") 
						|| curNode.name.contains("for_head_") ){
					Enumeration<String> e = curNode.inedges.keys();
					while(e.hasMoreElements()){
						Edge curEdge = (Edge)curNode.inedges.get(e.nextElement());
						VexNode nextNode = curEdge.tailnode;
						if(Integer.parseInt(nextNode.name.split("_")[nextNode.name.split("_").length - 1])
								> Integer.parseInt(curNode.name.split("_")[curNode.name.split("_").length - 1])){
							continue;
						}else if(Integer.parseInt(nextNode.name.split("_")[nextNode.name.split("_").length - 1])
								< Integer.parseInt(curNode.name.split("_")[curNode.name.split("_").length - 1])){
							basisPathList.add(curNode);
							visitBeginToTargetPointBasisPathHelp(allBasisPathList, basisPathList, nextNode, targetNode);
							break;	
						}
					}
				}else{
					Enumeration<String> e = curNode.inedges.keys();
					while(e.hasMoreElements()){
						Edge curEdge = (Edge)curNode.inedges.get(e.nextElement());
						VexNode nextNode = curEdge.tailnode;
						basisPathList.add(curNode);
						visitBeginToTargetPointBasisPathHelp(allBasisPathList, basisPathList, nextNode, targetNode);
						break;
					}
				}
			} else {  //û�з��ʹ��ķ�֧�ڵ㣬��ٷ�֧����
				curNode.setVisited(true);
				Enumeration<String> e = curNode.inedges.keys();
				basisPathList.add(curNode);
				while(e.hasMoreElements()){
					Edge curEdge = (Edge)curNode.inedges.get(e.nextElement());
					VexNode nextNode = curEdge.tailnode;
					ArrayList<VexNode> newBasisPathList = new ArrayList<VexNode>(basisPathList);
					visitBeginToTargetPointBasisPathHelp(allBasisPathList, newBasisPathList, nextNode, targetNode);
				}
			}
		} else if(curNode.inedges.size() > 2){  //Switch���
			if(curNode.visited){  //�Ѿ����ʹ��ķ�֧�ڵ㣬�߳��߼��ϵ���ֵС�Ľڵ�
				Enumeration<String> e = curNode.inedges.keys();
				while(e.hasMoreElements()){
					Edge curEdge = (Edge)curNode.inedges.get(e.nextElement());
					VexNode nextNode = curEdge.tailnode;
					basisPathList.add(curNode);
					visitBeginToTargetPointBasisPathHelp(allBasisPathList, basisPathList, nextNode, targetNode);
					break;
				}
				
			} else {  //û�з��ʹ��Ľڵ㣬���з�֧����
				curNode.setVisited(true);
				Enumeration<String> e = curNode.inedges.keys();
				basisPathList.add(curNode);
				while(e.hasMoreElements()){
					Edge curEdge = (Edge)curNode.inedges.get(e.nextElement());
					VexNode nextNode = curEdge.tailnode;
					ArrayList<VexNode> newBasisPathList = new ArrayList<VexNode>(basisPathList);
					visitBeginToTargetPointBasisPathHelp(allBasisPathList, newBasisPathList, nextNode, targetNode);
				}
			}
		}
	}
	
	//add by ALUO
	/**��ȡ��������·��**/
	public ArrayList<ArrayList<VexNode>> visitBasisPath(){
		ArrayList<ArrayList<VexNode>> allBasisPathList = new ArrayList<ArrayList<VexNode>>();
		ArrayList<VexNode> basisPathList = new ArrayList<VexNode>();
		// �ҵ�������ͼ��ڿ�ʼ
		VexNode first = null;
		
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode n = (VexNode) e.nextElement();
			if (!n.getVisited() && n.inedges.size() == 0) {
				first = n;
				break;
			}
		}
		if (first == null) {
			throw new RuntimeException("������ͼ��ڴ���");
		}
		//Stack<VexNode> stack = new Stack<VexNode>();
		visitBasisPathHelp(allBasisPathList, basisPathList, first);
		// �����ʱ�־�������û�false
		for (Enumeration<VexNode> e = nodes.elements(); e.hasMoreElements();) {
			VexNode n = e.nextElement();
			n.setVisited(false);
		}
		
		/*
		 *  ���Ի���·�����ɽ������ӡ���ļ���
		 */
//		try {
//			FileWriter fw = new FileWriter("D:\\workspace\\DTSEmbed20171030\\temp\\Basis.txt", true);
//			StringBuilder sb = new StringBuilder();
//			sb.append("Method:" + allBasisPathList.get(0).get(0).name.split("func_head_")[1].split("_0")[0] + "\r\n");
//			int i = 1;
//			for(ArrayList<VexNode> temp : allBasisPathList){
//				sb.append("��" + i + "����");
//				for(VexNode temp2 : temp){
//					sb.append(temp2.name + "->");
//				}
//				sb.append("\r\n");
//				i++;
//			}
//			sb.append("OVER!\r\n");
//			fw.write(sb.toString());
//			fw.close();
//		} catch (IOException e) {
//			// TODO �Զ����ɵ� catch ��
//			e.printStackTrace();
//		}
		int i = 1;
		for(ArrayList<VexNode> temp : allBasisPathList){
			System.out.print("��" + i + "����");
			for(VexNode temp2 : temp){
				System.out.print(temp2.name + "->");
			}
			System.out.println();
			i++;
		}
		System.out.println("OVER!");
		
		return allBasisPathList;
	}
	
	//add by ALUO
	/**��ȡ��������·������ʵ��**/
	public void visitBasisPathHelp(ArrayList<ArrayList<VexNode>> allBasisPathList, ArrayList<VexNode> basisPathList, VexNode curNode){
		if(curNode == null) {
			System.out.println("��ȡβ�ڵ���ִ���");
			return;
		}
		if(curNode.outedges.size() == 0){
			curNode.setVisited(true);
			basisPathList.add(curNode);
			allBasisPathList.add(basisPathList);
		} else if(curNode.outedges.size() == 1){  //˳��ִ��
			curNode.setVisited(true);
			basisPathList.add(curNode);
			Enumeration<String> e = curNode.outedges.keys();
			while(e.hasMoreElements()){
				Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
				VexNode nextNode = curEdge.headnode;
				visitBasisPathHelp(allBasisPathList, basisPathList, nextNode);
			}
		} else if(curNode.outedges.size() == 2){  //�������������һ�ַ�֧��䣬 �ڶ���ѭ�����
			if(curNode.visited){  //�Ѿ����ʹ��ķ�֧�ڵ㣬�����һ����֧
				if(curNode.name.contains("for_head")){
					Enumeration<String> e = curNode.outedges.keys();
					while(e.hasMoreElements()){
						Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
						VexNode nextNode = curEdge.headnode;
						if(nextNode.name.contains("for_out")){
							basisPathList.add(curNode);
							visitBasisPathHelp(allBasisPathList, basisPathList, nextNode);
							break;
						}else{
							continue;
						}
					}
				}else if(curNode.name.contains("while_head")){
					Enumeration<String> e = curNode.outedges.keys();
					while(e.hasMoreElements()){
						Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
						VexNode nextNode = curEdge.headnode;
						if(nextNode.name.contains("while_out")){
							basisPathList.add(curNode);
							visitBasisPathHelp(allBasisPathList, basisPathList, nextNode);
							break;
						}else{
							continue;
						}
					}
				}else if(curNode.name.contains("do_while_out1_")){
					Enumeration<String> e = curNode.outedges.keys();
					while(e.hasMoreElements()){
						Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
						VexNode nextNode = curEdge.headnode;
						if(nextNode.name.contains("do_while_out2_")){
							basisPathList.add(curNode);
							visitBasisPathHelp(allBasisPathList, basisPathList, nextNode);
							break;
						}else{
							continue;
						}
					}
				}else{
					Enumeration<String> e = curNode.outedges.keys();
					while(e.hasMoreElements()){
						Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
						VexNode nextNode = curEdge.headnode;
						basisPathList.add(curNode);
						visitBasisPathHelp(allBasisPathList, basisPathList, nextNode);
						break;
					}
				}
			} else {  //û�з��ʹ��ķ�֧�ڵ㣬��ٷ�֧����
				curNode.setVisited(true);
				Enumeration<String> e = curNode.outedges.keys();
				basisPathList.add(curNode);
				while(e.hasMoreElements()){
					Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
					VexNode nextNode = curEdge.headnode;
					ArrayList<VexNode> newBasisPathList = new ArrayList<VexNode>(basisPathList);
					visitBasisPathHelp(allBasisPathList, newBasisPathList, nextNode);
				}
			}
		} else if(curNode.outedges.size() > 2){  //Switch���
			if(curNode.visited){  //�Ѿ����ʹ��ķ�֧�ڵ㣬�߳��߼��ϵĵ�һ���߷�֧
				Enumeration<String> e = curNode.outedges.keys();
				while(e.hasMoreElements()){
					Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
					VexNode nextNode = curEdge.headnode;
					basisPathList.add(curNode);
					visitBasisPathHelp(allBasisPathList, basisPathList, nextNode);
					break;
				}
			} else {  //û�з��ʹ��Ľڵ㣬���з�֧����
				curNode.setVisited(true);
				Enumeration<String> e = curNode.outedges.keys();
				basisPathList.add(curNode);
				while(e.hasMoreElements()){
					Edge curEdge = (Edge)curNode.outedges.get(e.nextElement());
					VexNode nextNode = curEdge.headnode;
					ArrayList<VexNode> newBasisPathList = new ArrayList<VexNode>(basisPathList);
					visitBasisPathHelp(allBasisPathList, newBasisPathList, nextNode);
				}
			}
		}
	}
}