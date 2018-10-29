package softtest.summary.gcc.fault;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import softtest.ast.c.ASTCastExpression;
import softtest.ast.c.ASTDeclaration;
import softtest.ast.c.ASTMultiplicativeExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ConditionData;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.StateMachineUtils;
import softtest.rules.gcc.fault.IAOFunction;
import softtest.rules.gcc.fault.IAO_PREStateMachine;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.VariableNameDeclaration;


public class MethodIAOPreConditionVisitor extends CParserVisitorAdapter implements MethodFeatureVisitor {

	private static MethodIAOPreConditionVisitor instance;
	private MethodNameDeclaration methodDecl = null;
	
	//����if�ж���������޵�ȫ��/�α���
	private Set<SymbolFactor> ifVarSet = null;
	
	private MethodIAOPreConditionVisitor() {	
	}
	
	public static MethodIAOPreConditionVisitor getInstance() {
		if (instance == null) {
			instance = new MethodIAOPreConditionVisitor(); 
		}
		return instance;
	}
	
	public void visit(VexNode vexNode) {
		if (vexNode.getTreenode() == null) {
			return;
		}
		MethodIAOPreCondition feature = new MethodIAOPreCondition();	
		SimpleNode node = vexNode.getTreenode();
		methodDecl = InterContext.getMethodDecl(vexNode);
		
		ifVarSet = new HashSet<SymbolFactor>();
		//�ҵ�����if�ж���������޵�ȫ��/�α���
		findExistIfStatementVar(node);
		if(node==null || feature==null){
			return;
		}
		if (methodDecl != null) {
			node.jjtAccept(this, feature);
		}
		if (feature.isEmpty()) {
			return;
		}
		// ��������ĺ���������ӵ�����ժҪ��
		MethodSummary summary = InterContext.getMethodSummary(vexNode);
		if (summary != null) {
			summary.addPreCondition(feature);
			if (Config.INTER_METHOD_TRACE) {
				if (methodDecl != null) {
					System.err.println(methodDecl.getFullName() + " " + feature);
				}
			}
		}
	}
	
	//�ҵ�����if�ж���������޵�ȫ��/�α���
	private void findExistIfStatementVar(SimpleNode treeNode) {
		String if_xpath = ".//SelectionStatement[@Image='if']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(treeNode, if_xpath);
		Iterator<SimpleNode> itr = evaluationResults.iterator();
		while(itr.hasNext()) {
			SimpleNode snode = itr.next();
			VexNode vex = snode.getConcreteNode().getCurrentVexNode();
			if(vex == null || vex.getCondata() == null || vex.getCondata().getDomainsTable() == null ) {
				continue;
			}
			ConditionData conData=vex.getCondata();
			Set<SymbolFactor> syms= conData.getDomainsTable().keySet();
			for(SymbolFactor sym: syms) {
				//����Ѿ�������ifVarSet�еĻ�������
				//������ǷǾֲ�����Ӧ�������������ڴ˴�ͨ�������޷���ȡ������Ϣ ���д�����
				if(ifVarSet.contains(sym))
					continue;
				//���if�ж��д���var����Must����Domain����unknown��Ҳ����empty
				if(conData.getMustDomain(sym) != null && !conData.getMustDomain(sym).equals(new IntegerDomain(IntegerDomain.DEFAULT_MIN,IntegerDomain.DEFAULT_MIN)) && !conData.isMustContradict()) {
					ifVarSet.add(sym);	
				}
			}
		}
	}

	/**
	 * 1. �����[/]��[/=]��[%]��[%=]�Ĳ���
	 * 2. ����math�����漰�Ƿ�����ĺ�������
	 */
	@Override
	public Object visit(ASTStatement node, Object data) {		
		MethodIAOPreCondition feature = (MethodIAOPreCondition)data;
			
		//1�� �����[/]��[/=]��[%]��[%=]�Ĳ������������ĸֱ���ǵ����������Ǳ��ʽ
		String xpath = ".//MultiplicativeExpression[@Operators='/' or @Operators='%'] |.//AssignmentExpression/AssignmentOperator[@Operators='/=' or @Operators='%=']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		for (SimpleNode snode : evaluationResults) {
			//��MultiplicativeExpression��AssignmentExpression����
			if (snode instanceof ASTMultiplicativeExpression) {
				if(snode.jjtGetNumChildren() != 2)
					continue;
			}else{
				if(snode.jjtGetParent().jjtGetNumChildren() != 3)
					continue;
			}
			ExpressionValueVisitor expvst = new ExpressionValueVisitor();
			ExpressionVistorData visitdata = new ExpressionVistorData();
			visitdata.currentvex = snode.getCurrentVexNode();
			visitdata.currentvex.setfsmCompute(true);
			Domain mydomain=null;
			Object re=null;
			if(snode instanceof ASTMultiplicativeExpression){
				//expvst.visit((SimpleNode)snode.jjtGetChild(1), visitdata);
				//add by zl,20110919
				ASTUnaryExpression aaa;
				if(snode.jjtGetChild(1) instanceof ASTCastExpression)
				{
					aaa=(ASTUnaryExpression)((SimpleNode)snode.jjtGetChild(1)).getFirstDirectChildOfType(ASTUnaryExpression.class);
					if(aaa==null)
					{
						aaa = (ASTUnaryExpression)snode.jjtGetChild(0);
					}
				}else
				{
					aaa = (ASTUnaryExpression)snode.jjtGetChild(1);					
				}
				expvst.visit((SimpleNode)aaa.jjtGetChild(0), visitdata);
				if(aaa.jjtGetChild(0).jjtGetNumChildren()==1)
					re=((SimpleNode)snode.jjtGetChild(1)).getVariableNameDeclaration();
				else if(aaa.jjtGetChild(0).jjtGetNumChildren()==2)
					re=Search.searchInVariableAndMethodUpward(((SimpleNode)aaa.jjtGetChild(0)).getImage(), snode.getScope());
				//re = ((SimpleNode)snode.jjtGetChild(1)).getVariableNameDeclaration();
				//end add 
				visitdata.currentvex.setfsmCompute(false);
				Expression value1 = visitdata.value;
				if(value1!=null)
				mydomain = value1.getDomain(visitdata.currentvex.getSymDomainset());
			}else{
				expvst.visit((SimpleNode)snode.jjtGetParent().jjtGetChild(2), visitdata);
				re = ((SimpleNode)snode.jjtGetParent().jjtGetChild(2)).getVariableNameDeclaration();
				visitdata.currentvex.setfsmCompute(false);
				Expression value1 = visitdata.value;
				if(value1!=null)
				mydomain = value1.getDomain(visitdata.currentvex.getSymDomainset());
			}
			if(!(re instanceof VariableNameDeclaration))
				continue;
			VariableNameDeclaration varDecl = (VariableNameDeclaration)re;
			isIAOVar(node, varDecl, mydomain, IAOType.NO_ZERO, feature, null);
		}
		
		
		//2: ��Կ⺯��,��������������Ϊ�⺯���Ĳ�������
		//��f(int i){ div(1, i); }�ɴ���; ��f2(int i){ div(1, i+5); }���ɴ���
		xpath = ".//PrimaryExpression[@Method='true']";
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		//�õ�math���¿���IAO�ĺ�����Ϣ
		List<IAOFunction> iaoFuncList = IAOFunction.getIAOList(); 
		
		for (SimpleNode snode : evaluationResults) {
			MethodNameDeclaration method = StateMachineUtils.getMethodNameDeclaration(snode);
			if(method == null || !method.isLib()) //ֻ����ϵͳ�⺯��
				continue;
			
			String methodName = method.getImage();
			IAOFunction function = null; //��¼��ǰ�ڵ���math���µ��ĸ�����
			for(IAOFunction iaoFunc : iaoFuncList) {
				if(methodName.equals(iaoFunc.getFuncName())) {
					function = iaoFunc;
					break;
				}
			}
			if(function == null)
				continue;
			
			//�õ��ÿ⺯�����ܳ���IAO��ʵ��AST�ڵ�
			SimpleNode param = null;
			if(snode instanceof ASTPrimaryExpression) {
				param = StateMachineUtils.getArgument(snode, function.getParamIndex());	
			}
			else {
				SimpleNode decl = (SimpleNode) snode.getFirstParentOfType(ASTDeclaration.class);
				param = (SimpleNode) decl.jjtGetChild(1);
			}
			//��ʵ��AST�ڵ������������
			ExpressionValueVisitor expvst = new ExpressionValueVisitor();
			ExpressionVistorData visitdata = new ExpressionVistorData();
			visitdata.currentvex = param.getCurrentVexNode();
			if(visitdata.currentvex!=null){
				visitdata.currentvex.setfsmCompute(true);
			}
			expvst.visit(param, visitdata);
			visitdata.currentvex.setfsmCompute(false);
			Expression value1 = visitdata.value;
			Domain mydomain=null;
			if(value1!=null)
			mydomain = value1.getDomain(visitdata.currentvex.getSymDomainset());
			Object re = param.getVariableNameDeclaration();
			if(!(re instanceof VariableNameDeclaration))
				continue;
			VariableNameDeclaration varDecl = (VariableNameDeclaration)re;
			isIAOVar(node, varDecl, mydomain, function.getType(), feature, null);
		}
		return super.visit(node, data);
	}
	
	
	private String getType(IAOType type) {
		switch(type) {
			case ABOVE_ZERO:
				return new String("����0");
			case NO_ZERO:
				return new String("��0");
			case NO_BELOW_ZERO:
				return new String("���ڵ���0");
			case TRI_LIMIT:
				return new String("����ȡֵӦΪ[-1,1]");
		}
		return new String("");
	}

	/** 
	 * �����Զ��庯�������ù��������µ�IAO����
	 */
	@Override
	public Object visit(ASTPrimaryExpression node, Object data) {
		MethodIAOPreCondition feature = (MethodIAOPreCondition)data;
		if (node.isMethod() && node.getMethodDecl()!= null) {
			// �����ú���������IAO��ǰ��Լ��
			MethodNameDeclaration calledMethod = (MethodNameDeclaration)node.getMethodDecl();
			if(calledMethod.getMethodSummary() == null)
				return null;
			// ��鱻���ú������Ƿ��ж��βε�IAOǰ��Լ��
			MethodIAOPreCondition iaoPreCond = (MethodIAOPreCondition) calledMethod.getMethodSummary().findMethodFeature(MethodIAOPreCondition.class);
			if(iaoPreCond == null || iaoPreCond.isEmpty())
				return null;	
			Set<Variable> vars = iaoPreCond.getIAOVariables();			
			for(Variable var: vars) {
				MethodIAOPreCondition.IAOInfo info = iaoPreCond.getIAOInfo(var);
				IAOType type = info.getIAOType();
				if(var.isParam()) {
					//�õ�ʵ��
					SimpleNode argNode = StateMachineUtils.getArgument(node, var.getParamIndex());
					ExpressionValueVisitor expvst = new ExpressionValueVisitor();
					ExpressionVistorData visitdata = new ExpressionVistorData();
					visitdata.currentvex = argNode.getCurrentVexNode();
					visitdata.currentvex.setfsmCompute(true);
					expvst.visit(argNode, visitdata);
					visitdata.currentvex.setfsmCompute(false);
					Expression value1 = visitdata.value;
					Domain mydomain=null;
					if(value1!=null)
					mydomain = value1.getDomain(visitdata.currentvex.getSymDomainset());
					Object re = argNode.getVariableNameDeclaration();
					if(!(re instanceof VariableNameDeclaration))
						continue;
					VariableNameDeclaration varDecl = (VariableNameDeclaration)re;
					String traceStr = "�����ڸú���������" + calledMethod.getImage() + " ������" +info.getDes();
					isIAOVar(node, varDecl, mydomain, type, feature, traceStr);
				} else {
					//�õ�ȫ�ֱ��������Ա����
					VariableNameDeclaration varDecl = IAO_PREStateMachine.findLocalVariableDecl(var, node);
					if(varDecl != null && node.getCurrentVexNode()!= null) {
						Domain domain = node.getCurrentVexNode().getDomain(varDecl);
						//������øú���ǰ���ñ���δ���ֹ������߸ñ������ֹ�����unknown���������൱��unknown, IAO����
						if(domain == null ||domain.getDomaintype()==DomainType.UNKNOWN) {
							String traceStr = "�����ڸú���������" + calledMethod.getImage() + " ������" +info.getDes();
							isIAOVar(node, varDecl, null, type, feature, traceStr);
						}
					}
				}
			}	
		}
		return null;
	}
	
	public  boolean isIAOVar(SimpleNode node, VariableNameDeclaration varDecl, Domain myDomain, IAOType type, MethodIAOPreCondition feature, String trace) {
		//�õ���ǰvarDecl��Ӧ��ȫ�ֱ��������ǲα��������ǳ�Ա����
		Variable var = Variable.getVariable(varDecl);
		if(var == null)
			return false;
		String restrict = getType(type);	
		//������Ϣ
		String traceStr = new String("");
		if(trace != null)
			traceStr = trace;
		
		//��������ǷǾֲ�����������domain����unknown
		if(myDomain == null || myDomain.isUnknown() ||  myDomain.equals(new IntegerDomain(IntegerDomain.DEFAULT_MIN,IntegerDomain.DEFAULT_MAX))) {
			//����ZX_IAO_PRE_8.cpp����IAOӦ���ϱ�,������Ϊ����ժҪ��Ϣ�洢
			if(ifVarSet.contains(varDecl)) {
				return false;
			}
			String des = null;	
			if(var.isParam()) {	
				des = methodDecl.getImage() + "���������" + (var.getParamIndex()+1) + "�α�����Լ������Ϊ" + restrict + traceStr;
			}else {
				des = methodDecl.getImage() + "������ȫ�ֱ���" + var.getName() +"Լ������Ϊ" + restrict + traceStr;
			}
			feature.addVariable(var, type, methodDecl.getImage(), des);
			return true;
		}
		return false;
	}
	
}
