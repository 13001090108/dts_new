package softtest.fsmanalysis.c;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import org.apache.log4j.Logger;

import softtest.DefUseAnalysis.c.DUAnalysisVisitor;
import softtest.ast.c.*;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.callgraph.c.DumpCGraphVisitor;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cluster.c.SimpleBean;
import softtest.config.c.Config;
import softtest.config.c.SuccessRe;
import softtest.database.c.DBAccess;
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.dscvp.c.DSCVP;
import softtest.dscvp.c.DSCVPElement;
import softtest.dscvp.c.GenerateDSCVP;
import softtest.dts.c.DTSC;
import softtest.fsm.c.FSMLoader;
import softtest.fsm.c.FSMMachine;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterMethodVisitor;
import softtest.interpro.c.InterContext;
import softtest.pretreatment.Pretreatment;
import softtest.scvp.c.SCVPPrintVisitor;
import softtest.scvp.c.SCVPString;
import softtest.scvp.c.SCVPVisitor;
import softtest.summary.lib.c.LibManager;
import softtest.symboltable.c.*;
import softtest.tools.c.jaxen.MatchesFunction;


/**
 * <p>
 * C source file analyser, analyses the source files with the finite state machine,
 * and records the results into database.
 * </p>
 */
public class CAnalysis {
	public static String LIB_SUMMARYS_PATH;

	private static Logger logger = Logger.getRootLogger();
	
	private static List<AnalysisElement> elements;
	
	private FSMControlFlowData cfData;

	
	/** ͷ�ļ�����*/
	public static TreeSet<String> incSet = new TreeSet<String>();
	/**
	 * �����ļ�ʱ��ͳ������
	 */
	private int failFileNum;
	private int percent = 0;
	//dongyk 20121023Ԥ����Ľ���
	private int prePercent = 0;
	private int count = 0;
	private int limitedNum = 0;
	private Pretreatment pre;
	private List<AnalysisElement> orders;
	
	//qian-2011-02-25
	private static AnalysisElement curElement;

	
	/**
	 * ȫ�ַ����ĺ������ù�ϵͼ
	 */
	private InterCallGraph interCGraph;
	
	private static long syntaxTreeTime =0;
	private static long symbolTableTime =0;
	private static long globalAnalysisTime =0;
	private static int timeOutfiles =0;
	
	
	public CAnalysis(List<String>files, Map<String, String> fsmList) {
		
		cfData = new FSMControlFlowData();
		failFileNum = 0;
		
		//�������ݿ������ļ���ѡ�е��Զ���
		for (String fsmPath : fsmList.keySet()) {
			FSMMachine fsm = FSMLoader.loadXML(fsmPath);
			fsm.setType(fsmList.get(fsmPath));
			FSMAnalysisVisitor.addFSMS(fsm);
		}
		elements = new ArrayList<AnalysisElement>();
		for (String file : files) {
			elements.add(new AnalysisElement(file));
		}
		Collections.sort(elements);
		interCGraph = InterCallGraph.getInstance();
	}
	
	private class PreAnalysisThread extends Thread
	{
		AnalysisElement element;
		String temp;
		public PreAnalysisThread(AnalysisElement element,String temp)
		{
			this.element=element;
			this.temp=temp;
		}
		public void run()
		{
			try {
				long start = System.currentTimeMillis();
				CParser parser = CParser.getParser(new CCharStream(new FileInputStream(temp)));
				//��һ�������ɳ����﷨��AST
				ASTTranslationUnit astroot = parser.TranslationUnit();
				long end = System.currentTimeMillis();
				if(!Config.SKIP_PREANALYSIS){
					if(Config.PreAnalysisASTRoot){
						logger.info("Ԥ����1��"+element+"�����﷨����ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
					}
				}
				
				//�ڶ��������ɷ��ű����������������������ʽ���ͷ�����
				start = System.currentTimeMillis();
				ScopeAndDeclarationFinder sc = new ScopeAndDeclarationFinder();
				astroot.jjtAccept(sc, null);
				OccurrenceAndExpressionTypeFinder expTypeFinder = new OccurrenceAndExpressionTypeFinder();
				astroot.jjtAccept(expTypeFinder, null);
				end = System.currentTimeMillis();
				if(!Config.SKIP_PREANALYSIS){
					if(Config.PreAnalysisSymbolTable){
						logger.info("Ԥ����2��"+element+"���ɷ��ű��ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
					}
				}
				
				//��������zys:����ȫ�ֺ������÷���
				start = System.currentTimeMillis();
				astroot.jjtAccept(new InterMethodVisitor(), element);
				end = System.currentTimeMillis();
				if(!Config.SKIP_PREANALYSIS){
					if(Config.PreAnalysisInterMethodVisitor){
						logger.info("Ԥ����3��"+element+"���ȫ�ַ�����ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
					}
				}
				
			} catch (Exception e) {
				element.setCError(true);
				logger.error("Exception in pre process file " + element.getFileName(), e);
			} catch (Error er) {
				element.setCError(true);
				logger.error("Error in pre process file " + element.getFileName(), er);
			} finally {
				System.gc();
			}
			synchronized (cfData) {
				cfData.notify();
			}
		}
	}
	/**
	 * ��Դ�ļ�����Ԥ����������Ԥ������м��ļ���Ԥ����
	 * �õ�����ȫ�ֵĺ������ù�ϵ���Լ�������Դ�ļ���������ϵ
	 *
	 */
	public void preAnalyse() {
		if(Config.GCC_TYPE==1 || Config.ANALYSIS_I){
			String headers[]=Config.LIB_HEADER_PATH.split(";");
			for(int i=0;i<headers.length;i++){
				Pretreatment.systemInc.add(headers[i]);
			}
		}
		int index=0;
		for(AnalysisElement element : elements) {
			String fileName = element.getFileName();
			String temp = null;
			//zh 2013-05-13
			DTSC.setFileName(fileName);
			index++;
			prePercent = (((int)((index*1.0/elements.size())*100))%100 )+ 1; 
			
			if(Config.ANALYSIS_I){
				temp = fileName;
				pre.findIncludeLib(temp);
				if(!Config.SKIP_PREANALYSIS)
					logger.info("[��ʼԤ�����ļ�:" + fileName + "]");
			}else{
				if(!Config.SKIP_PREANALYSIS)
					logger.info("[��ʼԤ�����ļ�:" + fileName + "]");
				// ��Դ�����ļ�����Ԥ����
				temp = pre.pretreat(fileName, pre.getInclude(), pre.getMacro());
			}
			//
			if (Config.ANALYSIS_R && Config.InterFile_Simplified) {
				temp = findLib(temp);
			}
			if(Config.ANALYSIS_I ){
				if(Config.FILEREPLACE && Config.InterFile_Simplified)
					temp = findLibAndReplace(temp);
				else if(Config.FILEREPLACE && !Config.InterFile_Simplified)
					temp = fileReplace(temp);
				else if(!Config.FILEREPLACE && Config.InterFile_Simplified)
					temp = findLib(temp);
			}
			
			if (temp == null || pre.isError()) {
				failFileNum++;
				element.setCError(true);
				continue;
			}
			element.setInterFileName(temp);
			if(Config.ANALYSIS_I){
				element.setFileName( setIFileName(fileName) );
			}
			
			//�Ƿ�����Ԥ����׶�
			if(!Config.SKIP_PREANALYSIS){
				try {
					PreAnalysisThread thread = new PreAnalysisThread(element,temp);
					long start = System.currentTimeMillis (); 
					thread.setDaemon(true);
					
					synchronized (cfData) {
						thread.start();
						try{
							cfData.wait(Config.ASTTREE_TIMEOUT);
						}catch(InterruptedException e){
							logger.info("[Interrupt error in waiting for the ASTTREE building of File " + fileName + " for time out]");
							e.printStackTrace();
						}
					}
					long end = System.currentTimeMillis (); 
					if(end-start>=Config.ASTTREE_TIMEOUT){
						thread.stop();
						element.setCError(true);
						logger.error("[Stop the pre-analysis of File " + fileName + " for ASTTREE building time out]");
					}
				} catch (Exception e) {
					logger.error("Exception in pre-analysis: " + element, e);
				} catch (Error error) {
					logger.error("Error in pre-analysis: " + element, error);
				} finally {
					System.gc();
				}
			}
				
			// remove the .. or . directory in the file path
			try {//TODO 
				//����linux�����ɵ�.i�ļ���·����ʽ��windows��ͬ��
				//����ֱ�ӷ���linux �����ɵ�.i�ļ�ʱgetCanonicalPath()�õ��Ľ���᲻��ȷ,��ʱ��������һ��
				fileName = element.getFileName();
				fileName = fileName.replace('\"', ' ').trim();
				if(!Config.ANALYSIS_I){
					fileName = (new File(fileName)).getCanonicalPath();
				}
				element.setFileName(fileName);
			} catch (Exception e) {	
				logger.error("Exception in pre process file " + fileName, e);
				e.printStackTrace();
			}
			if(!Config.SKIP_PREANALYSIS){
				logger.info("[����]\n");
//				index++;
//				prePercent = (((int)((index*1.0/elements.size())*100))%100 )+ 1; 
			}
		}
	}
	
	/**
	 * ���ļ�Ϊ��λ����C/c����,���ж��α�������
	 * ��һ�ζ�Դ�ļ�����Ԥ��������Ԥ����õ����м����Ԥ�������õ��ļ����������˹�ϵ�ͺ�������ͼ
	 * �ڶ��α���������˳����ļ����й��̼����
	 * 
	 * @return ������Դ���������
	 */
	public int analyse(String dbfilename) {
		deleteAndCreateTemp(Config.PRETREAT_DIR);
		
		//zys:����Դ�ļ���Ԥ����֮���ļ����к�ӳ�俪��
		Config.COMPUTE_LINE_NUM=true;
		// chh ��ʼ������ʶ�����Ҫ�滻���ַ������� 
		if(Config.FILEREPLACE)
			UnknownString.setReplaceString();
		//�Ƿ�����Ԥ����׶Σ�����һ���ľ�����ʧ
		SuccessRe.check();
		preAnalyse();
		
		if (Config.USE_SUMMARY) {
			// �������ɿ⺯����ժҪ��Ϣ
			LibManager libManager = LibManager.getInstance();
			libManager.loadLib(LIB_SUMMARYS_PATH);
			Set<MethodNameDeclaration> libDecls = libManager.compileLib(pre.getLibIncludes());
			InterContext interContext = InterContext.getInstance();
			interContext.addLibMethodDecl(libDecls);
		}
		int index = 0;
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.FileAnalysisOrder)
					logger.info("���������ļ��������򡭡�����");
			}
			//�������Ԥ����������ĸ����������ļ�
			if(Config.SKIP_PREANALYSIS){
				orders=elements;
			}else{
				orders = interCGraph.getAnalysisTopoOrder();
				//liuli��2010.8.2 
				Iterator<AnalysisElement> iterator = orders.iterator();
				while(iterator.hasNext()){
					AnalysisElement order = iterator.next();
					if(order.getInterFileName() == ""){
						iterator.remove();
					}
				}
				// ����е��ļ�û���ں�������ͼ�ϳ��֣���ֱ�ӽ�������ӵ������б����
				if (orders.size() > elements.size()) {
					logger.error("�ļ����������쳣�������������ļ������ڴ������ļ�����");
				}else if (orders.size() < elements.size()) {
					for (AnalysisElement element : elements) {
						boolean exist = false;
						for (AnalysisElement order : orders) {
							if (order == element) {
								exist = true;
								break;
							}
						}
						if (!exist) {
								orders.add(element);
								
						}
					}
				}
			}
		if(!Config.SKIP_METHODANALYSIS){
			if(Config.FileAnalysisOrder){
				logger.info("�ļ��������������ɡ�������");
				logger.info("�����������ļ�������"+orders.size()+"\t���������ļ�����:"+elements.size()+"\n");
				if(orders.size()!=elements.size())
					logger.error("�ļ����������쳣�������������ļ���С�ڴ������ļ�����");
			}
		}
		
		DBAccess dbAccess = DBAccess.getInstance();
		cfData.setDB(dbAccess);
		dbAccess.openDataBase(dbfilename);
		/*xwt 2011.11.3*/
		if(Config.TRIAL_OUTPUT_ALL){
			dbAccess.openComplementDB(dbfilename.substring(0, dbfilename.indexOf("."))+"_.mdb");
		}
		String fileName = null;
		SuccessRe.check();
		index = 0;
		for(AnalysisElement element : orders) {
			index++;
			//dongyk 20121023Ԥ����Ľ��� �� �����Ľ��� �ֱ������ʾ
			percent = ((int)(index*1.0/orders.size()*100))%100+1;
			if (element.isCError() || element.getFileName().matches(InterContext.INCFILE_POSTFIX) ) {
				continue;
			}
			
			
			if(Config.ANALYSIS_I){
			//chh �滻�����ʱ�ļ���_temp.i��β����ʾ�ļ���ʱҪ��Ӧ����Ӧ��Դ.i�ļ�
				fileName = element.getInterFileName().replace(".temp", ".i");
			}else{
				fileName = element.getFileName();
			}
			
			// �����µķ����̣߳�����Դ����
			DTSC.setFileName(fileName);
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.FSMInstanceAnalysis){
					logger.info("����ʼ���������ļ� " + fileName + "��");
				}
			}
			try {
				AnalysisThread thread = new AnalysisThread(element);
				long start = System.currentTimeMillis (); 
				thread.setDaemon(true);
				
				synchronized (cfData) {
					thread.start();
					try{
						cfData.wait(Config.TIMEOUT);
					}catch(InterruptedException e){
						logger.info("[Interrupt error in waiting for the analysis of File " + fileName + " for time out]");
						e.printStackTrace();
					}
				}
				long end = System.currentTimeMillis (); 
				if(end-start>=Config.TIMEOUT){
					thread.stop();
					timeOutfiles++;
					logger.info("[Stop the analysis of File " + fileName + " for time out]");
				}
			} catch (Exception e) {
				logger.error("Exception in analysis: " + element, e);
			} catch (Error error) {
				logger.error("Error in analysis: " + element, error);
			} finally {
				System.gc();
			}
			if(!Config.SKIP_METHODANALYSIS){
				logger.info("[����]\n");
			}
//			index++;
//			//dongyk 20121023Ԥ����Ľ��� �� �����Ľ��� �ֱ������ʾ
//			percent = ((int)(index*1.0/elements.size()*100))%100+1;
		}
		//chh  ɾ���ֹ����滻����ʱ�ļ�
		for(AnalysisElement element : elements) {
			if(element.getInterFileName().endsWith(".temp")){
				File tempfile=new File(element.getInterFileName());
				tempfile.delete();
			}
		}
		if (Config.ISTRIAL) {
			trialOutput(dbAccess);
		}
		
		if(Config.STEP_TIME_TRACE){
			logger.info("���ɳ����﷨������ʱ��: " + syntaxTreeTime+ "(ms).");
			logger.info("���ɷ��ű���ʱ��: " + symbolTableTime + "(ms).");
			logger.info("ȫ�ַ�������ʱ��: " + globalAnalysisTime + "(ms).");
		}
		
		/** ytang add 20161109*/
		//�����ݿ����ٴζ�ȡDip�����ж��μ���
		if(Config.Cluster)
			computeDipByString(dbAccess, dbfilename);//add by JJL,2016-12-4
		
/**		
		try{
			Hashtable<Integer, String> dscvpmap = dbAccess.readDSCVP();
			for(Iterator iter=dscvpmap.keySet().iterator(); iter.hasNext(); )   {   
	            Integer key = (Integer)iter.next();   
	            String value = dscvpmap.get(key);   
	    	    Pattern pp=Pattern.compile("[A-Za-z1-9]* \\[structure=entrance of function[^\\|\\&]+\\]\\]");
	    	    Matcher mm=pp.matcher(value);
	    	    int i = 0;
	    	    String cp = new String(value);
	    	    while(mm.find()){
	    		     String scvp = mm.group(0);
	    		     int s = mm.start();
	    		     int e = mm.end();
	    		     //System.out.println(value.substring(s, e));
	    		     HashSet<SCVPString> scvps = new HashSet<SCVPString>();
	    		     GenerateDSCVP.getMethodVariableSCVP(value.substring(s, e), scvps);
	    		     StringBuilder replacement = new StringBuilder();
	    		     Iterator iter1 = scvps.iterator();
	    		     while (iter1.hasNext()){
	    		    	 SCVPString ss = (SCVPString)iter1.next();
	    		    	 replacement.append(ss.toString());
	    		     }
	    		     if (replacement.length() > 0){
	    		       String ss = cp.replace(value.substring(s, e), replacement.toString());
	    		       cp = ss;
	    		     }
	    		     System.out.println(cp);
	    	    }
	    	    dbAccess.writeDSCVP(key, cp);
	            
	        } 
		}catch(Exception e){
			e.printStackTrace();
		}
		
		dbAccess.closeDataBase();
*/		

		/*xwt 2011.11.3*/
		if(Config.TRIAL_OUTPUT_ALL){
			dbAccess.closeComplementDB();
		}
		percent = 100;
		return count;
	}
	
	/**���μ���IP��ֵ*/
	public void computeDipByString(DBAccess dbAccess, String dbfilename) {
		List<SimpleBean> dbList = dbAccess.readAccess();
		dbAccess.closeDataBase();
		
		System.out.println("����dblist����ʼ���μ���");
		for (int i = 0; i < dbList.size(); i++) {
			System.out.println("��ʼ����" + i);
			SimpleBean sb = dbList.get(i);
			DSCVPElement eroot = sb.getF();
			String stringF = sb.getStringF();
			if (stringF != null && stringF.contains("entrance of function")) {
				if (eroot != null) {
					//��һ��û��SCVP��ֱ��getchild
					HashMap<String, HashSet<DSCVPElement>> child = eroot.getChild();
					for (Entry<String, HashSet<DSCVPElement>> entry : child.entrySet()) {
						String key = entry.getKey();
						HashSet<DSCVPElement> value = child.get(key);
						computeDipInSet(value);
					}
				}
			}
		}

		try{
			dbAccess.openDataBase(dbfilename);
			//����DSCVP�� ������dblist�е�stringF
			updateDSCVP(dbList);
			dbAccess.writeAccessToDSCVP(dbList);
		}catch(Exception e){
			e.printStackTrace();
		}
		DTSC.dbList = dbList;
		dbAccess.closeDataBase();
	}
	
	/**����dblist��DSCVP*/
	public void updateDSCVP(List<SimpleBean> dbList) {
		for (int i=0; i<dbList.size(); i++) {
			SimpleBean sb = dbList.get(i);
			DSCVPElement eroot = sb.getF();
			String DSCVPString = getStringByDSCVPElement(eroot);
			sb.setStringF(DSCVPString);
		}
	}
	
	/**����DSCVPElement����DSCVP*/
	public String getStringByDSCVPElement(DSCVPElement eroot) {
		if (eroot == null) 
			return null;
		DSCVP droot = new DSCVP();
		GenerateDSCVP.getDSCVP(droot, eroot);
		StringBuilder sb = new StringBuilder();
		while (droot != null){
			sb.append(droot.getDscvp().toString());
			droot = droot.getNextLayer();
		}
		return sb.toString();
	}
	
	public void computeDipInSet(HashSet<DSCVPElement> oldset) {
		if (oldset != null) {
			HashSet<DSCVPElement> newset = new HashSet<DSCVPElement>();
			for (DSCVPElement eroot: oldset) {
				computeDip(eroot, newset);
			}
			if (!newset.isEmpty()) {
				//��newset�滻oldset
				oldset.clear();
				for (DSCVPElement eroot: newset) {
					oldset.add(eroot);
				}
			}
			
		}
	}
	private HashSet<DSCVPElement> visited = new HashSet<DSCVPElement>();
	public void computeDip(DSCVPElement eroot, HashSet<DSCVPElement> set) {
		if (eroot == null)
			return;
		SCVPString scvp = eroot.getSCVP();
		if (scvp != null) {
			if (scvp.getStructure().contains("entrance of function")) {
				//System.out.println(scvp.getStructure());
				HashSet<DSCVPElement> addset = new HashSet<DSCVPElement>();
				if(!visited.contains(eroot)) {
					visited.add(eroot);
					GenerateDSCVP.getMethodVariableSCVP(eroot, scvp, addset,1);
				}
				if (addset != null && !addset.isEmpty()) {
					String condition = "";
					if (scvp.getStructure().contains("#")){//����������ֵ
						String s = scvp.getStructure();
						condition = s.substring(s.indexOf("#"), s.length());
					} else if (scvp.getStructure().contains("$")) {
						String s = scvp.getStructure();
						condition = s.substring(s.indexOf("$"), s.length());
					}
					if (condition.equals("")) {
						for (DSCVPElement d : addset){//������add��������
							set.add(d);
						}
					} else {
						for (DSCVPElement d : addset){//�������е�S����condition�ֶ�
							SCVPString methodSCVP= d.getSCVP();
							String newStructure = methodSCVP.getStructure()+condition;
							SCVPString newSCVP = new SCVPString();
							newSCVP.setStructure(newStructure);
							newSCVP.setConstants(methodSCVP.getConstants());
							newSCVP.setOccs(methodSCVP.getOccs());
							newSCVP.setPosition(methodSCVP.getPosition());
							
							d.setSCVP(newSCVP);
							set.add(d);
						}
					}
					
				} else {//����޷��ҵ�����ǰ��ժҪ�����,����ԭDSCVPElement
					set.add(eroot);
				}
				
			}
		}
		//System.out.println("���������仰˵��if������Ŷ~~");
		HashMap<String, HashSet<DSCVPElement>> child = eroot.getChild();
		for (Entry<String, HashSet<DSCVPElement>> entry : child.entrySet()) {
			String key = entry.getKey();
			HashSet<DSCVPElement> value = child.get(key);
			computeDipInSet(value);
//			if(!visited.contains(value)) {
//				visited.add(value);
//				computeDipInSet(value);
//			}
		}
	}
	
	public long  getSyntaxTreeTime(){
		return this.syntaxTreeTime;
	}
	
	public long  getSymbolTableTime(){
		return this.symbolTableTime;
	}
	
	public long  getGlobalAnalysisTime(){
		return this.globalAnalysisTime;
	}
	
	public int getTimeoutfiles(){
		return this.timeOutfiles;
	}
	
	private String setIFileName(String fileName){
		File file=null;
		BufferedReader read=null;
		String fileNameI  = "";
		try 
		{
			file=new File(fileName);
			
			if(file.exists()){
				read = new BufferedReader(new FileReader(fileName));
				String line = null;
				line = read.readLine();
				if(line!=null && line.startsWith("#")){
					String[] infors = line.split(" ");
					fileNameI = infors[2];
				}						

				fileNameI = fileNameI.replace("//", "/").trim();
				fileNameI = fileNameI.replace("\"", "").trim();
				}
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try {
				if(read!=null)
					read.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		return fileNameI;
		
	}
	private void trialOutput(DBAccess dbAccess) {
		Map<String, Integer> counts = new HashMap<String, Integer>();
		Map<String, Integer> limiteds = new HashMap<String, Integer>();
		List<Report> reports = cfData.getReports();
		String key;
		for (Report report: reports) {
			key = report.getEclass() + "_" +report.getFsmName();
			Integer num = counts.get(key);
			if (num != null) {
				counts.put(key, new Integer(num.intValue() + 1));
			} else {
				counts.put(key, new Integer(1));
				limiteds.put(key, new Integer(0));
			}
		}
		for (Report report: reports) {
			key = report.getEclass() + "_" +report.getFsmName();
			int num = limiteds.get(key).intValue();
			if (num < Config.MAXIP && num < (int)(Config.PERCENT * counts.get(key))/100.0) {
				dbAccess.exportErrorData(report.getIPMethod(),report.getEclass(), report.getFsmName(), report.getFileName(), report.getRelatedVarName(),
						report.getBeginLine(), report.getErrorLine(), report.getDesp(),
						report.getSrcCode(), report.getPreCond(), report.getTraceInfo());
				System.out.println(report.hashCode());
				limiteds.put(key, new Integer(num + 1));
			}				
		}
		for (String key1 : limiteds.keySet()) {
			limitedNum += limiteds.get(key1).intValue();
		}
		/*xwt 2011.11.3*/
		if(Config.TRIAL_OUTPUT_ALL){
			List<Report> all_reports = cfData.getAllReports();
			for (Report report: all_reports) {
				dbAccess.exportErrorData(report.getIPMethod(),report.getEclass(), report.getFsmName(), report.getFileName(), report.getRelatedVarName(),
						report.getBeginLine(), report.getErrorLine(), report.getDesp(),
						report.getSrcCode(), report.getPreCond(), report.getTraceInfo());
			}
		}
	}
	
	public int progress() {
		return percent;
	}
	
	public int preProgress() {
		return prePercent;
	}
	
	public List<Report> getReports() {
		return cfData.getReports();
	}
	
	public int getFailFileNum() {
		return failFileNum;
	}
	
	public int getErrorNum() {
		return cfData.errorNum;
	}
	
	public int getReportNum() {
		if (Config.ISTRIAL) {
			return limitedNum; 
		}
		return getErrorNum();
	}
	
	public static boolean needScan(String fileName1, String fileName2) {
		if (fileName2 == null || fileName2.trim().length() == 0) {
			return true;
		}
		// ������ϵͳͷ�ļ��еĺ�������
//		if (fileName2.matches(InterContext.INCFILE_POSTFIX)) {
//			return !Pretreatment.isSysInc(fileName2);
//		}
		String fileName11 = fileName1.replace('"', ' ').trim();
		// if the file name contain chineset chars, we just check the postfix of the path
		if (fileName2.contains("\\\\") && !fileName2.endsWith(".h")) {
			return true;
		}
		String[] temp1 = fileName2.split("/|\\\\");
		String[] temp2 = fileName11.split("/|\\\\");
		if (temp1[temp1.length - 1].contains(temp2[temp2.length - 1])) {
			return true;
		}
		return fileName11.equals(fileName2);
	}
	
	/**
	 * ɾ�����е�tempĿ¼��Ȼ�󴴽�һ���µĿյ�tempĿ¼
	 */
	public static void deleteAndCreateTemp(String tempDirPath){
		File file=new File(tempDirPath);
		if(file.exists()){
			try{
				deleteDirectory(file);
			}catch(IOException e){	
				fail("fail to delete temp file");
			}
		}
		
		file=new File(tempDirPath);
		file.mkdirs();
	}
	
	/**
	 * ɾ��һ��Ŀ¼�����������������
	 * @param dir Ŀ¼��
	 * @throws IOException ɾ��ʧ��
	 */
	
	private static void deleteDirectory(File dir) throws IOException {
		if ((dir == null) || !dir.isDirectory()) {
			throw new IllegalArgumentException("Argument " + dir
					+ " is not a directory. ");
		}
		File[] entries = dir.listFiles();
		int sz = entries.length;
		for (int i = 0; i < sz; i++) {
			if (entries[i].isDirectory()) {
				deleteDirectory(entries[i]);
			} else {
				entries[i].delete();
			}
		}
		dir.delete();
	}

	class AnalysisThread extends Thread {
		//Test:����ģʽ�Ƿ������߳�ִ�п��������Ч�ʣ�
		
		private FSMAnalysisVisitor analyser;
		private AnalysisElement element;
		
		public AnalysisThread(AnalysisElement element) {
			this.element = element;
			analyser = new FSMAnalysisVisitor(cfData);
		}
		
		@Override
		public void run() {
			try {
				doAnalyse(element); 
			} catch (ParseException e) {
				logger.error("Unknown exception when parsing the source file :" + element.getFileName(), e);
			} catch (RuntimeException e) {
				logger.error("Unknown exception when analysing the source file :" + element.getFileName(), e);
			} catch (ThreadDeath td) {
				logger.info("[Analysis thread for File " + element.getFileName() + " stop for time out]");
			} catch (Error error) {
				logger.error("Unknown error when analysing the source file :" + element.getFileName(), error);
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			synchronized (cfData) {
				cfData.notify();
			}
		}
		
		/**
		 * Ԥ������ɺ���ʽ��Դ�ļ����з������
		 * @param element
		 * @throws Exception
		 */
		
		private void doAnalyse(AnalysisElement element) throws Exception {
			SuccessRe.check();
			long start = System.currentTimeMillis();
			CParser parser = CParser.getParser(new CCharStream(new FileInputStream(element.getInterFileName())));
			//��һ�������ɳ����﷨��AST
			ASTTranslationUnit astroot = parser.TranslationUnit();
			long end = System.currentTimeMillis();
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.MethodAnalysisASTRoot)
					logger.info("������1��"+"�����﷨����ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
			}
			syntaxTreeTime += end-start;
			
			//qian-2011-02-25
			curElement = element;
			
			//�ڶ��������ɷ��ű����������������������ʽ���ͷ�����
			start = System.currentTimeMillis();
			ScopeAndDeclarationFinder sc = new ScopeAndDeclarationFinder();
			astroot.jjtAccept(sc, null);
			OccurrenceAndExpressionTypeFinder expTypeFinder = new OccurrenceAndExpressionTypeFinder();
			astroot.jjtAccept(expTypeFinder, null);
			end = System.currentTimeMillis();
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.MethodAnalysisSymbolTable)
					logger.info("������2��"+"���ɷ��ű��ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
			}
			symbolTableTime += end-start;
			
			//��������zys:����ȫ�ֺ������÷���
			start = System.currentTimeMillis();
			astroot.jjtAccept(new InterMethodVisitor(), element);
			end = System.currentTimeMillis();
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.MethodAnalysisInterMethodVisitor)
					logger.info("������3��"+"ȫ�ַ�����ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
			}
			globalAnalysisTime += end-start;
			
			//���Ĳ������ɺ������ù�ϵͼ������ӡ
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.MethodAnalysisInterMethodVisitor){
					logger.info("������4�������ļ��ں������ù�ϵͼ��ʼ����");
				}
			}
			
			start = System.currentTimeMillis();
			CGraph g = new CGraph();
			((AbstractScope)(astroot.getScope())).resolveCallRelation(g);
			List<CVexNode> list = g.getTopologicalOrderList(element);
			Collections.reverse(list);
			//�����µ��ļ��ĺ�����Ϣ�����ʽ
			//dump(g, list);
			end = System.currentTimeMillis();
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.MethodAnalysisInterMethodVisitor)
					logger.info("������4��"+"�����ļ��ں������ù�ϵͼ���������õ�ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
			}
			
			//���岽�����ɿ�����ͼ
			start = System.currentTimeMillis();
			ControlFlowVisitor cfv = new ControlFlowVisitor(element.getFileName());
			ControlFlowData flow = new ControlFlowData();
			for (CVexNode cvnode : list) {
				SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
				if (node instanceof ASTFunctionDefinition) {
					cfv.visit((ASTFunctionDefinition)node, flow);
				} 
			}
			end = System.currentTimeMillis();
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.MethodAnalysisControlFlowVisitor)
					logger.info("������5��"+"���ɿ�����ͼ��ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
			}
			if(!Config.HW_RULES_CUT){
				//�����������㶨��ʹ����
				if(!Config.SKIP_METHODANALYSIS){
					if(Config.MethodAnalysisDUAnalysisVisitor){
						logger.info("������6�����㶨��ʹ������ʼ����");
					}
				}
				
				start = System.currentTimeMillis();
				astroot.jjtAccept(new DUAnalysisVisitor(), null);
				end = System.currentTimeMillis();
				if(!Config.SKIP_METHODANALYSIS){
					if(Config.MethodAnalysisDUAnalysisVisitor)
						logger.info("������6��"+"���㶨��ʹ�������������õ�ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
				}
				//DUStatistics.print();
				//���߲��������������
				if(!Config.SKIP_METHODANALYSIS){
					if(Config.MethodAnalysisDomainVisitor){
						logger.info("������7���������������ʼ����");
					}
				}
				
				start = System.currentTimeMillis();
				ControlFlowDomainVisitor cfd = new ControlFlowDomainVisitor();
				int i=1;
				for (CVexNode cvnode : list) {
					
					long start1 = System.currentTimeMillis();
					if(!Config.SKIP_METHODANALYSIS){
						if(Config.MethodAnalysisDomainVisitor)
							logger.info("["+i+"]����{"+cvnode.getName()+"}������ʼ����");
					}
					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
					if (node instanceof ASTFunctionDefinition) {
						cfd.visit((ASTFunctionDefinition)node, null);
					} 
					end = System.currentTimeMillis();
					if(!Config.SKIP_METHODANALYSIS){
						if(Config.MethodAnalysisDomainVisitor)
							logger.info("["+i+"]����{"+cvnode.getName()+"}����������������������õ�ʱ��Ϊ��"+ (double)(end-start1)/1000+"(s)");
						++i;
					}
				}
				end = System.currentTimeMillis();
				if(!Config.SKIP_METHODANALYSIS){
					if(Config.MethodAnalysisDomainVisitor)
						logger.info("������7��"+"��������������������õ�ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
				}
			}
			//if(Config.Cluster) {
				//add by cmershen,2016.5.3 ����������������SCVPģ�ͷ���
				start = System.currentTimeMillis();
				logger.info("������7'������SCVP��ֵģ�ͷ�����ʼ����");
				astroot.jjtAccept(new SCVPVisitor(), null);
	//			for (CVexNode cvnode : list) {
	//				SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
	//				if (node instanceof ASTFunctionDefinition) {
	//					System.out.println(cvnode.toString());
	//					node.jjtAccept(new SCVPVisitor(), null);
	//				} 
	//			}
				
				end = System.currentTimeMillis();
				logger.info("������7'������SCVP��ֵģ�ͽ��������õ�ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
				//add by cmershen,2016.8.1
				
				start = System.currentTimeMillis();
				logger.info("������7.1�����SCVP��ֵ��Ϣ��ʼ����");
//				for (CVexNode cvnode : list) {
//					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
//					if (node instanceof ASTFunctionDefinition) {
//						System.out.println(cvnode.toString());
//						node.jjtAccept(new SCVPPrintVisitor(), null);
//					} 
//				}
				end = System.currentTimeMillis();
				logger.info("������7.1�����SCVP��ֵ��Ϣ���������õ�ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
			//}
//			astroot.jjtAccept(new NPDGenerateVisitor(), cfData);
//			astroot.jjtAccept(new IAOGenerateVisitor(), cfData);
//			astroot.jjtAccept(new OOBGenerateVisitor(), cfData);
			//�ڰ˲����Զ�������
			MatchesFunction.registerSelfInSimpleContext();
			if(Config.ANALYSIS_I){
				String fileName = element.getInterFileName();
				cfData.setParseFileName(getIFilePath(fileName));
			}else{
				cfData.setParseFileName(element.getFileName());
			}
			
			cfData.srcFileLine = 0;
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.FSMInstanceAnalysis){
					logger.info("������8����ʼ�����Զ�����������");
				}
			}
			start = System.currentTimeMillis();
			//added by liuyan 2015/11/4
			ArrayList<String> funcList = new ArrayList<String>();
			for(CVexNode vNode : list){
				String string = vNode.getName();
				String string2[] = vNode.getName().split("_");
				StringBuffer temp = new StringBuffer();
				for(int i=0; i<string2.length-2;++i){
					temp.append(string2[i]);
					if(i != string2.length-3){
						temp.append("_");
					}
				}
				funcList.add(temp.toString());
//				funcList.add(string.substring(0,string.length()-4));
			}
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.FSMInstanceAnalysis){
					logger.info("�����Զ��������ĺ���Ϊ��"+funcList);
				}
			}
		
			//123
			astroot.jjtAccept(analyser, cfData);
			
			end = System.currentTimeMillis();
			if(!Config.SKIP_METHODANALYSIS){
				if(Config.FSMInstanceAnalysis){
					logger.info("������8��"+"�Զ����������������õ�ʱ��Ϊ��"+ (double)(end-start)/1000+"(s)");
				}
			}
			if(Config.FSM_STATISTIC)
				logger.info("δ�ͷŵ��Զ�����"+FSMMachine.statistic);
			count += cfData.srcFileLine;
			if (!Config.SKIP_METHODANALYSIS){
				logger.info(element+" �����ļ�����Ϊ: " + cfData.srcFileLine);
				
			}

		}
		
		private void dump(CGraph g, List<CVexNode> list) {
			if(!Config.SKIP_METHODANALYSIS || Config.TRACE){
				if (Config.CallGraph || Config.TRACE) 
				{
					for(CVexNode n:list){
						System.out.print(n.getName()+"  ");
					}
					System.out.println();
					
					String name ="temp/FileInterCallGraph_" + element.getFileName().substring(element.getFileName().lastIndexOf("\\")+1, element.getFileName().lastIndexOf("."));
					g.accept(new DumpCGraphVisitor(), name + ".dot");
					
					if(!Config.SKIP_METHODANALYSIS){
						if(Config.MethodAnalysisInterMethodVisitor){
							if(Config.CallGraph){
								logger.info("��4 �ļ��ں������á���ϵͼ��������ļ�" + name + ".dot");
							}
						}
					}
					
					System.out.println("�ļ��ں������ù�ϵͼ��������ļ�" + name + ".dot");
					try {
						java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + name + ".jpg " + name + ".dot").waitFor();
					} catch (IOException e1) {
						System.out.println(e1);
						logger.error("�밲װ����Graphvix�������г���");
						logger.error(e1);
					} catch (InterruptedException e2) {
						System.out.println(e2);
						logger.error("�밲װ����Graphvix�������г���");
						logger.error(e2);
					}
					
					if(!Config.SKIP_METHODANALYSIS){
						if(Config.MethodAnalysisInterMethodVisitor){
							if(Config.CallGraph){
								logger.info("��4 �ļ��ں������á���ϵͼ��ӡ�����ļ�" + name + ".jpg");
							}
						}
					}
					
					System.out.println("�ļ��ں������ù�ϵͼ��ӡ�����ļ�" + name + ".jpg");
				}
			}
		}
		
		private String getIFilePath(String fileName){
			File file=null;
			BufferedReader read=null;
			String filetemp = "",temp1 = "",temp2 = "";
			try 
			{
				file=new File(fileName);			
				if(file.exists()){
					read = new BufferedReader(new FileReader(file));
					String line = null;
					line = read.readLine();
					if(line!=null && line.startsWith("#")){
						String[] infors = line.split(" ");
						if (infors.length > 3) {
							// ·�����д��пո�
							for (int i = 2; i < infors.length; i++) {
								temp1 +=  " " + infors[i];
							}
						} else {
							temp1 = infors[infors.length - 1];
						}						
					}
					line = read.readLine();
					//TODO �������ʲôԭ��ʲô�����
					if(!line.contains("<built-in>")){
						String[] infors = line.split(" ");
						if (infors.length > 3) {
							// ·�����д��пո�
							for (int i = 2; i < infors.length; i++) {
								temp2 +=  " " + infors[i];
							}
						} 
					}
					filetemp = temp2+temp1;
					filetemp = filetemp.replace("\\\\", "\\").trim();
					filetemp = filetemp.replace("//", "/").trim();
					filetemp = filetemp.replace("\"", "").trim();
					filetemp = filetemp.replace("./", "").trim();
					}
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
				try {
					if(read!=null)
						read.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return filetemp;
		}


	}

	/**
	 * ͨ��DTSC��ں����ķ������õ�Pretreatment��ʵ��������ƽ̨��KEIL GCC��
	 * @param pre
	 */
	public void setPretreatmentInstance(Pretreatment pre) {
		this.pre=pre;
	}

	public static AnalysisElement getElement(String fileName) {
		AnalysisElement temp=new AnalysisElement(fileName);
		if(CAnalysis.elements!=null){
			for(AnalysisElement a : CAnalysis.elements){
				if(a.equals(temp)){
					temp=a;
					break;
				}
			}
		}
		return temp;
	}
	
	
	//qian-2011-02-25
	public static AnalysisElement getCurAnalyElmt() {
		return curElement;
	}
	
	
	
	/**  ��Ŀǰϵͳ����ʶ����ַ��������滻
	 *
	 * @param interFile
	 * @return
	 */
	public static  String fileReplace(String interFile){
		String result=interFile;
		
		/*��Ҫ���滻���ַ���*/
		Object[] definelist = UnknownString.getdefine();
		
		/*�滻��*/
		Object[] replaceby = UnknownString.getreplaceBy();
		
		File inter = new File(interFile);
		result=result.replace(".i", ".temp");
		File resultFile = new File(result);
		BufferedReader read = null;
		BufferedWriter write = null;
		try {
			read = new BufferedReader(new FileReader(inter));
			write = new BufferedWriter(new FileWriter(resultFile));
			String line = null;
			while((line = read.readLine()) != null) {
				line=" "+line+" ";
				for(int i = 0; i < definelist.length; i++){
					String defStr = definelist[i].toString() , replaceStr = replaceby[i].toString();
					
					/*���滻�������Ǻ�����ʽ�������Ȼ�ȡ������*/
					String funcName = "";
					if(defStr.contains("("))
						funcName = defStr.substring(0, defStr.indexOf("(")+1).trim();
					
					/*�ļ��г��ֵĺͱ��滻�ַ�����ȫһ���ģ�ֱ���滻����Ϊ�������͡�������������ʽ����
					 * �����ַ�������Ҫ��ת�����\�� */
					if(line.contains(defStr) || line.trim().endsWith(defStr.trim()+";")||line.trim().endsWith(defStr.trim()+"\"")){
						defStr = defStr.replaceAll("\\(", "\\\\\\(");
						defStr = defStr.replaceAll("\\)", "\\\\\\)");
						replaceStr = replaceStr.replaceAll("\\(", "\\\\\\(");
						replaceStr = replaceStr.replaceAll("\\)", "\\\\\\)");
						
						if(line.contains(defStr))
							line = line.replaceAll(defStr, replaceStr);
						else if(line.trim().endsWith(defStr.trim()+";")){
							line = line.replace(" "+(defStr.trim())+";", replaceStr+";");
						}
						else if(line.trim().endsWith(defStr.trim()+"\"")){
							line = line.replace((defStr.trim())+"\"", replaceStr.trim()+"\"");
						}
					}
					else if(line.trim().matches(defStr+".*")){
						line = line.replaceAll(defStr, replaceStr);
						line = line.replaceAll("\\s*;\\s*", ";");
						String temp=line.substring( line.lastIndexOf(' '),line.lastIndexOf(";"));
						line = line.replaceAll(temp, "");
						line = line.replaceAll("_at_", temp+" _at_ ");
					}
					/*���ں�����ʽ�ı��滻�����ļ��г��ֵĴ���ԭʼ���滻���������ܲ�ͬ������Ҫ�����滻��defStr
					 * ���滻��replacebyStr�еĲ������ļ��е�ͳһ*/
					else if(defStr.contains("(") && line.contains(funcName)){
						line = line.replace(";", " ;");
						int firIndex = defStr.indexOf("("), secIndex = defStr.indexOf(")",firIndex);
						
						/*1����ȡԭʼ���滻���ĺ�������*/
						String[] parOld = defStr.substring(firIndex+1,secIndex).split(",");
						
						while (line.contains(funcName)){
							defStr = definelist[i].toString() ;
							replaceStr = replaceby[i].toString();
							firIndex = line.indexOf(funcName)+funcName.length();
							/*��Ϊ�����д����ţ�Ҫ�������ųɶ�ƥ����ȷ������*/
							int temp = firIndex, flag = 0;
							for(; temp<line.length(); temp++){
								if(flag==0 && line.charAt(temp) == ')'){
									secIndex = temp;
									break;
								}
								else if(flag != 0 && line.charAt(temp) == ')'){
									flag --;
								}
								else if(line.charAt(temp) == '('){
									flag ++;
								}
							}
								
							
							/*2����ȡ�ļ��еĺ�������*/
							String[] parNew = line.substring(firIndex,secIndex).split(",");
							/*3���������������һ�����޷������滻���������ļ��еĺ�������parNew��ԭʼ���еĲ����滻*/
							if(parNew.length != parOld.length)
								break;
							else{
								for(int j = 0; j < parNew.length; j++){
									/*��������ַ���ǰ��û�пո���ӡ�\b���ֽ��ƥ�䣬�Ա�֤�滻������������*/
									if(parOld[j] .startsWith(" ") && parOld[j].endsWith(" ")){
										defStr = defStr.replaceAll(parOld[j], parNew[j]);
									}
									else if(!parOld[j] .startsWith(" ") && parOld[j].endsWith(" ")){
										defStr = defStr.replaceAll("\\b"+parOld[j], parNew[j]);
									}
									else if(parOld[j] .startsWith(" ") && !parOld[j].endsWith(" ")){
										defStr = defStr.replaceAll(parOld[j]+"\\b", parNew[j]);
									}
									else if(!parOld[j] .startsWith(" ") && !parOld[j].endsWith(" ")){
										defStr = defStr.replaceAll("\\b"+parOld[j]+"\\b", parNew[j]);
									}
									String tempParOld = parOld[j].trim();
									if(!tempParOld.startsWith("\\b"))
										tempParOld = "\\b"+tempParOld;
									if(!tempParOld.endsWith("\\b"))
										tempParOld = tempParOld+"\\b";
									replaceStr = replaceStr.replaceAll(tempParOld, parNew[j]);
								}
								defStr = defStr.replaceAll("\\(", "\\\\\\(");
								defStr = defStr.replaceAll("\\)", "\\\\\\)");
								replaceStr = replaceStr.replaceAll("\\(", "\\\\\\(");
								replaceStr = replaceStr.replaceAll("\\)", "\\\\\\)");
								/*��*��Ҳ��������ʽ�������ַ�����ת���*/
								defStr = defStr.replaceAll("\\*","\\\\\\*");
								replaceStr = replaceStr.replaceAll("\\*", "\\\\\\*");
								defStr = defStr.trim();
								/*4���滻*/
								line = line.replaceAll(defStr, replaceStr);
							}
						}
					}	
				}
				if(!line.trim().equals(""))
				line = line.trim();
				write.write(line);
				write.write("\n");
			}
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
	/**���м䴦���ļ���ȥ������Ŀ��С������У������ȡ�����ϵͳͷ�ļ�
	 * 
	 * @param file
	 * @return
	 */
	
	public static String findLib(String file){
		String outFile = file.replace(".i", ".temp");
		File inter = new File(file);
		File outer = new File(outFile);
		BufferedReader read = null;
		BufferedWriter write = null;
		try {
			read = new BufferedReader(new FileReader(inter));
			write = new BufferedWriter(new FileWriter(outer));
			String line = null;
			String nextline = null;
			String[] fileItem, nextLineItem;
			String fileName = "", incName = null;
			StringBuffer buffer = new StringBuffer();
			boolean delete = false;
			/*��ȡ�ļ�������������·�������滻Ϊ.c��׺*/
			fileItem = file.replaceAll("\"", "").split("\\\\");
			fileName = fileItem[fileItem.length-1].replace(".i", ".c");
			if(fileName.endsWith(".gcc"))
				fileName = fileName.replace(".gcc", "");
			
			while((line = read.readLine()) != null) {
				buffer.append(line);
				/* ����#~~filename~~~����˵����.i�ļ���������ʼ�µ�ͷ�ļ�չ��*/
				if(line.startsWith("#") && line.contains(fileName)){
					delete = false;
					while((nextline = read.readLine())!= null){
						if(nextline.equals("")){
							buffer.append("\n"+"");
							continue;
						}
						/*�ٴ�����#����ʾ������һ��ͷ�ļ�չ���Ŀ�ʼ����#��һ�а���ͷ�ļ��ļ���*/
						if(nextline.startsWith("#"))
						{
							nextLineItem = nextline.split(" ");
							if(nextLineItem.length >= 3){
								incName = nextLineItem[2].replaceAll("\"", "");
								if(incName.endsWith(".h")){
									incName = incName.replace("\\\\", "/");
									incName = incName.replace("//", "/");
									/*�����ͷ�ļ���δ����incset����ͷ�ļ�չ������copy��.temp�ļ�
									 * ����˵�����ͷ�ļ��ѱ���������Ҫ����ͷ�ļ�չ������ɾ������delete������Ϊtrue*/
									if(incSet.add(incName)){
										buffer.append("\n"+nextline);
									}
									else{
										buffer.delete(0, buffer.length());
										delete = true;
									}
									break;
								}
								else{
									if(!incName.equals(fileName)){
										buffer.append("\n"+nextline);
										break;
									}
									else{
										write.write(buffer.toString());
										write.write("\n");
										buffer.delete(0, buffer.length());
										buffer.append(nextline);
									}
								}
							}
							else{
								buffer.append("\n"+nextline);
								break;
							}
						}
						else{
							buffer.append("\n"+nextline);
							break;
						}
					}
					if(!delete)
					write.write(buffer.toString()+"\n");
				}
				else{
					if(!delete)
						write.write(buffer.toString()+"\n");
					buffer.delete(0, buffer.length());
					continue;
				}
				buffer.delete(0, buffer.length());
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		return outFile;
	}
	/** ���м䴦���ļ���ȥ������Ŀ��С������У������ȡ�����ϵͳͷ�ļ�,ͬʱ���в���ʶ���ַ������滻
	 * 
	 * @param file
	 * @return
	 */
	public static String findLibAndReplace(String file){
		Object[] definelist = UnknownString.getdefine();
		Object[] replaceby = UnknownString.getreplaceBy();
		String outFile = file.replace(".i", ".temp");
		File inter = new File(file);
		File outer = new File(outFile);
		BufferedReader read = null;
		BufferedWriter write = null;
		try {
			read = new BufferedReader(new FileReader(inter));
			write = new BufferedWriter(new FileWriter(outer));
			String line = null;
			String nextline = null;
			String[] fileItem, nextLineItem;
			String fileName = "", incName = null;
			StringBuffer buffer = new StringBuffer();
			boolean delete = false;
			fileItem = file.replaceAll("\"", "").split("\\\\");
			fileName = fileItem[fileItem.length-1].replace(".i", ".c");
			if(fileName.endsWith(".gcc"))
				fileName = fileName.replace(".gcc", "");
			while((line = read.readLine()) != null) {
				buffer.append(line);
				if(line.startsWith("#") && line.contains(fileName)){
					delete = false;
					while((nextline = read.readLine())!= null){
						if(nextline.equals("")){
							buffer.append("\n"+"");
							continue;
						}
						if(nextline.startsWith("#"))
						{
							nextLineItem = nextline.split(" ");
							if(nextLineItem.length >= 3){
								incName = nextLineItem[2].replaceAll("\"", "");
								if(incName.endsWith(".h")){
									incName = incName.replace("\\\\", "/");
									incName = incName.replace("//", "/");
									if(incSet.add(incName)){
										buffer.append("\n"+nextline);
									}
									else{
										buffer.delete(0, buffer.length());
										delete = true;
									}
									break;
								}
								else{
									if(!incName.equals(fileName)){
										buffer.append("\n"+nextline);
										break;
									}
									else{
										buffer.replace(0, buffer.length(), fileReplaceInFindLib(buffer.toString(),definelist,replaceby)) ;
										write.write(buffer.toString());
										write.write("\n");
										buffer.delete(0, buffer.length());
										buffer.append(nextline);
									}
								}
							}
							else{
								buffer.append("\n"+nextline);
								break;
							}
						}
						else{
							buffer.append("\n"+nextline);
							break;
						}
					}
					if(!delete){
						buffer.replace(0, buffer.length(), fileReplaceInFindLib(buffer.toString(),definelist,replaceby)) ;
						write.write(buffer.toString()+"\n");
					}
				}
				else{
					if(!delete){
						buffer.replace(0, buffer.length(), fileReplaceInFindLib(buffer.toString(),definelist,replaceby)) ;
						write.write(buffer.toString()+"\n");
					}
					buffer.delete(0, buffer.length());
					continue;
				}
				buffer.delete(0, buffer.length());
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		return outFile;
	}
	
	private static String fileReplaceInFindLib(String buffer,Object[] definelist,Object[] replaceby){
		String[] line = buffer.split("\n");
		String result = "";
		for(int i=0; i<line.length; i++){
			line[i] = " "+line[i]+" ";
			for(int j = 0; j < definelist.length; j++){
				String defStr = definelist[j].toString() , replaceStr = replaceby[j].toString();
				String funcName = "";
				if(defStr.contains("("))
					funcName = defStr.substring(0, defStr.indexOf("(")+1);
				if(line[i].contains(defStr)||line[i].trim().endsWith(defStr.trim()+";")){
					defStr = defStr.replaceAll("\\(", "\\\\\\(");
					defStr = defStr.replaceAll("\\)", "\\\\\\)");
					replaceStr = replaceStr.replaceAll("\\(", "\\\\\\(");
					replaceStr = replaceStr.replaceAll("\\)", "\\\\\\)");
					if(line[i].contains(defStr))
						line[i] = line[i].replaceAll(defStr, replaceStr);
					else if(line[i].trim().endsWith(defStr.trim()+";")){
						line[i] = line[i].replace(" "+(defStr.trim())+";", replaceStr+";");
					}
				}
				else if(defStr.contains("(") && line[i].contains(funcName)){
					line[i] = line[i].replace(";", " ;");
					int firIndex = defStr.indexOf("("), secIndex = defStr.indexOf(")",firIndex);
					String[] parOld = defStr.substring(firIndex+1,secIndex).split(",");
					while (line[i].contains(funcName)){
						defStr = definelist[j].toString() ;
						replaceStr = replaceby[j].toString();
						firIndex = line[i].indexOf(funcName)+funcName.length();
						int temp = firIndex, flag = 0;
						for(; temp<line[i].length(); temp++){
							if(flag==0 && line[i].charAt(temp) == ')'){
								secIndex = temp;
								break;
							}
							else if(flag != 0 && line[i].charAt(temp) == ')'){
								flag --;
							}
							else if(line[i].charAt(temp) == '('){
								flag ++;
							}
						}
							
							
						String[] parNew = line[i].substring(firIndex,secIndex).split(",");
						if(parNew.length != parOld.length)
							break;
						else{
							for(int k = 0; k < parNew.length; k++){
								if(parOld[k] .startsWith(" ") && parOld[k].endsWith(" ")){
									defStr = defStr.replaceAll(parOld[k], parNew[k]);
								}
								else if(!parOld[k] .startsWith(" ") && parOld[k].endsWith(" ")){
									defStr = defStr.replaceAll("\\b"+parOld[k], parNew[k]);
								}
								else if(parOld[k] .startsWith(" ") && !parOld[k].endsWith(" ")){
									defStr = defStr.replaceAll(parOld[k]+"\\b", parNew[k]);
								}
								else if(!parOld[k] .startsWith(" ") && !parOld[k].endsWith(" ")){
									defStr = defStr.replaceAll("\\b"+parOld[k]+"\\b", parNew[k]);
								}
								String tempParOld = parOld[k].trim();
								if(!tempParOld.startsWith("\\b"))
									tempParOld = "\\b"+tempParOld;
								if(!tempParOld.endsWith("\\b"))
									tempParOld = tempParOld+"\\b";
								replaceStr = replaceStr.replaceAll(tempParOld, parNew[k]);
							}
							defStr = defStr.replaceAll("\\(", "\\\\\\(");
							defStr = defStr.replaceAll("\\)", "\\\\\\)");
							replaceStr = replaceStr.replaceAll("\\(", "\\\\\\(");
							replaceStr = replaceStr.replaceAll("\\)", "\\\\\\)");
							defStr = defStr.replaceAll("\\*","\\\\\\*");
							replaceStr = replaceStr.replaceAll("\\*", "\\\\\\*");
							defStr = defStr.trim();
							line[i] = line[i].replaceAll(defStr, replaceStr);
						}
					}
				}	
			}
			line[i] = line[i].trim();
			result += (line[i]+"\n");
		}
		return result;
	}
}
