package softtest.summary.gcc.fault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTAssignmentOperator;
import softtest.ast.c.ASTCompoundStatement;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTInitDeclarator;
import softtest.ast.c.ASTParameterList;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.ConditionData;
import softtest.domain.c.interval.Domain;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.ScopeType;
import softtest.interpro.c.Variable;
import softtest.rules.c.StateMachineUtils;
import softtest.summary.c.MMFetureType;
import softtest.summary.c.MethodFeatureVisitor;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.ClassScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.MethodScope;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;

//lrt ����calloc���������ͷŵĴ���
public class MethodMMFeatureVisitor extends CParserVisitorAdapter implements
		MethodFeatureVisitor {
	class MMInfo {
		public MMInfo(String traceinfo, MMFetureType mmtype, int startline) {
			this.traceinfo = traceinfo;
			this.mmtype = mmtype;
			this.startline = startline;
		}

		String traceinfo;

		MMFetureType mmtype;

		int startline;
	}

	private final static String MM_FUNC_XPATH = ".//PostfixExpression/PrimaryExpression[@Method='true']";

	private static MethodMMFeatureVisitor instance;

	private MethodMMFeatureVisitor() {
	}

	public static MethodMMFeatureVisitor getInstance() {
		if (instance == null) {
			instance = new MethodMMFeatureVisitor();
		}
		return instance;
	}

	private MethodNameDeclaration methodDecl = null;

	public void visit(VexNode vexNode) {

		MethodMMFeature mlfFeature = new MethodMMFeature();
		SimpleNode treeNode = vexNode.getTreenode();
		if (treeNode == null)
			return;

		methodDecl = InterContext.getMethodDecl(vexNode);
		treeNode.jjtAccept(instance, mlfFeature);

		// ��������ĺ���������ӵ�����ժҪ��

		MethodSummary summary = InterContext.getMethodSummary(vexNode);
		if (summary != null && !mlfFeature.isEmpty()) {
			// ��ִ�����if��� ��Ӻ���������
			summary.addSideEffect(mlfFeature);
			if (Config.INTER_METHOD_TRACE) {
				if (methodDecl != null) {
					System.err.println(methodDecl.getFullName() + " "
							+ mlfFeature);
				}
			}
		}
	}

	public static VariableNameDeclaration findArgDeclInQual(
			SimpleNode methodNode) {
		VariableNameDeclaration result = null;

		ASTArgumentExpressionList argl = (ASTArgumentExpressionList) methodNode
				.getNextSibling();
		if (argl != null) {
			ASTAssignmentExpression anode = (ASTAssignmentExpression) argl
					.jjtGetChild(0);
			result = anode.getVariableNameDeclaration();
		}

		return result;

	}

	/**
	 * ���Ҹ�ֵ���ʽ��߽ڵ��ж�Ӧ�ı�������
	 * 
	 * @param methodNode
	 * @return
	 */

	public static VariableNameDeclaration findAssginDeclInQual(
			SimpleNode methodNode) {
		VariableNameDeclaration result = null;

		SimpleNode assignExp = findAssignExp(methodNode);
		if (assignExp instanceof ASTInitDeclarator) {
			ASTDirectDeclarator dnode = (ASTDirectDeclarator) assignExp
					.getFirstChildOfType(ASTDirectDeclarator.class);
			result = dnode.getVariableNameDeclaration();
		} else if (assignExp instanceof ASTAssignmentExpression) {
			SimpleNode snode = (SimpleNode) assignExp.jjtGetChild(0);
			// ASTPrimaryExpression pnode = (ASTPrimaryExpression) snode
			// .getFirstChildOfType(ASTPrimaryExpression.class);
			if (snode instanceof ASTUnaryExpression) {

				result = (VariableNameDeclaration) ((ASTUnaryExpression) snode)
						.getVariableDecl();
			}
		}
		return result;

	}

	public static VariableNameDeclaration findAssginDeclInQualWithPointer(
			SimpleNode methodNode) {
		return findAssginDeclInQual(methodNode);

	}

	/** add by xwt:ȫ�ֱ���ָ���ڴ����Ϊ���ͷ� */
	private boolean findGlobalVar(SimpleNode node,
			HashMap<VariableNameDeclaration, MMInfo> frees) {
		String xpath = ".//Expression/AssignmentExpression[count(*)=3 and ./AssignmentOperator";
		List<SimpleNode> result = StateMachineUtils.getEvaluationResults(node,
				xpath);
		if (result.size() == 0) {
			return false;
		}
		for (SimpleNode varNode : result) {
			if (!(varNode.jjtGetChild(0) instanceof ASTUnaryExpression))
				continue;
			if (!(varNode.jjtGetChild(2) instanceof ASTAssignmentExpression))
				continue;
			ASTUnaryExpression p = (ASTUnaryExpression) varNode.jjtGetChild(0);
			VariableNameDeclaration leftvar = p.getVariableDecl();
			if (leftvar == null)
				continue;
			if (leftvar != null
					&& !(leftvar.getScope() instanceof SourceFileScope))
				continue;
			if (varNode.jjtGetChild(2) instanceof ASTAssignmentExpression) {
				ASTAssignmentExpression a = (ASTAssignmentExpression) varNode
						.jjtGetChild(2);
				ASTUnaryExpression un = (ASTUnaryExpression) a
						.getFirstChildOfType(ASTUnaryExpression.class);
				if (un == null)
					continue;
				VariableNameDeclaration rightvar = un.getVariableDecl();
				if (rightvar != null) {
					frees.put(rightvar, new MMInfo(null,
							MMFetureType.GLOABAL_FREE, un.getBeginLine()));
					return true;
				}
			}
		}
		return false;
	}

	private boolean findMM_Func(SimpleNode node,
			HashMap<VariableNameDeclaration, MMInfo> frees,
			HashMap<VariableNameDeclaration, MMInfo> mallocs) {
		List<SimpleNode> result = StateMachineUtils.getEvaluationResults(node,
				MM_FUNC_XPATH);
		if (result.size() == 0) {
			return false;
		}

		for (SimpleNode methodNode : result) {

			MethodNameDeclaration decl = getMethodNameDecalaration(methodNode);
			VariableNameDeclaration varDecl = null;
			if (decl == null) {
				continue;
			}
			// ���ȿ���malloc��free
			if (decl.getImage().equals("malloc")
					&& decl.getParameterCount() == 1) {
				varDecl = findAssginDeclInQualWithPointer(methodNode);
				if (varDecl != null) {
					mallocs.put(varDecl, new MMInfo(null, MMFetureType.MALLOC,
							methodNode.getBeginLine()));
				}
			} else if (decl.getImage().equals("free")
					&& decl.getParameterCount() == 1) {
				varDecl = findArgDeclInQual(methodNode);
				if (varDecl != null) {

					if (methodNode.getCurrentVexNode() == null)
						continue;
					VexNode free = methodNode.getCurrentVexNode();
					if (free.getContradict())
						continue;
					SimpleNode selectSta = (SimpleNode) methodNode
							.getFirstParentOfType(ASTSelectionStatement.class); // ��
					if (selectSta == null) {
						frees.put(varDecl, new MMInfo(null, MMFetureType.FREE,
								methodNode.getBeginLine()));
					} else {
						if (findOtherNotFreeBranch(selectSta, varDecl,
								methodNode)) {
							// ����if(p)free p��
							// selectSta = (SimpleNode)
							// selectSta.jjtGetChild(0).jjtGetChild(0);
							ConditionData condition = selectSta
									.getCurrentVexNode().getCondata();

							if (condition == null)
								continue;
							if (condition.getDomainsTable() == null)
								continue;
							if (condition.getDomainsTable().size() != 1)
								continue;

							// Enumeration<VariableNameDeclaration> a =
							// condition.getDomainsTable().keys();
							SimpleNode condNode = condition.getCurrentVex()
									.getTreenode();
							List list = condNode
									.findChildrenOfType(ASTUnaryExpression.class);
							if (list == null || list.size() == 0) {
								continue;
							}
							ASTUnaryExpression pe = (ASTUnaryExpression) list
									.get(0);
							VariableNameDeclaration var = pe.getVariableDecl();
							if (var != null) {
								if (!var.equals(varDecl))
									continue;
								frees.put(varDecl,
										new MMInfo(null, MMFetureType.FREE,
												methodNode.getBeginLine()));
							}
						}
					}
				}
			} else if (decl.getImage().equals("calloc")// added by
														// lrt(��calloc������ʶ��)
					&& decl.getParameterCount() == 2) {
				varDecl = findAssginDeclInQualWithPointer(methodNode);
				if (varDecl != null) {
					mallocs.put(varDecl, new MMInfo(null, MMFetureType.CALLOC,
							methodNode.getBeginLine()));
				}
			} else {
				if (decl.getMethodSummary() != null) {
					MethodMMFeature mmFeture = (MethodMMFeature) decl
							.getMethodSummary().findMethodFeature(
									MethodMMFeature.class);
					if (mmFeture == null) {
						continue;
					}
					// ����Ƿ�ͨ������ֵ���������ڴ�ָ��
					if (mmFeture.isAllocateAndReturn()) {
						varDecl = findAssginDeclInQualWithPointer(methodNode);
						if (varDecl != null) {
							mallocs.put(
									varDecl,
									new MMInfo(mmFeture.getRetTrace(), mmFeture
											.getMMRetrunType(), methodNode
											.getBeginLine()));
						}
					}
					// ����Ƿ�ͨ����������MM��ر���
					HashMap<Variable, MMFetureType> mmFetures = mmFeture
							.getMMFetures();
					for (Variable variable : mmFetures.keySet()) {
						if (variable.isParam()) {
							Node n = methodNode.getNextSibling();
							if (n != null
									&& n instanceof ASTArgumentExpressionList) {
								varDecl = MethodMMFeatureVisitor.getArgDecl(
										(ASTArgumentExpressionList) n,
										variable.getParamIndex());
							}
							if (varDecl != null) {
								MMFetureType type = mmFetures.get(variable);
								if (type == MMFetureType.MALLOC) {
									mallocs.put(
											varDecl,
											new MMInfo(mmFeture
													.getDesp(variable), type,
													methodNode.getBeginLine()));
								}
								if (type == MMFetureType.FREE) {
									frees.put(
											varDecl,
											new MMInfo(mmFeture
													.getDesp(variable), type,
													methodNode.getBeginLine()));
								}
							}
						} else if (variable.getScopeType() == ScopeType.INTER_SCOPE) {
							SourceFileScope sfScope = findSourceFileScope(methodNode
									.getScope());
							VariableNameDeclaration varDecl1 = (VariableNameDeclaration) Search
									.searchInVariableAndMethodUpward(
											variable.getName(), sfScope);
							if (varDecl1 != null) {
								MMFetureType type = mmFetures.get(variable);
								if (type == MMFetureType.MALLOC) {
									mallocs.put(
											varDecl1,
											new MMInfo(mmFeture
													.getDesp(variable), type,
													methodNode.getBeginLine()));
								} else if (type == MMFetureType.FREE) {
									frees.put(
											varDecl1,
											new MMInfo(mmFeture
													.getDesp(variable), type,
													methodNode.getBeginLine()));
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	private static SourceFileScope findSourceFileScope(Scope scope) {
		Scope parent = scope;
		while (parent != null && !(parent instanceof SourceFileScope)) {
			parent = parent.getParent();
		}
		return (SourceFileScope) parent;
	}

	/**
	 * ���Һ���������Ƿ��й����ڴ�������ͷŵ���䣬��¼��֮��صı���
	 */
	@Override
	public Object visit(ASTCompoundStatement node, Object feature) {
		MethodMMFeature mlffeature = null;
		if (feature instanceof MethodMMFeature) {
			mlffeature = (MethodMMFeature) feature;
		}

		HashMap<VariableNameDeclaration, MMInfo> mallocs = new HashMap<VariableNameDeclaration, MMInfo>();
		HashMap<VariableNameDeclaration, MMInfo> frees = new HashMap<VariableNameDeclaration, MMInfo>();

		boolean find = findMM_Func(node, frees, mallocs);
		boolean find1 = findGlobalVar(node, frees);

		if (!find && !find1) {
			return null;
		}

		String xpath = ".//JumpStatement[@Image='return']/Expression";
		List<SimpleNode> returnExps = StateMachineUtils.getEvaluationResults(
				node, xpath);
		VariableNameDeclaration re = null;
		for (SimpleNode rExp : returnExps) {
			List<SimpleNode> mallocs1 = StateMachineUtils.getEvaluationResults(
					rExp, MM_FUNC_XPATH);
			List temp = rExp.findChildrenOfType(ASTUnaryExpression.class);
			for (Iterator iter = temp.iterator(); iter.hasNext();) {
				ASTUnaryExpression pnode = (ASTUnaryExpression) iter.next();
				re = pnode.getVariableDecl();
				if (re != null)
					break;
			}

			if (mallocs1 != null
					&& mallocs1.size() != 0
					&& (mallocs1.get(0).getImage().equals("malloc") || mallocs1
							.get(0).getImage().equals("strdup"))) {
				mlffeature.setAllocateAndReturn(true, MMFetureType.MALLOC);
			}
			if (re != null && mallocs.containsKey(re)) {
				mlffeature.setAllocateAndReturn(true, mallocs.get(re).mmtype);
				String trace = "����" + methodDecl.getImage() + "�ڵ�"
						+ rExp.getBeginLine() + "�з��ض�̬�����ڴ�";
				if (mallocs.get(re).traceinfo != null) {
					trace += ", " + mallocs.get(re).traceinfo;
				}
				mlffeature.setRetTrace(trace);
			}

			List<SimpleNode> result = StateMachineUtils.getEvaluationResults(
					rExp, MM_FUNC_XPATH); // return malloc or mm����
			for (SimpleNode methodNode : result) {
				MethodNameDeclaration decl = getMethodNameDecalaration(methodNode);
				if (decl == null) {
					continue;
				}
				if (decl.getMethodSummary() != null) {
					MethodMMFeature mmFeture = (MethodMMFeature) decl
							.getMethodSummary().findMethodFeature(
									MethodMMFeature.class);
					if (mmFeture == null) {
						if ((decl.getImage().equals("malloc") || decl
								.getImage().equals("malloc"))
								&& decl.getParameterCount() == 1) {
							mlffeature.setAllocateAndReturn(true,
									MMFetureType.MALLOC);
							mlffeature.setRetTrace("����" + methodDecl.getImage()
									+ "�ڵ�" + methodNode.getBeginLine()
									+ "�з��ض�̬�����ڴ�");

						}
						continue;
					}
					// ����Ƿ�ͨ������ֵ���������ڴ�ָ��
					if (mmFeture.isAllocateAndReturn()) {
						mlffeature.setAllocateAndReturn(true,
								mmFeture.getMMRetrunType());
						mlffeature.setRetTrace("����" + methodDecl.getImage()
								+ "�ڵ�" + methodNode.getBeginLine()
								+ "�з��ض�̬�����ڴ�");
					}
				}
			}
		}

		// ���������ӵ����������õ�ժҪ��
		for (VariableNameDeclaration varDecl : frees.keySet()) {
			Variable variable = Variable.getVariable(varDecl);
			if (variable != null) {
				String trace = null;
				mlffeature.addMMVariable(variable, frees.get(varDecl).mmtype);
				if (frees.get(varDecl).mmtype == MMFetureType.GLOABAL_FREE) {
					trace = "Goalbal pointer to dynamic memory on line"
							+ frees.get(varDecl).startline + " in function \""
							+ methodDecl.getImage() + "\"";
					if (Config.DTS_LANGUAGE == Config.LANG_CHINESE) {
						trace = "����" + methodDecl.getImage() + "�ڵ�"
								+ frees.get(varDecl).startline + "����ȫ�ֱ���ָ��̬�ڴ�";
					}
				} else {
					trace = "����" + methodDecl.getImage() + "�ڵ�"
							+ frees.get(varDecl).startline + "�з����ͷŶ�̬�ڴ�";
					if (frees.get(varDecl).traceinfo != null) {
						trace += ", " + frees.get(varDecl).traceinfo;
					}
				}
				mlffeature.addDesp(variable, trace);
			}
		}

		// ���������Ͳ����Ƿ��ں����з����˿ռ�

		// 1 ��������ʼ��������Ϣ�������� paramTable��
		SimpleNode methodNode = methodDecl.getMethodNameDeclaratorNode();
		String paramPath = ".//ParameterList/ParameterDeclaration[./Declarator/Pointer]";
		List<SimpleNode> evaluationResults = StateMachineUtils
				.getEvaluationResults(methodNode, paramPath);

		HashMap<VariableNameDeclaration, Domain> paramMap = new HashMap<VariableNameDeclaration, Domain>();
		for (SimpleNode snode : evaluationResults) {
			VariableNameDeclaration paramDecl = snode
					.getVariableNameDeclaration();
			if (paramDecl != null) {
				if (paramDecl.getType().isPointType()) {
					paramMap.put(paramDecl, paramDecl.getDomain());

					// 2 �ҵ�����return��䴦�Ĳ���������Ϣ
					for (VariableNameDeclaration varDecl : mallocs.keySet()) {

						SimpleNode simpleNode = varDecl.getNode();
						try {
							if (simpleNode != null) {

								VexNode vexNode = simpleNode
										.getCurrentVexNode();
								if (vexNode != null) {
									Graph graph = vexNode.getGraph();

									Edge returnEdge;
									VexNode returnVexNode;
									if (graph != null) {
										VexNode lastVexNode = graph
												.getExitNode();
										Hashtable<String, Edge> inedges = lastVexNode
												.getInedges();
										if (!inedges.isEmpty()) {
											Collection<Edge> edgeValue = inedges
													.values();
											Iterator<Edge> iter = edgeValue
													.iterator();
											while (iter.hasNext()) {
												returnEdge = iter.next();
												returnVexNode = returnEdge
														.getTailNode();

												Hashtable<VariableNameDeclaration, Domain> table = returnVexNode
														.getVarDomainSet()
														.getTable();
												for (VariableNameDeclaration var : table
														.keySet()) {
													if (var.getScope() instanceof MethodScope
															&& (var.getParamIndex() == -1)
															&& var.getType()
																	.isPointType()) {
														Domain d = table
																.get(var);
														// ��������Ͳ����ڵ��ڸս��뺯��ʱ������ϢΪ�գ�����return��������Ϣ��Ϊ�գ�����Ϊ�������˿ռ����
														if (d != null
																&& !d.isUnknown()
																&& !Domain
																		.isEmpty(d)) {

															VariableNameDeclaration paramVar = var
																	.getNode()
																	.getVariableNameDeclaration();
															if (paramVar != null) {
																Variable v = Variable
																		.getVariable(paramVar);
																if (v != null
																		&& paramMap
																				.get(paramVar) == null) {
																	mlffeature
																			.addMMVariable(
																					v,
																					mallocs.get(varDecl).mmtype);
																	String trace = "����"
																			+ methodDecl
																					.getImage()
																			+ "�ڵ�"
																			+ mallocs
																					.get(varDecl).startline
																			+ "��������䶯̬�ڴ�";

																	if (mallocs
																			.get(varDecl).traceinfo != null) {
																		trace += ", "
																				+ mallocs
																						.get(varDecl).traceinfo;
																	}
																	mlffeature
																			.addDesp(
																					v,
																					trace);
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				}
			}
		}

		for (VariableNameDeclaration varDecl : mallocs.keySet()) {
			if (!isInterVar(varDecl)) {
				continue;
			}
			Variable variable = Variable.getVariable(varDecl);
			if (variable != null) {
				mlffeature.addMMVariable(variable, mallocs.get(varDecl).mmtype);
				String trace = "����" + methodDecl.getImage() + "�ڵ�"
						+ mallocs.get(varDecl).startline + "��������䶯̬�ڴ�";

				if (mallocs.get(varDecl).traceinfo != null) {
					trace += ", " + mallocs.get(varDecl).traceinfo;
				}
				mlffeature.addDesp(variable, trace);
			}
		}

		// {
		// VexNode vNode = varDecl.getNode().getCurrentVexNode();
		// Hashtable<VariableNameDeclaration, Domain>
		// table=vNode.getVarDomainSet().getTable();
		// for(VariableNameDeclaration var:table.keySet())
		// {
		// if(var.getScope() instanceof SourceFileScope)
		// {
		// Variable variable = Variable.getVariable(var);
		// if (variable != null) {
		// Domain d=table.get(var);
		// //zys 2011.6.24 ֻ�Է���ȷ���Ըı��ȫ�ֱ������ɺ���ժҪ
		// if(d!=null && !d.isUnknown() && !Domain.isEmpty(d))
		// mlffeature.addMMVariable(variable, mallocs.get(varDecl).mmtype);
		// String trace = "����" + methodDecl.getImage() + "�ڵ�"
		// + mallocs.get(varDecl).startline + "��������䶯̬�ڴ�";
		//
		// if (mallocs.get(varDecl).traceinfo != null)
		// {
		// trace += ", " + mallocs.get(varDecl).traceinfo;
		// }
		// mlffeature.addDesp(variable, trace);
		// }
		// }
		// else if(var.getScope() instanceof MethodScope && isInterVar(varDecl))
		// {
		// Variable variable = Variable.getVariable(var);
		// if (variable != null) {
		// Domain d=table.get(var);
		// if(d!=null && !d.isUnknown() && !Domain.isEmpty(d))
		// mlffeature.addMMVariable(variable, mallocs.get(varDecl).mmtype);
		// String trace = "����" + methodDecl.getImage() + "�ڵ�"
		// + mallocs.get(varDecl).startline + "��������䶯̬�ڴ�";
		//
		// if (mallocs.get(varDecl).traceinfo != null)
		// {
		// trace += ", " + mallocs.get(varDecl).traceinfo;
		// }
		// mlffeature.addDesp(variable, trace);
		// }
		// }
		// }
		//
		// if (!isInterVar(varDecl))
		// {
		// continue;
		// }
		//
		// if( (varDecl.getScope() instanceof MethodScope) &&
		// varDecl.getParamIndex()<0 )
		// {
		// if (varDecl != null) {
		// Variable variable =
		// Variable.getVariable(varDecl.getNode().getVariableNameDeclaration());
		// if (variable != null)
		// {
		// mlffeature.addMMVariable(variable, mallocs.get(varDecl).mmtype);
		// String trace = "����" + methodDecl.getImage() + "�ڵ�"
		// + mallocs.get(varDecl).startline + "��������䶯̬�ڴ�";
		//
		// if (mallocs.get(varDecl).traceinfo != null)
		// {
		// trace += ", " + mallocs.get(varDecl).traceinfo;
		// }
		// mlffeature.addDesp(variable, trace);
		// }
		//
		// }
		// }
		// }}
		return null;

	}

	/**
	 * ���Һ�������Ӧ�ĳ����﷨��ڵ��ϣ���Ӧ�Ĳ����ڵ�
	 * 
	 * @param node
	 * @param index
	 * @return
	 */

	public static VariableNameDeclaration getArgDecl(
			ASTArgumentExpressionList node, int index) {

		if (node == null) {
			return null;
		}
		if (index > node.jjtGetNumChildren()) {
			return null;
		}
		if (node.jjtGetChild(index) instanceof ASTAssignmentExpression) {
			ASTAssignmentExpression aNode = (ASTAssignmentExpression) node
					.jjtGetChild(index);

			ASTPrimaryExpression pNode = (ASTPrimaryExpression) aNode
					.getFirstChildOfType(ASTPrimaryExpression.class);

			if (pNode != null) {

				return pNode.getVariableDecl();
			}
			// return pd.getVariableNameDeclaration();
		}
		return null;
	}

	private static SimpleNode findAssignExp(SimpleNode node) {
		SimpleNode parent = node;
		while (!(parent instanceof ASTStatement)) {
			if (parent instanceof ASTAssignmentExpression) {

				if (parent.jjtGetNumChildren() == 3) {
					if (parent.jjtGetChild(1) instanceof ASTAssignmentOperator) {
						ASTAssignmentOperator aao = (ASTAssignmentOperator) parent
								.jjtGetChild(1);
						if (aao.getOperators().equals("=")) {
							return parent;
						}
					}
				}
			} else if (parent instanceof ASTInitDeclarator
					&& parent.jjtGetNumChildren() == 2) {
				return parent;
			} else if (parent == null
					|| parent instanceof ASTArgumentExpressionList) {
				return null;
			}
			parent = (SimpleNode) parent.jjtGetParent();
		}
		return null;
	}

	private MethodNameDeclaration getMethodNameDecalaration(SimpleNode node) {
		MethodNameDeclaration decl = null;
		if (node instanceof ASTPrimaryExpression) {
			decl = ((ASTPrimaryExpression) node).getMethodDecl();
		}
		return decl;
	}

	private boolean isInterVar(VariableNameDeclaration vnd) {
		// �ж��Ƿ��ǲ������Ա������ȫ�ֱ���
		if (vnd.getScope() == null) {
			return true;
		}
		if (vnd.isParam() || vnd.getScope() instanceof ClassScope
				|| vnd.getScope() instanceof SourceFileScope) {
			return true;
		}
		return false;
		// // �ж��Ƿ��ǲ������Ա������ȫ�ֱ���
		// if (vnd.getScope() == null)
		// {
		// return true;
		// }
		//
		// if (vnd.isParam() ||
		// vnd.getNode().getVariableNameDeclaration().isParam() ||vnd.getScope()
		// instanceof ClassScope
		// || vnd.getScope() instanceof SourceFileScope)
		// {
		// return true;
		// }
		// return false;
		// }
	}

	boolean findOtherNotFreeBranch(SimpleNode selectSta,
			VariableNameDeclaration var, SimpleNode methodNode) {
		if (selectSta == null || selectSta.getVexlist() == null
				|| selectSta.getVexlist().size() < 2)
			return false;
		// VexNode selectNode = selectSta.getFirstVexNode();
		VexNode selectNode = selectSta.getVexlist().get(1);
		if (selectNode == null)
			return false;
		Enumeration<Edge> edges = selectNode.getOutedges().elements();
		while (edges.hasMoreElements()) {
			Edge outEdge = edges.nextElement();
			VexNode firstVexNode = outEdge.getHeadNode();
			// �з�֧ʡ��
			if (firstVexNode.isBackNode()) {
				if (!outEdge.getContradict())// ʡ�Է�֧�ǲ��ɴ�·���������������δ��ȫ�ͷ�
					return true;
				else
					continue;// ʡ�Է�֧�ǲ��ɴ�·���������ж�����·��
			}
			SimpleNode firstNode = firstVexNode.getTreenode();
			if (methodNode.isSelOrAncestor(firstNode)) // ��
				continue;
			List<SimpleNode> freeList = StateMachineUtils.getEvaluationResults(
					firstNode, MM_FUNC_XPATH);
			if (freeList == null)
				return true;

			for (SimpleNode mNode : freeList) {
				MethodNameDeclaration decl = getMethodNameDecalaration(mNode);
				VariableNameDeclaration varDecl = null;
				if (decl == null) {
					continue;
				}
				if (decl.getImage().equals("free")
						&& mNode instanceof ASTPrimaryExpression) {
					varDecl = findArgDeclInQual(mNode);
					if (varDecl != null) {
						if (mNode.getCurrentVexNode() == null)
							continue;
						VexNode free = mNode.getCurrentVexNode();
						if (free.getContradict())
							continue;
						if (varDecl.equals(var))
							return false;
					}
				}
			}
			return true;
		}
		return true;
	}
}
