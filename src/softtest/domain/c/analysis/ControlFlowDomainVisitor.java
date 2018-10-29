package softtest.domain.c.analysis;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import softtest.domain.c.analysis.DomainVexVisitor;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.DumpGraphVisitor;
import softtest.cfg.c.Graph;
import softtest.config.c.Config;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.MethodNameDeclaration;

public class ControlFlowDomainVisitor extends CParserVisitorAdapter{
	static Logger logger = Logger.getLogger(ControlFlowDomainVisitor.class);
	
	@Override
	public Object visit(ASTFunctionDefinition node, Object data) {
		Graph g = node.getGraph();
		if (g == null) {
			return null;
		}
		
		// �������ժҪ�Ѿ�����������ڼ���ú���
		MethodNameDeclaration methodDecl = node.getDecl();
		if (methodDecl.getMethod() != null && methodDecl.getMethod().getMtSummmary() != null) {
			return null;
		}
		g.clearEdgeContradict();
		g.clearVexNodeContradict();
		
		g.numberOrderVisit(new DomainVexVisitor(), null);
		g.clearVisited();
		
		// ������ļ���������
		dump(g, node.getDecl());
		return null;
	}
	
	private void dump(Graph g, MethodNameDeclaration methodDecl ) {
		if (Config.TRACECFG)
		{
			Config.DUMP_DOMAIN=true;
			Config.DUMP_SYMBOL=true;
			String name = "temp/Domain_" + methodDecl.getImage();
			g.accept(new DumpGraphVisitor(), name + ".dot");
			
			System.out.println("������ͼ(�����������������ļ�" + name + ".dot");
			try {
				java.lang.Runtime.getRuntime().exec(
						"dot -Tjpg -o " + name + ".jpg " + name + ".dot").waitFor();
			} catch (IOException e1) {
				System.out.println(e1);
				logger.error("�밲װ����Graphvix�������г���");
				logger.error(e1);
			} catch (InterruptedException e2) {
				System.out.println(e2);
				logger.error("�밲װ����Graphvix�������г���");
				logger.error(e2);
			}
			
			System.out.println("������ͼ(�����������ӡ�����ļ�" + name + ".jpg");
		}
		if(!Config.SKIP_METHODANALYSIS || Config.TRACE){
			if(Config.MethodAnalysisDomainVisitor || Config.TRACE){
				if (Config.Domain || Config.TRACE)
				{
					Config.DUMP_DOMAIN=true;
					Config.DUMP_SYMBOL=true;
					String name = "temp/Domain_" + methodDecl.getImage();
					g.accept(new DumpGraphVisitor(), name + ".dot");
					//added by liuyan 2015/10/31
					
					if(!Config.SKIP_METHODANALYSIS){
						if(Config.MethodAnalysisDomainVisitor){
							if(Config.Domain){
								logger.info("��7 �����������������ļ�" + name + ".dot");
							}
						}
					}
					
					System.out.println("������ͼ(�����������������ļ�" + name + ".dot");
					try {
						java.lang.Runtime.getRuntime().exec(
								"dot -Tjpg -o " + name + ".jpg " + name + ".dot").waitFor();
					} catch (IOException e1) {
						System.out.println(e1);
						logger.error("�밲װ����Graphvix�������г���");
						logger.error(e1);
					} catch (InterruptedException e2) {
						System.out.println(e2);
						logger.error("�밲װ����Graphvix�������г���");
						logger.error(e2);
					}
					
					if(!Config.SKIP_METHODANALYSIS){
						if(Config.MethodAnalysisDomainVisitor){
							if(Config.Domain){
								logger.info("��7 �����������ӡ�����ļ�" + name + ".jpg");
							}
						}
					}
					
					System.out.println("������ͼ(�����������ӡ�����ļ�" + name + ".jpg");
				}
			}
		}
	}
}
