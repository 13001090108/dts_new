package softtest.SDataBase.c;

/** 
 * @author 
 * Miss_lizi
 * 文件级别的特征信息数据结构
 */
public class FileInformatica {
	private String filename;
	private int vex_num;
	private int edge_num;
	private int McCabe;
	
	public FileInformatica(String fileinfo) {
		String[] res = fileinfo.split("#");
		this.filename = res[0];
		this.vex_num =  Integer.valueOf(res[1]);
		this.edge_num = Integer.valueOf(res[2]);
		this.McCabe = Integer.valueOf(res[3]);
	}
	
	public String getFilename(){
		return this.filename;
	}
	
	public int getVex_num(){
		return this.vex_num;
	}
	
	public int getEdge_num(){
		return this.edge_num;
	}
	
	public int getMcCabe(){
		return this.McCabe;
	}
}
