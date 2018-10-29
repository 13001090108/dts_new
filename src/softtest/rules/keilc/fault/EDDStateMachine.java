package softtest.rules.keilc.fault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTStorageClassSpecifier;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * @author st EDD - Extern variable type Differ with Definition
 */
public class EDDStateMachine {

	/**
	 * ����ȫ�ֱ���
	 */
	private static Map<String, String> globals = new HashMap<String, String>();

	/**
	 * ���д������ⲿ��������ĳ���ⲿ�����Ѿ���飬��Ӵ˱��г�ȥ
	 */
	private static Map<String, String> externs = new HashMap<String, String>();
	private static Map<String, ExternVariable> externMsg = new HashMap<String, ExternVariable>();
	
	public static List<FSMMachineInstance> createEDDStateMachines(	SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		String xpath = ".//Declaration[.//DeclarationSpecifiers/StorageClassSpecifier[@Image='extern']]/InitDeclaratorList/InitDeclarator/Declarator/DirectDeclarator";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xpath);
		String RType[]={"idata","pdata","xdata","bdata","far","data","code"};
		// ���ÿ���ļ���������ȫ�ֱ���
		addGlobals(node);

		List<String> toRemove = new ArrayList<String>();
		// ����֮ǰ�ļ���δ�ܴ����extern�ⲿ����
		for (String exVarName : externs.keySet()) {
			if (globals.get(exVarName) != null) {
				String defType = globals.get(exVarName);
				String useType = externs.get(exVarName);
				if (defType == null || useType == null) {
					continue;
				}
				if (!defType.equals(useType)) {
					//chh  �����xdata,idata��keil c51�Ĵ洢������˵���ı���
					for(String rtype:RType)
					{
						if(defType.contains(rtype))
						{
							defType=defType.replaceFirst(rtype+" ", "");
						}
						if(useType.contains(rtype))
						{
							useType=useType.replaceFirst(rtype+" ", "");
						}
					}
					//end
					//chh  ����volatile��ʾ�Ĳ��ȶ�������Ҫȥ����volatile��������ܱȽ����ͣ������п��ܲ�����
					if(defType.contains("volatile"))
					{
						defType=defType.replaceFirst("volatile"+" ", "");
					}
					if(useType.contains("volatile"))
					{
						useType=useType.replaceFirst("volatile"+" ", "");
					}
					//end
					//chh  �������飬������Ϊ��[**]type��,����Ҫ���е������ж�
					if(defType.contains("]")&&useType.contains("]")&&!defType.equals(useType))
					{
						String tmp=defType.substring(defType.indexOf("]"));
						if(!useType.endsWith(tmp))
							{
							addFSM(list, null, fsm, exVarName);
								continue;
							}
						else
						{
							defType=defType.substring(defType.indexOf("["),defType.indexOf("]")).trim();
							useType=useType.substring(useType.indexOf("["),useType.indexOf("]")).trim();
							if(defType.equals("[")||useType.equals("["))
								continue;
							else 
								addFSM(list, null, fsm, exVarName);
						}
					}
					//end
					if (!defType.equals(useType))
					addFSM(list, null, fsm, exVarName);
				}
				toRemove.add(exVarName);
			}
		}
		for(String exVarName : toRemove) {
			externs.remove(exVarName);
		}
		
		
		// ����ÿ���ļ���������extern�ⲿ����
		for (Iterator<SimpleNode> itr = evaluationResults.iterator(); itr.hasNext();) {
			ASTDirectDeclarator declNode = (ASTDirectDeclarator) itr.next();
			String exVarName = declNode.getImage();
			if (globals.get(exVarName) != null) {
				String defType = globals.get(exVarName);
				String useType = declNode.getDecl().getType().toString();
				if (defType == null || useType == null) {
					continue;
				}
				if (!defType.equals(useType)) {
					//chh  �����xdata,idata��keil c51�Ĵ洢������˵���ı���
					for(String rtype:RType)
					{
						if(defType.contains(rtype))
						{
							defType=defType.replaceFirst(rtype+" ", "");
						}
						if(useType.contains(rtype))
						{
							useType=useType.replaceFirst(rtype+" ", "");
						}
					}
					//end
					//chh  ����volatile��ʾ�Ĳ��ȶ�������Ҫȥ����volatile��������ܱȽ����ͣ������п��ܲ�����
					if(defType.contains("volatile"))
					{
						defType=defType.replaceFirst("volatile"+" ", "");
					}
					if(useType.contains("volatile"))
					{
						useType=useType.replaceFirst("volatile"+" ", "");
					}
					//end
					//chh  �������飬������Ϊ��[**]type��,����Ҫ���е������ж�
					if(defType.contains("]")&&useType.contains("]")&&!defType.equals(useType))
					{
						String tmp=defType.substring(defType.indexOf("]"));					
						if(!useType.endsWith(tmp))
							{
								addFSM(list, declNode, fsm, null);
								continue;
							}
						else
						{
							defType=defType.substring(defType.indexOf("["),defType.indexOf("]")).trim();
							useType=useType.substring(useType.indexOf("["),useType.indexOf("]")).trim();
							if(defType.equals("[")||useType.equals("["))
								continue;
							else 
								addFSM(list, declNode, fsm, null);
						}
					}
					//end
					if (!defType.equals(useType))
					{
					addFSM(list, declNode, fsm, null);
					}
				}
			} else {
				// ��ʱ���ܴ�ȫ�ֱ�������鵽����ӵ���ϣ��
				if (declNode.getDecl() instanceof VariableNameDeclaration) {
					externs.put(declNode.getImage(), declNode.getDecl().getType().toString());
					externMsg.put(declNode.getImage(), new ExternVariable(declNode.getImage(), declNode.getFileName(), declNode.getBeginLine()));
				}
			}
		}
		return list;
	}

	private static void addGlobals(SimpleNode node) {
		SourceFileScope sfScope = node.getScope().getEnclosingSourceFileScope();
		for (VariableNameDeclaration varDecl : sfScope.getVariableDeclarations().keySet()) {
			//zys 2010.3.29:���ں���ָ����int (*funP)();������δ֪
			if(varDecl.getTypeImage()!=null){
			
				/*chenhonghe 2010.4.8 �����ⲿ��externa����������Ӧ����Ϊȫ�ֱ����ӽ��� �����Լ�������ж�*/
				
					if(((SimpleNode)varDecl.getNode().jjtGetParent().jjtGetParent().jjtGetParent().jjtGetParent().jjtGetChild(0).jjtGetChild(0) instanceof ASTStorageClassSpecifier))
						{
							if(((SimpleNode)varDecl.getNode().jjtGetParent().jjtGetParent().jjtGetParent().jjtGetParent().jjtGetChild(0).jjtGetChild(0) ).getImage()!="extern")
							globals.put(varDecl.getImage(), varDecl.getTypeImage());
						}
					else    globals.put(varDecl.getImage(), varDecl.getTypeImage());
					
			}
		}
	}

	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node,FSMMachine fsm, String name) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		if (node != null) {
			VariableNameDeclaration varDecl = node.getVariableNameDeclaration();
			fsminstance.setRelatedASTNode(node);
			fsminstance.setRelatedVariable(varDecl);
			fsminstance.setDesp("�ⲿ����" + varDecl.getImage() + "���ļ�" + varDecl.getFileName() + "��" + node.getBeginLine() + "�е�������ñ����Ķ��岻ͬ");
		} else {
			ExternVariable extern = externMsg.get(name);
			fsminstance.setDesp("�ⲿ����" + name + "���ļ�" + extern.getFileName() + "��" + extern.getBeginLine() + "�е�������ñ����Ķ��岻ͬ");
		}
		list.add(fsminstance);
	}

	private static class ExternVariable {
		
		private String name;

		private String fileName;

		private int beginLine;

		public ExternVariable() {
			
		}
		
		public ExternVariable(String name, String fileName, int beginLine) {
			this.name = name;
			this.fileName = fileName;
			this.beginLine = beginLine;
		}

		public int getBeginLine() {
			return beginLine;
		}

		public void setBeginLine(int beginLine) {
			this.beginLine = beginLine;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}
	
}


