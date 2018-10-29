package softtest.dts.c;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import softtest.config.c.Config;

public class DTSCFrame extends JFrame implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String LOG_DIR = "log";
	
	/**
	 * The label to show the progress of analysing
	 */
	JProgressBar jpb;
	
	/**
	 * The label to show the number of source codes file analysed
	 */
	JLabel jlfilecount;
	
	/**
	 * The label to show the time consumed when analysing
	 */
	JLabel jltime;
	
	/**
	 * The label to show the number of source codes line analysed
	 */
	JLabel jllinenumber;
	
	/**
	 * The label to show the number of source codes line analysed
	 */
	JPanel jlbutton;
	
	JLabel jreportIp;
	
	JLabel jtotalIp;
	JLabel jinfo;
	Container jp;
	/**
	 * The label to show the log of analysing
	 */
	private JButton jbutton;
	
	public DTSCFrame() {
		init();
	}
	
	private void init() {
		jpb=new JProgressBar();
		jlfilecount=new JLabel();
		jltime=new JLabel();
		jllinenumber=new JLabel();
		jreportIp = new JLabel();
		jtotalIp = new JLabel();
		jinfo=new JLabel();
		
		jlbutton=new JPanel();
		this.setResizable(false);
		this.setLocation(400,300);
        this.setSize(525,225);
        jp=this.getContentPane(); 
        if(Config.SHOW_DIALOG)
        	this.setVisible(true);
        else
        	this.setVisible(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //dongyk 20121023Ԥ����Ľ��� �� �����Ľ��� �ֱ������ʾ
        if (Config.DTS_LANGUAGE == Config.LANG_CHINESE) {
        	if (Config.ISTRIAL) {
        		this.setTitle("(���ð�)����Ԥ����");
        	} else {
        		this.setTitle("����Ԥ����");
        	}
        }
        if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
        	if (Config.ISTRIAL) {
        		this.setTitle("DTSGCC (trial version) is preprocessing");
        	} else {
        		this.setTitle("DTSGCC is preprocessing");
        	}
        }
		jp.setLayout(null);
			
		jpb.setBounds(10,40,500,30);
		jlfilecount.setBounds(10,80,500,30);
//		jpb.setBounds(10,10,500,30);
//		jlfilecount.setBounds(10,40,500,30);
		jllinenumber.setBounds(10,80,500,30);
		jltime.setBounds(10,120,500,30);
		jreportIp.setBounds(10,160,500,30);
		jtotalIp.setBounds(210,160,500,30);
		jinfo.setBounds(10,150,500,30);
		
		jp.add(jpb);
		jp.add(jlfilecount);
//		jp.add(jllinenumber);
//		jp.add(jltime);		
//		jp.add(jreportIp);
//        jp.add(jtotalIp);
//        jp.add(jinfo);
        
        if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
        {
			jlfilecount.setText("�ļ���: ");
			jllinenumber.setText("����������: ");
			jltime.setText("����ʱ��: ");
			jreportIp.setText("����IP��: ");
			jtotalIp.setText("����IP��: ");
			jbutton = new JButton("�鿴��־");
        }
        if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
		{
			jlfilecount.setText("file amount: ");
			jllinenumber.setText("total line amount: ");
			jltime.setText("analysis time: ");
			jreportIp.setText("reported IP amount: ");
			jtotalIp.setText("remained IP amount: ");	
			jbutton = new JButton("check the log");
		}
		//jdh
		if(!Config.ISTRIAL){
			jreportIp.setVisible(false);
			jtotalIp.setVisible(false);
		}
		
		jbutton.addActionListener(this);
		
		jlbutton.setBounds(220,150,500,40);
		
//		jp.add(jlbutton);	
	}
	
	/**
	 * Set the error message after analysing
	 * @param msg
	 */
	public void setMessage(String msg) {
		jlfilecount.setText(msg);
		jllinenumber.setText("");
		jltime.setText("");
		jlbutton.add(jbutton);
	}
	
	/**
	 * update the progress bar of the dtscpp gui
	 * @param prog
	 */
	public void updateProg(int prog) {
		jpb.setValue(prog);
    	jpb.setStringPainted(true);
	}
	
	/**
	 * Finish the analyse within the time 
	 * @param fileNum the number of source file analysed
	 * @param line the line of source code analysed
	 * @param time the time consumed when analysing
	 */
	public void finish(int fileNum, int line, long time, int limitedIp, int totalIp) {
		if (Config.DTS_LANGUAGE == Config.LANG_CHINESE)
		{
			if (Config.ISTRIAL) {
				this.setTitle("DTSGCC�����ð棩��������");
	        } else {
	        	this.setTitle("DTSGCC��������");
	        }
			jpb.setValue(100);
	    	jpb.setStringPainted(true);
	    	jlfilecount.setText("�ļ���: "+ fileNum +"  ��");
			jllinenumber.setText("����������: "+ line + "  ��");
			jltime.setText("����ʱ��: "+ time +"  ��");
			jreportIp.setText("����IP����"  + limitedIp);
			jtotalIp.setText("����IP����" + (totalIp - limitedIp));			
		}
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH)
		{
			if (Config.ISTRIAL) {
				this.setTitle("Analysis of DTSGCC��Trial Version�� terminated");
	        } else {
	        	this.setTitle("Analysis of DTSGCC terminated");
	        }
			jpb.setValue(100);
	    	jpb.setStringPainted(true);
	    	jlfilecount.setText("file amount: "+ fileNum +"  ��");
			jllinenumber.setText("total line amount: "+ line + "  ��");
			jltime.setText("analysis time: "+ time +"  ��");
			jreportIp.setText("reported IP amount��"  + limitedIp);
			jtotalIp.setText("remained IP amount��" + (totalIp - limitedIp));			
		}
		jlbutton.add(jbutton);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == jbutton) {
			Process rt = null;
			try {
				rt = Runtime.getRuntime().exec("notepad.exe "+Config.FILE);//, null, new File(LOG_DIR));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
	}
}
