package softtest.domain.c.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import softtest.ast.c.ASTEqualityExpression;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTRelationalExpression;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * ѭ������������
 * @author zys	
 * 2010-5-17
 */
public abstract class IterationCalculation implements Serializable {
	/** һ�ε�����ѭ�������б��������Ƿ����ı�*/
	boolean domainChanged=true;
	
	/**��ʶѭ�������Ƿ�Ӧ����ֹ */
	boolean calculateOver=false;
	
	/** ��¼ѭ�������б���ÿ�ε���������䣬�Ա��´ε���ʱwidening*/
	transient Domain lastDomain=null;
	
	/** ���浱ǰ�ڵ�ÿ�ε��������б��������䣬�����´ε���ʱwidening����Ҫ����Է�ѭ�������еı�����*/
	transient VarDomainSet lastVarDomainSet=null;
	
	int counter=0;
	
	/** ��ѭ�����е���������Ϊѭ��ͷ��㣺while_head,for_head,do_while_out1*/
	public abstract void iterationExec(VexNode iterationHead);

	/** ÿ��widening��narrowing������ֹ�������ʱ���ݽṹ�ͷ��ڴ�*/
	public void clear()
	{
		if(lastVarDomainSet!=null)
			lastVarDomainSet.clearDomainSet();
		if(lastDomain!=null)
			lastDomain=null;
	}
	
	/**��ʱֻ�����ѭ������while(i<100 && j<100 || k>100)��ȡѭ�������еı��� */
	public List<VariableNameDeclaration> getLeftVar(VexNode iterationHead)
	{
		List<VariableNameDeclaration> varList=new ArrayList<VariableNameDeclaration>();
		List<Node> expressionList=getRelationalExpression(iterationHead);
		for(Node node : expressionList){
			AbstractExpression expressionNode=null;
			if(node instanceof ASTRelationalExpression)
				expressionNode=(ASTRelationalExpression)node;
			else if(node instanceof ASTEqualityExpression)
				expressionNode=(ASTEqualityExpression)node;
			ASTPrimaryExpression priExpr=(ASTPrimaryExpression) ((SimpleNode)(expressionNode.jjtGetChild(0))).getFirstChildOfType(ASTPrimaryExpression.class);
			if(priExpr!=null && priExpr.getVariableNameDeclaration()!=null)
				varList.add(priExpr.getVariableNameDeclaration());
		}
		return varList;
	}
	
	/** ��ȡѭ���е��������ʽ*/
	private List<Node> getRelationalExpression(VexNode iterationHead)
	{
		List<Node> relationExprList=new ArrayList<Node>();
		
		String nodeName=iterationHead.getName();
		SimpleNode treeNode=iterationHead.getTreenode();
		ASTExpression expressionNode=null;
		if(nodeName.startsWith("while_head")){
			expressionNode=(ASTExpression) treeNode.jjtGetChild(0);
		}else if(nodeName.startsWith("for_head")){
			if(treeNode.forChild[1]){
				if(treeNode.forChild[0]){
					expressionNode=(ASTExpression) treeNode.jjtGetChild(1);
				}else{
					expressionNode=(ASTExpression) treeNode.jjtGetChild(0);
				}
			}
		}else if(nodeName.startsWith("do_while_out1"))
		{
			expressionNode=(ASTExpression) treeNode;
		}
		if(expressionNode==null)
			return null;
		//for(;i<5;i++)
		relationExprList=expressionNode.findChildrenOfType(ASTRelationalExpression.class);
		//for(;i==5;i++)
		expressionNode.findChildrenOfType(ASTEqualityExpression.class, relationExprList);
		
		return relationExprList;
	}
	
	public boolean isDomainChanged(Domain lastDomain, Domain thisDomain) {
		boolean ret=true;
		DomainType domainType=lastDomain.getDomaintype();
		switch(domainType)
		{
		case INTEGER:
		case DOUBLE:
			if(lastDomain.equals(thisDomain))
				ret=false;
			break;
		case POINTER:
			break;
		case UNKNOWN:
			break;
		default:
			break;
		}
		
		return ret;
	}
	
	public boolean isIterationOver() {
		if(counter>=Config.LOOPNUM)
			return false;
		return domainChanged&&!calculateOver;
	}

	public void setDomainChanged(boolean domainChanged) {
		this.domainChanged = domainChanged;
	}

	public Domain getLastDomain() {
		return lastDomain;
	}

	public void setLastDomain(Domain lastDomain) {
		this.lastDomain = lastDomain;
	}

	public VarDomainSet getLastVarDomainSet() {
		return lastVarDomainSet;
	}

	public void setLastVarDomainSet(VarDomainSet lastVarDomainSet) {
		this.lastVarDomainSet = lastVarDomainSet;
	}
}
