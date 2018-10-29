package softtest.summary.gcc.fault;

import java.util.HashMap;
import java.util.Set;

import softtest.interpro.c.Method;
import softtest.interpro.c.Variable;
import softtest.summary.c.MethodFeature;

public class MethodRLFeature extends MethodFeature{

	public final static String METHOD_RL_FEATURE = "METHOD_RL_FEATURE";
	
	private HashMap<Variable, Method> varAllocFree;
	
	//����������������Ϣ
	private HashMap<Variable, String> desp;
	
	private boolean isAllocateAndReturn = false;
    
	private Method allocMethod;//����ֱ�ӷ�����Դ��������ʱ��ʶ�������ҳ�����Ƶò��ã���ӭָ��
	private Method releaseMethod;
	private String retTrace = "";
	
	public MethodRLFeature() {
		super(METHOD_RL_FEATURE);
		varAllocFree = new HashMap<Variable,Method>();
		desp = new HashMap<Variable, String>();
	}
	public boolean isAllocateAndReturn() {
		return isAllocateAndReturn;
	}
	public void setAllocateAndReturn(boolean isAllocateAndReturn) {
		this.isAllocateAndReturn = isAllocateAndReturn;
	}
	
	public void addDesp(Variable var, String desp) {
		this.desp.put(var, desp);
	}
	
	public HashMap<Variable, String> getAllDesp(){
		return desp;
	}
	
	public String getDesp(Variable var) {
		return desp.get(var);
	}
	
	public HashMap<Variable, Method> getRLFetures() {
		return varAllocFree;
	}
	
	public void setRetTrace(String retTrace) {
		this.retTrace = retTrace;
	}
	
	public String getRetTrace() {
		return retTrace;
	}
	/**
	 * �жϸ�RL����ժҪ�Ƿ�Ϊ��
	 */
	public boolean isEmpty() {
		return varAllocFree.isEmpty() && !isAllocateAndReturn;
	}
	
	/**
	 * �ж�RL����ժҪ�Ƿ����ĳ����
	 */
	public boolean isContain(Variable variable) {
		return varAllocFree.containsKey(variable);
	}
	
	public Set<Variable> getRLVariable(){
		return varAllocFree.keySet();
	}
	
	/**
	 * ���ָ��������Լ����Ϣ��ǰ��Լ����
	 */
	public void addVariable(Variable var, Method methodName) {
		varAllocFree.put(var, methodName);
	}
	
	public Method getMethodNameDeclaration(Variable var) {
		if(var == null)
			return null;
		return varAllocFree.get(var);
	}
	public void setRelatedReleasedMethod(Method releaseMethod) {
		this.releaseMethod = releaseMethod;
		
	}
	public Method getRelatedReleasedMethod() {
		return this.releaseMethod;
		
	}
	public void setAllocMethod(Method allocMethod) {
		this.allocMethod = allocMethod;
	}
	public Method getAllocMethod() {
		return allocMethod;
	}
}
