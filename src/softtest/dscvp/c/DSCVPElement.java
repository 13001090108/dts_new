package softtest.dscvp.c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import softtest.ast.c.ASTConstant;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.scvp.c.SCVP;
import softtest.scvp.c.SCVPString;
import softtest.symboltable.c.NameOccurrence;

//չ�����SCVP�ṹ����״�ṹ��ÿ���������һ��DSCVP�ṹ��ʾ
public class DSCVPElement{
	//��ǰչ�����
	int layer;
	//��ǰ��ε�������Դ����
	int dataSourceCnt;
	//��ǰ��α���
	int variableCnt;
    //���ڳ����﷨��
	SimpleNode location;
	//���ڿ�����ͼ
	VexNode cfnode;
	//չ�����SCVP
	String dscvpElementStr;
	//�󶨵�SCVP
	SCVPString scvp;
	//��һ���ṹ
	HashMap<String, HashSet<DSCVPElement>> child = new HashMap<String, HashSet<DSCVPElement>>();
	
	DSCVPElement(int layer, int dataSourceCnt){
		this.layer = layer;
		this.dataSourceCnt = dataSourceCnt;
	}

	public void setStr(){
		if (scvp == null){
			dscvpElementStr = null;
			return;
		}
		dscvpElementStr = scvp.toString();
	}
	
	public String getStr(){
		return dscvpElementStr;
	}
	
	public void addChild(String s, DSCVPElement ele){
		if (child.containsKey(s)){
			child.get(s).add(ele);
		}else{
			HashSet<DSCVPElement> hs = new HashSet<DSCVPElement>();
			hs.add(ele);
			child.put(s, hs);
		}
	}
	
	public SCVPString getSCVP(){
		return scvp;
	}
	
	public void setSCVP(SCVPString scvp){
		this.scvp = scvp;
	}
	
	public SimpleNode getLocation() {
		return location;
	}

	public void setLocation(SimpleNode location) {
		this.location = location;
	}

	public VexNode getCfnode() {
		return cfnode;
	}

	public void setCfnode(VexNode cfnode) {
		this.cfnode = cfnode;
	}
	public int getLayer() {
		return layer;
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}

	public int getDataSourceCnt() {
		return dataSourceCnt;
	}

	public void setDataSourceCnt(int dataSourceCnt) {
		this.dataSourceCnt = dataSourceCnt;
	}

	public int getVariableCnt() {
		return variableCnt;
	}

	public void setVariableCnt(int variableCnt) {
		this.variableCnt = variableCnt;
	}
	
	//add by JJL,ȥ��feature��2016/10/17
	public String getS() {//JJL,scvp = none???
		if (scvp != null) {
			return scvp.getStructure();
		} else {
			return "";
		}
		//return scvp.getStructure();
	}
	
	public String getC() {
		List<String> constants = scvp.getConstants();
		StringBuilder res = new StringBuilder();
		if(constants!=null && constants.size() > 0) {
			for(int i=0;i<constants.size();i++) {
				res.append(constants.get(i));
				if(i!=constants.size()-1)
					res.append(" ");
			}
		} else {
			res.append("null");
		}
		return res.toString();
	}
	
	public String getV() 
	{
		List<String> occs = scvp.getOccs();
		StringBuilder res = new StringBuilder();
		if(occs!=null && occs.size() > 0) {
			res.append(occs + "");
		} else {
			res.append("null");
		}
		return res.toString();
	}
	
	public HashMap<String, HashSet<DSCVPElement>> getChild() {
		return child;
	}
}
