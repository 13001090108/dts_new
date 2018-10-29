package softtest.summary.gcc.fault;



import java.util.List;
import java.util.Set;


import softtest.ast.c.ASTAdditiveExpression;
import softtest.ast.c.ASTCompoundStatement;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.interpro.c.ScopeType;
import softtest.rules.c.StateMachineUtils;
import softtest.rules.gcc.fault.BOStateMachine;
import softtest.rules.gcc.fault.BO.BOFunction;
import softtest.rules.gcc.fault.BO.BOHelper;
import softtest.rules.gcc.fault.BO.BOType;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;

import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_AbstPointer;



/**
 * ��¼BO����ժҪ�Ķ�����
 * @author chh
 */
public class MethodBOPreConditionVisitor  extends CParserVisitorAdapter implements MethodFeatureVisitor {
	private static MethodBOPreConditionVisitor instance;
	
	private MethodBOPreConditionVisitor(){
	}
	
	MethodNameDeclaration methodDecl = null;
	
	public static MethodBOPreConditionVisitor getInstance(){
		if(instance == null){
			instance = new MethodBOPreConditionVisitor();
		}
		return instance;
	}
	//���ʺ�����㣬���㺯��ժҪ
	public void visit(VexNode vexNode) {
		SimpleNode node = vexNode.getTreenode();
		if (node == null) {
			return;
		}
		MethodBOPreCondition feature = new MethodBOPreCondition();
		
		methodDecl = InterContext.getMethodDecl(vexNode);
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

	/**
	 * ȷ����ǰ�����Ƿ����BOǰ��Լ��
	 * @param bufDomain
	 * @param srcDecl
	 * @param feature
	 */
	private void confirmBOVar(long size, VariableNameDeclaration bufDecl, StringBuffer despmet,  MethodBOPreCondition feature,boolean flag) {
		Variable v = Variable.getVariable(bufDecl);
		StringBuffer despbuf = new StringBuffer("Ŀ�껺������"+bufDecl.getImage()+"��");
		if (v == null){
			//�Ȳ��ǲ���Ҳ�����ⲿ������ֻ���Ǻ����ڲ�����ľֲ�����//��ʱֻ���һά����
				SimpleNode srcNode=bufDecl.getNode();
				ASTPrimaryExpression pNode=(ASTPrimaryExpression) srcNode.getSingleChildofType(ASTPrimaryExpression.class);
				if(pNode==null||pNode.jjtGetNumChildren()!=0)
					return ;
				else{
					VariableNameDeclaration src=pNode.getVariableDecl();
					v=Variable.getVariable(src);
					if(v==null)
						return ;
				}
		}
			
		
		if (feature.contains(bufDecl)) {//����Ѿ�  ���ڣ��Ƚ����䣬������ǿԼ������
			long oldsize = feature.getsize(v);
			
			if ((size>oldsize&&flag==false)||(size<oldsize&&flag==true)) {
				feature.addVariable(v, size, despbuf.toString(), despmet.toString(), flag);
			}
			return;
		}
		
		//��ǰ����Ϊ���������ⲿ��������¼
		if (v.isParam() ||  bufDecl.getScope() instanceof SourceFileScope) {
			feature.addVariable(v, size, despbuf.toString(), despmet.toString(), flag);
		}
		
	}
	/**
	 * ȷ����ǰ�����Ƿ����BOǰ��Լ��
	 * @param bufDomain
	 * @param srcDecl
	 * @param feature
	 */
	private void confirmBOVar(long size, VariableNameDeclaration srcDecl, StringBuffer despbuf, StringBuffer despmet, MethodBOPreCondition feature,boolean flag) {
		Variable v = Variable.getVariable(srcDecl);
		if (v == null){
			//�Ȳ��ǲ���Ҳ�����ⲿ������ֻ���Ǻ����ڲ�����ľֲ�����//��ʱֻ���һά����
				SimpleNode srcNode=srcDecl.getNode();
				ASTPrimaryExpression pNode=(ASTPrimaryExpression) srcNode.getSingleChildofType(ASTPrimaryExpression.class);
				if(pNode==null||pNode.jjtGetNumChildren()!=0)
					return ;
				else{
					VariableNameDeclaration src=pNode.getVariableDecl();
					v=Variable.getVariable(src);
					if(v==null)
						return ;
				}
		}
			
		
		if (feature.contains(srcDecl)) {//����Ѿ�  ���ڣ��Ƚ����䣬������ǿԼ������
			long oldsize = feature.getsize(v);
			
			if ( size<oldsize&&flag==true) {
				feature.addVariable(v, size, despbuf.append("��").toString(), despmet.toString(), flag);
			}
			return;
		}
		
		//��ǰ����Ϊ���������ⲿ��������¼
		if (v.isParam() ||  srcDecl.getScope() instanceof SourceFileScope) {
			feature.addVariable(v, size,despbuf.append("��").toString(), despmet.toString(), flag);
		}
		
	}
	
	@Override
	/**
	 * ���ܣ���д�����﷨���ķ����߷��������㺯��ժҪ
	 */
	public Object visit(ASTCompoundStatement node,Object data) {
	
		MethodBOPreCondition feature = (MethodBOPreCondition)data;
		//ƥ������еĺ�����㣬���Ƿ����bopreconditonժҪ��Ϣ
		String xPath=".//PostfixExpression/PrimaryExpression[@Method='true'] ";
		
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		
		//����һ��������������߶���
		//chh  ExpressionValueVisitor expVisitor = new ExpressionValueVisitor();
		
		//�������к���
		for(SimpleNode snode: evaluationResults) {
			//�õ�����������Ϣ
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(snode);
			
			if(methodDecl == null) {
				continue;
			}
			
			StringBuffer despmet = new StringBuffer("�����ļ���"+snode.getBeginLine()+"�����˺���"+methodDecl.getImage()+"��");
			//����BOFunction ���͵ĺ���Ҳ��Ҫ����Ƿ�Ӧ�����ɺ���ժҪ��FORBIDDEN�Ͳ���Ҫ��
			if(BOFunction.isBOfunc(methodDecl)!=null){
				BOFunction bofunc=BOFunction.isBOfunc(methodDecl);
				if(bofunc.type == BOType.FORMATIN){
					SimpleNode fsArg = StateMachineUtils.getArgument(snode, bofunc.srcIndex);
					ASTConstant  strNode=null;
					//ĿǰĬ�ϸ�ʽ�ַ������ַ�������
					if(fsArg.getSingleChildofType(ASTConstant.class)!=null)
						{
							strNode=(ASTConstant) fsArg.getSingleChildofType(ASTConstant.class);
							if(strNode.getImage().contains("%"))
							{
								List<Long> lenList = BOHelper.getFSVarsLenList(strNode.getImage(),false);
								
								int index = bofunc.bufIndex;
								int len = lenList.size();
								
								for (int i = 0; i < len; ++i) {
									SimpleNode argNode = StateMachineUtils.getArgument(snode, index+i);
									if(argNode==null)
									{
										continue;
									}
									SimpleNode  pNode=(SimpleNode) argNode.getSingleChildofType(ASTPrimaryExpression.class);
									VariableNameDeclaration  bufDecl=null;
									long bufsize=0;
									if(pNode!=null){
										bufDecl=pNode.getVariableNameDeclaration();
										bufsize= BOStateMachine.getbuflength(pNode);
									}
					
									long l = lenList.get(i);
									if(l==-1||bufDecl==null||(bufsize!=0&&!(bufDecl.getScope() instanceof SourceFileScope))) 
										continue;
									else 
										this.confirmBOVar( l, bufDecl,despmet ,feature,false);
									//���ֻ᲻�����//Ŀǰֻ�����ַ���
								}
							}
						}
				}
				else if(bofunc.type == BOType.FORMATSTRING){
					
						SimpleNode bufArg = StateMachineUtils.getArgument(snode, bofunc.bufIndex);
						SimpleNode  pNode=(SimpleNode) bufArg.getSingleChildofType(ASTPrimaryExpression.class);
						VariableNameDeclaration  bufDecl=null;
						long bufsize=0;
						if(pNode!=null){
							bufDecl=pNode.getVariableNameDeclaration();
							bufsize= BOStateMachine.getbuflength(pNode);
						}
						SimpleNode srcArg = StateMachineUtils.getArgument(snode, bofunc.srcIndex);
						ASTConstant  srcNode=(ASTConstant) srcArg.getSingleChildofType(ASTConstant.class);
						if(srcNode==null)
							continue;
						if (bufDecl == null) {
							continue;
						}
						
						if (bofunc.limitLen != -1) {
							//�õ�Ŀ�껺���������ͣ��ж��Ƿ�Ϊ���ֽ��ַ�
							boolean isUnicode = false;
							boolean isStringRelated = false;
							
							
							CType varType = bufDecl.getType();
							if (varType.toString().endsWith("wchar_t")) {
								isUnicode = true;
								isStringRelated = true;
							} else if (varType.toString().endsWith("char")) {
								isStringRelated = true;
							}
							
							SimpleNode limitArg = (SimpleNode) StateMachineUtils.getArgument(snode, bofunc.limitLen);
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
							VariableNameDeclaration  limitvar=limitArg.getVariableNameDeclaration();
							if(limitvar!=null&&(bufsize!=0)
									&&(limitLen==0||limitvar.getScope() instanceof SourceFileScope)){
								if (isUnicode) {
									bufsize/= 2;
								}
								if (isStringRelated) {
									--bufsize;
								} 
								this.confirmBOVar(bufsize, limitvar,new StringBuffer("Ŀ�껺������"+bufDecl.getImage()), despmet, feature,true);
							}
							else if((bufsize==0||bufDecl.getScope() instanceof SourceFileScope)&&limitLen!=0&&bufDecl!=null){
								if (isStringRelated) {
									++limitLen;
								} 
								if (isUnicode) {
									limitLen *= 2;
								}
								this.confirmBOVar(limitLen, bufDecl, despmet, feature,false);
							}
						} else {
							long fssize = BOStateMachine.getFSDomain(srcNode.getImage(), snode, bofunc);
						
							if (bufsize!=0&&bufsize<fssize&&!(bufDecl.getScope() instanceof SourceFileScope)){
								continue;
							}
								
							boolean result=false;
							List<Long> lenList = BOHelper.getFSVarsLenList(srcNode.getImage(),true);
							for(int i = 0; i < lenList.size(); ++i)
							{
								SimpleNode argNode = StateMachineUtils.getArgument(snode, bofunc.srcIndex+1+i);
								if(argNode==null)
								{
									continue;
								}
								SimpleNode  srcpNode=(SimpleNode) argNode.getFirstChildInstanceofType(ASTPrimaryExpression.class);
								NameDeclaration  bufRe=null;
								if(srcNode!=null)
									bufRe=srcpNode.getVariableNameDeclaration();
								if (bufRe!=null&&bufRe instanceof VariableNameDeclaration) {
									VariableNameDeclaration varDecl = ((VariableNameDeclaration)bufRe);
									if (varDecl.getScope() instanceof SourceFileScope||varDecl.isParam()) 
										result = true;	
								}
								else if(bufRe!=null&&bufRe instanceof MethodNameDeclaration){
									result = true;
								}
								else if(bufRe==null&&argNode.getFirstChildInstanceofType(ASTConstant.class)==null){
									result = true;
								}
							}
							if(bufDecl!=null&&(bufsize==0||bufDecl.getScope() instanceof SourceFileScope)&&result==false&&fssize!=0)
								this.confirmBOVar(fssize, bufDecl, despmet, feature,false);
						}	
				}
				else 
					continue;
			}
			MethodSummary  methodSum=methodDecl.getMethodSummary();

			if(methodSum == null) {
				continue;
			}
			//�ж��Ƿ���BO�ĺ���ժҪ
			MethodBOPreCondition boPrecon = (MethodBOPreCondition) methodSum.findMethodFeature(MethodBOPreCondition.class);
			if(boPrecon == null) {
				continue;
			}
			//�жϴ˺����Ƿ��ǿ⺯�������Զ��庯���ֿ�����()
			if(methodDecl.isLib()) {
			
				if(boPrecon.getSubtype() == BOType.BUFFERCOPY) {
					
					//�õ���ǰ������buf������㣬Ŀ�Ļ�����������
					SimpleNode bufNode = StateMachineUtils.getArgument(snode, boPrecon.getBufIndex());				
					if (bufNode == null)
						continue;
					
					//modified by nmh
					SimpleNode  bufpNode = null;
					VariableNameDeclaration  bufDecl=null;
					long bufsize=0;
					ASTPostfixExpression postNode = (ASTPostfixExpression)bufNode.getSingleChildofType(ASTPostfixExpression.class);
					if(postNode!= null && ( postNode.getOperators().contains(".") || postNode.getOperators().contains("->") ))
					{
						NameDeclaration decl = null;
						decl = postNode.getVariableDecl();
						if (decl instanceof VariableNameDeclaration) {
							bufDecl = (VariableNameDeclaration) decl;
						}
						if (bufDecl == null) {
							continue;
						}
						bufsize=BOStateMachine.getbuflength(postNode);
					}
					else
					{
						bufpNode = (SimpleNode) bufNode.getSingleChildofType(ASTPrimaryExpression.class);					
						if(bufpNode!=null)
							bufDecl=bufpNode.getVariableNameDeclaration();
						if (bufDecl == null) {
							continue;
						}
						bufsize=BOStateMachine.getbuflength(bufpNode);
					}
					
					boolean isStringRelated = false; //��ʾĿ�Ļ������������Ƿ�Ϊ�ַ�����ص�,�������ֽ��ַ�
					boolean isUnicode = false;
					
					CType varType = bufDecl.getType();
					if (varType.toString().endsWith("char")) {
						isStringRelated = true;
					} else if (varType.toString().endsWith("wchar_t")) {
						isStringRelated = true;
						isUnicode = true;
					}
					
					
					int arrayTypeSize=4;  //Ĭ��Ϊint�ͣ�4���ֽ�
					/*bufsizeΪĿ�Ļ��������ֽڳ���*/
					if(varType instanceof CType_AbstPointer)
					{
						CType originalArrayType = ((CType_AbstPointer) varType).getOriginaltype();
						if(originalArrayType!=null)
						{
							arrayTypeSize = originalArrayType.getSize();
						}
					}
					if(arrayTypeSize != 0 )
					   bufsize/= arrayTypeSize;
					//end
					
					SimpleNode srcNode = null;
					SimpleNode srcpNode= null;
					long srcsize=0;
					if(boPrecon.getLimitLen() != -1) {
						long tmpsize=0;
						SimpleNode priNode =null;
						srcNode = StateMachineUtils.getArgument(snode, boPrecon.getLimitLen());
						if(srcNode.jjtGetNumChildren()==1&&srcNode.jjtGetChild(0) instanceof ASTAdditiveExpression){
							srcpNode=(SimpleNode) srcNode.jjtGetChild(0);
							tmpsize=BOStateMachine.getsrclength(srcpNode);
						}
						else{
							if(srcNode != null )
							{
								priNode = (SimpleNode) srcNode.getFirstChildInstanceofType(ASTPrimaryExpression.class);
								if(priNode != null && priNode.jjtGetNumChildren()==0){
									srcpNode=priNode;
									tmpsize=BOStateMachine.getsrclength(srcpNode);
								}
								else if(priNode !=null && priNode.jjtGetNumChildren()==1&&priNode.jjtGetChild(0) instanceof ASTConstant){
									srcpNode=(SimpleNode) priNode.getSingleChildofType(ASTConstant.class);
									tmpsize=BOStateMachine.getsrclength(srcpNode);
								}
								else if(priNode !=null && priNode.jjtGetNumChildren()==1&&priNode.jjtGetChild(0) instanceof ASTExpression){
									srcpNode=(SimpleNode) priNode;
									tmpsize=BOStateMachine.getsrclength(srcpNode);
								}
							}
						}
						
						
						if (!boPrecon.isNeedNull()) { //not need to reserve one byte for '\0', such as memset.
							if (tmpsize != 0) {
								if (!isUnicode) {
									srcsize = tmpsize;
								} else { //wide characters.
									srcsize = tmpsize*2;
								}
							}
							else if(bufsize!=0){
								if (isUnicode) {
									bufsize/=2;
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
							/*else if(bufsize!=0){
								if (isUnicode) {
									bufsize/=2;
								} 
								if (isStringRelated) {
									--bufsize;
								}
							}*/
						}
					} else {
						srcNode = StateMachineUtils.getArgument(snode, boPrecon.getSrcIndex());
						if(srcNode == null)
							continue;
						ASTPostfixExpression srcpostNode = (ASTPostfixExpression)srcNode.getSingleChildofType(ASTPostfixExpression.class);
						if(srcpostNode!= null && ( srcpostNode.getOperators().contains(".") || srcpostNode.getOperators().contains("->") ))
						{
							srcsize=BOStateMachine.getsrclength(srcpostNode);//�����Դ�������Ƴ��ȵ�����ֵ
						}
						else
						{
							srcpNode = (SimpleNode)srcNode.getSingleChildofType(ASTPrimaryExpression.class);
							if(srcpNode==null)
								continue;
							if (srcpNode.getSingleChildofType(ASTConstant.class)!=null) { //it's a constant string.
								String string =((ASTConstant) srcpNode.getSingleChildofType(ASTConstant.class)).getImage();
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
								srcsize=BOStateMachine.getsrclength(srcpNode);//�����Դ�������Ƴ��ȵ�����ֵ
							}
						}
						
					}
					
					VariableNameDeclaration srcDecl=null ;
					if (srcNode != null){//&&!(srcpNode instanceof ASTExpression)
						srcDecl = StateMachineUtils.getVarDeclaration(srcNode);
					}
					
					if(methodDecl.getImage().startsWith("strcat")||methodDecl.getImage().startsWith("strncat")){
						
						if(bufsize!=0 && bufpNode!=null){
							bufsize-=BOStateMachine.getoldsize( (ASTPrimaryExpression) bufpNode);
						}
						if(srcsize!=0 && bufpNode!=null){
							srcsize+=BOStateMachine.getoldsize( (ASTPrimaryExpression) bufpNode);
						}
					}
					
					if(bufsize!=0&&srcDecl!=null&&(srcsize==0||srcDecl.getScope() instanceof SourceFileScope))
						this.confirmBOVar(bufsize, srcDecl,new StringBuffer("Ŀ�껺������"+bufDecl.getImage()), despmet, feature, true);
					else if((bufsize==0||bufDecl.getScope() instanceof SourceFileScope)&&srcsize!=0&&bufDecl!=null)
						this.confirmBOVar(srcsize, bufDecl, despmet, feature, false);
					methodDecl.getMethod().setMtSummmary(methodSum);
				}
			} else {
			//�ú���Ϊ�û��Զ���ĺ���������ǰ�������Ĵ���
				Set<Variable> varList = boPrecon.getBOVariables();
				
				for(Variable var: varList) {
					despmet = despmet.append( boPrecon.getDespmet(var));
					if(boPrecon.getflag(var)==true){
						if (var.isParam() || var.getScopeType() == ScopeType.INTER_SCOPE) {	
							long bufsize = boPrecon.getsize(var);
							VariableNameDeclaration srcDecl = null;
							if(var.getParamIndex() == -1)//�����ȫ�ֱ�������
							{
								NameDeclaration nd=Search.searchInVariableUpward(var.getName(),snode.getScope());
								if(nd instanceof VariableNameDeclaration)
									srcDecl = (VariableNameDeclaration)nd;
							}
							else
							{
								SimpleNode argNode = StateMachineUtils.getArgument(snode, var.getParamIndex());
								srcDecl = StateMachineUtils.getVarDeclaration(argNode);
							}
								
							if (srcDecl == null||(!srcDecl.isParam()&&!(srcDecl.getScope() instanceof SourceFileScope)))
								continue;
							this.confirmBOVar(bufsize, srcDecl, new StringBuffer(boPrecon.getDespbuf(var)),despmet, feature,true);
						}
					}
					else{
						if (var.isParam()) {
							
							long srcsize = boPrecon.getsize(var);
							SimpleNode argNode = StateMachineUtils.getArgument(snode, var.getParamIndex());
							VariableNameDeclaration bufDecl = StateMachineUtils.getVarDeclaration(argNode);
							if (bufDecl == null||(!bufDecl.isParam()&&!(bufDecl.getScope() instanceof SourceFileScope)))
								continue;
							this.confirmBOVar(srcsize, bufDecl, despmet, feature,false);
						}
					}
				}
			}
		}
		return null;
	}

}


