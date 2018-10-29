package softtest.rules.c.fault;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTConstant;
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
import softtest.summary.c.fault.MethodOOBPreCondition;
import softtest.summary.c.fault.MethodOOBPreConditionVisitor;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_BaseType;
/**
 * ������ժҪ������Խ���Զ���
 * @author zys	
 * 2010-5-12
 */
public class OOBSUMMARYStateMachine extends BasicStateMachine{

	public static List<FSMMachineInstance> createOOBSUMMARYStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		/** ���ҵ�ǰ���������к������ýڵ㣬�Դ���MethodOOBPreCondition�ĺ�������OOB��� */
		String xPath=".//PrimaryExpression[@Method='true']";
		List<SimpleNode> funcNodeList = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode funcNode : funcNodeList) 
		{
			MethodNameDeclaration mnd=((ASTPrimaryExpression)funcNode).getMethodDecl();
			MethodSummary ms=mnd.getMethodSummary();
			if(ms!=null)
			{
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
				
				//�����βε��±�λ�ã��õ�ʵ�δ����±����,Ŀǰֻ���������������f(i) f(1),f(i+j)֮�����ʱ������
				if(funcNode.jjtGetParent().jjtGetNumChildren()==1||!subScriptVar.isParam())
				{//�޲����ĺ������ã���f(); �������ǰ�������������ı�����ȻΪȫ�ֱ���    
				//���䣺��chh���в����ĺ������ã�ǰ�������������ı���Ҳ�п�����ȫ�ֱ���,���Լ��˱����Ƿ���ȫ�ֱ������ж�
					NameDeclaration var=Search.searchInVariableUpward(subScriptVar.getName(), funcNode.getScope());
					if(var!=null)
					{
						d=getDomain(funcNode, (VariableNameDeclaration) var);
					}
				}else{
					ASTAssignmentExpression assignExpr=(ASTAssignmentExpression) funcNode.getNextSibling().jjtGetChild(subScriptVar.getParamIndex());
					ASTPrimaryExpression priExpr=(ASTPrimaryExpression) assignExpr.getSingleChildofType(ASTPrimaryExpression.class);
					if(priExpr!=null)
					{
						VariableNameDeclaration var=priExpr.getVariableNameDeclaration();
						if(var!=null)
						{
							//����β��Ǳ�������f(i)
							d=getDomain(funcNode, var);
						}else{
							//����β��ǳ���,����f(1)
							ASTConstant cons=(ASTConstant) priExpr.getSingleChildofType(ASTConstant.class);
							int value=Integer.parseInt(cons.getImage());
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
						d = value1.getDomain(visitdata.currentvex.getLastsymboldomainset());
					}
				}
				
				if(d==null || Domain.isEmpty(d))
					continue;
				//�����������±��ֵ������ֻ������������
				Domain temp=Domain.intersect(new IntegerDomain(summaryInterval), d, CType_BaseType.intType);
				if(temp!=null && Domain.isEmpty(temp))
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
	
	private static Domain getDomain(SimpleNode funcNode, VariableNameDeclaration var)
	{
		Domain d=null;
		if(var==null)
			return d;
		
		VexNode vex=funcNode.getCurrentVexNode();
		d=vex.getVarDomainSet().getDomain(var);
		if(d==null && var.getScope() instanceof SourceFileScope)
		{//�����ȫ�ֱ��������������ڿ�����ͼ��δ֪������ͼ��ȡ���ʼ��ֵ��
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
		
		return d;
	}
	
	@Override
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodOOBPreConditionVisitor.getInstance());
	}

}
