package softtest.CharacteristicExtract.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import softtest.SimDetection.c.*;
import softtest.ast.c.*;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Graph;
import softtest.fsm.c.FSMMachineInstance;
import softtest.rules.c.StateMachineUtils;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;



/** 
 * @author 
 * Miss_lizi
 * ��ȡ�����һЩ����Ԫ
 */
public class Metric{
	
	//���ı��ĵ��еõ����б������Լ��ؼ���
	 private List<String>  list_reserved = getReservedWords(); 
	
	public Metric(){
		
	}
	
	
	/** ����graph����Ľڵ����õ�������Ϣ���õ��������ļ���������Ϣ*/
	public String getMetric(String filePath) throws Exception{
		//���ɳ����﷨��
		CParser parser=CParser.getParser(new CCharStream(new FileInputStream(filePath)));
		ASTTranslationUnit node = parser.TranslationUnit();
		if(node == null){
			return null;
		}else{
			//���ɶ�Ӧ�Ŀ�����ͼ��
			ScopeAndDeclarationFinder sc=new ScopeAndDeclarationFinder();
			node.jjtAccept(sc, null);
			OccurrenceAndExpressionTypeFinder o=new OccurrenceAndExpressionTypeFinder();
			node.jjtAccept(o, null);
//			CGraph g = new CGraph(filePath);
			CGraph g = new CGraph();
			((AbstractScope)node.getScope()).resolveCallRelation(g);
			
			//String result = new ArrayList<String>();
			Graph_Info gi = new Graph_Info();
			String cgraphinfo = gi.getFileInfo(g);
			
			return cgraphinfo;
		}
		
	}
	
	/** �õ�ÿ��������������Ϣ*/
	public List<String> getFuncMetric(List<CVexNode> node_list, String filePath){
		List<String> result = new ArrayList<String>();
		if(node_list.size() == 0){
			return result;
		}
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		for(CVexNode c : node_list){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition) {
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				List<Integer> list = new ArrayList<Integer>();
				
				Graph_Info gi = new Graph_Info();
				String graphinfo = gi.getFuncInfo(g, function,filePath);
				//System.out.println(graphinfo);
				
				HashSet<String> opr_set = new HashSet<String>();    //��ų����в�����������
				HashSet<String> opd_set = new HashSet<String>();    //��ų����в�����������
				HashMap<String, Integer> opr_map = new HashMap<String, Integer>();      //�����ҵ�Ψһ�������ĸ���
				HashMap<String, Integer> opd_map = new HashMap<String, Integer>();      //�����ҵ�Ψһ�������ĸ���
				int oprnum = 0;    //���������еĲ������ĸ���
				int opdnum = 0;    //���������еĲ������ĸ���  
				
				//�õ�Ψһ�Ĳ������Ͳ�����
				list = null;
				oprnum = getOperator(node,opr_set,oprnum,opr_map);     //�õ����������еķ���		
				oprnum = getOperatorNum(node,opr_set,oprnum,opr_map);   //��֮ǰ�Ļ����ϼ���int��Щ���߰����
				list = getopr_opd(node, list_reserved, opr_set, oprnum, opr_map, opd_set, opdnum, opd_map);
				//list�д洢��5�����֣�����˳��ֱ�Ϊ��opdnum��opd��oprnum��opr�ʹʻ�����
				//System.out.println(node);
				/** �õ���������Ч���� len*/
				long len = getFuncLen(node);
				/** �õ�������McCabe������V(G)��m-n��2*/
				long McCabe = g.getedgecount() - g.getVexNum() + 2;
				/** ��NΪ����Ĵʻ�������N=N1+N2*/
				long vocabulary_count = (long)(list.get(4));
				/** ����ʻ��Ϊ��ͬ������������Ͳ�ͬ����������������ܺ�*/
				long words = (opr_set.size() + opd_set.size());
				/** V=(N1+N2)log2(n1+n2)*/
				double capacity = vocabulary_count*(Math.log((double)words)/Math.log((double)2));
				/** ���ƽ��������Ϣ��: AVGS=(N1+N2)/(lc_stat)*/
				float AVGS = ((float)vocabulary_count/len);
				/** �ʻ�Ƶ�ʣ�VOCF=(N1+N2)/(n1+n2)*/
				float VOCF = ((float)vocabulary_count/(float)words);
				/** ����ע�ͱȣ�COMF=lc_bcom/(lc_stat)*/
				float COMF = (((float)getFuncIniLen(node)-(float)len)/(float)len);
				
				//��������    ������Ч����   McCabe����   ����ʻ��   ����ʻ���   ���������   ���ƽ��������Ϣ��   �ʻ�Ƶ��   ����ע�ͱ�
				//String res = "#" + filePath + "�е�" + node.getImage() + "������������Ϊ��";
				String res = "#" +len;
				//res += "#" +len;
				res += "#" +McCabe;
				res += "#" +words;
				res += "#" +vocabulary_count;
				res += "#" +capacity;
				res += "#" +AVGS;
				res += "#" +VOCF;
				res += "#" +COMF;
				
				graphinfo += res;
				result.add(graphinfo);
			} 			
		}
		return result;
	}
	
	
	/** �õ�һ����������Ч������ժ��ע���Լ����У����һ�е�����û��������*/
	public int getFuncLen(SimpleNode node){
		int len = 0;
		ASTFunctionDefinition fnode = (ASTFunctionDefinition)node;
		ASTStatementList thenode = null;
		if(fnode.jjtGetNumChildren() == 2){
			len++;    //��Ϊ������ռ��һ�У��൱��ASTDeclarator��һ��
			thenode = (ASTStatementList)fnode.jjtGetChild(1).jjtGetChild(0);
			for(int j = 0; j < thenode.jjtGetNumChildren(); j++){
				ASTStatement ssnode = (ASTStatement)thenode.jjtGetChild(j);
				int line_len = ssnode.getEndFileLine() - ssnode.getBeginFileLine() + 1;
				len += line_len;
			}
		}else if(fnode.jjtGetNumChildren() == 3){
			len++;
			thenode = (ASTStatementList)fnode.jjtGetChild(2).jjtGetChild(0);
			for(int j = 0; j < thenode.jjtGetNumChildren(); j++){
				ASTStatement ssnode = (ASTStatement)thenode.jjtGetChild(j);
				int line_len = ssnode.getEndFileLine() - ssnode.getBeginFileLine() + 1;
				len += line_len;
			}
		}
		//System.out.println( node.getImage() + "������Ч����Ϊ��" + len);
		return len;
	}

	/** �õ�һ�������ĳ�ʼ����*/
	public int getFuncIniLen(SimpleNode node){
		ASTFunctionDefinition fnode = (ASTFunctionDefinition)node;
		int initial = fnode.getEndFileLine() - fnode.getBeginFileLine() + 1;
		return initial;
	}
	
	/** �õ�һ���ļ��ĳ�ʼ����*/
	public int getlenIni(SimpleNode node){
		int initial = node.getEndFileLine() - node.getBeginFileLine() + 1;
		return initial;
	}
	/** �õ��������Ч������ժ��ע���Լ����У����һ�е�����û��������*/
	public int getLen(SimpleNode node){
		String Xpath = ".//FunctionDefinition";
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
		int len = 0;
		int num_fun = 0;
		if(evaluationResults.size() == 0){
			return 0;
		}else{
			for(SimpleNode snode : evaluationResults){
				ASTFunctionDefinition fnode = (ASTFunctionDefinition)snode;
				//getProject(fnode);
				ASTStatementList thenode = null;
				if(fnode.jjtGetNumChildren() == 2){
					len++;    //��Ϊ������ռ��һ�У��൱��ASTDeclarator��һ��
					thenode = (ASTStatementList)fnode.jjtGetChild(1).jjtGetChild(0);
				}else if(fnode.jjtGetNumChildren() == 3){
					len++;
					thenode = (ASTStatementList)fnode.jjtGetChild(2).jjtGetChild(0);
				}
				for(int j = 0; j < thenode.jjtGetNumChildren(); j++){
					ASTStatement ssnode = (ASTStatement)thenode.jjtGetChild(j);
					int line_len = ssnode.getEndFileLine() - ssnode.getBeginFileLine() + 1;
					len += line_len;				
				}
				num_fun++;
			}
			//System.out.println("�����ĺ�������Ϊ��" + num_fun);
			return len;
		}
		  
	}
	
	/**�õ�������ÿһ������������*/
	public  boolean getProject(List<SimpleNode> nodes, FSMMachineInstance fsmin){
		Iterator<SimpleNode> nodeIterator = nodes.iterator();
		int project = 0;
		while (nodeIterator.hasNext()){
			SimpleNode snode = nodeIterator.next();
			if(snode instanceof ASTAssignmentExpression){
				project = snode.getEndLine();
			}
			//project = snode.getImage();
			fsmin.setDesp("����һ��ѽ" + project);
		}
		//String project = snode.getImage();
		//System.out.println("�ú�����Ϊ" + project);
		return true;
	}
		
	/**�õ�������for��whileѭ���ĸ���*/
	public  List<Integer> getCount_forWhile(SimpleNode node){
			String Xpath = ".//IterationStatement";
			List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
			evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);	
			int count_f = 0;
			int count_w = 0;
			List<Integer> list = new ArrayList<Integer>();
			
			for(SimpleNode snode : evaluationResults){
				ASTIterationStatement inode = (ASTIterationStatement)snode;
				if(inode.getImage().equals("for")){
					count_f++;
				}
				if(inode.getImage().equals("while")){
					count_w++;
				}
			}
			//System.out.println("������forѭ���ĸ���Ϊ��" + count_f);
			//System.out.println("������whileѭ���ĸ���Ϊ��" + count_w);
			list.add(count_f);
			list.add(count_w);
			return list;
		}
	
	
	/**�õ����������ϸ����,���������������Щ
	 * ��ת���ַ��Լ������ŵ�û�п��ǣ�������ʱ�����ƣ�*/
	public  int getOperator(SimpleNode node, HashSet<String> opr_set, int oprnum,HashMap<String, Integer> opr_map){
		String Xpath = ".//InitDeclarator | .//AssignmentOperator | .//RelationalExpression | .//EqualityExpression |"
				+ ".//AdditiveExpression | .//MultiplicativeExpression | .//LogicalANDExpression | .//LogicalORExpression |"
				+ ".//ShiftExpression | .//ExclusiveORExpression | .//InclusiveORExpression | .//ANDExpression "
				+ "| .//PostfixExpression | .//UnaryExpression | .//UnaryOperator | .//ConditionalExpression | .//Pointer";
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
		List<String> list = new ArrayList<String>();
		
		for(SimpleNode snode : evaluationResults){
			if(snode instanceof ASTInitDeclarator){     //����ͬʱ��ֵ��ʱ��int i=1 ��������е� = ��ȡ
				if(snode.jjtGetNumChildren() > 1){
					oprnum++;
					opr_set.add("=");
					list.add("=");
					if(opr_map.containsKey("=")){
						opr_map.put("=", opr_map.get("=") + 1);
					}else{
						opr_map.put("=", 1);
					}
				}
			}else if(snode instanceof ASTANDExpression){    //������Ϊ&����ANDExpression�еõ����ǰ�λ�룬Unary��������ȡ��ַ��
				if(!snode.getOperators().equals("")){
					oprnum++;
					opr_set.add(snode.getOperators()+"and");
					list.add(snode.getOperators()+"and");
					if(opr_map.containsKey(snode.getOperators()+"and")){
						opr_map.put(snode.getOperators()+"and", opr_map.get(snode.getOperators()+"and") + 1);
					}else{
						opr_map.put(snode.getOperators()+"and", 1);
					}
				}
			}else if(snode instanceof ASTPostfixExpression | snode instanceof ASTUnaryExpression){
				if(!snode.getOperators().equals("")){
					oprnum++;
					opr_set.add(snode.getOperators());
					list.add(snode.getOperators());
					if(opr_map.containsKey(snode.getOperators())){
						opr_map.put(snode.getOperators(), opr_map.get(snode.getOperators()) + 1);
					}else{
						opr_map.put(snode.getOperators(), 1);
					}
				}
			}else if(snode instanceof ASTConditionalExpression){
				if(!snode.getOperators().equals("")){
					oprnum++;
					opr_set.add(snode.getOperators());
					list.add(snode.getOperators());
					if(opr_map.containsKey(snode.getOperators())){
						opr_map.put(snode.getOperators(), opr_map.get(snode.getOperators()) + 1);
					}else{
						opr_map.put(snode.getOperators(), 1);
					}
				}
			}else if(snode instanceof ASTPointer){
				oprnum++;
				opr_set.add("*");
				list.add("*");
				if(opr_map.containsKey("*")){
					opr_map.put("*", opr_map.get("*") + 1);
				}else{
					opr_map.put("*", 1);
				}
			}else{
				if(!snode.getOperators().equals("")){
					oprnum++;
					opr_set.add(snode.getOperators());
					list.add(snode.getOperators());
					if(opr_map.containsKey(snode.getOperators())){
						opr_map.put(snode.getOperators(), opr_map.get(snode.getOperators()) + 1);
					}else{
						opr_map.put(snode.getOperators(), 1);
					}
				}
			}	
		}
		//System.out.println( list);
		//System.out.println("��������ĸ���Ϊ" + oprnum);
		//System.out.println("�����������Ϊ" + opr_set.size());
		//System.out.println("Ψһ����������������Ϊ" + opr);
		return oprnum;
	}

	//��ȡ�ļ��е����ݣ���ŵ�list��
	public  List<String> getReservedWords(){
		File file = new File("reserved.txt");
        BufferedReader reader = null;
        List<String> list_reserved = new ArrayList<String>();
        String str = "";  
        try{
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            // һ�ζ���һ�У�ֱ������nullΪ�ļ�����
            while ((tempString = reader.readLine()) != null) {        
            	str += tempString;           
            }
            reader.close();
            String[] str_reserved = str.split(" ");
            for(int i = 1; i < str_reserved.length; i++){
            	list_reserved.add(str_reserved[i]);
            }
            //System.out.println(list_reserved.size());
        }catch(IOException e){
            e.printStackTrace();
        }
       // System.out.println(list_reserved);
        return list_reserved;
        
	}
		
	/**�õ����͵���Щ����Щһ������operator��
	 * ��û��дcase default������ASTLabeledStatement�ڵ��� */
	public  int getOperatorNum(SimpleNode node,  HashSet<String> opr_set, int oprnum,HashMap<String, Integer> opr_map){
		String Xpath = ".//TypeSpecifier | .//JumpStatement | .//StructOrUnion";
		//TypeSpecifier���ǻ����ȡ����һ��char �Ҳ�֪��Ϊʲô
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
		List<String> list = new ArrayList<String>();
		
		for(SimpleNode snode : evaluationResults){
			if(snode instanceof ASTTypeSpecifier){				
				if(snode.isLeaf()){
					oprnum++;
					list.add(snode.getImage());
					opr_set.add(snode.getImage());
					if(opr_map.containsKey(snode.getImage())){
						opr_map.put(snode.getImage(), opr_map.get(snode.getImage()) + 1);
					}else{
						opr_map.put(snode.getImage(), 1);
					}
				}
			}else{
				oprnum++;
				list.add(snode.getImage());
				opr_set.add(snode.getImage());
				if(opr_map.containsKey(snode.getImage())){
					opr_map.put(snode.getImage(), opr_map.get(snode.getImage()) + 1);
				}else{
					opr_map.put(snode.getImage(), 1);
				}
			}
		}
		//System.out.println("~~~~~~~~~~~~~~~~~~~~~�ָ��ߣ�û��ժ��������~~~~~~~~~~~~~~~~~~~");
		//System.out.println("����������֮���������Ϊ��" + oprnum);
		//System.out.println("����������֮���������Ϊ��" + list);
		//System.out.println("����������֮�������������Ϊ:" + opr_set.size());
		int opr = 0;
		for (String key : opr_map.keySet()) {
			if(opr_map.get(key) == 1){
				opr++;
			}
		}
		//System.out.println("����������֮��Ψһ����������������Ϊ" + opr);
		return oprnum;
	}
		
	/**��Ҫ�ж��ǲ��������ǲ������ĵĲ���  (�ڵ㡢�ؼ����б���oprset��oprlist��oprnum��opdset��opdlist��opdnum)
	�����ж�va_list�Ƿ�Ψһ��ʱ��Ҫע��һ��*/
	public  List<Integer> getopr_opd(SimpleNode node, List<String> list_reserved, HashSet<String> opr_set, int oprnum, HashMap<String, Integer> opr_map,HashSet<String> opd_set, int opdnum, HashMap<String, Integer> opd_map){
		String Xpath = ".//Declarator | .//PrimaryExpression | .//StructOrUnionSpecifier | .//Fieldld | .//Constant";
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
		List<String> list = new ArrayList<String>();
		List<Integer> res = new ArrayList<Integer>();
			
		//va_list�������Ҳ��Ī�����������opd��
		for(SimpleNode snode : evaluationResults){
			String str = snode.getImage();
			if(str != ""){
			if(list_reserved.contains(str)){    //����ؼ��ֲ��֣�Ӧ�÷ŵ�opr��
				oprnum++;
				opr_set.add(str);
				if(opr_map.containsKey(str)){
					opr_map.put(str, opr_map.get(str) + 1);
				}else{
					opr_map.put(str, 1);
				}
			}else{                  //����opd����
				opdnum++;
				opd_set.add(str);
				list.add(str);
				if(opd_map.containsKey(str)){
					opd_map.put(str, opd_map.get(str) + 1);
				}else{
					opd_map.put(str, 1);
					}
				}
			}
		}
		//System.out.println("=======================�ָ��ߣ�ժ���������������==================");
			
		int opr = 0;	
		Iterator<Map.Entry<String, Integer>> iter_opr = opr_map.entrySet().iterator();
			/*while (iter_opr.hasNext()){
				Map.Entry<String, Integer> entry = iter_opr.next();
				String key = entry.getKey();			
				if(key.equals("char") && entry.getValue() == 1){
					oprnum--;				
					opr_set.remove(key);
				}else if(entry.getValue() == 1){
					opr++;
				}		
			}*/
		while (iter_opr.hasNext()){
			Map.Entry<String, Integer> entry = iter_opr.next();
			String key = entry.getKey();			
			if(entry.getValue() == 1){
				opr++;
			}		
		}	
	
		//System.out.println(opr_set);
		//System.out.println(list);
		int opd = 0;
		Iterator<Map.Entry<String, Integer>> iter_opd = opd_map.entrySet().iterator();
		while (iter_opd.hasNext()){
			Map.Entry<String, Integer> entry = iter_opd.next();
			String key = entry.getKey();
			if(key.equals("va_list") && entry.getValue() == 1){
				opdnum--;
				opd_set.remove(key);
			}else if(entry.getValue() == 1){
				opd++;
			}		
		}	
		//System.out.println("��������������Ϊ��" + opdnum);
		//System.out.println("����Ψһ���������������Ϊ" + opd);
		//System.out.println("����������������Ϊ:" + opd_set.size());
			
		res.add(oprnum);
		res.add(opr);
		res.add(opdnum);
		res.add(opd);
		res.add((oprnum + opdnum));
		//System.out.println(node.getImage() + "�ʻ���Ϊ��" + (opdnum + oprnum));
		return res;
	}
	
	
	
}