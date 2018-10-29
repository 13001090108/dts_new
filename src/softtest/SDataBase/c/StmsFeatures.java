package softtest.SDataBase.c;



/** 
 * @author 
 * Miss_lizi
 * 语句块级别特征信息数据结构
 */
public class StmsFeatures {
	private String filename;
	private String funcname;
	private int startline;
	private int endline;
	private int features;
//	private boolean isdanger = false;
	
	public StmsFeatures(String info){
		String[] res = info.split("#");
		filename = res[0];
		funcname = res[1];
		startline = Integer.valueOf(res[2]);
		endline = Integer.valueOf(res[3]);
		features = Integer.valueOf(res[4]);
//		if(!res[5].equals("false")){
//			isdanger = true;
//		}
	}
	
	public String getFilename(){
		return this.filename;
	}
	
	public String getFuncname(){
		return this.funcname;
	}
	
	public int getStartline(){
		return this.startline;
	}
	
	public int getEndLine(){
		return this.endline;
	}
	
	public int getFeatures(){
		return this.features;
	}
	
//	public boolean getIsDanger(){
//		return this.isdanger;
//	}
}
