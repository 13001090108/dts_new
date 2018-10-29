package softtest.interpro.c;

import softtest.ast.c.*;
import softtest.config.c.Config;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.fsmanalysis.c.CAnalysis;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.CType_Function;

public class InterMethodVisitor extends CParserVisitorAdapter {
	
	/**
	 * ��ǰ�����������г����﷨�����ڵĺ����ڵ㴦
	 */
	private MethodNode currMethod = null;
	
	/**
	 * ���������﷨������ͨ�����������ڵ㣬������Ӧ�ĺ�����Ϣ�ڵ�
	 */
	@Override
	public Object visit(ASTFunctionDefinition node, Object data) {
		MethodNameDeclaration methodDecl = node.getDecl();
		if(methodDecl.isLib())
			return data;
		Method method = getMethod(methodDecl);
		//zys:2010.8.9	�����#include��ʽ���룬Ԥ��������method�����ڴ˸������ļ���
		//����������ȷ���ļ����ù�ϵ��.h�ļ����������ļ����ù�ϵ��
		if(method.getFileName().matches(InterContext.INCFILE_POSTFIX)){
			method.setFileName(methodDecl.getFileName());
		}
		AnalysisElement element = (AnalysisElement)data;
		MethodNode mtNode = null;
		if(Config.ANALYSIS_I){
			method.setFileName( method.getFileName().replace("\\", "/") );
		}
		
		if(element!=null && method.getFileName().equals(element.getFileName()))
		{
			mtNode=new MethodNode(element, method);
		}else{
			if(method.getFileName().equals("unknown"))
				mtNode=new MethodNode(CAnalysis.getElement(element.getFileName()), method);
			else
				mtNode=new MethodNode(CAnalysis.getElement(method.getFileName()), method);
		}
		mtNode = InterCallGraph.getInstance().addMethodNode(mtNode);
		methodDecl.setMethod(mtNode.getMethod());
		MethodNode old = currMethod;
		currMethod = mtNode;
		super.visit(node, data);
		currMethod = old;
		return data;
	}
	
	/**Ϊ���������ڵ�Ҳ���������Ϣ */
//	public Object visit(ASTFunctionDeclaration node, Object data)
//	{
//		MethodNameDeclaration methodDecl = node.getDecl();
//		if(methodDecl==null)
//			return null;
//		Method method = getMethod(methodDecl);
//		AnalysisElement element = (AnalysisElement)data;
//		MethodNode mtNode = null;
//		if(element!=null && method.getFileName().equals(element.getFileName()))
//		{
//			mtNode=new MethodNode(element, method);
//		}else{
//			mtNode=new MethodNode(new AnalysisElement(method.getFileName()), method);
//		}
//		mtNode = InterCallGraph.getInstance().addMethodNode(mtNode);
//		methodDecl.setMethod(mtNode.getMethod());
//		
//		return null;
//	}
	
	/**
	 * zys:��⵱ǰ�ڵ��Ƿ�Ϊ��������,�������õķ�ʽĿǰ�����֣�
	 * ֱ�ӵ��ã�f();
	 * ����ָ����ã�void (*p)();		p=&f;		p();
	 * ����һ�ֺ���ָ����ã�void (*p)();		p=&f;		��*p��();
	 */
	public Object visit(ASTPrimaryExpression node,Object data) 
	{
		//ֻ�к������ڲ���PrimaryExpr�ڵ���Ҫ����
		if(node.getScope().getEnclosingMethodScope()==null || !node.isMethod())
			return super.visit(node, data);
		//zys:��ʱ��������ָ��	2010.4.8
		MethodNameDeclaration mnd=null;
		if(node.getType() instanceof CType_Function)
		{
			mnd=node.getMethodDecl();
		}
		
		if(mnd!=null && !(mnd.getNode() instanceof ASTNestedFunctionDefinition))
		{
			if(mnd.getMethodNameDeclaratorNode()==null)
			{
				if(mnd.isLib())
					return super.visit(node, data);
				//������û��Զ���ͷ�ļ��еĺ�������
				if(mnd.getFileName().matches(InterContext.INCFILE_POSTFIX))
				{
					Method method=getMethod(mnd);
					MethodNode mtNode = InterCallGraph.getInstance().findMethodNode(method.getFileName(), method);
					if(mtNode.getElement()==null){
						mtNode.updateElement(CAnalysis.getElement(method.getFileName()));
					}
					if(currMethod != null){
						currMethod.addCall(mtNode);
					}
					mnd.setMethod(mtNode.getMethod());
				}else{
					//�����extern�������ⲿ����
					Method method=mnd.getMethod();
					if(method==null)
					{
						method=new Method("unknown",mnd.getImage(), mnd.getParams(), 
								((CType_Function)mnd.getType()).getReturntype(), mnd.hasVarArg());
					}
							
					MethodNode mtNode = InterCallGraph.getInstance().findMethodNode(method.getFileName(), method);
					if(currMethod != null){
						currMethod.addCall(mtNode);
					}					
					mnd.setMethod(mtNode.getMethod());
				}
			}else
			{
				Method method = getMethod(mnd);
				MethodNode mtNode = InterCallGraph.getInstance().findMethodNode(mnd.getFileName(), method);
				if(currMethod != null){
					currMethod.addCall(mtNode);
				}
				mnd.setMethod(mtNode.getMethod());
			}
		}
		return super.visit(node, data);
	}

	/**
	 * ���ݺ���������Ϣ�����ɺ���������Ϣ
	 * 
	 * @param methodDecl ����������Ϣ
	 * @return ����������Ϣ
	 */
	public static Method getMethod(MethodNameDeclaration methodDecl) {
		if (methodDecl.getMethod() != null) {
			return methodDecl.getMethod();
		}
		Method method = new Method(methodDecl.getFileName(),methodDecl.getImage(), methodDecl.getParams(), 
				((CType_Function)methodDecl.getType()).getReturntype(), methodDecl.hasVarArg());
		return method;
	}
}
