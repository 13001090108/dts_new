package softtest.tools.c.logswitch;  

import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;  
import javax.swing.JScrollPane;  
import javax.swing.JTree;  
import javax.swing.tree.DefaultTreeModel;  
 
public class GCCSwitch   
{  
    public static void main(String[] args)  
    {  
        JFrame frame = new JFrame("GCC��־����");  
        frame.setBounds(200, 200, 420, 550);  
        
        JTree tree = new JTree();  
        final CheckBoxTreeNode rootNode = new CheckBoxTreeNode("��־�ܿ���");  
        final CheckBoxTreeNode node1 = new CheckBoxTreeNode("Ԥ�������"); 
        final CheckBoxTreeNode node1_1 = new CheckBoxTreeNode("���ɳ����﷨��");  
        final CheckBoxTreeNode node1_2 = new CheckBoxTreeNode("���ɷ��ű�");  
        final CheckBoxTreeNode node1_3 = new CheckBoxTreeNode("ȫ�ֺ������÷���");     
        final CheckBoxTreeNode node2 = new CheckBoxTreeNode("��������");
        final CheckBoxTreeNode node2_1 = new CheckBoxTreeNode("�ļ�����˳��");  
        final CheckBoxTreeNode node2_2 = new CheckBoxTreeNode("ȫ�ֺ������ù�ϵ"); 
        final CheckBoxTreeNode node2_3 = new CheckBoxTreeNode("ȫ���ļ�������ϵ"); 
          
        final CheckBoxTreeNode node2_4 = new CheckBoxTreeNode("���ɳ����﷨��"); 
        final CheckBoxTreeNode node2_5 = new CheckBoxTreeNode("���ɷ��ű�"); 
        final CheckBoxTreeNode node2_6 = new CheckBoxTreeNode("�ļ���ȫ�ֺ������÷���");
        final CheckBoxTreeNode node2_6_1 = new CheckBoxTreeNode("����ļ��ں������ù�ϵ");
        final CheckBoxTreeNode node2_7 = new CheckBoxTreeNode("������ͼ����");
        final CheckBoxTreeNode node2_8 = new CheckBoxTreeNode("���㶨��ʹ��������");
        final CheckBoxTreeNode node2_8_1 = new CheckBoxTreeNode("�������ʹ��������");
        final CheckBoxTreeNode node2_9 = new CheckBoxTreeNode("�������");
        final CheckBoxTreeNode node2_9_1 = new CheckBoxTreeNode("��������������");
        final CheckBoxTreeNode node2_10 = new CheckBoxTreeNode("ʵ������");
        final CheckBoxTreeNode node2_10_1 = new CheckBoxTreeNode("״̬��ת��");
        
        node1.add(node1_1);  
        node1.add(node1_2);
        node1.add(node1_3);
        node2.add(node2_1);
        node2.add(node2_2);
        node2.add(node2_3);
        node2.add(node2_4);
        node2.add(node2_5);
        node2.add(node2_6);
        node2.add(node2_7);
        node2.add(node2_8);
        node2.add(node2_9);
        node2.add(node2_10);
        
        node2_6.add(node2_6_1);
        node2_8.add(node2_8_1);
        node2_9.add(node2_9_1);
        node2_10.add(node2_10_1);
       
        rootNode.add(node1);  
        rootNode.add(node2); 
        

        //rootNode.setSelected(true);
        
        DefaultTreeModel model = new DefaultTreeModel(rootNode);  
        
        tree.addMouseListener(new CheckBoxTreeNodeSelectionListener());  
        tree.setModel(model);  
        tree.setCellRenderer(new CheckBoxTreeCellRenderer());  
        JScrollPane scroll = new JScrollPane(tree);  
        scroll.setBounds(0, 0, 300, 320);  
        frame.getContentPane().add(scroll);  
       
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        frame.setVisible(true);
        
        frame.addWindowListener(new WindowAdapter() {
        	public void windowClosing(WindowEvent arg0){
        		boolean[] WTF;
        		WTF = new boolean[25];
        		WTF[0]=rootNode.isSelected();
        		WTF[1]=!(node1.isSelected());
        		WTF[2]=node1_1.isSelected();
        		WTF[3]=node1_2.isSelected();
        		WTF[4]=node1_3.isSelected();
        		WTF[5]=!(node2.isSelected());
        		WTF[6]=node2_1.isSelected();
        		WTF[7]=node2_2.isSelected();
        		WTF[8]=node2_3.isSelected();
        		WTF[9]=node2_4.isSelected();
        		WTF[10]=node2_5.isSelected();
        		WTF[11]=node2_6.isSelected();
        		WTF[12]=node2_6_1.isSelected();
        		WTF[13]=node2_7.isSelected();
        		WTF[14]=node2_8.isSelected();
        		WTF[15]=node2_8_1.isSelected();
        		WTF[16]=node2_9.isSelected();
        		WTF[17]=node2_9_1.isSelected();
        		WTF[18]=node2_10.isSelected();
        		WTF[19]=node2_10_1.isSelected();
        		WriteToFile(WTF);
        	}  	
		});
    }  
    
    public static void WriteToFile(boolean[] x){
    	File file = new File("config\\config.ini");
    	//String[] arr = new String[10];//����arr���飬�洢���ڽ������޸ĵĿ�����Ϣ
    	String s = "";//����s�ַ������洢���ڽ������޸ĵĿ�����Ϣ
    	String temp = "";//����һ��temp��ʱ�������洢���ڶ�ȡ����һ����Ϣ
    	if(file.exists()){
    		try 
    		{
    			BufferedReader reader = new BufferedReader(new FileReader(
						file));
				String config;
				
				while((config = reader.readLine())!= null){
					if(config.startsWith("#")){
						temp = config;//temp����#��ͷ����һ�е���Ϣȫ����������
						continue;
					}
					else if(config.length() == 0){
						temp = "";//�������оͽ�temp�������
						//s = s + "\r\n";//��s������һ������
						continue;
					}
					else if(config.startsWith("-SKIP_PREANALYSIS")){
						temp = "";//�������ڽ����ϲ����Ŀ��أ��ͽ�temp��գ���������Щ��Ϣ
						continue;
					}
					else if(config.startsWith("-PreAnalysisASTRoot")){
						temp = "";//ͬ��
						continue;
					}
					else if(config.startsWith("-PreAnalysisSymbolTable")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-PreAnalysisInterMethodVisitor")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-SKIP_METHODANALYSIS")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-FileAnalysisOrder")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-GlobalFunctionCall")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-GlobalFileCallRelation")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-MethodAnalysisASTRoot")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-MethodAnalysisSymbolTable")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-MethodAnalysisInterMethodVisitor")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-CallGraph")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-MethodAnalysisControlFlowVisitor")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-MethodAnalysisDUAnalysisVisitor")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-DU")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-MethodAnalysisDomainVisitor")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-Domain")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-FSMInstanceAnalysis")){
						temp = "";
						continue;
					}
					else if(config.startsWith("-StateTransition")){
						temp = "";
						continue;
					}
					else {//�������������Ҫ������Ϣ�Ŀ���
						temp = temp + "\r\n" + config ;
						s= s + temp + "\r\n\r\n";//����Щ��Ϣ���ӵ�s�в�����һ������
						temp = "";//���temp
					}
				}
				reader.close();
    		}    
    		catch (Exception e)
			{
				System.err.println("Error in reading the config file.");
			}
    	}
    	try
    	{
    		FileWriter fw = new FileWriter(file,false);
    		
    		fw.write(s);//��s�б������Ϣ����д�뵽�ļ���
    		
    		fw.write("#�Ƿ�����Ԥ�������\r\n");
   			fw.write("-SKIP_PREANALYSIS "+x[1]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴Ԥ���������ɳ����﷨��\r\n");
   			fw.write("-PreAnalysisASTRoot "+x[2]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴Ԥ���������ɷ��ű�\r\n");
   			fw.write("-PreAnalysisSymbolTable "+x[3]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴Ԥ������ȫ�ֺ������÷���\r\n");
   			fw.write("-PreAnalysisInterMethodVisitor "+x[4]+"\r\n\r\n");
   			fw.write("#�Ƿ���������������־����\r\n");
   			fw.write("-SKIP_METHODANALYSIS "+x[5]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴�ļ�����˳��\r\n");
   			fw.write("-FileAnalysisOrder "+x[6]+"\r\n\r\n");
   			fw.write("#�Ƿ����ȫ�ֺ������ù�ϵ\r\n");
   			fw.write("-GlobalFunctionCall "+x[7]+"\r\n\r\n");
   			fw.write("#�Ƿ����ȫ���ļ�������ϵ\r\n");
   			fw.write("-GlobalFileCallRelation "+x[8]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴�������������ɳ����﷨��\r\n");
   			fw.write("-MethodAnalysisASTRoot "+x[9]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴�������������ɷ��ű�\r\n");
   			fw.write("-MethodAnalysisSymbolTable "+x[10]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴����������ȫ�ֺ������÷���\r\n");
   			fw.write("-MethodAnalysisInterMethodVisitor "+x[11]+"\r\n\r\n");
   			fw.write("#�Ƿ�����ļ��ں������ù�ϵ\r\n");
   			fw.write("-CallGraph "+x[12]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴���������п�����ͼ����\r\n");
   			fw.write("-MethodAnalysisControlFlowVisitor "+x[13]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴���������м��㶨��ʹ��������\r\n");
   			fw.write("-MethodAnalysisDUAnalysisVisitor "+x[14]+"\r\n\r\n");
   			fw.write("#�Ƿ����������ͼ(DU����ʹ����)\r\n");
   			fw.write("-DU "+x[15]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴�����������������\r\n");
   			fw.write("-MethodAnalysisDomainVisitor "+x[16]+"\r\n\r\n");
   			fw.write("#�Ƿ����������ͼ(�������)\r\n");
   			fw.write("-Domain "+x[17]+"\r\n\r\n");
   			fw.write("#�Ƿ�鿴ʵ�������׶�\r\n");
   			fw.write("-FSMInstanceAnalysis "+x[18]+"\r\n\r\n");
   			fw.write("#�Ƿ����״̬��ת������\r\n");
   			fw.write("-StateTransition "+x[19]+"");
   			
   			fw.flush();
   			fw.close();
   		} catch (FileNotFoundException e)
    	{
    		e.printStackTrace();
    	} catch (IOException e)
    	{
    		e.printStackTrace();
    	}
    }
} 


