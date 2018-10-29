package softtest.rules.gcc.fault;

import java.util.*;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.domain.c.interval.PointerDomain;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MethodSummary;
import softtest.summary.gcc.fault.MethodBOPreCondition;
import softtest.summary.gcc.fault.MethodBOPreConditionVisitor;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_Array;
import softtest.domain.c.interval.Domain;

/**
 * <p>
 * To Check the Buffer Overflow fault in the gcc program.
 * </p>
 * 
 * @author chh
 */
public class BO_PREStateMachine extends BasicStateMachine
{
	/**
	 * <p>Creates all the BO_PRE State Machine instances.</p>
	 * 
	 * @param node the function entry AST node
	 * @param fsm the BO_PRE FILE State Machine
	 * @return BO_PRE State Machine instances
	 */
	
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodBOPreConditionVisitor.getInstance()); 
	}
	
	public static List<FSMMachineInstance> createB0_PREStateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		
		//�ҳ���ǰ�������еĺ������ýڵ�
		String xPath=".//PostfixExpression/PrimaryExpression[@Method='true'] ";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);

		
		//ƥ���Ƿ����BOPreCondition�ĺ���
		
		for (SimpleNode snode : evaluationResults) {
			ASTPrimaryExpression pnode=(ASTPrimaryExpression) snode;
			MethodNameDeclaration methodDecl =StateMachineUtils.getMethodNameDeclaration(pnode);
			MethodBOPreCondition boPrecon = null;
			
			if (methodDecl == null) {
				continue;
			}
			MethodSummary ms=methodDecl.getMethodSummary();
			if (ms!= null) {
				boPrecon =(MethodBOPreCondition)ms.findMethodFeature(MethodBOPreCondition.class);
			}

			if (boPrecon != null&&!methodDecl.isLib()) {
				FSMMachineInstance fsminstance = fsm.creatInstance();
				fsminstance.setRelatedASTNode(pnode);
				fsminstance.setStateData(methodDecl);//���ô��ں���ժҪ�ĺ����������Ա���ڽ����ж��Ƿ����BO
				list.add(fsminstance);
			}
		}
		return list;
	}
	
	
	/**
	 * <p>Checks the first class function.</p>
	 * 
	 * @param nodes
	 * @param fsmin
	 * @return
	 */
	public static boolean checkBufferFlow(List<SimpleNode> nodes, FSMMachineInstance fsmin) {
		Iterator<SimpleNode> nodeIterator = nodes.iterator();
		while (nodeIterator.hasNext()) {
			SimpleNode fnode = (SimpleNode) nodeIterator.next();
			if (fsmin.getRelatedASTNode() != fnode) {
				continue;
			}
			boolean result = false;
			String bufferName = "";
			String funcName = null;
			long  srcsize = 0,bufsize = 0,size=0;
			//���ڴ���BOǰ��Լ����������ժҪ�ĺ�����������Ĵ���
			MethodNameDeclaration methodDecl = (MethodNameDeclaration)fsmin.getStateData();	
				
			if (methodDecl == null)
				continue;
			MethodBOPreCondition boPreCon = (MethodBOPreCondition)methodDecl.getMethodSummary().findMethodFeature(MethodBOPreCondition.class);

			if (boPreCon == null)
				continue;
			funcName = methodDecl.getImage();
			String despmet = "";	
			if(!methodDecl.isLib()){
				//��ǰ����Ϊ�û��Զ���ĺ���
				Set<Variable> varList = boPreCon.getBOVariables();
				for (Variable var : varList) {//������ǰ�������е�ǰ��Լ��
					SimpleNode argNode = null;
					despmet = boPreCon.getDespmet(var).toString();
					if (var.isParam()) {//modified by nmh �����˺�����Ϊ���������
						argNode = StateMachineUtils.getArgument(fnode, var.getParamIndex());
						ASTPrimaryExpression priExpr=(ASTPrimaryExpression) argNode.getSingleChildofType(ASTPrimaryExpression.class);
						if(priExpr != null)
						{
							if(priExpr.isMethod())
							{
								Domain d = null;
								MethodNameDeclaration method = priExpr.getMethodDecl();
								if(method.getMethod()!=null)
									d= method.getMethod().getReturnDomain();
								if(d instanceof IntegerDomain)//���ﻹ�е㲻����
								{
									if(boPreCon.getflag(var)==true&&d!=null&&((IntegerDomain)d).getMax()!=Long.MAX_VALUE)
										size= ((IntegerDomain)d).getMax();
									else if(boPreCon.getflag(var)==false&&d!=null&&((IntegerDomain)d).getMin()!=Long.MIN_VALUE)
										size=((IntegerDomain)d).getMin();
									else continue;
								}
								else if(d instanceof PointerDomain)
								{
									PointerDomain pd = (PointerDomain)d;
									size = pd.offsetRange.getMax() - pd.offsetRange.getMin() + 1;
								}
							}
							else
							{
								if(boPreCon.getflag(var)==true&&argNode!=null)
									size=BOStateMachine.getsrclength((SimpleNode) argNode.getSingleChildofType(ASTPrimaryExpression.class));//���㵱ǰ�βε�ʵ������ֵ
								else if(boPreCon.getflag(var)==false&&argNode!=null)
									size=BOStateMachine.getbuflength((SimpleNode) argNode.getSingleChildofType(ASTPrimaryExpression.class));//���㵱ǰ�βε�ʵ������ֵ
							}
						}
					}
					else if(var.getType().isIntegerType()&&!var.isParam()){
						
						Domain d=null;
						NameDeclaration varDecl=Search.searchInVariableUpward(var.getName(), fnode.getScope());
						if(var!=null && varDecl != null)
						{
							VexNode vex=fnode.getCurrentVexNode();
							
							d=vex.getVarDomainSet().getDomain((VariableNameDeclaration)varDecl);
							if(d==null && varDecl.getScope() instanceof SourceFileScope)
							{//�����ȫ�ֱ��������������ڿ�����ͼ��δ֪������ͼ��ȡ���ʼ��ֵ��
								Object initValue=((VariableNameDeclaration) varDecl).getVariable().getValue();
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
						if(boPreCon.getflag(var)==true&&d!=null&&((IntegerDomain)d).getMax()!=Long.MAX_VALUE)//((IntegerDomain)mydomain).getMin()!=Long.MIN_VALUE
							size= ((IntegerDomain)d).getMax();
						else if(boPreCon.getflag(var)==false&&d!=null&&((IntegerDomain)d).getMin()!=Long.MIN_VALUE)
							size=((IntegerDomain)d).getMin();
						else continue;
					}
					else if(var.getType().isArrayType()&&!var.isParam()){
						VariableNameDeclaration varDecl = BOStateMachine.findLocalVariableDecl(var, fnode);
						if(varDecl == null)
							continue;
						SimpleNode node= varDecl.getNode();
						if(varDecl!=null&&varDecl.getType()!=null&&varDecl.getType() instanceof CType_Array)
						{
							CType curtype=varDecl.getType();
							CType type=curtype.getSimpleType();
							CType_Array atype=(CType_Array)type;
							size=atype.getSize();
							if(size==1)//CType_Array��getSize()������δ֪���鳤�ȵ��������1//Ŀǰ������char����
								size=0;
						}
					}
					else if(var.getType().isPointType() &&!var.isParam()){//����ָ��
						VariableNameDeclaration varDecl = BOStateMachine.findLocalVariableDecl(var, fnode);
						if(varDecl == null)
							continue;					
						VexNode vex=fnode.getCurrentVexNode();
						Domain d=vex.getVarDomainSet().getDomain((VariableNameDeclaration)varDecl);
						PointerDomain pdomain = null;
						if(d instanceof PointerDomain)
							pdomain = (PointerDomain)d;
						if(pdomain == null)
							continue;
						size = pdomain.offsetRange.getMax() - pdomain.offsetRange.getMin() + 1;
					}
				
					if(boPreCon.getflag(var)==true){
						
						bufsize = boPreCon.getsize(var);
						srcsize=size;
						if (bufsize!=0&&bufsize<srcsize) {//�Ƚ�Լ������͵�ǰ����ʱ�βε�ʵ������ֵ���ж��Ƿ����BO_PRE
							result = true;
							bufferName = boPreCon.getDespbuf(var).toString();
							break;
						}
					}
					else {
						
						srcsize = boPreCon.getsize(var);
						bufsize=size;
						if (bufsize!=0&&bufsize<srcsize) {//�Ƚ�Լ������͵�ǰ����ʱ�βε�ʵ������ֵ���ж��Ƿ����BO_PRE
							result = true;
							bufferName = boPreCon.getDespbuf(var).toString();
							break;
						}
					}
				}
			
					if (result) {
						String desp = "�ڵ�"+fnode.getBeginLine()+"ʹ���˺���"+funcName+"��"+despmet+"���ܵ��»����������"+bufferName+"Դ�������ռ�"+srcsize+",Ŀ�Ļ������ռ�"+bufsize;
						fsmin.setDesp(desp);
						
						return true;
					}
				}	
		}
		
		return false;
	}
}

