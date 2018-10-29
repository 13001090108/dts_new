package softtest.scvp.c;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class SCVPString implements Serializable{
	/**
	 * @author cmershen
	 * @date 2016.10.31
	 * @description Ӧ��Ҫ�󣬽�SCVP��д��String������ʽ���Է�ֹjvm���������
	 */
	private List<String> ipOccs;
	private String structure;
	private List<String> constants;
	private List<String> occs;
	private Position position;//P�����û���ʲô�﷨����
	private String var;
	
	public String getVar() {
		return var;
	}

	public void setVar(String var) {
		this.var = var;
	}
	
	
	public SCVPString() {
		structure = "";
		constants = new ArrayList<String>();
		occs = new ArrayList<String>();
		position = new Position();
	}
	
	public void setIpOccs(List<String> ipOccs) {
		this.ipOccs = ipOccs;
	}
	public void addIpOccs(String ipocc) {
		if (ipOccs == null)
			ipOccs = new ArrayList<String>();
		ipOccs.add(ipocc);
	}
	
	public List<String> getIpOcc() {
		return ipOccs;
	}
	
	public String getStructure() {
		return structure;
	}
	public void setStructure(String structure) {
		this.structure = structure;
	}
	public List<String> getConstants() {
		return constants;
	}
	public void setConstants(List<String> constants) {
		this.constants = constants;
	}
	public List<String> getOccs() {
		return occs;
	}
	public void setOccs(List<String> variables) {
		this.occs = variables;
	}
	public Position getPosition() {
		return position;
	}
	public void setPosition(Position position) {
		this.position = position;
	}
	@Override
	public String toString() {
//		return var +" [structure=" + structure + ", constants=" + constants + ", occs=" + occs + ", position="
//				+ position + "]";
		return "[structure=" + structure + ", constants=" + constants + ", occs=" + occs + ", position="
		+ position + "]";//modify by JJL, ȥ������
	}
	
	public String replaceString() {
		return "[structure=" + structure + ", constants=" + constants + ", occs=" + occs + ", position="
				+ position + "]";
	}
}
