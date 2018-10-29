package softtest.summary.c;

import java.util.Hashtable;

import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Variable;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;

/**
 * 
 * @author ����
 */
public class MethodPostCondtionVisitor implements MethodFeatureVisitor {

	private static MethodPostCondtionVisitor instance;
	
	private MethodPostCondtionVisitor() {	
	}
	
	public static MethodPostCondtionVisitor getInstance() {
		if (instance == null) {
			instance = new MethodPostCondtionVisitor(); 
		}
		return instance;
	}
	
	/**
	 * zys:�ں������ڴ�����¼���иú������ⲿ�������޸�:
	 * 		Ŀǰ������ȫ�ֱ�������ָ�롢������ʽ�Ĳ�����ʱ������
	 */
	public void visit(VexNode vexNode) {
		MethodPostCondition feature = new MethodPostCondition();
		Hashtable<VariableNameDeclaration, Domain> table=vexNode.getVarDomainSet().getTable();
		for(VariableNameDeclaration var:table.keySet())
		{
			if(var.getScope() instanceof SourceFileScope)
			{
				Variable variable = Variable.getVariable(var);
				if (variable != null) {
					Domain d=table.get(var);
					//zys 2011.6.24	ֻ�Է���ȷ���Ըı��ȫ�ֱ������ɺ���ժҪ
					if(d!=null && !d.isUnknown() && !Domain.isEmpty(d))
						feature.addDomain(variable, table.get(var));
				}
			}
		}
		
		// ��������ĺ���������ӵ�����ժҪ��
		MethodSummary summary = InterContext.getMethodSummary(vexNode);
		if (summary != null && !feature.isEmpty()) {
			summary.addPostCondition(feature);
			if (Config.INTER_METHOD_TRACE) {
				MethodNameDeclaration methodDecl = InterContext.getMethodDecl(vexNode);
				if (methodDecl != null) {
					System.err.println(methodDecl.getFullName() + " " + feature);
				}
			}
		}
	}
}
