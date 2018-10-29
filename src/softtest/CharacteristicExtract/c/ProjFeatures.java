package softtest.CharacteristicExtract.c;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Graph;
import softtest.config.c.Config;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.interpro.c.MethodNode;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;


/** 
 * @author 
 * Miss_lizi
 * ��ȡ�ļ������Լ�ȫ�ֺ����ĵ��ù�ϵ
 */
public class ProjFeatures {
	
	//������      ����˳��       ���к�������        ǰ��       ���       ����
	
	
	public static void main(String[] args) throws Exception{
		ProjFeatures p = new ProjFeatures();
		String analysisDir1="C:/Users/Miss_Lizi/Desktop/gsl-2.3";
		String analysisDir2="C:/Users/Miss_Lizi/Desktop/antiword-0.37/Docs";
		p.getAll(analysisDir1);
		//p.getAll(analysisDir2);
	}
	

	//����Ԥ����ĳ�ʼ��
	private List<String> init(String projpath,Pretreatment pre){
		List<String> file = new ArrayList<String>();
		pre.setPlatform(PlatformType.GCC);
		
		File srcFileDir=new File(projpath);
		collect(srcFileDir,file);
		System.out.println(file.size());
//		for(int i = 0; i < file.size(); i++){
//			System.out.println(file.get(i));
//			System.out.println("*****");
//		}
		return file;
	}
	
	private String AnayliseFileFeature(List<String> file,Pretreatment pre,List<AnalysisElement> elements) {
		StringBuffer res = new StringBuffer();
		for(String srcFile  :  file){
			AnalysisElement element=new AnalysisElement(srcFile);
			elements.add(element);
			//Ԥ����֮���.IԴ�ļ�
			String afterPreprocessFile=null;
			List<String> include = new ArrayList<String>();
			include.add("C:/Program Files (x86)/DTS/DTS/DTSGCC/include");
			List<String> macro = new ArrayList<String>();
			afterPreprocessFile=pre.pretreat(srcFile, include, macro);
			
			try {
				//�õ��ļ��ں�����������Ϣ
				Graph_Info gi = new Graph_Info();
				List<CVexNode> list_cvex = new ArrayList<CVexNode>();
				list_cvex = gi.getCVexNode(srcFile);
				if(res.length() == 0){
					if(getFeatures(srcFile,list_cvex).length() != 0){
						res.append(getFeatures(srcFile,list_cvex));
					}
				}else{
					if(getFeatures(srcFile,list_cvex).length() != 0){
						res.append("#" + getFeatures(srcFile,list_cvex));
					}
				}
				
				
				//���������﷨��
				//System.out.println("���ɳ����﷨��...");
				System.out.println(afterPreprocessFile);				
				CParser parser=CParser.getParser(new CCharStream(new FileInputStream(afterPreprocessFile)));
				ASTTranslationUnit root=parser.TranslationUnit();
				
					
				//�������ű�
				//System.out.println("���ɷ��ű�...");
				ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
				root.jjtAccept(sc, null);
				OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
				root.jjtAccept(o, null);
					
				//����ȫ�ֺ������ù�ϵ
				//System.out.println("����ȫ�ֺ������ù�ϵ...");
				root.jjtAccept(new InterMethodVisitor(), element);
					
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res.toString();
	}
	
	/** �õ����������������Ϣ*/
	public List<String> getAll(String projpath){
		Pretreatment pre = new Pretreatment();
		List<AnalysisElement> elements= new ArrayList<AnalysisElement>();
		List<String> res = new ArrayList<String>();
		
		
		List<String> file = new ArrayList<String>();
		file = init(projpath,pre);
		
		//��һ����������.CԴ�ļ�����Ԥ���룬�õ����к�����������ϵ����String����
		String features = "";
		features = AnayliseFileFeature(file,pre,elements);
		
		//System.out.println(features.length());
			
		//�ڶ���������ȫ�ֺ������ù�ϵͼ,�õ��ļ�����˳����String��ʽ����
		Config.TRACE=true;
		List<AnalysisElement> orders = new ArrayList<AnalysisElement>();
//		InterCallGraph iv = new InterCallGraph();
		InterCallGraph iv = InterCallGraph.getInstance();
//		InterCallGraph interCGraph = iv.getInstance_not();
		InterCallGraph interCGraph = iv.getInstance();
		orders = interCGraph.getAnalysisTopoOrder();
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println(orders.size());
		
		// ����е��ļ�û���ں�������ͼ�ϳ��֣���ֱ�ӽ�������ӵ������б����
		if (orders.size() != elements.size()) {
			for (AnalysisElement element : elements) {
				boolean exist = false;
				for (AnalysisElement order : orders) {
					if (order == element) {
						exist = true;
					}
				}
				if (!exist) {
					orders.add(element);
				}
			}
		}
		StringBuffer fileorder = new StringBuffer();
		for(int i = 0; i < orders.size(); i++){
			if(fileorder.length() == 0){
				fileorder.append(orders.get(i));
			}else{
				fileorder.append("#" + orders.get(i));
			}
		}
		
		//���������ռ�ǰ����̹�ϵ
		List<MethodNode> topo = new ArrayList<MethodNode>();
		topo = interCGraph.getMethodTopoOrder();
		List<MethodNode> allfunc = new ArrayList<MethodNode>();
		//����stdio_printf���ֵĵ��ù�ϵ
		for(MethodNode mtnode : topo){
			String func = format(mtnode.getMethod().toString());
			if(!func.startsWith("stdio")){
				allfunc.add(mtnode);
			}
		}
		StringBuffer after = new StringBuffer();
		StringBuffer before = new StringBuffer();
		HashSet<MethodNode> set = new HashSet<MethodNode>();
		for(int i = 1; i < topo.size(); i++) {
			MethodNode curNode = topo.get(i);
			for(MethodNode callNode : curNode.getOrderCalls()) {
				if(callNode.getToponum() < curNode.getToponum()) {
					set.add(callNode);
					set.add(curNode);
					//   caller -> callee
					String caller = format(curNode.getMethod().toString());
					String callee = format(callNode.getMethod().toString());
					//��̹�ϵ
					if(after.length() == 0){
						after.append(caller + "," + callee);
					}else{
						after.append("#" + caller + "," + callee);
					}
					//ǰ����ϵ
					if(before.length() == 0){
						before.append(callee + "," + caller);
					}else{
						before.append("#" + callee + "," + caller);
					}
				}
			}
		}
		
		//���Ĳ����õ������ĺ����ڵ�
		StringBuffer independent = new StringBuffer();
		if(set.size() == 0){
			String[] str = features.split("#");
			for(int i = 0; i < str.length; i++){
				String[] str1 = str[i].split(",");
				if(independent.length() == 0){
					independent.append(str1[0]);
				}else{
					independent.append("#" + str1[0]);
				}
			}
		}else{
			for(MethodNode mtnode : allfunc){
				if(!set.contains(mtnode)){
					if(independent.length() == 0){
						independent.append(format(mtnode.getMethod().toString()));
					}else{
						independent.append("#" + format(mtnode.getMethod().toString()));
					}
				}
			}
		}
		
		
		res.add(fileorder.toString());
		res.add(features);
		res.add(after.toString());
		res.add(before.toString());
		res.add(independent.toString());
		res.add(projpath);
//		for(int i = 0; i < res.size(); i++){
//			System.out.println(res.get(i));
//		}
		
		return res;
	}
	
	
	private static String format(String nodeName) {
		String str = "";
		StringBuffer res = new StringBuffer();
		if(nodeName.startsWith("::"))
			str =  nodeName.substring(2);
		else
			str =  nodeName.contains("~")?nodeName.replaceAll("::", "__").replace('~', '_'):nodeName.replaceAll("::", "__");
		String[] array = str.split("_");
		for(int i = 0; i < array.length - 2; i++){
			res.append(array[i] + "_");
		}
		res.append(array[array.length - 2]);
		return res.toString();
	}
	
	
	/** �õ������ṹ�͹���������*/
	public String getFeatures(String filePath, List<CVexNode> list_cvex) throws Exception{
		FunctionFeatures f = new FunctionFeatures();
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		StringBuffer str1 = new StringBuffer();
		Func_Features ff = new Func_Features();
		
		String[] str = filePath.split("\\\\");
 		String file = str[str.length - 1];
 		file = file.substring(0, file.length() - 2);
 		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				//System.out.println(c.name);
				if(str1.length() == 0){
					str1.append(file + "_" + node.getImage());
				}else{
					str1.append("#" + file + "_" + node.getImage());
				}
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				str1.append("," + f.getFunctionStructFeature(g));
				//�Ѻ���Ĺ������������ӵ�����
				str1.append("," + ff.getFunctionFeatures(filePath, function.getImage()));
			}
			node = null;
		}
		f = null;
		ff = null;
		//System.out.println(str1.toString());
		return str1.toString();
	}
	
	
	//�ռ�����·���µ�����.CԴ�ļ�
	private void collect(File srcFileDir,List<String> file) {
		if (srcFileDir.isFile() && srcFileDir.getName().matches(InterContext.SRCFILE_POSTFIX)) {
			file.add(srcFileDir.getPath());
		}else if (srcFileDir.isDirectory()) {
			File[] fs = srcFileDir.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collect(fs[i],file);
			}
		}
	}
}
