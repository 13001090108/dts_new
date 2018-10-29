package softtest.CharacteristicExtract.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.text.html.HTMLDocument.Iterator;

import softtest.DefUseAnalysis.c.DUAnalysisVisitor;
import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTCompoundStatement;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTFunctionDeclaration;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTJumpStatement;
import softtest.ast.c.ASTParameterDeclaration;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CEdge;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.GraphVisitor;
import softtest.cfg.c.VexNode;
import softtest.depchain.c.DepChain;
import softtest.depchain.c.DepChainUtil;
import softtest.depchain.c.ExternalInputSource;
import softtest.depchain.c.ExternalInputSourceVisitor;
import softtest.depchain.c.NodeLineMapVisitor;
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.interpro.c.Method;
import softtest.interpro.c.MethodNode;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;
import softtest.rules.c.StateMachineUtils;
import softtest.scvp.c.Position;
import softtest.scvp.c.SCVP;
import softtest.scvp.c.SCVPString;
import softtest.scvp.c.SCVPVisitor;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class Func_Features {
	 
	private  List<AnalysisElement> elements= new ArrayList<AnalysisElement>();;
	//private String analysisDir="";
	private List<String> files=new ArrayList<String>();
	private InterCallGraph interCGraph =InterCallGraph.getInstance();
	private String[] args;
	private Pretreatment pre=new Pretreatment();
	
	public Func_Features()
	{
//		this.analysisDir=args[0];
//		this.setArgs(args);
		
	}
	private HashMap<String, ASTTranslationUnit> astmap = new HashMap<String, ASTTranslationUnit>();
	private HashMap<String, Graph> cfgmap = new HashMap<String, Graph>();
	private HashMap<Graph, String> cfgmap2 = new HashMap<Graph, String>();
	private HashMap<String, CGraph> cgMap = new HashMap<String, CGraph>();
	
	
	

	//收集测试路径下的所有.C源文件
	private void collect(File srcFileDir) {
		if (srcFileDir.isFile() && srcFileDir.getName().matches(InterContext.SRCFILE_POSTFIX)) {
			files.add(srcFileDir.getPath());
		}else if (srcFileDir.isDirectory()) {
			File[] fs = srcFileDir.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collect(fs[i]);
			}
		}
	}

	//进行预编译的初始化
	private void init(String analysisDir){
		//pre = new Pretreatment();
		pre.setPlatform(PlatformType.GCC);
		File srcFileDir=new File(analysisDir);
		collect(srcFileDir);
	}
	
	/**对所有.C源文件依次进行处理：预编译、分析、生成全局函数调用关系图*/
	private void process(){
		//第一步：对所有.C源文件进行预编译
		PreAnalysis();
		
		//第二步：生成全局函数调用关系图
		List<AnalysisElement> orders = interCGraph.getAnalysisTopoOrder();
		if (orders.size() != elements.size()) {
			for (AnalysisElement element : elements) {
				boolean exist = false;
				for (AnalysisElement order : orders) {
					if (order == element) {
						exist = true;
					}
				}
				if (!exist) {
					orders.add(element);
				}
			}
		}
	}
	
	private void PreAnalysis(){
		getReservedWords();
		for(String srcFile:files){
			AnalysisElement element=new AnalysisElement(srcFile);
			elements.add(element);
			//预编译之后的.I源文件
			String afterPreprocessFile=null;
			List<String> include = new ArrayList<String>();
			include.add("C:/Program Files (x86)/DTS/DTS/DTSGCC/include");
			List<String> macro = new ArrayList<String>();
			afterPreprocessFile=pre.pretreat(srcFile, include, macro);
			
			try {
				
				//产生抽象语法树
				System.out.println("生成抽象语法树...");
				System.out.println(afterPreprocessFile);
				CParser parser=CParser.getParser(new CCharStream(new FileInputStream(afterPreprocessFile)));
				ASTTranslationUnit root=parser.TranslationUnit();
				astmap.put(srcFile, root);//把语法树扔内存里，通过文件名检索
				
				//产生符号表
				System.out.println("生成符号表...");
				ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
				root.jjtAccept(sc, null);
				OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
				root.jjtAccept(o, null);
				
				//生成全局函数调用关系
				System.out.println("生成全局函数调用关系...");
				root.jjtAccept(new InterMethodVisitor(), element);
				
				//文件内函数调用关系图
				System.out.println("生成文件内函数调用关系...");
				CGraph g = new CGraph();
				((AbstractScope)(root.getScope())).resolveCallRelation(g);
				List<CVexNode> list = g.getTopologicalOrderList(element);
				Collections.reverse(list);
				cgMap.put(srcFile, g);
				
				//生成控制流图
				System.out.println("生成控制流图...");
				ControlFlowVisitor cfv = new ControlFlowVisitor(element.getFileName());
				ControlFlowData flow = new ControlFlowData();
				for (CVexNode cvnode : list) {
					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
					if (node instanceof ASTFunctionDefinition) {
						cfv.visit((ASTFunctionDefinition)node, flow);
						cfgmap.put(node.getImage(), ((ASTFunctionDefinition)node).getGraph());
						cfgmap2.put(((ASTFunctionDefinition)node).getGraph(), node.getImage());
					} 
				}
				
				//生成定义使用链
				System.out.println("生成定义使用链...");
				root.jjtAccept(new DUAnalysisVisitor(), null);
				
				//清空存放函数内所有声明的变量
				global_map.clear();
				entrance_map.clear();
				
				//计算SCVP
				System.out.println("计算scvp...");
				//root.jjtAccept(new SCVPVisitor(), null);
				for (CVexNode cvnode : list) {
					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
					if (node instanceof ASTFunctionDefinition) {
						//System.out.println(cvnode.toString());
						node.jjtAccept(new SCVPVisitor(), null);
						getEntranceVars(cvnode);
						getGlobalVarsList(cvnode);
						
					} 
				}
				System.out.println("OK.");
				
				
				
		

				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public HashSet<SCVPString> getReturnVars(String funcname){
		System.out.println(funcname);
		HashSet<SCVPString> res = new HashSet<SCVPString>();
		List<SCVPString> res1 = new ArrayList<SCVPString>();
		//InterCallGraph iv = new InterCallGraph();
		//InterCallGraph callGraph = iv.getInstance_not();
		InterCallGraph callGraph = InterCallGraph.getInstance();
		for(MethodNode mn : callGraph.getINTERMETHOD()) {   //getINTERMETHOD()是得到当前分析过程中全局的函数节点信息
			Method m = mn.getMethod();
			if(m.getName().equals(funcname)){
				m.getReturnList();
				for(SCVPString str : m.getReturnList()){
					str.toString();
					if(!res1.contains(str)){
						res1.add(str);
					}
					res.add(str);
				}
			}
			
		}
//		for(SCVPString str : res1){
//			System.out.println(str);
//		}
		return res;
	}
	
	private NameOccurrence locate(String fileName, String funcName, String symbolName, int line) {
		Graph cfg = cfgmap.get(funcName);
		String occStr = symbolName+":"+line;
		if (cfg == null) return null;		
		NameOccurrence res = cfg.getOcctable().get(occStr);		
		return res;
	}
	
	/**存储每个变量的最初来源形式以及是否存在外部输入的情况*/
	private HashMap<String,String> map_eachvar_source = new HashMap<String, String>();
	
	
	public void getEachVarSource(String funcname){
		for(String var : vars){
//			SCVP scvp = null;
			int min = Integer.MAX_VALUE;
			StringBuffer sb = new StringBuffer();
			sb.append("变量" + var + "的来源为#");
			boolean flag = false;
			for(String str : map_scvp.keySet()){
				if(str.split(":")[0].equals(var)){
					if(map_scvp.get(str).getStructure().contains("Function") ){
						String fun = map_scvp.get(str).getStructure().split(":")[map_scvp.get(str).getStructure().split(":").length-1];
						if(list_outsource.contains(fun)){
							flag = true;
						}
					}
					int line = Integer.valueOf(str.split(":")[1]);
					if(line < min){
						min = line;
					//	scvp = map_scvp.get(str);
					}
				}
			}
			if(global_map.get(funcname).contains(var.split(":")[0])){ //该变量为局部自定义的
				sb.append("局部定义形式");
			}else if(entrance_map.get(funcname).contains(var.split(":")[0])){
				//if(entrance_map.get(funcname).contains(var.split(":")[0])){
					sb.append("参数传入形式");
				//}
			}else{   //该变量为全局变量
				sb.append("全局变量形式");
			}
			if(flag){
				sb.append(",并且存在外部输入情况");
			}
			map_eachvar_source.put(var, sb.toString());
		}
		
//		for(String str : map_eachvar_source.keySet()){
//			System.out.println(map_eachvar_source.get(str));
//		}
	}
	
	
	
	/**存储每个变量有关联的其他变量*/
	private HashMap<String, List<String>> related = new HashMap<String, List<String>>();
	private HashMap<String, HashSet<NameOccurrence>> related_occ = new HashMap<String, HashSet<NameOccurrence>>();
	private HashSet<String> vars = new HashSet<String>();
	private HashMap<String, String> result = new HashMap<String, String>();
	
	public void getVars(){
		for(String str : map_scvp.keySet()){
			String var = str.split(":")[0];
			if(!map_scvp.get(str).getOccs().isEmpty()){
				HashSet<NameOccurrence> occs = map_scvp.get(str).getOccs();
				for(NameOccurrence occ : occs){
					String var1 = occ.toString().split(":")[0];
					vars.add(var1);
				}
			}
			vars.add(var);
		}
	}
	
	/**获得每个变量与其相关的变量*/
	public void getEachVarRelated(){
		for(String str : map_scvp.keySet()){
			String var = str.split(":")[0];
			if(!related.containsKey(var)){
				related.put(var, new ArrayList<String>());
				related_occ.put(var, new HashSet<NameOccurrence>());
			}
			if(!map_scvp.get(str).getOccs().isEmpty()){
				HashSet<NameOccurrence> occs = map_scvp.get(str).getOccs();
				related_occ.get(var).addAll(occs);
				for(NameOccurrence occ : occs){
					String var1 = occ.toString().split(":")[0];
					related.get(var).add(map_eachvar_source.get(var1));
				}
				//related.get(var).addAll(occs);
			}
		}
		for(String str : related.keySet()){
			StringBuffer sb = new StringBuffer();
			List<String> value = related.get(str);
			//sb.append("return中第"+ count++ + "变量的来源为：" + map_eachvar_source.get(str).split("#")[1]);
			sb.append("[详细分析]return中变量来源为：" + map_eachvar_source.get(str).split("#")[1]);
			//System.out.println("return中"+ map_eachvar_source.get(str));
			if(!value.isEmpty()){
				sb.append("。其相关的变量来源分别为：");
				//System.out.println("和其相关的变量来源分别为：");
				for(String occ : value){
					sb.append(occ.split("#")[1] + ",");
					//System.out.println(occ);
				}
			}
			result.put(str, sb.toString());
		}
	}
	
	
	
	private List<String> list_outsource = new ArrayList<String>();
	public boolean isFormOutside(String var){
		for(String str : map_scvp.keySet()){
			if(str.split(":")[0].equals(var)){
				if(map_scvp.get(str).getStructure().contains("Function") ){
					String fun = map_scvp.get(str).getStructure().split(":")[map_scvp.get(str).getStructure().split(":").length-1];
					if(list_outsource.contains(fun)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	
	/** 得到对应变量在结果中的高行数*/
	public HashSet<NameOccurrence> getHigest(String var){
		HashSet<NameOccurrence> list = new HashSet<NameOccurrence>();
		int max = 0;
		SCVP res = null;
		for(String str : map_scvp.keySet()){
			int line = Integer.valueOf(str.split(":")[1]);
			if(max < line){
				max = line;
				res = map_scvp.get(str);
			}
		}
		list.addAll(res.getOccs());
//		for(NameOccurrence occ : list){
//			System.out.println(occ.toString());
//		}
		return list;
	}
	
	public int getALLFuncFeatures(String filePath, String name) throws Exception {
		int res = 0;
		init(filePath);
		Graph_Info h = new Graph_Info();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
	
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition ){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				String funcname = function.getImage();
				if(funcname.equals(name)){
					res = getFunctionFeatures(filePath,funcname);	
				}
			}
		}
		return res;
	}
	
	public void getALLFuncFeatures(String filePath) throws Exception {
		init(filePath);
//		init();
		Graph_Info h = new Graph_Info();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
	
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				String funcname = function.getImage();
				getFunctionFeatures(filePath,funcname);			
			}
			
		}
	}

	//HashSet<SCVPString> returnscvps = new HashSet<SCVPString>();
	public int getFunctionFeatures(String filepath, String funcname) throws Exception{
		StringBuffer ress = new StringBuffer();
		StatementFeature s = new StatementFeature();
		
		ress.append(filepath + "#" + funcname + "#");
		//String varname = args[3];
		//int line = Integer.valueOf(args[4]);
		
		process();
		//得到函数内所有return语句中的变量的scvp,有可能对应多条return语句
		//returnscvps.clear();
		HashSet<SCVPString> returnscvps = getReturnVars(funcname);
		//List<SCVPString> returnscvps = getReturnVars(funcname); 
		//System.out.println(returnscvps.size());
		
		if(returnscvps.isEmpty()){
			ress.append("There is no return statements");
		}
		
		for(SCVPString returnscvp : returnscvps){
			String fn = returnscvp.getPosition().getFunctionName();
			int line = returnscvp.getPosition().getBeginLine();
			//s.getReturnFeature(filepath,fn,line);
			String return_feature = s.getReturnFeature(filepath,fn,line);
			//System.out.println(s.getReturnFeature(filepath,fn,line));
			List<String> vars_return = returnscvp.getOccs(); //得到一条return的相关变量
			List<String> res = new ArrayList<String>();
			for(String str : vars_return){
				map_scvp.clear();
				related.clear();
				map_eachvar_source.clear();
				vars.clear();
				related_occ.clear();
				result.clear();
				NameOccurrence occ = locate(filepath, funcname, str.split(":")[0], Integer.valueOf(str.split(":")[1]));
				generate2(occ,funcname);     //这里得到map_scvp
				getVars();
				getEachVarSource(funcname);
				getEachVarRelated();
				String gather = result.get(str.split(":")[0]);
				if(gather == null){
					StringBuffer sb = new StringBuffer();
					sb.append("return中变量来源为：");
					//sb.append("return中变量" + str.split(":")[0] + "的来源为#");
					if(global_map.get(funcname).contains(str.split(":")[0])){ //该变量为局部自定义的
						sb.append("局部定义形式");
					}else if(entrance_map.get(funcname).contains(str.split(":")[0])){
						//if(entrance_map.get(funcname).contains(var.split(":")[0])){
							sb.append("参数传入形式");
						//}
					}else{   //该变量为全局变量
						sb.append("全局变量形式");
					}
					if(isFormOutside(str.split(":")[0])){
						sb.append(",并且存在外部输入情况");
					}
					res.add(sb.toString());
				}else{
					res.add(gather);
				}
				
				
				//getEachVarSource(funcname);
//				System.out.println("*****开始分析的变量是" + str + "*****");
//				//getHigest(str);
//				for(String str1 : map_scvp.keySet()){
//					//System.out.println(map_scvp.get(str1).getStructure());
//					map_scvp.get(str1).printSCVP();
//					System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
//				}
//				System.out.println("*****" + str + "分析结束*****");
			}
			//System.out.println("这是我提出最终的答案结果：");
//			for(String str : res){
//				System.out.println(str);
//			}
			
			for(String str : res){
				ress.append(str+"$ ");
				//System.out.println(str);
			}
			
			ress.append(return_feature + "@@");
			//System.out.println(sb.toString());
		}
		
		

		
//		for(String str : global_map.keySet()){
//			HashSet<String> set = global_map.get(str);
//			System.out.println(str);
//			for(String str1 : set){
//				System.out.println(str1);
//			}
//		}
//		
//		for(String str : entrance_map.keySet()){
//			HashSet<String> set = entrance_map.get(str);
//			System.out.println(str);
//			for(String str1 : set){
//				System.out.println(str1);
//			}
//		}
	
		
		StringBuffer r = new StringBuffer();
		for(int i = 1; i < ress.toString().split("#").length; i++){
			r.append(ress.toString().split("#")[i]);
		}
		return r.toString().hashCode();
	}
	
		
	private HashSet<NameOccurrence> vis = new HashSet<>();
	private HashMap<String,SCVP> map_scvp = new HashMap<String,SCVP>();
	
	//记得在每次调用该方法的时候要先清空一下存储结果的地方
	private void getvairable_scvp(NameOccurrence occ, DepGraphNode g, int cond, int depth, String issame) {
			//System.out.println()
			//System.out.println("!!!!!!!!!!!!!!!!!!!");
			//System.out.println(occ);
			if (vis.contains(occ)) {
				return;
			}
			vis.add(occ);
			//count++;
			if (g==null)
				return;
			
			
			String func = ((ASTFunctionDefinition)occ.getLocation().getFirstParentOfType(ASTFunctionDefinition.class)).getImage();
			
			
			if(occ.getScvp_lsc() != null && func.equals(issame)){
				map_scvp.put(occ.getScvp_lsc().occ.toString(), occ.getScvp_lsc());
//				System.out.println(occ.getSCVP_wy().getPosition().getfunctionName());
//				System.out.println(occ.getSCVP_wy());
//				System.out.println("~~~~~~~~~~~~~~~~");
			}
			
			
			int line = Integer.valueOf(occ.toString().split(":")[1]);
			
			if (!map2.containsKey(func)) {
				map2.put(func, new HashSet<Integer>());
			}
			map2.get(func).add(line);
			if (cond == 1) {
				condLineMap.put(func +"_"+ occ.toString().split(":")[1], true);
				//g.setCond(true);
			} else {
				
			}
			if(occ==null)
				return;
			if(occ.getOccurrenceType()==OccurrenceType.USE) {
				regenerateDU(occ, occ.getLocation().getCurrentVexNode());
				List<NameOccurrence> def = occ.getUse_def();  //得到这个变量的所有定义点，都在它的上面
				for(NameOccurrence o : def) {
	//				if(o.getImage().equals(occ.getImage()))
	//					continue;
					DepGraphNode g2 = g.addChild(o);
					//List<DepGraphNode> ifNodes = new ArrayList<DepChain.DepGraphNode>();
	
					VexNode from = o.getLocation().getCurrentVexNode();
					VexNode to = occ.getLocation().getCurrentVexNode();
					List<VexNode> pList = Graph.findAPath(from, to);
					for(int i=0;i<pList.size()-1;i++) {
						VexNode a = pList.get(i);
						if(a.toString().contains("if_head")) {
							for(NameOccurrence o1:a.getOccurrences()){
								DepGraphNode g3 = g.addChild(o1);
								getvairable_scvp(o1, g3, 1, depth+1,issame);
							}
						}
					}
					
					getvairable_scvp(o,g2, cond, depth+1,issame);
				}
				if (def == null || def.size() == 0) {
					//无定义点，尝试找全局
					Graph cfg = occ.getLocation().getCurrentVexNode().getGraph();
					String funcname = cfgmap2.get(cfg);
					InterCallGraph callGraph = InterCallGraph.getInstance();
					MethodNode curMNode = null;
					for(MethodNode mn : callGraph.getINTERMETHOD()) {   //getINTERMETHOD()是得到当前分析过程中全局的函数节点信息
						Method m = mn.getMethod();
						if (m.getName().equals(funcname)) {  //找到该函数的全局结点信息
							curMNode = mn;
							break;
						}
					}
					if (curMNode != null){
						//得到调用点的scvp信息
						Map<Position, ArrayList<SCVPString>> callerInfo = curMNode.getMethod().getCallerInfo();
						if (callerInfo == null || callerInfo.size() == 0) {
							List<Method> callers = new ArrayList<>();//curMNode的调用者，去调用curMNode的调用者的调用者里面找
							for(MethodNode mn : callGraph.getINTERMETHOD()) {  
								for (MethodNode mn2 : mn.getCalls()) {
									if (mn2 == curMNode) {
										callers.add(mn.getMethod());
									}
								}
							}
							
							for (Method caller : callers) {
								Map<String, List<SCVPString>> ext = caller.getExternalEffects();  //有副作用的节点信息
								for (String occstr : ext.keySet()) {
									//System.out.println(ext.size() + "***************");
									SCVPString value = ext.get(occstr).get(0);//假设只有1个
									String occName = occ.toString().split(":")[0];
									String occName1 = occstr.split(":")[0];
									if (occName.equals(occName1)) {
										String fileName = value.getPosition().getfileName();
										String funcName = value.getPosition().getFunctionName();
										String symbolName = occName1;
										int line2 = Integer.valueOf( occstr.split(":")[1]);
										NameOccurrence next = locate(fileName, funcName, symbolName, line2);
										if (next != null) {
											DepGraphNode g4 = g.addChild(next);
											getvairable_scvp(next, g4, cond, depth+1,issame);
										}
									}							
								}
							}
						} else {
							for (List<SCVPString> val : callerInfo.values()) {
								SCVPString scvp = val.get(0);    //仍然只假装有一个啊
								//System.out.println(val.size() + "***************");
								for (String nextocc : scvp.getOccs()) {
									String fileName = scvp.getPosition().getfileName();
									String funcName = scvp.getPosition().getFunctionName();
									String symbolName = nextocc.split(":")[0];
									int line2 = Integer.valueOf( nextocc.split(":")[1]);
									NameOccurrence next = locate(fileName, funcName, symbolName, line2);
									if (next != null) {
										DepGraphNode g5 = g.addChild(next);
										getvairable_scvp(next, g5, cond, depth+1,issame);
									}
								}
							}
						}
					}
	
				}
			} else 	if(occ.getOccurrenceType()==OccurrenceType.DEF_AFTER_USE) {
				//regenerateDU(occ, occ.getLocation().getCurrentVexNode());
				List<NameOccurrence> def = occ.getDef_use();   //得到后面所有使用这个元素的使用点
				for(NameOccurrence o : def) {
					DepGraphNode g2 = g.addChild(o);
	
					VexNode from = o.getLocation().getCurrentVexNode();
					VexNode to = occ.getLocation().getCurrentVexNode();
					List<VexNode> pList = Graph.findAPath(from, to);
					for(int i=0;i<pList.size()-1;i++) {
						VexNode a = pList.get(i);
						if(a.toString().contains("if_head")) {
							for(NameOccurrence o1:a.getOccurrences()){
								DepGraphNode g3 = g.addChild(o1);
								getvairable_scvp(o1, g3, 1, depth+1,issame);
							}
						}
					}
					
					getvairable_scvp(o,g2, cond, depth+1,issame);
				}
			}
			else if(occ.getOccurrenceType() == OccurrenceType.DEF) {
				SCVPString scvp = occ.getSCVP();
				List<String> vlist = scvp.getOccs();
				
				for(String v : vlist) {
					Graph cfg = occ.getLocation().getCurrentVexNode().getGraph();
					NameOccurrence o = cfg.getOcctable().get(v);
					if(o.getImage().toString().equals(occ.getImage()))
						continue;
					DepGraphNode g2 = g.addChild(o);
	
					VexNode from = o.getLocation().getCurrentVexNode();
					VexNode to = occ.getLocation().getCurrentVexNode();
					List<VexNode> pList = Graph.findAPath(from, to);
					for(int i=0;i<pList.size()-1;i++) {
						VexNode a = pList.get(i);
						if(a.toString().contains("if_head")) {
							for(NameOccurrence o1:a.getOccurrences()){
								DepGraphNode g3 = g.addChild(o1);
	//							if (cond==0) {
	//								cond=1;
	//								System.out.println(o + "," + g);
	//							}
								getvairable_scvp(o1, g3,1, depth+1,issame);
							}
						}
					}
					
					getvairable_scvp(o, g2, cond, depth+1,issame);
				}
				String s = scvp.getStructure();
				if(s.contains("Function:")) { //返回值
					int id = s.indexOf("Function");
					s=s.substring(id);
					String[] fs = s.trim().split("Function:");
					ASTStatement statement = (ASTStatement) occ.getLocation().getFirstParentOfType(ASTStatement.class);
					List<Node> priList = statement.findChildrenOfType(ASTPrimaryExpression.class);
	
					for(String f:fs) {
						for(Node n : priList) {
							ASTPrimaryExpression pri = (ASTPrimaryExpression)n;
							if(pri.isMethod() && pri.getImage().equals(f)) {
								if(pri.getMethodDecl()==null)
									continue;
								Method m = pri.getMethodDecl().getMethod();
								SimpleNode entNode = pri.getMethodDecl().getMethodNameDeclaratorNode();
								Graph cfg = null;
								if(entNode instanceof ASTFunctionDefinition)
									cfg = ((ASTFunctionDefinition)entNode).getGraph();
								List<SCVPString> ret = m.getReturnList();
								for(SCVPString scvpString : ret) {
									for(String v : scvpString.getOccs()) {
										if(cfg!=null) {
											NameOccurrence o = cfg.occtable.get(v);
											DepGraphNode g2 = g.addChild(o);
											getvairable_scvp(o, g2,cond, depth+1,issame);
										}
									}
								}
							}
						}
					}
				}
			}
			else if(occ.getOccurrenceType() == OccurrenceType.ENTRANCE) {
				ASTFunctionDefinition funcdef = (ASTFunctionDefinition) occ.getLocation().getFirstParentOfType(ASTFunctionDefinition.class);
				if(funcdef!=null) {
					HashMap<Position, ArrayList<SCVPString>> callerInfo = funcdef.getDecl().getMethod().getCallerInfo();
					for(Position p : callerInfo.keySet()) {
						Graph cfg = cfgmap.get(p.getFunctionName());
						if(cfg!=null) {
							for(SCVPString scvp : callerInfo.get(p)) {
								String fileName = p.getFileName();
								String funcName = p.getFunctionName();
								String symbolName = scvp.getVar();
								int line1 = p.getBeginLine(); 
								NameOccurrence occ2 = locate(fileName, funcName, symbolName, line1);
								if (occ2 == null)
									continue;
								DepGraphNode g3 = g.addChild(occ2);
								getvairable_scvp(occ2, g3, cond, depth+1,issame);
								for(String v:scvp.getOccs()) {
									NameOccurrence o = cfg.occtable.get(v);
									DepGraphNode g2 = g.addChild(o);
									getvairable_scvp(o, g2,cond, depth+1,issame);
								}
							}
						}
					}
				}
			}
		}

	private HashMap<String, HashSet<String>> entrance_map = new HashMap<String, HashSet<String>>();
	public void getEntranceVars(CVexNode cnode){
		HashSet<String> res = new HashSet<String>();
		SimpleNode node = cnode.getMethodNameDeclaration().getNode();
		if (node instanceof ASTFunctionDefinition){
			ASTDeclarator dnode = (ASTDeclarator)node.jjtGetChild(1);
			String Xpath = ".//DirectDeclarator";
			List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
			evaluationResults = StateMachineUtils.getEvaluationResults(dnode, Xpath);
			for(SimpleNode s : evaluationResults){
				if(s.isLeaf() && !s.getImage().equals(node.getImage())){
					res.add(s.getImage());
				}
			}
		}
		entrance_map.put(node.getImage(),res);
//		if(res.size() != 0){
//			entrance_map.put(node.getImage(),res);
//		}
	}
	
	public HashMap<String, HashSet<String>> global_map = new HashMap<String, HashSet<String>>();
	public void getGlobalVarsList(CVexNode cnode){
		HashSet<String> res = new HashSet<String>();
		SimpleNode node = cnode.getMethodNameDeclaration().getNode();
		if (node instanceof ASTFunctionDefinition){
			ASTCompoundStatement com = (ASTCompoundStatement)node.jjtGetChild(node.jjtGetNumChildren()-1);
			String Xpath = ".//DirectDeclarator | .//PrimaryExpression";
			List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
			evaluationResults = StateMachineUtils.getEvaluationResults(com, Xpath);
			for(SimpleNode s : evaluationResults){
				if(s.isLeaf() && !s.getImage().equals(node.getImage())){
					res.add(s.getImage());
				}
			}
		}
//		System.out.println(node.getImage());
//		for(String str1 : res){
//			System.out.println(str1);
//		}
		global_map.put(node.getImage(),res);
	}
	
	/**获得C语言中常用的外部输入函数*/
	public void getReservedWords(){
		File file = new File("./outputlib.txt");
        BufferedReader reader = null;
        String str = "";  
        try{
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {        
            	str += tempString;           
            }
            reader.close();
            String[] str_reserved = str.split(" ");
            for(int i = 1; i < str_reserved.length; i++){
            	list_outsource.add(str_reserved[i]);
            }        
        }catch(IOException e){
            e.printStackTrace();
        }
	}

	
	public static void main(String[] args) throws Exception{
		String[] str = new String[6];
		str[0] = "C:/Users/Miss_Lizi/Desktop/depchain";
		str[1] = "C:/Users/Miss_Lizi/Desktop/depchain/depchain.c";
		str[2] = "fun";
		str[3] = "x";
		str[4] = "18";
		String filePath = "C:/Users/lsc/Desktop/5.c";
		Func_Features test = new Func_Features();
		test.getALLFuncFeatures(filePath);
		
		//test.getFunctionFeatures(str[1], str[2]);
//		System.out.println(test.analyse2());//输入域分析
//		System.out.println(test.getCondLineMap());//高亮的条件节点 ,就是条件分支那几个地方
//		System.out.println("调用路径：");
		HashSet<String> set = test.global_map.get("f2");
		for(String str1 : set){
			System.out.println("f2中的变量有" + str1);
		}
		
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
	
//	public static void main(String[] args) throws Exception {
//		DepChain test=new DepChain(args);
//		int type = Integer.valueOf(args[5]);
//		if (type==1) {
//			System.out.println(test.analyse1());//外部输入源
//		} else if(type==2) {
//			System.out.println(test.analyse2());//输入域分析
//			System.out.println(test.getCondLineMap());//高亮的条件节点
//			System.out.println("调用路径：");
//			for(String p : test.pathSet) {//调用路径
//				System.out.println(p);
//			}
//		} else if(type==3) {
//			System.out.println(test.analyse3());//目标函数的输入域分析
//			System.out.println("调用路径：");
//			for(String p : test.pathSet) {
//				System.out.println(p);
//			}
//			
//		} else if(type == 4) {
//			//test.process();
//			System.out.println(test.analyse4());//影响域分析
//		} else if (type == 5) {
//			test.sryGraduateTest();
//		} else if (type == 6) {
//			test.sryGraduateTestEffectField();
//		}
//	}
	
	

	public MethodNode getMethodNodeByFuncName(String funcName) {
		MethodNode mn = null;
		Graph cfg = cfgmap.get(funcName);
	    ConcurrentHashMap <Method, MethodNode> interMethodsTable = InterCallGraph.getInstance().getCallRelationTable();
		try {
			SimpleNode treeNode = cfg.getEntryNode().getTreenode();
			ASTFunctionDefinition fd = null;
			if (treeNode instanceof ASTFunctionDefinition) {
				fd = (ASTFunctionDefinition) treeNode;
			} else {
				fd = (ASTFunctionDefinition) treeNode.getFirstParentOfType(ASTFunctionDefinition.class);
				
			}		
			mn =  interMethodsTable.get(fd.getDecl().getMethod());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
		return mn;
	}
	
	public Map<String, Set<Integer>> analyse4() {
		process();
		Map<String, Set<Integer>> map = new HashMap<>();
		Queue<NameOccurrence> queue = new LinkedList<>();
		NameOccurrence occ = locate(args[1],args[2],args[3],Integer.valueOf(args[4]));
		
		queue.offer(occ);
		map.put(args[2], new HashSet<Integer>());
		//map.get(args[2]).add(Integer.valueOf(args[4]));
		
		while (!queue.isEmpty()) {
			NameOccurrence cur = queue.poll();
			ASTFunctionDefinition astNode = (ASTFunctionDefinition)cur.getLocation().getFirstParentOfType(ASTFunctionDefinition.class);
			String funcName = astNode.getImage();
			if (!map.containsKey(funcName)) {
				map.put(funcName, new HashSet<Integer>());
			}
//			System.out.println(cur);
//			System.out.println(funcName);
			map.get(funcName).add(Integer.valueOf(cur.toString().split(":")[1]));
			if (cur.getOccurrenceType() == OccurrenceType.DEF || cur.getOccurrenceType() == OccurrenceType.ENTRANCE) {
				List<NameOccurrence> defUseList = cur.getDef_use();
				for (NameOccurrence o1 : defUseList) {
					queue.offer(o1);
				}
			} else if (cur.getOccurrenceType() == OccurrenceType.USE) {
				List<NameOccurrence> effect = cur.getEffected();
				for (NameOccurrence o2 : effect) {
					queue.offer(o2);
				}
				// 考虑过程间参数情况！
				SimpleNode node = cur.getLocation();
				ASTArgumentExpressionList argnode = (ASTArgumentExpressionList) node.getFirstParentOfType(ASTArgumentExpressionList.class);
				ASTAssignmentExpression assignnode = (ASTAssignmentExpression) node.getFirstParentOfType(ASTAssignmentExpression.class);

				// 位于参数
				if (argnode != null && assignnode != null) {
					while (assignnode.jjtGetParent().equals(argnode)) {
						assignnode = (ASTAssignmentExpression) assignnode.getFirstParentOfType(ASTAssignmentExpression.class);
					}
					int idx = assignnode.getIndexOfParent()+1;
					Node primaryExpression = argnode.getPrevSibling();
					if (primaryExpression instanceof ASTPrimaryExpression) {
						if (((ASTPrimaryExpression) primaryExpression).isMethod()) {
							SimpleNode funcheadNode = ((ASTPrimaryExpression)primaryExpression).getMethodDecl().getNode();
							Graph cfg = null;
							if (funcheadNode instanceof ASTFunctionDefinition) {
								cfg =((ASTFunctionDefinition) funcheadNode).getGraph();
							} else if (funcheadNode instanceof ASTFunctionDeclaration) {
								//System.out.println("[image]"+funcheadNode.getImage());
								cfg = cfgmap.get(funcheadNode.getImage());

							}
							if (cfg == null) {
								continue;
							}
							NameOccurrence argocc = null;
							for (NameOccurrence headocc : cfg.getEntryNode().getOccurrences()){
								ASTParameterDeclaration paramnode = (ASTParameterDeclaration) headocc.getLocation().getFirstParentOfType(ASTParameterDeclaration.class);
								if (paramnode == null)
									break;
								if (paramnode.getIndexOfParent()+1 == idx) {
									argocc = headocc;
									break;
								}
							}
							//System.out.println(argocc);
							if (argocc != null) {
								queue.offer(argocc);
							}
						}
					}
				}
			}
			
		}
		
		return map;
	}
	//生成所有输入点
	public List<ExternalInputSource> getExternalSourceList() {
		//process();//预处理
		List<ExternalInputSource> list = new ArrayList<>();
		for(Graph g : cfgmap2.keySet()) {
			g.numberOrderVisit(new ExternalInputSourceVisitor(), list);
		}
		
		return list;
	}
	
	
	
	@SuppressWarnings("unused")
	public Map<String, Set<ExternalInputSource>> analyse1() {
		process();
		List<ExternalInputSource> list = getExternalSourceList();
		
		Map<String, Set<ExternalInputSource>> map = new HashMap<>();
		
		for (ExternalInputSource i : list) {
			String func = i.getFuncName();
			int line = i.getLine();
			if (!map.containsKey(func)) {
				map.put(func, new HashSet<ExternalInputSource>());
			}
			map.get(func).add(i);
		}
		
		return map;
	}
	private Map<String, Set<Integer>> map2 = new HashMap<>();
	
	private Map<String, Set<String>> vexMap = new HashMap<>();
	
	private Map<String, Boolean> condLineMap = new HashMap<>();
	
	public HashSet<String> pathSet = new HashSet<String>();

	private void generate2(NameOccurrence occ, String func) {
		DepGraphNode g = new DepGraphNode(occ);
		pathSet.clear();
		//g.printToFile();
		vis.clear();
		getvairable_scvp(occ, g, 0, 1,func);
		String funcname = cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph());
		pathSet.add(funcname);
		dfsGetAllPath(g,funcname);
		//g.printToFile();
	}
	
	private void dfsGetAllPath(DepGraphNode root, String path) {  //得到调用路径的信息
		if (root.child == null || root.child.size() == 0) {
			pathSet.add(path);
		}
		String curFuncName = cfgmap2.get(root.occ.getLocation().getCurrentVexNode().getGraph());
		for (DepGraphNode child : root.child) {
			String childFuncname = cfgmap2.get(child.occ.getLocation().getCurrentVexNode().getGraph());
			if (!curFuncName.equals(childFuncname))
				dfsGetAllPath(child, path + "->" + childFuncname);
			else
				dfsGetAllPath(child, path);
		}
	}



	
	
	//private int count = 0;
	
	//String func = "fun";
	
	
	
	@SuppressWarnings("unused")
	private class DepGraphNode {
		private NameOccurrence occ;
		private List<DepGraphNode> child;
		private List<VexNode> path;
		private boolean isCond;
		public DepGraphNode(NameOccurrence occ) {
			this.occ=occ;
			child = new ArrayList<DepGraphNode>();
			path = new ArrayList<VexNode>();
			isCond = false;
			
		}
		public JSONObject generateJSON() {
			JSONObject root = new JSONObject();
			JSONArray pathArray = new JSONArray();
			for(VexNode v : path)
				pathArray.add(v.getName());
			
			root.put("path", pathArray);
			root.put("NameOccurrence", occ.toString());
			root.put("VexNode", occ.getLocation().getCurrentVexNode().getName());//?
			root.put("functionName", cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph()));
			
			JSONArray childArray = new JSONArray();
			for(DepGraphNode c : child) {
				childArray.add(c.generateJSON());
			}
			
			root.put("child", childArray);
			root.put("isCond", isCond);
			return root;
		}
		public DepGraphNode addChild(NameOccurrence o) {
			DepGraphNode childNode = new DepGraphNode(o);
			child.add(childNode);
			
			//try {
			
				VexNode from = o.getLocation().getCurrentVexNode();
				VexNode to = occ.getLocation().getCurrentVexNode();
				childNode.path = Graph.findAPath(from, to);
				 
				return childNode;
//			} catch (Exception e) {
//				//e.printStackTrace();
//				return null;
//			}
		}
		
		public NameOccurrence getOcc() {
			return occ;
		}
		public void setOcc(NameOccurrence occ) {
			this.occ = occ;
		}
		public List<DepGraphNode> getChild() {
			return child;
		}
		public void setChild(List<DepGraphNode> child) {
			this.child = child;
		}

		public List<String> visit(VexNode defNode, VexNode useNode) {
			return null;
		}
	
	
		public void print() {
			String fileName = occ.getLocation().getFileName();
			String funcName = cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph());
			String line = occ.toString().split(":")[1];
			String occname = occ.getImage();
			this.dfs(this, this, "路径：\n{文件名："+fileName+", 函数名:"+funcName+", 代码行:"+line+", 符号名:"+occname+"}", false, null);
		}
		public void printToFile() {
			try {
				FileWriter fw = new FileWriter("C:\\Users\\Miss_Lizi\\Desktop\\1.txt");
				String fileName = occ.getLocation().getFileName();
				String funcName = cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph());
				String line = occ.toString().split(":")[1];
				String occname = occ.getImage();
				this.dfs(this, this, "路径：\n{文件名："+fileName+", 函数名:"+funcName+", 代码行:"+line+", 符号名:"+occname+"}", true, fw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		private void dfs(DepGraphNode root, DepGraphNode node,String path, boolean toFile, FileWriter fw) {
			if(node==null || node.child==null || node.child.size()==0) {
				List<VexNode> list = null;
				if(toFile)
					try {
						fw.write(path+"\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				else
					System.out.println(path+"\n");
			}
			else {
				for(DepGraphNode child : node.child) {
					String fileName = child.occ.getLocation().getFileName();
					String funcName = cfgmap2.get(child.occ.getLocation().getCurrentVexNode().getGraph());
					String line = child.occ.toString().split(":")[1];
					String occname = child.occ.getImage();
					
					VexNode from = child.occ.getLocation().getCurrentVexNode();
					VexNode to = node.occ.getLocation().getCurrentVexNode();
					this.path = Graph.findAPath(from, to);
					String circumstance = analysisPath(this.path);
					if(circumstance.equals("\n"))
						circumstance="";
					String path2 = path+"\n"+"{文件名："+fileName+", 函数名:"+funcName+", 代码行:"+line+", 符号名:"+occname+", "+circumstance+"}";
					dfs(root, child, path2,toFile, fw);
				}
			}
		}
		private String analysisPath(List<VexNode> list) {
			String str  = "\n";
			for(int i=0;i<list.size()-1;i++) {
				VexNode from = list.get(i);
				VexNode to = list.get(i+1);
				if(from.toString().contains("if_head")) {
					int begin = from.getTreenode().getBeginLine();
					String code="";
					code+=begin+"\t"+readLineOfFile(from.getTreenode().getFileName(), begin).replace(" ", "")+"\n";
					Edge e = from.getEdgeByHead(to);
					if(e.toString().contains("T")) {
						str+="条件：\n"+code+"---真分支\n";
					}
					else {
						str+="条件：\n"+code+"---假分支\n";
					}
				}
			}
			return str;
		}
		@Override
		public String toString() {
			return "[occ=" + occ.toString() + ", child=" + child + "]";
		}
		public List<VexNode> getPath() {
			return path;
		}
		public void setPath(List<VexNode> path) {
			this.path = path;
		}
		public boolean isCond() {
			return isCond;
		}
		public void setCond(boolean isCond) {
			this.isCond = isCond;
		}
	}

	private String readLineOfFile(String fileName, int lineNumber) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName))); 
			String line;
			line = reader.readLine();
			int num = 0; 
			String str = "";
			while (line != null) { 
				if (lineNumber == ++num) { 
					str=line;
					break;
				} 
				line = reader.readLine(); 
			} 
			reader.close();
			return str; 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	} 
	
	private void regenerateDU(NameOccurrence occ,VexNode vNode) {
		HashSet<Integer> visited = new HashSet<Integer>();
		visited.add(vNode.getSnumber());
		
		Queue<VexNode> q = new LinkedList<VexNode>();
		for(Edge e:vNode.getInedges().values()) {
			if(!visited.contains(e.getTailNode().getSnumber())) {
				q.offer(e.getTailNode());
				visited.add(e.getTailNode().getSnumber());
			}
		}
		VexNode cur = null;
		while(!q.isEmpty()) {
			cur = q.poll();
			visited.add(cur.getSnumber());
			//cur是函数调用点且有对occ的副作用
			List<Node> list = cur.getTreenode().findChildrenOfType(ASTPrimaryExpression.class);
			if(list.size()!=0) {
				ASTPrimaryExpression node = (ASTPrimaryExpression)(list.get(0));
				if(node.isMethod()) { //节点本身是函数调用
					if(node.getImage().equals("malloc")) {
						occ.addUseDef(new NameOccurrence(null, node, "malloc"));
						break;
					}
					if(node.getMethodDecl()==null)
						break;
					HashMap<String, List<SCVPString>> externalEffects = node.getMethodDecl().getMethod().getExternalEffects();
					if(!externalEffects.isEmpty()) {
						for(String key:externalEffects.keySet()) {
							if(key.split(":")[0].equals(occ.getImage())) {
								//vNode的定义点在node点，occ的定义点是key
								occ.getUse_def().clear();
								List<SCVPString> value = externalEffects.get(key);
								for(SCVPString scvp : value) {
									Position p = scvp.getPosition();
									String methodName = p.getFunctionName();
									//System.out.println(methodName);
									Graph g = cfgmap.get(methodName);
									NameOccurrence defOcc = g.getOcctable().get(key);
									occ.addUseDef(defOcc);
								}
							}
						}
					}
				}
			}
			for(Edge e:cur.getInedges().values()) {
				if(!visited.contains(e.getTailNode().getSnumber())) {
					q.offer(e.getTailNode());
					visited.add(e.getTailNode().getSnumber());
				}
			}
		}
		
	}
	public List<AnalysisElement> getElements() {
		return elements;
	}
	public void setElements(List<AnalysisElement> elements) {
		this.elements = elements;
	}
	
	public List<String> getFiles() {
		return files;
	}
	public void setFiles(List<String> files) {
		this.files = files;
	}
	public InterCallGraph getInterCGraph() {
		return interCGraph;
	}
	public void setInterCGraph(InterCallGraph interCGraph) {
		this.interCGraph = interCGraph;
	}
	public Pretreatment getPre() {
		return pre;
	}
	public void setPre(Pretreatment pre) {
		this.pre = pre;
	}
	public HashMap<String, ASTTranslationUnit> getAstmap() {
		return astmap;
	}
	public void setAstmap(HashMap<String, ASTTranslationUnit> astmap) {
		this.astmap = astmap;
	}
	public HashMap<String, Graph> getCfgmap() {
		return cfgmap;
	}
	public void setCfgmap(HashMap<String, Graph> cfgmap) {
		this.cfgmap = cfgmap;
	}
	public HashMap<Graph, String> getCfgmap2() {
		return cfgmap2;
	}
	public void setCfgmap2(HashMap<Graph, String> cfgmap2) {
		this.cfgmap2 = cfgmap2;
	}
	public HashMap<String, CGraph> getCgMap() {
		return cgMap;
	}
	public void setCgMap(HashMap<String, CGraph> cgMap) {
		this.cgMap = cgMap;
	}
	public Map<String, Set<String>> getVexMap() {
		return vexMap;
	}
	public void setVexMap(Map<String, Set<String>> vexMap) {
		this.vexMap = vexMap;
	}
	public String[] getArgs() {
		return args;
	}
	public void setArgs(String[] args) {
		this.args = args;
	}
	public Map<String, Boolean> getCondLineMap() {
		return condLineMap;
	}
	public void setCondLineMap(Map<String, Boolean> condLineMap) {
		this.condLineMap = condLineMap;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}
