package softtest.CharacteristicExtract.c;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class testM {
	 public static void main(String[] args) {  
	        // TODO Auto-generated method stub  
		 
		 double [][] array = {
	              {-1,1,56},
	              {-4,13,0},
	              {1 ,0,2}};
		
     Matrix A = new Matrix(array);//构造矩阵  
	     A.eig().getD().print(0,2);
	    
	     
	      EigenvalueDecomposition Eig = A.eig();  
	      Matrix D = Eig.getD();  
	       Matrix V = Eig.getV();  
	   D.print(D.getColumnDimension(), D.getRowDimension());//打印特征值  
	   double res = 0;
	   for(int i = 0 ; i < D.getColumnDimension(); i++){
			res += D.get(i, i);
		}
	  
	   System.out.println( Math.floor(res));
	    }  
}
