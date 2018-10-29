package softtest.fsmanalysis.c;

import java.util.*;

import org.apache.log4j.Logger;

import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.*;
import softtest.config.c.Config;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.ValueSet;
import softtest.domain.c.symbolic.Expression;
import softtest.ast.c.*;
import softtest.fsm.c.*;
import softtest.symboltable.c.VariableNameDeclaration;

public class FSMControlFlowVisitor implements GraphVisitor {
	static Logger logger = Logger.getLogger(FSMControlFlowVisitor.class);

	public void calculateIN(VexNode n, Object data) {
		List<Edge> list = new ArrayList<Edge>();
		for (Edge e:n.getInedges().values()) {
			list.add(e);
		}
		Collections.sort(list);
		if (n.getSnumber() != 0) {
			n.getFSMMachineInstanceSet().clear();
		}

		// ����ǰ���ڵ��U(in)
		for(Edge edge:list){
			VexNode pre = edge.getTailNode();
			// �жϷ�֧�Ƿ�ì��
			if (edge.getContradict()) { 
				continue;
			}
			
			// ���ǰ����û�з��ʹ�������
			if (!pre.getVisited()) {
				continue;
			}

			if (edge.getName().startsWith("T")) {
				FSMMachineInstanceSet temp = new FSMMachineInstanceSet(pre.getFSMMachineInstanceSet());
				temp.calCondition(pre, true);
				n.mergeFSMMachineInstances(temp);
			} else if (edge.getName().startsWith("F")) {
				// ����һ��ѭ��:��ѭ���Ļر��Զ���״̬Ҳ�ϲ�������
				if (n.getName().startsWith("while_out") || n.getName().startsWith("for_out")) {
					FSMControlFlowData loopdata = (FSMControlFlowData) data;
					loopdata.reporterror = false;
					visit(pre, loopdata);
					loopdata.reporterror = true;
				}
				FSMMachineInstanceSet temp = new FSMMachineInstanceSet(pre.getFSMMachineInstanceSet());
				temp.calCondition(pre, false);
				n.mergeFSMMachineInstances(temp);
			}else if(n.getName().startsWith("label_head_default") || n.getName().startsWith("label_head_case")){
				if(pre.getName().startsWith("switch_head")){
					FSMMachineInstanceSet temp = new FSMMachineInstanceSet(pre.getFSMMachineInstanceSet());
					temp.calSwitch(n, pre);
					n.mergeFSMMachineInstances(temp);
				}
			}else {
				
				n.mergeFSMMachineInstances(pre.getFSMMachineInstanceSet());
			}
		}
	}

	public void calculateOUT(VexNode n, Object data) {
		
		//add by WangQian 2010-10-15  switch��case:/default: �����{}�����  ����xpathѰ·��������
		if(n.getName().indexOf("label")!= -1)
		{
			return;
		}
		
		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e.hasMoreElements();) {
			list.add(e.nextElement());
		}
		
		Collections.sort(list);
		VexNode pre = null;
		//added by liuyan 2015/11/4
		for(Edge e:list){
			String str = e.getTailNode().toString();
			StringBuffer stringBuffer = new StringBuffer();
			StringBuffer string = new StringBuffer();
			if(str.length() > 11 && str.charAt(2) == 'f'){
				for(int i = 2; i<11;++i){
					stringBuffer.append(str.charAt(i));
				}
				String string2[] = str.split("_");
				for(int j = 2;j<string2.length-1;++j){
					string.append(string2[j]);
					if(j != string2.length-2){
						string.append("_");
					}
				}
			}
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.FSMInstanceAnalysis){
					if(stringBuffer.toString().equals("func_head")){
						logger.info("��ʼ��������"+string.toString()+"()��������");
					}
				}
			}
		}
		
		
		SimpleNode treenode = n.getTreenode();
		if(!Config.SKIP_METHODANALYSIS){
			if(Config.FSMInstanceAnalysis){
				logger.info("������ǰ�Ľڵ�����Ϊ["+treenode.getCurrentVexNode().getName()+"]");
				
			}
		}
		
		if (treenode == null)
			return;
		if (!n.isBackNode()) {
			// ���¼���״̬����
			n.getFSMMachineInstanceSet().calDomainSet(n);
		}
		// ���㵱ǰ�ڵ��Out
		List<FSMMachineInstance> todelete = new ArrayList<FSMMachineInstance>();
		Set<FSMMachineInstance> fsmSet = n.getFSMMachineInstanceSet().getTable().keySet();
		
		//added by liuyan 2015/10/13
//		int tempCount = fsmSet.size();
//		logger.info("FSMControlFlowVisitor.java++" + "calculateOUT" + "fsmSet���ܴ�С��" + tempCount);
//		logger.info(fsmSet);
		
		// �������е�״̬��ʵ��
		int countNum = 1;
		if(!Config.SKIP_METHODANALYSIS){
			if(Config.FSMInstanceAnalysis){
				logger.info("�ڵ�["+treenode.getCurrentVexNode().getName()+"]������״̬��ʵ������Ϊ��"+fsmSet.size());
			}
		}
		
		for (FSMMachineInstance fsminstance : fsmSet) {
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.FSMInstanceAnalysis){
					logger.info("�ڵ�["+treenode.getCurrentVexNode().getName()+"]�ĵ�["+countNum+"]��״̬��״̬��ϢΪ��");
					++countNum;
					logger.info(fsminstance);
				}
			}
			
			// �����µ�״̬ʵ������
			// ״̬�仯ֻ����һ���ˣ����˴���p=f(p.i);�����
			boolean recursion = false;
			do {
				recursion = false;
				FSMStateInstanceSet newstates = new FSMStateInstanceSet();
				
				// ������ǰ״̬��ʵ��������״̬ʵ��
				Set<FSMStateInstance> stateSet = fsminstance.getStates().getTable().keySet();
				for (FSMStateInstance stateinstance : stateSet) {
					ValueSet oldValueSet=n.getValueSet();
					SymbolDomainSet oldSymbolDomainSet=n.getSymDomainset();
					n.setSymDomainset(stateinstance.getSymbolDomainSet());
					n.setValueSet(stateinstance.getValueSet());
					FSMState state = stateinstance.getState();
	
					if (state.getRelatedMethod() != null) {
						try {
							Object[] args = new Object[2];
							args[0] = n;
							args[1] = fsminstance;
							state.getRelatedMethod().invoke(null, args);
						} catch (Exception e) {
							logger.error("Can't invoke state method in FSM " + fsminstance.getFSMMachine().getName(), e);
						}
					}
					Hashtable<String, FSMTransition> trans = state.getOutTransitions();
				
					// ���㵱ǰ״̬ʵ�������п���״̬ת��
					boolean stateschanged = false;
 					for (Enumeration<FSMTransition> e = trans.elements(); e.hasMoreElements();) {
						FSMTransition transition = e.nextElement();
						//���condition�Ƿ���Ҫ״̬ת��
						if (transition.evaluate(fsminstance, stateinstance, n)) {
							// ��Ҫ״̬ת��
							if (transition.getToState().isError()) {
								FSMControlFlowData loopdata = (FSMControlFlowData) data;
								if (loopdata != null && loopdata.reporterror) {
									FSMAnalysisVisitor.dumpError(fsminstance, n, n.getTreenode());
									FSMAnalysisVisitor.errorReport(loopdata, fsminstance, n, false);
									fsminstance.setErrorReport(true);
								}		
								
							}
							//added by xwt	
							else if(transition.getToState().isFinal() ){
								if(Config.TRIAL_OUTPUT_ALL && !fsminstance.isErrorReport()){
									FSMControlFlowData loopdata = (FSMControlFlowData) data;
									FSMAnalysisVisitor.errorReport(loopdata, fsminstance, n, true);
								}
							}
							FSMStateInstance newstateinstance = new FSMStateInstance(transition.getToState());
							newstateinstance.setSymbolDomainSet(stateinstance.getSymbolDomainSet());
							newstateinstance.setValueSet(stateinstance.getValueSet());
							newstateinstance.setSiteNode(n);
							newstates.addStateInstance(newstateinstance);
							stateschanged = true;
							recursion = true;
							
							if(!Config.SKIP_METHODANALYSIS){
								if (Config.FSMInstanceAnalysis) 
								{
									if(Config.StateTransition){
										System.out.println("In Line " + n.getTreenode().getBeginLine() + " " + fsminstance.getResultString()+":"+transition.getName()
												+":"+fsminstance.getStates().toString()+" >> "+newstateinstance.getState().toString());
										System.out.println("VexNode : " + n.getName() + " " +state.getName()+"-->>"+transition.getToState());
										logger.info("�ڵ�["+n.getName()+"]��״̬��ת������Ϊ:");
										logger.info("��" + n.getTreenode().getBeginLine() + "�б���" + fsminstance.getResultString()+":"+transition.getName()
												+":"+fsminstance.getStates().toString()+" >> "+newstateinstance.getState().toString());
										logger.info("�ڵ�[" + n.getName() + "]��״̬ת����ת��״̬Ϊ��" +state.getName()+"-->>"+transition.getToState());
									}
							}
							
							}
						}
					}
					if (!stateschanged) {
						// ״̬û�б仯
						newstates.addStateInstance(stateinstance);
					}
//					if(n.isIP) {
//						System.out.println(n.getName()+" is IP.");
//						for(int i = 0;i<n.reportList.size();i++) {
//							Report report = n.reportList.get(i);
//							FSMMachineInstance fsminstance1 = n.fsmList.get(i);
//							SimpleNode node = n.getTreenode();
//							logger.info("IP Classification = "+report.getFsmName());
//							logger.info("Related variable = "+report.getRelatedVarName());
//							logger.info(getDefinitionNodes(fsminstance1, n, node));
//						}
//					}
					n.setValueSet(oldValueSet);
					n.setSymDomainset(oldSymbolDomainSet);

					if(fsminstance.isErrorReport())
						break;
				}
				fsminstance.setStates(newstates);
				
				if (newstates.isEmpty()) {
					todelete.add(fsminstance);
					break;
				}
				if ((fsminstance.getFSMMachine().getName().startsWith("MLF")&& !fsminstance.getFSMMachine().getName().equals("MLF_LOOP"))
						|| fsminstance.getFSMMachine().getName().equals("RL") 
                        || fsminstance.getFSMMachine().getName().equals("UVF_P")
						|| fsminstance.getFSMMachine().getName().startsWith("UFM")) {
					break;//zys:2010.8.5	Ϊʲô������	
					//xwt:2011.10.28   ��MLF��ģʽ�������״̬����ͬһ��������ͼ�ڵ��ϲ������ٽ�������ת���жϣ��������ظ�������Դ
				}
				//xwt:��������ifΪ��һ�������ģʽ����Ȼ�ܲ���������������ȥɶ���õĽ���취�ˣ��鷳����
				//ԭ���ǣ�MLF_LOOPԭ����MLF��ģ�ֱ��break���У�������Ϊ��startת����loop����for_head�����֧��һ���ڵ㣬����½ڵ������Ϸ����ڴ棬����Ͳ���break�ˣ����ԣ����⴦��
				if(fsminstance.getFSMMachine().getName().equals("MLF_LOOP")){
					boolean isLoop = true;
					Enumeration<FSMStateInstance> table = fsminstance.getStates().getTable().keys();				
					while (table.hasMoreElements()){
						FSMStateInstance stateInstance = table.nextElement();
						if(stateInstance.getState().getName().equals("Loop"))
							isLoop = false;						
					}
					if(isLoop)
						break;
				}
			} while(recursion);
		}

		// ɾ����Щ�յ�״̬��
		for (FSMMachineInstance it : todelete) {
			n.rmFSMMachineInstanceSet(it);
			if(Config.FSM_STATISTIC){
				String fsmname=it.getFSMMachine().getName();
				Integer count=FSMMachine.statistic.get(fsmname);
				if(count!=null){
					count--;
					try{
					if(count<0){
						throw new RuntimeException(fsmname+" = "+count+" error!");
					}else if(count==0){
						FSMMachine.statistic.remove(fsmname);
					}else{
						FSMMachine.statistic.put(fsmname, count);
					}
					}catch(RuntimeException e){
						e.printStackTrace();
						continue;
					}
				}
			}
		}

		//�жϵ�ǰ�ڵ��ǰ���ڵ�pre,������̽ڵ㶼�ѷ��ʹ�,��pre��״̬��ʵ���������
		if (Config.FSM_REMOVE_PER_NODE) {
			for(Edge edge : list){
				pre = edge.getTailNode();
//				logger.info("ǰ�����pre����"+pre);
				boolean allvisited = true;
				for (Edge tempedge : pre.getOutedges().values()) {
					if (!tempedge.getHeadNode().getVisited()) {
						allvisited = false;
						break;
					}
				}
				if (allvisited && pre.getVisited()) {
					//zys:	2011.9.21	��ѭ����ֱ��ǰ���ڵ�������⴦����ѭ���˳��ڵ���Ҫ�ٴη���ѭ��ͷ�ڵ㣬��ʱ�����ֱ��ǰ����״̬����գ����Ӱ��
					//�����ĵ������㣬����״̬��ʧ
					if(pre.getOutedges().size()==1){
						VexNode nextNode=pre.getOutedges().elements().nextElement().getHeadNode();
//						logger.info("���ýڵ������nexeNode����"+nextNode);
						String temp=nextNode.getName();
						if(temp.startsWith("while_head") || temp.startsWith("for_head") 
								|| temp.startsWith("do_while_out1") ){
							//�ҵ�ѭ���ڵ�ĳ��ڽڵ㣬��������ʱ�ʶ�ж��Ƿ�������ѭ��ǰ���ڵ��״̬��ʵ��
							Edge outEdge=null;
							for(Edge out : nextNode.getOutedges().values()){
								if(out.getName().startsWith("F_")){
									outEdge=out;
									break;
								}
							}
							VexNode outNode=outEdge.getHeadNode();
							if(!outNode.getVisited())
								continue;
						}
					}
					pre.clear();//modify by JJL, 2016-11-23, ȡ�����ٿ�����ͼ
				}else{
					//System.out.println("�Զ���ʵ������δ��գ�"+pre.getName());
				}
			}
		}

	}
	private static List<VexNode> getDefinitionNodes(FSMMachineInstance fsminstance, VexNode n, SimpleNode node) {
		List<VexNode> defList = new ArrayList<VexNode>();
		String ipImage = fsminstance.getVarImage();
		Hashtable<VariableNameDeclaration, Expression> valuetable = n.getValueSet().getTable();
		logger.info("Value set of the IP variable is:"+n.getValueSet());
		for(VariableNameDeclaration key:valuetable.keySet()) {
			if(key.getImage().equals(ipImage)) {
				String useDomain = valuetable.get(key).toString();//ʹ�õ�(IP��)�ķ�����
				//use BFS to search the first vexnode that doesn't contain "useDomain"
				Queue<VexNode> queue = new LinkedList<VexNode>();
				queue.offer(n);
				while(!queue.isEmpty()) {
					VexNode temp = queue.poll();
					boolean containsDomain = false;
					for(Expression exp : temp.getLastvalueset().getTable().values()) {
						if(exp.toString().equals(useDomain)) {
							containsDomain = true;
							break;
						}							
					}
					if(temp==n)
						containsDomain = true;
					if(!containsDomain) {
						defList.add(temp);
						break;
					}
					for(VexNode parent : getParentNodes(temp)) {
						queue.offer(parent);
					}
				}
			}
		}

		return defList;
	}
	private static VexNode getChild(VexNode temp, String domain) {
		Hashtable<String, Edge> outedges = temp.getOutedges();
		for(Edge edge:outedges.values()) {
			VexNode child = edge.getHeadNode();
			if(child.getValueSet().getTable().values().contains(domain))
				return child;
		}
		return null;
	}
	private static List<VexNode> getParentNodes(VexNode n) {
		List<VexNode> parentNodes = new ArrayList<VexNode>();
		Hashtable<String, Edge> inedges = n.getInedges();
		for(Edge edge:inedges.values()) {
			parentNodes.add(edge.getTailNode());
		}
		return parentNodes;
	}
	/** �Խڵ���з��� */
	public void visit(VexNode n, Object data) {
		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e
				.hasMoreElements();) {
			list.add(e.nextElement());
		}
		//zys: 2011.9.21	ѭ���˳��ڵ�ķ��ʱ�ʶ����ڵ����״̬��ʱ��Ҫ�����ж�
		if(!(n.getName().startsWith("while_out") || n.getName().startsWith("for_out")
				|| n.getName().startsWith("do_while_out1")))
			n.setVisited(true);

		if (list.isEmpty()) {
			return;
		}
		calculateIN(n, data);
		
		//zys: 2011.9.21	ѭ���˳��ڵ�ķ��ʱ�ʶ����ڵ����״̬��ʱ��Ҫ�����ж�
		if(n.getName().startsWith("while_out") || n.getName().startsWith("for_out")
				|| n.getName().startsWith("do_while_out1"))
			n.setVisited(true);
		
		for(FSMMachineInstance fsmin:n.getFSMMachineInstanceSet().getTable().values())
		{
			if (fsmin.getRelatedObject() != null) {
				fsmin.getRelatedObject().calculateIN(fsmin, n, data);
				fsmin.getRelatedObject().calculateOUT(fsmin, n, data);
			}
		}

		if(!n.getContradict()){
	//		System.out.println(n.getName());
			calculateOUT(n, data);
		}
		
	}

	/** �Ա߽��з��� */
	public void visit(Edge e, Object data) {

	}

	/** ��ͼ���з��� */
	public void visit(Graph g, Object data) {

	}
}
