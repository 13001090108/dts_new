/* Generated By:JJTree: Do not edit this line. ASTStructDeclaratorList.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package softtest.ast.c;

public
class ASTStructDeclaratorList extends SimpleNode {
  public ASTStructDeclaratorList(int id) {
    super(id);
  }

  public ASTStructDeclaratorList(CParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=4faf69d6c6de5b91a790c1f951eedb03 (do not edit this line) */
