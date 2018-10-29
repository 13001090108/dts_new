package softtest.domain.c.interval;

import java.util.HashSet;


import softtest.config.c.Config;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.domain.c.interval.IntegerDomain;
import softtest.symboltable.c.Type.*;
import softtest.symboltable.c.VariableNameDeclaration;

public class PointerDomain extends Domain {
	//zys:2010.8.11	��¼�������������ʱ�Ƿ�Ϊ��������
	private boolean isMinus=false;
	CType elementtype;
	Expression length;
	PointerValue value;
	
	//add by zhouhb
	//��ʶָ��ƫ������Χ
	public IntegerDomain offsetRange;
	//��ʶָ��ռ���Чֵ����
	public IntegerDomain allocRange;
    //���ڱ�ʶָ��ָ��ռ�������͡���ָ��ΪNUll��ָ��δ��ʼ��ΪNotNull
//    public CType AllocType=CType_AllocType.Unkown; 
//    //���ڱ�ʶָ��ָ��ռ�������ͻ�ۺ�ľ�����Ϣ��
    //add by zhouhb 2010/8/31
    public HashSet<CType_AllocType> Type;
    //��ʶ����ռ����ƣ���offsetRangeһͬ��������ռ�
    public String name;
	//�洢�ռ��ϵ���ʵֵ����ʱδ��ֵ����
	public Domain realDomain;
	//�������ϣ����ڼ�¼������Ϣ
	public HashSet<VariableNameDeclaration>VariableInMemory=new HashSet<VariableNameDeclaration>();
	//����ռ��ϱ�������
	public HashSet<VariableNameDeclaration>Alias=new HashSet<VariableNameDeclaration>();
	
	
	public PointerDomain() {
		elementtype=CType_BaseType.getBaseType("void");
		domaintype=DomainType.POINTER;
		value=PointerValue.UNKOWN;
		offsetRange=new IntegerDomain(IntegerDomain.DEFAULT_MIN,IntegerDomain.DEFAULT_MAX);
		allocRange=new IntegerDomain(0,0);
		Type=new HashSet();
	}
	
	public PointerDomain(PointerValue value){
		this();
		this.value=value;
	}
	/**
	 * �õ�һ��ָ����δ֪����
	 * 
	 * @return һ��ָ����δ֪����
	 */
	public static PointerDomain getUnknownDomain() {
		PointerDomain r = new PointerDomain(PointerValue.UNKOWN);
		r.setUnknown(true);
		r.Type.add(CType_AllocType.heapType);
		return r;
	}
	public CType getElementtype() {
		return elementtype;
	}

	public void setElementtype(CType elementtype) {
		this.elementtype = elementtype;
	}

	public Expression getLength() {
		return length;
	}

	public Expression getSize(){
		Expression ret=null;
		if(length==null||elementtype==null){
			ret =new Expression(SymbolFactor.genSymbol(CType_BaseType.getBaseType("int")));
		}else{
			ret=length.mul(new Expression(elementtype.getSize()));
		}
		return ret;
	}
	
	public void setLength(Expression length) {
		this.length = length;
	}

	public PointerValue getValue() {
		return value;
	}

	public void setValue(PointerValue value) {
		this.value = value;
	}

	//��ȡ�ռ䳤��
	public long getLen(PointerDomain d){
		long Len=0;
		//temp���ڴ��ÿ���ռ�ĳ���
		long temp;
		for (IntegerInterval range : d.offsetRange.intervals) {
			temp=range.getMax()-range.getMin()+1;
			if(temp>Len)
				Len=temp;
		}
		return Len;	
	}
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		if(unknown)
			return "unknown";
		else{
			String result = " " + "offsetRange: "+ ((offsetRange != null) ? offsetRange.toString() : "");
			result = result + " " + "Eval: "+ ((allocRange!=null)? allocRange.toString(): "");
			result = result +" "+"Type:"+ Type.toString();
			if(value==PointerValue.NULL){
				return "NULL"+result;
			}else if(value==PointerValue.NULL_OR_NOTNULL){
				return "NULL_OR_NOTNULL"+result;
			}else if(value==PointerValue.EMPTY){
				return "EMPTY"+result;
			}else if(value==PointerValue.UNKOWN){
				return "UNKOWN"+result;
			}else {
//				b.append("&");
//				b.append(elementtype.toString());
//				b.append("(");
//				b.append(length);
//				b.append(")");
				return "NOTNULL"+result;
				//return b.toString()+result;
			}
		}
		//��ʱ����������������CPPͨ�����Ժ��������Ӧ����
//		result = result +" "+"Memory:"+ name;
//		result = result +" "+"Alias:"+Name(Alias);
//		result = result +" "+"VariableInMemory:"+Name(VariableInMemory);
	}
	
	@Override
	public Domain clone() throws CloneNotSupportedException {
		PointerDomain domain=(PointerDomain)super.clone();
		domain.elementtype=elementtype;
		domain.length=length;
		domain.value=value;
		domain.Type=Type;
		return domain;
	}
	public static PointerDomain add(PointerDomain pointdomain, IntegerDomain i) {
		if(pointdomain.unknown)
			return PointerDomain.getUnknownDomain();
		PointerDomain result;
		try{
		result = (PointerDomain)pointdomain.clone();
		result.offsetRange=IntegerDomain.sub(result.offsetRange, i);
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	
	public static PointerDomain sub(PointerDomain pointdomain, IntegerDomain i) {
		if(pointdomain.unknown)
			return PointerDomain.getUnknownDomain();
		PointerDomain result;
		try{
		result = (PointerDomain)pointdomain.clone();
		result.offsetRange=IntegerDomain.add(result.offsetRange, i);
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	
	public static PointerDomain inverse(PointerDomain a) {
		PointerDomain ret= null;
		try {
			ret= (PointerDomain)a.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		if(a.unknown)
			return ret;
		switch(ret.value){
		case NULL:
			ret.value=PointerValue.NOTNULL;
			break;
		case EMPTY:
			ret.value=PointerValue.NULL_OR_NOTNULL;
			break;
		case NULL_OR_NOTNULL:
			ret.value=PointerValue.EMPTY;
			break;
		case NOTNULL:
			ret.value=PointerValue.NULL;
			break;
		}
		//����ָ��ģ�͹������䣬ԭ�еķ������㲻�������������󣬹��������޸�
		//��ֵ�����޸Ĳ���ȷ���д��޸�
		//modified by zhouhb 2010/7/19
		ret.offsetRange=IntegerDomain.inverse(a.offsetRange);
		//ret.AllocType=ret.AllocTypeInverse(a.AllocType);
		ret.Type=ret.TypeInverse(a.Type);
		return ret;
	}
	
	public static PointerDomain intersect(PointerDomain a,PointerDomain b){
		if(a.unknown || a.getValue()==PointerValue.UNKOWN)
			return (PointerDomain) b.copy();
		if(b.unknown || b.getValue()==PointerValue.UNKOWN)
			return (PointerDomain) a.copy();
		PointerDomain ret= new PointerDomain();
		ret.setElementtype(ret.getElementtype());
		Expression e1=a.getLength();
		Expression e2=b.getLength();
		ret.setLength(e1!=null?e1:e2);
		
		if(a.getValue()==b.getValue()){
			ret.setValue(a.getValue());
		}else if(a.getValue()==PointerValue.NULL_OR_NOTNULL){
			ret.setValue(b.getValue());
		}else if(b.getValue()==PointerValue.NULL_OR_NOTNULL){
			ret.setValue(a.getValue());
		}else {
			ret.setValue(PointerValue.EMPTY);
		}
		//����ָ��ģ�͹������䣬ԭ�еķ������㲻�������������󣬹��������޸�
		//modified by zhouhb 2010/7/19
		ret.offsetRange=IntegerDomain.intersect(a.offsetRange, b.offsetRange);
		ret.allocRange=IntegerDomain.intersect(a.allocRange, b.allocRange);
		if(ret.value.toString().equals("NULL")){
			//ret.AllocType=CType_AllocType.Null;
			ret.Type.add(CType_AllocType.Null);
		}else{
			//ret.AllocType=ret.AllocTypeIntersect(a.AllocType, b.AllocType);
			ret.Type=ret.TypeIntersect(a.Type, b.Type);
		}
		return ret;
	}
	
	public static PointerDomain union(PointerDomain a,PointerDomain b){
		PointerDomain ret= new PointerDomain();
		ret.setElementtype(ret.getElementtype());
		Expression e1=a.getLength();
		Expression e2=b.getLength();
		ret.setLength(e1!=null?e1:e2);
		
		if (Config.DOMAIN_CONSERVATIVE) {
			if (a.unknown || b.unknown) {
				ret = PointerDomain.getUnknownDomain();
				return ret;
			}
		} else {
			if (a.unknown) {
				if (!b.unknown && b.value == PointerValue.NOTNULL) {
					// notnull��unknown ��ȡunknown
					ret = PointerDomain.getUnknownDomain();
				} else {
					ret = (PointerDomain) b.copy();
				}
				return ret;
			}
			if (b.unknown) {
				if (!a.unknown && a.value == PointerValue.NOTNULL) {
					// notnull��unknown ��ȡunknown
					ret = PointerDomain.getUnknownDomain();
				} else {
					ret = (PointerDomain) a.copy();
				}
				return ret;
			}
		}
		
		if(a.getValue()==b.getValue()){
			ret.setValue(a.getValue());
		}else if(a.getValue()==PointerValue.EMPTY){
			ret.setValue(b.getValue());
		}else if(b.getValue()==PointerValue.EMPTY){
			ret.setValue(a.getValue());
		}else{
			ret.setValue(PointerValue.NULL_OR_NOTNULL);
		}
		try{
		if (a.offsetRange == null && b.offsetRange != null)
			ret.offsetRange = b.offsetRange.clone();
		else if (b.offsetRange == null && a.offsetRange != null)
			ret.offsetRange= a.offsetRange.clone();
		else if (b.offsetRange == null && a.offsetRange == null)
			ret.offsetRange=null;
		else ret.offsetRange=IntegerDomain.union(a.offsetRange,b.offsetRange);
		
		if (a.allocRange == null && b.allocRange != null)
			ret.allocRange = b.allocRange.clone();
		else if (b.allocRange == null && a.allocRange != null)
			ret.allocRange = a.allocRange.clone();
		else if (b.allocRange == null && a.allocRange == null)
			ret.allocRange = null;
		else ret.allocRange=IntegerDomain.union(a.allocRange,b.allocRange);
		
		//ret.AllocType=AllocTypeUnion(a.AllocType,b.AllocType);
		ret.Type=TypeUnion(a.Type,b.Type);
		}catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		return ret;
	} 
	
//	public static CType AllocTypeUnion(CType a,CType b){
//		CType result;
//		if(a.equals(CType_AllocType.heapType)){
//			if(b.equals(CType_AllocType.heapType))
//				result=a;
//			else if(b.equals(CType_AllocType.stackType)||b.equals(CType_AllocType.staticType)||b.equals(CType_AllocType.NotNull))
//				result=CType_AllocType.NotNull;
//			else
//				result=CType_AllocType.Unkown;
//		}else if(a.equals(CType_AllocType.stackType)){
//			if(b.equals(CType_AllocType.stackType))
//				result=a;
//			else if(b.equals(CType_AllocType.heapType)||b.equals(CType_AllocType.staticType)||b.equals(CType_AllocType.NotNull))
//				result=CType_AllocType.NotNull;
//			else 
//				result=CType_AllocType.Unkown;
//		}else if(a.equals(CType_AllocType.staticType)){
//			if(b.equals(CType_AllocType.staticType))
//				result=a;
//			else if(b.equals(CType_AllocType.heapType)||b.equals(CType_AllocType.stackType)||b.equals(CType_AllocType.NotNull))
//				result=CType_AllocType.NotNull;
//			else 
//				result=CType_AllocType.Unkown;
//		}else if(a.equals(CType_AllocType.Null)){
//			if(b.equals(CType_AllocType.Null))
//				result=a;
//			else 
//				result=CType_AllocType.Unkown;
//		}else if(a.equals(CType_AllocType.NotNull)){
//			if(b.equals(CType_AllocType.NotNull))
//				result=a;
//			else if(b.equals(CType_AllocType.heapType)||b.equals(CType_AllocType.staticType)||b.equals(CType_AllocType.stackType)||b.equals(CType_AllocType.NotNull))
//				result=CType_AllocType.NotNull;
//			else 
//				result=CType_AllocType.Unkown;
//		}else if(a.equals(CType_AllocType.Empty)){
//			if(b.equals(CType_AllocType.Empty))
//				result=a;
//			else
//				result=CType_AllocType.Unkown;
//		}else
//			result=CType_AllocType.Unkown;
//		return result;
//	}

//	public static CType AllocTypeIntersect(CType a,CType b){
//		CType result;
//		if(a.equals(CType_AllocType.heapType)){
//			if(b.equals(CType_AllocType.heapType)||b.equals(CType_AllocType.NotNull)||b.equals(CType_AllocType.Unkown))
//				result=a;
//			else
//				result=CType_AllocType.Empty;
//		}else if(a.equals(CType_AllocType.stackType)){
//			if(b.equals(CType_AllocType.stackType)||b.equals(CType_AllocType.NotNull)||b.equals(CType_AllocType.Unkown))
//				result=a;
//			else 
//				result=CType_AllocType.Empty;
//		}else if(a.equals(CType_AllocType.staticType)){
//			if(b.equals(CType_AllocType.staticType)||b.equals(CType_AllocType.NotNull)||b.equals(CType_AllocType.Unkown))
//				result=a;
//			else 
//				result=CType_AllocType.Empty;
//		}else if(a.equals(CType_AllocType.Null)){
//			if(b.equals(CType_AllocType.Null)||b.equals(CType_AllocType.Unkown))
//				result=a;
//			else 
//				result=CType_AllocType.Empty;
//		}else if(a.equals(CType_AllocType.NotNull)){
//			if(b.equals(CType_AllocType.NotNull)||b.equals(CType_AllocType.Unkown))
//				result=a;
//			else if(b.equals(CType_AllocType.heapType)||b.equals(CType_AllocType.staticType)||b.equals(CType_AllocType.stackType))
//				result=b;
//			else 
//				result=CType_AllocType.Empty;
//		}else if(a.equals(CType_AllocType.Unkown)){
//			result=b;
//		}else
//			result=CType_AllocType.Empty;
//		return result;
//	}
	
//	public static CType AllocTypeInverse(CType a){
//		CType result;
//		if(a.equals(CType_AllocType.heapType)||a.equals(CType_AllocType.stackType)||a.equals(CType_AllocType.staticType)||a.equals(CType_AllocType.NotNull)){
//			result=CType_AllocType.Null;
//		}else if(a.equals(CType_AllocType.Null)){
//				result=CType_AllocType.NotNull;
//		}else if(a.equals(CType_AllocType.Unkown)){
//				result=CType_AllocType.Empty;
//		}else 
//			result=CType_AllocType.Unkown;
//		return result;
//	}
	
	public static HashSet<CType_AllocType> TypeUnion(HashSet<CType_AllocType> a,HashSet<CType_AllocType> b){
		HashSet<CType_AllocType> result=(HashSet<CType_AllocType>)a.clone();
		for(CType_AllocType type:b){
			result.add(type);
		}
		return result;
	}
	
	//���������󽻽�һ����
	//modified by zhouhb 2010/10/19
	public static HashSet<CType_AllocType> TypeIntersect(HashSet<CType_AllocType> a,HashSet<CType_AllocType> b){
		HashSet<CType_AllocType> result=new HashSet();
		if(!a.isEmpty()){
			if(a.contains(CType_AllocType.NotNull)&&b.contains(CType_AllocType.NotNull))
				for(CType_AllocType type:a){
					if(b.contains(type)){
						result.add(type);
					}
				}
			else if(a.contains(CType_AllocType.NotNull))
				result=b;
			else if(b.contains(CType_AllocType.NotNull))
				result=a;
			else
				result=b;
		}
		return result;
	}
	
	//���������󷴽�һ����
	//modified by zhouhb 2010/10/19
	public static HashSet<CType_AllocType> TypeInverse(HashSet<CType_AllocType> a){
		HashSet<CType_AllocType> result=(HashSet<CType_AllocType>)a.clone();
		if(result.contains(CType_AllocType.stackType)){
			result.remove(CType_AllocType.stackType);
			result.add(CType_AllocType.Null);
		}
		if(result.contains(CType_AllocType.heapType)){
			result.remove(CType_AllocType.heapType);
			result.add(CType_AllocType.Null);
		}
		if(result.contains(CType_AllocType.staticType)){
			result.remove(CType_AllocType.staticType);
			result.add(CType_AllocType.Null);
		}
		if(result.contains(CType_AllocType.Null)){
			result.remove(CType_AllocType.Null);
			result.add(CType_AllocType.NotNull);
		}
		return result;
	}

	public static PointerDomain getEmptyDomain(){
		PointerDomain ret=new PointerDomain();
		ret.value=PointerValue.EMPTY;
		return ret;
	}
	
	public static PointerDomain getFullDomain(){
		PointerDomain ret=new PointerDomain();
		ret.value=PointerValue.NULL_OR_NOTNULL;
		return ret;
	}
	
	public static PointerDomain getNullDomain(){
		PointerDomain ret=new PointerDomain();
		ret.value=PointerValue.NULL;
		return ret;
	}
	
	@Override
	public boolean isCanonical() {
		if (unknown) {
			return false;
		}
		if(value==PointerValue.NULL){
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		if (unknown) {
			return result;
		}
		result = PRIME * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PointerDomain other = (PointerDomain) obj;
		if(this.unknown && other.unknown)
			return true;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	//���������ͼ�б�ʾ
	public HashSet<String> Name(HashSet<VariableNameDeclaration> Variable){
		HashSet<String> result=new HashSet<String>();
		for(VariableNameDeclaration v:Variable){
			result.add(v.getImage());
		}
		return result;
	}
	
	//CPP�棺����NULLԤ�����Ϊ0����������0����Ӧ���ǿ�ָ�뻹�ǳ���0
	//��ʱ��������
//	private boolean IsNullPoint(ASTConstant node, Object data){
//			
//		
//	}
	private HashSet<VariableNameDeclaration> aliasMerge(HashSet<VariableNameDeclaration>Alias1,HashSet<VariableNameDeclaration>Alias2){
		for(VariableNameDeclaration name:Alias1){
			if(!Alias2.contains(name))
				Alias2.add(name);
		}
		for(VariableNameDeclaration v:Alias2){
			Domain domain =v.getDomain();
			((PointerDomain)domain).Alias=Alias2;
			v.setDomain(domain);
		}
		return Alias2;
	}
	private HashSet<VariableNameDeclaration> VariableInMemoryMerge(HashSet<VariableNameDeclaration>v1,HashSet<VariableNameDeclaration>v2){
		for(VariableNameDeclaration name:v1){
			if(!v2.contains(name))
				v2.add(name);
		}
		for(VariableNameDeclaration v:v2){
			Domain domain =v.getDomain();
			((PointerDomain)domain).VariableInMemory=v2;
			v.setDomain(domain);
		}
		return v2;
	}
	
	//��������ӱ���
	private boolean IsAlias(VariableNameDeclaration v1,VariableNameDeclaration v2){
		if(((PointerDomain)v1.getDomain()).name==((PointerDomain)v2.getDomain()).name&&
				((PointerDomain)v1.getDomain()).offsetRange.equals(((PointerDomain)v2.getDomain()).offsetRange))
			return true;
		else
			return false;
		
	}
	public static PointerDomain valueOf(String str) {
		PointerValue value = PointerValue.valueOf(str);
		return new PointerDomain(value);
	}

	public boolean isMinus() {
		return isMinus;
	}

	public void setMinus(boolean isMinus) {
		this.isMinus = isMinus;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Domain intersect(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Domain subtract(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Domain union(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Domain unionwithUnkown(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}
}
