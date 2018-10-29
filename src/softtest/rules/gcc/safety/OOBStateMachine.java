package softtest.rules.gcc.safety;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import softtest.ast.c.*;
import softtest.rules.c.BasicStateMachine;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.*;
import softtest.symboltable.c.Type.*;
import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.ExpressionValueVisitor;
import softtest.domain.c.analysis.ExpressionVistorData;
import softtest.domain.c.interval.*;
import softtest.domain.c.symbolic.Expression;
import softtest.fsm.c.*;
/**
 * 
 *   modified by chh
 *
 */
public class OOBStateMachine extends BasicStateMachine{
	private static int OOBNO = 0;
	// ÿ���������һ��״̬��
	public static List<FSMMachineInstance> createOOBStateMachines(SimpleNode node, FSMMachine fsm) {
	
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		// ��������ʹ�õ�ʱ�򴴽�״̬�� a[1] = 1;
		String xpath = ".//UnaryExpression/PostfixExpression[contains(@Operators,'[')]" ;
			
		List evaluationResults = node.findXpath(xpath);
		
		Set<VariableNameDeclaration> varset = new HashSet<VariableNameDeclaration>();
		
out:	for (Object snode : evaluationResults) {
			ASTPostfixExpression post = (ASTPostfixExpression) snode;
			ASTPrimaryExpression primary=(ASTPrimaryExpression)post.jjtGetChild(0);
			VariableNameDeclaration curvar;
			if(primary.getFirstParentOfType(ASTLogicalANDExpression.class)!=null)
				continue;
			//chh  ָ�����÷�ʽ���������÷�ʽ�ֱ���   ���磺((char *)buf)[2]=3;
			if(primary.getVariableDecl()!=null){
				curvar=primary.getVariableDecl();
			}else {
				primary=(ASTPrimaryExpression)primary.getFirstChildOfType(ASTPrimaryExpression.class);
				if(primary==null)//typedef char cc;i=sizeof(cc[5]);
					continue;
				curvar=primary.getVariableDecl();
			}
			//end
			ArrayList<Boolean> flags=post.getFlags();
			ArrayList<String> operators=post.getOperatorType();
			int j=1;
			for(int i=0;i<flags.size();i++){
				boolean flag=flags.get(i);
				String operator=operators.get(i);
				if(operator.equals("[")){
					if(flag){
						j++;
					}else{
						throw new RuntimeException("ASTPostfixExpression error!");
					}
					if(curvar!=null&&!varset.contains(curvar)&&!curvar.isParam()){
						FSMMachineInstance fsminstance = fsm.creatInstance();
						fsminstance.setRelatedVariable(curvar);
						fsminstance.setRelatedASTNode((SimpleNode)snode);
						varset.add(curvar);
						list.add(fsminstance);
						OOBNO++;
					}
				}else if(operator.equals("(")){
					continue out;
				}else if(operator.equals(".")){
					ASTFieldId field=null;
					if(flag){
						field=(ASTFieldId)post.jjtGetChild(j++);
					}else{
						throw new RuntimeException("ASTPostfixExpression error!");
					}
					curvar=field.getVariableNameDeclaration();
				}else if(operator.equals("->")){
					ASTFieldId field=null;
					if(flag){
						field=(ASTFieldId)post.jjtGetChild(j++);
					}else{
						throw new RuntimeException("ASTPostfixExpression error!");
					}
					curvar=field.getVariableNameDeclaration();
				}else{
					//do nothing
				}
			}
		}
		//chh  char c = *(buf+4);Ϊ�˼�������ϣ������������鴴���Զ���
		xpath=".//AssignmentExpression/UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression/PostfixExpression/PrimaryExpression/Expression/AssignmentExpression/AdditiveExpression";
		evaluationResults = node.findXpath(xpath);
		for (Object snode : evaluationResults) {
			ASTPrimaryExpression primary=(ASTPrimaryExpression) ((ASTAdditiveExpression) snode).getFirstChildInstanceofType(ASTPrimaryExpression.class);
			VariableNameDeclaration curvar = primary.getVariableNameDeclaration();
			if(primary.getFirstParentOfType(ASTLogicalANDExpression.class)!=null)
				continue;
			if(curvar!=null&&curvar.getType()!=null&&(curvar.getType() instanceof CType_Array||curvar.getType().isPointType())
					&&!curvar.isParam()) 
			{
					FSMMachineInstance fsminstance = fsm.creatInstance();
					fsminstance.setRelatedVariable(curvar);
					fsminstance.setRelatedASTNode((SimpleNode)snode);
					list.add(fsminstance);
					OOBNO++;
				
			}
			else continue;
		}//end
		xpath=".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression[contains(@Operators,'++')]/UnaryExpression/PostfixExpression/PrimaryExpression|" +
				".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression/PostfixExpression[contains(@Operators,'++')]/PrimaryExpression|" +
				".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression[contains(@Operators,'--')]/UnaryExpression/PostfixExpression/PrimaryExpression|" +
				".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression/PostfixExpression[contains(@Operators,'--')]/PrimaryExpression";
		evaluationResults = node.findXpath(xpath);
		for (Object snode : evaluationResults) {
			ASTPrimaryExpression primary=(ASTPrimaryExpression) snode;
			if(primary.getFirstParentOfType(ASTLogicalANDExpression.class)!=null)
				continue;
			VariableNameDeclaration curvar = primary.getVariableNameDeclaration();
			if(curvar!=null&&curvar.getType()!=null&&curvar.getType().isPointType()&&!curvar.isParam()) 
			{
					FSMMachineInstance fsminstance = fsm.creatInstance();
					fsminstance.setRelatedVariable(curvar);
					fsminstance.setRelatedASTNode((SimpleNode)snode);
					list.add(fsminstance);
					OOBNO++;
				
			}
			else continue;
		}
		return list;
	}
	/*
	 * // chh   ��char c = *(buf+4)��bufΪ4��Ԫ�ص����顱������ϵ������
	 **/
	public static boolean checkSameVariableAndOOB0(VariableNameDeclaration variable,FSMMachineInstance fsmin){
		
		ASTAdditiveExpression addexp=(ASTAdditiveExpression)fsmin.getRelatedASTNode();
		VariableNameDeclaration curvar=variable;
		IntegerInterval size=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
		IntegerInterval usesize=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
		if(curvar.getType().isArrayType())	{	
			CType curtype=curvar.getType();
			CType type=curtype.getSimpleType();
			CType_Array atype=(CType_Array)type;
			size=new IntegerInterval(0,atype.getDimSize()-1);
			if(size.getMax()==0) return false;
		}
		else if(!curvar.getType().isArrayType()&&curvar.getType().isPointType()){
			ASTPrimaryExpression primary=(ASTPrimaryExpression) ((ASTAdditiveExpression) addexp).getFirstChildInstanceofType(ASTPrimaryExpression.class);
			size=OOBStateMachine.getrange(primary, curvar);
			if(size.getMax()==Long.MIN_VALUE&&size.getMin()==Long.MIN_VALUE) return false;
		}
		for(int k=1;k<addexp.jjtGetNumChildren();k++)
		{
			if(addexp.jjtGetChild(k) instanceof ASTUnaryExpression)
			{
				ASTPrimaryExpression child=(ASTPrimaryExpression) ((SimpleNode) addexp.jjtGetChild(k)).getFirstChildOfType(ASTPrimaryExpression.class);
				ExpressionValueVisitor expvst = new ExpressionValueVisitor();
				ExpressionVistorData visitdata = new ExpressionVistorData();
				visitdata.currentvex = child.getCurrentVexNode();
				visitdata.currentvex.setfsmCompute(true);
				expvst.visit(child, visitdata);
				visitdata.currentvex.setfsmCompute(false);
				Expression value1 = visitdata.value;
				Domain mydomain=null;
				if(value1!=null)
				mydomain = value1.getDomain(visitdata.currentvex.getLastsymboldomainset());
				
				/* �ⲿ��û��������ʲô��˼  nmh */
				if((mydomain instanceof IntegerDomain)&&((IntegerDomain)mydomain).getMin()==((IntegerDomain)mydomain).getMax()){
					usesize.setMin(usesize.getMin()+((IntegerDomain)mydomain).getMin());
					usesize.setMax(usesize.getMax()+((IntegerDomain)mydomain).getMax());
				}
				else {
					return false;
				}	
				/*
				//��ʱ��ô�޸�
				if(mydomain!=null){
					usesize = ((IntegerDomain)mydomain).jointoOneInterval();
				}
				*/
			}
			else{ 
				return false;
			}
		}
		/*
		if(!(size.getMax()==Long.MAX_VALUE&&size.getMin()==Long.MIN_VALUE)&&
				(usesize.getMax()>size.getMax()||usesize.getMin()<size.getMin())
				&&!curvar.isParam()){
		*/
		//modified by nmh
		if(confirmFuncOOB(usesize,size)&&!curvar.isParam()){
			fsmin.setDesp("�ڵ� " + curvar.getNode().getBeginLine() + " �ж���Ĵ�СΪ " + (size.getMax()-size.getMin()+1) + " ��ָ�������\""+curvar.getImage()+"\"�����ڵ�"+addexp.getBeginLine()+"��Խ�� ");
			fsmin.setNodeUseToFindPosition(addexp);
			return true;
		}
		else return false;
	
	}
	
	/*
	 * // nmh
	 * ����д�˺���checkSameVariableAndOOB0
	 * ����  ��char c = *(buf+4)��bufΪ4��Ԫ�ص����顱������ϵ������
	 **/
public static boolean checkExpOOB(VariableNameDeclaration variable,FSMMachineInstance fsmin){
		
		ASTAdditiveExpression addexp=(ASTAdditiveExpression)fsmin.getRelatedASTNode();
		VariableNameDeclaration curvar=variable;
		int index = -1;
		VariableNameDeclaration varDecl = null;
		IntegerInterval size=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
		IntegerInterval retInterval = new IntegerInterval(0, 0); //��ʼ��retInterval������
		int childrenNum = addexp.jjtGetNumChildren();		
		for (int i = 0; i < childrenNum; ++i) {
			SimpleNode pmNode = (SimpleNode) addexp.jjtGetChild(i);
			String xPath = "./PostfixExpression[count(*)=1]/PrimaryExpression"; //�ų�int ns[]; ns[0] += ns[0] + 8;
			List<SimpleNode > primExpNodeList = StateMachineUtils.getEvaluationResults(pmNode, xPath);
			if (primExpNodeList == null || primExpNodeList.size() == 0) {
				continue;
			}
			
			ASTPrimaryExpression primExpNode = (ASTPrimaryExpression) primExpNodeList.get(0);
			NameDeclaration nameDecl = primExpNode.getVariableDecl();
			if (!(nameDecl instanceof VariableNameDeclaration)) {
				continue;
			}
			
			varDecl = (VariableNameDeclaration) nameDecl;
			if (varDecl.getType() instanceof CType_Pointer || varDecl.getType() instanceof CType_Array) {
				index = i;
				break;
			}
		}
		if( index == -1 )
			return false;
		if(curvar.getType().isArrayType())	{	
			CType curtype=curvar.getType();
			CType type=curtype.getSimpleType();
			CType_Array atype=(CType_Array)type;
			size=new IntegerInterval(0,atype.getDimSize()-1);
			if(size.getMax()==0) return false;
		}
		else if(!curvar.getType().isArrayType()&&curvar.getType().isPointType()){
			ASTPrimaryExpression primary=(ASTPrimaryExpression) ((ASTAdditiveExpression) addexp).getFirstChildInstanceofType(ASTPrimaryExpression.class);
			size=OOBStateMachine.getrange(primary, curvar);
			if(size.getMax()==Long.MIN_VALUE&&size.getMin()==Long.MIN_VALUE) return false;
		}
		
		ExpressionValueVisitor expVisitor = new ExpressionValueVisitor();			
		String operator = null;
		ArrayList<String>opes = addexp.getOperatorType();
		if(opes.size() != childrenNum - 1 )
			return false;
		for (int m = 0, i = 0; m < childrenNum; ++m) {
			if (m == index) {
				continue;
			}
			operator = opes.get(i);
			SimpleNode pmNode = (SimpleNode) addexp.jjtGetChild(m);
			ExpressionVistorData domainData = new ExpressionVistorData();
			domainData.currentvex = pmNode.getCurrentVexNode();
			pmNode.jjtAccept(expVisitor, domainData);
			Expression value1 = domainData.value;
			Domain mydomain=null;
			if(value1!=null)
				mydomain = value1.getDomain(domainData.currentvex.getSymDomainset());

			if (domainData != null && mydomain != null && !mydomain.isUnknown() && mydomain instanceof IntegerDomain) {
				IntegerDomain iDomain = (IntegerDomain) mydomain;
				if(iDomain.isUnknown())
					return false;
				IntegerInterval tmpInter = iDomain.jointoOneInterval();

				if (operator.equals("+")) {
					retInterval = IntegerInterval.add(retInterval, tmpInter);
				} else if (operator.equals("-")) {
					retInterval = IntegerInterval.sub(retInterval, tmpInter);
				}
			}
		}

		if(confirmFuncOOB(retInterval,size)&&!curvar.isParam()){
			fsmin.setDesp("�ڵ� " + curvar.getNode().getBeginLine() + " �ж���Ĵ�СΪ " + (size.getMax()-size.getMin()+1) + " ��ָ�������\""+curvar.getImage()+"\"�����ڵ�"+addexp.getBeginLine()+"��Խ�� ");
			fsmin.setNodeUseToFindPosition(addexp);
			return true;
		}
		else return false;
	
	}

	public static boolean checkSameVariableAndOOB(List nodes, FSMMachineInstance fsmin) {
		
		Iterator itor = nodes.iterator();
out:	while (itor.hasNext()) {
		SimpleNode snode=(SimpleNode)itor.next();
			//chh   ���.//AssignmentExpression/UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression/PostfixExpression/PrimaryExpression/Expression/AssignmentExpression/AdditiveExpression��
			if(snode instanceof ASTAdditiveExpression && fsmin.getRelatedASTNode() instanceof ASTAdditiveExpression
					&&snode==fsmin.getRelatedASTNode()){
				if(checkExpOOB(fsmin.getRelatedVariable(),fsmin)&&!fsmin.getRelatedVariable().isParam())
					return true;
			}
			/*chh ���".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression[contains(@Operators,'++')]/UnaryExpression/PostfixExpression/PrimaryExpression|" +
			".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression/PostfixExpression[contains(@Operators,'++')]/PrimaryExpression|" +
			".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression[contains(@Operators,'--')]/UnaryExpression/PostfixExpression/PrimaryExpression|" +
			".//UnaryExpression[/UnaryOperator[1][contains(@Operators,'*')]]/UnaryExpression/PostfixExpression[contains(@Operators,'--')]/PrimaryExpression";*/
			else if(snode instanceof ASTPrimaryExpression&&!fsmin.getRelatedVariable().isParam()
					&&snode==fsmin.getRelatedASTNode()){
				IntegerInterval range=getrange((ASTPrimaryExpression) snode,fsmin.getRelatedVariable());
				if(range.getMax()<0&&!(range.getMax()==Long.MAX_VALUE && range.getMin()==Long.MIN_VALUE)) {
					fsmin.setDesp("�ڵ� " + fsmin.getRelatedVariable().getNode().getBeginLine() + " �ж����ָ��\""+fsmin.getRelatedVariable().getImage()+"\"�����ڵ�"+snode.getBeginLine()+"��Խ�磬Ŀǰƫ�����䣺 "+range);
					fsmin.setNodeUseToFindPosition(snode);
					return true;
				}
			}
			//chh   ���.//UnaryExpression/PostfixExpression[contains(@Operators,'[')]�ҵ��Ľڵ㣬���  ������[��������ʹ�ó��֡�
			else if(snode instanceof ASTPostfixExpression&&(fsmin.getRelatedASTNode()) instanceof ASTPostfixExpression
					)
			{
			
			ASTPostfixExpression post = (ASTPostfixExpression) snode;
			ASTPrimaryExpression primary=(ASTPrimaryExpression)post.jjtGetChild(0);
			VariableNameDeclaration curvar;
			//chh  ָ�����÷�ʽ((char *)array)[]/(pointer+?)[]���������÷�ʽarray[]�ֱ���
			if(primary.getVariableDecl()!=null)
				curvar=primary.getVariableDecl();
			else{
				primary=(ASTPrimaryExpression)primary.getFirstChildOfType(ASTPrimaryExpression.class);
				if(primary==null)
					continue;
				curvar=primary.getVariableDecl();
			}
			//end
			if(curvar==null||curvar.getType()==null){
				continue;
			}
			CType curtype=curvar.getType();
			
			/**begin:modified by nmh.����Ƿ��������ת��������char buf[] */
			
			boolean isTypeCast = false;
			CType expType = null;
			ASTCastExpression castExpNode = (ASTCastExpression) post.getFirstChildOfType(ASTCastExpression.class);
			if (castExpNode != null) {
				isTypeCast = true;
				expType = castExpNode.getType();
			}
			
			/**end:modified by nmh*/
			
			ArrayList<Boolean> flags=post.getFlags();
			ArrayList<String> operators=post.getOperatorType();
			int j=1;
			for(int i=0;i<flags.size();i++){
				boolean flag=flags.get(i);
				String operator=operators.get(i);
				if(operator.equals("[")){
					ASTExpression expression =null;
					CType type=curtype.getSimpleType();
					if(flag){
						expression=(ASTExpression)post.jjtGetChild(j++);
						if(curvar==fsmin.getRelatedVariable()){
							//���Խ��
							VexNode vex=post.getCurrentVexNode();

							if(curtype!=null&&(curtype instanceof CType_Array||curtype.isPointType())){
								CType_Array atype=null;
								IntegerInterval size=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
								// ָ�����÷�ʽpointer[]���������÷�ʽarray[]�ֱ���
								if(curtype instanceof CType_Array)
								{
									atype=(CType_Array)type;
									if(atype.getDimSize()!=-1&&atype.getDimSize()!=0)
										size=new IntegerInterval(0,atype.getDimSize()-1);
									else 
										size=new IntegerInterval(0,0);
									if(size.getMax()==0)
										return false;
								}
								else if(!(curtype instanceof CType_Array)&&curtype.isPointType())
								{
									size=OOBStateMachine.getrange(primary, curvar);
									if(size.getMin()==Long.MIN_VALUE&&size.getMax()==Long.MAX_VALUE||
											(size.getMax()==0&&size.getMin()==0))
										return false;
								}
								// (buf+i-j)[3]='c';//�ݲ���������i��jΪ������ȫ�ֱ��������
								if(primary.getFirstParentOfType(ASTAdditiveExpression.class)!=null
										&&primary.getFirstParentOfType(ASTPrimaryExpression.class)!=null
										&&((SimpleNode)((ASTPrimaryExpression)primary.getFirstParentOfType(ASTPrimaryExpression.class)).jjtGetParent()).getOperators().equals("["))
								{
									SimpleNode parent=(SimpleNode) primary.getFirstParentOfType(ASTAdditiveExpression.class);
									for(int k=1;k<parent.jjtGetNumChildren();k++)
									{
										if(parent.jjtGetChild(k) instanceof ASTUnaryExpression)
										{
											ASTPrimaryExpression child=(ASTPrimaryExpression) ((SimpleNode) parent.jjtGetChild(k)).getFirstChildOfType(ASTPrimaryExpression.class);
											ExpressionValueVisitor expvst = new ExpressionValueVisitor();
											ExpressionVistorData visitdata = new ExpressionVistorData();
											visitdata.currentvex = child.getCurrentVexNode();
											visitdata.currentvex.setfsmCompute(true);
											expvst.visit(child, visitdata);
											visitdata.currentvex.setfsmCompute(false);
											Expression value1 = visitdata.value;
											Domain mydomain=null;
											if(value1!=null)
											mydomain = value1.getDomain(visitdata.currentvex.getLastsymboldomainset());
											if((mydomain instanceof IntegerDomain)&&((IntegerDomain)mydomain).getMin()==((IntegerDomain)mydomain).getMax()){
												size.setMax(size.getMax()-((IntegerDomain)mydomain).getMax());
												size.setMin(size.getMin()-((IntegerDomain)mydomain).getMin());
											}
											else{ 
												size=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
												break;
											}
										}
										else{ 
											size=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
											break;
										}
									}
								
								}
								//end
								if(vex!=null && expression.getDomain(vex) != null){
									IntegerDomain idomain=Domain.castToIntegerDomain(expression.getDomain(vex));
									if(idomain.isUnknown())
										return false;
									IntegerInterval interval = idomain.jointoOneInterval();
									
									/**begin: modified by nmh. ���ӹ�������ת���ļ��*/
									if (isTypeCast && !(expType instanceof CType_Unkown) && (expType instanceof CType_AbstPointer||expType instanceof  CType_Array)) {
										interval = handleTypeCastInterval(interval, curtype, expType);
									}
									/**end: modified by nmh*/

									//��ʱ��ô�޸�
									if(idomain!=null&&confirmFuncOOB(interval,size)&&!curvar.isParam()){
										fsmin.setDesp("�ڵ� " + curvar.getNode().getBeginLine() + " �ж���Ĵ�СΪ " +size+ " �������ָ��\""+curvar.getImage()+"\"�����ڵ�"+snode.getBeginLine()+"��Խ�� ���±귶Χ��"+interval);
										fsmin.setNodeUseToFindPosition(snode);
										return true;
									}	
								}
							}
						}
					}else{
						throw new RuntimeException("ASTPostfixExpression error!");
					}
					
					if(type instanceof CType_AbstPointer){
						CType_AbstPointer ptype=(CType_AbstPointer) type;
						curtype=ptype.getOriginaltype();
					}
				}else if(operator.equals("(")){
					continue out;
				}else if(operator.equals(".")){
					ASTFieldId field=null;
					if(flag){
						field=(ASTFieldId)post.jjtGetChild(j++);
					}else{
						throw new RuntimeException("ASTPostfixExpression error!");
					}
					curvar=field.getVariableNameDeclaration();
					if(curvar==null)
						continue out;
					else
						curtype=curvar.getType();
				}else if(operator.equals("->")){
					ASTFieldId field=null;
					if(flag){
						field=(ASTFieldId)post.jjtGetChild(j++);
					}else{
						throw new RuntimeException("ASTPostfixExpression error!");
					}
					curvar=field.getVariableNameDeclaration();
					if(curvar==null)
						continue out;
					else
						curtype=curvar.getType();
				}else{
					//do nothing
				}
			}
		  }
		
		}
		return false;
	}
	public static IntegerInterval getrange(ASTPrimaryExpression snode,VariableNameDeclaration var){
		IntegerInterval size= new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
		ExpressionValueVisitor expvst = new ExpressionValueVisitor();
		ExpressionVistorData visitdata = new ExpressionVistorData();
		visitdata.currentvex = snode.getCurrentVexNode();
		visitdata.currentvex.setfsmCompute(true);
		expvst.visit(snode, visitdata);
		visitdata.currentvex.setfsmCompute(false);
		Expression value1 = visitdata.currentvex.getValue(var);
		PointerDomain mydomain=null;
		
			try
			{
				if(value1!=null && value1.getDomain(visitdata.currentvex.getSymDomainset())!=null&&value1.getDomain(visitdata.currentvex.getSymDomainset()).getDomaintype()== DomainType.POINTER)
				mydomain = (PointerDomain) value1.getDomain(visitdata.currentvex.getSymDomainset()).clone();
			} catch (CloneNotSupportedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		if(mydomain!=null&&mydomain.offsetRange!=null){
			IntegerInterval Len=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);
			//temp���ڴ��ÿ���ռ�ĳ���
			for (IntegerInterval range : mydomain.offsetRange.intervals) {
				if(range.getMax()==Long.MAX_VALUE||range.getMin()==Long.MIN_VALUE
						||(range.getMax()==range.getMin()))
					//��ֹ  char *p=���㲻���ı��ʽ��*q=p+1����ʱq��offsetRangeΪ{-1��-1}
					{
					Len=new IntegerInterval(Long.MIN_VALUE,Long.MAX_VALUE);	
					break;
					}
				if(range.getMax()<Len.getMax())
					Len.setMax(range.getMax());
				if(range.getMin()>Len.getMin())
					Len.setMin(range.getMin());
			}
			if(Len.getMax()!=Long.MAX_VALUE&&Len.getMin()!=Long.MIN_VALUE)
				size=Len;
		}
		return size;
	}
	
	/**
	 * add by nmh
	 * ���㾭������ת����ľ�������
	 * @param interval
	 * @param arrType
	 * @param expType
	 * @return
	 */
	private static IntegerInterval handleTypeCastInterval(IntegerInterval interval, CType arrType, CType expType) {
		IntegerInterval retInterval = null;
		
		CType originalArrayType = ((CType_AbstPointer) arrType).getOriginaltype();
		int arrayTypeSize = originalArrayType.getSize();
		
		CType originalCastType = ((CType_AbstPointer) expType).getOriginaltype();
		int expTypeSize = originalCastType.getSize();
		
		if (arrayTypeSize < expTypeSize) {
			retInterval = IntegerInterval.mul(interval, (expTypeSize/arrayTypeSize));
		} else {
			retInterval = IntegerInterval.div(interval, (arrayTypeSize/expTypeSize));
		}
		
		return retInterval;
	}
	
	/**
	 * add by nmh
	 * ȷ�Ϻ��������Ƿ����OOB
	 * @param interval
	 * @param arrayInterval
	 * @return
	 * 
	 */
	private static boolean confirmFuncOOB(IntegerInterval interval, IntegerInterval arrayInterval) {
		if (interval != null && !interval.isEmpty()) {
			long min, max;
			
			/**����interval�����ʽ��д���*/
			if (interval.isCanonical()) {
				max = min = interval.getMin();
			} else {
				if (interval.getMin() != Math.round(Double.NEGATIVE_INFINITY)) {
					min = interval.getMin();
				} else {
					min = interval.getMax();
				}
				
				max = interval.getMax();
			}
			
			if ((min < arrayInterval.getMin() || max > arrayInterval.getMax())) {
				return true;
			}
		}	
		return false;
	}
	
	//�ж��ַ����Ƿ��з������ַ�
	public static boolean isNum(String str){ 
		if(str==null||str.isEmpty()){
			return false;
		}
        String   patternStr= "\\d\\.?\\d*";    
        Pattern   p=Pattern.compile(patternStr);     
        Matcher   m=p.matcher(str); 
        boolean   b=m.matches();
		return b;
	}
	public static int getOOBNO(){
		return OOBNO;
	}
}
