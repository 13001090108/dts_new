/* Generated By:JJTree: Do not edit this line. ASTDirectDeclarator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

import java.util.ArrayList;

import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.Type.CType;

public class ASTDirectDeclarator extends SimpleNode {
	
	/**
	 * @author	zys
	 * KeilC中的绝对地址变量声明，如int xdata x _at_ 0x9000
	 */
	private String memoryAddress=null;
	
	NameDeclaration decl=null;
	
	public NameDeclaration getDecl() {
		return decl;
	}

	public void setDecl(NameDeclaration decl) {
		this.decl = decl;
	}

	boolean istypedef = false;

	CType type = null;
	
	ArrayList<Boolean> flags = new ArrayList<Boolean>();

	public ASTDirectDeclarator(int id) {
		super(id);
	}

	public ASTDirectDeclarator(CParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(CParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public boolean isIstypedef() {
		return istypedef;
	}

	public void setIstypedef(boolean istypedef) {
		this.istypedef = istypedef;
	}

	public CType getType() {
		return type;
	}

	public void setType(CType type) {
		this.type = type;
	}
	
	@Override
	public String getImage() {
		if(image.equals("")){
			SimpleNode child=(SimpleNode)this.getFirstDirectChildOfType(ASTDeclarator.class);
			if(child!=null){
				image=child.getImage();
			}
		}
		return image;
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
	
	public boolean isFunctionName(){
		//zys:对于函数内部的函数声明节点进行处理
		if(this.getOperators().equals("("))
		{
			return true;
		}
		ASTDeclarator de = this.getTopDeclarator();
		if (de.jjtGetParent() instanceof ASTFunctionDefinition
				|| de.jjtGetParent() instanceof ASTFunctionDeclaration
				|| de.jjtGetParent() instanceof ASTNestedFunctionDefinition
				|| de.jjtGetParent() instanceof ASTNestedFunctionDeclaration) {
			return true;
		}
		if(de.jjtGetParent() instanceof ASTDirectDeclarator){
			ASTDirectDeclarator dir = (ASTDirectDeclarator) de.jjtGetParent();
			if(dir.isFunctionName() && this.image.equals(dir.image)){
				return true;
			}
		}
		return false;
	}
	//dyk 20120517
	public ASTDeclarator getTopDeclarator(){
		ASTDeclarator ret=null;
				
		ASTDirectDeclarator nodeDire=this;
		SimpleNode nodeInit=(SimpleNode)nodeDire.getFirstParentOfType(ASTInitDeclarator.class);
		if(nodeInit!=null)
		{
			ret=(ASTDeclarator)nodeInit.getFirstChildOfType(ASTDeclarator.class);
		}else
		{
			ret=(ASTDeclarator)this.jjtGetParent();
		}
		
		return ret;
	}

	public String getMemoryAddress() {
		return memoryAddress;
	}

	//xyf
	
		private SimpleNode argNode;
		
		public void setArgumentNode(SimpleNode argNode) {
			this.argNode = argNode;
		}
		
		public SimpleNode getArgumnetNode() {
			return this.argNode;
		}
	
	
	/**
	 * @author zys	2010.3.5
	 * <p>如果为keilc中的绝对地址变量声明，则添加变量的memoryAddress属性</p>
	 */
	public void setMemoryAddress() {
		if(this.jjtGetParent() instanceof ASTDeclarator)
		{
			ASTDeclarator declarator=(ASTDeclarator)this.jjtGetParent();
			if(declarator.getNextSibling() instanceof ASTAssignmentExpression){
				ASTAssignmentExpression addressNode=(ASTAssignmentExpression)declarator.getNextSibling();
				ASTConstant constantNode=(ASTConstant)(addressNode.getFirstChildOfType(ASTConstant.class));
				memoryAddress=constantNode.getImage();
			}
		}
	}
}
/*
 * JavaCC - OriginalChecksum=b320189fa712340342ce8676d23675b8 (do not edit this
 * line)
 */
