package softtest.SDataBase.c;



/** 
 * @author 
 * Miss_lizi
 * 函数级别的特征信息数据结构
 */
public class FuncInformation {
	//函数名称    函数有效行数   McCabe度量   程序词汇表   程序词汇量   程序词容量   语句平均承载信息量   词汇频率   函数注释比
	private String filepath;
	private String Funcname;
	private int func_vex_num;
	private int func_edge_num;
	private String len;
	private int McCabe;
	private String words;
	private int vocabulary_count;
	private String capacity;
	private String AVGS;
	private String VOCF;
	private String COMF;
	
	public FuncInformation(String funcInfo){
		String[] res = funcInfo.split("#");
		this.filepath = res[0];
		this.Funcname = res[1];
		this.func_vex_num = Integer.valueOf(res[2]);
		this.func_edge_num = Integer.valueOf(res[3]);
		this.len = res[4];
		this.McCabe = Integer.valueOf(res[5]);
		this.words = res[6];
		this.vocabulary_count = Integer.valueOf(res[7]);
		this.capacity = res[8];
		this.AVGS = res[9];
		this.VOCF = res[10];
		this.COMF = res[11];
	}
	
	public String getfilepath(){
		return this.filepath;
	}
	
	public String getfuncname(){
		return this.Funcname;
	}
	
	public int getfunc_vex_num(){
		return this.func_vex_num;
	}
	
	public int getfunc_edge_num(){
		return this.func_edge_num;
	}
	
	public String getlen(){
		return this.len;
	}
	
	public int getMcCabe(){
		return this.McCabe;
	}
	
	public String getWords(){
		return this.words;
	}
	
	public int getVoc(){
		return this.vocabulary_count;
	}
	
	public String getCapacity(){
		return this.capacity;
	}
	
	public String getAVGS(){
		return this.AVGS;
	}
	
	public String getVOCF(){
		return this.VOCF;
	}
	
	public String getCOMF(){
		return this.COMF;
	}
}
