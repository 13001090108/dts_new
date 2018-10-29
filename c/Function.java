package softtest.SimDetection.c;

import java.util.LinkedList;
import java.util.List;

import softtest.CharacteristicExtract.c.StatementFeature;
import softtest.SDataBase.c.DataBaseAccess;

public class Function {
	String filePath;
	String funcName;
	double funFea;
	double structFea;
	public Function(){};
	public Function(String str){
		String tmp[] = str.split("#");
		if(tmp.length != 4){
			System.out.println("º¯Êý³õÊ¼»¯Ê§°Ü");
		}
		else{
			this.filePath = tmp[0];
			this.funcName = tmp[1];	
			this.funFea = Double.valueOf(tmp[3]);
			this.structFea = Double.valueOf(tmp[2]);
		}
	}
	
	public boolean dection(Function f){
		return this.funFea == f.funFea && this.structFea == f.structFea;
	}
	
	public List<List<String>> dection() throws Exception{
		List<List<String>> res  = new LinkedList<>();
		DataBaseAccess db = new DataBaseAccess().getInstance();
		db.openDataBase();
		res = db.readFunRes(this.funFea, this.structFea);
		db.closeDataBase();
		return res;
	}
}
