package softtest.fsm.c;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.*;

/** ״̬��ʵ�������ڸ��پ���״̬�������������ǰ����״̬ */
public class FSMMachineInstance {
	/** ״̬�� */
	private FSMMachine fsm = null;

	/** ��Ӧ�ı��� */
	private VariableNameDeclaration v = null;

	/** ��ǰ����״̬���� */
	private FSMStateInstanceSet states = null;

	/** ״̬�������Ķ��� */
	private FSMRelatedCalculation relatedobject = null;

	/** ��ʾ����ʱ�õ����ַ��� */
	private String resultstring = "";
	
	/**�ض���ʶ����*/
	private boolean flag = false;//added by zhanghh
	
	/**��Ӧ�Ŀ�����ͼ*/
	private VexNode releatedVexNode = null;//added by zhanghh	
	
	/**
	 * <p>The state related data used for storing the FSM state informations</p>
	 */
	private Object stateData;
	
	/**�Ƿ񾭹�����״̬*/
	private boolean isError = false;//added by xwt  2011.11.3

	/** ��Ӧ�ĳ����﷨���ϵĽ�� * */
	SimpleNode relatedASTNode = null;

	/** ���캯�� */
	public FSMMachineInstance() {
		states = new FSMStateInstanceSet();
	}

	/** �������캯�� */
	public FSMMachineInstance(FSMMachineInstance instance) {
		fsm = instance.fsm;
		v = instance.v;
		if (instance.relatedobject != null) {
			relatedobject = instance.relatedobject.copy();
		}
		resultstring = instance.resultstring;
		relatedASTNode = instance.relatedASTNode;
		releatedVexNode = instance.releatedVexNode;
		
		nodeUseToFindPosition = instance.nodeUseToFindPosition;
		states = new FSMStateInstanceSet(instance.states);
		stateData = instance.stateData;
		traceinfo = instance.traceinfo;
		desp = instance.desp;
		isError = instance.isError;
	}
	
	public boolean isErrorReport() {
		return isError;
	}
	
	public void setErrorReport(boolean isError) {
		this.isError = isError;
	}
	/**
	 * <p>Gets the state related data.</p>
	 * 
	 * @return the state related data
	 */
	public Object getStateData() {
		return stateData;
	}
	
	/**
	 * <p>Sets the state related data.</p>
	 * 
	 * @param stateData
	 */
	public void setStateData(Object stateData) {
		this.stateData = stateData;
	}
	
	/** �����µ�״̬�������������ں� */
	public void addStateInstance(FSMStateInstance state) {
		states.addStateInstance(state);
	}

	/** �����µ�״̬�������������ں� */
	public void addStateInstanceWithoutConditon(FSMStateInstance state) {
		states.addStateInstanceWithoutConditon(state);
	}
	
	/** ����״̬���� */
	public void setStates(FSMStateInstanceSet states) {
		this.states = states;
	}

	/** ���״̬���� */
	public FSMStateInstanceSet getStates() {
		return states;
	}

	/** ���ý���ַ��� */
	public void setResultString(String resultstring) {
		this.resultstring = resultstring;
	}

	/** ��ý���ַ��� */
	public String getResultString() {
		if (resultstring == null || resultstring.trim().length() ==0 ) {
			if (v != null) {
				return v.getImage();
			}
		} else {
			return resultstring;
		}
		return "";
	}

	/** ����һ����ʼ״̬ʵ�� */
	public FSMStateInstance createStartStateInstance() {
		FSMState start = fsm.getStartState();
		if (start == null) {
			throw new RuntimeException("The fsm does not have a start state.");
		}
		return new FSMStateInstance(start);
	}

	/** ����״̬�� */
	public void setFSMMachine(FSMMachine fsm) {
		this.fsm = fsm;
	}

	/** ���״̬�� */
	public FSMMachine getFSMMachine() {
		return fsm;
	}
	/** �����ַ��� */
	//added by cmershen 2016.3.21
	//��ʱIP�ᱨ�������������Լ���������
	private String varImage;
	
	public String getVarImage() {
		return varImage;
	}

	public void setVarImage(String varImage) {
		this.varImage = varImage;
	}

	/** ������ر��� */
	public void setRelatedVariable(VariableNameDeclaration v) {
		if (fsm.isVariableRelated()) {
			this.v = v;
		} else {
			throw new RuntimeException(
					"Try to assign a variable to a nonvariableRelated statemachine.");
		}
	}

	/** �����ر��� */
	public VariableNameDeclaration getRelatedVariable() {
		return v;
	}

	/** ������ض��� */
	public void setRelatedObject(FSMRelatedCalculation relatedobject) {
		this.relatedobject = relatedobject;
	}

	/** �����ض��� */
	public FSMRelatedCalculation getRelatedObject() {
		return relatedobject;
	}

	/** �����ض���ͱ�����״̬����ȣ�����Ϊ��� */
	@Override
	public boolean equals(Object o) {
		if ((o == null) || !(o instanceof FSMMachineInstance)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		boolean b = true;
		FSMMachineInstance x = (FSMMachineInstance) o;
		if (stateData != x.stateData) {
			b = false;
		}
		if (relatedobject != null) {
			if (fsm != x.fsm || v != x.v) {
				b = false;
			} else if (fsm == x.fsm && v == x.v) {
				if (!relatedobject.equals(x.relatedobject))
					b = false;
				if (x.relatedASTNode != this.relatedASTNode) {
					b = false;
				}
			}
		} else {
			if (fsm != x.fsm || v != x.v || relatedASTNode != x.relatedASTNode) {
				b = false;
			}
		}
		return b;
	}

	/** ��Ϊ״̬��ʵ���ᱻ������Ϊhashtable��key,Ϊ�˱�ֵ֤��ȵ�״̬��ʵ������ͬ��hashcode��������д�˸÷��� */
	@Override
	public int hashCode() {
		int i = 0;
		if (v != null) {
			i = i + v.hashCode();
		}
		if (relatedobject != null) {
			i = i + relatedobject.hashCode();
		}
		i = i + fsm.hashCode();
		return i;
	}

	/** ��ӡ */
	@Override
	public String toString() {
		String s = "";
		s = s + fsm + " on " + getResultString() + ":" + states;
		return s;
	}

	/** ���ݵ�ǰ�������ڵ㣬����״̬��������״̬���� */
	public void calDomainSet(VexNode vex) {
		states.calDomainSet(vex);
	}

	/** ����ǰ���������жϽڵ���Ƿ�Ϊ���֧��־������״̬��������״̬���� */
	public void calCondition(VexNode pre, boolean istruebranch) {
		states.calCondition(pre, istruebranch);
	}

	/** ����caselabel�ڵ��switch�ڵ㣬����״̬��������״̬���� */
	public void calSwitch(VexNode n, VexNode pre) {
		states.calSwitch(n, pre);
	}

	// add by ZhangGuanNan
	/** �������״̬��ʵ����صĳ����﷨���ϵĽ��* */
	public void setRelatedASTNode(SimpleNode snode) {
		this.relatedASTNode = snode;
	}

	/** ��ȡ���״̬��ʵ����صĳ����﷨���ϵĽ��* */
	public SimpleNode getRelatedASTNode() {
		return this.relatedASTNode;
	}
	
	/** ����������λ����������ڵصĳ����﷨���ڵ㣬�����״̬��ʵ����*/
	public SimpleNode nodeUseToFindPosition;
	
	public void setNodeUseToFindPosition(SimpleNode node){
		this.nodeUseToFindPosition=node;
	}
	public SimpleNode getNodeUseToFindPosition(){
		return this.nodeUseToFindPosition;
	}
	
	/**����ͬһ�ļ��в�ͬ״̬��ʵ���ı�ʶ* */
	public int  idFlag;
	public void setFsmIdFlag(int idflag){
		this.idFlag=idflag;
	}
	public int getFsmIdFlag(){
		return idFlag;
	}
	// end by zhanggn
	
	/**�����ض�����*/
	public void setFlag(boolean bool){
		this.flag = bool;
	}
	
	/**��ȡ�ض�����*/
	public boolean getFlag(){
		return this.flag;
	}
	
	/**�趨��صĿ������ڵ�*/
	public void setReleatedVexNode(VexNode vexNode){
		this.releatedVexNode = vexNode;
	}
	
	/**��ȡ��Ӧ�Ŀ������ڵ�*/
	public VexNode getReleatedVexNode(){
		return this.releatedVexNode;
	}
	
	private String traceinfo = "";
	public String getTraceinfo() {
		return traceinfo;
	}

	public void setTraceinfo(String traceinfo) {
		this.traceinfo = traceinfo;
	}
	
	private String desp = "";
	public String getDesp() {
		return desp;
	}
	
	public void setDesp(String desp) {
		this.desp = desp;
	}
}
