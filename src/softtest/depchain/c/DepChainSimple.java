package softtest.depchain.c;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import softtest.DefUseAnalysis.c.DUAnalysisVisitor;
import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTFunctionDeclaration;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTParameterDeclaration;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.CParserVisitor;
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
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.interpro.c.Method;
import softtest.interpro.c.MethodNode;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;
import softtest.rules.gcc.fault.OOB_CheckStateMachine;
import softtest.scvp.c.Position;
import softtest.scvp.c.SCVPString;
import softtest.scvp.c.SCVPVisitor;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;


public class DepChainSimple {
	private List<AnalysisElement> elements= new ArrayList<AnalysisElement>();;
	private String analysisDir="";
	private List<String> files=new ArrayList<String>();			//���ڴ洢�ռ���������.c�ļ�
	private InterCallGraph interCGraph =InterCallGraph.getInstance();   //�õ�������Щ�������ļ���������ϵ
	private String[] args;
	private Pretreatment pre=new Pretreatment();
	
	public DepChainSimple(String[] args)
	{
		
		//add by lsc 2018/9/20
		//�˴�Ϊ����·���µ��ļ���args[0]��ʾ����·���µ�����.c�ļ���args[1]��ʾ����ָ����.c�ļ�
		this.analysisDir=args[1];
		this.setArgs(args);
		init();
	}
	private HashMap<String, ASTTranslationUnit> astmap = new HashMap<String, ASTTranslationUnit>();
	private HashMap<String, Graph> cfgmap = new HashMap<String, Graph>();
	private HashMap<Graph, String> cfgmap2 = new HashMap<Graph, String>();
	private HashMap<String, CGraph> cgMap = new HashMap<String, CGraph>();
	//�ռ�����·���µ�����.CԴ�ļ�
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

	//����Ԥ����ĳ�ʼ��
	private void init()
	{
		pre.setPlatform(PlatformType.GCC);
		
		File srcFileDir=new File(analysisDir);
		collect(srcFileDir);
	}
	
	
	public static void main(String[] args) throws Exception {
		DepChainSimple test=new DepChainSimple(args);
		int type = Integer.valueOf(args[5]);
		if(type==2) {
			System.out.println(test.analyse2());//���������
			
	
			//���д����������û�ܵõ�����
			System.out.println(test.getCondLineMap());//�����������ڵ�
			System.out.println("����·����");
			for(String p : test.pathSet) {//����·��
				System.out.println(p);
			}
		
			//ע����һ�����
//			test.listSCVP("f2");
			//modify by lsc 2018/9/18
			test.listSCVP(args[2]);                    //��ӡ��Ӧ������scvp��Ϣ
			
		} 
	}
	
	
	
	/**������.CԴ�ļ����ν��д���Ԥ���롢����������ȫ�ֺ������ù�ϵͼ*/
	private void process()
	{
		//��һ����������.CԴ�ļ�����Ԥ����
		PreAnalysis();
		
		//�ڶ���������ȫ�ֺ������ù�ϵͼ
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
	private void PreAnalysis()
	{
		for(String srcFile:files)
		{
			AnalysisElement element=new AnalysisElement(srcFile);
			elements.add(element);
			//Ԥ����֮���.IԴ�ļ�
			String afterPreprocessFile=null;
			List<String> include = new ArrayList<String>();
			include.add("C:/Program Files (x86)/DTS/DTS/DTSGCC/include");
			List<String> macro = new ArrayList<String>();
			afterPreprocessFile=pre.pretreat(srcFile, include, macro);//���ø���������Ԥ����ָ�������м��ļ�
			
			try {
				String temp=element.getFileName();
				//���������﷨��
				System.out.println("���ɳ����﷨��...");
				System.out.println(afterPreprocessFile);
				CParser parser=CParser.getParser(new CCharStream(new FileInputStream(afterPreprocessFile)));
				ASTTranslationUnit root=parser.TranslationUnit();
				astmap.put(srcFile, root);//���﷨�����ڴ��ͨ���ļ�������
				
				//�������ű�
				System.out.println("���ɷ��ű�...");
				ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
				root.jjtAccept(sc, null);
				OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
				root.jjtAccept(o, null);
				
				//����ȫ�ֺ������ù�ϵ
				System.out.println("����ȫ�ֺ������ù�ϵ...");
				root.jjtAccept(new InterMethodVisitor(), element);
				
				//�ļ��ں������ù�ϵͼ
				System.out.println("�����ļ��ں������ù�ϵ...");
				CGraph g = new CGraph();
				((AbstractScope)(root.getScope())).resolveCallRelation(g);
				List<CVexNode> list = g.getTopologicalOrderList(element);
				Collections.reverse(list);
				cgMap.put(srcFile, g);
				
				//���ɿ�����ͼ
				System.out.println("���ɿ�����ͼ...");
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
				
				//���ɶ���ʹ����
				System.out.println("���ɶ���ʹ����...");
				
				/**add by lsc 2018/9/14�˴��ȽϹؼ��ĵ�����ASTTranslationUnit.java�е�
				 * public Object jjtAccept(CParserVisitor visitor, Object data) ����*/
				root.jjtAccept(new DUAnalysisVisitor(), null);
			
		
				//��������
				//ע�� lsc 208/9/19
//				ControlFlowDomainVisitor cfd = new ControlFlowDomainVisitor();
//				for (CVexNode cvnode : list) {
//					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
//					//if (node instanceof ASTFunctionDefinition && args[2].equals("f2") && args[3].equals("b") && args[4].equals("7")) {
//					if (node instanceof ASTFunctionDefinition && !args[1].contains("uucp")){
//						//cfd.visit((ASTFunctionDefinition)node, null);
//					} 
//				}
				
				//����SCVP
				System.out.println("����scvp...");
				//root.jjtAccept(new SCVPVisitor(), null);
				for (CVexNode cvnode : list) {
					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
					if (node instanceof ASTFunctionDefinition) {
//						System.out.println(cvnode.toString());
						
						node.jjtAccept(new SCVPVisitor(), null);
					
					} 
				}
				System.out.println("OK.");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	
	
	private Map<String, Set<Integer>> map2 = new HashMap<>();
	public Map<String, Set<Integer>> analyse2(){
		

		process();
		// ��ȡҪ�������NameOccurrence
		NameOccurrence occ = locate(args[1],args[2],args[3],Integer.valueOf(args[4]));
		
		//add test by lsc 2018/9/16
//	    System.out.println("lsc");
//	    System.out.println(occ.getOccurrenceType());
//	    System.out.println(occ.toString());
		
	    
	    
		generate2(occ);
		//modifyResult();
		return map2;
	}

	
	
	public HashSet<String> pathSet = new HashSet<String>();

	/**
	 * 
	 * @param occ
	 */
	private void generate2(NameOccurrence occ) {
		DepGraphNode g = new DepGraphNode(occ);
		
		//���·������
		pathSet.clear();
		
		//���Set����������׼���洢������Ϣ(NameOccurrence)
		vis.clear();
		
		//��dfs������ģ��ݹ飬�����ֵ��������Ϊ�����ṩg,��ȫ�ֱ������������������ĵ��ù�ϵ
		dfs(occ, g, 0, 1);
		
		
        /*
         * test point add by lsc 2018/9/20 ,ȫ�ֱ���map2�д洢��ÿ�������������Դ����к�
		for(Map.Entry<String, Set<Integer>> entry : map2.entrySet()) {
			String fun = entry.getKey();
			Set<Integer> set = entry.getValue();
			for(int i : set) {
				System.out.print(i+" ");
			}
			System.out.println();
		}
		*/
		
		//���漸�д�������弴�����������ĵ��ù�ϵ
		String funcname = cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph());
		pathSet.add(funcname);
		dfsGetAllPath(g,funcname);
		
	}
	


	
	/**
	 * ��ӡ�������ڸ����ڵ��scvp����Ϣ
	 * @param funcName
	 */
	private void listSCVP(String funcName) {
		Graph cfg = cfgmap.get(funcName);
		JSONObject jsonObject = new JSONObject(true);
		
		GraphVisitor visitor = new DepChainUtil.listSCVPVisitor();
		cfg.numberOrderVisit(visitor, jsonObject);
		
		//modify by lsc 2018/9/18��仰Ҫ�ú÷���
		System.out.println(JSON.toJSONString(jsonObject, true));
	}
	
	private Map<String, Set<String>> vexMap = new HashMap<>();
	private Map<String, Boolean> condMap = new HashMap<>();
	private Map<String, Boolean> condLineMap = new HashMap<>();
	
	/*modify by lsc 2018/9/20
	private JSONObject generate(NameOccurrence occ) {
		//try {
			//FileWriter fw = new FileWriter("highlight.json");
		DepGraphNode g = new DepGraphNode(occ);
		dfs(occ, g, 0, 1);
		//g.print();
		JSONObject jsonObject = g.generateJSON();
		parse(jsonObject);
		
		JSONObject jsonObject2 = new JSONObject();
		for(String key : vexMap.keySet()) {
			JSONArray valueArr = new JSONArray();
			valueArr.addAll(vexMap.get(key));
			jsonObject2.put(key, valueArr);
		}
		String funcname = cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph());

		dfsGetAllPath(g,funcname);

		return jsonObject2;
	}
	*/
	private void dfsGetAllPath(DepGraphNode root, String path) {
		if (root.child == null || root.child.size() == 0) {
			pathSet.add(path);
		}
		String curFuncName = cfgmap2.get(root.occ.getLocation().getCurrentVexNode().getGraph());
		for (DepGraphNode child : root.child) {
			String childFuncname = cfgmap2.get(child.occ.getLocation().getCurrentVexNode().getGraph());
			
			if (!curFuncName.equals(childFuncname))
			//analysis by lsc 2018/9/16 ��ͷ ->�����ǲ���ָ���ˣ�
//				dfsGetAllPath(child, path + "->" + childFuncname);
				//add by lsc 2018/9/18
				dfsGetAllPath(child, path + "<-" + childFuncname);
			else
				dfsGetAllPath(child, path);
		}
	}



	private void parse(JSONObject obj) {
		String funcName = obj.getString("functionName");
		if(!vexMap.containsKey(funcName))
			vexMap.put(funcName, new HashSet<String>());
		String vexName = obj.getString("VexNode");
		vexMap.get(funcName).add(vexName);
		JSONArray path = obj.getJSONArray("path");
		for(String vex : path.toJavaList(String.class)) {
			vexMap.get(funcName).add(vex);
			//condMap.put(vex, obj.getBoolean("isCond"));
		}
		
		JSONArray child = obj.getJSONArray("child");
		for(JSONObject childObj : child.toJavaList(JSONObject.class)) {
			parse(childObj);
		}
	}
	
	
	
	private HashSet<NameOccurrence> vis = new HashSet<>();
	/**
	 * 
	 * @param NameOccurrence occ
	 * @param DepGraphNode g
	 * @param int cond
	 * @param int depth
	 */
	private void dfs(NameOccurrence occ, DepGraphNode g, int cond, int depth) {
		if (vis.contains(occ)) {
			return;
		}
	
		
		vis.add(occ);
		//count++;
		if (g==null)
			return;
		//if (depth > 5) return;
		String func = ((ASTFunctionDefinition)occ.getLocation().getFirstParentOfType(ASTFunctionDefinition.class)).getImage();
		
		int line = Integer.valueOf(occ.toString().split(":")[1]);
		
		
		//add by lsc2018/9/16
		//map2�Ķ��� �� private Map<String, Set<Integer>> map2 = new HashMap<>();
		if (!map2.containsKey(func)) {
			map2.put(func, new HashSet<Integer>());
		}
		map2.get(func).add(line);
		
		//add by lsc 2018/9/16
		//condLineMap�Ķ��� ��private Map<String, Boolean> condLineMap = new HashMap<>();
		if (cond == 1) {
			condLineMap.put(func +"_"+ occ.toString().split(":")[1], true);
//			System.out.println(func +"_"+ occ.toString().split(":")[1]+"lsc");
			//g.setCond(true);
		} else {
			
		}
		if(occ==null)
			return;
		if(occ.getOccurrenceType()==OccurrenceType.USE) {
			//���д������û��  note by lsc 2018/9/20
			regenerateDU(occ, occ.getLocation().getCurrentVexNode());
			List<NameOccurrence> def = occ.getUse_def();
			for(NameOccurrence o : def) {
//				if(o.getImage().equals(occ.getImage()))
//					continue;
				DepGraphNode g2 = g.addChild(o);
				//List<DepGraphNode> ifNodes = new ArrayList<DepChain.DepGraphNode>();

				VexNode from = o.getLocation().getCurrentVexNode();
				VexNode to = occ.getLocation().getCurrentVexNode();
				List<VexNode> pList = Graph.findAPath(from, to);
				
				//ΪʲôpList.size-1 ?
				for(int i=0;i<pList.size()-1;i++) {
					VexNode a = pList.get(i);
					if(a.toString().contains("if_head")) {
						for(NameOccurrence o1:a.getOccurrences()){
							DepGraphNode g3 = g.addChild(o1);
							dfs(o1, g3, 1, depth+1);
						}
					}
				}
				
				dfs(o,g2, cond, depth+1);
			}
		
			if (def == null || def.size() == 0) {
				//�޶���㣬������ȫ��
				Graph cfg = occ.getLocation().getCurrentVexNode().getGraph();
				String funcname = cfgmap2.get(cfg);
				InterCallGraph callGraph = InterCallGraph.getInstance();
				MethodNode curMNode = null;
				for(MethodNode mn : callGraph.getINTERMETHOD()) {
					Method m = mn.getMethod();
					if (m.getName().equals(funcname)) {
						curMNode = mn;
						break;
					}
				}
				if (curMNode != null){
					Map<Position, ArrayList<SCVPString>> callerInfo = curMNode.getMethod().getCallerInfo();
					if (callerInfo == null || callerInfo.size() == 0) {
						List<Method> callers = new ArrayList<>();//curMNode�ĵ�����
						for(MethodNode mn : callGraph.getINTERMETHOD()) {
							for (MethodNode mn2 : mn.getCalls()) {
								if (mn2 == curMNode) {
									callers.add(mn.getMethod());
								}
							}
						}
						
						for (Method caller : callers) {
							Map<String, List<SCVPString>> ext = caller.getExternalEffects();
							for (String occstr : ext.keySet()) {
								SCVPString value = ext.get(occstr).get(0);//����ֻ��1��
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
										dfs(next, g4, cond, depth+1);
									}
								}							
							}
						}
					} else {
						for (List<SCVPString> val : callerInfo.values()) {
							SCVPString scvp = val.get(0);
							for (String nextocc : scvp.getOccs()) {
								String fileName = scvp.getPosition().getfileName();
								String funcName = scvp.getPosition().getFunctionName();
								String symbolName = nextocc.split(":")[0];
								int line2 = Integer.valueOf( nextocc.split(":")[1]);
								NameOccurrence next = locate(fileName, funcName, symbolName, line2);
								if (next != null) {
									DepGraphNode g5 = g.addChild(next);
									dfs(next, g5, cond, depth+1);
								}
							}
						}
					}
				}

			}
		} else 	if(occ.getOccurrenceType()==OccurrenceType.DEF_AFTER_USE) {
			//regenerateDU(occ, occ.getLocation().getCurrentVexNode());
			List<NameOccurrence> def = occ.getDef_use();
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
							dfs(o1, g3, 1, depth+1);
						}
					}
				}
				
				dfs(o,g2, cond, depth+1);
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
							dfs(o1, g3,1, depth+1);
						}
					}
				}
				
				dfs(o, g2, cond, depth+1);
			}
			String s = scvp.getStructure();
			if(s.contains("Function:")) { //����ֵ
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
										dfs(o, g2,cond, depth+1);
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
							dfs(occ2, g3, cond, depth+1);
							for(String v:scvp.getOccs()) {
								NameOccurrence o = cfg.occtable.get(v);
								DepGraphNode g2 = g.addChild(o);
								dfs(o, g2,cond, depth+1);
							}
						}
					}
				}
			}
		}
	}
	
	/**add by lsc 2018/9/19
	 * ��ȡҪ�������NameOccurrence
	 * @param fileName
	 * @param funcName
	 * @param symbolName
	 * @param line
	 * @return
	 */
	private NameOccurrence locate(String fileName, String funcName, String symbolName, int line) {
		Graph cfg = cfgmap.get(funcName);
		String occStr = symbolName+":"+line;
		if (cfg == null) return null;
		return cfg.getOcctable().get(occStr);
	}
	
	/**
	 * occ;
		 child;
		private List<VexNode> path;
		 isCond;
	 * @author lsc
	 *
	 */
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
		
		//��ȡ���ڵ����Ϣ���������ӽڵ㣬·�����������������ֵ�
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
		
	
	
		public void print() {
			String fileName = occ.getLocation().getFileName();
			String funcName = cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph());
			String line = occ.toString().split(":")[1];
			String occname = occ.getImage();
			this.dfs(this, this, "·����\n{�ļ�����"+fileName+", ������:"+funcName+", ������:"+line+", ������:"+occname+"}", false, null);
		}
		public void printToFile() {
			try {
				FileWriter fw = new FileWriter("C:\\Users\\cmershen\\Desktop\\DTS\\duʵ������\\1.txt");
				String fileName = occ.getLocation().getFileName();
				String funcName = cfgmap2.get(occ.getLocation().getCurrentVexNode().getGraph());
				String line = occ.toString().split(":")[1];
				String occname = occ.getImage();
				this.dfs(this, this, "·����\n{�ļ�����"+fileName+", ������:"+funcName+", ������:"+line+", ������:"+occname+"}", true, fw);
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
					String path2 = path+"\n"+"{�ļ�����"+fileName+", ������:"+funcName+", ������:"+line+", ������:"+occname+", "+circumstance+"}";
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
						str+="������\n"+code+"---���֧\n";
					}
					else {
						str+="������\n"+code+"---�ٷ�֧\n";
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
	
	/**
	 * �ӵ�ǰ�ڵ㣬�������ɶ���ʹ����
	 * @param occ
	 * @param vNode
	 */
	private void regenerateDU(NameOccurrence occ,VexNode vNode) {
		HashSet<Integer> visited = new HashSet<Integer>();
		visited.add(vNode.getSnumber());
		
		Queue<VexNode> q = new LinkedList<VexNode>();
		for(Edge e:vNode.getInedges().values()) {
			if(!visited.contains(e.getTailNode().getSnumber())) {
				
				/* add by lsc 2018/9/16
				 * add()��offer()��������������һ��Ԫ�ء�
				 * һЩ�����д�С���ƣ�����������һ�����Ķ����м���һ�����
				 * ���� add() �����ͻ��׳�һ�� unchecked �쳣��
				 * ������ offer() �����᷵�� false����˾Ϳ����ڳ����н�����Ч���жϣ�
				 */
				q.offer(e.getTailNode());
				visited.add(e.getTailNode().getSnumber());
			}
		}
		VexNode cur = null;
		while(!q.isEmpty()) {
			/* add by lsc 2018/9/16
			 * remove() �� poll() �������ǴӶ�����ɾ����һ��Ԫ�ء��������Ԫ��Ϊ�գ�
			 * ����remove() ����Ϊ�� Collection �ӿڵİ汾���ƻ��׳��쳣��
			 * �����µ� poll() �������ÿռ��ϵ���ʱֻ�Ƿ��� null������µķ������ʺ����׳����쳣�����������
			 * 
			 */
			cur = q.poll();
			visited.add(cur.getSnumber());
			//cur�Ǻ������õ����ж�occ�ĸ�����
			List<Node> list = cur.getTreenode().findChildrenOfType(ASTPrimaryExpression.class);
			if(list.size()!=0) {
				ASTPrimaryExpression node = (ASTPrimaryExpression)(list.get(0));
				if(node.isMethod()) { //�ڵ㱾���Ǻ�������
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
								//vNode�Ķ������node�㣬occ�Ķ������key
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
	public String getAnalysisDir() {
		return analysisDir;
	}
	public void setAnalysisDir(String analysisDir) {
		this.analysisDir = analysisDir;
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
	
	/*
	 
	 
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
	
	 
	 private void analysisFuncCall(NameOccurrence occ,String fileName, String funcName) {
		Hashtable<String, CVexNode> nodes = cgMap.get(fileName).nodes;
		CVexNode funcNode = null;
		for (String key : nodes.keySet()) {
			if(key.split("_")[0].equals(funcName)) {
				funcNode = nodes.get(key);
				break;
			}
		}
		if(funcNode!=null) {
			Queue<CVexNode> q = new LinkedList<CVexNode>();
			HashSet<CVexNode> visited = new HashSet<CVexNode>();
			q.offer(funcNode);
			visited.add(funcNode);
			
			while(!q.isEmpty()) {
				CVexNode cur = q.poll();
				for(CEdge edge : cur.getInedges().values()) {
					CVexNode nextNode = edge.getTailNode();
					String callerName = nextNode.getName().split("_")[0];
					String calleeName = cur.getName().split("_")[0];
					List<VexNode> callNodeList = DepChainUtil.findCallVexodes(cfgmap.get(callerName), calleeName);
					Set<NameOccurrence> occs = new HashSet<>();
					
					for(VexNode n : callNodeList) {
						occs.addAll(DepChainUtil.findCondition(n));
						
					}
					System.out.println("����" + callerName + "������" + calleeName);
					System.out.println("������Ӱ��ı�����:" + occs);
					for(NameOccurrence o: occs) {
						generate(o);
					}
				}
			}
		}
	}
	 
	 
	 	private void listMethodSummary(String funcName) {
		Graph cfg = cfgmap.get(funcName);
		SimpleNode entrance = cfg.getEntryNode().getTreenode();
		if(entrance instanceof ASTFunctionDefinition) {
			Method m = ((ASTFunctionDefinition)entrance).getDecl().getMethod();
			System.out.println(m.getCallerInfo());
			System.out.println(m.getExternalEffects());
			System.out.println(m.getReturnList());
		}
		
	}
	 
	 
	 
	//������������Դ
	public void getSourceByNode() {
		process();
		NameOccurrence occ = locate(args[1],args[2],args[3],Integer.valueOf(args[4]));
		listSCVP(args[2]);
		listMethodSummary(args[2]);
		generate(occ);
		analysisFuncCall(occ, args[1], args[2]);//�����������ù�ϵ
	}
	
	
	public void sryGraduateTest() {
		process();
		int i = 1,j = 1;
		for(Graph cfg : cfgmap2.keySet()) {
			int nodes = cfg.nodes.size();
			NameOccurrence target = null;
			//System.out.println(cfg.occtable);
			
			for (NameOccurrence occ : cfg.occtable.values()) {
				i++;
				if (i%8== 1) {
					try {
						System.out.println("��"+(j++)+"��Ŀ���:");
						System.out.println("�ļ���:"+cfg.getEntryNode().getTreenode().getFileName());
						System.out.println("������:"+ occ);
	
						generate2(occ);
						
						System.out.println(condLineMap);
						System.out.println(map2);
						System.out.println("����·����");
						for(String p : pathSet) {
							System.out.println(p);
						}
					} catch (Exception e) {
						
					}
					condLineMap.clear();
					map2.clear();
				}
			}
		}
		
	}
	

	//�������������
	public List<ExternalInputSource> getExternalSourceList() {
		//process();//Ԥ����
		List<ExternalInputSource> list = new ArrayList<>();
		for(Graph g : cfgmap2.keySet()) {
			g.numberOrderVisit(new ExternalInputSourceVisitor(), list);
		}
		
		return list;
	}
	



*/
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
