package softtest.fsm.c;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;

/** ���ڲ���.dot�ļ���״̬�������� */
public class DumpFSMVisitor implements FSMVisitor {
	/** ����״̬����״̬����ӡ״̬���� */
	public void visit(FSMState n, Object data) {
		FileWriter out = (FileWriter) data;
		try {
			String s = "";
			if (n.isStart()) {
				s = s + ",style=bold";
			}
			if (n.isFinal()) {
				s = s + ",peripheries=2";
			}
			if (n.isError()) {
				s = s + ",color=red";
			}
			// s=s+",orientation=90";
			out.write(n.getName() + "[label=\"" + n.getName() + "\"" + s
					+ "];\n");
		} catch (IOException ex) {
		}
	}

	/** ����״̬����ת������ӡ���� */
	public void visit(FSMTransition e, Object data) {
		FileWriter out = (FileWriter) data;
		try {
			String s = "";
			s = e.getFromState().getName() + " -> " + e.getToState().getName()
					+ "[label=\"" + e.getName() + "\"";
			s = s + "];\n";
			out.write(s);
		} catch (IOException ex) {
		}
		// ������ǰû�д�ӡ
		Iterator<FSMCondition> i = e.getConditions().iterator();
		while (i.hasNext()) {
			visit(i.next(), out);
		}
	}

	/** ����״̬��������������״̬���Ϻ�ת������ */
	public void visit(FSMMachine g, Object data) {
		FileWriter out = null;
		try {
			out = new FileWriter((String) data);
			out.write("digraph G {\n");
			out.write("label=\"" + g.getName() + "\";\n");
			// out.write("rotate=45;\n");

			for (Enumeration<FSMState> e = g.getStates().elements(); e
					.hasMoreElements();) {
				FSMState n = e.nextElement();
				visit(n, out);
			}

			for (Enumeration<FSMTransition> e = g.getTransitions().elements(); e
					.hasMoreElements();) {
				FSMTransition edge = e.nextElement();
				visit(edge, out);
			}
			out.write(" }");
		} catch (IOException ex) {
		}
		//added finally by liuyan 2015.6.3
		finally{
			if( out != null ){
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/** ���������� */
	public void visit(FSMCondition c, Object data) {
		// dataΪFileWriter
	}
}