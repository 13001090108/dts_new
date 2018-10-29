package softtest.summary.lib.c;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import softtest.ast.c.ParseException;
import softtest.interpro.c.Method;
import softtest.summary.c.MethodSummary;
import softtest.summary.lib.c.LibMethodSummary;
import softtest.symboltable.c.MethodNameDeclaration;

/**
 * 
 * �⺯������ժҪ�������������ָ��Ŀ¼�м��ؿ⺯������ժҪ�����������ɺ�����������������ժҪ
 * @author qipeng
 *
 */
public class LibManager {

	/**
	 * <p>��ǰ�������ļ��м��ص����к����⣬�����ж������������Ϣ�ĺ����б�</p>
	 */
	private Map<String, ArrayList<LibMethodSummary>> libSummarys;
	private static LibManager instance;
	
	private LibManager() {
		libSummarys = new HashMap<String, ArrayList<LibMethodSummary>>();
	}
	
	public static LibManager getInstance() {
		if (instance == null) {
			instance = new LibManager();
		}
		return instance;
	}
	
	public void clear() {
		if (libSummarys != null) {
			libSummarys.clear();
		}
	}
	
	/**
	 * ����ָ��Ŀ¼�����е������ļ������ĺ���ժҪ��Ϣ
	 * 
	 * @param path ��������ժҪ��Ϣ���ļ�Ŀ¼
	 */
	public void loadLib(String path) {
		File dir = new File(path);
		if(!dir.exists())
			return;
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().endsWith(".xml")) {
				LibLoader.loadLibSummarys(files[i].getAbsolutePath());	
			}
		}
	}
	
	/** zys:2010.6.22 ���ص���XML�ļ��Ŀ⺯��ժҪ*/
	public void loadSingleLibFile(String path)
	{
		String temp[]=path.split("/");
		String fileName=temp[temp.length-1];
		File dir=new File(temp[0]);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().endsWith(fileName)) {
				LibLoader.loadLibSummarys(files[i].getAbsolutePath());	
			}
		}
	}
	
	/**
	 * ��ӿ⺯��ժҪ��Ϣ����ǰ��������
	 * 
	 * @param libName �⺯�����ڿ�����
	 * @param libSummary ����ժҪ��Ϣ
	 */
	public void addLib(String libName, LibMethodSummary libSummary) {
		if (libName.contains(";")) {
			String[] libNames = libName.split(";");
			for (int i = 0; i < libNames.length; i++) {
				addLib(libNames[i], libSummary);
			}
		} else {
			ArrayList<LibMethodSummary> summarys = libSummarys.get(libName);
			if (summarys == null) {
				summarys = new ArrayList<LibMethodSummary>();
				summarys.add(libSummary);
				libSummarys.put(libName, summarys);
			} else {
				summarys.add(libSummary);
			}
		}
	}
	
	/**
	 * ���ݵ�ǰ�������õĿ⣬�����������п�ĺ���ժҪ��Ϣ
	 * 
	 * @param includeLibs ��ǰ�������õĿ��б�
	 * @return ���ɵĿ⺯���ĺ�������
	 * @throws ParseException 
	 */
	//20141125
	public Set<MethodNameDeclaration> compileLib(Set<String> includeLibs) {
		Map<Method, MethodNameDeclaration> libDecl = new HashMap<Method, MethodNameDeclaration>();
		for (String lib : libSummarys.keySet()) {
//			for (String include : includeLibs) {
//				int i=include.lastIndexOf('/');
//				String header=include.substring(i+1, include.length());
//				if (header.equalsIgnoreCase(lib)) { //change "include" to lower-case format by Xinhong Yao. 11/14/2009
					ArrayList<LibMethodSummary> summarys = libSummarys.get(lib);
					for (LibMethodSummary summary : summarys) {
						MethodNameDeclaration decl = summary.compileSummary();						
						if (decl != null) {
							if (libDecl.containsKey(decl.getMethod())) {
								MethodNameDeclaration oldDecl = libDecl.get(decl.getMethod());
								MethodSummary mtSummary = oldDecl.getMethodSummary();
								mtSummary.addSummary(decl.getMethodSummary());
							} else {
								libDecl.put(decl.getMethod(), decl);
							}
						}
//					}
//				}
			}
		}
		return new HashSet<MethodNameDeclaration>(libDecl.values());
	}
	//modified by liuyan 2014.9.5
//	public Set<MethodNameDeclaration> compileLib(Set<String> includeLibs) throws ParseException {
//		Map<Method, MethodNameDeclaration> libDecl = new HashMap<Method, MethodNameDeclaration>();
//		for (String lib : libSummarys.keySet()) {
//				//����������м��ļ�����,����������õ�ͷ�ļ�,����ȫ�����ش���ժҪ��ͷ�ļ�
//				for (String include : includeLibs) {
//					if (include.toLowerCase().endsWith(lib)) { //change "include" to lower-case format by Xinhong Yao. 11/14/2009
//						ArrayList<LibMethodSummary> summarys = libSummarys.get(lib);
//						for (LibMethodSummary summary : summarys) {
//							MethodNameDeclaration decl = summary.compileSummary();
//							if (decl != null) {
//								if (libDecl.containsKey(decl.getMethod())) {
//									MethodNameDeclaration oldDecl = libDecl.get(decl.getMethod());
//									MethodSummary mtSummary = oldDecl.getMethodSummary();
//									mtSummary.addSummary(decl.getMethodSummary());
//								} else {
//									libDecl.put(decl.getMethod(), decl);
//								}
//							}
//						}
//					}
//				}
//			}
//		return new HashSet<MethodNameDeclaration>(libDecl.values());
//	}
}
