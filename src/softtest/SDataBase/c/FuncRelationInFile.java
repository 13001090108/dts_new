package softtest.SDataBase.c;


/** 
 * @author 
 * Miss_lizi
 * 文件级别特征信息数据结构
 */
public class FuncRelationInFile {
	private String filepath;
	private String num;
	private String VexFeature;
	private String after;
	private String before;
	
	
	public FuncRelationInFile(String info){
		String[] res = info.split("@");
		this.filepath = res[0];
		this.num = res[1];
		if(res.length < 5){
			this.after = null;
			this.before = null;
		}else{
			this.VexFeature = res[2];
			this.after = res[3];
			this.before = res[4];
		}
		
	}
	
	public String getFilepath(){
		return this.filepath;
	}
	
	public String getNum(){
		return this.num;
	}
	
	public String getVexNum(){
		return this.VexFeature;
	}
	
	public String getAfter(){
		return this.after;
	}
	
	public String getBefore(){
		return this.before;
	}
}
