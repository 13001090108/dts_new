package softtest.test.c.gcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * �������ڲ���JUNIT���������������еĲ����������ļ��ľ���·����
 *  FileListFactory(String dirPath)ִ�к󣬽���ӡ���������Ŀ���ļ��м��ɡ�
 * @author zys
 */
public class TestCaseGenerator
{
	private static final String filePathgcc="testcase/gcc/gcc.dg";
	private static final String filePathkeil="testcase/keilc";
	
	/**
	 * 1��ͨ���ⲿ��������GCC�µ�Դ�ļ���������г���dg-error�����ʾ��Դ�ļ�������󣬽�
	 * ����Դ�ļ��б���Ϊһ�������ļ�������Ϊ��testcase/gcc/errorfiles.txt��
	 * 2�������ɲ�������ʱ�����ȴ�����errorfiles.txt�����ɴ���Դ�ļ��б�
	 * 3����ɨ����ӦĿ¼ʱ����ɨ�赽��Դ�ļ����б����ļ��Աȣ�����������ʾ���ļ��������
	 * �����ɲ�������
	 */
	private static final String errorFilePath="testcase/gcc/errorfiles.txt";
	public static void main(String[] args)
	{
		ReadErrorSrcFiles(errorFilePath);
		FileListFactory(filePathgcc);
	}

	/**
	 * <p>
	 * ��������JUNIT���������������еĲ������ݣ������������ļ��ľ��Ե�ַ
	 * </p>
	 * @param dirPath
	 * 		Ŀ���ļ��е�·����ַ
	 * @return 
	 * 		��ӡ���ɵ��ļ���ַ�б�
	 */
	static StringBuilder sb=new StringBuilder();
	static int i=0;
	public static String FileListFactory(String dirPath)
	{
		File dir=new File(dirPath);
		if(!dir.isDirectory())
		{
			return dir.getAbsolutePath();
		}
		
		File[] fileList=dir.listFiles();
		int fileNum=fileList.length;
		for(int j=0;j<fileNum;j++)
		{
			if(fileList[j].isDirectory())
			{
				//FileListFactory(fileList[j].getAbsolutePath());
			}else {
				String filePath=fileList[j].getAbsolutePath();
				if(errorFiles.contains(filePath))
				{
					continue;
				}
				filePath=filePath.replace("\\","/");
				if(filePath.endsWith(".c") || filePath.endsWith(".C"))
				{
					/*sb.append("public void testSrcFile"+i+"() throws Exception\n{\n");
					sb.append("\tassert(new File(\"").append(filePath).append("\").exists());\n").append(
								"\tassertTrue(\"Parsed Failed\", setUp(\"").append(filePath).append("\"));\n}\n");
					System.out.println(sb.toString());*/
					sb.append("\n///////////////////  "+(i)+"   ///////////////////\n");
					sb.append("{\n\t\""+filePath+"\"\n"+"\t,"+"\n"+"\t\"OK\""+"\n},");
					System.out.print(sb.toString());
					i++;
				}
			}
			sb.delete(0, sb.length());
		}
		return sb.toString();
	}

	private static List<String> errorFiles = new ArrayList<String>();
	public static void ReadErrorSrcFiles(String errorFilePath)
	{
		File errorFile=new File(errorFilePath);
		if(!errorFile.exists())
			return;
		try
		{
			BufferedReader br=new BufferedReader(new FileReader(errorFile));
			String path;
			while((path=br.readLine())!=null)
			{
				int i=path.lastIndexOf(".c");
				path=path.substring(0, i+2);
				errorFiles.add(path);
			}
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
		
	}
}
