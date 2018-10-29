package softtest.SimDetection.c;

import java.io.File;
import java.util.*;

import javax.xml.crypto.Data;

import softtest.CharacteristicExtract.c.StatementFeature;
import softtest.CharacteristicExtract.c.funcinfile;
import softtest.CharacteristicExtract.c.getFeatures;
import softtest.SDataBase.c.*;
import softtest.callgraph.c.CVexNode;
import softtest.interpro.c.InterContext;

public class SimDet {
	/**结果数据库默认存储路径***/
	public static String resDBPath = "res";
	/**测试工程名字*/
	public static String projectName;
	/**测试工程路径*/
	public static String projectPath;
	/**收集所有的文件路径*/
	private static List<String> filePathList;
	public SimDet(String projectPath){
		filePathList = new ArrayList<String>();
		setProjectPath(projectPath);
	}
	public void setProjectPath(String projectPath){
		this.projectPath = projectPath;
		File file = new File(projectPath);
		projectName = file.getName(); 
		resDBPath = resDBPath + "-" + projectName;
	}
	public void setResDBPath(String path){
		resDBPath = path + this.projectName;
	}
	public DataBaseAccess mydb = new DataBaseAccess().getInstance();
	/**语句块级别的相似性判定，不考虑嵌套关系*/
	public void blockDetBefore() throws Exception{
		resdb.clearBlock();
		for(String testFilePath : filePathList){
			//System.out.println("/***"+ testFilePath + "  start******/");
			//得到文件内的所有的语句块的特征
			StatementFeature sf = new StatementFeature();
			List<String> blockFeatures = sf.getFeatures(testFilePath);
			for(String blockFeature : blockFeatures){
				//System.out.println(blockFeature);
				Block b = new Block(blockFeature);
				List<LinkedList<String>> block_res = b.dection();
				if(block_res == null || block_res.size() == 0){
					//System.out.println("没有找到相似的语句块");
				}else{
					//System.out.println("结果数据库生成中");
					List<String> infor = new LinkedList<String>();
					infor.add(b.filePath);
					infor.add(Integer.toString(b.beginLine));
					infor.add(Integer.toString(b.endLine));
					for(List<String> list : block_res){
						List<String> insert = new LinkedList<String>();
						insert.addAll(infor); insert.addAll(list);
						resdb.insertBlock(insert);
					}
				}
			}
		}
	}

	/**语句块级别的相似判定,考虑嵌套关系**/
	public void blockDet() throws Exception{
		resdb.clearBlock();
		for(String testFilePath : filePathList){
			//System.out.println("/***"+ testFilePath + "  start******/");
			int []pre = {Integer.MAX_VALUE,-1};
			//得到文件内的所有的语句块的特征
			StatementFeature sf = new StatementFeature();
			List<String> blockFeatures = sf.getFeatures(testFilePath);
			for(String blockFeature : blockFeatures){
				//System.out.println(blockFeature);
				Block b = new Block(blockFeature);
				//判断是不是存在嵌套关系
				//System.out.println(pre[0] + "," + pre[1]);
				if(b.beginLine < pre[0] || b.endLine > pre[1]){
					List<LinkedList<String>> block_res = b.dection();
					if(block_res == null || block_res.size() == 0){
						//System.out.println("没有找到相似的语句块");
					}else{
						pre[0] = b.beginLine; pre[1] = b.endLine;
						//System.out.println("结果数据库生成中");
						List<String> infor = new LinkedList<String>();
						infor.add(b.filePath);
						infor.add(Integer.toString(b.beginLine));
						infor.add(Integer.toString(b.endLine));
						for(List<String> list : block_res){
							List<String> insert = new LinkedList<String>();
							insert.addAll(infor); insert.addAll(list);
							resdb.insertBlock(insert);
						}
					}
				}
			}
		}
	}
	
	/**函数级别的相似判定**/
	public void functionDet() throws Exception{
		resdb.clearFunction();
		for(String testFilePath : filePathList){
			getFeatures get = new getFeatures();
			get.getAll(testFilePath);
			List<String> funFeatures = get.getFuncStruct();
			for(String feature : funFeatures){
				feature += "#0";
				System.out.println(feature);
				Function f = new Function(feature);
				List<List<String>> fun_res = f.dection();
				if(fun_res == null || fun_res.size() == 0){
					System.out.println("没有找到相似的函数");
				}else{
					System.out.println("结果数据库生成中");
					List<String> infor = new LinkedList<String>();
					infor.add(f.filePath);
					infor.add(f.funcName);
					for(List list : fun_res){
						List<String> insert = new LinkedList<String>();
						insert.addAll(infor); insert.addAll(list);
						resdb.insetrFunction(insert);
					}
				}
			}
		}
	}
	
	/**文件级别的相似判定**/
	public void  fileDet() throws Exception{
		resdb.clearFile();
		for(String testFilePath : filePathList){
			//System.out.println("/****正在分析文件 : " + testFilePath + "*****/");
			FileTest f = new FileTest(testFilePath);
			List<LinkedList<String>> file_res = f.dection();
			if(file_res == null || file_res.size() == 0){
				//System.out.println(testFilePath + "没有找到相似的文件");
			}else{
				System.out.println("结果数据库生成中");
				for(List<String> list : file_res){
					resdb.insertFile(list);
				}
			}
		}
	}
	
	public void  projectDet() throws Exception{
		resdb.clearProject();
		Project p = new Project(projectPath);
		List<LinkedList<String>> project_res = p.dection();
		if(project_res == null || project_res.size() == 0){
			System.out.println("没有找到相似的程序");
		}else{
			System.out.println("结果数据库生成中");
			for(List<String> list : project_res){
				resdb.insertProject(list);
			}
		}	
	}
	public static SimResultDB resdb;
	public static DataBaseAccess testdb;
	/**在进行测试前，需要完成三项工作
	 * 1.建立结果数据库 
	 * 2.打开样本库数据库
	 * 3.路径收集*/
	public void beforDet(){
		testdb = DataBaseAccess.getInstance();
		testdb.openDataBase();
		File srcFile = new File(projectPath);
		if (!srcFile.exists()) {
			throw new RuntimeException("Error: File " + srcFile.getName() + " does not exist.");
		}
		collect(srcFile);
	}
	
	/**收集所有的.c 文件*/
	private void collect(File srcFile) {
		if (srcFile.isFile() && srcFile.getName().matches(InterContext.SRCFILE_POSTFIX)) {
			filePathList.add(srcFile.getPath());
		} else if (srcFile.isDirectory()) {
			File[] fs = srcFile.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collect(fs[i]);
			}
		}
	}
	
	public static void mains(String[] args) throws Exception {
		String filePath = "C:/Users/Miss_Lizi/Desktop/uucp-1.07";
		//String filePath = args[0];
		SimDet simDet = new SimDet(filePath);
		if(args.length == 3){
			simDet.setResDBPath(args[2]);
		}
//		String test = args[1];//测试类型
		String test = "Project";
		
		
		simDet.beforDet();
		resdb = SimResultDB.getInstance(resDBPath);
		resdb.openSimResultDB();
		switch(test){
			case "Block":
				simDet.blockDet();
				break;
			case "Function":
				simDet.functionDet();
				break;
			case "File":
				simDet.fileDet();
				break;
			case "Project":
				simDet.projectDet();
				break;
			default :
				System.out.println("输入错误");
		}
		testdb.closeDataBase();
		resdb.closeSimResultDB();
		
	}
	public static void main(String[] args) throws Exception {
//		String filePaths = "D:/workspace/testcase/antiword-0.37;"
//				+ "D:/workspace/testcase/barcode-0.98;"
//				+ "D:/workspace/testcase/gsl-2.3;"
//				+ "D:/workspace/testcase/spell-1.0;"
//				+ "D:/workspace/testcase/uucp-1.07";
		String filePaths = "C:/Users/Miss_Lizi/Desktop/uucp-1.07";
		String []testFile = filePaths.split(";");
		for(String filePath : testFile){
			SimDet simDet = new SimDet(filePath);
//			SimDet simDet = new SimDet(args[0]);
			if(args.length == 3){
				simDet.setResDBPath(args[2]);
			}
//			String test = args[1];//测试类型
			
			String test = "Block";
//			String test = "Function";
//			String test = "File";
//			String test = "Project";
			
			
			simDet.beforDet();
			resdb = SimResultDB.getInstance(resDBPath);
			resdb.openSimResultDB();
			//long start = System.currentTimeMillis();
			switch(test){
				case "Block":
					//simDet.blockDet();
					simDet.blockDetBefore();
					break;
				case "Function":
					simDet.functionDet();
					break;
				case "File":
					simDet.fileDet();
					break;
				case "Project":
					simDet.projectDet();
					break;
				default :
					System.out.println("输入错误");
			}
//			long end = System.currentTimeMillis();
//			System.out.println(filePath + "使用的时间为："  + (end - start)  + "ms");
			testdb.closeDataBase();
			resdb.closeSimResultDB();
			System.out.println("测试结束");
		}
		
	}
	
}
