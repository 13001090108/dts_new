package softtest.rules.gcc.question;


import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.MethodNameDeclaration;



/** 
 * @author chh
 */
public class MCStateMachine {

	
	// math���ļ��е��������� acos  asin  atan  cos  sin  tanh  sqrt  fabs, cosh ,sinh,ceil,exp,floor,tan,log,log10   stdlib���ļ��е��������� abs labs.
	private static String xpath1 = ".//PostfixExpression[./PrimaryExpression[@Image='acos' or @Image='asin' or @Image='atan' or @Image='cos' or @Image='sin' or @Image='tanh' or @Image='sqrt' or @Image='fabs' or @Image='abs'" +
			" or @Image='labs' or @Image='cosh' or @Image='sinh' or @Image='ceil' or @Image='exp' or @Image='floor' or @Image='tan' or @Image='log' or @Image='log10']and ./ArgumentExpressionList[count(*)=1 and .//Constant]]";
	//math���ļ���˫�������� atan2 frexp pow fmod
	private static String xpath2 = ".//PostfixExpression[./PrimaryExpression[@Image='atan2' or @Image='frexp' or @Image = 'pow' or @Image='fmod'] and ./ArgumentExpressionList[count(*)=2 and count(.//Constant)=2]]";
	private static String xpath_math = xpath1 + " | " + xpath2;
	
	public static List<FSMMachineInstance> createMathConstantMachines(SimpleNode node, FSMMachine fsm){
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		
		evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath_math);
		for (SimpleNode snode : evaluationResults) {
			SimpleNode pnode = (SimpleNode)snode.jjtGetChild(0);
			MethodNameDeclaration methodDecl = StateMachineUtils.getMethodNameDeclaration(pnode);
			if(methodDecl != null && methodDecl.isLib() ) {
					addFSM(snode, fsm, list);
			}
		}
	    return list;
	}	
	
	private static void addFSM(SimpleNode node, FSMMachine fsm, List<FSMMachineInstance> list) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
				fsminstance.setDesp(" Use  a lib function "+node.getImage()+" with its argument is constant");
			} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
				fsminstance.setDesp("��"+node.getBeginLine()+"��:ʹ���˲���Ϊ��������ѧ����"+node.getImage()+",��ʹ��math�еĿ⺯��ʱ�����������ǳ���������ֵ����ʱ�Ϳɼ���������Ϊ���������Ӧֱ��ʹ����������");
			}		
		list.add(fsminstance);
	}
}