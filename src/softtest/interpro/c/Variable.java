package softtest.interpro.c;

import java.io.Serializable;

import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;

/**
 * 
 * ȫ�ֶ����ı�����Ϣ��������������������������Ա�������Լ�ȫ�ֱ���
 * ���ھֲ������ں����������˳�֮��ʧȥ���ã���˲�����¼����Ϣ
 *
 */
public class Variable implements Serializable {

	/**ÿһ����������ʵֵ */
	private Object value;
	/**
	 * ÿһ��������������һ��Variable��������¼��������ʱ�ĳ�ʼ����ֵ��Ϣ
	 */
	public Variable(String name,Object value)
	{
		this.name=name;
		this.value=value;
	}
	/**
	 * ������Ӧ������
	 */
	private String name;
	
	/**
	 * �����������򣬷����֣������������������򣩣��࣬ȫ��
	 */
	private ScopeType scopeType;
	
	private String scopeName;
	
	/**
	 * ����������
	 */
	private CType type;
	
	/**
	 * �βα�����Ӧ�ĺ����б����ţ�ֻ���ڸñ���Ϊ��������ʱ���������
	 */
	private int paramIndex = -1;
	
	public Variable(ScopeType scopeType, String name, CType type) {
		this.scopeType = scopeType;
		this.name = name;
		this.type = type;
	}
	 
	/**
	 * ����һ�������ı��ط������ű������������Ӧ��ȫ�ֶ������뺯��ժҪ�����ı�������
	 * @param decl
	 * @return
	 */
	public static Variable getVariable(VariableNameDeclaration decl) {
		if (decl == null) {
			return null;
		}
		if (decl.getVariable() != null) {
			return decl.getVariable();
		}
		Scope scope = decl.getScope();
		Variable var = null;
		if (scope instanceof MethodScope && decl.isParam()) {
			var = new Variable(ScopeType.METHOD_SCOPE, decl.getImage(), decl.getType());
			var.setParamIndex(decl.getParamIndex());
			var.setScopeName(decl.getFileName());
			decl.setVariable(var);
		} else if (scope instanceof ClassScope) {
			var = new Variable(ScopeType.CLASS_SCOPE, decl.getImage(), decl.getType());
			var.setScopeName(scope.getName());
			decl.setVariable(var);
		} else if (scope instanceof SourceFileScope) {
			var = new Variable(ScopeType.INTER_SCOPE, decl.getImage(), decl.getType());
			var.setScopeName(decl.getFileName());
			decl.setVariable(var);
		}
		return var;
	}
	
	
	@Override
	public boolean equals(Object o) {
		if ((o == null) ||  !(o instanceof Variable)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		Variable var = (Variable)o;
		if (!this.name.equals(var.getName())) {
			return false;
		}
		if (this.scopeType != var.getScopeType()) {
			return false;
		}
		if (this.scopeName==null||!this.scopeName.equals(var.getScopeName())) {
			return false;
		}
		if (this.getParamIndex() != var.getParamIndex()) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int ret = 0;
		if (scopeName != null) {
			ret = name.hashCode() + scopeName.hashCode() + this.paramIndex;
		} else {
			ret = name.hashCode() + this.paramIndex;
		}
		return ret;
	}
	
	
	// getters and setters
	public String getName() {
		return name;
	}

	public int getParamIndex() {
		return paramIndex;
	}

	public ScopeType getScopeType() {
		return scopeType;
	}

	public CType getType() {
		return type;
	}

	public boolean isParam() {
		return this.paramIndex != -1;
	}
	public void setParamIndex(int paramIndex) {
		this.paramIndex = paramIndex;
	}

	public String getScopeName() {
		return scopeName;
	}

	public void setScopeName(String scopeName) {
		this.scopeName = scopeName;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}
}
