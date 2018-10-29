package softtest.SimDetection.c;
import java.util.*;

import softtest.CharacteristicExtract.c.*;
import softtest.SDataBase.c.*;

/**文件级别的相似性判定
 * by bubu
 * */

/**主要思路就是来判断两个图中是不是同构，或者是不是存在子图的关系
 * */

public class FileTest {
	/***用来设置每次从数据库中批量读取的数目****/
	public static int DBstep = 2000;
	
	/** 文件的路径*/
	public String filePath;
	/** 默认构造函数
	 * @throws Exception */
	public FileTest(String filePath) throws Exception{
		this.filePath = filePath;
		init();
	}
	public FileTest(LinkedList<String> list){
		if(!list.isEmpty()){
			this.filePath = list.get(0);
			fileGraph = new CallGraph(list.get(1),list.get(2),list.get(3),list.get(4));
		}
		
	}
	public void init() throws Exception{
		funcinfile featureHelper = new funcinfile();
		String []fileFeature = featureHelper.getAll(filePath).split("@");
		if(fileFeature.length == 5){
			fileGraph = new CallGraph(fileFeature[1],fileFeature[2],fileFeature[3],fileFeature[4]);
		}
	}
	public CallGraph fileGraph;
	
	public String dection(FileTest f){
		return this.fileGraph.dection(f.fileGraph);
	}
	
	public String getMatchNodes(){
		return this.fileGraph.matchNodesToString();
	}
	DataBaseAccess db = new DataBaseAccess().getInstance();
	public List<LinkedList<String>> dection() throws Exception{
		db.openDataBase();
		int num = db.readFileCount();
		List<LinkedList<String>> res = new LinkedList<>();
		if(this.fileGraph == null){
			System.out.println("  文件中节点数为0， 不予以分析。");
			return res;}
		for(int i = 0; i <= num; i = i + DBstep){
			List<LinkedList<String>> dbres = db.readFileRes(i, DBstep);
			for(LinkedList<String> list : dbres){
				System.out.println("      分析样本库中：" + list.get(0));
				if(list.size() != 5){
//					System.out.println("      " + list.get(0) + "特征值错误"); 
					continue;
					}
				if(Integer.valueOf(list.get(1)) == 0){
//					System.out.println("      " + list.get(0) + "节点个数为0"); 
					continue;}
				FileTest test = new FileTest(list);
				String detRes = this.dection(test);
				if(detRes != "false"){
					LinkedList<String> tmp = new LinkedList<String>();
					tmp.add(this.filePath);
					tmp.add(test.filePath);
					tmp.add(detRes);
					tmp.add(this.getMatchNodes());
					res.add(tmp);
				}
				//System.out.println(list);
			}
		}
		return res;
	}
	
	
		
//	/***以下测试专用**********/
//	public void testCGraph(){
//		FunNode f1 = new FunNode("1",0,0);
//		FunNode f2 = new FunNode("2",0,0);
//		FunNode f3 = new FunNode("3",0,0);
//		for(int i = 1; i <= 3; i++){
//			preNodes.put(Integer.toString(i), new HashSet<String>());
//			postNodes.put(Integer.toString(i), new HashSet<String>());
//		}
//		postNodes.get("1").add("2"); postNodes.get("1").add("3");
//		preNodes.get("2").add("1"); preNodes.get("3").add("1");
//		
//		setEntryNode();
//		setOutNode();
//		setCallSeq();
//		printSeq();
//	}
//	private void printSeq(){
//		for(LinkedList<String> list : callSeq){
//			for(String s : list){
//				System.out.print(s+ "->");
//			}
//			System.out.println();
//		}
//	}
//	/** 图中的调用序列集合*/
//	public LinkedList<LinkedList<String>> callSeq = new LinkedList<>();
//	/** 建立调用关系序列 */
//	private void setCallSeq() {
//		// 找到控制流图入口开始
//		for(String node : entryNode){
//			callSeq.addAll(dfs(node));
//		}
//	}
//	
//	/** 得到调用关系序列*/
//	public LinkedList<LinkedList<String>> getCallSeq(){
//		return this.callSeq;
//	}
//	/** 两个调用关系序列的匹配*/
//	public LinkedList<LinkedList<String>> matchSeq(LinkedList<LinkedList<String>> seq){
//		LinkedList<LinkedList<String>> res = new LinkedList<LinkedList<String>>();
//		for(LinkedList<String> list1 : this.callSeq){
//			int length = 0;
//			//int index = list1.size();
//			for(LinkedList<String> list2 : seq){
//				//ListIterator<String> ite1 = list1.listIterator<String>();
//				//while(ite1.hasPrevious())
//				
//			
//			}
//		}
//		return res;
//		
//	}
	public static void main(String[] args) throws Exception {
		
		String path1 = "D:/workspace/testcase/gsl-2.3/rng/random.c";
		String path2 = "D:/workspace/testcase/gsl-2.3/rng/mt.c";
		
		funcinfile featureHelper = new funcinfile();
		//System.out.println(featureHelper.getAll(path1));
		//System.out.println(featureHelper.getAll(path4));
		
//		Graph_Info g = new Graph_Info();
//		System.out.println(g.getCGraph(path4));
//		System.out.println(featureHelper.getAll(path3));
		FileTest f1 = new FileTest(path1);
		FileTest f2 = new FileTest(path2);
		f1.dection(f2);
		System.out.print(f1.getMatchNodes());
		
	}
}
