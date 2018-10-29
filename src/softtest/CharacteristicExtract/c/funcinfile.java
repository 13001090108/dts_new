package softtest.CharacteristicExtract.c;

import java.io.File;
import java.util.*;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CEdge;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import softtest.interpro.c.InterContext;


/** 
 * @author 
 * Miss_lizi
 * 提取文件内函数调用关系
 */
public class funcinfile {
	
	public static void main(String[] args) throws Exception{
		//String filePath = "C:/Users/Miss_Lizi/Desktop/testCallGraph.c";
		Graph_Info h = new Graph_Info();
		funcinfile ff = new funcinfile();
		//List<CVexNode> list_cvex = h.getCVexNode(filePath);

		String filePath1 = "C:/Users/Miss_Lizi/Desktop/uucp-1.07";
		String filePath2 = "C:/Users/Miss_Lizi/Desktop/depchain/depchain.c";
		Graph_Info hh = new Graph_Info();
		
		//List<CVexNode> list_cvex1 = hh.getCVexNode(filePath);
		//ff.getAll(filePath);
		
		//System.out.println(ff.getAll(filePath));
		System.out.println(ff.getAll(filePath2));
//		for(int i = 0; i < ff.getFuncFeatures(filePath2).size(); i++){
//			System.out.println(ff.getFuncFeatures(filePath2).get(i));
//		}
		//System.out.println(ff.getFuncFeatures(filePath2));
//		System.out.println(ff.Features);
//		System.out.println(ff.Relation_after);
//		System.out.println(ff.Relation_before);
		
		
	}

	/** 文件中函数的总个数*/
	private int num = 0;
	
	/** 函数调用图中节点特征*/
	private String Features = "null";
	
	/** 函数调用图中节点关系（后继）*/
	private String Relation_after = "null";
	
	/** 函数调用图中节点关系（前驱）*/
	private String Relation_before = "null";
	
	
	
	
	/** 得到所有的特征信息*/
	public String getAll(String filePath)throws Exception{
		StringBuffer res = new StringBuffer();
		Graph_Info h = new Graph_Info();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
		 
		num = list_cvex.size();
		getAfterRelation(list_cvex);
		getBeforeRelation(list_cvex);
		getFeatures(filePath,list_cvex);
		
		
		res.append(filePath + "@");
		res.append(list_cvex.size() + "@");
		res.append(getFeatures(filePath,list_cvex) + "@");
		res.append(getAfterRelation(list_cvex) + "@" + getBeforeRelation(list_cvex));
		
		return res.toString();
	}
	
	public List<String> getFuncFeatures(String filePath)throws Exception{
		StringBuffer res = new StringBuffer();
		List<String> res1 = new ArrayList<String>();
		FunctionFeatures f = new FunctionFeatures();
		Graph_Info h = new Graph_Info();
		Func_Features ff = new Func_Features();
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
		//Random r = new Random(14);
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				res.setLength(0);
				res.append(filePath + "#");
				res.append(node.getImage());
				//System.out.println(c.name);
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				res.append("#" + f.getFunctionStructFeature(g));
				res.append("#" + rotatingHash(ff.getFunctionFeatures(filePath, function.getImage())));
				//把后面的功能特征码最后加到这里
				//res.append("#" + r.nextInt());
				res.append("#" + node.getBeginFileLine());
				res.append("#" + node.getEndFileLine());
				res1.add(res.toString());
			}
		}
		//System.out.println(res);
		return res1;
	}
	
	/** 得到所有信息，提供给外部对比的接口*/
	public List<String> getFileFeature(String filePath)throws Exception{
		Graph_Info h = new Graph_Info();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
		List<String> res = new ArrayList<String>();
		res.add(filePath);
		res.add(String.valueOf(list_cvex.size()));
		res.add(getFeatures(filePath,list_cvex));
		res.add(getAfterRelation(list_cvex));
		res.add(getAfterRelation(list_cvex));
		return res;
	}
	
	
	public String getFeatures1(String filePath, List<CVexNode> list_cvex) throws Exception{
		FunctionFeatures f = new FunctionFeatures();
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		StringBuffer str1 = new StringBuffer();
		String[] str = filePath.split("/");
 		String file = str[str.length - 1];
 		file = file.substring(0, file.length() - 2);
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				//System.out.println(c.name);
				if(str1.length() == 0){
					str1.append(file + "_" + node.getImage());
				}else{
					str1.append("#" + file + "_" + node.getImage());
				}
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				str1.append("," + f.getFunctionStructFeature(g));
				//把后面的功能特征码最后加到这里
				str1.append("," + -100);
			}
		}
		//System.out.println(str1);
		return str1.toString();
	}
	
	/** 得到函数结构和功能特征码*/
	public String getFeatures(String filePath, List<CVexNode> list_cvex) throws Exception{
		FunctionFeatures f = new FunctionFeatures();
		Func_Features ff = new Func_Features();
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		StringBuffer str1 = new StringBuffer();
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				System.out.println(c.name);
				if(str1.length() == 0){
					str1.append(getNumber(c));
				}else{
					str1.append("#" + getNumber(c));
				}
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				str1.append("," + f.getFunctionStructFeature(g));
				//把后面的功能特征码最后加到这里
				str1.append("," + rotatingHash(ff.getFunctionFeatures(filePath, function.getImage())));
			}
		}
		if(str1.length() != 0){
			Features = str1.toString();
		}
		return str1.toString();
	}
	
	/** 得到函数之间节点关系（后继）*/
	public String getAfterRelation(List<CVexNode> list_cvex){
		StringBuffer str = new StringBuffer();
		for(CVexNode c : list_cvex){
			if(str.length() == 0){
				str.append(getNumber(c));
			}else{
				str.append("#" + getNumber(c));
			}
			Enumeration enuout = c.getOutedges().elements();
			while(enuout.hasMoreElements()) { 
				CEdge e = (CEdge)enuout.nextElement();
				CVexNode head = e.getHeadNode();
				str.append("," + getNumber(head));
			}
		}
		if(str.length() != 0){
			Relation_after = str.toString();
		}
		return str.toString();
	}
	
	/** 得到函数之间节点关系（前驱）*/
	public String getBeforeRelation(List<CVexNode> list_cvex){
		StringBuffer str = new StringBuffer();
		for(CVexNode c : list_cvex){
			if(str.length() == 0){
				str.append(getNumber(c));
			}else{
				str.append("#" + getNumber(c));
			}
			Enumeration enuout = c.getInedges().elements();
			while(enuout.hasMoreElements()) { 
				CEdge e = (CEdge)enuout.nextElement();
				CVexNode tail = e.getTailNode();
				str.append("," + getNumber(tail));
			}
		}
		if(str.length() != 0){
			Relation_before = str.toString();
		}
		return str.toString();
	}
	
	
	/** 收集当前测试工程的所有源文件（.C源文件），并将搜索到的.C文件加入到待分析列表中*/
	public  static  List<String> collect(File srcFile,List<String> res) {
		if (srcFile.isFile() && srcFile.getName().matches(InterContext.SRCFILE_POSTFIX)) {
			res.add(srcFile.getPath());
		} else if (srcFile.isDirectory()) {
			File[] fs = srcFile.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collect(fs[i],res);
			}
		}
		return res;
	}
	
	/**将得到的hashcode进行一次位运算哈希函数*/
	public int rotatingHash(int code){
		String str = String.valueOf(code);
		int hash = str.length();
		for(int i = 0; i < str.length(); i++){
			hash = (hash<<4)^(hash>>28)^str.charAt(i);
		}
		return hash%500000;
	}
	
	/** 得到函数在文件内的编号*/
	public String getNumber(CVexNode c){
		String[] arr = c.getName().split("_");
		return arr[arr.length - 1];
	}
	
	/** 得到函数的后继关系*/
	public String getRelationAfter(){
		return this.Relation_after;
	}
	
	/** 得到函数的前驱关系*/
	public String getRelationBefore(){
		return this.Relation_before;
	}
}
