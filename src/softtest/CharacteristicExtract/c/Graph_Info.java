package softtest.CharacteristicExtract.c;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import softtest.SDataBase.c.DataBaseAccess;
import softtest.SDataBase.c.FileInformatica;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.TokenMgrError;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.callgraph.c.DumpCGraphVisitor;
import softtest.cfg.c.DumpGraphVisitor;
import softtest.cfg.c.Graph;
import softtest.database.c.DBAccess;
import softtest.interpro.c.InterContext;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;

/** 
 * @author 
 * Miss_lizi
 * 
 */
public class Graph_Info {
	private  List<String> filePathList;
	
	/** 返回给定路径的文件的特征信息*/
	public  String  getFunction(String filePath) throws Exception{
		String file_metric = new String();
		
		Metric m = new Metric();	
		file_metric = m.getMetric(filePath);
		//得到改文件内函数调用关系图的特征信息
//		System.out.println("文件特征信息为：");
//		System.out.println(file_metric);
		return file_metric;
	}
	
	/** 返回给定路径的文件中所有函数的特征信息*/
	public   List<String> getAllFuncInfo(String filePath) throws Exception{
		List<CVexNode> graph_list = new ArrayList<CVexNode>();
		List<String> func_metric = new ArrayList<String>();
		graph_list = getCVexNode(filePath);
	
		if(graph_list.size() == 0){
			return null;
		}else{
			Metric m = new Metric();
			//System.out.println(graph_list);
			func_metric = m.getFuncMetric(graph_list,filePath);	
		}
		
		
		System.out.println("文件内各个函数的特征信息为：");
		for(String s : func_metric){
			System.out.println(s);
		}
		return func_metric;
	}
	
public static  void main(String[] args) throws Exception{
		
	
	   StatementFeature sf = new StatementFeature();
	   funcinfile ff = new funcinfile();
	   ProjFeatures pf = new ProjFeatures();
	   String filePath = "E:/lib/Workspace/testcase/allegro5-master";
	   
	   String filePath_danger = "..\\testcase_danger";
       String filePath1 = "C:/Users/Miss_Lizi/workspace/testcase/Collections-C-master";
       
       
//       try{
    	  // System.out.println(ff.getAll(filePath1));
//		}catch (TokenMgrError e){
//			e.printStackTrace();
//			System.out.println("抓到我了");
//		}
      
	   
	   //String analysisDir1="C:/Users/Miss_Lizi/Desktop/file_order";
	   //String aa = "C:/Users/Miss_Lizi/Desktop/antiword-0.37";
	   
	   
		File srcFile = new File(filePath);
		File srcFile_danger = new File(filePath_danger);
		List<String> file = new ArrayList<String>();
		List<String> file_danger = new ArrayList<String>();
		file = collect(srcFile,file);
		file_danger = collect(srcFile_danger,file_danger);
		
		List<File> folder = new ArrayList<File>();
		folder = collect_folder(srcFile,folder);
	
		CreateTable dbtest = new CreateTable();
		dbtest.openDataBase();
		
		
		
		//dbtest.insert_Stmstable(sf.getFeatures(filePath1), false);
		//dbtest.insert_Stmstable(sf.getFeatures(filePath1), false);
//		dbtest.insert_Functable(ff.getFuncFeatures(filePath1), false);
//		dbtest.insert_Filetable(ff.getAll(filePath1));
		//dbtest.insert_Functable(ff.getFuncFeatures(filePath1),false);
		
//		/**检验一下有没有错*/
//		for(String file_c : file){
//			System.out.println(file_c);
//			try{
//				sf.getFeatures(file_c);
//				ff.getFuncFeatures(file_c);
//		    	ff.getAll(file_c);
//			}catch (Exception e){
//				e.printStackTrace();
//			}catch (TokenMgrError e){
//				e.printStackTrace();
//				//System.out.println("抓到我了");
//			}
//		}
//		
//		for(File str : folder){
//			try{
//				System.out.println(str+ "***********");
//				pf.getAll(str.toString());
//			}catch (Exception e){
//				e.printStackTrace();
//			}catch (TokenMgrError e){
//				e.printStackTrace();
//			}
//			
//		}
		
		for(String file_c : file){
			try{
				System.out.println(file_c);
				List<String> list1 = new ArrayList<String>();
				list1 = sf.getFeatures(file_c);
				dbtest.insert_Stmstable(list1, false);
				List<String> list = new ArrayList<String>();
				list = ff.getFuncFeatures(file_c);
				dbtest.insert_Functable(list, false);
				dbtest.insert_Functable_10000(list, false);
				String str = ff.getAll(file_c);
				dbtest.insertFiletable(str);
				list = null;
				list1 = null;
				str = null;
			}catch (Exception e){
				e.printStackTrace();
			}catch (TokenMgrError e){
				e.printStackTrace();
				//System.out.println("抓到我了");
			}catch(OutOfMemoryError e){
				e.printStackTrace();
			}
			
		}
		
		
		
		
//		//扫描有漏洞的源码的语句块
//		for(String file_c : file_danger){
//			dbtest.insert_Stmstable(sf.getFeatures(file_c), true);
//			dbtest.insert_Functable(ff.getFuncFeatures(file_c), true);
//			dbtest.insertFiletable(ff.getAll(file_c));
//		}

		
		/**project  结果插入数据库*/	
		for(File str : folder){
			try{
				System.out.println(str + "****************");
				//pf.getAll(str.toString());\
				List<String> list = new ArrayList<String>();
				list = pf.getAll(str.toString());
				dbtest.insertProjtable(list);
				list = null;
				//dbtest.insertProjtablenull(pf.getAll(str.toString()));
			}catch (Exception e){
				e.printStackTrace();
			}catch (TokenMgrError e){
				e.printStackTrace();
				//System.out.println("抓到我了");
			}catch(OutOfMemoryError e){
				e.printStackTrace();
			}
			
		}
		
		sf = null;
		ff = null;
		pf = null;
		//dbAccess.insertProjFeatures(pf.getAll(aa));
		dbtest.closeDataBase();
	}
	


	
	/**返回文件内所有函数的节点列表*/
	public   List<CVexNode> getCVexNode(String filePath) throws Exception{
		//System.out.println("生成抽象语法树...");
		CParser parser = CParser.getParser(new CCharStream(new FileInputStream(filePath)));
		ASTTranslationUnit root=parser.TranslationUnit();
		//产生符号表
		//System.out.println("生成符号表...");
		ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
		root.jjtAccept(sc, null);
		OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
		root.jjtAccept(o, null);
		
		CGraph g = new CGraph();
		((AbstractScope)root.getScope()).resolveCallRelation(g);
		
		List<CVexNode> list=g.getTopologicalOrderList();
		Collections.reverse(list);
		return list;
	}
	
	/**返回函数间调用关系图*/
	public  CGraph getCGraph(String filePath) throws Exception{
		//System.out.println("生成抽象语法树...");
		CParser parser = CParser.getParser(new CCharStream(new FileInputStream(filePath)));
		ASTTranslationUnit root=parser.TranslationUnit();
		
		//产生符号表
		//System.out.println("生成符号表...");
		ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
		root.jjtAccept(sc, null);
		OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
		root.jjtAccept(o, null);
		
//		CGraph g = new CGraph(filePath);
		CGraph g = new CGraph();               //add by lsc 2018/10/24
		((AbstractScope)root.getScope()).resolveCallRelation(g);
		
		List<CVexNode> list=g.getTopologicalOrderList();
		Collections.reverse(list);
		/*System.out.println("调用关系拓扑逆序：");
		for(CVexNode n:list){
			System.out.print(n.getName()+"  ");
		}
		System.out.println();*/
		
		
		String name ="wangy/wangy";
		g.accept(new DumpCGraphVisitor(), name + ".dot");
		//System.out.println("调用图图输出到了文件" + name + ".dot");
		try {
			java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + name + ".jpg " + name + ".dot").waitFor();
		} catch (IOException e1) {
			System.out.println(e1);
		} catch (InterruptedException e2) {
			System.out.println(e2);
		}
		//System.out.println("调用图打印到了文件" + name + ".jpg");
		
		//System.out.println();
		return g;
	}

	/**得到函数的控制流图信息*/
	public  String getFuncInfo(Graph graph, ASTFunctionDefinition node, String filePath){
		List<Long> list = new ArrayList<Long>();
		String name = filePath + "#" + node.getImage();
		Long vex_num = (long)graph.getVexNum();
		Long edge_num = graph.getedgecount();
		//System.out.println("函数的控制流图的节点数为：" + vex_num);
		//System.out.println("函数的控制流图的边数为：" + edge_num);
		list.add(vex_num);
		list.add(edge_num);
		
		return toString(list,name);
	}
	
	/**得到文件中函数的调用关系图信息*/
	public  String getFileInfo(CGraph cg){
		if(cg == null){
			return null;
		}
		List<Long> list = new ArrayList<Long>();
		String name = cg.getname();
		Long vex_num = cg.getnodecount();
		Long edge_num = cg.getedgecount();
		//System.out.println("函数的控制流图的节点数为：" + vex_num);
		//System.out.println("函数的控制流图的边数为：" + edge_num);
		long gMcCabe = edge_num - vex_num + 2;
		list.add(vex_num);
		list.add(edge_num);
		list.add(gMcCabe);
		
		return toString(list,name);
	}
	
	/**转换为字符串的形式*/
	public  String toString( List<Long> list, String name){
		String res = name;
		for(int i = 0 ; i < list.size(); i++){
			res += "#" + list.get(i);
		}
		return res;
	}

	/** 收集当前测试工程的所有源文件（.C源文件），并将搜索到的.C文件加入到待分析列表中*/
	public  static  List<String> collect(File srcFile,List<String> res) {
		//List<String> res = new ArrayList<String>();
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
	
	/** 收集当前测试工程的所有文件夹*/
	public static List<File> collect_folder(File srcFile, List<File> res){
		if(srcFile.isDirectory()){
			res.add(srcFile);
		}
		File[] fileList = srcFile.listFiles();
		//List<File> wjjList = new ArrayList<File>();//新建一个文件夹集合
		for (int i = 0; i < fileList.length; i++) {
		   if (fileList[i].isDirectory()) {//判断是否为文件夹
		        //res.add(fileList[i]);
		        collect_folder(fileList[i],  res);
		   }
		}
		return res;
	}
	
}
