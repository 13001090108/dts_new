package softtest.symboltable.c.Type;

import java.util.ArrayList;
import java.util.Hashtable;
import softtest.cfg.c.Edge;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.symboltable.c.ClassNameDeclaration;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.VariableNameDeclaration;

public class CType_Struct extends CType {
		
	protected int size = -1;
	
	//add by zhouhb
	//2011.4.7
	/**��Žṹ���Ա�������Ƽ�������*/
	protected Hashtable<String, CType> fieldType = new Hashtable<String, CType>();
	
	/**��ʶ�ṹ���������˳�����ڽṹ���ʼ������ʶ��*/
	protected ArrayList<String>mems = new ArrayList<String>();
	
	/** ���ó�Ա���������� */
	public CType addType(String image, CType field) {
		return fieldType.put(image, field);
	}
	
	/** ��¼��Ա�����ĳ���˳�� */
	public boolean addmem(String image) {
		return mems.add(image);
	}
	
	/** ��ó�Ա���������� */
	public CType getCType(String image) {
		return fieldType.get(image);
	}

	/** �����ϣ�� */
	public void clearDomainSet() {
		fieldType.clear();
	}

	/** ���ù�ϣ�� */
	public void setTable(Hashtable<String, CType> fieldType) {
		this.fieldType = fieldType;
	}

	/** ��ù�ϣ�� */
	public Hashtable<String, CType> getfieldType() {
		return fieldType;
	}

	/** �жϹ�ϣ���Ƿ�Ϊ�� */
	public boolean isEmpty() {
		return fieldType.isEmpty();
	}

	/** ��ӡ */
	@Override
	public String toString() {
		if(Config.Field){
			StringBuffer b = new StringBuffer();
			b.append("members of struct " + name + ": " );
			if (fieldType.size() == 0) {
				b.append("no member");
				return b.toString();
			}
			
			CType field=null;
			for(String image : fieldType.keySet())
			{
				field=fieldType.get(image);
				b.append(image + ":" + field.getName() + " ");
			}
			return b.toString();
		}else
			return "struct " + name;
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	//��֪����û���ð�����ʱ��ǳ����������
	public CType_Struct() {
		
	}
	//end by zhouhb
	public CType_Struct(String name) {
		super(name);
	}
	
	@Override
	public boolean isClassType() {
		return true;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public void calClassSize(Scope declscope) {
		if(declscope!=null){
			int tsize=0;
			ClassNameDeclaration typedecl=(ClassNameDeclaration)Search.searchInClassUpward(getName(), declscope);
			if(typedecl!=null){
				Scope s=typedecl.getNode().getScope();
				for(Object o:s.getVariableDeclarations().keySet()){
					VariableNameDeclaration field=(VariableNameDeclaration)o;
					if(field.getType() == null)
						continue;
					int fsize=field.getType().getSize();
						//����pack
					int pack = softtest.config.c.Config.PACK_SIZE;
					if (pack > 8) {
						pack = 8;
					}
					if (fsize % pack != 0) {
						fsize = fsize + (pack - fsize%pack);
					}
					tsize+=fsize;
				}
			}
			size=tsize;
		}
			
	}
	
	@Override
	public int getSize() {
		return size;
	}
}
