package softtest.symboltable.c;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.*;
import softtest.scvp.c.SCVP;
import softtest.scvp.c.SCVPString;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;

public class NameOccurrence implements Serializable
{
	private NameDeclaration decl;
	private SimpleNode location;
	private String image;
	

	private SCVP scvp_lsc = null;
	
	public SCVP getScvp_lsc() {
		return scvp_lsc;
	}


	//zys:
    /** �������� */
    public enum OccurrenceType{
    	/** ���� */
    	DEF,
    	/** ʹ�� */
    	USE,
    	DEF_AFTER_USE,//i++;i+=5;
    	//add by cmershen,2016.3.14,�������
    	ENTRANCE
    }
    //added by cmershen,2016.5.3 ���������
    public enum DefinitionType {
    	ASSIGN,//��ֵ
    	CONDITION,//����
    	LOOP,//ѭ��
    	LIB,//�⺯��
    	PARAMETER,//��������
    	FUNCTION//����������
    }
    public String methodName = "";
    /** ��������,ֻ��Ա����ĳ��� */
    private OccurrenceType occurrenceType;
    //added by cmershen,2016.5.3 ���������
    public DefinitionType definitionType;
    /** ʹ��-�������������˿��Ե��ﱾʹ�ó��ֵ����ж�����֣����ڶ�����֣�������Ϊnull,�����е�Ԫ�ض�Ϊ������� */
    private List<NameOccurrence> use_def = null;
    
    /** ����-ʹ�����������˱�����������п��Ե����ʹ�ó��֣�����ʹ�ó��֣�������Ϊnull,�����е�Ԫ�ض�Ϊʹ�ó��� */
    private List<NameOccurrence> def_use = null;
    
    /** ����-ȡ���������������˱�����������п��Ե���Ķ�����֣�����ʹ�ó��֣�������Ϊnull,�����е�Ԫ�ض�Ϊ������� */
    private List<NameOccurrence> def_undef = null;
    
    /** ȡ������-�������������˿��Ե��ﱾ������ֵ����ж�����֣�����ʹ�ó��֣�������Ϊnull,�����е�Ԫ�ض�Ϊ������� */
    private List<NameOccurrence> undef_def = null;
    
    //added by cmershen,2016.5.3,���Ӷ�ֵ��Ϣ�б�
   // public List<SCVP> scvpList = null;
    public List<SCVPString> scvpList = null;
    // added by cmershen, 2017.10.30, ����Ӱ����
    private List<NameOccurrence> effected = new ArrayList<>();
    public SCVPString getSCVP(){
    	if (scvpList != null && scvpList.size() != 0)
    		return scvpList.get(0);
    	return null;
    }
    
	public NameOccurrence(NameDeclaration decl,SimpleNode location, String image)
	{
		this.location = location;
		this.image = image;
		this.decl=decl;
		 //added by cmershen,2016.5.3,���Ӷ�ֵ��Ϣ�б�
		this.scvpList = new ArrayList<SCVPString>();
	}
	
    /** ����-ȡ������Ӷ��壬��֤���ظ���� */
    public boolean addDefUndef(NameOccurrence occ){
    	if(occ.getOccurrenceType()==OccurrenceType.DEF || occ.getOccurrenceType() == OccurrenceType.DEF_AFTER_USE){
    		for(NameOccurrence o:def_undef){
    			if(o.getLocation()==occ.getLocation()){
    				return false;
    			}
    		}
    		def_undef.add(occ);
    		return true;
    	}
    	return false;
    }
    
    /** ����-ʹ������Ӷ��壬��֤���ظ���� */
    public boolean addDefUse(NameOccurrence occ){
    	if(occ.getOccurrenceType()==OccurrenceType.USE || occ.getOccurrenceType() == OccurrenceType.DEF_AFTER_USE){
    		for(NameOccurrence o:def_use){
    			if(o.getLocation()==occ.getLocation()){
    				return false;
    			}
    		}
    		def_use.add(occ);
    		return true;
    	}
    	return false;
    }
    
    /** ����-ȡ������Ӷ��壬��֤���ظ���� */
    public boolean addUndefDef(NameOccurrence occ){
    	// modified by cmershen,2016.10.10,���entrance����
    	if(occ.getOccurrenceType()==OccurrenceType.DEF || occ.getOccurrenceType() == OccurrenceType.DEF_AFTER_USE || occ.getOccurrenceType() == OccurrenceType.ENTRANCE){
    		for(NameOccurrence o:undef_def){
    			if(o.getLocation()==occ.getLocation()){
    				return false;
    			}
    		}
    		undef_def.add(occ);
    		return true;
    	}
    	return false;
    }
    
    /** ��ʹ��-��������Ӷ��壬��֤���ظ���� */
    public boolean addUseDef(NameOccurrence occ){ 
    	// modified by cmershen,2016.3.14,���entrance����
    	if(occ.getOccurrenceType()==OccurrenceType.DEF || occ.getOccurrenceType() == OccurrenceType.DEF_AFTER_USE || occ.getOccurrenceType() == OccurrenceType.ENTRANCE){
    		for(NameOccurrence o:use_def){
    			if(o.getLocation()==occ.getLocation()){
    				return false;
    			}
    		}
    		use_def.add(occ);
    		return true;
    	}
    	return false;
    }

	public SimpleNode getLocation()
	{
		return location;
	}

	public boolean equals(Object o)
	{
		NameOccurrence n = (NameOccurrence) o;
		return n.location.equals(location);
	}

	public int hashCode()
	{
		return location.hashCode();
	}

	public String getImage()
	{
		return image;
	}

	public boolean isMethodInvocation(){
		return decl instanceof MethodNameDeclaration;
	}
	
	public String toString()
	{
		return getImage()  + (this.isMethodInvocation() ? "(method call:"+ location.getBeginLine()+")" : ":" + location.getBeginLine());
	}

	public void setImage(String image) {
		this.image = image;
	}

	public void setLocation(SimpleNode location) {
		this.location = location;
	}

	public OccurrenceType getOccurrenceType()
	{
		return occurrenceType;
	}

	public void setOccurrenceType(OccurrenceType occurrenceType)
	{
		this.occurrenceType = occurrenceType;
	}

	public List<NameOccurrence> getUse_def()
	{
		return use_def;
	}

	public void setUse_def(List<NameOccurrence> use_def)
	{
		this.use_def = use_def;
	}

	public List<NameOccurrence> getDef_use()
	{
		return def_use;
	}

	public void setDef_use(List<NameOccurrence> def_use)
	{
		this.def_use = def_use;
	}

	public List<NameOccurrence> getDef_undef()
	{
		return def_undef;
	}

	public void setDef_undef(List<NameOccurrence> def_undef)
	{
		this.def_undef = def_undef;
	}

	public List<NameOccurrence> getUndef_def()
	{
		return undef_def;
	}

	public void setUndef_def(List<NameOccurrence> undef_def)
	{
		this.undef_def = undef_def;
	}
	
	/** �������� */
    public void setDeclaration(NameDeclaration decl){
    	this.decl=decl;
    }
    
    /** ������� */
    public NameDeclaration getDeclaration(){
    	return decl;
    }
    
    /** ���������� */
    public OccurrenceType checkOccurrenceType(){
    	OccurrenceType type=OccurrenceType.USE;
    	if(isOnLeftHandSide() || isSelfAssignment()){
    		type=OccurrenceType.DEF;
    		if (isSelfAssignment())// || isAssignSameVar(location)) 
    		{
				type = OccurrenceType.DEF_AFTER_USE;
			}
    	}
    	//added by cmershen,2016.3.7
    	//scanf,gets,strcpy,strdup�ȿ⺯���������Ƕ���㣡
    	//2016.10.13 cmershen ����free
    	SimpleNode astNode = this.getLocation();
    	String[] defFuncs = new String[]{"scanf","gets","strcpy","strdup","free"};
    	if(astNode.containsParentOfType(ASTStatement.class)) {
	    	SimpleNode statementAncestor = (SimpleNode) astNode.getFirstParentOfType(ASTStatement.class);
	    	if(statementAncestor.containsChildOfType(ASTPrimaryExpression.class)) {
	    		SimpleNode primaryNode = (SimpleNode) statementAncestor.getFirstChildOfType(ASTPrimaryExpression.class);
	    		if (((ASTPrimaryExpression)primaryNode).isMethod()) { // �ýڵ��Ǻ�������
	    			String funcName = primaryNode.getImage();
	    			for(String defFunc:defFuncs) {
		    			if(funcName.equals(defFunc)) {
		    				System.out.println(funcName+"�����Ƕ���㣬����������");
		    				type = OccurrenceType.DEF;
		    				methodName = funcName;
		    			}
	    			}
	    		}
	    	}
    	}
    	if(type==OccurrenceType.USE){
    		if(use_def==null){
    			use_def=new LinkedList<NameOccurrence>();
    		}
    		def_use=null;
    		def_undef=null;
    		undef_def=null;
    	}else if (type == OccurrenceType.DEF ){
			use_def = null;
			if (def_use == null) {
				def_use = new LinkedList<NameOccurrence>();
			}
			if (def_undef == null) {
				def_undef = new LinkedList<NameOccurrence>();
			}
			if (undef_def == null) {
				undef_def = new LinkedList<NameOccurrence>();
			}
		} else {
			if (use_def == null) {
				use_def = new LinkedList<NameOccurrence>();
			}
			if (def_use == null) {
				def_use = new LinkedList<NameOccurrence>();
			}
			if (def_undef == null) {
				def_undef = new LinkedList<NameOccurrence>();
			}
			if (undef_def == null) {
				undef_def = new LinkedList<NameOccurrence>();
			}
		}
    	return type;
    }
    
    /**
     * @author zys	2010.1.31
     * @return �жϵ�ǰ�������֣�NameOccurence)�Ƿ�λ�ڣ���࣬���Ƿ�Ϊ������ֵDEF
     */
    public boolean isOnLeftHandSide() {
    	SimpleNode declarator=null;
        if (location.jjtGetParent() instanceof ASTDeclarator) {
        	declarator = (ASTDeclarator) location.jjtGetParent();
        	if(declarator.getNextSibling()!=null){
            	return true;
            }
        } 
        //Ϊ��֧�������±�Ķ���ʹ�ö��壬ԭ�еı������ַ��ʲ���Pri������Post������location�����仯
        //add by zhouhb
        //2011.3.23
        if (location instanceof ASTPostfixExpression && decl.getType().isArrayType()) {
        	Node assignment=location.jjtGetParent().jjtGetParent();
        	if(assignment instanceof ASTAssignmentExpression && assignment.jjtGetNumChildren()==3){
        		return true;
        	}
        } 
        if(location.jjtGetParent() instanceof ASTPostfixExpression)
        {
        	declarator = (ASTPostfixExpression) location.jjtGetParent();
        	Node assignment=declarator.jjtGetParent().jjtGetParent();
        	if(assignment instanceof ASTAssignmentExpression && assignment.jjtGetNumChildren()==3){
        		return true;
        	}
        	//add else branch by cmershen,2016.5.31 ��Ӷ�*p=2���͵�֧��
        	else 
        	{
        		Node child = assignment;
        		assignment = assignment.jjtGetParent();
        		if(assignment instanceof ASTAssignmentExpression && assignment.jjtGetNumChildren()==3 && child == assignment.jjtGetChild(0)){
            		return true;
            	}
        	}
        }
        return false;
    }
    
    /**�жϵ�ǰ���ʽ�Ƿ�Ϊ��ֵ���ʽ�������ֵ���ʽ�����Ҳ������ͬ�ı���(�������Ը�ֵi++�ı��֣����򷵻�true
     * ����int i=0��j=5;
     * 	i=j+i+3;
     * @author zys	2010.3.12
     * @return 
     */
    public boolean isAssignSameVar(SimpleNode node)
    {
    	Node assignmentNode=node.getFirstParentOfType(ASTAssignmentExpression.class);
    	if(assignmentNode!=null 
    			&& assignmentNode instanceof ASTAssignmentExpression
    			&& assignmentNode.jjtGetNumChildren()==3)
    	{
    		ASTUnaryExpression leftExpr=(ASTUnaryExpression)assignmentNode.jjtGetChild(0);
    		ASTAssignmentExpression rightExpr=(ASTAssignmentExpression)assignmentNode.jjtGetChild(2);
    		
    		//ȡ����������
    		ASTPrimaryExpression leftVar=(ASTPrimaryExpression)leftExpr.getFirstChildOfType(ASTPrimaryExpression.class);
    		String varName=leftVar.getImage();
    		
    		//ȡ���Ҳ���ʽ�����еı������б�
    		List rightVars=rightExpr.findChildrenOfType(ASTPrimaryExpression.class);
    		for(Object o:rightVars)
    		{
    			if(o instanceof ASTPrimaryExpression)
    			{
    				ASTPrimaryExpression priExp=(ASTPrimaryExpression)o;
    				if(priExp.getImage().equals(varName))
    				{
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }
    /**
     * @author zys	2010.1.31
     * @return �жϵ�ǰ�������֣�NameOccurence)�Ƿ�Ϊ�������Լ���++ --)�����Ƿ�Ϊ������ֵDEF
     */
    public boolean isSelfAssignment() {
    	if(location instanceof ASTPrimaryExpression){
	    	if(location.jjtGetParent() instanceof ASTPostfixExpression){
		    	ASTPostfixExpression postfixExp=(ASTPostfixExpression)location.jjtGetParent();
		    	//����i++�����
		    	if(postfixExp.getOperatorType().contains("++") || postfixExp.getOperatorType().contains("--"))
		    	{
		    		return true;
		    	}
		    	//����++i�����
		    	SimpleNode unaryExp=(SimpleNode)(postfixExp.jjtGetParent().jjtGetParent());
		    	if(unaryExp instanceof ASTUnaryExpression && (unaryExp.getOperatorType().contains("++") || unaryExp.getOperatorType().contains("--")))
		    	{
		    		return true;
		    	}
	    	}
    	}
    	return false;
    }

	public List<NameOccurrence> getEffected() {
		return effected;
	}

	public void setEffected(List<NameOccurrence> effected) {
		this.effected = effected;
	}
}
