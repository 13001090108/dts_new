package softtest.ast.c;

import java.util.ArrayList;

import softtest.symboltable.c.Type.CType;
/**���б��ʽ���ڵ�ĳ�����࣬�ṩ������Ϣ�ӿ�֧�֣���ǰ���ܵľ�����ʽ�ڵ������
 * ASTAdditiveExpression
 * ASTANDExpression
 * ASTArgumentExpressionList
 * ASTAssignmentExpression
 * ASTCastExpression
 * ASTConditionalExpression
 * ASTConstant
 * ASTConstantExpression
 * ASTEqualityExpression
 * ASTExclusiveORExpression
 * ASTExpression
 * ASTFieldId
 * ASTInclusiveORExpression
 * ASTLogicalANDExpression
 * ASTLogicalORExpression
 * ASTMultiplicativeExpression
 * ASTPostfixExpression
 * ASTPrimaryExpression
 * ASTRelationalExpression
 * ASTShiftExpression
 * ASTUnaryExpression
 * */
public abstract class AbstractExpression extends SimpleNode {
	CType type = null;
	/**
	 * �ýڵ��Ƿ��Ǻ������ýڵ�
	 */
	protected boolean isMethod;
	
	public AbstractExpression(CParser p, int i) {
		super(p, i);
	}

	public AbstractExpression(int i) {
		super(i);
	}
	
	public CType getType() {
		return type;
	}

	public void setType(CType type) {
		this.type = type;
	}

	public boolean isMethod() {
		return isMethod;
	}
	
	public Object jjtAccept(CParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}
