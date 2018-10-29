package softtest.SimDetection.c;

import java.util.*;

import softtest.CharacteristicExtract.c.ProjFeatures;
import softtest.SDataBase.c.DataBaseAccess;

public class Project {
	/***用来设置每次从数据库中批量读取的数目****/
	public static int DBstep = 10;
	public CallGraph proGraph;
	public String projectPath;
	public Project(){}
	public Project(String projectPath){
		this.projectPath = projectPath;
		ProjFeatures p = new ProjFeatures();
		/**proFeature中有文件加载顺序、节点信息、前驱、后继、孤立节点、工程路径等信息**/
		List<String> proFeature = p.getAll(projectPath);
		//得到文件的调用关系序列
		fileLoadSeq.addAll(Arrays.asList(proFeature.get(0).split("#")));
		proGraph = new CallGraph(0,proFeature.get(1),proFeature.get(2),proFeature.get(3),proFeature.get(4));
	}
	
	public Project(LinkedList<String> proDBInfor){
		//set工程路径
		this.projectPath = proDBInfor.get(0);
		//得到文件的调用关系序列
		fileLoadSeq.addAll(Arrays.asList(proDBInfor.get(1).split("#")));
		proGraph = new CallGraph(0,proDBInfor.get(2),proDBInfor.get(3),proDBInfor.get(4),proDBInfor.get(5));
	}
	LinkedList<String> fileLoadSeq = new LinkedList<String>();

	/**匹配调用序列
	 * @throws Exception **/
	public LinkedList<FileTest> filesInfor;
	public void setFilesInfor() throws Exception{
		ListIterator<String> ite = this.fileLoadSeq.listIterator();
		filesInfor = new LinkedList<FileTest>();
		while(ite.hasNext()){
			FileTest fileForTest = new FileTest(ite.next());
			if(fileForTest.fileGraph != null){
				filesInfor.add(fileForTest);
			}
		}
	}
	public boolean isMatchLoadSeq(LinkedList<String> callSeqDB) throws Exception{
		if(this.fileLoadSeq.size() == 0){
			return false;
		}
		db = new DataBaseAccess().getInstance();
		db.openDataBase();
		/**找到数据库中可以被分析的文件，过滤掉那些不可以被分析的文件，提升效率**/
		LinkedList<FileTest> fileInDBPro = new LinkedList<FileTest>();
		for(String s : callSeqDB){
//			System.out.println( s + " : "  + db.readFileRes(s));
			FileTest fileTmp = new FileTest(db.readFileRes(s));
			if(fileTmp.fileGraph != null){
				fileInDBPro.add(fileTmp);
			}
		}
		if(fileInDBPro.size() == 0){
			return false;
		}
		
		int pre = -1;
		boolean isSame = false;
		/**被测程序的调用序列   是不是数据库中程序调用序列的子集  */
		for(int i = 0; i < this.filesInfor.size(); i++){
			isSame = false;
			for(int j = pre + 1; j < fileInDBPro.size(); j++){
				FileTest f1 = this.filesInfor.get(i);
				FileTest f2 = fileInDBPro.get(j);
				String res  = f1.dection(f2);
				System.out.println(f1.filePath + " 和 " + f2.filePath + " 的结果为： " + res);
				f1.fileGraph.printNodeInfor();
				f2.fileGraph.printNodeInfor();
				if(!res.equals("false")){
					isSame = true;
					pre = j;
					break;
				}
			}
			if(isSame == false){
				break;
			}
		}
		if(isSame == true){return true;}
		/**反过来     数据库中程序的调用序列   是不是 被测程序调用序列的子集  */
		pre = -1;
		for(int i = 0; i < fileInDBPro.size(); i++){
			isSame = false;
			for(int j = pre + 1; j < this.filesInfor.size(); j++){
				FileTest f1 = fileInDBPro.get(i);
				FileTest f2 = this.filesInfor.get(j);
				String res  = f1.dection(f2);
				System.out.println(f1.filePath + " 和 " + f2.filePath + " 的结果为： " + res);
				if(!f1.dection(f2).equals("false")){
					isSame = true;
					pre = j;
					break;
				}
			}
			if(isSame == false){
				return false;
			}
		}
		return isSame;
	}
	
	public String isMatchCallGraph(Project dbPro) throws Exception{
		if(this.proGraph == null || dbPro.proGraph == null){ return "false";}
		return this.proGraph.dection(dbPro.proGraph);
	}
	
	public DataBaseAccess db;
	
	public String dection(Project dbPro) throws Exception{
		setFilesInfor();
		if(isMatchLoadSeq(dbPro.fileLoadSeq)){
			return "matchLoadSeq";
		}
		String graphRes = isMatchCallGraph(dbPro);
		if(!graphRes.equals("false")){
			return graphRes + "matchCallGraph";
		}
		return "false";
	}
	public List<LinkedList<String>> dection() throws Exception{
		db = new DataBaseAccess().getInstance();
		db.openDataBase();
		int max = db.maxProID();
		List<LinkedList<String>> res = new LinkedList<LinkedList<String>>();
		for(int i = 123; i <= max; i = i + DBstep){
			List<LinkedList<String>>  dbRes = db.readProRes(i, DBstep);
			/**dbRes 有 程序路径，调用顺序，节点信息，前驱，后继，孤立节点信息*/
			for(LinkedList<String> proDBInfor : dbRes){
				/** start 比较加载顺序**/
				System.out.println("*****************比较" + proDBInfor.get(0));
				//处理数据库中为空的信息，即加载顺序为空，并且图也为空
				if(proDBInfor.get(1).equals("") && proDBInfor.get(2).equals("")){
					continue;
				}
				Project dbPro = new Project(proDBInfor);
				String decRes = dection(dbPro);
				if(!decRes.equals("false")){
					LinkedList<String> listAdd = new LinkedList<String>();
					listAdd.add(projectPath);
					listAdd.add(proDBInfor.get(0));
					listAdd.add(decRes);
					res.add(listAdd);
				}
			}
		}
		return res;
	}
	
	public boolean isMatchLoadSeqTest(LinkedList<FileTest> callSeqDB) throws Exception{
		int preTest = -1;
		int preDB = -1;
		for(int i = preTest + 1; i < this.filesInfor.size(); i++){
			boolean find = false;
			for(int j = preDB + 1; j < callSeqDB.size(); j++){
				preDB++;
				if(!filesInfor.get(i).dection(callSeqDB.get(j)).equals("false")){
					preDB = j;
					find = true;
					break;
				}
			}
			if(find == false){
				break;
			}
		}
		return false;
	}
	public static void main(String[] args) throws Exception {
		String path1 = "D:/workspace/testcase/uucp-1.07";
		//String path2 = "D:/workspace/testcase/gsl-2.3/monte";
//		ProjFeatures p = new ProjFeatures();
//		List<String> proFeature = p.getAll(path1);
//		ProjFeatures p2 = new ProjFeatures();
//		List<String> proFeature2 = p2.getAll(path2);
//		for(int i = 0; i < 6; i++){
//			System.out.println(proFeature.get(i));
//			System.out.println(proFeature2.get(i));
//			System.out.println();
//		}
		
		Project pro = new Project(path1);
		//Project pro2= new Project(path2);
//		pro.setFilesInfor();pro2.setFilesInfor();
//		System.out.println(pro.fileLoadSeq);
//		System.out.println(pro2.fileLoadSeq);
		pro.dection();
	}

}
