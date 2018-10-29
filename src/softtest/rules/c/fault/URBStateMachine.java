package softtest.rules.c.fault;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;

/**
 * ����Ƿ��в��ɴ��֧
 * @author zys	
 * 2010-4-13
 */
public class URBStateMachine {
	
	public static List<FSMMachineInstance> createURBStateMachines(SimpleNode node, FSMMachine fsm) 
	{
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		if(node instanceof ASTFunctionDefinition)
		{
			Graph g=((ASTFunctionDefinition)node).getGraph();
			Hashtable<String, Edge> nodesTable=g.edges;
	out:	for(Edge edge:nodesTable.values())
			{
				if(edge.getContradict() && 
						(edge.getName().startsWith("T") || edge.getName().startsWith("F")))
				{
					//zys:2010.4.23 ��һ�����ɴ��ֻ֧�ϱ�һ��
					//���ҵ�ǰì�ܱߵ���һ���ߣ����������Ҳì�ܣ��򱾴β����ϱ�����ֻ�ڲ��ɴ��֧�ĵ�һ��ì�ܱ����ϱ���
//					VexNode pre=edge.getTailNode();
//					for(Edge preEdge:pre.getInedges().values())
//					{
//						if(preEdge.getContradict())
//							break out;
//					}
					VexNode temp=edge.getTailNode();
					addFSM(list, temp, fsm);
				}
			}
		}
		return list;
	}
	
	private static void addFSM(List<FSMMachineInstance> list, VexNode vex, FSMMachine fsm) {
		//System.out.println("��ǰ������"+(num++)+"���Զ�����");
		FSMMachineInstance fsminstance = fsm.creatInstance();
		SimpleNode node=vex.getTreenode();
		fsminstance.setRelatedASTNode(node);
		fsminstance.setReleatedVexNode(vex);
		String vexName=vex.getName();
		if(vexName.startsWith("if")){
			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�е�if���ڲ��ɴ��֧");
		}else if(vexName.startsWith("for")){
			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�е�forѭ�����ڲ��ɴ��֧");
		}else if(vexName.startsWith("switch")){
			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�е�switch���ڲ��ɴ��֧");
		}else if(vexName.startsWith("while")){
			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�е�while���ڲ��ɴ��֧");
		}else if(vexName.startsWith("do-while")){
			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�е�do-while���ڲ��ɴ��֧");
		}else{
			fsminstance.setDesp("λ�ڵ�"+node.getBeginLine()+"�д��ڲ��ɴ��֧");
		}
		list.add(fsminstance);
	}
}
