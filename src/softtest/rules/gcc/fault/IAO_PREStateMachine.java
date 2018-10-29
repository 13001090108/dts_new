package softtest.rules.gcc.fault;

import java.util.*;

import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.*;
import softtest.ast.c.*;
import softtest.domain.c.interval.*;
import softtest.domain.c.symbolic.Expression;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.*;
import softtest.summary.gcc.fault.IAOType;
import softtest.summary.gcc.fault.MethodIAOPreCondition;
import softtest.summary.gcc.fault.MethodIAOPreConditionVisitor;
import softtest.summary.gcc.fault.MethodIAOPreCondition.IAOInfo;
import softtest.symboltable.c.*;

public class IAO_PREStateMachine extends BasicStateMachine {

	@Override
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodIAOPreConditionVisitor.getInstance());
	}
	static int i=1;
	public static List<FSMMachineInstance> createIAOStateMachines(SimpleNode node, FSMMachine fsm) {	
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		//�ҵ����еĺ������ýڵ�
		String xPath = ".//PrimaryExpression[@Method='true']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		
		for (SimpleNode snode : evaluationResults) {
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(snode);
			if(methodDecl == null || methodDecl.getMethodSummary() == null)
				continue;	
			//����ú����й��ڷǷ������ǰ����Ϣ
			MethodIAOPreCondition feature = (MethodIAOPreCondition)methodDecl.getMethodSummary().findMethodFeature(MethodIAOPreCondition.class);
			if (feature == null) {
				continue;
			}
			Set<Variable> variables = feature.getIAOVariables();
			ExpressionValueVisitor expvst = new ExpressionValueVisitor();

			//step1:���������ǰ����Ϣ�������ǶԲα�����Լ��
			for(Variable var : variables) {
				IAOInfo iaoInfo = feature.getIAOInfo(var);
				if(var.isParam()) { 
					SimpleNode param = null;
					if(snode instanceof ASTPrimaryExpression) {
						param = StateMachineUtils.getArgument(snode, var.getParamIndex());
					}
					else {
						//�쳣�������ȿ���
						SimpleNode decl = (SimpleNode) snode.getFirstParentOfType(ASTDeclaration.class);
						param = (SimpleNode) decl.jjtGetChild(1);
					}
					if(param == null) {
						System.out.println("Func is " + snode.getImage() + " Index is : " +var.getParamIndex() );
						continue;
					}
					ExpressionVistorData visitdata = new ExpressionVistorData();
					visitdata.currentvex = param.getCurrentVexNode();
					visitdata.currentvex.setfsmCompute(true);
					expvst.visit(param, visitdata);
					visitdata.currentvex.setfsmCompute(false);
					Expression value1 = visitdata.value;
					Domain mydomain=null;
					if(value1!=null)
					mydomain = value1.getDomain(visitdata.currentvex.getSymDomainset());
					if(mydomain == null)
						continue;
					//���myDomain����iaoInfo����ô���һ��fsmInstance
					if(isContain(mydomain, iaoInfo)) {
						FSMMachineInstance fsminstance = fsm.creatInstance();
//						System.out.println("������IAO_PRE�����ǲ����ĵ�"+i+"��IAO_PRE״̬��ʵ�����ڵ� "+ snode.getBeginLine()+ "�У���"+snode.getBeginColumn()+"������״̬���������� ");
//						i++;
						addFSMDescript(fsminstance, snode, fsm, iaoInfo, list);
					}
				}
			}
			
			//step2:���������ǰ����Ϣ�������Ƕ�ȫ�ֱ�������Ա������Լ��
			if(snode.getCurrentVexNode() == null)
				continue;
			//��øú�������֮ǰ�ı����򼯺�
			VexNode vexNode=snode.getCurrentVexNode();
			if(vexNode == null)
				continue;
			for(Variable var : variables) {
				IAOInfo iaoInfo = feature.getIAOInfo(var);
				if(!var.isParam()) { //���ǲα���������ȫ�ֱ����ͳ�Ա����
					VariableNameDeclaration varDecl = findLocalVariableDecl(var, node);
					if(varDecl == null)
						continue;
					Domain domain = vexNode.getDomain(varDecl);
					if(domain == null)
						continue;
					if(isContain(domain, iaoInfo)) {
						FSMMachineInstance fsminstance = fsm.creatInstance();
//						System.out.println("������IAO_PRE�����ǲ����ĵ�"+i+"��IAO_PRE״̬��ʵ�����ڵ� "+ snode.getBeginLine() + "�У���"+snode.getBeginColumn()+"������״̬���������� ");
//						i++;
						addFSMDescript(fsminstance, snode, fsm, iaoInfo, list);
					}
				}
			}
		}
		return list;
	}
	
	public static VariableNameDeclaration findLocalVariableDecl(Variable variable, SimpleNode node) {
		Scope scope = node.getScope().getEnclosingClassScope();
		if (scope == null) {
			scope = node.getScope().getEnclosingSourceFileScope();
		}
		if (scope != null) {
			Set<VariableNameDeclaration> vars = scope.getVariableDeclarations().keySet();
			for (VariableNameDeclaration var : vars) {
				if (var.getVariable() == variable) {
					return var;
				}
			}
		}
		return null;
	}

	
	/*
	 * �ж�domain����iaoInfo�����������Ϸ���true,�����Ϸ���false
	 * ���domain����ֵ��unkown������false
	 */
	public static boolean isContain(Domain domain, IAOInfo iaoInfo) {
		if(domain == null || domain.getDomaintype()==DomainType.UNKNOWN)
			return false;
		IAOType iaoType = iaoInfo.getIAOType();
		if(domain instanceof IntegerDomain) {
			return intergerIsContain((IntegerDomain)domain, iaoType);
		}
		if(domain instanceof DoubleDomain) {
			return doubleIsContain((DoubleDomain)domain, iaoType);
		}
		return false;
	}
	
	/*
	 * �ṩ��IAOStateMachine���õĽӿ�
	 * �ж�domain����IAOType�����������Ϸ���true,�����Ϸ���false
	 * ���domain����ֵ��unkown������false
	 */
	public static boolean isContain(Domain domain, IAOType type) {
		if(domain == null || domain.getDomaintype()==DomainType.UNKNOWN)
			return false;
		if(domain instanceof IntegerDomain) {
			return intergerIsContain((IntegerDomain)domain, type);
		}
		if(domain instanceof DoubleDomain) {
			return doubleIsContain((DoubleDomain)domain, type);
		}
		return false;
	}
	
	/*
	 * ����һ��double���͵ı�������ֵ���ж��Ƿ�ֻ����iaoTpye
	 */
	public static boolean doubleIsContain(DoubleDomain domain, IAOType iaoType) {
		TreeSet<DoubleInterval> intervalSets = domain.getIntervals();
		Iterator<DoubleInterval> itr = intervalSets.iterator();
		while(itr.hasNext()) {
			DoubleInterval interval = itr.next();
			if(isContain(interval, iaoType)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * ����һ��Double��interval���ж��Ƿ����iaoType
	 */
	private static boolean isContain(DoubleInterval interval, IAOType iaoType) {
		double max = interval.getMax();
		double min = interval.getMin();
		//������ʼ��Ϊ[-inf, inf]
		if(max == DoubleDomain.DEFAULT_MAX && min == DoubleDomain.DEFAULT_MIN) 
			return true;
		boolean isContainZero = interval.contains(0);	
		switch(iaoType) {
		case NO_ZERO:
			if(isContainZero)
				return true;
			break;
		case NO_BELOW_ZERO:
			if(min < 0)
				return true;
			break;
		case ABOVE_ZERO:
			if(min <= 0)
				return true;
			break;
		case TRI_LIMIT:
			if(min < -1 || max > 1)
				return true;
			break;
		}
		return false;
	}

	/*
	 * ����һ�����ͱ�������ֵ���ж��Ƿ�ֻ����iaoTpye
	 */
	public static boolean intergerIsContain(IntegerDomain domain, IAOType iaoType) {
		TreeSet<IntegerInterval> intervalSets = domain.getIntervals();
		Iterator<IntegerInterval> itr = intervalSets.iterator();
		while(itr.hasNext()) {
			IntegerInterval interval = itr.next();
			if(isContain(interval, iaoType)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * ����һ������interval���ж��Ƿ����iaoType
	 */
	private static boolean isContain(IntegerInterval interval, IAOType iaoType) {
		long max = interval.getMax();
		long min = interval.getMin();
		//������ʼ��Ϊ[-inf, inf]
		if(max == IntegerDomain.DEFAULT_MAX && min == IntegerDomain.DEFAULT_MIN) 
			return true;	// fixed by cmershen,2017.6.1
		boolean isContainZero = interval.contains(0);	
		switch(iaoType) {
		case NO_ZERO:
			if(isContainZero)
				return true;
			break;
		case NO_BELOW_ZERO:
			if(min < 0)
				return true;
			break;
		case ABOVE_ZERO:
			if(min <= 0)
				return true;
			break;
		case TRI_LIMIT:
			if(min < -1 || max > 1)
				return true;
			break;
		}
		return false;
	}

	/*
	 * ״̬��ʵ������
	 */
	private static void addFSMDescript(FSMMachineInstance fsminstance, SimpleNode snode, FSMMachine fsm, IAOInfo iaoInfo, List<FSMMachineInstance> list) {
		fsminstance.setRelatedASTNode(snode);
		String funcName = iaoInfo.getFuncName();
		fsminstance.setDesp(funcName + "�������ô��ڷǷ�����" );
		fsminstance.setTraceinfo(iaoInfo.getDes());
		list.add(fsminstance);
	}

}
