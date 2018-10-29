package softtest.scvp.c;

import java.io.FileWriter;
import java.io.IOException;

import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.GraphVisitor;
import softtest.cfg.c.VexNode;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;

public class SCVPDumpVisitor implements GraphVisitor{

	public void visit(VexNode n, Object data) {
		try {
			FileWriter fw = (FileWriter)data;
			for(NameOccurrence occ:n.getOccurrences()) {
				fw.append("���ų��֣�"+occ.toString()+"\n");
				fw.append("���ͣ�"+occ.getOccurrenceType().toString()+"\n");
				fw.append("��ֵ��Ϣ��"+"\n");
				if (occ.getOccurrenceType() == OccurrenceType.USE && occ.getUse_def()!=null) {
					fw.append("����㣺" +occ.getUse_def().toString()+"\n" );
				}
				fw.append(occ.getSCVP()+"\n");			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}

	public void visit(Edge e, Object data) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Graph g, Object data) {
		// TODO Auto-generated method stub
		
	}

}
