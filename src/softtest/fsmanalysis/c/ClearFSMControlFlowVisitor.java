package softtest.fsmanalysis.c;
import softtest.cfg.c.*;



public class ClearFSMControlFlowVisitor implements GraphVisitor {
	/** �Խڵ���з��� */
	public void visit(VexNode n, Object data) {
		//n.setFSMMachineInstanceSet(null);
		n.getFSMMachineInstanceSet().clear();
	}

	/** �Ա߽��з��� */
	public void visit(Edge e, Object data) {

	}

	/** ��ͼ���з��� */
	public void visit(Graph g, Object data) {

	}
}
