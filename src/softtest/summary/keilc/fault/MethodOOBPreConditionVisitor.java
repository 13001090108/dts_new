package softtest.summary.keilc.fault;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.Power;
import softtest.domain.c.symbolic.Term;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_Array;

/**
 * ����Խ���ǰ������������
 * @author zys	
 * 2010-5-12
 */
public class MethodOOBPreConditionVisitor extends CParserVisitorAdapter implements MethodFeatureVisitor {

	private static MethodOOBPreConditionVisitor instance = null;
	private MethodNameDeclaration methodDecl = null;
	
	private MethodOOBPreConditionVisitor() {
	}
	
	public static MethodFeatureVisitor getInstance() {
		if (instance == null) {
			instance = new MethodOOBPreConditionVisitor();
		}
		return instance;
	}
	
	public void visit(VexNode vexNode) {
		SimpleNode node = vexNode.getTreenode();
		if (node == null) {
			return;
		}
		
		MethodOOBPreCondition feature = new MethodOOBPreCondition();
		
		methodDecl = InterContext.getMethodDecl(vexNode);
		if (methodDecl != null) {
			node.jjtAccept(instance, feature);
		}
		if (feature.isSubScriptMapEmpty()) {
			return;
		}
		
		MethodSummary mtSummary = InterContext.getMethodSummary(vexNode);
		if (mtSummary != null) {
			mtSummary.addPreCondition(feature);
		}
	}
	
	/** ����������ڲ����ں������ýڵ㣬��鿴�Ƿ���Ҫ�������ú�����ǰ���������д���*/
	public Object visit(ASTPrimaryExpression node, Object data)
	{
		if(node.isMethod())
		{
			calSummaryTransmit(node, (MethodOOBPreCondition) data);
		}
		return super.visit(node, data);
	}

	/** �ں����ڲ���������ʹ�ñ�����Ϊ�����±�Ľڵ㣬�����Ƿ���ҪΪ�ú������ɸ��±������ǰ������*/
	public Object visit(ASTPostfixExpression node, Object data)
	{
		if (node.jjtGetNumChildren() == 1 || !node.getOperators().startsWith("[")) 
		{
			return super.visit(node, data);
		}
		
		//ȡ��������,��������������λ�û�ȡ�䳤�ȣ�ά����
		ASTPrimaryExpression priExpr=(ASTPrimaryExpression) node.jjtGetChild(0);
		VariableNameDeclaration array=priExpr.getVariableNameDeclaration();
		
		//��ȡ�����ÿһά�±�
		int num=node.jjtGetNumChildren();
		//liuli
		if(array!= null && !(array.getType() instanceof CType_Array)){
			return super.visit(node, data);		
		}

		CType_Array temp = (CType_Array) array.getType();
		
		for(int i=1;i<num;i++)
		{	//liuli
			if(!(node.jjtGetChild(i) instanceof ASTExpression))
				continue;
			ASTExpression expNode = (ASTExpression) node.jjtGetChild(i);
			ASTPrimaryExpression subExpr=(ASTPrimaryExpression) expNode.getSingleChildofType(ASTPrimaryExpression.class);
			long length=temp.getDimSize();
			if(temp.getOriginaltype() instanceof CType_Array)
				temp=(CType_Array) temp.getOriginaltype();
			if(subExpr!=null)
			{//�����±�Ϊ������������a[i]
				VariableNameDeclaration subScript=subExpr.getVariableNameDeclaration();
				confirmOOBVar(new IntegerInterval(0,length-1), subScript, i-1, (MethodOOBPreCondition) data, node);
			}else{
				//�����±�Ϊ�������ӱ��ʽ����a[10] a[i*j],���������ʱ������
				
			}
		}
		return super.visit(node, data);
	}
	/**
	 * chh   ���������ȫ�ֱ������ߺ���������ֵ�������ڵľֲ������󽫴˱�����Ϊ�����±�ʹ�ã���ôӦ�÷����漰��ȫ��
	 * �Ͳ����������Ա�����ǰ�������ĺ���ժҪ��(Ŀǰֻ�ܴ����ڶԾֲ������ĸ�ֵ��ʹ���˵���ȫ�ֱ����򵥸��������������)
	 */
	private VariableNameDeclaration relationwith(VariableNameDeclaration varDecl, SimpleNode node)
	{
		List<VariableNameDeclaration> varlist=new  LinkedList<VariableNameDeclaration>();
		// ��ȫ�ֱ�����ӵ�varlist��
		Scope scope =methodDecl.getScope();
		Map<VariableNameDeclaration, ArrayList<NameOccurrence>> varOccs = scope.getVariableDeclarations();
		Iterator it=varOccs.keySet().iterator();
		VariableNameDeclaration key;
		while(it.hasNext())
		{ 
			key=(VariableNameDeclaration)it.next();
			if(key.getScope() instanceof SourceFileScope)
			varlist.add(key);
		}
		SimpleNode decNode=methodDecl.getMethodNameDeclaratorNode();
		if(decNode instanceof ASTFunctionDefinition)
		{
			String xpath=".//ParameterList/ParameterDeclaration/Declarator/DirectDeclarator";
			List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(decNode, xpath);
			Iterator<SimpleNode> itr = evaluationResults.iterator();
			while(itr.hasNext()) {
				SimpleNode snode = itr.next();
				VariableNameDeclaration parvar=snode.getVariableNameDeclaration();
				varlist.add(parvar);
			}
		}
		VexNode vex=node.getCurrentVexNode();
		Expression leftvalue=new Expression(0);
		if(vex!=null&&varDecl!=null)leftvalue=vex.getValue(varDecl);
		ArrayList<Term> list=new ArrayList<Term>();
		if(leftvalue!=null)
		list=leftvalue.getTerms();
		int _num=0;
		for(Term t:list){
			for(VariableNameDeclaration var:varlist){
				if(t.toString().contains(var.getImage()+"_"))
					{
						_num++;
						if(_num>1)return null;
						else return var;
					}
			}
		}
		return null;
	}
	/**
	 * �������������ʹ��ʱ���±����������ñ���Ϊ����������ȫ�ֱ�������Ϊ�ú������ɸ������±��ǰ��������
	 * ���Ϊ����������ͬʱ��ȡ�ò������ں����б��е�λ�ã�index��
	 * 
	 * ����Ǿֲ�������������ǰ��������
	 */
	private void confirmOOBVar(IntegerInterval integerInterval,VariableNameDeclaration varDecl, int index,MethodOOBPreCondition feature, SimpleNode node) 
	{
		
		Variable var=null;
		VariableNameDeclaration relationvar=null;
		if(varDecl!=null&&(varDecl.isParam() || varDecl.getScope() instanceof SourceFileScope))
		var= Variable.getVariable(varDecl);
		//��ȫ�ֱ�������������ֲ�������ʹ�þֲ�������Ϊ�����±꣬��Щȫ�ֱ��������ҲӦ������ǰ������
		else if(relationwith(varDecl,node)!=null)
		{
			relationvar=relationwith(varDecl,node);
			var=Variable.getVariable(relationwith(varDecl,node));
		}
		if (var == null) 
		{
			return;
		}
		
		String desp = "����" + methodDecl.getImage() + ";�����ļ���"+node.getBeginLine()+"��ʹ���˱���"+varDecl.getImage()+"���ܵ�������Խ��";
		/** ������������������ȫ�ֱ���,������βΣ���ͬʱ�����index */
		if (varDecl!=null&&(varDecl.isParam() || varDecl.getScope() instanceof SourceFileScope||relationvar!=null)) 
		{
			VexNode vex=node.getCurrentVexNode();
			Domain d=vex.getVarDomainSet().getDomain(varDecl);
			if(d!=null && !Domain.isEmpty(d)&&!d.equals(new IntegerDomain(new IntegerInterval())) && !(d.isUnknown()))
			{//����±������������֪���������ɺ���ժҪ��������ģʽ�����н��м��
				return;
			}
			if (feature.containsSubScriptVar(var)) {
				integerInterval = getNewInterval(feature.getSubScriptInterval(var), integerInterval);
			}
			//chh ���i��ֵ�����Ӽ��˳����㣬���Ҹ�ֵ���봫����أ���Ҫ���¼���i�Ŀ�ȡֵ��Χ; 
			//chh  ����i=i*k+k��k=4��i��ΪsizeΪ20�������±�ʱȡֵ��ΧΪ0-3
			Expression leftvalue=vex.getValue(varDecl);
			if(leftvalue!=null&&leftvalue.toString().contains("_"))
				{
				int offset=0,times=1,_num=0;	
				ArrayList<Term> list=leftvalue.getTerms();
					for(Term t:list)
					{
						if(!t.toString().contains("_"))
						{
							if(t.getOperator()=="+") 
								offset-=Integer.parseInt(t.toString());
							else if(t.getOperator()=="-") 
								offset+=Integer.parseInt(t.toString());
							else 
								return ;
						}
						else {
							_num++;
							if(_num>1||(!t.toString().contains(varDecl.getImage()+"_")&&(relationvar!=null&&!t.toString().contains(relationvar.getImage()+"_"))))
								return ;
							else 
							{
								 ArrayList<Power> listp=t.getPowers();
								 for(Power p:listp)
								 {
									 if(!p.toString().contains("_"))
										{
											if(p.getOperator()=="*") 
												times*=Integer.parseInt(p.toString());
											else 
												return;
										}
								 }
							}
						}
					}
					integerInterval=new IntegerInterval((integerInterval.getMin()+offset)/times,(integerInterval.getMax()+offset)/times);
					
				}
			//end  
			feature.addSubScriptVariable(var, integerInterval, desp);
		}
	}

	private void confirmOOBVar(IntegerInterval summaryInterval, VariableNameDeclaration varDecl, SimpleNode node, String oldDesp, MethodOOBPreCondition feature) {
		Variable var=null;
		VariableNameDeclaration relationvar=null;
		if(varDecl!=null&&(varDecl.isParam() || varDecl.getScope() instanceof SourceFileScope))
		var= Variable.getVariable(varDecl);
		//��ȫ�ֱ�������������ֲ�������ʹ�þֲ�������Ϊ�����±꣬��Щȫ�ֱ��������ҲӦ������ǰ������
		else if(relationwith(varDecl,node)!=null)
		{
			relationvar=relationwith(varDecl,node);
			var=Variable.getVariable(relationwith(varDecl,node));
		}
		
		String desp = "����" + methodDecl.getImage() + ";�����ļ���"+node.getBeginLine()+"�е�����"+oldDesp;
		
		if (varDecl!=null&&(varDecl.isParam() || varDecl.getScope() instanceof SourceFileScope||relationvar!=null)) 
		{
			VexNode vex=node.getCurrentVexNode();
			Domain d=vex.getVarDomainSet().getDomain(varDecl);
			if(d!=null && !Domain.isEmpty(d)&&!(Domain.castToIntegerDomain(d).getMax()==Long.MAX_VALUE&&Domain.castToIntegerDomain(d).getMin()==Long.MIN_VALUE)
					&&!(d.isUnknown()))
			{
				//����±������������֪���������ɺ���ժҪ��������ģʽ�����н��м��
				return;
			}
			if (feature.containsSubScriptVar(var)) {
				summaryInterval = getNewInterval(feature.getSubScriptInterval(var), summaryInterval);
			}
			//chh ���i��ֵ�����Ӽ��˳����㣬���Ҹ�ֵ���봫����أ���Ҫ���¼���i�Ŀ�ȡֵ��Χ; 
			//chh  ����i=i*k+k��k=4��i��ΪsizeΪ20�������±�ʱȡֵ��ΧΪ0-3
			Expression leftvalue=vex.getValue(varDecl);
			if(leftvalue!=null&&leftvalue.toString().contains("_"))
				{
				int offset=0,times=1,_num=0;	
				ArrayList<Term> list=leftvalue.getTerms();
					for(Term t:list)
					{
						if(!t.toString().contains("_"))
						{
							if(t.getOperator()=="+") 
								offset-=Integer.parseInt(t.toString());
							else if(t.getOperator()=="-") 
								offset+=Integer.parseInt(t.toString());
							else 
								return ;
						}
						else {
							_num++;
							if(_num>1||(!t.toString().contains(varDecl.getImage()+"_")&&(relationvar!=null&&!t.toString().contains(relationvar.getImage()+"_"))))
								return ;
							else 
							{
								 ArrayList<Power> listp=t.getPowers();
								 for(Power p:listp)
								 {
									 if(!p.toString().contains("_"))
										{
											if(p.getOperator()=="*") 
												times*=Integer.parseInt(p.toString());
											else 
												return;
										}
								 }
							}
						}
					}
					summaryInterval=new IntegerInterval(summaryInterval.getMin(),(summaryInterval.getMax()+offset)/times);
					
				}
			feature.addSubScriptVariable(var, summaryInterval, desp);
		}
	}
	
	private IntegerInterval getNewInterval(IntegerInterval x, IntegerInterval y) {
		long xMax = x.getMax(), yMax = y.getMax();
		
		return xMax > yMax ? new IntegerInterval(0, yMax) : new IntegerInterval(0, xMax);
	}
	
	/**���㱻���ú�����ǰ�������Ƿ���Ҫ���ϲ㺯������ */
	private void calSummaryTransmit(ASTPrimaryExpression node, MethodOOBPreCondition feature) {
		MethodNameDeclaration mnd=node.getMethodDecl();
		MethodSummary summary = null;
		if(mnd != null)//xwt:��ʱ�������
		summary = mnd.getMethodSummary();
		if(summary!=null)
		{
			MethodOOBPreCondition preCondition = (MethodOOBPreCondition) summary.findMethodFeature(MethodOOBPreCondition.class);
			if(preCondition!=null)
			{
				Set<Variable> subScriptVarSet = preCondition.getSubScriptVariableSet();
				for (Variable subScriptVar : subScriptVarSet) {
					if (subScriptVar.isParam()) {
						//����Ǻ�����������,���ҵ���Ӧλ�õ�ʵ��
						int index=subScriptVar.getParamIndex();
						if(index!=-1)
						{//Ŀǰֻ���Ǽ򵥵ĺ���������f(10) f(i)����ʱδ����f(i+j)
							ASTAssignmentExpression assignExpr=(ASTAssignmentExpression) node.getNextSibling().jjtGetChild(index);
							ASTPrimaryExpression priExpr=(ASTPrimaryExpression) assignExpr.getSingleChildofType(ASTPrimaryExpression.class);
							if(priExpr!=null)
							{
								VariableNameDeclaration varDecl=priExpr.getVariableNameDeclaration();
								confirmOOBVar(preCondition.getSubScriptInterval(subScriptVar), varDecl, node, preCondition.getSubScriptDesp(subScriptVar), feature);
							}
						}
					} else {
						//�����ȫ�ֱ�������
						NameDeclaration nd=Search.searchInVariableUpward(subScriptVar.getName(),node.getScope());
						if(nd instanceof VariableNameDeclaration)
						{
							confirmOOBVar(preCondition.getSubScriptInterval(subScriptVar), (VariableNameDeclaration)nd, node, preCondition.getSubScriptDesp(subScriptVar), feature);
						}
					}
				}
			}
		}
	}
}

