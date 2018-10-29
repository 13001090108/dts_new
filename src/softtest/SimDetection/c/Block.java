package softtest.SimDetection.c;

import java.util.*;

import softtest.CharacteristicExtract.c.StatementFeature;
import softtest.SDataBase.c.*;

/**语句块级别的相似性判定
 * by bubu
 * */

public class Block {
	String filePath;
	String funcName;
	int beginLine = 0;
	int endLine = 0;
	String feature = "";
	public Block(){};
	public Block(String blockInfor){
		String[]blockFeatures = blockInfor.split("#");
		this.filePath = blockFeatures[0];
		this.funcName = blockFeatures[1];
		this.beginLine = Integer.valueOf(blockFeatures[2]);
		this.endLine = Integer.valueOf(blockFeatures[3]);
		this.feature = blockFeatures[4];
	}
	public boolean dection(Block b){
		return this.feature == b.feature;
	}
	public List<LinkedList<String>> dection() throws Exception{
		List<LinkedList<String>> res  = new LinkedList<>();
		if(feature.equals("")){
			//System.out.println("语句块不符合规范，不进行测试");
			return res;
		}
		//System.out.println("正在生成语句块的特征值为" + feature);
		DataBaseAccess db = new DataBaseAccess().getInstance();
		db.openDataBase();
		//System.out.println("正在查找......");
		res = db.readBlockRes(Integer.valueOf(feature));
		db.closeDataBase();
		return res;
	}
}
