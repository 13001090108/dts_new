package softtest.config.c;


public class Config
{
	/**���������з������أ�������������Ч�� */
	public static boolean Field=true;
	
	//dyk 20130624 ��������Ĳ���
	public static int VAR_LEVEL = 5;
	
	/**�����з����������С���� */
	public static int MaxArray=10;
	
	/**Ϊ����߻�Ϊ����Ĳ���Ч�ʣ����ֻ������Ϊ�Ĺ����࣬������������ʹ��������������Ĳ��� */
	
	public static boolean HW_RULES_CUT=false;

	/**��״̬����������ʱ,true��ʾÿ������һ���ڵ�,����ͼ��յ�ǰ�ڵ��״̬��ʵ�����ϣ�����߷���Ч�� */
	public static boolean FSM_REMOVE_PER_NODE=true;
	
	/**���ڼ��״̬������ʱ������״̬���Ĵ��������������Ĭ��Ϊfalse */
	public static boolean FSM_STATISTIC=false; 
	
	/**�Ƿ���в�ʶ���滻*/
	public static boolean FILEREPLACE = true;
	
	/**�Ƿ��Ԥ������.i�м��ļ����л�����ȥ���ظ���ͷ�ļ�չ�� */
	public static boolean InterFile_Simplified = false;

	/**����ļ��ں��������Ϣ���ļ�·�� */
		//public static String TRACEPATH="C:\\temp";
	public static String TRACEPATH="C:\\temp";
	/**����GCC��������(��ͬ�����������õ����м��ļ�·������һ�£� liuli��2010.8.12
	 * <p>GCC_TYPE=0 ��ʾ��������ΪMinGW</p>
	 * <p>GCC_TYPE=1 ��ʾ��������Ϊcygwin</p>
	 */
	public static  int GCC_TYPE=1;
	
	/** Ϊ��ʶ��ĳ�����Ƿ�Ϊ�⺯�������ϵͳͷ�ļ���·�������Ϊ��·�����Էֺŷֿ�*/
	public static String LIB_HEADER_PATH="/usr/include;/usr/local/include;";
	
	/** �Ƿ���������Ŀ�����ͼ��״̬��ͼ */
	public static boolean TRACE = false;
	
	public static boolean TRACECFG = false;
	
	/**�Ƿ�����ļ����ù�ϵͼ*/  //qian-2011-07-27
	public static boolean TRACE_FCG = false;
	
	/**�Ƿ��ǳ�ȫ�ֺ������ù�ϵͼ*/ //qian-2011-07-27
	public static boolean TRACE_ICG = false;
	
	/** �Ƿ��Զ��ر�progFrame���� */
	public static boolean AUTOCLOSE = false;

	/**0:·�������� 1����ͬ״̬�ϲ�2��������ϲ�*/
	public static int PATH_SENSITIVE=2;
	/** Ϊ����·�����з���ʱ·����ը������·����������*/
	public static int PATH_LIMIT = 10000000;
	
	/** ������ʱ���ƣ���λΪ���� */
	public static long TIMEOUT = 1200000;
	
	/**�����﷨��ʱ�ĳ�ʱ���� */
	public static long ASTTREE_TIMEOUT=1200000;

	/**��·�����з���ǰ�������жϵ�ǰ������ͼ�Ľڵ��������������˸����ޣ���Ϊ����߷���Ч�ʲ��ٽ���·�����з��� */
	public static int MAXVEXNODENUM=100;

	/**�����м��ļ�.i�е������жϽڵ���ܷǳ����ӣ���չ�������������﷨����αȽ϶࣬���������ʱ��������˸����
	 * ���ޣ����ٷ������������жϽڵ� */
	public static int MAXASTTREEDEPTH=50;
	
	/**Ԥ����֮���м��ļ��Ĵ��λ��*/
	public static String PRETREAT_DIR = "temp";
	
	/**zys:2010.5.17 ��ʱ���أ�����ѭ�������widening/narrowing�Ľ�*/
	public static boolean LOOPCAL=true;
	
	/**Ϊ��֤ѭ������������ֹ���趨һ������������ */
	public static final int LOOPNUM=5;
	
	/**Ϊ��֤ѭ�������Ŀ��ٴ����趨һ��ѭ������Ĳ�� */
	public static int LOOPLAYER=2;
	
	/**zys <p>����viewer���ߵ���ʱ��������������Ϊfalse���������к�ӳ��
	 * ������Ԥ����֮���.i�м��ļ�����ʱ��Ч��</p>*/
	public static boolean COMPUTE_LINE_NUM=true;
	
	/** zys	�Ƿ���ȫ�ֺ������÷���*/
	public static final boolean INTER_METHOD_TRACE = false;

	/** �������ɿ⺯����ժҪ��Ϣ*/
	public static final boolean USE_SUMMARY = true;
	
	//dyk �⺯��û���˹�ժҪ���򷵻�ֵΪȫ����
	public static final boolean LIB_NOSUMMARY_NPD=true;
	
	/**�Ƿ�ͨ�����кŽ���ע�� */
	public static final boolean REGISTER = false;
	
	/** �Ƿ�ʹ����Ȩ�ļ���ע�� */
	public static final boolean FILE_LICENSE = false;

	/**�Ƿ�ͨ�����������м���*/
	public static  final boolean LOCK = false;
	
	/** �Ƿ�Ϊ����� */
	public static final boolean NETWORK_LOCK = false;
	public static final Nlock_size NLOCK_SIZE = Nlock_size.S54;  //Ĭ��ΪS54
	
	/** �Ƿ�Ϊ�������� */        
	public static final boolean ISSERVER=false;

	/** �Ƿ���ʹ�ô������޶�*/
	public static final boolean PHASE_REGISTER = false;
	public static final int PHASE_NUMUBER = 10;
	
	public static final boolean ISTRIAL=false;
	/**���԰�ÿ��IP�������*/
	public static final double PERCENT=20;
	
	/**���԰�ÿ��IP��������Ŀ*/
	public static final int MAXIP=10;
	

	/** �Ƿ����±������Ķ����ļ� */
	public static boolean COMPILEFSM = false;

	/** �Ƿ���CԤ�����ļ�*/
	public static boolean DELETE_PRETREAT_FILES = false;
	
	/** ���ڼ���ĳ�������͵��ֽ�����sizeof��*/
	public static int INT_SIZE=4;
	
	/** ����union���ֽ���ʱ��Ĭ�ϵ�#pragma pack 1*/
	public static int PACK_SIZE=1;


	/**zys:�жϵ�ǰ�Ƿ��ǻع������������ģʽ�����true����Խ����д���ݿ�*/
	public static boolean REGRESS_RULE_TEST=false;

	public static String version= "DTSGCC7";
	
	/**������ͼ��ӡʱ���Ƿ��ӡ������Ϣ */
	public static boolean DUMP_DOMAIN=false;
	
	/**������ͼ��ӡʱ���Ƿ��ӡ����������Ϣ */
	public static boolean DUMP_SYMBOL=false;
	/**���ַ������� liuli��2010.6.25
	 * ANALYSIS_PΪ���ʾΪ���̷���
	 * ANALYSIS_IΪ���ʾΪ�м��ļ�����
	 * ANALYSIS_RΪ���ʾΪԴ�ļ�����*/
	public static boolean ANALYSIS_P=false;
	public static boolean ANALYSIS_I=false;
	public static boolean ANALYSIS_R=false;
	
	public final static int LANG_ENGLISH = 0;
	public final static int LANG_CHINESE = 1;

	/**xwt 2011.11.3   	�Ƿ������ȱ��ģʽ���ݿ�*/


	public static boolean TRIAL_OUTPUT_ALL = false;
	
	/**zys 2011.6.24	�ڶԷ�������Ĺ��ܽ��лع����ʱsofttest.test.c.symbolic.TestSymbolic.java�������ɱ������ */
	public static boolean TEST_SYMBOLIC = false;
	
	public static int DTS_LANGUAGE = LANG_CHINESE;
	//����unknow�Ĵ���Ϊtrueʱunknow�����κ�����ֵ��Ϊunknow
	public static boolean DOMAIN_CONSERVATIVE=true;
	public static boolean USEUNKNOWN = true;
	
	//stat���ݿ�����
	public static String DB_STAT_PASSWORD = "741852963";
	
	//���⹤�̲��õ��Ƿ���HPC������
	public static boolean isHPC=false;
	
	public static boolean SHOW_DIALOG = true;
	
	/*-----------��־log������Ϣ------------*/
	//log control begin
	
//	public static String LOG_FILE=".\\log\\c.log";
	public static String LOG_FILE=".\\log\\";
	public static String FILE;
	/** �Ƿ���log�м�¼ÿһ��������ĺ�ʱ*/
	public static boolean STEP_TIME_TRACE=false;
	
	//�Ƿ��������ʱ���쳣��Ϣ
	public static boolean PRINT_LOG_ERROR=true;
	
	///** true��ʾ��־�ļ�ɾ������������,������;false��ʾ��ԭ��־�����д*/
	public static boolean LOG_REPLACE=true;
	
	/**�Ƿ�����Ԥ������̣��������м��ļ�ʱ������������Ϊtrue����������Ԥ���������ֱ�ӽ��з�����ע�⣬����Դ�ļ�ʱ�������ر�������Ϊfalse*/
	public static boolean SKIP_PREANALYSIS=false;
	/**�Ƿ�鿴Ԥ���������ɳ����﷨�����أ�����������Ϊtrue�����Բ鿴Ԥ���������ɳ����﷨���Ĺ���*/
	public static boolean PreAnalysisASTRoot=false;
	/**�Ƿ�鿴Ԥ���������ɷ��ű��أ�����������Ϊtrue�����Բ鿴Ԥ���������ɷ��ű�Ĺ���*/
	public static boolean PreAnalysisSymbolTable=false;
	/**�Ƿ�鿴Ԥ������ȫ�ֺ������÷������أ�����������Ϊtrue�����Բ鿴Ԥ������ȫ�ֺ������÷����Ĺ���*/
	public static boolean PreAnalysisInterMethodVisitor=false;
	
	/**�Ƿ�鿴�ļ�����˳�򿪹أ�����������Ϊtrue�����Բ鿴�ļ��ķ���˳�򣬷���˳���ǰ�����ĸ˳��������߰�����������*/
	public static boolean FileAnalysisOrder=true;
	
	/**�Ƿ���������������־���̿��أ�����������Ϊtrue��������������������־���̣���1/2/3/4/5/6����������Ч������Ϊfalse�� ���Բ鿴���������������*/
	public static boolean SKIP_METHODANALYSIS=true;
	/**1 �Ƿ�鿴�������������ɳ����﷨�����أ�����������Ϊtrue�����Բ鿴���������ɳ����﷨���Ĺ���*/
	public static boolean MethodAnalysisASTRoot=false;
	/**2 �Ƿ�鿴�������������ɷ��ű��أ�����������Ϊtrue�����Բ鿴�������������ɷ��ű�Ĺ���*/
	public static boolean MethodAnalysisSymbolTable=false;
	/**3 �Ƿ�鿴����������ȫ�ֺ������÷������أ�����������Ϊtrue�����Բ鿴����������ȫ�ֺ������÷����Ĺ���*/		
	public static boolean MethodAnalysisInterMethodVisitor=false;
	/**4 �Ƿ�鿴���������п�����ͼ�������أ�����������Ϊtrue�����Բ鿴���������п�����ͼ�����Ĺ���*/
	public static boolean MethodAnalysisControlFlowVisitor=false;
	/**5 �Ƿ�鿴���������м��㶨��ʹ�����������أ�����������Ϊtrue�����Բ鿴���������ж���ʹ���������Ĺ���*/
	public static boolean MethodAnalysisDUAnalysisVisitor=false;
	/**6 �Ƿ�鿴��������������������أ�����������Ϊtrue�����Բ鿴������������������Ĺ���*/
	public static boolean MethodAnalysisDomainVisitor=false;
	
	/**�Ƿ�鿴ʵ�������׶ο��أ�����������Ϊtrue�����Բ鿴ʵ�������Ľ����־����*/
	public static boolean FSMInstanceAnalysis=false;
	
	
	/**�Ƿ�鿴���ص�״̬�����鿴��true ���鿴��false*/
	public static boolean LoadFSM = true;
	//��Щ������Ҫ����ͼ�λ��ƹ��ߣ�������Щ���ص���Ч��Ҫ��װ�ù��ߣ�Graphviz
	//Graphviz begin
	/**�Ƿ����ȫ�ֺ������ù�ϵ*/
	public static boolean GlobalFunctionCall = false;
	/**�Ƿ����ȫ���ļ�������ϵ*/
	public static boolean GlobalFileCallRelation = false;
	/**�Ƿ�����ļ��ں������ù�ϵ*/
	public static boolean CallGraph = false;
	/**�Ƿ����������ͼ(DU����ʹ����)*/
	public static boolean DU = false;
	/**�Ƿ����������ͼ(�������)*/
	public static boolean Domain = false;
	/**�Ƿ����״̬��ת������*/
	public static boolean StateTransition = false;
	
	//Graphviz end
	
	//log control end
	//added by cmershen,�Ƿ����
	public static boolean Cluster = false;
}
