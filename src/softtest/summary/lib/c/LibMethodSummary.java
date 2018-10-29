package softtest.summary.lib.c;

import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import softtest.ast.c.*;
import softtest.domain.c.interval.Domain;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.interpro.c.Method;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.ScopeAndDeclarationFinder;
import softtest.symboltable.c.SourceFileScope;

/**
 * �⺯������ժҪ��Ϣ
 * 
 * @author qipeng
 *
 */
public class LibMethodSummary {
	
	/**
	 * <p>�⺯�����ڵ�C-C++�еĿ�����</p>
	 */
	private String libName;
	
	/**
	 * <p>�⺯����</p>
	 */
	private String methodName;
	
	/**
	 * <p>�⺯���ĺ���ǩ�����������C++�﷨�ĺ���ǩ���������������ɺ���ժҪ</p>
	 */
	private String signature;
	
	/**
	 * <p>�⺯���ķ���ֵ��</p>
	 */
	private Domain retDomain;
	
	/**
	 * <p>��ǰ�⺯�������еĺ���������Ϣ</p>
	 */
	private Set<LibMethodDesp> features;
	
	/**
	 * �⺯���Ƿ������ڴ�ռ�
	 */
	private boolean isAllocate;
	
	public LibMethodSummary(String libName, String methodName, String signature) {
		this.libName = libName.toLowerCase();
		this.methodName = methodName;
		this.signature = signature;
		features = new HashSet<LibMethodDesp>();
		LibManager libManager = LibManager.getInstance();
		libManager.addLib(this.libName, this);
	}

	/**
	 * ��Ӻ���������Ϣ
	 * 
	 */
	public void addFeature(LibMethodDesp feature) {
		features.add(feature);
	}
	
	/**
	 * ���ÿ⺯������ֵ����
	 * @param retDomain
	 */
	public void setRetDomain(Domain retDomain) {
		this.retDomain = retDomain;
	}
	
	public Domain getRetDomain() {
		return this.retDomain;
	}

	/**
	 * ���ÿ⺯���Ƿ�����ڴ�ռ�
	 * @param isAllocate
	 */
	public void setIsAllocate(boolean isAllocate){
		this.isAllocate = isAllocate;
	}
	/**
	 * ���ݺ���ǩ�����������ɺ����������ٸ��ݺ���������Ϣ�����ɺ���ժҪ
	 * ���ú�����Ӧ������XMLժҪ����ȫ��ת���ɺ���ժҪ
	 * @return
	 */
	public MethodNameDeclaration compileSummary() {
		//��һ�������ɳ����﷨��AST
		CParser parser = CParser.getParser(new CCharStream(new StringReader(this.signature)));
		ASTTranslationUnit root=null;
		try {
			root = parser.TranslationUnit();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		ScopeAndDeclarationFinder sc = new ScopeAndDeclarationFinder();
		root.jjtAccept(sc, null);
		SourceFileScope scope = (SourceFileScope)root.getScope();
		Map<MethodNameDeclaration, ArrayList<NameOccurrence>> methods = scope.getMethodDeclarations();
		MethodNameDeclaration methodDecl = null;
		if (methods.size() != 0) {
			methodDecl = methods.keySet().iterator().next();
		} 
		if (methodDecl != null) {
			methodDecl.setLibFunction(true);
			methodDecl.setNode(null);
			if (methodDecl.getScope() instanceof SourceFileScope) {
				methodDecl.setScope(null);
			}
			Method method = InterMethodVisitor.getMethod(methodDecl);
			methodDecl.setMethod(method);
			if(retDomain!=null)
				method.setReturnValue(retDomain);
			method.setIsAllocate(isAllocate);
			MethodSummary mtSummary = new MethodSummary();
			for (LibMethodDesp feature: features) {
				feature.createFeature(libName, root, mtSummary);
			}
			method.setMtSummmary(mtSummary);
			return methodDecl;
		}
		return null;
	}
	
	public String toString() {
		String str = this.signature + "\n";
		for (LibMethodDesp feature : features) {
			str += feature.type + "[" + feature.value + "]";
		}
		str += "\n";
		if (retDomain != null) {
			str += retDomain.toString();
		}else if(isAllocate){
			str += new Boolean(isAllocate).toString();
		}
		return str;
	}
}
