package softtest.dscvp.c;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import softtest.ast.c.ASTAdditiveExpression;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTExclusiveORExpression;
import softtest.ast.c.ASTFieldId;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTInclusiveORExpression;
import softtest.ast.c.ASTMultiplicativeExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.VarDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.dts.c.DTSC;
import softtest.fsm.c.FSMMachineInstance;
import softtest.fsmanalysis.c.CAnalysis;
import softtest.fsmanalysis.c.FSMControlFlowData;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.Method;
import softtest.interpro.c.MethodNode;
import softtest.rules.c.AliasSet;
import softtest.scvp.c.Position;
import softtest.scvp.c.SCVP;
import softtest.scvp.c.SCVPString;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.NameOccurrence.DefinitionType;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;

public class GenerateDSCVP {
	
	//������Щ��Ϣ��һ�������ã��ȷ��ţ���󶼿�ɾ������Ϊ��������
	public static FSMControlFlowData loopdata;
	public static FSMMachineInstance fsminstance;
	public static VexNode n = null;
	public static SimpleNode node;
	public static boolean isAllReport;
	
	//IP��Ķ�λ��Ϣ�������ټ�
	public static SimpleNode IPSimpleNode;
	public static VexNode IPVexNode;
	public static FSMMachineInstance IPFsminstance;
	
	//���캯��
	public GenerateDSCVP(FSMControlFlowData loopdata, FSMMachineInstance fsminstance, VexNode n, SimpleNode node,boolean isAllReport){
		this.fsminstance = fsminstance;
		this.loopdata = loopdata;
		this.n = n;
		this.node = node;
		this.isAllReport = isAllReport;
	}
	
	//add by ytang 20160625, �д��Ľ�
	public static void getIPLocation(FSMControlFlowData loopdata, FSMMachineInstance fsminstance, VexNode n, SimpleNode node,boolean isAllReport){
		try {
//			 writeTxtFile(fsminstance.getResultString(), DTSC.res_file);
//			 writeTxtFile("\n", DTSC.res_file);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (node != null){
			IPSimpleNode = node;
		}else{
			IPSimpleNode = fsminstance.getRelatedASTNode();	
		}
		if (n != null){
			IPVexNode = n;
		}else{
			IPVexNode = fsminstance.getReleatedVexNode();
		}
		IPFsminstance = fsminstance;
		if (IPVexNode == null)
			IPVexNode = IPSimpleNode.getCurrentVexNode();
		return;
	}
	
	//���SCVP���Ƕ����ģ�����IP��ͨ��ֻ��ʹ�õ㣬����������ݽṹ�����ʺ�IP�㣬�ڴ˽����¸Ľ����������Ϊһ��LIST
	public static SCVPString generateIPsSCVP(FSMMachineInstance IPFsminstance, SimpleNode IPSimpleNode, VexNode IPVexNode){
		if (IPVexNode == null)
			return null;

		String relateVariable = null;
		if (IPFsminstance.getRelatedVariable() != null){
			relateVariable = IPFsminstance.getRelatedVariable().getImage();
			VexNode nnn =  IPFsminstance.getRelatedVariable().getNode().getCurrentVexNode();
		}

		
		SCVPString root = new SCVPString();
	    if (IPFsminstance.getFSMMachine().getName().contains("RL") 
	    	&& (IPVexNode.getName().contains("return") || IPVexNode.getName().startsWith("func_out"))){//û�п���func_out_f_16
	    	if (relateVariable == null)
	    		return new SCVPString();
	    	//BFS�����
	    	Queue<VexNode> q = new LinkedList<VexNode>();
	    	for (Edge e : IPVexNode.getInedges().values()){
				q.offer(e.getTailNode());
			}
	    	boolean visit = false;
	    	while (!q.isEmpty()) {
	    		VexNode v = q.poll();
	    		for (NameOccurrence noc: v.getOccurrences()){
	    			//if (noc.getImage() != null && noc.getImage().equals(relateVariable) && noc.getOccurrenceType() == OccurrenceType.DEF){
					if (noc.getImage() != null && noc.getImage().equals(relateVariable)){//jjl,�ҵ����������Ľڵ�
						IPVexNode = v;
						visit = true;
						break;
					}
				}
	    		if (visit)
	    			break;
	    		for (Edge e : v.getInedges().values()){
					q.offer(e.getTailNode());
				}	    		
	    	}

	    }
		
        if (IPFsminstance.getFSMMachine().getName().contains("IAO")){
        	IPVexNode = IPSimpleNode.getCurrentVexNode();
        }
		List<NameOccurrence> occs = IPVexNode.getOccurrences();
		for(NameOccurrence occ : occs) {
		//if(occ.getOccurrenceType()==OccurrenceType.DEF) {
			//����ж�������һ��׼ȷ
			if(!IPFsminstance.getFSMMachine().getName().equals("NPD_EXP")){
				if (IPFsminstance.getRelatedVariable() != null){
					if (!IPFsminstance.getRelatedVariable().getImage().equals(occ.getImage())){
						continue;
					}
				}
			}
			
			if (IPFsminstance.getRelatedVariable() != null){
				if (IPFsminstance.getRelatedVariable().getImage().indexOf(occ.getImage()) == -1){
					continue;
				}
			}		
			
			try {
				//S
				SimpleNode astnode = (SimpleNode) occ.getLocation().getFirstParentOfType(ASTStatement.class);
				if (astnode == null) {
					break;
				}//barcode nullPointer-jjl
				List<Node> nodes = astnode.findChildOfTypes(new Class[]{//uucp
						ASTAdditiveExpression.class,//+ -
						ASTMultiplicativeExpression.class,//* /
						ASTInclusiveORExpression.class,//&|
						ASTExclusiveORExpression.class//^
						});//ö�������в�������ast�ڵ㣬�д����
				
				String structure="";
				if(occ.definitionType==DefinitionType.CONDITION) {
					VarDomainSet set = IPVexNode.getVarDomainSet();
					structure+="Condition:"+set.toString();
				}
				else if(occ.definitionType==DefinitionType.LOOP) {
					VarDomainSet set = IPVexNode.getVarDomainSet();
					structure+="Loop:"+set.toString();
				}
				else if(occ.definitionType==DefinitionType.LIB) {
					System.out.println(occ.getLocation());
					structure+="Lib Function:";
				}
				
				for(Node node:nodes) {
					structure+="Operator:"+((SimpleNode)node).getOperators().replace(" ", "");
				}
				
				root.setStructure(structure);
				//C
				List<Node> constants=astnode.findChildrenOfType(ASTConstant.class);
				List<String> cs = new ArrayList<String>();
				for (Node c: constants)
					cs.add(String.valueOf(((ASTConstant)c).getValue()));
				root.setConstants(cs);
				//V
				List<Node> temp=astnode.findChildOfTypes(new Class[]{ASTPrimaryExpression.class,ASTFieldId.class});
				//List<SimpleNode> variables = new LinkedList<SimpleNode>();
				HashSet<NameOccurrence> occs2 = new HashSet<NameOccurrence>();
				
				HashSet<NameOccurrence> occs2root = new HashSet<NameOccurrence>();
				occs2root.add(occ);
				
				for(Node node:temp) {
					if(((SimpleNode)node).getImage().equals(occ.getImage())==false) {
						if(((SimpleNode)node).isLeaf())  {
							if(node instanceof ASTPrimaryExpression) {
								if(((ASTPrimaryExpression)node).isMethod()==false) {
									//variables.add((ASTPrimaryExpression)node);
									for(NameOccurrence occ2:IPVexNode.getOccurrences()) {
										//�ж�Ϊuse������
										//if(occ2.getOccurrenceType()==OccurrenceType.USE) {
										if (IPFsminstance.getFSMMachine().getName().equals("NPD_EXP")){
											if (relateVariable.contains("->") ){
												if (relateVariable.equals(occ2.getImage()))
												   occs2.add(occ2);
											}else{
												if (relateVariable.contains(occ2.getImage())){
													occs2.add(occ2);
												}
											}
										}else if (IPFsminstance.getFSMMachine().getName().equals("IAO")){
											occs2.add(occ2);
										}else{
											occs2.add(occ2);
										}
										//}
									}
								}
								else {
									structure+="Function:"+((ASTPrimaryExpression)node).getImage();
									root.setStructure(structure);
								}
							}
							else if(node instanceof ASTFieldId) {
								//variables.add((SimpleNode) node);
								for(NameOccurrence occ2:IPVexNode.getOccurrences()) {
									if(occ2.getOccurrenceType()==OccurrenceType.USE) {
										occs2.add(occ2);
									}
								}
							}
						}
					}
				}	
				//scvp.setVariables(variables);
				if (IPFsminstance.getFSMMachine().getName().equals("NPD_EXP") || IPFsminstance.getFSMMachine().getName().equals("IAO") || IPFsminstance.getFSMMachine().getName().equals("OOB")){
					occs2.add(occ);	
					List<String> occs2s = new ArrayList<String>();
					Iterator iter = occs2.iterator();
					while (iter.hasNext()){
						NameOccurrence os = (NameOccurrence)iter.next();
						occs2s.add(os.toString());
					}
					root.setOccs(occs2s);
					
				}else{
					List<String> occ2s = new ArrayList<String>();
					Iterator iter = occs2root.iterator();
					while (iter.hasNext()){
						NameOccurrence os = (NameOccurrence)iter.next();
						occ2s.add(os.toString());
					}
					root.setOccs(occ2s);
				}
				//P
				Position p=new Position(IPVexNode,occ);
				root.setPosition(p);
				//dump
				//root.printSCVP();		
				//}
				break;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	    return root;
	}
	
	public static String start(){
		//�½�Element
		
		//��������DSCVP
		StringBuilder sb = new StringBuilder();
		DSCVP droot;
		try {
			DSCVPElement eroot = new DSCVPElement(0,0);
			//CAnalysis.dscvpElementList.add(eroot);
			//�õ�IP��ĳ����﷨����������ͼ�ڵ�
			//getIPLocation(loopdata, fsminstance, n, node, isAllReport);
			//����IP���SCVP��Ϣ����ΪDSCVP�ĵ�һ��ṹ,��occs�Ǳ�������
			SCVPString root = generateIPsSCVP(IPFsminstance, IPSimpleNode, IPVexNode);
			//DTSC.dscvpElementList.put(root, eroot);//
			//�ݹ�����DSCVPElement
			generateDSCVPElement(IPFsminstance.getFSMMachine().getName(),IPVexNode, root, eroot, 1);
			droot = new DSCVP();
			getDSCVP(droot, eroot);
			
			while (droot != null){
				try {
					//writeTxtFile(droot.dscvp.toString(), DTSC.res_file);
					sb.append(droot.dscvp.toString());
					//writeTxtFile("\n", DTSC.res_file);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				droot = droot.nextLayer;
			}
			DTSC.DipList.put(sb.toString(), eroot);//JJL, ����ȫ��
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
		return sb.toString();
	}
	
	//���ɵ�ǰIP�ĵ�һ��DSCVPElement
	public static void generateDSCVPElement(String Type, VexNode n, SCVPString root, DSCVPElement eroot, int count){
		if (count >= 20)
			return;
		if (root == null)
			return;
		List<String> occsList = root.getOccs();
		if (occsList == null || occsList.size() == 0)
			return;
		
		if (root != null && root.getVar()!=null && root.getVar().equals("str")){
			System.out.println("NPD");
		}
		
		//VexNode currentNode = rootocc.getLocation().getCurrentVexNode();
		//��ǰ��������ڽڵ�
		if (n == null)
			return;
			
		VexNode entryNode = null;
		Method method = null;
		if (n.getGraph().getVexNum() != 0){
			entryNode = n.getGraph().getEntryNode();
			method = null;
			if(entryNode.getName().startsWith("func_head")) { //������ڽڵ㣬��¼�����д����ָ�����
				ASTFunctionDefinition methodNode = (ASTFunctionDefinition)entryNode.getTreenode();
				//�ҵ�����ժҪ��Ϣ
				method = methodNode.getDecl().getMethod();
			}
		}
		
		//�õ���ǰ���������б���
		eroot.setVariableCnt(occsList.size());
		SCVPString child = null;
		Graph g = n.getGraph();
		Hashtable<String, NameOccurrence> occtable = g.getOcctable();
		for (String occstring : occsList){
			//if (occ.getImage().equals(root.getOcc().getImage()))
			//	continue;
			if (!occtable.containsKey(occstring.toString()))
				continue;
			NameOccurrence occ = occtable.get(occstring.toString());
			
			//����RLģʽ�����Ķ�ֵ
			if (occ.getOccurrenceType() == NameOccurrence.OccurrenceType.DEF && Type.startsWith("RL") && eroot.getLayer() == 0){
				DSCVPElement echild = new DSCVPElement(eroot.getLayer() + 1, 1);
				child = occ.getSCVP();
				echild.setSCVP(occ.getSCVP());
				eroot.addChild(occ.toString(), echild);
				generateDSCVPElement(Type, n, child, echild, ++count);
			}
			
			//�����������Ϊȫ�ֱ���
			if (occ.getDeclaration().getScope() instanceof SourceFileScope){
				List<NameOccurrence> useDefs = occ.getUse_def();
				//���������ں����ڲ�
				if (useDefs != null){
					for (NameOccurrence useDef : useDefs){
						//ĿǰĬ��һ��NameOccurrenceֻ��һ��SCVP
						child = useDef.getSCVP();
						DSCVPElement echild = new DSCVPElement(eroot.getLayer() + 1, useDefs.size());
						echild.setSCVP(child);
						eroot.addChild(occ.toString(), echild);
						//�ݹ�
						generateDSCVPElement(Type, n, child, echild, ++count);
					}
					return;
				}
				
				//����㲻�ں����ڲ�
				HashSet<SCVPString> children = new HashSet<SCVPString>();
				getGlobalVariableSCVP(occ, children, method, 1);
				for (SCVPString chi : children){
					DSCVPElement echild = new DSCVPElement(eroot.getLayer() + 1, children.size());
					echild.setSCVP(chi);
					eroot.addChild(occ.toString(), echild);
					//�ݹ�
					//generateDSCVPElement(chi, echild, count++);
				}
			//�ֲ���������������
			}else{
				//�ҵ����������ж����
				List<NameOccurrence> useDefs = null;

				if (occ.getOccurrenceType() == NameOccurrence.OccurrenceType.DEF){
					useDefs = occ.getUndef_def();
				}else{
					useDefs = occ.getUse_def();
				}			
				
				for (NameOccurrence useDef : useDefs){
					System.out.println(occ.toString());
					System.out.println(useDef.toString());
					if(occ.toString().equals(useDef.toString()))
						continue;
					//���������Ǻ�������ڵ�, ����������
					if (useDef.getOccurrenceType() == NameOccurrence.OccurrenceType.ENTRANCE){
						if (useDef == null || useDef.getSCVP() == null)
							continue ;

						DSCVPElement echild = new DSCVPElement(eroot.getLayer() + 1, 1);

						echild.setSCVP(useDef.getSCVP());
						String structure = useDef.getSCVP().getStructure();
						
						if(structure!=null && !structure.endsWith("#") && !structure.endsWith("$")) {
							//child.setStructure(structure+"#IP��="+n.toString()+",�����="+useDef.getLocation().getCurrentVexNode().toString()+"#");
							//VexNode ip=n;
							VexNode use=occ.getLocation().getCurrentVexNode();
							VexNode def=useDef.getLocation().getCurrentVexNode();
							structure+=bfs(def,use,occ);
							
						}
						echild.getSCVP().setStructure(structure);
						eroot.addChild(occ.toString(), echild);
					}else{
						//ĿǰĬ��һ��NameOccurrenceֻ��һ��SCVP
						child = useDef.getSCVP();
						if (child == null)
							return;
						//��������ֵ
						try{
							if (child.getStructure().contains("Function") && !child.getStructure().contains("Function:fopen") && !child.getStructure().contains("Function:malloc") && !child.getStructure().contains("Function:calloc") && !child.getStructure().contains("Function:free")){
								ASTStatement stat = (ASTStatement)useDef.getLocation().getFirstParentOfType(ASTStatement.class);
								List pris = stat.findChildrenOfType(ASTPrimaryExpression.class);
								Iterator iter = pris.iterator();
								while(iter.hasNext()){
									//������ ֻ֧��һ���������Ľ�ʱҪ���Ƕ���������ߺ������ض��ֵ
									ASTPrimaryExpression node = (ASTPrimaryExpression)iter.next();
									if (node.isMethod()){
										List<SCVPString> resultList = new ArrayList<SCVPString>();
									    getReturnFunctionSCVP(resultList, node.getMethodDecl().getMethod(), node);//�˴����ܱ��գ�sry debugged at 12.19
										Iterator<SCVPString> resultiter = resultList.iterator();
										while (resultiter.hasNext()){
											child = resultiter.next();
											DSCVPElement echild = new DSCVPElement(eroot.getLayer() + 1, useDefs.size());
											String structure = child.getStructure();
											
											if(!structure.endsWith("#") && !structure.endsWith("$")) {
												//child.setStructure(structure+"#IP��="+n.toString()+",�����="+useDef.getLocation().getCurrentVexNode().toString()+"#");
												VexNode use=occ.getLocation().getCurrentVexNode();
												VexNode def=useDef.getLocation().getCurrentVexNode();
												structure+=bfs(def,use,occ);
												child.setStructure(structure);
											}
											echild.setSCVP(child);
											eroot.addChild(occ.toString(), echild);
										    //���ٵݹ�	
										    //generateDSCVPElement(child, echild, count++);
										}
									}	 
								}
							}else{	
								DSCVPElement echild = new DSCVPElement(eroot.getLayer() + 1, useDefs.size());
								String structure = child.getStructure();
								
								if(!structure.endsWith("#") && !structure.endsWith("$")) {
									//child.setStructure(structure+"#IP��="+n.toString()+",�����="+useDef.getLocation().getCurrentVexNode().toString()+"#");
									VexNode use=occ.getLocation().getCurrentVexNode();
									VexNode def=useDef.getLocation().getCurrentVexNode();
									structure+=bfs(def,use,occ);
									child.setStructure(structure);
								}
								echild.setSCVP(child);
								eroot.addChild(occ.toString(), echild);
							    //�ݹ�	
							    generateDSCVPElement(Type, n, child, echild, ++count);
							}
						}catch(Exception e){
							e.printStackTrace();
						}
						    
					}

				}
				
			}
		}
	}
	


	private static String bfs(VexNode def, VexNode ip,NameOccurrence ipRelatedOcc) {
		Queue<VexNode> q = new LinkedList<VexNode>();
		boolean[] vis = new boolean[def.getGraph().getVexNum()+5];
		Stack<VexNode> cond = new Stack<VexNode>();//����ջ
		q.offer(def);
		String ret="";
		while(!q.isEmpty()) {
			VexNode cur = q.poll();
			vis[cur.getSnumber()]=true;
			if(cur==ip)
				break;
			if(cur.toString().contains("if_head")) //����ͷ
				cond.push(cur);
			else if(cur.toString().contains("if_out") && !cond.isEmpty()) //����β
				cond.pop();
			for(Edge e:cur.getOutedges().values()) {
				if(!vis[e.getHeadNode().getSnumber()] && e.getHeadNode().getSnumber()<=ip.getSnumber())
					q.offer(e.getHeadNode());
			}
		}
		
		System.out.println(cond);
		while(!cond.isEmpty()) {
			VexNode cur=cond.pop();
			for(NameOccurrence occ:cur.getOccurrences()) {
				if(occ.getImage().equals(ipRelatedOcc.getImage())) {
					Domain domain = cur.getDomain((VariableNameDeclaration) occ.getDeclaration());
					System.out.println(domain);
					if(ret.equals(""))
						ret="#"+domain.toString()+"#";
					else
						ret="#"+domain.toString()+"#"+ret;
				}
				else {
					if(ret.equals(""))
						ret="$condition$";
					else if(ret.endsWith("#"))
						ret+="$condition$";
				}
			}
		}
		return ret;
	}

	public static void getReturnFunctionSCVP(List<SCVPString> resultList, Method method, ASTPrimaryExpression node){
		List<SCVPString> returnlist = method.getReturnList();
		Iterator returniter = returnlist.iterator();
		while (returniter.hasNext()){
			SCVPString current = (SCVPString)returniter.next();
			if (current.getStructure().contains("Function:") && !current.getStructure().contains("Function:malloc")&& !current.getStructure().contains("Function:calloc")){
				String structure = current.getStructure();
				String functionName = null;
			    Pattern pp=Pattern.compile("Function:(\\w+)");
			    Matcher mm=pp.matcher(structure);
			    while(mm.find()){
			       functionName = mm.group(1);
			    }
			    NameDeclaration nd = Search.searchInMethodUpward(functionName, node.getScope().getEnclosingSourceFileScope());
			    Method m = ((MethodNameDeclaration)nd).getMethod();
			    List<SCVPString> reReturnList = m.getReturnList();
			    getReturnFunctionSCVP(resultList, m, node);
			}else{
				resultList.add(current);
			}
		}	
	}
	
	//�õ�ȫ�ֱ����Ķ����, occΪȫ�ֱ������֣� scvpsΪ�ñ��������ж����ṹ,methodΪ��ǰ�����Ľṹ
	public static void getGlobalVariableSCVP(NameOccurrence occ, HashSet<SCVPString> scvps, Method method, int layer){
		if (layer == 3)
			return;
		//������ں����ⲿ
		if (method == null || occ == null)
			return;
		//���ҵ�ǰ����������ͼǰ���
		boolean ff = true;
		if (layer == 1){
			List<VexNode> vexList = new ArrayList<VexNode>();
			//added by cmershen,���ӽڵ���ʱ�ǣ�����������
			HashSet<Integer> visited = new HashSet<Integer>(); 
			VexNode current = occ.getLocation().getCurrentVexNode();
			for (Edge e : current.getInedges().values()){
				vexList.add(e.getTailNode());
				visited.add(e.getTailNode().getSnumber());
			}
			while (vexList.size() > 0){
				int size = vexList.size();
				for (int i = 0; i < size; i++){
					VexNode v = vexList.get(0);
					List<Node> nodes = v.getTreenode().findChildrenOfType(ASTPrimaryExpression.class);
					if(nodes.size()!=0) {
						ASTPrimaryExpression node = (ASTPrimaryExpression)(nodes.get(0));
						if(node.isMethod()) { //�ڵ㱾���Ǻ�������
							HashMap<String, List<SCVPString>> extEffects = node.getMethodDecl().getMethod().getExternalEffects();
							for (Map.Entry<String, List<SCVPString>> effect : extEffects.entrySet()){
								//�г��֣�����ຯ�������Լ�ÿ�������ඨ��㶼���Կ����Ƕ�Դ������ͬ����
								List<SCVPString> scvpList = effect.getValue();
								for (SCVPString li : scvpList){
									if (li.getVar().equals(occ.getImage())){
										scvps.add(li);
										ff = false;
									}
								}
							}
						}
					}
					for (Edge e : v.getInedges().values()){
						if(!visited.contains(e.getTailNode().getSnumber())) {
							vexList.add(e.getTailNode());
							visited.add(e.getTailNode().getSnumber());
						}
					}
					vexList.remove(0);
				}
			}
		}
		
		
		//�ҵ��ú��������е�����
		if (ff){
			HashMap<Position, ArrayList<SCVPString>> callers = method.getCallerInfo();
			for (Map.Entry<Position, ArrayList<SCVPString>> call : callers.entrySet()){
				Position pos = call.getKey();
				ArrayList<SCVPString> scvplist = call.getValue();
				int flag = 1;
				for (SCVPString scvp:scvplist){
					if (scvp.getVar().equals(occ.getImage())){
						scvps.add(scvp);
						flag = 0;
					}
				}
				//����������û���ҵ����������ݹ飬ֱ���ҵ�Ϊֹ
				if (flag == 1){
				   getGlobalVariableSCVP(occ, scvps, pos.getMethod(),++layer);
				}
			}
		}
	}
	
	/** add by JJL, 20161204*/
	public static void getMethodVariableSCVP(DSCVPElement eroot, SCVPString scvp, HashSet<DSCVPElement> addset,int times) {
		if(times>3)//pruning in spite of dead loop,cmershen,2016.12.29
			return;
		System.out.println("GetFunctionSCVP times="+times);
		if (scvp == null)
			return;
		String structure = scvp.getStructure();
		if (!(structure.contains("entrance"))) {
			return;
		}
		Position pos = scvp.getPosition();
		String methodname = pos.getMethod().toString();
		int index = 0;
	    Pattern p=Pattern.compile("index:(\\w+).*");//
	    Matcher m=p.matcher(structure);
	    while(m.find()){
	       String sindex = m.group(1);
	       index = Integer.valueOf(sindex);
	    }
		//����ǰ��ժҪ
	    ConcurrentHashMap <Method, MethodNode> interMethodsTable = InterCallGraph.getInstance().getCallRelationTable();
	    Iterator iter = interMethodsTable.entrySet().iterator();
	    while (iter.hasNext()){
	    	Map.Entry<Method, MethodNode> entry = (Map.Entry<Method, MethodNode>)iter.next();
	    	Method method = entry.getKey();
	    	if (method.toString().equals(methodname)){//���Һ�����
	    		HashMap<Position, ArrayList<SCVPString>> callers = method.getCallerInfo();
	    		for (Map.Entry<Position, ArrayList<SCVPString>> call : callers.entrySet()){
	    			SCVPString current = null;
	    			ArrayList<SCVPString> list = call.getValue();
    			    if (index != call.getKey().getIndex())
    			    	continue;
	    			for (int i = 0; i < list.size(); i++){
	    				current = list.get(i);
	    				if (current.getStructure().contains("entrance")){
	    					getMethodVariableSCVP(eroot, current, addset,times+1);
	    				}else{
	    					DSCVPElement d = new DSCVPElement(eroot.getLayer(), list.size());
	    					d.setSCVP(current);
	    					addset.add(d);
	    				}
	    			}

	    		}
	    	}
	    }
	}
	
	public static void getMethodVariableSCVP(String scvpstring, HashSet<SCVPString> scvps){
		//������ں����ⲿ
		if (!scvpstring.contains("entrance"))
			return;
		int index = 0;	
		String methodname = null;
	    Pattern p=Pattern.compile("index:(\\w+).*method=([\\w\\_]+)");
	    Matcher m=p.matcher(scvpstring);
	    while(m.find()){
	       String sindex = m.group(1);
	       methodname = m.group(2);
	       index = Integer.valueOf(sindex);
	    }
	    ConcurrentHashMap <Method, MethodNode> interMethodsTable = InterCallGraph.getInstance().getCallRelationTable();
	    Iterator iter = interMethodsTable.entrySet().iterator();
	    while (iter.hasNext()){
	    	Map.Entry<Method, MethodNode> entry = (Map.Entry<Method, MethodNode>)iter.next();
	    	Method method = entry.getKey();
	    	if (method.toString().equals(methodname)){
	    		HashMap<Position, ArrayList<SCVPString>> callers = method.getCallerInfo();
	    		for (Map.Entry<Position, ArrayList<SCVPString>> call : callers.entrySet()){
	    			SCVPString current = null;
	    			ArrayList<SCVPString> list = call.getValue();
    			    if (index != call.getKey().getIndex())
    			    	continue;
	    			for (int i = 0; i < list.size(); i++){
	    				current = list.get(i);
	    				if (current.getStructure().contains("entrance")){
	    					getMethodVariableSCVP(current.toString(), scvps);
	    				}else{
	    					scvps.add(current);
	    				}
	    			}

	    		}
	    	}
	    }
	}
	
	public static void getDSCVP(DSCVP droot, DSCVPElement eroot){
		
        if (eroot == null) {  
            return;  
        }  
        LinkedList<DSCVPElement> queue = new LinkedList<DSCVPElement>();  
        eroot.setStr();
        if (eroot.getStr() != null)
        	droot.appendDSCVPString(eroot.getStr());
        queue.addLast(eroot);
        DSCVP pre = droot;
        int layer = 1;
        while (!queue.isEmpty()) {  
        	DSCVP tmp = new DSCVP();
        	pre.nextLayer = tmp;
        	int size = queue.size();
        	if (size>0)
        		tmp.appendDSCVPString("layer: " + layer+" ");
        	layer++;
        	for (int i = 0; i < size; i++){
        		DSCVPElement curNode = queue.pollFirst();  
        		HashMap<String, HashSet<DSCVPElement>> children = curNode.child;
	        	if (children != null){
	        		int cnt = 0;
	        		for (Map.Entry<String, HashSet<DSCVPElement>> entry:children.entrySet()){
	        			HashSet<DSCVPElement> hset = entry.getValue();
	        			int count = 0;
	        			for (Iterator<DSCVPElement> iter=hset.iterator(); iter.hasNext();){
	        				DSCVPElement de = iter.next();
	        				de.setStr();
	        				System.out.println("deString : " + de.getStr());
	        				tmp.appendDSCVPString(de.getStr());
	        				if (count++ < hset.size() - 1)
	        					tmp.appendDSCVPString(" | ");
	        				queue.addLast(de);
	        			}
	        			if (cnt++ < children.size() - 1)
	        				tmp.appendDSCVPString(" & ");
	        		}
	        	}
        	}
        	pre = tmp;
        	tmp.appendDSCVPString("\n");
        }  
	}
	
    public static boolean writeTxtFile(String content,File fileName)throws Exception{ 
	 FileWriter writer=new FileWriter(fileName.getPath(), true);
	 System.out.println("name: " + fileName.getPath());
	 try{
		 //ʹ��������캯��ʱ���������kuka.txt�ļ���
		 //���Ȱ�����ļ���ɾ������Ȼ�󴴽��µ�kuka.txt
		 writer.write(content);
		 //writer.write("hello");
     } catch (IOException e){
           e.printStackTrace();
     }finally{
         writer.close();
     }
	return true;   
 }
	
}
