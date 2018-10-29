package softtest.scvp.c;


import java.io.FileWriter;
import java.io.IOException;


import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.cfg.c.Graph;
import softtest.dscvp.c.NameOccurrenceHashtableVisitor;
import softtest.interpro.c.Method;

public class SCVPVisitor extends CParserVisitorAdapter {
	@Override
	public Object visit(ASTFunctionDefinition node, Object data)
	{
		Graph g = node.getGraph();
		if (g == null) {
			return null;
		}
		
		// added by cmershen,2016.5.3
		g.numberOrderVisit(new SCVPControlFlowVisitor(), data);
		// added by cmershen,2016.5.17
		NameOccurrenceHashtableVisitor nhvisitor = new NameOccurrenceHashtableVisitor();
		g.numberOrderVisit(nhvisitor, data);
		
		// ��������������
		SideEffectsVisitor visitor = new SideEffectsVisitor();	
		g.numberOrderVisit(visitor, data);
		//�������������Ϣ���ļ�
		//added by cmershen 2016.12.13
//		try {
//			Method m = node.getDecl().getMethod();
//			String filename="c:\\scvp\\"+m.toString()+"_scvp.txt";
//			FileWriter fw = new FileWriter(filename, true);
//			
//			g.numberOrderVisit(new SCVPDumpVisitor(), fw);
//			
//			fw.append("ǰ��ժҪ��\n");
//			fw.append(m.getCallerInfo().toString()+"\n");
//			
//			fw.append("����ժҪ��\n");
//			fw.append(m.getExternalEffects().toString()+"\n");
//			
//			fw.append("����ֵ��\n");
//			fw.append(m.getReturnList().toString()+"\n");
//			
//			fw.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		return null;
	}
	
}
