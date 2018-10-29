package softtest.fsmanalysis.c;

import java.util.List;

import softtest.database.c.*;
import softtest.fsmanalysis.c.Report;

/** ���ڿ�����״̬���������ݴ��� */
public class FSMControlFlowData {
	/** �Ƿ񱨸���� */
	public boolean reporterror = false;

	/**
	 * ��ǰ���������д��󱨸�ļ�¼��Ԫ�����ڵ�Ԫ����
	 */
	private List<Report> reports;
	
	/**
	 * ��ǰ���������б���ļ�¼��Ԫ�����ڵ�Ԫ����
	 */
	private List<Report> all_reports;
	
	/**
	 * ��ǰģʽ�����м�������ĸ���
	 */
	public int errorNum = 0;
	
	/**
	 * ��ǰɨ����ļ���
	 */
	private String curFileName = "";
	
	/**
	 * ��ǰɨ����ʹ�õ����ݿ�
	 */
	private DBAccess db;
	
	public FSMControlFlowData() {
	}

	public void setParseFileName(String curFileName) {
		this.curFileName = curFileName;
	}
	
	public String getCurFileName() {
		return curFileName;
	}
	
	public void setDB(DBAccess db) {
		this.db = db;
	}
	public DBAccess getDB() {
		return db;
	}
	
	public void setReports(List<Report> reports) {
		this.reports = reports;
	}
	
	public void addReport(Report report) {
		if (reports == null) {
			return;
		}
		reports.add(report);
	}
	
	public List<Report> getReports() {
		return reports;
	}
	
	public void setAllReports(List<Report> reports) {
		this.all_reports = reports;
	}
	
	public void addAllReport(Report report) {
		if (all_reports == null) {
			return;
		}
		all_reports.add(report);
	}
	
	public List<Report> getAllReports() {
		return all_reports;
	}
	
	int srcFileLine = 0;
}
