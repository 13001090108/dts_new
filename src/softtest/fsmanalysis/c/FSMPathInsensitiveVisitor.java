package softtest.fsmanalysis.c;

import java.util.*;

import org.apache.log4j.Logger;

import softtest.ast.c.SimpleNode;
import softtest.cfg.c.*;
import softtest.config.c.Config;
import softtest.fsm.c.*;

public class FSMPathInsensitiveVisitor implements GraphVisitor {
	static Logger logger = Logger.getLogger(FSMPathInsensitiveVisitor.class);

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
		VexNode pre = null;
		for(Edge edge:list){
			// �жϷ�֧�Ƿ�ì��
			if (edge.getContradict()) { 
				continue;
			}
			pre = edge.getTailNode();

			if (edge.getName().startsWith("F")) {
				// ����һ��ѭ��
				//zys:2010.4.22 �����ǰ��տ������ڵ�˳���������������ѭ���ڵ�ʱ��while_out�ȣ�ѭ��ͷ����Ѿ�������
				//�Ƿ��б�Ҫ�ٱ���һ�飿
				if (n.getName().startsWith("while_out") || n.getName().startsWith("for_out")) {
					FSMControlFlowData loopdata = (FSMControlFlowData) data;
					if (loopdata != null) {
						loopdata.reporterror = false;
					}
					visit(pre, loopdata);
					if (loopdata != null) {
						loopdata.reporterror = true;
					}
				}
				n.mergFSMMachineInstancesWithoutConditon(pre.getFSMMachineInstanceSet());
			}else {
				n.mergFSMMachineInstancesWithoutConditon(pre.getFSMMachineInstanceSet());
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
		
		
		VexNode pre = null;
		Iterator<Edge> iter = list.iterator();
		
		SimpleNode treenode = n.getTreenode();

		if(!Config.SKIP_METHODANALYSIS){
			if(Config.FSMInstanceAnalysis){
				logger.info("������ǰ�Ľڵ�����Ϊ["+treenode.getCurrentVexNode().getName()+"]");
			}
		}
		
		// ���㵱ǰ�ڵ��Out
		List<FSMMachineInstance> todelete = new ArrayList<FSMMachineInstance>();
		Set<FSMMachineInstance> fsmSet = n.getFSMMachineInstanceSet().getTable().keySet();
		
		//added by liuyan 2015/10/13
//		int temp = fsmSet.size();
//		logger.info("FSMPathInsensitiveVisitor.java**" + "caculateOUT" + "fsmSet���ܴ�С��" + temp + "\n");
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
						if (transition.evaluate(fsminstance, stateinstance, n)) {
							// ��Ҫ״̬ת��
							if (transition.getToState().isError()) {
								FSMControlFlowData loopdata = (FSMControlFlowData) data;
								if (loopdata != null && loopdata.reporterror) {
									FSMAnalysisVisitor.dumpError(fsminstance, n, n.getTreenode());
									FSMAnalysisVisitor.errorReport(loopdata, fsminstance, n,false);
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
							newstateinstance.setSiteNode(n);
							newstates.addStateInstanceWithoutConditon(newstateinstance);
							stateschanged = true;
							recursion = true;
							if(!Config.SKIP_METHODANALYSIS){
								if(Config.FSMInstanceAnalysis){
									if (Config.StateTransition) 
									{
										System.out.println("In Line " + n.getTreenode().getBeginLine() + " " + fsminstance.getResultString()+":"+transition.getName()
												+":"+fsminstance.getStates().toString()+" >> "+newstateinstance.getState().toString());
										System.out.println("VexNode : " + n.getName() + " " +state.getName()+"-->>"+transition.getToState());
										//added by liuyan 2015/10/31
										logger.info("�ڵ�["+n.getName()+"]��״̬��ת������Ϊ:");
										logger.info("��" + n.getTreenode().getBeginLine() + "�б���" + fsminstance.getResultString()+":"+transition.getName()
													+":"+fsminstance.getStates().toString()+" >> "+newstateinstance.getState().toString());
										logger.info("�ڵ�[" + n.getName() + "]��״̬ת����ת��״̬Ϊ��" +state.getName()+"-->>"+transition.getToState());
								}
							}
							
							}
//							if (Config.StateTransition || Config.TRACE) 
//							{
//								System.out.println("In Line " + n.getTreenode().getBeginLine() + " " + fsminstance.getResultString()+":"+transition.getName()
//										+":"+fsminstance.getStates().toString()+" >> "+newstateinstance.getState().toString());
//								System.out.println("VexNode : " + n.getName() + " " +state.getName()+"-->>"+transition.getToState());
//							
//								if(Config.StateTransition){
//									logger.info("��" + n.getTreenode().getBeginLine() + "�б���" + fsminstance.getResultString()+":"+transition.getName()
//											+":"+fsminstance.getStates().toString()+" >> "+newstateinstance.getState().toString());
//									logger.info("�ڵ� : " + n.getName() + " " +state.getName()+"-->>"+transition.getToState());
//								}
////							logger.info("����2");
//							
//							}
						}
					}
					if (!stateschanged) {
						// ״̬û�б仯
						newstates.addStateInstanceWithoutConditon(stateinstance);
					}
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
				}
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
		}

		if (Config.FSM_REMOVE_PER_NODE) {
			for(Edge edge : list){
				pre = edge.getTailNode();
				boolean allvisited = true;
				for (Edge tempedge : pre.getOutedges().values()) {
					if (!tempedge.getHeadNode().getVisited()) {
						allvisited = false;
						break;
					}
				}
				if (allvisited && pre.getVisited()) {
					//pre.getFSMMachineInstanceSet().clear();
					pre.clear();
				}
			}
		}
	}

	/** �Խڵ���з��� */
	public void visit(VexNode n, Object data) {
		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e
				.hasMoreElements();) {
			list.add(e.nextElement());
		}
		n.setVisited(true);

		if (list.isEmpty()) {
			return;
		}
		calculateIN(n, data);
		for(FSMMachineInstance fsmin:n.getFSMMachineInstanceSet().getTable().values())
		{
			if (fsmin.getRelatedObject() != null) {
				fsmin.getRelatedObject().calculateIN(fsmin, n, data);
				fsmin.getRelatedObject().calculateOUT(fsmin, n, data);
			}
		}

		if(!n.getContradict()){
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
