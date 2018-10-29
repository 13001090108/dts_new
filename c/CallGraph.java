package softtest.SimDetection.c;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import softtest.CharacteristicExtract.c.funcinfile;

public class CallGraph {
	
	/** ͼ�нڵ����*/
	private int nodeNum = 0;
	/** ͼ�нڵ����Ϣ��StringΪ�ڵ�����֣�������Ϊ����*/
	private HashMap<String,FunNode> node = new HashMap<String,FunNode>();
	
	/** ǰ����ϵ����*/
	public Hashtable<String, HashSet<String>> preNodes = new Hashtable<String, HashSet<String>>();

	/** ��̹�ϵ����*/
	public Hashtable<String, HashSet<String>> postNodes = new Hashtable<String, HashSet<String>>();
	
	/** ��ڽڵ㼯��*/
	public HashSet<String> entryNode = new HashSet<String>();
	
	/** ���ڽڵ㼯��*/
	public HashSet<String> outNode = new HashSet<String>();
	
	/** ��ͼ�й����Ľڵ㣬����û��ǰ��Ҳû�к�̵Ľڵ�*/
	public HashSet<String> isolatedNode = new HashSet<String>();
	
	/** ��ȡ��ڽڵ㼯�ϣ�Ҳ���ǲ�����ǰ���ڵ�*/
	private void setEntryNode(){
		for(Object s : preNodes.keySet()){
			if(preNodes.get(s) == null || preNodes.get(s).size() <= 0){
				entryNode.add((String)s);
			}
		}
	}
	
	/** ��ȡ���ڽڵ㼯�ϣ�Ҳ���ǲ����ں�̽ڵ�*/
	private void setOutNode(){
		for(Object s : postNodes.keySet()){
			if(postNodes.get(s) == null || postNodes.get(s).size() <= 0){
				outNode.add((String)s);
			}
		}
	}
	
	/** ��ȡ�����Ľڵ㣬Ҳ���Ǽ��ǳ��ڽڵ㣬������ڽڵ�ĵ�****/
	private void setIsolateNode(){
		isolatedNode.addAll(entryNode);
		isolatedNode.retainAll(outNode);
	}
	
	/**���ͼ�е���Ϣ*/
	public void clearGraph(){
		this.nodeNum = 0;
		this.node.clear();
		this.outNode.clear();
		this.entryNode.clear();
		this.postNodes.clear();
		this.preNodes.clear();
		this.isolatedNode.clear();
	}
	
	
	/**Ҫ�Ժ�������ͼ���й��죬��Ϊ���ݿ��еĴ����ʽ�������ַ���
	 * 1.�ڵ����   
	 * 2.�ڵ����Ϣ   f1,1,2 # f2,2,3 ������ʽ;# ��Ϊ�ڵ�ָ�,  f1��ʾ������������������ֱ��ʾ�����Ľṹ�����͹���������
	 * 3.�ڵ�ĺ�̹�ϵ    f1,f2,f3 # f2,f3��#��Ϊ�ָ���f1Ϊ��ʼ�ڵ㣬���̽ڵ���f2,f3.
	 * ***/
	public CallGraph(){}
	
	/**���캯�����������ļ�������������ж�����Ϊ����û���ṩ�����Ľڵ㣬Ҫ�Լ�ȥ��ȡһ��*/
	public CallGraph(String nodeNum,String nodeInfo, String nodeRelationAfter,
			String nodeRelationBefore){
		this.nodeNum = Integer.valueOf(nodeNum);
		InitGraph(nodeInfo,nodeRelationAfter,nodeRelationBefore);
		this.setIsolateNode();
	}
	
	/**���캯���������ڳ��򼶱���������ж��������ṩ�����Ľڵ���Ϣ*/
	public CallGraph(int nodeNum, String nodeInfo, String nodeRelationAfter,
			String nodeRelationBefore,String isolateNode){
		this.nodeNum = nodeInfo.split("#").length;
		InitGraph(nodeInfo,nodeRelationAfter,nodeRelationBefore);
		/**���������ڵ�Ľṹ*/
		isolatedNode.addAll(Arrays.asList(isolateNode.split("#")));
	}
	
	public void InitGraph(String nodeInfo, String nodeRelationAfter,
			String nodeRelationBefore){
		/**�����ڵ���Ϣ*/
		if(!nodeInfo.equals("") || !nodeInfo.equals("")){
			String []tmp = nodeInfo.split("#");
			this.nodeNum = tmp.length;
			for(String funNode : tmp){
				String []funFea = funNode.split(",");
				FunNode f = new FunNode(funFea[0],Double.valueOf(funFea[1]),Double.valueOf(funFea[2]));
				node.put(funFea[0], f);
			}
		}
		/**�����ڵ��ϵ, ��̹�ϵ*/
		if(nodeRelationAfter.equals("") || !nodeRelationAfter.equals("")){
			String []tmp = nodeRelationAfter.split("#");
			for(String relate : tmp){
				String []r = relate.split(",");
				HashSet<String> set = new HashSet<String>();
				//Collections.addAll(set, r);
				for(int i = 1; i < r.length; i++){
					set.add(r[i]);
				}
				this.postNodes.put(r[0], set);
			}
		}
		/**�����ڵ��ϵ, ǰ���ϵ*/
		if(nodeRelationBefore.equals("") || !nodeRelationBefore.equals("")){
			String []tmp = nodeRelationBefore.split("#");
			for(String relate : tmp){
				String []r = relate.split(",");
				HashSet<String> set = new HashSet<String>();
				for(int i = 1; i < r.length; i++){
					set.add(r[i]);
				}
				this.preNodes.put(r[0], set);
			}
		}
		/**�ҵ����ڽڵ����ڽڵ�*/
		setEntryNode();
		setOutNode();
	}
	
	public Hashtable<String, String> getMatchNodes(CallGraph callGraph){
		Hashtable<String, String> matchNodes = new Hashtable<String, String>();
		Iterator<String> ite1 = this.node.keySet().iterator();
		while(ite1.hasNext()){
			String fun1 = ite1.next();
			for(String fun2 : callGraph.node.keySet()){
				if(node.get(fun1).isMatch(callGraph.node.get(fun2))){
					matchNodes.put(fun1, fun2);
					callGraph.node.remove(fun2);
					break;
				}
			}
		}
		return matchNodes;
	}
	public Hashtable<String, String> matchNodes;
	public String dection(CallGraph callGraph){
		matchNodes = getMatchNodes(callGraph);
		/**�ҵ�����ͼ��ƥ��Ľڵ�**/
		//����Ҳ���ƥ��Ľڵ㣬��ô˵������ͼ�ǲ����Ƶģ�
		if(matchNodes.isEmpty()){return "false";}
		//�����ͼ�ڵ������ȣ�����ƥ��Ľڵ���ĿҲ��ͬ�Ļ���˵������ȫ��ȵģ������ж�Ϊ���ơ�
		if(this.nodeNum == callGraph.nodeNum  && matchNodes.size() == this.nodeNum){
			return "total";
		}
		//����ﵽƥ��Ľڵ����Ŀ����ͼ�нڵ���Ŀ�Ļ���������Ϊ������ͼ�ͳ�ͼ�������˵������
		if(matchNodes.size() == this.nodeNum || matchNodes.size() == callGraph.nodeNum){
			return "subset";
		}
		//���ͼ�����еĽڵ㶼�ǹ����ڵ�Ļ������ʱ�򣬿��Կ���һ����ֵ���������Ƴ̶�Ϊ�ٷ�֮���٣������ж�����
		//�����������趨��һ����ֵΪ0.6�����ڿ����޸�
		//���Կ����ǲ�����ͬ��һ���������
		if(this.isolatedNode.size() == this.nodeNum && matchNodes.size() / this.nodeNum > 0.6){
			if(callGraph.isolatedNode.size() == callGraph.nodeNum || matchNodes.size() / callGraph.nodeNum > 0.6){
				return "isloate";}
		}
		//������Ϊ�˴�����ڽ�����ͬ�����	
		if(matchNodes.size() / callGraph.nodeNum > 0.6 || matchNodes.size() / this.nodeNum > 0.6){
			return "part";
		}
		return "false";
		//return dfsMatchNodes(f);
	}
	public String matchNodesToString(){
		StringBuilder sb = new StringBuilder();
		//Hashtable<String, HashSet<String>>
		for(String key : matchNodes.keySet()){
			sb.append("[" + key + ": " + matchNodes.get(key));
			sb.append("]  ");
		}
		return sb.toString();
	}
	
	/**********������ʱ��û���ˣ��ǹ�ȥд�ģ������᲻��ɾ��   ********/
	/** ���ָ���ڵ��һ��û�з��ʵ����ڽڵ㣬���ܷ���null*/
	public FunNode getAdjUnvisitedVertex(FunNode f) {
		HashSet<String> list = postNodes.get(f.funName);
		//����˽ڵ�ĺ�̽ڵ���Ϊ�գ��򷵻ؿ�
		if(list.size() <= 0){return null;}
		
		for (String post : list) {
			if(node.get(post).visited == false){
				node.get(post).visited = true;
				return node.get(post);
			}
		}
		
		//������еĺ�̽ڵ㶼���ʹ��ˣ���ʾ��ǰ�ڵ��Ѿ�������ϣ���ʱ���е�visit��Ҫ������Ϊ��ֵ��
		for (String post : list) {
			node.get(post).visited = true;
		}
		return null;
	}
	
	public boolean isMatchNode(String node1, String node2, CallGraph f){
		if(this.node.get(node1).visited == true){
			return true;
		}
		this.node.get(node1).visited = true;
		/**���ж������ڵ��ǲ�����ͬ��*/
		if(!this.node.get(node1).isMatch(f.node.get(node2))){return false;}
		
		/**����ǳ��ڽڵ�Ļ��� �����ж�����*/
		if(this.outNode.contains(node1) && f.outNode.contains(node2)){return true;}
		
		/**���һ���ǳ��ڽڵ������һ�����ǳ��ڽڵ㣬 �����ж�������*/
		if(this.outNode.contains(node1) || f.outNode.contains(node2)){return false;}
		
		/**�����̽ڵ�����жϣ����ڿ��ǵ���ͼ�����������ֻҪ����ƥ��ļ���*/
		HashSet<String> postNodesOfNode1 = postNodes.get(node1);
		HashSet<String> postNodesOfNode2 = f.postNodes.get(node2);
		
		for(String post1 : postNodesOfNode1){
			String matchPost1 = this.getMatchNodes(f).get(post1);
			if(matchPost1 == null){return false;}
			if(isMatchNode(post1, matchPost1, f)){return true;}
			return false;
		}
		return false;
	}
	
	/** ��ȱ�����·��*/
	public LinkedList<LinkedList<String>> dfs(String s) {
		LinkedList<LinkedList<String>> res = new LinkedList<LinkedList<String>>();
		if(postNodes.get(s).size() <= 0 || outNode.contains(s)){
			LinkedList<String> seq = new LinkedList<String>();
			seq.add(s);
			res.add(seq);
			return res;
		}
		for(String next : postNodes.get(s)){
			LinkedList<LinkedList<String>> postSeq = dfs(next);
			for(LinkedList<String> list : postSeq){
				LinkedList<String> seq = new LinkedList<String>();
				seq.add(s);
				seq.addAll(list);
				res.add(seq);
			}
		}
		return res;
		
		
	}
	
	public void printNodeInfor(){
		for(String s : this.node.keySet()){
			System.out.println( s + " : " + node.get(s).functionVal + ", " + node.get(s).structVal);
		}
	}
}
