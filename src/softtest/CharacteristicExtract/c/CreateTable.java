package softtest.CharacteristicExtract.c;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import softtest.SDataBase.c.FuncInfo;
import softtest.SDataBase.c.FuncRelationInFile;
import softtest.SDataBase.c.ProjFeatures;
import softtest.SDataBase.c.StmsFeatures;



public class CreateTable {


	private  Connection connect;
	private static CreateTable instance;

	public static CreateTable getInstance() {
		if (instance == null) {
			instance = new CreateTable();
		}
		return instance;
	}
	/***��Ĭ�ϵ����ݿ⣬ Ĭ��ʵ������ݿ�Ϊdtstest*/
	public void openDataBase() throws SQLException {
		if (connect == null) {
			connect = openDataBase("dtstest");
		}
	}
	
	/***��ָ�����Ƶ����ݿ�*/
	public Connection openDataBase(String dbName) {
		Connection conn = null;
		String driver = "com.mysql.cj.jdbc.Driver";
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection("jdbc:mysql://10.109.252.104/FeaturesDB","root",dbName);
			System.out.println("Success connect Mysql server!");
		} catch (Exception e) {
			System.out.print("Fail connect Mysql server!");
			e.printStackTrace();
		}
		return conn;
	}
	/** �ر����ݿ����� */
	public void closeDataBase() {
		if (connect != null) {
			try {
				connect.close();
			} catch (SQLException ex) {
				System.out.print("Fail close Mysql server!");
				ex.printStackTrace();
			}
		}
		connect = null;
	}
	public static void main(String args[]) throws SQLException {
		CreateTable dbtest = new CreateTable();
		dbtest.openDataBase();
//		for(int i = 0; i < 100; i++){
//			for(int j = 0; j < 100; j++){
//				dbtest.createTable_Functable_10000(i,j);
//			}
//		}
		System.out.println("The sum of Function is : ");
		dbtest.search("Functable");
		System.out.println("The sum of Project is : ");
		dbtest.search("Projtable");
		System.out.println("The sum of File is : ");
		dbtest.search("Filetable");
		int sum = 0;
		for(int i = 0; i < 100; i++){
			sum += dbtest.search_stms(String.valueOf(i));
		}
		System.out.println(sum);
		
		//dbtest.insertStmsFeatures_test("filename#funcname#3#5#25");
		//dbtest.createTable("MutiStmsTable");
		
		//dbtest.clearAll_Stms();
		//dbtest.clearAll_Stms();
		//dbtest.changename();
//		dbtest.createTable_Filetable();
//		dbtest.createTable_Functable();
//		dbtest.createTable_Projtable();
//		for(int i = 0; i < 100; i++){
//			String str1 = "alter table stms" + i + " add ID int";
//			String str = " alter table stms" + i + " add primary key(ID)";
//			dbtest.insert(str1);
//			dbtest.insert(str);
//		}
		//dbtest.createTable_stmstable("stms2");
		//String insert="insert into testdb_stms (username,password) values('xiaoming','1234567')"; 
		
		dbtest.closeDataBase();
	}
	
	
	public void search(String tablename){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}try{
			Statement stmt = connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
			ResultSet rset = stmt.executeQuery("select * from " + tablename);
			rset.last();
			int rowCount = rset.getRow(); 
			System.out.println(rowCount);
		}
		catch (SQLException e) {
			System.out.println("Fail to search table.");
		}

	}
	
	
	public int search_stms(String str){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}try{
			Statement stmt = connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
			ResultSet rset = stmt.executeQuery("select * from stms" + str);
			rset.last();
			return rset.getRow();
		}
		catch (SQLException e) {
			//System.out.println("Fail to search table.");
			return 0;
		}

	}
	
	/**��������������*/
	public void createTable_stmstable(String tabName){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}
		PreparedStatement ps = null;
		try {
		String tableSql = "create table " + tabName + " (FilePath VARCHAR(100), Function VARCHAR(50), StartLine int, EndLine int, "
				+ "Features int, IsDanger bool)";    
		//Statement smt = connect.createStatement();
		ps = connect.prepareStatement(tableSql);
		ps.executeUpdate();
		System.out.println("Success creat table " + tabName);
		} catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Fail close Mysql server!",ex);
				}
			}
		}
	}
	
	
	/**��������������*/
	public void createTable_Functable(){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}
		PreparedStatement ps = null;
		try {
		String tableSql = "create table  Functabletest  (FilePath VARCHAR(100), Function VARCHAR(50), StructFeature int, FuncFeature int, "
				+ " IsDanger bool, StartLine int, EndLine int)";    
		//Statement smt = connect.createStatement();
		ps = connect.prepareStatement(tableSql);
		ps.executeUpdate();
		System.out.println("Success creat table ");
		} catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Fail close Mysql server!",ex);
				}
			}
		}
	}
	
	/**�����ļ�������*/
	public void createTable_Filetable(){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}
		PreparedStatement ps = null;
		try {
		String tableSql = "create table Filetabletest (FilePath VARCHAR(100), VexNum int, VexFeature VARCHAR(100), RelationAfter VARCHAR(50), "
				+ "  RelationBefore VARCHAR(50))";    
		//Statement smt = connect.createStatement();
		ps = connect.prepareStatement(tableSql);
		ps.executeUpdate();
		System.out.println("Success creat table " );
		} catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Fail close Mysql server!",ex);
				}
			}
		}
	}
	
	/**��������������*/
	public void createTable_Projtablenull(){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}
		PreparedStatement ps = null;
		try {
		String tableSql = "create table ProjtableNull  (FilePath VARCHAR(100), FileOrder VARCHAR(5000) , FuncFeatures VARCHAR(1000), "
				+ "  RelationAfter VARCHAR(100), RelationBefore VARCHAR(100), independent VARCHAR(1000))";    
		//Statement smt = connect.createStatement();
		ps = connect.prepareStatement(tableSql);
		ps.executeUpdate();
		System.out.println("Success creat table " );
		} catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Fail close Mysql server!",ex);
				}
			}
		}
	}
	
	/**��������������*/
	public void createTable_Projtable(){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}
		PreparedStatement ps = null;
		try {
		String tableSql = "create table Projtable  (FilePath VARCHAR(100), FileOrder VARCHAR(5000) , FuncFeatures VARCHAR(1000), "
				+ "  RelationAfter VARCHAR(100), RelationBefore VARCHAR(100), independent VARCHAR(1000))";    
		//Statement smt = connect.createStatement();
		ps = connect.prepareStatement(tableSql);
		ps.executeUpdate();
		System.out.println("Success creat table " );
		} catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Fail close Mysql server!",ex);
				}
			}
		}
	}
	
	
	
	public void createTable(String tabName) {
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}
		PreparedStatement ps = null;
		try {
		String tableSql = "create table " + tabName + " (username varchar(50) not null primary key,"    
					 + "password varchar(20) not null ); ";
		//Statement smt = connect.createStatement();
		ps = connect.prepareStatement(tableSql);
		ps.executeUpdate();
		System.out.println("Success creat table " + tabName);
		} catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Fail close Mysql server!",ex);
				}
			}
		}
	}
	
	public void clearAll_Stms(){
		
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}try{
			Statement stmt= connect.createStatement();//����һ��Statement����
//			for(int i = 0; i < 100; i++){
//				String str = "truncate table stms" + i;
//				stmt.executeUpdate(str);//ִ��sql���
//			}
			String str = "truncate table Projtable";
			//String str1 = "truncate table Filetable";
			//String str2 = "truncate table Projtable";
			stmt.executeUpdate(str);
			//stmt.executeUpdate(str1);
			//stmt.executeUpdate(str2);
		}
		catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} 
	}

	public void insert(String str){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}try{
		//String insert="insert into testdb_stms (username,password) values('name','123456')";        
		Statement stmt= connect.createStatement();//����һ��Statement����
		stmt.executeUpdate(str);//ִ��sql���
		}
		catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} 
	}
	
	/** �����ݿ���д������������Ϣ*/
	public void insert_Stmstable(List<String> stms_list,boolean flag){
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
		int tag = fi.getFeatures()%100;
		Statement select = null;
		PreparedStatement pstmt = null;
		if (connect == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// Ԥ����ָ��
			//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into stms" + tag + " (FilePath,Function,StartLine,EndLine,Features,IsDanger) values (?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = connect.prepareStatement(strSQL);
			// ���ò����ֵ
			pstmt.setString(1, fi.getFilename());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setInt(3, fi.getStartline());
			pstmt.setInt(4, fi.getEndLine());
			pstmt.setInt(5,fi.getFeatures());
			pstmt.setBoolean(6, false);
			pstmt.executeUpdate();
			fi = null;
			pstmt = null;
			//FileNum++;
		} catch (SQLException e) {
			//System.out.println("Fail to insert stmstable" + e.getMessage());
		}
	}
	
	public void insertMutiFeatures(String StmsInfo){
//		if(StmsInfo == null){
//			return;
//		}
		StmsFeatures fi = new StmsFeatures(StmsInfo);
		PreparedStatement pstmt = null;
		if (connect == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// Ԥ����ָ��
			//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into MutiStmsTable (FilePath,Function,StartLine,EndLine,Features,IsDanger) values (?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = connect.prepareStatement(strSQL);
			// ���ò����ֵ
			pstmt.setString(1, fi.getFilename());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setInt(3, fi.getStartline());
			pstmt.setInt(4, fi.getEndLine());
			pstmt.setInt(5,fi.getFeatures());
			pstmt.setBoolean(6, false);
			pstmt.executeUpdate();
			fi = null;
			pstmt = null;
			//FileNum++;
		} catch (SQLException e) {
			//System.out.println("Fail to insert stmstable" + e.getMessage());
		}
	}
	
	public void insertStmsFeatures_test(String StmsInfo){
//		if(StmsInfo == null){
//			return;
//		}
		StmsFeatures fi = new StmsFeatures(StmsInfo);
		int tag = fi.getFeatures()%100;
		Statement select = null;
		PreparedStatement pstmt = null;
		if (connect == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// Ԥ����ָ��
			//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into stms21 (FilePath,Function,StartLine,EndLine,Features,IsDanger) values (?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = connect.prepareStatement(strSQL);
			// ���ò����ֵ
			pstmt.setString(1, fi.getFilename());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setInt(3, fi.getStartline());
			pstmt.setInt(4, fi.getEndLine());
			pstmt.setInt(5,fi.getFeatures());
			pstmt.setBoolean(6, false);
			pstmt.executeUpdate();
			System.out.println("�������ݳɹ�");
			fi = null;
			pstmt = null;
			//FileNum++;
		} catch (SQLException e) {
			//System.out.println("Fail to insert stmstable" + e.getMessage());
		}
	}
	
	public void insertStmsFeatures_danger(String StmsInfo){
//		if(StmsInfo == null){
//			return;
//		}
		StmsFeatures fi = new StmsFeatures(StmsInfo);
		int tag = fi.getFeatures()%100;
		Statement select = null;
		PreparedStatement pstmt = null;
		if (connect == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// Ԥ����ָ��
			//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into stms" + tag + " (FilePath,Function,StartLine,EndLine,Features,IsDanger) values (?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = connect.prepareStatement(strSQL);
			// ���ò����ֵ
			pstmt.setString(1, fi.getFilename());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setInt(3, fi.getStartline());
			pstmt.setInt(4, fi.getEndLine());
			pstmt.setInt(5,fi.getFeatures());
			pstmt.setBoolean(6, true);
			pstmt.executeUpdate();
			fi = null;
			pstmt = null;
			//FileNum++;
		} catch (SQLException e) {
			//System.out.println("Fail to insert stmstable" + e.getMessage());
		} 
	}
	
	/** �����ݿ���д�뺯��������Ϣ*/
	public void insert_Functable(List<String> func_list ,boolean flag){
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
	public void insertFuncInfo(String Fileinfo) {	
		FuncInfo fi = new FuncInfo(Fileinfo);
		PreparedStatement pstmt = null;
		if (connect == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// Ԥ����ָ��
			//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
			strSQL="insert into Functable (FilePath,Function,StructFeature,FuncFeature,IsDanger,StartLine,EndLine) values (?,?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = connect.prepareStatement(strSQL);
			// ���ò����ֵ
			pstmt.setString(1, fi.getFilepath());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setString(3, fi.getStruct());
			pstmt.setString(4, fi.getFunction());
			pstmt.setBoolean(5, false);
			pstmt.setString(6, fi.getStartLine());
			pstmt.setString(7, fi.getEndLine());
			pstmt.executeUpdate();
			fi = null;
			pstmt = null;
			//FileNum++;
		} catch (SQLException e) {
			//System.out.println("Fail to insert functable" + e.getMessage());
		}
		
	}
	
	/**��������������*/
	public void createTable_Functable_10000(int i, int j){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}
		PreparedStatement ps = null;
		try {
		String tableSql = "create table  Functable_" + i + "_" + j
				+ " (ID int NOT NULL PRIMARY KEY AUTO_INCREMENT,"
				+ " FilePath VARCHAR(100), Function VARCHAR(50), StructFeature VARCHAR(50), FuncFeature VARCHAR(50), "
				+ " IsDanger bool, StartLine int, EndLine int)";    
		//Statement smt = connect.createStatement();
		ps = connect.prepareStatement(tableSql);
		ps.executeUpdate();
		System.out.println("Success creat table ");
		} catch (SQLException e) {
			System.out.println("Fail to creat table : " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					// ex.printStackTrace();
					throw new RuntimeException("Fail close Mysql server!",ex);
				}
			}
		}
	}


	
	public void insertFuncInfo(String Fileinfo, boolean flag) {	
		FuncInfo fi = new FuncInfo(Fileinfo);
		int i = Math.abs((int)(Double.valueOf(fi.getStruct())%100));
		int j = Math.abs((int)(Double.valueOf(fi.getFunction())%100));
		this.insertFuncInfo(Fileinfo, i, j, flag);
	}
	
	
	/** �����ݿ���д�뺯��������Ϣ*/
	public void insert_Functable_10000(List<String> func_list ,boolean flag){
		if(func_list == null){
			return;
		}else{
			for(String funcinfn : func_list){
				insertFuncInfo(funcinfn,flag);		
			}
		}
	}

	public void insertFuncInfo(String Fileinfo, int i, int j,boolean flag) {	
		FuncInfo fi = new FuncInfo(Fileinfo);
		PreparedStatement pstmt = null;
		if (connect == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL="";
			// Ԥ����ָ��
			//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
			String tableName = "Functable_" + i + "_" + j;
			strSQL="insert into " + tableName + " (FilePath,Function,StructFeature,FuncFeature,IsDanger,StartLine,EndLine) values (?,?,?,?,?,?,?)";
			//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
			pstmt = connect.prepareStatement(strSQL);
			// ���ò����ֵ
			pstmt.setString(1, fi.getFilepath());
			pstmt.setString(2, fi.getFuncname());
			pstmt.setString(3, fi.getStruct());
			pstmt.setString(4, fi.getFunction());
			pstmt.setBoolean(5, flag);
			pstmt.setString(6, fi.getStartLine());
			pstmt.setString(7, fi.getEndLine());
			pstmt.executeUpdate();
			fi = null;
			pstmt = null;
			//FileNum++;
		} catch (SQLException e) {
			//System.out.println("Fail to insert functable" + e.getMessage());
		}
		
	}
	
	
	/** �����ݿ���뺯��������Ϣ*/
		public void insertFuncInfo_danger(String Fileinfo) {
	//		if(Fileinfo == null){
	//			return;
	//		}
			//System.out.println(Fileinfo);
			
			FuncInfo fi = new FuncInfo(Fileinfo);
			Statement select = null;
			PreparedStatement pstmt = null;
			if (connect == null) {
				throw new RuntimeException("database connection closed.");
			}
			try {
				String strSQL="";
				// Ԥ����ָ��
				//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
				strSQL="insert into Functable (FilePath,Function,StructFeature,FuncFeature,IsDanger,StartLine,EndLine) values (?,?,?,?,?,?,?)";
				//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
				pstmt = connect.prepareStatement(strSQL);
				// ���ò����ֵ
				pstmt.setString(1, fi.getFilepath());
				pstmt.setString(2, fi.getFuncname());
				pstmt.setString(3, fi.getStruct());
				pstmt.setString(4, fi.getFunction());
				pstmt.setBoolean(5, true);
				pstmt.setString(6, fi.getStartLine());
				pstmt.setString(7, fi.getEndLine());
				pstmt.executeUpdate();
				fi = null;
				pstmt = null;
				//FileNum++;
			} catch (SQLException e) {
				//System.out.println("Fail to insert functable" + e.getMessage());
			} 
			
		}
	
	
		/** �����ݿ���д����򼶱�������Ϣ*/
		public void insertProjtable(List<String> Info){
			ProjFeatures pf = new ProjFeatures(Info);
			//System.out.println(fi.getFuncname());
			PreparedStatement pstmt = null;
			if (connect == null) {
				throw new RuntimeException("database connection closed.");
			}
			try {
				String strSQL="";
				// Ԥ����ָ��
				strSQL="insert into Projtable (FilePath,FileOrder,FuncFeatures,RelationAfter,RelationBefore,independent) values (?,?,?,?,?,?)";
				// ���ò����ֵ
				pstmt = connect.prepareStatement(strSQL);
				pstmt.setString(1, pf.getProjpath());
				pstmt.setString(2, pf.getFileorder());
				pstmt.setString(3, pf.getFeature());
				pstmt.setString(4, pf.getAfter());
				pstmt.setString(5,pf.getBefore());
				pstmt.setString(6, pf.getIndept());
				pstmt.executeUpdate();
				pf = null;
				pstmt = null;
				//FileNum++;
			} catch (SQLException e) {
				//System.out.println("Fail to insert projtable" + e.getMessage());
			}
		}
		
		/** �����ݿ���д����򼶱�������Ϣ*/
		public void insertProjtablenull(List<String> Info){
			ProjFeatures pf = new ProjFeatures(Info);
			//System.out.println(fi.getFuncname());
			Statement select = null;
			PreparedStatement pstmt = null;
			if (connect == null) {
				throw new RuntimeException("database connection closed.");
			}
			try {
				String strSQL="";
				if(pf.getFileorder().length() > 1){
					// Ԥ����ָ��
					strSQL="insert into ProjtableNull (FilePath,FileOrder,FuncFeatures,RelationAfter,RelationBefore,independent) values (?,?,?,?,?,?)";
					// ���ò����ֵ
					pstmt = connect.prepareStatement(strSQL);
					pstmt.setString(1, pf.getProjpath());
					pstmt.setString(2, pf.getFileorder());
					pstmt.setString(3, pf.getFeature());
					pstmt.setString(4, pf.getAfter());
					pstmt.setString(5,pf.getBefore());
					pstmt.setString(6, pf.getIndept());
					pstmt.executeUpdate();
					pstmt = null;
					pf = null;
				}
				
				//FileNum++;
			} catch (SQLException e) {
				//System.out.println("Fail to insert projtable" + e.getMessage());
			}
		}
	
		/** �����ݿ�����ļ�������Ϣ*/
		public void insertFiletable(String Fileinfo) {
//			if(Fileinfo == null){
//				return;
//			}
			//System.out.println(Fileinfo);
			
			FuncRelationInFile fi = new FuncRelationInFile(Fileinfo);
			if(!fi.getNum().equals("0")){
				//System.out.println(fi.getNum());
				Statement select = null;
				PreparedStatement pstmt = null;
				if (connect == null) {
					throw new RuntimeException("database connection closed.");
				}
				try {
					String strSQL="";
					// Ԥ����ָ��
					//strSQL="insert into feature (�ļ�·��������ͼ�ĵ���������ͼ�ı���) values (?,?,?,?,?,?,?,?,?,?)";
					strSQL="insert into Filetable (FilePath,VexNum,VexFeature,RelationAfter,RelationBefore) values (?,?,?,?,?)";
					//strSQL="insert into IP (StartLine,IPLine) values (?,?)";
					pstmt = connect.prepareStatement(strSQL);
					// ���ò����ֵ
					pstmt.setString(1, fi.getFilepath());
					pstmt.setString(2, fi.getNum());
					pstmt.setString(3, fi.getVexNum());
					pstmt.setString(4, fi.getAfter());
					pstmt.setString(5, fi.getBefore());
					pstmt.executeUpdate();
					pstmt = null;
					fi = null;
				} catch (SQLException e) {
					//System.out.println("Fail to insert filetable" + e.getMessage());
				} 
			}
			
		}
	
		
	public void changename(){
		if (connect == null) {
			throw new RuntimeException("mysql connection closed.");
		}try{
		//String insert="insert into testdb_stms (username,password) values('name','123456')";        
		Statement stmt= connect.createStatement();//����һ��Statement����
		PreparedStatement pstmt = null;
		String strSQL="";
		//int i = 21;
		//strSQL =  "rename table simdect.stms"+i+ " to FeaturesDB.stms"+i;
//		pstmt = connect.prepareStatement(strSQL);
//		pstmt.setString(0, "stms22");
//		pstmt.setString(1, "stms22");
		//pstmt.executeUpdate();
		
		
		//stmt.executeUpdate(strSQL);
		for(int i = 23; i < 100; i++){
			strSQL =  "rename table simdect.stms"+i+ " to FeaturesDB.stms"+i;
			stmt.executeUpdate(strSQL);//ִ��sql���
		}
		}
		catch (SQLException e) {
			//System.out.println("Fail to creat table : " + e.getMessage());
		} 
	}
		
	
}
