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
 * 提取程序的一些度量元
 */
public class Metric{
	
	//从文本文档中得到所有保留字以及关键字
	 private List<String>  list_reserved = getReservedWords(); 
	
	public Metric(){
		
	}
	
	
	/** 根据graph传入的节点来得到度量信息，得到的整个文件的特征信息*/
	public String getMetric(String filePath) throws Exception{
		//生成抽象语法树
		CParser parser=CParser.getParser(new CCharStream(new FileInputStream(filePath)));
		ASTTranslationUnit node = parser.TranslationUnit();
		if(node == null){
			return null;
		}else{
			//生成对应的控制流图。
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
	
	/** 得到每个函数的特征信息*/
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
				
				HashSet<String> opr_set = new HashSet<String>();    //存放程序中操作符的种类
				HashSet<String> opd_set = new HashSet<String>();    //存放程序中操作数的种类
				HashMap<String, Integer> opr_map = new HashMap<String, Integer>();      //用于找到唯一操作符的个数
				HashMap<String, Integer> opd_map = new HashMap<String, Integer>();      //用于找到唯一操作数的个数
				int oprnum = 0;    //程序中所有的操作符的个数
				int opdnum = 0;    //程序中所有的操作数的个数  
				
				//得到唯一的操作符和操作数
				list = null;
				oprnum = getOperator(node,opr_set,oprnum,opr_map);     //得到代码中所有的符号		
				oprnum = getOperatorNum(node,opr_set,oprnum,opr_map);   //在之前的基础上加上int这些乱七八糟的
				list = getopr_opd(node, list_reserved, opr_set, oprnum, opr_map, opd_set, opdnum, opd_map);
				//list中存储了5个数字，按照顺序分别为：opdnum、opd、oprnum、opr和词汇总数
				//System.out.println(node);
				/** 得到函数的有效行数 len*/
				long len = getFuncLen(node);
				/** 得到函数的McCabe度量：V(G)＝m-n＋2*/
				long McCabe = g.getedgecount() - g.getVexNum() + 2;
				/** 若N为程序的词汇量，则N=N1+N2*/
				long vocabulary_count = (long)(list.get(4));
				/** 程序词汇表为不同运算符种类数和不同运算对象种类数的总和*/
				long words = (opr_set.size() + opd_set.size());
				/** V=(N1+N2)log2(n1+n2)*/
				double capacity = vocabulary_count*(Math.log((double)words)/Math.log((double)2));
				/** 语句平均承载信息量: AVGS=(N1+N2)/(lc_stat)*/
				float AVGS = ((float)vocabulary_count/len);
				/** 词汇频率：VOCF=(N1+N2)/(n1+n2)*/
				float VOCF = ((float)vocabulary_count/(float)words);
				/** 函数注释比：COMF=lc_bcom/(lc_stat)*/
				float COMF = (((float)getFuncIniLen(node)-(float)len)/(float)len);
				
				//函数名称    函数有效行数   McCabe度量   程序词汇表   程序词汇量   程序词容量   语句平均承载信息量   词汇频率   函数注释比
				//String res = "#" + filePath + "中的" + node.getImage() + "函数度量特征为：";
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
	
	
	/** 得到一个函数的有效行数（摘除注释以及空行）最后一行的括号没有算在内*/
	public int getFuncLen(SimpleNode node){
		int len = 0;
		ASTFunctionDefinition fnode = (ASTFunctionDefinition)node;
		ASTStatementList thenode = null;
		if(fnode.jjtGetNumChildren() == 2){
			len++;    //因为函数会占领一行，相当于ASTDeclarator那一行
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
		//System.out.println( node.getImage() + "函数有效行数为：" + len);
		return len;
	}

	/** 得到一个函数的初始行数*/
	public int getFuncIniLen(SimpleNode node){
		ASTFunctionDefinition fnode = (ASTFunctionDefinition)node;
		int initial = fnode.getEndFileLine() - fnode.getBeginFileLine() + 1;
		return initial;
	}
	
	/** 得到一个文件的初始行数*/
	public int getlenIni(SimpleNode node){
		int initial = node.getEndFileLine() - node.getBeginFileLine() + 1;
		return initial;
	}
	/** 得到代码的有效行数（摘除注释以及空行）最后一行的括号没有算在内*/
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
					len++;    //因为函数会占领一行，相当于ASTDeclarator那一行
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
			//System.out.println("包含的函数个数为：" + num_fun);
			return len;
		}
		  
	}
	
	/**得到代码中每一个函数的名字*/
	public  boolean getProject(List<SimpleNode> nodes, FSMMachineInstance fsmin){
		Iterator<SimpleNode> nodeIterator = nodes.iterator();
		int project = 0;
		while (nodeIterator.hasNext()){
			SimpleNode snode = nodeIterator.next();
			if(snode instanceof ASTAssignmentExpression){
				project = snode.getEndLine();
			}
			//project = snode.getImage();
			fsmin.setDesp("在哪一行呀" + project);
		}
		//String project = snode.getImage();
		//System.out.println("该函数名为" + project);
		return true;
	}
		
	/**得到代码中for和while循环的个数*/
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
			//System.out.println("代码中for循环的个数为：" + count_f);
			//System.out.println("代码中while循环的个数为：" + count_w);
			list.add(count_f);
			list.add(count_w);
			return list;
		}
	
	
	/**得到运算符的详细个数,仅包括运算符号那些
	 * （转义字符以及标点符号等没有考虑，后期有时间完善）*/
	public  int getOperator(SimpleNode node, HashSet<String> opr_set, int oprnum,HashMap<String, Integer> opr_map){
		String Xpath = ".//InitDeclarator | .//AssignmentOperator | .//RelationalExpression | .//EqualityExpression |"
				+ ".//AdditiveExpression | .//MultiplicativeExpression | .//LogicalANDExpression | .//LogicalORExpression |"
				+ ".//ShiftExpression | .//ExclusiveORExpression | .//InclusiveORExpression | .//ANDExpression "
				+ "| .//PostfixExpression | .//UnaryExpression | .//UnaryOperator | .//ConditionalExpression | .//Pointer";
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
		List<String> list = new ArrayList<String>();
		
		for(SimpleNode snode : evaluationResults){
			if(snode instanceof ASTInitDeclarator){     //声明同时赋值的时候，int i=1 这种情况中的 = 提取
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
			}else if(snode instanceof ASTANDExpression){    //这里以为&。从ANDExpression中得到的是按位与，Unary那里则是取地址符
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
		//System.out.println("总运算符的个数为" + oprnum);
		//System.out.println("运算符的种类为" + opr_set.size());
		//System.out.println("唯一的运算符的种类个数为" + opr);
		return oprnum;
	}

	//读取文件中的内容，存放到list中
	public  List<String> getReservedWords(){
		File file = new File("reserved.txt");
        BufferedReader reader = null;
        List<String> list_reserved = new ArrayList<String>();
        String str = "";  
        try{
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            // 一次读入一行，直到读入null为文件结束
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
		
	/**得到类型的那些（这些一定都是operator）
	 * 还没有写case default具体在ASTLabeledStatement节点上 */
	public  int getOperatorNum(SimpleNode node,  HashSet<String> opr_set, int oprnum,HashMap<String, Integer> opr_map){
		String Xpath = ".//TypeSpecifier | .//JumpStatement | .//StructOrUnion";
		//TypeSpecifier总是会多提取出来一个char 我不知道为什么
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
		//System.out.println("~~~~~~~~~~~~~~~~~~~~~分割线（没有摘除那俩）~~~~~~~~~~~~~~~~~~~");
		//System.out.println("加上类型名之后总运算符为：" + oprnum);
		//System.out.println("加上类型名之后总运算符为：" + list);
		//System.out.println("加上类型名之后运算符的种类为:" + opr_set.size());
		int opr = 0;
		for (String key : opr_map.keySet()) {
			if(opr_map.get(key) == 1){
				opr++;
			}
		}
		//System.out.println("加上类型名之后唯一的运算符的种类个数为" + opr);
		return oprnum;
	}
		
	/**需要判断是操作数还是操作符的的部分  (节点、关键字列表、oprset、oprlist、oprnum、opdset、opdlist、opdnum)
	后期判断va_list是否唯一的时候，要注意一下*/
	public  List<Integer> getopr_opd(SimpleNode node, List<String> list_reserved, HashSet<String> opr_set, int oprnum, HashMap<String, Integer> opr_map,HashSet<String> opd_set, int opdnum, HashMap<String, Integer> opd_map){
		String Xpath = ".//Declarator | .//PrimaryExpression | .//StructOrUnionSpecifier | .//Fieldld | .//Constant";
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
		List<String> list = new ArrayList<String>();
		List<Integer> res = new ArrayList<Integer>();
			
		//va_list这个东西也会莫名其妙出现在opd中
		for(SimpleNode snode : evaluationResults){
			String str = snode.getImage();
			if(str != ""){
			if(list_reserved.contains(str)){    //输入关键字部分，应该放到opr中
				oprnum++;
				opr_set.add(str);
				if(opr_map.containsKey(str)){
					opr_map.put(str, opr_map.get(str) + 1);
				}else{
					opr_map.put(str, 1);
				}
			}else{                  //加入opd部分
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
		//System.out.println("=======================分割线（摘除了特殊的那俩）==================");
			
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
		//System.out.println("所有运算数总数为：" + opdnum);
		//System.out.println("所有唯一运算数的种类个数为" + opd);
		//System.out.println("所有运算数的种类为:" + opd_set.size());
			
		res.add(oprnum);
		res.add(opr);
		res.add(opdnum);
		res.add(opd);
		res.add((oprnum + opdnum));
		//System.out.println(node.getImage() + "词汇数为：" + (opdnum + oprnum));
		return res;
	}
	
	
	
}
