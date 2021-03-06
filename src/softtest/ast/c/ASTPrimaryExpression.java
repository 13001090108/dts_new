/* Generated By:JJTree: Do not edit this line. ASTPrimaryExpression.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

import java.util.List;

import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;
import softtest.interpro.c.*;

public class ASTPrimaryExpression extends AbstractExpression {
	public ASTPrimaryExpression(int id) {
		super(id);
	}

	public ASTPrimaryExpression(CParser p, int id) {
		super(p, id);
	}

	public VariableNameDeclaration getVariableDecl() {
		VariableNameDeclaration ret = null;
		if (!image.equals("")) {
			Scope scope = getScope();
			NameDeclaration decl = Search.searchInVariableUpward(image, scope);
			if (decl instanceof VariableNameDeclaration) {
				ret = (VariableNameDeclaration) decl;
			}
		}
		return ret;
	}

	/** 
	 * 2011.6.24	目前尚未解决的问题：如果是函数指针形式的函数调用，如何获取正确的函数调用？
	 * @return
	 * @author zys
	 */
	public MethodNameDeclaration getMethodDecl() {
		if(!isMethod())
			return null;
		MethodNameDeclaration ret = null;
		if (!image.equals("")) {
			Scope scope = getScope();
			if(scope==null)
				return null;
			NameDeclaration decl = Search.searchInMethodUpward(image, scope);
			if (decl instanceof MethodNameDeclaration) {
				ret = (MethodNameDeclaration) decl;
				//dongyukun 临时消除method为空的现象
				MethodNameDeclaration mnd=ret;
				if(mnd.getMethod()==null&&mnd.getType() instanceof CType_Function){
					CType_Function ctype=(CType_Function)mnd.getType();
					Method  method=new Method(mnd.getFileName(),mnd.getImage(),mnd.getParams(),ctype.getReturntype(),ctype.isVarArg());
					ret.setMethod(method);
				}
				
			}
		}
		return ret;
	}

	/** Accept the visitor. **/
	public Object jjtAccept(CParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	//xyf
		private SimpleNode argNode;
		
		public void setArgumentNode(SimpleNode argNode) {
			this.argNode = argNode;
		}
		
		public SimpleNode getArgumnetNode() {
			return this.argNode;
		}
	
	
	public boolean isMethod() {
		ASTPostfixExpression postExpr=(ASTPostfixExpression) this.jjtGetParent();
		if(postExpr.getOperators().equals("(") /*&& jjtGetNumChildren()==0*/){
			/*zys 2011.6.22	对于函数指针的使用，不当作函数调用
			 * typedef struct {void (*fp)();}comp;
			 * void f(){
			 * 	comp com;
			 * 	(*com.fp)();//此处确实为函数调用，但函数指针目前并未精确处理，因此此处的函数调用语句跳过不处理
			 * }
			 */
			isMethod=true;
		}
		return isMethod;
	}
}
/* JavaCC - OriginalChecksum=fa377405528c9856524a71ef64b293ba (do not edit this line) */
