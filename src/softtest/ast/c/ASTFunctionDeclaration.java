/* Generated By:JJTree: Do not edit this line. ASTFunctionDeclaration.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

import java.util.ArrayList;

import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.Type.CType;

public class ASTFunctionDeclaration extends SimpleNode {
	CType type = null;
	
	ArrayList<Boolean> flags = new ArrayList<Boolean>();
	
	MethodNameDeclaration decl;
	
	public MethodNameDeclaration getDecl() {
		return decl;
	}

	public void setDecl(MethodNameDeclaration decl) {
		this.decl = decl;
	}

	public CType getType() {
		return type;
	}

	public void setType(CType type) {
		this.type = type;
	}

	public ASTFunctionDeclaration(int id) {
		super(id);
	}

	public ASTFunctionDeclaration(CParser p, int id) {
		super(p, id);
	}

	@Override
	public String getImage() {
		if (image.equals("")) {
			SimpleNode child = (SimpleNode) getFirstDirectChildOfType(ASTDeclarator.class);
			if (child != null) {
				image = child.getImage();
			}
		}
		return image;
	}

	/** Accept the visitor. **/
	public Object jjtAccept(CParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
	
	public ArrayList<Boolean> getFlags() {
		return flags;
	}

	public void setFlags(ArrayList<Boolean> flags) {
		this.flags = flags;
	}
	
	void setFlag(boolean flag){
		flags.add(flag);
	}
	
	public void setOperatorTypeAndFlag(String str,boolean flag){
		this.setOperatorType(str);
		this.setFlag(flag);
	}
	
	public int getFirstSlot(){
		int ret=jjtGetNumChildren();
		for(boolean b:flags){
			if(b){
				ret--; 
			}
		}
		return ret;
	}
}
/*
 * JavaCC - OriginalChecksum=124b5580d975d679bad59213497626ae (do not edit this
 * line)
 */
