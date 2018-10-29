package softtest.tools.c.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**�����-I������ʽ�������м��ļ����ɵ��������ɣ����е�Դ�ļ�·����Ϣ���ܶ�ʧ��IP���ݿ���ֻ���ļ���������ȱʧ�ļ�·������
 * ����IPȷ��ʱ��Ҫ�˹������滻�������ߵĹ��ܣ�
 * <p>����IP���ݿ⣬�����ݿ��Ӧ�Ĺ���Ŀ¼�������Զ���ȫȱʧ��·����Ϣ������IPȷ��</p> 
 * <p>Ŀǰ��ȱ�ݣ������ļ����ظ���������޷���������</p>
 * 
 * 2010.12.18
 * */
public class AddFilePathToMDB {

	/**�ݴ�����ݿ��ж�ȡ���ļ���,�����ļ�·���Ĳ�����ͬ���ļ���ʱ������ */
	private ArrayList<String> dbFileNameList=null;
	
	//�ݴ�Դ�ļ����µ�����.c�ļ�
	HashMap<String,String> srcFileList=null;
	
	private String path_MDB="";
	
	private Connection conn=null;
	
	public static final char FILE_SEPARATOR=File.separatorChar;
	
	public AddFilePathToMDB(String mdbPath)
	{
		this.path_MDB=mdbPath;
		dbFileNameList=new ArrayList<String>();
		srcFileList=new HashMap<String,String>();
		openDatabase(path_MDB);
	}
	
	public static void main(String[] args) {
		/* 
		 * "E:\\DTSGCC\\Mdb\\symbolic\\antiword_interfile.mdb";
		 * "E:\\DTSGCC\\TestCase\\antiword-0.37\\antiword-0.37";
		 * 
		 * 
		 * "E:\\DTSGCC\\TestCase\\make-3.81\\make-3.81";
		 * 
		 * String mdbPath="E:\\DTSGCC\\Mdb\\binutils4.mdb";
		String srcDir="E:\\DTSGCC\\TestCase\\binutils-2.20.1\\src";
		 */
		if(args.length!=2){
			System.out.println("�������벻��ȷ��\nparam1: the mdb to be replaced" +
					"\nparam2: the source file absolute path");
			return;
		}
		String mdbPath=args[0];
		String srcDir=args[1];
		AddFilePathToMDB t=new AddFilePathToMDB(mdbPath);
		t.queryFileNameFromMDB();
		//System.out.println(t.getFileNameList().size());
		t.queryAllSrcFiles(srcDir);
		//System.out.println(t.getSrcFileList().size());
		int i=t.updateFilePathInMDB();
		System.out.println(i);
	}

	private void openDatabase(String mdbPath){
		File file = new File(mdbPath);
		if (!file.exists()) {
			throw new RuntimeException("Ŀ�����ݿⲻ���ڣ�");
		}
		
		String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
		String url = "jdbc:odbc:DRIVER=Microsoft Access Driver (*.mdb);DBQ=" + mdbPath;
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, "", "");
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException("Access database driver error",ex);
		} catch (SQLException ex) {
			throw new RuntimeException("Access database connect error",ex);
		}
	}
	
	private void queryFileNameFromMDB()
	{
		String sql="select distinct File from IP";
		ResultSet rs=null;
		Statement stmt=null;
		try {
			stmt = conn.createStatement();
			rs=stmt.executeQuery(sql);
			String name="";
			while (rs.next()) {
				name=rs.getString(1);
				dbFileNameList.add(name);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try {
				rs.close();
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**��Դ�ļ����в�ѯ���е�.c�ļ��������ļ����ظ���� */
	private void queryAllSrcFiles(String dirPath){
		File srcDir=new File(dirPath);
		if(!srcDir.exists()){
			throw new RuntimeException("Դ�ļ��в����ڣ�");
		}
		File[] fileList=srcDir.listFiles();
		int fileNum=fileList.length;
		for(int j=0;j<fileNum;j++){
			if(fileList[j].isDirectory())
			{
				queryAllSrcFiles(fileList[j].getAbsolutePath());
			}else {
				String filePath=fileList[j].getAbsolutePath();
				filePath=filePath.replace("\\","/");
				if(filePath.endsWith(".c") || filePath.endsWith(".C")
						|| filePath.endsWith(".def") || filePath.endsWith(".y"))
				{//����Դ�ļ��ĺ�׺Ϊdef
					String fileName=fileList[j].getName();
					if(srcFileList.get(fileName)!=null){
						srcFileList.remove(fileName);
						System.out.println("Դ�ļ��д���ͬ���ļ�"+fileName+"�����ֶ��滻��");
					}else{
						srcFileList.put(fileName, filePath);
					}
				}
			}
		}
	}
	
	private int updateFilePathInMDB(){
		int updateCount=0;
		String sql="";
		String path="";
		Statement stmt=null;
		try {
			stmt=conn.createStatement();
			for(String file : dbFileNameList){
				String name=file;
				if(name.indexOf(':')>=0){
					//�������windows�е��̷�����˵����������·��
					continue;
				}
				else if(name.indexOf(FILE_SEPARATOR)>=0){
					//����������·����������ļ���
					int i=name.lastIndexOf(FILE_SEPARATOR);
					name=name.substring(i+1, name.length());
				}
				
				path=srcFileList.get(name);
				if(path!=null){
					sql="update IP set File="+"\'"+path+"\'"+" where File="+"\'"+file+"\'";
					updateCount+=stmt.executeUpdate(sql);
				}else{
					System.out.println("δ���ҵ�Դ�ļ�·����"+name);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return updateCount;
	}

	public ArrayList<String> getFileNameList() {
		return dbFileNameList;
	}

	public void setFileNameList(ArrayList<String> fileNameList) {
		this.dbFileNameList = fileNameList;
	}

	public HashMap<String, String> getSrcFileList() {
		return srcFileList;
	}

	public void setSrcFileList(HashMap<String, String> srcFileList) {
		this.srcFileList = srcFileList;
	}
}
