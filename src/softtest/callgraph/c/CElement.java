package softtest.callgraph.c;

import java.io.Serializable;

/** CVexNode CEdge CGraph�ĳ������ */
public abstract class CElement implements Serializable{
	/** ������ģʽ��accept���� */
	public abstract void accept(CGraphVisitor visitor, Object data);
}
