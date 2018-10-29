package softtest.domain.c.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import softtest.cfg.c.Edge;
import softtest.cfg.c.VexNode;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType_BaseType;

public class WideningCalculation extends IterationCalculation {
	private boolean firstCompute=true;
	
	public boolean isFirstCompute() {
		return firstCompute;
	}

	public void setFirstCompute(boolean firstCompute) {
		this.firstCompute = firstCompute;
	}

	@Override
	public void iterationExec(VexNode iterationHead) {
		counter++;
		String nodeName=iterationHead.getName();
		
		List<Edge> list = new ArrayList<Edge>();
		for (Edge e:iterationHead.getInedges().values()) {
			VexNode v=e.getTailNode();
			String name=v.getName();
			if(name.startsWith("continue"))
				continue;
			list.add(e);
		}
		Collections.sort(list);
		
		if(nodeName.startsWith("while_head") || nodeName.startsWith("for_head"))
		{
			VexNode preIterationNode=null;
			VexNode postIterationNode=null;
			if(list.size()==2){
				/**dongyk 20120306 ���ѭ������ִ��һ�Σ�������ѭ��ִ��ǰ����Ӧ�Ľ��*/
				if(iterationHead.getLoopExecuteAtleastOnce())
				{
					preIterationNode=iterationHead;
				}else{
					preIterationNode=list.get(0).getTailNode();
				}
				postIterationNode=list.get(1).getTailNode();
			}else if(list.size()==1){//������ת��û�е�ѭ��ͷ���Ļرߣ����ع��������112
				calculateOver=true;
				domainChanged=false;
				return;
			}
			
			if(postIterationNode.getContradict() || preIterationNode.getContradict()){
				calculateOver=true;
				domainChanged=false;
				return;
			}
			List<VariableNameDeclaration> varList=getLeftVar(iterationHead);//��ȡѭ���е�������������
			if(varList.size()==0){//���û���ҵ�������������ֹͣ����������Ƚϸ��ӵ�ѭ������
				calculateOver=true;
				domainChanged=false;
				return;
			}
				
			VariableNameDeclaration v=varList.get(0);//��ʱֻ���������������������while(i<100)
			Expression valueExpr=null;
			if(isFirstCompute())
			{
				if(v!=null)
				{
					valueExpr=preIterationNode.getValueSet().getValue(v);
					if(valueExpr==null)
					{
						// ���ѭ���������Ժ������βλ�ȫ�ֱ�������ѭ�������ĳ�ʼ����δ֪����ʱ������
						lastVarDomainSet=new VarDomainSet();
					}else{
						Domain domain=valueExpr.getDomain(preIterationNode.getSymDomainset());
						SymbolFactor s=SymbolFactor.genSymbol(v.getType(), v.getImage());
						valueExpr=new Expression(s);
						iterationHead.getValueSet().addValue(v, valueExpr);
						iterationHead.addSymbolDomain(s, domain);
						lastDomain=domain;
						lastVarDomainSet=new VarDomainSet(iterationHead.getVarDomainSet());
					}
				}
				setFirstCompute(false);
			}else if(isIterationOver()){
				Expression postExpr=postIterationNode.getValueSet().getValue(v);
				if(postExpr==null){
					calculateOver=true;
					domainChanged=false;
					return;
				}
				Domain postDomain=postExpr.getDomain(SymbolDomainSet.intersect(
						iterationHead.getCondata().getTrueMayDomainSet(),iterationHead.getSymDomainset()));
				Expression preExpr=preIterationNode.getValueSet().getValue(v);
				Domain preDomain=null;
				if(preExpr!=null)
					preDomain=preExpr.getDomain(iterationHead.getSymDomainset());
				
				Domain domain=Domain.union(postDomain, preDomain, v.getType());
				//��widening֮ǰ�ȵ����������Ϊһ��
				if(domain!=null && lastDomain!=null && domain.getDomaintype()!=lastDomain.getDomaintype())
					domain=Domain.castToType(domain, CType_BaseType.getBaseType(lastDomain.getDomaintype().name()));
				domain=Domain.wideningDomain(lastDomain, domain);
				/**dongyk 20120306 ���ѭ������ִ��һ�Σ�lastVarDomainSet���ǵ����ڵ��VarDomainSet*/
				if(iterationHead.getLoopExecuteAtleastOnce()&&counter<3)
				{
					lastVarDomainSet=new VarDomainSet(iterationHead.getVarDomainSet());
				}
				lastVarDomainSet=VarDomainSet.widening(lastVarDomainSet, iterationHead.getVarDomainSet(),iterationHead);
				
				SymbolFactor s=SymbolFactor.genSymbol(v.getType(), v.getImage());
				valueExpr=new Expression(s);
				iterationHead.getValueSet().addValue(v, valueExpr);
				iterationHead.addSymbolDomain(s, domain);
				
				if(lastDomain!=null)
				{
					domainChanged=isDomainChanged(lastDomain,domain);
				}
				lastDomain=domain;
			}
		}else if(nodeName.startsWith("do_while_out1")){
			//do-whileѭ����ͷ���ֻ��һ�����
			VexNode postIterationNode=null;
			if(list.size()==1){
				postIterationNode=list.get(0).getTailNode();
			}else{
				calculateOver=true;
				domainChanged=false;
				return;
			}
			if(postIterationNode.getContradict()){
				calculateOver=true;
				domainChanged=false;
				return;
			}
			List<VariableNameDeclaration> varList=getLeftVar(iterationHead);//��ȡѭ���е�������������
			if(varList.size()==0){//���û���ҵ�������������ֹͣ����������Ƚϸ��ӵ�ѭ������
				calculateOver=true;
				domainChanged=false;
				return;
			}
			
			VariableNameDeclaration v=varList.get(0);//��ʱֻ���������������������while(i<100)
			Expression valueExpr=null;
			if(isIterationOver()){
				Expression postExpr=postIterationNode.getValueSet().getValue(v);
				if(postExpr==null){
					calculateOver=true;
					domainChanged=false;
					return;
				}
				Domain domain=postExpr.getDomain(iterationHead.getSymDomainset());
				domain=Domain.castToType(domain, v.getType());
				if(isFirstCompute()){
					SymbolFactor s=SymbolFactor.genSymbol(v.getType(), v.getImage());
					valueExpr=new Expression(s);
					iterationHead.getValueSet().addValue(v, valueExpr);
					iterationHead.addSymbolDomain(s, domain);
					lastDomain=domain;
					lastVarDomainSet=new VarDomainSet(iterationHead.getVarDomainSet());
					setFirstCompute(false);
				}else{
					domain=Domain.wideningDomain(lastDomain, domain);
					lastVarDomainSet=VarDomainSet.widening(lastVarDomainSet, iterationHead.getVarDomainSet(),iterationHead);

					SymbolFactor s=SymbolFactor.genSymbol(v.getType(), v.getImage());
					valueExpr=new Expression(s);
					iterationHead.getValueSet().addValue(v, valueExpr);
					iterationHead.addSymbolDomain(s, domain);
					
					if(lastDomain!=null)
					{
						domainChanged=isDomainChanged(lastDomain,domain);
					}
					lastDomain=domain;
				}
			}
		}
	}
}
