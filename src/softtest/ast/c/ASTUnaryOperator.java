/* Generated By:JJTree: Do not edit this line. ASTUnaryOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

public
class ASTUnaryOperator extends SimpleNode {
  public ASTUnaryOperator(int id) {
    super(id);
  }

  public ASTUnaryOperator(CParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=021b2052076bc17f32879a9ba72302ba (do not edit this line) */