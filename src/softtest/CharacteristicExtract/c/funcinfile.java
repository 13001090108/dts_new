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
 * ��ȡ�ļ��ں������ù�ϵ
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

	/** �ļ��к������ܸ���*/
	private int num = 0;
	
	/** ��������ͼ�нڵ�����*/
	private String Features = "null";
	
	/** ��������ͼ�нڵ��ϵ����̣�*/
	private String Relation_after = "null";
	
	/** ��������ͼ�нڵ��ϵ��ǰ����*/
	private String Relation_before = "null";
	
	
	
	
	/** �õ����е�������Ϣ*/
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
				//�Ѻ���Ĺ������������ӵ�����
				//res.append("#" + r.nextInt());
				res.append("#" + node.getBeginFileLine());
				res.append("#" + node.getEndFileLine());
				res1.add(res.toString());
			}
		}
		//System.out.println(res);
		return res1;
	}
	
	/** �õ�������Ϣ���ṩ���ⲿ�ԱȵĽӿ�*/
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
				//�Ѻ���Ĺ������������ӵ�����
				str1.append("," + -100);
			}
		}
		//System.out.println(str1);
		return str1.toString();
	}
	
	/** �õ������ṹ�͹���������*/
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
				//�Ѻ���Ĺ������������ӵ�����
				str1.append("," + rotatingHash(ff.getFunctionFeatures(filePath, function.getImage())));
			}
		}
		if(str1.length() != 0){
			Features = str1.toString();
		}
		return str1.toString();
	}
	
	/** �õ�����֮��ڵ��ϵ����̣�*/
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
	
	/** �õ�����֮��ڵ��ϵ��ǰ����*/
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
	
	
	/** �ռ���ǰ���Թ��̵�����Դ�ļ���.CԴ�ļ�����������������.C�ļ����뵽�������б���*/
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
	
	/**���õ���hashcode����һ��λ�����ϣ����*/
	public int rotatingHash(int code){
		String str = String.valueOf(code);
		int hash = str.length();
		for(int i = 0; i < str.length(); i++){
			hash = (hash<<4)^(hash>>28)^str.charAt(i);
		}
		return hash%500000;
	}
	
	/** �õ��������ļ��ڵı��*/
	public String getNumber(CVexNode c){
		String[] arr = c.getName().split("_");
		return arr[arr.length - 1];
	}
	
	/** �õ������ĺ�̹�ϵ*/
	public String getRelationAfter(){
		return this.Relation_after;
	}
	
	/** �õ�������ǰ����ϵ*/
	public String getRelationBefore(){
		return this.Relation_before;
	}
}
