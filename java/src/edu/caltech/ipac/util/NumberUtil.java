/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import java.text.NumberFormat;

/**
 * A class with static methods that do operations on Numbers.
 * @author Tatiana Goldina
 */
public class NumberUtil {


    /**
     * Truncate double precision number to a given number
     * of decimal places.
     * @param d double precision number to truncate
     * @param decimalPlaces number of decimal places
     * @return truncated double
     */
    public static double truncate( double d, int decimalPlaces ) {
        double shift = Math.pow(10, decimalPlaces);
        return Math.floor(d*shift)/shift;
    }

    /**
     * Format a double precision number truncated
     * to a given number of decimal places.
     * @param d double precision number to format
     * @param integerPlaces minimum number of integer digits to display
     * @param decimalPlaces number of decimal digits to display
     * @return formatted string for a truncated double
     */
    public static String formatTruncated( double d, int integerPlaces, int decimalPlaces) {
        NumberFormat nf = NumberFormat.getInstance();
        // NumberFormat class default value for grouping used is set to true and group size of 3 
        boolean groupUsed = false; 
        nf.setMinimumIntegerDigits(integerPlaces);
        nf.setMinimumFractionDigits(decimalPlaces);
        nf.setMaximumFractionDigits(decimalPlaces);
        nf.setGroupingUsed(groupUsed);
        return nf.format(truncate(d, decimalPlaces));
    }

   public static void main(String args[]) {
       try {
           if (args.length != 3) {
               System.err.println("Usage: truncate d integerPlaces decimalPlaces");
               System.exit(1);
           }
           double d = Double.parseDouble(args[0]);
           int integerPlaces = Integer.parseInt(args[1]);
           int decimalPlaces = Integer.parseInt(args[2]);

           System.out.println("Truncating "+d+" to "+decimalPlaces+" places: "+
                              truncate(d, decimalPlaces));

           System.out.println("Format with "+integerPlaces+" integerPlaces: "+
                              formatTruncated(d, integerPlaces, decimalPlaces));
           System.exit(0);
       } catch (Exception e) {
           System.err.println("Can not truncate/format number \""+args[0]+
                              "\" to \""+args[1]+"\" integer place(s)"+
                              " and \""+args[2]+"\" decimal place(s).");
           System.exit(1);
       }
   }
}


