package softtest.summary.gcc.fault;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import softtest.interpro.c.Variable;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.summary.c.MethodFeature;
import softtest.summary.gcc.fault.IAOType;

public class MethodIAOPreCondition extends MethodFeature {

	public final static String METHOD_IAO_PRECONDITION = "METHOD_IAO_PRECONDITION";
	private Map<Variable, IAOInfo> varList;
	
	//ʹ���ڲ���洢IAO������Ӧ����Ϣ����ӦIAO���ͣ�����������Ϣ
	public class IAOInfo {
		private String funcName;
		private IAOType type;
		private String des;
		
		public IAOInfo(IAOType type, String funcName, String des) {
			this.type = type;
			this.funcName = funcName;
			this.des = des;
		}
		
		public IAOType getIAOType() {
			return type;
		}
		
		public String getDes() {
			return des;
		}
		
		public String getFuncName() {
			return funcName;
		}
	}
	
	public MethodIAOPreCondition() {
		super(METHOD_IAO_PRECONDITION);
		varList = new HashMap<Variable, IAOInfo>();
	}
	
	/**
	 * ���ָ��������Լ����Ϣ��ǰ��Լ����
	 */
	public void addVariable(Variable var, IAOType type, String funcName, String des) {
		varList.put(var, new IAOInfo(type, funcName, des));
	}
	/**
	 * ���ָ��������Լ����Ϣ��ǰ��Լ����
	 */
	public void addVariable(Variable var, IAOInfo info) {
		varList.put(var, info);
	}
	/**
	 * �ж�IAOǰ��Լ���Ƿ������ĳ������Լ��
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
	
	/**
	 * ��ȡǰ��������ĳָ������������Լ��
	 * @param var
	 * @return
	 */
	public IAOInfo getIAOInfo(Variable var) {
		return varList.get(var);
	}
	
	/**
	 * �жϸ�IAOǰ��Լ���Ƿ�Ϊ��
	 * @return
	 */
	public boolean isEmpty() {
		return varList.size() == 0;
	}
	
	public Set<Variable> getIAOVariables() {
		return varList.keySet();
	}
	
	/**
	 * �õ���ǰ��Ӧ����var��type
	 */
	public IAOType getIAOType(Variable var) {
		return getIAOInfo(var).type;
	}
	
	/**
	 * �õ���ǰ��Ӧ����var��description
	 */
	public String getIAODesp(Variable var) {
		return getIAOInfo(var).des;
	}
	
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
	
}
