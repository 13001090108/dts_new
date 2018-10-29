package softtest.SDataBase.c;

/** 
 * @author 
 * Miss_lizi
 * 函数的结构和功能特征
 */
public class FuncFeatures {
	private String filepath;
	private String funcname;
	private int structfeature;
	private int funcfeature;
	
	public FuncFeatures(String Info){
		String[] res = Info.split("#");
		filepath = res[0];
		funcname = res[1];
		structfeature = Integer.parseInt(res[2]);
		funcfeature = Integer.parseInt(res[3]);
	}
	
	public String getFilepath(){
		return this.filepath;
	}
	
	public String getFuncname(){
		return this.funcname;
	}
	
	public int getSF(){
		return this.structfeature;
	}
	
	public int getF(){
		return this.funcfeature;
	}
}
