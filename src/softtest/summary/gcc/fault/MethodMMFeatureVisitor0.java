package softtest.summary.gcc.fault;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTCompoundStatement;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTJumpStatement;
import softtest.ast.c.ASTParameterList;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.PointerDomain;
import softtest.interpro.c.InterContext;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.AbstractNameDeclaration;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_AllocType;
import softtest.symboltable.c.Type.CType_Pointer;
import softtest.symboltable.c.Type.CType_Typedef;

public class MethodMMFeatureVisitor0 extends CParserVisitorAdapter implements MethodFeatureVisitor
{
	private static MethodMMFeatureVisitor0 instance;
	
	public static MethodMMFeatureVisitor0 getInstance()
	{
		if (instance == null)
		{
			instance = new MethodMMFeatureVisitor0();
		}
		return instance;
	}
	
	private MethodMMFeatureVisitor0(){}

	public void visit(VexNode vexNode)
	{
		SimpleNode treeNode = vexNode.getTreenode();
		if(treeNode == null || !(treeNode instanceof ASTFunctionDefinition))
		{
			return;
		}
		
		//�����˶Ժ����������͵��ж�
		boolean returnPointer = false;
		ASTFunctionDefinition func = (ASTFunctionDefinition)treeNode;
		MethodNameDeclaration decl = func.getDecl();
		CType returnType = decl.getMethod().getReturnType();
		if(returnType instanceof CType_Typedef)
		{
			CType_Typedef typedef = (CType_Typedef)returnType;
			returnType = typedef.getOriginaltype();
		}
		if(returnType instanceof CType_Pointer)
		{
			returnPointer = true;
		}
		
		VisitData data = new VisitData();
		
		SimpleNode parameterList = (SimpleNode)treeNode.getFirstChildOfType(ASTParameterList.class);
		if(parameterList != null)
		{
			parameterList.jjtAccept(this, data);
		}
		List<AbstractNameDeclaration> parameters = data.vlist;
		
		if(parameters.size() == 0)
		{
			return; //???          �������û�е���
		}
		
		int paramIndex = -1;
		for(int i=0; i<parameters.size(); i++)
		{
			if(parameters.get(i) instanceof VariableNameDeclaration)
			{
				VariableNameDeclaration parameter = (VariableNameDeclaration)parameters.get(i);
				if(parameter.getType() instanceof CType_Pointer)
				{
					data.alias.add(parameter);
					paramIndex = i+1;
					break;                                                                                  //Ŀǰֻ������һ��ָ����������
				}
			}
		}
		
		data.vlist.clear();
		SimpleNode compoundStatement = (SimpleNode)treeNode.getFirstChildOfType(ASTCompoundStatement.class);
		if(compoundStatement != null)
		{
			compoundStatement.jjtAccept(this, data);
		}
		
		
		
		MethodMMFeature0 mlfFeature = new MethodMMFeature0();
		mlfFeature.setMMRelease(data.isAllocFree);
		if(returnPointer)
		{
			mlfFeature.setMMAllocate(data.isReturnAlloc);
		}
		mlfFeature.setParamIndex(paramIndex);
		
		MethodSummary summary = InterContext.getMethodSummary(vexNode);
		if(summary != null)
		{
			summary.addSideEffect(mlfFeature);
		}
	}
	
	public Object visit(ASTJumpStatement node, Object data)
	{
		if( !(data instanceof VisitData))
		{
			return null;
		}
		
		VisitData visitData = (VisitData)data;
		
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		if("return".equals(node.getImage()))
		{
			if(node.jjtGetNumChildren() > 0)
			{
				int sn = visitData.vlist.size();
				SimpleNode cnode = (SimpleNode)node.jjtGetChild(0);
				cnode.jjtAccept(this, visitData);
				int en = visitData.vlist.size() - sn;
				for(int i=0; i<en; i++)
				{
					AbstractNameDeclaration decl = visitData.vlist.get(i);
					if(decl instanceof VariableNameDeclaration)
					{
						VariableNameDeclaration varDecl = (VariableNameDeclaration)decl;
						
						vex.setfsmCompute(false);
						Domain varDomain = vex.getDomain(varDecl);
						
						if(varDomain instanceof PointerDomain)
						{
							PointerDomain pVarDomain = (PointerDomain)varDomain;
							if(pVarDomain.Type.contains(CType_AllocType.heapType))
							{
								visitData.isReturnAlloc = true;
								break;
							}
						}
					}
					if(decl instanceof MethodNameDeclaration)
					{
						MethodNameDeclaration methodDecl = (MethodNameDeclaration)decl;
						
						if(methodDecl.getMethodSummary() != null)
						{
							MethodMMFeature0 mlfFeature = (MethodMMFeature0)methodDecl.getMethodSummary().findMethodFeature(MethodMMFeature0.class);
							if(mlfFeature != null && mlfFeature.isMMAllocate())
							{
								visitData.isReturnAlloc = true;
								break;
							}
						}
						
						
						/*boolean flag = false;
						if("malloc".equals(methodDecl.getImage()) 
								|| "calloc".equals(methodDecl.getImage())
								|| "realloc".equals(methodDecl.getImage())
								|| "strup".equals(methodDecl.getImage()))
						{
							flag = true;
						}
						else
						{
							if(methodDecl.getMethodSummary() != null)
							{
								MethodMMFeature0 mlfFeature = (MethodMMFeature0)methodDecl.getMethodSummary().findMethodFeature(MethodMMFeature0.class);
								if(mlfFeature != null && mlfFeature.isMMAllocate())
								{
									flag = true;
								}
							}
						}
						
						if(flag)
						{
							visitData.isReturnAlloc = true;
							break;
						}*/
					}
				}
			}
			
		}
		else
		{
			super.visit(node, visitData);
		}
		
		return null;
	}
	
	/**û�п��ǲ��ɴ�·�������� �����������ķ�����û�п��ǲ��ɴ�·��������
	 * ���ڼ�����
	 * ����������������ϲ��Բ���
	 * */
	
	public Object visit(ASTSelectionStatement node, Object data)
	{
		if( !(data instanceof VisitData))
		{
			return null;
		}
		
		VisitData visitData = (VisitData)data;
		
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		if("if".equals(node.getImage()))
		{
			int sn = visitData.vlist.size();
			Node expression = node.jjtGetChild(0);
			expression.jjtAccept(this, visitData);
			int en = visitData.vlist.size() - sn;
			boolean flag = true;                                          //true��ʾif������ֻ��alias�еı����й�, alias�еı��������϶�ָ��ͬһ���ڴ�ռ�
			for(int i=0; i<en; i++)                                   //en����õ���1ô
			{
				AbstractNameDeclaration decl = visitData.vlist.get(i);
				if(!(decl instanceof VariableNameDeclaration) || !visitData.alias.contains(decl))
				{
					flag = false;
					break;
				}
			}
			
			Set<VariableNameDeclaration> origin = new HashSet<VariableNameDeclaration>(visitData.alias);
			if(node.jjtGetNumChildren() == 2)                                                                 //�����ϲ�û�п��ǲ��ɴ�·��������
			{
				Node cnode = node.jjtGetChild(1);
				cnode.jjtAccept(this, visitData);
				if(!flag || !visitData.isAllocFree)
				{
					visitData.alias.addAll(origin);
					visitData.isAllocFree = false;
				}
				
			}
			if(node.jjtGetNumChildren() == 3)
			{
				Node c1node = node.jjtGetChild(1);
				c1node.jjtAccept(this, visitData);
				Set<VariableNameDeclaration> tempAlias = visitData.alias;
				boolean tempFree = visitData.isAllocFree;
				visitData.alias = origin;
				
				Node c2node = node.jjtGetChild(2);
				c2node.jjtAccept(this, visitData);
				
				if(!(flag && (tempFree || visitData.isAllocFree)))
				{
					visitData.alias.addAll(tempAlias);
					visitData.isAllocFree = false;
				}
			}
		}
		else
		{
			super.visit(node, visitData);
		}
		
		
		return null;
	}
	
	public Object visit(ASTAssignmentExpression node, Object data)
	{
		if( !(data instanceof VisitData))
		{
			return null;
		}
		
		VisitData visitData = (VisitData)data;
		
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		if(node.jjtGetNumChildren() == 3)
		{
			SimpleNode operator = (SimpleNode)node.jjtGetChild(1);
			if("=".equals(operator.getOperators()))                     //if(operator.getOperatorType().size() == 1 && "=".equalsIgnoreCase(operator.getOperatorType().get(0)))
			{
				SimpleNode left = (SimpleNode)node.jjtGetChild(0);
				SimpleNode right = (SimpleNode)node.jjtGetChild(2);
				
				/**���Һ����˳Ѱ��Ҳ����=���ҽ����*/
				right.jjtAccept(this, visitData);
				
				//��ô����Խ����쳣�� java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
				if(visitData.vlist.size() == 0)
				{
					return null;
				}
				
				
				AbstractNameDeclaration rv = visitData.vlist.get(0);                                 //����������Ӧ�����õ���ʽ��a=b...=c���ұߵ����������ڴ���+=���ʽ
				
				left.jjtAccept(this, visitData);
				
				/*if(! (visitData.vlist.get(0) instanceof VariableNameDeclaration))
				{
					System.err.println("������ MethodMMFeatureVisitor0 ���� visit(ASTAssignmentExpression node");
					System.err.println(node.getFileName());
					System.err.println(node.getBeginLine());
					System.err.println(vex.getName());
					System.err.println(visitData.vlist.get(0));
				}*/
				
				/**����ᱨ����  ��ô����*/
//				VariableNameDeclaration lv = (VariableNameDeclaration)visitData.vlist.get(0);
				if(!(visitData.vlist.get(0) instanceof VariableNameDeclaration))
				{
					return null;
				}
				VariableNameDeclaration lv = (VariableNameDeclaration)visitData.vlist.get(0);
				
				/**�����Ƿ���Ҫ��һ���жϣ�lv!=null �� lv�����ڱ���left֮ǰvlist.get(0)
				 * Ӧ�ò��� =��ֵ�������߱�Ȼ��һ������ Ҫ��Ȼ���붼����ͬ��
				 * */
				if(rv instanceof VariableNameDeclaration)                                        //instanceofӦ���ܷ�null��
				{
					if(visitData.alias.contains(rv))
					{
						visitData.alias.add(lv);
					}
					else
					{
						visitData.alias.remove(lv);
					}
				}
				else
					if(rv instanceof MethodNameDeclaration)
					{
						visitData.alias.remove(lv);
					}
				
			}
			else
			{
				super.visit(node, data);
			}
		}
		else
		{
			super.visit(node, data);
		}
		
		return null;
	}
	
	public Object visit(ASTUnaryExpression node, Object data)
	{
		if( !(data instanceof VisitData))
		{
			return null;
		}
		
		VisitData visitData = (VisitData)data;
		
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		super.visit(node, visitData);
		
		if("++".equals(node.getOperators()) || "--".equals(node.getOperators()))
		{
			VariableNameDeclaration v = (VariableNameDeclaration)visitData.vlist.get(0);
			visitData.alias.remove(v);
		}
		
		return null;
	}
	
	public Object visit(ASTPostfixExpression node, Object data)
	{
		if( !(data instanceof VisitData))
		{
			return null;
		}
		
		VisitData visitData = (VisitData)data;
		
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		super.visit(node, visitData);
		
		if("++".equals(node.getOperators()) || "--".equals(node.getOperators()))
		{
			VariableNameDeclaration v = (VariableNameDeclaration)visitData.vlist.get(0);                  //ֻ������һ���� �ҿ϶���VariableNameDeclaration
			visitData.alias.remove(v);
		}
		
		return null;
	}
	
	public Object visit(ASTPrimaryExpression node, Object data)
	{
		if( !(data instanceof VisitData))
		{
			return null;
		}
		
		VisitData visitData = (VisitData)data;
		
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		super.visit(node, data);                       //�����������ô
		
		
		
		if(node.isMethod() && node.getMethodDecl() != null)
		{
			MethodNameDeclaration methodDecl = node.getMethodDecl();
			if(methodDecl.getMethodSummary() != null)
			{
				MethodMMFeature0 mlfFeature = (MethodMMFeature0)methodDecl.getMethodSummary().findMethodFeature(MethodMMFeature0.class);
				if(mlfFeature != null && mlfFeature.isMMRelease())
				{
					int paramIndex = mlfFeature.getParamIndex();
					SimpleNode argumentList = (SimpleNode)node.getNextSibling();
					
					/*if(paramIndex < 0)
					{
						System.err.println("ERROR! ParamIndex :: " + paramIndex);
					}*/
					
					
					if(argumentList != null && argumentList.jjtGetNumChildren() >= paramIndex && paramIndex > 0)
					{
						
						SimpleNode argument = (SimpleNode)argumentList.jjtGetChild(paramIndex-1);
						VisitData vd = new VisitData();
						super.visit((SimpleNode)argument, vd);
						for(int i=0; i<vd.vlist.size(); i++)
						{
							if(vd.vlist.get(i) instanceof VariableNameDeclaration)
							{
								VariableNameDeclaration v = (VariableNameDeclaration)vd.vlist.get(i);
								if(visitData.alias.contains(v))
								{
									visitData.isAllocFree = true;
//									visitData.alias.clear();                          //???Ϊʲôע�͵������
									break;
								}
								
							}
						}
					}
				}
			}
			
		}
		
		
		
		
		/*if(node.isMethod() && node.getMethodDecl() != null)
		{
			MethodNameDeclaration methodDecl = node.getMethodDecl();
			
			*//**������ز���*//*
			boolean flag = false;
			if(methodDecl != null && "free".equals(methodDecl.getImage()))
			{
				flag = true;
			}
			else
			{
				if(methodDecl.getMethodSummary() != null)
				{
					MethodMMFeature0 mlfFeature = (MethodMMFeature0)methodDecl.getMethodSummary().findMethodFeature(MethodMMFeature0.class);
					if(mlfFeature != null && mlfFeature.isMMRelease())
					{
						flag = true;
					}
				}
			}
			
			if(flag)
			{
				SimpleNode argumentList = (SimpleNode)node.getNextSibling();
				if(argumentList != null)
				{
					VisitData vd = new VisitData();
					super.visit((SimpleNode)argumentList, vd);
					for(int i=0; i<vd.vlist.size(); i++)
					{
						if(vd.vlist.get(i) instanceof VariableNameDeclaration)
						{
							VariableNameDeclaration v = (VariableNameDeclaration)vd.vlist.get(i);
							if(visitData.alias.contains(v))
							{
								visitData.isAllocFree = true;
//								visitData.alias.clear();
								break;
							}
							
						}
					}
				}
			}
			
			visitData.vlist.add(0, methodDecl);                          //�����������Ϊ�˸�ֵ���ʽ���ж�
		}*/
		
		
		
		
		else
		{
			VariableNameDeclaration v = node.getVariableDecl();
			if(v != null)
			{
				visitData.vlist.add(0, v);
			}
		}
		
		return null;
	}
	
	public Object visit(ASTDirectDeclarator node, Object data)
	{
		if( !(data instanceof VisitData))
		{
			return null;
		}
		
		VisitData visitData = (VisitData)data;
		
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		super.visit(node, data);                       //�����������ô
		
		if(node.getDecl() != null && node.getDecl() instanceof AbstractNameDeclaration)
		{
			AbstractNameDeclaration decl = (AbstractNameDeclaration)node.getDecl();
			visitData.vlist.add(decl);
		}
		
		return null;
	}
	
	public Object visit(ASTArgumentExpressionList node, Object data)                     //���������α���ASTArgumentExpressionList�ڵ�
	{
		return null;
	}
	
	public Object Visit(SimpleNode node, Object data)
	{
	
		VexNode vex = node.getCurrentVexNode();                         //ע����currentVexNode()����
		if(vex.getContradict())
		{
			return null;
		}
		
		node.childrenAccept(this, data);
		
		return null;
	}
	
	
	
	class VisitData
	{
		boolean isReturnAlloc = false;
		
		boolean isAllocFree = false;
		
		Set<VariableNameDeclaration> alias = new HashSet<VariableNameDeclaration>();
		
		List<AbstractNameDeclaration> vlist = new LinkedList<AbstractNameDeclaration>();                //����ṹ����add(0,...)����ô
		//��ʵvlist�����滻ΪVariableNameDeclaration varһ���ܹ�����ASTAssignmentExpression�����Ҫ�� ���ǿ��ǵ�return p? p: malloc ����if(p&q)ȡ����Щ���ʽ��primary�е�Declaration
	}
	
	
}
