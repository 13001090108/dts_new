package softtest.interpro.c;

import java.util.ArrayList;
import java.util.Stack;


// add by ALUO
// ��ȡ��������ͼ����������֮�������·��
public class GetAllICGPath {
	InterCallGraph interCallGraph;  // ��������ͼ
	boolean hasPath = true;  // ����֮���Ƿ���ͨ·
	int n;  // �ڵ�����
	int start, end;  // ��ʼ������ �ڵ���
	Stack<Integer> theStack;  
	ArrayList<Integer> tempList; 
	public ArrayList<ArrayList<MethodNode>> allPathList = new ArrayList<ArrayList<MethodNode>>();
	
	public GetAllICGPath(InterCallGraph interCallGraph, MethodNode start, MethodNode end) {  
        this.interCallGraph = interCallGraph;  
        this.start = start.matrixNumber;  
        this.end = end.matrixNumber;  
    } 
	
	public boolean getResult() {   
        n = interCallGraph.getINTERMETHOD().size();  
        theStack = new Stack<Integer>();  
  
        if (!isConnectable(start, end)) {  
        	hasPath = false;  
//            counterexample = "�ڵ�֮��û��ͨ·";  
        } else {  
            for (int j = 0; j < n; j++) {  
                tempList = new ArrayList<Integer>();  
                for (int i = 0; i < n; i++) {  
                    tempList.add(0);
                }  
                interCallGraph.getMethodNodeList()[j].setAllVisitedList(tempList);  
            }  
  
            hasPath = getAllPathList(start, end);  
        }  
        return hasPath;  
    }  
  
    private boolean getAllPathList(int start, int end) {  
    	interCallGraph.getMethodNodeList()[start].visited = true; // mark it  
        theStack.push(start); // push it  
  
        while (!theStack.isEmpty()) {  
            int v = getAdjUnvisitedVertex(theStack.peek());  
            if (v == -1) // if no such vertex,  
            {  
                tempList = new ArrayList<Integer>();  
                for (int j = 0; j < n; j++) {  
                    tempList.add(0);  
                }  
                interCallGraph.getMethodNodeList()[theStack.peek()].setAllVisitedList(tempList);// ��ջ���ڵ���ʹ��Ľڵ��������  
                theStack.pop();  
            } else // if it exists,  
            {  
                theStack.push(v); // push it  
            }  
  
            if (!theStack.isEmpty() && end == theStack.peek()) {  
            	interCallGraph.getMethodNodeList()[end].visited = false; // mark it  
                printTheStack(theStack);  
//                System.out.println();  
                theStack.pop();  
            }  
        }  
  
        return hasPath;  
    }  
  
    // �ж������ڵ��Ƿ�����ͨ  
    private boolean isConnectable(int start, int end) {  
        ArrayList<Integer> queue = new ArrayList<Integer>();  
        ArrayList<Integer> visited = new ArrayList<Integer>();  
        queue.add(start);  
        while (!queue.isEmpty()) {  
            for (int j = 0; j < n; j++) {  
                if (interCallGraph.getAdjustMatrix()[start][j] == 1 && !visited.contains(j)) {  
                    queue.add(j);  
                }  
            }  
            if (queue.contains(end)) {  
                return true;  
            } else {  
                visited.add(queue.get(0));  
                queue.remove(0);  
                if (!queue.isEmpty()) {  
                    start = queue.get(0);  
                }  
            }  
        }  
        return false;  
    }  
  
    // ��ڵ�v���ڣ���������ڵ�û�б����ʵ�����������ڵ㲻��ջ��  
    public int getAdjUnvisitedVertex(int v) {  
        ArrayList<Integer> arrayList = interCallGraph.getMethodNodeList()[v]  
                .getAllVisitedList();  
        for (int j = 0; j < n; j++) {  
            if (interCallGraph.getAdjustMatrix()[v][j] == 1 && arrayList.get(j) == 0 && !theStack.contains(j)) {  
            	interCallGraph.getMethodNodeList()[v].setVisited(j);  
                return j;  
            }  
        }  
        return -1;  
    } // end getAdjUnvisitedVertex()  
  
//    public void printTheStack(Stack<Integer> theStack2) {  
//        for (Integer integer : theStack2) {  
//            System.out.print(cGraph.displayCVexNode(integer));  
//            if (integer != theStack2.peek()) {  
//                System.out.print("-->");  
//            }  
//        }  
//    }  
    
    public void printTheStack(Stack<Integer> theStack2){
    	ArrayList<MethodNode> list = new ArrayList<MethodNode>();
    	for (Integer integer : theStack2) {  
			list.add(interCallGraph.displayMethodNode(integer));    
    	} 
    	allPathList.add(list);
    }
}
