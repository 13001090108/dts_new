package softtest.pretreatment;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;


import softtest.ast.c.CParser;
import softtest.config.c.Config;

/**
 * <p>
 * Calls the GCC or KEIL to do the preprocessor for the C program.
 * </p>
 * 
 * @author
 * 
 */
public class Pretreatment implements Serializable
{
	/**
	 * ���л�ID add by lsc 2018/10/16
	 */
	private static final long serialVersionUID = 2217780499222051372L;
	private static Logger logger = Logger.getRootLogger();
	//�������ͬ���ļ������ļ�ĩβ���sNumber������ʶ
	private int sNumber=1;
//	static String userDir = System.getProperty("user.dir");
//	static String userDir = ClassLoader.getSystemResource("").toString();
//	static URL url = Thread.currentThread().getContextClassLoader().getResource("");
//	static String string = url.toString();
//	static File file = new File(string);
//	static String userDir = file.getParentFile().getParentFile().getAbsolutePath();
//	
	
	 String aString = Pretreatment.class.getResource("/").getPath();
	 String aString2 = aString.substring(1, aString.length() - 5);
	 String userDir = aString2;
	
	private String PRETREAT_FILE = userDir + "\\cpp\\defines.txt";

	List<String> include = new ArrayList<String>();
	List<String> macro = new ArrayList<String>();
	
	/**�����ռ�Դ�ļ��������ϵͳͷ�ļ�����#include <stdio.h> */
	private Set<String> libIncludes=new HashSet<String>();
	
	/**ϵͳ���������е�ͷ�ļ�Ŀ¼,���ݲ�ͬ�ı�������ȡ��gccΪGCCINC��keilΪC51INC */
	//public static String systemInc[];
	public static List<String> systemInc = new ArrayList<String>();;
	private boolean isError;

	private PlatformType platform;
	
	public void setPlatform(PlatformType platform)
	{
		this.platform = platform;
		CParser.setType(platform.name());
	}
	
	

	
	public PlatformType getPlatform()
	{
		return this.platform;
	}
	
	Process rt = null;

	/**���ø���������Ԥ����ָ�������м��ļ�	 */
	public String pretreat(String filePath, List<String> incFiles, List<String> macroList)
	{
		// ����ļ�·�����пո����Դ�ļ�·��������
		if (filePath.contains(" ") && !filePath.startsWith("\"")) {
			filePath = "\"" + filePath + "\"";
		}
		if (PRETREAT_FILE.contains(" ") && !PRETREAT_FILE.startsWith("\"")) {
			PRETREAT_FILE = "\"" + PRETREAT_FILE + "\"";
		}
		
		String[] temp = filePath.replace('/', '\\').split("\\\\");
		File tempFile = null;
		String options = filePath;
		String result = null;
		String tempName;
		try {
			tempName=temp[temp.length - 1];
			//����ļ��������������ţ���ȥ��������
			if(tempName.endsWith("\""))
				tempName=tempName.substring(0, tempName.length()-1);
			if(tempName.endsWith(".c")){
				tempName = tempName.replaceFirst("\\.c", "\\.i");
			}else if(tempName.endsWith(".C")){
				tempName = tempName.replaceFirst("\\.C", "\\.i");
			}
			
			tempFile = new File(Config.PRETREAT_DIR +"\\"+ tempName);
			
			
			
			if (Config.DELETE_PRETREAT_FILES) {
				tempFile.deleteOnExit();
			}
			
			//zys:�����ͬ��Ŀ¼�д���ͬ����.cԴ�ļ���Ϊ�˷�ֹ���ǣ�Ԥ����ʱ�����жϲ���������
			if(tempFile.exists())
			{
				tempName=temp[temp.length - 1];
				//����ļ��������������ţ���ȥ��������
				if(tempName.endsWith("\""))
					tempName=tempName.substring(0, tempName.length()-1);
				if(tempName.endsWith(".c") || tempName.endsWith(".C")){
					tempName=tempName.substring(0, tempName.length()-2)+(sNumber++);
					tempName = tempName+".i";
				}
				tempFile = new File(Config.PRETREAT_DIR +"\\"+ tempName);
				try {
					if(tempFile.exists() == false) {
						tempFile.createNewFile();
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				
				
			}else if(!tempFile.createNewFile()) {
				throw new RuntimeException("Error in creating the temp files");
			}
		} catch (IOException e) {
			throw new RuntimeException("Error in creating the temp files " + tempFile.getAbsolutePath());
		}
		result = tempFile.getAbsolutePath();
		
		
		// macro files
		if(macroList.size()>0)
		{
			if (platform == PlatformType.KEIL) {
				options += " DEFINE(";
				for (Iterator<String> iter = macroList.iterator(); iter.hasNext();) {
					String macro = (String) iter.next();
					options +=macro+",";
				} 
				options=options.substring(0,(options.length()-1));
				options += ")";
			}else if (platform == PlatformType.GCC) {
				for (Iterator<String> iter = macroList.iterator(); iter.hasNext();) {
					String macro = (String) iter.next();
					options +=(" -D"+macro);
				} 
			} 
		}
		
		// include files
		if(incFiles.size()>0)
		{
			if (platform == PlatformType.KEIL) {
				options += " INCDIR(";
				for (Iterator<String> iter = incFiles.iterator(); iter.hasNext();) 
				{
					String include = (String) iter.next();
					options +=include+";";
				}
				options=options.substring(0,(options.length()-1));
				options += ")";
			}else if (platform == PlatformType.GCC) {
				for (Iterator<String> iter = incFiles.iterator(); iter.hasNext();) {
					String include = (String) iter.next();
					options += (" -I \"" + include + "\"");
				}
			}
		}
		
		// add precompile parameters
		if (platform == PlatformType.KEIL) {
			options += (" pp");
		} else if (platform == PlatformType.GCC) {
			options += (" -include " + PRETREAT_FILE + " -w -E -o \"" + result+"\"");
		} 
		// do the c preprocessor
		String cmd;
		try {
			if (platform == PlatformType.KEIL) {
				cmd="c51 " + options+" ("+result+")";
				rt = Runtime.getRuntime().exec(cmd);
			} else if (platform == PlatformType.GCC) {
				cmd="gcc " + options;
				rt = Runtime.getRuntime().exec(cmd);
			} 
			final InputStream is1 = rt.getInputStream();
			new Thread(new Runnable() {
			    public void run() {
			        BufferedReader br = new BufferedReader(new InputStreamReader(is1));
			        try {
			        	String temp=null;
						while((temp=br.readLine())!= null)
						{
							System.out.println(temp);
						}
					} catch (IOException e) {
						try {
							br.close();
							is1.close();
							rt.destroy();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
			    }
			}).start(); 
			InputStream is2 = rt.getErrorStream();
			BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
			String line = br2.readLine();
			if (line != null) {
				isError = true;
				logger.info(line);
			} else {
				isError = false;
			}
			while((line = br2.readLine()) != null) {
				logger.info(line);
			}
			rt.waitFor();  
		} catch (Exception e) {
			e.printStackTrace();
			rt.destroy();
			System.gc();
			throw new RuntimeException("Error in doing c preprocessor");
		}finally{
			System.gc();
		}
		if (Config.USE_SUMMARY)
			findIncludeLib(result);
		
		if (Config.InterFile_Simplified) {
			result = fileSimplified(result);
		}
		return result;
	}
	
	/**��ȡԴ�ļ��������ϵͳͷ�ļ��������ɿ⺯��ժҪ*/
	public void findIncludeLib(String interFile)
	{
//		File file=null;
//		BufferedReader read=null;
//		try 
//		{
//			file=new File(interFile);
//			if(file.exists()){
//				read = new BufferedReader(new FileReader(file));
//				String line = null;
//				while((line = read.readLine()) != null) 
//				{
//					//Thread.sleep(1);
//					//gcc��keilԤ�����ĸ�ʽ��һ�£�gcc����# ���� "**.h"  ,��keil����#line ���� "**.h"
//					if (line.startsWith("#")) 
//					{
//						String[] infors = line.split(" ");
//						try{
//							String num=infors[1];
//							Integer.parseInt(num);
//						}catch(NumberFormatException e){
//							continue;//������������ļ�����#pragma����������������������
//						}
//						String include = "";
//						if (infors.length >=3) {
//							include=infors[2];
//							// ·�����д��пո�
//							include = include.replace("\\\\", "\\");
//							include = include.replace("\"", "").trim();
//							if(!include.matches(InterContext.INCFILE_POSTFIX) 
//									&& !include.matches(InterContext.SRCFILE_POSTFIX))
//							{
//								for (int i = 3; i < infors.length; i++) {
//									include +=  " " + infors[i];
//									include = include.replace("\"", "").trim();
//									if(include.matches(InterContext.INCFILE_POSTFIX))
//										break;
//								}
//							}
//						}
//						if(include.length()==0 || !include.matches(InterContext.INCFILE_POSTFIX))
//							continue;
//						File temp = new File(include);
//
//						if(!Config.ANALYSIS_I)
//							include = temp.getCanonicalPath();
//
//						// �����ǰͷ�ļ���ϵͳͷ�ļ��������⺯���б���
//						for (String s : systemInc) {
//							if (include.startsWith(s)) {
//								libIncludes.add(include);
//								break;
//							}
//						}
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally{
//			try {
//				if(read!=null)
//					read.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	/**���м䴦���ļ���ȥ������Ŀ��С������У������ȡ�����ϵͳͷ�ļ�����������صĿ⺯��ժҪ*/
	private String fileSimplified(String interFile) {
		String postfixSimplified=null;
		if (platform == PlatformType.GCC) {
			postfixSimplified=".gcc";
		}else if (platform == PlatformType.VC) {
			postfixSimplified=".vc";
		}else if (platform == PlatformType.KEIL) {
			postfixSimplified=".keil";
		}
		String result = interFile + postfixSimplified;
		BufferedReader read = null;
		BufferedWriter write = null;
		try {
			File inter = new File(interFile);
			File resultFile = new File(result);
			read = new BufferedReader(new FileReader(inter));
			write = new BufferedWriter(new FileWriter(resultFile));
			String line = null;
			boolean skip = false;
			boolean head = false;
			String headBuffer= "";
			while((line = read.readLine()) != null) {
				// parse the line information in the intermdiate file
				if (line.startsWith("#line")) {
					if (head) {
						headBuffer = "";
					}
					head = true;
					String[] infors = line.split(" ");
					// replace all the " and \ in the file path
					String include = "";
					if (infors.length > 3) {
						// ·�����д��пո�
						for (int i = 2; i < infors.length; i++) {
							include +=  " " + infors[i];
						}
					} else {
						include = infors[infors.length - 1];
					}
					include = include.replace("\\\\", "\\");
					include = include.replace("\"", "").trim();
					
					// ������ϵͳ��ͷ�ļ����е����ͺͺ�������
					int i = 0;
					for (String s : systemInc) {
						if (include.startsWith(s)) {
							libIncludes.add(include);
							skip = false;
						}
						i++;
						break;
					}
					if (i == systemInc.size()) {
						skip = false;
					}
					if (!skip) {
						//addInFile(include);
					}
				} else if (line.trim().length() != 0&& !line.trim().startsWith("#")){
					head = false;
				}
				// filter all the system include file copy
				if (!skip) {
					if (head) {
						headBuffer += (line + "\r\n");
					} else {
						if (headBuffer.length() != 0) {
							write.write(headBuffer.toString());
							headBuffer = "";
						}
						write.write(line);
						write.write("\n");
					}
				}
			}
			inter.delete();
		} catch (Exception e) {
			return interFile;
		}
		//added finally by liuyan 2015.6.3
		finally{
			if( read != null ){
				try {
					read.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if( write != null ){
				try {
					write.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	public boolean isError()
	{
		return isError;
	}

	public static void main(String[] args) throws IOException
	{
		// List<String> include = new ArrayList<String>();
		// include.add("D:\\Program Files\\Microsoft Visual Studio
		// 8\\VC\\PlatformSDK\\Include");
		// include.add("D:\\Program Files\\Microsoft Visual Studio
		// 8\\VC\\include");
		// include.add("D:\\cygwin\\usr\\include");
		// include.add("C:\\Test\\SourceCode");
		// Pretreatment pre = new Pretreatment();
		// pre.pretreat("c:\\Test\\SourceCode\\file\\access.c", include);
		// pre.pretreat("c:\\test\\test.c", include);
		// batchPretreat("C:\\Test\\cximage599c_full\\jpeg");

		// batchPretreat("C:\\Test\\gui");
	}

	public void batchPretreat(String path, List<String> interFileName)
	{
		File srcFile = new File(path);
		if (!srcFile.exists())
		{
			throw new RuntimeException("Error: File " + srcFile.getName()
					+ " does not exist.");
		}
		List<String> fileList = new ArrayList<String>();
		collect(fileList, srcFile);

		for (String file : fileList)
		{
			String fileName = this.pretreat(file, include, macro);
			if (interFileName != null)
			{
				interFileName.add(fileName);
			}
		}
	}

	private static void collect(List<String> fileList, File srcFile)
	{
		if (srcFile.isFile()
				&& (srcFile.getName().endsWith(".c") || srcFile.getName()
						.endsWith(".C")))
		{
			fileList.add(srcFile.getPath());
		} else if (srcFile.isDirectory())
		{
			File[] fs = srcFile.listFiles();
			for (int i = 0; i < fs.length; i++)
			{
				collect(fileList, fs[i]);
			}
		}
	}

	public void setInclude(List<String> include) {
		this.include = include;
	}

	public void setMacro(List<String> macro) {
		this.macro = macro;
	}

	public List<String> getInclude() {
		return include;
	}

	public List<String> getMacro() {
		return macro;
	}

	public Set<String> getLibIncludes() {
		return libIncludes;
	}
}