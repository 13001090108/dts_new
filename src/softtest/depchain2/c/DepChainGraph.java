package softtest.depchain2.c;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;



public class DepChainGraph {
	/** ��㼯�� */
	public Map<String, DepGraphNode> nodes = new HashMap<String, DepGraphNode>();

	/** �߼��� */
	public Map<String, DepGraphEdge> edges = new HashMap<String, DepGraphEdge>();
}
