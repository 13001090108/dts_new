package softtest.cfg.c;

import java.io.Serializable;

/** VexNode Edge Graph�ĳ������ */
public abstract class Element implements Serializable {
	/** ������ģʽ��accept���� */
	public abstract void accept(GraphVisitor visitor, Object data);
}
