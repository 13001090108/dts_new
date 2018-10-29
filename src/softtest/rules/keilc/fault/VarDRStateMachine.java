package softtest.rules.keilc.fault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTInterrupt;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.SimpleNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.symboltable.c.MethodScope;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * @author zys, Songying
 * VarDR--Variable Data Race���������ݾ���֮ �жϺ��������������ͬ�ı���
 * */
public class VarDRStateMachine {
	
	public static List<FSMMachineInstance> createVarDRStateMachines(SimpleNode node, FSMMachine fsm) 
	{
		
		//System.out.println("Enter the creation process now" + "\n" + node.getClass());
		//�ж�node�ڵ�����ʣ������ж�Scope = ��File�������á�
		//if Scope = "File", node.class = xx.ASTTranslationUnit;
		//else { node.class = ASTFunctionDefinition }.
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		
		Map <VariableNameDeclaration, ArrayList<NameOccurrence>> GlbVarMap ;
		
		GlbVarMap = node.getScope().getEnclosingSourceFileScope().getVariableDeclarations(); // Find all the global variables in the files where the function in
		
		//zys:
		for(VariableNameDeclaration var:GlbVarMap.keySet())
		{
			ArrayList<NameOccurrence> occurList=GlbVarMap.get(var);
			
			//Ϊÿ��ȫ�ֱ�������һ���б����б�����ʹ�øñ����ĺ�������ڵ�
			ArrayList<ASTFunctionDefinition> occurInFuncList=new ArrayList<ASTFunctionDefinition>();
			
			for(NameOccurrence o: occurList)
			{
				//�ҵ���ǰ�������ֽڵ����ڵĺ����ڵ�,��ͬһ�����еĶ�γ���ֻ���һ��
				SimpleNode occurNode=o.getLocation();
				ASTFunctionDefinition funcNode=(ASTFunctionDefinition) occurNode.getFirstParentOfType(ASTFunctionDefinition.class);
				if(funcNode!=null && !occurInFuncList.contains(funcNode))
				{
					occurInFuncList.add(funcNode);
				}
			}
			
			//���ʹ���˵�ǰ�����ĺ����б����Ƿ�ͬʱ�����жϺ�������жϺ���
			if(occurInFuncList.size()<2)
			{
				continue;
			}
			else for(ASTFunctionDefinition n:occurInFuncList)
			{
				if(n.findDirectChildOfType(ASTInterrupt.class).size()>0)
				{
					addFSM(list, n, var,fsm);
					//System.out.println("add a FSM");
					break;
				}
				
			}
		}
		/*
		Iterator itr_GlbVarMap  = GlbVarMap.entrySet().iterator(); // travle the map of global variables
		while (itr_GlbVarMap.hasNext())
		{
			Map.Entry entry = (Map.Entry) itr_GlbVarMap.next();
			VariableNameDeclaration VarName = (VariableNameDeclaration) entry.getKey();
			ArrayList<NameOccurrence> Array_NameOcr = (ArrayList<NameOccurrence>) entry.getValue();
			
			for(NameOccurrence o:Array_NameOcr)
			{
				o.getLocation()
			}
			System.out.println(VarName);
			if(VarName.getScope().getEnclosingMethodScope()!=null)
			{
				System.out.println(" in Function: "+(VarName.getScope().getEnclosingMethodScope()).getName());
			}
			System.out.println(entry.getValue());
		}*/
		
		return list;
		
		
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, VariableNameDeclaration nd, FSMMachine fsm) {
		FSMMachineInstance fsminstance = fsm.creatInstance();
		fsminstance.setRelatedASTNode(node);
		fsminstance.setRelatedVariable(nd);
		fsminstance.setReleatedVexNode(node.getCurrentVexNode());
		fsminstance.setDesp("ȫ�ֱ���" + nd.getImage() + "���жϺ���" + node.getImage() + "�з��ʳ�ͻ");
		list.add(fsminstance);
	}
	/*
	  	Map map = new HashMap();
       	Iterator iter = map.entrySet().iterator();
       	while (iter.hasNext()) {
    	Map.Entry entry = (Map.Entry) iter.next();
    	Object key = entry.getKey();
    	Object val = entry.getValue();
    	
    	
} 
	 */
	/*
	public static boolean checkSameVarMod(List nodes, FSMMachineInstance fsmin) 
	{
		
		
	}
	
	public static boolean checkAssign(List nodes, FSMMachineInstance fsmin) {
		
	}
	
	public static boolean checkDuplicateMod(List nodes, FSMMachineInstance fsmin) 
	{
		
		
	}
	
	private static void addFSM(List<FSMMachineInstance> list, SimpleNode node, FSMMachine fsm) 
	{
		
	}
	
	private static ASTPrimaryExpression getPrimarynode(SimpleNode node) {

		
	}
*/	
}
