package softtest.scvp.c;

import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;

public class SCVPUtils {
    public static boolean isOnLeftHandSide(SimpleNode location) {
    	SimpleNode declarator=null;
        if (location.jjtGetParent() instanceof ASTDeclarator) {
        	declarator = (ASTDeclarator) location.jjtGetParent();
        	if(declarator.getNextSibling()!=null){
            	return true;
            }
        } 
        //Ϊ��֧�������±�Ķ���ʹ�ö��壬ԭ�еı������ַ��ʲ���Pri������Post������location�����仯
        //add by zhouhb
        //2011.3.23
        if (location instanceof ASTPostfixExpression ) {
        	Node assignment=location.jjtGetParent().jjtGetParent();
        	if(assignment instanceof ASTAssignmentExpression && assignment.jjtGetNumChildren()==3){
        		return true;
        	}
        } 
        if(location.jjtGetParent() instanceof ASTPostfixExpression)
        {
        	declarator = (ASTPostfixExpression) location.jjtGetParent();
        	Node assignment=declarator.jjtGetParent().jjtGetParent();
        	if(assignment instanceof ASTAssignmentExpression && assignment.jjtGetNumChildren()==3){
        		return true;
        	}
        }
        return false;
    }
}
