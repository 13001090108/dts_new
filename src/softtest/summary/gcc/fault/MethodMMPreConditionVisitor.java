package softtest.summary.gcc.fault;

import java.util.ArrayList;
import java.util.List;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTAssignmentOperator;
import softtest.ast.c.ASTCastExpression;
import softtest.ast.c.ASTDeclaration;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTExpressionStatement;
import softtest.ast.c.ASTInitDeclarator;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ConditionData;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.ClassScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_Pointer;

//��û�п��Ǳ������� 
public class MethodMMPreConditionVisitor extends CParserVisitorAdapter
		implements MethodFeatureVisitor
{
	
	class VisitData
	{
		List<VariableNameDeclaration> freelist = new ArrayList<VariableNameDeclaration>();
		MethodMMPreCondition preFeature = new MethodMMPreCondition();
		
		/**modified by xwt  2011.6.30 */
		/*֮ǰֻ�ܴ���һ����䣬������int *a = (int *)malloc(10);b =a;��bΪȫ�ֱ�����ʱ�޷���⣬���޸���չ*/
		List<VariableNameDeclaration> alloclist = new ArrayList<VariableNameDeclaration>();
		
		void clear()
		{
			freelist.clear();
			freelist = null;
			preFeature = null;
		}
	}

	private static MethodMMPreConditionVisitor instance;

	private MethodNameDeclaration methodDecl;

	private MethodMMPreConditionVisitor()
	{
	}

	public static MethodMMPreConditionVisitor getInstance()
	{
		if (instance == null)
		{
			instance = new MethodMMPreConditionVisitor();
		}
		return instance;
	}

	public void visit(VexNode vexNode)
	{
		if (vexNode.getTreenode() == null)
		{
			return;
		}
//		MethodMMPreCondition feature = new MethodMMPreCondition();

		// �������������Ӧ���﷨���ڵ�һ��Ϊfunction_definition��ctor_definition��dtor_definition
		SimpleNode node = vexNode.getTreenode();
		if (node == null)
		{
			return;
		}
		
		
//		System.err.println("wavericq_MethodMMPreConditionVisitor::visit::VexNode   " + vexNode + "   " + node);
		
		/*if(node.getFileName().indexOf("test2") != -1)
		{
			System.out.println("wavericq_MethodMMPreConditionVisitor::visit::VexNode   " + vexNode + "   " + node);
		}*/
		VisitData data = new VisitData();
		
		methodDecl = InterContext.getMethodDecl(vexNode);
		node.jjtAccept(instance, data);
		// ��������ĺ���������ӵ�����ժҪ��
		MethodSummary summary = InterContext.getMethodSummary(vexNode);
		if (summary != null && !data.preFeature.isEmpty())
		{
			summary.addPreCondition(data.preFeature);
			if (Config.INTER_METHOD_TRACE)
			{
				if (methodDecl != null)
				{
					System.err
							.println(methodDecl.getFullName() + " " + data.preFeature);
				}
			}
		}
	}
	
	public Object visit(ASTSelectionStatement sNode, Object data)
	{
		if(!(data instanceof VisitData))
		{
			return null;
		}
		VisitData vd = (VisitData)data;
		
		// ��һ���ж� �����ifһ�����ɴ�� �Ͳ���ֱ�����±��������﷨�� ��Ϊ�Ѿ���ɵ�֧�� �����ڻ������
		if (sNode.getImage().equals("if"))
		{

			List<VariableNameDeclaration> sl = (List<VariableNameDeclaration>) vd.freelist; // �Ƿ����
			MethodMMPreCondition sPreCon = vd.preFeature;
			VexNode sVex = sNode.getCurrentVexNode();
			int num = sNode.jjtGetNumChildren();
			if (num == 2)
			{
				//
				SimpleNode cNode = (SimpleNode) sNode.jjtGetChild(1);
				VexNode cVex = cNode.getCurrentVexNode();
				if (!cVex.getContradict())
				{
					// if(p)free(p) ���Ǿ�����һ������ Ҳ�кܶ������ ��ֻ��һ��if(p) ����if(p == NULL)
					/*if (sVex.getOccurrences().size() == 1)
					{
						NameOccurrence sno = sVex.getOccurrences().get(0);
						if (sno.getDeclaration() instanceof VariableNameDeclaration) // �᲻�����if(foo())�����
						{
							VariableNameDeclaration sVarDecl = (VariableNameDeclaration) sno
									.getDeclaration();
							List<VariableNameDeclaration> cl = new ArrayList<VariableNameDeclaration>();
							cNode.jjtAccept(this, cl);
							if (cl.contains(sVarDecl))
							{
								sl.add(sVarDecl);
							}
						}

					}*/
					ConditionData condition = cVex.getCondata();
					if(condition != null && condition.getDomainsTable() != null && condition.getDomainsTable().size() == 1)
					{
						//".//SelectionStatement/Expression/descendant-or-self::PrimaryExpression"
//						List<VariableNameDeclaration> cl = new ArrayList<VariableNameDeclaration>();
						VisitData cData = new VisitData();
						cNode.jjtAccept(this, cData);
						ASTExpression expression = (ASTExpression)sNode.findDirectChildOfType(ASTExpression.class).get(0);             //�ǲ���Ҫ��Щ�����ж� ��ֹ��ָ��
						ASTPrimaryExpression pe = (ASTPrimaryExpression)expression.findChildrenOfType(ASTPrimaryExpression.class).get(0);
						if(pe.getVariableDecl() != null)
						{
							VariableNameDeclaration varDecl = pe.getVariableDecl();
							for(VariableNameDeclaration var : cData.freelist)
							{
								if(varDecl.equals(var))
									sl.add(var);
							}
							
						}
						for(Variable var : cData.preFeature.getVariables())
						{
							sPreCon.addVariable(var, cData.preFeature.getDesp(var));
						}
//						cl.clear();
//						cl = null;
						cData.clear();
						cData = null;
						
					}

				}
			}
			else
				if (num == 3) // �����ì�ܱ�/���ɴ�·���أ����ǵıȽϴֲ�
				{
					SimpleNode c1Node = (SimpleNode) sNode.jjtGetChild(1);
					SimpleNode c2Node = (SimpleNode) sNode.jjtGetChild(2);
					VexNode c1Vex = c1Node.getCurrentVexNode();
					VexNode c2Vex = c2Node.getCurrentVexNode();
					if (c1Vex.getContradict())
					{
						if (!c2Vex.getContradict())
						{
							c2Node.jjtAccept(this, data);
						}

					}
					else
					{
						if (c2Vex.getContradict())
						{
							c1Node.jjtAccept(this, data);
						}
						else
						{
//							List<VariableNameDeclaration> cl1 = new ArrayList<VariableNameDeclaration>();
//							List<VariableNameDeclaration> cl2 = new ArrayList<VariableNameDeclaration>();
							VisitData c1Data = new VisitData();
							VisitData c2Data = new VisitData();
							c1Node.jjtAccept(this, c1Data);
							c2Node.jjtAccept(this, c2Data);
							if (c1Data.freelist.size() > 0 && c2Data.freelist.size() > 0)
							{
								for (VariableNameDeclaration cv1 : c1Data.freelist)
								{
									for (VariableNameDeclaration cv2 : c2Data.freelist)
									{
										if (cv1.equals(cv2))           //if(cv1 == cv2)
											sl.add(cv1);
									}
								}
							}
							for(Variable var : c1Data.preFeature.getVariables())
							{
								sPreCon.addVariable(var, c1Data.preFeature.getDesp(var));
							}
							for(Variable var : c2Data.preFeature.getVariables())
							{
								sPreCon.addVariable(var, c2Data.preFeature.getDesp(var));
							}
//							cl1.clear();
//							cl2.clear();
//							cl1 = null;
//							cl2 = null;
							c1Data.clear();
							c2Data.clear();
							c1Data = null;
							c2Data = null;

						}
					}
				}
		}
		else
		{
			super.visit((SimpleNode) sNode, data);
		}

		return null;
	}
	
	//����ô֪�������֮ǰ��ֵ����Ϣ lastdomain������ֵ��׼��
	public Object visit(ASTPrimaryExpression pNode, Object data)             //������Ϊprimary��Ҷ�ӽڵ㣬û���ӽڵ�    ����ô
	{
//		super.visit((SimpleNode) pNode, data);
		
		if(!(data instanceof VisitData))
		{
			return null;
		}
		VisitData vd = (VisitData)data;
		
		List<VariableNameDeclaration> flist = (List<VariableNameDeclaration>)vd.freelist; // �Ƿ����
		
		/**modified by xwt  2011.6.30 */
		List<VariableNameDeclaration> alist = (List<VariableNameDeclaration>)vd.alloclist;
		
		MethodMMPreCondition preCon = vd.preFeature;
		VexNode pVex = pNode.getCurrentVexNode();
		
		if (pNode.isMethod() && pNode.getMethodDecl() != null)
		{
			MethodNameDeclaration methodDecl = pNode.getMethodDecl();
			if(methodDecl.getImage().equals("free") && methodDecl.getParameterCount() == 1)  //��ô�õ�free/malloc�Ĳ���    MND����
			{
				SimpleNode argu = (SimpleNode)pNode.getNextSibling();     // ���ǲ���Ҫ�Ӹ��ж� Ԥ����ָ��
				ASTUnaryExpression parameter = (ASTUnaryExpression) argu.getFirstChildOfType(ASTUnaryExpression.class);
				//ASTPrimaryExpression parameter = (ASTPrimaryExpression)argu.getFirstChildOfType(ASTPrimaryExpression.class);
				VariableNameDeclaration pVarDecl = parameter.getVariableDecl(); // ���ǲ���Ҫ�Ӹ��ж�
				pVex.setfsmCompute(true);
				Domain pLastDomain = pVex.getDomain(pVarDecl);
				pVex.setfsmCompute(false);
				Domain pDomain = pVex.getDomain(pVarDecl);
				if(pLastDomain instanceof PointerDomain && pDomain instanceof PointerDomain)
				{
					PointerDomain pld = (PointerDomain)pLastDomain;
					PointerDomain pd = (PointerDomain)pDomain;
					if(pld.getValue() != PointerValue.NULL && pd.getValue() == PointerValue.NULL)          //???
					{
						flist.add(pVarDecl);
					}
				}			
			}
			
			else
				if(methodDecl.getImage().equals("malloc") && methodDecl.getParameterCount() == 1)               //�����Լ򻯿�����Ϊȫ�ֱ����ں�����ĸ��Ʋ������������� ֻ���Ǹ�ֵ
				{
					SimpleNode expressionStatement = pVex.getTreenode();   //�ܱ�֤ô
					if (expressionStatement instanceof ASTExpressionStatement)
					{
						SimpleNode leftAssign = (SimpleNode) expressionStatement
								.getFirstChildOfType(ASTAssignmentExpression.class);
						ASTUnaryExpression pe = (ASTUnaryExpression) leftAssign.getFirstChildOfType(ASTUnaryExpression.class);
						//ASTPrimaryExpression pe = (ASTPrimaryExpression) leftAssign.getFirstChildOfType(ASTPrimaryExpression.class);
						VariableNameDeclaration varDecl = pe.getVariableDecl();
						boolean find = false;
						for (VariableNameDeclaration var : flist)
						{
							if (varDecl.equals(var))
							{
								find = true;
								break;
							}
						}
						if (!find)
						{
							if (isMMVar(varDecl, pNode))
							{
								Variable variable = Variable
										.getVariable(varDecl);
								if (!preCon.contains(variable))
								{
									preCon.addVariable(variable, "ȫ�ֱ���" + variable.getName()+"�ڵ�"
											+ pNode.getBeginLine() + "��," + "ָ����malloc������ڴ�");
								}
							}
							/*modified by xwt  2011.6.30 */
							else{
								if(varDecl!=null&&varDecl.getType() instanceof CType_Pointer){
									varDecl.setType(new CType_Pointer());
								    alist.add(varDecl);
								    
								}
							}
						}
					}
					/*modified by xwt  2011.6.30 */
					else{
						if(expressionStatement instanceof ASTDeclaration){
							ASTDirectDeclarator dd = (ASTDirectDeclarator) expressionStatement
							.getFirstChildOfType(ASTDirectDeclarator.class);
							VariableNameDeclaration varDecl = (VariableNameDeclaration) dd.getDecl();
							if(varDecl.getType() instanceof CType_Pointer){
								varDecl.setType(new CType_Pointer());
							    alist.add(varDecl);
							}
						}
					}
				}
				else
				{
					if(methodDecl.getMethodSummary() != null)
					{
						MethodMMPreCondition preMethod = (MethodMMPreCondition) methodDecl.getMethodSummary().findMethodFeature(MethodMMPreCondition.class);
						if(preMethod != null)
						{
							for(Variable var : preMethod.getVariables())
							{
								boolean find = false;
								for(VariableNameDeclaration varDecl : flist)
								{
									Variable variable = Variable.getVariable(varDecl);
									if(variable!=null&&variable.equals(var))
									{
										find = true;
										break;
									}
								}
								if(!find)
								{
									if(!preCon.contains(var))
									{
										preCon.addVariable(var, "ȫ�ֱ���" + var.getName()+"�ڵ�"
												+ pNode.getBeginLine() + "��," + "ָ����" + pNode.getImage() +"������ڴ�");
									}
								}
							}
						}
					}
				}
		}
		
		return null;
	}
	
	/**modified by xwt  2011.6.30 */
	public Object visit(ASTAssignmentExpression pNode, Object data) {
		if(!(data instanceof VisitData))
		{
			return null;
		}
		VisitData vd = (VisitData)data;
		String xpath = ".//PrimaryExpression[@Method='true']";	
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(pNode, xpath);
		if(pNode.jjtGetNumChildren() == 3 && evaluationResults.size()==0)
		{
			List<VariableNameDeclaration> alist = (List<VariableNameDeclaration>)vd.alloclist;
			MethodMMPreCondition preCon = vd.preFeature;
			SimpleNode operator = (SimpleNode)pNode.jjtGetChild(1);
			if("=".equals(operator.getOperators()))
			{
				ASTUnaryExpression left;
				if(pNode.jjtGetChild(0) instanceof ASTUnaryExpression)
				{
					left = (ASTUnaryExpression)pNode.jjtGetChild(0);
				}else
				{
					left = (ASTUnaryExpression)pNode.getFirstChildOfType(ASTUnaryExpression.class);
				}
				ASTUnaryExpression right = (ASTUnaryExpression) ((ASTAssignmentExpression)pNode.jjtGetChild(2))
				.getFirstChildOfType(ASTUnaryExpression.class);
				VariableNameDeclaration varDecll = left.getVariableDecl();
				VariableNameDeclaration varDeclr = right.getVariableDecl();
				boolean find = false;
				for (VariableNameDeclaration var : alist)
				{
					//xwt�����ұ��ʽ�ұߵı����Ƿ�ָ�������ڴ�
					if (varDeclr!=null && varDeclr.equals(var))
					{
						find = true;
						break;
					}
				}
				if(varDecll!=null&&!isMMVar(varDecll, pNode) && find){//��ߵı�����mm�������ұ߱���heap
					
					varDecll.setType(new CType_Pointer());
					alist.add(varDecll);
				}
				if(varDecll!=null&&isMMVar(varDecll, pNode))
				{
					 Variable variable = Variable
					 .getVariable(varDecll);
				     if (variable!=null &&!preCon.contains(variable))
		             {
					       preCon.addVariable(Variable
							.getVariable(varDecll),"ȫ�ֱ���" + variable.getName()+"�ڵ�"
							  + pNode.getBeginLine() + "��," + "��ָ�븳ֵ");
		             }
		        }			
			}	
		}
		else
		{
			super.visit((SimpleNode) pNode, data);
		}
		return null;
	}

	/*
	 * @Override public Object visit(ASTnew_expression node, Object data) {
	 * MethodMMPreCondition feature = (MethodMMPreCondition) data;
	 * VariableNameDeclaration varDec = findAssignVar(node); if(isMMVar(varDec,
	 * node)) { Variable variable = Variable.getVariable(varDec); if(variable !=
	 * null) { if(feature.contains(variable)) { //����ǰ�������д˱���ǰ����Ϣ��ɾ��ԭ��¼����¼�µ�
	 * feature.removeVariable(variable); } if (Config.DTS_LANGUAGE ==
	 * Config.LANG_ENGLISH) { feature.addVariable(variable, node.getFileName() +
	 * " on line " + node.getBeginLine()+ " in function \"" +
	 * methodDecl.getName() + "\""); } else if (Config.DTS_LANGUAGE ==
	 * Config.LANG_CHINESE) { feature.addVariable(variable, "���ļ�" +
	 * node.getFileName() + "�к���" + methodDecl.getName() + "��" +
	 * node.getBeginLine() + "�д������ڴ�"); } } } return super.visit(node, data); }
	 */
	/*private List<SimpleNode> frees = new ArrayList<SimpleNode>();
	*//**
	 * ���������ù����У�����ժҪ�Ĵ���
	 *//*
	public Object visit(ASTPrimaryExpression pNode, Object data)
	{
		MethodMMPreCondition feature = (MethodMMPreCondition) data;
		if (pNode.isMethod() && pNode.getMethodDecl() != null)
		{
			MethodNameDeclaration methodDecl = pNode.getMethodDecl();
			if(methodDecl.getImage().equals("free") && methodDecl.getParameterCount() == 1)
			{
				if(pNode.getFirstParentOfType(ASTSelectionStatement.class) == null)
				{
					frees.add(pNode);
				}
				
			}
			else
				if(methodDecl.getImage().equals("malloc") && methodDecl.getParameterCount() == 1)
				{
					VariableNameDeclaration varDecl = findAssignVar(pNode);
					if(isMMVar(varDecl, pNode))
					{
						if(frees.size() == 0)
						{
							Variable variable = Variable.getVariable(varDecl);
							if (variable != null)
							{
								if (feature.contains(variable))
								{ // ����ǰ�������д˱���ǰ����Ϣ��ɾ��ԭ��¼����¼�µ�
									feature.removeVariable(variable);
								}
								if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
								{
									feature.addVariable(variable, pNode.getFileName()
											+ " on line " + pNode.getBeginLine()
											+ " in function \"" + methodDecl.getImage()
											+ "\"");
								}
								else
									if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
									{
										feature.addVariable(variable, "���ļ�"
												+ pNode.getFileName() + "�к���"
												+ methodDecl.getImage() + "��"
												+ pNode.getBeginLine() + "�д������ڴ�");
									}
							}
						}
					}
				}
				else
				{
					MethodMMPreCondition preMMFeature = (MethodMMPreCondition) methodDecl.getMethodSummary().findMethodFeature(MethodMMPreCondition.class);
					// �˺�����ǰ����Ϣ
					if (preMMFeature != null && frees.size() == 0)
					{
						for (Variable variable : preMMFeature.getVariables())
						{
							if (feature.contains(variable))
							{ // ����ǰ�������д˱���ǰ����Ϣ�����ټ�¼
								continue;
							}
							ArrayList<String> desp = preMMFeature
									.getDesp(variable); // �˱����ĵ�ǰǰ����Ϣ����
							if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
							{
								desp.add(pNode.getFileName() + " on line "
										+ pNode.getBeginLine()
										+ " in function \""
										+ this.methodDecl.getImage() + "\"");
							}
							else
								if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
								{
									// ������������
									desp.add("���ļ�" + pNode.getFileName()
											+ "�к���"
											+ this.methodDecl.getImage() + "��"
											+ pNode.getBeginLine() + "�д�");
								}
							feature.addVariable(variable, desp);
						}
					}

				}
		}
		
		return null;
	}*/

	/*public Object visit(ASTPrimaryExpression node, Object data)
	{
		// public Object visit(ASTid_expression node, Object data) {
		MethodMMPreCondition feature = (MethodMMPreCondition) data;
		if (node.isMethod() && node.getMethodDecl() != null)
		{
			MethodNameDeclaration methodDecl = (MethodNameDeclaration) node
					.getMethodDecl();
			if (methodDecl.getImage().equals("malloc"))
			{
				VariableNameDeclaration varDec = findAssignVar(node);
				if (isMMVar(varDec, node))
				{
					Variable variable = Variable.getVariable(varDec);
					if (variable != null)
					{
						if (feature.contains(variable))
						{ // ����ǰ�������д˱���ǰ����Ϣ��ɾ��ԭ��¼����¼�µ�
							feature.removeVariable(variable);
						}
						if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
						{
							feature.addVariable(variable, node.getFileName()
									+ " on line " + node.getBeginLine()
									+ " in function \"" + methodDecl.getImage()
									+ "\"");
						}
						else
							if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
							{
								feature.addVariable(variable, "���ļ�"
										+ node.getFileName() + "�к���"
										+ methodDecl.getImage() + "��"
										+ node.getBeginLine() + "�д������ڴ�");
							}
					}
				}
				return null;
			}
			MethodSummary mtSummary = methodDecl.getMethodSummary();
			if (mtSummary != null)
			{
				for (MethodFeature preFeature : mtSummary.getPreConditions())
				{
					if (preFeature instanceof MethodMMPreCondition)
					{ // �˺�����ǰ����Ϣ
						MethodMMPreCondition preMMCondition = (MethodMMPreCondition) preFeature; // �˺�����ǰ����Ϣ
						for (Variable variable : preMMCondition.getVariables())
						{
							if (feature.contains(variable))
							{ // ����ǰ�������д˱���ǰ����Ϣ�����ټ�¼
								continue;
							}
							ArrayList<String> desp = preMMCondition
									.getDesp(variable); // �˱����ĵ�ǰǰ����Ϣ����
							if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
							{
								desp.add(node.getFileName() + " on line "
										+ node.getBeginLine()
										+ " in function \""
										+ this.methodDecl.getImage() + "\"");
							}
							else
								if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
								{
									// ������������
									desp.add("���ļ�" + node.getFileName() + "�к���"
											+ this.methodDecl.getImage() + "��"
											+ node.getBeginLine() + "�д�");
								}
							feature.addVariable(variable, desp);
						}
					}
				}
			}
		}
		return null;
	}*/

	/*
	 * @Override public Object visit(ASTqualified_id node, Object data) {
	 * MethodMMPreCondition feature = (MethodMMPreCondition)data;
	 * if(node.isMethod() && node.getNameDeclaration() != null) {
	 * MethodNameDeclaration methodDecl = (MethodNameDeclaration)
	 * node.getNameDeclaration(); MethodSummary mtSummary =
	 * methodDecl.getMethodSummary(); if(mtSummary != null) { for(MethodFeature
	 * preFeature : mtSummary.getPreConditions()) { if(preFeature instanceof
	 * MethodMMPreCondition) { MethodMMPreCondition preMMCondition =
	 * (MethodMMPreCondition) preFeature; for(Variable variable :
	 * preMMCondition.getVariables()) { if(feature.contains(variable)) {
	 * //����ǰ�������д˱���������Ϣ�����ټ�¼ continue; } ArrayList<String> desp =
	 * preMMCondition.getDesp(variable); if (Config.DTS_LANGUAGE ==
	 * Config.LANG_ENGLISH) { desp.add(node.getFileName() + " on line " +
	 * node.getBeginLine() + " in function \"" + this.methodDecl.getName() +
	 * "\""); } else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE) { //������������
	 * desp.add("���ļ�" + node.getFileName() + "�к���" + this.methodDecl.getName() +
	 * "��" + node.getBeginLine() + "�д�"); } feature.addVariable(variable, desp);
	 * } } } } } return null; }
	 */

	private static boolean isMMVar(VariableNameDeclaration var, SimpleNode node)
	{
		if (var == null)
			return false;
		if (var.getType() == null)
			return false;
		if (!(var.getType() instanceof CType_Pointer))
		{ // �Ƿ�Ϊָ������
			return false;
		}
		if (!(var.getScope() instanceof ClassScope || var.getScope() instanceof SourceFileScope))
		{ // �Ƿ�Ϊȫ�ֻ������Ա����
			return false;
		}

		VexNode vex = node.getCurrentVexNode();
		if (vex == null || vex.getContradict())
		{ // �Ƿ��ڲ��ɴ�·��
			return false;
		}
		return true;
	}

	private static SimpleNode findAssignExp(SimpleNode node)
	{
		/*
		 * SimpleNode parent = node;
		 * 
		 * while(!(parent instanceof ASTstatement)){ if(parent instanceof
		 * ASTassignment_expression){
		 * if(parent.getOperatorTypeString().equals("[=]")) return parent; }
		 * if(parent.jjtGetParent()!=null){ parent
		 * =(SimpleNode)parent.jjtGetParent(); }else return null; } return null;
		 */
		SimpleNode parent = node;
		while (!(parent instanceof ASTStatement))
		{
			if (parent instanceof ASTAssignmentExpression)
			{

				if (parent.jjtGetNumChildren() == 3)
				{
					if (parent.jjtGetChild(1) instanceof ASTAssignmentOperator)
					{
						ASTAssignmentOperator aao = (ASTAssignmentOperator) parent
								.jjtGetChild(1);
						if (aao.getOperators().equals("="))
						{
							return parent;
						}
					}
				}

				// ArrayList<String> optrs = parent.getOperatorType();
				// if (optrs.size() == 1 && optrs.get(0).equals("="))
				// {
				// return parent;
				// }
			}
			else
				if (parent instanceof ASTInitDeclarator
						&& parent.jjtGetNumChildren() == 2)
				{
					return parent;
				}
				else
					if (parent == null)
					{
						return null;
					}
			parent = (SimpleNode) parent.jjtGetParent();
		}
		return null;
	}

	public static VariableNameDeclaration findAssignVar(SimpleNode node)
	{
		/*
		 * SimpleNode assExp = findAssignExp(node); if(assExp == null) return
		 * null; ExpressionDomainVisitor exp = new ExpressionDomainVisitor();
		 * Object re = exp.visit((SimpleNode)assExp.jjtGetChild(0), new
		 * DomainData(false)); if (!(re instanceof VariableNameDeclaration)) {
		 * return null; } return (VariableNameDeclaration)re;
		 */
		VariableNameDeclaration result = null;

		SimpleNode assignExp = findAssignExp(node);
		if (assignExp instanceof ASTInitDeclarator)
		{
			ASTDirectDeclarator dnode = (ASTDirectDeclarator) assignExp
					.getFirstChildOfType(ASTDirectDeclarator.class);
			result = dnode.getVariableNameDeclaration();
		}
		else
			if (assignExp instanceof ASTAssignmentExpression)
			{
				SimpleNode snode = (SimpleNode) assignExp.jjtGetChild(0);
				ASTPrimaryExpression pnode = (ASTPrimaryExpression) snode
						.getFirstChildOfType(ASTPrimaryExpression.class);
				result = pnode.getVariableDecl();
			}

		return result;
	}
}
