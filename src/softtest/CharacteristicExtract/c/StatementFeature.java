package softtest.CharacteristicExtract.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import softtest.ast.c.*;
import softtest.callgraph.c.*;
import softtest.cfg.c.*;
import softtest.rules.c.StateMachineUtils;


/** 
 * @author 
 * Miss_lizi
 * 提取语句块特征码
 */

public class StatementFeature {
	private List<String> list = getReservedWords();
	
	
	
	public static void main(String[] args) throws Exception{
		Graph_Info h = new Graph_Info();
		StatementFeature sf = new StatementFeature();
		String filePath = "C:/Users/Miss_Lizi/Desktop/assignment.c";
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
		
		
		//System.out.println(sf.getSelfStatementsFeature(filePath, "switchtest", 48, 57));
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		System.out.println(list_cvex.size());
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			//List<String> a = new ArrayList<String>();
			if (node instanceof ASTFunctionDefinition){
				//ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				List<VexNode> list_g = g.getAllnodes();
				for(VexNode v : list_g){
					System.out.println(sf.getStatementFeature(v));	
				}
				for(VexNode v : list_g){
					System.out.println(sf.getStatementFeature(v).hashCode());	
				}
			}
			
		}
		//sf.getFeatures(filePath);

	}
	
	
	/** 获取该文件内所有函数的语句块*/
	public List<String> getFeatures(String filePath) throws Exception{
		Graph_Info h = new Graph_Info();
		StatementFeature sf = new StatementFeature();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		
		List<String> res = new ArrayList<String>();
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			List<String> a = new ArrayList<String>();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				a = sf.getAllFeaturesInFunction(g, function, filePath);
			}
			res.addAll(a);
		}
		for(int i = 0; i < res.size(); i++){
			System.out.println(res.get(i));
		}
		return res;
	}
	
	/**得到一个函数内的一个语句块信息*/
	public List<String> getAllFeaturesInFunction(Graph g,ASTFunctionDefinition function, String filePath){
		StringBuffer res = new StringBuffer();
		List<String> list_res = new ArrayList<String>();
		String func = filePath + "#";
		func += function.getImage();
		//res.append(filePath + "#" + g.name);
		List<VexNode> list = g.getAllnodes();
		int i = 0;
		List<VexNode> stmt_list = new ArrayList<VexNode>();
		while(i <list.size()){
			stmt_list.clear();
			VexNode v = list.get(i);
			if((v.getName().contains("head")) && !v.getName().startsWith("func") && !v.getName().contains("for_head")){
				SimpleNode stms = v.getTreenode();
				//String[] table = v.getName().split("_");
				int first = Integer.valueOf(v.getName().split("_")[v.getName().split("_").length - 1]);
				int stm_start = stms.getBeginFileLine();
				int stm_end = stms.getEndFileLine();
				res.append("#" + stm_start + "#" + stm_end);
				res.append("#" + getStatementsFeatures(stm_start, stm_end, first, list));	
				list_res.add(func + res.toString());
				res.setLength(0);
			}
			if(v.getName().contains("for_init")){
				VexNode ifnode = list.get(i+1);
				SimpleNode stms = ifnode.getTreenode();
				String[] table = ifnode.getName().split("_");
				int first = Integer.valueOf(table[table.length - 1]);
				int stm_start = stms.getBeginFileLine();
				int stm_end = stms.getEndFileLine();
				res.append("#" + stm_start + "#" + stm_end);
				res.append("#" + getStatementsFeatures(stm_start, stm_end, first, list));
				list_res.add(func + res.toString());
				res.setLength(0);
			}
			i++;
		}
		return list_res;
	}
	

	public List<String> getAllFeaturesInOneFunction(Graph g,ASTFunctionDefinition function){
		StringBuffer res = new StringBuffer();
		List<String> list_res = new ArrayList<String>();
//		String func = filePath + "#";
//		func += function.getImage();
		//res.append(filePath + "#" + g.name);
		List<VexNode> list = g.getAllnodes();
		int i = 0;
		//List<VexNode> stmt_list = new ArrayList<VexNode>();
		while(i <list.size()){
			//stmt_list.clear();
			VexNode v = list.get(i);
			if((v.getName().contains("head")) && !v.getName().startsWith("func") && !v.getName().contains("for_head")){
				SimpleNode stms = v.getTreenode();
				String[] table = v.getName().split("_");
				int first = Integer.valueOf(table[table.length - 1]);
				int stm_start = stms.getBeginFileLine();
				int stm_end = stms.getEndFileLine();
				res.append("#" + stm_start + "#" + stm_end);
				res.append("#" + getStatementsFeatures(stm_start, stm_end, first, list));
				list_res.add(res.toString());
				res.setLength(0);
			}
			if(v.getName().contains("for_init")){
				VexNode ifnode = list.get(i+1);
				SimpleNode stms = ifnode.getTreenode();
				String[] table = ifnode.getName().split("_");
				int first = Integer.valueOf(table[table.length - 1]);
				int stm_start = stms.getBeginFileLine();
				int stm_end = stms.getEndFileLine();
				res.append("#" + stm_start + "#" + stm_end);
				res.append("#" + getStatementsFeatures(stm_start, stm_end, first, list));
				list_res.add(res.toString());
				res.setLength(0);
			}
			i++;
		}
		return list_res;
	}
	

	/**根据起始结束行，得到语句特征值*/
	public int getStatementsFeatures(int start, int end, int first, List<VexNode> list){
		int count = end - start + 2;
		List<VexNode> aim_list = new ArrayList<VexNode>();
		int i = 0;
		while(i < list.size()){
			String[] table = list.get(i).getName().split("_");
			int num = Integer.valueOf(table[table.length - 1]);
			if(num == first){
				while(count !=  0 && i < list.size()){
					aim_list.add(list.get(i++));
					count--;
				}
				break;
			}
			i++;
		}
//		for(VexNode del : aim_list){                                                                                                                                                                                                                                                                                              
//			System.out.println(getStatementFeature(del));
//		}
//		System.out.println("这是数据库里的");
//		System.out.println(getStatementsNum(aim_list));
		return getStatementsNum(aim_list);
	}
	
	
	
	/** 根据给定起始行和终止行，给出自定义语句块的特征值*/
	public String getSelfStatementsFeature(String filepath, String func, int start, int end) throws Exception{
		StringBuffer res = new StringBuffer();
		int count = end - start + 2;
		if(end - start > 30){
			return res.toString();
		}
		Graph_Info h = new Graph_Info();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filepath);
		ControlFlowVisitor cfv = new ControlFlowVisitor(filepath);
		ControlFlowData flow = new ControlFlowData();
		List<VexNode> aim_list = new ArrayList<VexNode>();
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();	
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				ASTDeclarator name = (ASTDeclarator)function.jjtGetChild(1);
			    String str = name.getImage();
			    if(str.contains(func)){
			    	cfv.visit((ASTFunctionDefinition)node, flow);
					Graph g = ((ASTFunctionDefinition) node).getGraph();
					List<VexNode> list = g.getAllnodes();
					for(int i = 0; i < list.size(); i++){
						SimpleNode sd = list.get(i).getTreenode();
						if(sd.getBeginFileLine() == start){
							while(count !=  0 && i < list.size()){
								aim_list.add(list.get(i++));
								count--;
							}
							break;
						}
					}
			    }
			}
		}
//		for(VexNode del : aim_list){
//			System.out.println(getStatementFeature(del));
//		}
		int aim = getStatementsNum(aim_list); 
		return Integer.toString(aim);
	}
	
	public String getReturnFeature(String filePath, String funcname, int start) throws Exception{
		Graph_Info h = new Graph_Info();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filePath);
		ControlFlowVisitor cfv = new ControlFlowVisitor(filePath);
		ControlFlowData flow = new ControlFlowData();
		int res = 0;
		String str = "";
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				if(function.getImage().equals(funcname)){
					String Xpath = ".//JumpStatement";
					List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
					evaluationResults = StateMachineUtils.getEvaluationResults(node, Xpath);
					for(SimpleNode s : evaluationResults){
						ASTJumpStatement jump = (ASTJumpStatement)s;
						if(s.getBeginLine() == start){
							str = visitJumpStatement(jump);
							res = rotatingHash(visitJumpStatement(jump).hashCode());
						}
					}
//					cfv.visit((ASTFunctionDefinition)node, flow);
//					Graph g = ((ASTFunctionDefinition) node).getGraph();
//					List<VexNode> list_g = g.getAllnodes();
				}
							
			}
			
		}
		//return res;
		return str;
	}
	
	/** 得到单一语句的特征，字符串的形式*/
	public String getStatementFeature(VexNode node){
		String name = node.getName();
		SimpleNode tnode = node.getTreenode();
		StringBuffer res = new StringBuffer();
		if(name.startsWith("decl_stmt")){
			res.append(visitDeclarationStatement((ASTDeclaration)tnode));
		}else if(name.startsWith("stmt")){
			res.append(visitExpressionStatement((ASTExpressionStatement)tnode));
		}else if(tnode instanceof ASTJumpStatement){
			res.append(visitJumpStatement((ASTJumpStatement)tnode));
		}else if(name.startsWith("label_head")){
			res.append(visitLabelStatement((ASTLabeledStatement)tnode));
		}else if(name.contains("out") && !name.startsWith("func")){
			res.append("}");
		}else if(name.startsWith("switch_head")){
			res.append(visitSwitchStatements((ASTSelectionStatement)tnode));
		}else if(name.startsWith("do_while_head")){
			res.append(visitDoWhileStatements((ASTIterationStatement)tnode));
		}else if(name.startsWith("while_head")){
			res.append(visitWhileStatements((ASTIterationStatement)tnode));
		}else if(name.startsWith("for_head")){
			res.append(visitForStatements((ASTIterationStatement)tnode));
		}else if(name.startsWith("if_head")){
			res.append(visitIfStatements((ASTSelectionStatement)tnode));
		}
		return res.toString();
	}
	
	/** 得到单一语句的特征，字符串的形式*/
	public String getStatementFeatureInFunction(VexNode node){
		String name = node.getName();
		SimpleNode tnode = node.getTreenode();
		StringBuffer res = new StringBuffer();
		if(name.startsWith("decl_stmt")){
			res.append(visitDeclarationStatement((ASTDeclaration)tnode));
		}else if(name.startsWith("stmt")){
			res.append(visitExpressionStatement((ASTExpressionStatement)tnode));
		}else if(tnode instanceof ASTJumpStatement){
			res.append(visitJumpStatement((ASTJumpStatement)tnode));
		}else if(name.startsWith("label_head")){
			res.append(visitLabelStatement((ASTLabeledStatement)tnode));
		}else if(name.startsWith("switch_head") || name.startsWith("switch_out")){
			res.append(visitSwitchStatements((ASTSelectionStatement)tnode));
		}else if(name.startsWith("do_while_head") || name.startsWith("do_while_out_2")){
			res.append(visitDoWhileStatements((ASTIterationStatement)tnode));
		}else if(name.startsWith("while_head") || name.startsWith("while_out")){
			res.append(visitWhileStatements((ASTIterationStatement)tnode));
		}else if(name.startsWith("for_head") || name.startsWith("for_out")){
			res.append(visitForStatements((ASTIterationStatement)tnode));
		}else if(name.startsWith("if_head") || name.startsWith("if_out")){
			res.append(visitIfStatements((ASTSelectionStatement)tnode));
		}else if(name.startsWith("for_init")){
//			ASTExpression second = (ASTExpression)tnode.jjtGetChild(1);
//			res.append(visitDeclarationStatement((ASTDeclaration)tnode));
		}else if(name.startsWith("for_inc")){
			ASTExpression enode = (ASTExpression)tnode;
			ASTPostfixExpression pnode = (ASTPostfixExpression)enode.getFirstChildOfType(ASTPostfixExpression.class);
			if(pnode.jjtGetNumChildren() == 1){
				if(pnode.getOperators().isEmpty()){
					ASTUnaryExpression unode = (ASTUnaryExpression)pnode.getFirstParentOfType(ASTUnaryExpression.class);
					res.append
					(unode.getOperators() + pnode.getType().toString());
				}else{
					res.append(pnode.getType().toString() + pnode.getOperators());
				}
			}else{
				res.append(func_param(pnode));
			}
		}else if(name.startsWith("func")){
			ASTFunctionDefinition func = (ASTFunctionDefinition)tnode;
			String str = func.getType().toString();
			int i = 0;
			while(i < str.length() && str.charAt(i) != '('){	
				i++;
			}
			res.append(str.substring(i));
		}
		return res.toString();
	}
	
	
	/**得到语句块特征的统一接口*/
	public String getFeatures(List<VexNode> while_list){    //现在给出的是一个字符串形式，后面改成特征码的形式
		StringBuffer res = new StringBuffer();
		for(VexNode del : while_list){
			res.append(getStatementFeature(del));
		}
		return res.toString();
	}
	
	
	/**提取单个声明语句的特征信息 ASTDeclaration */
	public String visitDeclarationStatement(ASTDeclaration dnode){     //单条声明特征的提取
		//可以提取出声明，声明同时定义，声明是结构体（给出结构体内部的类型定义）	                                                   
		StringBuffer res = new StringBuffer();
//		ASTDeclaration dnode = (ASTDeclaration)node.getTreenode();
		ASTInitDeclaratorList inode = (ASTInitDeclaratorList)dnode.jjtGetChild(1);
		for(int i = 0; i < inode.jjtGetNumChildren(); i++){
			ASTInitDeclarator node1 = (ASTInitDeclarator)inode.jjtGetChild(i);
			ASTDeclarator tnode = (ASTDeclarator)node1.jjtGetChild(0);
			if(tnode.getType().toString().contains("struct")){
				if(tnode.getType().toString().contains("*")){
					res.append("*struct:");
				}else{
					res.append("struct:");
				}
				String[] str = tnode.getType().toString().split(":");
				for(int j = 1; j < str.length; j++){
					String[] str1 = str[j].split(" ");
					for(String s : str1){
						if(list.contains(s)){
							res.append(" "+ s);
						}
					}
				}
			}else{
				res.append(tnode.getType().toString());
			}
			if(node1.jjtGetNumChildren() == 1){
				res.append(" " + "var" + i + " ");
			}else{
				res.append(" var_assign");
				ASTInitializer iinode = (ASTInitializer)node1.jjtGetChild(1);
				ASTUnaryExpression unode = (ASTUnaryExpression)iinode.getFirstChildInstanceofType(ASTUnaryExpression.class);
				if(unode.jjtGetNumChildren() == 1 && unode.jjtGetChild(0) instanceof ASTPostfixExpression){
					//ASTPostfixExpression pnode1 = (ASTPostfixExpression)unode.jjtGetChild(0);
					ASTPrimaryExpression pnode = (ASTPrimaryExpression)unode.getFirstChildOfType(ASTPrimaryExpression.class);
					//res.append(func_param(pnode1));
					if(pnode.isLeaf()){
						if(pnode.isMethod()){
							if(list.contains(pnode.getImage())){
								res.append(" lib_func_" + pnode.getImage());
							}else{
								res.append(" method");
							}
						}
					}else{
						//ASTConstant con = (ASTConstant)pnode.jjtGetChild(0);
						//res.append(" constant " + con.getType().toString() + " " + con.getImage());
						res.append(" constant ");
					}
				}else if(unode.jjtGetNumChildren() == 1 && unode.jjtGetChild(0) instanceof ASTInitializerList){
					int j = 0;
					String Xpath = ".//AssignmentExpression";
					List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
					evaluationResults = StateMachineUtils.getEvaluationResults(unode, Xpath);
					for(SimpleNode snode : evaluationResults){
						ASTAssignmentExpression ssnode = (ASTAssignmentExpression)snode; 
						res.append(" " + ssnode.getType().toString() + " var" + (j++));
					}
				}else{
					if(unode.jjtGetChild(0) instanceof ASTUnaryOperator){
						ASTUnaryOperator anode = (ASTUnaryOperator)unode.jjtGetChild(0);
						res.append(" " + anode.getOperators() + "var");
					}
				}
			}
		}
		return res.toString();
	}
	
	/** 对ASTAssignmentExpression*/
	public String visitAssignmentExpression(ASTAssignmentExpression right){
		StringBuffer res = new StringBuffer();
		String Xpath = ".//UnaryExpression";
		List<SimpleNode> unary = new LinkedList<SimpleNode>();
		unary = StateMachineUtils.getEvaluationResults(right, Xpath);
		for(SimpleNode un : unary){
			ASTUnaryExpression una = (ASTUnaryExpression)un;
			if(una.jjtGetChild(0) instanceof ASTPostfixExpression){
				ASTPostfixExpression pnode2 = (ASTPostfixExpression)una.jjtGetChild(0);
				ASTPrimaryExpression pri = (ASTPrimaryExpression) pnode2.jjtGetChild(0);
				if(pnode2.jjtGetNumChildren() != 1){
					if(pri.isMethod()){
						res.append(" " + func_param(pnode2));
					}else{
						if(pri.getType() != null){
							if(!pri.getType().toString().isEmpty()){
								if(pri.getType().toString().contains("struct")){
								
									if(pri.getType().toString().contains("*")){
										res.append("*struct:");
									}else{
										res.append("struct:");
									}
									String[] str = pri.getType().toString().split(":");
									for(int j = 1; j < str.length; j++){
										String[] str1 = str[j].split(" ");
										for(String s : str1){
											if(list.contains(s)){
												res.append(" "+ s);
											}
										}
									}
									if(pnode2.jjtGetChild(1) instanceof ASTFieldId){
										ASTFieldId f = (ASTFieldId)pnode2.jjtGetChild(1);
										if(f.getType() != null){
											res.append("->" + f.getType().toString());
										}
									}
									
								}	
												
							}else{
								res.append(" " + func_param(pnode2));
							}
						}else{
							res.append(" constant " + pnode2.getType().toString());
						}
					}
				}else{
					if(!pri.getImage().isEmpty()){
						if(pri.isMethod()){
							res.append(" " + func_param(pnode2));
						}else{
							res.append(" " + pnode2.getType().toString());
						}
					}else{
						res.append(" constant " + pnode2.getType().toString());
					}
				}
			}else{
//				if(una.jjtGetChild(0) instanceof ASTUnaryOperator){
//					ASTUnaryOperator uo = (ASTUnaryOperator)una.jjtGetChild(0);
//					res.append(" " + uo.getOperators());
//				}
//				ASTPostfixExpression pnodee = (ASTPostfixExpression)una.jjtGetChild(1).jjtGetChild(0);
//				res.append(func_param(pnodee));
				continue;
			}
		}
		String Xpath1 = ".//AdditiveExpression | .//AssignmentOperator | .//MultiplicativeExpression | .//LogicalANDExpression"
				+ "| .//EqualityExpression | .//ExclusiveORExpression | .//LogicalORExpression | .//InclusiveORExpression | .//RelationalExpression";
		List<SimpleNode> opers = new LinkedList<SimpleNode>();
		opers = StateMachineUtils.getEvaluationResults(right, Xpath1);
		for(SimpleNode opr : opers){
			res.append(" " + opr.getOperators());
		}
		return res.toString();
	}
	
	
	/**提取单个赋值语句的特征信息  ASTExpressionStatement*/
	public String visitExpressionStatement(ASTExpressionStatement node){
		//函数直接的使用也属于这里，比如scanf、print那种
		StringBuffer res = new StringBuffer();
		if(node.isLeaf()){
			return res.toString();
		}
		ASTAssignmentExpression assign = (ASTAssignmentExpression)node.getFirstChildInstanceofType(ASTAssignmentExpression.class);
		//ASTAssignmentExpression assign = (ASTAssignmentExpression)node.jjtGetChild(0);
		if(assign.jjtGetNumChildren() == 3){
			ASTAssignmentExpression right = (ASTAssignmentExpression)assign.jjtGetChild(2);
			ASTPostfixExpression pnode = (ASTPostfixExpression)assign.getFirstChildInstanceofType(ASTPostfixExpression.class);
			if(pnode.jjtGetNumChildren() == 1){
				res.append(pnode.getType().toString() + " varleft");
			}else{
				ASTPrimaryExpression pri = (ASTPrimaryExpression)pnode.jjtGetChild(0);
				if(pri.getType() != null){
					if(!pri.getType().toString().isEmpty()){
						if(pri.getType().toString().contains("struct")){
							
							if(pri.getType().toString().contains("*")){
								res.append("*struct:");
							}else{
								res.append("struct:");
							}
							String[] str = pri.getType().toString().split(":");
							for(int j = 1; j < str.length; j++){
								String[] str1 = str[j].split(" ");
								for(String s : str1){
									if(list.contains(s)){
										res.append(" "+ s);
									}
								}
							}
							if(pnode.jjtGetChild(1) instanceof ASTFieldId){
								ASTFieldId f = (ASTFieldId)pnode.jjtGetChild(1);
								if(f.getType() != null){
									res.append("->" + f.getType().toString());
								}
							}
							
						}	
					
										
					}else{
						res.append(" " + func_param(pnode));
					}
				}else{
					res.append(" constant " + pnode.getType().toString());
				}
			}
			ASTAssignmentOperator op = (ASTAssignmentOperator)assign.jjtGetChild(1);
			res.append(" " +op.getOperators() + " ");
			//ASTPostfixExpression pnode2 = (ASTPostfixExpression)right.getFirstChildOfType(ASTPostfixExpression.class);
			
			res.append(visitAssignmentExpression(right));
			
			
//			String Xpath = ".//UnaryExpression";
//			List<SimpleNode> unary = new LinkedList<SimpleNode>();
//			unary = StateMachineUtils.getEvaluationResults(right, Xpath);
//			for(SimpleNode un : unary){
//				ASTUnaryExpression una = (ASTUnaryExpression)un;
//				ASTPostfixExpression pnode2 = (ASTPostfixExpression)una.jjtGetChild(0);
//				ASTPrimaryExpression pri = (ASTPrimaryExpression) pnode2.jjtGetChild(0);
//				if(pnode2.jjtGetNumChildren() != 1){
//					if(!pnode2.getImage().isEmpty()){
//						if(pri.getType().toString().contains("struct")){
//							ASTFieldId f = (ASTFieldId)pnode2.jjtGetChild(1);
//							if(pri.getType().toString().contains("*")){
//								res.append("*struct:");
//							}else{
//								res.append("struct:");
//							}
//							String[] str = pri.getType().toString().split(":");
//							for(int j = 1; j < str.length; j++){
//								String[] str1 = str[j].split(" ");
//								for(String s : str1){
//									if(list.contains(s)){
//										res.append(" "+ s);
//									}
//								}
//							}
//							res.append("->" + f.getType().toString());					
//						}else{
//							res.append(" " + func_param(pnode2));
//						}
//					}else{
//						res.append(" constant " + pnode2.getType().toString());
//					}
//				}else{
//					if(!pri.getImage().isEmpty()){
//						if(pri.isMethod()){
//							res.append(" " + func_param(pnode2));
//						}else{
//							res.append(" " + pnode2.getType().toString());
//						}
//					}else{
//						res.append(" constant " + pnode2.getType().toString());
//					}
//				}
//			}
//			String Xpath1 = ".//AdditiveExpression | .//AssignmentOperator | .//MultiplicativeExpression | .//LogicalANDExpression"
//					+ "| .//EqualityExpression | .//ExclusiveORExpression | .//LogicalORExpression | .//InclusiveORExpression";
//			List<SimpleNode> opers = new LinkedList<SimpleNode>();
//			opers = StateMachineUtils.getEvaluationResults(right, Xpath1);
//			for(SimpleNode opr : opers){
//				res.append(" " + opr.getOperators());
//			}
			
			
		}else if(assign.jjtGetNumChildren() == 1){     //直接使用函数的情况
			if(assign.jjtGetChild(0) instanceof ASTConditionalExpression){
				res.append(visitAssignmentExpression(assign));
			}else{
				ASTPostfixExpression pnode2 = (ASTPostfixExpression)assign.getFirstChildInstanceofType(ASTPostfixExpression.class);
				ASTPrimaryExpression pri = (ASTPrimaryExpression)pnode2.jjtGetChild(0);
				if(pnode2.jjtGetNumChildren() != 1){
					if(pri.getType() != null){
						if(!pri.getType().toString().isEmpty()){
							if(pri.getType().toString().contains("struct")){
								
								
								if(pri.getType().toString().contains("*")){
									res.append("*struct:");
								}else{
									res.append("struct:");
								}
								String[] str = pri.getType().toString().split(":");
								for(int j = 1; j < str.length; j++){
									String[] str1 = str[j].split(" ");
									for(String s : str1){
										if(list.contains(s)){
											res.append(" "+ s);
										}
									}
								}
								if(pnode2.jjtGetChild(1) instanceof ASTFieldId){
									ASTFieldId f = (ASTFieldId)pnode2.jjtGetChild(1);
									if(f.getType() != null){
										res.append("->" + f.getType().toString());
									}
								}
								
							}	
											
						}else{
							res.append(" " + func_param(pnode2));
						}
					}else{
						res.append(" constant " + pnode2.getType().toString());
					}
				}else{
					if(!pri.getImage().isEmpty()){
						if(pri.isMethod()){
							res.append(" " + func_param(pnode2));
						}else{
							res.append(" " + pnode2.getType().toString());
						}
					}else{
						res.append(" constant " + pnode2.getType().toString());
					}
				}
			}
		}
		return res.toString();
	}
	
	/**提取单个赋值语句的特征信息  ASTExpressionStatement*/
	public String visitExpressionStatement(ASTAssignmentExpression assign){
		//函数直接的使用也属于这里，比如scanf、print那种
		StringBuffer res = new StringBuffer();
		if(assign.jjtGetNumChildren() == 3){
			ASTAssignmentExpression right = (ASTAssignmentExpression)assign.jjtGetChild(2);
			ASTPostfixExpression pnode = (ASTPostfixExpression)assign.getFirstChildInstanceofType(ASTPostfixExpression.class);
			if(pnode.jjtGetNumChildren() == 1){
				res.append(pnode.getType().toString() + " varleft");
			}else{
				ASTPrimaryExpression a = (ASTPrimaryExpression)pnode.jjtGetChild(0);
				if(a.getType().toString().contains("struct")){
					if(a.getType().toString().contains("*")){
						res.append("*struct:");
					}else{
						res.append("struct:");
					}
					String[] str = a.getType().toString().split(":");
					for(int j = 1; j < str.length; j++){
						String[] str1 = str[j].split(" ");
						for(String s : str1){
							if(list.contains(s)){
								res.append(" "+ s);
							}
						}
					}
					if(pnode.jjtGetChild(1) instanceof ASTFieldId){
						ASTFieldId f = (ASTFieldId)pnode.jjtGetChild(1);
						if(f.getType() != null){
							res.append("->" + f.getType().toString() + " varleft");
						}
					}
				}else{
					res.append(func_param(pnode));
				}
			}
			ASTAssignmentOperator op = (ASTAssignmentOperator)assign.jjtGetChild(1);
			res.append(" " +op.getOperators() + " ");
			//ASTPostfixExpression pnode2 = (ASTPostfixExpression)right.getFirstChildOfType(ASTPostfixExpression.class);
			
			res.append(visitAssignmentExpression(right));
			
			
//			String Xpath = ".//UnaryExpression";
//			List<SimpleNode> unary = new LinkedList<SimpleNode>();
//			unary = StateMachineUtils.getEvaluationResults(right, Xpath);
//			for(SimpleNode un : unary){
//				ASTUnaryExpression una = (ASTUnaryExpression)un;
//				ASTPostfixExpression pnode2 = (ASTPostfixExpression)una.jjtGetChild(0);
//				ASTPrimaryExpression pri = (ASTPrimaryExpression) pnode2.jjtGetChild(0);
//				if(pnode2.jjtGetNumChildren() != 1){
//					if(!pnode2.getImage().isEmpty()){
//						if(pri.getType().toString().contains("struct")){
//							ASTFieldId f = (ASTFieldId)pnode2.jjtGetChild(1);
//							if(pri.getType().toString().contains("*")){
//								res.append("*struct:");
//							}else{
//								res.append("struct:");
//							}
//							String[] str = pri.getType().toString().split(":");
//							for(int j = 1; j < str.length; j++){
//								String[] str1 = str[j].split(" ");
//								for(String s : str1){
//									if(list.contains(s)){
//										res.append(" "+ s);
//									}
//								}
//							}
//							res.append("->" + f.getType().toString());					
//						}else{
//							res.append(" " + func_param(pnode2));
//						}
//					}else{
//						res.append(" constant " + pnode2.getType().toString());
//					}
//				}else{
//					if(!pri.getImage().isEmpty()){
//						if(pri.isMethod()){
//							res.append(" " + func_param(pnode2));
//						}else{
//							res.append(" " + pnode2.getType().toString());
//						}
//					}else{
//						res.append(" constant " + pnode2.getType().toString());
//					}
//				}
//			}
//			String Xpath1 = ".//AdditiveExpression | .//AssignmentOperator | .//MultiplicativeExpression | .//LogicalANDExpression"
//					+ "| .//EqualityExpression | .//ExclusiveORExpression | .//LogicalORExpression | .//InclusiveORExpression";
//			List<SimpleNode> opers = new LinkedList<SimpleNode>();
//			opers = StateMachineUtils.getEvaluationResults(right, Xpath1);
//			for(SimpleNode opr : opers){
//				res.append(" " + opr.getOperators());
//			}
			
			
		}else{     //直接使用函数的情况
			ASTPostfixExpression pnode = (ASTPostfixExpression)assign.getFirstChildOfType(ASTPostfixExpression.class);
			if(pnode.jjtGetNumChildren() == 1){
				if(pnode.getOperators().isEmpty()){
					ASTUnaryExpression unode = (ASTUnaryExpression)assign.jjtGetChild(0);
					res.append(unode.getOperators() + pnode.getType().toString());
				}else{
					res.append(pnode.getType().toString() + pnode.getOperators());
				}
			}else{
				res.append(func_param(pnode));
			}
		}
		return res.toString();
	}	
	
	/**提取跳转语句块的特征信息  ASTJumpStatement*/
	public String  visitJumpStatement(ASTJumpStatement jump){    //goto语句还没有考虑，后面再加上
		StringBuffer res = new StringBuffer();
		//ASTJumpStatement jump = (ASTJumpStatement)node.getTreenode();
		String name = jump.getImage();
		if(name.equals("continue")){
			res.append(name);
		}else if(name.equals("break")){
			res.append(name);
		}else if(name.equals("goto")){
			
		}else{       // return
			//
			List<VexNode> node = jump.getVexlist();
//			for(int i = 0; i < node.size(); i++){
//				System.out.println(node.get(i) + "!!!!!");
//			}
			res.append("return ");
			if(!jump.isLeaf()){
				ASTAssignmentExpression anode =(ASTAssignmentExpression)jump.getFirstChildOfType(ASTAssignmentExpression.class);
				//res.append(visitAssignmentExpression(anode));
				if(anode.jjtGetChild(0) instanceof ASTUnaryExpression){
					ASTPostfixExpression pnode = (ASTPostfixExpression)jump.getFirstChildOfType(ASTPostfixExpression.class);
					if(pnode.jjtGetNumChildren() == 1){
						if(pnode.getType().toString().contains("struct")){
							String[] str = pnode.getType().toString().split(":");
							if(pnode.getType().toString().contains("*")){
								res.append("*struct:" + str[str.length - 1]);
							}else{
								res.append("struct:" + str[str.length - 1]);
							}						
						}else{
							res.append(pnode.getType().toString());
						}
					}else{
						res.append(func_param(pnode));
					}
				}else{             //对应的是一个运算表达式
					res.append(visitAssignmentExpression(anode));
					//if(anode.jjtGetChild(0) instanceof ASTConditionalExpression){
						
						
						
//						ASTConditionalExpression cnode = (ASTConditionalExpression)anode.jjtGetChild(0);
//						ASTRelationalExpression rnode = (ASTRelationalExpression)cnode.getFirstChildOfType(ASTRelationalExpression.class);
//						String Xpath = ".//PostfixExpression";
//						List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
//						evaluationResults = StateMachineUtils.getEvaluationResults(rnode, Xpath);
//						int flag = 0;
//						for(SimpleNode s : evaluationResults){
//							ASTPostfixExpression pnode = (ASTPostfixExpression)s;
//							if(pnode.jjtGetNumChildren() == 2){
//								res.append(func_param(pnode));
//							}else{
//								res.append(pnode.getType().toString() + " ");
//							}
//							if(flag == 0){
//								res.append("< ");
//							}
//							flag = 1;
//						}
//						res.append("? ");
//						ASTExpression enode = (ASTExpression)cnode.jjtGetChild(1);
//						ASTPostfixExpression pnode2 = (ASTPostfixExpression)enode.getFirstChildOfType(ASTPostfixExpression.class);
//						res.append(pnode2.getType().toString());
//						res.append(" : ");
//						ASTPostfixExpression  pnode3 = (ASTPostfixExpression)cnode.jjtGetChild(2).jjtGetChild(0);
//						res.append(pnode3.getType().toString());
						
//					}else{
//						String Xpath = ".//PostfixExpression";
//						List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
//						evaluationResults = StateMachineUtils.getEvaluationResults(jump, Xpath);
//						for(SimpleNode s : evaluationResults){
//							ASTPostfixExpression pnode = (ASTPostfixExpression)s;
//							if(pnode.jjtGetNumChildren() == 2){
//								res.append(func_param(pnode));
//							}else{
//								res.append(pnode.getType().toString() + " ");
//							}
//						}
//					}
				}
			}
			
		}
		
		return res.toString();
	}
	
	/**提取label语句的特征信息 ASTLabeledStatement*/
	public String visitLabelStatement(ASTLabeledStatement label){
		StringBuffer res = new StringBuffer();
		//ASTLabeledStatement label = (ASTLabeledStatement)node.getTreenode();
		if(label.jjtGetNumChildren() == 1){
			res.append("label_defalut: ");		
		}else{
			res.append("label_case: ");	
		}
		return res.toString();
	}
	
	/**提取do_while和while的出口语句特征 ASTExpression*/
	public String visitDoWhileOutExpression(ASTExpression enode){
		StringBuffer res = new StringBuffer();
		ASTAssignmentExpression assign = (ASTAssignmentExpression)enode.jjtGetChild(0);
		
		res.append(visitAssignmentExpression(assign));
		
//		SimpleNode rnode = (SimpleNode)assign.jjtGetChild(0);
//		if(assign.jjtGetChild(0) instanceof ASTRelationalExpression){
//			rnode = (ASTRelationalExpression)assign.jjtGetChild(0);
//		}else if(assign.jjtGetChild(0) instanceof ASTEqualityExpression){
//			rnode = (ASTEqualityExpression)assign.jjtGetChild(0);
//		}else if(assign.jjtGetChild(0) instanceof ASTAdditiveExpression){
//			rnode = (ASTAdditiveExpression)assign.jjtGetChild(0);
//		}
//		
//		String Xpath = ".//UnaryExpression";
//		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
//		evaluationResults = StateMachineUtils.getEvaluationResults(assign, Xpath);
//		for(SimpleNode s : evaluationResults){
//			ASTUnaryExpression un = (ASTUnaryExpression)s;
//			if(un.jjtGetChild(0) instanceof ASTPostfixExpression){
//				ASTPostfixExpression pnode = (ASTPostfixExpression)un.jjtGetChild(0);
//				if(pnode.jjtGetNumChildren() == 1){
//					ASTPrimaryExpression left = (ASTPrimaryExpression)pnode.jjtGetChild(0);
//					if(left.isLeaf()){
//						res.append(pnode.getType().toString() + " ");
//					}else{
//						res.append("constant " + pnode.getType().toString());
//					}
//				}else{
//					res.append(func_param(pnode));
//				}
//				res.append("opretators ");
//			}else{
//				continue;
//			}
//			
//		}
		
//		ASTPostfixExpression pnode1 = (ASTPostfixExpression)rnode.jjtGetChild(0).jjtGetChild(0);
//		ASTPostfixExpression pnode2 = (ASTPostfixExpression)rnode.jjtGetChild(1).jjtGetChild(0);
//		if(pnode1.jjtGetNumChildren() == 1){
//			ASTPrimaryExpression left = (ASTPrimaryExpression)pnode1.jjtGetChild(0);
//			if(left.isLeaf()){
//				res.append(pnode1.getType().toString() + " ");
//			}else{
//				res.append("constant " + pnode1.getType().toString());
//			}
//		}else{
//			res.append(func_param(pnode1));
//		}
//		res.append(rnode.getOperators() + " ");
//		if(pnode2.jjtGetNumChildren() == 1){
//			ASTPrimaryExpression left = (ASTPrimaryExpression)pnode2.jjtGetChild(0);
//			if(left.isLeaf()){
//				res.append(pnode2.getType().toString() + " ");
//			}else{
//				res.append("constant " + left.getType().toString());
//			}
//		}else{
//			res.append(func_param(pnode2));
//		}
		return res.toString();
	}
	
	/**提取if语句块的特征信息 ASTSelectionStatement*/
	public String visitIfStatements(ASTSelectionStatement fi){
		StringBuffer res = new StringBuffer();
		res.append("if:(");
		ASTExpression enode = (ASTExpression)fi.jjtGetChild(0);
		res.append(visitDoWhileOutExpression(enode));
		res.append(")" + "{");
		return res.toString();
	}
	
	
	
	/**提取for语句块的特征信息 ASTSelectionStatement*/
	public String visitForStatements(ASTIterationStatement fo){
		StringBuffer res = new StringBuffer();
		res.append("for:(");
		if(fo.jjtGetNumChildren() == 4){
			if(fo.jjtGetChild(0) instanceof ASTDeclaration){
				ASTDeclaration dnode = (ASTDeclaration)fo.jjtGetChild(0);
				res.append(visitDeclarationStatement(dnode));
			}else if(fo.jjtGetChild(0) instanceof ASTExpression){
				ASTAssignmentExpression ass = (ASTAssignmentExpression)fo.jjtGetChild(0).jjtGetChild(0);
				res.append(visitExpressionStatement(ass));
			}
			ASTExpression second = (ASTExpression)fo.jjtGetChild(1);
			ASTExpression third = (ASTExpression)fo.jjtGetChild(2);
			res.append(visitDoWhileOutExpression(second));
			ASTPostfixExpression pnode = (ASTPostfixExpression)third.getFirstChildOfType(ASTPostfixExpression.class);
			if(pnode.jjtGetNumChildren() == 1){
				if(pnode.getOperators().isEmpty()){
					ASTUnaryExpression unode = (ASTUnaryExpression)pnode.getFirstParentOfType(ASTUnaryExpression.class);
					res.append(unode.getOperators() + pnode.getType().toString());
				}else{
					res.append(pnode.getType().toString() + pnode.getOperators());
				}
			}else{
				res.append(func_param(pnode));
			}
			res.append(")" + "{");
		}
		return res.toString();
	}
	
	/**提取while语句块的特征信息 ASTIterationStatement*/
	public String visitWhileStatements(ASTIterationStatement whi){
		StringBuffer res = new StringBuffer();
		res.append("while:(");
		ASTExpression enode = (ASTExpression)whi.jjtGetChild(0);
		res.append(visitDoWhileOutExpression(enode));
		res.append(")");
		return res.toString();
	}
	
	
	
	/**提取do_while语句块的特征信息 ASTIterationStatement*/
	public String visitDoWhileStatements(ASTIterationStatement dowhile){
		StringBuffer res = new StringBuffer();
		res.append("do_while:(");
		ASTExpression enode = (ASTExpression)dowhile.jjtGetChild(1);
		res.append(visitDoWhileOutExpression(enode));
		res.append(")");
		return res.toString();
	}

	/**提取switch语句块的特征信息  ASTSelectionStatement*/
	public String visitSwitchStatements(ASTSelectionStatement swt){
		StringBuffer res = new StringBuffer();
		res.append("switch:(");
		ASTExpression enode = (ASTExpression)swt.jjtGetChild(0);
		res.append(enode.getType().toString() + ")" + "{");
		return res.toString();
	}
	
	/**对函数进行处理*/
	public String func_param(ASTPostfixExpression pnode){
		StringBuffer res = new StringBuffer();
		ASTPrimaryExpression func = (ASTPrimaryExpression)pnode.jjtGetChild(0);
		if(func.isLeaf()){
			if(func.isMethod()){
				if(list.contains(func.getImage())){
					res.append("lib_func_" + func.getImage());
				}else{
					res.append("method_");
				}
			}
		}
		String Xpath = ".//AssignmentExpression";
		List<SimpleNode> evaluationResults = new LinkedList<SimpleNode>();
		evaluationResults = StateMachineUtils.getEvaluationResults(pnode, Xpath);
		int j = 0;
		for(SimpleNode s : evaluationResults){
			ASTAssignmentExpression ssnode = (ASTAssignmentExpression)s;
			if(ssnode.getType() != null){
				if(ssnode.getType().toString().contains("struct")){
					res.append("(" + "struct" + " param" + j++ + ")");
				}else{
					res.append("(" + ssnode.getType().toString() + " param" + j++ + ")");
				}
			}
		}
		return res.toString();
	}
	
	/**获得C语言中常用的关键字和库函数*/
	public  List<String> getReservedWords(){
		File file = new File(".\\reserved.txt");

		
		if(!file.exists()){
		    //先得到文件的上级目录，并创建上级目录，在创建文件
		    file.getParentFile().mkdir();
		    try {
		        //创建文件
		        file.createNewFile();
		        System.out.println(file.getAbsolutePath()+"++++++++++++++++++++++++++++++++++++++++++++++++++");
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		
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
        }catch(IOException e){
            e.printStackTrace();
        }
        return list_reserved;   
	}
	
	/**将得到的hashcode进行一次位运算哈希函数*/
	public int rotatingHash(int code){
		String str = String.valueOf(code);
		int hash = str.length();
		for(int i = 0; i < str.length(); i++){
			hash = (hash<<4)^(hash>>28)^str.charAt(i);
		}
		return hash%500000;
	}
	
	/** 得到语句块的特征码*/
	public int getStatementsNum(List<VexNode> list){
		int res = 0;
		for(VexNode node : list){
			res += getStatementFeature(node).hashCode();
			res = res % 5000000;
		}
		return rotatingHash(res);
	}
}