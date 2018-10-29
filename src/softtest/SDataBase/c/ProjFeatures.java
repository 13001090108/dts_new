package softtest.SDataBase.c;

import java.util.List;

/** 
 * @author 
 * Miss_lizi
 * 程序级别特征信息
 */

public class ProjFeatures {
	private String projpath = null;
	private String fileorder = null;
	private String features = null;
	private String after = null;
	private String before = null;
	private String indept = null;
	
	public ProjFeatures(List<String> info){
		fileorder = info.get(0);
		features = info.get(1);
		after = info.get(2);
		before = info.get(3);
		indept = info.get(4);
		projpath = info.get(5);
	}
	
	public String getProjpath(){
		return this.projpath;
	}
	
	public String getFileorder(){
		return this.fileorder;
	}
	
	public String getFeature(){
		return this.features;
	}
	
	public String getAfter(){
		return this.after;
	}
	
	public String getBefore(){
		return this.before;
	}
	
	public String getIndept(){
		return this.indept;
	}
}
