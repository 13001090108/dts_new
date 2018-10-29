package softtest.domain.c.analysis;

import java.util.List;

import org.apache.log4j.Logger;

import softtest.ast.c.*;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.DoubleMath;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.Factor;
import softtest.domain.c.symbolic.NumberFactor;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.domain.c.symbolic.Term;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;

public class ConditionDomainVisitor extends CParserVisitorAdapter {
	public Object visit(SimpleNode node, Object data) {
		// ��ֹ�������Ӵ���
		// node.childrenAccept(this, data);
		return null;
	}
	
	public Object visit(ASTExpression node, Object data) {
		//zys:2010.8.11	�������жϽڵ㣬����������﷨��������ޣ����ٵݹ�������ӽ��
		int depth=node.getDescendantDepth();
		if(depth<=Config.MAXASTTREEDEPTH){
			node.childrenAccept(this, data);
		}else{
			//Logger.getRootLogger().info("ConditionDomainVisitor: ASTExpression in .i "+node.getBeginFileLine()+" depth="+depth+" overflow");
		}
//		node.childrenAccept(this, data);
		return data;
	}
	
	/**��ʱֻ������򵥵�a+b���ͣ�added by zys 2011.6.20	if(a+b) */
	public Object visit(ASTAdditiveExpression node, Object data){
		if(node.jjtGetNumChildren()==2){
			SimpleNode leftNode=(SimpleNode) node.jjtGetChild(0);
			SimpleNode rightNode=(SimpleNode) node.jjtGetChild(1);
			resetSymbolExpressionForConditionVariable(leftNode);
			resetSymbolExpressionForConditionVariable(rightNode);
		}
		return data;
	}
	
	/**��ʱ�����κδ���added by zys 2011.6.20	if(a*b) */
	public Object visit(ASTMultiplicativeExpression node, Object data){
		
		return data;
	}
	
	public Object visit(ASTAssignmentExpression node, Object data) {
		//chh   ����if��a+b���е�a+b�������͵ı��ʽҪ��������з���
		SimpleNode child = (SimpleNode) node.jjtGetChild(0);
		if(child!=null&&!(child instanceof ASTCastExpression)
				&&!(child instanceof ASTLogicalORExpression)
				&&!(child instanceof ASTLogicalANDExpression)
				&&!(child instanceof ASTEqualityExpression)
				&&!(child instanceof ASTRelationalExpression)
				&&!(child instanceof ASTUnaryExpression))
		{
			/**Begin: added by zys 2011.6.20 if(a+b)��if(a*b) */
			child.jjtAccept(this, data);
			/**End: added by zys 2011.6.20	if(a+b)��if(a*b) */
			
			ConditionData condata = (ConditionData) data;
			Expression  exp=null;
			ExpressionVistorData expdata = new ExpressionVistorData();
			expdata.currentvex = condata.currentvex;
			expdata.sideeffect = false;
			child.jjtAccept(new ExpressionValueVisitor(), expdata);
			exp=expdata.value;
			
			if(exp!=null && exp.getSingleFactor() instanceof NumberFactor){
				NumberFactor f=(NumberFactor)exp.getSingleFactor();
				Domain may=null,must=null;
				SymbolFactor s=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
				if((f.getDoubleValue()==0)){	
					may=IntegerDomain.getEmptyDomain();
					must=IntegerDomain.getEmptyDomain();
					condata.addMayDomain(s, may);
					condata.addMustDomain(s, must);
				}else{
					may=IntegerDomain.getFullDomain();
					must=IntegerDomain.getFullDomain();
					condata.addMayDomain(s, may);
					condata.addMustDomain(s, must);
				}
				
				return data;
			}
			if(exp==null)
			{
				return data;
			}
			for(Term t:exp.getTerms()){
				Factor f=t.getSingleFactor();
				if(f instanceof SymbolFactor){
					SymbolFactor s=(SymbolFactor)f;
					Expression temp=new Expression(s).sub(exp);
					Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
					CType type=s.getType();
					
					if(rightdomain!=null){
						Domain may=null,must=null;
						may = Domain.intersect(leftdomain, rightdomain, type);
						if (rightdomain.isCanonical()) {
							must = Domain.intersect(leftdomain, rightdomain, type);
						} else {
							must = Domain.getEmptyDomainFromType(s.getType());
						}
						
							condata.addMayDomain(s, may);
							condata.addMustDomain(s, must);
						
					}
				}
				//����ϵ��Ϊ-1���
				f=t.getMinusSingleFactor();
				if(f instanceof SymbolFactor){
					SymbolFactor s=(SymbolFactor)f;
					Expression temp=new Expression(s).add(exp);
					Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
					CType type=s.getType();
					
					if(rightdomain!=null){
						Domain may=null,must=null;
						may = Domain.intersect(leftdomain, rightdomain, type);
						if (rightdomain.isCanonical()) {
							must = Domain.intersect(leftdomain, rightdomain, type);
						} else {
							must = Domain.getEmptyDomainFromType(s.getType());
						}
					
							condata.addMayDomain(s, may);
							condata.addMustDomain(s, must);
						
					}
				}
			}
			return data;
		}
		else
		{
			node.childrenAccept(this, data);
			return data;
		}
		//end
	}
	public Object visit(ASTCastExpression node, Object data) {
		node.childrenAccept(this, data);
		return data;
	} 
	
	public Object visit(ASTLogicalORExpression node, Object data) {
		// ||
		if(node.containsChildOfType(ASTPrimaryExpression.class)){
			List<Node> primarys =node.findChildrenOfType(ASTPrimaryExpression.class);
			for(Node pri:primarys){
				if(pri.jjtGetNumChildren()==0){
					VariableNameDeclaration v =((SimpleNode)pri).getVariableNameDeclaration();
					if(v!=null){
						if(v.getType()instanceof CType_Array&&!(((CType_Array)v.getType()).getOriginaltype() instanceof CType_BaseType||((CType_Array)v.getType()).getOriginaltype() instanceof CType_Qualified)||
								(v.getType()instanceof CType_Struct&&pri.jjtGetParent().jjtGetNumChildren()==1)){
							return data;
						}
					}
				}
			}
		}
		ConditionData condata = (ConditionData) data;
		ConditionData leftdata = new ConditionData(condata.currentvex);
		Node currentnode = node.jjtGetChild(0);
		currentnode.jjtAccept(this, leftdata);

		for (int i = 1; i < node.jjtGetNumChildren(); i++) {
			ConditionData rightdata = new ConditionData(condata.currentvex);
			currentnode =  node.jjtGetChild(i);
			currentnode.jjtAccept(this, rightdata);
			leftdata=condata.calLogicalOrExpression(leftdata, rightdata);
		}
		condata.setDomainsTable(leftdata.getDomainsTable());
		return data;
	}
	
	public Object visit(ASTLogicalANDExpression node, Object data) {
		//&&
		ConditionData condata = (ConditionData) data;
		ConditionData leftdata = new ConditionData(condata.currentvex);
		Node currentnode = node.jjtGetChild(0);
		currentnode.jjtAccept(this, leftdata);

		for (int i = 1; i < node.jjtGetNumChildren(); i++) {
			ConditionData rightdata = new ConditionData(condata.currentvex);
			currentnode =  node.jjtGetChild(i);
			currentnode.jjtAccept(this, rightdata);
			leftdata=condata.calLogicalAndExpression(leftdata, rightdata);
		}
		condata.setDomainsTable(leftdata.getDomainsTable());
		return data;
	}
	
	public Object visit(ASTEqualityExpression node, Object data) {
		//== !=
		ConditionData condata = (ConditionData) data;
		if(node.getOperatorType().size()>1){
			return data;
		}
		String operator=node.getOperatorType().get(0);
		SimpleNode left = (SimpleNode) node.jjtGetChild(0);
		SimpleNode right = (SimpleNode) node.jjtGetChild(1);
		
		//begin: modified by zys 2011.6.20
		VariableNameDeclaration leftVar=resetSymbolExpressionForConditionVariable(left);
		VariableNameDeclaration rightVar=resetSymbolExpressionForConditionVariable(right);
		//end: modified by zys 2011.6.20
		
		Expression rightvalue=null,leftvalue=null;
		ExpressionVistorData expdata = new ExpressionVistorData();
		expdata.currentvex = condata.currentvex;
		expdata.sideeffect = false;
		left.jjtAccept(new ExpressionValueVisitor(), expdata);
		leftvalue=expdata.value;
		expdata.value = null;
		right.jjtAccept(new ExpressionValueVisitor(), expdata);
		rightvalue=expdata.value;
		//����һЩ���������޷������ʲ�����������������ж�
		//add by zhouhb 2010/8/23
//		if(node.containsChildOfType(ASTPrimaryExpression.class)){
//			List<Node> primarys =node.findChildrenOfType(ASTPrimaryExpression.class);
//			for(Node pri:primarys){
//				if(pri.jjtGetNumChildren()==0){
//					VariableNameDeclaration v =((SimpleNode)pri).getVariableNameDeclaration();
//					if(v!=null){
//						if(v.getType()instanceof CType_Pointer&&!(((CType_Pointer)v.getType()).getOriginaltype() instanceof CType_BaseType||((CType_Pointer)v.getType()).getOriginaltype() instanceof CType_Qualified)||
//								v.getType()instanceof CType_Array&&!(((CType_Array)v.getType()).getOriginaltype() instanceof CType_BaseType||((CType_Pointer)v.getType()).getOriginaltype() instanceof CType_Qualified)||
//								v.getType()instanceof CType_Struct){
//							return data;
//						}
//					}
//				}
//			}
//		}
		if(leftvalue==null || rightvalue==null)
		{
			return data;
		}
		
		//add by zhouhb
		//���������������һ��δ֪�����账��
//		Domain LDomain=leftvalue.getDomain(expdata.currentvex.getSymDomainset());
//		Domain RDomain=rightvalue.getDomain(expdata.currentvex.getSymDomainset());
//		if(LDomain==null||RDomain==null){
//			return data;
//		}

		Expression exp=leftvalue.sub(rightvalue);
		if(exp.getSingleFactor() instanceof NumberFactor){
			NumberFactor f=(NumberFactor)exp.getSingleFactor();
			Domain may=null,must=null;
			SymbolFactor s=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			if((f.getDoubleValue()==0&&operator.equals("=="))||
					(f.getDoubleValue()!=0&&operator.equals("!="))){
				may=IntegerDomain.getFullDomain();
				must=IntegerDomain.getFullDomain();
				condata.addMayDomain(s, may);
				condata.addMustDomain(s, must);	
			}else{
				may=IntegerDomain.getEmptyDomain();
				must=IntegerDomain.getEmptyDomain();
				condata.addMayDomain(s, may);
				condata.addMustDomain(s, must);	
			}
			return data;
		}
//		if(Config.USEUNKNOWN && (LDomain.isUnknown() && RDomain.isUnknown()) ){
//			return data;
//		}
		for(Term t:exp.getTerms()){
			Factor f=t.getSingleFactor();
			if(f instanceof SymbolFactor){
				SymbolFactor s=(SymbolFactor)f;
				Expression temp=new Expression(s).sub(exp);
				Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
				//���ڿ�ָ�룬�˴�ȡֵΪ0��������Ӧ����ת��
			    //add by zhouhb 2010/7/19
				if(rightdomain!=null && rightdomain.toString().equals("[0,0]")||expdata.value.toString().equals("0")){
					//����'\0'ͨ���������Ȼ��Ӧ����Ϊ[0,0]���˴������Ǵ����
					//modified by zhouhb 2010/8/23
					ASTConstant con=(ASTConstant)right.getFirstChildOfType(ASTConstant.class);
					if(con!=null && con.getImage().startsWith("\'"))
						return data;
					PointerDomain pDomain=new PointerDomain();
					pDomain.offsetRange.intervals.clear();
					//pDomain.AllocType=CType_AllocType.Null;
					pDomain.Type.add(CType_AllocType.Null);
					pDomain.setValue(PointerValue.NULL);
					rightdomain=pDomain;
				}
				Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
				CType type=s.getType();
				
				if(rightdomain!=null){
					Domain may=null,must=null;
					may = Domain.intersect(leftdomain, rightdomain, type);
					if (rightdomain.isCanonical()) {
						must = Domain.intersect(leftdomain, rightdomain, type);
					} else {
						must = Domain.getEmptyDomainFromType(s.getType());
					}
					if (operator.equals("==")) {
						condata.addMayDomain(s, may);
						condata.addMustDomain(s, must);
					}else{
						Domain temp1=Domain.inverse(must);
						Domain temp2=Domain.inverse(may);
						//����ָ�����ռ���˵���ǿռ�����ɹ���ֱ�Ӽ̳�"!="��ߵķ������Լ���
						//add by zhouhb 2010/7/19
						if(leftdomain instanceof PointerDomain && rightdomain instanceof PointerDomain && ((PointerDomain)rightdomain).getValue().toString().equals("NULL")){
							if(temp1 instanceof IntegerDomain)
							{
								temp1=Domain.castToType(temp1, new CType_Pointer());
								temp2=Domain.castToType(temp2, new CType_Pointer());
							}							
							((PointerDomain)temp1).offsetRange=((PointerDomain)leftdomain).offsetRange;
							((PointerDomain)temp1).allocRange=((PointerDomain)leftdomain).allocRange;
							((PointerDomain)temp1).Type=((PointerDomain)leftdomain).Type;
							((PointerDomain)temp2).offsetRange=((PointerDomain)leftdomain).offsetRange;
							((PointerDomain)temp2).allocRange=((PointerDomain)leftdomain).allocRange;
							((PointerDomain)temp2).Type=((PointerDomain)leftdomain).Type;
						}
						condata.addMayDomain(s,temp1);
						condata.addMustDomain(s,temp2);
					}
				}
			}
			//����ϵ��Ϊ-1���
			f=t.getMinusSingleFactor();
			if(f instanceof SymbolFactor){
				SymbolFactor s=(SymbolFactor)f;
				Expression temp=new Expression(s).add(exp);
				Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
				Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
				CType type=s.getType();
				
				if(rightdomain!=null){
					Domain may=null,must=null;
					may = Domain.intersect(leftdomain, rightdomain, type);
					if (rightdomain.isCanonical()) {
						must = Domain.intersect(leftdomain, rightdomain, type);
					} else {
						must = Domain.getEmptyDomainFromType(s.getType());
					}
					if (operator.equals("==")) {
						condata.addMayDomain(s, may);
						condata.addMustDomain(s, must);
					}else{
						condata.addMayDomain(s, Domain.inverse(must));
						condata.addMustDomain(s, Domain.inverse(may));
					}
				}
			}
		}
		return data;
	}
	
	/**modified by zys 2011.6.20
	 * �޸�ԭ�򣺶���if(x>y)��if(x>0)���Ƶı��ʽ�����x��y�˴��ķ��ű��ʽΪ�������ͣ��������������ķ���s1+s2���򵥸����ŵķ����Ա�ʾs1^2 s1*s2,
	 * ��ʱ��ҪΪ���������������µķ��ţ�������Ӧ��condata*/
	private VariableNameDeclaration resetSymbolExpressionForConditionVariable(SimpleNode n){
		VariableNameDeclaration leftVar=n.getVariableNameDeclaration();
		if(leftVar!=null){
			VexNode vex=n.getCurrentVexNode();
			Expression leftExpr=vex.getValue(leftVar);
			if(leftExpr==null){
				return null;
				//throw new RuntimeException("RelationalExpresion����ֵ�������ʽΪ��!");
			}else{
				if(leftExpr.isComplicated()){
					Domain d=vex.getDomain(leftVar);
					SymbolFactor s=SymbolFactor.genSymbol(leftVar.getType(), leftVar.getImage());
					vex.addSymbolDomain(s, d);
					vex.addValue(leftVar, new Expression(s));
				}
			}
		}else {
			SimpleNode operatorNode=(SimpleNode) n.getFirstChildOfType(ASTAssignmentOperator.class);
			if(operatorNode!=null){
				//���������ʽ�а�����ֵ��������е�������if((v=getValue()) > 0)
				SimpleNode leftNode=(SimpleNode) operatorNode.getLastSibling();
				leftVar=resetSymbolExpressionForConditionVariable(leftNode);
			}
		}
		return leftVar;
	}
	
	public Object visit(ASTRelationalExpression node, Object data) {
		ConditionData condata = (ConditionData) data;
		if(node.getOperatorType().size()>1){
			return data;
		}
		String operator=node.getOperatorType().get(0);
		SimpleNode left = (SimpleNode) node.jjtGetChild(0);
		SimpleNode right = (SimpleNode) node.jjtGetChild(1);
		
		//begin: modified by zys 2011.6.20
		VariableNameDeclaration leftVar=resetSymbolExpressionForConditionVariable(left);
		VariableNameDeclaration rightVar=resetSymbolExpressionForConditionVariable(right);
		//end: modified by zys 2011.6.20
		
		Expression rightvalue=null,leftvalue=null;
		ExpressionVistorData expdata = new ExpressionVistorData();
		expdata.sideeffect = false;
		expdata.currentvex = condata.currentvex;
		left.jjtAccept(new ExpressionValueVisitor(), expdata);
		leftvalue=expdata.value;
		expdata.value = null;
		right.jjtAccept(new ExpressionValueVisitor(), expdata);
		rightvalue=expdata.value;
	
		if(leftvalue==null || rightvalue==null)
		{
			return data;
		}
		
		Expression exp=leftvalue.sub(rightvalue);
		
		if(exp.getSingleFactor() instanceof NumberFactor){
			NumberFactor f=(NumberFactor)exp.getSingleFactor();
			Domain may=null,must=null;
			SymbolFactor s=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			if((f.getDoubleValue()>0 && operator.equals(">"))||//zys:�˴����ж�����Ҳ������
					(f.getDoubleValue()>=0 && operator.equals(">="))||
					(f.getDoubleValue()<0 && operator.equals("<"))||
					(f.getDoubleValue()<=0 && operator.equals("<="))){
				may=Domain.getFullDomainFromType(f.getType());
				must=Domain.getFullDomainFromType(f.getType());
				
				//zys:2010.3.26 	��������޶��������㣬��ֱ��ȡ��ǰ�������򼴿�
//				may=f.getDomain(node.getCurrentVexNode().getSymDomainset());
//				must=f.getDomain(node.getCurrentVexNode().getSymDomainset());
				condata.addMayDomain(s, may);
				condata.addMustDomain(s, must);	
			}else{//zys:	��һ��Ϊ����
				may=IntegerDomain.getEmptyDomain();
				must=IntegerDomain.getEmptyDomain();
				condata.addMayDomain(s, may);
				condata.addMustDomain(s, must);	
			}
			return data;
		}
		//chh  2010.12.8    if(m>n)m,n��δ֪ʱ�޷��жϡ�
		Domain LDomain=leftvalue.getDomain(expdata.currentvex.getSymDomainset());
		Domain RDomain=rightvalue.getDomain(expdata.currentvex.getSymDomainset());
		if(Config.USEUNKNOWN && (LDomain!=null && RDomain!=null && LDomain.isUnknown() && RDomain.isUnknown())){
			return data;
		}
		
		for(Term t:exp.getTerms()){
			Factor f=t.getSingleFactor();
			if(f instanceof SymbolFactor){
				SymbolFactor s=(SymbolFactor)f;
				Expression temp=new Expression(s).sub(exp);
				Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
				
				if(rightdomain!=null && !rightdomain.isUnknown()){
					CType type=s.getType();
					Domain may=null,must=null;
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
										
					IntegerDomain integerdomain=null;
					DoubleDomain doubledomain=null;
					switch (Domain.getDomainTypeFromType(type)){
					case INTEGER:
						integerdomain=Domain.castToIntegerDomain(rightdomain);
						long max = integerdomain.getMax(), min = integerdomain.getMin();
						if(operator.equals(">")){
							may = Domain.intersect(leftdomain,new IntegerDomain(integerdomain.getMin()+1, Long.MAX_VALUE),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(max != Long.MAX_VALUE ? max+1 : max, Long.MAX_VALUE),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals(">=")) {
							may = Domain.intersect(leftdomain,new IntegerDomain(integerdomain.getMin(), Long.MAX_VALUE),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(integerdomain.getMax(), Long.MAX_VALUE),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals("<")) {
							may = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,integerdomain.getMax()-1),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,min != Long.MIN_VALUE ? min-1 : min),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals("<=")) {
							may = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,integerdomain.getMax()),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,integerdomain.getMin()),type);
							condata.addMustDomain(s, must);
						}else{
							throw new RuntimeException("This is not a legal RelationalExpression");
						}
						break;
					case DOUBLE:
						doubledomain=Domain.castToDoubleDomain(rightdomain);
						if(operator.equals(">")){
							may = Domain.intersect(leftdomain,new DoubleDomain(DoubleMath.nextfp(doubledomain.getMin()), Double.POSITIVE_INFINITY),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(DoubleMath.nextfp(doubledomain.getMax()), Double.POSITIVE_INFINITY),type);							condata.addMustDomain(s, must);
						}else if (operator.equals(">=")) {
							may = Domain.intersect(leftdomain,new DoubleDomain(doubledomain.getMin(), Double.POSITIVE_INFINITY),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(doubledomain.getMax(), Double.POSITIVE_INFINITY),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals("<")) {
							may = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,DoubleMath.prevfp(doubledomain.getMax())),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,DoubleMath.prevfp(doubledomain.getMin())),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals("<=")) {
							may = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,doubledomain.getMax()),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,doubledomain.getMin()),type);
							condata.addMustDomain(s, must);
						}else{
							throw new RuntimeException("This is not a legal RelationalExpression");
						}
						break;
					}
				}
			}
			//����ϵ��Ϊ-1�����
			f=t.getMinusSingleFactor();
			if(f instanceof SymbolFactor){
				SymbolFactor s=(SymbolFactor)f;
				Expression temp=exp.add(new Expression(s));
				Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
				
				if(rightdomain!=null && !rightdomain.isUnknown()){
					CType type=s.getType();
					Domain may=null,must=null;
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
										
					IntegerDomain integerdomain=null;
					DoubleDomain doubledomain=null;
					
					switch (Domain.getDomainTypeFromType(type)){
					case INTEGER:
						integerdomain=Domain.castToIntegerDomain(rightdomain);
						long max = integerdomain.getMax(), min = integerdomain.getMin();
						if(operator.equals("<")){
							may = Domain.intersect(leftdomain,new IntegerDomain(integerdomain.getMin()+1, Long.MAX_VALUE),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(max != Long.MAX_VALUE ? max+1 : max, Long.MAX_VALUE),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals("<=")) {
							may = Domain.intersect(leftdomain,new IntegerDomain(integerdomain.getMin(), Long.MAX_VALUE),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(integerdomain.getMax(), Long.MAX_VALUE),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals(">")) {
							may = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,integerdomain.getMax()-1),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,min != Long.MIN_VALUE ? min-1 : min),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals(">=")) {
							may = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,integerdomain.getMax()),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new IntegerDomain(Long.MIN_VALUE,integerdomain.getMin()),type);
							condata.addMustDomain(s, must);
						}else{
							throw new RuntimeException("This is not a legal RelationalExpression");
						}
						break;
					case DOUBLE:
						doubledomain=Domain.castToDoubleDomain(rightdomain);
						if(operator.equals("<")){
							may = Domain.intersect(leftdomain,new DoubleDomain(DoubleMath.nextfp(doubledomain.getMin()), Double.POSITIVE_INFINITY),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(DoubleMath.nextfp(doubledomain.getMax()), Double.POSITIVE_INFINITY),type);							condata.addMustDomain(s, must);
						}else if (operator.equals("<=")) {
							may = Domain.intersect(leftdomain,new DoubleDomain(doubledomain.getMin(), Double.POSITIVE_INFINITY),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(doubledomain.getMax(), Double.POSITIVE_INFINITY),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals(">")) {
							may = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,DoubleMath.prevfp(doubledomain.getMax())),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,DoubleMath.prevfp(doubledomain.getMin())),type);
							condata.addMustDomain(s, must);
						}else if (operator.equals(">=")) {
							may = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,doubledomain.getMax()),type);
							condata.addMayDomain(s, may);
							must = Domain.intersect(leftdomain,new DoubleDomain(Double.NEGATIVE_INFINITY,doubledomain.getMin()),type);
							condata.addMustDomain(s, must);
						}else{
							throw new RuntimeException("This is not a legal RelationalExpression");
						}
						break;
					}
				}
			}
		}
		return data;
	}
	
	public Object visit(ASTUnaryExpression node, Object data) {
		ConditionData condata = (ConditionData) data;
		
		//add by zhouhb
		 //2010.9.23
		boolean field=Config.Field && node.getVariableDecl()!=null&&!node.getImage().equals("")&&(node.getImage().contains(".")||node.getImage().contains("->"));
		boolean isMulLevelPointer=false;
		if(node.jjtGetChild(0) instanceof ASTUnaryOperator)
		{
			ASTUnaryOperator o=(ASTUnaryOperator)node.jjtGetChild(0);
			String os=o.getOperatorType().get(0);	
			if(os.equals("*"))
			{
				VariableNameDeclaration v=node.getVariableDecl();
				if(v!=null&&v.getType().isPointType()){
					isMulLevelPointer=true;
				}
			}
		}		
		if(field||isMulLevelPointer){
			VariableNameDeclaration v=node.getVariableDecl();
			if(v==null){
				return data;
			}
			Expression exp=condata.currentvex.getValue(v);
			
			//zhouhb 2011.6.15	if(A.p)
			if(exp==null){
				exp=new Expression(SymbolFactor.genSymbol(v.getType(), v.getImage()));
			}
			for(Term t:exp.getTerms()){
				Factor f=t.getSingleFactor();
				if(f instanceof SymbolFactor){
					SymbolFactor s=(SymbolFactor)f;
					Expression temp=new Expression(s).sub(exp);
					Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
					CType type=s.getType();
					
					if(rightdomain!=null){
						Domain may=null,must=null;
						may = Domain.intersect(leftdomain, rightdomain, type);
						if (rightdomain.isCanonical()) {
							must = Domain.intersect(leftdomain, rightdomain, type);
						} else {
							must = Domain.getEmptyDomainFromType(type);
						}
						//zys:2010.4.24 if(p)������Ϊp!=0
						if(temp.toString().equals("0")){
							may=Domain.inverse(may);
							must=Domain.inverse(must);
						}
						condata.addMayDomain(s, may);
						condata.addMustDomain(s, must);
					}
				}
				//����ϵ��Ϊ-1���
				f=t.getMinusSingleFactor();
				if(f instanceof SymbolFactor){
					SymbolFactor s=(SymbolFactor)f;
					Expression temp=new Expression(s).add(exp);
					Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
					CType type=s.getType();
					
					if(rightdomain!=null){
						Domain may=null,must=null;
						may = Domain.intersect(leftdomain, rightdomain, type);
						if (rightdomain.isCanonical()) {
							must = Domain.intersect(leftdomain, rightdomain, type);
						} else {
							must = Domain.getEmptyDomainFromType(type);
						}
						condata.addMayDomain(s, may);
						condata.addMustDomain(s, must);
					}
				}
			}
			//return data;
		}//end by zhouhb
		
		if(node.jjtGetChild(0) instanceof ASTUnaryOperator){
			ASTUnaryOperator operator=(ASTUnaryOperator)node.jjtGetChild(0);
			String o=operator.getOperatorType().get(0);			
			if(o.equals("!")){
				ConditionData tempdata = new ConditionData(condata.currentvex);
				node.jjtGetChild(1).jjtAccept(this, tempdata);
				SymbolDomainSet may = tempdata.getFalseMayDomainSet();
				SymbolDomainSet must = tempdata.getFalseMustDomainSet();

				condata.addMayDomain(may);
				condata.addMustDomain(must);
				//condata.setDomainsTable(tempdata.getDomainsTable());
				//zys:2010.4.23	����ʱ�������������
				tempdata.clearDomains();
			}else if(o.equals("-")){//liuli����if(-(����/���ʽ))�Ĵ���				
				ASTUnaryExpression temp = (ASTUnaryExpression)node.jjtGetChild(1);
				ASTPrimaryExpression primary=(ASTPrimaryExpression)temp.getSingleChildofType(ASTPrimaryExpression.class);
				if(primary!=null){
					primary.jjtAccept(this, data);
				}
			}else if(o.equals("*"))
			{
				
				ConditionData tempdata = new ConditionData(condata.currentvex);
				node.jjtGetChild(1).jjtAccept(this, tempdata);
				SymbolDomainSet may = tempdata.getTrueMayDomainSet();
				SymbolDomainSet must = tempdata.getTrueMustDomainSet();

				condata.addMayDomain(may);
				condata.addMustDomain(must);
				tempdata.clearDomains();  
				
				//����  *p   Ҳ��ָ������
				VariableNameDeclaration v=node.getVariableDecl();
				if(v==null){
					return data;
				}
			}
		}
		else if(!field){
			ASTPrimaryExpression primary=(ASTPrimaryExpression)node.getSingleChildofType(ASTPrimaryExpression.class);
			if(primary!=null){
				primary.jjtAccept(this, data);
			}
		}
		return data;
	}

	public Object visit(ASTPrimaryExpression node, Object data){
		ConditionData condata = (ConditionData) data;
		//chh  �����жϣ�node�����ӽڵ�Ļ���������������ӽڵ���ASTConstant���⣩
		if(!node.getImage().equals("")&&(node.jjtGetNumChildren()==0||(node.jjtGetNumChildren()==1&&node.jjtGetChild(0) instanceof ASTConstant))){
			//begin: modified by zys 2011.6.20
			//VariableNameDeclaration v=node.getVariableDecl();
			VariableNameDeclaration v=resetSymbolExpressionForConditionVariable(node);
			//end: modified by zys 2011.6.20
			
			if(v==null){
				return data;
			}
			Expression exp=condata.currentvex.getValue(v);
			
			//zys:2010.3.29	if(p)
			if(exp==null){
				exp=new Expression(SymbolFactor.genSymbol(v.getType(), v.getImage()));
			}
			if(exp.getSingleFactor() instanceof NumberFactor){
				NumberFactor f=(NumberFactor)exp.getSingleFactor();
				Domain may=null,must=null;
				SymbolFactor s=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
				if(f.getDoubleValue()==0){
					may=IntegerDomain.getEmptyDomain();
					must=IntegerDomain.getEmptyDomain();
					condata.addMayDomain(s, may);
					condata.addMustDomain(s, must);	
				}else{
					may=IntegerDomain.getFullDomain();
					must=IntegerDomain.getFullDomain();
					condata.addMayDomain(s, may);
					condata.addMustDomain(s, must);	
				}
				return data;
			}
			
			for(Term t:exp.getTerms()){
				Factor f=t.getSingleFactor();
				if(f instanceof SymbolFactor){
					SymbolFactor s=(SymbolFactor)f;
					Expression temp=new Expression(s).sub(exp);
					Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
					CType type=s.getType();
					
					if(rightdomain!=null){
						Domain may=null,must=null;
						may = Domain.intersect(leftdomain, rightdomain, type);
						if (rightdomain.isCanonical()) {
							must = Domain.intersect(leftdomain, rightdomain, type);
						} else {
							must = Domain.getEmptyDomainFromType(type);
						}
						//zys:2010.4.24 if(p)������Ϊp!=0
						if(temp.toString().equals("0")){
							may=Domain.inverse(may);
							must=Domain.inverse(must);
						}
						condata.addMayDomain(s, may);
						condata.addMustDomain(s, must);
					}
				}
				//����ϵ��Ϊ-1���
				f=t.getMinusSingleFactor();
				if(f instanceof SymbolFactor){
					SymbolFactor s=(SymbolFactor)f;
					Expression temp=new Expression(s).add(exp);
					Domain rightdomain = temp.getDomain(condata.currentvex.getSymDomainset());
					Domain leftdomain=s.getDomainWithoutNull(condata.currentvex.getSymDomainset());
					CType type=s.getType();
					
					if(rightdomain!=null){
						Domain may=null,must=null;
						may = Domain.intersect(leftdomain, rightdomain, type);
						if (rightdomain.isCanonical()) {
							must = Domain.intersect(leftdomain, rightdomain, type);
						} else {
							must = Domain.getEmptyDomainFromType(type);
						}
						condata.addMayDomain(s, may);
						condata.addMustDomain(s, must);
					}
				}
			}
			return data;
		}else{
			node.childrenAccept(this, data);
		}
		return data;
	}
	//chh  ���if���������������������˴˺���
	public Object visit(ASTConstant node, Object data) {
		ConditionData condata = (ConditionData) data;
			Domain may=null,must=null;
			SymbolFactor s=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			if(node.getImage().equals("0")){
				may=IntegerDomain.getEmptyDomain();
				must=IntegerDomain.getEmptyDomain();
				condata.addMayDomain(s, may);
				condata.addMustDomain(s, must);	
			}else{
				may=IntegerDomain.getFullDomain();
				must=IntegerDomain.getFullDomain();
				condata.addMayDomain(s, may);
				condata.addMustDomain(s, must);	
			}
		return data;
	}
	//end
}
