package softtest.rules.gcc.fault;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTLogicalANDExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.domain.c.symbolic.Expression;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MethodSummary;
import softtest.summary.gcc.fault.MethodOOBPreCondition;
import softtest.summary.gcc.fault.MethodOOBPreConditionVisitor;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_BaseType;
/**
 * ������ժҪ������Խ���Զ���
 *   modified by chh
 * 2010-5-12
 */
public class OOB_PREStateMachine extends BasicStateMachine{

	public static List<FSMMachineInstance> createOOB_PREStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		/** ���ҵ�ǰ���������к������ýڵ㣬�Դ���MethodOOBPreCondition�ĺ�������OOB��� */
		String xPath=".//PrimaryExpression[@Method='true']";
		List<SimpleNode> funcNodeList = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode funcNode : funcNodeList) 
		{
			MethodNameDeclaration mnd=((ASTPrimaryExpression)funcNode).getMethodDecl();
			if(mnd==null){ continue;}
			MethodSummary ms=mnd.getMethodSummary();
			if(ms!=null)
			{
				if(funcNode.getFirstParentOfType(ASTLogicalANDExpression.class)!=null){
					continue;
				}
				MethodOOBPreCondition preCondition = (MethodOOBPreCondition)ms.findMethodFeature(MethodOOBPreCondition.class);
				if(preCondition!=null)
				{
					FSMMachineInstance fsmInstance = fsm.creatInstance();
					fsmInstance.setRelatedASTNode(funcNode);
					fsmInstance.setStateData(preCondition);
					list.add(fsmInstance);
				}
			}
		}
		return list;
	}
	
	public static boolean checkFunctionCallOOB(List<ASTPrimaryExpression> nodes, FSMMachineInstance fsmin)
	{
		//�Ƿ������ı�־λ
		boolean b=false;
		for(ASTPrimaryExpression funcNode:nodes)
		{
			if (!fsmin.getRelatedASTNode().equals(funcNode)) 
			{
				continue;
			}
			
			MethodOOBPreCondition preCondition = (MethodOOBPreCondition) fsmin.getStateData();
			Set<Variable> subScriptVarSet = preCondition.getSubScriptVariableSet();
			
			for (Variable subScriptVar : subScriptVarSet) 
			{
				IntegerInterval summaryInterval = preCondition.getSubScriptInterval(subScriptVar);
				
				//��ȡ����ǰ�ý�������������������
				Domain d=null;
				
				if(!subScriptVar.isParam())//ȫ�ֱ���
				{
					NameDeclaration var=Search.searchInVariableUpward(subScriptVar.getName(), funcNode.getScope());
					if(var!=null)
					{
						d=getDomain(funcNode, (VariableNameDeclaration) var);
					}
				}else{//�����βε��±�λ�ã��õ�ʵ�δ����±����,Ŀǰֻ���������������f(i) f(1),f(i+j)֮�����ʱ������
					if(funcNode.getNextSibling() == null)
						continue;
					ASTAssignmentExpression assignExpr=(ASTAssignmentExpression) funcNode.getNextSibling().jjtGetChild(subScriptVar.getParamIndex());
					ASTPrimaryExpression priExpr=(ASTPrimaryExpression) assignExpr.getSingleChildofType(ASTPrimaryExpression.class);
					if(priExpr!=null)
					{
						VariableNameDeclaration var=priExpr.getVariableNameDeclaration();
						if(var!=null)
						{
							//����β��Ǳ�������f(i)
							//add by nmh
							int idx = preCondition.getSubScriptIndex(subScriptVar);
							if( idx != -1)
							{
								VexNode vex = priExpr.getCurrentVexNode();
								ArrayList<VariableNameDeclaration> memlist = var.mems;
								if(memlist != null)
									d = vex.getDomain((VariableNameDeclaration)memlist.get(idx));
													
							}
							//
							else
								d=getDomain(funcNode, var);
						}else if(priExpr.isMethod()){
							MethodNameDeclaration method = priExpr.getMethodDecl();
							if(method.getMethod()!=null)
							d= method.getMethod().getReturnDomain();
						}
						else {
							//����β��ǳ���,����f(1)
							ASTConstant cons=(ASTConstant) priExpr.getSingleChildofType(ASTConstant.class);
							int value=0;
							if(cons!=null&&OOBStateMachine.isNum(cons.getImage()))
								value=Integer.parseInt(cons.getImage());
							d=new IntegerDomain(new IntegerInterval(value,value));
						}
					}else{
						//���ʵ�μȲ��ǵ���������Ҳ���ǵ���������������ʵ�α��ʽ��ֵ����f(a+b)
						ExpressionValueVisitor expvst = new ExpressionValueVisitor();
						ExpressionVistorData visitdata = new ExpressionVistorData();
						visitdata.currentvex = assignExpr.getCurrentVexNode();
	
						visitdata.currentvex.setfsmCompute(true);
						expvst.visit(assignExpr, visitdata);
						visitdata.currentvex.setfsmCompute(false);
						
						Expression value1 = visitdata.value;
						if(value1!=null)						
						   d = value1.getDomain(visitdata.currentvex.getLastsymboldomainset());
					}
				}
				
				if(d==null || Domain.isEmpty(d) || d.isUnknown())
					continue;
				//�����������±��ֵ������ֻ������������
				IntegerDomain temp= Domain.castToIntegerDomain(d);
//				if(temp.getMax()!=Long.MAX_VALUE&&temp.getMin()!=Long.MIN_VALUE&&
//						(temp.getMin()<summaryInterval.getMin()||temp.getMax()>summaryInterval.getMax()))
				if(confirmFuncOOB(temp, summaryInterval, 0))
				{
					if(fsmin.getDesp()==null || fsmin.getDesp().length()==0)
					{
						String desp = "�ڴ����"+funcNode.getBeginLine()+"����";
						fsmin.setDesp(desp+preCondition.getSubScriptDesp(subScriptVar));
					}else{
						String desp =fsmin.getDesp()+"\n";
						fsmin.setDesp(desp+preCondition.getSubScriptDesp(subScriptVar));
					}
					b=true;
				}
			}
		}
		return b;
	}
	
	/*
	 * ȷ�Ϻ��������Ƿ����OOB
	 * @param iDomain
	 * @param summaryInterval
	 * @return
	 * 
	 * ����һ������flag
	 * �����֪������Ҫ����Լ�����������flag = 0
	 * �����֪������Ҫ����Լ��������С����flag = 1
	 * ���ǵ���������δ֪����ƫ����֪�����
	 * 
	 */
	private static boolean confirmFuncOOB(IntegerDomain iDomain, IntegerInterval summaryInterval, int flag) {
		IntegerInterval interval = iDomain.jointoOneInterval();
		if (interval != null && !interval.isEmpty()) {
			long min, max;
			
			/**����interval�����ʽ��д���*/
			if (interval.isCanonical()) {
				max = min = interval.getMin();
			} else {
				if (interval.getMin() != Math.round(Double.NEGATIVE_INFINITY)) {
					min = interval.getMin();
				} else {
					min = interval.getMax();
				}
				
				max = interval.getMax();
			}
			
			if (flag == 0 && (min < summaryInterval.getMin() || max > summaryInterval.getMax())) {
				return true;
			}
			if(flag == 1 && ( max < summaryInterval.getMax())){
				return true;
			}
		}
		
		return false;
	}
	 
	private static Domain getDomain(SimpleNode funcNode, VariableNameDeclaration var)
	{
		Domain d=null;
		if(var==null)
			return d;
		
		VexNode vex=funcNode.getCurrentVexNode();
		d=vex.getVarDomainSet().getDomain(var);
		if(d==null && var.getScope() instanceof SourceFileScope)
		{//�����ȫ�ֱ��������������ڿ�����ͼ��δ֪������ͼ��ȡ���ʼ��ֵ��
			if(var.getVariable() != null)
			{
				Object initValue=var.getVariable().getValue();
				if(initValue!=null){
					try{
						long t=((Long)initValue).longValue();
						d=new IntegerDomain(t,t);
					}catch(Exception e){
						d=null;
					}
				}
			}
		}		
		return d;
	}
	
	@Override
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodOOBPreConditionVisitor.getInstance());
	}

}
