package softtest.rules.gcc.rule;


import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.Set;


import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.SimpleNode;

import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.interpro.c.InterContext;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.LocalScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.MethodScope;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;


/**
 * @author LiuChang
 * Same Name 
 */
public class SNStateMachine {


	
	public static List<FSMMachineInstance> createSNStateMachines(SimpleNode node, FSMMachine fsm) {
		MethodNameDeclaration md = InterContext.getMethodDecl(node.getCurrentVexNode());
		if(md == null || md.isLib()) {
			return null;
		}
		Set<String> globals = new HashSet<String>();
		if(node.getScope() instanceof MethodScope) {
			MethodScope ms = (MethodScope) node.getScope();
			SourceFileScope sfs = ms.getEnclosingSourceFileScope();
			if(sfs != null) {
				for(VariableNameDeclaration v : sfs.getVariableDeclarations().keySet()) {
					if(v.isParam() == false)
					globals.add(v.getImage());
				}
			} 
		}
		

		
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		List<SimpleNode> result = null;
		List<SimpleNode> result1 = null;
		String xpath = ".//Statement//DirectDeclarator";//�ֲ�����
		String path = ".//DirectDeclarator";//���������ľֲ�����
		result = StateMachineUtils.getEvaluationResults(node, xpath);
		result1 = StateMachineUtils.getEvaluationResults(node, path);
		Iterator<SimpleNode> itr = result.iterator();
		Iterator<SimpleNode> itr1 = result1.iterator();
		while(itr.hasNext()) {
			ASTDirectDeclarator id = (ASTDirectDeclarator) itr.next();
			if(globals.contains(id.getImage()) 
					&& id.getVariableNameDeclaration() != null 
					&& id.getVariableNameDeclaration().getScope() instanceof LocalScope) {
				addFSM(list, id, fsm, null);
			}
			}
		while(itr1.hasNext()){
			ASTDirectDeclarator id = (ASTDirectDeclarator) itr1.next();
			if(globals.contains(id.getImage())) {
				addFSM(list, id, fsm, null);
			}
		}
		return list;
	}

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm, SimpleNode n) {
		FSMMachineInstance fsmInstance = fsm.creatInstance();
		fsmInstance.setRelatedASTNode(node);
		VariableNameDeclaration v = (node).getVariableNameDeclaration();
		if(v == null)
			return;		
			if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) 		
				fsmInstance.setDesp("the local variable" + v.getImage() + "in line" + node.getBeginLine() + "has the same name with a global variable");
			else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
				fsmInstance.setDesp("�ڵ� " + node.getBeginLine() + " �еľֲ����� " + v.getImage() + " ��ȫ�ֱ���ͬ����\n" +
				"C���Ա�����������ֲ�������ȫ�ֱ���ͬ�������ֲ�������������ֻ������������ģ���ڲ���Ϊ���Ȿ������Ҫ��ȫ�ֱ������£������ڴ���ͬ���ľֲ�����������ȫ�ֱ���δ�õ�ʵ����Ч�ĸ��£���˽�ֹ�ֲ�������ȫ�ֱ���ͬ����");
			
		
		list.add(fsmInstance);
	}
	
}
