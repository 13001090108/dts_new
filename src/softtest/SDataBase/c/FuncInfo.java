package softtest.SDataBase.c;

/** 
 * @author 
 * Miss_lizi
 * 函数的结构和功能特征
 */
public class FuncInfo {
	private String filepath;
	private String funcname;
	private String struct;
	private String function;
	private String startline;
	private String endline;
	
	public FuncInfo(String fileinfo){
		String[] res = fileinfo.split("#");
		filepath = res[0];
		funcname = res[1];
		struct = res[2];
		function = res[3];
		startline = res[4];
		endline = res[5]; 
	}
	
	public String getFilepath(){
		return this.filepath;
	}
	
	public String getFuncname(){
		return this.funcname;
	}
	
	public String getStruct(){
		return this.struct;
	}
	
	public String getFunction(){
		return this.function;
	}
	
	public String getStartLine(){
		return this.startline;
	}
	
	public String getEndLine(){
		return this.endline;
	}
	
}
