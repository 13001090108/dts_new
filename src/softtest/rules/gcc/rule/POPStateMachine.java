package softtest.rules.gcc.rule;

import java.util.LinkedList;
import java.util.List;
import softtest.ast.c.ASTAbstractDeclarator;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTDeclarationSpecifiers;
import softtest.ast.c.ASTIdentifierList;
import softtest.ast.c.ASTParameterDeclaration;
import softtest.ast.c.ASTParameterList;
import softtest.ast.c.ASTParameterTypeList;
import softtest.ast.c.ASTStructOrUnionSpecifier;
import softtest.ast.c.ASTTypeSpecifier;
import softtest.ast.c.SimpleNode;
import softtest.config.c.Config;
import softtest.fsm.c.FSMMachine;
import softtest.fsm.c.FSMMachineInstance;
import softtest.fsm.c.FSMRelatedCalculation;
import softtest.rules.c.StateMachineUtils;

/**
 * @author nieminhui
 * @POP means Parameter of Procedure 
 * @1st:��ֹʹ���޲κ������޲κ����Ĳ���ʹ��fun(void)��ʽ
 * @2nd:��ֹ���̲���ֻ�����ͣ�û�б�ʶ��
 * @3th:���̺ͺ����Ĳ�������ʹ��������������Ҫֻ�б�ʶ��û����������
 * @4th:����������ò�Ҫ�ô�ʡ�Ժ�
 */
public class POPStateMachine {
	public static List<FSMMachineInstance> createPOPStateMachines(
			SimpleNode node, FSMMachine fsm) {
		List<FSMMachineInstance> list = new LinkedList<FSMMachineInstance>();
		//modified by nieminhui
		//2011.3.22
		//����ֻ�б�ʶ��û�����͵�����
		String xPath = "./Declarator/DirectDeclarator";
		xPath+="|./Declarator/DirectDeclarator/ParameterTypeList/ParameterList";
		xPath+="|./Declarator/DirectDeclarator/IdentifierList";
		xPath+="|./Declarator/DirectDeclarator/ParameterTypeList";
		List<SimpleNode> evaluationResults = StateMachineUtils.getEvaluationResults(node, xPath);
		for (SimpleNode snode : evaluationResults){
			FSMMachineInstance fsminstance = fsm.creatInstance();
			fsminstance.setRelatedASTNode(snode);
			fsminstance.setRelatedObject(new FSMRelatedCalculation(snode));
			list.add(fsminstance);
		}
		return list;
	}
	public static boolean checkParameter(List<SimpleNode> nodes, FSMMachineInstance fsmin){
		boolean result=false;
		SimpleNode simnode=fsmin.getRelatedASTNode();
		for(SimpleNode node:nodes){
			if(simnode!=node)
				continue;
			if(simnode instanceof ASTDirectDeclarator){
				result=checkNullPara(simnode,fsmin);
			}else if(simnode instanceof ASTParameterList){
				result=checkParaDec(simnode,fsmin);
			}else if(simnode instanceof ASTIdentifierList){
				result=checkIdentifier(simnode,fsmin);
			}else if(simnode instanceof ASTParameterTypeList){
				result=checkParaEllipsis(simnode,fsmin);
			}
		}
		return result;
	}
	public static boolean checkNullPara(SimpleNode simnode,FSMMachineInstance fsmin){
		boolean result=false;
		ASTDirectDeclarator dec=(ASTDirectDeclarator)simnode;
		List list1=dec.findDirectChildOfType(ASTParameterTypeList.class);
		List list2=dec.findDirectChildOfType(ASTIdentifierList.class);
		if(list1.size()==0 && list2.size()==0){
			result=true;
			String id=dec.getImage();
			addFSMDescriptionNullPara(id,fsmin);
		}
		return result;
	}
	//modified by nieminhui
	//2011.3.22
	//�޸��˶Բ���Ϊvoid�ĺ��������Ĵ���
	public static boolean checkParaDec(SimpleNode simnode,FSMMachineInstance fsmin){
		boolean result=false;
		ASTParameterList paralist=(ASTParameterList)simnode;
		ASTDirectDeclarator fun=(ASTDirectDeclarator)paralist.jjtGetParent().jjtGetParent();
		String funcname=fun.getImage();
		int len=paralist.jjtGetNumChildren();
		/*
		if(len==1){
			if(((SimpleNode)paralist.jjtGetChild(0)).getImage().equals("void")){
				return false;
			}
		}
		*/
		boolean type[]=new boolean[len];
		boolean id[]=new boolean[len];
		for(int i=0;i<len;++i){
			type[i]=true;
			id[i]=true;
		}
		for(int i=0;i<len;++i){
			ASTParameterDeclaration paradec=(ASTParameterDeclaration)paralist.jjtGetChild(i);
			if(paradec.jjtGetNumChildren() > 1){
				if(paradec.jjtGetChild(1) instanceof ASTAbstractDeclarator){
					ASTDeclarationSpecifiers dec_sp=(ASTDeclarationSpecifiers)paradec.jjtGetChild(0);
				    int size=dec_sp.jjtGetNumChildren();
				    boolean istype=false;
				    for(int j=0;j<size;j++){
				    	if(dec_sp.jjtGetChild(j) instanceof ASTTypeSpecifier || dec_sp.jjtGetChild(j) instanceof ASTStructOrUnionSpecifier){
						    id[i]=false;
						    istype=true;
						    break;
					    }
				    }
				    if(!istype){
				    	type[i]=false;
				    }
				}

			}else if(paradec.jjtGetNumChildren() == 1){
				if(((SimpleNode)paradec.jjtGetChild(0).jjtGetChild(0)).getImage().equals("void")){
					return false;
				}
				result = true;
				addFSMDescriptionParaDec(funcname,(i+1)+"",fsmin);
				return result;
			}
		}
		boolean temp[]=new boolean[len];
		String var="";
	   //��������ʱ��Ĳ�������
		for(int i=0;i<len;++i){
			temp[i]=type[i]&&id[i];
			if(temp[i]){
				continue;
			}else{
				var+=(i+1)+" ";
				result=true;
			}
		}
		if(result){
			addFSMDescriptionParaDec(funcname,var,fsmin);
		}

		return result;
	}
	//add by nieminhui
	//2011.3.22
	//���Ӷ�ֻ�б�ʶ��û�����͵Ľڵ�Ĵ���
	public static boolean checkIdentifier(SimpleNode simnode,FSMMachineInstance fsmin){
		ASTIdentifierList identifierlist = (ASTIdentifierList)simnode;
		String funcname=((ASTDirectDeclarator)identifierlist.jjtGetParent()).getImage();
		addFSMDescriptionIdentifier(funcname,fsmin);
		return true;
	}
	//modified by nieminhui
	//�޸��˶Ժ���������ʡ�Ժŵĺ��������Ĵ���
	//2011.3.22
	public static boolean checkParaEllipsis(SimpleNode simnode,FSMMachineInstance fsmin){
		boolean result=false;
		ASTParameterTypeList paralist=(ASTParameterTypeList)simnode;
		paralist.isVararg();
		String funname=((ASTDirectDeclarator)paralist.jjtGetParent()).getImage();
		if(paralist.isVararg())
		{
			result = true;
			addFSMDescriptionParaEllipsis(funname, fsmin);
		}
		return result;
	}
	private static void addFSMDescriptionNullPara(String variable, FSMMachineInstance fsminstance) {	
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("Procedure "+variable+" has no parameter.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("����"+variable+"û�в�����");
		}	
	}
	private static void addFSMDescriptionParaDec(String funid,String var, FSMMachineInstance fsminstance){
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("The "+var+" Parameter of Procedure Defination"+funid+" is not proper declaration.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("��������"+funid+"�ĵ�"+var+"�����������Ĳ�ǡ��");
		}	
	}
	//add by nieminhui
	//2011.3.22
	private static void addFSMDescriptionIdentifier(String funid,FSMMachineInstance fsminstance){
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("The Procedure Defination"+funid+" have paramenter, but have not any proper declaration.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("��������"+funid+"ֻ�б�ʶ��û����������");
		}	
	}
	private static void addFSMDescriptionParaEllipsis(String funname, FSMMachineInstance fsminstance){
		if (Config.DTS_LANGUAGE == Config.LANG_ENGLISH) {
			fsminstance.setDesp("The Parameters of Procedure Definination"+funname+" have ellipsis.");
		} else if (Config.DTS_LANGUAGE == Config.LANG_CHINESE){
			fsminstance.setDesp("��������"+funname+"�Ĳ�����ʡ�Ժ�");
		}	
	}
}
