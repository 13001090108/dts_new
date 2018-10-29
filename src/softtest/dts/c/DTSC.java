package softtest.dts.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import LONGMAI.NoxTimerKey;
import softtest.cluster.c.ReadDB;
import softtest.cluster.c.SimpleBean;
import softtest.config.c.Config;
import softtest.config.c.SuccessRe;
import softtest.database.c.DBAccess;
import softtest.dscvp.c.DSCVPElement;
import softtest.fsmanalysis.c.CAnalysis;
import softtest.fsmanalysis.c.ProjectStat;
import softtest.interpro.c.InterContext;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;
import softtest.registery.Authentication;
import softtest.registery.Registery;
import softtest.registery.RegisteryClient;
import softtest.registery.SysInfo;
import softtest.registery.file.RegViewer;
import softtest.registery.file.Register;
import softtest.registery.file.Reset;
import softtest.scvp.c.SCVP;
import softtest.scvp.c.SCVPString;

public class DTSC {
	//����ļ� ytang 20161107
	public static File res_file = new File("C:\\Users\\lenovo\\Desktop\\SCVPresult.txt");
	//public static HashMap<SCVPString, DSCVPElement> dscvpElementList = new HashMap<SCVPString, DSCVPElement>();//JJL,����Dscvp
	public static HashMap<String, DSCVPElement> DipList = new HashMap<String, DSCVPElement>();//JJL,����Dip,2016-12-2
	public static List<SimpleBean> dbList = new ArrayList<SimpleBean>();//JJL������DBlist��20161204
	
	private String fsmDirPath;
	private static String fileName;
	private Map<String, String> fsmList;
	
	private CAnalysis analyser;
	
	static DTSCFrame progFrame;
	
	private String resultdb;
	private String analysisModel;
	
	static Logger logger =Logger.getLogger(DTSC.class);
	private static final int lablen  = 73;
	/**
	 * <p>The input c source files or directory to be tested.</p>
	 */
	private String srcFilesPath;
	
	/**
	 * <p>The list contains all the c files to test.</p>
	 */
	private List<String> filePathList;
	
	/**
	 * <p>The DTSC config file for do the preprocess.</p>
	 */
	private final static String CONFIG_FILE = "config\\config.ini";
	
	/**
	 * <p>The note prefix in the config file.</p>
	 */
	private final static String NOTE_PREFIX = "#";
	
	/**
	 * <p>The c include files the project depends when doing the preprocessing.</p>
	 */
	private List<String> incDirs;
	
	/**
	 * <p>The macro list gcc compiler commond options.</p>
	 */
	private List<String> macroList;
	
	/**
	 * <p>The list contains all dirs of projects.</p>
	 */
	private LinkedHashSet<String> allProjDir;
	
	/**
	 * <p>The Cpp Preprocesser</p>
	 */
	private Pretreatment pre;
	
	/** ����ȫ�ִ洢��DSCVPElement*/
	public HashMap<String, DSCVPElement> getDipList() {
		return this.DipList;
	}
	
	public static void setDBList(List<SimpleBean> dbList) {
		dbList = dbList;
	}
	
	public List<SimpleBean> getDBList() {
		return this.dbList;
	}
	
	public DTSC() {
		filePathList = new ArrayList<String>();
		incDirs = new ArrayList<String>();
		macroList = new ArrayList<String>();
		pre = new Pretreatment();
		fsmList = new HashMap<String, String>();
		allProjDir = new LinkedHashSet<String>();
	}
	
	public static void main(String args[])
	{
		BasicConfigurator.configure();
		
		//����־��������Ϊ������
		Log log=new Log();
		
		log.init(args[args.length-1]);
		
        PropertyConfigurator.configure("log4j.properties") ;
        Calendar cal = Calendar.getInstance(); 
        int month=cal.get(Calendar.MONTH)+1;
        logger.info("***********DTSGCC�ļ�������ʼ: "+cal.get(Calendar.YEAR)+"/"+month+"/"+cal.get(Calendar.DAY_OF_MONTH)+"/"+"***********");
		if (Config.NETWORK_LOCK)
		{
			Authentication au = new Authentication();
			au.initialize();
			boolean result = au.checkIdentity();
			if (!result)
			{
				System.exit(0);
			}
		}
        
		if (Config.LOCK){
			NoxTimerKey aNox = new NoxTimerKey();
			int [] keyHandles =  new int[8];
			int [] nKeyNum = new int[1];
			int nAppID = 0xFFFFFFFF;
			int rightLock=aNox.NoxFind(nAppID, keyHandles, nKeyNum);
			SuccessRe.setL(rightLock==0?true:false);
			//���Ҽ�����
	        if( 0 != rightLock)
	        {
	        	JOptionPane.showMessageDialog(null, "������������");
	            return;
	        }
		}
        if(Config.REGISTER){
        	boolean rightRe=Registery.checkRegistery();
        	if (!rightRe) {
				RegisteryClient rc = new RegisteryClient();
				rc.launchFrame();
				rc.setVisible(true);
				return;
			}
        	SuccessRe.setR(rightRe);
		} else {
			if (Config.LOCK) {
				//���Ȩ��
				boolean rightLock=SysInfo.checkPermission();
				SuccessRe.setL(rightLock);
				if (!rightLock) {
					return;
				}
			}
		}
        
        if (Config.FILE_LICENSE)
		{
			int result = Register.verify();

			if (result == Register.UNREGISTERED)
			{
				RegViewer.launch();
				return;
			}

			if (result == Register.ERROR)
			{
				// JOptionPane.showMessageDialog(null, "�û����������������", "ERROR!",
				// JOptionPane.ERROR_MESSAGE);
				Reset.launch();
				return;
			}
			SuccessRe.setFL(result);
		}
        
        if (args.length < 2) {      	
        	usage();
        	return;		
		}
		
		DTSC dtsc=new DTSC();
		
		//��temp�ļ���ɾ��
		CAnalysis.deleteAndCreateTemp(Config.PRETREAT_DIR);
		
		
		//��ȡ�����ļ�������Ԥ����ǰ�ĳ�ʼ��
		dtsc.initPretreatment(args);
		
		
		
		//�����û����洰�ڣ���ʾ���Խ��ȼ���Ҫ�Ĳ��Խ��
		progFrame = new DTSCFrame();
		
		try {
			dtsc.process();
		} catch (RuntimeException e) {
			System.out.println(e);
			progFrame.setMessage(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * @author ssj
	 *�����ļ�����
	 */
	public void projprocess(String fileName){
		File projectFile = null;
		try{
			projectFile =  new File(fileName);
			if (projectFile != null && projectFile.exists()) {
				fileName = projectFile.getAbsolutePath(); 
				ProjectAnalyser projAna = new ProjectAnalyser(projectFile.getAbsolutePath());
				projAna.process();
				filePathList.addAll(projAna.filePathList);
				for(String files : projAna.filePathList){
					allProjDir.add(files.substring(0, files.lastIndexOf(File.separatorChar)));
				}
				incDirs.addAll(projAna.incDirs);
			}
		}catch(ProjAnalyserException e) {
			e.printStackTrace();
		}
	}
	public void process() {
		long start = System.currentTimeMillis();

		if(srcFilesPath.endsWith("\n")){
			srcFilesPath = srcFilesPath.substring(0, srcFilesPath.indexOf("\""));
		}
		
		List<String> allFiles = new ArrayList<String>();
		String srcFilesPaths[] = srcFilesPath.split(";");
		
		for(String srcFilePath:srcFilesPaths)
		{
			File srcFile = new File(srcFilePath);
			if (!srcFile.exists()) {
				throw new RuntimeException("Error: File " + srcFile.getName() + " does not exist.");
			}
			//����.c .h�ȵ������ļ��б�
			collectAll(allFiles, srcFile);
			
			LinkedHashSet<String> allDir = new LinkedHashSet<String>();	
			//liuli:��ȡ����������жϽ��к��ַ���
			if(analysisModel.equals("-I")){
				Config.ANALYSIS_I = true;
			}else if(analysisModel.equals("-P")){
				Config.ANALYSIS_P = true;
			}else if(analysisModel.equals("-R")){
				Config.ANALYSIS_R = true;
			}
			
			if(Config.ANALYSIS_I){
				collectI(srcFile);
			}
			
			if(Config.ANALYSIS_P){
				if(srcFile.isFile() && srcFile.getAbsolutePath().matches(ProjectAnalyser.PROJECTFILE_EXT)){
					projprocess(srcFile.getAbsolutePath());
				}
				if (srcFile.isDirectory()) {
					// ������ǰĿ¼�����й����ļ��������д���
					for (String fileName : allFiles) {
						allDir.add(fileName.substring(0, fileName
								.lastIndexOf(File.separatorChar)));
						if (fileName.matches(ProjectAnalyser.PROJECTFILE_EXT)) {
							allProjDir.add(fileName.substring(0, fileName
									.lastIndexOf(File.separatorChar)));
							projprocess(fileName);
						}
					}
					// ����Ŀ¼�¶�û���ҵ������ļ������ռ�����Դ�����ļ����м��
					if (allProjDir.size() == 0) {
						Config.ANALYSIS_R = true;
					} else {
						// �ҵ������ļ���������Ŀ¼�²����ڹ����ļ�������ЩĿ¼��ȡ�ռ���Ŀ¼������Դ�����ļ����м��
						for (String fileDir : allDir) {
							boolean exist = false;
							for (String projFileDir : allProjDir) {
								if (projFileDir.equals(fileDir)) {
									exist = true;
									break;
								}
							}
							if (!exist) {
								File src = new File(fileDir);
								boolean flag = false;
								File[] fs = src.listFiles();
								for (int i = 0; i < fs.length; i++) {
									if (fs[i].isFile() && fs[i].getName().matches(InterContext.SRCFILE_POSTFIX)) {
										filePathList.add(fs[i].getPath());
										flag = true;
									}
								}
								if (flag) {
									incDirs.add(fileDir);
								}
							}
						}
					}
				}
			}
			
			if(Config.ANALYSIS_R){
				//�������ļ�������ͷ�ļ���������Ŀ¼�����������ͷ��
		    	Set<String> incSet = new HashSet<String>();
				for (String fileName : allFiles) {
					if (fileName.matches(InterContext.INCFILE_POSTFIX)) {
						int lastSep = fileName.lastIndexOf(File.separator);
						incSet.add(fileName.substring(0, lastSep));
					}
				}
				if(srcFile.isFile() && srcFile.getAbsolutePath().matches(InterContext.SRCFILE_POSTFIX)){
					int lastSep = srcFile.getAbsolutePath().lastIndexOf(File.separator);
					incSet.add(srcFile.getAbsolutePath().substring(0, lastSep));
				}
				for (String inc : incSet) {
					incDirs.add(inc);
				}
				//Դ�ļ�·���µ�����.cԴ�ļ�
				collect(srcFile);
			}
		}
		
		

		//����״̬���ļ�
		loadFsm();
		
		analyser = new CAnalysis(filePathList, fsmList);
			
		ProgressThread pthread=new ProgressThread(); 
		pthread.start();
		
		//����Ԥ������Ϣ
		pre.setInclude(incDirs);
		pre.setMacro(macroList);
		analyser.setPretreatmentInstance(pre);
		
		int line = analyser.analyse(resultdb);
		
		long end = System.currentTimeMillis();
		
		for(String srcFilePath:srcFilesPaths)
		{
			ProjectStat projInfo = new ProjectStat(srcFilePath, resultdb, filePathList.size() - analyser.getFailFileNum(), line, (int)(end-start)/1000, analyser.getErrorNum(),
					analyser.getSyntaxTreeTime(), analyser.getSymbolTableTime(), analyser.getGlobalAnalysisTime(),analyser.getTimeoutfiles());
			DBAccess dbAccess = DBAccess.getInstance();
			dbAccess.saveProjStat(projInfo);
			
			logger.info("��������ʱ��: " + (end - start)/1000 + "(s).");
			logger.info("���γɹ�����C�ļ�������: " + (filePathList.size() - analyser.getFailFileNum()));
			logger.info("Ԥ����ʧ���ļ������� " + analyser.getFailFileNum());
			logger.info("���η�������������: " + line);
			logger.info("���δ�����������ֹ�����: " + analyser.getErrorNum());

			progFrame.finish(filePathList.size() - analyser.getFailFileNum(), line, (end - start)/1000, analyser.getReportNum(), analyser.getErrorNum());
			dbAccess.openDataBase(resultdb);
			dbAccess.writeResult((int) (end - start) / 1000, filePathList.size(), line);
			//dbAccess.closeDataBase();
			if(Config.Cluster) {
				//��ʼ����2016/10/13��JJL
				ReadDB rdb = new ReadDB();
				rdb.start(dbList, dbAccess);

				rdb.generateDepChain(dbList,dbAccess);

			}
			//rdb.startCluster(dscvpElementList, resultdb);
			//rdb.startCluster(DipList, resultdb);
		}
	}
	public static String readFile(String fileName) throws IOException
	{
		Reader reader = new FileReader(fileName);

		try
		{
			// create a StringBuffer instance.
			StringBuffer sb = new StringBuffer();

			// buffer for reading.
			char[] buffer = new char[1024];

			// number of read chars.
			int k = 0;

			// Read characters and append to string buffer
			while ((k = reader.read(buffer)) != -1)
			{
				sb.append(buffer, 0, k);
			}

			// return read content
			return new String(sb);
		}
		finally
		{
			//add finally if condition by liuyan 2015.6.3 
			if( reader != null ){
				try
				{
					reader.close();
				}
				catch (IOException ioe)
				{
					// ignore

				}
			}
		}
	}
	/**
	 * <p>Loads the fsm defination files from the given directory.</p>
	 */
	private void loadFsm() {
		Map<String, String> fsms = DBAccess.getScanTypes(DBAccess.CONFIG_MDB_PATH);
		File path=new File(fsmDirPath);
		File[] ffs = path.listFiles();
		for (int i = 0; i < ffs.length; i++) {
			// ����ģʽ���ͷ�Ŀ¼����
			if (ffs[i].isDirectory()) {
				File[] models = ffs[i].listFiles();
				for (int j = 0; j < models.length; j++) {
					for(String str:fsms.keySet()){
						String temp = str+"-";
						
						if(models[j].getName().startsWith(temp) && ffs[i].getName().equalsIgnoreCase(fsms.get(str))){
							fsmList.put(fsmDirPath + ffs[i].getName() + File.separator + models[j].getName() , fsms.get(str));
							if (Config.TRACE) {
								System.out.println("���صĹ���ģʽ��Ϊ��	" + fsmDirPath + ffs[i].getName()+ File.separator + models[j].getName());
							}
							//added by liuyan 2015-10-26
							if(Config.LoadFSM){
								logger.info("���ص�ģʽΪ��	" + fsmDirPath + ffs[i].getName()+ File.separator + models[j].getName());
							}
							
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * @author zys
	 * <p>�����û������ļ�config.ini����ʼ��Ԥ���뻷��������ƽ̨��gcc/keil)������ѡ�ͷ�ļ����궨�壩
	 * </p>
	 */
	private void initPretreatment(String[] options) {
		// we should add more command options here
		srcFilesPath = options[0];
		resultdb = options[1];
		if(options.length == 2){//ȱʡ��������Ϊ-R
			analysisModel = "-R";
		}else{
			analysisModel = options[2];
		}
		
		File configFile = new File(CONFIG_FILE);
		if (configFile.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(configFile));
				String config;
				while ((config = reader.readLine()) != null ) {
					if (config.startsWith(NOTE_PREFIX)) {
						continue;
					}
					String[] configs = config.split(" ");
					if (configs[0].equalsIgnoreCase("-I")) {
						StringBuffer sb=new StringBuffer();
						for(int i=1;i<configs.length;i++)
						{
							sb.append(configs[i]);
							sb.append(" ");
						}
						sb.deleteCharAt(sb.length()-1);
						incDirs.add(sb.toString());
					} else if (configs[0].equalsIgnoreCase("-D") && configs.length == 2) {
						macroList.add(configs[1]);
					} else if (configs[0].equalsIgnoreCase("-gcc")) {
						pre.setPlatform(PlatformType.GCC);
						fsmDirPath="softtest\\rules\\gcc\\";
						String INCLUDE = System.getenv("GCCINC");
						if(INCLUDE==null){
							logger.info("System environment variable \"GCCINC\" error!");
							throw new RuntimeException("System environment variable \"GCCINC\" error!");
						}
						String[] temp = INCLUDE.split(";");
						for(int i = 0;i<temp.length;i++){
							if(Config.GCC_TYPE==1){
								temp[i]=temp[i].replace('\\', '/');
							}
							Pretreatment.systemInc.add(temp[i]);
							//��GCCINC�е�ͷ�ļ�Ŀ¼���Զ�ʶ��Ϊͷ�ļ�Ŀ¼
							incDirs.add(temp[i]);
						}

						CAnalysis.LIB_SUMMARYS_PATH="gcc_lib";
					} else if (configs[0].equalsIgnoreCase("-keil")) {
						pre.setPlatform(PlatformType.KEIL);
						fsmDirPath="softtest\\rules\\keilc\\";
						String INCLUDE = System.getenv("C51INC");
						if(INCLUDE==null){
							logger.info("System environment variable \"C51INC\" error!");
							throw new RuntimeException("System environment variable \"C51INC\" error!");
						}
						String[] temp = INCLUDE.split(";");
						for(int i = 0;i<temp.length;i++){
							Pretreatment.systemInc.add(temp[i]);
							//��C51INC�е�ͷ�ļ�Ŀ¼���Զ�ʶ��Ϊͷ�ļ�Ŀ¼
							incDirs.add(temp[i]);
						}
						CAnalysis.LIB_SUMMARYS_PATH="keil_lib";
					}else if(2<=configs.length){
						 String str=configs[0].substring(1).trim();
						 Class clazz=Class.forName("softtest.config.c.Config");
						 //System.out.println(str);
						 Field fld = clazz.getDeclaredField(str);
						 if (fld==null) {
							continue;
						}
						 Config cfg=new Config();
						 if(fld.getName().equals(str)){
		                	Class clz=fld.getType();
		                	if(clz==int.class){
		                		fld.setInt(cfg, Integer.parseInt(configs[1].trim()));
		                	}else if(clz==long.class){
		                		fld.setLong(cfg, Long.parseLong(configs[1].trim()));
		                	}else if(clz==boolean.class){
		                		fld.setBoolean(cfg, Boolean.parseBoolean(configs[1].trim()));
		                	}else if (clz==String.class) {
		                		fld.setAccessible(true);
		                		StringBuffer sb=new StringBuffer();
		                		for(int i=1;i<configs.length;i++){
		                			sb.append(configs[i]);
		                		}
		                		fld.set(cfg, sb.toString().trim());
		                		fld.setAccessible(false);
							}
		                }
					}
				}
			}catch(NullPointerException ne){
				System.err.println("Please restart the application and set the environment variable(GCCINC/C51INC).");
			}catch (Exception e) {
				//added by liuyan 2015.6.5 
				e.printStackTrace();
				//end
				System.err.println("Error in reading the config file.");
			}
		}
		if(options.length>3){
			String[] s=options[3].split(";");
			for(int i=0;i<s.length;i++){
				if(!s[i].equals(""))
					incDirs.add(s[i]);			
			}
        }   
	
	}
	
	/**
	 * <p>Prints the usage message if got wrong arguments.</p>
	 */
	private static void usage() {
		System.out.println("Defect Testing System for Keil Cx51!");
		System.out.println("Usage:\n\t java sourcefiles targetdb");
		
		System.out.println("-r/-p/-a");
		System.out.println("\t\t\t sourcefiles:the KeilC filePaths you want to test");
		System.out.println("\t\t\t targetdb:the .mdb database file including the test results");
		System.out.println("\t\t\t analysisModel:the analysis model to Process the source file");
		
	}
	
	/**
	 * �ռ���ǰ���Թ��̵������ļ���.CԴ�ļ���.Hͷ�ļ������Ա����Զ�������ʱ��ƥ��
	 */ 
	private void collectAll(List<String> allFiles, File srcFile) {
		if (srcFile.isFile()) {
			allFiles.add(srcFile.getPath());
		} else if (srcFile.isDirectory()) {
			File[] fs = srcFile.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collectAll(allFiles, fs[i]);
			}
		}
	}
	
	/**
	 * �ռ���ǰ���Թ��̵�����Դ�ļ���.CԴ�ļ�����������������.C�ļ����뵽�������б���
	 */
	private void collect(File srcFile) {
		if (srcFile.isFile() && srcFile.getName().matches(InterContext.SRCFILE_POSTFIX)) {
			filePathList.add(srcFile.getPath());
		} else if (srcFile.isDirectory()) {
			File[] fs = srcFile.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collect(fs[i]);
			}
		}
	}
	/**
	 * liuli
	 * �ռ���ǰ���Թ��̵������м��ļ���.i�м��ļ�����������������.i�ļ����뵽�������б���
	 */
	private void collectI(File srcFile) {
		if (srcFile.isFile() && srcFile.getName().matches(InterContext.IFILE_POSTFIX)) {
			filePathList.add(srcFile.getPath());
		} else if (srcFile.isDirectory()) {
			File[] fs = srcFile.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collectI(fs[i]);
			}
		}
	}
	//zhanghong 2013-05-13
	public static void setFileName(String file){
		fileName = file;
	}
	
	/**2013-5-20*/
	public String replace(String fileName,int len){
		String text = "";
		int l = fileName.length();
		int index;
		index = 3+len+3;
		while(fileName.charAt(index) != File.separatorChar){
			
			index++;	
			if(index > l)
				break;
		}
		text = fileName.substring(index);
		text = fileName.substring(0, 3) + "..."+ text;
		return text;
	}
	public void showFileName(boolean isPre,String fileName){
		String text= "";
		if(Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			if(isPre){
				text = "����Ԥ���� "+ fileName+" �ļ�";
			}else{
				text = "���ڷ����� "+ fileName+" �ļ�";
			}
			
			int textLen = text.length();
			
			if(textLen >lablen){
				int len = textLen - lablen;
				text = replace(fileName,len);
				if(isPre){
					text = "����Ԥ���� "+ text+" �ļ�";
				}else{
					text = "���ڷ����� "+ text+" �ļ�";
				}
			}
		}else if(Config.DTS_LANGUAGE == Config.LANG_ENGLISH){
			if(isPre){
				text = "DTS is preprocessing file�� "+ fileName;
			}else{
				text = "DTS is analyzing file�� "+ fileName;
			}
			
			int textLen = text.length();
			
			if(textLen >lablen){
				int len = textLen - lablen;
				text = replace(fileName,len);
				if(isPre){
					text = "DTS is preprocessing file�� "+ text;
				}else{
					text = "DTS is analyzing file�� "+ text;
				}
			}
		}
		
		progFrame.jlfilecount.setText(text);
	}
	class ProgressThread extends Thread
	{
		
		@Override
		//dongyk 20121023Ԥ����Ľ��� �� �����Ľ��� �ֱ������ʾ
		public void run()
		{
			boolean isPreProcessFinished=true;
			while (analyser.progress() < 100)
			{
				if(analyser.preProgress()<100&&analyser.progress()==0)
				{	
					//progFrame.updateProg(analyser.preProgress());
					progFrame.updateProg(analyser.preProgress());
					showFileName(true,fileName);
					/**2013-3-27 ��ʾ����Ԥ������ļ���*/
					//String fileName = CAnalysis.getPreAnalysisFileName();

					//showFileName(true,fileName);
					//System.out.println(fileName);
					
				}
				else
				{
					if(isPreProcessFinished)
					{
						if (Config.DTS_LANGUAGE == Config.LANG_CHINESE) 
						{
							if (Config.ISTRIAL) {
								//progFrame.setTitle("DTSCPP�����ð棩���ڷ���......");
								progFrame.setTitle("�����ð棩���ڷ���");
					        } else {
					        	//progFrame.setTitle("DTSCPP���ڷ���......");
					        	progFrame.setTitle("���ڷ���");
					        }
						}
						if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
						{
							if (Config.ISTRIAL) {
								//progFrame.setTitle("DTSCPP��Trial Version�� is analyzing......");
								progFrame.setTitle("��Trial Version�� Analyzing");
					        } else {
					        	//progFrame.setTitle("DTSCPP is analyzing......");
					        	progFrame.setTitle("Analyzing");
					        }
						}
						isPreProcessFinished=false;
					}
					//progFrame.updateProg(analyser.progress());
					progFrame.updateProg(analyser.progress());
					showFileName(false,fileName);
					/**��̬��ʾ���ڷ������ļ��� 2013-3-27*/
					//String fileName = CAnalysis.getAnalysisFileName();
				
					//showFileName(false,fileName);
					//System.out.println(fileName);
				}
				try
				{
					Thread.sleep(100);
				}
				catch (Exception e)
				{
				}
			}
			progFrame.jpb.setBounds(10,10,500,30);
			progFrame.jlfilecount.setBounds(10,40,500,30);
			
			progFrame.jp.add(progFrame.jllinenumber);
			progFrame.jp.add(progFrame.jltime);		
			progFrame.jp.add(progFrame.jreportIp);
			progFrame.jp.add(progFrame.jtotalIp);
			progFrame.jp.add(progFrame.jinfo);
			progFrame.jp.add(progFrame.jlbutton);	
			if(Config.AUTOCLOSE){
				progFrame.dispose();
			    progFrame.setVisible(false);
			}
		}
	}

	/*class ProgressThread extends Thread{
		@Override
		//dongyk 20121023Ԥ����Ľ��� �� �����Ľ��� �ֱ������ʾ
		public void run(){
			boolean isPreProcessFinished=true;
			

			while(analyser.progress()<100){
				progFrame.updateProg(analyser.progress(), fileName);
			//	System.out.println(fileName);
				try {
					Thread.sleep(100);
				} catch (Exception e) {
				}
			}
			if(Config.AUTOCLOSE){
				progFrame.dispose();
			    progFrame.setVisible(false);
			}
		}
	}*/
}
//end