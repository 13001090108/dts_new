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
	
	/** 图中节点个数*/
	private int nodeNum = 0;
	/** 图中节点的信息，String为节点的名字，可以作为索引*/
	private HashMap<String,FunNode> node = new HashMap<String,FunNode>();
	
	/** 前驱关系集合*/
	public Hashtable<String, HashSet<String>> preNodes = new Hashtable<String, HashSet<String>>();

	/** 后继关系集合*/
	public Hashtable<String, HashSet<String>> postNodes = new Hashtable<String, HashSet<String>>();
	
	/** 入口节点集合*/
	public HashSet<String> entryNode = new HashSet<String>();
	
	/** 出口节点集合*/
	public HashSet<String> outNode = new HashSet<String>();
	
	/** 在图中孤立的节点，即既没有前驱也没有后继的节点*/
	public HashSet<String> isolatedNode = new HashSet<String>();
	
	/** 获取入口节点集合，也就是不存在前驱节点*/
	private void setEntryNode(){
		for(Object s : preNodes.keySet()){
			if(preNodes.get(s) == null || preNodes.get(s).size() <= 0){
				entryNode.add((String)s);
			}
		}
	}
	
	/** 获取出口节点集合，也就是不存在后继节点*/
	private void setOutNode(){
		for(Object s : postNodes.keySet()){
			if(postNodes.get(s) == null || postNodes.get(s).size() <= 0){
				outNode.add((String)s);
			}
		}
	}
	
	/** 获取孤立的节点，也就是既是出口节点，又是入口节点的点****/
	private void setIsolateNode(){
		isolatedNode.addAll(entryNode);
		isolatedNode.retainAll(outNode);
	}
	
	/**清空图中的信息*/
	public void clearGraph(){
		this.nodeNum = 0;
		this.node.clear();
		this.outNode.clear();
		this.entryNode.clear();
		this.postNodes.clear();
		this.preNodes.clear();
		this.isolatedNode.clear();
	}
	
	
	/**要对函数调用图进行构造，因为数据库中的存放形式是三个字符串
	 * 1.节点个数   
	 * 2.节点的信息   f1,1,2 # f2,2,3 这种形式;# 作为节点分隔,  f1表示函数名，后边两个数分别表示函数的结构特征和功能特征。
	 * 3.节点的后继关系    f1,f2,f3 # f2,f3；#作为分隔，f1为初始节点，其后继节点有f2,f3.
	 * ***/
	public CallGraph(){}
	
	/**构造函数，可用于文件级别的相似性判定，因为这里没有提供独立的节点，要自己去提取一下*/
	public CallGraph(String nodeNum,String nodeInfo, String nodeRelationAfter,
			String nodeRelationBefore){
		this.nodeNum = Integer.valueOf(nodeNum);
		InitGraph(nodeInfo,nodeRelationAfter,nodeRelationBefore);
		this.setIsolateNode();
	}
	
	/**构造函数，可用于程序级别的相似性判定，这里提供独立的节点信息*/
	public CallGraph(int nodeNum, String nodeInfo, String nodeRelationAfter,
			String nodeRelationBefore,String isolateNode){
		this.nodeNum = nodeInfo.split("#").length;
		InitGraph(nodeInfo,nodeRelationAfter,nodeRelationBefore);
		/**建立孤立节点的结构*/
		isolatedNode.addAll(Arrays.asList(isolateNode.split("#")));
	}
	
	public void InitGraph(String nodeInfo, String nodeRelationAfter,
			String nodeRelationBefore){
		/**建立节点信息*/
		if(!nodeInfo.equals("") || !nodeInfo.equals("")){
			String []tmp = nodeInfo.split("#");
			this.nodeNum = tmp.length;
			for(String funNode : tmp){
				String []funFea = funNode.split(",");
				FunNode f = new FunNode(funFea[0],Double.valueOf(funFea[1]),Double.valueOf(funFea[2]));
				node.put(funFea[0], f);
			}
		}
		/**建立节点关系, 后继关系*/
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
		/**建立节点关系, 前序关系*/
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
		/**找到出口节点和入口节点*/
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
		/**找到两个图中匹配的节点**/
		//如果找不到匹配的节点，那么说明两个图是不相似的；
		if(matchNodes.isEmpty()){return "false";}
		//如果两图节点个数相等，并且匹配的节点数目也相同的话，说明是完全相等的，可以判定为相似。
		if(this.nodeNum == callGraph.nodeNum  && matchNodes.size() == this.nodeNum){
			return "total";
		}
		//如果达到匹配的节点的数目等于图中节点数目的话，可以认为存在子图和超图的情况，说明相似
		if(matchNodes.size() == this.nodeNum || matchNodes.size() == callGraph.nodeNum){
			return "subset";
		}
		//如果图中所有的节点都是孤立节点的话，这个时候，可以考虑一个阈值，比如相似程度为百分之多少，可以判定相似
		//这里就是随便设定了一个阈值为0.6，后期可以修改
		//可以看做是部分相同的一个特例情况
		if(this.isolatedNode.size() == this.nodeNum && matchNodes.size() / this.nodeNum > 0.6){
			if(callGraph.isolatedNode.size() == callGraph.nodeNum || matchNodes.size() / callGraph.nodeNum > 0.6){
				return "isloate";}
		}
		//以下是为了处理存在交叉相同的情况	
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
	
	/**********以下暂时先没用了，是过去写的，但是舍不得删哇   ********/
	/** 获得指定节点的一个没有访问的相邻节点，可能返回null*/
	public FunNode getAdjUnvisitedVertex(FunNode f) {
		HashSet<String> list = postNodes.get(f.funName);
		//如果此节点的后继节点结合为空，则返回空
		if(list.size() <= 0){return null;}
		
		for (String post : list) {
			if(node.get(post).visited == false){
				node.get(post).visited = true;
				return node.get(post);
			}
		}
		
		//如果所有的后继节点都访问过了，表示当前节点已经遍历完毕，此时所有的visit都要重新置为初值。
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
		/**先判断两个节点是不是相同的*/
		if(!this.node.get(node1).isMatch(f.node.get(node2))){return false;}
		
		/**如果是出口节点的话， 可以判定相似*/
		if(this.outNode.contains(node1) && f.outNode.contains(node2)){return true;}
		
		/**如果一个是出口节点而另外一个不是出口节点， 可以判定不相似*/
		if(this.outNode.contains(node1) || f.outNode.contains(node2)){return false;}
		
		/**对其后继节点进行判断，由于考虑到子图的情况，所以只要是有匹配的即可*/
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
	
	/** 深度遍历找路径*/
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
