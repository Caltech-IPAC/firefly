package edu.caltech.ipac.astro.simbad;


public class SimbadTest {
   public static void main(String args[]) {

      SimbadClient  sClient = new SimbadClient();
      SimbadObject       simbadO; 
      try {
	 simbadO = sClient.searchByName("m31");
	 System.out.println(simbadO.getRa());
	 System.out.println(simbadO.getDec());
	 }
      catch (Exception e) {
         System.out.println("exception " + e.getMessage());
         }
      // output should be 10.69125, 41.27166667
   }
}


