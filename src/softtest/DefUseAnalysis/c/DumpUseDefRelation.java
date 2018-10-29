package softtest.DefUseAnalysis.c;

import java.io.*;

import softtest.cfg.c.*;
import softtest.symboltable.c.*;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;

public class DumpUseDefRelation implements GraphVisitor { // ��ͼ�����ķ����߽ӿ�
	static String[] colors = { "coral", "crimson", "hotpink", "lightpink",
			"orangered", "pink", "red", "violetred", "brown", "chocolate",
			"rosybrown", "saddlebrown", "sandybrown", "tan", "darkorange",
			"orange", "orangered", "darkgoldenrod", "gold", "greenyellow",
			"lightyellow", "yellow", "yellowgreen", "chartreuse", "darkgreen",
			"forestgreen", "limegreen", "red", "palegreen", "springgreen",
			"aquamarine", "cyan", "lightcyan", "turquoise", "aliceblue",
			"blue", "blueviolet", "darkslateblue", "lightblue", "navy",
			"powderblue", "skyblue", "steelblue", "darkviolet", "orchid",
			"purple", "violet" };

	/** ���ʿ�����ͼ�Ľڵ� */
	public void visit(VexNode n, Object data) {
		FileWriter out = (FileWriter) data;
		try {
			String s = "";
			// s = s + ",color=red";
			out.write(n.getName() + "[label=\"" + n.getName() + "\"" + "];\n");
			for (NameOccurrence occ : n.getOccurrences()) {
				if (!(occ.getDeclaration() instanceof VariableNameDeclaration)) {
					continue;
				}
				VariableNameDeclaration v = (VariableNameDeclaration) occ
						.getDeclaration();
				if (occ.getOccurrenceType() == OccurrenceType.USE || occ.getOccurrenceType() == OccurrenceType.DEF_AFTER_USE) {
					for (NameOccurrence use : occ.getUse_def()) {
						VexNode vex = use.getLocation().getCurrentVexNode();
						if (vex == null) {
							continue;
						}
						s = n.getName() + " -> " + vex.getName() + "[label=\""
								+ v.getImage() + "(DU)\"";
						s = s
								+ ",color="
								+ colors[occ.getLocation().hashCode()
										% colors.length]
								+ ",style=dashed,arrowsize=0.4";
						s = s + "];\n";
						out.write(s);
					}
				}
			}
		} catch (IOException ex) {
		}
	}

	/** ���ʿ�����ͼ�ıߣ���ӡ���� */
	public void visit(Edge e, Object data) {
		FileWriter out = (FileWriter) data;
		try {
			String s = "";
			s = e.getTailNode().getName() + " -> " + e.getHeadNode().getName()
					+ "[label=\"" + e.getName() + "\"";
			// s= s + ",color=red";
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

			for (VexNode n : g.nodes.values()) {
				visit(n, out);
			}
			for (Edge e : g.edges.values()) {
				visit(e, out);
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
