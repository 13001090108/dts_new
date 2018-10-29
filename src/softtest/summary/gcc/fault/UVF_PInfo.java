package softtest.summary.gcc.fault;

import softtest.domain.c.interval.IntegerDomain;



public class UVF_PInfo{
	
	public enum UVF_PType {
		/**��UVF_Pǰ�û������Ϣ*/
		NEED,
		/**����UVF_Pǰ�û������Ϣ*/
		NOT_NEED,
	}
	
	/**��������*/
	private String name;
	/**paramIndex=-10����ʾ�ǲ���*/
	public int paramIndex;
	/**UVF_PType*/
	public UVF_PType type;
	/**��Ҫ����ʼ��������*/
	private IntegerDomain needInitedomain;
	private IntegerDomain initedomain;

	public  UVF_PInfo(String name, int paramIndex, UVF_PType type,IntegerDomain needInitedomain,IntegerDomain initedomain){
		this.type = type;
		this.paramIndex = paramIndex;
		this.name = name ;
		this.needInitedomain=needInitedomain;
		this.initedomain=initedomain;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getParamIndex() {
		return paramIndex;
	}

	public void setParamIndex(int paramIndex) {
		this.paramIndex = paramIndex;
	}

	public UVF_PType getType() {
		return type;
	}

	public void setType(UVF_PType type) {
		this.type = type;
	}

	public IntegerDomain getNeedInitedomain() {
		return needInitedomain;
	}

	public void setNeedInitedomain(IntegerDomain needInitedomain) {
		this.needInitedomain = needInitedomain;
	}

	public IntegerDomain getInitedomain() {
		return initedomain;
	}

	public void setInitedomain(IntegerDomain initedomain) {
		this.initedomain = initedomain;
	}

}
