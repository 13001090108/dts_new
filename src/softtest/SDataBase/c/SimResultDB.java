package softtest.SDataBase.c;

import java.sql.*;
import java.io.*;
import java.util.*;

import javax.swing.filechooser.FileSystemView;


public class SimResultDB{
	
	
	/** start 
	 * by bubu
	 * ���Ľ�����ݿ�
	***/
	public static String FEATURE_MDB_PATH = "set\\SimDet.mdb";
	private static String FileName = "SimDet.mdb";
	private Connection resDBcon;
	public SimResultDB(){}
	
	private static SimResultDB instance;
	public static SimResultDB getInstance(String fileName) {
		if (instance == null) {
			FileName = fileName + ".mdb";
			instance = new SimResultDB();
		}
		return instance;
	}
	public static SimResultDB getInstance() {
		if (instance == null) {
			instance = new SimResultDB();
		}
		return instance;
	}
	public void openSimResultDB() {
		if (resDBcon == null) {
			File file = new File(FileName);
			if (!file.exists()) {
				SimResultDB.createSimResultDB(FEATURE_MDB_PATH);
			}
			String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
			String url = "jdbc:odbc:DRIVER=Microsoft Access Driver (*.mdb);DBQ=" + FileName;
			try {
				Class.forName(driver);
				resDBcon = DriverManager.getConnection(url, "", "");
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException("Access database driver error",ex);
			} catch (SQLException ex) {
				ex.printStackTrace();
				throw new RuntimeException("Access database connect error4");
			}
		}
	}
	public boolean insertDB(List<String> list,String str) {
		PreparedStatement select = null;
		PreparedStatement pstmt = null;
		if (resDBcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL = "";
			strSQL = str;
			pstmt = resDBcon.prepareStatement(strSQL);
			int i = 1;
			for(String s : list){
				pstmt.setString(i++,s);
			}
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
		return true;
	}
	public boolean insertBlock(List<String> list) {
		String strSQL = "insert into Block (Ŀ��FilePath,Ŀ��StartLine,Ŀ��EndLine,����FilePath,����StartLine,����EndLine) values(?,?,?,?,?,?)";
		return insertDB(list,strSQL);
	}
	
	public boolean insertFile(List<String> list) {
		String strSQL = "insert into File (Ŀ��FilePath,����FilePath, type,matchNodes) values(?,?,?,?)";
		return insertDB(list,strSQL);
	}
	
	public boolean insetrFunction(List<String> list) {
		String strSQL = "insert into Function (Ŀ��FilePath,Ŀ��FunName,����FilePath,����FunName) values(?,?,?,?)";
		return insertDB(list,strSQL);
	}
	
	public boolean insertProject(List<String> list) {
		String strSQL = "insert into Project (Ŀ��Path,����Path,type) values(?,?,?)";
		return insertDB(list,strSQL);
	}
	public boolean clearDB(String str,String str2) {
		PreparedStatement select = null;
		PreparedStatement pstmt = null;
		if (resDBcon == null) {
			throw new RuntimeException("database connection closed.");
		}
		try {
			String strSQL = "";
			strSQL = str;
			pstmt = resDBcon.prepareStatement(strSQL);
			pstmt.executeUpdate();
			strSQL = str2;
			pstmt = resDBcon.prepareStatement(strSQL);
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
		return true;
	}
	public boolean clearBlock() {
		String strSQL1 = "delete from Block";
		String strSQL2 = "alter table Block alter column ID counter(1,1)";
		return clearDB(strSQL1,strSQL2);
	}
	
	public boolean clearFile() {
		String strSQL1 = "delete from File";
		String strSQL2 = "alter table File alter column ID counter(1,1)";
		return clearDB(strSQL1,strSQL2);
	}
	
	public boolean clearFunction() {
		String strSQL1 = "delete from Function";
		String strSQL2 = "alter table Function alter column ID counter(1,1)";
		return clearDB(strSQL1,strSQL2);
	}
	
	public boolean clearProject() {
		String strSQL1 = "delete from Project";
		String strSQL2 = "alter table Project alter column ID counter(1,1)";
		return clearDB(strSQL1,strSQL2);
	}
	public void closeSimResultDB() {
		if (resDBcon != null) {
			try {
				resDBcon.close();
			} catch (SQLException ex) {
				// ex.printStackTrace();
				throw new RuntimeException("Access database connect error6",ex);
			}
		}
		resDBcon = null;
	}
	public static void createSimResultDB(String templateName) {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(templateName); // �����ļ�������
			fos = new FileOutputStream(FileName);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
		} catch (FileNotFoundException ex) {
			// ex.printStackTrace();
			throw new RuntimeException("���Ӳ��Խ���ļ�����", ex);
		} catch (IOException ex) {
			// ex.printStackTrace();
			throw new RuntimeException("���Ӳ��Խ���ļ�����",ex);
		} finally {
			try {
				if (fis != null)
					fis.close();
				if (fos != null)
					fos.close();
			} catch (IOException ex) {
				// ex.printStackTrace();
				throw new RuntimeException("���Ӳ��Խ���ļ�����",ex);			}
		}
		//clearData(pathname);
	}
	
	
}
