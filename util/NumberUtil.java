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


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
