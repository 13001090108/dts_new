package softtest.dscvp.c; 

import java.io.BufferedReader;  
import java.io.BufferedWriter;
import java.io.File;  
import java.io.FileOutputStream;  
import java.io.FileReader;  
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;  
  
public class FileOperation {  
    public static boolean writeTxtFile(String content,File fileName)throws Exception{ 
	 FileWriter writer=new FileWriter(fileName.getPath());
	 System.out.println("name: " + fileName.getPath());
	 try{
		 //ʹ��������캯��ʱ���������kuka.txt�ļ���
		 //���Ȱ�����ļ���ɾ������Ȼ�󴴽��µ�kuka.txt
		 writer.write(content);
		 writer.write("hello");
     } catch (IOException e){
           e.printStackTrace();
     }finally{
         writer.close();
     }
	return true;   
 }
} 