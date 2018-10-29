package softtest.CharacteristicExtract.c;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import Jama.Matrix;

import javax.swing.text.html.HTMLDocument.Iterator;

import Jama.Matrix;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;

/** 
 * @author 
 * Miss_lizi
 * ��ȡ�����ṹ������
 */
public class FunctionFeatures {
	
	public static void main(String[] args) throws Exception{
		String filePath2 = "C:/Users/Miss_Lizi/Desktop/depchain/depchain.c";
		FunctionFeatures f = new FunctionFeatures();
		List<String> res = f.getAllFunctionStructFeatures(filePath2);
		for(int i = 0; i < res.size(); i++){
			System.out.println(res.get(i));
		}
	}

	/** �õ��ļ������к����Ľṹ������ */
	public List<String> getAllFunctionStructFeatures(String filepath) throws Exception{
		Func_Features ff = new Func_Features();
		List<String> res = new ArrayList<String>();
		Graph_Info h = new Graph_Info();
		List<CVexNode> list_cvex = new ArrayList<CVexNode>();
		list_cvex = h.getCVexNode(filepath);
		ControlFlowVisitor cfv = new ControlFlowVisitor(filepath);
		ControlFlowData flow = new ControlFlowData();
		
		for(CVexNode c : list_cvex){
			SimpleNode node = c.getMethodNameDeclaration().getNode();
			//List<String> a = new ArrayList<String>();
			if (node instanceof ASTFunctionDefinition){
				ASTFunctionDefinition function = (ASTFunctionDefinition)node;
				cfv.visit((ASTFunctionDefinition)node, flow);
				Graph g = ((ASTFunctionDefinition) node).getGraph();
				String str = filepath + "#" + function.getImage() + "#";
				str += getFunctionStructFeature(g);
				//ff.getALLFuncFeatures(filepath,function.getImage());
				str += "#" + rotatingHash(ff.getFunctionFeatures(filepath, function.getImage()));
				res.add(str);
				//A.print(0, 0);
			}
		}
		return res;
	}
	
	/** �õ������Ľṹ������*/
	public double getFunctionStructFeature(Graph g){
		List<VexNode> list = g.getAllnodes();
		int dimension = list.size();
		double[][] res = new double[dimension+1][dimension+1];
		res = createMatrix(g);
		Matrix A = new Matrix(res);
		//A.print(2, 0);
		return getStructFeature(A);
	}
	
	/** ����������������ͼ�Ķ�ά����*/
	public double[][] createMatrix(Graph g){
		List<VexNode> list = g.getAllnodes();
		int dimension = list.size();
		double[][] res = new double[dimension+1][dimension+1];
		for(VexNode v : list){
			Enumeration enuin = v.getInedges().elements();
			while(enuin.hasMoreElements()) { 
				Edge e = (Edge)enuin.nextElement();
				VexNode head = e.getHeadNode();
				VexNode tail = e.getTailNode();
				fill(res,head,tail);	
			}
			Enumeration enuout = v.getOutedges().elements();
			while(enuout.hasMoreElements()) { 
				Edge e = (Edge)enuout.nextElement();
				VexNode head = e.getHeadNode();
				VexNode tail = e.getTailNode();
				fill(res,head,tail);
			}
		}
		return res;
	}
	
	/**�����ά������*/
	public void fill(double[][] martix, VexNode head, VexNode tail){
		String[] table_head = head.getName().split("_");
		int ii = Integer.valueOf(table_head[table_head.length - 1]);
		String[] table_tail = tail.getName().split("_");
		int jj = Integer.valueOf(table_tail[table_tail.length - 1]);
		martix[ii][jj] = assignFeature(head,tail);
	}
	
	/** ���������������������д���*/
	public double assignFeature( VexNode head, VexNode tail){
		int res = 0;
		StatementFeature sf = new StatementFeature();
		int a = sf.getStatementFeatureInFunction(head).hashCode();
		int b = sf.getStatementFeatureInFunction(tail).hashCode();
		return (sf.rotatingHash(a) + sf.rotatingHash(b))%10000;
	}

	/** ���ݾ�������ֵ����������*/
	public double getStructFeature(Matrix A){
		Matrix mat = A.eig().getD();  //�õ�����ֵ�ĶԽǾ���
		//mat.print(2, 0);
		double res = 1;
		//mat.print(2, 0);
		int i = 0;
		while(i < mat.getColumnDimension() && (int)mat.get(i, i) != 0){  //��ֹ�ر�С�Ľ������½�����������
			//System.out.println(mat.get(i, i));
			res = (res * mat.get(i, i))%500000;
			i++;
		}
		if(i == 0){
			return getOrigianl(A);
		}
		return res;
	}
	
	/** ��ԭʼ������м��㣬�õ�������*/
	public double getOrigianl(Matrix A){
		double res = 0;
		for(int i = 0; i < A.getColumnDimension(); i++){
			for(int j = 0; j < A.getColumnDimension(); j++){
				if(A.get(i, j) != 0){
					//res = (res * A.get(i, j))%500000;
					res = (res + A.get(i, j))%500000;
				}
			}
		}
		return res;
	}
	
	/**���õ���hashcode����һ��λ�����ϣ����*/
	public int rotatingHash(int code){
		String str = String.valueOf(code);
		int hash = str.length();
		for(int i = 0; i < str.length(); i++){
			hash = (hash<<4)^(hash>>28)^str.charAt(i);
		}
		return hash%500000;
	}
}
	
	
