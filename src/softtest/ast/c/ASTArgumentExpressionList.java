/* Generated By:JJTree: Do not edit this line. ASTArgumentExpressionList.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

public
class ASTArgumentExpressionList extends AbstractExpression {
  public ASTArgumentExpressionList(int id) {
    super(id);
  }

  public ASTArgumentExpressionList(CParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=670939c78062e6d1c56dfd3011765973 (do not edit this line) */