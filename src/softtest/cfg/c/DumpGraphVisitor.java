package softtest.cfg.c;

import java.io.*;
import java.util.*;

import softtest.config.c.*;

/** ���ڲ���.dot�ļ��Ŀ�����ͼ������ */
public class DumpGraphVisitor implements GraphVisitor { // ��ͼ�����ķ����߽ӿ�
	/** ���ʿ�����ͼ�Ľڵ㣬��ӡ�ڵ����֣���ǰ�ı����򼯣������޶��� */
	public void visit(VexNode n, Object data) {
		FileWriter out = (FileWriter) data;
		try {
			String s = "";
			
			if(Config.DUMP_SYMBOL){
				s=s+n.getValueSet();
			}
			if(Config.DUMP_DOMAIN){
				s = s + "\\n"+n.getVarDomainSet();
				//n.getStructDomain("test");
				if (n.getCondata() != null) {
					s = s + "\\n" + n.getCondata();
				}
			}
			
			s = s+"\"" ;
			if (n.getContradict()) {
				s = s + ",color=red";
			}
			out.write(n.name + "[label=\"" + n.name + "\\n" + s + "];\n");
		} catch (IOException ex) {
		}
	}

	/** ���ʿ�����ͼ�ıߣ���ӡ���� */
	public void visit(Edge e, Object data) {
		FileWriter out = (FileWriter) data;
		try {
			String s = null;
			s = e.tailnode.name + " -> " + e.headnode.name + "[label=\"" + e.name + "\"";
			if (e.getContradict()) {
				s = s + ",color=red";
			}
			s = s + "];\n";
			out.write(s);
		} catch (IOException ex) {
		}
	}

	/** ���ʿ�����ͼ������������ڵ㼯�Ϻͱ߼��� */
	public void visit(Graph g, Object data) {
		FileWriter out = null;
		try {
			out = new FileWriter((String) data);
			out.write("digraph G {\n");

			for (Enumeration<VexNode> e = g.nodes.elements(); e.hasMoreElements();) {
				VexNode n = e.nextElement();
				visit(n, out);
			}

			for (Enumeration<Edge> e = g.edges.elements(); e.hasMoreElements();) {
				Edge edge = e.nextElement();
				visit(edge, out);
			}
			out.write(" }");
			
		} catch (IOException ex) {
		}
		//modified by liuyan 2015.6.3
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
}
