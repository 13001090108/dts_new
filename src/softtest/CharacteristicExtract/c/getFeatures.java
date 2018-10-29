package softtest.CharacteristicExtract.c;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import softtest.SDataBase.c.DataBaseAccess;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Graph;
import softtest.interpro.c.InterContext;


/** 
 * @author 
 * Miss_lizi
 * 提取所有特征码
 */
public class getFeatures {
	/** 语句块特征码集合*/
	private List<String> list_stms = new ArrayList<String>();
	
	/** 函数结构特征码集合*/
	private List<String> list_func_struct = new ArrayList<String>();
	
	/** 函数度量元特征集合*/
	private List<String> list_funcinfo = new ArrayList<String>();
	
	/** 文件对应特征信息*/
	private String fileinfo = "";
	
	/** 文件内所有函数的结构和功能特征码*/
	private List<String> list_funcinfile = new ArrayList<String>();

	/** 函数调用图中节点关系（后继）*/
	private List<String> list_Relation_after = new ArrayList<String>();
	
	/** 函数调用图中节点关系（前驱）*/
	private List<String> list_Relation_before = new ArrayList<String>();
	
	public static void main(String[] args) throws Exception{
		getFeatures get = new getFeatures();
		String filePath = "C:/Users/Miss_Lizi/Desktop/global.c";
		get.getAll(filePath);
//		for(int i = 0; i < get.list_func_struct.size(); i++){
//			System.out.println(get.list_func_struct.get(i));
//		}
	}
	
	/** 得到所有的特征信息*/
	public void getAll(String filepath)throws Exception{
		File srcFile = new File(filepath);
		FunctionFeatures f = new FunctionFeatures();
		StatementFeature sf = new StatementFeature();
		funcinfile ff = new funcinfile();
		List<String> file = new ArrayList<String>();
		file = collect(srcFile,file);
		for(String file_c : file){
			Graph_Info gi = new Graph_Info();
			List<CVexNode> list_cvex = new ArrayList<CVexNode>();
			list_cvex = gi.getCVexNode(file_c);
			ControlFlowVisitor cfv = new ControlFlowVisitor(file_c);
			ControlFlowData flow = new ControlFlowData();
			fileinfo = gi.getFunction(file_c);
			ff.getAll(file_c);
			StringBuffer str1 = new StringBuffer();
			for(CVexNode c : list_cvex){
				SimpleNode node = c.getMethodNameDeclaration().getNode();
				//List<String> a = new ArrayList<String>();
				if (node instanceof ASTFunctionDefinition){
					if(str1.length() == 0){
						str1.append(getNumber(c));
					}else{
						str1.append("#" + getNumber(c));
					}
					ASTFunctionDefinition function = (ASTFunctionDefinition)node;
					cfv.visit((ASTFunctionDefinition)node, flow);
					Graph g = ((ASTFunctionDefinition) node).getGraph();
					String str = file_c + "#" + function.getImage() + "#";
					list_func_struct.add(str + f.getFunctionStructFeature(g));
					list_stms.addAll(sf.getAllFeaturesInFunction(g, function, file_c));
					list_funcinfo.addAll(gi.getAllFuncInfo(file_c));
					str1.append("," + f.getFunctionStructFeature(g));
					//把后面的功能特征码最后加到这里
					str1.append("," + 0);
				}
			}
			list_funcinfile.add(str1.toString());
			list_Relation_after.add(ff.getRelationAfter());
			list_Relation_before.add(ff.getRelationBefore());
		}
	
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
	
	
	public String getNumber(CVexNode c){
		String[] arr = c.getName().split("_");
		return arr[arr.length - 1];
	}
	
	
	/** 返回语句块特征码*/
	public List<String> getStmsStatement(){
		return this.list_stms;
	}
	
	/** 返回函数结构特征码*/
	public List<String> getFuncStruct(){
		return this.list_func_struct;
	}
	
	/** 返回函数度量特征信息*/
	public List<String> getFuncInfo(){
		return this.list_funcinfo;
	}
	
	/** 返回文件信息*/
	public String getFileInfo(){
		return this.fileinfo;
	}
}
