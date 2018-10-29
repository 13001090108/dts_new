package softtest.SDataBase.c;

import java.sql.*;
import java.io.*;
import java.util.*;


import softtest.database.c.DBAccess;


/** 
 * @author 
 * Miss_lizi
 * 数据库的读入和读出的接口实现
 */
public class DataBaseAccess{
	
	public static String TEMPLATE_MDB_PATH = "set\\template.Mdb";
	//public static String TEMPLATE_MDB_PATH = "..\\set\\Dbtemplate.Mdb";
	private Connection dbcon;

	public static String DBName = "testdb.mdb";
	private static int FileNum = 0;
	private static int FunNum = 0;
	private static DataBaseAccess instance;

	public static DataBaseAccess getInstance() {
		if (instance == null) {
			instance = new DataBaseAccess();
		}
		return instance;
	}
	
	/** 得到文件特征库中的总个数*/
	public static int getFileNum(){
		return FileNum;
	}
	
	/** 得到文件特征库中的总个数*/
	public static int getFunNum(){
		return FunNum;
	}
	
	/** 创建一个新的空的mdb文件 */
	public static void createMdbFile(String templateName, String pathname) {
		copyFileToFile(templateName, pathname);
		//clearData(pathname);
	}
	
	/** 文件拷贝 */
	public static void copyFileToFile(String F1, String F2) {
		// 现文件对拷,从F1拷贝到F2,若F2存在则会被覆盖;适用于任何文件.
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(F1); // 建立文件输入流
			fos = new FileOutputStream(F2);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
		} catch (FileNotFoundException ex) {
			// ex.printStackTrace();
			throw new RuntimeException("连接数据库文件 " + F1 + "出错", ex);
		} catch (IOException ex) {
			// ex.printStackTrace();
			throw new RuntimeException("连接数据库文件 " + F1 + "出错",ex);
		} finally {
			try {
				if (fis != null)
					fis.close();
				if (fos != null)
					fos.close();
			} catch (IOException ex) {
				// ex.printStackTrace();
				throw new RuntimeException("连接数据库文件 Dbtemplate.mdb出错",ex);
			}
		}
	}
	
	/** 打开默认样本库*/
	public void openDataBase() {
		if (dbcon == null) {
			dbcon = openDataBase(DBName, TEMPLATE_MDB_PATH);
		}
	}
	
	
	/** 打开数据库连接 */
	public Connection openDataBase(String dbName, String templateName) {
		Connection conn;
		File file = new File(dbName);
		if (!file.exists()) {
			DBAccess.createMdbFile(templateName, dbName);
		}
		String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
		String url = "jdbc:odbc:DRIVER=Microsoft Access Driver (*.mdb);DBQ="+ dbName;
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, "", "");
		} catch (ClassNotFoundException ex) {
			// ex.printStackTrace();
			throw new RuntimeException("Access database driver error",ex);
		} catch (SQLException ex) {
			 ex.printStackTrace();
			throw new RuntimeException("Access database connect error4");
		}
		return conn;
	}

	/** 根据指定路径打开数据库*/
	public void openDataBase(String dbName) {
		if (dbcon == null) {
			dbcon = openDataBase(dbName, TEMPLATE_MDB_PATH);
		}
	}
	
	/** 关闭数据库连接 */
	public void closeDataBase() {
		if (dbcon != null) {
			try {
				dbcon.close();
			} catch (SQLException ex) {
				// ex.printStackTrace();
				throw new RuntimeException("Access database connect error6",ex);
			}
		}
		dbcon = null;
	}
	
	/** 向数据库读入文件特征信息*/
	public void insertFileInfo(String Fileinfo) {
//		if(Fileinfo == null){
//			return;
//		}
		//System.out.println(Fileinfo);
		
		FuncRelationInFile fi = new FuncRelationInFile(Fileinfo);
		if(!fi.getNum().equals("0")){
			//System.out.println(fi.getNum());
			Statement select = null;
			PreparedStatement pstmt = null;
			if (dbcon == null) {
				throw new RuntimeException("database connection closed.");
			}
			try {
				String strSQL="";
				// 预编译指令
				//strSQL="insert into feature (文件路径，调用图的点数，调用图的边数) values (?,?,?,?,?,?,?,?,?,?)";
				strSQL="insert into FileInfo (FilePath,VexNum,VexFeature,RelationAfter,RelationBefore) values (?,?,?,?,?)";
				//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
				pstmt = dbcon.prepareStatement(strSQL);
				// 设置插入的值
				pstmt.setString(1, fi.getFilepath());
				pstmt.setString(2, fi.getNum());
				pstmt.setString(3, fi.getVexNum());
				pstmt.setString(4, fi.getAfter());
				pstmt.setString(5, fi.getBefore());
				pstmt.executeUpdate();
			} catch (SQLException ex) {
				ex.printStackTrace();
				throw new RuntimeException("Access database connect error14",ex);
			} finally {
				if (select != null) {
					try {
						select.close();
					} catch (SQLException ex) {
						// ex.printStackTrace();
						throw new RuntimeException("Access database connect error15",ex);
					}
				}
				if (pstmt != null) {
					try {
						pstmt.close();
					} catch (SQLException ex) {
						// ex.printStackTrace();
						throw new RuntimeException("Access database connect error16",ex);
					}
				}
			}
		}
		
	}
	
	
	/** 向数据库中写入函数特征信息*/
	public void insertFuncInfo(List<String> func_list ,boolean flag){
		if(func_list == null){
			return;
		}else{
			for(String funcinfn : func_list){
				if(flag == false){
					insertFuncInfo(funcinfn);
				}else{
					insertFuncInfo_danger(funcinfn);
				}			
			}
		}
	}
	
	
	
	/** 向数据库读入函数特征信息*/
	public void insertFuncInfo(String Fileinfo) {
//		if(Fileinfo == null){
//			return;
//		}
		//System.out.println(Fileinfo);
		
		FuncInfo fi = new FuncInfo(Fileinfo);
		Statement select = null;
		PreparedStatement pstmt = null;
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// 预编译指令
			//strSQL="insert into feature (文件路径，调用图的点数，调用图的边数) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into FuncInfo (FilePath,FuncName,StructFeature,FuncFeature,IsDanger,StartLine,EndLine) values (?,?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = dbcon.prepareStatement(strSQL);
			// 设置插入的值
			pstmt.setString(1, fi.getFilepath());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setString(3, fi.getStruct());
			pstmt.setString(4, fi.getFunction());
			pstmt.setBoolean(5, false);
			pstmt.setString(6, fi.getStartLine());
			pstmt.setString(7, fi.getEndLine());
			pstmt.executeUpdate();
			//FileNum++;
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Access database connect error14",ex);
		} finally {
			if (select != null) {
				try {
					select.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error15",ex);
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error16",ex);
				}
			}
		}
		
	}
	

	/** 向数据库读入函数特征信息*/
	public void insertFuncInfo_danger(String Fileinfo) {
//		if(Fileinfo == null){
//			return;
//		}
		//System.out.println(Fileinfo);
		
		FuncInfo fi = new FuncInfo(Fileinfo);
		Statement select = null;
		PreparedStatement pstmt = null;
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// 预编译指令
			//strSQL="insert into feature (文件路径，调用图的点数，调用图的边数) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into FuncInfo (FilePath,FuncName,StructFeature,FuncFeature,IsDanger,StartLine,EndLine) values (?,?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = dbcon.prepareStatement(strSQL);
			// 设置插入的值
			pstmt.setString(1, fi.getFilepath());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setString(3, fi.getStruct());
			pstmt.setString(4, fi.getFunction());
			pstmt.setBoolean(5, true);
			pstmt.setString(6, fi.getStartLine());
			pstmt.setString(7, fi.getEndLine());
			pstmt.executeUpdate();
			//FileNum++;
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Access database connect error14",ex);
		} finally {
			if (select != null) {
				try {
					select.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error15",ex);
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error16",ex);
				}
			}
		}
		
	}
	
//	/** 向数据库读入函数特征信息*/
//	public void insertFuncInfo(List<String> funclist){
//		if(funclist == null){
//			return;
//		}else{
//			for(String funcinfn : funclist){
//				insert(funcinfn);
//			}
//		}
//	}
//	
//	/** 向数据库读入函数特征信息*/
//	public void insert(String FuncInfo) {
//		if(FuncInfo == null){
//			return;
//		}
//		FuncInformation fi = new FuncInformation(FuncInfo);
//		//System.out.println(fi.getEdge_num());
//		Statement select = null;
//		PreparedStatement pstmt = null;
//		if (dbcon == null) {
//			throw new RuntimeException("database connection closed.");
//		}
//		try {
//			String strSQL="";
//			// 预编译指令
//			//strSQL="insert into feature (文件路径，调用图的点数，调用图的边数) values (?,?,?,?,?,?,?,?,?,?)";
//			strSQL="insert into FuncInfo (Funcname,Func_Vexnum,Func_Edgenum,Len,McCabe,Words,Vocabulary_count,Capacity,AVGS,VOCF,COMF,FilePath) values (?,?,?,?,?,?,?,?,?,?,?,?)";
//			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
//			pstmt = dbcon.prepareStatement(strSQL);
//			// 设置插入的值
//			pstmt.setString(1, fi.getfuncname());
//			pstmt.setInt(2, fi.getfunc_vex_num());
//			pstmt.setInt(3, fi.getfunc_edge_num());
//			pstmt.setString(4, fi.getlen());
//			pstmt.setInt(5,fi.getMcCabe());
//			pstmt.setString(6,fi.getWords());
//			pstmt.setInt(7, fi.getVoc());
//			pstmt.setString(8,fi.getCapacity());
//			pstmt.setString(9,fi.getAVGS());
//			pstmt.setString(10,fi.getVOCF());
//			pstmt.setString(11,fi.getCOMF());
//			pstmt.setString(12,fi.getfilepath());
//			pstmt.executeUpdate();
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			throw new RuntimeException("Access database connect error14",ex);
//		} finally {
//			if (select != null) {
//				try {
//					select.close();
//				} catch (SQLException ex) {
//					// ex.printStackTrace();
//					throw new RuntimeException("Access database connect error15",ex);
//				}
//			}
//			if (pstmt != null) {
//				try {
//					pstmt.close();
//				} catch (SQLException ex) {
//					// ex.printStackTrace();
//					throw new RuntimeException("Access database connect error16",ex);
//				}
//			}
//		}
//	}
	
	
	/** 向数据库中写入语句块特征信息*/
	public void insertStmsInfo(List<String> stms_list,boolean flag){
		if(stms_list == null){
			return;
		}else{
			for(String funcinfn : stms_list){
				if(flag == true){
					insertStmsFeatures_danger(funcinfn);
				}else{
					insertStmsFeatures(funcinfn);
				}
			}
		}
	}
	
	
	
	
	
	public void insertStmsFeatures(String StmsInfo){
//		if(StmsInfo == null){
//			return;
//		}
		StmsFeatures fi = new StmsFeatures(StmsInfo);
		//System.out.println(fi.getFuncname());
		Statement select = null;
		PreparedStatement pstmt = null;
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// 预编译指令
			//strSQL="insert into feature (文件路径，调用图的点数，调用图的边数) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into StmsInfo (FilePath,Function,StartLine,EndLine,Features,IsDanger) values (?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = dbcon.prepareStatement(strSQL);
			// 设置插入的值
			pstmt.setString(1, fi.getFilename());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setInt(3, fi.getStartline());
			pstmt.setInt(4, fi.getEndLine());
			pstmt.setInt(5,fi.getFeatures());
			pstmt.setBoolean(6, false);
			pstmt.executeUpdate();
			//FileNum++;
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Access database connect error14",ex);
		} finally {
			if (select != null) {
				try {
					select.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error15",ex);
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error16",ex);
				}
			}
		}
	}
	
	public void insertStmsFeatures_danger(String StmsInfo){
//		if(StmsInfo == null){
//			return;
//		}
		StmsFeatures fi = new StmsFeatures(StmsInfo);
		//System.out.println(fi.getFuncname());
		Statement select = null;
		PreparedStatement pstmt = null;
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// 预编译指令
			//strSQL="insert into feature (文件路径，调用图的点数，调用图的边数) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into StmsInfo (FilePath,Function,StartLine,EndLine,Features,IsDanger) values (?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = dbcon.prepareStatement(strSQL);
			// 设置插入的值
			pstmt.setString(1, fi.getFilename());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setInt(3, fi.getStartline());
			pstmt.setInt(4, fi.getEndLine());
			pstmt.setInt(5,fi.getFeatures());
			pstmt.setBoolean(6, true);
			pstmt.executeUpdate();
			//FileNum++;
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Access database connect error14",ex);
		} finally {
			if (select != null) {
				try {
					select.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error15",ex);
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error16",ex);
				}
			}
		}
	}
	
	/** 向数据库中写入程序级别特征信息*/
	public void insertProjFeatures(List<String> Info){
		ProjFeatures pf = new ProjFeatures(Info);
		//System.out.println(fi.getFuncname());
		Statement select = null;
		PreparedStatement pstmt = null;
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// 预编译指令
			strSQL="insert into ProjInfo (ProjPath,FileOrder,FuncFeatures,RelationAfter,RelationBefore,independent) values (?,?,?,?,?,?)";
			pstmt = dbcon.prepareStatement(strSQL);
			// 设置插入的值
			pstmt.setString(1, pf.getProjpath());
			pstmt.setString(2, pf.getFileorder());
			pstmt.setString(3, pf.getFeature());
			pstmt.setString(4, pf.getAfter());
			pstmt.setString(5,pf.getBefore());
			pstmt.setString(6, pf.getIndept());
			pstmt.executeUpdate();
			//FileNum++;
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Access database connect error14",ex);
		} finally {
			if (select != null) {
				try {
					select.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error15",ex);
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Access database connect error16",ex);
				}
			}
		}
	}
	
	
	
	//************************以下是所有读取相关*******************************
	
	public List<List<String>> readFunRes(double a, double b){
		String strSQL="SELECT * FROM FuncInfo where StructFeature = '" + Double.toString(b) + "' and FuncFeature = '" + Double.toString(a) + "'";
		return readFunRes(a, b, strSQL);
	}
	public List<List<String>> readFunResWithVulnerability(double a, double b){
		String strSQL="SELECT * FROM FuncInfo where IsDanger = false and StructFeature = '" + Double.toString(b) + "' and FuncFeature = '" + Double.toString(a) + "'";
		return readFunRes(a, b, strSQL);
	}
	public List<List<String>> readFunRes(double a, double b,String str){
		PreparedStatement select = null;
		List<List<String>> res = new LinkedList<>();
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="SELECT * FROM FuncInfo where StructFeature = '" + Double.toString(b) + "' and FuncFeature = '" + Double.toString(a) + "'";
			select = dbcon.prepareStatement(strSQL);
			ResultSet rs = select.executeQuery();
			ResultSetMetaData md = rs.getMetaData(); //获得结果集结构信息,元数据  
		    while (rs.next()) {
		    	List<String> tmp = new LinkedList<>();
		    	//tmp.add(rs.getString("FilePath"));
		    	//tmp.add(rs.getString("FuncName"));
		    	tmp.add(rs.getObject(2).toString());
		    	tmp.add(rs.getObject(3).toString());
	            res.add(tmp);  
	        }  
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Access database connect error8",e);
		}
		finally {
			if (select != null) {
				try { select.close(); } catch (Exception e) {}
			}
		}
		return res;
	}
	
	public List<LinkedList<String>> readBlockRes(int a){
		String strSQL="SELECT * FROM StmsInfo where Features = " + Integer.toString(a);
		return readBlockRes(a,strSQL);
	}
	public List<LinkedList<String>> readBlockResWithVulnerability(int a){
		String strSQL="SELECT * FROM StmsInfo where Features = " + Integer.toString(a)
				+ " and IsDanger = true";
		return readBlockRes(a,strSQL);
	}
	public List<LinkedList<String>> readBlockRes(int a,String str){
		PreparedStatement select = null;
		List<LinkedList<String>> res = new LinkedList<>();
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			select = dbcon.prepareStatement(str);
			ResultSet rs = select.executeQuery();
		    while (rs.next()) {  
		    	LinkedList<String> data = new LinkedList<String>();
		    	data.add(rs.getString("FilePath"));
		    	data.add(rs.getString("StartLine"));
		    	data.add(rs.getString("EndLine"));
//		    	data.add(rs.getObject(2).toString());
//		    	data.add(rs.getObject(4).toString());
//		    	data.add(rs.getObject(5).toString());
	            res.add(data);  
	        }  
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Access database connect error8",e);
		}
		finally {
			if (select != null) {
				try { select.close(); } catch (Exception e) {}
			}
		}
		return res;
	}
	/**获取总条数**/
	public int readFileCount(){
		PreparedStatement select = null;
		int res = 0;
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="SELECT Count(*) FROM FileInfo";
			select = dbcon.prepareStatement(strSQL);
			ResultSet rs = select.executeQuery();
			if(rs.next()){
				res = rs.getInt(1);
			}
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Access database connect error8",e);
		}
		finally {
			if (select != null) {
				try { select.close(); } catch (Exception e) {}
			}
		}
		return res;
	}
	/**对文件特征值进行批量读取， 从start行一直读到 start+count行**/
	public List<LinkedList<String>> readFileRes(int start,int step){
		PreparedStatement select = null;
		List<LinkedList<String>> res = new LinkedList<>();
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="SELECT * FROM FileInfo where Num >= " + Integer.toString(start) +"and"
					+ " Num < " + Integer.toString(start+step);
			select = dbcon.prepareStatement(strSQL);
			ResultSet rs = select.executeQuery();
		    while (rs.next()) {  
		    	LinkedList<String> tmp = new LinkedList<String>();
		    	tmp.add(rs.getString("FilePath"));
		    	tmp.add(rs.getString("VexNum"));
		    	tmp.add(rs.getString("VexFeature"));
		    	tmp.add(rs.getString("RelationAfter"));
		    	tmp.add(rs.getString("RelationBefore"));
	            res.add(tmp);  
	        }  
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Access database connect error8",e);
		}
		finally {
			if (select != null) {
				try { select.close(); } catch (Exception e) {}
			}
		}
		return res;
	}
	
	
	/**根据文件的路径，读取存储在数据库中的文件的特征值**/
	public LinkedList<String> readFileRes(String filePath){
		PreparedStatement select = null;
		LinkedList<String> res = new LinkedList<>();
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="SELECT * FROM FileInfo where FilePath = '" + filePath + "'";
			select = dbcon.prepareStatement(strSQL);
			ResultSet rs = select.executeQuery();
		    while (rs.next()) {  
		    	res.add(rs.getString("FilePath"));
		    	res.add(rs.getString("VexNum"));
		    	res.add(rs.getString("VexFeature"));
		    	res.add(rs.getString("RelationAfter"));
		    	res.add(rs.getString("RelationBefore"));
	        }  
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Access database connect error8",e);
		}
		finally {
			if (select != null) {
				try { select.close(); } catch (Exception e) {}
			}
		}
		return res;
	}
	
	/**读取程序表中，程序的最大编号，这个目前好像没用了，后期看看如果不需要的话，是可以删除掉的**/
	public int maxProID(){
		PreparedStatement select = null;
		int res = 0;
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="SELECT max(ID) FROM ProjInfo";
			select = dbcon.prepareStatement(strSQL);
			ResultSet rs = select.executeQuery();
		    while (rs.next()) {  
	            res = rs.getInt(1);  
	        }  
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Access database connect error8",e);
		}
		finally {
			if (select != null) {
				try { select.close(); } catch (Exception e) {}
			}
		}
		return res;
	}
	/**读取程序的文件调用序列，同样可以采取批量读取的方法。**/
	public List<LinkedList<String>> readProRes(int start,int step){
		PreparedStatement select = null;
		List<LinkedList<String>> res = new LinkedList<LinkedList<String>>();
		if (dbcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="SELECT * FROM ProjInfo where ID >= " + Integer.toString(start) +" and"
					+ " ID < " + Integer.toString(start+step);
			select = dbcon.prepareStatement(strSQL);
			ResultSet rs = select.executeQuery();
		    while (rs.next()) {
		    	LinkedList<String> tmp = new LinkedList<String>();
		    	tmp.add(rs.getString("ProjPath"));
		    	tmp.add(rs.getString("FileOrder"));
		    	tmp.add(rs.getString("FuncFeatures"));
		    	tmp.add(rs.getString("RelationAfter"));
		    	tmp.add(rs.getString("RelationBefore"));
		    	tmp.add(rs.getString("independent"));
	            res.add(tmp);  
	        }  
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Access database connect error8",e);
		}
		finally {
			if (select != null) {
				try { select.close(); } catch (Exception e) {}
			}
		}
		return res;
	}

	
}
