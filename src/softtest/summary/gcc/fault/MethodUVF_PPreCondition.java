package softtest.summary.gcc.fault;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import softtest.domain.c.interval.IntegerDomain;
import softtest.summary.c.MethodFeature;
import softtest.summary.gcc.fault.UVF_PInfo.UVF_PType;
import softtest.symboltable.c.VariableNameDeclaration;
/**
 * ������ָ������ʹ��ժҪ����¼��ǰ�����������������е�ָ����������ݵĳ�ʼ����Լ��
 * @author zl
 *
 */
public class MethodUVF_PPreCondition extends MethodFeature{
	public final static String METHOD_UVF_P_PRECONDITION = "METHOD_UVF_P_PRECONDITION";
	private Map<VariableNameDeclaration, UVF_PInfo> varList;
	

	
	public MethodUVF_PPreCondition(){
		super(METHOD_UVF_P_PRECONDITION);
		varList = new HashMap<VariableNameDeclaration, UVF_PInfo>();
	}
	/**
	 * ���ָ��������Լ����Ϣ��ǰ��Լ����
	 */
	public void addVariable(VariableNameDeclaration var, int paramIndex, UVF_PType type,IntegerDomain needInitedomain,IntegerDomain initedomain) {
		varList.put(var, new UVF_PInfo(var.getImage(), paramIndex, type,needInitedomain,initedomain));
	}
	/**
	 * ���ָ��������Լ����Ϣ��ǰ��Լ����
	 */
	public void addVariable(VariableNameDeclaration var, UVF_PInfo info) {
		varList.put(var, info);
	}
	/**
	 * �ж�UVFǰ��Լ���Ƿ������ĳ������Լ��
	 * @param var
	 * @return
	 */
	public boolean contains(VariableNameDeclaration var) {
		if (var == null) {
			return false;
		}
		return varList.containsKey(var); 
	}
	/**
	 * ��ȡǰ��������ĳָ�������ĳ�ʼ��Լ��
	 * @param var
	 * @return
	 */
	public UVF_PInfo getUVF_PInfo(VariableNameDeclaration var) {
		return varList.get(var);
	}
	/**
	 * �жϸ�UVFǰ��Լ���Ƿ�Ϊ��
	 * @return
	 */
	public boolean isEmpty() {
		return varList.size() == 0;
	}
	/**
	 * ��ȡ����ǰ��Լ���ı�������
	 * @return
	 */
	public Set<VariableNameDeclaration> getUVF_PVariables() {
		return varList.keySet();
	}
	/**
	 * �õ���ǰ��Ӧ����var��type
	 */
	public UVF_PType getUVF_PType(VariableNameDeclaration var) {
		return getUVF_PInfo(var).type;
	}
	/**
	 * �õ���ǰ��Ӧ����var��paramIndex
	 */
	public int getUVF_PIndex(VariableNameDeclaration var) {
		return getUVF_PInfo(var).paramIndex;
	}
	@Override
	public String toString() {
		return "MethodUVF_PPreCondition [varList=" + varList + ", name=" + name
				+ ", isEmpty()=" + isEmpty() + ", getUVF_PVariables()="
				+ getUVF_PVariables() + ", getName()=" + getName()
				+ ", toString()=" + super.toString() + ", getClass()="
				+ getClass() + ", hashCode()=" + hashCode() + "]";
	}
	
}
