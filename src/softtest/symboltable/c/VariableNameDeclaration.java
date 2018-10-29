package softtest.symboltable.c;

import java.util.ArrayList;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.symbolic.Factor;
import softtest.interpro.c.Variable;
import softtest.symboltable.c.Type.*;

public class VariableNameDeclaration extends AbstractNameDeclaration {
	/* whether the variable is static*/
	//��Ϊ���ӳ�Ա�����ڴ˳�����Ա��������
	//add by zhouhb
	public ArrayList<VariableNameDeclaration> mems = new ArrayList<VariableNameDeclaration>();
	
	//����ָ�����int *p����p���⣬Ϊ��֧��������������*p������Ϊ����������ǩ����
	private boolean isGen =false;
	
	private boolean isStatic = false;
	
	private boolean isExtern = false;
	
	private boolean isEnum = false;
	
	/* the parameter index If the variable is a parameter of a function*/
	private int paramIndex = -1;
	
	/**zys:��ȡ��������ʱ�ĳ�ʼֵ���Դ˳�ʼ�����ŵ�ֵ������޳�ʼֵ���ʼ��ֵΪһ���ӱ��ʽ���������Ϊ��*/
	private Variable variable;
	
	//����������ʾ��Ա�����븴�ӽṹ������ ��Ա���ӹ�ϵ
	//add by dongyk
	private NameDeclaration fatherVariable=null;

	public NameDeclaration getFatherVariable() {
		return fatherVariable;
	}

	public void setFatherVariable(NameDeclaration fatherDecl) {
		this.fatherVariable = fatherDecl;
	}
	
	
	//add by zhouhb
	//2011.4.25
	public VariableNameDeclaration(String fileName,Scope scope,String image,SimpleNode node){
		super(fileName,scope,image,node);
	}
	
	public VariableNameDeclaration(ASTPostfixExpression node){
		super(node);
	}
	//end by zhouhb
	
	public VariableNameDeclaration(ASTUnaryExpression node){
		super(node);
	}
	//end by dongyk
	
	public VariableNameDeclaration(ASTDirectDeclarator node) {
		super(node);
	}

	public VariableNameDeclaration(ASTEnumerator node) {
		super(node);
		isEnum=true;
	}
	
	public VariableNameDeclaration(ASTParameterDeclaration node)
	{
		super(node);
	}
	
	@Override
	//modified by zhouhb
	//2011.5.5
	public boolean equals(Object o) {
		if(Config.Field){
			VariableNameDeclaration n = (VariableNameDeclaration) o;
			if(node==null){
				return true;
			}else if(n==null||n.node==null)
			{
				return true;
			}else{
				return n.node.equals(node);
			}
		}else{
			VariableNameDeclaration n = (VariableNameDeclaration) o;
			return n.node.equals(node);
		}
	}


	@Override
	//modified by zhouhb
	//2011.5.5
	public int hashCode() {
		if(Config.Field){
			//add by zhouhb
			//2011.5.5
			//��һ���ڵ��Ϲ�������ṹ���Ա����ʱ�����߶༶ָ������ʱ���������ýڵ��image��ΪhashCode
			return this.getImage().hashCode();
		}else{
			return node.getImage().hashCode();
		}
	}

	public boolean isPrimitiveType() {
		return this.isPrimitiveType(this.type);
	}

	private boolean isPrimitiveType(CType t) {
		return t instanceof CType_BaseType || (t instanceof CType_Qualified && isPrimitiveType(((CType_Qualified) t)
						.getOriginaltype()));
	}

	public boolean isArray() {
		return isArray(this.type);
	}

	private boolean isArray(CType t) {
		return t instanceof CType_Array
				|| (t instanceof CType_Qualified && isArray(((CType_Qualified) t)
						.getOriginaltype()));
	}

	public String getTypeImage() {
		if(type!=null)
			return type.toString();
		return null;
	}

	public int getArrayDepth() {
		if (isArray())
			return -1;
		int deep = 0;
		CType t = this.type;
		while (t instanceof CType_Qualified || t instanceof CType_Array) {
			if (t instanceof CType_Array) {
				deep++;
				t = ((CType_Array) t).getOriginaltype();
			} else
				t = ((CType_Qualified) t).getOriginaltype();
		}
		return deep;

	}
	
	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}
	
	public boolean isExtern () {
		return isExtern;
	}

	public void setExtern (boolean isExtern) {
		this.isExtern = isExtern;
	}

	public boolean isEnum() {
		return isEnum;
	}

	public void setEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}

	public Variable getVariable() {
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}
	
	public boolean isParam() {
		return paramIndex != -1;
	}

	public int getParamIndex() {
		return paramIndex;
	}

	public void setParamIndex(int paramIndex) {
		this.paramIndex = paramIndex;
	}
	/** ������ʼ���� */
	private Domain domain = null;

	/** ���ó�ʼ�� */
	public void setDomain(Domain domain) {
		this.domain = domain;
	}
	/** ��ó�ʼ�� */
	public Domain getDomain() {
		if(domain==null)	
			return null;
		else
			try{
				return (domain).clone();}
			catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return null;
			}
	}
	
	public boolean isGen() {
		return isGen;
	}

	public void setGen(boolean isGen) {
		this.isGen = isGen;
	}
}
