/* Generated By:JJTree: Do not edit this line. ASTAdditiveExpression.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

public
class ASTAdditiveExpression extends AbstractExpression {
  public ASTAdditiveExpression(int id) {
    super(id);
  }

  public ASTAdditiveExpression(CParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=54012d6d05c05c145c521cebfd738d2f (do not edit this line) */
