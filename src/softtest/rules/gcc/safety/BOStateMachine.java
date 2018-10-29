package softtest.rules.gcc.safety;



import java.util.*;

import softtest.cfg.c.Edge;
import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.ValueSet;
import softtest.ast.c.ASTAdditiveExpression;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.domain.c.symbolic.Expression;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;

import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.rules.gcc.fault.BO.BOFunction;
import softtest.rules.gcc.fault.BO.BOHelper;
import softtest.rules.gcc.fault.BO.BOType;
//import softtest.rules.c.StreamInRecord;


import softtest.summary.c.MethodSummary;
import softtest.summary.gcc.fault.MethodBOPreCondition;
import softtest.summary.gcc.fault.MethodBOPreConditionVisitor;

import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.NameOccurrence;

import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_Array;
import softtest.symboltable.c.Scope;



/**
 * <p>
 * To Check the Buffer Overflow fault in the gcc program.
 * </p>
 * @author chh
 */
public class BOStateMachine extends BasicStateMachine
{
	/**
	 * <p>Creates all the BO State Machine instances.</p>
	 * 
	 * @param node the function entry AST node
	 * @param fsm the BO FILE State Machine
	 * @return BO State Machine instances
	 */
	
	public void registFetureVisitors() {
		super.registFetureVisitors();
		InterContext.addPreConditionVisitor(MethodBOPreConditionVisitor.getInstance()); 
	}
	
	public static List<FSMMachineInstance> createB0StateMachines(SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		
		//�ҳ���ǰ�������еĺ������ýڵ�
		String xPath=".//PostfixExpression/PrimaryExpression[@Method='true']";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);

		
		//ƥ���Ƿ����BoFunction�еĺ����Լ��Ƿ����BOPreCondition�ĺ�����ע��BUFFERCOPY�ĺ���������BOFunction�У�
		
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

			if (boPrecon != null&&methodDecl.isLib()) {
				FSMMachineInstance fsminstance = fsm.creatInstance();
				fsminstance.setRelatedASTNode(pnode);
				fsminstance.setStateData(methodDecl);//���ô��ں���ժҪ�ĺ����������Ա���ڽ����ж��Ƿ����BO
				list.add(fsminstance);
			} else {
			 //û�к���ժҪ����ȥBOFunction��ƥ�亯������ҪΪFORMATSTRING������
				
				if(BOFunction.isBOfunc(methodDecl)!=null){
					FSMMachineInstance fsminstance = fsm.creatInstance();
					fsminstance.setRelatedASTNode(pnode);
					fsminstance.setStateData(BOFunction.isBOfunc(methodDecl));
					list.add(fsminstance);
				}
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
	out:	while (nodeIterator.hasNext()) {
			SimpleNode fnode = (SimpleNode) nodeIterator.next();
			if (fsmin.getRelatedASTNode() != fnode) {
				continue;
			}
			boolean result = false;
			BOFunction bofunc = null;
			String bufferName = "";
			String funcName = null;
			if (fsmin.getStateData() instanceof BOFunction) {
				bofunc = (BOFunction)fsmin.getStateData();
				funcName = bofunc.name;
				if (bofunc.type == BOType.FORMATIN) {
					
					SimpleNode fsArg = StateMachineUtils.getArgument(fnode, bofunc.srcIndex);
					ASTConstant  strNode=null;
					if(fsArg.getSingleChildofType(ASTConstant.class)!=null)
						{
							strNode=(ASTConstant) fsArg.getSingleChildofType(ASTConstant.class);
							if(strNode.getImage().contains("%"))
							{
								List<Long> lenList = BOHelper.getFSVarsLenList(strNode.getImage(),false);
								
								int index = bofunc.bufIndex;
								long len = lenList.size();
								
								for (int i = 0; i < len; ++i) {
									SimpleNode argNode = StateMachineUtils.getArgument(fnode, index+i);
									if(argNode==null)
									{
										continue;
									}
									SimpleNode  pNode=(SimpleNode) argNode.getSingleChildofType(ASTPrimaryExpression.class);
									NameDeclaration  bufRe=null;
									long bufsize=0;
									if(pNode!=null){
										bufRe=pNode.getVariableNameDeclaration();
										bufsize= BOStateMachine.getbuflength(pNode);
									}
									//Ŀǰֻ�����ַ���
									if (bufRe instanceof VariableNameDeclaration) {
										VariableNameDeclaration varDecl = ((VariableNameDeclaration)bufRe);
										String typeName = varDecl.getType().toString();
										if ((typeName.contains("[")||typeName.contains("*"))&&(typeName.endsWith("char") || typeName.endsWith("wchar_t"))) { //maybe buffer overflow.
											boolean needCreateFSM = false;
											
											long l = lenList.get(i);
											if (l == -1) {
												needCreateFSM = true;
											} else {
												if (l>bufsize&&bufsize!=0) {
													needCreateFSM = true;
												}
											}
											
											if (needCreateFSM) {
												bufferName = varDecl.getImage();
												fsmin.setResultString(bufferName);
												if (varDecl.getNode() != null) {
														bufferName="�ڵ� "+varDecl.getNode().getBeginLine()+" �ж���ı���\""+bufferName+"\"";
												} else {
														bufferName="\""+bufferName+"\"";
												}
												String desp = "ʹ���˸�ʽ�����뺯��"+bofunc.name+"���ܵ���"+bufferName+"���������";
												fsmin.setDesp(desp);
												
												return true;
											}
										}
									}
								}
							}
						}
					
				}
				if (bofunc.type == BOType.FORBIDDEN) {
					String desp = "ʹ�������뺯��"+bofunc.name+"���ܵ���"+bufferName+"���������";
					fsmin.setDesp(desp);
					
					return true;
				} 
				
				
				if(StateMachineUtils.getArgument(fnode, bofunc.bufIndex)==null||
						((SimpleNode)StateMachineUtils.getArgument(fnode, bofunc.bufIndex)).getSingleChildofType(ASTPrimaryExpression.class)==null) 
					continue;
				SimpleNode bufArg = StateMachineUtils.getArgument(fnode, bofunc.bufIndex);
				SimpleNode  pNode =(SimpleNode) bufArg.getSingleChildofType(ASTPrimaryExpression.class);
				NameDeclaration  bufRe = pNode.getVariableNameDeclaration();
				long bufsize = BOStateMachine.getbuflength(pNode);
				
				if (bufRe instanceof VariableNameDeclaration) {
					VariableNameDeclaration varDecl = (VariableNameDeclaration)bufRe;
					bufferName = varDecl.getImage();
					fsmin.setResultString(bufferName);
					if (varDecl.getNode() != null) {
							bufferName="�ڵ� "+varDecl.getNode().getBeginLine()+" �ж���ı���\""+bufferName+"\"";
					} else {
							bufferName="\""+bufferName+"\"";
					}
				}	 
				
				long srcsize = 0, dstsize = bufsize;
				
				if (bofunc.type == BOType.FORMATSTRING) {
					SimpleNode fsArg = StateMachineUtils.getArgument(fnode, bofunc.srcIndex);
					ASTConstant  fsNode=(ASTConstant) fsArg.getSingleChildofType(ASTConstant.class);
					if(fsNode==null)
						continue;
					if (bofunc.limitLen != -1) {
						//�õ�Ŀ�껺���������ͣ��ж��Ƿ�Ϊ���ֽ��ַ�
						boolean isUnicode = false;
						boolean isStringRelated = false;
						
						SimpleNode idNode = (SimpleNode) bufArg.getSingleChildofType(ASTPrimaryExpression.class);
						VariableNameDeclaration varDecl = idNode.getVariableNameDeclaration();
						if (varDecl == null) {
							continue;
						}
						
						CType varType = varDecl.getType();
						if (varType.toString().endsWith("wchar_t")) {
							isUnicode = true;
							isStringRelated = true;
						} else if (varType.toString().endsWith("char")) {
							isStringRelated = true;
						}
						
						SimpleNode limitArg = (SimpleNode) StateMachineUtils.getArgument(fnode, bofunc.limitLen);
						long tmpsize=0;
						if(limitArg.jjtGetNumChildren()==1&&limitArg.jjtGetChild(0) instanceof ASTAdditiveExpression){
							limitArg=(SimpleNode) limitArg.jjtGetChild(0);
							tmpsize=BOStateMachine.getsrclength(limitArg);
						}
						else {
							limitArg=(SimpleNode) limitArg.getFirstChildInstanceofType(ASTPrimaryExpression.class);
							if(limitArg.jjtGetNumChildren()==0)
								tmpsize=BOStateMachine.getsrclength(limitArg);
							else if(limitArg.jjtGetNumChildren()==1&&limitArg.jjtGetChild(0) instanceof ASTConstant)
								tmpsize=BOStateMachine.getsrclength((SimpleNode) limitArg.getSingleChildofType(ASTConstant.class));
							else if(limitArg.jjtGetNumChildren()==1&&limitArg.jjtGetChild(0) instanceof ASTExpression){
								tmpsize=BOStateMachine.getsrclength((SimpleNode) limitArg);
							}
						}
						long limitLen = tmpsize;//getsrclength(limitArg);
						if( (bofunc.name).equals("snprintf") )
						{
							if (bufsize<limitLen&&bufsize!=0) {
								result = true;
							}
						}
						else
						{
							if (isStringRelated) {
								++limitLen;
							} 
							if (isUnicode) {
								limitLen *= 2;
							}

							srcsize =  limitLen;
							if (bufsize<limitLen&&bufsize!=0) {
								result = true;
							}
						}
					} else {
						long fssize = getFSDomain(fsNode.getImage(), fnode, bofunc);
						if (bufsize!=0&&bufsize<fssize) {
							srcsize = fssize;
							result = true;
						}
						List<Long> lenList = BOHelper.getFSVarsLenList(fsNode.getImage(),true);
						for(int i = 0; i < lenList.size(); ++i)
						{
							SimpleNode argNode = StateMachineUtils.getArgument(fnode, bofunc.srcIndex+1+i);
							SimpleNode  priNode=(SimpleNode) argNode.getFirstChildInstanceofType(ASTPrimaryExpression.class);
							NameDeclaration  bufvar=null;
							if(priNode!=null)
								bufvar=priNode.getVariableNameDeclaration();
							if (bufvar!=null&&bufvar instanceof VariableNameDeclaration) {
								VariableNameDeclaration varDecl = ((VariableNameDeclaration)bufvar);
								if (varDecl.isParam() && (!BOHelper.unknownTypeAsMax)) //varDecl.getScope() instanceof SourceFileScope||
									//result = true;	
									continue out;
							}
							else if(((ASTPrimaryExpression)priNode).isMethod() &&(!BOHelper.unknownTypeAsMax)){
								//result = true;
								continue out;
							}
							else if(bufvar==null&&!((ASTPrimaryExpression)priNode).isMethod()&&
									argNode.getSingleChildofType(ASTConstant.class)==null){
								//result = true;
								continue out;
							}
						}
					}
				} 
				
				if (result) {
					String desp;
					if(srcsize!=-1 && (!BOHelper.unknownTypeAsMax))
						
						desp= "ʹ���˸�ʽ������"+bofunc.name+"���ܵ���"+bufferName+"�����������Դ�������ռ�"+srcsize+"��Ŀ�Ļ������ռ�"+dstsize;
					else
						desp= "ʹ���˸�ʽ������"+bofunc.name+"���ܵ���"+bufferName+"�����������Դ�������ռ� δ֪��Ŀ�Ļ������ռ�"+dstsize;
					fsmin.setDesp(desp);
					
					return true;
				}
				
			} else {
			//���ڿ⺯����������Ĵ���
				MethodNameDeclaration methodDecl = (MethodNameDeclaration)fsmin.getStateData();	
				
				if (methodDecl == null)
					continue;
				MethodBOPreCondition boPreCon = (MethodBOPreCondition)methodDecl.getMethodSummary().findMethodFeature(MethodBOPreCondition.class);

				if (boPreCon == null)
					continue;
				long  srcsize = 0,bufsize = 0;
				 
				
				funcName = methodDecl.getImage();
				
				if (methodDecl.isLib()) {//��ǰ�����ǿ⺯�������ݿ⺯������ժҪ�����ʽ��д���
					SimpleNode bufArg = StateMachineUtils.getArgument(fnode, boPreCon.getBufIndex());
					
					if(bufArg == null)
						continue;

					//�õ�Ŀ�Ļ�����������
					SimpleNode pNode =  null;
					VariableNameDeclaration varDecl = null;
					ASTPostfixExpression postNode = (ASTPostfixExpression)bufArg.getSingleChildofType(ASTPostfixExpression.class);
					if(postNode!= null && ( postNode.getOperators().contains(".") || postNode.getOperators().contains("->") ))
					{
						NameDeclaration decl = null;
						decl = postNode.getVariableDecl();
						if (decl instanceof VariableNameDeclaration) {
							varDecl = (VariableNameDeclaration) decl;
						}
						if (varDecl == null) {
							continue;
						}
						bufsize=BOStateMachine.getbuflength(postNode);
					}
					else
					{
						pNode = (SimpleNode) bufArg.getSingleChildofType(ASTPrimaryExpression.class);					
						if(pNode!=null)
							varDecl=pNode.getVariableNameDeclaration();
						if (varDecl == null) {
							continue;
						}
						bufsize=BOStateMachine.getbuflength(pNode);
					}
					if(bufsize==0)
						continue;

					boolean isStringRelated = false; //��ʾĿ�Ļ������������Ƿ�Ϊ�ַ�����ص�,�������ֽ��ַ�
					boolean isUnicode = false;
					
					CType varType = varDecl.getType();
					if (varType.toString().endsWith("char")) {
						isStringRelated = true;
					} else if (varType.toString().endsWith("wchar_t")) {
						isStringRelated = true;
						isUnicode = true;
					}

					
					SimpleNode srcArg = null;					
					if (boPreCon.getLimitLen() == -1) {//û�����Ƹ��Ƴ��ȵĿ⺯������strcpy
						srcArg = StateMachineUtils.getArgument(fnode, boPreCon.getSrcIndex());
						
						/**
						 * 1. Check whether the current argument is a constant string.
						 */
						ASTPostfixExpression srcpostNode = (ASTPostfixExpression)srcArg.getSingleChildofType(ASTPostfixExpression.class);
						if(srcpostNode!= null && ( srcpostNode.getOperators().contains(".") || srcpostNode.getOperators().contains("->") ))
						{
							srcsize=BOStateMachine.getsrclength(srcpostNode);//�����Դ�������Ƴ��ȵ�����ֵ
						}
						else
						{
							SimpleNode priNode = (SimpleNode) srcArg.getSingleChildofType(ASTPrimaryExpression.class);
							if(priNode==null)
								continue;
							if (priNode.jjtGetNumChildren() != 0&&priNode.getSingleChildofType(ASTConstant.class)!=null) { //it's a constant string.
								String string =((ASTConstant) priNode.getSingleChildofType(ASTConstant.class)).getImage();
								if (string.startsWith("\"")) {
									string = string.substring(1);
								} else if (string.startsWith("L\"")) { //it's wide characters.
									string = string.substring(2);
								}

								if (string.endsWith("\"")) {
									string = string.substring(0, string.length()-1);
								}

								long length = BOHelper.getConstStrLength(string);

								if (isUnicode) { //it's wide characters.
									length *= 2;
								}

								srcsize = length;
							} else {
								srcsize=BOStateMachine.getsrclength(priNode);//�����Դ�������Ƴ��ȵ�����ֵ
							}
						}
						
						
					} else {
						srcArg = StateMachineUtils.getArgument(fnode, boPreCon.getLimitLen());
						long tmpsize=0;
						if(srcArg.jjtGetNumChildren()==1&&srcArg.jjtGetChild(0) instanceof ASTAdditiveExpression){
							tmpsize=BOStateMachine.getsrclength((SimpleNode) srcArg.jjtGetChild(0));
						}
						else{
							SimpleNode priNode = (SimpleNode) srcArg.getFirstChildInstanceofType(ASTPrimaryExpression.class);
							if(priNode.jjtGetNumChildren()==0)
								tmpsize=BOStateMachine.getsrclength(priNode);
							else if(priNode.jjtGetNumChildren()==1&&priNode.jjtGetChild(0) instanceof ASTConstant)
								tmpsize=BOStateMachine.getsrclength((SimpleNode) priNode.getSingleChildofType(ASTConstant.class));
							else if(priNode.jjtGetNumChildren()==1&&priNode.jjtGetChild(0) instanceof ASTExpression){
								tmpsize=BOStateMachine.getsrclength((SimpleNode) priNode);
							}
						}
						if(tmpsize==0)
							continue;
						
						if (!boPreCon.isNeedNull()) { //not need to reserve one byte for '\0', such as memset.
							if (tmpsize != 0) {
								if (!isUnicode) {
									srcsize = tmpsize;
								} else { //wide characters.
									srcsize = tmpsize*2;
								}
							}
						} else {
							if (tmpsize != 0) {
								
								if (isStringRelated) {
									++tmpsize;
								}
								
								if (isUnicode) { //it's wide characters.
									tmpsize *= 2;
								}
								
								srcsize = tmpsize;
							}
						}
					}
					//strncat��strcat������Ŀ�껺������Ч��������Դ������һ�����ȵĴ������´�size����Ŀ�껺�����������
					if(methodDecl.getImage().startsWith("strcat")||methodDecl.getImage().startsWith("strncat")){
						if(bufsize!=0){
							if(pNode != null)
								bufsize-=BOStateMachine.getoldsize(pNode);
							else
								bufsize-=BOStateMachine.getoldsize(postNode);
						}
					}
					
					if (bufsize<srcsize&&bufsize!=0) {//�Ƚ�����ֵ���ж��Ƿ���ܴ���BO
						result = true;
					}
				} 
				
				if (result) {
					String desp;
					if(BOHelper.unknownTypeAsMax)
						desp = "ʹ���˻�������������"+funcName+"���ܵ���"+bufferName+"�����������Դ�������ռ�δ֪"+ ",Ŀ�Ļ������ռ�"+bufsize;
					else
					    desp= "ʹ���˻�������������"+funcName+"���ܵ���"+bufferName+"�����������Դ�������ռ�"+srcsize+",Ŀ�Ļ������ռ�"+bufsize;
					fsmin.setDesp(desp);
					
					return true;
				}
			}
		}
		
		return false;
	}
	

	public static long getFSDomain(String formatString, SimpleNode fnode, BOFunction bofunc) {
		if (formatString == null) {
			return 0;
		}
		
		boolean isUnicode = false;
		if (formatString.startsWith("\"")) {
			formatString = formatString.substring(1);
		} else if (formatString.startsWith("L\"")) {
			formatString = formatString.substring(2);
			isUnicode = true;
		}
		
		if (formatString.endsWith("\"")) {
			formatString = formatString.substring(0, formatString.length()-1);
		}
		
		BOHelper helper = new BOHelper(formatString, fnode, bofunc);
		long length =  helper.getForStrLength();
		if (isUnicode) {
			length *= 2;
		}
	
		return length;
	}
	
/**
 * ���㻺����size�������	
 */
	public static long getsrclength(SimpleNode snode)
	{
		long length=0;
		if(snode instanceof ASTConstant){
			ASTConstant cnode=(ASTConstant)snode;
			if(cnode.getType().toString().equals("int") ||cnode.getType().toString().equals("long")||cnode.getType().toString().equals("unsigned int"))//һ����limiten�ڵ�
			{
				if(OOBStateMachine.isNum(cnode.getImage()))
					length=java.lang.Integer.parseInt(cnode.getImage());
			}
			else if(cnode.getType().toString().equals("*char"))
			{
				length=cnode.getImage().length();//������Ҳ��
			}
		}
		else if(snode instanceof ASTPrimaryExpression || snode instanceof ASTPostfixExpression){
			VariableNameDeclaration curvar = null;
			SimpleNode pnode = snode;
			if(snode instanceof ASTPrimaryExpression)
			{
				curvar=((ASTPrimaryExpression)pnode).getVariableNameDeclaration();
			}
			else
			{
				NameDeclaration decl = null;
				decl = ((ASTPostfixExpression)pnode).getVariableDecl();
				if (decl instanceof VariableNameDeclaration) {
					curvar = (VariableNameDeclaration) decl;
				}
			}
			if(curvar!=null&&curvar.getType()!=null&&(curvar.getType().toString().equals("int")
				||curvar.getType().toString().equals("long")||curvar.getType().toString().equals("unsigned int")))
			{
				VexNode vex=snode.getCurrentVexNode();
				Domain d=vex.getVarDomainSet().getDomain(curvar);
				if(d!=null&&((IntegerDomain)d).getMax()!=Long.MAX_VALUE && !d.isUnknown())//((IntegerDomain)mydomain).getMin()!=Long.MIN_VALUE
					length= ((IntegerDomain)d).getMax();
				else length=0;
			}
			else if(curvar!=null&&curvar.getType()!=null&&curvar.getType().isPointType())
			{
				ExpressionValueVisitor expvst = new ExpressionValueVisitor();
				ExpressionVistorData visitdata = new ExpressionVistorData();
				visitdata.currentvex = pnode.getCurrentVexNode();
				visitdata.currentvex.setfsmCompute(true);
				expvst.visit(pnode, visitdata);
				visitdata.currentvex.setfsmCompute(false);
				Expression value1 = visitdata.currentvex.getValue(curvar);
				PointerDomain mydomain=null;
				
					try
					{
						if(value1!=null&&value1.getDomain(visitdata.currentvex.getSymDomainset())!=null&&
								value1.getDomain(visitdata.currentvex.getSymDomainset()).getDomaintype()==DomainType.POINTER)
						mydomain = (PointerDomain) value1.getDomain(visitdata.currentvex.getSymDomainset()).clone();
					} catch (CloneNotSupportedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				//PointerValue v = mydomain.getValue();
				//String strValue = v.toString();
				if(mydomain!=null && mydomain.getValue().name().equals("UNKOWN"))
				{
					length = 2147483647; //��ֵΪ��������
					return length;
				}
				
				if(mydomain!=null&&mydomain.allocRange!=null){
					
					long Len=0;
					//temp���ڴ��ÿ���ռ�ĳ���
					long temp;
					//�ⲿ�ֻ�������,allocRange,offsetRange --nmh					
					for (IntegerInterval range : mydomain.allocRange.intervals) {
						temp=range.getMax();
					    if(temp>Len)
						  Len=temp;
				    }
				    if(Len == 0)
				    	for (IntegerInterval range : mydomain.offsetRange.intervals) {
				    		temp=range.getMax() - range.getMin() + 1;
				    		if(temp>Len)
				    			Len=temp;
				    	}
					length= Len;
				}
				if(length==0){
					if(curvar!=null&&curvar.getType()!=null&&curvar.getType() instanceof CType_Array)
					{
						CType curtype=curvar.getType();
						CType type=curtype.getSimpleType();
						CType_Array atype=(CType_Array)type;
						length=atype.getSize();
						if(length==1)//CType_Array��getSize()������δ֪���鳤�ȵ��������1//Ŀǰ������char����
							length=0;
					}
				}
			}  
			else if(curvar==null&&snode.getImage().equals("sizeof")){
				ASTExpression expression=(ASTExpression)snode.getFirstChildInstanceofType(ASTExpression.class);
				VexNode vex=snode.getCurrentVexNode();
				IntegerDomain idomain=Domain.castToIntegerDomain(expression.getDomain(vex));
				length=idomain.getMax();
			}
			
		}
		else if(snode instanceof ASTAdditiveExpression){
			ASTAdditiveExpression expression=(ASTAdditiveExpression) snode;
			VexNode vex=snode.getCurrentVexNode();
			Domain ret = null;
			ExpressionVistorData expdata = new ExpressionVistorData();
			expdata.currentvex = vex;
			expdata.sideeffect = false;
			expression.jjtAccept(new ExpressionValueVisitor(), expdata);
			ret = expdata.value.getDomain(vex.getSymDomainset());
			IntegerDomain idomain=Domain.castToIntegerDomain(ret);
			if(idomain!=null)
				length=idomain.getMax();
		}
		return length;
	}
	
	public static long getbuflength(SimpleNode snode){
		long length=0;
		if(snode instanceof ASTPrimaryExpression || snode instanceof ASTPostfixExpression){
			VariableNameDeclaration curvar = null;
			SimpleNode pnode = snode;
			if(snode instanceof ASTPrimaryExpression)
			{
				curvar=((ASTPrimaryExpression)pnode).getVariableNameDeclaration();
			}
			else
			{
				NameDeclaration decl = null;
				decl = ((ASTPostfixExpression)pnode).getVariableDecl();
				if (decl instanceof VariableNameDeclaration) {
					curvar = (VariableNameDeclaration) decl;
				}
			}
			if(curvar!=null&&curvar.getType()!=null&&(curvar.getType().toString()=="int"
				||curvar.getType().toString()=="long"||curvar.getType().toString()=="unsigned int"))
			{
				VexNode vex=snode.getCurrentVexNode();
				Domain d=vex.getVarDomainSet().getDomain(curvar);
				if(d!=null && !d.isUnknown() &&((IntegerDomain)d).getMin()!=Long.MIN_VALUE)//((IntegerDomain)mydomain).getMin()!=Long.MIN_VALUE
					length= ((IntegerDomain)d).getMin();
				else length=0;
			}
			else if(curvar!=null&&curvar.getType()!=null&&curvar.getType().isPointType())
				{
					ExpressionValueVisitor expvst = new ExpressionValueVisitor();
					ExpressionVistorData visitdata = new ExpressionVistorData();
					visitdata.currentvex = pnode.getCurrentVexNode();
					visitdata.currentvex.setfsmCompute(true);
					expvst.visit(pnode, visitdata);
					visitdata.currentvex.setfsmCompute(false);
					Expression value1 = visitdata.currentvex.getValue(curvar);
					PointerDomain mydomain=null;
					
						try
						{
							if(value1!=null && value1.getDomain(visitdata.currentvex.getSymDomainset())!=null &&
									!value1.getDomain(visitdata.currentvex.getSymDomainset()).isUnknown() &&
									value1.getDomain(visitdata.currentvex.getSymDomainset()).getDomaintype()==DomainType.POINTER)
							mydomain = (PointerDomain) value1.getDomain(visitdata.currentvex.getSymDomainset()).clone();
						} catch (CloneNotSupportedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					
					if(mydomain!=null&&mydomain.offsetRange!=null){
						long Len=0;
						//temp���ڴ��ÿ���ռ�ĳ���
						long temp;
						for (IntegerInterval range : mydomain.offsetRange.intervals) {
							temp=range.getMax()-range.getMin()+1;
							if(temp>Len&&!(range.getMax()==0&&range.getMin()==0))
								Len=temp;
						}
						length=  Len;
					}
				} 
				if(length==0){
					if(curvar!=null&&curvar.getType()!=null&&curvar.getType() instanceof CType_Array)
					{
						CType curtype=curvar.getType();
						CType type=curtype.getSimpleType();
						CType_Array atype=(CType_Array)type;
						length=atype.getSize();
						if(length==1)//CType_Array��getSize()������δ֪���鳤�ȵ��������1//Ŀǰ������char����
							length=0;
					}
					
				}
			}
		
		return length;
	}

	/**
	 * ���strcat��strncat���������㵱ǰĿ�껺����ָ��ռ�ԭ����Чֵ����
	 * @param pNode
	 * @return
	 */
	public static long getoldsize(SimpleNode pNode){
		VariableNameDeclaration curvar=null;
		if(pNode instanceof ASTPrimaryExpression)
		{
			curvar=((ASTPrimaryExpression)pNode).getVariableNameDeclaration();
		}
		else
		{
			NameDeclaration decl = null;
			decl = ((ASTPostfixExpression)pNode).getVariableDecl();
			if (decl instanceof VariableNameDeclaration) {
				curvar = (VariableNameDeclaration) decl;
			}
		}
		long oldsize=0;
		VexNode n=pNode.getCurrentVexNode();
		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e.hasMoreElements();) {
			list.add(e.nextElement());
		}
		Collections.sort(list);

		for (Edge edge : list) {
			VexNode pre = edge.getTailNode();
			
			// �жϷ�֧�Ƿ�ì��
			if (edge.getContradict()) {
				continue;
			}

//			// ���ǰ����û�з��ʹ�������
//			if (!pre.getVisited()) {
//				continue;
//			}
			// ��֧Ϊ������
			if (edge.getName().startsWith("F")) {
				 continue;
			}
			if(curvar.getType().isPointType())
			{
				
				ExpressionValueVisitor expvst = new ExpressionValueVisitor();
				ExpressionVistorData visitdata = new ExpressionVistorData();
				//pNode.getCurrentVexNode().getInedges().elements().nextElement().getTailNode();
				visitdata.currentvex = pre;
				visitdata.currentvex.setfsmCompute(true);
				expvst.visit(pNode, visitdata);
				visitdata.currentvex.setfsmCompute(false);
				Expression value1 = visitdata.currentvex.getValue(curvar);
				PointerDomain mydomain=null;
				try
				{
					if(value1!=null && value1.getDomain(visitdata.currentvex.getSymDomainset())!=null &&
							!value1.getDomain(visitdata.currentvex.getSymDomainset()).isUnknown() &&
							value1.getDomain(visitdata.currentvex.getSymDomainset()).getDomaintype()==DomainType.POINTER)
					mydomain = (PointerDomain) value1.getDomain(visitdata.currentvex.getSymDomainset()).clone();
				} catch (CloneNotSupportedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(mydomain!=null&&mydomain.allocRange!=null){
					long size=0;
					//temp���ڴ��ÿ���ռ�ĳ���
					long temp;
					for (IntegerInterval range : mydomain.allocRange.intervals) {
						temp=range.getMax();
						if(temp>size)
							size=temp;
					}
					if(size>1&&size-1>oldsize)
					oldsize=  size-1;
				}
			}
		}
		
		return oldsize;
	}
	
	public static VariableNameDeclaration findLocalVariableDecl(Variable variable, SimpleNode node) {
		Scope scope = node.getScope().getEnclosingSourceFileScope();
		if (scope != null) {
			Map<VariableNameDeclaration, ArrayList<NameOccurrence>> vars = scope.getVariableDeclarations();
			for (VariableNameDeclaration var : vars.keySet()) {
				if (var.getVariable() == variable) {
					return var;
				}
			}
		}
		return null;
	}
}

