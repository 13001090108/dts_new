package softtest.summary.gcc.fault;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import softtest.interpro.c.Variable;
import softtest.rules.gcc.fault.BO.BOType;
import softtest.summary.c.MethodFeature;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * BOǰ����������ժҪ��ʵ����
 *
 */
public class MethodBOPreCondition extends MethodFeature {
	//ʹ���ڲ���洢BO������Ӧ����Ϣ����Ӧ���䣬����������Ϣ
	class BOInfo {
		long size;
		String desbuf;
		String desmet;
		boolean flag;
		public BOInfo(long size, String desbuf,String desmet, boolean flag) {
			this.size = size;
			this.desbuf = desbuf;
			this.desmet = desmet;
			this.flag = flag;
		}
	}
	private Map<Variable, BOInfo> varList;
	
	//�����ĸ����Խ��Կ⺯��������
	private BOType subtype;//��ǰBO����ժҪ���ͣ���BUFFERCOPY��
	private int bufIndex;//Ŀ�Ļ�����λ����� ��strcpy(buf, src),��ʱ��ֵΪ0
	private int srcIndex;//Դ������λ�����
	private int limitLen;//���Ƹ��Ƴ��Ȳ�������ţ���strncpy(buf, src, n);��ʱΪ2����������û�����Ƴ��ȵģ�ֵ��Ϊ-1
	private boolean needNull;//�ж��Ƿ���ҪΪ�ú���Ԥ��һ���ֽڱ���'\0'

	public final static String METHOD_BO_PRECONDITION = "METHOD_BO_PRECONDITION";
	
	
	public MethodBOPreCondition() {
		super(METHOD_BO_PRECONDITION);
		varList = new HashMap<Variable, BOInfo>();
	}
	
	/**
	 * ���ָ��������Լ����Ϣ��ǰ��Լ����
	 * @param var
	 * @param des
	 */
	public void addVariable(Variable var,long size, String desbuf, String desmet, boolean flag) {
		varList.put(var, new BOInfo(size, desbuf, desmet, flag));
	}
	/**
	 * �ж�BOǰ��Լ���Ƿ������ĳ������Լ��
	 * @param var
	 * @return
	 */
	public boolean contains(VariableNameDeclaration var) {
		Variable variable = Variable.getVariable(var);
		if (variable == null) {
			return false;
		}
		return varList.containsKey(variable); 
	}
	
	public Set<Variable> getBOVariables() {
		return varList.keySet();
	}
	/**
	 * ��ȡǰ��������ĳָ������������Լ��
	 * @param var
	 * @return
	 */
	public BOInfo getBoInfo(Variable var) {
		return varList.get(var);
	}
	/**
	 * �жϸ�BOǰ��Լ���Ƿ�Ϊ��
	 * @return
	 */
	public boolean isEmpty() {
		return varList.size() == 0;
	}
	//����б�Ҫ��
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.name);
		for (Variable var : varList.keySet()) {
			sb.append(" = " + var.toString());
			sb.append(" [");
			sb.append(varList.get(var) + " ");
			sb.append("]");
		}
		return sb.toString();
	}

	public int getBufIndex() {
		return bufIndex;
	}

	public void setBufIndex(int bufIndex) {
		this.bufIndex = bufIndex;
	}

	public int getSrcIndex() {
		return srcIndex;
	}

	public void setSrcIndex(int srcIndex) {
		this.srcIndex = srcIndex;
	}

	public int getLimitLen() {
		return limitLen;
	}

	public void setLimitLen(int limitLen) {
		this.limitLen = limitLen;
	}

	public BOType getSubtype() {
		return subtype;
	}

	public void setSubtype(BOType subtype) {
		this.subtype = subtype;
	}

	public boolean isNeedNull() {
		return needNull;
	}

	public void setNeedNull(boolean needNull) {
		this.needNull = needNull;
	}

	/**
	 * �õ���ǰ��Ӧ����var������ֵ
	 * @param var
	 * @return
	 */
	public long getsize(Variable var) {
		return varList.get(var).size;
	}
	
	/**
	 * �õ���ǰ��Ӧ����������Դ����������Ŀ�껺�����ı�ʶ
	 * @param var
	 */
	public boolean getflag(Variable var){
		return varList.get(var).flag;
	}
	/**
	 * �õ�ǰ����������Ŀ�껺����������
	 */
	public String getDespbuf(Variable var){
		return varList.get(var).desbuf;
	}
	public String getDespmet(Variable var){
		return varList.get(var).desmet;
	}
}


