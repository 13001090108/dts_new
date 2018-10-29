package softtest.domain.c.analysis;

import java.util.*;

import softtest.ast.c.*;
import softtest.cfg.c.*;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.NumberFactor;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.interpro.c.Method;
import softtest.interpro.c.InterContext;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_AbstPointer;
import softtest.symboltable.c.Type.CType_AllocType;
import softtest.symboltable.c.Type.CType_Enum;
import softtest.symboltable.c.Type.CType_Pointer;
import softtest.symboltable.c.Type.CType_Struct;
import softtest.symboltable.c.Type.CType_Union;

/** ������������Ŀ�����ͼ������ */
public class DomainVexVisitor implements GraphVisitor {
	
	//��һ������Ľڵ�ı��
	int lastVexNodeNumber=0;
	
	static int looplayer = 0;
		
	List<String> loopNodeNames = null;
	List<String> loopOutNodeNames = null;
	
	private void calculateIN(VexNode n, Object data) {

		if(n.getName().startsWith("func_head"))
		{			
			looplayer = 0;
			
			loopNodeNames = new ArrayList<String>();
			loopOutNodeNames = new ArrayList<String>();
			
			return;
		}
		if (n.getValueSet() != null) {
			n.getValueSet().clearValueSet();
		}
		
		if (n.getName().startsWith("while_head") || n.getName().startsWith("for_head") || n.getName().startsWith("do_while_out1")) {
			n.setLoopHead(true);
			if(!loopNodeNames.contains(n.getName()))
			{
				looplayer ++;
				loopNodeNames.add(n.getName());
			}
		}else
		{
			n.setLoopHead(false);
		}
		
		if (n.getName().startsWith("while_out") || n.getName().startsWith("for_out") || n.getName().startsWith("do_while_out2")) 
		{
			if(!loopOutNodeNames.contains(n.getName()))
			{
				looplayer --;
				loopOutNodeNames.add(n.getName());
			}
		}

		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e.hasMoreElements();) {
			Edge edge=e.nextElement();
			list.add(edge);
		}
		
		Collections.sort(list);
		
		/**dongyk 20120306 ͳ��forѭ��ִ�еĴ��� */
		int forExecuteTimes=0;
		
		for (Edge edge : list) {
			forExecuteTimes++;
			VexNode pre = edge.getTailNode();
			ValueSet valueset=pre.getValueSet();
			SymbolDomainSet domainset=pre.getSymDomainset();
			
			//������ 20120829 ���ǰ���ǳ�����ֹ�ڵ㣬�򲻴���
			if(pre.getName().startsWith("exit")||pre.getName().startsWith("abort"))
			{
				edge.setContradict(true);
				continue;
			}
						
			// �жϷ�֧�Ƿ�ì��
			if (edge.getContradict()) {
				continue;
			}

			// ���ǰ����û�з��ʹ�������
			if (!pre.getVisited()) {
				continue;
			}
			
			/**dongyk 20120306 n��ѭ��ͷ�ڵ㣬����ѭ������ִ����һ�Σ���������һ��ǰ���ڵ� */
			if(n.isLoopHead()&&n.getLoopExecuteAtleastOnce()&&forExecuteTimes==1&&lastVexNodeNumber>getNodeNum(n)){
				continue;
			}
			/**dongyk 20120306 n��ѭ��ͷ�ڵ㣬����ѭ������ִ����һ�Σ���������n�е�����ֵ */
			
			if(n.isLoopHead()&&n.getLoopExecuteAtleastOnce()&&forExecuteTimes==2&&lastVexNodeNumber>getNodeNum(n)){
				n.setLastsymboldomainset(domainset);
				n.setLastvalueset(valueset);
				n.setSymDomainset(domainset);				
				n.setVarDomainSet(pre.getVarDomainSet());			
			}

			if (edge.getName().startsWith("T")) {
				//zys:�����ѭ��֮������֧
				if (pre.getName().startsWith("while_head") || pre.getName().startsWith("for_head") || pre.getName().startsWith("do_while_out1")) {
					ConditionData condata = pre.getCondata();
						//for(; ;) while(i<1);
						if(condata!=null){
							SymbolDomainSet ds = condata.getTrueMayDomainSet();
							SymbolDomainSet dsTrue=condata.getTrueMustDomainSet();
							
							if(dsTrue.contains(domainset))
							{
								pre.setLoopExecuteAtleastOnce(true);
							}
							
							domainset=SymbolDomainSet.intersect(domainset, ds);							
							if(!domainset.isContradict()){
								domainset=SymbolDomainSet.union(domainset, ds);
							} 
							n.mergeValueSet(valueset,domainset);
						}
				}else{
					ConditionData condata = pre.getCondata();
					if (condata != null) {
						SymbolDomainSet ds = condata.getTrueMayDomainSet();
						domainset=SymbolDomainSet.intersect(domainset, ds);
						if(domainset.isContradict()){
							edge.setContradict(true);
							//zys:2010.4.23
							continue;
						}else{
							n.mergeValueSet(valueset,domainset);
						}
					}else{				
						n.mergeValueSet(valueset,domainset);
					}
				}
			} else if (edge.getName().startsWith("F")) {
				// ����һ��ѭ��
				if (n.getName().startsWith("while_out") || n.getName().startsWith("for_out") || n.getName().startsWith("do_while_out2")) {
					
					if(!Config.LOOPCAL||looplayer > Config.LOOPLAYER)
					{
						visit(pre, data);
						domainset = pre.getSymDomainset();
						valueset=pre.getValueSet();
					}else{
						IterationCalculation iterCal=pre.getIterationCal();
						List<VexNode> loopList=getLoopNodes(pre,n);
						while(iterCal!=null && iterCal.isIterationOver())
						{
							//�������ʱ���䷢���仯���������widening���е���
							for(VexNode vex : loopList)
							{
								//�ڵ����в��ȶ�ʱ��ÿ�������ѭ���ڽڵ�ͱߵ�contradict��ʶ
								vex.setContradict(false);
								for(Edge e : vex.getOutedges().values())
									e.setContradict(false);
								visit(vex,data);
							}
						}
						if(iterCal instanceof WideningCalculation && !iterCal.calculateOver){
							//������������䲻�䣬�����narrowing����
							Domain wideningDomain=iterCal.lastDomain;
							VarDomainSet wideningVar=new VarDomainSet(iterCal.lastVarDomainSet);
							iterCal.clear();
							iterCal=new NarrowingCalculation();
							iterCal.setLastDomain(wideningDomain);
							iterCal.setLastVarDomainSet(wideningVar);
							pre.setIterationCal(iterCal);
							
							while(iterCal.isIterationOver())
							{
								for(VexNode vex : loopList)
								{
									//�ڵ����в��ȶ�ʱ��ÿ�������ѭ���ڽڵ�ͱߵ�contradict��ʶ
									vex.setContradict(false);
									for(Edge e : vex.getOutedges().values())
										e.setContradict(false);
									visit(vex,data);
								}
							}
							iterCal.clear();
						}
					}
				}
				ConditionData condata = pre.getCondata();
				if (condata != null) {
					SymbolDomainSet ds = condata.getFalseMayDomainSet();
					if (pre.getName().startsWith("while_head") || pre.getName().startsWith("for_head") || pre.getName().startsWith("do_while_out1")) {
						if(!Config.LOOPCAL||looplayer > Config.LOOPLAYER){
							SymbolDomainSet old=pre.getSymDomainset();
							pre.setSymDomainset(null);
							ds=condata.getFalseMayDomainSet();
							pre.setSymDomainset(old);
						}else{
							valueset=pre.getValueSet();
							domainset=pre.getSymDomainset();
						}
						domainset=SymbolDomainSet.union(SymbolDomainSet.intersect(domainset, ds), ds);
					} else {
						domainset=SymbolDomainSet.intersect(domainset, ds);
					}
					if(domainset.isContradict()){
						edge.setContradict(true);
						//zys:2010.4.23
						continue;
					}else{
						n.mergeValueSet(valueset,domainset);
					}		
				}else{
					n.mergeValueSet(valueset,domainset);
				}
			}else if (n.getName().startsWith("label_head_default")) {
				//default��֧����Ӧ�û�ȡ����case��֧�ĺϲ����䣬��switch�б�����������ȡ��ֵ
				if(pre.getName().startsWith("switch_head")){
					defaultDomain(pre,n);
					domainset=n.getSymDomainset();
					SymbolDomainSet ds=pre.getSymDomainset();
					domainset=SymbolDomainSet.union(SymbolDomainSet.intersect(domainset, ds), ds);
					n.mergeValueSet(valueset,domainset);
					n.removeUnusedSymbols();
					continue;
				}
			}else{
				n.mergeValueSet(valueset,domainset);
			}
			n.mergeSymbolDomainSet(domainset);
		}

		boolean b = !n.getInedges().isEmpty();
		for(Edge edge:list){
			if (n.getName().startsWith("while_head") || n.getName().startsWith("for_head")||n.getName().startsWith("do_while_head")) {
				//����for��While��� do-while���ֻ����һ�����
				if(edge.getContradict()){
					break;
				}
			}
			if (!edge.getContradict()) {
				b = false;
				break;
			}
		}
		//�ýڵ�ì�ܣ����ɴ�,�������г��ߵ�ì�ܱ�־
		if (b || (n.getSymDomainset() != null && n.getSymDomainset().isContradict())) {
			n.setContradict(true);
			for (Edge edge: n.getOutedges().values()) {
				edge.setContradict(true);
			}
			return;
		}
	}
	
	private void calculateOUT(VexNode n ,Object data){
		//�ں���ͷ��㣬�����еķ�ָ�����Ͳ������������ʼ��
		if(n.getName().startsWith("func_head")){
			ASTFunctionDefinition temp=(ASTFunctionDefinition) n.getTreenode();
			Set<VariableNameDeclaration> paramList=temp.getScope().getVariableDeclarations().keySet();
			for(VariableNameDeclaration param : paramList)
			{
				CType type=param.getType();
				//��ֹ����ָ�����͵Ĳ�����ʼ��������NPD�ȵ�ģʽ��
				if(type == null || type instanceof CType_Union 
						|| type instanceof CType_Struct || type instanceof CType_Enum)
					continue;
				Domain d=null;
				if(Config.USEUNKNOWN){
					d = Domain.getUnknownDomain(Domain.getDomainTypeFromType(type));
				}else {
					if(type instanceof CType_Pointer)
						continue;//����unknownʱ������ʼ��ָ�����͵ı���
					d = Domain.getFullDomainFromType(type);	
				}
				SymbolFactor sym = SymbolFactor.genSymbol(type, param.getImage());
				Expression expr = new Expression(sym);
				n.addSymbolDomain(sym, d);
				n.addValue(param, expr);
			}
			return;
		}
		// �����µ�ֵ
		SimpleNode treenode = n.getTreenode();
		ExpressionVistorData exprdata = new ExpressionVistorData();
		exprdata.currentvex=n;
		exprdata.sideeffect=true;
		ExpressionValueVisitor exprvisitor = new ExpressionValueVisitor();
		ConditionData condata = new ConditionData(n);
		ConditionDomainVisitor convisitor = new ConditionDomainVisitor();
		if (!n.isBackNode()) {
			// ������֧���ܣ�if do while for switch
			if (n.getName().startsWith("if_head_")) {
				treenode.jjtGetChild(0).jjtAccept(exprvisitor, exprdata);
				treenode.jjtGetChild(0).jjtAccept(convisitor, condata);
				n.setCondata(condata);
			} else if (n.getName().startsWith("assert_")) {
				treenode.getFirstChildInstanceofType(ASTConditionalExpression.class).jjtGetChild(0).jjtAccept(exprvisitor, exprdata);
				treenode.getFirstChildInstanceofType(ASTConditionalExpression.class).jjtGetChild(0).jjtAccept(convisitor, condata);
				n.setCondata(condata);
			} else if (n.getName().startsWith("decl_stmt_")
						|| n.getName().startsWith("for_init_")
						|| n.getName().startsWith("for_inc_")
						|| n.getName().startsWith("return_")
						|| n.getName().startsWith("stmt_")) {
				treenode.jjtAccept(exprvisitor, exprdata);
			} else if (n.getName().startsWith("switch_head_")) {
				treenode.jjtGetChild(0).jjtAccept(exprvisitor, exprdata);
			} else if (n.getName().startsWith("label_head_case")) {
				treenode.jjtGetChild(0).jjtAccept(exprvisitor, exprdata);
				caseDomain(n,exprdata,exprvisitor);
			} else if (n.getName().startsWith("switch_out_")){
				//switch_outʱ��ȡ������ǰ���ڵ�Ľ�����кϲ�
				switchOutDomain(n);
			}else if (n.getName().startsWith("for_head_")) {
				/**dongyk 20120306 ���ڵ�����Ϊ��ѭ��ͷ�ڵ��־*/
				Node con=null;
				if(treenode.forChild[1]){
					if(treenode.forChild[0]){
						con=treenode.jjtGetChild(1);
					}else{
						con=treenode.jjtGetChild(0);
					}
				}
				if(con!=null){
					con.jjtAccept(exprvisitor, exprdata);
					iterationCalculate(n,condata,convisitor,con);
				}
			} else if (n.getName().startsWith("do_while_out1_")) {
				/**dongyk 20120306 ���ڵ�����Ϊ��ѭ��ͷ�ڵ��־*/
			//	n.setLoopHead(true);
				treenode.jjtAccept(exprvisitor, exprdata);
				iterationCalculate(n, condata, convisitor, treenode);
			} else if (n.getName().startsWith("while_head_")) {
				/**dongyk 20120306 ���ڵ�����Ϊ��ѭ��ͷ�ڵ��־*/
			//	n.setLoopHead(true);
				treenode.jjtGetChild(0).jjtAccept(exprvisitor, exprdata);
				iterationCalculate(n, condata, convisitor, treenode.jjtGetChild(0));
			} 
		}else if (n.getName().startsWith("func_out")){
			//�������return ��䣬����㺯������ֵ
			MethodNameDeclaration mnd=((ASTFunctionDefinition)treenode).getDecl();
			Method method=mnd.getMethod();
			for(Edge e:n.getInedges().values()){
				if(method==null)
					break;
				if(e.getContradict()){
					continue;
				}
				VexNode v=e.getTailNode();
				if (v.getTreenode() instanceof ASTJumpStatement && v.getTreenode().getImage().equals("return")) {
					if(v.getTreenode().jjtGetNumChildren() == 0)//liuli:����û�з���ֵ��return�����ֱ������
						continue;
					ASTExpression expr=(ASTExpression) v.getTreenode().jjtGetChild(0);
					exprdata.currentvex = v;
					expr.jjtAccept(exprvisitor, exprdata);
					//zys:2010.8.12	���return �����㲻���������պ�������ֵ���ʹ���һ�������͵�ȫֵ����
					if(exprdata.value==null){
						CType type=method.getReturnType();
						Domain d;
						if(Config.USEUNKNOWN)
							d = Domain.getUnknownDomain(Domain.getDomainTypeFromType(type));
						else	
							d = Domain.getFullDomainFromType(type);
						method.addReturnValue(d);
						break;
					}
					//zys:2010.7.21
					Domain d=exprdata.value.getDomain(exprdata.currentvex.getSymDomainset());
//					Domain d=exprdata.value.getDomain(v.getSymDomainset());
					//��������ֵ���� ssj
					if(d == null){
//						CType type=method.getReturnType();		
//            			if(type!=null && type instanceof CType_Pointer){
//            				String str = "NULL";
//            				PointerDomain pdomain = PointerDomain.valueOf(str);
//            				method.addReturnValue(pdomain);
//            			}else{
//            				continue;
//            			}
					}else if(d.getDomaintype() == DomainType.POINTER){
						if(method.getReturnDomain() != null && method.getReturnDomain().getDomaintype() == DomainType.POINTER){
							Domain pdomain = Domain.union(d, method.getReturnDomain(), method.getReturnType());
							method.addReturnValue(pdomain);
						}else if(method.getReturnDomain() == null){
							method.addReturnValue(d);
						}
						PointerDomain pd = (PointerDomain)d;
						HashSet<CType_AllocType> Type = pd.Type;
						if(Type.contains(CType_AllocType.heapType)){
							method.setIsAllocate(true);
						}
					}
					else if(method != null && d != null && d.getDomaintype() != DomainType.POINTER){//liuli:����dΪ�յ�ʱ�򲻼���
						method.addReturnValue(Domain.union(d, method.getReturnDomain(), method.getReturnType()));
					}		
				}
			}
			if(Config.INTER_METHOD_TRACE)
			{
				if (method != null) 
				{
					System.err.println("Method Return Domain : " + method + " = " + method.getReturnDomain());
				}
			}
			if (Config.USE_SUMMARY) 
			{
				//���㺯��ժҪ
				InterContext.cntMethodFeture(n);
			}
			
		}
		n.removeUnusedSymbols();

		if (n.getSymDomainset() != null && n.getSymDomainset().isEmpty()) {
			//n.setSymDomainset(null);
		}
	}

	/** �Խڵ���з��� */
	public void visit(VexNode n, Object data) {
		// ��ǰ���ڵ��domain��ǰ�ڵ�domain
		n.setVisited(true);
		calculateIN(n, data);
		
		//���õ�ǰ�ڵ��lastvardomainset
		if(n.getValueSet()==null){
			n.setLastvalueset(null);
		}else{
			ValueSet vs=new ValueSet(n.getValueSet());
			n.setLastvalueset(vs);
		}
		if(n.getSymDomainset()==null){
			n.setLastsymboldomainset(null);
		}else{
			n.setLastsymboldomainset(new SymbolDomainSet(n.getSymDomainset()));
		}
		
		//��ֹѭ������ʱ������ѭ��
		if(n.getIterationCal()!=null || !n.getContradict()){
			calculateOUT(n,data);
		}
		
		lastVexNodeNumber=getNodeNum(n);		
	}

	/** �Ա߽��з��� */
	public void visit(Edge e, Object data) {

	}

	/** ��ͼ���з��� */
	public void visit(Graph g, Object data) {

	}
	
	private int getNodeNum(VexNode n)
	{
		int index=n.getName().lastIndexOf("_");
		String num=n.getName();
		num=n.getName().substring(index+1);
		return Integer.parseInt(num);
	}

	/** �ж�node�ڵ��Ƿ�Ϊ������������ */
	private boolean isSingleChild(SimpleNode node){
		while(node.jjtGetNumChildren() == 1){
			if((node.jjtGetChild(0) instanceof ASTPrimaryExpression)){
				return true;				
			}
			node = (SimpleNode) node.jjtGetChild(0);
		}
		return false;
	}
	
	public static void iterationCalculate(VexNode n, ConditionData condata, ConditionDomainVisitor convisitor, Node con) {
		if(Config.LOOPCAL && DomainVexVisitor.looplayer <= Config.LOOPLAYER)
		{
			IterationCalculation iterCal=null;
			if(n.getIterationCal()==null)
			{
				iterCal=new WideningCalculation();
				n.setIterationCal(iterCal);
			}else{
				iterCal=n.getIterationCal();
			}
			iterCal.iterationExec(n);
			condata=ConditionData.calLoopCondtion(condata,n,convisitor, con);
			n.setCondata(condata);
			
		}else{
			condata=ConditionData.calLoopCondtion(condata,n,convisitor,con);
			n.setCondata(condata);
		}
	}
	/** ��switch_out�ڵ㣬������ǰ����Case��֧�е�������кϲ�*/
	private void switchOutDomain(VexNode n) {
		ValueSet oldvalueset=n.getValueSet();
		SymbolDomainSet oldsymset=n.getSymDomainset();
		
		List<Edge> list = new ArrayList<Edge>();
		for (Enumeration<Edge> e = n.getInedges().elements(); e.hasMoreElements();) {
			list.add(e.nextElement());
		}
		Collections.sort(list);

		for (Edge edge : list) {
			VexNode pre = edge.getTailNode();
			SymbolDomainSet domainset=pre.getSymDomainset();

			// �жϷ�֧�Ƿ�ì��
			if (edge.getContradict()) {
				continue;
			}
			oldsymset=SymbolDomainSet.intersect(oldsymset, domainset);
			oldsymset=SymbolDomainSet.union(oldsymset, domainset);
			n.mergeValueSet(oldvalueset, oldsymset);
		}
	}
	
	/** �����ж�switch_head�����������Ƿ�Ϊ������
	 * ���������������Ϊ�������򽫵�ǰcase��֧�е���������Ƚϣ����һ�£�������case��֧����Ϊì�ܣ�
	 * �������Ϊ�������򽫵�ǰcase��֧�еı�����������Ϊ��ǰ����*/
	private void caseDomain(VexNode n, ExpressionVistorData exprdata, ExpressionValueVisitor exprvisitor)
	{
		Expression caseValue=exprdata.value;
		//case (0x08 | 0x02 | 0x01):
		//case 0x1;
		//case (0xffffff<<2):
		if(caseValue==null)
			return;
		//zys:2010.8.4	ȡ��switch(n)�еı�����������ֵ��Ϊcase i:��i��ֵ
		Expression switchHeadValue = null;
		Expression exp =null;//switch�б�����case�б�����������ֵ
		VexNode pre=null;
		Edge inEdge=null;
		for(Edge e : n.getInedges().values())
		{
			pre=e.getTailNode();
			if(pre.getName().startsWith("switch_head")){
				inEdge=e;
				break;
			}
		}
		
		if(pre.getTreenode().jjtGetNumChildren()==0||inEdge==null)
		{
			return;
		}
		SimpleNode expnode = (SimpleNode) pre.getTreenode().jjtGetChild(0);
		
		if(!isSingleChild(expnode))
			return;
		
		//1�������ж�switch(n)��nΪ���������
		ASTConstant con = (ASTConstant) (expnode.getSingleChildofType(ASTConstant.class));
		if(con!=null){
			((SimpleNode) pre.getTreenode().jjtGetChild(0)).jjtAccept(exprvisitor, exprdata);						
			switchHeadValue=exprdata.value;
			
			exp =switchHeadValue.sub(caseValue);
			if(exp.getSingleFactor() instanceof NumberFactor){
				NumberFactor f=(NumberFactor)exp.getSingleFactor();
				if(f.getDoubleValue()==0){
					//liuli��2010.5.7�����ǰ��ȷ��Ϊ���֧�����丸�ڵ���������߾���Ϊ���ɴ�
					for (Edge edge1: pre.getOutedges().values()) {
						if(!edge1.toString().equals(inEdge.toString()))
							edge1.setContradict(true);
					}
				}else{
					inEdge.setContradict(true);
				}
			}
		}else{
		//2�����switch(n)��Ϊ����
			ASTPrimaryExpression name = (ASTPrimaryExpression) (expnode.getSingleChildofType(ASTPrimaryExpression.class));
			VariableNameDeclaration v=null;
			if(!name.isMethod()){ //switch����Ϊ���������
				v=name.getVariableDecl(); 
				if(v == null && name.jjtGetNumChildren()>0){							
					SimpleNode cast = (SimpleNode)name.jjtGetChild(0);
					name  = (ASTPrimaryExpression)cast.getSingleOrCastChildofType(ASTPrimaryExpression.class);
					if(name==null || name.getVariableDecl()==null)
						return;
					v=name.getVariableDecl(); 
				}
				switchHeadValue=pre.getValue(v);
				if(switchHeadValue==null)
					return;
			}else{
				//Ŀǰ����ʶ��Ϊ�˺�����switch ((*__errno_location ()))
				return;
			}
			exp =switchHeadValue.sub(caseValue);
			if(exp.getSingleFactor() instanceof NumberFactor){
				NumberFactor f=(NumberFactor)exp.getSingleFactor();
				if(f.getDoubleValue()==0){
					//liuli��2010.5.7�����ǰ��ȷ��Ϊ���֧�����丸�ڵ���������߾���Ϊ���ɴ�
					for (Edge edge1: pre.getOutedges().values()) {
						if(!edge1.toString().equals(inEdge.toString()))
							edge1.setContradict(true);
					}
				}else{
					inEdge.setContradict(true);
				}
			}else{
				Domain d=caseValue.getDomain(n.getSymDomainset());
				if(d==null || Domain.isEmpty(d) || d.isUnknown()){
					//zys 2011.6.25	case�е������㲻��������case X: XΪһ��ö�ٳ���ֵ
					return;
				}
				if(switchHeadValue.isComplicated()){
					//zys 2011.6.21: ���switch(n)�б����ķ��ű��ʽ�Ƚϸ��ӣ���ֱ��Ϊ�������µķ��ű��ʽ
					SymbolFactor s=SymbolFactor.genSymbol(v.getType(), v.getImage());
					Expression expr=new Expression(s);
					n.addValue(v, expr);
					n.addSymbolDomain(s, d);
				}else{
					//ZYS 2011.6.21	Ŀǰֻ�ܴ���x=Y+n���ּ򵥵ķ��ű��ʽ����Ȼ������Ҳ���Դ���x=m*Y+n�������ͣ�����ʱδ������
					SymbolFactor s=null;
					Domain caseDomain=null;
					if(exp.getTerms().size()==1){
						s=(SymbolFactor) exp.getSingleFactor();
						caseDomain=Domain.castToType(new IntegerDomain(0,0), v.getType());
					}else if(exp.getTerms().size()==2){
						s=(SymbolFactor) exp.getTerms().get(1).getSingleFactor();
						if(s==null)//zys 2011.6.25	case�е������㲻��������case X: XΪһ��ö�ٳ���ֵ
							return;
						Expression temp=new Expression(s).sub(exp);
						caseDomain=temp.getDomain(n.getSymDomainset());
					}
					n.addSymbolDomain(s, caseDomain);
				}
			}
		}
	}
	
	/**����ȡ�����е�case��֧���������������䣬����������кϲ�Ȼ��ȡ����������switch_headͷ����еı�������ȡ���� */
	private void defaultDomain(VexNode pre,VexNode n){
		ExpressionVistorData exprdata = new ExpressionVistorData();
		exprdata.currentvex=n;
		exprdata.sideeffect=true;
		ExpressionValueVisitor exprvisitor = new ExpressionValueVisitor();
		Domain caseDomainSet = null,switchHeadDomain = null;
		SimpleNode expnode = (SimpleNode) pre.getTreenode().jjtGetChild(0);
		ASTPrimaryExpression name = (ASTPrimaryExpression) (expnode.getSingleChildofType(ASTPrimaryExpression.class));
		if (name == null || name.isMethod())
			return;
		VariableNameDeclaration v=name.getVariableDecl();
		if(v == null && name.jjtGetNumChildren()>0){							
			SimpleNode cast = (SimpleNode)name.jjtGetChild(0);
			name  = (ASTPrimaryExpression)cast.getSingleOrCastChildofType(ASTPrimaryExpression.class);
			if(name==null || name.getVariableDecl()==null)
				return;
			v=name.getVariableDecl(); 
		}
		
		//�ȼ��������case�������䣬��ȡ��
		for (Edge edge : pre.getOutedges().values()) {
			VexNode head = edge.getHeadNode();
			if(head.getName().startsWith("label_head_case_")){
				((SimpleNode) head.getTreenode().jjtGetChild(0)).jjtAccept(exprvisitor, exprdata);
				Expression e = exprdata.value;
				if(e==null)
					continue;
				Domain dm = e.getDomain(head.getSymDomainset());
				if(dm != null){
					caseDomainSet = Domain.union(caseDomainSet, dm, v.getType());	
				}  							
			}
		}
		caseDomainSet = Domain.inverse(caseDomainSet);
		
		//Ȼ������switchͷ�����������䣬����������ȡ��
		SymbolFactor s=SymbolFactor.genSymbol(v.getType(),v.getImage());
		Expression exp1=new Expression(s);
		n.addValue(v, exp1);
		switchHeadDomain=pre.getDomain(v);
		if(switchHeadDomain == null){//���switchͷ����������������δ֪
			switchHeadDomain=s.getDomainWithoutNull(n.getSymDomainset());			
			switchHeadDomain = Domain.intersect(switchHeadDomain, caseDomainSet, v.getType());
		}else{
			switchHeadDomain = Domain.intersect(switchHeadDomain, caseDomainSet, v.getType());
		}
		n.addSymbolDomain(s, switchHeadDomain); 
	}
	
	/** ��ȡ������ͼ��ÿ��ѭ����������ȫ�����
	 * ������Ƕ��ѭ��ʱ����ȷ��ȡÿ��ѭ�������������н��*/
	private List<VexNode> getLoopNodes(VexNode loopHead,VexNode loopTail)
	{
		List<VexNode> list=new ArrayList<VexNode>();
		
		Graph g=loopHead.getGraph();
		ArrayList<VexNode> listnodes=new ArrayList<VexNode>();
		listnodes.addAll(g.nodes.values());
		Collections.sort(listnodes);
		boolean b=false;
		if(loopHead.getName().startsWith("do_while_out1"))
		{//��do-whileѭ�����ԣ�����do_while_out1�ڵ�ΪloopHead�ڵ㣬do_while_out2�ڵ�ΪloopTail�ڵ�
			for(VexNode v : listnodes)
			{
				if(b==true){
					if(!v.equals(loopTail)){
						list.add(v);
					}else{
						break;
					}
				}else{
					//ȡdo_while_out1�ڵ�����֧����Ϊ��do_while_head�ڵ�
					Edge trueEdge=null;
					for(Edge vex : loopHead.getOutedges().values()){
						if(vex.getName().startsWith("T_")){
							trueEdge=vex;
							break;
						}
					}
					VexNode temp=trueEdge.getHeadNode();
					if(v.equals(temp))
					{
						list.add(v);
						b=true;
					}else{
						continue;
					}
				}
			}
		}else{
			for(VexNode v : listnodes)
			{
				if(b==true){
					if(!v.equals(loopTail) && !v.equals(loopTail)){
						list.add(v);
					}else{
						break;
					}
				}else{
					if(v.equals(loopHead))
					{
						list.add(v);
						b=true;
					}else{
						continue;
					}
				}
			}
		}
		return list;
	}
}
