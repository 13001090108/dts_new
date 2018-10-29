package softtest.rules.c.fault;

import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTInitDeclarator;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTRelationalExpression;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.SimpleNode;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.domain.c.symbolic.Expression;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_BaseType;

/**
 * Datatype OverFlow
 * ������������ڸ�ֵʱ��û�������Ŀǰֻ����������ͣ�(unsigned)int/char/short
 * 
 * <p>Ŀǰ���������⣺��ģʽ����Ҫ���ȫ�ֱ�����ʼ��ʱ��ֵ�Ƿ�Խ�磬Ҳ��Ҫ����ļ��ڲ�������ֵ���Ƚϱ��ʽ�Ƿ�
 * Խ�磬���Ա��Զ�������ȫ��������Scope="File")����Ҫ����·�����еģ��������ɴ�·�����󱨣�;��ʵ����
 * �޷�ʵ������ģʽ������Ŀǰ���Զ�������Ϊ��·�����У�
 * 2010.4.23
 * </p>
 * @author zys	
 * 2010-4-13
 */
public class DOFStateMachine {
	
	public static List<FSMMachineInstance> createDOFStateMachines(SimpleNode node, FSMMachine fsm) 
	{
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();   
		 //���ҵ�ǰԴ�ļ������еĸ�ֵ����,��������ʱ��ʼ����ֵ(InitDeclarator)����ʽ��ֵ(AssignmentExpression)
		String xpath = ".//InitDeclarator[count(*)=2] | " +
					   ".//AssignmentExpression[count(*)=3] | " +
					   ".//AssignmentExpression[/UnaryExpression/PostfixExpression[@Operators='++']] | "+
					   ".//AssignmentExpression[/UnaryExpression/PostfixExpression[@Operators='--']] | "+
					   ".//AssignmentExpression[/UnaryExpression[@Operators='++']/UnaryExpression/PostfixExpression] | "+
					   ".//AssignmentExpression[/UnaryExpression[@Operators='--']/UnaryExpression/PostfixExpression] |" +
					   ".//RelationalExpression|"+
					   //".//AssignmentExpression[not(./UnaryExpression)]"; 
					   ".//AssignmentExpression/UnaryExpression/PostfixExpression[@Operators='(']//AssignmentExpression[not(./UnaryExpression)]";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		
		for(SimpleNode tempNode : evaluationResults)
		{
			if(tempNode instanceof ASTInitDeclarator)
			{
				ASTDeclarator declarator=(ASTDeclarator) tempNode.jjtGetChild(0);
				CType type=declarator.getType();
				
				//����Ǻ�����Ա�����ʼ��ʱ��ֵ���������ᱨwarning
				if(tempNode.getParentsOfType(ASTFunctionDefinition.class).size()==0)
				{
					VariableNameDeclaration v=declarator.getVariableNameDeclaration();
					if(v!=null && v.getVariable()!=null)
					{
						Object value=v.getVariable().getValue();
						if(value.getClass() == Long.class)
						{
							try{
								long t=((Long)value).longValue();
								IntegerDomain valueDomain=new IntegerDomain(t,t);
								if(compareDomain(type, valueDomain))
								{
									addFSM(list, declarator, fsm, valueDomain,null);
									continue;
								}
							}catch(NumberFormatException e){
								//���parseLong(value)���ݹ�������ת���쳣����ʱֱ���ϱ�IP
								//e.printStackTrace();
								addFSM(list, declarator, fsm, IntegerDomain.getFullDomain(),null);
								continue;
							}
						}
					}
				}else
				{
					//����Ǻ����ڲ���ʼ��ʱ��ֵ����ͨ����������ʼ�����ʽ�����Ҳ��ֵ
						IntegerDomain valueDomain=domainBuilder(declarator);
						if(valueDomain==null || valueDomain.equals(new IntegerDomain(new IntegerInterval())))
							continue;
						if (compareDomain(declarator.getType(), valueDomain)) {
							addFSM(list, declarator, fsm, valueDomain,null);
							continue;
						}
				}
			} else if (tempNode instanceof ASTAssignmentExpression){
				ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode) tempNode.jjtGetChild(0)).getSingleChildofType(ASTPrimaryExpression.class);
				//chh  �����ʽ������AssignmentExpression
				if(tempNode.jjtGetNumChildren()==3||
							(tempNode.jjtGetNumChildren()==1&&tempNode.jjtGetChild(0) instanceof ASTUnaryExpression&&
									p!=null&&!p.isMethod())){
					if (p != null) {
						IntegerDomain valueDomain=domainBuilder(p);
						if(valueDomain==null || valueDomain.equals(new IntegerDomain(new IntegerInterval())))
							continue;
						if (compareDomain(p.getType(), valueDomain)) {
							addFSM(list, p, fsm, valueDomain,null);
							continue;
						}
					}
				}
				//chh  ���ʽAssignmentExpression[not(./UnaryExpression)]
				else
				{
						ExpressionValueVisitor expvst = new ExpressionValueVisitor();
						ExpressionVistorData visitdata = new ExpressionVistorData();
						if(tempNode.getCurrentVexNode()== null)continue;
						else  visitdata.currentvex = tempNode.getCurrentVexNode();

						visitdata.currentvex.setfsmCompute(true);
						expvst.visit( tempNode, visitdata);
						visitdata.currentvex.setfsmCompute(false);
						
						Expression value1 = visitdata.value;
						
						IntegerDomain valueDomain ;
						if(value1!=null&&visitdata.currentvex.getLastsymboldomainset()!=null)
							valueDomain= (IntegerDomain)value1.getDomain(visitdata.currentvex.getLastsymboldomainset());
						else continue;
						if(valueDomain==null || valueDomain.equals(new IntegerDomain(new IntegerInterval())))
							continue;
						if (compareDomain(((AbstractExpression)(tempNode.jjtGetChild(0))).getType(), valueDomain)) {
							addFSM(list, (SimpleNode)tempNode.jjtGetChild(0), fsm, valueDomain,null);
							continue;
						}
				}
			}else if (tempNode instanceof ASTRelationalExpression)
			{
				//Ŀǰ��ʱ�������ӵıȽϱ��ʽ����i+j<5	i<j+k	i<j*3
				SimpleNode left=(SimpleNode) tempNode.jjtGetChild(0);
				SimpleNode right=(SimpleNode) tempNode.jjtGetChild(1);
				IntegerDomain leftDomain=null;
				IntegerDomain rightDomain=null;
				ASTPrimaryExpression leftPri=(ASTPrimaryExpression) left.getSingleChildofType(ASTPrimaryExpression.class);
				if(leftPri!=null)
				{
					leftDomain=domainBuilder(leftPri);
				}
				ASTPrimaryExpression rightPri=(ASTPrimaryExpression) right.getSingleChildofType(ASTPrimaryExpression.class);
				if(rightPri!=null)
				{
					rightDomain=domainBuilder(rightPri);
				}
				
				//ȡ���ұ��ʽ����һ����������Ϣ��Ŀǰ�ٶ�����������ͬ��
				CType type=null;
				if(leftPri!=null)
					type=leftPri.getType();
				//�ж����������Ƿ��ͻ
				if(rightDomain!=null && type!=null)
				{
					if (compareDomain(type, rightDomain)) {
						addFSM(list, tempNode, fsm, leftDomain,rightDomain);
						continue;
					}
				}
			}
		}
		return list;
	}
	
	/**
	 * @param list
	 * @param node
	 * @param fsm
	 * @param domain1
	 * @param domain2
	 * ����Ǹ�ֵ���ʽ�������Զ�������domain2Ϊnull,���ɵ�desp������Ϊ������Ϣ
	 * ����ǱȽϱ��ʽ�������Զ�������domain2������ʽ�Ҳ����䣬desp��Ӧ�ؽ�������
	 */
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm,Domain domain1,Domain domain2) {
		//System.out.println("��ǰ������"+(num++)+"���Զ�����");
		FSMMachineInstance fsminstance = fsm.creatInstance();
		VariableNameDeclaration v=node.getVariableNameDeclaration();
		String varName="";
		CType type=null;
		if(v!=null)
		{
			varName=v.getImage();
			type=v.getType();
		}
		fsminstance.setRelatedASTNode(node);
		fsminstance.setReleatedVexNode(node.getCurrentVexNode());
		if(domain2==null)
		{
			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�еı���" + varName + ",��������Ϊ"+type
				+",��ֵ����Ϊ"+domain1+"�����������������");
		}
		else
		{
			ASTUnaryExpression left=(ASTUnaryExpression) node.jjtGetChild(0);
			ASTUnaryExpression right=(ASTUnaryExpression) node.jjtGetChild(1);
			ASTPrimaryExpression leftPri=(ASTPrimaryExpression) left.getSingleChildofType(ASTPrimaryExpression.class);
			ASTPrimaryExpression rightPri=(ASTPrimaryExpression) right.getSingleChildofType(ASTPrimaryExpression.class);

			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�еıȽϱ��ʽ��������" + leftPri.getImage() + "(����Ϊ��"+domain1+")���Ҳ����"+rightPri.getImage()
					+"����Ϊ("+domain2+")�����߱Ƚ�ʱ���������������");
		}
		
			list.add(fsminstance);
		
	}
	
	private static IntegerDomain domainBuilder(SimpleNode node)
	{
		VariableNameDeclaration var=null;
		Expression value=null;
		IntegerDomain domain=null;
		if(node instanceof ASTDeclarator)
		{
			var = ((ASTDeclarator)node).getVariableNameDeclaration();
			if(var==null)
				return null;
			value=node.getCurrentVexNode().getValue(var);
			((ASTDeclarator)node).setType(var.getType());
		}else if(node instanceof ASTPrimaryExpression)
		{
			var= ((ASTPrimaryExpression) node).getVariableDecl();
			if(var!=null)
			{
				value = node.getCurrentVexNode().getValue(var);
				((ASTPrimaryExpression)node).setType(var.getType());
			}else
			{
				//��������ҵ�������ֵ���ʽ
				ASTConstant cons=(ASTConstant) node.getSingleChildofType(ASTConstant.class);
				if(cons!=null)
				{
					long t=cons.getValue();
					domain=new IntegerDomain(t,t);
					return domain;
				}else
				{
					//����ǱȽϸ��ӵı��ʽ������if((i-j)<5)��ֱ�ӷ��ؿ���
					return null;
				}
			}
		}
		
		if(value==null)
		{
			domain=Domain.castToIntegerDomain(node.getCurrentVexNode().getVarDomainSet().getDomain(var));
		}else{
			domain = Domain.castToIntegerDomain(
										value.getDomain(node.getCurrentVexNode().getSymDomainset()));
		}
		
		return domain;
	}
	
	private static boolean compareDomain(CType type,IntegerDomain valueDomain)
	{
		if(type.getName().equals(CType_BaseType.charType.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.charType);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals(CType_BaseType.uCharType.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.uCharType);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals(CType_BaseType.shortType.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.shortType);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals(CType_BaseType.uShortType.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.uShortType);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals(CType_BaseType.intType.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.intType);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals(CType_BaseType.uIntType.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.uIntType);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals(CType_BaseType.sfrType.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.sfrType);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals(CType_BaseType.sfr16Type.toString()))
		{
			IntegerDomain fullDomain=IntegerDomain.getFullDomain(CType_BaseType.sfr16Type);
			if(fullDomain.getMax()<valueDomain.getMin() || fullDomain.getMin()>valueDomain.getMax())
			{
				return true;
			}
		}else if(type.getName().equals("pointer"))
		{
			//�ݲ�����ָ������
		}
		
		return false;
	}
}
