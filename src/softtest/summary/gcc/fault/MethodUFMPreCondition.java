package softtest.summary.gcc.fault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import softtest.interpro.c.Variable;
import softtest.summary.c.MethodFeature;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * @author cjie
 */
public class MethodUFMPreCondition extends MethodFeature {

	public final static String METHOD_UFM_PRECONDITION = "METHOD_UFM_PRECONDITION";
	
	private Map<Variable, ArrayList<String>> varList;
	
	public MethodUFMPreCondition() {
		super(METHOD_UFM_PRECONDITION);
		varList = new HashMap<Variable, ArrayList<String>>();
	}
	
	/**
	 * ����ָ��������Լ����Ϣ��ǰ��Լ����
	 * @param var
	 * @param des
	 */
	public void addVariable(Variable var, String des) {
		ArrayList<String> desList = null;
		if (varList.get(var) == null) {
			desList = new ArrayList<String>();
			varList.put(var, desList);
		} else {
			desList = varList.get(var);
		}
		desList.add(des);
	}
	
	/**
	 * ����ָ��������Լ����Ϣ����ǰ��Լ����
	 * @param var
	 * @param des
	 */
	public void addVariable(Variable var, ArrayList<String> des) {
		varList.put(var, des);
	}
	
	/**
	 * �ж�UFMǰ��Լ���Ƿ������ĳ������Լ��
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
	
	public Set<Variable> getUFMVariables() {
		return varList.keySet();
	}
	
	/**
	 * ��ȡǰ��������ĳָ��������Լ����
	 * @param var
	 * @return
	 */
	public ArrayList<String> getDesp(Variable var) {
		return varList.get(var);
	}
	
	public String getDespString(Variable var) {
		ArrayList<String> desps = varList.get(var);
		String re = "\"" + var.getName() + "\" in ";
		//if (Config.DTS_LANGUAGE == Config.LANG_CHINESE) {
			re = "\"" + var.getName() + "\"";
		//}
		for (String desp : desps) {
			re += ", " + desp;
		}
		return re;
	}
	
	/**
	 * �жϸ�UFMǰ��Լ���Ƿ�Ϊ��
	 * @return
	 */
	public boolean isEmpty() {
		return varList.size() == 0;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.name);
		for (Variable var : varList.keySet()) {
			sb.append(" = " + var.toString());
			sb.append(" [");
			for (String des : varList.get(var)) {
				sb.append(des + " ");
			}
			sb.append("]");
		}
		return sb.toString();
	}
}