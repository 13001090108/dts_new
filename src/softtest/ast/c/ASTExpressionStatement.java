/* Generated By:JJTree: Do not edit this line. ASTExpressionStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

public
class ASTExpressionStatement extends SimpleNode {
  public ASTExpressionStatement(int id) {
    super(id);
  }

  public ASTExpressionStatement(CParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=5dbfff3c1dca43da9082358f0549b180 (do not edit this line) */
