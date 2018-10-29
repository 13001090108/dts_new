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
 * ��ȡ����������
 */
public class getFeatures {
	/** ���������뼯��*/
	private List<String> list_stms = new ArrayList<String>();
	
	/** �����ṹ�����뼯��*/
	private List<String> list_func_struct = new ArrayList<String>();
	
	/** ��������Ԫ��������*/
	private List<String> list_funcinfo = new ArrayList<String>();
	
	/** �ļ���Ӧ������Ϣ*/
	private String fileinfo = "";
	
	/** �ļ������к����Ľṹ�͹���������*/
	private List<String> list_funcinfile = new ArrayList<String>();

	/** ��������ͼ�нڵ��ϵ����̣�*/
	private List<String> list_Relation_after = new ArrayList<String>();
	
	/** ��������ͼ�нڵ��ϵ��ǰ����*/
	private List<String> list_Relation_before = new ArrayList<String>();
	
	public static void main(String[] args) throws Exception{
		getFeatures get = new getFeatures();
		String filePath = "C:/Users/Miss_Lizi/Desktop/global.c";
		get.getAll(filePath);
//		for(int i = 0; i < get.list_func_struct.size(); i++){
//			System.out.println(get.list_func_struct.get(i));
//		}
	}
	
	/** �õ����е�������Ϣ*/
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
					//�Ѻ���Ĺ������������ӵ�����
					str1.append("," + 0);
				}
			}
			list_funcinfile.add(str1.toString());
			list_Relation_after.add(ff.getRelationAfter());
			list_Relation_before.add(ff.getRelationBefore());
		}
	
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
	
	
	public String getNumber(CVexNode c){
		String[] arr = c.getName().split("_");
		return arr[arr.length - 1];
	}
	
	
	/** ��������������*/
	public List<String> getStmsStatement(){
		return this.list_stms;
	}
	
	/** ���غ����ṹ������*/
	public List<String> getFuncStruct(){
		return this.list_func_struct;
	}
	
	/** ���غ�������������Ϣ*/
	public List<String> getFuncInfo(){
		return this.list_funcinfo;
	}
	
	/** �����ļ���Ϣ*/
	public String getFileInfo(){
		return this.fileinfo;
	}
}
