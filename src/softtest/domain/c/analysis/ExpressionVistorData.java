package softtest.domain.c.analysis;

import java.util.ArrayList;
import java.util.List;

import softtest.cfg.c.VexNode;
import softtest.domain.c.symbolic.Expression;

public class ExpressionVistorData {
    public Expression value;
    
    /** �����Ƿ���������õı�־ */
    public boolean sideeffect=true;
    
    public VexNode currentvex=null;
       
    public List<Integer> arrayIndex=new ArrayList<Integer>();   
}
