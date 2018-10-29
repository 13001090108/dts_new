package softtest.fsm.c;

import java.io.IOException;
import java.util.*;
import javax.tools.*;
import org.w3c.dom.*;

import softtest.config.c.Config;
import softtest.rules.c.BasicStateMachine;

/** ����ģʽ״̬�� */
public class FSMMachine extends FSMElement {
	/** �ڲ���� */
	private int statecount = 0;

	/** �ڲ���� */
	private int transitioncount = 0;

	/** ���� */
	private String name;
	
	/** ��� */
	private String type;

	/** ��Χ */
	private FSMScope scope = FSMScope.METHOD;
	
	/** �Ƿ�·�����еı�־ */
	private boolean ispathsensitive = false;

	/** �Ƿ������صı�־ */
	private boolean isvariablerelated = false;

	/** ״̬���� */
	private Hashtable<String, FSMState> states = new Hashtable<String, FSMState>();

	/** ת������ */
	private Hashtable<String, FSMTransition> transitions = new Hashtable<String, FSMTransition>();

	/** ״̬�������й��������������� */
	private Class relatedclass = null;

	/** ���״̬���Ĺ����� */
	public Class getRelatedClass() {
		return relatedclass;
	}

	/** ����״̬���Ĺ����� */
	public void setRelatedClass(Class relatedclass) {
		this.relatedclass = relatedclass;
	}

	/** ���״̬���� */
	public Hashtable<String, FSMState> getStates() {
		return states;
	}

	/** ����״̬���� */
	public void setStates(Hashtable<String, FSMState> states) {
		this.states = states;
	}

	/** ���ת������ */
	public Hashtable<String, FSMTransition> getTransitions() {
		return transitions;
	}

	/** ����ת������ */
	public void setTransitions(Hashtable<String, FSMTransition> transitions) {
		this.transitions = transitions;
	}

	/** ȱʡ�������캯�� */
	public FSMMachine() {
		name = "";
		fsm = this;
	}

	/** ָ�����ƹ���״̬�� */
	public FSMMachine(String name) {
		this.name = name;
	}

	/** ������� */
	public String getName() {
		return name;
	}

	/** �������� */
	public void setName(String name) {
		this.name = name;
	}

	/** ������� */
	public String getType() {
		return type;
	}

	/** �������� */
	public void setType(String type) {
		this.type = type;
	}
	
	public FSMScope getScope() {
		return scope;
	}
	
	public void setScope(FSMScope scope) {
		this.scope = scope;
	}
	 
	/** ���ñ�����ر�־ */
	public void setVariableRelated(boolean isvariablerelated) {
		this.isvariablerelated = isvariablerelated;
	}

	/** ��ñ�����ر�־ */
	public boolean isVariableRelated() {
		return isvariablerelated;
	}

	/** ����·�����б�־ */
	public void setPathSensitive(boolean ispathsensitive) {
		this.ispathsensitive = ispathsensitive;
	}

	/** ���·�����б�־ */
	public boolean isPathSensitive() {
		return ispathsensitive;
	}

	/** ״̬�������ߵ�accept���� */
	@Override
	public void accept(FSMVisitor visitor, Object data) {
		visitor.visit(this, data);
	}

	/** ����һ��״̬�������״̬�Ѿ����ڣ����׳��쳣 */
	public FSMState addState(FSMState state) {
		if (states.get(state.getName()) != null) {
			throw new RuntimeException("The state has already existed.");
		}
		states.put(state.getName(), state);
		state.setSnumber(statecount++);
		state.fsm = this;
		return state;
	}

	/** ����һ��ָ�����Ƶ�״̬�����յ����ƽ�Ϊname */
	public FSMState addState(String name) {
		FSMState state = new FSMState(name);
		return addState(state);
	}

	/** ����һ��û��ָ�����Ƶ�״̬�����յ����ƽ�Ϊstatecount */
	public FSMState addState() {
		String name = "" + statecount;
		return addState(name);
	}

	/** ����һ��ָ������������״̬��ת�������趨����Ϊname */
	public FSMTransition addTransition(FSMState fromstate, FSMState tostate,
			String name) {
		if (fromstate == null || tostate == null) {
			throw new RuntimeException(
					"An transition's fromstate or tostate cannot be null.");
		}
		if (transitions.get(name) != null) {
			throw new RuntimeException("The transition has already existed.");
		}
		if (states.get(fromstate.getName()) != fromstate
				|| states.get(tostate.getName()) != tostate) {
			throw new RuntimeException("There is a contradiction.");
		}

		if (tostate.getInTransitions().get(name) != null) {
			throw new RuntimeException("There is a contradiction.");
		}

		if (fromstate.getOutTransitions().get(name) != null) {
			throw new RuntimeException("There is a contradiction.");
		}

		FSMTransition e = new FSMTransition(name, fromstate, tostate);
		transitions.put(name, e);
		e.setSnumber(transitioncount++);
		e.fsm = this;

		return e;
	}

	/** ����һ��ָ������������״̬��ת�������趨���� */
	public FSMTransition addTransition(String from, String to, String name) {
		FSMState fromstate = states.get(from);
		FSMState tostate = states.get(to);
		return addTransition(fromstate, tostate, name);
	}

	/** ����һ��ָ������������״̬��ת��,����Ĭ��Ϊtransitioncount */
	public FSMTransition addTransition(String from, String to) {
		String name = "" + transitioncount;
		return addTransition(from, to, name);
	}

	/** ����һ��ָ������������״̬��ת��,����Ĭ��Ϊtransitioncount */
	public FSMTransition addTransition(FSMState fromstate, FSMState tostate) {
		String name = "" + transitioncount;
		return addTransition(fromstate, tostate, name);
	}

	/** ����һ��ָ��β��ͷ�ڵ�ıߣ����趨����Ϊname+edgecount */
	public FSMTransition addTransition(String name, FSMState fromstate,
			FSMState tostate) {
		name = name + transitioncount;
		return addTransition(fromstate, tostate, name);
	}

	/** ɾ��ָ����ת��������Ҳ�����ת�����߸�ת������״̬���е�ת�����׳��쳣 */
	public void removeTransition(FSMTransition e) {
		if (transitions.get(e.getName()) != e || e == null) {
			throw new RuntimeException("Cannot find the transition.");
		}
		if (e.getFromState() == null || e.getToState() == null) {
			throw new RuntimeException("There is a contradiction.");
		}
		if (e.getToState().getInTransitions().get(e.getName()) != e
				|| e.getFromState().getOutTransitions().get(e.getName()) != e) {
			throw new RuntimeException("There is a contradiction.");
		}

		e.getToState().getInTransitions().remove(e.getName());
		e.getFromState().getOutTransitions().remove(e.getName());
		transitions.remove(e.getName());

	}

	/** ɾ��ָ����ת�� */
	public void removeTransition(String name) {
		FSMTransition e = transitions.get(name);
		removeTransition(e);
	}

	/** ɾ��ָ��״̬�����н���ת�� */
	public void removeInTransitions(FSMState state) {
		LinkedList<FSMTransition> temp = new LinkedList<FSMTransition>();
		temp.clear();
		for (Enumeration<FSMTransition> e = state.getInTransitions().elements(); e
				.hasMoreElements();) {
			temp.add(e.nextElement());
		}
		ListIterator<FSMTransition> i = temp.listIterator();
		while (i.hasNext()) {
			FSMTransition tran = i.next();
			removeTransition(tran);
		}
	}

	/** ɾ��ָ��״̬�����г���ת�� */
	public void removeOutTransitions(FSMState state) {
		LinkedList<FSMTransition> temp = new LinkedList<FSMTransition>();
		temp.clear();
		for (Enumeration<FSMTransition> e = state.getOutTransitions()
				.elements(); e.hasMoreElements();) {
			temp.add(e.nextElement());
		}
		ListIterator<FSMTransition> i = temp.listIterator();
		while (i.hasNext()) {
			FSMTransition tran = i.next();
			removeTransition(tran);
		}
	}

	/** ɾ��ָ��״̬���������ת�� */
	public void removeState(FSMState state) {
		if (states.get(state.getName()) != state || state == null) {
			throw new RuntimeException("Cannot find the state.");
		}
		removeInTransitions(state);
		removeOutTransitions(state);
		states.remove(state.getName());
	}

	/** ɾ��ָ��״̬���������ת�� */
	public void removeState(String name) {
		FSMState state = states.get(name);
		removeState(state);
	}

	/** ���״̬���Ŀ�ʼ״̬ */
	public FSMState getStartState() {
		FSMState state = null;
		for (Enumeration<FSMState> e = states.elements(); e.hasMoreElements();) {
			FSMState temp = e.nextElement();
			if (temp.isStart()) {
				state = temp;
				break;
			}
		}
		return state;
	}

	/** zys:����ͳ��״̬�����������У�����״̬��ʵ���Ĵ������������*/
	public static HashMap<String,Integer> statistic=new HashMap<String,Integer>();
	
	/** ����һ��״̬��ʵ����������ʵ����״̬���ϼ���һ����ʼ״̬ʵ�� */
	public FSMMachineInstance creatInstance() {
		if(Config.FSM_STATISTIC){
			if(statistic.get(name)!=null){
				Integer count=statistic.get(name);
				count++;
				statistic.put(name, count);
			}else{
				statistic.put(name, 1);
			}
		}
		FSMMachineInstance instance = new FSMMachineInstance();
		instance.setFSMMachine(this);
		FSMState start = getStartState();
		if (start != null) {
			FSMStateInstance e = new FSMStateInstance(start);
			instance.addStateInstance(e);
		}
		return instance;
	}

	/** ����xml */
	@Override
	public void loadXML(Node n) {
		setName(n.getNodeName());
		if (n.getAttributes().getNamedItem("Scope") != null) {
			String scope = n.getAttributes().getNamedItem("Scope").getNodeValue();
			if (scope.equals("Method")) {
				setScope(FSMScope.METHOD);
			} else if (scope.equals("Class")) {
				setScope(FSMScope.CLASS);
			} else if (scope.equals("File")) {
				setScope(FSMScope.FILE);
			} else if (scope.equals("Project")) {
				setScope(FSMScope.PROJECT);
			}
		}
		if (n.getAttributes().getNamedItem("Type") != null) {
			setName(n.getAttributes().getNamedItem("Type").getNodeValue());
		}
		if (n.getAttributes().getNamedItem("isPathSensitive") != null
				&& n.getAttributes().getNamedItem("isPathSensitive")
						.getNodeValue().equals("true")) {
			setPathSensitive(true);
		} else {
			setPathSensitive(false);
		}
		Node nodeclass = n.getAttributes().getNamedItem("Class");
		Node nodeaction = n.getAttributes().getNamedItem("Action");
		if (n.getAttributes().getNamedItem("isVariableRelated") != null
				&& n.getAttributes().getNamedItem("isVariableRelated")
						.getNodeValue().equals("true")) {
			setVariableRelated(true);
			// ������·����ص�״̬�������ж�����ͨ���ö�������״̬��ʵ���Ĵ���
			if (nodeclass == null || nodeaction == null) {
				throw new RuntimeException(
						"VariableRelated fsm must has \"Class\" and \"Action\" attributes.");
			}
		} else {
			setVariableRelated(false);
		}

		// ����״̬������������������
		if (nodeclass != null) {
			if (softtest.config.c.Config.COMPILEFSM) {
				JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
				if (compiler != null) {
					// ����ָ�������Դ�룬������ٶȽ���
					int result = compiler.run(null, null, null, nodeclass
							.getNodeValue().replace('.', '\\')
							+ ".java");
					if (result != 0) {
						throw new RuntimeException(
								"Fail to compile the related action file.");
					}
				} else {
					System.out.println("���붯������ʧ��");
					throw new RuntimeException(
							"Fail to compile the related action file.");
				}
			}
			try {
				Class relatedclass = Class.forName(nodeclass.getNodeValue());
				setRelatedClass(relatedclass);
			} catch (ClassNotFoundException e) {
				// e.printStackTrace();
				throw new RuntimeException("Fail to find the related class."
						+ nodeclass.getNodeValue(),e);
			}
			// ״̬��ע�ắ��ժҪ�ķ���������
			if(relatedclass!=null){
				try{
					Object fsm = relatedclass.newInstance();
					if (fsm instanceof BasicStateMachine) {
						((BasicStateMachine)fsm).registFetureVisitors();
					}
				}catch(Exception e){
				}
			}
		}
		loadAction(n, this.relatedclass);
	}

	/** ��ӡ */
	@Override
	public String toString() {
		return name;
	}

	/** ������ļ� */
	public void dump() {
		String name = "temp/" + getName();
		accept(new DumpFSMVisitor(), name + ".dot");
		System.out.println("״̬����������ļ�" + name + ".dot");
		try {
			java.lang.Runtime.getRuntime().exec(
					"dot -Tjpg -o " + name + ".jpg " + name + ".dot").waitFor();
		} catch (IOException e1) {
			System.out.println(e1);
		} catch (InterruptedException e2) {
			System.out.println(e2);
		}
		System.out.println("״̬����ӡ�����ļ�" + name + ".jpg");
	}
}