package softtest.domain.c.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.domain.c.symbolic.DoubleFactor;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.IntegerFactor;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Method;
import softtest.interpro.c.Variable;
import softtest.summary.c.MethodFeature;
import softtest.summary.c.MethodPostCondition;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;

/**modified by zys 	2010.7.26
 * ��һЩ���ӱ��ʽ����ʱ����LogicalORExpression��AdditiveExpression....),��ԭ�����ȼ���������ʽ��ֵ��
 * Ȼ�����μ���������ʽ��ֵ����������ֵ֮��Ĳ�����������м��������м���������ļ�����д��ݣ��õ����ձ��ʽ��ֵ��
 * 
 * ����ڼ��������ĳһ���ڳ�������ֵΪNULL��������� 
 * 1�����ȼ���������ı��ʽ���Է�ֹ���а����������޸ģ���if(i*k>0 && i++>5),�����ֵi*k>0���㲻���������������&&�Ҳ���ʽ��
 * 		��ֹ©�����е�i++���㣻
 * 2�������б��ʽ��������Ϻ�Ϊ�������ʽ��һ��ȫ����
 * 3������������Ĵ���ͨ�������쳣�ķ�ʽʵ��
 *  */
public class ExpressionValueVisitor extends CParserVisitorAdapter {
	/**
	 * ASTAdditiveExpression
	 * ASTANDExpression
	 * ASTArgumentExpressionList
	 * ASTAssignmentExpression
	 * ASTCastExpression
	 * ASTConditionalExpression
	 * ASTConstant
	 * ASTConstantExpression
	 * ASTEqualityExpression
	 * ASTExpression
	 * ASTFieldId
	 * ASTInclusiveORExpression
	 * ASTLogicalANDExpression
	 * ASTLogicalORExpression
	 * ASTPostfixExpression
	 * ASTPrimaryExpression
	 * ASTRelationalExpression
	 * ASTShiftExpression
	 * ASTUnaryExpression
	 */	
	public Object visit(ASTAdditiveExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		node.jjtGetChild(0).jjtAccept(this, expdata);
		Expression leftvalue=expdata.value;
		Expression rightvalue=null;
		try{	
			//�����ҽ��з��ż���
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				Node c = node.jjtGetChild(i);
				expdata.value = null;
				c.jjtAccept(this, expdata);
				rightvalue = expdata.value;
				String operator = node.getOperatorType().get(i-1);
				if(leftvalue==null || rightvalue==null)
					throw new MyNullPointerException("AdditiveExpression Value NULL in(.i) "+node.getBeginFileLine());
				if (operator.equals("+")){
					leftvalue=leftvalue.add(rightvalue);
				}else{
					leftvalue=leftvalue.sub(rightvalue);
				}
			}
		}catch(MyNullPointerException e){
			super.visit(node,expdata);
			SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
			expdata.currentvex.addSymbolDomain(sym, DoubleDomain.getFullDomain());
			leftvalue=new Expression(sym);
			expdata.value=leftvalue;
			return data;
		}
		
		expdata.value=leftvalue;
		return data;
	}
	
	public Object visit(ASTANDExpression node, Object data) {
		return dealBinaryBitOperation(node,data,"&");
	}
	
	public Object visit(ASTArgumentExpressionList node, Object data) {
		//��ASTPostfixExpression��ͳһ����
		return super.visit(node, data);
	}
	//modified by zhouhb
	public Object visit(ASTInitDeclarator node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		ASTDeclarator declarator = null;
		if(node.jjtGetChild(0) instanceof ASTDeclarator){
			declarator=(ASTDeclarator)node.jjtGetChild(0);
		}else if(node.jjtGetChild(1) instanceof ASTDeclarator){
			declarator=(ASTDeclarator)node.jjtGetChild(1);
		}
		NameDeclaration decl=declarator.getFinalDirectDeclarator().getDecl();
		//������ṹ�������Ϣ
		//add by zhouhb 2010/8/19
		//zys 2011.6.23	Ϊʲô�������� File *fp=null;�Ͳ��ܴ����ˣ�
//		if(!Config.Field){
//			if(decl!=null){
//				if(decl.getType() instanceof CType_Array && (((CType_Array)decl.getType()).getOriginaltype() instanceof CType_Struct||((CType_Array)decl.getType()).getOriginaltype() instanceof CType_Pointer)||
//						decl.getType() instanceof CType_Struct||decl.getType() instanceof CType_Pointer && (((CType_Pointer)decl.getType()).getOriginaltype() instanceof CType_Struct||((CType_Pointer)decl.getType()).getOriginaltype() instanceof CType_Typedef||((CType_Pointer)decl.getType()).getOriginaltype() instanceof CType_Pointer))
//					return data;	
//			}
//		}
		if(node.jjtGetNumChildren()==1){
			if(node.getType() instanceof CType_Pointer)	{
				SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
				PointerDomain p=new PointerDomain();
				expdata.currentvex.addSymbolDomain(sym, p);
				expdata.value=new Expression(sym);
			}else if(node.getType() instanceof CType_Array){
				SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
				PointerDomain p=new PointerDomain();
				//modified by zhouhb 2010/7/23
				//�޸��������ʼ��ʱ����ָ�������ΪNULL
				p.setValue(PointerValue.NOTNULL);
				//modified by zhouhb 2010/6/22
				//����������ȫ�ֱ�����ʼ������Ĺ��� eg.char[a]
				ASTConstantExpression constant=(ASTConstantExpression)node.getFirstChildOfType(ASTConstantExpression.class);
				//�ж��Ƿ�Ϊchar a[]����δ����ά�ȵ���������
				if(constant!=null){
					constant.jjtAccept(this, data);	
					if(expdata.value!=null)
					{
						Domain dd=expdata.value.getDomain(expdata.currentvex.getSymDomainset());					
						IntegerDomain size=IntegerDomain.castToIntegerDomain(dd);
						//�ж�����ά���Ƿ�Ϊ��������
						if(size!=null){
						int arraySize=(int)size.getMin();	
						p.offsetRange=new IntegerDomain(0,arraySize*((CType_Array)node.getType()).getOriginaltype().getSize()-1);
						//p.AllocType=CType_AllocType.stackType;
						if(p.Type.contains(CType_AllocType.NotNull)){
							p.Type.remove(CType_AllocType.NotNull);
						}
						p.Type.add(CType_AllocType.stackType);
						}
					}
				}
				expdata.currentvex.addSymbolDomain(sym, p);
				expdata.value=new Expression(sym);
				expdata.currentvex.addValue((VariableNameDeclaration)decl, expdata.value);
			}
				return data;
		}
		if(decl instanceof VariableNameDeclaration){
			VariableNameDeclaration v=(VariableNameDeclaration)decl;
			node.jjtGetChild(1).jjtAccept(this, expdata);
			if(node.jjtGetChild(1) instanceof ASTInitializer){
				if(node.getType() instanceof CType_Array){		
					//���������ʼ������eg.int a[5]={3,3,4}
					//modified by zhouhb 2010/7/21
					
					if(node.containsChildOfType(ASTInitializerList.class)){
						int num=((ASTInitializerList)node.getFirstChildOfType(ASTInitializerList.class)).jjtGetNumChildren();
						PointerDomain p=new PointerDomain();
						p.offsetRange=new IntegerDomain(0,((CType_Array)node.getType()).getDimSize()-1);
						p.allocRange=new IntegerDomain(num,num);
						//p.AllocType=CType_AllocType.stackType;
						if(num>0)
						{
							p.setValue(PointerValue.NOTNULL);
						}
						if(p.Type.contains(CType_AllocType.NotNull)){
							p.Type.remove(CType_AllocType.NotNull);
						}
						p.Type.add(CType_AllocType.stackType);
						SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
						expdata.value=new Expression(sym);
						expdata.currentvex.addSymbolDomain(sym, p);
						expdata.currentvex.addValue(v, expdata.value);
						Expression exp=expdata.value;
						
						ExpressionVistorData expdata1=expdata;
						CType_Array typeArr=(CType_Array)node.getType();
						//ֻ����һά����
						for(int i=0;i<num&&i<=Config.MaxArray&&typeArr.getDimSize()==typeArr.getMemNum();i++)
						{
							String arrImage=v.getImage()+"["+i+"]";
							NameDeclaration decl0=Search.searchInVariableAndMethodUpward(arrImage, node.getScope());
							if(decl0!=null&&decl0 instanceof VariableNameDeclaration)
							{
								VariableNameDeclaration arrV=(VariableNameDeclaration)decl0;
								ASTInitializerList aList=(ASTInitializerList)node.getFirstChildOfType(ASTInitializerList.class);
								((AbstractExpression)aList.jjtGetChild(i).jjtGetChild(0)).jjtAccept(this, expdata1);
								
								if(expdata1.value==null)
									expdata1.value=new Expression(SymbolFactor.genSymbol(arrV.getType(), arrV.getImage()));
								expdata1.currentvex.addValue(arrV, expdata1.value);									
							}							
						}
						expdata.value=exp;
					}else{
						Expression e=expdata.value;
						PointerDomain p=(PointerDomain)e.getDomain(expdata.currentvex.getSymDomainset());
						if(p!=null){
							p.offsetRange=new IntegerDomain(0,((CType_Array)node.getType()).getDimSize()-1);
							//p.AllocType=CType_AllocType.stackType;
							if(p.Type.contains(CType_AllocType.NotNull)){
								p.Type.remove(CType_AllocType.NotNull);
							}
							p.Type.add(CType_AllocType.stackType);
							SymbolFactor temp=(SymbolFactor)e.getSingleFactor();
							expdata.currentvex.addSymbolDomain(temp, p);
						}
					}
				}//add by zhouhb 2010/8/16
				 //�޸��˿�ָ�븳ֵʹ��
				else if(node.getType() instanceof CType_Pointer){
					if(node.containsChildOfType(ASTConstant.class)&&!node.containsParentOfType(ASTEqualityExpression.class)&&((ASTConstant)node.getFirstChildOfType(ASTConstant.class)).getImage().equals("0")){
						//�����ָ��NULL
						SymbolFactor p=SymbolFactor.genSymbol(node.getType());
						PointerDomain pDomain=new PointerDomain();
						pDomain.offsetRange.intervals.clear();
						//pDomain.AllocType=CType_AllocType.Null;
						pDomain.Type.add(CType_AllocType.Null);
						pDomain.setValue(PointerValue.NULL);
						expdata.currentvex.addSymbolDomain(p, pDomain);
						expdata.value=new Expression(p);
					}
				}
				if(expdata.sideeffect && expdata.currentvex != null) {
					if(expdata.value==null)
						expdata.value=new Expression(SymbolFactor.genSymbol(decl.getType(), decl.getImage()));
					expdata.currentvex.addValue(v, expdata.value);				
				}
			}
		}
		return data;
	}
	
	public Object visit(ASTAssignmentExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		//modified by zhouhb
		SimpleNode postfix=(SimpleNode)node.jjtGetParent().jjtGetParent();	
		if(postfix.getImage().equals("calloc")&&!(node.containsChildOfType(ASTConstant.class)))
			return data;
		if(node.jjtGetNumChildren()==1){
			node.jjtGetChild(0).jjtAccept(this, expdata);
		} 
		//���Ӷ� func(int,a)��������һ����ʵ�ǲ������ͣ��ڶ���������ʵ�ε�֧��
		else if(node.jjtGetNumChildren()==2) {
			node.jjtGetChild(1).jjtAccept(this, expdata);
		}else {
			Expression leftvalue,rightvalue;
			SimpleNode firstchild=(SimpleNode)node.jjtGetChild(0);
			SimpleNode secondchild=(SimpleNode)node.jjtGetChild(1);
			SimpleNode thirdchild=(SimpleNode)node.jjtGetChild(2);
			thirdchild.jjtAccept(this, expdata);
			rightvalue=expdata.value;
			if(rightvalue==null)
				return data;
			if(secondchild.getOperatorType().get(0).equals("=")){
				firstchild.jjtAccept(this, expdata);
				//add by zhouhb 2010/8/16
				 //�޸��˿�ָ�븳ֵʹ��
				if(node.getType() instanceof CType_Pointer){
					//add by zhouhb 2010/11/18
					//����ָ������
					ASTPrimaryExpression pri=(ASTPrimaryExpression)firstchild.getFirstChildOfType(ASTPrimaryExpression.class);
					if(pri.getType() instanceof CType_Array){
						
						/**dongyk 2012.4.11 ���Ӷ�ָ������(������Ԫ����ָ������)�Ĵ���*/
						IntegerDomain domain=Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
						//ASTConstant ac=(ASTConstant)thirdchild.getFirstChildOfType(ASTConstant.class);
						//if(ac!=null)
						//{
							//String image=ac.getImage();
							if(domain!=null)
							{
								PointerDomain pDomain=new PointerDomain();
								if(domain.getMin()<1)
								{
									pDomain.offsetRange.intervals.clear();
									//pDomain.AllocType=CType_AllocType.Null;
									pDomain.Type.add(CType_AllocType.Null);
									pDomain.setValue(PointerValue.NULL);
								}
								if(domain.getMin()>0)
								{
									pDomain.Type.add(CType_AllocType.NotNull);
									pDomain.setValue(PointerValue.NOTNULL);
								}
								//������������ʽ�����ͱ��ʽ��ת��Ϊ�����ţ���֪��Ϊʲô��ָ��������������������
								if(expdata.value==null||expdata.value.getSingleFactor()==null||
										(expdata.value.getSingleFactor()!=null&&
												expdata.value.getSingleFactor() instanceof IntegerFactor)){
									expdata.value=new Expression(SymbolFactor.genSymbol(node.getType()));
								}
								expdata.currentvex.addSymbolDomain((SymbolFactor)expdata.value.getSingleFactor(), pDomain);
								rightvalue=expdata.value;
							}
						//}
						
						//return data;
					}
					if(node.containsChildOfType(ASTConstant.class)&&
							!node.containsParentOfType(ASTEqualityExpression.class)){
						
						IntegerDomain domain=Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
											
						ASTConstant ac=(ASTConstant)node.getFirstChildOfType(ASTConstant.class);
						String image=ac.getImage();
						if(domain!=null&&domain.getMin()<1&&image.equals("0"))
						{
						
							PointerDomain pDomain=new PointerDomain();
							pDomain.offsetRange.intervals.clear();
							//pDomain.AllocType=CType_AllocType.Null;
							pDomain.Type.add(CType_AllocType.Null);
							pDomain.setValue(PointerValue.NULL);
							//������������ʽ�����ͱ��ʽ��ת��Ϊ�����ţ���֪��Ϊʲô��ָ��������������������
							if(expdata.value==null||expdata.value.getSingleFactor()==null||
									(expdata.value.getSingleFactor()!=null&&
											expdata.value.getSingleFactor() instanceof IntegerFactor)){
								expdata.value=new Expression(SymbolFactor.genSymbol(node.getType()));
							}
							expdata.currentvex.addSymbolDomain((SymbolFactor)expdata.value.getSingleFactor(), pDomain);
							rightvalue=expdata.value;
						}
					}
				}
				expdata.value=rightvalue;
			}else if(secondchild.getOperatorType().get(0).equals("*=")){
				firstchild.jjtAccept(this, expdata);
				leftvalue=expdata.value;
				if(leftvalue!=null)
				{
					expdata.value=leftvalue.mul(rightvalue);
				}
			}else if(secondchild.getOperatorType().get(0).equals("/=")){
				firstchild.jjtAccept(this, expdata);
				leftvalue=expdata.value;
				if(leftvalue!=null)
					expdata.value=leftvalue.div(rightvalue);
			}else if(secondchild.getOperatorType().get(0).equals("+=")){
				firstchild.jjtAccept(this, expdata);
				leftvalue=expdata.value;
				if(leftvalue!=null){
					expdata.value=leftvalue.add(rightvalue);
				}
			}else if(secondchild.getOperatorType().get(0).equals("-=")){
				firstchild.jjtAccept(this, expdata);
				leftvalue=expdata.value;
				if(leftvalue!=null){
					expdata.value=leftvalue.sub(rightvalue);
				}
			}else {
				node.jjtGetChild(0).jjtAccept(this, expdata);
				leftvalue=expdata.value;
				if(leftvalue!=null){
					IntegerDomain i1 = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
					IntegerDomain i2 = Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
					// �������ȷ�������ֵ��ֱ�Ӳ���һ������ȡֵδ���ķ���
					if (i1!=null&&i2!=null&&i1.isCanonical() && i2.isCanonical()) {
						long temp = 0;
						if(secondchild.getOperatorType().get(0).equals("&=")){
							temp=i1.getMin()& i2.getMin();
						}else if(secondchild.getOperatorType().get(0).equals("|=")){
							temp=i1.getMin()| i2.getMin();
						}else if(secondchild.getOperatorType().get(0).equals("^=")){
							temp=i1.getMin() ^ i2.getMin();
						}else if(secondchild.getOperatorType().get(0).equals(">>=")){
							temp=i1.getMin() >> i2.getMin();
						}else if(secondchild.getOperatorType().get(0).equals("<<=")){
							temp=i1.getMin() << i2.getMin();
						}else if(secondchild.getOperatorType().get(0).equals("%=")){
							temp=i1.getMin() % i2.getMin();
						}
						expdata.value = new Expression(new IntegerFactor(temp));
					} else {
						expdata.value = new Expression(SymbolFactor.genSymbol(CType_BaseType.getBaseType("int")));
					}
				}
				
			}
			ASTPrimaryExpression p = (ASTPrimaryExpression) firstchild.getSingleChildofType(ASTPrimaryExpression.class);
			if(p!=null){
				VariableNameDeclaration v = p.getVariableDecl();
				if (v != null && expdata.sideeffect && expdata.currentvex != null&&expdata.value!=null) {
					expdata.currentvex.addValue(v, expdata.value);
				}
			}
			if(Config.Field){
				//����ʵ�ֽṹ���Ա���еļ���		
				ASTPostfixExpression po=(ASTPostfixExpression) firstchild.getSingleChildofType(ASTPostfixExpression.class);
				if(po!=null){
					
					ASTPrimaryExpression pri=(ASTPrimaryExpression)po.getFirstDirectChildOfType(ASTPrimaryExpression.class);
					VariableNameDeclaration vPri=pri.getVariableDecl();
					
					if(vPri!=null&&(vPri.getType() instanceof CType_Array||vPri.getType() instanceof CType_Pointer))
					{
						for(int i=0;i<expdata.arrayIndex.size();i++)
						{
							int vIndex=expdata.arrayIndex.get(i).intValue();
							String image=pri.getImage()+"["+vIndex+"]";
							NameDeclaration declArrar=Search.searchInVariableAndMethodUpward(image, node.getScope());
							if(declArrar!=null&& expdata.sideeffect && expdata.currentvex != null&&expdata.value!=null)
							{
								VariableNameDeclaration vArray=(VariableNameDeclaration)declArrar;
								Expression arrExpression=expdata.currentvex.getValue(vArray);
								if(arrExpression==null)
								{
									expdata.currentvex.addValue(vArray, expdata.value);
								}
								else
								{
									Domain arrDomain=arrExpression.getDomain(expdata.currentvex.getSymDomainset());
									Domain rightDomain=expdata.value.getDomain(expdata.currentvex.getSymDomainset());
									Domain newDomain=Domain.union(arrDomain, rightDomain, vArray.getType());
									
									SymbolFactor sym=SymbolFactor.genSymbol(vArray.getType());
									expdata.currentvex.addSymbolDomain(sym, newDomain);								
									expdata.currentvex.getValueSet().addValue(vArray, new Expression(sym));
								}
							}
							
						}
					}
				/**							
					VariableNameDeclaration v = po.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null&&expdata.value!=null) {
						expdata.currentvex.addValue(v, expdata.value);
					}*/
				}
				//����ʵ�ֶ༶ָ���Ա���еļ���
				ASTUnaryExpression un=(ASTUnaryExpression) firstchild.getSingleChildofType(ASTUnaryExpression.class);
				if(un!=null){
					VariableNameDeclaration v = un.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null&&expdata.value!=null) {
						expdata.currentvex.addValue(v, expdata.value);
					}
				}
			}
		}
		return data;
	}
	
	public Object visit(ASTCastExpression node, Object data) {
		super.visit(node, data);
		//�����µı��뻷����NULL����Ϊ0����ʱΪ(void *)0�����޸�
		//add by zhouhb
		//����"=="���ʽ�Ĵ���ʱ��������ָ���Ӧ����
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		//ǿ������ת�����ܻ��������а汾���ܷ��������͵Ĵ���������Ϣ���ڴ�����
		//add by zhouhb 2010/8/30
		ASTTypeName type=(ASTTypeName)node.getFirstChildOfType(ASTTypeName.class);//
		if(type!=null){
			CType cast=type.getType();
			if(cast instanceof CType_Pointer&&!(((CType_Pointer)cast).getOriginaltype() instanceof CType_BaseType))
			{
				return data;
			}
		}
		
		if(node.jjtGetNumChildren()==2
				&&node.containsChildOfType(ASTConstant.class)
				&&!node.containsParentOfType(ASTEqualityExpression.class)){
			//�����ָ��NULL
			Expression leftvalue=expdata.value;
			IntegerDomain domain=null;
			if(leftvalue!=null){
				domain = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
			}	
			if(domain==null||(domain!=null&&domain.getMin()<1&&
					((ASTConstant)node.getFirstChildOfType(ASTConstant.class)).getImage().equals("0")&&
					(((ASTTypeName)node.getFirstChildOfType(ASTTypeName.class)).getType() instanceof CType_Pointer))){
				SymbolFactor p=SymbolFactor.genSymbol(node.getType());
				PointerDomain pDomain=new PointerDomain();
				pDomain.offsetRange.intervals.clear();
				//pDomain.AllocType=CType_AllocType.Null;
				pDomain.Type.add(CType_AllocType.Null);
				pDomain.setValue(PointerValue.NULL);
				expdata.currentvex.addSymbolDomain(p, pDomain);
				expdata.value=new Expression(p);
				}
		}
		return data;
	}
	
	public Object visit(ASTConditionalExpression node, Object data) {
		super.visit(node, data);
		//����void ConditionalExpression() #ConditionalExpression(>1): {}���Բ������ɵ�֧�������Դ���������
		if (node.jjtGetNumChildren() == 1) {
			throw new RuntimeException("ASTConditionalExpression can't generate single child");
			//liuli:����������ĵڶ������ʽ����Ϊ�գ���c = (++a ? : b);�������
		}else if(node.jjtGetNumChildren() == 2){
			Expression firstvalue,thirdvalue;
			ExpressionVistorData expdata=(ExpressionVistorData)data;
			
			SimpleNode firstchild=(SimpleNode)node.jjtGetChild(0);
			firstchild.jjtAccept(this, expdata);
			firstvalue=expdata.value;
			expdata.value = null;		
			SimpleNode thirdchild = (SimpleNode) node.jjtGetChild(1);
			thirdchild.jjtAccept(this, expdata);
			thirdvalue = expdata.value;
			
			IntegerDomain i = Domain.castToIntegerDomain(firstvalue.getDomain(expdata.currentvex.getSymDomainset()));
			if (i != null ) {
				if (i.isCanonical()&& i.getMin() == 0) {
					expdata.value = thirdvalue;
				} else if(!i.contains(0)){
					expdata.value = new Expression(1);//���ڶ�������Ϊ�գ�ȱʡֵΪ1
				}
				return data;
			}
		
			//����һ���µķ��ţ���Ϊ���ߵĲ�
			CType type=node.getType();
			SymbolFactor sym=SymbolFactor.genSymbol(type);
			if(type!=null){
				Domain d1=Domain.castToType(new IntegerDomain(1,1), type);
				Domain d2=Domain.castToType(thirdvalue.getDomain(expdata.currentvex.getSymDomainset()), type);
				expdata.currentvex.addSymbolDomain(sym, Domain.union(d1, d2, type));							
			}
			expdata.value=new Expression(sym);
		}else {
			Expression firstvalue,secondvalue,thirdvalue;
			ExpressionVistorData expdata=(ExpressionVistorData)data;
			
			SimpleNode firstchild=(SimpleNode)node.jjtGetChild(0);
			firstchild.jjtAccept(this, expdata);
			firstvalue=expdata.value;
			expdata.value = null;
			
			SimpleNode secondchild=(SimpleNode)node.jjtGetChild(1);
			secondchild.jjtAccept(this, expdata);
			secondvalue=expdata.value;
			expdata.value = null;
			
			SimpleNode thirdchild = (SimpleNode) node.jjtGetChild(2);
			thirdchild.jjtAccept(this, expdata);
			thirdvalue = expdata.value;

			IntegerDomain i=null;
			Domain d=null;
			if(firstvalue!=null&&expdata!=null&&expdata.currentvex!=null)
			{
				d=firstvalue.getDomain(expdata.currentvex.getSymDomainset());			
			}
			if(d!=null)
				i= Domain.castToIntegerDomain(d);
			if (i != null ) {
				if (i.isCanonical()&& i.getMin() == 0) {
					expdata.value = thirdvalue;
				} else if(i.isCanonical()&& i.getMax() == 1){
					expdata.value = secondvalue;
				}else
				{
					expdata.value=thirdvalue;
					
				}
				return data;
			}
		
			//����һ���µķ��ţ���Ϊ���ߵĲ�
			CType type=node.getType();
			SymbolFactor sym=SymbolFactor.genSymbol(type);
			if(type!=null && secondvalue!=null && thirdvalue!=null){
				Domain d1=Domain.castToType(secondvalue.getDomain(expdata.currentvex.getSymDomainset()), type);
				Domain d2=Domain.castToType(thirdvalue.getDomain(expdata.currentvex.getSymDomainset()), type);
				expdata.currentvex.addSymbolDomain(sym, Domain.union(d1, d2, type));
			}
			expdata.value=new Expression(sym);
		}
		return data;
	}
	
	public Object visit(ASTConstant node, Object data) {
		ExpressionVistorData expdata = (ExpressionVistorData) data;
		String image = node.getImage();
		if (image.startsWith("\"") || image.startsWith("L\"") 
				||image.equals("__FUNCTION__") ||image.equals("__PRETTY_FUNCTION__") ||image.equals("__func__")) {//���Ӷ�L"abcd"��ʽ���ַ�����ֵ��ʽ�Ĵ���
			PointerDomain p=new PointerDomain();
			p.offsetRange=new IntegerDomain(0,image.length()-2);
			p.allocRange=new IntegerDomain(image.length()-1,image.length()-1);
			//p.AllocType=CType_AllocType.staticType;
			if(p.Type.contains(CType_AllocType.NotNull)){
				p.Type.remove(CType_AllocType.NotNull);
			}
			p.Type.add(CType_AllocType.staticType);
			p.setElementtype(CType_BaseType.getBaseType("char"));
			p.setLength(new Expression(image.length()+1));
			p.setValue(PointerValue.NOTNULL);
			SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
			expdata.currentvex.addSymbolDomain(sym, p);
			expdata.value=new Expression(sym);
		}else if (( image.startsWith("L\'") ||  image.startsWith("\'") )&& !(image.startsWith("\'\\x")) ) {
			if (image.length() <= 2) {
				throw new RuntimeException("This is not a legal character");
			}
			if( image.startsWith("L\'")){
				image = image.substring(1, image.length());
			}
			//liuli:2010.7.23��������int i='DpV!';���
			if(image.length()>3){
				if(image.startsWith("\'") && image.endsWith("\'")){
					if(!image.startsWith("\'\\")){
						SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
						IntegerDomain p=new IntegerDomain();
						expdata.currentvex.addSymbolDomain(sym, p);
						expdata.value=new Expression(sym);
						return data;
					}
				}else{
					throw new RuntimeException("This is not a legal character");
				}
			}
			int count = 1;
			int secondChar = image.charAt(count++);
			int nextChar = image.charAt(count++);
			char value = (char) secondChar;
			if (secondChar == '\\') {
				switch (nextChar) {
				case 'b':
					value = '\b';
					break;
				case 't':
					value = '\t';
					break;
				case 'n':
					value = '\n';
					break;
				case 'f':
					value = '\f';
					break;
				case 'r':
					value = '\r';
					break;
				case '\"':
					value = '\"';
					break;
				case '\'':
					value = '\'';
					break;
				case '\\':
					value = '\\';
					break;
				case 'a':						
					break;
				case 'v':					
					break;
				case 'e':
					break;
				case '?':					
					break;
				default: // octal (well-formed: ended by a ' )
					if ('0' <= nextChar && nextChar <= '7') {
						int number = nextChar - '0';
						if (count >= image.length()) {
							throw new RuntimeException("This is not a legal character");
						}
						nextChar = image.charAt(count);
						if (nextChar != '\'') {
							count++;
							if (!('0' <= nextChar && nextChar <= '7')) {
								throw new RuntimeException("This is not a legal character");
							}
							number = (number * 8) + nextChar - '0';
							if (count >= image.length()) {
								throw new RuntimeException("This is not a legal character");
							}
							nextChar = image.charAt(count);
							if (nextChar != '\'') {
								count++;
								if (!('0' <= nextChar && nextChar <= '7')) {
									throw new RuntimeException("This is not a legal character");
								}
								number = (number * 8) + nextChar - '0';
							}
						}
						value = (char) number;
					} else {
						throw new RuntimeException("This is not a legal character");
					}
				}
				if (count >= image.length()) {
					System.out.println("cnt:" + count + "  " + image + image.length());
					throw new RuntimeException("This is not a legal character");
				}
				nextChar = image.charAt(count++);
			}
			if (nextChar != '\'') {
				throw new RuntimeException("This is not a legal character");
			}
			if(secondChar == '\\'){
				int value_temp = image.charAt(count-2);
				switch(value_temp){
				case 'a':
					expdata.value=new Expression(new IntegerFactor(007));
					break;
				case 'v':
					expdata.value=new Expression(new IntegerFactor(011));
					break;
				case '?':
					expdata.value=new Expression(new IntegerFactor(063));
					break;
				default:
					expdata.value=new Expression(new IntegerFactor(value));
				}
			}else{
				expdata.value=new Expression(new IntegerFactor(value));
			}
			

		} else {
			if(image.startsWith("\'\\x")){
				image = image.replace("\'", "");
				image = image.replace("\\x", "0x");
			}
			boolean isInteger = false;
			
			if (image.endsWith("l") || image.endsWith("L")){
				image = image.substring(0, image.length() - 1);//��L��β
				if(image.endsWith("l") || image.endsWith("L")){
					image = image.substring(0, image.length() - 1);//��LL��β
					if(image.endsWith("u") || image.endsWith("U")){
						image = image.substring(0, image.length() - 1);//��ULL��β
					}
				}else if(image.endsWith("u") || image.endsWith("U")){
					image = image.substring(0, image.length() - 1);//��UL��β
				}
			}else if(image.endsWith("u") || image.endsWith("U")){
				image = image.substring(0, image.length() - 1);//��U��β
				if(image.endsWith("l") || image.endsWith("L")){
					image = image.substring(0, image.length() - 1);//��LU��β
					if(image.endsWith("l") || image.endsWith("L")){
						image = image.substring(0, image.length() - 1);//��LLU��β
					}
				}
			}				
			
			char[] source = image.toCharArray();
			int length = source.length;
			long intValue = 0;
			double doubleValue = 0;
			long computeValue = 0L;
			try {
				if (source[0] == '0') {
					if (length == 1) {
						computeValue = 0;
					} else {
						final int shift, radix;
						int j;
						if ((source[1] == 'x') || (source[1] == 'X')) {
							shift = 4;
							j = 2;
							radix = 16;
						} else {
							shift = 3;
							j = 1;
							radix = 8;
						}
						while (source[j] == '0') {
							j++; // jump over redondant zero
							if (j == length) { // watch for 000000000000000000
								computeValue = 0;
								break;
							}
						}
						while (j < length) {
							int digitValue = 0;
							if (radix == 8) {
								if ('0' <= source[j] && source[j] <= '7') {
									digitValue = source[j++] - '0';
								} else {
									throw new RuntimeException(
											"This is not a legal integer");
								}
							} else {
								if ('0' <= source[j] && source[j] <= '9') {
									digitValue = source[j++] - '0';
								} else if ('a' <= source[j] && source[j] <= 'f') {
									digitValue = source[j++] - 'a' + 10;
								} else if ('A' <= source[j] && source[j] <= 'F') {
									digitValue = source[j++] - 'A' + 10;
								}else if (source[j] == 'u' ||source[j] == 'l' || source[j] == 'U' || source[j] == 'L') {
									j++;
									continue;
								}  else {
									throw new RuntimeException(
											"This is not a legal integer");
								}
							}
							computeValue = (computeValue << shift) | digitValue;

						}
					}
				} else { // -----------regular case : radix = 10-----------
					for (int i = 0; i < length; i++) {
						int digitValue;
						if ('0' <= source[i] && source[i] <= '9') {
							digitValue = source[i] - '0';
						} else if (source[i] == 'u' ||source[i] == 'l' || source[i] == 'U' || source[i] == 'L') {
							continue;
						} else {
							throw new RuntimeException(
									"This is not a legal integer");
						}
						computeValue = 10 * computeValue + digitValue;
					}
				}
				intValue =computeValue;
				isInteger = true;
			} catch (RuntimeException e) {
			}
			
			if (isInteger) {
				expdata.value=new Expression(new IntegerFactor(intValue));
			} else {
				doubleValue = Double.valueOf(image);
				expdata.value=new Expression(new DoubleFactor(doubleValue));
			}
		}
		return data;
	}
	
	public Object visit(ASTConstantExpression node, Object data) {
		super.visit(node, data);
		return data;
	}
	
	public Object visit(ASTEqualityExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		node.jjtGetChild(0).jjtAccept(this, expdata);
		Expression leftvalue=expdata.value;
		Expression rightvalue=null;
		try{
			//���δ����ҽ��з��ż���
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				Node c = node.jjtGetChild(i);
				expdata.value = null;
				c.jjtAccept(this, expdata);
				rightvalue = expdata.value;
				if(leftvalue==null || rightvalue==null){
					throw new MyNullPointerException("EqualityExpression Value NULL in(.i) "+node.getBeginFileLine());
				}
				String operator = node.getOperatorType().get(i-1);
				if(leftvalue.isValueEqual(rightvalue,expdata.currentvex.getSymDomainset())){
					if (operator.equals("==")){
						leftvalue=new Expression(1);
					}else{
						leftvalue=new Expression(0);
					}
				}else if(leftvalue.isValueMustNotEqual(rightvalue,expdata.currentvex.getSymDomainset())){
					if (operator.equals("!=")){
						leftvalue=new Expression(1);
					}else{
						leftvalue=new Expression(0);
					}
				}else{
					//ͨ���쳣������������
					throw new MyNullPointerException("EqualityExpression Value [0,1] in(.i) "+node.getBeginFileLine());
				}
			}
		}catch(MyNullPointerException e){
			super.visit(node,expdata);
			SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0,1));
			leftvalue=new Expression(sym);
			expdata.value=leftvalue;
			return data;
		}
		expdata.value=leftvalue;
		return data;
	}
	
	public Object visit(ASTExclusiveORExpression node, Object data) {
		return dealBinaryBitOperation(node,data,"^");
	}
	
	public Object visit(ASTExpression node, Object data) {
		//zys:2010.8.11	�������жϽڵ㣬����������﷨��������ޣ����ٵݹ�������ӽ��
		int depth=node.getDescendantDepth();
		if(depth<=Config.MAXASTTREEDEPTH){
			super.visit(node, data);
		}else{
			//Logger.getRootLogger().info("ExpressionValueVisitor: ASTExpression in .i "+node.getBeginFileLine()+" depth="+depth+" overflow");
		}
		//super.visit(node, data);
		return data;
	}
	
	public Object visit(ASTFieldId node, Object data) {
		//��ASTPostfixExpression��ͳһ����
		return super.visit(node, data);
	}
	
	public Object visit(ASTInclusiveORExpression node, Object data) {
		return dealBinaryBitOperation(node,data,"|");
	}
	
	public Object visit(ASTLogicalANDExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		node.jjtGetChild(0).jjtAccept(this, expdata);
		Expression leftvalue=expdata.value;
		Expression rightvalue=null;
		DoubleDomain d1=null;
		DoubleDomain d2=null;
		try{
			//�����ҽ��з��ż���
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				Node c = node.jjtGetChild(i);
				expdata.value = null;
				c.jjtAccept(this, expdata);
				rightvalue = expdata.value;
				
				if(leftvalue==null || rightvalue==null){
					throw new MyNullPointerException("LogicalANDExpression Value NULL in(.i) "+node.getBeginFileLine());
				}
				//zys:2010.8.9	����&&�Ķ�·���ԣ��������ʽֵΪ0,���ټ����Ҳ���ʽ��ֵ
				d1=Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
				if(d1!=null && d1.isCanonical() && d1.getMin()==0){
					leftvalue=new Expression(0);
					break;
				}
				
				d2=Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
				if(d1!=null&&d2!=null){
					if(!d1.contains(0)&&!d2.contains(0)){
						leftvalue=new Expression(1);
						continue;
					}else if(d1.isCanonical()&&d1.getMin()==0){
						leftvalue=new Expression(0);
						continue;
					}else if(d2.isCanonical()&&d2.getMin()==0){
						leftvalue=new Expression(0);
						continue;
					}
				}else{
					throw new MyNullPointerException("LogicalANDExpression Domain NULL in(.i) "+node.getBeginFileLine());
				}
			}
		}catch(MyNullPointerException e){
			super.visit(node,expdata);
			SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0,1));
			leftvalue=new Expression(sym);
			expdata.value=leftvalue;
			return data;
		}
			
		expdata.value=leftvalue;
		return data;
	}
	
	public Object visit(ASTLogicalORExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		node.jjtGetChild(0).jjtAccept(this, expdata);
		
		Expression leftvalue=expdata.value;
		DoubleDomain d1=null;
		Expression rightvalue=null;
		DoubleDomain d2=null;
		try{
			//���ݱ��ʽ����ֵ�����δ����ҽ��з��ż��㣬�����ݱ��ʽ�ķ��Ž��м���
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				Node c = node.jjtGetChild(i);
				expdata.value = null;
				c.jjtAccept(this, expdata);
				rightvalue = expdata.value;
				
				if(leftvalue==null || rightvalue==null){
					throw new MyNullPointerException("LogicalORExpression Value NULL in(.i) "+node.getBeginFileLine());
				}
				//zys:2010.8.9	����||�Ķ�·���ԣ��������ʽֵΪ1,���ټ����Ҳ���ʽ��ֵ
				d1=Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
				if(d1!=null && !d1.contains(0)){
					leftvalue=new Expression(1);
					break;
				}
				
				d2=Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
				if(d1!=null&&d2!=null){
					if(!d1.contains(0)||!d2.contains(0)){
						leftvalue=new Expression(1);
						break;
					}else if(d1.isCanonical()&&d1.getMin()==0&&d2.isCanonical()&&d2.getMin()==0){
						leftvalue=new Expression(0);
						continue;
					}
				}else{
					throw new MyNullPointerException("LogicalORExpression Domain NULL in(.i) "+node.getBeginFileLine());
				}
			}
		}catch(MyNullPointerException e){
			super.visit(node,expdata);
			SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0,1));
			leftvalue=new Expression(sym);
			expdata.value=leftvalue;
			return data;
		}
		
		expdata.value=leftvalue;
		return data;
	}
	
	public Object visit(ASTMultiplicativeExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		node.jjtGetChild(0).jjtAccept(this, expdata);
		Expression leftvalue=expdata.value;
		Expression rightvalue=null;
		try{	
			//�����ҽ��з��ż���
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				Node c = node.jjtGetChild(i);
				expdata.value = null;
				c.jjtAccept(this, expdata);
				rightvalue = expdata.value;
				String operator = node.getOperatorType().get(i-1);
				if(leftvalue==null || rightvalue==null)
					throw new MyNullPointerException("MultiplicativeExpression Value NULL in(.i) "+node.getBeginFileLine());
				if (operator.equals("*")){
					//2010.12.03 liuli:��expression�ó��ȹ���ʱ���ᵼ�¼���������ѭ��
					if(rightvalue.getTerms().size()*leftvalue.getTerms().size() >1000){
						IntegerDomain i1=Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));	
						IntegerDomain i2=Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));		
						if(i1!=null&&i2!=null&&i1.isCanonical()&&i2.isCanonical()){
							leftvalue=new Expression(i1.getMin()*i2.getMin());
						}else{
							leftvalue=new Expression(SymbolFactor.genSymbol(node.getType()));
						}
					}else{
						leftvalue=leftvalue.mul(rightvalue);
					}	
				}else if (operator.equals("/")){
					if(rightvalue.getTerms().size()*leftvalue.getTerms().size() >1000){
						IntegerDomain i1=Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));	
						IntegerDomain i2=Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));		
						if(i1!=null&&i2!=null&&i1.isCanonical()&&i2.isCanonical()){
							if(i2.getMin()==0)
							{
								leftvalue=new Expression(IntegerDomain.DEFAULT_MAX);
							}else
							{
								leftvalue=new Expression(i1.getMin()/i2.getMin());
							}
						}else{
							leftvalue=new Expression(SymbolFactor.genSymbol(node.getType()));
						}
					}else{
						leftvalue=leftvalue.div(rightvalue);
					}					
				}else{//%
					IntegerDomain i1=Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));	
					IntegerDomain i2=Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));		
					if(i1!=null&&i2!=null&&i1.isCanonical()&&i2.isCanonical()){
						if(i2.getMin()==0)
						{
							leftvalue=new Expression(IntegerDomain.DEFAULT_MAX);
						}else
						{
							leftvalue=new Expression(i1.getMin()%i2.getMin());
						}
					}else{
						leftvalue=new Expression(SymbolFactor.genSymbol(CType_BaseType.getBaseType("int")));
						continue;
					}
				}
			}
		}catch(MyNullPointerException e){
			super.visit(node,expdata);
			SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
			if(Config.USEUNKNOWN)
				expdata.currentvex.addSymbolDomain(sym, DoubleDomain.getUnknownDomain());
			else
				expdata.currentvex.addSymbolDomain(sym, DoubleDomain.getFullDomain());
			leftvalue=new Expression(sym);
			expdata.value=leftvalue;
			return data;
		}
		expdata.value=leftvalue;
		return data;
	}
	
	public Object visit(ASTPostfixExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		ASTPrimaryExpression primary=(ASTPrimaryExpression)node.jjtGetChild(0);
		primary.jjtAccept(this, data);
		ArrayList<Boolean> flags=node.getFlags();
		ArrayList<String> operators=node.getOperatorType();
		Expression currentvalue=expdata.value;		
		CType currenttype=primary.getType();
		int j=1;
		for(int i=0;i<flags.size();i++){
			boolean flag=flags.get(i);
			String operator=operators.get(i);
			if(operator.equals("[")){
				ASTExpression expression =null;
				if(flag){
					expression=(ASTExpression)node.jjtGetChild(j++);
					expression.jjtAccept(this, data);					
					if(Config.Field){
						//forѭ����Ŀ���� ѭ��������쳣ʱ��ʱ��ֹѭ����ִ�в�����
						for(int loopFlag=0;loopFlag<1;loopFlag++)
						{
							try
							{
								ExpressionVistorData expdataIndex=(ExpressionVistorData)data;
								Domain domain=expdataIndex.value.getDomain(expdataIndex.currentvex.getSymDomainset());
								IntegerDomain vDomain=Domain.castToIntegerDomain(domain);
												
								if(vDomain!=null)
								{
									VariableNameDeclaration array = null;							
									NameDeclaration declArr=Search.searchInVariableAndMethodUpward(primary.getImage(), node.getScope());
									if (declArr instanceof VariableNameDeclaration) {
										array = (VariableNameDeclaration) declArr;								
									}
									if(array.getType() instanceof CType_Array)
									{										
										CType_Array temp = (CType_Array) array.getType();
										long length=temp.getDimSize();
										IntegerDomain arrDomain=new IntegerDomain(new IntegerInterval(0,length));							
										vDomain=IntegerDomain.intersect(arrDomain, vDomain);
																			
										List<Integer> intNums=vDomain.getNums();
										for(int intN=0;intN<intNums.size();intN++)
										{
											int index=intNums.get(intN).intValue();							
											String arrImage=primary.getImage()+"["+index+"]";
											NameDeclaration declArrar=Search.searchInVariableAndMethodUpward(arrImage, node.getScope());								
											if(declArrar!=null&&expdata!=null)
											{ 
												expdata.arrayIndex.add(new Integer(index));
												
											}else
											{
												break;
											}
											
										}
									}else if(array.getType() instanceof CType_Pointer)
									{										
										CType_Pointer temp = (CType_Pointer) array.getType();
										long length=Config.MaxArray;
										IntegerDomain arrDomain=new IntegerDomain(new IntegerInterval(0,length));							
										vDomain=IntegerDomain.intersect(arrDomain, vDomain);
																			
										List<Integer> intNums=vDomain.getNums();
										for(int intN=0;intN<intNums.size();intN++)
										{
											int index=intNums.get(intN).intValue();							
											String arrImage=primary.getImage()+"["+index+"]";
											NameDeclaration declArrar=Search.searchInVariableAndMethodUpward(arrImage, node.getScope());								
											if(declArrar!=null&&expdata!=null)
											{ 
												expdata.arrayIndex.add(new Integer(index));
												
											}else
											{
												break;
											}
											
										}
									}
								}
							}catch(Exception e)
							{
								break;
							}
						}
																		
						Scope scope=node.getScope();
			        	NameDeclaration decl=Search.searchInVariableAndMethodUpward(node.getImage(), scope);
			        	if(decl instanceof VariableNameDeclaration){
			        		//�������� 
			        		VariableNameDeclaration v=(VariableNameDeclaration)decl;
			        		currentvalue=expdata.currentvex.getValue(v);
			        	}
					}
				}else{
					throw new RuntimeException("ASTPostfixExpression error!");
				}
				//zys:2010.9.13 ��Ϊ���Գ�����ʱ������
				if(currenttype==null){
					Scope scope=node.getScope();
					NameDeclaration decl0=Search.searchInVariableAndMethodUpward(primary.getImage(), scope);
					if(decl0!=null&&decl0 instanceof VariableNameDeclaration)
					{
						VariableNameDeclaration decl00=(VariableNameDeclaration)decl0;
						currenttype=decl00.getType();
					}
					if(currenttype==null)
					{
						logger.error(primary.getBeginFileLine()+"�е����ͷ�������");
						throw new RuntimeException(primary.getBeginFileLine()+"�е����ͷ�������");
					}
				}
				CType atype=currenttype.getSimpleType();
				if(atype instanceof CType_AbstPointer){
					CType_AbstPointer ptype=(CType_AbstPointer) atype;
					currenttype=ptype.getOriginaltype();
					if(currentvalue!=null){
					Domain domain=Domain.castToType(currentvalue.getDomain(expdata.currentvex.getSymDomainset()), atype);
					if(domain instanceof PointerDomain){
						PointerDomain pdomain=(PointerDomain)domain;
						SymbolFactor sym=SymbolFactor.genSymbol(atype);
						if(currenttype.getSimpleType() instanceof CType_Array){
							CType_Array temp=(CType_Array)currenttype.getSimpleType();
							if(temp.isFixed()){
								pdomain =new PointerDomain();
								pdomain.setElementtype(currenttype);
								pdomain.setLength(new Expression(temp.getDimSize()));
								expdata.currentvex.addSymbolDomain(sym, pdomain);;
								}
							}
							currentvalue=new Expression(sym);
						}
					}
				}
			}else if(operator.equals("(")){
				ASTArgumentExpressionList expressionlist=null;
				//add by zhouhb
				//��ָ����غ����Ĵ���
				//������ģ������
				//add by zhouhb 2010/10/19
				//if(node.getImage().contains("malloc")||node.getImage().contains("Malloc")||node.getImage().contains("malloc"))
				if(node.getImage().equals("malloc"))
				{
					//�����˶�malloc�����а��������������õ��жϣ��Դ����δ֪�������账��
					//add by zhouhb 2010/8/18
					if(((SimpleNode)node.jjtGetChild(1)).containsChildOfType(ASTPrimaryExpression.class)){
						List<Node> primarys =((SimpleNode)node.jjtGetChild(1)).findChildrenOfType(ASTPrimaryExpression.class);
           				for(Node pri:primarys)
           				{
           					if(pri.jjtGetNumChildren()==0){
           						VariableNameDeclaration v =((SimpleNode)pri).getVariableNameDeclaration();
           						if(v!=null&&v.getScope() instanceof SourceFileScope)
           							return data;
           					}
           				}
					}
						node.jjtGetChild(1).jjtAccept(this, expdata);
						if(node.containsChildOfType(ASTTypeName.class)){
							ASTTypeName type=(ASTTypeName)node.getFirstChildOfType(ASTTypeName.class);
							expdata.value=expdata.value.div(new Expression(type.getType().getSize()));
						}
						//����malloc(10)ʱ��������Ϣ�����޸�
						//modified by zhouhb 2010/7/19
						long mallocsize;
						if(expdata.value==null)
						{
							return data;
						}
						IntegerDomain size=IntegerDomain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
						//���malloc����ռ��Բ������ݽ�����Ĭ�Ϸ��������
						if(size!=null&&!size.isEmpty()&&!size.getIntervals().isEmpty()){
							mallocsize=(int)size.getIntervals().first().getMin();
						}else{
							mallocsize=IntegerDomain.DEFAULT_MAX;
						}
						SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
						PointerDomain p=new PointerDomain();
						p.offsetRange=new IntegerDomain(0,mallocsize-1);
						//p.AllocType=CType_AllocType.heapType;
						if(p.Type.contains(CType_AllocType.NotNull)){
							p.Type.remove(CType_AllocType.NotNull);
						}
						p.Type.add(CType_AllocType.heapType);
						p.setValue(PointerValue.NULL_OR_NOTNULL);
						expdata.currentvex.addSymbolDomain(sym, p);
						expdata.value=new Expression(sym);
						return data;
//				}else if(node.getImage().equals("calloc")){
//					if(((SimpleNode)node.jjtGetChild(1)).containsChildOfType(ASTPrimaryExpression.class)){
//						List<Node> primarys =((SimpleNode)node.jjtGetChild(1)).findChildrenOfType(ASTPrimaryExpression.class);
//           				for(Node pri:primarys)
//           				{
//           					if(pri.jjtGetNumChildren()==0){
//           						VariableNameDeclaration v =((SimpleNode)pri).getVariableNameDeclaration();
//           						if(v!=null&&v.getScope() instanceof SourceFileScope)
//           							return data;
//           					}
//           				}
//					}
//						node.jjtGetChild(1).jjtAccept(this, expdata);
//						if(node.containsChildOfType(ASTTypeName.class)){
//						ASTTypeName type=(ASTTypeName)node.getFirstChildOfType(ASTTypeName.class);
//						if(type.getType()!=null){
//							long callocsize;
//							IntegerDomain size=IntegerDomain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
//							if(size==null){
//								callocsize=IntegerDomain.DEFAULT_MAX;
//							}else{
//								callocsize=(int)size.getIntervals().first().getMin();
//							}
//							SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
//							PointerDomain p=new PointerDomain();
//							p.offsetRange=new IntegerDomain(0,callocsize-1);
//							//p.AllocType=CType_AllocType.heapType;
//							if(p.Type.contains(CType_AllocType.NotNull)){
//								p.Type.remove(CType_AllocType.NotNull);
//							}
//							p.Type.add(CType_AllocType.heapType);
//							p.setValue(PointerValue.NULL_OR_NOTNULL);
//							expdata.currentvex.addSymbolDomain(sym, p);
//							expdata.value=new Expression(sym);
//							return data;
//							}
//						}
//				}if(node.getImage().equals("xmalloc")){
//					node.jjtGetChild(1).jjtAccept(this, expdata);
//					node.setType(CType_Pointer.VOID);
//					SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
//					PointerDomain p=new PointerDomain();
//					//p.AllocType=CType_AllocType.heapType;
//					if(p.Type.contains(CType_AllocType.NotNull)){
//						p.Type.remove(CType_AllocType.NotNull);
//					}
//					p.Type.add(CType_AllocType.heapType);
//					p.setValue(PointerValue.NOTNULL);
//					expdata.currentvex.addSymbolDomain(sym, p);
//					expdata.value=new Expression(sym);
//					return data;
//				}else if(node.getImage().equals("realloc")){
//					if(((SimpleNode)node.jjtGetChild(1)).containsChildOfType(ASTPrimaryExpression.class)){
//						List<Node> primarys =((SimpleNode)node.jjtGetChild(1)).findChildrenOfType(ASTPrimaryExpression.class);
//           				for(Node pri:primarys)
//           				{
//           					if(pri.jjtGetNumChildren()==0){
//           						VariableNameDeclaration v =((SimpleNode)pri).getVariableNameDeclaration();
//           						if(v!=null&&v.getScope() instanceof SourceFileScope)
//           							return data;
//           					}
//           				}
//					}
//							node.jjtGetChild(1).jjtAccept(this, expdata);
//							long reallocsize;
//							IntegerDomain size=IntegerDomain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
//							if(size==null){
//								reallocsize=IntegerDomain.DEFAULT_MAX;
//							}else{
//								reallocsize=(int)size.getIntervals().first().getMin();
//							}
//							SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
//							PointerDomain p=new PointerDomain();
//							p.offsetRange=new IntegerDomain(0,reallocsize-1);
//							//p.AllocType=CType_AllocType.heapType;
//							if(p.Type.contains(CType_AllocType.NotNull)){
//								p.Type.remove(CType_AllocType.NotNull);
//							}
//							p.Type.add(CType_AllocType.heapType);
//							p.setValue(PointerValue.NULL_OR_NOTNULL);
//							//ͬ�����Ŷ�Ӧ������ֵ
//							ASTPrimaryExpression pri = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
//							VariableNameDeclaration v = pri.getVariableDecl();
//							Expression ve=expdata.currentvex.getValue(v);
//							SymbolFactor temp=(SymbolFactor)ve.getSingleFactor();
//							expdata.currentvex.addSymbolDomain(temp, p);
//							expdata.currentvex.addSymbolDomain(sym, p);
//							expdata.value=new Expression(sym);
//							return data;
				}//������ģ������
				//add by zhouhb 2010/10/19
//				else if(node.getImage().contains("free")||node.getImage().contains("Free")||node.getImage().contains("FREE")){
				else if(node.getImage().equals("free")){		
						node.jjtGetChild(1).jjtAccept(this, expdata);
						PointerDomain p=new PointerDomain();
						p.offsetRange=new IntegerDomain(0,0);
						//p.AllocType=CType_AllocType.Null;
						p.Type.add(CType_AllocType.Null);
						p.setValue(PointerValue.NULL);
						VariableNameDeclaration v;
						if(!Config.Field){
							ASTPrimaryExpression pri = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
							v = pri.getVariableDecl();
							//�ṹ���Ա��free�����Դ���
							//add by zhouhb 2010/8/18
								if(v!=null&&(v.getType()instanceof CType_Pointer&&CType_Pointer.getOrignType(v.getType()) instanceof CType_Struct)){
									return data;
								}
						}else{
							ASTPostfixExpression po = (ASTPostfixExpression)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTPostfixExpression.class);
							v = (VariableNameDeclaration)Search.searchInVariableAndMethodUpward(po.getImage(), node.getScope());
						}
						Expression ve=null;
						if(v!=null)
							ve=expdata.currentvex.getValue(v);
						if(ve==null || ve!=null && !(ve.getSingleFactor() instanceof SymbolFactor))
							return data;
						SymbolFactor temp=(SymbolFactor)ve.getSingleFactor();
						//����ͷŶ���ָ�������㣬��ve��ΪSingleFactor(eg.1+S)�����ͷŲ��ɹ�
						if(temp==null){
							return data;
						}else{
						expdata.currentvex.addSymbolDomain(temp, p);
						expdata.value=new Expression(temp);
						return data;
						}
				}
//				}//���ڴ����ַ����ı�׼�⺯������Ӧָ���������
				//��������ʱ�������������Ϊ�������Ҳ�����Ϊ��������������
				//2010/7/19
				//�����������Ϊ���ʽ����Ĵ���
				else if(node.getImage().equals("strcpy")){
					node.jjtGetChild(1).jjtAccept(this, expdata);		
					ASTPrimaryExpression priLeft = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
					VariableNameDeclaration vLeft = priLeft.getVariableDecl();
					if(vLeft!=null){
						Expression eLeft=expdata.currentvex.getValue(vLeft);
						if(eLeft!=null){
							Domain vDomainTemp=eLeft.getDomain(expdata.currentvex.getSymDomainset());
							if(vLeft.getType() instanceof CType_Pointer){
								PointerDomain vDomain=(PointerDomain)Domain.castToType(vDomainTemp,vLeft.getType());
								try{
									if(eLeft.getSingleFactor() instanceof SymbolFactor&&vDomain!=null){
										Domain tempDomain=vDomain.clone();
										//�Ҳ�����Ϊ����ʱ�Ĵ���	
										if(node.containsChildOfType(ASTConstant.class)){
											ASTConstant con = (ASTConstant)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTConstant.class);
											if(!(eLeft.getSingleFactor() instanceof SymbolFactor))
												return data;
											SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
											((PointerDomain)tempDomain).allocRange=new IntegerDomain(con.getImage().length()-1,con.getImage().length()-1);
											expdata.currentvex.addSymbolDomain(temp, tempDomain);
										}//�Ҳ�����Ϊ������������ʱ�Ĵ���	
										else{
											ASTPrimaryExpression priRight = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1).jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
											//�Ҳ�����Ϊ����
											if(!priRight.isMethod()){
												VariableNameDeclaration vRight = priRight.getVariableDecl();
												//�����˵�����Ĭ������δ֪
												if(vRight==null)
												{
													return data;
												}
										
												Expression eRight=expdata.currentvex.getValue(vRight);
												//�����˵�����Ĭ������δ֪
												if(eRight==null){
													return data;
												}
												PointerDomain size=(PointerDomain)Domain.castToType(eRight.getDomain(expdata.currentvex.getSymDomainset()),vRight.getType());
												//�Ҳ�����Ϊ��������ʱsizeΪ��
												if(size==null){
													size=new PointerDomain();
													size.allocRange=new IntegerDomain(0,IntegerDomain.DEFAULT_MAX);
												}else{
													((PointerDomain)tempDomain).allocRange=size.allocRange;
												}
												SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
												expdata.currentvex.addSymbolDomain(temp, tempDomain);
											}//�Ҳ�����Ϊ��������
											else{
												
											}
										}
									}
								}catch(CloneNotSupportedException e){
									e.printStackTrace();
									return null;
								}
							}
						}
					}
					return data;
				}
				else if(node.getImage().equals("sqrt")){
					expressionlist=(ASTArgumentExpressionList)node.jjtGetChild(1);
					if(expressionlist!=null)
					{
						expressionlist.jjtGetChild(0).jjtAccept(this, expdata);
						DoubleDomain ddomain=DoubleDomain.castToDoubleDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
						
						if(ddomain instanceof DoubleDomain&& !ddomain.isUnknown())
						{
							ddomain=DoubleDomain.sqrt(ddomain);							
							SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
							expdata.currentvex.addSymbolDomain(sym, ddomain);
							expdata.value=new Expression(sym);
						}
					}				
					return data;
				}else if(node.getImage().equals("strncpy")){
					node.jjtGetChild(1).jjtAccept(this, expdata);		
					ASTPrimaryExpression priLeft = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
					VariableNameDeclaration vLeft = priLeft.getVariableDecl();
					if(vLeft!=null){
						Expression eLeft=expdata.currentvex.getValue(vLeft);
						if(eLeft!=null){
							Domain vDomainTemp=eLeft.getDomain(expdata.currentvex.getSymDomainset());
							if(vLeft.getType() instanceof CType_Pointer){
								PointerDomain vDomain=(PointerDomain)Domain.castToType(vDomainTemp,vLeft.getType());
								try{
									if(eLeft.getSingleFactor() instanceof SymbolFactor&&vDomain!=null){
										Domain tempDomain=vDomain.clone();
										//�Ҳ�����Ϊ����ʱ�Ĵ���	
										if(node.containsChildOfType(ASTConstant.class)){
											ASTConstant con = (ASTConstant)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTConstant.class);
											if(!(eLeft.getSingleFactor() instanceof SymbolFactor))
												return data;
											SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
											((PointerDomain)tempDomain).allocRange=new IntegerDomain(con.getImage().length()-1,con.getImage().length()-1);
											expdata.currentvex.addSymbolDomain(temp, tempDomain);
										}//�Ҳ�����Ϊ������������ʱ�Ĵ���	
										else{
											ASTPrimaryExpression priRight = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1).jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
											//�Ҳ�����Ϊ����
											if(!priRight.isMethod()){
												VariableNameDeclaration vRight = priRight.getVariableDecl();
												//�����˵�����Ĭ������δ֪
												if(vRight==null)
												{
													return data;
												}
										
												Expression eRight=expdata.currentvex.getValue(vRight);
												//�����˵�����Ĭ������δ֪
												if(eRight==null){
													return data;
												}
																								
												PointerDomain size=(PointerDomain)Domain.castToPointerDomain(eRight.getDomain(expdata.currentvex.getSymDomainset()));
												//�Ҳ�����Ϊ��������ʱsizeΪ��
												if(size==null){
													size=new PointerDomain();
													size.allocRange=new IntegerDomain(0,IntegerDomain.DEFAULT_MAX);
												}else{
													((PointerDomain)tempDomain).allocRange=size.allocRange;
												}
												SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
												expdata.currentvex.addSymbolDomain(temp, tempDomain);
											}//�Ҳ�����Ϊ��������
											else{
												
											}
										}
									}
								}catch(CloneNotSupportedException e){
									e.printStackTrace();
									return null;
								}
							}else if(vLeft.getType() instanceof CType_Array){
								PointerDomain vDomain=(PointerDomain)Domain.castToType(vDomainTemp,vLeft.getType());
								try{
									if(eLeft.getSingleFactor() instanceof SymbolFactor&&vDomain!=null){
										Domain tempDomain=vDomain.clone();
										//�Ҳ�����Ϊ����ʱ�Ĵ���	
										SimpleNode argNode=(SimpleNode)node.jjtGetChild(1);
										if(((SimpleNode)argNode.jjtGetChild(1)).containsChildOfType(ASTConstant.class)){
											ASTConstant con = (ASTConstant)((SimpleNode)argNode.jjtGetChild(1)).getFirstChildOfType(ASTConstant.class);
											if(!(eLeft.getSingleFactor() instanceof SymbolFactor))
												return data;
											SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
											((PointerDomain)tempDomain).allocRange=new IntegerDomain(con.getImage().length()-1,con.getImage().length()-1);
											expdata.currentvex.addSymbolDomain(temp, tempDomain);
										}//�Ҳ�����Ϊ������������ʱ�Ĵ���	
										else{
											ASTPrimaryExpression priRight = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1).jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
											//�Ҳ�����Ϊ����
											if(!priRight.isMethod()){
												VariableNameDeclaration vRight = priRight.getVariableDecl();
												//�����˵�����Ĭ������δ֪
												if(vRight==null)
												{
													return data;
												}
										
												Expression eRight=expdata.currentvex.getValue(vRight);
												//�����˵�����Ĭ������δ֪
												if(eRight==null){
													return data;
												}
												
												PointerDomain size=(PointerDomain)Domain.castToType(eRight.getDomain(expdata.currentvex.getSymDomainset()),vRight.getType());
												//�Ҳ�����Ϊ��������ʱsizeΪ��
												if(size==null){
													size=new PointerDomain();
													size.allocRange=new IntegerDomain(0,IntegerDomain.DEFAULT_MAX);
												}else{
													((PointerDomain)tempDomain).allocRange=size.allocRange;
												}
												SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
												expdata.currentvex.addSymbolDomain(temp, tempDomain);
											}//�Ҳ�����Ϊ��������
											else{
												
											}
										}
										node.jjtGetChild(1).jjtGetChild(2).jjtAccept(this, expdata);
										Expression thirdValue=expdata.value;
										IntegerDomain thirdDomain=(IntegerDomain)Domain.castToIntegerDomain(thirdValue.getDomain(expdata.currentvex.getLastsymboldomainset()));
										thirdDomain=(IntegerDomain) Domain.intersect(thirdDomain, new IntegerDomain(0,IntegerDomain.DEFAULT_MAX), new CType_BaseType("int"));
										((PointerDomain)tempDomain).allocRange=(IntegerDomain) Domain.union(((PointerDomain)tempDomain).allocRange,thirdDomain,new CType_BaseType("int"));
									}
								}catch(CloneNotSupportedException e){
									e.printStackTrace();
									return null;
								}
							}
						}
					}
					return data;
				}
				//��������ʱ�������������Ϊ�������Ҳ�����Ϊ��������������
//				else if(node.getImage().equals("strcat")){
//					node.jjtGetChild(1).jjtAccept(this, expdata);	
//					ASTPrimaryExpression priLeft = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
//					VariableNameDeclaration vLeft = priLeft.getVariableDecl();
//					if(vLeft!=null)
//					{
//						Expression eLeft=expdata.currentvex.getValue(vLeft);
//						Domain vDomain=eLeft.getDomain(expdata.currentvex.getSymDomainset());
//						try{
//							if(eLeft.getSingleFactor() instanceof SymbolFactor&&vDomain!=null){
//								//�Ҳ�����Ϊ����ʱ�Ĵ���	
//								Domain tempDomain=vDomain.clone();
//								if(node.containsChildOfType(ASTConstant.class)){
//								ASTConstant con = (ASTConstant)((SimpleNode)node.jjtGetChild(1)).getFirstChildOfType(ASTConstant.class);
//								SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
//								((PointerDomain)tempDomain).allocRange=IntegerDomain.add(((PointerDomain)vDomain).allocRange,con.getImage().length()-2);
//								expdata.currentvex.addSymbolDomain(temp, tempDomain);
//							}//�Ҳ�����Ϊ����ʱ�Ĵ���	
//							else{ASTPrimaryExpression priRight = (ASTPrimaryExpression)((SimpleNode)node.jjtGetChild(1).jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
//								VariableNameDeclaration vRight = priRight.getVariableDecl();
//								Expression eRight=expdata.currentvex.getValue(vRight);
//								PointerDomain size=(PointerDomain)eRight.getDomain(expdata.currentvex.getSymDomainset());
//								//�Ҳ�����Ϊ��������ʱsizeΪ��
//								if(size==null){
//									size=new PointerDomain();
//									size.allocRange=new IntegerDomain(0,IntegerDomain.DEFAULT_MAX);
//								}else{
//									((PointerDomain)tempDomain).allocRange=IntegerDomain.add(((PointerDomain)tempDomain).allocRange,IntegerDomain.sub(size.allocRange,1));
//								}
//								SymbolFactor temp=(SymbolFactor)eLeft.getSingleFactor();
//								expdata.currentvex.addSymbolDomain(temp, tempDomain);
//								}
//							}
//						}catch(CloneNotSupportedException e){
//							e.printStackTrace();
//							return null;
//						}
//					}
//					return data;
//				}else if(node.getImage().equals("strncpy")){
//					//������
//				}else if(node.getImage().equals("strncat")){
//					//������
//				}//����ĺܲ�׼ȷ��Ϊ�˼����󱨣���������
				 //modified by zhouhb 2010/8/18
//				else if(node.getImage().equals("strlen")){
//					Object re = ((SimpleNode)node.jjtGetChild(1)).getVariableNameDeclaration();
//					if(expdata.value==null){
//						SymbolFactor s=SymbolFactor.genSymbol(node.getType());
//    					expdata.value=new Expression(s);
//					}
//					SymbolFactor sym=(SymbolFactor)expdata.value.getSingleFactor();
//					if(re instanceof VariableNameDeclaration){
//						Expression e=expdata.currentvex.getValue((VariableNameDeclaration)re);
//						//����ͨ���������ݣ�������ж�
//						//add by zhouhb 2010/7/19
//						if(e==null){
//							IntegerDomain size=new IntegerDomain(IntegerDomain.DEFAULT_MIN,IntegerDomain.DEFAULT_MAX);
//							expdata.currentvex.addSymbolDomain(sym, size);
//						}else{
//						Domain vDomain=e.getDomain(expdata.currentvex.getSymDomainset());
//						if(vDomain instanceof PointerDomain){
//						    IntegerDomain size=IntegerDomain.sub(((PointerDomain)vDomain).allocRange,1);
//						    if(size.getIntervals().contains(new IntegerInterval(-1,-1))){
//								size.intervals.remove(new IntegerInterval(-1,-1));
//								size.intervals.add(new IntegerInterval(0,0));
//						    }
//							expdata.currentvex.addSymbolDomain(sym, size);
//						    }
//						}
//					}else if(node.containsChildOfType(ASTConstant.class)){
//						ASTConstant con = (ASTConstant)node.getFirstChildOfType(ASTConstant.class);
//						IntegerDomain size=new IntegerDomain(con.getImage().length()-2,con.getImage().length()-2);
//						expdata.currentvex.addSymbolDomain(sym,size);
//					}
//					
//				}
				if(flag){
					expressionlist=(ASTArgumentExpressionList)node.jjtGetChild(j++);
					expressionlist.jjtAccept(this, data);
				}
			}else if(operator.equals(".")){
				ASTFieldId field=null;
				if(flag){
					field=(ASTFieldId)node.jjtGetChild(j++);
					field.jjtAccept(this, data);
					if(Config.Field){
						Scope scope=node.getScope();
			        	NameDeclaration decl=Search.searchInVariableAndMethodUpward(node.getImage(), scope);
			        	if(decl instanceof VariableNameDeclaration){
			        		//��������
			        		VariableNameDeclaration v=(VariableNameDeclaration)decl;
			        		currentvalue=expdata.currentvex.getValue(v);
			        		if(currentvalue==null){
			        			SymbolFactor sym=SymbolFactor.genSymbol(v.getType(),v.getImage());
								expdata.value=new Expression(sym);
								expdata.currentvex.addValue(v, expdata.value);
			        		}
			        		}
					}
				}else{
					throw new RuntimeException("ASTPostfixExpression error!");
				}
				if(Config.Field){
					if(currentvalue==null){
						currentvalue=new Expression(SymbolFactor.genSymbol(field.getType()));
					}
				}else
					currentvalue=new Expression(SymbolFactor.genSymbol(field.getType()));
			}else if(operator.equals("->")){
				ASTFieldId field=null;
				if(flag){
					field=(ASTFieldId)node.jjtGetChild(j++);
					field.jjtAccept(this, data);
					if(Config.Field){
						Scope scope=node.getScope();
			        	NameDeclaration decl=Search.searchInVariableAndMethodUpward(node.getImage(), scope);
			        	if(decl instanceof VariableNameDeclaration){
			        		//��������
			        		VariableNameDeclaration v=(VariableNameDeclaration)decl;
			        		currentvalue=expdata.currentvex.getValue(v);
			        		}
					}
				}else{
					throw new RuntimeException("ASTPostfixExpression error!");
				}
				if(Config.Field){
					if(currentvalue==null){
						currentvalue=new Expression(SymbolFactor.genSymbol(field.getType()));
					}
				}else
					currentvalue=new Expression(SymbolFactor.genSymbol(field.getType()));
			}else if(operator.equals("++")&&currentvalue!=null){
				Expression temp=currentvalue.add(new Expression(1));
				ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode)node.jjtGetChild(j-1)).getSingleChildofType(ASTPrimaryExpression.class);
				if(p!=null){
					VariableNameDeclaration v = p.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null) {
						expdata.currentvex.addValue(v, temp);
						
					}
				}
				if(Config.Field){
					VariableNameDeclaration v = node.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null&&expdata.value!=null) {
						expdata.currentvex.addValue(v, temp);
					}
				}
			}else if(operator.equals("--")&&currentvalue!=null){
				Expression temp=currentvalue.sub(new Expression(1));
				ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode)node.jjtGetChild(j-1)).getSingleChildofType(ASTPrimaryExpression.class);
				if(p!=null){
					VariableNameDeclaration v = p.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null) {
						expdata.currentvex.addValue(v, temp);
					}
				}
				if(Config.Field){
					VariableNameDeclaration v = node.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null&&expdata.value!=null) {
						expdata.currentvex.addValue(v, temp);
					}
				}
			}else{
				currentvalue=new Expression(SymbolFactor.genSymbol(CType_BaseType.getBaseType("int")));
			}
		}
		//add by zhouhb
		//����p=f(a,b)ʱ����Ϊf,a,b�ֱ�������������F,A,B�����ջ�����һ���µķ���P������δ��F��Ӧ��������P����
		//���˵�ʱΪʲô��ô�ģ����ұ���
//		if(primary.jjtGetNumChildren()==0&&primary.getType() instanceof CType_Function){
//			currentvalue=func;
//		}
		expdata.value=currentvalue;
		return data;
	}
	
	
    @Override
	public Object visit(ASTPrimaryExpression node, Object data) {
    	super.visit(node, data);
    	ExpressionVistorData expdata=(ExpressionVistorData)data;
    	String image=node.getImage();
    	if(!image.equals("")){
    		if(image.equals("NULL"))
    		{
    			SymbolFactor sf =SymbolFactor.genSymbol(new CType_Pointer());
    			expdata.currentvex.addSymbolDomain(sf,PointerDomain.getNullDomain());
    			expdata.value = new Expression(sf);
    			return expdata;
    		}
    		
    		//�������
    		Scope scope=node.getScope();
        	NameDeclaration decl=Search.searchInVariableAndMethodUpward(image, scope);
        	if(decl instanceof VariableNameDeclaration){
        		//��������
        		VariableNameDeclaration v=(VariableNameDeclaration)decl;
        		expdata.value=expdata.currentvex.getValue(v);
        		//������ṹ��������Ϣ
        		//add by zhouhb 2010/8/19
        		if(!Config.Field){
        			if(v.getType() instanceof CType_Array && ((CType_Array)v.getType()).getOriginaltype() instanceof CType_Struct||v.getType() instanceof CType_Struct)
            			return data;
        		}
        		if(expdata.value==null){
        			//ȫ�ֱ�����ֵ���� �����ȫ�ֱ������ҳ�ʼֵ������ʱ����������뵽��ǰ����� ssj
        			//ȫ�ֱ������ܳ�ʼ��������main����a()��b(),a()�޸���global��b()�ڷ���ʱ�Ͳ���
        			//ȡglobal�ĳ�ʼֵ��
//        			if(v.getScope() instanceof SourceFileScope){
//        				Variable var = v.getVariable();
//        				if(var == null){
//        					if(var.getValue() instanceof Double){
//            					double vardouble = new Double(var.getValue().toString());
//            					expdata.value = new Expression(vardouble);
//            				}else if(var.getValue() instanceof Long){
//            					long varlong = Long.valueOf(var.getValue().toString());
//            					expdata.value = new Expression(varlong);
//            				}else if (var.getValue() instanceof PointerValue){
//            					PointerDomain pd=new PointerDomain((PointerValue) var.getValue());	
//            					SymbolFactor s=SymbolFactor.genSymbol(v.getType(), v.getImage());
//            					expdata.currentvex.addSymbolDomain(s, pd);
//            					expdata.value=new Expression(s);
//            				}else{
//            					expdata.value=new Expression(SymbolFactor.genSymbol(v.getType(),v.getImage()));
//            					
//            				}
//        				}else{
//        					expdata.value=new Expression(SymbolFactor.genSymbol(v.getType(),v.getImage()));
//        				}
//        			}else {
//        				SymbolFactor sym=SymbolFactor.genSymbol(v.getType(),v.getImage());
//        				//modified by zhouhb
//        				//��������f(a,b)�в����ĳ�ʼ������
//        				//remodified by zhouhb  2010/8/6
//        				//���������ʼ��
////        				if(v.getType() instanceof CType_Pointer&&node.jjtGetNumChildren()==0){
////    						PointerDomain p=new PointerDomain();
////    						expdata.currentvex.addSymbolDomain(sym, p);
////        				}
//        				expdata.value=new Expression(sym);
//        			}
        			//�����жϸ�����
        			SymbolFactor sym=SymbolFactor.genSymbol(v.getType(),v.getImage());
					expdata.value=new Expression(sym);
        			if ( expdata.currentvex != null) {
        				if(v.getScope() instanceof SourceFileScope){
            				Variable var = Variable.getVariable(v);
            				//dyk ����var.getType()!=null ������
            				if(var != null&&var.getType()!=null){
            					if(!var.getType().isArrayType()){
            						if(Config.USEUNKNOWN){
	            						Domain d = Domain.getUnknownDomain(Domain.getDomainTypeFromType(v.getType()));
	            					    expdata.currentvex.addSymbolDomain(sym,d);
            						}
	            					else
	        							expdata.currentvex.addSymbolDomain(sym,
	                							Domain.getFullDomainFromType(v.getType()));
            					}

            					else{
            						PointerDomain pd=new PointerDomain((PointerValue) PointerValue.NOTNULL);
            						expdata.currentvex.addSymbolDomain(sym, pd);
            					}
            				}
        				}
            				
        				expdata.currentvex.addValue(v, expdata.value);
        			}
        		}
        	}else if(decl instanceof MethodNameDeclaration){
        		MethodNameDeclaration mnd = (MethodNameDeclaration)decl;
        		Method method=null;
        		MethodSummary ms =null;
        		//�⺯������ֵ���� ssj
        		if(mnd != null && mnd.isLib()){
        	        InterContext interContext = InterContext.getInstance();
        	        Map<String, MethodNameDeclaration> libDecls = interContext.getLibMethodDecls();
        	        MethodNameDeclaration  libmethod=libDecls.get(mnd.getImage());
        	        if(libmethod!=null){
        	        	method = libmethod.getMethod();
        	        	if(method != null){
        	        		Domain d = method.getReturnDomain();
        	                SymbolFactor s = SymbolFactor.genSymbol(node.getType());
        	                if(Config.USEUNKNOWN && d==null)
        	                	d = Domain.getUnknownDomain(Domain.getDomainTypeFromType(node.getType()));
        	                node.getCurrentVexNode().addSymbolDomain(s, d);
        	                expdata.value=new Expression(s);
        	        	}    
        	        }
        	        //�ǿ⺯��������û��ժҪ����ֱ������һ�����ţ��䷵��ֵ����Ϊ�������
        	        else if(Config.LIB_NOSUMMARY_NPD&&mnd.getType()!=null)
        	        {
        	        	if(mnd.getType() instanceof CType_Function&&((CType_Function)mnd.getType()).getReturntype()!=null)
        	        	{
        	        		CType returnType=((CType_Function)mnd.getType()).getReturntype();
        	        		if(returnType instanceof CType_Pointer)
        	        		{
        	        			SymbolFactor s=SymbolFactor.genSymbol(mnd.getType(),mnd.getImage());
        	        			node.getCurrentVexNode().addSymbolDomain(s, new PointerDomain(PointerValue.NULL_OR_NOTNULL));
        	        			expdata.value=new Expression(s);
        	        		}if(returnType instanceof CType_Pointer)
        	        		{
        	        			SymbolFactor s=SymbolFactor.genSymbol(mnd.getType(),mnd.getImage());
        	        			node.getCurrentVexNode().addSymbolDomain(s, new PointerDomain(PointerValue.NULL_OR_NOTNULL));
        	        			expdata.value=new Expression(s);
        	        		}
        	        		
        	        	}
        	        }
        	        if(expdata.value==null){
        	        	//�ǿ⺯��������û��ժҪ����ֱ������һ�����ţ��䷵��ֵ����δ֪
        	        	expdata.value=new Expression(SymbolFactor.genSymbol(mnd.getType(),mnd.getImage()));
        	        }
        	    }else{
            		//chh  ������ʽ��������������ʱ���Կ��Լ������������ֵ�ĺ�������ȡ�䷵��ֵ����
        	    	if(mnd!=null)
            			method=mnd.getMethod();
            		if(method!=null&&method.getReturnDomain()!=null)
        			{
        				//������
            			if((method.getReturnDomain().getDomaintype()==DomainType.INTEGER)&&
        						(((IntegerDomain)method.getReturnDomain()).getMax()==((IntegerDomain)method.getReturnDomain()).getMin()))
        					expdata.value=new Expression(((IntegerDomain)method.getReturnDomain()).getMax());
        				//������
            			else if((method.getReturnDomain().getDomaintype()==DomainType.DOUBLE)&&
        						(((DoubleDomain)method.getReturnDomain()).getMax()==((DoubleDomain)method.getReturnDomain()).getMin()))
        					expdata.value=new Expression(((DoubleDomain)method.getReturnDomain()).getMax());
            			else if(method.getReturnDomain().getDomaintype()==DomainType.POINTER){
            				CType type=node.getType();
                			CType pointer=null;
                			if(type!=null && type instanceof CType_Function){
                				pointer=((CType_Function)type).getReturntype();
                				if(pointer!=null && pointer instanceof CType_Pointer){
                					SymbolFactor s=SymbolFactor.genSymbol(type);
                					expdata.value=new Expression(s);
                					expdata.currentvex.addSymbolDomain(s, method.getReturnDomain());           				
                				}
                			}
            			}
            			else if(expdata.value == null){
            				CType type = method.getReturnType();
            				expdata.value=new Expression(SymbolFactor.genSymbol(type));
            			}
        			}else{//�����ݲ�����
            			//zys:�����ָ�����͵ĸ�ֵ���������Ĭ�ϸ�ֵΪNULLORNOTNULL
            			CType type=node.getType();
            			CType pointer=null;
            			if(type!=null && type instanceof CType_Function){
            				pointer=((CType_Function)type).getReturntype();
            				//�����������ֵ����Ϊ�汾��ʱ���������ͣ����账������©��
            				//add by zhouhb 2010/8/30
            				if(pointer!=null && pointer instanceof CType_Pointer&&!(((CType_Pointer)pointer).getOriginaltype() instanceof CType_BaseType)){
            					return data;
            				}
            				//end by zhouhb
            				if(pointer!=null && pointer instanceof CType_Pointer){
            					SymbolFactor s=SymbolFactor.genSymbol(type);
            					expdata.value=new Expression(s);
            					PointerDomain domain=new PointerDomain();
            					expdata.currentvex.addSymbolDomain(s, domain);
            				}
            			}
            			if(expdata.value==null){
            				expdata.value=new Expression(SymbolFactor.genSymbol(type));
            			}
            		}
            		//chh  ������ʽ��������������ʱ���Կ��Լ������������ֵ�ĺ�������ȡ�䷵��ֵ���� end
            		
            		//�������� ���ȫ�ֱ��������������� ssj
            		if(mnd!=null)
            			ms = mnd.getMethodSummary();
            		if(ms != null && !ms.getPostConditions().isEmpty()){
                		Set<MethodFeature> mtdpostcond = ms.getPostConditions();
               			for(MethodFeature mtdfea:mtdpostcond)
               			{
               				if(mtdfea instanceof MethodPostCondition) {
                    			MethodPostCondition mtdpost = (MethodPostCondition)mtdfea;
                				Map<Variable, Domain> msvariables = mtdpost.getPostConds();
                				for(Variable msvariable:msvariables.keySet())
                				{//zys:Ϊ���������еı��������µķ��ű��ʽ
                					VariableNameDeclaration v = (VariableNameDeclaration)Search.searchInVariableUpward(msvariable.getName(),scope);
                    					/**zys:����ժҪ���ɵ����ʣ�
                    					 * 1���ٶ���file1.c��file2.c����Դ�ļ���#include "file3.c"��
                    					 * 		��file3.c�н���һ��������������ѡ��ĺ���
                    					 * 2�����file1.c�ȷ�������������������Ĳ�ͬ��������file1.c�иú�����ժҪ��Ϣ
                    					 * 3����file2.c����ʱ�����ú������ã������Ȼ�ȡ֮ǰ���ɵ�ժҪ���ٰ��ձ��ļ��е���������ѡ������µ�ժҪ
                    					 * 4�����Ϸ����Ľ���͵���ժҪ��Ϣ�ڵ����ļ����Ҳ��� */
                					if(v==null)
                    					continue;
                					SymbolFactor s=SymbolFactor.genSymbol(v.getType());
                    				Domain d=msvariables.get(msvariable);
                    				node.getCurrentVexNode().addSymbolDomain(s, d);
                					Expression expr=new Expression(s);
                    				node.getCurrentVexNode().addValue(v, expr);
                				}
               				}
                		}
                	}
        	    }
//        		 if(Config.USEUNKNOWN && expdata.value.getDomain(expdata.currentvex.getSymDomainset())==null)
//        			 expdata.currentvex.addSymbolDomain(expdata.value.getAllSymbol().iterator().next(),  Domain.getUnknownDomain(Domain.getDomainTypeFromType(node.getType())));
        	}
    	}
        return data;
    }
    

	public Object visit(ASTRelationalExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		node.jjtGetChild(0).jjtAccept(this, expdata);
		Expression leftvalue=expdata.value;
		Expression rightvalue=null;
		DoubleDomain d1=null;
		DoubleDomain d2=null;
		try{
			//�����ҽ��з��ż���
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				Node c = node.jjtGetChild(i);
				expdata.value = null;
				c.jjtAccept(this, expdata);
				rightvalue = expdata.value;
				
				if(leftvalue==null || rightvalue==null){
					throw new MyNullPointerException("RelationalExpression Value NULL in(.i) "+node.getBeginFileLine());
				}
				
				String operator = node.getOperatorType().get(i-1);
				d1=Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
				d2=Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
				if(d1==null || d2==null){
					throw new MyNullPointerException("RelationalExpression Domain NULL in(.i) "+node.getBeginFileLine());
				}
				if(Config.USEUNKNOWN && (d1.isUnknown() && d2.isUnknown())){
					throw new MyNullPointerException("RelationalExpression Domain UNKNOWN in(.i) "+node.getBeginFileLine());
				}
				if(operator.equals(">")){
					/**dongyk 20120306 �ж�ѭ���Ƿ�����һ��*/
					/*if(expdata.currentvex.getLoopHead()&&d1.getIntervals().size()>0&&d2.getIntervals().size()>0&&(d1.getMin()>d2.getMax())){
						expdata.currentvex.setLoopExecuteAtleastOnce(true);
					}*/
					if(d1.getMin()>d2.getMax()){
						leftvalue=new Expression(1);
						continue;
					}else if(d1.getMax()<=d2.getMin()){
						leftvalue=new Expression(0);
						continue;
					}
				}else if(operator.equals(">=")){
					/**dongyk 20120306 �ж�ѭ���Ƿ�����һ��*/
					/*if(expdata.currentvex.getLoopHead()&&d1.getIntervals().size()>0&&d2.getIntervals().size()>0&&(d1.getMin()>=d2.getMax())){
						expdata.currentvex.setLoopExecuteAtleastOnce(true);
					}*/
					if(d1.getMin()>=d2.getMax()){
						leftvalue=new Expression(1);
						continue;
					}else if(d1.getMax()<d2.getMin()){
						leftvalue=new Expression(0);
						continue;
					}
				}else if(operator.equals("<")){
					/**dongyk 20120306 �ж�ѭ���Ƿ�����һ��*/
					/*if(expdata.currentvex.getLoopHead()&&d1.getIntervals().size()>0&&d2.getIntervals().size()>0&&(d1.getMax()<d2.getMin())){
						//expdata.currentvex.setLoopExecuteAtleastOnce(true);
					}*/
					if(d1.getMin()<d2.getMax()){
						leftvalue=new Expression(1);
						continue;
					}else if(d1.getMax()>=d2.getMin()){
						leftvalue=new Expression(0);
						continue;
					}
				}else if(operator.equals("<=")){
					/**dongyk 20120306 �ж�ѭ���Ƿ�����һ��*/
					/*if(expdata.currentvex.getLoopHead()&&d1.getIntervals().size()>0&&d2.getIntervals().size()>0&&(d1.getMax()<=d2.getMin())){
						expdata.currentvex.setLoopExecuteAtleastOnce(true);
					}*/
					if(d1.getMin()<=d2.getMax()){
						leftvalue=new Expression(1);
						continue;
					}else if(d1.getMax()>d2.getMin()){
						leftvalue=new Expression(0);
						continue;
					}
				}
				throw new MyNullPointerException("RelationalExpression Domain Unknown in(.i) "+node.getBeginFileLine());
			}
		}catch(MyNullPointerException e){
			//super.visit(node,expdata);
			SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0,1));
			leftvalue=new Expression(sym);
			expdata.value=leftvalue;
			return data;
		}
		
		expdata.value=leftvalue;
		return data;
	}
	
	public Object visit(ASTShiftExpression node, Object data) {
		return dealBinaryBitOperation(node,data,node.getOperators());
	}
	
	public Object visit(ASTUnaryExpression node, Object data) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		if(node.jjtGetChild(0) instanceof ASTPostfixExpression){
			ExpressionVistorData expdata1=expdata;
			node.jjtGetChild(0).jjtAccept(this, expdata1);
			
			expdata.arrayIndex=expdata1.arrayIndex;
			return expdata;
		}else if(node.getOperatorType().size()==1){
			node.jjtGetChild(0).jjtAccept(this, expdata);
			if(node.getOperatorType().get(0).equals("++")){
				if(expdata.value==null){
					expdata.value=new Expression(1);
				}else{
					expdata.value=expdata.value.add(new Expression(1));
				}								
				ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode)node.jjtGetChild(0)).getSingleChildofType(ASTPrimaryExpression.class);
				if(p!=null){
					VariableNameDeclaration v = p.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null) {
						expdata.currentvex.addValue(v, expdata.value);
					}
				}
				if(Config.Field){
					ASTPostfixExpression po=(ASTPostfixExpression) ((SimpleNode)node.jjtGetChild(0)).getSingleChildofType(ASTPostfixExpression.class);
					if(po!=null){
						VariableNameDeclaration v = po.getVariableDecl();
						if (v != null && expdata.sideeffect && expdata.currentvex != null&&expdata.value!=null) {
							expdata.currentvex.addValue(v, expdata.value);
						}
					}
				}
			}else{
				if(expdata.value==null){
					expdata.value=new Expression(1);
				}else{
					expdata.value=expdata.value.sub(new Expression(1));
				}				
				ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode)node.jjtGetChild(0)).getSingleChildofType(ASTPrimaryExpression.class);
				if(p!=null){
					VariableNameDeclaration v = p.getVariableDecl();
					if (v != null && expdata.sideeffect && expdata.currentvex != null) {
						expdata.currentvex.addValue(v, expdata.value);
					}
				}
			}
			return data;
		}else if(node.jjtGetChild(0) instanceof ASTUnaryOperator){
			ASTUnaryOperator operator=(ASTUnaryOperator)node.jjtGetChild(0);
			String o=operator.getOperatorType().get(0);
			AbstractExpression castexpression=(AbstractExpression)node.jjtGetChild(1);
			node.jjtGetChild(1).jjtAccept(this, expdata);
			if(o.equals("&&")){
				//��֪����ô����
			}//modified by zhouhb
			else if(o.equals("&")){
				SymbolFactor sym=SymbolFactor.genSymbol(node.getType());
				PointerDomain p=new PointerDomain();
				p.offsetRange=new IntegerDomain(0,0);
				//p.AllocType=CType_AllocType.stackType;
				if(p.Type.contains(CType_AllocType.NotNull)){
					p.Type.remove(CType_AllocType.NotNull);
				}
				p.Type.add(CType_AllocType.stackType);
				p.setValue(PointerValue.NOTNULL);
				p.setElementtype(castexpression.getType());
				expdata.currentvex.addSymbolDomain(sym, p);
				expdata.value=new Expression(sym);
				return data;
			}else if(o.equals("*")){
				//add by zhouhb
				//2011.12.15
				String image=node.getImage();
		    	if(!image.equals("")){
		    		//�������
		    		Scope scope=node.getScope();
		        	NameDeclaration decl=Search.searchInVariableAndMethodUpward(image, scope);
		        	if(decl instanceof VariableNameDeclaration){
		        		//��������
		        		VariableNameDeclaration v=(VariableNameDeclaration)decl;
		        		expdata.value=expdata.currentvex.getValue(v);
		        	}
		        	if(expdata.value==null){
		        		
		        	}
		        	return data;
		        		//end by zhouhb
		        	}
			}else if(o.equals("+")){
				//������
			}else if(o.equals("-")){
				if(expdata.value!=null){
					expdata.value=new Expression(0).sub(expdata.value);
				}else{
					SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
					expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0,1));
					expdata.value=new Expression(sym);
				}
				return data;
			}else if(o.equals("~")){
				if(expdata.value==null)
				{
					return data;
				}
				IntegerDomain i=Domain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
				if(i!=null&&i.isCanonical()){
					expdata.value=new Expression(new IntegerFactor(~i.getMin()));
					return data;
				}
			}else if(o.equals("!")){
				if(expdata.value==null){
					SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
					expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0,1));
					expdata.value=new Expression(sym);
					return data;
				}
				IntegerDomain i=Domain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
				if(i!=null){
					if(i.isCanonical()&&i.getMin()==0){
						expdata.value=new Expression(1);
						return data;
					}else if(!i.contains(0)){
						expdata.value=new Expression(0);
						return data;
					}else{
						SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
						expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0,1));
						expdata.value=new Expression(sym);
						return data;
					}
				}
			}else{
				throw new RuntimeException("ASTUnaryOperator error!");
			}
		}else if(node.getImage().equals("sizeof")){
			node.jjtGetChild(0).jjtAccept(this, expdata);		
			ASTConstant con = (ASTConstant)node.getSingleChildofType(ASTConstant.class);
        	if(node.jjtGetChild(0) instanceof ASTTypeName){
				ASTTypeName typeName=(ASTTypeName)node.jjtGetChild(0);
				if(typeName.getType()!=null){
					expdata.value=new Expression(typeName.getType().getSize());
					return data;
				}
			}if(con != null){
				//liuli 2010.8.13 ��Ӷ�sizeof ("qwefff");��������Ĵ���
				Domain ret = expdata.value.getDomain(con.getCurrentVexNode().getSymDomainset());
				if(ret != null && ret instanceof PointerDomain){
					PointerDomain p = (PointerDomain)ret;
					long i = p.getLen(p);
					expdata.value=new Expression(i);
					return data;
				}
			}else{
				//liuli��sizeof�Ĳ����п���Ϊ�Զ�������ͣ����﷨���в�������ASTTypeNameʶ��
				Scope scope=node.getScope();
				String image = ((ASTUnaryExpression)node.jjtGetChild(0)).getImage();
	        	NameDeclaration decl=Search.searchInVariableUpward(image, scope);
	        	if(decl != null && decl.getType()!=null){
					expdata.value=new Expression(decl.getType().getSize());
					return data;
				}
	        	//end
				AbstractExpression child=(AbstractExpression)node.jjtGetChild(0);
				if(child.getType()!=null){
					if(child.getFirstChildOfType(ASTFieldId.class)!=null && ((AbstractExpression)child.getFirstChildOfType(ASTFieldId.class)).getType()!=null)
						child = (AbstractExpression) child.getFirstChildOfType(ASTFieldId.class);
					expdata.value=new Expression(child.getType().getSize());
					return data;
				}
			}
		}else if(node.getImage().contains("alignof")){
			//��֪����ô����
		}else if(node.getImage().contains("real")){
			//��֪����ô����
		}else if(node.getImage().contains("imag")){
			//��֪����ô����
		}else{
			//��֪����ô����
		}
		expdata.value=new Expression(SymbolFactor.genSymbol(node.getType()));
		return data;
	}
		
	private Object dealBinaryBitOperation(AbstractExpression node, Object data,String op) {
		ExpressionVistorData expdata=(ExpressionVistorData)data;
		node.jjtGetChild(0).jjtAccept(this, expdata);
		Expression leftvalue=expdata.value;
		Expression rightvalue=null;
		try{
			//�����ҽ��з��ż���
			for (int i = 1; i < node.jjtGetNumChildren(); i++) {
				Node c = node.jjtGetChild(i);
				c.jjtAccept(this, expdata);
				rightvalue = expdata.value;
				
				if(leftvalue==null || rightvalue==null){
					throw new MyNullPointerException("BinaryBitOperation Value NULL in(.i) "+node.getBeginFileLine());
				}
				
				IntegerDomain i1=Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
				IntegerDomain i2=Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
				if(i1==null || i2==null){
					throw new MyNullPointerException("BinaryBitOperation Domain NULL in(.i) "+node.getBeginFileLine());
				}
				
				//�������ȷ�������ֵ��ֱ�Ӳ���һ������ȡֵδ���ķ��ţ�����
				if(i1!=null&&i2!=null&&i1.isCanonical()&&i2.isCanonical()){
					long temp=0;
					if(op.equals("&")){
						temp=i1.getMin()&i2.getMin();
					}else if(op.equals("|")){
						temp=i1.getMin()|i2.getMin();
					}else if(op.equals("^")){
						temp=i1.getMin()^i2.getMin();
					}else if(op.equals(">>")){
						temp=i1.getMin()>>i2.getMin();
					}else if(op.equals("<<")){
						temp=i1.getMin()<<i2.getMin();
					}else if(op.equals("%")){
						temp=i1.getMin()%i2.getMin();
					}
					leftvalue=new Expression(new IntegerFactor(temp));
				}
				else if(i1!=null&&!i1.contains(0)&&i2!=null&&!i2.contains(0)&&op.equals("&"))
				{
					if(i1.getMin()>0||i2.getMin()>0)
					{
						SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
						expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(1,IntegerDomain.DEFAULT_MAX));
						leftvalue=new Expression(sym);
					}
					else
						throw new MyNullPointerException("BinaryBitOperation Domain Unknown in(.i) "+node.getBeginFileLine());
					
				}
				else{
					throw new MyNullPointerException("BinaryBitOperation Domain Unknown in(.i) "+node.getBeginFileLine());
				}
			}
		}catch(MyNullPointerException e){
			super.visit(node,expdata);
			SymbolFactor sym=SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
			if(Config.USEUNKNOWN)
				expdata.currentvex.addSymbolDomain(sym, IntegerDomain.getUnknownDomain());
			else
				expdata.currentvex.addSymbolDomain(sym, IntegerDomain.getFullDomain());
			leftvalue=new Expression(sym);
			expdata.value=leftvalue;
			return data;
		}
		expdata.value=leftvalue;
		return data;
	}
	
	static Logger logger=Logger.getRootLogger();
	class MyNullPointerException extends Exception{
		public MyNullPointerException(String msg){
			//logger.info("�����Զ����쳣��"+msg);
		}
	}
}
