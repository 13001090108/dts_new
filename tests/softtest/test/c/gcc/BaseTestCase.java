package softtest.test.c.gcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.ParseException;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;

/**
 * ������Ϊgcc_test��keil_test�ع���Եĸ���
 * @author zys����ɽ	2009.12.17
 *
 */
public class BaseTestCase extends TestCase
{
	/**
	 * <p>Indicates which C language standard using in the test.</p>
	 */
	private static Pretreatment pre = new Pretreatment();
	
	private static ArrayList<String> includeFiles=new ArrayList<String>();//ͷ�ļ��б�
	
	private List<String> macroList = new ArrayList<String>();//�궨���б�
	
	/**
	 * �������õı�������gcc KEIL
	 * @param platform
	 */
	public static void setPlatformType(PlatformType platform)
	{
		pre.setPlatform(platform);
	}

	/**
	 * ����ͷ�ļ�����ʼ��includeFiles
	 * @param includeDir
	 * 		ͷ�ļ�����Ŀ¼�ĵ�ַ
	 */
	public static void InitIncludeFiles(String includeDir)
	{
		File dir=new File(includeDir);
		if(!dir.exists())
			return;
		includeFiles.add(includeDir);//�Ƚ���ǰĿ¼����ͷ�ļ�Ŀ¼
		File[] fileList=dir.listFiles();
		int length=fileList.length;
		for(int i=0;i<length;i++)
		{
			if(fileList[i].isDirectory())
				includeFiles.add(fileList[i].getAbsolutePath());
		}
	}
	
	/**
	 * ����Ĳ��Է����ࣺ�ȵ���Pretreatment��Դ������ļ�����Ԥ������.c�ļ�����Ϊ.i�ļ���Ȼ��
	 * ����CParser���з���
	 * @param caseFileName
	 * 		�������ļ���·�����������ļ���Ŀ¼
	 * @return
	 * 		true,��ʾ���ļ������ɹ���
	 * 		false,��ʾ����ʧ�ܻ�����쳣
	 */
	public boolean test(String caseFileName)
	{
		File caseFile = new File(caseFileName);//��������CԴ�ļ���Դ�ļ���
		List<String> interFileName = new ArrayList<String>();//Ԥ�������ļ��б�
		
		if(!caseFile.exists())
			return false;
		if (caseFile.isFile())
		{
			String interFilePath = pre.pretreat(caseFile.getAbsolutePath(),
					includeFiles, macroList);
			interFileName.add(interFilePath);
		}
		if (caseFile.isDirectory())
		{
			pre.batchPretreat(caseFile.getAbsolutePath(),interFileName);
		}

		CParser parser;
		java.io.InputStream input;
		for(String srcFile:interFileName)
		{
			try
			{
				input = new java.io.FileInputStream(new java.io.File(srcFile));
				CCharStream cs = new CCharStream(input);
				parser = CParser.getParser(cs);

				parser.TranslationUnit();
			} catch (java.io.FileNotFoundException e)
			{
				System.out.println("File  not found:"+srcFile);
				return false;
			} catch (ParseException e)
			{
				e.printStackTrace();
				System.out.println("\nEncountered errors during parse.In File:"+srcFile);
				return false;
			} 
		}
		return true;
	}

	private static String CONFIG_FILE = "config/config.ini";//�û������ļ��������趨���õı�����ΪGCC��KEIL
	private final static String NOTE_PREFIX = "#";//�û��������������������ͷ�ļ�
	/**
	 * 		��CONFIG_FILE�ж�ȡ�û����õı���������
	 * @return
	 * 		����ֵΪkeil �� gcc
	 */
	public static String initFileType()
	{
		File configFile = new File(CONFIG_FILE);
		if (configFile.exists())
		{
			try
			{
				BufferedReader reader = new BufferedReader(new FileReader(
						configFile));
				String config;
				while ((config = reader.readLine()) != null)
				{
					if (config.trim().startsWith(NOTE_PREFIX) 
							|| config.trim().startsWith("-D"))
					{
						continue;
					}else if(config.trim().startsWith("-I"))
					{
						//GCC������ⲿ�Զ���ͷ�ļ�����ϵͳͷ�ļ���
						//includeFiles.add(o);//��config.ini�ļ��ж�ȡ�ⲿ�����ͷ�ļ�
						continue;
					}else if (config.trim().equalsIgnoreCase("-gcc"))
					{
						CParser.setType("gcc");
						return "gcc";
					} else if (config.trim().equalsIgnoreCase("-keil"))
					{
						CParser.setType("keil");
						return "keil";
					}
				}
			} catch (Exception e)
			{
				System.err.println("Error in reading the config file.");
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
}
